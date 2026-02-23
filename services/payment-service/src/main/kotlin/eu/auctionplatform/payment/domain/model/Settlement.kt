package eu.auctionplatform.payment.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Represents a seller settlement — the payout to a seller after a
 * successful payment from the buyer.
 *
 * The net amount is the hammer price minus the platform commission.
 * Settlement is initiated after the buyer's payment is confirmed and
 * the escrow holding period has elapsed.
 *
 * @property id Unique settlement identifier.
 * @property sellerId The seller receiving the payout.
 * @property paymentId The buyer payment this settlement originates from.
 * @property netAmount The amount to be paid to the seller (hammerPrice - commission).
 * @property commission The platform commission amount.
 * @property commissionRate The commission percentage rate (e.g. 0.10 for 10%).
 * @property status Current settlement lifecycle status.
 * @property settledAt Timestamp when the bank transfer was confirmed.
 * @property bankReference Bank wire transfer reference for reconciliation.
 * @property createdAt Timestamp when the settlement record was created.
 */
data class Settlement(
    val id: UUID,
    val sellerId: UUID,
    val paymentId: UUID,
    val netAmount: BigDecimal,
    val commission: BigDecimal,
    val commissionRate: BigDecimal,
    val status: SettlementStatus,
    val settledAt: Instant?,
    val bankReference: String?,
    val createdAt: Instant
)
