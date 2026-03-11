package eu.auctionplatform.compliance.infrastructure.persistence.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import eu.auctionplatform.compliance.domain.model.FraudAlert
import eu.auctionplatform.compliance.domain.model.FraudAlertStatus
import eu.auctionplatform.compliance.domain.model.FraudAlertType
import eu.auctionplatform.compliance.domain.model.FraudSeverity
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * Repository for [FraudAlert] persistence operations using direct JDBC.
 *
 * Uses the default datasource via [AgroalDataSource] for full control
 * over SQL queries and connection management.
 */
@ApplicationScoped
class FraudAlertRepository @Inject constructor(
    private val dataSource: AgroalDataSource,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(FraudAlertRepository::class.java)

        private const val SELECT_COLUMNS = """
            id, type, severity, status, title, description, user_id, lot_id, auction_id,
            risk_score, evidence, resolution, resolved_by, resolved_at, detected_at,
            created_at, updated_at
        """

        private const val INSERT = """
            INSERT INTO app.fraud_alerts
                (id, type, severity, status, title, description, user_id, lot_id, auction_id,
                 risk_score, evidence, resolution, resolved_by, resolved_at, detected_at,
                 created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?)
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_COLUMNS FROM app.fraud_alerts WHERE id = ?
        """

        private const val UPDATE_STATUS = """
            UPDATE app.fraud_alerts
               SET status = ?, resolution = ?, resolved_by = ?, resolved_at = ?, updated_at = ?
             WHERE id = ?
        """
    }

    /**
     * Persists a new fraud alert.
     */
    fun save(alert: FraudAlert) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT).use { stmt ->
                stmt.setObject(1, alert.id)
                stmt.setString(2, alert.type.name)
                stmt.setString(3, alert.severity.name)
                stmt.setString(4, alert.status.name)
                stmt.setString(5, alert.title)
                stmt.setString(6, alert.description)
                stmt.setObject(7, alert.userId)
                stmt.setObject(8, alert.lotId)
                stmt.setObject(9, alert.auctionId)
                stmt.setDouble(10, alert.riskScore)
                stmt.setString(11, objectMapper.writeValueAsString(alert.evidence))
                stmt.setString(12, alert.resolution)
                stmt.setObject(13, alert.resolvedBy)
                stmt.setTimestamp(14, alert.resolvedAt?.let { Timestamp.from(it) })
                stmt.setTimestamp(15, Timestamp.from(alert.detectedAt))
                stmt.setTimestamp(16, Timestamp.from(alert.createdAt))
                stmt.setTimestamp(17, Timestamp.from(alert.updatedAt))
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Inserted fraud alert: id=%s, type=%s, severity=%s", alert.id, alert.type, alert.severity)
    }

    /**
     * Finds a fraud alert by its identifier.
     */
    fun findById(id: UUID): FraudAlert? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toModel() else null
                }
            }
        }
    }

    /**
     * Returns a paged list of fraud alerts with optional filters.
     *
     * @param severity Optional severity filter.
     * @param status   Optional status filter.
     * @param type     Optional type filter.
     * @param page     Page number (1-based).
     * @param pageSize Number of items per page.
     * @return List of matching fraud alerts ordered by detected_at descending.
     */
    fun findAll(
        severity: FraudSeverity?,
        status: FraudAlertStatus?,
        type: FraudAlertType?,
        page: Int,
        pageSize: Int
    ): List<FraudAlert> {
        val offset = (page - 1) * pageSize
        val (whereClause, params) = buildWhereClause(severity, status, type)

        val sql = """
            SELECT $SELECT_COLUMNS FROM app.fraud_alerts
            $whereClause
            ORDER BY detected_at DESC
            LIMIT ? OFFSET ?
        """

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                var idx = 1
                for (param in params) {
                    stmt.setString(idx++, param)
                }
                stmt.setInt(idx++, pageSize)
                stmt.setInt(idx, offset)
                stmt.executeQuery().use { rs ->
                    return rs.toModelList()
                }
            }
        }
    }

    /**
     * Returns the total count of fraud alerts matching the given filters.
     */
    fun count(
        severity: FraudSeverity?,
        status: FraudAlertStatus?,
        type: FraudAlertType?
    ): Long {
        val (whereClause, params) = buildWhereClause(severity, status, type)
        val sql = "SELECT COUNT(*) FROM app.fraud_alerts $whereClause"

        dataSource.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                var idx = 1
                for (param in params) {
                    stmt.setString(idx++, param)
                }
                stmt.executeQuery().use { rs ->
                    rs.next()
                    return rs.getLong(1)
                }
            }
        }
    }

    /**
     * Updates the status and resolution fields of a fraud alert.
     */
    fun updateStatus(
        id: UUID,
        status: FraudAlertStatus,
        resolution: String?,
        resolvedBy: UUID?,
        resolvedAt: java.time.Instant?
    ) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                stmt.setString(2, resolution)
                stmt.setObject(3, resolvedBy)
                stmt.setTimestamp(4, resolvedAt?.let { Timestamp.from(it) })
                stmt.setTimestamp(5, Timestamp.from(java.time.Instant.now()))
                stmt.setObject(6, id)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Updated fraud alert status: id=%s, status=%s", id, status)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun buildWhereClause(
        severity: FraudSeverity?,
        status: FraudAlertStatus?,
        type: FraudAlertType?
    ): Pair<String, List<String>> {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<String>()

        if (severity != null) {
            conditions.add("severity = ?")
            params.add(severity.name)
        }
        if (status != null) {
            conditions.add("status = ?")
            params.add(status.name)
        }
        if (type != null) {
            conditions.add("type = ?")
            params.add(type.name)
        }

        val whereClause = if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"
        return whereClause to params
    }

    private fun ResultSet.toModelList(): List<FraudAlert> {
        val models = mutableListOf<FraudAlert>()
        while (next()) {
            models.add(toModel())
        }
        return models
    }

    private fun ResultSet.toModel(): FraudAlert {
        val evidenceJson = getString("evidence") ?: "[]"
        val evidence: List<String> = objectMapper.readValue(evidenceJson)

        return FraudAlert(
            id = getObject("id", UUID::class.java),
            type = FraudAlertType.valueOf(getString("type")),
            severity = FraudSeverity.valueOf(getString("severity")),
            status = FraudAlertStatus.valueOf(getString("status")),
            title = getString("title"),
            description = getString("description"),
            userId = getObject("user_id", UUID::class.java),
            lotId = getObject("lot_id", UUID::class.java),
            auctionId = getObject("auction_id", UUID::class.java),
            riskScore = getDouble("risk_score"),
            evidence = evidence,
            resolution = getString("resolution"),
            resolvedBy = getObject("resolved_by", UUID::class.java),
            resolvedAt = getTimestamp("resolved_at")?.toInstant(),
            detectedAt = getTimestamp("detected_at").toInstant(),
            createdAt = getTimestamp("created_at").toInstant(),
            updatedAt = getTimestamp("updated_at").toInstant()
        )
    }
}
