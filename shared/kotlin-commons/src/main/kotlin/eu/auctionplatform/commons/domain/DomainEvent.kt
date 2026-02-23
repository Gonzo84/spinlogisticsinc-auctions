package eu.auctionplatform.commons.domain

import java.time.Instant

/**
 * Base interface for all domain events in the auction platform.
 *
 * Every event carries enough metadata to be routed, stored, and replayed
 * without requiring additional context from the publishing aggregate.
 */
interface DomainEvent {

    /** Unique event identifier (UUIDv7 – time-sortable). */
    val eventId: String

    /** Discriminator string used for (de)serialisation routing, e.g. "BidPlacedEvent". */
    val eventType: String

    /** Identifier of the aggregate instance that produced this event. */
    val aggregateId: String

    /** Logical type name of the aggregate, e.g. "Auction", "Lot". */
    val aggregateType: String

    /** Brand / tenant that owns this aggregate. */
    val brand: String

    /** Point in time when the event was raised (UTC). */
    val timestamp: Instant

    /** Monotonically increasing version within the aggregate's event stream. */
    val version: Long
}
