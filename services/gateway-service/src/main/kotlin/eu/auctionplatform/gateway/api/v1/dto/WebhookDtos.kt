package eu.auctionplatform.gateway.api.v1.dto

import java.time.Instant

// =============================================================================
// Webhook DTOs – Additional request/response types for webhook processing
// =============================================================================

/**
 * Generic webhook receipt response returned after processing any webhook.
 * Includes deduplication status for idempotent handling.
 */
data class WebhookReceiptResponse(
    val accepted: Boolean = true,
    val eventId: String? = null,
    val duplicate: Boolean = false,
    val message: String? = null
)

/**
 * Internal event envelope published to NATS after a webhook has been
 * validated, deduplicated, and acknowledged.
 */
data class WebhookNatsEvent(
    val eventId: String,
    val source: String,
    val eventType: String,
    val payload: String,
    val receivedAt: Instant = Instant.now()
)

/**
 * Health check response DTO.
 */
data class HealthResponse(
    val status: String,
    val service: String = "gateway-service",
    val timestamp: Instant = Instant.now(),
    val checks: Map<String, ComponentHealth> = emptyMap()
)

/**
 * Individual component health status.
 */
data class ComponentHealth(
    val status: String,
    val details: Map<String, Any>? = null
)
