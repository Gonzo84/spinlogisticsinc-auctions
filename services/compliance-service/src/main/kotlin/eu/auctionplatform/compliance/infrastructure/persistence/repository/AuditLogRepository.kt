package eu.auctionplatform.compliance.infrastructure.persistence.repository

import eu.auctionplatform.compliance.domain.model.AuditLogEntry
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Append-only repository for the platform audit log.
 *
 * Supports insert and filtered query operations. Updates and deletes are
 * intentionally not provided -- the audit log is immutable by design for
 * regulatory compliance.
 */
@ApplicationScoped
class AuditLogRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    private val logger = LoggerFactory.getLogger(AuditLogRepository::class.java)

    companion object {
        private const val SELECT_COLUMNS = """
            id, timestamp, user_id, action, entity_type, entity_id,
            details, ip_address, source
        """

        private const val INSERT = """
            INSERT INTO app.audit_log
                (id, timestamp, user_id, action, entity_type, entity_id,
                 details, ip_address, source)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
    }

    /**
     * Appends a new entry to the audit log.
     */
    fun insert(entry: AuditLogEntry) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT).use { stmt ->
                stmt.setObject(1, entry.id)
                stmt.setTimestamp(2, Timestamp.from(entry.timestamp))
                stmt.setObject(3, entry.userId)
                stmt.setString(4, entry.action)
                stmt.setString(5, entry.entityType)
                stmt.setString(6, entry.entityId)
                stmt.setString(7, entry.details)
                stmt.setString(8, entry.ipAddress)
                stmt.setString(9, entry.source)
                stmt.executeUpdate()
            }
        }
        logger.trace("Audit log entry appended: action={}, entityType={}", entry.action, entry.entityType)
    }

    /**
     * Queries the audit log with optional filters and pagination.
     *
     * @param action     Optional action filter (exact match).
     * @param entityType Optional entity type filter (exact match).
     * @param userId     Optional user ID filter.
     * @param source     Optional source/service filter.
     * @param from       Optional lower bound for timestamp (inclusive).
     * @param to         Optional upper bound for timestamp (exclusive).
     * @param page       Page number (1-based).
     * @param size       Page size.
     * @return Pair of (items, total count).
     */
    fun query(
        action: String? = null,
        entityType: String? = null,
        userId: UUID? = null,
        source: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        page: Int = 1,
        size: Int = 50
    ): Pair<List<AuditLogEntry>, Long> {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (!action.isNullOrBlank()) {
            conditions.add("action = ?")
            params.add(action)
        }
        if (!entityType.isNullOrBlank()) {
            conditions.add("entity_type = ?")
            params.add(entityType)
        }
        if (userId != null) {
            conditions.add("user_id = ?")
            params.add(userId)
        }
        if (!source.isNullOrBlank()) {
            conditions.add("source = ?")
            params.add(source)
        }
        if (from != null) {
            conditions.add("timestamp >= ?")
            params.add(Timestamp.from(from))
        }
        if (to != null) {
            conditions.add("timestamp < ?")
            params.add(Timestamp.from(to))
        }

        val whereClause = if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"
        val offset = (page - 1) * size

        val countSql = "SELECT COUNT(*) FROM app.audit_log $whereClause"
        val querySql = """
            SELECT $SELECT_COLUMNS FROM app.audit_log
            $whereClause
            ORDER BY timestamp DESC
            LIMIT ? OFFSET ?
        """

        val total: Long
        val items: List<AuditLogEntry>

        dataSource.connection.use { conn ->
            // Count
            conn.prepareStatement(countSql).use { stmt ->
                params.forEachIndexed { index, param -> setParam(stmt, index + 1, param) }
                stmt.executeQuery().use { rs ->
                    rs.next()
                    total = rs.getLong(1)
                }
            }

            // Query
            conn.prepareStatement(querySql).use { stmt ->
                params.forEachIndexed { index, param -> setParam(stmt, index + 1, param) }
                stmt.setInt(params.size + 1, size)
                stmt.setInt(params.size + 2, offset)
                stmt.executeQuery().use { rs ->
                    items = rs.toModelList()
                }
            }
        }

        return Pair(items, total)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun setParam(stmt: java.sql.PreparedStatement, index: Int, param: Any) {
        when (param) {
            is String -> stmt.setString(index, param)
            is UUID -> stmt.setObject(index, param)
            is Timestamp -> stmt.setTimestamp(index, param)
            else -> stmt.setObject(index, param)
        }
    }

    private fun ResultSet.toModelList(): List<AuditLogEntry> {
        val models = mutableListOf<AuditLogEntry>()
        while (next()) {
            models.add(toModel())
        }
        return models
    }

    private fun ResultSet.toModel(): AuditLogEntry = AuditLogEntry(
        id = getObject("id", UUID::class.java),
        timestamp = getTimestamp("timestamp").toInstant(),
        userId = getObject("user_id", UUID::class.java),
        action = getString("action"),
        entityType = getString("entity_type"),
        entityId = getString("entity_id"),
        details = getString("details"),
        ipAddress = getString("ip_address"),
        source = getString("source")
    )
}
