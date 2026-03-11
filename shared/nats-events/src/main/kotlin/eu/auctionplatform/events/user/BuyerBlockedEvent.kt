package eu.auctionplatform.events.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.time.Instant

/**
 * Emitted when a buyer's account is blocked by an admin due to
 * fraud, non-payment, or policy violation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class BuyerBlockedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "user.buyer.blocked",

    @JsonProperty("aggregateId")
    override val aggregateId: String,

    @JsonProperty("aggregateType")
    override val aggregateType: String = "User",

    @JsonProperty("brand")
    override val brand: String,

    @JsonProperty("timestamp")
    override val timestamp: Instant,

    @JsonProperty("version")
    override val version: Long,

    @JsonProperty("metadata")
    override val metadata: EventMetadata? = null,

    /** The blocked user's ID. */
    @JsonProperty("userId")
    val userId: String,

    /** Reason for the block. */
    @JsonProperty("reason")
    val reason: String,

    /** Admin user ID who initiated the block. */
    @JsonProperty("blockedBy")
    val blockedBy: String,

    /** When the block expires. Null means permanent. */
    @JsonProperty("blockedUntil")
    val blockedUntil: Instant? = null
) : BaseEvent()
