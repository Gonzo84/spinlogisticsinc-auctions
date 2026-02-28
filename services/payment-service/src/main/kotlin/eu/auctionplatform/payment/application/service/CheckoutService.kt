package eu.auctionplatform.payment.application.service

import eu.auctionplatform.payment.domain.model.Invoice
import eu.auctionplatform.payment.domain.model.InvoiceType
import eu.auctionplatform.payment.domain.model.Payment
import eu.auctionplatform.payment.domain.model.PaymentStatus
import eu.auctionplatform.payment.infrastructure.persistence.repository.InvoiceRepository
import eu.auctionplatform.payment.infrastructure.persistence.repository.PaymentRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Orchestrates the checkout flow for won auction lots.
 *
 * Responsibilities:
 * - Create payment records with correct VAT calculation.
 * - Initiate payment processing via the PSP (Adyen).
 * - Handle incoming payment webhooks from Adyen.
 * - Generate invoices upon payment completion.
 *
 * ## Checkout flow
 *
 * 1. Buyer wins lot(s) in an auction.
 * 2. [initiateCheckout] creates [Payment] record(s) with PENDING status.
 * 3. Buyer selects payment method and calls [processPayment].
 * 4. PSP processes the payment and sends a webhook.
 * 5. [handlePaymentWebhook] updates the payment status and triggers
 *    settlement creation and invoice generation.
 */
@ApplicationScoped
class CheckoutService @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val invoiceRepository: InvoiceRepository,
    private val vatCalculationService: VatCalculationService,
    private val settlementService: SettlementService
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(CheckoutService::class.java)

        /** Default buyer premium rate (15%). */
        val DEFAULT_BUYER_PREMIUM_RATE: BigDecimal = BigDecimal("0.15")

        /** Payment due within 5 days of checkout. */
        private const val PAYMENT_DUE_DAYS = 5L

        /** Invoice number sequence counter (in production, use DB sequence). */
        private val invoiceSequence = AtomicLong(0)
    }

    /**
     * Initiates checkout for one or more won lots.
     *
     * Creates a [Payment] record for each lot with the calculated VAT,
     * buyer premium, and total amount. All payments are created with
     * [PaymentStatus.PENDING].
     *
     * @param buyerId The buyer's UUID.
     * @param lotDetails List of lot details (lotId, auctionId, hammerPrice,
     *        sellerCountry, buyerCountry, buyerType, sellerType, buyerVatId).
     * @return List of created payment records.
     */
    fun initiateCheckout(
        buyerId: UUID,
        lotDetails: List<LotCheckoutDetail>
    ): List<Payment> {
        LOG.infof("Initiating checkout for buyer %s with %s lot(s)", buyerId, lotDetails.size)

        val now = Instant.now()
        val dueDate = now.plusSeconds(PAYMENT_DUE_DAYS * 24 * 60 * 60)

        val payments = lotDetails.map { lot ->
            val buyerPremium = lot.hammerPrice
                .multiply(DEFAULT_BUYER_PREMIUM_RATE)
                .setScale(2, RoundingMode.HALF_UP)

            val vatResult = vatCalculationService.calculateVat(
                hammerPrice = lot.hammerPrice,
                buyerPremium = buyerPremium,
                buyerCountry = lot.buyerCountry,
                sellerCountry = lot.sellerCountry,
                buyerType = lot.buyerType,
                sellerType = lot.sellerType,
                buyerVatId = lot.buyerVatId
            )

            val totalAmount = lot.hammerPrice
                .add(buyerPremium)
                .add(vatResult.vatAmount)
                .setScale(2, RoundingMode.HALF_UP)

            val payment = Payment(
                id = UUID.randomUUID(),
                buyerId = buyerId,
                sellerId = lot.sellerId,
                auctionId = lot.auctionId,
                lotId = lot.lotId,
                hammerPrice = lot.hammerPrice,
                buyerPremium = buyerPremium,
                buyerPremiumRate = DEFAULT_BUYER_PREMIUM_RATE,
                vatAmount = vatResult.vatAmount,
                vatRate = vatResult.vatRate,
                vatScheme = vatResult.vatScheme,
                totalAmount = totalAmount,
                currency = lot.currency,
                country = lot.buyerCountry,
                paymentMethod = null,
                pspReference = null,
                status = PaymentStatus.PENDING,
                dueDate = dueDate,
                paidAt = null,
                createdAt = now
            )

            paymentRepository.save(payment)

            LOG.infof(
                "Created payment %s for lot %s (total=%s %s, vat=%s, scheme=%s)",
                payment.id, lot.lotId, totalAmount, lot.currency,
                vatResult.vatAmount, vatResult.vatScheme
            )

            payment
        }

        return payments
    }

    /**
     * Submits a payment for processing via the PSP (Adyen).
     *
     * Transitions the payment from PENDING to PROCESSING and initiates
     * the payment with Adyen. The actual payment result will arrive
     * asynchronously via webhook.
     *
     * @param paymentId The payment UUID.
     * @param paymentMethod The payment method chosen by the buyer (e.g. "card", "ideal", "bank_transfer").
     * @return The updated payment, or null if not found.
     * @throws IllegalStateException if the payment is not in PENDING status.
     */
    fun processPayment(paymentId: UUID, paymentMethod: String): Payment? {
        val payment = paymentRepository.findById(paymentId)
            ?: return null

        check(payment.status == PaymentStatus.PENDING) {
            "Payment $paymentId is in status ${payment.status}, expected PENDING"
        }

        LOG.infof(
            "Processing payment %s via %s (amount=%s %s)",
            paymentId, paymentMethod, payment.totalAmount, payment.currency
        )

        // Transition to PROCESSING
        paymentRepository.updateStatus(paymentId, PaymentStatus.PROCESSING)

        // In a real implementation, this would call the Adyen API:
        // - Create a payment session
        // - Return redirect URL or payment action for 3DS
        // - The actual result comes back via webhook
        //
        // For now, we simulate the PSP interaction by updating the payment method.
        // The webhook handler will complete the flow.

        return paymentRepository.findById(paymentId)
    }

    /**
     * Handles an incoming payment webhook from Adyen.
     *
     * Processes the webhook notification, updates the payment status, and
     * triggers downstream actions (settlement creation, invoice generation)
     * on successful payment.
     *
     * @param webhookData The parsed webhook payload.
     * @return true if the webhook was processed successfully, false otherwise.
     */
    fun handlePaymentWebhook(webhookData: PaymentWebhookData): Boolean {
        LOG.infof(
            "Processing payment webhook: pspReference=%s, eventCode=%s, success=%s",
            webhookData.pspReference, webhookData.eventCode, webhookData.success
        )

        val payment = webhookData.merchantReference?.let { ref ->
            try {
                paymentRepository.findById(UUID.fromString(ref))
            } catch (e: IllegalArgumentException) {
                LOG.warnf("Invalid merchant reference in webhook: %s", ref)
                null
            }
        }

        if (payment == null) {
            LOG.warnf(
                "Payment not found for webhook pspReference=%s, merchantReference=%s",
                webhookData.pspReference, webhookData.merchantReference
            )
            return false
        }

        return when (webhookData.eventCode) {
            "AUTHORISATION" -> handleAuthorisation(payment, webhookData)
            "CANCELLATION" -> handleCancellation(payment, webhookData)
            "REFUND" -> handleRefund(payment, webhookData)
            else -> {
                LOG.warnf("Unhandled webhook event code: %s", webhookData.eventCode)
                true // Acknowledge but do not process
            }
        }
    }

    // -----------------------------------------------------------------------
    // Internal webhook handlers
    // -----------------------------------------------------------------------

    private fun handleAuthorisation(payment: Payment, webhookData: PaymentWebhookData): Boolean {
        // BUG-12: Guard against duplicate webhooks for already-finalized payments
        if (payment.status == PaymentStatus.COMPLETED || payment.status == PaymentStatus.FAILED) {
            LOG.warnf("Ignoring duplicate webhook for already-finalized payment %s (status=%s)", payment.id, payment.status)
            return true
        }

        if (webhookData.success) {
            val now = Instant.now()
            paymentRepository.markCompleted(
                id = payment.id,
                paymentMethod = webhookData.paymentMethod ?: "unknown",
                pspReference = webhookData.pspReference,
                paidAt = now
            )

            LOG.infof("Payment %s completed successfully (psp=%s)", payment.id, webhookData.pspReference)

            // Generate invoices — failure should not prevent settlement creation
            try {
                generateInvoices(payment)
            } catch (e: Exception) {
                LOG.errorf(e, "Failed to generate invoices for payment %s: %s", payment.id, e.message)
            }

            // Create settlement for the seller — failure should not affect payment status
            try {
                settlementService.createSettlement(payment.id)
            } catch (e: Exception) {
                LOG.errorf(e, "Failed to create settlement for payment %s: %s", payment.id, e.message)
            }

            return true
        } else {
            paymentRepository.updateStatus(payment.id, PaymentStatus.FAILED)
            LOG.warnf(
                "Payment %s failed: reason=%s (psp=%s)",
                payment.id, webhookData.reason, webhookData.pspReference
            )
            return true
        }
    }

    private fun handleCancellation(payment: Payment, webhookData: PaymentWebhookData): Boolean {
        paymentRepository.updateStatus(payment.id, PaymentStatus.FAILED)
        LOG.infof("Payment %s cancelled (psp=%s)", payment.id, webhookData.pspReference)
        return true
    }

    private fun handleRefund(payment: Payment, webhookData: PaymentWebhookData): Boolean {
        if (webhookData.success) {
            paymentRepository.updateStatus(payment.id, PaymentStatus.REFUNDED)
            LOG.infof("Payment %s refunded (psp=%s)", payment.id, webhookData.pspReference)
        }
        return true
    }

    // -----------------------------------------------------------------------
    // Invoice generation
    // -----------------------------------------------------------------------

    private fun generateInvoices(payment: Payment) {
        val now = Instant.now()
        val year = LocalDate.ofInstant(now, ZoneOffset.UTC).year

        // Buyer invoice
        val buyerInvoice = Invoice(
            id = UUID.randomUUID(),
            paymentId = payment.id,
            invoiceNumber = generateInvoiceNumber(year, "B"),
            type = InvoiceType.BUYER,
            pdfUrl = null, // PDF generated asynchronously
            issuedAt = now
        )
        invoiceRepository.save(buyerInvoice)
        LOG.infof("Generated buyer invoice %s for payment %s", buyerInvoice.invoiceNumber, payment.id)

        // Seller invoice (self-billing / credit note)
        val sellerInvoice = Invoice(
            id = UUID.randomUUID(),
            paymentId = payment.id,
            invoiceNumber = generateInvoiceNumber(year, "S"),
            type = InvoiceType.SELLER,
            pdfUrl = null,
            issuedAt = now
        )
        invoiceRepository.save(sellerInvoice)
        LOG.infof("Generated seller invoice %s for payment %s", sellerInvoice.invoiceNumber, payment.id)
    }

    /**
     * Generates a sequential invoice number.
     *
     * Format: INV-{year}-{type}{sequence} (e.g. "INV-2026-B000123").
     * In production, this should use a database sequence to guarantee uniqueness
     * across service instances.
     */
    private fun generateInvoiceNumber(year: Int, typePrefix: String): String {
        val seq = invoiceSequence.incrementAndGet()
        return "INV-$year-$typePrefix${seq.toString().padStart(6, '0')}"
    }
}

/**
 * Details for a single lot being checked out.
 */
data class LotCheckoutDetail(
    val lotId: UUID,
    val auctionId: UUID,
    val sellerId: UUID,
    val hammerPrice: BigDecimal,
    val currency: String = "EUR",
    val buyerCountry: String,
    val sellerCountry: String,
    val buyerType: String,
    val sellerType: String,
    val buyerVatId: String?
)

/**
 * Parsed Adyen webhook notification data.
 */
data class PaymentWebhookData(
    /** Adyen PSP reference. */
    val pspReference: String,
    /** Our payment ID, sent as merchantReference to Adyen. */
    val merchantReference: String?,
    /** Webhook event code (e.g. "AUTHORISATION", "CANCELLATION", "REFUND"). */
    val eventCode: String,
    /** Whether the operation was successful. */
    val success: Boolean,
    /** Payment method type (e.g. "visa", "ideal", "sepadirectdebit"). */
    val paymentMethod: String?,
    /** Failure reason, if applicable. */
    val reason: String?,
    /** Amount in minor units (cents). */
    val amountValue: Long?,
    /** ISO 4217 currency code. */
    val amountCurrency: String?
)
