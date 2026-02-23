package eu.auctionplatform.events.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.time.Instant

/**
 * Type of account: business entities or private individuals.
 * EU B2B platforms often require different VAT and KYC treatment per type.
 */
enum class AccountType {
    BUSINESS,
    PRIVATE
}

/**
 * Emitted when a new user completes registration on the platform.
 *
 * Downstream consumers use this to trigger the KYC verification flow,
 * create the user's wallet / deposit account, and send a welcome email.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserRegisteredEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "user.registered",

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

    /** User identifier (same as aggregateId). */
    @JsonProperty("userId")
    val userId: String,

    /** Account classification for VAT / KYC purposes. */
    @JsonProperty("accountType")
    val accountType: AccountType,

    /** User's email address. */
    @JsonProperty("email")
    val email: String,

    /** Preferred language (ISO 639-1 code, e.g., "nl", "de", "fr"). */
    @JsonProperty("language")
    val language: String,

    /** ISO 3166-1 alpha-2 country code of the user's primary address. */
    @JsonProperty("country")
    val country: String,

    /** Company name; required when accountType is BUSINESS. */
    @JsonProperty("companyName")
    val companyName: String? = null,

    /** EU VAT identification number; required when accountType is BUSINESS. */
    @JsonProperty("vatId")
    val vatId: String? = null
) : BaseEvent()
