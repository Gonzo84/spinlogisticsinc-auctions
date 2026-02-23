package eu.auctionplatform.events.auction

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Enumeration of reasons a bid may be rejected by the auction engine.
 */
enum class BidRejectionReason {
    /** Bid amount does not meet the minimum required increment above the current high bid. */
    BELOW_INCREMENT,

    /** The auction lot has already closed; no further bids are accepted. */
    AUCTION_CLOSED,

    /** The bidder's account has been blocked or suspended by an administrator. */
    USER_BLOCKED,

    /** The bidder has not paid the required deposit for this lot or auction. */
    DEPOSIT_REQUIRED,

    /** The seller (lot owner) is not permitted to bid on their own lot. */
    SELLER_CANNOT_BID
}

/**
 * Emitted when a bid attempt is rejected by the auction engine.
 *
 * This event is consumed by the bidding UI to display rejection feedback
 * and by the monitoring/analytics pipeline for fraud and compliance tracking.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BidRejectedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "auction.bid.rejected",

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

    /** Lot the bid was attempted on. */
    @JsonProperty("lotId")
    val lotId: String,

    /** User who attempted the bid. */
    @JsonProperty("bidderId")
    val bidderId: String,

    /** The rejected bid amount. */
    @JsonProperty("amount")
    val amount: BigDecimal,

    /** Machine-readable reason for the rejection. */
    @JsonProperty("reason")
    val reason: BidRejectionReason
) : BaseEvent()
