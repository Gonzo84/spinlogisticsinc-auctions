package eu.auctionplatform.co2.infrastructure.persistence.repository

import eu.auctionplatform.co2.domain.model.Co2Calculation
import io.agroal.api.AgroalDataSource
import io.quarkus.agroal.DataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * Repository for [Co2Calculation] persistence operations using direct JDBC.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 */
@ApplicationScoped
class Co2CalculationRepository @Inject constructor(
    @DataSource("system")
    private val dataSource: AgroalDataSource
) {

    private val logger = LoggerFactory.getLogger(Co2CalculationRepository::class.java)

    companion object {
        private const val SELECT_COLUMNS = """
            id, lot_id, category_id, co2_avoided_kg, calculated_at, version
        """

        private const val SELECT_BY_LOT_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.co2_calculations
             WHERE lot_id = ?
             ORDER BY version DESC
             LIMIT 1
        """

        private const val INSERT_CALCULATION = """
            INSERT INTO app.co2_calculations
                (id, lot_id, category_id, co2_avoided_kg, calculated_at, version)
            VALUES (?, ?, ?, ?, ?, ?)
        """

        private const val GET_PLATFORM_TOTAL = """
            SELECT COALESCE(SUM(c.co2_avoided_kg), 0) AS total_co2_avoided,
                   COUNT(DISTINCT c.lot_id) AS total_lots
              FROM app.co2_calculations c
             INNER JOIN (
                 SELECT lot_id, MAX(version) AS max_version
                   FROM app.co2_calculations
                  GROUP BY lot_id
             ) latest ON c.lot_id = latest.lot_id AND c.version = latest.max_version
        """

        private const val GET_SELLER_TOTAL = """
            SELECT COALESCE(SUM(c.co2_avoided_kg), 0) AS total_co2_avoided,
                   COUNT(DISTINCT c.lot_id) AS total_lots
              FROM app.co2_calculations c
             INNER JOIN (
                 SELECT lot_id, MAX(version) AS max_version
                   FROM app.co2_calculations
                  GROUP BY lot_id
             ) latest ON c.lot_id = latest.lot_id AND c.version = latest.max_version
             WHERE c.lot_id IN (
                 SELECT id FROM app.co2_lot_seller_mapping WHERE seller_id = ?
             )
        """
    }

    /**
     * Returns the latest CO2 calculation for the given lot, or null if none exists.
     *
     * @param lotId The catalog lot identifier.
     * @return The latest calculation, or null.
     */
    fun findByLotId(lotId: UUID): Co2Calculation? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_LOT_ID).use { stmt ->
                stmt.setObject(1, lotId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toCalculation() else null
                }
            }
        }
    }

    /**
     * Inserts a new CO2 calculation.
     *
     * @param calculation The calculation to persist.
     */
    fun insert(calculation: Co2Calculation) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT_CALCULATION).use { stmt ->
                stmt.setObject(1, calculation.id)
                stmt.setObject(2, calculation.lotId)
                stmt.setObject(3, calculation.categoryId)
                stmt.setBigDecimal(4, calculation.co2AvoidedKg)
                stmt.setTimestamp(5, Timestamp.from(calculation.calculatedAt))
                stmt.setInt(6, calculation.version)
                stmt.executeUpdate()
            }
        }
        logger.debug("Inserted CO2 calculation: id={}, lot={}, co2={}kg",
            calculation.id, calculation.lotId, calculation.co2AvoidedKg)
    }

    /**
     * Returns the total CO2 avoided across the entire platform.
     *
     * Uses the latest version of each lot's calculation to avoid double counting.
     *
     * @return A pair of (totalCo2AvoidedKg, totalLots).
     */
    fun getPlatformTotal(): Pair<BigDecimal, Long> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(GET_PLATFORM_TOTAL).use { stmt ->
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        Pair(rs.getBigDecimal("total_co2_avoided"), rs.getLong("total_lots"))
                    } else {
                        Pair(BigDecimal.ZERO, 0L)
                    }
                }
            }
        }
    }

    /**
     * Returns the total CO2 avoided for a specific seller.
     *
     * Relies on the `co2_lot_seller_mapping` view/table to associate lots
     * with sellers. Uses the latest version of each lot's calculation.
     *
     * @param sellerId The seller's user identifier.
     * @return A pair of (totalCo2AvoidedKg, totalLots).
     */
    fun getSellerTotal(sellerId: UUID): Pair<BigDecimal, Long> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(GET_SELLER_TOTAL).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) {
                        Pair(rs.getBigDecimal("total_co2_avoided"), rs.getLong("total_lots"))
                    } else {
                        Pair(BigDecimal.ZERO, 0L)
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toCalculation(): Co2Calculation = Co2Calculation(
        id = getObject("id", UUID::class.java),
        lotId = getObject("lot_id", UUID::class.java),
        categoryId = getObject("category_id", UUID::class.java),
        co2AvoidedKg = getBigDecimal("co2_avoided_kg"),
        calculatedAt = getTimestamp("calculated_at").toInstant(),
        version = getInt("version")
    )
}
