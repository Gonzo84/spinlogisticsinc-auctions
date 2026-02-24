package eu.auctionplatform.broker.infrastructure.persistence.repository

import eu.auctionplatform.broker.domain.model.Lead
import eu.auctionplatform.broker.domain.model.LeadStatus
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Repository for [Lead] persistence operations using direct JDBC.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 */
@ApplicationScoped
class LeadRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    private val logger = LoggerFactory.getLogger(LeadRepository::class.java)

    companion object {
        private const val SELECT_COLUMNS = """
            id, seller_id, broker_id, company_name, contact_name,
            contact_email, contact_phone, status, notes,
            scheduled_visit_date, created_at, updated_at
        """

        private const val SELECT_BY_BROKER_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.leads
             WHERE broker_id = ?
             ORDER BY created_at DESC
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.leads
             WHERE id = ?
        """

        private const val INSERT_LEAD = """
            INSERT INTO app.leads
                (id, seller_id, broker_id, company_name, contact_name,
                 contact_email, contact_phone, status, notes,
                 scheduled_visit_date, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val UPDATE_STATUS = """
            UPDATE app.leads
               SET status = ?, scheduled_visit_date = ?, updated_at = ?
             WHERE id = ?
        """
    }

    /**
     * Returns all leads assigned to the given broker, ordered by creation time descending.
     *
     * @param brokerId The broker's user identifier.
     * @return List of leads belonging to the broker.
     */
    fun findByBrokerId(brokerId: UUID): List<Lead> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_BROKER_ID).use { stmt ->
                stmt.setObject(1, brokerId)
                stmt.executeQuery().use { rs ->
                    return rs.toLeadList()
                }
            }
        }
    }

    /**
     * Returns a single lead by its identifier, or null if not found.
     *
     * @param id The lead identifier.
     * @return The lead, or null.
     */
    fun findById(id: UUID): Lead? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toLead() else null
                }
            }
        }
    }

    /**
     * Inserts a new lead into the database.
     *
     * @param lead The lead domain model to persist.
     */
    fun insert(lead: Lead) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT_LEAD).use { stmt ->
                stmt.setObject(1, lead.id)
                stmt.setObject(2, lead.sellerId)
                stmt.setObject(3, lead.brokerId)
                stmt.setString(4, lead.companyName)
                stmt.setString(5, lead.contactName)
                stmt.setString(6, lead.contactEmail)
                stmt.setString(7, lead.contactPhone)
                stmt.setString(8, lead.status.name)
                stmt.setString(9, lead.notes)
                stmt.setTimestamp(10, lead.scheduledVisitDate?.let { Timestamp.from(it) })
                stmt.setTimestamp(11, Timestamp.from(lead.createdAt))
                stmt.setTimestamp(12, Timestamp.from(lead.updatedAt))
                stmt.executeUpdate()
            }
        }
        logger.debug("Inserted lead: id={}, broker={}", lead.id, lead.brokerId)
    }

    /**
     * Updates the status of a lead and optionally sets the scheduled visit date.
     *
     * @param id                 The lead identifier.
     * @param status             The new status.
     * @param scheduledVisitDate The scheduled visit date (nullable).
     * @param updatedAt          The update timestamp.
     */
    fun updateStatus(id: UUID, status: LeadStatus, scheduledVisitDate: Instant?, updatedAt: Instant) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                stmt.setTimestamp(2, scheduledVisitDate?.let { Timestamp.from(it) })
                stmt.setTimestamp(3, Timestamp.from(updatedAt))
                stmt.setObject(4, id)
                val rowsUpdated = stmt.executeUpdate()
                if (rowsUpdated == 0) {
                    logger.warn("No lead found to update: id={}", id)
                }
            }
        }
        logger.debug("Updated lead status: id={}, status={}", id, status)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toLeadList(): List<Lead> {
        val leads = mutableListOf<Lead>()
        while (next()) {
            leads.add(toLead())
        }
        return leads
    }

    private fun ResultSet.toLead(): Lead = Lead(
        id = getObject("id", UUID::class.java),
        sellerId = getObject("seller_id", UUID::class.java),
        brokerId = getObject("broker_id", UUID::class.java),
        companyName = getString("company_name"),
        contactName = getString("contact_name"),
        contactEmail = getString("contact_email"),
        contactPhone = getString("contact_phone"),
        status = LeadStatus.valueOf(getString("status")),
        notes = getString("notes"),
        scheduledVisitDate = getTimestamp("scheduled_visit_date")?.toInstant(),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant()
    )
}
