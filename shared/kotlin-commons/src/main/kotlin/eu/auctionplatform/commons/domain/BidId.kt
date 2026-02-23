package eu.auctionplatform.commons.domain

import eu.auctionplatform.commons.util.IdGenerator
import java.util.UUID

/**
 * Strongly-typed identifier for a Bid entity.
 */
@JvmInline
value class BidId(val value: UUID) {

    override fun toString(): String = value.toString()

    companion object {
        /** Generates a new [BidId] backed by a UUIDv7. */
        fun generate(): BidId = BidId(IdGenerator.generateUUIDv7())

        /** Parses a UUID string into a [BidId]. */
        fun fromString(value: String): BidId = BidId(UUID.fromString(value))
    }
}
