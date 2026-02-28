package eu.auctionplatform.auction.infrastructure.persistence.repository

import eu.auctionplatform.auction.infrastructure.persistence.entity.AuctionEventEntity
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Event-store repository for auction aggregate events.
 *
 * Uses direct JDBC via an [AgroalDataSource] (the "SandBox" pattern) rather
 * than Panache, giving full control over SQL and optimistic-concurrency
 * conflict detection during event appends.
 */
@ApplicationScoped
class AuctionEventRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(AuctionEventRepository::class.java)
        private const val SELECT_COLUMNS = """
            event_id, aggregate_id, aggregate_type, event_type,
            event_data, version, created_at, brand, metadata
        """

        private const val SELECT_BY_AGGREGATE_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.auction_events
             WHERE aggregate_id = ?
             ORDER BY version ASC
        """

        private const val SELECT_AFTER_VERSION = """
            SELECT $SELECT_COLUMNS
              FROM app.auction_events
             WHERE aggregate_id = ?
               AND version > ?
             ORDER BY version ASC
        """

        private const val CHECK_VERSION_CONFLICT = """
            SELECT COUNT(*) FROM app.auction_events
             WHERE aggregate_id = ? AND version >= ?
        """

        private const val INSERT_EVENT = """
            INSERT INTO app.auction_events
                (event_id, aggregate_id, aggregate_type, event_type,
                 event_data, version, created_at, brand, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
    }

    /**
     * Returns all events for the given aggregate, ordered by version ascending.
     *
     * @param aggregateId The auction aggregate identifier.
     * @return Ordered list of events; empty if the aggregate does not exist.
     */
    fun findByAggregateId(aggregateId: UUID): List<AuctionEventEntity> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_AGGREGATE_ID).use { stmt ->
                stmt.setObject(1, aggregateId)
                stmt.executeQuery().use { rs ->
                    return rs.toEntityList()
                }
            }
        }
    }

    /**
     * Returns events for the given aggregate with version strictly greater
     * than [version], ordered by version ascending.
     *
     * Used for incremental rehydration after loading from a snapshot.
     *
     * @param aggregateId The auction aggregate identifier.
     * @param version The snapshot version to load events after.
     * @return Ordered list of events after the specified version.
     */
    fun findByAggregateIdAfterVersion(aggregateId: UUID, version: Long): List<AuctionEventEntity> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_AFTER_VERSION).use { stmt ->
                stmt.setObject(1, aggregateId)
                stmt.setLong(2, version)
                stmt.executeQuery().use { rs ->
                    return rs.toEntityList()
                }
            }
        }
    }

    /**
     * Atomically appends a batch of events to the event store with optimistic
     * concurrency control.
     *
     * Before inserting, the method checks that no events exist with a version
     * greater than or equal to [expectedVersion]. If a conflict is detected
     * (another writer has appended events concurrently), the method returns
     * `false` without modifying the store, allowing the caller to retry with
     * a fresh aggregate state.
     *
     * @param aggregateId The auction aggregate identifier.
     * @param events The events to append (must have sequential versions).
     * @param expectedVersion The version the caller expects the aggregate to
     *        be at before appending. Typically `aggregate.version` before the
     *        command was processed.
     * @return `true` if the events were successfully appended; `false` if a
     *         version conflict was detected.
     */
    fun appendEvents(
        aggregateId: UUID,
        events: List<AuctionEventEntity>,
        expectedVersion: Long
    ): Boolean {
        if (events.isEmpty()) return true

        // Use the JTA-managed connection (no manual commit/rollback).
        // The calling @Transactional method handles the transaction boundary.
        dataSource.connection.use { conn ->
            // Check for version conflicts
            conn.prepareStatement(CHECK_VERSION_CONFLICT).use { checkStmt ->
                checkStmt.setObject(1, aggregateId)
                checkStmt.setLong(2, expectedVersion + 1)
                checkStmt.executeQuery().use { rs ->
                    rs.next()
                    val conflictCount = rs.getInt(1)
                    if (conflictCount > 0) {
                        LOG.warnf(
                            "Optimistic concurrency conflict for aggregate %s at expected version %d. " +
                                "Found %d events at or above that version.",
                            aggregateId, expectedVersion, conflictCount
                        )
                        return false
                    }
                }
            }

            // Insert all events in the batch
            conn.prepareStatement(INSERT_EVENT).use { insertStmt ->
                for (event in events) {
                    insertStmt.setObject(1, event.eventId)
                    insertStmt.setObject(2, event.aggregateId)
                    insertStmt.setString(3, event.aggregateType)
                    insertStmt.setString(4, event.eventType)
                    insertStmt.setString(5, event.eventData)
                    insertStmt.setLong(6, event.version)
                    insertStmt.setTimestamp(7, Timestamp.from(event.createdAt))
                    insertStmt.setString(8, event.brand)
                    insertStmt.setString(9, event.metadata)
                    insertStmt.addBatch()
                }
                insertStmt.executeBatch()
            }

            LOG.debugf(
                "Appended %d events to aggregate %s (versions %d..%d)",
                events.size, aggregateId,
                events.first().version, events.last().version
            )
            return true
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toEntityList(): List<AuctionEventEntity> {
        val entities = mutableListOf<AuctionEventEntity>()
        while (next()) {
            entities.add(toEntity())
        }
        return entities
    }

    private fun ResultSet.toEntity(): AuctionEventEntity = AuctionEventEntity(
        eventId = getObject("event_id", UUID::class.java),
        aggregateId = getObject("aggregate_id", UUID::class.java),
        aggregateType = getString("aggregate_type"),
        eventType = getString("event_type"),
        eventData = getString("event_data"),
        version = getLong("version"),
        createdAt = getTimestamp("created_at").toInstant(),
        brand = getString("brand"),
        metadata = getString("metadata")
    )
}
