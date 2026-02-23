package eu.auctionplatform.events.catalog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.time.Instant

/**
 * Emitted when a lot transitions from one lifecycle status to another.
 *
 * Typical flow: DRAFT -> PENDING_REVIEW -> APPROVED -> LIVE -> SOLD / UNSOLD / WITHDRAWN
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class LotStatusChangedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "catalog.lot.status_changed",

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

    /** Lot whose status changed (same as aggregateId). */
    @JsonProperty("lotId")
    val lotId: String,

    /** Previous lifecycle status. */
    @JsonProperty("oldStatus")
    val oldStatus: String,

    /** New lifecycle status. */
    @JsonProperty("newStatus")
    val newStatus: String,

    /** Human-readable reason for the transition (e.g., "Approved by moderator", "Withdrawn by seller"). */
    @JsonProperty("reason")
    val reason: String? = null
) : BaseEvent()
