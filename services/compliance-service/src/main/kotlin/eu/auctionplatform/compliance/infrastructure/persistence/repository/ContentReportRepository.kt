package eu.auctionplatform.compliance.infrastructure.persistence.repository

import eu.auctionplatform.compliance.domain.model.ContentReport
import eu.auctionplatform.compliance.domain.model.ContentReportStatus
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * Repository for [ContentReport] persistence operations using direct JDBC.
 *
 * Supports DSA transparency reporting and moderation workflows.
 */
@ApplicationScoped
class ContentReportRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(ContentReportRepository::class.java)
        private const val SELECT_COLUMNS = """
            id, reporter_id, lot_id, reason, status, created_at, resolved_at
        """

        private const val INSERT = """
            INSERT INTO app.content_reports
                (id, reporter_id, lot_id, reason, status, created_at, resolved_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_COLUMNS FROM app.content_reports WHERE id = ?
        """

        private const val SELECT_BY_STATUS_PAGED = """
            SELECT $SELECT_COLUMNS FROM app.content_reports
            WHERE status = ? ORDER BY created_at DESC
            LIMIT ? OFFSET ?
        """

        private const val COUNT_BY_STATUS = """
            SELECT COUNT(*) FROM app.content_reports WHERE status = ?
        """

        private const val SELECT_ALL_PAGED = """
            SELECT $SELECT_COLUMNS FROM app.content_reports
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
        """

        private const val COUNT_ALL = """
            SELECT COUNT(*) FROM app.content_reports
        """

        private const val UPDATE_STATUS = """
            UPDATE app.content_reports
               SET status = ?, resolved_at = ?
             WHERE id = ?
        """

        private const val COUNT_BY_STATUS_IN_RANGE = """
            SELECT status, COUNT(*) as cnt FROM app.content_reports
            WHERE created_at >= ? AND created_at < ?
            GROUP BY status
        """

        private const val COUNT_IN_RANGE = """
            SELECT COUNT(*) FROM app.content_reports
            WHERE created_at >= ? AND created_at < ?
        """
    }

    /**
     * Persists a new content report.
     */
    fun insert(report: ContentReport) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT).use { stmt ->
                stmt.setObject(1, report.id)
                stmt.setObject(2, report.reporterId)
                stmt.setObject(3, report.lotId)
                stmt.setString(4, report.reason)
                stmt.setString(5, report.status.name)
                stmt.setTimestamp(6, Timestamp.from(report.createdAt))
                stmt.setTimestamp(7, report.resolvedAt?.let { Timestamp.from(it) })
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Inserted content report: id=%s, lotId=%s", report.id, report.lotId)
    }

    /**
     * Finds a content report by its identifier.
     */
    fun findById(id: UUID): ContentReport? {
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
     * Returns a paged list of content reports filtered by status.
     */
    fun findByStatus(status: ContentReportStatus, page: Int, size: Int): List<ContentReport> {
        val offset = (page - 1) * size
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_STATUS_PAGED).use { stmt ->
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
     * Returns the total count of content reports with the given status.
     */
    fun countByStatus(status: ContentReportStatus): Long {
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
     * Returns a paged list of all content reports.
     */
    fun findAllPaged(page: Int, size: Int): List<ContentReport> {
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
     * Returns the total count of all content reports.
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
     * Updates the status and resolved timestamp of a content report.
     */
    fun updateStatus(id: UUID, status: ContentReportStatus, resolvedAt: java.time.Instant?) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                stmt.setTimestamp(2, resolvedAt?.let { Timestamp.from(it) })
                stmt.setObject(3, id)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Updated content report status: id=%s, status=%s", id, status)
    }

    /**
     * Returns a breakdown of report counts by status within a time range.
     * Used for DSA transparency reports.
     */
    fun countByStatusInRange(from: java.time.Instant, to: java.time.Instant): Map<String, Long> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(COUNT_BY_STATUS_IN_RANGE).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(from))
                stmt.setTimestamp(2, Timestamp.from(to))
                stmt.executeQuery().use { rs ->
                    val result = mutableMapOf<String, Long>()
                    while (rs.next()) {
                        result[rs.getString("status")] = rs.getLong("cnt")
                    }
                    return result
                }
            }
        }
    }

    /**
     * Returns the total number of reports within a time range.
     */
    fun countInRange(from: java.time.Instant, to: java.time.Instant): Long {
        dataSource.connection.use { conn ->
            conn.prepareStatement(COUNT_IN_RANGE).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(from))
                stmt.setTimestamp(2, Timestamp.from(to))
                stmt.executeQuery().use { rs ->
                    rs.next()
                    return rs.getLong(1)
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toModelList(): List<ContentReport> {
        val models = mutableListOf<ContentReport>()
        while (next()) {
            models.add(toModel())
        }
        return models
    }

    private fun ResultSet.toModel(): ContentReport = ContentReport(
        id = getObject("id", UUID::class.java),
        reporterId = getObject("reporter_id", UUID::class.java),
        lotId = getObject("lot_id", UUID::class.java),
        reason = getString("reason"),
        status = ContentReportStatus.valueOf(getString("status")),
        createdAt = getTimestamp("created_at").toInstant(),
        resolvedAt = getTimestamp("resolved_at")?.toInstant()
    )
}
