package eu.auctionplatform.commons.domain

import eu.auctionplatform.commons.util.IdGenerator
import java.util.UUID

/**
 * Strongly-typed identifier for a User aggregate.
 */
@JvmInline
value class UserId(val value: UUID) {

    override fun toString(): String = value.toString()

    companion object {
        /** Generates a new [UserId] backed by a UUIDv7. */
        fun generate(): UserId = UserId(IdGenerator.generateUUIDv7())

        /** Parses a UUID string into a [UserId]. */
        fun fromString(value: String): UserId = UserId(UUID.fromString(value))
    }
}
