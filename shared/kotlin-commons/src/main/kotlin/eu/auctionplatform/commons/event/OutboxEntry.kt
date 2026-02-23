package eu.auctionplatform.commons.event

import java.time.Instant

/**
 * Represents a row in the transactional outbox table.
 *
 * The outbox pattern guarantees at-least-once delivery by persisting events in
 * the same database transaction as the aggregate state change. A background
 * poller (or CDC connector) reads unpublished entries and forwards them to NATS.
 */
data class OutboxEntry(

    /** Database-generated surrogate key. Null for entries that have not been persisted yet. */
    val id: Long? = null,

    /** Identifier of the aggregate that produced the event. */
    val aggregateId: String,

    /** Discriminator string, e.g. "BidPlacedEvent". */
    val eventType: String,

    /** Serialised event payload (JSON). */
    val payload: String,

    /** Target NATS subject for this event. */
    val natsSubject: String,

    /** Whether the event has been successfully published to NATS. */
    val published: Boolean = false,

    /** Timestamp when the outbox entry was created (within the transaction). */
    val createdAt: Instant = Instant.now(),

    /** Timestamp when the event was successfully published. Null if not yet published. */
    val publishedAt: Instant? = null,

    /** Number of publish attempts that have failed so far. */
    val retryCount: Int = 0
)
