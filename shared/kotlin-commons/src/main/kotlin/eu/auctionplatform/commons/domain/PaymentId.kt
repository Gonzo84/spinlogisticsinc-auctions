package eu.auctionplatform.commons.domain

import eu.auctionplatform.commons.util.IdGenerator
import java.util.UUID

/**
 * Strongly-typed identifier for a Payment aggregate.
 */
@JvmInline
value class PaymentId(val value: UUID) {

    override fun toString(): String = value.toString()

    companion object {
        fun generate(): PaymentId = PaymentId(IdGenerator.generateUUIDv7())
        fun fromString(value: String): PaymentId = PaymentId(UUID.fromString(value))
    }
}
