package eu.auctionplatform.compliance.infrastructure.persistence.repository

import eu.auctionplatform.compliance.domain.model.GdprRequest
import eu.auctionplatform.compliance.domain.model.GdprRequestStatus
import eu.auctionplatform.compliance.domain.model.GdprRequestType
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * Repository for [GdprRequest] persistence operations using direct JDBC.
 *
 * Uses the named `system` datasource via [AgroalDataSource] for full control
 * over SQL queries and connection management.
 */
@ApplicationScoped
class GdprRequestRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    private val logger = LoggerFactory.getLogger(GdprRequestRepository::class.java)

    companion object {
        private const val SELECT_COLUMNS = """
            id, user_id, type, status, reason, requested_at, completed_at, rejection_reason
        """

        private const val INSERT = """
            INSERT INTO app.gdpr_requests
                (id, user_id, type, status, reason, requested_at, completed_at, rejection_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_COLUMNS FROM app.gdpr_requests WHERE id = ?
        """

        private const val SELECT_BY_USER_ID = """
            SELECT $SELECT_COLUMNS FROM app.gdpr_requests
            WHERE user_id = ? ORDER BY requested_at DESC
        """

        private const val SELECT_BY_STATUS = """
            SELECT $SELECT_COLUMNS FROM app.gdpr_requests
            WHERE status = ? ORDER BY requested_at ASC
            LIMIT ? OFFSET ?
        """

        private const val COUNT_BY_STATUS = """
            SELECT COUNT(*) FROM app.gdpr_requests WHERE status = ?
        """

        private const val SELECT_ALL_PAGED = """
            SELECT $SELECT_COLUMNS FROM app.gdpr_requests
            ORDER BY requested_at DESC
            LIMIT ? OFFSET ?
        """

        private const val COUNT_ALL = """
            SELECT COUNT(*) FROM app.gdpr_requests
        """

        private const val UPDATE_STATUS = """
            UPDATE app.gdpr_requests
               SET status = ?, completed_at = ?, rejection_reason = ?
             WHERE id = ?
        """
    }

    /**
     * Persists a new GDPR request.
     */
    fun insert(request: GdprRequest) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT).use { stmt ->
                stmt.setObject(1, request.id)
                stmt.setObject(2, request.userId)
                stmt.setString(3, request.type.name)
                stmt.setString(4, request.status.name)
                stmt.setString(5, request.reason)
                stmt.setTimestamp(6, Timestamp.from(request.requestedAt))
                stmt.setTimestamp(7, request.completedAt?.let { Timestamp.from(it) })
                stmt.setString(8, request.rejectionReason)
                stmt.executeUpdate()
            }
        }
        logger.debug("Inserted GDPR request: id={}, type={}", request.id, request.type)
    }

    /**
     * Finds a GDPR request by its identifier.
     */
    fun findById(id: UUID): GdprRequest? {
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
     * Finds all GDPR requests for a given user, ordered by most recent first.
     */
    fun findByUserId(userId: UUID): List<GdprRequest> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_USER_ID).use { stmt ->
                stmt.setObject(1, userId)
                stmt.executeQuery().use { rs ->
                    return rs.toModelList()
                }
            }
        }
    }

    /**
     * Returns a paged list of GDPR requests filtered by status.
     */
    fun findByStatus(status: GdprRequestStatus, page: Int, size: Int): List<GdprRequest> {
        val offset = (page - 1) * size
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                stmt.setInt(2, size)
                stmt.setInt(3, offset)
                stmt.executeQuery().use { rs ->
                    return rs.toModelList()
                }
            }
        }
    }

    /**
     * Returns the total count of GDPR requests with the given status.
     */
    fun countByStatus(status: GdprRequestStatus): Long {
        dataSource.connection.use { conn ->
            conn.prepareStatement(COUNT_BY_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                stmt.executeQuery().use { rs ->
                    rs.next()
                    return rs.getLong(1)
                }
            }
        }
    }

    /**
     * Returns a paged list of all GDPR requests ordered by most recent first.
     */
    fun findAllPaged(page: Int, size: Int): List<GdprRequest> {
        val offset = (page - 1) * size
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_ALL_PAGED).use { stmt ->
                stmt.setInt(1, size)
                stmt.setInt(2, offset)
                stmt.executeQuery().use { rs ->
                    return rs.toModelList()
                }
            }
        }
    }

    /**
     * Returns the total count of all GDPR requests.
     */
    fun countAll(): Long {
        dataSource.connection.use { conn ->
            conn.prepareStatement(COUNT_ALL).use { stmt ->
                stmt.executeQuery().use { rs ->
                    rs.next()
                    return rs.getLong(1)
                }
            }
        }
    }

    /**
     * Updates the status, completion timestamp, and rejection reason of a GDPR request.
     */
    fun updateStatus(id: UUID, status: GdprRequestStatus, completedAt: java.time.Instant?, rejectionReason: String?) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                stmt.setTimestamp(2, completedAt?.let { Timestamp.from(it) })
                stmt.setString(3, rejectionReason)
                stmt.setObject(4, id)
                stmt.executeUpdate()
            }
        }
        logger.debug("Updated GDPR request status: id={}, status={}", id, status)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toModelList(): List<GdprRequest> {
        val models = mutableListOf<GdprRequest>()
        while (next()) {
            models.add(toModel())
        }
        return models
    }

    private fun ResultSet.toModel(): GdprRequest = GdprRequest(
        id = getObject("id", UUID::class.java),
        userId = getObject("user_id", UUID::class.java),
        type = GdprRequestType.valueOf(getString("type")),
        status = GdprRequestStatus.valueOf(getString("status")),
        reason = getString("reason"),
        requestedAt = getTimestamp("requested_at").toInstant(),
        completedAt = getTimestamp("completed_at")?.toInstant(),
        rejectionReason = getString("rejection_reason")
    )
}
