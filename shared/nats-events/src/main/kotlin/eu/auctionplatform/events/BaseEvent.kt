package eu.auctionplatform.events

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import eu.auctionplatform.events.auction.*
import eu.auctionplatform.events.catalog.*
import eu.auctionplatform.events.compliance.GdprErasureEvent
import eu.auctionplatform.events.media.ImageProcessedEvent
import eu.auctionplatform.events.media.ImageUploadedEvent
import eu.auctionplatform.events.payment.CheckoutCompletedEvent
import eu.auctionplatform.events.payment.DepositPaidEvent
import eu.auctionplatform.events.payment.DepositRefundedEvent
import eu.auctionplatform.events.payment.LotRelistRequestedEvent
import eu.auctionplatform.events.payment.NonPaymentPenaltyEvent
import eu.auctionplatform.events.payment.PaymentSettledEvent
import eu.auctionplatform.events.payment.SettlementReadyEvent
import eu.auctionplatform.events.user.BuyerBlockedEvent
import eu.auctionplatform.events.user.KycVerifiedEvent
import eu.auctionplatform.events.user.UserRegisteredEvent
import java.time.Instant

/**
 * Contextual metadata attached to every domain event for tracing,
 * auditing, and debugging across the distributed auction platform.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventMetadata(
    @JsonProperty("traceId")
    val traceId: String,

    @JsonProperty("userId")
    val userId: String? = null,

    @JsonProperty("userAgent")
    val userAgent: String? = null,

    @JsonProperty("sourceIp")
    val sourceIp: String? = null
)

/**
 * Base class for all domain events emitted on the NATS event bus.
 *
 * Every concrete event is a Kotlin data class that extends this abstract class,
 * providing a consistent envelope with identity, aggregate correlation,
 * multi-brand support, optimistic-concurrency versioning, and optional
 * observability metadata.
 *
 * Field semantics:
 * - [eventId]        UUIDv7 — globally unique, time-sortable identifier.
 * - [eventType]      Dot-notation name, e.g. "auction.bid.placed".
 * - [aggregateId]    The primary entity this event mutates.
 * - [aggregateType]  Logical aggregate name, e.g. "Auction", "Lot", "User".
 * - [brand]          Tenant / white-label brand code.
 * - [timestamp]      Wall-clock time the event was produced (UTC).
 * - [version]        Monotonically increasing aggregate version for ordering.
 * - [metadata]       Optional trace / audit context.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "eventType",
    visible = true
)
@JsonSubTypes(
    // Auction domain
    JsonSubTypes.Type(value = BidPlacedEvent::class, name = "auction.bid.placed"),
    JsonSubTypes.Type(value = ProxyBidTriggeredEvent::class, name = "auction.bid.proxy_triggered"),
    JsonSubTypes.Type(value = AuctionExtendedEvent::class, name = "auction.extended"),
    JsonSubTypes.Type(value = AuctionClosedEvent::class, name = "auction.closed"),
    JsonSubTypes.Type(value = LotAwardedEvent::class, name = "auction.lot.awarded"),
    JsonSubTypes.Type(value = ReserveMetEvent::class, name = "auction.reserve.met"),
    JsonSubTypes.Type(value = BidRejectedEvent::class, name = "auction.bid.rejected"),
    JsonSubTypes.Type(value = DepositRequiredEvent::class, name = "auction.deposit.required"),
    JsonSubTypes.Type(value = AuctionCancelledEvent::class, name = "auction.cancelled"),
    // Catalog domain
    JsonSubTypes.Type(value = LotCreatedEvent::class, name = "catalog.lot.created"),
    JsonSubTypes.Type(value = LotUpdatedEvent::class, name = "catalog.lot.updated"),
    JsonSubTypes.Type(value = LotStatusChangedEvent::class, name = "catalog.lot.status_changed"),
    // Payment domain
    JsonSubTypes.Type(value = CheckoutCompletedEvent::class, name = "payment.checkout.completed"),
    JsonSubTypes.Type(value = SettlementReadyEvent::class, name = "payment.settlement.ready"),
    JsonSubTypes.Type(value = DepositPaidEvent::class, name = "payment.deposit.paid"),
    JsonSubTypes.Type(value = NonPaymentPenaltyEvent::class, name = "payment.non-payment.penalty"),
    JsonSubTypes.Type(value = LotRelistRequestedEvent::class, name = "payment.lot.relist-requested"),
    JsonSubTypes.Type(value = PaymentSettledEvent::class, name = "payment.settlement.settled"),
    JsonSubTypes.Type(value = DepositRefundedEvent::class, name = "payment.deposit.refunded"),
    // User domain
    JsonSubTypes.Type(value = UserRegisteredEvent::class, name = "user.registered"),
    JsonSubTypes.Type(value = KycVerifiedEvent::class, name = "user.kyc.verified"),
    JsonSubTypes.Type(value = BuyerBlockedEvent::class, name = "user.buyer.blocked"),
    // Media domain
    JsonSubTypes.Type(value = ImageUploadedEvent::class, name = "media.image.uploaded"),
    JsonSubTypes.Type(value = ImageProcessedEvent::class, name = "media.image.processed"),
    // Compliance domain
    JsonSubTypes.Type(value = GdprErasureEvent::class, name = "compliance.gdpr.erasure")
)
abstract class BaseEvent {
    abstract val eventId: String
    abstract val eventType: String
    abstract val aggregateId: String
    abstract val aggregateType: String
    abstract val brand: String
    abstract val timestamp: Instant
    abstract val version: Long
    abstract val metadata: EventMetadata?
}
