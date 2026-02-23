package eu.auctionplatform.events.catalog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.time.Instant

/**
 * Emitted when one or more fields of a lot are updated by the seller
 * or by an administrator.
 *
 * Only the changed fields are included in [updatedFields] to keep
 * the payload minimal and to support partial-update projections.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class LotUpdatedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "catalog.lot.updated",

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

    /** Lot that was updated (same as aggregateId). */
    @JsonProperty("lotId")
    val lotId: String,

    /** Map of field name to new value for every field that changed. */
    @JsonProperty("updatedFields")
    val updatedFields: Map<String, Any>
) : BaseEvent()
