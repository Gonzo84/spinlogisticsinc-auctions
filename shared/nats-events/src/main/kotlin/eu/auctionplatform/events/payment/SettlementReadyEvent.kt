package eu.auctionplatform.events.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Emitted when funds from a completed checkout have cleared and the
 * seller payout can be initiated.
 *
 * The finance / treasury service listens for this event to schedule
 * the actual bank transfer to the seller's account.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SettlementReadyEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "payment.settlement.ready",

    @JsonProperty("aggregateId")
    override val aggregateId: String,

    @JsonProperty("aggregateType")
    override val aggregateType: String = "Settlement",

    @JsonProperty("brand")
    override val brand: String,

    @JsonProperty("timestamp")
    override val timestamp: Instant,

    @JsonProperty("version")
    override val version: Long,

    @JsonProperty("metadata")
    override val metadata: EventMetadata? = null,

    /** Unique settlement identifier (same as aggregateId). */
    @JsonProperty("settlementId")
    val settlementId: String,

    /** Seller to be paid out. */
    @JsonProperty("sellerId")
    val sellerId: String,

    /** Originating payment identifier from the checkout. */
    @JsonProperty("paymentId")
    val paymentId: String,

    /** Net amount to be paid to the seller (after commission deduction). */
    @JsonProperty("netAmount")
    val netAmount: BigDecimal,

    /** Platform commission amount withheld from the seller payout. */
    @JsonProperty("commission")
    val commission: BigDecimal,

    /** ISO 4217 currency code. */
    @JsonProperty("currency")
    val currency: String
) : BaseEvent()
