package eu.auctionplatform.events.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Emitted when a user successfully pays a security deposit, unlocking
 * their ability to bid on deposit-gated lots or auction sessions.
 *
 * The auction service listens for this event to update the bidder's
 * eligibility in real time.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DepositPaidEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "payment.deposit.paid",

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

    /** Unique deposit identifier (same as aggregateId). */
    @JsonProperty("depositId")
    val depositId: String,

    /** User who paid the deposit. */
    @JsonProperty("userId")
    val userId: String,

    /** Deposit amount paid. */
    @JsonProperty("amount")
    val amount: BigDecimal,

    /** ISO 4217 currency code. */
    @JsonProperty("currency")
    val currency: String,

    /** Payment service provider transaction reference for reconciliation. */
    @JsonProperty("pspReference")
    val pspReference: String
) : BaseEvent()
