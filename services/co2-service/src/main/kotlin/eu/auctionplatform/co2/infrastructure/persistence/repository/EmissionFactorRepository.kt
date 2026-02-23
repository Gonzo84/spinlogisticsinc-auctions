package eu.auctionplatform.co2.infrastructure.persistence.repository

import eu.auctionplatform.co2.domain.model.EmissionFactor
import io.agroal.api.AgroalDataSource
import io.quarkus.agroal.DataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Repository for [EmissionFactor] persistence operations using direct JDBC.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 */
@ApplicationScoped
class EmissionFactorRepository @Inject constructor(
    @DataSource("system")
    private val dataSource: AgroalDataSource
) {

    private val logger = LoggerFactory.getLogger(EmissionFactorRepository::class.java)

    companion object {
        private const val SELECT_COLUMNS = """
            id, category_id, product_type, new_manufacturing_co2_kg,
            reuse_factor, source, last_updated
        """

        private const val SELECT_BY_CATEGORY_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.emission_factors
             WHERE category_id = ?
        """

        private const val SELECT_ALL = """
            SELECT $SELECT_COLUMNS
              FROM app.emission_factors
             ORDER BY product_type ASC
        """

        private const val INSERT_FACTOR = """
            INSERT INTO app.emission_factors
                (id, category_id, product_type, new_manufacturing_co2_kg,
                 reuse_factor, source, last_updated)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """

        private const val UPDATE_FACTOR = """
            UPDATE app.emission_factors
               SET product_type = ?,
                   new_manufacturing_co2_kg = ?,
                   reuse_factor = ?,
                   source = ?,
                   last_updated = ?
             WHERE id = ?
        """
    }

    /**
     * Returns the emission factor for the given category, or null if not found.
     *
     * @param categoryId The catalog category identifier.
     * @return The emission factor, or null.
     */
    fun findByCategoryId(categoryId: UUID): EmissionFactor? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_CATEGORY_ID).use { stmt ->
                stmt.setObject(1, categoryId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toEmissionFactor() else null
                }
            }
        }
    }

    /**
     * Returns all emission factors, ordered by product type.
     *
     * @return List of all emission factors.
     */
    fun findAll(): List<EmissionFactor> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_ALL).use { stmt ->
                stmt.executeQuery().use { rs ->
                    return rs.toEmissionFactorList()
                }
            }
        }
    }

    /**
     * Inserts a new emission factor.
     *
     * @param factor The emission factor to persist.
     */
    fun insert(factor: EmissionFactor) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT_FACTOR).use { stmt ->
                stmt.setObject(1, factor.id)
                stmt.setObject(2, factor.categoryId)
                stmt.setString(3, factor.productType)
                stmt.setBigDecimal(4, factor.newManufacturingCo2Kg)
                stmt.setBigDecimal(5, factor.reuseFactor)
                stmt.setString(6, factor.source)
                stmt.setTimestamp(7, Timestamp.from(factor.lastUpdated))
                stmt.executeUpdate()
            }
        }
        logger.debug("Inserted emission factor: id={}, type={}", factor.id, factor.productType)
    }

    /**
     * Updates an existing emission factor.
     *
     * @param factor The emission factor with updated values.
     */
    fun update(factor: EmissionFactor) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_FACTOR).use { stmt ->
                stmt.setString(1, factor.productType)
                stmt.setBigDecimal(2, factor.newManufacturingCo2Kg)
                stmt.setBigDecimal(3, factor.reuseFactor)
                stmt.setString(4, factor.source)
                stmt.setTimestamp(5, Timestamp.from(factor.lastUpdated))
                stmt.setObject(6, factor.id)
                val rowsUpdated = stmt.executeUpdate()
                if (rowsUpdated == 0) {
                    logger.warn("No emission factor found to update: id={}", factor.id)
                }
            }
        }
        logger.debug("Updated emission factor: id={}, type={}", factor.id, factor.productType)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toEmissionFactorList(): List<EmissionFactor> {
        val factors = mutableListOf<EmissionFactor>()
        while (next()) {
            factors.add(toEmissionFactor())
        }
        return factors
    }

    private fun ResultSet.toEmissionFactor(): EmissionFactor = EmissionFactor(
        id = getObject("id", UUID::class.java),
        categoryId = getObject("category_id", UUID::class.java),
        productType = getString("product_type"),
        newManufacturingCo2Kg = getBigDecimal("new_manufacturing_co2_kg"),
        reuseFactor = getBigDecimal("reuse_factor"),
        source = getString("source"),
        lastUpdated = getTimestamp("last_updated").toInstant()
    )
}
