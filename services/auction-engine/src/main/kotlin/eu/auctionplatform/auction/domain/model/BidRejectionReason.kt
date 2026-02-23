package eu.auctionplatform.auction.domain.model

/**
 * Machine-readable reason codes for bid rejection.
 *
 * These codes are persisted in [BidRejectedEvent] and surfaced to the API layer
 * so that clients can present localised feedback to the user.
 *
 * - **BELOW_INCREMENT** – The bid amount does not meet the minimum required increment
 *                         above the current high bid.
 * - **AUCTION_CLOSED** – The auction is no longer accepting bids (status is not ACTIVE/CLOSING).
 * - **USER_BLOCKED** – The bidder's account has been suspended or blocked from this auction.
 * - **DEPOSIT_REQUIRED** – The auction requires a deposit and the bidder has not placed one.
 * - **SELLER_CANNOT_BID** – The seller of the lot is not permitted to bid on their own auction.
 * - **INVALID_AMOUNT** – The bid amount is invalid (e.g. negative, zero, or non-numeric).
 */
enum class BidRejectionReason {

    BELOW_INCREMENT,
    AUCTION_CLOSED,
    USER_BLOCKED,
    DEPOSIT_REQUIRED,
    SELLER_CANNOT_BID,
    INVALID_AMOUNT
}
