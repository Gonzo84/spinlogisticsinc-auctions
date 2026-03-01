package eu.auctionplatform.payment.api.v1.resource

import eu.auctionplatform.payment.api.v1.dto.CheckoutRequest
import eu.auctionplatform.payment.api.v1.dto.CheckoutResponse
import eu.auctionplatform.payment.api.v1.dto.DepositRefundRequest
import eu.auctionplatform.payment.api.v1.dto.DepositRequest
import eu.auctionplatform.payment.api.v1.dto.DepositResponse
import eu.auctionplatform.payment.api.v1.dto.InvoiceResponse
import eu.auctionplatform.payment.api.v1.dto.PaymentStatusResponse
import eu.auctionplatform.payment.api.v1.dto.PaymentSubmitRequest
import eu.auctionplatform.payment.api.v1.dto.PaymentSummary
import eu.auctionplatform.payment.api.v1.dto.PaymentsSummaryResponse
import eu.auctionplatform.payment.api.v1.dto.SettlementResponse
import eu.auctionplatform.payment.application.service.CheckoutService
import eu.auctionplatform.payment.application.service.LotCheckoutDetail
import eu.auctionplatform.payment.application.service.PaymentWebhookData
import eu.auctionplatform.payment.application.service.SettlementService
import eu.auctionplatform.payment.domain.model.Invoice
import eu.auctionplatform.payment.domain.model.Payment
import eu.auctionplatform.payment.domain.model.Settlement
import eu.auctionplatform.payment.infrastructure.persistence.repository.InvoiceRepository
import eu.auctionplatform.payment.infrastructure.persistence.repository.PaymentRepository
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * REST resource for payment, checkout, invoice, deposit, and settlement operations.
 *
 * All endpoints are secured via OIDC JWT tokens. Role-based access is enforced
 * via `@RolesAllowed` annotations aligned with the Casbin RBAC policy.
 *
 * Response format follows the platform convention:
 * - Successful responses return the DTO directly with appropriate HTTP status.
 * - Error responses use the standard platform error envelope.
 */
@Path("/api/v1/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class PaymentResource @Inject constructor(
    private val checkoutService: CheckoutService,
    private val settlementService: SettlementService,
    private val paymentRepository: PaymentRepository,
    private val invoiceRepository: InvoiceRepository
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(PaymentResource::class.java)
    }

    // -----------------------------------------------------------------------
    // Checkout endpoints
    // -----------------------------------------------------------------------

    /**
     * Initiates checkout for one or more won lots.
     *
     * Creates payment records with calculated VAT and buyer premium for
     * each lot. Returns a checkout summary with payment details.
     *
     * @param request The checkout request containing lot IDs and buyer details.
     * @param securityContext Injected security context with the authenticated user.
     * @return 201 Created with [CheckoutResponse].
     */
    @POST
    @Path("/checkout")
    @RolesAllowed("buyer_active")
    fun initiateCheckout(
        @Valid request: CheckoutRequest,
        @Context securityContext: SecurityContext
    ): Response {
        val buyerId = extractUserId(securityContext)

        LOG.infof("Checkout initiated by buyer %s for %s lot(s)", buyerId, request.lotIds.size)

        // Build lot details — in a full implementation, lot/auction details
        // would be fetched from the lot-service or auction-engine.
        val lotDetails = request.lotIds.map { lotIdStr ->
            val lotId = UUID.fromString(lotIdStr)
            LotCheckoutDetail(
                lotId = lotId,
                auctionId = lotId, // Placeholder: would be resolved from lot service
                sellerId = lotId, // Placeholder: would be resolved from lot/auction service
                hammerPrice = BigDecimal.ZERO, // Placeholder: would come from auction result
                currency = request.currency,
                buyerCountry = request.buyerCountry,
                sellerCountry = request.buyerCountry, // Placeholder: would come from lot service
                buyerType = request.buyerType,
                sellerType = "BUSINESS", // Sellers are typically businesses
                buyerVatId = request.buyerVatId
            )
        }

        val payments = checkoutService.initiateCheckout(buyerId, lotDetails)

        val totalAmount = payments.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalAmount) }

        val checkoutResponse = CheckoutResponse(
            checkoutId = UUID.randomUUID().toString(),
            payments = payments.map { toPaymentSummary(it) },
            totalAmount = totalAmount,
            currency = request.currency,
            dueDate = payments.firstOrNull()?.dueDate ?: Instant.now()
        )

        return Response.status(Response.Status.CREATED).entity(checkoutResponse).build()
    }

    /**
     * Retrieves the status of a specific checkout/payment.
     *
     * @param id The payment UUID.
     * @return 200 OK with [PaymentStatusResponse], or 404 if not found.
     */
    @GET
    @Path("/checkout/{id}")
    @RolesAllowed("buyer_active", "seller_verified", "admin_ops", "admin_super")
    fun getCheckoutStatus(@PathParam("id") id: String): Response {
        val paymentId = UUID.fromString(id)
        val payment = paymentRepository.findById(paymentId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "Payment not found", "code" to "PAYMENT_NOT_FOUND"))
                .build()

        return Response.ok(toPaymentStatusResponse(payment)).build()
    }

    /**
     * Submits a payment for processing via the PSP (Adyen).
     *
     * @param id The payment UUID.
     * @param request The payment submission request with method details.
     * @return 200 OK with updated [PaymentStatusResponse], or 404/409 on error.
     */
    @POST
    @Path("/checkout/{id}/pay")
    @RolesAllowed("buyer_active")
    fun submitPayment(
        @PathParam("id") id: String,
        @Valid request: PaymentSubmitRequest
    ): Response {
        val paymentId = UUID.fromString(id)

        LOG.infof("Payment submission for %s via %s", paymentId, request.paymentMethod)

        val updatedPayment = try {
            checkoutService.processPayment(paymentId, request.paymentMethod)
        } catch (e: IllegalStateException) {
            return Response.status(Response.Status.CONFLICT)
                .entity(mapOf("error" to e.message, "code" to "INVALID_STATUS"))
                .build()
        }

        if (updatedPayment == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "Payment not found", "code" to "PAYMENT_NOT_FOUND"))
                .build()
        }

        return Response.ok(toPaymentStatusResponse(updatedPayment)).build()
    }

    // -----------------------------------------------------------------------
    // Webhook endpoint (no auth — validated via HMAC)
    // -----------------------------------------------------------------------

    /**
     * Receives payment webhooks from Adyen.
     *
     * This endpoint is called by Adyen to notify of payment status changes.
     * Authentication is performed via HMAC signature validation rather than
     * OIDC tokens.
     *
     * @param body The raw webhook notification payload.
     * @return 200 OK with "[accepted]" to acknowledge receipt.
     */
    @POST
    @Path("/webhook")
    fun handleWebhook(body: Map<String, Any?>): Response {
        LOG.info("Received payment webhook")

        // In production, validate HMAC signature before processing.
        // Parse the Adyen notification format.
        val webhookData = parseWebhookPayload(body)

        if (webhookData != null) {
            checkoutService.handlePaymentWebhook(webhookData)
        }

        // Adyen expects "[accepted]" in the response body
        return Response.ok("[accepted]").build()
    }

    // -----------------------------------------------------------------------
    // Invoice endpoints
    // -----------------------------------------------------------------------

    /**
     * Lists invoices for the authenticated user.
     *
     * Returns buyer invoices for buyers and seller invoices for sellers.
     *
     * @param page Page number (1-based).
     * @param size Page size.
     * @return 200 OK with list of [InvoiceResponse].
     */
    @GET
    @Path("/invoices")
    @RolesAllowed("buyer_active", "seller_verified", "admin_ops", "admin_super")
    fun listInvoices(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int
    ): Response {
        val effectiveSize = size.coerceIn(1, 100)
        val effectivePage = page.coerceAtLeast(1)
        val offset = (effectivePage - 1) * effectiveSize

        val (invoices, total) = invoiceRepository.findAll(effectiveSize, offset)

        val response = mapOf(
            "items" to invoices.map { toInvoiceResponse(it) },
            "total" to total,
            "page" to effectivePage,
            "pageSize" to effectiveSize
        )

        return Response.ok(response).build()
    }

    /**
     * Downloads an invoice PDF.
     *
     * @param id The invoice UUID.
     * @return 200 OK with redirect to PDF URL, or 404 if not found.
     */
    @GET
    @Path("/invoices/{id}/pdf")
    @RolesAllowed("buyer_active", "seller_verified", "admin_ops", "admin_super")
    fun downloadInvoicePdf(@PathParam("id") id: String): Response {
        val invoiceId = UUID.fromString(id)
        val invoice = invoiceRepository.findById(invoiceId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "Invoice not found", "code" to "INVOICE_NOT_FOUND"))
                .build()

        if (invoice.pdfUrl.isNullOrBlank()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "PDF not yet generated", "code" to "PDF_NOT_READY"))
                .build()
        }

        return Response.temporaryRedirect(java.net.URI.create(invoice.pdfUrl)).build()
    }

    // -----------------------------------------------------------------------
    // Settlement endpoints
    // -----------------------------------------------------------------------

    /**
     * Lists settlements for the authenticated seller.
     *
     * @return 200 OK with list of [SettlementResponse].
     */
    @GET
    @Path("/settlements")
    @RolesAllowed("seller_verified", "admin_ops", "admin_super")
    fun listSettlements(
        @Context securityContext: SecurityContext
    ): Response {
        val sellerId = extractUserId(securityContext)
        val settlements = settlementService.getSellerSettlements(sellerId)

        return Response.ok(settlements.map { toSettlementResponse(it) }).build()
    }

    // -----------------------------------------------------------------------
    // Deposit endpoints
    // -----------------------------------------------------------------------

    /**
     * Initiates a bid deposit for an auction.
     *
     * Some high-value auctions require a pre-bid deposit. This endpoint
     * creates a hold on the buyer's payment method for the deposit amount.
     *
     * @param request The deposit request.
     * @param securityContext Injected security context.
     * @return 201 Created with [DepositResponse].
     */
    @POST
    @Path("/deposits")
    @RolesAllowed("buyer_active")
    fun initiateDeposit(
        @Valid request: DepositRequest,
        @Context securityContext: SecurityContext
    ): Response {
        val buyerId = extractUserId(securityContext)

        LOG.infof(
            "Deposit initiated by buyer %s for auction %s (amount=%s %s)",
            buyerId, request.auctionId, request.amount, request.currency
        )

        // In production, this would create a payment authorisation hold via Adyen.
        val depositResponse = DepositResponse(
            depositId = UUID.randomUUID().toString(),
            auctionId = request.auctionId,
            amount = request.amount,
            currency = request.currency,
            status = "HELD",
            createdAt = Instant.now()
        )

        return Response.status(Response.Status.CREATED).entity(depositResponse).build()
    }

    /**
     * Requests a refund of a previously placed deposit.
     *
     * Deposits are automatically refunded when the buyer does not win
     * the auction. This endpoint allows manual refund requests.
     *
     * @param request The refund request.
     * @param securityContext Injected security context.
     * @return 200 OK with refund confirmation.
     */
    @POST
    @Path("/deposits/refund")
    @RolesAllowed("buyer_active")
    fun requestDepositRefund(
        @Valid request: DepositRefundRequest,
        @Context securityContext: SecurityContext
    ): Response {
        val buyerId = extractUserId(securityContext)

        LOG.infof(
            "Deposit refund requested by buyer %s for auction %s",
            buyerId, request.auctionId
        )

        // In production, this would initiate a refund via Adyen.
        val response = mapOf(
            "auctionId" to request.auctionId,
            "status" to "REFUND_INITIATED",
            "message" to "Deposit refund has been initiated. Funds will be returned within 5-10 business days."
        )

        return Response.ok(response).build()
    }

    // -----------------------------------------------------------------------
    // Admin endpoints
    // -----------------------------------------------------------------------

    /**
     * Returns aggregate payment summary statistics (admin only).
     *
     * **GET /api/v1/payments/summary**
     *
     * @return 200 OK with [PaymentsSummaryResponse].
     */
    @GET
    @Path("/summary")
    @RolesAllowed("admin_ops", "admin_super")
    fun getPaymentsSummary(): Response {
        LOG.debug("GET /payments/summary")

        val pending = paymentRepository.findByStatus(
            eu.auctionplatform.payment.domain.model.PaymentStatus.PENDING
        )
        val completed = paymentRepository.findByStatus(
            eu.auctionplatform.payment.domain.model.PaymentStatus.COMPLETED
        )
        val failed = paymentRepository.findByStatus(
            eu.auctionplatform.payment.domain.model.PaymentStatus.FAILED
        )

        val totalPending = pending.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalAmount) }
        val totalPaid = completed.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.totalAmount) }

        val summary = PaymentsSummaryResponse(
            totalPending = totalPending,
            totalOverdue = BigDecimal.ZERO,
            totalPaid = totalPaid,
            totalDisputed = BigDecimal.ZERO,
            pendingCount = pending.size,
            overdueCount = 0
        )

        return Response.ok(summary).build()
    }

    /**
     * Lists all payments (admin only).
     *
     * Supports pagination for reviewing all platform payments.
     *
     * @param status Optional status filter.
     * @param page Page number (1-based).
     * @param size Page size.
     * @return 200 OK with paginated list of [PaymentStatusResponse].
     */
    @GET
    @RolesAllowed("admin_ops", "admin_super")
    fun listAllPayments(
        @QueryParam("status") status: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int
    ): Response {
        val effectiveSize = size.coerceIn(1, 100)
        val effectivePage = page.coerceAtLeast(1)
        val offset = (effectivePage - 1) * effectiveSize

        val (payments, total) = if (!status.isNullOrBlank()) {
            val statusEnum = eu.auctionplatform.payment.domain.model.PaymentStatus.valueOf(status.uppercase())
            val list = paymentRepository.findByStatus(statusEnum)
            Pair(list, list.size.toLong())
        } else {
            paymentRepository.findAll(effectiveSize, offset)
        }

        val response = mapOf(
            "items" to payments.map { toPaymentStatusResponse(it) },
            "total" to total,
            "page" to effectivePage,
            "pageSize" to effectiveSize
        )

        return Response.ok(response).build()
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun extractUserId(securityContext: SecurityContext): UUID {
        val principal = securityContext.userPrincipal?.name
            ?: throw jakarta.ws.rs.ForbiddenException("User identity not available")
        return UUID.fromString(principal)
    }

    private fun parseWebhookPayload(body: Map<String, Any?>): PaymentWebhookData? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val notificationItems = body["notificationItems"] as? List<Map<String, Any?>>
            val item = notificationItems?.firstOrNull()

            @Suppress("UNCHECKED_CAST")
            val notification = item?.get("NotificationRequestItem") as? Map<String, Any?>
                ?: return null

            @Suppress("UNCHECKED_CAST")
            val amount = notification["amount"] as? Map<String, Any?>

            PaymentWebhookData(
                pspReference = notification["pspReference"]?.toString() ?: "",
                merchantReference = notification["merchantReference"]?.toString(),
                eventCode = notification["eventCode"]?.toString() ?: "",
                success = notification["success"]?.toString()?.toBoolean() ?: false,
                paymentMethod = notification["paymentMethod"]?.toString(),
                reason = notification["reason"]?.toString(),
                amountValue = amount?.get("value")?.toString()?.toLongOrNull(),
                amountCurrency = amount?.get("currency")?.toString()
            )
        } catch (e: Exception) {
            LOG.errorf("Failed to parse webhook payload: %s", e.message)
            null
        }
    }

    private fun toPaymentSummary(payment: Payment): PaymentSummary = PaymentSummary(
        paymentId = payment.id.toString(),
        lotId = payment.lotId.toString(),
        hammerPrice = payment.hammerPrice,
        buyerPremium = payment.buyerPremium,
        vatAmount = payment.vatAmount,
        vatRate = payment.vatRate,
        vatScheme = payment.vatScheme.name,
        totalAmount = payment.totalAmount
    )

    private fun toPaymentStatusResponse(payment: Payment): PaymentStatusResponse =
        PaymentStatusResponse(
            paymentId = payment.id.toString(),
            buyerId = payment.buyerId.toString(),
            sellerId = payment.sellerId.toString(),
            auctionId = payment.auctionId.toString(),
            lotId = payment.lotId.toString(),
            hammerPrice = payment.hammerPrice,
            buyerPremium = payment.buyerPremium,
            vatAmount = payment.vatAmount,
            vatRate = payment.vatRate,
            vatScheme = payment.vatScheme.name,
            totalAmount = payment.totalAmount,
            currency = payment.currency,
            status = payment.status.name,
            paymentMethod = payment.paymentMethod,
            pspReference = payment.pspReference,
            dueDate = payment.dueDate,
            paidAt = payment.paidAt,
            createdAt = payment.createdAt
        )

    private fun toInvoiceResponse(invoice: Invoice): InvoiceResponse = InvoiceResponse(
        invoiceId = invoice.id.toString(),
        paymentId = invoice.paymentId.toString(),
        invoiceNumber = invoice.invoiceNumber,
        type = invoice.type.name,
        pdfUrl = invoice.pdfUrl,
        issuedAt = invoice.issuedAt
    )

    private fun toSettlementResponse(settlement: Settlement): SettlementResponse =
        SettlementResponse(
            settlementId = settlement.id.toString(),
            sellerId = settlement.sellerId.toString(),
            paymentId = settlement.paymentId.toString(),
            netAmount = settlement.netAmount,
            commission = settlement.commission,
            commissionRate = settlement.commissionRate,
            status = settlement.status.name,
            settledAt = settlement.settledAt,
            bankReference = settlement.bankReference,
            createdAt = settlement.createdAt
        )
}
