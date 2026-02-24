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
 */
data class CheckoutRequest(

    /** List of lot IDs to check out. */
    @field:NotEmpty(message = "At least one lot is required")
    @JsonProperty("lotIds")
    val lotIds: List<String>,

    /** Buyer's country code (ISO 3166-1 alpha-2). */
    @field:NotBlank(message = "Buyer country is required")
    @JsonProperty("buyerCountry")
    val buyerCountry: String,

    /** Buyer's account type: "BUSINESS" or "CONSUMER". */
    @field:NotBlank(message = "Buyer type is required")
    @JsonProperty("buyerType")
    val buyerType: String,

    /** Buyer's VAT identification number (required for reverse charge). */
    @JsonProperty("buyerVatId")
    val buyerVatId: String? = null,

    /** ISO 4217 currency code (defaults to EUR). */
    @JsonProperty("currency")
    val currency: String = "EUR"
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

    /** ISO 4217 currency code (defaults to EUR). */
    @JsonProperty("currency")
    val currency: String = "EUR",

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

    @JsonProperty("vatAmount")
    val vatAmount: BigDecimal,

    @JsonProperty("vatRate")
    val vatRate: BigDecimal,

    @JsonProperty("vatScheme")
    val vatScheme: String,

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

    @JsonProperty("vatAmount")
    val vatAmount: BigDecimal,

    @JsonProperty("vatRate")
    val vatRate: BigDecimal,

    @JsonProperty("vatScheme")
    val vatScheme: String,

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
    val createdAt: Instant
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

    @JsonProperty("commission")
    val commission: BigDecimal,

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
