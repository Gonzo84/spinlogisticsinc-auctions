package eu.auctionplatform.seller.infrastructure.nats

import com.fasterxml.jackson.databind.JsonNode
import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.seller.infrastructure.persistence.repository.SellerProfileRepository
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// =============================================================================
// Payment Event Seller Consumer -- NATS JetStream subscriber for payment events
// =============================================================================

/**
 * Consumes payment domain events from NATS JetStream and updates seller
 * settlement records and lot payment info accordingly.
 *
 * Subscribed subjects (on the PAYMENT stream):
 * - `payment.settlement.ready` -- INSERT into seller_settlements with netAmount.
 * - `payment.checkout.completed` -- UPDATE seller_lots with payment info.
 *
 * Uses a durable pull consumer named "seller-payment-consumer" on the
 * "PAYMENT" stream to ensure at-least-once delivery and survive restarts.
 */
@Singleton
class PaymentEventSellerConsumer @Inject constructor(
    connection: Connection,
    private val sellerProfileRepository: SellerProfileRepository
) : NatsConsumer(
    connection = connection,
    streamName = STREAM_NAME,
    durableName = DURABLE_NAME,
    filterSubject = FILTER_SUBJECT,
    maxRedeliveries = 5,
    deadLetterSubject = "seller.dlq.payment",
    batchSize = 10
) {

    private var consumerThread: Thread? = null

    companion object {
        private val LOG: Logger = Logger.getLogger(PaymentEventSellerConsumer::class.java)

        /** NATS JetStream stream for payment events. */
        const val STREAM_NAME: String = "PAYMENT"

        /** Durable consumer name -- persists across restarts. */
        const val DURABLE_NAME: String = "seller-payment-consumer"

        /** Subject filter matching all payment events. */
        const val FILTER_SUBJECT: String = "payment.>"

        // Subject prefixes for routing
        private const val SUBJECT_SETTLEMENT_READY = "payment.settlement.ready"
        private const val SUBJECT_SETTLEMENT_SETTLED = "payment.settlement.settled"
        private const val SUBJECT_CHECKOUT_COMPLETED = "payment.checkout.completed"
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts the consumer loop in a dedicated daemon thread on application startup.
     */
    fun onStart(@Observes event: StartupEvent) {
        LOG.infof("Starting PaymentEventSellerConsumer [durable=%s]", DURABLE_NAME)
        consumerThread = Thread({
            start()
        }, "seller-payment-consumer").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Signals the consumer loop to stop gracefully on application shutdown.
     */
    fun onStop(@Observes event: ShutdownEvent) {
        LOG.infof("Stopping PaymentEventSellerConsumer [durable=%s]", DURABLE_NAME)
        stop()
        consumerThread?.interrupt()
    }

    // -------------------------------------------------------------------------
    // Message handling
    // -------------------------------------------------------------------------

    /**
     * Routes an incoming NATS message to the appropriate handler based on the
     * subject pattern.
     */
    override fun handleMessage(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        LOG.debugf("Received payment event on subject [%s], payload size=%s bytes",
            subject, message.data.size)

        try {
            when {
                subject.startsWith(SUBJECT_SETTLEMENT_SETTLED) -> handleSettlementSettled(payload)
                subject.startsWith(SUBJECT_SETTLEMENT_READY) -> handleSettlementReady(payload)
                subject.startsWith(SUBJECT_CHECKOUT_COMPLETED) -> handleCheckoutCompleted(payload)
                else -> LOG.debugf("Ignoring unrelated subject [%s] on payment stream", subject)
            }
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to process payment event on [%s]: %s", subject, ex.message)
            throw ex // re-throw so the base class handles nak/dead-letter
        }
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    /**
     * Handles `payment.settlement.ready` events by inserting a settlement
     * record into seller_settlements and updating seller metrics.
     *
     * Expected payload fields (from [SettlementReadyEvent]):
     * - `sellerId` -- seller to be paid out
     * - `netAmount` -- net amount after commission (NOT `amount`)
     * - `commission` -- platform commission withheld
     * - `currency` -- ISO 4217 currency code
     * - `paymentId` -- originating payment identifier
     *
     * Note: Uses `netAmount` (not `amount`) as the SettlementReadyEvent
     * field is named `netAmount`, not `amount`.
     */
    private fun handleSettlementReady(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val sellerIdStr = node.requiredText("sellerId")
        val netAmount = node.requiredDecimal("netAmount")
        val commission = node.optionalDecimal("commission") ?: BigDecimal.ZERO
        val currency = node.optionalText("currency") ?: "EUR"
        val paymentIdStr = node.optionalText("paymentId")

        val sellerId = UUID.fromString(sellerIdStr)
        val paymentId = paymentIdStr?.let {
            try { UUID.fromString(it) } catch (_: IllegalArgumentException) { null }
        }

        // Resolve seller profile id (sellerId in event may be the user_id)
        val sellerProfileId = sellerProfileRepository.findSellerProfileIdByUserId(sellerId)
            ?: sellerId // fall back to using it directly as seller_profile.id

        LOG.infof("Recording settlement for seller %s (netAmount=%s, commission=%s)",
            sellerProfileId, netAmount, commission)

        // Compute hammer price as netAmount + commission
        val hammerPrice = netAmount.add(commission)

        // Determine lotId from the event -- settlement may reference a lot indirectly
        val lotIdStr = node.optionalText("lotId") ?: node.optionalText("aggregateId")
        val lotId = lotIdStr?.let {
            try { UUID.fromString(it) } catch (_: IllegalArgumentException) { null }
        }

        // Insert settlement record (with paymentId for later status updates)
        sellerProfileRepository.insertSettlement(
            sellerId = sellerProfileId,
            lotId = lotId ?: UUID.randomUUID(), // fallback if no lotId
            lotTitle = null,
            hammerPrice = hammerPrice,
            commission = commission,
            netAmount = netAmount,
            currency = currency,
            status = "READY",
            paymentId = paymentId
        )

        // Update seller metrics
        sellerProfileRepository.settlePayment(sellerProfileId, netAmount)

        LOG.infof("Settlement recorded for seller %s (net=%s %s)", sellerProfileId, netAmount, currency)
    }

    /**
     * Handles `payment.settlement.settled` events by updating the seller
     * settlement row status from "READY" to "PAID" and setting settled_at.
     *
     * Expected payload fields (from [PaymentSettledEvent]):
     * - `paymentId` -- originating payment identifier (used to locate the settlement row)
     * - `bankReference` -- bank/PSP reference for the transfer
     * - `settledAt` -- timestamp when the settlement was completed
     * - `sellerId` -- seller who received the payout
     */
    private fun handleSettlementSettled(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val paymentIdStr = node.requiredText("paymentId")
        val settledAtStr = node.optionalText("settledAt")
        val sellerIdStr = node.optionalText("sellerId")

        val paymentId = UUID.fromString(paymentIdStr)
        val settledAt = settledAtStr?.let {
            try { Instant.parse(it) } catch (_: Exception) { Instant.now() }
        } ?: Instant.now()

        LOG.infof("Settlement settled for paymentId=%s (settledAt=%s)", paymentId, settledAt)

        // Update the seller_settlements row status from READY to PAID
        val updated = sellerProfileRepository.updateSettlementStatusByPaymentId(
            paymentId = paymentId,
            status = "PAID",
            settledAt = settledAt
        )

        if (updated) {
            LOG.infof("Successfully updated seller settlement to PAID for paymentId=%s", paymentId)
        } else {
            LOG.warnf(
                "No seller settlement found for paymentId=%s (seller=%s) -- may not have been created yet",
                paymentId, sellerIdStr
            )
        }
    }

    /**
     * Handles `payment.checkout.completed` events by updating the seller lot
     * status to reflect that payment was received.
     *
     * Expected payload fields (from [CheckoutCompletedEvent]):
     * - `lotId` -- lot that was purchased
     * - `hammerPrice` -- winning bid amount
     * - `totalAmount` -- total charged (hammer + premium + VAT)
     */
    private fun handleCheckoutCompleted(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotIdStr = node.requiredText("lotId")
        val lotId = UUID.fromString(lotIdStr)
        val hammerPrice = node.optionalDecimal("hammerPrice")

        LOG.infof("Checkout completed for lot %s (hammerPrice=%s)", lotId, hammerPrice)

        // Update lot status to PAID
        sellerProfileRepository.updateLotStatus(lotId, "PAID")

        // Update current_bid to final hammer price if available
        if (hammerPrice != null) {
            sellerProfileRepository.updateLotBid(lotId, hammerPrice, null)
        }

        LOG.infof("Successfully updated lot %s to PAID status", lotId)
    }

    // -------------------------------------------------------------------------
    // JSON extraction helpers
    // -------------------------------------------------------------------------

    private fun JsonNode.requiredText(field: String): String {
        return this.get(field)?.asText()
            ?: throw IllegalArgumentException("Required field '$field' missing from payment event payload")
    }

    private fun JsonNode.requiredDecimal(field: String): BigDecimal {
        return this.get(field)?.decimalValue()
            ?: throw IllegalArgumentException("Required field '$field' missing from payment event payload")
    }

    private fun JsonNode.optionalText(field: String): String? =
        this.get(field)?.takeIf { !it.isNull }?.asText()

    private fun JsonNode.optionalDecimal(field: String): BigDecimal? =
        this.get(field)?.takeIf { !it.isNull }?.decimalValue()
}
