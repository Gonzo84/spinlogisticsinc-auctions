package eu.auctionplatform.notification.domain.model

import java.util.UUID

/**
 * User-defined delivery preferences for a specific [NotificationType].
 *
 * Each user can independently enable or disable each delivery channel
 * (email, push, SMS) per notification type. In-app notifications are
 * always delivered and not governed by preferences.
 *
 * @property userId           The user whose preferences these are.
 * @property notificationType The notification type this preference applies to.
 * @property emailEnabled     Whether email delivery is enabled for this type.
 * @property pushEnabled      Whether push notification delivery is enabled.
 * @property smsEnabled       Whether SMS delivery is enabled for this type.
 */
data class NotificationPreference(
    val userId: UUID,
    val notificationType: NotificationType,
    val emailEnabled: Boolean = true,
    val pushEnabled: Boolean = true,
    val smsEnabled: Boolean = false
) {

    /**
     * Returns the set of enabled [NotificationChannel]s based on these preferences.
     *
     * In-app is always included regardless of preference settings.
     */
    fun enabledChannels(): Set<NotificationChannel> {
        val channels = mutableSetOf(NotificationChannel.IN_APP)
        if (emailEnabled) channels.add(NotificationChannel.EMAIL)
        if (pushEnabled) channels.add(NotificationChannel.PUSH)
        if (smsEnabled) channels.add(NotificationChannel.SMS)
        return channels
    }
}
