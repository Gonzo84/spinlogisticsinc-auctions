package eu.auctionplatform.auction.domain.event

import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.auction.domain.model.BidRejectionReason
import eu.auctionplatform.commons.domain.DomainEvent
import java.math.BigDecimal
import java.time.Instant

// =============================================================================
// Base class for auction domain events
// =============================================================================

/**
 * Abstract base for all domain events originating from the Auction aggregate.
 *
 * Provides the common metadata fields required by [DomainEvent] and fixes
 * [aggregateType] to `"Auction"` for all subclasses.
 */
sealed class AuctionEvent(
    override val eventId: String,
    override val eventType: String,
    override val aggregateId: String,
    override val aggregateType: String = AGGREGATE_TYPE,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long
) : DomainEvent {

    companion object {
        const val AGGREGATE_TYPE: String = "Auction"
    }
}

// =============================================================================
// Auction lifecycle events
// =============================================================================

/**
 * Raised when a new auction is created via [CreateAuctionCommand].
 *
 * This event carries the full initial state of the auction and is always the
 * first event in an auction's event stream (version 1).
 */
data class AuctionCreatedEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val lotId: String,
    val startTime: Instant,
    val endTime: Instant,
    val startingBidAmount: BigDecimal,
    val startingBidCurrency: String,
    val reservePriceAmount: BigDecimal?,
    val reservePriceCurrency: String?,
    val sellerId: String
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    companion object {
        const val EVENT_TYPE: String = "AuctionCreatedEvent"
    }
}

/**
 * Raised when the auction's end time is extended due to the anti-sniping rule.
 *
 * This event records both the previous and new end times, as well as the
 * extension count, enabling audit trails and replay consistency.
 */
data class AuctionExtendedEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val previousEndTime: Instant,
    val newEndTime: Instant,
    val extensionCount: Int,
    val triggeringBidId: String
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    companion object {
        const val EVENT_TYPE: String = "AuctionExtendedEvent"
    }
}

/**
 * Raised when the auction bidding period ends and the auction transitions
 * to [AuctionStatus.CLOSED].
 */
data class AuctionClosedEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val finalBidAmount: BigDecimal?,
    val finalBidCurrency: String?,
    val winnerId: String?,
    val totalBids: Int,
    val reserveMet: Boolean
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    companion object {
        const val EVENT_TYPE: String = "AuctionClosedEvent"
    }
}

/**
 * Raised when the lot is formally awarded to the winning bidder after
 * the auction has closed and all post-close validations have passed.
 */
data class LotAwardedEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val lotId: String,
    val winnerId: String,
    val winningBidAmount: BigDecimal,
    val winningBidCurrency: String,
    val winningBidId: String,
    override val metadata: Map<String, String>? = null,
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    /** Alias for [aggregateId] so downstream NATS consumers can read `auctionId`. */
    @get:JsonProperty("auctionId")
    val auctionId: String get() = aggregateId

    /** Alias for [winningBidAmount] so downstream NATS consumers can read `hammerPrice`. */
    @get:JsonProperty("hammerPrice")
    val hammerPrice: BigDecimal get() = winningBidAmount

    /** Alias for [winningBidCurrency] so downstream NATS consumers can read `currency`. */
    @get:JsonProperty("currency")
    val currency: String get() = winningBidCurrency

    companion object {
        const val EVENT_TYPE: String = "LotAwardedEvent"
    }
}

/**
 * Raised when an auction is cancelled before or after closing but before
 * the lot is awarded.
 */
data class AuctionCancelledEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val reason: String,
    val cancelledBy: String?
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    companion object {
        const val EVENT_TYPE: String = "AuctionCancelledEvent"
    }
}

// =============================================================================
// Bid events
// =============================================================================

/**
 * Raised when a valid bid is placed on the auction, either directly by
 * a user or by the proxy-bid engine.
 */
data class BidPlacedEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val bidId: String,
    val bidderId: String,
    val bidAmount: BigDecimal,
    val bidCurrency: String,
    val isProxy: Boolean,
    val previousHighBidAmount: BigDecimal?,
    val previousHighBidCurrency: String?,
    val previousHighBidderId: String?
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    companion object {
        const val EVENT_TYPE: String = "BidPlacedEvent"
    }
}

/**
 * Raised when the proxy-bid engine generates a counter-bid on behalf of
 * a user with an active auto-bid.
 *
 * This is semantically distinct from [BidPlacedEvent] to enable separate
 * notification handling (e.g. "You were outbid, but your auto-bid countered").
 */
data class ProxyBidTriggeredEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val bidId: String,
    val bidderId: String,
    val bidAmount: BigDecimal,
    val bidCurrency: String,
    val triggeringBidId: String,
    val maxAutoBidAmount: BigDecimal,
    val maxAutoBidCurrency: String
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    companion object {
        const val EVENT_TYPE: String = "ProxyBidTriggeredEvent"
    }
}

/**
 * Raised when a bid is rejected due to a validation failure.
 */
data class BidRejectedEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val bidderId: String,
    val attemptedAmount: BigDecimal,
    val attemptedCurrency: String,
    val reason: BidRejectionReason,
    val message: String
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    companion object {
        const val EVENT_TYPE: String = "BidRejectedEvent"
    }
}

// =============================================================================
// Auto-bid events
// =============================================================================

/**
 * Raised when a user configures or updates their auto-bid on an auction.
 */
data class AutoBidSetEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val bidderId: String,
    val maxAmount: BigDecimal,
    val maxAmountCurrency: String
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    companion object {
        const val EVENT_TYPE: String = "AutoBidSetEvent"
    }
}

/**
 * Raised when an auto-bid is exhausted because the competing bid exceeds
 * the auto-bid's maximum amount.
 */
data class AutoBidExhaustedEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val bidderId: String,
    val maxAmount: BigDecimal,
    val maxAmountCurrency: String,
    val competingBidAmount: BigDecimal,
    val competingBidCurrency: String
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    companion object {
        const val EVENT_TYPE: String = "AutoBidExhaustedEvent"
    }
}

// =============================================================================
// Reserve events
// =============================================================================

/**
 * Raised when a bid meets or exceeds the auction's reserve price for
 * the first time.
 */
data class ReserveMetEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val bidId: String,
    val bidAmount: BigDecimal,
    val bidCurrency: String,
    val reserveAmount: BigDecimal,
    val reserveCurrency: String
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    companion object {
        const val EVENT_TYPE: String = "ReserveMetEvent"
    }
}

// =============================================================================
// Featured events
// =============================================================================

/**
 * Raised when an admin marks an active auction as "featured" for homepage promotion.
 */
data class AuctionFeaturedEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val featuredBy: String,
    val featuredAt: Instant,
    override val metadata: Map<String, String>? = null,
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    companion object {
        const val EVENT_TYPE: String = "AuctionFeaturedEvent"
    }
}

/**
 * Raised when an admin revokes an award, reverting the auction to CLOSED status.
 */
data class AwardRevokedEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val revokedBy: String,
    val reason: String,
    val originalWinnerId: String,
    val originalHammerPrice: BigDecimal,
    override val metadata: Map<String, String>? = null,
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    companion object {
        const val EVENT_TYPE: String = "AwardRevokedEvent"
    }
}

/**
 * Raised when an admin removes the "featured" flag from an auction.
 */
data class AuctionUnfeaturedEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val unfeaturedBy: String,
    override val metadata: Map<String, String>? = null,
) : AuctionEvent(
    eventId = eventId,
    eventType = EVENT_TYPE,
    aggregateId = aggregateId,
    brand = brand,
    timestamp = timestamp,
    version = version
) {
    companion object {
        const val EVENT_TYPE: String = "AuctionUnfeaturedEvent"
    }
}
