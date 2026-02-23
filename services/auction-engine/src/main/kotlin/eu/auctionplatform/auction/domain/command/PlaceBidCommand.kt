package eu.auctionplatform.auction.domain.command

import eu.auctionplatform.commons.domain.AuctionId
import eu.auctionplatform.commons.domain.Money
import eu.auctionplatform.commons.domain.UserId

/**
 * Command to place a bid on an active auction.
 *
 * The auction aggregate validates the following invariants upon receiving
 * this command:
 * - The auction must be in [AuctionStatus.ACTIVE] or [AuctionStatus.CLOSING].
 * - The [bidderId] must not be the seller of the lot.
 * - The [bidderId] must not be blocked.
 * - The [amount] must be greater than or equal to the current high bid plus
 *   the minimum increment for the current price tier.
 * - If the deposit threshold has been reached, the bidder must have a deposit
 *   on file.
 *
 * @property auctionId Identifier of the auction to bid on.
 * @property bidderId Identifier of the user placing the bid.
 * @property amount Monetary amount of the bid.
 * @property isProxy `true` if this bid originates from the proxy-bid engine
 *                   rather than direct user action. Defaults to `false`.
 */
data class PlaceBidCommand(
    val auctionId: AuctionId,
    val bidderId: UserId,
    val amount: Money,
    val isProxy: Boolean = false
) {

    init {
        require(amount.isPositive()) {
            "Bid amount must be positive, got: $amount"
        }
    }
}
