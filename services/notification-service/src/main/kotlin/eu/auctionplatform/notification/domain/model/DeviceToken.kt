package eu.auctionplatform.notification.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Represents a registered device token for push notification delivery.
 *
 * Users may have multiple device tokens (e.g. multiple phones, web browsers).
 * Tokens can be deactivated when the user logs out or uninstalls the app.
 *
 * @property id        Unique identifier for this device token record (UUIDv7).
 * @property userId    The user who owns this device.
 * @property platform  The device platform: "ios", "android", or "web".
 * @property token     The FCM/APNs/Web Push token string.
 * @property active    Whether this token is currently active for delivery.
 * @property createdAt UTC instant when the token was registered.
 */
data class DeviceToken(
    val id: UUID,
    val userId: UUID,
    val platform: String,
    val token: String,
    val active: Boolean = true,
    val createdAt: Instant = Instant.now()
) {

    companion object {
        /** Allowed platform values. */
        val VALID_PLATFORMS = setOf("ios", "android", "web")
    }

    init {
        require(platform in VALID_PLATFORMS) {
            "Platform must be one of $VALID_PLATFORMS, got '$platform'"
        }
    }

    /** Returns a copy with the token deactivated. */
    fun deactivate(): DeviceToken = copy(active = false)
}
