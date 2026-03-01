package eu.auctionplatform.analytics.api.v1.resource

import eu.auctionplatform.analytics.api.v1.dto.AuctionMetricsResponse
import eu.auctionplatform.analytics.api.v1.dto.CategoryPopularityResponse
import eu.auctionplatform.analytics.api.v1.dto.DailyBidVolumeResponse
import eu.auctionplatform.analytics.api.v1.dto.DailyRevenueEntryResponse
import eu.auctionplatform.analytics.api.v1.dto.PlatformOverviewResponse
import eu.auctionplatform.analytics.api.v1.dto.RegistrationTrendResponse
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
import org.jboss.logging.Logger
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
        private val LOG: Logger = Logger.getLogger(AnalyticsResource::class.java)
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
    @RolesAllowed("admin_ops", "admin_super")
    fun getOverview(): Response {
        LOG.debug("GET /overview")

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
    @RolesAllowed("admin_ops", "admin_super")
    fun getAuctionMetrics(@PathParam("id") id: UUID): Response {
        LOG.debugf("GET /auctions/%s", id)

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
    @RolesAllowed("admin_ops", "admin_super")
    fun getRevenueReport(
        @QueryParam("from") from: String?,
        @QueryParam("to") to: String?
    ): Response {
        val toDate = to?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val fromDate = from?.let { LocalDate.parse(it) } ?: toDate.minusDays(30)

        LOG.debugf("GET /revenue from=%s to=%s", fromDate, toDate)

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
    @RolesAllowed("admin_ops", "admin_super")
    fun getUserGrowthReport(
        @QueryParam("from") from: String?,
        @QueryParam("to") to: String?
    ): Response {
        val toDate = to?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val fromDate = from?.let { LocalDate.parse(it) } ?: toDate.minusDays(30)

        LOG.debugf("GET /growth from=%s to=%s", fromDate, toDate)

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

    // -------------------------------------------------------------------------
    // Registration trends (monthly)
    // -------------------------------------------------------------------------

    /**
     * Returns monthly registration trends.
     *
     * **GET /api/v1/analytics/registrations?months=12**
     *
     * @param months Number of months to look back (default 12).
     * @return 200 OK with a list of monthly registration trends.
     */
    @GET
    @Path("/registrations")
    @RolesAllowed("admin_ops", "admin_super")
    fun getRegistrationTrends(
        @QueryParam("months") months: Int?
    ): Response {
        val lookbackMonths = months ?: 12
        LOG.debugf("GET /registrations months=%d", lookbackMonths)

        val entries = analyticsService.getMonthlyRegistrations(lookbackMonths)

        val response = entries.map { entry ->
            RegistrationTrendResponse(
                month = entry.month,
                buyers = entry.buyers,
                sellers = entry.sellers,
                total = entry.total
            )
        }

        return Response.ok(ApiResponse.ok(response)).build()
    }

    // -------------------------------------------------------------------------
    // Category popularity
    // -------------------------------------------------------------------------

    /**
     * Returns category popularity data.
     *
     * **GET /api/v1/analytics/categories**
     *
     * @return 200 OK with a list of category popularity entries.
     */
    @GET
    @Path("/categories")
    @RolesAllowed("admin_ops", "admin_super")
    fun getCategoryPopularity(): Response {
        LOG.debug("GET /categories")

        val entries = analyticsService.getCategoryMetrics()

        val response = entries.map { entry ->
            CategoryPopularityResponse(
                category = entry.category,
                lotCount = entry.lotCount,
                bidCount = entry.bidCount,
                revenue = entry.revenue,
                sellThroughRate = entry.sellThroughRate,
                avgPrice = entry.avgPrice
            )
        }

        return Response.ok(ApiResponse.ok(response)).build()
    }

    // -------------------------------------------------------------------------
    // Daily bid volume
    // -------------------------------------------------------------------------

    /**
     * Returns daily bid volume data.
     *
     * **GET /api/v1/analytics/bids/daily?days=30**
     *
     * @param days Number of days to look back (default 30).
     * @return 200 OK with a list of daily bid volume entries.
     */
    @GET
    @Path("/bids/daily")
    @RolesAllowed("admin_ops", "admin_super")
    fun getDailyBidVolume(
        @QueryParam("days") days: Int?
    ): Response {
        val lookbackDays = days ?: 30
        LOG.debugf("GET /bids/daily days=%d", lookbackDays)

        val entries = analyticsService.getDailyBidVolume(lookbackDays)

        val response = entries.map { entry ->
            DailyBidVolumeResponse(
                date = entry.reportDate.toString(),
                bids = entry.totalBids,
                uniqueBidders = entry.uniqueBidders
            )
        }

        return Response.ok(ApiResponse.ok(response)).build()
    }
}
