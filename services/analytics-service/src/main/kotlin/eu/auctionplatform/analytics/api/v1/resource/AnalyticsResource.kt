package eu.auctionplatform.analytics.api.v1.resource

import eu.auctionplatform.analytics.api.v1.dto.AuctionMetricsResponse
import eu.auctionplatform.analytics.api.v1.dto.DailyRevenueEntryResponse
import eu.auctionplatform.analytics.api.v1.dto.PlatformOverviewResponse
import eu.auctionplatform.analytics.api.v1.dto.RevenueReportResponse
import eu.auctionplatform.analytics.api.v1.dto.UserGrowthEntryResponse
import eu.auctionplatform.analytics.api.v1.dto.UserGrowthReportResponse
import eu.auctionplatform.analytics.application.service.AnalyticsService
import eu.auctionplatform.commons.dto.ApiResponse
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

/**
 * REST resource for platform analytics and reporting.
 *
 * Provides endpoints for platform overview, auction-level metrics,
 * revenue reports, and user growth data. All endpoints require
 * admin or moderator roles.
 */
@Path("/api/v1/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class AnalyticsResource {

    @Inject
    lateinit var analyticsService: AnalyticsService

    companion object {
        private val logger = LoggerFactory.getLogger(AnalyticsResource::class.java)
    }

    // -------------------------------------------------------------------------
    // Platform overview
    // -------------------------------------------------------------------------

    /**
     * Returns the platform-wide overview metrics.
     *
     * **GET /api/v1/analytics/overview**
     *
     * @return 200 OK with the platform overview.
     */
    @GET
    @Path("/overview")
    @RolesAllowed("admin", "moderator")
    fun getOverview(): Response {
        logger.debug("GET /overview")

        val metrics = analyticsService.getPlatformOverview()

        val response = PlatformOverviewResponse(
            activeAuctions = metrics.activeAuctions,
            totalBids24h = metrics.totalBids24h,
            totalRevenue30d = metrics.totalRevenue30d,
            registeredUsers = metrics.registeredUsers,
            activeBuyers = metrics.activeBuyers,
            activeSellers = metrics.activeSellers,
            calculatedAt = metrics.calculatedAt
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }

    // -------------------------------------------------------------------------
    // Auction metrics
    // -------------------------------------------------------------------------

    /**
     * Returns aggregated metrics for a specific auction.
     *
     * **GET /api/v1/analytics/auctions/{id}**
     *
     * @param id The auction identifier.
     * @return 200 OK with the auction metrics.
     */
    @GET
    @Path("/auctions/{id}")
    @RolesAllowed("admin", "moderator")
    fun getAuctionMetrics(@PathParam("id") id: UUID): Response {
        logger.debug("GET /auctions/{}", id)

        val metrics = analyticsService.getAuctionMetrics(id)

        val response = AuctionMetricsResponse(
            auctionId = metrics.auctionId,
            totalBids = metrics.totalBids,
            uniqueBidders = metrics.uniqueBidders,
            avgBidAmount = metrics.avgBidAmount,
            maxBid = metrics.maxBid,
            extensionCount = metrics.extensionCount,
            durationSeconds = metrics.durationSeconds
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }

    // -------------------------------------------------------------------------
    // Revenue report
    // -------------------------------------------------------------------------

    /**
     * Returns the daily revenue report for the specified date range.
     *
     * **GET /api/v1/analytics/revenue?from=YYYY-MM-DD&to=YYYY-MM-DD**
     *
     * Defaults to the last 30 days if no dates are provided.
     *
     * @param from Start date (inclusive, ISO format).
     * @param to   End date (inclusive, ISO format).
     * @return 200 OK with the revenue report.
     */
    @GET
    @Path("/revenue")
    @RolesAllowed("admin", "moderator")
    fun getRevenueReport(
        @QueryParam("from") from: String?,
        @QueryParam("to") to: String?
    ): Response {
        val toDate = to?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val fromDate = from?.let { LocalDate.parse(it) } ?: toDate.minusDays(30)

        logger.debug("GET /revenue from={} to={}", fromDate, toDate)

        val entries = analyticsService.getRevenueReport(fromDate, toDate)

        val totalRevenue = entries.fold(java.math.BigDecimal.ZERO) { acc, e -> acc.add(e.revenueEur) }
        val totalTransactions = entries.sumOf { it.transactionCount }

        val response = RevenueReportResponse(
            from = fromDate,
            to = toDate,
            totalRevenueEur = totalRevenue,
            totalTransactions = totalTransactions,
            dailyEntries = entries.map { entry ->
                DailyRevenueEntryResponse(
                    reportDate = entry.reportDate,
                    revenueEur = entry.revenueEur,
                    transactionCount = entry.transactionCount,
                    avgTransactionEur = entry.avgTransactionEur
                )
            }
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }

    // -------------------------------------------------------------------------
    // User growth report
    // -------------------------------------------------------------------------

    /**
     * Returns the user growth report for the specified date range.
     *
     * **GET /api/v1/analytics/growth?from=YYYY-MM-DD&to=YYYY-MM-DD**
     *
     * Defaults to the last 30 days if no dates are provided.
     *
     * @param from Start date (inclusive, ISO format).
     * @param to   End date (inclusive, ISO format).
     * @return 200 OK with the user growth report.
     */
    @GET
    @Path("/growth")
    @RolesAllowed("admin", "moderator")
    fun getUserGrowthReport(
        @QueryParam("from") from: String?,
        @QueryParam("to") to: String?
    ): Response {
        val toDate = to?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val fromDate = from?.let { LocalDate.parse(it) } ?: toDate.minusDays(30)

        logger.debug("GET /growth from={} to={}", fromDate, toDate)

        val entries = analyticsService.getUserGrowthReport(fromDate, toDate)

        val totalNewRegistrations = entries.sumOf { it.newRegistrations }

        val response = UserGrowthReportResponse(
            from = fromDate,
            to = toDate,
            totalNewRegistrations = totalNewRegistrations,
            dailyEntries = entries.map { entry ->
                UserGrowthEntryResponse(
                    reportDate = entry.reportDate,
                    newRegistrations = entry.newRegistrations,
                    totalUsers = entry.totalUsers,
                    newBuyers = entry.newBuyers,
                    newSellers = entry.newSellers
                )
            }
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }
}
