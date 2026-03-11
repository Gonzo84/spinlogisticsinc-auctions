package eu.auctionplatform.commons.domain

import eu.auctionplatform.commons.util.IdGenerator
import java.util.UUID

/**
 * Strongly-typed identifier for a Settlement aggregate.
 */
@JvmInline
value class SettlementId(val value: UUID) {

    override fun toString(): String = value.toString()

    companion object {
        fun generate(): SettlementId = SettlementId(IdGenerator.generateUUIDv7())
        fun fromString(value: String): SettlementId = SettlementId(UUID.fromString(value))
    }
}
