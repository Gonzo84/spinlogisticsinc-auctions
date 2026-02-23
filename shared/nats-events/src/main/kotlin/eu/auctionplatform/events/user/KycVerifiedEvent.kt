package eu.auctionplatform.events.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.time.Instant

/**
 * Emitted when a user's Know Your Customer (KYC) verification check
 * completes — whether approved, rejected, or requiring manual review.
 *
 * The user service updates the user's verified status, and the auction
 * service uses this to gate bidding eligibility.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class KycVerifiedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "user.kyc.verified",

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

    /** User whose KYC check completed (same as aggregateId). */
    @JsonProperty("userId")
    val userId: String,

    /** KYC provider name (e.g., "onfido", "sumsub", "veriff"). */
    @JsonProperty("provider")
    val provider: String,

    /** External check ID from the KYC provider. */
    @JsonProperty("checkId")
    val checkId: String,

    /** Outcome of the KYC check (e.g., "APPROVED", "REJECTED", "PENDING_REVIEW"). */
    @JsonProperty("status")
    val status: String
) : BaseEvent()
