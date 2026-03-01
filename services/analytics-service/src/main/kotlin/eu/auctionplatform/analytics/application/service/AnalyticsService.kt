package eu.auctionplatform.analytics.application.service

import eu.auctionplatform.analytics.domain.model.AuctionMetrics
import eu.auctionplatform.analytics.domain.model.PlatformMetrics
import eu.auctionplatform.analytics.infrastructure.persistence.repository.AnalyticsRepository
import eu.auctionplatform.analytics.infrastructure.persistence.repository.CategoryMetricsEntry
import eu.auctionplatform.analytics.infrastructure.persistence.repository.DailyBidVolumeEntry
import eu.auctionplatform.analytics.infrastructure.persistence.repository.DailyRevenueEntry
import eu.auctionplatform.analytics.infrastructure.persistence.repository.MonthlyRegistrationEntry
import eu.auctionplatform.analytics.infrastructure.persistence.repository.UserGrowthEntry
import eu.auctionplatform.commons.exception.NotFoundException
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Application service that orchestrates analytics query operations.
 *
 * Provides a clean API for the REST layer to retrieve platform overviews,
 * auction-level metrics, revenue reports, and user growth data.
 */
@ApplicationScoped
class AnalyticsService {

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(AnalyticsService::class.java)
    }

    // -------------------------------------------------------------------------
    // Platform overview
    // -------------------------------------------------------------------------

    /**
     * Returns the latest platform-wide metrics snapshot.
     *
     * @return The most recent [PlatformMetrics].
     */
    fun getPlatformOverview(): PlatformMetrics {
        LOG.debug("Fetching platform overview")
        return analyticsRepository.getPlatformOverview()
    }

    // -------------------------------------------------------------------------
    // Auction metrics
    // -------------------------------------------------------------------------

    /**
     * Returns aggregated metrics for a specific auction.
     *
     * @param auctionId The auction identifier.
     * @return The [AuctionMetrics] for the auction.
     * @throws NotFoundException if no metrics exist for the auction.
     */
    fun getAuctionMetrics(auctionId: UUID): AuctionMetrics {
        LOG.debugf("Fetching auction metrics for auction=%s", auctionId)

        return analyticsRepository.getAuctionMetrics(auctionId)
            ?: throw NotFoundException(
                code = "AUCTION_METRICS_NOT_FOUND",
                message = "No metrics found for auction '$auctionId'."
            )
    }

    // -------------------------------------------------------------------------
    // Revenue report
    // -------------------------------------------------------------------------

    /**
     * Returns the daily revenue report for the given date range.
     *
     * @param from Start date (inclusive).
     * @param to   End date (inclusive).
     * @return List of daily revenue entries.
     */
    fun getRevenueReport(from: LocalDate, to: LocalDate): List<DailyRevenueEntry> {
        LOG.debugf("Fetching revenue report from=%s to=%s", from, to)
        return analyticsRepository.getRevenueReport(from, to)
    }

    // -------------------------------------------------------------------------
    // User growth report
    // -------------------------------------------------------------------------

    /**
     * Returns the user growth report for the given date range.
     *
     * @param from Start date (inclusive).
     * @param to   End date (inclusive).
     * @return List of daily user growth entries.
     */
    fun getUserGrowthReport(from: LocalDate, to: LocalDate): List<UserGrowthEntry> {
        LOG.debugf("Fetching user growth report from=%s to=%s", from, to)
        return analyticsRepository.getUserGrowthReport(from, to)
    }

    // -------------------------------------------------------------------------
    // Registration trends (monthly)
    // -------------------------------------------------------------------------

    /**
     * Returns monthly registration trends for the specified number of months.
     *
     * @param months Number of months to look back.
     * @return List of monthly registration entries.
     */
    fun getMonthlyRegistrations(months: Int): List<MonthlyRegistrationEntry> {
        LOG.debugf("Fetching monthly registrations for last %d months", months)
        return analyticsRepository.getMonthlyRegistrations(months)
    }

    // -------------------------------------------------------------------------
    // Category metrics
    // -------------------------------------------------------------------------

    /**
     * Returns category popularity metrics.
     *
     * @return List of category metrics entries, ordered by bid count descending.
     */
    fun getCategoryMetrics(): List<CategoryMetricsEntry> {
        LOG.debug("Fetching category metrics")
        return analyticsRepository.getCategoryMetrics()
    }

    // -------------------------------------------------------------------------
    // Daily bid volume
    // -------------------------------------------------------------------------

    /**
     * Returns daily bid volume for the specified number of lookback days.
     *
     * @param days Number of days to look back.
     * @return List of daily bid volume entries.
     */
    fun getDailyBidVolume(days: Int): List<DailyBidVolumeEntry> {
        LOG.debugf("Fetching daily bid volume for last %d days", days)
        return analyticsRepository.getDailyBidVolume(days)
    }

    // -------------------------------------------------------------------------
    // Platform metrics scheduler
    // -------------------------------------------------------------------------

    /**
     * Periodically computes and inserts a platform_metrics snapshot.
     *
     * Runs every 5 minutes. Aggregates total users from user_growth,
     * total bids in last 24h from bid_volume, and total revenue in
     * last 30 days from daily_revenue.
     */
    @Scheduled(every = "5m")
    fun computePlatformMetrics() {
        try {
            val totalUsers = analyticsRepository.countTotalUsers()
            val totalBids24h = analyticsRepository.countBids24h()
            val totalRevenue30d = analyticsRepository.sumRevenue30d()

            val metrics = PlatformMetrics(
                activeAuctions = 0, // requires cross-service query; set to 0 for now
                totalBids24h = totalBids24h,
                totalRevenue30d = totalRevenue30d,
                registeredUsers = totalUsers,
                activeBuyers = 0,   // requires cross-service query; set to 0 for now
                activeSellers = 0,  // requires cross-service query; set to 0 for now
                calculatedAt = Instant.now()
            )

            analyticsRepository.insertPlatformMetrics(metrics)
            LOG.debugf(
                "Computed platform metrics snapshot: users=%d, bids24h=%d, revenue30d=%s",
                totalUsers, totalBids24h, totalRevenue30d
            )
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to compute platform metrics snapshot: %s", ex.message)
        }
    }
}
