package eu.auctionplatform.auction.domain.model

import eu.auctionplatform.commons.domain.Money
import eu.auctionplatform.commons.domain.UserId
import java.time.Instant

/**
 * Immutable value object representing an automatic (proxy) bid configuration
 * for a specific bidder in an auction.
 *
 * When a competing bid is placed, the auction engine evaluates all active auto-bids
 * to determine whether a proxy bid should be generated on behalf of the auto-bidder.
 * The engine bids the minimum amount necessary to maintain the lead, up to [maxAmount].
 *
 * Auto-bid precedence rules when multiple auto-bids compete:
 * 1. The auto-bid with the highest [maxAmount] wins.
 * 2. If two auto-bids have equal [maxAmount], the one with the earlier [createdAt]
 *    timestamp wins (first-come-first-served).
 *
 * @property bidderId Identifier of the user who configured this auto-bid.
 * @property maxAmount The maximum amount the proxy engine is authorised to bid.
 * @property currentBidAmount The amount of the last proxy bid placed by this auto-bid,
 *                            or the initial bid amount when the auto-bid was set.
 * @property createdAt UTC instant when the auto-bid was configured.
 * @property active `true` if this auto-bid is still eligible to generate proxy bids;
 *                  `false` if it has been exhausted (i.e. the competing bid exceeds
 *                  [maxAmount]) or manually deactivated.
 */
data class AutoBid(
    val bidderId: UserId,
    val maxAmount: Money,
    val currentBidAmount: Money,
    val createdAt: Instant,
    val active: Boolean
) {

    /**
     * Returns `true` if this auto-bid can still generate a proxy bid that
     * exceeds the given [competingAmount] by at least [requiredIncrement].
     */
    fun canCounter(competingAmount: Money, requiredIncrement: Money): Boolean =
        active && maxAmount >= (competingAmount + requiredIncrement)

    /**
     * Returns a copy of this auto-bid with the [currentBidAmount] updated
     * to reflect the latest proxy bid.
     */
    fun withCurrentBid(newBidAmount: Money): AutoBid =
        copy(currentBidAmount = newBidAmount)

    /**
     * Returns a deactivated copy of this auto-bid. Used when the auto-bid
     * has been exhausted or manually cancelled.
     */
    fun deactivate(): AutoBid = copy(active = false)
}
