package eu.auctionplatform.seller.infrastructure.persistence.repository

import eu.auctionplatform.seller.api.v1.dto.CategoryBreakdown
import eu.auctionplatform.seller.api.v1.dto.MonthlyRevenue
import eu.auctionplatform.seller.domain.model.SellerDashboard
import eu.auctionplatform.seller.domain.model.SellerProfile
import eu.auctionplatform.seller.domain.model.SellerStatus
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Direct JDBC repository for [SellerProfile] persistence and dashboard metrics.
 *
 * Uses the named "system" datasource via Agroal, following the platform's
 * repository pattern of direct SQL control without ORM overhead.
 */
@ApplicationScoped
class SellerProfileRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(SellerProfileRepository::class.java)

        private const val SELECT_PROFILE_COLUMNS = """
            id, user_id, company_name, registration_no, ein,
            state, status, verified_at, created_at
        """

        private const val INSERT_PROFILE = """
            INSERT INTO app.seller_profiles
                (id, user_id, company_name, registration_no, ein,
                 state, status, verified_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_PROFILE_COLUMNS FROM app.seller_profiles WHERE id = ?
        """

        private const val SELECT_BY_USER_ID = """
            SELECT $SELECT_PROFILE_COLUMNS FROM app.seller_profiles WHERE user_id = ?
        """

        private const val SELECT_BY_EIN = """
            SELECT $SELECT_PROFILE_COLUMNS FROM app.seller_profiles WHERE ein = ?
        """

        private const val UPDATE_STATUS = """
            UPDATE app.seller_profiles SET status = ?, verified_at = ? WHERE id = ?
        """

        private const val UPDATE_PROFILE = """
            UPDATE app.seller_profiles
               SET company_name = ?, registration_no = ?, ein = ?, state = ?
             WHERE id = ?
        """

        private const val DELETE_BY_ID = """
            DELETE FROM app.seller_profiles WHERE id = ?
        """

        private const val SELECT_DASHBOARD = """
            SELECT active_lots, total_bids, total_hammer_sales,
                   pending_settlements, total_settled
              FROM app.seller_metrics
             WHERE seller_id = ?
        """

        private const val UPSERT_METRICS = """
            INSERT INTO app.seller_metrics
                (seller_id, period, active_lots, total_bids, total_hammer_sales,
                 pending_settlements, total_settled, updated_at)
            VALUES (?, 'ALL', ?, ?, ?, ?, ?, NOW())
            ON CONFLICT (seller_id) DO UPDATE SET
                active_lots = EXCLUDED.active_lots,
                total_bids = EXCLUDED.total_bids,
                total_hammer_sales = EXCLUDED.total_hammer_sales,
                pending_settlements = EXCLUDED.pending_settlements,
                total_settled = EXCLUDED.total_settled,
                updated_at = NOW()
        """

        private const val INCREMENT_ACTIVE_LOTS = """
            UPDATE app.seller_metrics
               SET active_lots = active_lots + 1, updated_at = NOW()
             WHERE seller_id = ?
        """

        private const val DECREMENT_ACTIVE_LOTS = """
            UPDATE app.seller_metrics
               SET active_lots = GREATEST(active_lots - 1, 0), updated_at = NOW()
             WHERE seller_id = ?
        """

        private const val INCREMENT_BIDS = """
            UPDATE app.seller_metrics
               SET total_bids = total_bids + 1, updated_at = NOW()
             WHERE seller_id = ?
        """

        private const val ADD_HAMMER_SALE = """
            UPDATE app.seller_metrics
               SET total_hammer_sales = total_hammer_sales + ?,
                   pending_settlements = pending_settlements + 1,
                   active_lots = GREATEST(active_lots - 1, 0),
                   updated_at = NOW()
             WHERE seller_id = ?
        """

        private const val SETTLE_PAYMENT = """
            UPDATE app.seller_metrics
               SET total_settled = total_settled + ?,
                   pending_settlements = GREATEST(pending_settlements - 1, 0),
                   updated_at = NOW()
             WHERE seller_id = ?
        """

        private const val EXISTS_BY_USER_ID = """
            SELECT COUNT(*) FROM app.seller_profiles WHERE user_id = ?
        """

        // -----------------------------------------------------------------------
        // Seller lot projection queries (used by NATS consumers)
        // -----------------------------------------------------------------------

        private const val SELECT_SELLER_ID_BY_LOT_ID = """
            SELECT seller_id FROM app.seller_lots WHERE id = ?
        """

        private const val SELECT_SELLER_PROFILE_ID_BY_USER_ID = """
            SELECT id FROM app.seller_profiles WHERE user_id = ?
        """

        private const val INSERT_SELLER_LOT = """
            INSERT INTO app.seller_lots
                (id, seller_id, title, status, reserve_price, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, NOW(), NOW())
            ON CONFLICT (id) DO NOTHING
        """

        private const val UPDATE_LOT_STATUS = """
            UPDATE app.seller_lots SET status = ?, updated_at = NOW() WHERE id = ?
        """

        private const val UPDATE_LOT_BID = """
            UPDATE app.seller_lots
               SET current_bid = ?, updated_at = NOW()
             WHERE id = ?
        """

        private const val UPDATE_LOT_BID_WITH_COUNT = """
            UPDATE app.seller_lots
               SET current_bid = ?, bid_count = ?, updated_at = NOW()
             WHERE id = ?
        """

        // -----------------------------------------------------------------------
        // Seller settlement queries (used by NATS consumers)
        // -----------------------------------------------------------------------

        private const val INSERT_SETTLEMENT = """
            INSERT INTO app.seller_settlements
                (id, seller_id, lot_id, lot_title, hammer_price, commission, commission_rate,
                 net_amount, currency, status, payment_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
        """

        private const val UPDATE_SETTLEMENT_STATUS_BY_PAYMENT_ID = """
            UPDATE app.seller_settlements
               SET status = ?, settled_at = ?, updated_at = NOW()
             WHERE payment_id = ?
        """

        private const val DELETE_SETTLEMENT_BY_LOT_ID = """
            DELETE FROM app.seller_settlements WHERE lot_id = ? AND status = 'READY'
        """

        private const val REVERT_HAMMER_SALE = """
            UPDATE app.seller_metrics
               SET total_hammer_sales = GREATEST(total_hammer_sales - ?, 0),
                   pending_settlements = GREATEST(pending_settlements - 1, 0),
                   active_lots = active_lots + 1,
                   updated_at = NOW()
             WHERE seller_id = ?
        """

        // -----------------------------------------------------------------------
        // Analytics queries (top categories and monthly revenue)
        // -----------------------------------------------------------------------

        private const val SELECT_TOP_CATEGORIES = """
            SELECT COALESCE(l.status, 'UNKNOWN') AS category_id,
                   COUNT(*) AS lot_count,
                   COALESCE(SUM(l.current_bid), 0) AS revenue
              FROM app.seller_lots l
             WHERE l.seller_id = ?
             GROUP BY l.status
             ORDER BY revenue DESC
             LIMIT 5
        """

        private const val SELECT_MONTHLY_REVENUE = """
            SELECT DATE_TRUNC('month', s.settled_at) AS month,
                   SUM(s.net_amount) AS amount,
                   COUNT(*) AS count
              FROM app.seller_settlements s
             WHERE s.seller_id = ? AND s.settled_at IS NOT NULL
             GROUP BY month
             ORDER BY month DESC
             LIMIT 12
        """

        private const val SELECT_LOT_STATUS_COUNTS = """
            SELECT status, COUNT(*) AS cnt
              FROM app.seller_lots
             WHERE seller_id = ?
             GROUP BY status
        """

        private const val SELECT_MONTHLY_SETTLEMENTS = """
            SELECT DATE_TRUNC('month', s.settled_at) AS month,
                   SUM(s.net_amount) AS total_net,
                   SUM(s.hammer_price) AS total_hammer,
                   SUM(s.commission) AS total_commission,
                   COUNT(*) AS settlement_count
              FROM app.seller_settlements s
             WHERE s.seller_id = ? AND s.settled_at IS NOT NULL
             GROUP BY month
             ORDER BY month DESC
             LIMIT 12
        """

        // -----------------------------------------------------------------------
        // CO2 insert (used by Co2EventSellerConsumer)
        // -----------------------------------------------------------------------

        private const val INSERT_SELLER_CO2 = """
            INSERT INTO app.seller_co2 (seller_id, lot_id, co2_saved_kg, created_at)
            VALUES (?, ?, ?, NOW())
        """
    }

    /**
     * Persists a new seller profile.
     *
     * @param profile The seller profile to insert.
     * @return The persisted profile.
     */
    fun save(profile: SellerProfile): SellerProfile {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT_PROFILE).use { stmt ->
                stmt.setObject(1, profile.id)
                stmt.setObject(2, profile.userId)
                stmt.setString(3, profile.companyName)
                stmt.setString(4, profile.registrationNo)
                stmt.setString(5, profile.ein)
                stmt.setString(6, profile.state)
                stmt.setString(7, profile.status.name)
                stmt.setTimestamp(8, profile.verifiedAt?.let { Timestamp.from(it) })
                stmt.setTimestamp(9, Timestamp.from(profile.createdAt))
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Saved seller profile %s for user %s", profile.id, profile.userId)
        return profile
    }

    /**
     * Finds a seller profile by its unique identifier.
     */
    fun findById(id: UUID): SellerProfile? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toSellerProfile() else null
                }
            }
        }
    }

    /**
     * Finds a seller profile by the associated user ID.
     */
    fun findByUserId(userId: UUID): SellerProfile? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_USER_ID).use { stmt ->
                stmt.setObject(1, userId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toSellerProfile() else null
                }
            }
        }
    }

    /**
     * Finds a seller profile by EIN.
     */
    fun findByEin(ein: String): SellerProfile? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_EIN).use { stmt ->
                stmt.setString(1, ein)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toSellerProfile() else null
                }
            }
        }
    }

    /**
     * Checks whether a seller profile already exists for the given user ID.
     */
    fun existsByUserId(userId: UUID): Boolean {
        dataSource.connection.use { conn ->
            conn.prepareStatement(EXISTS_BY_USER_ID).use { stmt ->
                stmt.setObject(1, userId)
                stmt.executeQuery().use { rs ->
                    rs.next()
                    return rs.getLong(1) > 0
                }
            }
        }
    }

    /**
     * Updates the verification status of a seller profile.
     */
    fun updateStatus(id: UUID, status: SellerStatus, verifiedAt: Instant? = null) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                stmt.setTimestamp(2, verifiedAt?.let { Timestamp.from(it) })
                stmt.setObject(3, id)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Updates mutable profile fields.
     */
    fun updateProfile(profile: SellerProfile) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_PROFILE).use { stmt ->
                stmt.setString(1, profile.companyName)
                stmt.setString(2, profile.registrationNo)
                stmt.setString(3, profile.ein)
                stmt.setString(4, profile.state)
                stmt.setObject(5, profile.id)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Deletes a seller profile by its identifier.
     */
    fun deleteById(id: UUID): Boolean {
        dataSource.connection.use { conn ->
            conn.prepareStatement(DELETE_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                return stmt.executeUpdate() > 0
            }
        }
    }

    // -----------------------------------------------------------------------
    // Dashboard metrics
    // -----------------------------------------------------------------------

    /**
     * Retrieves the dashboard KPIs for a seller.
     *
     * @param sellerId The seller profile identifier.
     * @return The dashboard data, or a zeroed-out dashboard if no metrics exist yet.
     */
    fun getDashboard(sellerId: UUID): SellerDashboard {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_DASHBOARD).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        SellerDashboard(
                            activeLots = rs.getInt("active_lots"),
                            totalBids = rs.getInt("total_bids"),
                            totalHammerSales = rs.getBigDecimal("total_hammer_sales") ?: BigDecimal.ZERO,
                            pendingSettlements = rs.getInt("pending_settlements"),
                            totalSettled = rs.getBigDecimal("total_settled") ?: BigDecimal.ZERO
                        )
                    } else {
                        SellerDashboard()
                    }
                }
            }
        }
    }

    /**
     * Upserts the full set of seller metrics (used for bulk reconciliation).
     */
    fun upsertMetrics(sellerId: UUID, dashboard: SellerDashboard) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPSERT_METRICS).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.setInt(2, dashboard.activeLots)
                stmt.setInt(3, dashboard.totalBids)
                stmt.setBigDecimal(4, dashboard.totalHammerSales)
                stmt.setInt(5, dashboard.pendingSettlements)
                stmt.setBigDecimal(6, dashboard.totalSettled)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Increments the active lots counter for a seller.
     */
    fun incrementActiveLots(sellerId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INCREMENT_ACTIVE_LOTS).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Decrements the active lots counter for a seller (floor at 0).
     */
    fun decrementActiveLots(sellerId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(DECREMENT_ACTIVE_LOTS).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Increments the total bids counter for a seller.
     */
    fun incrementBids(sellerId: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INCREMENT_BIDS).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Records a hammer sale for a seller (adds to total and increments pending settlements).
     */
    fun addHammerSale(sellerId: UUID, amount: BigDecimal) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(ADD_HAMMER_SALE).use { stmt ->
                stmt.setBigDecimal(1, amount)
                stmt.setObject(2, sellerId)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Records a settlement payment for a seller.
     *
     * @param sellerId The seller profile identifier.
     * @param netAmount The net amount settled (after commission deduction).
     */
    fun settlePayment(sellerId: UUID, netAmount: BigDecimal) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SETTLE_PAYMENT).use { stmt ->
                stmt.setBigDecimal(1, netAmount)
                stmt.setObject(2, sellerId)
                stmt.executeUpdate()
            }
        }
    }

    // -----------------------------------------------------------------------
    // Seller lot projection (used by NATS consumers)
    // -----------------------------------------------------------------------

    /**
     * Finds the seller_id for a given lot from the seller_lots projection table.
     *
     * This is used by auction event consumers to resolve the seller when
     * auction events do not carry a sellerId field.
     *
     * @param lotId The lot identifier (seller_lots.id).
     * @return The seller profile ID, or null if the lot is not in the projection.
     */
    fun findSellerIdByLotId(lotId: UUID): UUID? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_SELLER_ID_BY_LOT_ID).use { stmt ->
                stmt.setObject(1, lotId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getObject("seller_id", UUID::class.java) else null
                }
            }
        }
    }

    /**
     * Finds the seller profile ID for a given user ID.
     *
     * Catalog events contain the userId as `sellerId`, but seller_lots.seller_id
     * references seller_profiles.id, not user_id. This method resolves the mapping.
     *
     * @param userId The user identifier.
     * @return The seller profile ID, or null if no profile exists.
     */
    fun findSellerProfileIdByUserId(userId: UUID): UUID? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_SELLER_PROFILE_ID_BY_USER_ID).use { stmt ->
                stmt.setObject(1, userId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getObject("id", UUID::class.java) else null
                }
            }
        }
    }

    /**
     * Inserts a lot into the seller_lots projection table.
     *
     * Uses ON CONFLICT DO NOTHING for idempotency (at-least-once delivery).
     *
     * @param lotId The lot identifier (from catalog-service).
     * @param sellerId The seller profile identifier (seller_profiles.id).
     * @param title The lot title.
     * @param status The initial lot status.
     * @param reservePrice The seller's reserve price.
     */
    fun insertSellerLot(
        lotId: UUID,
        sellerId: UUID,
        title: String,
        status: String,
        reservePrice: BigDecimal?
    ) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT_SELLER_LOT).use { stmt ->
                stmt.setObject(1, lotId)
                stmt.setObject(2, sellerId)
                stmt.setString(3, title)
                stmt.setString(4, status)
                stmt.setBigDecimal(5, reservePrice)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Inserted seller lot projection [lotId=%s, sellerId=%s]", lotId, sellerId)
    }

    /**
     * Updates the status of a lot in the seller_lots projection table.
     *
     * @param lotId The lot identifier.
     * @param status The new status.
     */
    fun updateLotStatus(lotId: UUID, status: String) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_LOT_STATUS).use { stmt ->
                stmt.setString(1, status)
                stmt.setObject(2, lotId)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Updates the current bid (and optionally bid count) for a lot in
     * the seller_lots projection table.
     *
     * @param lotId The lot identifier.
     * @param currentBid The new current bid amount.
     * @param bidCount The new bid count (null to leave unchanged).
     */
    fun updateLotBid(lotId: UUID, currentBid: BigDecimal, bidCount: Int?) {
        if (bidCount != null) {
            dataSource.connection.use { conn ->
                conn.prepareStatement(UPDATE_LOT_BID_WITH_COUNT).use { stmt ->
                    stmt.setBigDecimal(1, currentBid)
                    stmt.setInt(2, bidCount)
                    stmt.setObject(3, lotId)
                    stmt.executeUpdate()
                }
            }
        } else {
            dataSource.connection.use { conn ->
                conn.prepareStatement(UPDATE_LOT_BID).use { stmt ->
                    stmt.setBigDecimal(1, currentBid)
                    stmt.setObject(2, lotId)
                    stmt.executeUpdate()
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Settlement records (used by NATS consumers)
    // -----------------------------------------------------------------------

    /**
     * Inserts a settlement record into the seller_settlements table.
     *
     * @param sellerId The seller profile identifier.
     * @param lotId The lot identifier.
     * @param lotTitle The lot title (for display, may be null).
     * @param hammerPrice The winning bid / hammer price.
     * @param commission The platform commission withheld.
     * @param netAmount The net amount to be paid to the seller.
     * @param currency ISO 4217 currency code.
     * @param status Settlement status (e.g. "READY", "PENDING", "SETTLED").
     */
    fun insertSettlement(
        sellerId: UUID,
        lotId: UUID,
        lotTitle: String?,
        hammerPrice: BigDecimal,
        commission: BigDecimal,
        commissionRate: BigDecimal = BigDecimal("0.10"),
        netAmount: BigDecimal,
        currency: String,
        status: String,
        paymentId: UUID? = null
    ) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT_SETTLEMENT).use { stmt ->
                stmt.setObject(1, UUID.randomUUID())
                stmt.setObject(2, sellerId)
                stmt.setObject(3, lotId)
                stmt.setString(4, lotTitle)
                stmt.setBigDecimal(5, hammerPrice)
                stmt.setBigDecimal(6, commission)
                stmt.setBigDecimal(7, commissionRate)
                stmt.setBigDecimal(8, netAmount)
                stmt.setString(9, currency)
                stmt.setString(10, status)
                stmt.setObject(11, paymentId)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Inserted settlement record for seller %s (lot=%s, net=%s %s, paymentId=%s)",
            sellerId, lotId, netAmount, currency, paymentId)
    }

    /**
     * Updates the settlement status and settled_at timestamp for a settlement
     * identified by its originating payment ID.
     *
     * Used by the PaymentEventSellerConsumer when processing
     * `payment.settlement.settled` events.
     *
     * @param paymentId The originating payment UUID.
     * @param status The new settlement status (e.g. "PAID").
     * @param settledAt When the settlement was completed (null to leave unchanged).
     * @return true if a row was updated, false if no settlement was found for the paymentId.
     */
    fun updateSettlementStatusByPaymentId(paymentId: UUID, status: String, settledAt: Instant?): Boolean {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_SETTLEMENT_STATUS_BY_PAYMENT_ID).use { stmt ->
                stmt.setString(1, status)
                stmt.setTimestamp(2, settledAt?.let { Timestamp.from(it) })
                stmt.setObject(3, paymentId)
                return stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * Deletes any READY settlement records for a given lot.
     *
     * Used when an award is revoked -- only READY (not yet PAID) settlements
     * should be cleaned up.
     *
     * @param lotId The lot identifier.
     * @return The number of rows deleted.
     */
    fun deleteSettlementByLotId(lotId: UUID): Int {
        dataSource.connection.use { conn ->
            conn.prepareStatement(DELETE_SETTLEMENT_BY_LOT_ID).use { stmt ->
                stmt.setObject(1, lotId)
                return stmt.executeUpdate()
            }
        }
    }

    /**
     * Reverses the metrics changes from [addHammerSale] when an award is revoked.
     *
     * Decrements pending_settlements, subtracts the hammer amount from
     * total_hammer_sales, and re-increments active_lots.
     *
     * @param sellerId The seller profile identifier.
     * @param hammerAmount The hammer price to subtract.
     */
    fun revertHammerSale(sellerId: UUID, hammerAmount: BigDecimal) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(REVERT_HAMMER_SALE).use { stmt ->
                stmt.setBigDecimal(1, hammerAmount)
                stmt.setObject(2, sellerId)
                stmt.executeUpdate()
            }
        }
    }

    // -----------------------------------------------------------------------
    // Analytics queries
    // -----------------------------------------------------------------------

    /**
     * Retrieves the top categories for a seller based on lot status breakdown
     * and revenue from current bids.
     *
     * @param sellerId The seller profile identifier.
     * @return Up to 5 category breakdowns ordered by revenue descending.
     */
    fun getTopCategories(sellerId: UUID): List<CategoryBreakdown> {
        val categories = mutableListOf<CategoryBreakdown>()
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_TOP_CATEGORIES).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        categories.add(
                            CategoryBreakdown(
                                category = rs.getString("category_id"),
                                lotCount = rs.getInt("lot_count"),
                                revenue = rs.getBigDecimal("revenue")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO
                            )
                        )
                    }
                }
            }
        }
        return categories
    }

    /**
     * Retrieves monthly revenue data for a seller from settled settlements.
     *
     * @param sellerId The seller profile identifier.
     * @return Up to 12 months of revenue data ordered by most recent first.
     */
    fun getMonthlyRevenue(sellerId: UUID): List<MonthlyRevenue> {
        val months = mutableListOf<MonthlyRevenue>()
        val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_MONTHLY_REVENUE).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val monthTs = rs.getTimestamp("month")
                        val monthStr = if (monthTs != null) {
                            monthTs.toLocalDateTime().format(monthFormatter)
                        } else {
                            "unknown"
                        }
                        months.add(
                            MonthlyRevenue(
                                month = monthStr,
                                revenue = rs.getBigDecimal("amount")?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO,
                                lotsSold = rs.getInt("count")
                            )
                        )
                    }
                }
            }
        }
        return months
    }

    /**
     * Retrieves lot status counts for a seller.
     *
     * @param sellerId The seller profile identifier.
     * @return A map of status to count.
     */
    fun getLotStatusCounts(sellerId: UUID): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_LOT_STATUS_COUNTS).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        counts[rs.getString("status")] = rs.getInt("cnt")
                    }
                }
            }
        }
        return counts
    }

    /**
     * Retrieves monthly settlement aggregations for a seller.
     *
     * @param sellerId The seller profile identifier.
     * @return Up to 12 months of settlement totals ordered by most recent first.
     */
    fun getMonthlySettlements(sellerId: UUID): List<MonthlySettlementRow> {
        val rows = mutableListOf<MonthlySettlementRow>()
        val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_MONTHLY_SETTLEMENTS).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val monthTs = rs.getTimestamp("month")
                        val monthStr = if (monthTs != null) {
                            monthTs.toLocalDateTime().format(monthFormatter)
                        } else {
                            "unknown"
                        }
                        rows.add(
                            MonthlySettlementRow(
                                month = monthStr,
                                totalNet = rs.getBigDecimal("total_net") ?: BigDecimal.ZERO,
                                totalHammer = rs.getBigDecimal("total_hammer") ?: BigDecimal.ZERO,
                                totalCommission = rs.getBigDecimal("total_commission") ?: BigDecimal.ZERO,
                                settlementCount = rs.getInt("settlement_count")
                            )
                        )
                    }
                }
            }
        }
        return rows
    }

    // -----------------------------------------------------------------------
    // CO2 records (used by Co2EventSellerConsumer)
    // -----------------------------------------------------------------------

    /**
     * Inserts a CO2 savings record for a seller's lot.
     *
     * @param sellerId The seller profile identifier.
     * @param lotId The lot identifier.
     * @param co2SavedKg The CO2 saved in kilograms.
     */
    fun insertCo2Record(sellerId: UUID, lotId: UUID, co2SavedKg: BigDecimal) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT_SELLER_CO2).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.setObject(2, lotId)
                stmt.setBigDecimal(3, co2SavedKg)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Inserted CO2 record for seller %s (lot=%s, co2=%s kg)", sellerId, lotId, co2SavedKg)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toSellerProfile(): SellerProfile = SellerProfile(
        id = getObject("id", UUID::class.java),
        userId = getObject("user_id", UUID::class.java),
        companyName = getString("company_name"),
        registrationNo = getString("registration_no"),
        ein = getString("ein"),
        state = getString("state"),
        status = SellerStatus.valueOf(getString("status")),
        verifiedAt = getTimestamp("verified_at")?.toInstant(),
        createdAt = getTimestamp("created_at").toInstant()
    )
}

/**
 * Row type for monthly settlement aggregation query results.
 */
data class MonthlySettlementRow(
    val month: String,
    val totalNet: BigDecimal,
    val totalHammer: BigDecimal,
    val totalCommission: BigDecimal,
    val settlementCount: Int
)
