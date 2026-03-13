package eu.auctionplatform.auction.domain.exception

import eu.auctionplatform.commons.domain.AuctionId
import eu.auctionplatform.commons.domain.Money
import eu.auctionplatform.commons.domain.UserId
import eu.auctionplatform.commons.exception.DomainException

/**
 * Sealed exception hierarchy for all domain-level errors originating from
 * the Auction aggregate.
 *
 * Using a sealed class ensures exhaustive `when` handling at call sites and
 * provides a single import point for all auction-related exceptions. Each
 * subclass carries a unique [code] for machine-readable API error responses.
 */
sealed class AuctionException(
    code: String,
    message: String,
    cause: Throwable? = null
) : DomainException(code, message, cause) {

    /**
     * The requested auction does not exist in the event store or read model.
     */
    class AuctionNotFoundException(
        auctionId: AuctionId
    ) : AuctionException(
        code = "AUCTION_NOT_FOUND",
        message = "Auction with id '$auctionId' was not found"
    )

    /**
     * A command was issued against an auction that is not in a state that
     * permits the operation (e.g. bidding on a CLOSED or CANCELLED auction).
     */
    class AuctionNotActiveException(
        auctionId: AuctionId,
        currentStatus: String
    ) : AuctionException(
        code = "AUCTION_NOT_ACTIVE",
        message = "Auction '$auctionId' is not active (current status: $currentStatus). " +
            "Bids can only be placed on auctions with status ACTIVE or CLOSING."
    )

    /**
     * The bid amount does not meet the minimum required increment above
     * the current high bid.
     */
    class BidBelowMinimumException(
        auctionId: AuctionId,
        bidAmount: Money,
        minimumRequired: Money
    ) : AuctionException(
        code = "BID_BELOW_MINIMUM",
        message = "Bid of $bidAmount on auction '$auctionId' is below the minimum " +
            "required bid of $minimumRequired"
    )

    /**
     * The seller of the lot attempted to bid on their own auction.
     */
    class SellerCannotBidException(
        auctionId: AuctionId,
        sellerId: UserId
    ) : AuctionException(
        code = "SELLER_CANNOT_BID",
        message = "User '$sellerId' is the seller of auction '$auctionId' and cannot bid on it"
    )

    /**
     * The bidder's account has been suspended or blocked from participating
     * in this auction.
     */
    class UserBlockedException(
        auctionId: AuctionId,
        userId: UserId
    ) : AuctionException(
        code = "USER_BLOCKED",
        message = "User '$userId' is blocked from bidding on auction '$auctionId'"
    )

    /**
     * The auction has reached the deposit threshold and the bidder has not
     * placed the required deposit.
     */
    class DepositRequiredException(
        auctionId: AuctionId,
        userId: UserId,
        depositAmount: Money
    ) : AuctionException(
        code = "DEPOSIT_REQUIRED",
        message = "A deposit of $depositAmount is required to bid on auction '$auctionId'. " +
            "User '$userId' has not placed the required deposit."
    )

    /**
     * The bid amount is invalid (e.g. negative, zero, or non-numeric).
     */
    class InvalidBidAmountException(
        auctionId: AuctionId,
        reason: String
    ) : AuctionException(
        code = "INVALID_BID_AMOUNT",
        message = "Invalid bid amount for auction '$auctionId': $reason"
    )

    class AuctionNotFeaturableException(
        id: AuctionId,
        status: String
    ) : AuctionException(
        code = "AUCTION_NOT_FEATURABLE",
        message = "Auction '$id' cannot be featured in status '$status'"
    )

    class AuctionAlreadyFeaturedException(
        id: AuctionId
    ) : AuctionException(
        code = "AUCTION_ALREADY_FEATURED",
        message = "Auction '$id' is already featured"
    )

    class AuctionNotFeaturedException(
        id: AuctionId
    ) : AuctionException(
        code = "AUCTION_NOT_FEATURED",
        message = "Auction '$id' is not currently featured"
    )

    class FeaturedLimitReachedException(
        maxFeatured: Int
    ) : AuctionException(
        code = "FEATURED_LIMIT_REACHED",
        message = "Maximum featured auction limit ($maxFeatured) reached"
    )
}
