package eu.auctionplatform.auction.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity for auction aggregate snapshots.
 *
 * Snapshots capture the full state of an auction aggregate at a specific
 * [version], enabling fast rehydration without replaying the entire event
 * stream from the beginning. When loading an aggregate, the repository
 * first checks for a snapshot and then replays only the events that
 * occurred after the snapshot's version.
 *
 * Only one snapshot per aggregate is stored (upsert semantics on [aggregateId]).
 */
@Entity
@Table(name = "auction_snapshots", schema = "app")
class AuctionSnapshotEntity(

    /** Identifier of the auction aggregate this snapshot belongs to. */
    @Id
    @Column(name = "aggregate_id", nullable = false, updatable = false)
    var aggregateId: UUID = UUID.randomUUID(),

    /** The aggregate version at which this snapshot was taken. */
    @Column(name = "version", nullable = false)
    var version: Long = 0,

    /** Serialised aggregate state as JSON text. */
    @Column(name = "state", nullable = false, columnDefinition = "TEXT")
    var state: String = "",

    /** UTC timestamp when the snapshot was created. */
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
