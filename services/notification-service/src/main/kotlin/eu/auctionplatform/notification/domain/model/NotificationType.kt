package eu.auctionplatform.notification.domain.model

/**
 * Enumerates all notification types supported by the platform.
 *
 * Each type corresponds to a specific domain event or user action that
 * triggers a notification. The [description] field provides a human-readable
 * summary for admin UIs and logging.
 *
 * @property description Human-readable description of the notification trigger.
 */
enum class NotificationType(val description: String) {

    /** Another bidder placed a higher bid on a lot the user is watching. */
    OVERBID("You have been outbid"),

    /** The user's bid was successfully placed. */
    BID_CONFIRMED("Your bid has been confirmed"),

    /** The proxy-bid engine placed an automatic counter-bid on behalf of the user. */
    AUTO_BID_TRIGGERED("Your auto-bid was triggered"),

    /** An auction the user is participating in is about to close. */
    CLOSING_SOON("Auction closing soon"),

    /** The user won the auction for a lot. */
    AUCTION_WON("You won the auction"),

    /** Payment is due for a lot the user has won. */
    PAYMENT_DUE("Payment is due"),

    /** Payment has been successfully received and confirmed. */
    PAYMENT_RECEIVED("Payment received"),

    /** Reminder to pick up the purchased lot. */
    PICKUP_REMINDER("Pickup reminder"),

    /** Settlement has been paid out to the seller. */
    SETTLEMENT_PAID("Settlement paid"),

    /** A new lot has been published in a category the user follows. */
    LOT_PUBLISHED("New lot published"),

    /** A new bid was placed on the seller's lot. */
    NEW_BID_SELLER("New bid on your lot"),

    /** The user's KYC verification has been approved. */
    KYC_APPROVED("KYC verification approved"),

    /** The user's security deposit has been confirmed. */
    DEPOSIT_CONFIRMED("Deposit confirmed"),

    /** Warning that a non-payment case may be opened if the user does not pay. */
    NON_PAYMENT_WARNING("Non-payment warning"),

    /** The reserve price on a lot has been met by a bid. */
    RESERVE_MET("Reserve price met on your lot"),

    /** Welcome notification sent upon user registration. */
    WELCOME("Welcome to the auction platform")
}
