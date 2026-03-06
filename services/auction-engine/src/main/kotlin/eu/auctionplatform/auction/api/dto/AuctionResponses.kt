package eu.auctionplatform.auction.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant

/**
 * Detailed response for a single auction, including full bid state
 * and timing information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuctionDetailResponse(

    /** Unique auction identifier (UUID). */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** Identifier of the lot being auctioned. */
    @JsonProperty("lotId")
    val lotId: String,

    /** Brand / tenant code (e.g. "troostwijk", "surplex"). */
    @JsonProperty("brand")
    val brand: String,

    /** Current lifecycle status of the auction. */
    @JsonProperty("status")
    val status: String,

    /** Scheduled start time (UTC). */
    @JsonProperty("startTime")
    val startTime: Instant,

    /** Current end time (UTC) -- may differ from original if anti-sniping extensions applied. */
    @JsonProperty("endTime")
    val endTime: Instant,

    /** Originally scheduled end time (UTC) before any extensions. */
    @JsonProperty("originalEndTime")
    val originalEndTime: Instant,

    /** Minimum opening bid amount. */
    @JsonProperty("startingBid")
    val startingBid: BigDecimal?,

    /** Current highest bid amount, null if no bids have been placed. */
    @JsonProperty("currentHighBid")
    val currentHighBid: BigDecimal?,

    /** User ID of the current high bidder, null if no bids. */
    @JsonProperty("currentHighBidderId")
    val currentHighBidderId: String?,

    /** Total number of bids placed on this auction. */
    @JsonProperty("bidCount")
    val bidCount: Int,

    /** Whether the reserve price has been met. */
    @JsonProperty("reserveMet")
    val reserveMet: Boolean,

    /** Number of anti-sniping extensions applied. */
    @JsonProperty("extensionCount")
    val extensionCount: Int,

    /** Identifier of the seller. */
    @JsonProperty("sellerId")
    val sellerId: String,

    /** UTC timestamp when the auction was created. */
    @JsonProperty("createdAt")
    val createdAt: Instant,

    /** UTC timestamp when the auction read model was last updated. */
    @JsonProperty("updatedAt")
    val updatedAt: Instant
)

/**
 * Summary response for auction listings (less detail than [AuctionDetailResponse]).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuctionSummaryResponse(

    /** Unique auction identifier (UUID). */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** Identifier of the lot being auctioned. */
    @JsonProperty("lotId")
    val lotId: String,

    /** Brand / tenant code. */
    @JsonProperty("brand")
    val brand: String,

    /** Current lifecycle status. */
    @JsonProperty("status")
    val status: String,

    /** Scheduled start time (UTC). */
    @JsonProperty("startTime")
    val startTime: Instant,

    /** Current end time (UTC). */
    @JsonProperty("endTime")
    val endTime: Instant,

    /** Current highest bid amount, null if no bids. */
    @JsonProperty("currentHighBid")
    val currentHighBid: BigDecimal?,

    /** Total number of bids placed. */
    @JsonProperty("bidCount")
    val bidCount: Int,

    /** Whether the reserve price has been met. */
    @JsonProperty("reserveMet")
    val reserveMet: Boolean
)

/**
 * Response for a single bid in the bid history.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BidResponse(

    /** Unique bid identifier. */
    @JsonProperty("bidId")
    val bidId: String,

    /** User ID of the bidder. */
    @JsonProperty("bidderId")
    val bidderId: String,

    /** Bid amount. */
    @JsonProperty("amount")
    val amount: BigDecimal,

    /** ISO 4217 currency code. */
    @JsonProperty("currency")
    val currency: String,

    /** Whether this bid was placed by the proxy-bid engine. */
    @JsonProperty("isProxy")
    val isProxy: Boolean,

    /** UTC timestamp when the bid was placed. */
    @JsonProperty("timestamp")
    val timestamp: Instant,

    /** Bid status (PLACED, OUTBID, WINNING). */
    @JsonProperty("status")
    val status: String
)

/**
 * Confirmation response returned after a bid is successfully placed.
 *
 * Provides the bidder with immediate feedback about the bid outcome,
 * including the new high bid, closing time (which may have been extended),
 * and reserve status.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BidConfirmationResponse(

    /** Unique identifier of the newly placed bid. */
    @JsonProperty("bidId")
    val bidId: String,

    /** Auction identifier. */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** The amount the bidder placed. */
    @JsonProperty("amount")
    val amount: BigDecimal,

    /** The new highest bid amount after this bid (may differ if proxy bids triggered). */
    @JsonProperty("newHighBid")
    val newHighBid: BigDecimal,

    /** Current closing time (may have been extended by anti-sniping). */
    @JsonProperty("closingTime")
    val closingTime: Instant,

    /** Reserve price status: "MET", "NOT_MET", or "NO_RESERVE". */
    @JsonProperty("reserveStatus")
    val reserveStatus: String,

    /** Whether an anti-sniping extension was triggered by this bid. */
    @JsonProperty("extensionApplied")
    val extensionApplied: Boolean
)

/**
 * Confirmation response returned after configuring an automatic (proxy) bid.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AutoBidConfirmationResponse(

    /** Auction identifier. */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** The configured maximum auto-bid amount. */
    @JsonProperty("maxAmount")
    val maxAmount: BigDecimal,

    /** The current effective bid amount from this auto-bid. */
    @JsonProperty("currentBidAmount")
    val currentBidAmount: BigDecimal,

    /** Whether the auto-bid is currently active. */
    @JsonProperty("active")
    val active: Boolean = true
)
