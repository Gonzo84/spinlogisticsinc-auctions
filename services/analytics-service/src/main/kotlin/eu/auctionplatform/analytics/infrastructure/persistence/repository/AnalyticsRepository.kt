package eu.auctionplatform.analytics.infrastructure.persistence.repository

import eu.auctionplatform.analytics.domain.model.AuctionMetrics
import eu.auctionplatform.analytics.domain.model.PlatformMetrics
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Repository for analytics persistence operations using direct JDBC.
 *
 * Queries the aggregate analytics tables to produce platform overview,
 * auction-level metrics, revenue reports, and user growth reports.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 */
@ApplicationScoped
class AnalyticsRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    private val logger = LoggerFactory.getLogger(AnalyticsRepository::class.java)

    companion object {
        private const val GET_PLATFORM_OVERVIEW = """
            SELECT active_auctions, total_bids_24h, total_revenue_30d,
                   registered_users, active_buyers, active_sellers, calculated_at
              FROM app.platform_metrics
             ORDER BY calculated_at DESC
             LIMIT 1
        """

        private const val GET_AUCTION_METRICS = """
            SELECT auction_id, total_bids, unique_bidders, avg_bid_amount,
                   max_bid, extension_count, duration_seconds
              FROM app.auction_metrics
             WHERE auction_id = ?
        """

        private const val GET_REVENUE_REPORT = """
            SELECT report_date, revenue_eur, transaction_count, avg_transaction_eur
              FROM app.daily_revenue
             WHERE report_date >= ? AND report_date <= ?
             ORDER BY report_date ASC
        """

        private const val GET_USER_GROWTH_REPORT = """
            SELECT report_date, new_registrations, total_users,
                   new_buyers, new_sellers
              FROM app.user_growth
             WHERE report_date >= ? AND report_date <= ?
             ORDER BY report_date ASC
        """

        private const val UPSERT_PLATFORM_METRICS = """
            INSERT INTO app.platform_metrics
                (id, active_auctions, total_bids_24h, total_revenue_30d,
                 registered_users, active_buyers, active_sellers, calculated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val UPSERT_AUCTION_METRICS = """
            INSERT INTO app.auction_metrics
                (auction_id, total_bids, unique_bidders, avg_bid_amount,
                 max_bid, extension_count, duration_seconds)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (auction_id) DO UPDATE SET
                total_bids      = EXCLUDED.total_bids,
                unique_bidders  = EXCLUDED.unique_bidders,
                avg_bid_amount  = EXCLUDED.avg_bid_amount,
                max_bid         = EXCLUDED.max_bid,
                extension_count = EXCLUDED.extension_count,
                duration_seconds = EXCLUDED.duration_seconds
        """

        private const val UPSERT_DAILY_REVENUE = """
            INSERT INTO app.daily_revenue
                (report_date, revenue_eur, transaction_count, avg_transaction_eur)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (report_date) DO UPDATE SET
                revenue_eur        = app.daily_revenue.revenue_eur + EXCLUDED.revenue_eur,
                transaction_count  = app.daily_revenue.transaction_count + EXCLUDED.transaction_count,
                avg_transaction_eur = (app.daily_revenue.revenue_eur + EXCLUDED.revenue_eur) /
                                      NULLIF(app.daily_revenue.transaction_count + EXCLUDED.transaction_count, 0)
        """

        private const val UPSERT_USER_GROWTH = """
            INSERT INTO app.user_growth
                (report_date, new_registrations, total_users, new_buyers, new_sellers)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (report_date) DO UPDATE SET
                new_registrations = EXCLUDED.new_registrations,
                total_users       = EXCLUDED.total_users,
                new_buyers        = EXCLUDED.new_buyers,
                new_sellers       = EXCLUDED.new_sellers
        """

        private const val INCREMENT_BIDS = """
            UPDATE app.auction_metrics
               SET total_bids = total_bids + 1
             WHERE auction_id = ?
        """

        private const val INCREMENT_EXTENSIONS = """
            UPDATE app.auction_metrics
               SET extension_count = extension_count + 1
             WHERE auction_id = ?
        """
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Returns the latest platform overview metrics.
     *
     * @return The most recent platform metrics snapshot, or a zeroed snapshot.
     */
    fun getPlatformOverview(): PlatformMetrics {
        dataSource.connection.use { conn ->
            conn.prepareStatement(GET_PLATFORM_OVERVIEW).use { stmt ->
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        PlatformMetrics(
                            activeAuctions = rs.getInt("active_auctions"),
                            totalBids24h = rs.getLong("total_bids_24h"),
                            totalRevenue30d = rs.getBigDecimal("total_revenue_30d") ?: BigDecimal.ZERO,
                            registeredUsers = rs.getLong("registered_users"),
                            activeBuyers = rs.getLong("active_buyers"),
                            activeSellers = rs.getLong("active_sellers"),
                            calculatedAt = rs.getTimestamp("calculated_at").toInstant()
                        )
                    } else {
                        PlatformMetrics(
                            activeAuctions = 0,
                            totalBids24h = 0,
                            totalRevenue30d = BigDecimal.ZERO,
                            registeredUsers = 0,
                            activeBuyers = 0,
                            activeSellers = 0,
                            calculatedAt = Instant.now()
                        )
                    }
                }
            }
        }
    }

    /**
     * Returns aggregated metrics for a specific auction.
     *
     * @param auctionId The auction identifier.
     * @return The auction metrics, or null if not found.
     */
    fun getAuctionMetrics(auctionId: UUID): AuctionMetrics? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(GET_AUCTION_METRICS).use { stmt ->
                stmt.setObject(1, auctionId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        AuctionMetrics(
                            auctionId = rs.getObject("auction_id", UUID::class.java),
                            totalBids = rs.getLong("total_bids"),
                            uniqueBidders = rs.getInt("unique_bidders"),
                            avgBidAmount = rs.getBigDecimal("avg_bid_amount") ?: BigDecimal.ZERO,
                            maxBid = rs.getBigDecimal("max_bid") ?: BigDecimal.ZERO,
                            extensionCount = rs.getInt("extension_count"),
                            durationSeconds = rs.getLong("duration_seconds")
                        )
                    } else {
                        null
                    }
                }
            }
        }
    }

    /**
     * Returns the daily revenue report for the given date range.
     *
     * @param from Start date (inclusive).
     * @param to   End date (inclusive).
     * @return List of daily revenue entries.
     */
    fun getRevenueReport(from: LocalDate, to: LocalDate): List<DailyRevenueEntry> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(GET_REVENUE_REPORT).use { stmt ->
                stmt.setObject(1, from)
                stmt.setObject(2, to)
                stmt.executeQuery().use { rs ->
                    val entries = mutableListOf<DailyRevenueEntry>()
                    while (rs.next()) {
                        entries.add(
                            DailyRevenueEntry(
                                reportDate = rs.getObject("report_date", LocalDate::class.java),
                                revenueEur = rs.getBigDecimal("revenue_eur") ?: BigDecimal.ZERO,
                                transactionCount = rs.getInt("transaction_count"),
                                avgTransactionEur = rs.getBigDecimal("avg_transaction_eur") ?: BigDecimal.ZERO
                            )
                        )
                    }
                    return entries
                }
            }
        }
    }

    /**
     * Returns the user growth report for the given date range.
     *
     * @param from Start date (inclusive).
     * @param to   End date (inclusive).
     * @return List of daily user growth entries.
     */
    fun getUserGrowthReport(from: LocalDate, to: LocalDate): List<UserGrowthEntry> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(GET_USER_GROWTH_REPORT).use { stmt ->
                stmt.setObject(1, from)
                stmt.setObject(2, to)
                stmt.executeQuery().use { rs ->
                    val entries = mutableListOf<UserGrowthEntry>()
                    while (rs.next()) {
                        entries.add(
                            UserGrowthEntry(
                                reportDate = rs.getObject("report_date", LocalDate::class.java),
                                newRegistrations = rs.getInt("new_registrations"),
                                totalUsers = rs.getLong("total_users"),
                                newBuyers = rs.getInt("new_buyers"),
                                newSellers = rs.getInt("new_sellers")
                            )
                        )
                    }
                    return entries
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Write operations (used by event consumers)
    // -------------------------------------------------------------------------

    /**
     * Inserts a new platform metrics snapshot.
     */
    fun insertPlatformMetrics(metrics: PlatformMetrics) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPSERT_PLATFORM_METRICS).use { stmt ->
                stmt.setObject(1, UUID.randomUUID())
                stmt.setInt(2, metrics.activeAuctions)
                stmt.setLong(3, metrics.totalBids24h)
                stmt.setBigDecimal(4, metrics.totalRevenue30d)
                stmt.setLong(5, metrics.registeredUsers)
                stmt.setLong(6, metrics.activeBuyers)
                stmt.setLong(7, metrics.activeSellers)
                stmt.setTimestamp(8, Timestamp.from(metrics.calculatedAt))
                stmt.executeUpdate()
            }
        }
        logger.debug("Inserted platform metrics snapshot at {}", metrics.calculatedAt)
    }

    /**
     * Upserts auction metrics (insert or update on conflict).
     */
    fun upsertAuctionMetrics(metrics: AuctionMetrics) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPSERT_AUCTION_METRICS).use { stmt ->
                stmt.setObject(1, metrics.auctionId)
                stmt.setLong(2, metrics.totalBids)
                stmt.setInt(3, metrics.uniqueBidders)
                stmt.setBigDecimal(4, metrics.avgBidAmount)
                stmt.setBigDecimal(5, metrics.maxBid)
                stmt.setInt(6, metrics.extensionCount)
                stmt.setLong(7, metrics.durationSeconds)
                stmt.executeUpdate()
            }
        }
        logger.debug("Upserted auction metrics for auction {}", metrics.auctionId)
    }

    /**
     * Upserts a daily revenue entry (accumulates revenue on conflict).
     */
    fun upsertDailyRevenue(entry: DailyRevenueEntry) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPSERT_DAILY_REVENUE).use { stmt ->
                stmt.setObject(1, entry.reportDate)
                stmt.setBigDecimal(2, entry.revenueEur)
                stmt.setInt(3, entry.transactionCount)
                stmt.setBigDecimal(4, entry.avgTransactionEur)
                stmt.executeUpdate()
            }
        }
        logger.debug("Upserted daily revenue for {}", entry.reportDate)
    }

    /**
     * Upserts a user growth entry.
     */
    fun upsertUserGrowth(entry: UserGrowthEntry) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPSERT_USER_GROWTH).use { stmt ->
                stmt.setObject(1, entry.reportDate)
                stmt.setInt(2, entry.newRegistrations)
                stmt.setLong(3, entry.totalUsers)
                stmt.setInt(4, entry.newBuyers)
                stmt.setInt(5, entry.newSellers)
                stmt.executeUpdate()
            }
        }
        logger.debug("Upserted user growth for {}", entry.reportDate)
    }

    /**
     * Increments the bid counter for an auction.
     */
    fun incrementBidCount(auctionId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INCREMENT_BIDS).use { stmt ->
                stmt.setObject(1, auctionId)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Increments the extension counter for an auction.
     */
    fun incrementExtensionCount(auctionId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INCREMENT_EXTENSIONS).use { stmt ->
                stmt.setObject(1, auctionId)
                stmt.executeUpdate()
            }
        }
    }
}

/**
 * A single day's revenue data.
 */
data class DailyRevenueEntry(
    val reportDate: LocalDate,
    val revenueEur: BigDecimal,
    val transactionCount: Int,
    val avgTransactionEur: BigDecimal
)

/**
 * A single day's user growth data.
 */
data class UserGrowthEntry(
    val reportDate: LocalDate,
    val newRegistrations: Int,
    val totalUsers: Long,
    val newBuyers: Int,
    val newSellers: Int
)
