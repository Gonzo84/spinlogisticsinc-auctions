package eu.auctionplatform.payment.infrastructure.nats

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
 * and anonymizes buyer data in the payment-service database.
 *
 * GDPR Article 17 with EU tax law retention (7 years):
 * - Financial records are RETAINED (hammer price, VAT, totals) per EU Directive 2006/112/EC
 * - Buyer identity is ANONYMIZED (buyer_id replaced with a deterministic anonymized UUID)
 * - Buyer/seller names are cleared
 *
 * This ensures compliance with both GDPR erasure rights and EU VAT record-keeping obligations.
 *
 * Uses the durable consumer name `payment-gdpr-erasure-consumer` to survive
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
        private const val DURABLE_NAME = "payment-gdpr-erasure-consumer"

        /**
         * Deterministic anonymized UUID used to replace buyer_id in payment records.
         * Using a fixed UUID allows financial aggregation queries to still work
         * while removing the link to the real user.
         */
        private val ANONYMIZED_BUYER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "payment-gdpr-erasure-consumer").apply { isDaemon = true }
    }

    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.info("Starting GDPR erasure consumer for payment-service")
        executor.submit { createConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.info("Shutting down GDPR erasure consumer for payment-service")
        executor.shutdownNow()
    }

    private fun createConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = DURABLE_NAME,
            filterSubject = NatsSubjects.COMPLIANCE_GDPR_ERASURE,
            deadLetterSubject = "dlq.payment.gdpr.erasure"
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

        LOG.infof("Processing GDPR erasure request for userId=%s in payment-service", userId)

        try {
            val userUuid = UUID.fromString(userId)

            dataSource.connection.use { conn ->
                conn.autoCommit = false
                try {
                    // Anonymize buyer_id in payment records.
                    // Financial amounts (hammer_price, vat_amount, total_amount) are RETAINED
                    // per EU tax law 7-year retention requirement.
                    conn.prepareStatement(
                        """
                        UPDATE app.payments
                        SET buyer_id = ?,
                            buyer_name = NULL,
                            payment_method = NULL,
                            updated_at = NOW()
                        WHERE buyer_id = ?
                        """.trimIndent()
                    ).use { stmt ->
                        stmt.setObject(1, ANONYMIZED_BUYER_ID)
                        stmt.setObject(2, userUuid)
                        val rows = stmt.executeUpdate()
                        LOG.debugf(
                            "Anonymized payment records: userId=%s, rowsUpdated=%d",
                            userId, rows
                        )
                    }

                    conn.commit()
                    LOG.infof("GDPR erasure completed for userId=%s in payment-service", userId)
                } catch (ex: Exception) {
                    conn.rollback()
                    throw ex
                }
            }
        } catch (ex: Exception) {
            LOG.errorf(
                ex, "Failed to process GDPR erasure for userId=%s in payment-service: %s",
                userId, ex.message
            )
            throw ex // Rethrow to trigger redelivery via NatsConsumer
        }
    }
}
