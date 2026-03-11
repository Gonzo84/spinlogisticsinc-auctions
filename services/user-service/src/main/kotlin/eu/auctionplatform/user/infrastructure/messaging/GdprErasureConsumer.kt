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
 * NATS JetStream consumer that listens for `compliance.gdpr.erasure` events
 * and anonymizes user data in the user-service database.
 *
 * GDPR Article 17 "Right to Erasure" implementation:
 * - Anonymizes PII fields (name, email, phone) in the `app.users` table
 * - Sets user status to `GDPR_DELETED`
 * - Removes KYC records for the user
 * - Anonymizes company data linked to the user
 *
 * Uses the durable consumer name `user-gdpr-erasure-consumer` to survive
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
        private const val DURABLE_NAME = "user-gdpr-erasure-consumer"
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "user-gdpr-erasure-consumer").apply { isDaemon = true }
    }

    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.info("Starting GDPR erasure consumer for user-service")
        executor.submit { createConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.info("Shutting down GDPR erasure consumer for user-service")
        executor.shutdownNow()
    }

    private fun createConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = DURABLE_NAME,
            filterSubject = NatsSubjects.COMPLIANCE_GDPR_ERASURE,
            deadLetterSubject = "dlq.user.gdpr.erasure"
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

        LOG.infof("Processing GDPR erasure request for userId=%s", userId)

        try {
            val userUuid = UUID.fromString(userId)
            val anonymizedEmail = "deleted-${userUuid.toString().take(8)}@anonymized.invalid"

            dataSource.connection.use { conn ->
                conn.autoCommit = false
                try {
                    // 1. Anonymize user PII fields
                    conn.prepareStatement(
                        """
                        UPDATE app.users
                        SET first_name = 'DELETED',
                            last_name = 'USER',
                            email = ?,
                            phone = NULL,
                            language = 'en',
                            status = 'GDPR_DELETED',
                            updated_at = NOW()
                        WHERE id = ? AND status != 'GDPR_DELETED'
                        """.trimIndent()
                    ).use { stmt ->
                        stmt.setString(1, anonymizedEmail)
                        stmt.setObject(2, userUuid)
                        val rows = stmt.executeUpdate()
                        LOG.debugf("Anonymized user record: userId=%s, rowsUpdated=%d", userId, rows)
                    }

                    // 2. Anonymize company data linked to the user
                    conn.prepareStatement(
                        """
                        UPDATE app.companies
                        SET company_name = 'DELETED COMPANY',
                            registration_no = NULL,
                            vat_id = NULL,
                            address = NULL,
                            city = NULL,
                            postal_code = NULL
                        WHERE user_id = ?
                        """.trimIndent()
                    ).use { stmt ->
                        stmt.setObject(1, userUuid)
                        val rows = stmt.executeUpdate()
                        LOG.debugf("Anonymized company records: userId=%s, rowsUpdated=%d", userId, rows)
                    }

                    // 3. Delete KYC records (these contain sensitive identity documents)
                    conn.prepareStatement(
                        """
                        DELETE FROM app.kyc_records WHERE user_id = ?
                        """.trimIndent()
                    ).use { stmt ->
                        stmt.setObject(1, userUuid)
                        val rows = stmt.executeUpdate()
                        LOG.debugf("Deleted KYC records: userId=%s, rowsDeleted=%d", userId, rows)
                    }

                    conn.commit()
                    LOG.infof("GDPR erasure completed for userId=%s", userId)
                } catch (ex: Exception) {
                    conn.rollback()
                    throw ex
                }
            }
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to process GDPR erasure for userId=%s: %s", userId, ex.message)
            throw ex // Rethrow to trigger redelivery via NatsConsumer
        }
    }
}
