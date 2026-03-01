package eu.auctionplatform.seller.application.service

import eu.auctionplatform.commons.dto.PagedResponse
import eu.auctionplatform.commons.exception.ConflictException
import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.commons.exception.ValidationException
import eu.auctionplatform.seller.api.v1.dto.CategoryBreakdown
import eu.auctionplatform.seller.api.v1.dto.Co2Report
import eu.auctionplatform.seller.api.v1.dto.LotSummary
import eu.auctionplatform.seller.api.v1.dto.MonthlyRevenue
import eu.auctionplatform.seller.api.v1.dto.SellerAnalytics
import eu.auctionplatform.seller.api.v1.dto.MonthlySettlementResponse
import eu.auctionplatform.seller.api.v1.dto.SellerRegistrationRequest
import eu.auctionplatform.seller.api.v1.dto.SettlementSummary
import eu.auctionplatform.seller.domain.model.SellerDashboard
import eu.auctionplatform.seller.domain.model.SellerProfile
import eu.auctionplatform.seller.domain.model.SellerStatus
import eu.auctionplatform.seller.infrastructure.persistence.repository.SellerProfileRepository
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Application service that orchestrates seller-related operations.
 *
 * Coordinates between the REST API layer and the repository/persistence layer,
 * enforcing business rules around seller registration, dashboard aggregation,
 * lot management, settlements, analytics, and CO2 reporting.
 */
@ApplicationScoped
class SellerService @Inject constructor(
    private val sellerProfileRepository: SellerProfileRepository,
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(SellerService::class.java)

        private const val DEFAULT_COMMISSION_RATE = "0.0500"

        // -- SQL for lot queries against the seller_metrics / external lot data --

        private const val SELECT_LOTS_BY_SELLER = """
            SELECT l.id, l.title, l.status, l.current_bid, l.reserve_price,
                   l.bid_count, l.closing_at, l.created_at
              FROM app.seller_lots l
             WHERE l.seller_id = ?
             ORDER BY l.created_at DESC
             LIMIT ? OFFSET ?
        """

        private const val COUNT_LOTS_BY_SELLER = """
            SELECT COUNT(*) FROM app.seller_lots WHERE seller_id = ?
        """

        private const val SELECT_LOT_BY_ID_AND_SELLER = """
            SELECT l.id, l.title, l.status, l.current_bid, l.reserve_price,
                   l.bid_count, l.closing_at, l.created_at
              FROM app.seller_lots l
             WHERE l.id = ? AND l.seller_id = ?
        """

        private const val SELECT_SETTLEMENTS_BY_SELLER = """
            SELECT s.id, s.lot_id, s.lot_title, s.hammer_price, s.commission,
                   s.net_amount, s.currency, s.status, s.settled_at
              FROM app.seller_settlements s
             WHERE s.seller_id = ?
             ORDER BY s.settled_at DESC NULLS LAST
        """

        private const val SELECT_ANALYTICS_BY_SELLER = """
            SELECT COALESCE(SUM(CASE WHEN m.period = 'ALL' THEN m.active_lots ELSE 0 END), 0) AS total_lots,
                   COALESCE(SUM(CASE WHEN m.period = 'ALL' THEN m.total_bids ELSE 0 END), 0) AS total_bids,
                   COALESCE(SUM(CASE WHEN m.period = 'ALL' THEN m.total_hammer_sales ELSE 0 END), 0) AS total_revenue,
                   COALESCE(MAX(m.sell_through_rate), 0) AS sell_through_rate,
                   COALESCE(SUM(CASE WHEN m.period = 'ALL' THEN m.total_settled ELSE 0 END), 0) AS total_settled
              FROM app.seller_metrics m
             WHERE m.seller_id = ?
        """

        private const val SELECT_CO2_BY_SELLER = """
            SELECT COALESCE(SUM(co2_saved_kg), 0) AS total_co2_saved,
                   COUNT(*) AS total_lots,
                   COALESCE(AVG(co2_saved_kg), 0) AS avg_co2_per_lot
              FROM app.seller_co2 WHERE seller_id = ?
        """

        private const val UPDATE_LOT_ACCEPT_BELOW_RESERVE = """
            UPDATE app.seller_lots SET status = 'ACCEPTED_BELOW_RESERVE' WHERE id = ? AND seller_id = ?
        """

        private const val UPDATE_LOT_RELIST = """
            UPDATE app.seller_lots SET status = 'RELISTED', closing_at = NULL WHERE id = ? AND seller_id = ?
        """

        private const val INIT_METRICS = """
            INSERT INTO app.seller_metrics (seller_id, period, active_lots, total_bids, total_hammer_sales, pending_settlements, total_settled, sell_through_rate)
            VALUES (?, 'ALL', 0, 0, 0, 0, 0, 0)
            ON CONFLICT (seller_id, period) DO NOTHING
        """
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers a new seller profile.
     *
     * Validates that the user does not already have a seller profile, then
     * creates one in PENDING status.
     *
     * @param userId  The authenticated user's ID.
     * @param request The registration request payload.
     * @return The newly created seller profile.
     * @throws ConflictException if a profile already exists for the user.
     * @throws ValidationException if required fields are missing or invalid.
     */
    fun register(userId: UUID, request: SellerRegistrationRequest): SellerProfile {
        // Validate required fields
        val errors = mutableMapOf<String, String>()
        if (request.companyName.isBlank()) {
            errors["companyName"] = "Company name is required"
        }
        if (request.country.isBlank()) {
            errors["country"] = "Country is required"
        }
        if (request.country.isNotBlank() && request.country.length != 2) {
            errors["country"] = "Country must be an ISO 3166-1 alpha-2 code"
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }

        // Check for existing profile
        if (sellerProfileRepository.existsByUserId(userId)) {
            throw ConflictException(
                code = "SELLER_ALREADY_REGISTERED",
                message = "A seller profile already exists for user $userId"
            )
        }

        val profile = SellerProfile(
            userId = userId,
            companyName = request.companyName,
            registrationNo = request.registrationNo,
            vatId = request.vatId,
            country = request.country,
            status = SellerStatus.PENDING,
            createdAt = Instant.now()
        )

        val saved = sellerProfileRepository.save(profile)

        // Initialise metrics row for the new seller
        initMetrics(saved.id)

        LOG.infof("Registered new seller profile %s for user %s", saved.id, userId)
        return saved
    }

    // -------------------------------------------------------------------------
    // Auto-registration (get-or-create)
    // -------------------------------------------------------------------------

    /**
     * Retrieves the seller profile for the given user, auto-creating one if it
     * does not yet exist. Mirrors user-service's getOrCreateUser() pattern.
     */
    fun getOrCreateSeller(userId: UUID, companyName: String, country: String, email: String): SellerProfile {
        sellerProfileRepository.findByUserId(userId)?.let { return it }

        val resolvedCompanyName = companyName.ifBlank {
            email.substringBefore("@").replaceFirstChar { c -> c.uppercase() }
        }
        val resolvedCountry = country.ifBlank { "NL" }.take(2).uppercase()

        val profile = SellerProfile(
            userId = userId,
            companyName = resolvedCompanyName,
            country = resolvedCountry,
            status = SellerStatus.VERIFIED,
            createdAt = Instant.now()
        )

        val saved = sellerProfileRepository.save(profile)
        initMetrics(saved.id)

        LOG.infof("Auto-created seller profile %s for user %s (company=%s)", saved.id, userId, resolvedCompanyName)
        return saved
    }

    // -------------------------------------------------------------------------
    // Dashboard
    // -------------------------------------------------------------------------

    /**
     * Retrieves the aggregated dashboard KPIs for a seller.
     *
     * @param sellerId The seller profile identifier.
     * @return The dashboard data with live metrics.
     * @throws NotFoundException if the seller profile does not exist.
     */
    fun getDashboard(sellerId: UUID): SellerDashboard {
        ensureSellerExists(sellerId)
        return sellerProfileRepository.getDashboard(sellerId)
    }

    // -------------------------------------------------------------------------
    // Lots
    // -------------------------------------------------------------------------

    /**
     * Retrieves a paginated list of lots belonging to the seller.
     *
     * @param sellerId The seller profile identifier.
     * @param page     The page number (1-based).
     * @param size     The page size.
     * @return A paginated response of lot summaries.
     */
    fun getMyLots(sellerId: UUID, page: Int, size: Int): PagedResponse<LotSummary> {
        ensureSellerExists(sellerId)

        val offset = ((page.coerceAtLeast(1)) - 1) * size
        val lots = mutableListOf<LotSummary>()
        var total = 0L

        dataSource.connection.use { conn ->
            // Get total count
            conn.prepareStatement(COUNT_LOTS_BY_SELLER).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        total = rs.getLong(1)
                    }
                }
            }

            // Get page of lots
            conn.prepareStatement(SELECT_LOTS_BY_SELLER).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.setInt(2, size)
                stmt.setInt(3, offset)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        lots.add(
                            LotSummary(
                                id = rs.getObject("id", UUID::class.java),
                                title = rs.getString("title") ?: "",
                                status = rs.getString("status") ?: "UNKNOWN",
                                currentBid = rs.getBigDecimal("current_bid"),
                                reservePrice = rs.getBigDecimal("reserve_price"),
                                bidCount = rs.getInt("bid_count"),
                                closingAt = rs.getTimestamp("closing_at")?.toInstant(),
                                createdAt = rs.getTimestamp("created_at")?.toInstant() ?: run {
                                    LOG.warn("Lot record missing created_at timestamp, using epoch as fallback")
                                    Instant.EPOCH
                                }
                            )
                        )
                    }
                }
            }
        }

        return PagedResponse(
            items = lots,
            total = total,
            page = page.coerceAtLeast(1),
            pageSize = size
        )
    }

    /**
     * Retrieves a single lot belonging to the seller by lot ID.
     *
     * @param sellerId The seller profile identifier.
     * @param lotId    The lot identifier.
     * @return The lot summary.
     * @throws NotFoundException if the lot does not exist or does not belong to the seller.
     */
    fun getLotById(sellerId: UUID, lotId: UUID): LotSummary {
        ensureSellerExists(sellerId)

        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_LOT_BY_ID_AND_SELLER).use { stmt ->
                stmt.setObject(1, lotId)
                stmt.setObject(2, sellerId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        return LotSummary(
                            id = rs.getObject("id", UUID::class.java),
                            title = rs.getString("title") ?: "",
                            status = rs.getString("status") ?: "UNKNOWN",
                            currentBid = rs.getBigDecimal("current_bid"),
                            reservePrice = rs.getBigDecimal("reserve_price"),
                            bidCount = rs.getInt("bid_count"),
                            closingAt = rs.getTimestamp("closing_at")?.toInstant(),
                            createdAt = rs.getTimestamp("created_at")?.toInstant() ?: run {
                                    LOG.warn("Lot record missing created_at timestamp, using epoch as fallback")
                                    Instant.EPOCH
                                }
                        )
                    }
                }
            }
        }

        throw NotFoundException(
            code = "LOT_NOT_FOUND",
            message = "Lot $lotId not found for seller $sellerId"
        )
    }

    // -------------------------------------------------------------------------
    // Settlements
    // -------------------------------------------------------------------------

    /**
     * Retrieves all settlement records for a seller.
     *
     * @param sellerId The seller profile identifier.
     * @return A list of settlement summaries ordered by most recent first.
     */
    fun getSettlements(sellerId: UUID): List<SettlementSummary> {
        ensureSellerExists(sellerId)

        val settlements = mutableListOf<SettlementSummary>()

        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_SETTLEMENTS_BY_SELLER).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        settlements.add(
                            SettlementSummary(
                                id = rs.getObject("id", UUID::class.java),
                                lotId = rs.getObject("lot_id", UUID::class.java),
                                lotTitle = rs.getString("lot_title"),
                                hammerPrice = rs.getBigDecimal("hammer_price") ?: BigDecimal.ZERO,
                                commission = rs.getBigDecimal("commission") ?: BigDecimal.ZERO,
                                netAmount = rs.getBigDecimal("net_amount") ?: BigDecimal.ZERO,
                                currency = rs.getString("currency") ?: "EUR",
                                status = rs.getString("status") ?: "PENDING",
                                settledAt = rs.getTimestamp("settled_at")?.toInstant()
                            )
                        )
                    }
                }
            }
        }

        return settlements
    }

    // -------------------------------------------------------------------------
    // Analytics
    // -------------------------------------------------------------------------

    /**
     * Retrieves aggregated analytics data for a seller.
     *
     * Computes sell-through rate, average hammer price, revenue breakdown
     * by category, and monthly revenue trends.
     *
     * @param sellerId The seller profile identifier.
     * @return The seller analytics data.
     */
    fun getAnalytics(sellerId: UUID): SellerAnalytics {
        ensureSellerExists(sellerId)

        var totalLots = 0
        var totalBids = 0
        var totalRevenue = BigDecimal.ZERO
        var sellThroughRate = BigDecimal.ZERO
        var totalSettled = BigDecimal.ZERO

        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_ANALYTICS_BY_SELLER).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        totalLots = rs.getInt("total_lots")
                        totalBids = rs.getInt("total_bids")
                        totalRevenue = rs.getBigDecimal("total_revenue") ?: BigDecimal.ZERO
                        sellThroughRate = rs.getBigDecimal("sell_through_rate") ?: BigDecimal.ZERO
                        totalSettled = rs.getBigDecimal("total_settled") ?: BigDecimal.ZERO
                    }
                }
            }
        }

        val totalSold = if (sellThroughRate > BigDecimal.ZERO && totalLots > 0) {
            sellThroughRate.multiply(BigDecimal(totalLots)).setScale(0, RoundingMode.HALF_UP).toInt()
        } else {
            0
        }

        val averageHammerPrice = if (totalSold > 0) {
            totalRevenue.divide(BigDecimal(totalSold), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val commissionRate = BigDecimal(DEFAULT_COMMISSION_RATE)
        val totalCommissionPaid = totalRevenue.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP)

        // Fetch real data for top categories and monthly revenue from the database
        val topCategories = sellerProfileRepository.getTopCategories(sellerId)
        val monthlyRevenue = sellerProfileRepository.getMonthlyRevenue(sellerId)

        return SellerAnalytics(
            totalLots = totalLots,
            totalSold = totalSold,
            sellThroughRate = sellThroughRate,
            averageHammerPrice = averageHammerPrice,
            totalRevenue = totalRevenue,
            totalCommissionPaid = totalCommissionPaid,
            topCategories = topCategories,
            monthlyRevenue = monthlyRevenue
        )
    }

    // -------------------------------------------------------------------------
    // CO2 Report
    // -------------------------------------------------------------------------

    /**
     * Generates a CO2 sustainability report for the seller's auction activities.
     *
     * @param sellerId The seller profile identifier.
     * @return The CO2 report with total savings and equivalent metrics.
     */
    fun getCo2Report(sellerId: UUID): Co2Report {
        ensureSellerExists(sellerId)

        var totalCo2Saved = BigDecimal.ZERO
        var totalLots = 0
        var avgCo2PerLot = BigDecimal.ZERO

        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_CO2_BY_SELLER).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        totalCo2Saved = rs.getBigDecimal("total_co2_saved") ?: BigDecimal.ZERO
                        totalLots = rs.getInt("total_lots")
                        avgCo2PerLot = rs.getBigDecimal("avg_co2_per_lot") ?: BigDecimal.ZERO
                    }
                }
            }
        }

        // 1 tree absorbs ~22 kg of CO2 per year
        val treeFactor = BigDecimal("22")
        val equivalentTrees = if (totalCo2Saved > BigDecimal.ZERO) {
            totalCo2Saved.divide(treeFactor, 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return Co2Report(
            sellerId = sellerId,
            totalCo2SavedKg = totalCo2Saved,
            totalLotsContributed = totalLots,
            averageCo2PerLotKg = avgCo2PerLot.setScale(2, RoundingMode.HALF_UP),
            equivalentTreesPlanted = equivalentTrees,
            reportPeriod = "ALL_TIME",
            generatedAt = Instant.now()
        )
    }

    // -------------------------------------------------------------------------
    // Lot Status Counts
    // -------------------------------------------------------------------------

    /**
     * Retrieves a map of lot status to count for the seller.
     *
     * This is more efficient than fetching all lots and counting client-side.
     *
     * @param sellerId The seller profile identifier.
     * @return A map of status strings to their respective counts.
     */
    fun getLotStatusCounts(sellerId: UUID): Map<String, Int> {
        ensureSellerExists(sellerId)
        return sellerProfileRepository.getLotStatusCounts(sellerId)
    }

    // -------------------------------------------------------------------------
    // Monthly Settlements
    // -------------------------------------------------------------------------

    /**
     * Retrieves monthly settlement aggregations for the seller.
     *
     * @param sellerId The seller profile identifier.
     * @return Up to 12 months of settlement totals.
     */
    fun getMonthlySettlements(sellerId: UUID): List<MonthlySettlementResponse> {
        ensureSellerExists(sellerId)
        return sellerProfileRepository.getMonthlySettlements(sellerId).map { row ->
            MonthlySettlementResponse(
                month = row.month,
                totalNet = row.totalNet,
                totalHammer = row.totalHammer,
                totalCommission = row.totalCommission,
                settlementCount = row.settlementCount
            )
        }
    }

    // -------------------------------------------------------------------------
    // Lot Actions
    // -------------------------------------------------------------------------

    /**
     * Accepts a lot that closed below its reserve price.
     *
     * This allows the seller to manually approve the sale at the highest bid
     * even though the reserve was not met.
     *
     * @param sellerId The seller profile identifier.
     * @param lotId    The lot identifier.
     * @throws NotFoundException if the lot does not exist or does not belong to the seller.
     */
    fun acceptBelowReserve(sellerId: UUID, lotId: UUID) {
        ensureSellerExists(sellerId)

        val updated = dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_LOT_ACCEPT_BELOW_RESERVE).use { stmt ->
                stmt.setObject(1, lotId)
                stmt.setObject(2, sellerId)
                stmt.executeUpdate()
            }
        }

        if (updated == 0) {
            throw NotFoundException(
                code = "LOT_NOT_FOUND",
                message = "Lot $lotId not found for seller $sellerId"
            )
        }

        LOG.infof("Seller %s accepted below-reserve for lot %s", sellerId, lotId)
    }

    /**
     * Relists a lot for auction (e.g. after it expired unsold or was withdrawn).
     *
     * Resets the lot status to RELISTED and clears the closing time so it can
     * be assigned to a new auction.
     *
     * @param sellerId The seller profile identifier.
     * @param lotId    The lot identifier.
     * @throws NotFoundException if the lot does not exist or does not belong to the seller.
     */
    fun relistLot(sellerId: UUID, lotId: UUID) {
        ensureSellerExists(sellerId)

        val updated = dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_LOT_RELIST).use { stmt ->
                stmt.setObject(1, lotId)
                stmt.setObject(2, sellerId)
                stmt.executeUpdate()
            }
        }

        if (updated == 0) {
            throw NotFoundException(
                code = "LOT_NOT_FOUND",
                message = "Lot $lotId not found for seller $sellerId"
            )
        }

        LOG.infof("Seller %s relisted lot %s", sellerId, lotId)
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves a seller profile by ID, throwing [NotFoundException] if it does not exist.
     */
    private fun ensureSellerExists(sellerId: UUID): SellerProfile {
        return sellerProfileRepository.findById(sellerId)
            ?: throw NotFoundException(
                code = "SELLER_NOT_FOUND",
                message = "Seller profile $sellerId not found"
            )
    }

    /**
     * Initialises the seller metrics row for a newly registered seller.
     */
    private fun initMetrics(sellerId: UUID) {
        try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(INIT_METRICS).use { stmt ->
                    stmt.setObject(1, sellerId)
                    stmt.executeUpdate()
                }
            }
        } catch (ex: Exception) {
            LOG.warnf("Failed to initialise metrics for seller %s: %s", sellerId, ex.message)
        }
    }
}
