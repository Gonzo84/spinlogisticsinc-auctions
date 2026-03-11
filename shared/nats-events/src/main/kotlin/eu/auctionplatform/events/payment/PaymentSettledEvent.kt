package eu.auctionplatform.events.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Emitted when an admin settles a payment and the bank transfer to the
 * seller has been initiated or completed.
 *
 * Downstream consumers:
 * - seller-service: updates settlement row status from READY → PAID
 * - notification-service: sends payout confirmation to seller
 * - analytics-service: records commission revenue
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaymentSettledEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "payment.settlement.settled",

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

    /** Seller receiving the payout. */
    @JsonProperty("sellerId")
    val sellerId: String,

    /** Originating payment identifier from the checkout. */
    @JsonProperty("paymentId")
    val paymentId: String,

    /** Net amount paid to the seller (after commission deduction). */
    @JsonProperty("netAmount")
    val netAmount: BigDecimal,

    /** Platform commission amount withheld. */
    @JsonProperty("commission")
    val commission: BigDecimal,

    /** ISO 4217 currency code. */
    @JsonProperty("currency")
    val currency: String,

    /** Bank or PSP reference for the transfer. */
    @JsonProperty("bankReference")
    val bankReference: String,

    /** Timestamp when the settlement was actually completed. */
    @JsonProperty("settledAt")
    val settledAt: Instant
) : BaseEvent()
