package eu.auctionplatform.gateway.api.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

// =============================================================================
// Webhook Payload DTOs – Adyen and Onfido
// =============================================================================

// -----------------------------------------------------------------------------
// Adyen Webhook DTOs
// -----------------------------------------------------------------------------

/**
 * Top-level Adyen webhook notification payload.
 *
 * Adyen sends a JSON body containing a `notificationItems` array, each wrapping
 * a [AdyenNotificationItem] inside a `NotificationRequestItem` envelope.
 *
 * @see <a href="https://docs.adyen.com/development-resources/webhooks">Adyen Webhooks</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AdyenWebhookPayload(
    @JsonProperty("live")
    val live: String? = null,

    @JsonProperty("notificationItems")
    val notificationItems: List<AdyenNotificationItemWrapper> = emptyList()
)

/**
 * Wrapper object for a single Adyen notification item.
 * Adyen nests each item inside a `NotificationRequestItem` key.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AdyenNotificationItemWrapper(
    @JsonProperty("NotificationRequestItem")
    val notificationRequestItem: AdyenNotificationItem
)

/**
 * Individual Adyen notification item containing payment event details.
 *
 * Key fields:
 * - [eventCode] -- the event type (e.g. "AUTHORISATION", "CAPTURE", "REFUND")
 * - [pspReference] -- Adyen's unique payment reference (used as event ID for dedup)
 * - [success] -- whether the operation was successful
 * - [additionalData] -- contains HMAC signature and other metadata
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AdyenNotificationItem(
    @JsonProperty("eventCode")
    val eventCode: String,

    @JsonProperty("success")
    val success: String = "false",

    @JsonProperty("pspReference")
    val pspReference: String,

    @JsonProperty("originalReference")
    val originalReference: String? = null,

    @JsonProperty("merchantAccountCode")
    val merchantAccountCode: String? = null,

    @JsonProperty("merchantReference")
    val merchantReference: String? = null,

    @JsonProperty("amount")
    val amount: AdyenAmount? = null,

    @JsonProperty("paymentMethod")
    val paymentMethod: String? = null,

    @JsonProperty("reason")
    val reason: String? = null,

    @JsonProperty("eventDate")
    val eventDate: String? = null,

    @JsonProperty("additionalData")
    val additionalData: Map<String, String>? = null
) {
    /**
     * Extracts the HMAC signature from the `additionalData` map.
     * Adyen places the signature under the key `hmacSignature`.
     */
    fun hmacSignature(): String? = additionalData?.get("hmacSignature")
}

/**
 * Adyen amount representation with currency and value (in minor units).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AdyenAmount(
    @JsonProperty("currency")
    val currency: String = "USD",

    @JsonProperty("value")
    val value: Long = 0
)

// -----------------------------------------------------------------------------
// Onfido Webhook DTOs
// -----------------------------------------------------------------------------

/**
 * Onfido webhook payload for identity verification events.
 *
 * Onfido sends events when KYC checks complete, documents are processed, or
 * reports are generated.
 *
 * @see <a href="https://documentation.onfido.com/api/latest/#webhooks">Onfido Webhooks</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OnfidoWebhookPayload(
    @JsonProperty("payload")
    val payload: OnfidoEventPayload
)

/**
 * Inner payload of an Onfido webhook event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OnfidoEventPayload(
    @JsonProperty("resource_type")
    val resourceType: String,

    @JsonProperty("action")
    val action: String,

    @JsonProperty("object")
    val eventObject: OnfidoEventObject
)

/**
 * Object details within an Onfido webhook event.
 *
 * Contains the resource identifier, status, and timestamps needed to
 * process verification outcomes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OnfidoEventObject(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("status")
    val status: String? = null,

    @JsonProperty("completed_at_iso8601")
    val completedAt: String? = null,

    @JsonProperty("href")
    val href: String? = null
)

// -----------------------------------------------------------------------------
// Shared / Internal DTOs
// -----------------------------------------------------------------------------

/**
 * Standardised webhook acknowledgement response returned to webhook providers.
 *
 * Most providers expect a `[accepted]` response with HTTP 200 to confirm
 * receipt. Returning the [eventId] helps with debugging and log correlation.
 */
data class WebhookAckResponse(
    val accepted: Boolean = true,
    val eventId: String? = null,
    val message: String? = null
)

/**
 * Internal representation of a webhook event after validation, used for
 * publishing to NATS streams.
 */
data class ValidatedWebhookEvent(
    val eventId: String,
    val source: String,
    val eventType: String,
    val payload: String,
    val receivedAt: Instant = Instant.now()
)
