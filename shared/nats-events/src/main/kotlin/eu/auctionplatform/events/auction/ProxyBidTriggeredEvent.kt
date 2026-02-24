package eu.auctionplatform.events.auction

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Emitted when the proxy-bid engine automatically places a bid on behalf
 * of a user who has set a maximum bid ceiling.
 *
 * This event always follows a [BidPlacedEvent] from a competing bidder
 * that triggered the auto-bid evaluation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ProxyBidTriggeredEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "auction.bid.proxy",

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

    /** Unique identifier of the proxy bid that was placed. */
    @JsonProperty("bidId")
    val bidId: String,

    /** Auction session this proxy bid belongs to. */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** Lot within the auction. */
    @JsonProperty("lotId")
    val lotId: String,

    /** User on whose behalf the proxy bid was placed. */
    @JsonProperty("autoBidOwnerId")
    val autoBidOwnerId: String,

    /** Actual amount placed by the proxy engine (minimum increment above competing bid). */
    @JsonProperty("amount")
    val amount: BigDecimal,

    /** The user's configured maximum ceiling for proxy bidding. */
    @JsonProperty("maxAmount")
    val maxAmount: BigDecimal,

    /** ISO 4217 currency code. */
    @JsonProperty("currency")
    val currency: String,

    /** The competing bid that triggered this proxy bid evaluation. */
    @JsonProperty("triggeringBidId")
    val triggeringBidId: String
) : BaseEvent()
