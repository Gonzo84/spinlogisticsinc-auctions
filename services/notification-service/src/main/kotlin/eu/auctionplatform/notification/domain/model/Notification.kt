package eu.auctionplatform.notification.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Immutable domain model representing a single notification sent (or pending)
 * to a user via a specific delivery channel.
 *
 * Each notification is persisted as an entry in the `notification_log` table
 * and tracks its delivery lifecycle through [status].
 *
 * @property id           Unique identifier (UUIDv7).
 * @property userId       The recipient user's identifier.
 * @property type         The type of notification (e.g. OVERBID, WELCOME).
 * @property channel      The delivery channel used (EMAIL, PUSH, SMS, IN_APP).
 * @property status       Current lifecycle status (PENDING, SENT, DELIVERED, etc.).
 * @property subject      The notification subject or title.
 * @property body         The rendered notification body.
 * @property templateData Key-value pairs used to render the notification template.
 * @property locale       The BCP-47 locale used for rendering (e.g. "en", "de", "nl").
 * @property readAt       UTC instant when the user read the notification (in-app only), or null.
 * @property sentAt       UTC instant when the notification was dispatched, or null if pending.
 * @property createdAt    UTC instant when the notification record was created.
 */
data class Notification(
    val id: UUID,
    val userId: UUID,
    val type: NotificationType,
    val channel: NotificationChannel,
    val status: NotificationStatus = NotificationStatus.PENDING,
    val subject: String,
    val body: String,
    val templateData: Map<String, Any> = emptyMap(),
    val locale: String = "en",
    val readAt: Instant? = null,
    val sentAt: Instant? = null,
    val createdAt: Instant = Instant.now()
) {

    /** Returns `true` if the notification has been read by the user. */
    val isRead: Boolean
        get() = readAt != null

    /** Returns a copy with status updated to [NotificationStatus.SENT] and sentAt set to now. */
    fun markSent(): Notification = copy(
        status = NotificationStatus.SENT,
        sentAt = Instant.now()
    )

    /** Returns a copy with status updated to [NotificationStatus.DELIVERED]. */
    fun markDelivered(): Notification = copy(
        status = NotificationStatus.DELIVERED
    )

    /** Returns a copy with status updated to [NotificationStatus.FAILED]. */
    fun markFailed(): Notification = copy(
        status = NotificationStatus.FAILED
    )

    /** Returns a copy marked as read at the current instant. */
    fun markRead(): Notification = copy(
        readAt = Instant.now()
    )
}
