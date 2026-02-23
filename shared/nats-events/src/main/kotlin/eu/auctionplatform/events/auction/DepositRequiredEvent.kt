package eu.auctionplatform.events.auction

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Emitted when a bidder attempts to place a bid on a lot that requires a
 * security deposit and the bidder has not yet paid it.
 *
 * The checkout / payment service listens for this event to present the
 * deposit payment flow to the user before they can bid.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DepositRequiredEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "auction.deposit.required",

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

    /** Auction session. */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** Lot the bidder attempted to bid on. */
    @JsonProperty("lotId")
    val lotId: String,

    /** User who needs to pay the deposit. */
    @JsonProperty("bidderId")
    val bidderId: String,

    /** The bid amount the user attempted to place. */
    @JsonProperty("bidAmount")
    val bidAmount: BigDecimal,

    /** The deposit amount required before the user can bid. */
    @JsonProperty("depositAmount")
    val depositAmount: BigDecimal
) : BaseEvent()
