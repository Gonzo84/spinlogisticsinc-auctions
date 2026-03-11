package eu.auctionplatform.commons.outbox

import io.agroal.api.AgroalDataSource
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.jboss.logging.Logger
import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant

/**
 * Abstract outbox poller that reads unpublished events from an outbox table
 * and publishes them to NATS. Services extend this and configure their specifics.
 *
 * The poller uses `SELECT ... FOR UPDATE SKIP LOCKED` so that multiple instances
 * can safely poll the same table without processing duplicates.
 *
 * Subclasses must:
 * 1. Set [tableName] to the service-specific outbox table.
 * 2. Set [batchSize] to control how many entries are fetched per poll cycle.
 * 3. Implement [mapRow] to convert a JDBC [ResultSet] row into an [OutboxEntry].
 * 4. Implement [publishEntry] to send the entry payload to NATS.
 * 5. Add `@Scheduled(every = "\${outbox.poll-interval:5s}")` on their override of [poll].
 *
 * @param meterRegistry Optional Micrometer registry for publishing custom metrics.
 *                      When null, no metrics are recorded. Injected by Quarkus CDI at runtime.
 */
abstract class OutboxPoller(
    private val dataSource: AgroalDataSource,
    private val meterRegistry: MeterRegistry? = null
) {
    companion object {
        private val LOG: Logger = Logger.getLogger(OutboxPoller::class.java)
    }

    /** Name of the outbox table, e.g. "outbox_events". */
    abstract val tableName: String

    /** Maximum number of entries to fetch per poll cycle. */
    abstract val batchSize: Int

    /** Publishes a single outbox entry to NATS. */
    abstract fun publishEntry(entry: OutboxEntry)

    /** Maps a JDBC [ResultSet] row (cursor on current row) to an [OutboxEntry]. */
    abstract fun mapRow(rs: ResultSet): OutboxEntry

    /**
     * Polls the outbox table for unpublished entries and publishes them.
     *
     * Subclasses should annotate their override with `@Scheduled` to enable periodic polling.
     * Example: `@Scheduled(every = "\${outbox.poll-interval:5s}")`
     */
    open fun poll() {
        val timerSample = meterRegistry?.let { Timer.start(it) }
        try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(
                    """
                    SELECT * FROM $tableName
                    WHERE published = false
                    ORDER BY created_at
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setInt(1, batchSize)
                    stmt.executeQuery().use { rs ->
                        var count = 0
                        while (rs.next()) {
                            val entry = mapRow(rs)
                            try {
                                publishEntry(entry)
                                markPublished(conn, entry.id)
                                count++
                                meterRegistry?.counter(
                                    "outbox.publish.total",
                                    "table", tableName
                                )?.increment()
                            } catch (e: Exception) {
                                LOG.errorf("Failed to publish outbox entry %s: %s", entry.id, e.message)
                                meterRegistry?.counter(
                                    "outbox.publish.failures",
                                    "table", tableName
                                )?.increment()
                            }
                        }
                        if (count > 0) {
                            LOG.infof("Published %s outbox entries from %s", count, tableName)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.errorf("Outbox poll failed for %s: %s", tableName, e.message)
        } finally {
            if (timerSample != null && meterRegistry != null) {
                timerSample.stop(
                    Timer.builder("outbox.poll.duration")
                        .tag("table", tableName)
                        .register(meterRegistry)
                )
            }
        }
    }

    private fun markPublished(conn: Connection, id: String) {
        conn.prepareStatement(
            "UPDATE $tableName SET published = true, published_at = NOW() WHERE id = ?"
        ).use { stmt ->
            stmt.setLong(1, id.toLong())
            stmt.executeUpdate()
        }
    }
}

/**
 * Represents a single row from a service's outbox table.
 *
 * This is a transport-level data class used by [OutboxPoller]; it is intentionally
 * separate from [eu.auctionplatform.commons.event.OutboxEntry] which models the
 * domain-level outbox concept with richer typing.
 */
data class OutboxEntry(
    val id: String,
    val natsSubject: String,
    val payload: String,
    val eventType: String,
    val createdAt: Instant
)
