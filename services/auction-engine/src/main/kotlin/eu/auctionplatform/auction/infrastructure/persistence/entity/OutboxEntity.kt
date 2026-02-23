package eu.auctionplatform.auction.infrastructure.persistence.entity

import eu.auctionplatform.commons.domain.DomainEvent
import eu.auctionplatform.commons.util.JsonMapper
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity for the transactional outbox table.
 *
 * The outbox pattern guarantees at-least-once delivery of domain events by
 * persisting them in the same database transaction as the aggregate state
 * change. A background poller reads unpublished entries and forwards them
 * to NATS JetStream.
 *
 * Entries that repeatedly fail publishing are eventually moved to the dead
 * letter queue (marked via the [deadLetter] flag) to prevent infinite retry
 * loops from blocking other messages.
 */
@Entity
@Table(name = "outbox", schema = "app")
class OutboxEntity(

    /** Database-generated surrogate key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,

    /** Identifier of the aggregate that produced the event. */
    @Column(name = "aggregate_id", nullable = false, updatable = false)
    var aggregateId: UUID = UUID.randomUUID(),

    /** Event type discriminator (e.g. "BidPlacedEvent"). */
    @Column(name = "event_type", nullable = false, updatable = false)
    var eventType: String = "",

    /** Serialised event payload as JSON text. */
    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "TEXT")
    var payload: String = "",

    /** Target NATS subject for this event. */
    @Column(name = "nats_subject", nullable = false, updatable = false)
    var natsSubject: String = "",

    /** Whether the event has been successfully published to NATS. */
    @Column(name = "published", nullable = false)
    var published: Boolean = false,

    /** UTC timestamp when the outbox entry was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    /** UTC timestamp when the event was successfully published. */
    @Column(name = "published_at")
    var publishedAt: Instant? = null,

    /** Number of failed publish attempts. */
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    /** Whether this entry has been moved to the dead letter queue. */
    @Column(name = "dead_letter", nullable = false)
    var deadLetter: Boolean = false
) {

    companion object {

        /**
         * Creates an [OutboxEntity] from a [DomainEvent] and a target NATS [subject].
         *
         * The event is serialised to JSON for the payload column. The entry is
         * initially marked as unpublished (published = false).
         *
         * @param event The domain event to enqueue for publication.
         * @param subject The NATS subject to publish the event to.
         * @return A new outbox entity ready for persistence.
         */
        fun fromDomainEvent(event: DomainEvent, subject: String): OutboxEntity {
            return OutboxEntity(
                aggregateId = UUID.fromString(event.aggregateId),
                eventType = event.eventType,
                payload = JsonMapper.toJson(event),
                natsSubject = subject,
                published = false,
                createdAt = Instant.now(),
                retryCount = 0,
                deadLetter = false
            )
        }
    }
}
