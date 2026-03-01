package eu.auctionplatform.events.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Emitted when a buyer is penalised for failing to complete payment
 * within the due date.
 *
 * The user service listens for this event to block the buyer from
 * further bidding until the penalty is resolved.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NonPaymentPenaltyEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "payment.non-payment.penalty",

    @JsonProperty("aggregateId")
    override val aggregateId: String,

    @JsonProperty("aggregateType")
    override val aggregateType: String = "Payment",

    @JsonProperty("brand")
    override val brand: String,

    @JsonProperty("timestamp")
    override val timestamp: Instant,

    @JsonProperty("version")
    override val version: Long,

    @JsonProperty("metadata")
    override val metadata: EventMetadata? = null,

    /** The overdue payment identifier (same as aggregateId). */
    @JsonProperty("paymentId")
    val paymentId: String,

    /** The buyer who failed to pay. */
    @JsonProperty("buyerId")
    val buyerId: String,

    /** The lot that was not paid for. */
    @JsonProperty("lotId")
    val lotId: String,

    /** The auction the lot belonged to. */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** The 25% forfeit penalty amount. */
    @JsonProperty("forfeitAmount")
    val forfeitAmount: BigDecimal,

    /** ISO 4217 currency code. */
    @JsonProperty("currency")
    val currency: String
) : BaseEvent()
