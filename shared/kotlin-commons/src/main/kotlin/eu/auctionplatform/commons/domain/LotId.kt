package eu.auctionplatform.commons.domain

import eu.auctionplatform.commons.util.IdGenerator
import java.util.UUID

/**
 * Strongly-typed identifier for a Lot aggregate.
 */
@JvmInline
value class LotId(val value: UUID) {

    override fun toString(): String = value.toString()

    companion object {
        /** Generates a new [LotId] backed by a UUIDv7. */
        fun generate(): LotId = LotId(IdGenerator.generateUUIDv7())

        /** Parses a UUID string into a [LotId]. */
        fun fromString(value: String): LotId = LotId(UUID.fromString(value))
    }
}
