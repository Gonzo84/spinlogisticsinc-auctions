package eu.auctionplatform.events.auction

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Emitted the first time a bid meets or exceeds the seller's reserve price.
 *
 * This is a one-time event per lot — once the reserve is met, it is never
 * re-emitted even if subsequent bids push the price higher.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReserveMetEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "auction.reserve.met",

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

    /** Lot whose reserve was met. */
    @JsonProperty("lotId")
    val lotId: String,

    /** The seller's configured reserve price (not disclosed to bidders). */
    @JsonProperty("reservePrice")
    val reservePrice: BigDecimal,

    /** The bid amount that met or exceeded the reserve. */
    @JsonProperty("currentBid")
    val currentBid: BigDecimal,

    /** User ID of the bidder whose bid met the reserve. */
    @JsonProperty("bidderId")
    val bidderId: String
) : BaseEvent()
