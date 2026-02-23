package eu.auctionplatform.events.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Emitted when a buyer successfully completes the checkout and payment
 * for an awarded lot.
 *
 * This event triggers settlement processing, receipt generation, and
 * logistics / handover coordination.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CheckoutCompletedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "payment.checkout.completed",

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

    /** Unique payment identifier (same as aggregateId). */
    @JsonProperty("paymentId")
    val paymentId: String,

    /** User ID of the buyer who completed checkout. */
    @JsonProperty("buyerId")
    val buyerId: String,

    /** Lot that was purchased. */
    @JsonProperty("lotId")
    val lotId: String,

    /** Auction session the lot belonged to. */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** Hammer price (winning bid amount). */
    @JsonProperty("hammerPrice")
    val hammerPrice: BigDecimal,

    /** Buyer premium amount charged on top of the hammer price. */
    @JsonProperty("buyerPremium")
    val buyerPremium: BigDecimal,

    /** VAT amount calculated per applicable EU jurisdiction rules. */
    @JsonProperty("vatAmount")
    val vatAmount: BigDecimal,

    /** Total charged: hammerPrice + buyerPremium + vatAmount. */
    @JsonProperty("totalAmount")
    val totalAmount: BigDecimal,

    /** ISO 4217 currency code. */
    @JsonProperty("currency")
    val currency: String,

    /** Payment method used (e.g., "BANK_TRANSFER", "CARD", "SEPA_DIRECT_DEBIT"). */
    @JsonProperty("paymentMethod")
    val paymentMethod: String
) : BaseEvent()
