package eu.auctionplatform.commons.dto

/**
 * Standard error response returned by all API endpoints.
 *
 * @property code     Machine-readable error code (e.g. "LOT_NOT_FOUND").
 * @property message  Human-readable description of the error.
 * @property details  Optional structured details (validation errors, context, etc.).
 * @property traceId  Optional distributed trace identifier for correlation in logs.
 */
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null,
    val traceId: String? = null
)
