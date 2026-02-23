package eu.auctionplatform.events.auction

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Emitted when a lot is formally awarded to the winning bidder after the
 * auction closes and all post-close validations pass.
 *
 * This event triggers downstream processes: invoice generation, checkout
 * flow initiation, and buyer notification.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class LotAwardedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "auction.lot.awarded",

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

    /** Auction session the lot belongs to. */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** Lot that was awarded. */
    @JsonProperty("lotId")
    val lotId: String,

    /** User ID of the winning bidder. */
    @JsonProperty("winnerId")
    val winnerId: String,

    /** Final hammer price the lot sold for. */
    @JsonProperty("hammerPrice")
    val hammerPrice: BigDecimal,

    /** ISO 4217 currency code. */
    @JsonProperty("currency")
    val currency: String,

    /** Buyer premium rate applied (e.g., 0.15 = 15%). */
    @JsonProperty("buyerPremiumRate")
    val buyerPremiumRate: BigDecimal
) : BaseEvent()
