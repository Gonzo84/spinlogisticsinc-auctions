package eu.auctionplatform.user.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Immutable domain model representing a security deposit paid by a user.
 *
 * Deposits are required for bidding on lots whose current price exceeds
 * the deposit threshold. The standard deposit amount is EUR 200.00.
 *
 * The deposit lifecycle follows: payment → active → refund request → refunded.
 *
 * @property id                 Unique identifier (UUIDv7).
 * @property userId             The user who paid the deposit.
 * @property amount             Deposit amount (default EUR 200.00).
 * @property currency           ISO 4217 currency code.
 * @property paidAt             UTC instant when the payment was confirmed.
 * @property refundRequestedAt  UTC instant when the user requested a refund.
 * @property refundedAt         UTC instant when the refund was completed.
 * @property pspReference       Payment service provider transaction reference.
 */
data class Deposit(
    val id: UUID,
    val userId: UUID,
    val amount: BigDecimal = BigDecimal("200.00"),
    val currency: String = "USD",
    val paidAt: Instant? = null,
    val refundRequestedAt: Instant? = null,
    val refundedAt: Instant? = null,
    val pspReference: String? = null
) {

    /** Returns `true` if the deposit has been paid and no refund has been requested or completed. */
    fun isActive(): Boolean = paidAt != null && refundRequestedAt == null && refundedAt == null

    /** Returns `true` if a refund has been requested but not yet processed. */
    fun isRefundPending(): Boolean = refundRequestedAt != null && refundedAt == null
}
