package eu.auctionplatform.events.compliance

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.time.Instant

/**
 * Type of GDPR data subject request.
 */
enum class GdprRequestType {
    /** Right of access — export all personal data held about the user. */
    EXPORT,

    /** Right to erasure ("right to be forgotten") — delete all personal data. */
    ERASURE
}

/**
 * Emitted when a GDPR data subject request (export or erasure) is processed.
 *
 * All services that store personal data must listen for ERASURE events and
 * purge or anonymise the affected user's data within the legally mandated
 * timeframe (typically 30 days under GDPR Article 17).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class GdprErasureEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "compliance.gdpr.erasure",

    @JsonProperty("aggregateId")
    override val aggregateId: String,

    @JsonProperty("aggregateType")
    override val aggregateType: String = "GdprRequest",

    @JsonProperty("brand")
    override val brand: String,

    @JsonProperty("timestamp")
    override val timestamp: Instant,

    @JsonProperty("version")
    override val version: Long,

    @JsonProperty("metadata")
    override val metadata: EventMetadata? = null,

    /** Unique request identifier (same as aggregateId). */
    @JsonProperty("requestId")
    val requestId: String,

    /** User ID of the data subject. */
    @JsonProperty("userId")
    val userId: String,

    /** Type of GDPR request: data export or data erasure. */
    @JsonProperty("requestType")
    val requestType: GdprRequestType,

    /** Current processing status (e.g., "PENDING", "IN_PROGRESS", "COMPLETED", "FAILED"). */
    @JsonProperty("status")
    val status: String,

    /** User ID of the administrator or DPO who approved the request. */
    @JsonProperty("approvedBy")
    val approvedBy: String? = null
) : BaseEvent()
