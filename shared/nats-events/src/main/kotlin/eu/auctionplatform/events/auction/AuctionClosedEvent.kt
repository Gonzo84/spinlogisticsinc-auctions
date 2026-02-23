package eu.auctionplatform.events.auction

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Emitted when an auction lot reaches its scheduled end time and no further
 * extensions are triggered.
 *
 * If [finalBid] is null, the lot received no bids and closed unsold.
 * If [reserveMet] is false, the lot closed below its reserve and the seller
 * may choose to accept or reject the highest offer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuctionClosedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "auction.closed",

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

    /** Auction session that closed. */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** Lot within the auction. */
    @JsonProperty("lotId")
    val lotId: String,

    /** Final highest bid amount; null if no bids were placed. */
    @JsonProperty("finalBid")
    val finalBid: BigDecimal? = null,

    /** User ID of the winning bidder; null if no bids were placed. */
    @JsonProperty("winnerId")
    val winnerId: String? = null,

    /** Total number of accepted bids placed on this lot. */
    @JsonProperty("bidCount")
    val bidCount: Int,

    /** Whether the reserve price was met at closing time. */
    @JsonProperty("reserveMet")
    val reserveMet: Boolean
) : BaseEvent()
