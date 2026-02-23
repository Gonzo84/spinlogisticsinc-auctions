package eu.auctionplatform.notification.infrastructure.nats

import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.notification.application.service.NotificationService
import eu.auctionplatform.notification.domain.model.NotificationType
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * NATS JetStream consumer that listens for payment-related domain events
 * and triggers the appropriate notifications.
 *
 * Subscribed subjects:
 * - `payment.checkout.completed` -- Triggers [NotificationType.PAYMENT_RECEIVED]
 *   to the buyer who completed the checkout.
 * - `payment.settlement.ready` -- Triggers [NotificationType.SETTLEMENT_PAID]
 *   to the seller whose settlement has been processed.
 *
 * Uses the durable consumer name `notification-payment-consumer` to survive
 * restarts and ensure at-least-once delivery.
 */
@ApplicationScoped
@Startup
class PaymentEventNotificationConsumer @Inject constructor(
    private val connection: Connection,
    private val notificationService: NotificationService
) {

    private val logger = LoggerFactory.getLogger(PaymentEventNotificationConsumer::class.java)

    companion object {
        private const val STREAM_NAME = "PAYMENT"
        private const val DURABLE_NAME = "notification-payment-consumer"

        private const val CHECKOUT_COMPLETED_FILTER = "payment.checkout.completed"
        private const val SETTLEMENT_READY_FILTER = "payment.settlement.ready"
    }

    private val executor: ExecutorService = Executors.newFixedThreadPool(2)

    /**
     * Starts consumer threads for payment event subjects.
     */
    @jakarta.annotation.PostConstruct
    fun init() {
        logger.info("Starting payment event notification consumers")

        executor.submit { createCheckoutCompletedConsumer().start() }
        executor.submit { createSettlementReadyConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        logger.info("Shutting down payment event notification consumers")
        executor.shutdownNow()
    }

    // -----------------------------------------------------------------------
    // Consumer factories
    // -----------------------------------------------------------------------

    private fun createCheckoutCompletedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-checkout",
            filterSubject = CHECKOUT_COMPLETED_FILTER,
            deadLetterSubject = "dlq.notification.payment.checkout"
        ) {
            override fun handleMessage(message: Message) {
                handleCheckoutCompleted(message)
            }
        }

    private fun createSettlementReadyConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-settlement",
            filterSubject = SETTLEMENT_READY_FILTER,
            deadLetterSubject = "dlq.notification.payment.settlement"
        ) {
            override fun handleMessage(message: Message) {
                handleSettlementReady(message)
            }
        }

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    /**
     * Handles `payment.checkout.completed` events.
     *
     * Sends [NotificationType.PAYMENT_RECEIVED] to the buyer confirming
     * that their payment was successfully processed.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleCheckoutCompleted(message: Message) {
        val payload = parsePayload(message) ?: return

        val buyerId = payload["buyerId"]?.toString() ?: return
        val totalAmount = payload["totalAmount"]?.toString() ?: "0"
        val currency = payload["currency"]?.toString() ?: "EUR"
        val orderId = payload["orderId"]?.toString() ?: ""
        val auctionId = payload["auctionId"]?.toString() ?: ""

        val data = mapOf(
            "orderId" to orderId,
            "auctionId" to auctionId,
            "totalAmount" to totalAmount,
            "currency" to currency
        )

        notificationService.sendNotification(
            userId = UUID.fromString(buyerId),
            type = NotificationType.PAYMENT_RECEIVED,
            data = data
        )

        logger.debug(
            "Sent PAYMENT_RECEIVED to buyer={} for order={} (amount={} {})",
            buyerId, orderId, totalAmount, currency
        )
    }

    /**
     * Handles `payment.settlement.ready` events.
     *
     * Sends [NotificationType.SETTLEMENT_PAID] to the seller confirming
     * that their settlement has been processed and funds transferred.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleSettlementReady(message: Message) {
        val payload = parsePayload(message) ?: return

        val sellerId = payload["sellerId"]?.toString() ?: return
        val settlementAmount = payload["settlementAmount"]?.toString() ?: "0"
        val currency = payload["currency"]?.toString() ?: "EUR"
        val settlementId = payload["settlementId"]?.toString() ?: ""

        val data = mapOf(
            "settlementId" to settlementId,
            "settlementAmount" to settlementAmount,
            "currency" to currency
        )

        notificationService.sendNotification(
            userId = UUID.fromString(sellerId),
            type = NotificationType.SETTLEMENT_PAID,
            data = data
        )

        logger.debug(
            "Sent SETTLEMENT_PAID to seller={} for settlement={} (amount={} {})",
            sellerId, settlementId, settlementAmount, currency
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun parsePayload(message: Message): Map<String, Any>? {
        return try {
            val json = String(message.data, Charsets.UTF_8)
            JsonMapper.instance.readValue(json, Map::class.java) as Map<String, Any>
        } catch (ex: Exception) {
            logger.error(
                "Failed to parse payment event payload on subject {}: {}",
                message.subject, ex.message, ex
            )
            null
        }
    }
}
