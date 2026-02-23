package eu.auctionplatform.notification.domain.model

/**
 * Lifecycle status of a notification record.
 */
enum class NotificationStatus {

    /** The notification has been created but not yet dispatched. */
    PENDING,

    /** The notification has been sent to the delivery channel. */
    SENT,

    /** The notification was confirmed delivered to the recipient. */
    DELIVERED,

    /** The delivery attempt failed (transient or permanent). */
    FAILED,

    /** The email bounced back (hard bounce). */
    BOUNCED
}
