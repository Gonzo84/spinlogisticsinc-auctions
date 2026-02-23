package eu.auctionplatform.analytics.api.v1.dto

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// =============================================================================
// Response DTOs
// =============================================================================

/**
 * Response representation of the platform-wide overview metrics.
 */
data class PlatformOverviewResponse(
    val activeAuctions: Int,
    val totalBids24h: Long,
    val totalRevenue30d: BigDecimal,
    val registeredUsers: Long,
    val activeBuyers: Long,
    val activeSellers: Long,
    val calculatedAt: Instant
)

/**
 * Response representation of metrics for a specific auction.
 */
data class AuctionMetricsResponse(
    val auctionId: UUID,
    val totalBids: Long,
    val uniqueBidders: Int,
    val avgBidAmount: BigDecimal,
    val maxBid: BigDecimal,
    val extensionCount: Int,
    val durationSeconds: Long
)

/**
 * Response representation of the daily revenue report.
 */
data class RevenueReportResponse(
    val from: LocalDate,
    val to: LocalDate,
    val totalRevenueEur: BigDecimal,
    val totalTransactions: Int,
    val dailyEntries: List<DailyRevenueEntryResponse>
)

/**
 * A single day's revenue data in the response.
 */
data class DailyRevenueEntryResponse(
    val reportDate: LocalDate,
    val revenueEur: BigDecimal,
    val transactionCount: Int,
    val avgTransactionEur: BigDecimal
)

/**
 * Response representation of the user growth report.
 */
data class UserGrowthReportResponse(
    val from: LocalDate,
    val to: LocalDate,
    val totalNewRegistrations: Int,
    val dailyEntries: List<UserGrowthEntryResponse>
)

/**
 * A single day's user growth data in the response.
 */
data class UserGrowthEntryResponse(
    val reportDate: LocalDate,
    val newRegistrations: Int,
    val totalUsers: Long,
    val newBuyers: Int,
    val newSellers: Int
)
