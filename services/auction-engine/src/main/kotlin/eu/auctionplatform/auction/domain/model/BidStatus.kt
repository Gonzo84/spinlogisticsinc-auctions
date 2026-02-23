package eu.auctionplatform.auction.domain.model

/**
 * Status of an individual bid within an auction.
 *
 * - **ACTIVE** – The bid is currently the highest bid or one of the active bids
 *                in the auction's bid history.
 * - **OUTBID** – The bid has been surpassed by a higher bid from another participant.
 * - **WINNING** – The bid is the highest bid and the auction has closed; the bidder
 *                 is the provisional winner pending award confirmation.
 * - **REJECTED** – The bid was rejected due to a validation failure (see [BidRejectionReason]).
 */
enum class BidStatus {

    ACTIVE,
    OUTBID,
    WINNING,
    REJECTED
}
