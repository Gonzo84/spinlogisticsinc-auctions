package eu.auctionplatform.seller.infrastructure.persistence.repository

import eu.auctionplatform.seller.domain.model.SellerDashboard
import eu.auctionplatform.seller.domain.model.SellerProfile
import eu.auctionplatform.seller.domain.model.SellerStatus
import io.agroal.api.AgroalDataSource
import io.quarkus.agroal.DataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Direct JDBC repository for [SellerProfile] persistence and dashboard metrics.
 *
 * Uses the named "system" datasource via Agroal, following the platform's
 * repository pattern of direct SQL control without ORM overhead.
 */
@ApplicationScoped
class SellerProfileRepository @Inject constructor(
    @DataSource("system")
    private val dataSource: AgroalDataSource
) {

    private val logger = LoggerFactory.getLogger(SellerProfileRepository::class.java)

    companion object {
        private const val SELECT_PROFILE_COLUMNS = """
            id, user_id, company_name, registration_no, vat_id,
            country, status, verified_at, created_at
        """

        private const val INSERT_PROFILE = """
            INSERT INTO app.seller_profiles
                (id, user_id, company_name, registration_no, vat_id,
                 country, status, verified_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_PROFILE_COLUMNS FROM app.seller_profiles WHERE id = ?
        """

        private const val SELECT_BY_USER_ID = """
            SELECT $SELECT_PROFILE_COLUMNS FROM app.seller_profiles WHERE user_id = ?
        """

        private const val SELECT_BY_VAT_ID = """
            SELECT $SELECT_PROFILE_COLUMNS FROM app.seller_profiles WHERE vat_id = ?
        """

        private const val UPDATE_STATUS = """
            UPDATE app.seller_profiles SET status = ?, verified_at = ? WHERE id = ?
        """

        private const val UPDATE_PROFILE = """
            UPDATE app.seller_profiles
               SET company_name = ?, registration_no = ?, vat_id = ?, country = ?
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
                (seller_id, active_lots, total_bids, total_hammer_sales,
                 pending_settlements, total_settled, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, NOW())
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
                stmt.setString(5, profile.vatId)
                stmt.setString(6, profile.country)
                stmt.setString(7, profile.status.name)
                stmt.setTimestamp(8, profile.verifiedAt?.let { Timestamp.from(it) })
                stmt.setTimestamp(9, Timestamp.from(profile.createdAt))
                stmt.executeUpdate()
            }
        }
        logger.debug("Saved seller profile {} for user {}", profile.id, profile.userId)
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
     * Finds a seller profile by VAT ID.
     */
    fun findByVatId(vatId: String): SellerProfile? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_VAT_ID).use { stmt ->
                stmt.setString(1, vatId)
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
                stmt.setString(3, profile.vatId)
                stmt.setString(4, profile.country)
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
     */
    fun settlePayment(sellerId: UUID, amount: BigDecimal) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SETTLE_PAYMENT).use { stmt ->
                stmt.setBigDecimal(1, amount)
                stmt.setObject(2, sellerId)
                stmt.executeUpdate()
            }
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toSellerProfile(): SellerProfile = SellerProfile(
        id = getObject("id", UUID::class.java),
        userId = getObject("user_id", UUID::class.java),
        companyName = getString("company_name"),
        registrationNo = getString("registration_no"),
        vatId = getString("vat_id"),
        country = getString("country"),
        status = SellerStatus.valueOf(getString("status")),
        verifiedAt = getTimestamp("verified_at")?.toInstant(),
        createdAt = getTimestamp("created_at").toInstant()
    )
}
