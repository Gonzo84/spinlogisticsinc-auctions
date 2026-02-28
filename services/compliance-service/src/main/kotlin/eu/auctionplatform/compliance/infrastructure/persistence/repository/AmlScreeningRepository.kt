package eu.auctionplatform.compliance.infrastructure.persistence.repository

import eu.auctionplatform.compliance.domain.model.AmlScreening
import eu.auctionplatform.compliance.domain.model.AmlScreeningStatus
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * Repository for [AmlScreening] persistence operations using direct JDBC.
 *
 * Uses the named `system` datasource for full SQL control.
 */
@ApplicationScoped
class AmlScreeningRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(AmlScreeningRepository::class.java)
        private const val SELECT_COLUMNS = """
            id, user_id, provider, status, check_id, completed_at, risk_level
        """

        private const val INSERT = """
            INSERT INTO app.aml_screenings
                (id, user_id, provider, status, check_id, completed_at, risk_level)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_COLUMNS FROM app.aml_screenings WHERE id = ?
        """

        private const val SELECT_BY_USER_ID = """
            SELECT $SELECT_COLUMNS FROM app.aml_screenings
            WHERE user_id = ? ORDER BY completed_at DESC NULLS LAST
        """

        private const val SELECT_LATEST_BY_USER_ID = """
            SELECT $SELECT_COLUMNS FROM app.aml_screenings
            WHERE user_id = ? ORDER BY completed_at DESC NULLS LAST
            LIMIT 1
        """

        private const val UPDATE = """
            UPDATE app.aml_screenings
               SET status = ?, check_id = ?, completed_at = ?, risk_level = ?
             WHERE id = ?
        """
    }

    /**
     * Persists a new AML screening record.
     */
    fun insert(screening: AmlScreening) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT).use { stmt ->
                stmt.setObject(1, screening.id)
                stmt.setObject(2, screening.userId)
                stmt.setString(3, screening.provider)
                stmt.setString(4, screening.status.name)
                stmt.setString(5, screening.checkId)
                stmt.setTimestamp(6, screening.completedAt?.let { Timestamp.from(it) })
                stmt.setString(7, screening.riskLevel)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Inserted AML screening: id=%s, userId=%s", screening.id, screening.userId)
    }

    /**
     * Finds an AML screening by its identifier.
     */
    fun findById(id: UUID): AmlScreening? {
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
     * Finds all AML screenings for a given user.
     */
    fun findByUserId(userId: UUID): List<AmlScreening> {
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
     * Finds the most recent AML screening for a given user.
     */
    fun findLatestByUserId(userId: UUID): AmlScreening? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_LATEST_BY_USER_ID).use { stmt ->
                stmt.setObject(1, userId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toModel() else null
                }
            }
        }
    }

    /**
     * Updates the mutable fields of an AML screening.
     */
    fun update(screening: AmlScreening) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE).use { stmt ->
                stmt.setString(1, screening.status.name)
                stmt.setString(2, screening.checkId)
                stmt.setTimestamp(3, screening.completedAt?.let { Timestamp.from(it) })
                stmt.setString(4, screening.riskLevel)
                stmt.setObject(5, screening.id)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Updated AML screening: id=%s, status=%s", screening.id, screening.status)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toModelList(): List<AmlScreening> {
        val models = mutableListOf<AmlScreening>()
        while (next()) {
            models.add(toModel())
        }
        return models
    }

    private fun ResultSet.toModel(): AmlScreening = AmlScreening(
        id = getObject("id", UUID::class.java),
        userId = getObject("user_id", UUID::class.java),
        provider = getString("provider"),
        status = AmlScreeningStatus.valueOf(getString("status")),
        checkId = getString("check_id"),
        completedAt = getTimestamp("completed_at")?.toInstant(),
        riskLevel = getString("risk_level")
    )
}
