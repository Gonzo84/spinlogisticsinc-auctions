package eu.auctionplatform.analytics.domain.model

import java.math.BigDecimal
import java.time.Duration
import java.util.UUID

/**
 * Aggregated metrics for a specific auction.
 *
 * @property auctionId      The auction identifier.
 * @property totalBids       Total number of bids placed on this auction.
 * @property uniqueBidders   Number of distinct bidders.
 * @property avgBidAmount    Average bid amount (EUR).
 * @property maxBid          Highest bid amount (EUR).
 * @property extensionCount  Number of time extensions triggered by late bids.
 * @property durationSeconds Total duration of the auction in seconds.
 */
data class AuctionMetrics(
    val auctionId: UUID,
    val totalBids: Long,
    val uniqueBidders: Int,
    val avgBidAmount: BigDecimal,
    val maxBid: BigDecimal,
    val extensionCount: Int,
    val durationSeconds: Long
)
