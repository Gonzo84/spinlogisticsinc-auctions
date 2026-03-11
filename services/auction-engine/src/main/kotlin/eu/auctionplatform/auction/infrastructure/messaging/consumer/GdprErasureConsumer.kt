package eu.auctionplatform.auction.infrastructure.messaging.consumer

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
 * NATS JetStream consumer that listens for `compliance.gdpr.erasure` events
 * and anonymizes bidder data in the auction-engine read model.
 *
 * **Event store immutability:** The `auction_events` table is immutable (event sourcing
 * invariant) and MUST NOT be modified. GDPR anonymization is applied only to the
 * `auction_read_model` projection table, which clears the `current_high_bidder_id`
 * for any auctions where the erased user was the high bidder.
 *
 * For full GDPR compliance with immutable event stores, an "anonymization overlay"
 * approach should be used at read time. This consumer handles the read model side.
 *
 * Uses the durable consumer name `auction-gdpr-erasure-consumer` to survive
 * restarts and ensure at-least-once delivery.
 */
@ApplicationScoped
@Startup
class GdprErasureConsumer @Inject constructor(
    private val connection: Connection,
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(GdprErasureConsumer::class.java)

        private const val STREAM_NAME = "COMPLIANCE"
        private const val DURABLE_NAME = "auction-gdpr-erasure-consumer"
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "auction-gdpr-erasure-consumer").apply { isDaemon = true }
    }

    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.info("Starting GDPR erasure consumer for auction-engine")
        executor.submit { createConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.info("Shutting down GDPR erasure consumer for auction-engine")
        executor.shutdownNow()
    }

    private fun createConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = DURABLE_NAME,
            filterSubject = NatsSubjects.COMPLIANCE_GDPR_ERASURE,
            deadLetterSubject = "dlq.auction.gdpr.erasure"
        ) {
            override fun handleMessage(message: Message) {
                handleGdprErasure(message)
            }
        }

    // -----------------------------------------------------------------------
    // Event handler
    // -----------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun handleGdprErasure(message: Message) {
        val payload = try {
            val json = String(message.data, Charsets.UTF_8)
            JsonMapper.instance.readValue(json, Map::class.java) as Map<String, Any>
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to parse GDPR erasure event payload: %s", ex.message)
            return
        }

        val userId = payload["userId"]?.toString() ?: run {
            LOG.warn("GDPR erasure event missing userId -- skipping")
            return
        }

        LOG.infof("Processing GDPR erasure request for userId=%s in auction-engine", userId)

        try {
            val userUuid = UUID.fromString(userId)

            dataSource.connection.use { conn ->
                // Anonymize bidder ID in auction read model.
                // The event store (auction_events) is immutable and NOT modified.
                // Only the read model projection is updated.
                conn.prepareStatement(
                    """
                    UPDATE app.auction_read_model
                    SET current_high_bidder_id = NULL,
                        updated_at = NOW()
                    WHERE current_high_bidder_id = ?
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setObject(1, userUuid)
                    val rows = stmt.executeUpdate()
                    LOG.infof(
                        "GDPR erasure completed for userId=%s in auction-engine: %d read model rows anonymized",
                        userId, rows
                    )
                }
            }
        } catch (ex: Exception) {
            LOG.errorf(
                ex, "Failed to process GDPR erasure for userId=%s in auction-engine: %s",
                userId, ex.message
            )
            throw ex // Rethrow to trigger redelivery via NatsConsumer
        }
    }
}
