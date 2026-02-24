package eu.auctionplatform.events.auction

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.time.Instant

/**
 * Emitted when the auction end time is extended due to an anti-sniping rule.
 *
 * Most EU B2B platforms apply a "soft close" policy: if a bid arrives within
 * the final N minutes of the auction, the closing time is pushed forward to
 * allow competing bidders to respond.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuctionExtendedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "auction.lot.extended",

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

    /** Auction session whose closing time was extended. */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** Lot within the auction. */
    @JsonProperty("lotId")
    val lotId: String,

    /** Previously scheduled end time before the extension. */
    @JsonProperty("oldEndTime")
    val oldEndTime: Instant,

    /** New end time after the anti-sniping extension was applied. */
    @JsonProperty("newEndTime")
    val newEndTime: Instant,

    /** The bid that triggered the anti-sniping extension rule. */
    @JsonProperty("triggeringBidId")
    val triggeringBidId: String,

    /** Running count of extensions applied to this lot in the current auction. */
    @JsonProperty("extensionCount")
    val extensionCount: Int
) : BaseEvent()
