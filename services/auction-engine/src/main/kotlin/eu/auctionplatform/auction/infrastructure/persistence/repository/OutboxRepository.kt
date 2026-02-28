package eu.auctionplatform.auction.infrastructure.persistence.repository

import eu.auctionplatform.auction.infrastructure.persistence.entity.OutboxEntity
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Repository for the transactional outbox table.
 *
 * Provides CRUD operations for outbox entries using direct JDBC. The outbox
 * guarantees at-least-once delivery by persisting events in the same database
 * transaction as the aggregate state change. A background poller reads
 * unpublished entries and forwards them to NATS JetStream.
 */
@ApplicationScoped
class OutboxRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(OutboxRepository::class.java)
        private const val INSERT_ENTRY = """
            INSERT INTO app.outbox
                (aggregate_id, event_type, payload, nats_subject, published,
                 created_at, published_at, retry_count, dead_letter)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val SELECT_PENDING = """
            SELECT id, aggregate_id, event_type, payload, nats_subject,
                   published, created_at, published_at, retry_count, dead_letter
              FROM app.outbox
             WHERE published = FALSE
               AND dead_letter = FALSE
             ORDER BY created_at ASC
             LIMIT ?
        """

        private const val UPDATE_PUBLISHED = """
            UPDATE app.outbox
               SET published = TRUE, published_at = ?
             WHERE id = ?
        """

        private const val INCREMENT_RETRY = """
            UPDATE app.outbox
               SET retry_count = retry_count + 1
             WHERE id = ?
        """

        private const val MOVE_TO_DLQ = """
            UPDATE app.outbox
               SET dead_letter = TRUE
             WHERE id = ?
        """
    }

    /**
     * Persists a new outbox entry.
     *
     * This method should be called within the same transaction as the event
     * store append to guarantee atomicity.
     *
     * @param entry The outbox entry to persist.
     */
    fun save(entry: OutboxEntity) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT_ENTRY).use { stmt ->
                stmt.setObject(1, entry.aggregateId)
                stmt.setString(2, entry.eventType)
                stmt.setString(3, entry.payload)
                stmt.setString(4, entry.natsSubject)
                stmt.setBoolean(5, entry.published)
                stmt.setTimestamp(6, Timestamp.from(entry.createdAt))
                stmt.setTimestamp(7, entry.publishedAt?.let { Timestamp.from(it) })
                stmt.setInt(8, entry.retryCount)
                stmt.setBoolean(9, entry.deadLetter)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Saved outbox entry for aggregate %s (eventType=%s)",
            entry.aggregateId, entry.eventType)
    }

    /**
     * Returns unpublished outbox entries that have not been moved to the dead
     * letter queue, ordered by creation time (oldest first).
     *
     * @param batchSize Maximum number of entries to return.
     * @return List of pending outbox entries.
     */
    fun findPendingEntries(batchSize: Int = 100): List<OutboxEntity> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_PENDING).use { stmt ->
                stmt.setInt(1, batchSize)
                stmt.executeQuery().use { rs ->
                    return rs.toEntityList()
                }
            }
        }
    }

    /**
     * Marks an outbox entry as successfully published and records the
     * publication timestamp.
     *
     * @param id The outbox entry identifier.
     */
    fun markAsPublished(id: Long) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_PUBLISHED).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(Instant.now()))
                stmt.setLong(2, id)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Marked outbox entry %d as published", id)
    }

    /**
     * Increments the retry counter for a failed publish attempt.
     *
     * @param id The outbox entry identifier.
     */
    fun incrementRetryCount(id: Long) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INCREMENT_RETRY).use { stmt ->
                stmt.setLong(1, id)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Incremented retry count for outbox entry %d", id)
    }

    /**
     * Moves an outbox entry to the dead letter queue by setting the
     * [dead_letter] flag. Entries in the DLQ are excluded from the
     * pending-entries query and require manual intervention or a
     * separate retry process.
     *
     * @param id The outbox entry identifier.
     */
    fun moveToDeadLetterQueue(id: Long) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(MOVE_TO_DLQ).use { stmt ->
                stmt.setLong(1, id)
                stmt.executeUpdate()
            }
        }
        LOG.warnf("Moved outbox entry %d to dead letter queue", id)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toEntityList(): List<OutboxEntity> {
        val entities = mutableListOf<OutboxEntity>()
        while (next()) {
            entities.add(toEntity())
        }
        return entities
    }

    private fun ResultSet.toEntity(): OutboxEntity = OutboxEntity(
        id = getLong("id"),
        aggregateId = getObject("aggregate_id", UUID::class.java),
        eventType = getString("event_type"),
        payload = getString("payload"),
        natsSubject = getString("nats_subject"),
        published = getBoolean("published"),
        createdAt = getTimestamp("created_at").toInstant(),
        publishedAt = getTimestamp("published_at")?.toInstant(),
        retryCount = getInt("retry_count"),
        deadLetter = getBoolean("dead_letter")
    )
}
