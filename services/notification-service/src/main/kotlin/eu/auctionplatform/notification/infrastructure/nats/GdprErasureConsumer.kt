package eu.auctionplatform.notification.infrastructure.nats

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
 * and deletes notification records for the erased user.
 *
 * Unlike other services that anonymize records, notification data has no
 * legal retention requirement, so all records are fully deleted:
 * - Notification log entries
 * - Notification preferences
 * - Device tokens (push notification tokens)
 *
 * Uses the durable consumer name `notification-gdpr-erasure-consumer` to survive
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
        private const val DURABLE_NAME = "notification-gdpr-erasure-consumer"
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "notification-gdpr-erasure-consumer").apply { isDaemon = true }
    }

    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.info("Starting GDPR erasure consumer for notification-service")
        executor.submit { createConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.info("Shutting down GDPR erasure consumer for notification-service")
        executor.shutdownNow()
    }

    private fun createConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = DURABLE_NAME,
            filterSubject = NatsSubjects.COMPLIANCE_GDPR_ERASURE,
            deadLetterSubject = "dlq.notification.gdpr.erasure"
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

        LOG.infof("Processing GDPR erasure request for userId=%s in notification-service", userId)

        try {
            val userUuid = UUID.fromString(userId)

            dataSource.connection.use { conn ->
                conn.autoCommit = false
                try {
                    // 1. Delete device tokens
                    conn.prepareStatement(
                        """
                        DELETE FROM app.device_tokens WHERE user_id = ?
                        """.trimIndent()
                    ).use { stmt ->
                        stmt.setObject(1, userUuid)
                        val rows = stmt.executeUpdate()
                        LOG.debugf("Deleted device tokens: userId=%s, rowsDeleted=%d", userId, rows)
                    }

                    // 2. Delete notification preferences
                    conn.prepareStatement(
                        """
                        DELETE FROM app.notification_preferences WHERE user_id = ?
                        """.trimIndent()
                    ).use { stmt ->
                        stmt.setObject(1, userUuid)
                        val rows = stmt.executeUpdate()
                        LOG.debugf("Deleted notification preferences: userId=%s, rowsDeleted=%d", userId, rows)
                    }

                    // 3. Delete notification log entries
                    conn.prepareStatement(
                        """
                        DELETE FROM app.notification_log WHERE user_id = ?
                        """.trimIndent()
                    ).use { stmt ->
                        stmt.setObject(1, userUuid)
                        val rows = stmt.executeUpdate()
                        LOG.debugf("Deleted notification log entries: userId=%s, rowsDeleted=%d", userId, rows)
                    }

                    conn.commit()
                    LOG.infof("GDPR erasure completed for userId=%s in notification-service", userId)
                } catch (ex: Exception) {
                    conn.rollback()
                    throw ex
                }
            }
        } catch (ex: Exception) {
            LOG.errorf(
                ex, "Failed to process GDPR erasure for userId=%s in notification-service: %s",
                userId, ex.message
            )
            throw ex // Rethrow to trigger redelivery via NatsConsumer
        }
    }
}
