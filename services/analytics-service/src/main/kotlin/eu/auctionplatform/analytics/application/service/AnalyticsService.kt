package eu.auctionplatform.analytics.application.service

import eu.auctionplatform.analytics.domain.model.AuctionMetrics
import eu.auctionplatform.analytics.domain.model.PlatformMetrics
import eu.auctionplatform.analytics.infrastructure.persistence.repository.AnalyticsRepository
import eu.auctionplatform.analytics.infrastructure.persistence.repository.DailyRevenueEntry
import eu.auctionplatform.analytics.infrastructure.persistence.repository.UserGrowthEntry
import eu.auctionplatform.commons.exception.NotFoundException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
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
        private val logger = LoggerFactory.getLogger(AnalyticsService::class.java)
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
        logger.debug("Fetching platform overview")
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
        logger.debug("Fetching auction metrics for auction={}", auctionId)

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
        logger.debug("Fetching revenue report from={} to={}", from, to)
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
        logger.debug("Fetching user growth report from={} to={}", from, to)
        return analyticsRepository.getUserGrowthReport(from, to)
    }
}
