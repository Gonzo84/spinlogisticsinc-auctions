package eu.auctionplatform.events.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.time.Instant

/**
 * Emitted when a lot should be relisted after the winning buyer fails
 * to complete payment.
 *
 * The auction engine or catalog service listens for this event to
 * return the lot to the seller for relisting.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class LotRelistRequestedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "payment.lot.relist-requested",

    @JsonProperty("aggregateId")
    override val aggregateId: String,

    @JsonProperty("aggregateType")
    override val aggregateType: String = "Lot",

    @JsonProperty("brand")
    override val brand: String,

    @JsonProperty("timestamp")
    override val timestamp: Instant,

    @JsonProperty("version")
    override val version: Long,

    @JsonProperty("metadata")
    override val metadata: EventMetadata? = null,

    /** The lot to be relisted. */
    @JsonProperty("lotId")
    val lotId: String,

    /** The auction the lot was originally in. */
    @JsonProperty("auctionId")
    val auctionId: String,

    /** The payment that triggered the relist (non-payment). */
    @JsonProperty("paymentId")
    val paymentId: String,

    /** Reason for relisting. */
    @JsonProperty("reason")
    val reason: String = "NON_PAYMENT"
) : BaseEvent()
