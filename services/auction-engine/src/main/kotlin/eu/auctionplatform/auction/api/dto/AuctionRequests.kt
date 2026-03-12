package eu.auctionplatform.auction.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant

/**
 * Request body for creating a new auction.
 *
 * The API layer validates field presence and basic constraints; domain-level
 * validation (e.g. startTime in the future, reservePrice > startingBid) is
 * enforced by the [CreateAuctionCommand].
 */
data class CreateAuctionRequest(

    /** Identifier of the lot to be auctioned. */
    @field:NotBlank(message = "Lot ID is required")
    @JsonProperty("lotId")
    val lotId: String? = null,

    /** Brand / tenant code (e.g. "troostwijk", "surplex"). */
    @field:NotBlank(message = "Brand is required")
    @JsonProperty("brand")
    val brand: String? = null,

    /** Scheduled start time (UTC ISO-8601). */
    @field:NotNull(message = "Start time is required")
    @JsonProperty("startTime")
    val startTime: Instant? = null,

    /** Scheduled end time (UTC ISO-8601). */
    @field:NotNull(message = "End time is required")
    @JsonProperty("endTime")
    val endTime: Instant? = null,

    /** Minimum opening bid amount. */
    @field:NotNull(message = "Starting bid is required")
    @field:DecimalMin(value = "0.01", message = "Starting bid must be positive")
    @JsonProperty("startingBid")
    val startingBid: BigDecimal? = null,

    /** ISO 4217 currency code (defaults to EUR). */
    @JsonProperty("currency")
    val currency: String = "EUR",

    /**
     * Optional minimum price that must be met for the lot to be awarded.
     * Null means no reserve.
     */
    @JsonProperty("reservePrice")
    val reservePrice: BigDecimal? = null,

    /** Identifier of the user selling the lot. */
    @field:NotBlank(message = "Seller ID is required")
    @JsonProperty("sellerId")
    val sellerId: String? = null
)

/**
 * Request body for placing a bid on an auction.
 */
data class PlaceBidRequest(

    /** Bid amount in the auction's settlement currency. */
    @field:NotNull(message = "Bid amount is required")
    @field:DecimalMin(value = "0.01", message = "Bid amount must be positive")
    @JsonProperty("amount")
    val amount: BigDecimal,

    /** ISO 4217 currency code (defaults to EUR). */
    @JsonProperty("currency")
    val currency: String = "EUR"
)

/**
 * Request body for configuring an automatic (proxy) bid.
 */
data class SetAutoBidRequest(

    /** Maximum amount the proxy engine may bid on behalf of the user. */
    @field:NotNull(message = "Max amount is required")
    @field:DecimalMin(value = "0.01", message = "Max amount must be positive")
    @JsonProperty("maxAmount")
    val maxAmount: BigDecimal,

    /** ISO 4217 currency code (defaults to EUR). */
    @JsonProperty("currency")
    val currency: String = "EUR"
)
