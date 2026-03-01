package eu.auctionplatform.payment.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Represents a payment for a won auction lot.
 *
 * A payment is created when a buyer wins a lot and enters checkout. It
 * captures the hammer price, buyer premium, calculated VAT, and total
 * amount due. The payment lifecycle is tracked through [PaymentStatus].
 *
 * @property id Unique payment identifier.
 * @property buyerId The winning bidder / buyer.
 * @property sellerId The seller of the lot.
 * @property auctionId The auction from which the lot was won.
 * @property lotId The specific lot that was purchased.
 * @property hammerPrice The final bid (hammer) price.
 * @property buyerPremium The buyer premium amount (hammerPrice * buyerPremiumRate).
 * @property buyerPremiumRate The percentage rate used to calculate the buyer premium (e.g. 0.15 for 15%).
 * @property vatAmount The calculated VAT amount.
 * @property vatRate The effective VAT rate applied (e.g. 0.21 for 21%).
 * @property vatScheme The VAT scheme that was applied (standard, reverse charge, etc.).
 * @property totalAmount The total amount due (hammerPrice + buyerPremium + vatAmount).
 * @property currency ISO 4217 currency code (e.g. "EUR").
 * @property country The country code used for VAT determination.
 * @property paymentMethod The payment method chosen by the buyer (e.g. "card", "bank_transfer").
 * @property pspReference The payment service provider reference (Adyen).
 * @property status Current payment lifecycle status.
 * @property dueDate Deadline by which payment must be completed.
 * @property paidAt Timestamp when payment was confirmed by the PSP.
 * @property createdAt Timestamp when the payment record was created.
 * @property lotTitle Denormalized lot title for display purposes (may be null).
 * @property buyerName Denormalized buyer display name (may be null).
 * @property sellerName Denormalized seller display name (may be null).
 */
data class Payment(
    val id: UUID,
    val buyerId: UUID,
    val sellerId: UUID,
    val auctionId: UUID,
    val lotId: UUID,
    val hammerPrice: BigDecimal,
    val buyerPremium: BigDecimal,
    val buyerPremiumRate: BigDecimal,
    val vatAmount: BigDecimal,
    val vatRate: BigDecimal,
    val vatScheme: VatScheme,
    val totalAmount: BigDecimal,
    val currency: String,
    val country: String,
    val paymentMethod: String?,
    val pspReference: String?,
    val status: PaymentStatus,
    val dueDate: Instant,
    val paidAt: Instant?,
    val createdAt: Instant,
    val lotTitle: String? = null,
    val buyerName: String? = null,
    val sellerName: String? = null
)
