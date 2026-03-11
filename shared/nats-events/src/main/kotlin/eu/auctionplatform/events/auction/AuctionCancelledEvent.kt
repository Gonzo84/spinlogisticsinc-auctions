package eu.auctionplatform.events.auction

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.time.Instant

/**
 * Emitted when an admin cancels an auction before it closes naturally.
 *
 * Downstream consumers should release any held deposits and notify
 * affected bidders.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuctionCancelledEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "auction.cancelled",

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

    /** The auction that was cancelled. */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** The lot associated with the auction. */
    @JsonProperty("lotId")
    val lotId: String,

    /** Reason for cancellation. */
    @JsonProperty("reason")
    val reason: String,

    /** Admin user ID who cancelled the auction. */
    @JsonProperty("cancelledBy")
    val cancelledBy: String
) : BaseEvent()
