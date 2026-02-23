package eu.auctionplatform.auction.domain.command

import eu.auctionplatform.commons.domain.AuctionId
import eu.auctionplatform.commons.domain.Money
import eu.auctionplatform.commons.domain.UserId

/**
 * Command to configure or update an automatic (proxy) bid for a user on an auction.
 *
 * When set, the auction engine will automatically bid on behalf of the [bidderId]
 * up to [maxAmount], using the minimum increment necessary to maintain the lead.
 *
 * Setting an auto-bid replaces any existing auto-bid for the same user on the
 * same auction. To cancel an auto-bid, the user should set [maxAmount] to zero
 * or use a dedicated cancel command (if supported by the API layer).
 *
 * Validation rules:
 * - The auction must be in a state that accepts bids.
 * - The [bidderId] must not be the seller.
 * - The [maxAmount] must be greater than the current high bid plus the minimum
 *   increment.
 *
 * @property auctionId Identifier of the auction.
 * @property bidderId Identifier of the user configuring the auto-bid.
 * @property maxAmount Maximum amount the proxy engine may bid on behalf of the user.
 */
data class SetAutoBidCommand(
    val auctionId: AuctionId,
    val bidderId: UserId,
    val maxAmount: Money
) {

    init {
        require(maxAmount.isPositive()) {
            "Auto-bid max amount must be positive, got: $maxAmount"
        }
    }
}
