package eu.auctionplatform.payment.api.v1.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant

// =============================================================================
// Request DTOs
// =============================================================================

/**
 * Request body for initiating checkout of won lots.
 *
 * Supports two input modes:
 * 1. **Simple mode** — provide [lotIds] only; the service resolves auction
 *    details (hammer price, seller ID) via inter-service HTTP lookups.
 * 2. **Explicit mode** — provide [items] with auctionId and hammerPrice
 *    pre-resolved by the caller (e.g. from the award response). This is
 *    the preferred mode as it avoids fragile inter-service calls.
 *
 * When [items] is non-empty it takes precedence over [lotIds].
 */
data class CheckoutRequest(

    /** List of lot IDs to check out (simple mode). */
    @JsonProperty("lotIds")
    val lotIds: List<String> = emptyList(),

    /** Lot checkout items with pre-resolved auction data (explicit mode). */
    @JsonProperty("items")
    val items: List<CheckoutItemRequest> = emptyList(),

    /** US state code (2-letter) of the buyer (destination for sales tax). */
    @field:NotBlank(message = "Buyer state is required")
    @JsonProperty("buyerState")
    val buyerState: String? = null,

    /** Buyer's exemption certificate ID (resale, manufacturing, or government), null if none. */
    @JsonProperty("exemptionCertificateId")
    val exemptionCertificateId: String? = null,

    /** ISO 4217 currency code (defaults to USD). */
    @JsonProperty("currency")
    val currency: String = "USD"
)

/**
 * A single lot item in a checkout request with pre-resolved auction data.
 */
data class CheckoutItemRequest(

    /** The lot UUID. */
    @field:NotBlank(message = "Lot ID is required")
    @JsonProperty("lotId")
    val lotId: String,

    /** The auction UUID that awarded this lot. */
    @field:NotBlank(message = "Auction ID is required")
    @JsonProperty("auctionId")
    val auctionId: String,

    /** The hammer price from the auction award. */
    @field:NotNull(message = "Hammer price is required")
    @field:DecimalMin(value = "0.01", message = "Hammer price must be positive")
    @JsonProperty("hammerPrice")
    val hammerPrice: BigDecimal,

    /** The seller UUID (optional, resolved from catalog if omitted). */
    @JsonProperty("sellerId")
    val sellerId: String? = null
)

/**
 * Request body for submitting a payment.
 */
data class PaymentSubmitRequest(

    /** Payment method: "card", "ideal", "bank_transfer", "sofort", etc. */
    @field:NotBlank(message = "Payment method is required")
    @JsonProperty("paymentMethod")
    val paymentMethod: String,

    /** Browser return URL for redirect-based payment methods (3DS, iDEAL). */
    @JsonProperty("returnUrl")
    val returnUrl: String? = null
)

/**
 * Request body for initiating a bid deposit.
 */
data class DepositRequest(

    /** The auction UUID the deposit is for. */
    @field:NotBlank(message = "Auction ID is required")
    @JsonProperty("auctionId")
    val auctionId: String,

    /** Deposit amount. */
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Amount must be positive")
    @JsonProperty("amount")
    val amount: BigDecimal,

    /** ISO 4217 currency code (defaults to USD). */
    @JsonProperty("currency")
    val currency: String = "USD",

    /** Payment method for the deposit. */
    @field:NotBlank(message = "Payment method is required")
    @JsonProperty("paymentMethod")
    val paymentMethod: String
)

/**
 * Request body for requesting a deposit refund.
 */
data class DepositRefundRequest(

    /** The auction UUID the deposit was placed for. */
    @field:NotBlank(message = "Auction ID is required")
    @JsonProperty("auctionId")
    val auctionId: String,

    /** Reason for refund request. */
    @JsonProperty("reason")
    val reason: String? = null
)

// =============================================================================
// Response DTOs
// =============================================================================

/**
 * Checkout initiation response containing payment details for each lot.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CheckoutResponse(

    @JsonProperty("checkoutId")
    val checkoutId: String,

    @JsonProperty("payments")
    val payments: List<PaymentSummary>,

    @JsonProperty("totalAmount")
    val totalAmount: BigDecimal,

    @JsonProperty("currency")
    val currency: String,

    @JsonProperty("dueDate")
    val dueDate: Instant
)

/**
 * Summary of a single payment within a checkout.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaymentSummary(

    @JsonProperty("paymentId")
    val paymentId: String,

    @JsonProperty("lotId")
    val lotId: String,

    @JsonProperty("hammerPrice")
    val hammerPrice: BigDecimal,

    @JsonProperty("buyerPremium")
    val buyerPremium: BigDecimal,

    @JsonProperty("taxAmount")
    val taxAmount: BigDecimal,

    @JsonProperty("taxRate")
    val taxRate: BigDecimal,

    @JsonProperty("taxScheme")
    val taxScheme: String,

    @JsonProperty("totalAmount")
    val totalAmount: BigDecimal
)

/**
 * Detailed payment status response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaymentStatusResponse(

    @JsonProperty("paymentId")
    val paymentId: String,

    @JsonProperty("buyerId")
    val buyerId: String,

    @JsonProperty("sellerId")
    val sellerId: String,

    @JsonProperty("auctionId")
    val auctionId: String,

    @JsonProperty("lotId")
    val lotId: String,

    @JsonProperty("hammerPrice")
    val hammerPrice: BigDecimal,

    @JsonProperty("buyerPremium")
    val buyerPremium: BigDecimal,

    @JsonProperty("taxAmount")
    val taxAmount: BigDecimal,

    @JsonProperty("taxRate")
    val taxRate: BigDecimal,

    @JsonProperty("taxScheme")
    val taxScheme: String,

    @JsonProperty("totalAmount")
    val totalAmount: BigDecimal,

    @JsonProperty("currency")
    val currency: String,

    @JsonProperty("status")
    val status: String,

    @JsonProperty("paymentMethod")
    val paymentMethod: String?,

    @JsonProperty("pspReference")
    val pspReference: String?,

    @JsonProperty("dueDate")
    val dueDate: Instant,

    @JsonProperty("paidAt")
    val paidAt: Instant?,

    @JsonProperty("createdAt")
    val createdAt: Instant,

    @JsonProperty("lotTitle")
    val lotTitle: String? = null,

    @JsonProperty("buyerName")
    val buyerName: String? = null,

    @JsonProperty("sellerName")
    val sellerName: String? = null
)

/**
 * Invoice summary response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class InvoiceResponse(

    @JsonProperty("invoiceId")
    val invoiceId: String,

    @JsonProperty("paymentId")
    val paymentId: String,

    @JsonProperty("invoiceNumber")
    val invoiceNumber: String,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("pdfUrl")
    val pdfUrl: String?,

    @JsonProperty("issuedAt")
    val issuedAt: Instant
)

/**
 * Settlement summary response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SettlementResponse(

    @JsonProperty("settlementId")
    val settlementId: String,

    @JsonProperty("sellerId")
    val sellerId: String,

    @JsonProperty("paymentId")
    val paymentId: String,

    @JsonProperty("netAmount")
    val netAmount: BigDecimal,

    @JsonProperty("commissionAmount")
    val commissionAmount: BigDecimal,

    @JsonProperty("commissionRate")
    val commissionRate: BigDecimal,

    @JsonProperty("status")
    val status: String,

    @JsonProperty("settledAt")
    val settledAt: Instant?,

    @JsonProperty("bankReference")
    val bankReference: String?,

    @JsonProperty("createdAt")
    val createdAt: Instant
)

/**
 * Aggregate payment summary for the admin dashboard.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaymentsSummaryResponse(

    @JsonProperty("totalPending")
    val totalPending: BigDecimal,

    @JsonProperty("totalOverdue")
    val totalOverdue: BigDecimal,

    @JsonProperty("totalPaid")
    val totalPaid: BigDecimal,

    @JsonProperty("totalDisputed")
    val totalDisputed: BigDecimal,

    @JsonProperty("pendingCount")
    val pendingCount: Int,

    @JsonProperty("overdueCount")
    val overdueCount: Int
)

/**
 * Deposit confirmation response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DepositResponse(

    @JsonProperty("depositId")
    val depositId: String,

    @JsonProperty("auctionId")
    val auctionId: String,

    @JsonProperty("amount")
    val amount: BigDecimal,

    @JsonProperty("currency")
    val currency: String,

    @JsonProperty("status")
    val status: String,

    @JsonProperty("createdAt")
    val createdAt: Instant
)
