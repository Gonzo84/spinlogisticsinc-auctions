package eu.auctionplatform.notification.infrastructure.push

import eu.auctionplatform.notification.domain.model.DeviceToken
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger

/**
 * Sends push notifications to user devices.
 *
 * This is a placeholder implementation that logs push notification
 * requests. In production, this would integrate with:
 * - Firebase Cloud Messaging (FCM) for Android and Web
 * - Apple Push Notification Service (APNs) for iOS
 *
 * Thread-safe and managed as an application-scoped CDI bean.
 */
@ApplicationScoped
class PushNotificationSender {

    companion object {
        private val LOG: Logger = Logger.getLogger(PushNotificationSender::class.java)
    }

    /**
     * Sends a push notification to one or more device tokens.
     *
     * @param deviceTokens List of active device tokens to target.
     * @param title        Notification title (shown in the notification shade).
     * @param body         Notification body text.
     * @param data         Optional data payload for the client app to process.
     * @param deepLink     Optional deep link URL for navigation on tap.
     * @return [PushResult] with the count of successful and failed deliveries.
     */
    fun sendPush(
        deviceTokens: List<DeviceToken>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        deepLink: String? = null
    ): PushResult {
        if (deviceTokens.isEmpty()) {
            LOG.debug("No device tokens provided -- skipping push notification")
            return PushResult(sent = 0, failed = 0)
        }

        var sent = 0
        var failed = 0

        for (token in deviceTokens) {
            try {
                sendToDevice(token, title, body, data, deepLink)
                sent++
            } catch (ex: Exception) {
                LOG.errorf(
                    ex, "Failed to send push to device: tokenId=%s, platform=%s, error=%s",
                    token.id, token.platform, ex.message
                )
                failed++
            }
        }

        LOG.infof(
            "Push notification batch complete: title='%s', sent=%s, failed=%s, totalTokens=%s",
            title, sent, failed, deviceTokens.size
        )

        return PushResult(sent = sent, failed = failed)
    }

    /**
     * Sends a push notification to a single device.
     *
     * TODO: Replace with actual FCM/APNs integration.
     * - For "android" and "web" tokens: use Firebase Admin SDK
     * - For "ios" tokens: use APNs HTTP/2 client or Firebase
     */
    private fun sendToDevice(
        deviceToken: DeviceToken,
        title: String,
        body: String,
        data: Map<String, String>,
        deepLink: String?
    ) {
        // Placeholder: log the push notification request
        LOG.infof(
            "PUSH [%s] -> tokenId=%s, platform=%s, title='%s', body='%s', deepLink=%s",
            if (deepLink != null) "deep-link" else "standard",
            deviceToken.id,
            deviceToken.platform,
            title,
            body.take(80),
            deepLink ?: "none"
        )

        // In production, dispatch based on platform:
        // when (deviceToken.platform) {
        //     "android", "web" -> sendViaFcm(deviceToken.token, title, body, data, deepLink)
        //     "ios" -> sendViaApns(deviceToken.token, title, body, data, deepLink)
        // }
    }
}

/**
 * Result of a push notification batch send.
 *
 * @property sent   Number of notifications successfully sent.
 * @property failed Number of notifications that failed to send.
 */
data class PushResult(
    val sent: Int,
    val failed: Int
) {
    /** Total number of attempted deliveries. */
    val total: Int get() = sent + failed
}
