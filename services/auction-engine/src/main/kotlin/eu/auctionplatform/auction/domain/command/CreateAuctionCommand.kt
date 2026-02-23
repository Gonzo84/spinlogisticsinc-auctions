package eu.auctionplatform.auction.domain.command

import eu.auctionplatform.commons.domain.Brand
import eu.auctionplatform.commons.domain.LotId
import eu.auctionplatform.commons.domain.Money
import eu.auctionplatform.commons.domain.UserId
import java.time.Instant

/**
 * Command to create a new auction for a given lot.
 *
 * Validation rules enforced at command-handling time:
 * - [startTime] must be in the future.
 * - [endTime] must be strictly after [startTime].
 * - [startingBid] must be positive.
 * - [reservePrice], if present, must be greater than [startingBid].
 *
 * @property lotId Identifier of the lot to be auctioned.
 * @property brand Brand / tenant that owns this auction.
 * @property startTime Scheduled start time (UTC) when the auction transitions to ACTIVE.
 * @property endTime Scheduled end time (UTC) when the auction will close (before extensions).
 * @property startingBid The minimum opening bid amount.
 * @property reservePrice Optional minimum price that must be met for the lot to be awarded.
 *                        `null` means no reserve (the lot will sell to the highest bidder
 *                        regardless of final price).
 * @property sellerId Identifier of the user who is selling the lot.
 */
data class CreateAuctionCommand(
    val lotId: LotId,
    val brand: Brand,
    val startTime: Instant,
    val endTime: Instant,
    val startingBid: Money,
    val reservePrice: Money?,
    val sellerId: UserId
) {

    init {
        require(endTime.isAfter(startTime)) {
            "End time ($endTime) must be after start time ($startTime)"
        }
        require(startingBid.isPositive()) {
            "Starting bid must be positive, got: $startingBid"
        }
        if (reservePrice != null) {
            require(reservePrice > startingBid) {
                "Reserve price ($reservePrice) must be greater than starting bid ($startingBid)"
            }
        }
    }
}
