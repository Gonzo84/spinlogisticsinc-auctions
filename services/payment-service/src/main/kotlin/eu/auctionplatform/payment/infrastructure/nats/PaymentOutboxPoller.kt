package eu.auctionplatform.payment.infrastructure.nats

import eu.auctionplatform.commons.outbox.OutboxEntry
import io.agroal.api.AgroalDataSource
import io.nats.client.Connection
import io.nats.client.impl.Headers
import io.nats.client.impl.NatsMessage
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.sql.ResultSet
import java.time.Instant

/**
 * Outbox poller for the payment service.
 *
 * Polls the `app.outbox` table every 5 seconds for unpublished events
 * and publishes them to NATS JetStream. Uses `SELECT ... FOR UPDATE SKIP LOCKED`
 * for safe concurrent polling across multiple instances.
 *
 * Outbox entries are written transactionally by services like CheckoutService,
 * NonPaymentService, and SettlementService to guarantee at-least-once
 * delivery of domain events.
 */
@ApplicationScoped
class PaymentOutboxPoller {

    companion object {
        private val LOG: Logger = Logger.getLogger(PaymentOutboxPoller::class.java)
        private const val TABLE_NAME = "app.outbox"
        private const val BATCH_SIZE = 50
    }

    @Inject
    lateinit var dataSource: AgroalDataSource

    @Inject
    lateinit var natsConnection: Connection

    /**
     * Scheduled polling method. Runs every 5 seconds to check for
     * unpublished outbox entries.
     */
    @Scheduled(every = "5s", identity = "payment-outbox-poller")
    fun poll() {
        try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(
                    """
                    SELECT * FROM $TABLE_NAME
                    WHERE published = false
                    ORDER BY created_at
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                    """.trimIndent()
                ).use { stmt ->
                    stmt.setInt(1, BATCH_SIZE)
                    stmt.executeQuery().use { rs ->
                        var count = 0
                        while (rs.next()) {
                            val entry = mapRow(rs)
                            try {
                                publishEntry(entry)
                                markPublished(conn, entry.id)
                                count++
                            } catch (e: Exception) {
                                LOG.errorf("Failed to publish outbox entry %s: %s", entry.id, e.message)
                            }
                        }
                        if (count > 0) {
                            LOG.infof("Published %s outbox entries from %s", count, TABLE_NAME)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.errorf("Outbox poll failed for %s: %s", TABLE_NAME, e.message)
        }
    }

    /**
     * Maps a JDBC [ResultSet] row to an [OutboxEntry].
     *
     * The outbox table schema (V004) has columns:
     * id (BIGSERIAL), aggregate_id (UUID), event_type (TEXT), payload (TEXT),
     * nats_subject (TEXT), published (BOOLEAN), created_at (TIMESTAMPTZ),
     * published_at (TIMESTAMPTZ), retry_count (INT), dead_letter (BOOLEAN).
     */
    private fun mapRow(rs: ResultSet): OutboxEntry = OutboxEntry(
        id = rs.getLong("id").toString(),
        natsSubject = rs.getString("nats_subject"),
        payload = rs.getString("payload"),
        eventType = rs.getString("event_type"),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )

    /**
     * Publishes a single outbox entry to NATS JetStream.
     *
     * Attaches the event type as a NATS header for consumer-side filtering
     * and routing.
     */
    private fun publishEntry(entry: OutboxEntry) {
        val headers = Headers()
        headers.add("event-type", entry.eventType)

        val message = NatsMessage.builder()
            .subject(entry.natsSubject)
            .data(entry.payload.toByteArray(Charsets.UTF_8))
            .headers(headers)
            .build()

        val ack = natsConnection.jetStream().publish(message)
        LOG.debugf(
            "Published outbox entry %s to %s (stream=%s, seq=%s)",
            entry.id, entry.natsSubject, ack.stream, ack.seqno
        )
    }

    /**
     * Marks an outbox entry as published.
     */
    private fun markPublished(conn: java.sql.Connection, id: String) {
        conn.prepareStatement(
            "UPDATE $TABLE_NAME SET published = true, published_at = NOW() WHERE id = ?"
        ).use { stmt ->
            stmt.setString(1, id)
            stmt.executeUpdate()
        }
    }
}
