package eu.auctionplatform.analytics.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Snapshot of platform-wide metrics at a point in time.
 *
 * @property activeAuctions  Number of currently active auctions.
 * @property totalBids24h    Total number of bids placed in the last 24 hours.
 * @property totalRevenue30d Total revenue (EUR) from completed transactions in the last 30 days.
 * @property registeredUsers Total number of registered users on the platform.
 * @property activeBuyers    Number of users who placed at least one bid in the last 30 days.
 * @property activeSellers   Number of users who have at least one active lot in the last 30 days.
 * @property calculatedAt    Timestamp when this snapshot was computed.
 */
data class PlatformMetrics(
    val activeAuctions: Int,
    val totalBids24h: Long,
    val totalRevenue30d: BigDecimal,
    val registeredUsers: Long,
    val activeBuyers: Long,
    val activeSellers: Long,
    val calculatedAt: Instant
)
