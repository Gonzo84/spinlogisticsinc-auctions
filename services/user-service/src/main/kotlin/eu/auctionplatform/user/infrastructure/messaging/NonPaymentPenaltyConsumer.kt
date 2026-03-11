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
 * NATS JetStream consumer that listens for non-payment penalty events
 * and suspends the offending buyer's account.
 *
 * ## Subscribed events
 *
 * - `payment.non-payment.penalty` -- Suspends the buyer by updating their
 *   status from ACTIVE to SUSPENDED in the `app.users` table.
 *
 * Uses the CDI-managed NATS [Connection] and an inner [NatsConsumer]
 * for durable, at-least-once delivery.
 *
 * **Design decision:** Uses SUSPENDED (not BLOCKED). SUSPENDED is temporary
 * and appropriate for a penalty that can be resolved.
 */
@ApplicationScoped
@Startup
class NonPaymentPenaltyConsumer @Inject constructor(
    private val connection: Connection,
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(NonPaymentPenaltyConsumer::class.java)

        private const val STREAM_NAME = "PAYMENT"
        private const val DURABLE_NAME = "user-non-payment-penalty-consumer"
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "user-non-payment-penalty").apply { isDaemon = true }
    }

    /**
     * Starts the consumer thread for non-payment penalty events.
     */
    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.info("Starting non-payment penalty consumer")
        executor.submit { createPenaltyConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.info("Shutting down non-payment penalty consumer")
        executor.shutdownNow()
    }

    // -----------------------------------------------------------------------
    // Consumer factory
    // -----------------------------------------------------------------------

    private fun createPenaltyConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = DURABLE_NAME,
            filterSubject = NatsSubjects.PAYMENT_NON_PAYMENT_PENALTY,
            deadLetterSubject = "dlq.user.payment.non-payment-penalty"
        ) {
            override fun handleMessage(message: Message) {
                handleNonPaymentPenalty(message)
            }
        }

    // -----------------------------------------------------------------------
    // Event handler
    // -----------------------------------------------------------------------

    /**
     * Handles `payment.non-payment.penalty` events.
     *
     * Suspends the buyer by updating their status from ACTIVE to SUSPENDED.
     * This is idempotent -- if the user is already SUSPENDED or in another
     * non-ACTIVE state, the update is a no-op (0 rows affected).
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleNonPaymentPenalty(message: Message) {
        val payload = parsePayload(message) ?: return

        val userId = payload["userId"]?.toString() ?: payload["buyerId"]?.toString() ?: run {
            LOG.warn("payment.non-payment.penalty event missing userId/buyerId -- skipping")
            return
        }
        val reason = payload["reason"]?.toString() ?: "NON_PAYMENT"

        LOG.infof("Processing non-payment penalty for user=%s, reason=%s", userId, reason)

        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                UPDATE app.users
                SET status = 'SUSPENDED', updated_at = NOW()
                WHERE id = ? AND status = 'ACTIVE'
                """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, UUID.fromString(userId))
                val updated = stmt.executeUpdate()
                if (updated > 0) {
                    LOG.infof("User %s suspended due to non-payment penalty (reason=%s)", userId, reason)
                } else {
                    LOG.infof(
                        "User %s not suspended -- already non-ACTIVE or not found (reason=%s)",
                        userId, reason
                    )
                }
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
                ex, "Failed to parse non-payment penalty event payload on subject %s: %s",
                message.subject, ex.message
            )
            null
        }
    }
}
