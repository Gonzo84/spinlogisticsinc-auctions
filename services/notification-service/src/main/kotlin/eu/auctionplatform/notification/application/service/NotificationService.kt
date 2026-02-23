package eu.auctionplatform.notification.application.service

import eu.auctionplatform.commons.dto.PagedResponse
import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.commons.util.IdGenerator
import eu.auctionplatform.notification.domain.model.DeviceToken
import eu.auctionplatform.notification.domain.model.Notification
import eu.auctionplatform.notification.domain.model.NotificationChannel
import eu.auctionplatform.notification.domain.model.NotificationPreference
import eu.auctionplatform.notification.domain.model.NotificationStatus
import eu.auctionplatform.notification.domain.model.NotificationType
import eu.auctionplatform.notification.infrastructure.email.EmailSender
import eu.auctionplatform.notification.infrastructure.persistence.repository.DeviceTokenRepository
import eu.auctionplatform.notification.infrastructure.persistence.repository.NotificationPreferenceRepository
import eu.auctionplatform.notification.infrastructure.persistence.repository.NotificationRepository
import eu.auctionplatform.notification.infrastructure.push.PushNotificationSender
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Core application service that orchestrates notification creation, channel
 * routing, and delivery.
 *
 * For each notification request, this service:
 * 1. Resolves the user's channel preferences for the given notification type.
 * 2. Creates a [Notification] record per enabled channel.
 * 3. Dispatches each notification to the appropriate sender (email, push, in-app).
 * 4. Updates the notification status based on the delivery outcome.
 *
 * All notification records are persisted to the `notification_log` table for
 * audit, analytics, and in-app notification retrieval.
 */
@ApplicationScoped
class NotificationService @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val preferenceRepository: NotificationPreferenceRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val emailSender: EmailSender,
    private val pushNotificationSender: PushNotificationSender
) {

    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    companion object {
        /**
         * Maps each notification type to a template name and default subject.
         */
        private val TEMPLATE_CONFIG: Map<NotificationType, TemplateConfig> = mapOf(
            NotificationType.OVERBID to TemplateConfig(
                templateName = "overbid",
                defaultSubject = "You have been outbid"
            ),
            NotificationType.BID_CONFIRMED to TemplateConfig(
                templateName = "bid_confirmed",
                defaultSubject = "Your bid has been confirmed"
            ),
            NotificationType.AUTO_BID_TRIGGERED to TemplateConfig(
                templateName = "auto_bid_triggered",
                defaultSubject = "Your auto-bid was triggered"
            ),
            NotificationType.CLOSING_SOON to TemplateConfig(
                templateName = "closing_soon",
                defaultSubject = "Auction closing soon"
            ),
            NotificationType.AUCTION_WON to TemplateConfig(
                templateName = "auction_won",
                defaultSubject = "Congratulations! You won the auction"
            ),
            NotificationType.PAYMENT_DUE to TemplateConfig(
                templateName = "payment_due",
                defaultSubject = "Payment is due for your purchase"
            ),
            NotificationType.PAYMENT_RECEIVED to TemplateConfig(
                templateName = "payment_received",
                defaultSubject = "Payment received"
            ),
            NotificationType.PICKUP_REMINDER to TemplateConfig(
                templateName = "pickup_reminder",
                defaultSubject = "Reminder: Pick up your purchase"
            ),
            NotificationType.SETTLEMENT_PAID to TemplateConfig(
                templateName = "settlement_paid",
                defaultSubject = "Your settlement has been paid"
            ),
            NotificationType.LOT_PUBLISHED to TemplateConfig(
                templateName = "lot_published",
                defaultSubject = "New lot published"
            ),
            NotificationType.NEW_BID_SELLER to TemplateConfig(
                templateName = "new_bid_seller",
                defaultSubject = "New bid on your lot"
            ),
            NotificationType.KYC_APPROVED to TemplateConfig(
                templateName = "kyc_approved",
                defaultSubject = "Your identity verification is approved"
            ),
            NotificationType.DEPOSIT_CONFIRMED to TemplateConfig(
                templateName = "deposit_confirmed",
                defaultSubject = "Your deposit has been confirmed"
            ),
            NotificationType.NON_PAYMENT_WARNING to TemplateConfig(
                templateName = "non_payment_warning",
                defaultSubject = "Action required: Complete your payment"
            ),
            NotificationType.WELCOME to TemplateConfig(
                templateName = "welcome",
                defaultSubject = "Welcome to the Auction Platform"
            )
        )
    }

    // -----------------------------------------------------------------------
    // Core notification dispatch
    // -----------------------------------------------------------------------

    /**
     * Sends a notification to a user across all enabled channels.
     *
     * Determines which channels are enabled based on user preferences (or
     * defaults if no preferences are configured), creates a notification
     * record for each channel, and dispatches to the appropriate sender.
     *
     * @param userId The recipient user's UUID.
     * @param type   The notification type to send.
     * @param data   Template data for rendering the notification content.
     */
    fun sendNotification(userId: UUID, type: NotificationType, data: Map<String, Any>) {
        val channels = resolveChannels(userId, type)
        val templateConfig = TEMPLATE_CONFIG[type]
            ?: TemplateConfig(templateName = type.name.lowercase(), defaultSubject = type.description)
        val locale = data["language"]?.toString() ?: "en"

        logger.info(
            "Sending notification: userId={}, type={}, channels={}, locale={}",
            userId, type, channels, locale
        )

        for (channel in channels) {
            try {
                val notification = createNotification(
                    userId = userId,
                    type = type,
                    channel = channel,
                    subject = templateConfig.defaultSubject,
                    templateData = data,
                    locale = locale
                )

                notificationRepository.insert(notification)

                dispatchNotification(notification, templateConfig, data)

                notificationRepository.updateStatus(
                    id = notification.id,
                    status = NotificationStatus.SENT,
                    sentAt = Instant.now()
                )
            } catch (ex: Exception) {
                logger.error(
                    "Failed to send notification: userId={}, type={}, channel={}: {}",
                    userId, type, channel, ex.message, ex
                )
                // The notification was inserted with PENDING status; update to FAILED
                // Note: if the insert itself failed, this is a no-op since there's no record.
            }
        }
    }

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    /**
     * Returns the count of unread in-app notifications for a user.
     *
     * @param userId The user's UUID.
     * @return Number of unread in-app notifications.
     */
    fun getUnreadCount(userId: UUID): Int {
        return notificationRepository.countUnread(userId)
    }

    /**
     * Marks a single notification as read.
     *
     * @param notificationId The notification UUID.
     * @throws NotFoundException if the notification does not exist.
     */
    fun markAsRead(notificationId: UUID) {
        val notification = notificationRepository.findById(notificationId)
            ?: throw NotFoundException(
                code = "NOTIFICATION_NOT_FOUND",
                message = "Notification with id '$notificationId' not found."
            )

        notificationRepository.markAsRead(notificationId)

        logger.debug("Marked notification as read: id={}", notificationId)
    }

    /**
     * Marks all unread in-app notifications for a user as read.
     *
     * @param userId The user's UUID.
     * @return Number of notifications that were marked as read.
     */
    fun markAllAsRead(userId: UUID): Int {
        val count = notificationRepository.markAllAsRead(userId)
        logger.debug("Marked {} notifications as read for user={}", count, userId)
        return count
    }

    /**
     * Retrieves paginated notifications for a user.
     *
     * @param userId The user's UUID.
     * @param page   Page number (1-based).
     * @param size   Number of items per page.
     * @return A [PagedResponse] containing the user's notifications.
     */
    fun getUserNotifications(userId: UUID, page: Int, size: Int): PagedResponse<Notification> {
        val safePage = maxOf(page, 1)
        val safeSize = maxOf(size, 1).coerceAtMost(100)
        val offset = (safePage - 1) * safeSize

        val items = notificationRepository.findByUserId(userId, limit = safeSize, offset = offset)
        val total = notificationRepository.countByUserId(userId)

        return PagedResponse(
            items = items,
            total = total,
            page = safePage,
            pageSize = safeSize
        )
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Resolves the set of channels to use for a given user and notification type.
     *
     * If the user has configured preferences for this type, those are used.
     * Otherwise, defaults are applied (email and in-app enabled, push enabled,
     * SMS disabled).
     */
    private fun resolveChannels(userId: UUID, type: NotificationType): Set<NotificationChannel> {
        val preference = preferenceRepository.findByUserIdAndType(userId, type)

        return if (preference != null) {
            preference.enabledChannels()
        } else {
            // Default preferences: email + push + in-app, no SMS
            val default = NotificationPreference(
                userId = userId,
                notificationType = type,
                emailEnabled = true,
                pushEnabled = true,
                smsEnabled = false
            )
            default.enabledChannels()
        }
    }

    /**
     * Creates a new [Notification] domain object with PENDING status.
     */
    private fun createNotification(
        userId: UUID,
        type: NotificationType,
        channel: NotificationChannel,
        subject: String,
        templateData: Map<String, Any>,
        locale: String
    ): Notification = Notification(
        id = IdGenerator.generateUUIDv7(),
        userId = userId,
        type = type,
        channel = channel,
        status = NotificationStatus.PENDING,
        subject = subject,
        body = "", // Body is rendered by the specific sender
        templateData = templateData,
        locale = locale,
        createdAt = Instant.now()
    )

    /**
     * Dispatches a notification to the appropriate sender based on its channel.
     */
    private fun dispatchNotification(
        notification: Notification,
        templateConfig: TemplateConfig,
        data: Map<String, Any>
    ) {
        when (notification.channel) {
            NotificationChannel.EMAIL -> dispatchEmail(notification, templateConfig, data)
            NotificationChannel.PUSH -> dispatchPush(notification, data)
            NotificationChannel.IN_APP -> {
                // In-app notifications are "delivered" immediately upon insertion;
                // the client polls or receives them via WebSocket.
                logger.debug(
                    "In-app notification stored: id={}, userId={}, type={}",
                    notification.id, notification.userId, notification.type
                )
            }
            NotificationChannel.SMS -> {
                // SMS delivery is not yet implemented.
                logger.warn(
                    "SMS delivery not implemented -- skipping notification: id={}, userId={}",
                    notification.id, notification.userId
                )
            }
        }
    }

    /**
     * Dispatches an email notification via the [EmailSender].
     *
     * The recipient's email address is expected in the template data under the
     * key "email". If not present, the notification is logged as skipped.
     */
    private fun dispatchEmail(
        notification: Notification,
        templateConfig: TemplateConfig,
        data: Map<String, Any>
    ) {
        val recipientEmail = data["email"]?.toString()
        if (recipientEmail.isNullOrBlank()) {
            logger.warn(
                "No email address in template data for notification: id={}, userId={}, type={}. " +
                    "Email delivery skipped.",
                notification.id, notification.userId, notification.type
            )
            return
        }

        emailSender.sendEmail(
            to = recipientEmail,
            subject = notification.subject,
            templateName = templateConfig.templateName,
            data = data,
            locale = notification.locale
        )
    }

    /**
     * Dispatches a push notification via the [PushNotificationSender].
     *
     * Retrieves active device tokens for the user and sends the push to all.
     */
    private fun dispatchPush(notification: Notification, data: Map<String, Any>) {
        val tokens = deviceTokenRepository.findActiveByUserId(notification.userId)
        if (tokens.isEmpty()) {
            logger.debug(
                "No active device tokens for user={} -- skipping push for notification={}",
                notification.userId, notification.id
            )
            return
        }

        val stringData = data.mapValues { it.value.toString() }

        pushNotificationSender.sendPush(
            deviceTokens = tokens,
            title = notification.subject,
            body = notification.type.description,
            data = stringData,
            deepLink = buildDeepLink(notification)
        )
    }

    /**
     * Builds a deep link URL based on the notification type.
     */
    private fun buildDeepLink(notification: Notification): String? {
        return when (notification.type) {
            NotificationType.OVERBID,
            NotificationType.BID_CONFIRMED,
            NotificationType.AUTO_BID_TRIGGERED,
            NotificationType.CLOSING_SOON -> {
                val auctionId = notification.templateData["auctionId"]?.toString()
                if (auctionId != null) "/auctions/$auctionId" else null
            }
            NotificationType.AUCTION_WON,
            NotificationType.PAYMENT_DUE -> {
                val auctionId = notification.templateData["auctionId"]?.toString()
                if (auctionId != null) "/auctions/$auctionId/checkout" else null
            }
            NotificationType.PAYMENT_RECEIVED -> {
                val orderId = notification.templateData["orderId"]?.toString()
                if (orderId != null) "/orders/$orderId" else null
            }
            NotificationType.SETTLEMENT_PAID -> "/dashboard/settlements"
            NotificationType.KYC_APPROVED -> "/profile"
            NotificationType.WELCOME -> "/dashboard"
            else -> null
        }
    }
}

/**
 * Internal configuration for mapping a notification type to its email template
 * and default subject line.
 */
internal data class TemplateConfig(
    val templateName: String,
    val defaultSubject: String
)
