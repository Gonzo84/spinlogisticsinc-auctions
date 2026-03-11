package eu.auctionplatform.commons.domain

import eu.auctionplatform.commons.util.IdGenerator
import java.util.UUID

/**
 * Strongly-typed identifier for a Broker aggregate.
 */
@JvmInline
value class BrokerId(val value: UUID) {

    override fun toString(): String = value.toString()

    companion object {
        fun generate(): BrokerId = BrokerId(IdGenerator.generateUUIDv7())
        fun fromString(value: String): BrokerId = BrokerId(UUID.fromString(value))
    }
}
