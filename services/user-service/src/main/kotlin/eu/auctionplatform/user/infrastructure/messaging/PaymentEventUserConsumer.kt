package eu.auctionplatform.user.infrastructure.messaging

import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import io.agroal.api.AgroalDataSource
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * NATS JetStream consumer that listens for payment domain events and
 * updates the user-service purchase projections.
 *
 * ## Subscribed events
 *
 * - `payment.checkout.completed` -- Updates the corresponding row in
 *   `app.user_purchases` to status `PAID`, so the buyer's purchase
 *   history reflects the completed payment.
 *
 * Uses the CDI-managed NATS [Connection] and an inner [NatsConsumer]
 * for durable, at-least-once delivery.
 */
@ApplicationScoped
@Startup
class PaymentEventUserConsumer @Inject constructor(
    private val connection: Connection,
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(PaymentEventUserConsumer::class.java)

        private const val STREAM_NAME = "PAYMENT"
        private const val DURABLE_NAME = "user-payment-consumer"
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "user-payment-consumer").apply { isDaemon = true }
    }

    /**
     * Starts the consumer thread for checkout completed events.
     */
    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.info("Starting payment event user consumer")
        executor.submit { createCheckoutCompletedConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.info("Shutting down payment event user consumer")
        executor.shutdownNow()
    }

    // -----------------------------------------------------------------------
    // Consumer factory
    // -----------------------------------------------------------------------

    private fun createCheckoutCompletedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-checkout",
            filterSubject = NatsSubjects.PAYMENT_CHECKOUT_COMPLETED,
            deadLetterSubject = "dlq.user.payment.checkout"
        ) {
            override fun handleMessage(message: Message) {
                handleCheckoutCompleted(message)
            }
        }

    // -----------------------------------------------------------------------
    // Event handler
    // -----------------------------------------------------------------------

    /**
     * Handles `payment.checkout.completed` events.
     *
     * Updates the purchase status from `PENDING_PAYMENT` to `PAID` for the
     * given user and lot combination.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleCheckoutCompleted(message: Message) {
        val payload = parsePayload(message) ?: return

        val buyerId = payload["buyerId"]?.toString() ?: payload["userId"]?.toString() ?: run {
            LOG.warn("payment.checkout.completed event missing buyerId/userId -- skipping")
            return
        }
        val lotId = payload["lotId"]?.toString() ?: run {
            LOG.warn("payment.checkout.completed event missing lotId -- skipping")
            return
        }

        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                UPDATE app.user_purchases
                SET status = 'PAID'
                WHERE user_id = ? AND lot_id = ?
                """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, UUID.fromString(buyerId))
                stmt.setObject(2, UUID.fromString(lotId))
                val updated = stmt.executeUpdate()
                LOG.debugf(
                    "Updated purchase status to PAID for user=%s lot=%s (rows=%s)",
                    buyerId, lotId, updated
                )
            }
        }
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
            LOG.errorf(
                ex, "Failed to parse payment event payload on subject %s: %s",
                message.subject, ex.message
            )
            null
        }
    }
}
