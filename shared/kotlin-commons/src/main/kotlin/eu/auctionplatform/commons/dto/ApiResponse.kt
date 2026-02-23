package eu.auctionplatform.commons.dto

/**
 * Standard envelope for all successful API responses.
 *
 * @param T        The type of the payload.
 * @property data  The response payload. Null for 204-style "no content" responses.
 * @property meta  Optional metadata (pagination cursors, rate-limit info, etc.).
 */
data class ApiResponse<T>(
    val data: T? = null,
    val meta: Map<String, Any>? = null
) {
    companion object {

        /** Wraps a non-null [data] payload with optional [meta]. */
        fun <T> ok(data: T, meta: Map<String, Any>? = null): ApiResponse<T> =
            ApiResponse(data = data, meta = meta)

        /** Returns an empty response (no payload, no meta). */
        fun <T> empty(): ApiResponse<T> = ApiResponse()
    }
}
