package eu.auctionplatform.events.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Emitted when a bidder's deposit is refunded after losing an auction
 * or after an auction is cancelled.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DepositRefundedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "payment.deposit.refunded",

    @JsonProperty("aggregateId")
    override val aggregateId: String,

    @JsonProperty("aggregateType")
    override val aggregateType: String = "Deposit",

    @JsonProperty("brand")
    override val brand: String,

    @JsonProperty("timestamp")
    override val timestamp: Instant,

    @JsonProperty("version")
    override val version: Long,

    @JsonProperty("metadata")
    override val metadata: EventMetadata? = null,

    /** The deposit that was refunded. */
    @JsonProperty("depositId")
    val depositId: String,

    /** The user receiving the refund. */
    @JsonProperty("userId")
    val userId: String,

    /** Refund amount. */
    @JsonProperty("amount")
    val amount: BigDecimal,

    /** ISO 4217 currency code. */
    @JsonProperty("currency")
    val currency: String = "EUR",

    /** PSP reference for the refund transaction, if available. */
    @JsonProperty("pspReference")
    val pspReference: String? = null
) : BaseEvent()
