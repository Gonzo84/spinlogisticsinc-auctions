package eu.auctionplatform.events.auction

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Emitted whenever a valid bid is accepted and recorded against a lot.
 *
 * Downstream consumers use this event to:
 * - Update the live bidding UI via WebSocket fan-out.
 * - Trigger proxy-bid evaluation for competing auto-bidders.
 * - Evaluate anti-sniping extension rules.
 * - Feed the bid-history projection.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BidPlacedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "auction.bid.placed",

    @JsonProperty("aggregateId")
    override val aggregateId: String,

    @JsonProperty("aggregateType")
    override val aggregateType: String = "Auction",

    @JsonProperty("brand")
    override val brand: String,

    @JsonProperty("timestamp")
    override val timestamp: Instant,

    @JsonProperty("version")
    override val version: Long,

    @JsonProperty("metadata")
    override val metadata: EventMetadata? = null,

    /** Unique identifier of the newly placed bid. */
    @JsonProperty("bidId")
    val bidId: String,

    /** Auction session this bid belongs to. */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** Specific lot within the auction. */
    @JsonProperty("lotId")
    val lotId: String,

    /** User who placed the bid. */
    @JsonProperty("bidderId")
    val bidderId: String,

    /** Bid amount in the lot's settlement currency. */
    @JsonProperty("amount")
    val amount: BigDecimal,

    /** ISO 4217 currency code, e.g. "USD". */
    @JsonProperty("currency")
    val currency: String,

    /** The highest bid amount before this bid was placed; null for the opening bid. */
    @JsonProperty("previousHighBid")
    val previousHighBid: BigDecimal? = null,

    /** Bidder ID of the previous high bidder; null for the opening bid. */
    @JsonProperty("previousHighBidderId")
    val previousHighBidderId: String? = null,

    /** True when this bid was automatically placed by the proxy-bid engine. */
    @JsonProperty("isProxy")
    val isProxy: Boolean = false,

    /** Running total of accepted bids on this lot. */
    @JsonProperty("bidCount")
    val bidCount: Int,

    /** Whether the lot's reserve price has been met after this bid. */
    @JsonProperty("reserveMet")
    val reserveMet: Boolean
) : BaseEvent()
