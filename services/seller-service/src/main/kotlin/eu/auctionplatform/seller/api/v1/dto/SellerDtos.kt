package eu.auctionplatform.seller.api.v1.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// =============================================================================
// Seller Service – Request / Response DTOs
// =============================================================================

// -----------------------------------------------------------------------------
// Registration
// -----------------------------------------------------------------------------

/**
 * Request payload for registering a new seller profile.
 */
data class SellerRegistrationRequest(
    val companyName: String,
    val registrationNo: String? = null,
    val ein: String? = null,
    val state: String
)

/**
 * Response payload representing a seller profile.
 */
data class SellerProfileResponse(
    val id: UUID,
    val userId: UUID,
    val companyName: String,
    val registrationNo: String?,
    val ein: String?,
    val state: String,
    val status: String,
    val commissionRate: BigDecimal?,
    val verifiedAt: Instant?,
    val createdAt: Instant
)

// -----------------------------------------------------------------------------
// Dashboard
// -----------------------------------------------------------------------------

/**
 * Response payload for the seller dashboard KPIs.
 */
data class DashboardResponse(
    val activeLots: Int,
    val totalBids: Int,
    val totalHammerSales: BigDecimal,
    val pendingSettlements: Int,
    val totalSettled: BigDecimal
)

// -----------------------------------------------------------------------------
// Lot Summaries
// -----------------------------------------------------------------------------

/**
 * Summary of a lot belonging to the seller, used in paginated listings.
 */
data class LotSummaryResponse(
    val id: UUID,
    val title: String,
    val status: String,
    val currentBid: BigDecimal?,
    val reservePrice: BigDecimal?,
    val bidCount: Int,
    val closingAt: Instant?,
    val createdAt: Instant
)

/**
 * Domain model used within the service layer before mapping to response.
 */
data class LotSummary(
    val id: UUID,
    val title: String,
    val status: String,
    val currentBid: BigDecimal?,
    val reservePrice: BigDecimal?,
    val bidCount: Int,
    val closingAt: Instant?,
    val createdAt: Instant
)

// -----------------------------------------------------------------------------
// Settlements
// -----------------------------------------------------------------------------

/**
 * Summary of a settlement record for the seller.
 */
data class SettlementSummaryResponse(
    val id: UUID,
    val lotId: UUID,
    val lotTitle: String?,
    val hammerPrice: BigDecimal,
    val commissionAmount: BigDecimal,
    val commissionRate: BigDecimal,
    val netAmount: BigDecimal,
    val currency: String,
    val status: String,
    val settledAt: Instant?
)

/**
 * Domain model for settlement summaries.
 */
data class SettlementSummary(
    val id: UUID,
    val lotId: UUID,
    val lotTitle: String?,
    val hammerPrice: BigDecimal,
    val commission: BigDecimal,
    val commissionRate: BigDecimal,
    val netAmount: BigDecimal,
    val currency: String,
    val status: String,
    val settledAt: Instant?
)

// -----------------------------------------------------------------------------
// Analytics
// -----------------------------------------------------------------------------

/**
 * Aggregated analytics data for the seller.
 */
data class SellerAnalyticsResponse(
    val totalLots: Int,
    val totalSold: Int,
    val sellThroughRate: BigDecimal,
    val averageHammerPrice: BigDecimal,
    val totalRevenue: BigDecimal,
    val totalCommissionPaid: BigDecimal,
    val topCategories: List<CategoryBreakdown>,
    val monthlyRevenue: List<MonthlyRevenue>
)

/**
 * Domain model for seller analytics.
 */
data class SellerAnalytics(
    val totalLots: Int,
    val totalSold: Int,
    val sellThroughRate: BigDecimal,
    val averageHammerPrice: BigDecimal,
    val totalRevenue: BigDecimal,
    val totalCommissionPaid: BigDecimal,
    val topCategories: List<CategoryBreakdown>,
    val monthlyRevenue: List<MonthlyRevenue>
)

data class CategoryBreakdown(
    val category: String,
    val lotCount: Int,
    val revenue: BigDecimal
)

data class MonthlyRevenue(
    val month: String,
    val revenue: BigDecimal,
    val lotsSold: Int
)

// -----------------------------------------------------------------------------
// Monthly Settlement Aggregation
// -----------------------------------------------------------------------------

/**
 * Monthly settlement aggregation response for the seller portal.
 */
data class MonthlySettlementResponse(
    val month: String,
    val totalNet: BigDecimal,
    val totalHammer: BigDecimal,
    val totalCommission: BigDecimal,
    val settlementCount: Int
)

// -----------------------------------------------------------------------------
// CO2 Report
// -----------------------------------------------------------------------------

/**
 * CO2 sustainability report for the seller's auction activities.
 */
data class Co2ReportResponse(
    val sellerId: UUID,
    val totalCo2SavedKg: BigDecimal,
    val totalLotsContributed: Int,
    val averageCo2PerLotKg: BigDecimal,
    val equivalentTreesPlanted: BigDecimal,
    val reportPeriod: String,
    val generatedAt: Instant
)

/**
 * Domain model for CO2 report.
 */
data class Co2Report(
    val sellerId: UUID,
    val totalCo2SavedKg: BigDecimal,
    val totalLotsContributed: Int,
    val averageCo2PerLotKg: BigDecimal,
    val equivalentTreesPlanted: BigDecimal,
    val reportPeriod: String,
    val generatedAt: Instant
)
