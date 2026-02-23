package eu.auctionplatform.notification.api.v1.dto

import eu.auctionplatform.notification.domain.model.DeviceToken
import eu.auctionplatform.notification.domain.model.Notification
import eu.auctionplatform.notification.domain.model.NotificationChannel
import eu.auctionplatform.notification.domain.model.NotificationPreference
import eu.auctionplatform.notification.domain.model.NotificationStatus
import eu.auctionplatform.notification.domain.model.NotificationType
import java.time.Instant
import java.util.UUID

// =============================================================================
// Request DTOs
// =============================================================================

/**
 * Request payload for updating a single notification preference.
 */
data class UpdatePreferenceRequest(
    val notificationType: NotificationType,
    val channel: NotificationChannel,
    val enabled: Boolean
)

/**
 * Request payload for bulk-updating notification preferences.
 */
data class UpdatePreferencesRequest(
    val preferences: List<PreferenceEntry>
)

/**
 * A single preference entry in a bulk update request.
 */
data class PreferenceEntry(
    val notificationType: NotificationType,
    val emailEnabled: Boolean = true,
    val pushEnabled: Boolean = true,
    val smsEnabled: Boolean = false
)

/**
 * Request payload for registering a device token for push notifications.
 */
data class RegisterDeviceTokenRequest(
    val platform: String,
    val token: String
)

// =============================================================================
// Response DTOs
// =============================================================================

/**
 * Response representation of a notification.
 */
data class NotificationResponse(
    val id: UUID,
    val userId: UUID,
    val type: NotificationType,
    val channel: NotificationChannel,
    val status: NotificationStatus,
    val subject: String,
    val body: String,
    val locale: String,
    val isRead: Boolean,
    val readAt: Instant?,
    val sentAt: Instant?,
    val createdAt: Instant
)

/**
 * Response representing the unread notification count.
 */
data class UnreadCountResponse(
    val unreadCount: Int
)

/**
 * Response representation of a notification preference.
 */
data class NotificationPreferenceResponse(
    val notificationType: NotificationType,
    val description: String,
    val emailEnabled: Boolean,
    val pushEnabled: Boolean,
    val smsEnabled: Boolean
)

/**
 * Response representation of a device token.
 */
data class DeviceTokenResponse(
    val id: UUID,
    val platform: String,
    val token: String,
    val active: Boolean,
    val createdAt: Instant
)

/**
 * Response for the mark-all-as-read operation.
 */
data class MarkAllReadResponse(
    val markedCount: Int
)

// =============================================================================
// Mapper extensions
// =============================================================================

/**
 * Converts a [Notification] domain model to a [NotificationResponse] DTO.
 */
fun Notification.toResponse(): NotificationResponse = NotificationResponse(
    id = id,
    userId = userId,
    type = type,
    channel = channel,
    status = status,
    subject = subject,
    body = body,
    locale = locale,
    isRead = isRead,
    readAt = readAt,
    sentAt = sentAt,
    createdAt = createdAt
)

/**
 * Converts a [NotificationPreference] domain model to a [NotificationPreferenceResponse] DTO.
 */
fun NotificationPreference.toResponse(): NotificationPreferenceResponse = NotificationPreferenceResponse(
    notificationType = notificationType,
    description = notificationType.description,
    emailEnabled = emailEnabled,
    pushEnabled = pushEnabled,
    smsEnabled = smsEnabled
)

/**
 * Converts a [DeviceToken] domain model to a [DeviceTokenResponse] DTO.
 */
fun DeviceToken.toResponse(): DeviceTokenResponse = DeviceTokenResponse(
    id = id,
    platform = platform,
    token = token,
    active = active,
    createdAt = createdAt
)

/**
 * Converts a [PreferenceEntry] request DTO to a [NotificationPreference] domain model.
 */
fun PreferenceEntry.toDomain(userId: UUID): NotificationPreference = NotificationPreference(
    userId = userId,
    notificationType = notificationType,
    emailEnabled = emailEnabled,
    pushEnabled = pushEnabled,
    smsEnabled = smsEnabled
)
