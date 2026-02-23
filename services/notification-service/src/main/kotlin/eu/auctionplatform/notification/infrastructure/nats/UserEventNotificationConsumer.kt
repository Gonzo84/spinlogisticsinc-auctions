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
 * NATS JetStream consumer that listens for user-related domain events
 * and triggers the appropriate notifications.
 *
 * Subscribed subjects:
 * - `user.registered.>` -- Triggers [NotificationType.WELCOME] email to the
 *   newly registered user.
 * - `user.kyc.verified` -- Triggers [NotificationType.KYC_APPROVED] to the
 *   user whose KYC verification was approved.
 *
 * Uses the durable consumer name `notification-user-consumer` to survive
 * restarts and ensure at-least-once delivery.
 */
@ApplicationScoped
@Startup
class UserEventNotificationConsumer @Inject constructor(
    private val connection: Connection,
    private val notificationService: NotificationService
) {

    private val logger = LoggerFactory.getLogger(UserEventNotificationConsumer::class.java)

    companion object {
        private const val STREAM_NAME = "USER"
        private const val DURABLE_NAME = "notification-user-consumer"

        private const val USER_REGISTERED_FILTER = "user.registered.>"
        private const val USER_KYC_VERIFIED_FILTER = "user.kyc.verified"
    }

    private val executor: ExecutorService = Executors.newFixedThreadPool(2)

    /**
     * Starts consumer threads for user event subjects.
     */
    @jakarta.annotation.PostConstruct
    fun init() {
        logger.info("Starting user event notification consumers")

        executor.submit { createUserRegisteredConsumer().start() }
        executor.submit { createKycVerifiedConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        logger.info("Shutting down user event notification consumers")
        executor.shutdownNow()
    }

    // -----------------------------------------------------------------------
    // Consumer factories
    // -----------------------------------------------------------------------

    private fun createUserRegisteredConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-registered",
            filterSubject = USER_REGISTERED_FILTER,
            deadLetterSubject = "dlq.notification.user.registered"
        ) {
            override fun handleMessage(message: Message) {
                handleUserRegistered(message)
            }
        }

    private fun createKycVerifiedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-kyc-verified",
            filterSubject = USER_KYC_VERIFIED_FILTER,
            deadLetterSubject = "dlq.notification.user.kyc"
        ) {
            override fun handleMessage(message: Message) {
                handleKycVerified(message)
            }
        }

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    /**
     * Handles `user.registered.>` events.
     *
     * Sends a [NotificationType.WELCOME] notification (typically email) to
     * the newly registered user.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleUserRegistered(message: Message) {
        val payload = parsePayload(message) ?: return

        val userId = payload["userId"]?.toString()
            ?: payload["aggregateId"]?.toString()
            ?: return
        val email = payload["email"]?.toString() ?: ""
        val firstName = payload["firstName"]?.toString() ?: ""
        val lastName = payload["lastName"]?.toString() ?: ""
        val language = payload["language"]?.toString() ?: "en"

        val data = mapOf(
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "fullName" to "$firstName $lastName".trim(),
            "language" to language
        )

        notificationService.sendNotification(
            userId = UUID.fromString(userId),
            type = NotificationType.WELCOME,
            data = data
        )

        logger.debug("Sent WELCOME to user={} (email={})", userId, email)
    }

    /**
     * Handles `user.kyc.verified` events.
     *
     * Sends a [NotificationType.KYC_APPROVED] notification to the user
     * whose identity verification has been approved.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleKycVerified(message: Message) {
        val payload = parsePayload(message) ?: return

        val userId = payload["userId"]?.toString()
            ?: payload["aggregateId"]?.toString()
            ?: return
        val firstName = payload["firstName"]?.toString() ?: ""

        val data = mapOf(
            "firstName" to firstName
        )

        notificationService.sendNotification(
            userId = UUID.fromString(userId),
            type = NotificationType.KYC_APPROVED,
            data = data
        )

        logger.debug("Sent KYC_APPROVED to user={}", userId)
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
                "Failed to parse user event payload on subject {}: {}",
                message.subject, ex.message, ex
            )
            null
        }
    }
}
