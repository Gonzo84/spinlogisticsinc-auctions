package eu.auctionplatform.broker.infrastructure.persistence.repository

import eu.auctionplatform.broker.domain.model.IntakeStatus
import eu.auctionplatform.broker.domain.model.LotIntake
import eu.auctionplatform.commons.util.JsonMapper
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.util.UUID

/**
 * Repository for [LotIntake] persistence operations using direct JDBC.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 */
@ApplicationScoped
class LotIntakeRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    private val logger = LoggerFactory.getLogger(LotIntakeRepository::class.java)

    companion object {
        private const val SELECT_COLUMNS = """
            id, broker_id, seller_id, lead_id, title, category_id,
            description, specifications, reserve_price, location_address,
            location_country, location_lat, location_lng, image_keys,
            status, created_at
        """

        private const val SELECT_BY_BROKER_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.lot_intakes
             WHERE broker_id = ?
             ORDER BY created_at DESC
        """

        private const val SELECT_BY_LEAD_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.lot_intakes
             WHERE lead_id = ?
             ORDER BY created_at DESC
        """

        private const val INSERT_INTAKE = """
            INSERT INTO app.lot_intakes
                (id, broker_id, seller_id, lead_id, title, category_id,
                 description, specifications, reserve_price, location_address,
                 location_country, location_lat, location_lng, image_keys,
                 status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
        """
    }

    /**
     * Returns all lot intakes for the given broker, ordered by creation time descending.
     *
     * @param brokerId The broker's user identifier.
     * @return List of lot intakes belonging to the broker.
     */
    fun findByBrokerId(brokerId: UUID): List<LotIntake> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_BROKER_ID).use { stmt ->
                stmt.setObject(1, brokerId)
                stmt.executeQuery().use { rs ->
                    return rs.toIntakeList()
                }
            }
        }
    }

    /**
     * Returns all lot intakes associated with a given lead, ordered by creation time descending.
     *
     * @param leadId The lead identifier.
     * @return List of lot intakes for the lead.
     */
    fun findByLeadId(leadId: UUID): List<LotIntake> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_LEAD_ID).use { stmt ->
                stmt.setObject(1, leadId)
                stmt.executeQuery().use { rs ->
                    return rs.toIntakeList()
                }
            }
        }
    }

    /**
     * Inserts a single lot intake into the database.
     *
     * @param intake The lot intake domain model to persist.
     */
    fun insert(intake: LotIntake) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT_INTAKE).use { stmt ->
                setIntakeParams(stmt, intake)
                stmt.executeUpdate()
            }
        }
        logger.debug("Inserted lot intake: id={}, broker={}", intake.id, intake.brokerId)
    }

    /**
     * Inserts multiple lot intakes in a single batch operation.
     *
     * All intakes are inserted within a single transaction. If any insertion
     * fails, the entire batch is rolled back.
     *
     * @param intakes The list of lot intakes to persist.
     */
    fun bulkInsert(intakes: List<LotIntake>) {
        if (intakes.isEmpty()) return

        dataSource.connection.use { conn ->
            val originalAutoCommit = conn.autoCommit
            conn.autoCommit = false
            try {
                conn.prepareStatement(INSERT_INTAKE).use { stmt ->
                    for (intake in intakes) {
                        setIntakeParams(stmt, intake)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
                conn.commit()
                logger.info("Bulk inserted {} lot intakes", intakes.size)
            } catch (ex: Exception) {
                conn.rollback()
                logger.error("Failed to bulk insert lot intakes: {}", ex.message, ex)
                throw ex
            } finally {
                conn.autoCommit = originalAutoCommit
            }
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun setIntakeParams(stmt: java.sql.PreparedStatement, intake: LotIntake) {
        stmt.setObject(1, intake.id)
        stmt.setObject(2, intake.brokerId)
        stmt.setObject(3, intake.sellerId)
        if (intake.leadId != null) {
            stmt.setObject(4, intake.leadId)
        } else {
            stmt.setNull(4, Types.OTHER)
        }
        stmt.setString(5, intake.title)
        stmt.setObject(6, intake.categoryId)
        stmt.setString(7, intake.description)
        stmt.setString(8, intake.specifications?.let { JsonMapper.toJson(it) })
        if (intake.reservePrice != null) {
            stmt.setBigDecimal(9, intake.reservePrice)
        } else {
            stmt.setNull(9, Types.DECIMAL)
        }
        stmt.setString(10, intake.locationAddress)
        stmt.setString(11, intake.locationCountry)
        if (intake.locationLat != null) {
            stmt.setDouble(12, intake.locationLat)
        } else {
            stmt.setNull(12, Types.DOUBLE)
        }
        if (intake.locationLng != null) {
            stmt.setDouble(13, intake.locationLng)
        } else {
            stmt.setNull(13, Types.DOUBLE)
        }
        stmt.setString(14, JsonMapper.toJson(intake.imageKeys))
        stmt.setString(15, intake.status.name)
        stmt.setTimestamp(16, Timestamp.from(intake.createdAt))
    }

    private fun ResultSet.toIntakeList(): List<LotIntake> {
        val intakes = mutableListOf<LotIntake>()
        while (next()) {
            intakes.add(toIntake())
        }
        return intakes
    }

    @Suppress("UNCHECKED_CAST")
    private fun ResultSet.toIntake(): LotIntake {
        val specsJson = getString("specifications")
        val imageKeysJson = getString("image_keys")

        return LotIntake(
            id = getObject("id", UUID::class.java),
            brokerId = getObject("broker_id", UUID::class.java),
            sellerId = getObject("seller_id", UUID::class.java),
            leadId = getObject("lead_id")?.let { it as? UUID ?: UUID.fromString(it.toString()) },
            title = getString("title"),
            categoryId = getObject("category_id", UUID::class.java),
            description = getString("description"),
            specifications = specsJson?.let {
                JsonMapper.instance.readValue(it, Map::class.java) as? Map<String, Any>
            },
            reservePrice = getBigDecimal("reserve_price"),
            locationAddress = getString("location_address") ?: "",
            locationCountry = getString("location_country") ?: "",
            locationLat = getObject("location_lat") as? Double,
            locationLng = getObject("location_lng") as? Double,
            imageKeys = imageKeysJson?.let {
                JsonMapper.instance.readValue(it, List::class.java) as? List<String>
            } ?: emptyList(),
            status = IntakeStatus.valueOf(getString("status")),
            createdAt = getTimestamp("created_at").toInstant()
        )
    }
}
