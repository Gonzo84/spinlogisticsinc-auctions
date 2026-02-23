package eu.auctionplatform.auction.infrastructure.persistence.entity

import eu.auctionplatform.commons.domain.DomainEvent
import eu.auctionplatform.commons.util.JsonMapper
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity mapped to the `auction_events` event-store table.
 *
 * Each row represents a single domain event that has been appended to an
 * auction aggregate's event stream. The combination of [aggregateId] and
 * [version] is unique, providing optimistic-concurrency guarantees.
 *
 * The [eventData] and [metadata] fields store JSON as plain text, keeping
 * the event store schema-agnostic and allowing the domain model to evolve
 * independently of the persistence layer.
 */
@Entity
@Table(name = "auction_events", schema = "app")
class AuctionEventEntity(

    /** Globally unique event identifier (UUIDv7). */
    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    var eventId: UUID = UUID.randomUUID(),

    /** Identifier of the aggregate instance that produced this event. */
    @Column(name = "aggregate_id", nullable = false, updatable = false)
    var aggregateId: UUID = UUID.randomUUID(),

    /** Logical aggregate type name (e.g. "Auction"). */
    @Column(name = "aggregate_type", nullable = false, updatable = false)
    var aggregateType: String = "Auction",

    /** Discriminator string for (de)serialisation routing (e.g. "BidPlacedEvent"). */
    @Column(name = "event_type", nullable = false, updatable = false)
    var eventType: String = "",

    /** Serialised event payload as JSON text. */
    @Column(name = "event_data", nullable = false, updatable = false, columnDefinition = "TEXT")
    var eventData: String = "",

    /** Monotonically increasing version within the aggregate's event stream. */
    @Column(name = "version", nullable = false, updatable = false)
    var version: Long = 0,

    /** UTC timestamp when the event was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    /** Brand / tenant code that owns this aggregate. */
    @Column(name = "brand", nullable = false, updatable = false)
    var brand: String = "",

    /** Optional tracing / audit metadata as JSON text. */
    @Column(name = "metadata", updatable = false, columnDefinition = "TEXT")
    var metadata: String? = null
) {

    /**
     * Deserialises the [eventData] JSON back into a [DomainEvent] instance.
     *
     * The Jackson ObjectMapper is configured with type information so that
     * the concrete event subclass (e.g. [BidPlacedEvent], [AuctionCreatedEvent])
     * is correctly resolved from the JSON payload.
     *
     * @return The reconstituted domain event.
     */
    fun toDomainEvent(): DomainEvent {
        return JsonMapper.instance.readValue(eventData, DomainEvent::class.java)
    }

    companion object {

        /**
         * Creates an [AuctionEventEntity] from a [DomainEvent].
         *
         * Serialises the entire event to JSON for storage in the event_data
         * column. The [version] parameter is the event's position in the
         * aggregate's event stream.
         *
         * @param event The domain event to persist.
         * @param version The stream version for this event.
         * @return A new entity ready for insertion into the event store.
         */
        fun fromDomainEvent(event: DomainEvent, version: Long): AuctionEventEntity {
            return AuctionEventEntity(
                eventId = UUID.fromString(event.eventId),
                aggregateId = UUID.fromString(event.aggregateId),
                aggregateType = event.aggregateType,
                eventType = event.eventType,
                eventData = JsonMapper.toJson(event),
                version = version,
                createdAt = event.timestamp,
                brand = event.brand
            )
        }

        /**
         * Creates an [AuctionEventEntity] from a [DomainEvent], using the
         * event's own version field.
         *
         * @param event The domain event to persist.
         * @return A new entity ready for insertion into the event store.
         */
        fun fromDomainEvent(event: DomainEvent): AuctionEventEntity {
            return fromDomainEvent(event, event.version)
        }
    }
}
