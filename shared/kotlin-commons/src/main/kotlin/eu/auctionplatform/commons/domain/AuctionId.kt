package eu.auctionplatform.commons.domain

import eu.auctionplatform.commons.util.IdGenerator
import java.util.UUID

/**
 * Strongly-typed identifier for an Auction aggregate.
 */
@JvmInline
value class AuctionId(val value: UUID) {

    override fun toString(): String = value.toString()

    companion object {
        /** Generates a new [AuctionId] backed by a UUIDv7. */
        fun generate(): AuctionId = AuctionId(IdGenerator.generateUUIDv7())

        /** Parses a UUID string into an [AuctionId]. */
        fun fromString(value: String): AuctionId = AuctionId(UUID.fromString(value))
    }
}
