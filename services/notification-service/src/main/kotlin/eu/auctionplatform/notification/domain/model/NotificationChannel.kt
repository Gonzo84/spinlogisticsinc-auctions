package eu.auctionplatform.notification.domain.model

/**
 * Delivery channels through which notifications can be sent to users.
 */
enum class NotificationChannel {

    /** Email delivery via SMTP. */
    EMAIL,

    /** Push notification via FCM (Android/Web) or APNs (iOS). */
    PUSH,

    /** SMS delivery via an external gateway. */
    SMS,

    /** In-app notification displayed within the platform UI. */
    IN_APP
}
