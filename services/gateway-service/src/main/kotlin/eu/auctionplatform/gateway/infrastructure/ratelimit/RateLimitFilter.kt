package eu.auctionplatform.gateway.infrastructure.ratelimit

import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory

// =============================================================================
// Rate Limit Filter – JAX-RS request filter for enforcing rate limits
// =============================================================================

/**
 * JAX-RS [ContainerRequestFilter] that enforces per-user rate limiting based on
 * the request path.
 *
 * The filter reads the authenticated user's identity from the JAX-RS
 * [SecurityContext] (populated by the Quarkus OIDC / JWT subsystem). Anonymous
 * requests that reach this filter are rate-limited by remote IP address as a
 * fallback.
 *
 * Rate limit tiers are applied based on the request URI:
 * - `/api/v1/auctions/{id}/bids` (POST) -- bid placement limit (10/min per auction)
 * - `/api/v1/search/**` -- search limit (30/min)
 * - Everything else -- general API limit (100/min)
 *
 * When a limit is exceeded the filter aborts the request with **429 Too Many
 * Requests** and includes standard `Retry-After` and `X-RateLimit-*` headers.
 */
@Provider
@Priority(Priorities.USER - 100) // Run early, before business logic filters
class RateLimitFilter @Inject constructor(
    private val rateLimitService: RateLimitService
) : ContainerRequestFilter {

    private val logger = LoggerFactory.getLogger(RateLimitFilter::class.java)

    @ConfigProperty(name = "rate-limit.enabled", defaultValue = "true")
    var enabled: Boolean = true

    companion object {
        /** Regex to extract auctionId from bid placement paths. */
        private val BID_PATH_PATTERN = Regex("/api/v1/auctions/([^/]+)/bids")

        /** Prefix for search endpoints. */
        private const val SEARCH_PATH_PREFIX = "/api/v1/search"

        /** Webhook paths are excluded from rate limiting. */
        private const val WEBHOOK_PATH_PREFIX = "/api/v1/webhooks"

        /** Health check paths are excluded. */
        private const val HEALTH_PATH_PREFIX = "/api/v1/health"
    }

    override fun filter(requestContext: ContainerRequestContext) {
        if (!enabled) {
            return
        }

        val path = requestContext.uriInfo.path
        val method = requestContext.method

        // Skip rate limiting for health checks, webhooks, and OPTIONS (CORS preflight)
        if (path.startsWith(HEALTH_PATH_PREFIX) ||
            path.startsWith(WEBHOOK_PATH_PREFIX) ||
            method.equals("OPTIONS", ignoreCase = true)
        ) {
            return
        }

        // Extract user identity from SecurityContext (JWT subject claim)
        val userId = requestContext.securityContext?.userPrincipal?.name
            ?: requestContext.getHeaderString("X-Forwarded-For")
            ?: requestContext.getHeaderString("X-Real-IP")
            ?: "anonymous"

        val allowed = when {
            // Bid placement: POST /api/v1/auctions/{auctionId}/bids
            method.equals("POST", ignoreCase = true) && BID_PATH_PATTERN.matches(path) -> {
                val auctionId = BID_PATH_PATTERN.find(path)?.groupValues?.get(1) ?: "unknown"
                // Apply both bid-specific and general limits
                rateLimitService.checkBidLimit(userId, auctionId) &&
                    rateLimitService.checkGeneralLimit(userId)
            }

            // Search endpoints
            path.startsWith(SEARCH_PATH_PREFIX) -> {
                rateLimitService.checkSearchLimit(userId) &&
                    rateLimitService.checkGeneralLimit(userId)
            }

            // General API limit
            else -> {
                rateLimitService.checkGeneralLimit(userId)
            }
        }

        if (!allowed) {
            logger.warn(
                "Rate limit exceeded for user={}, path={}, method={}",
                userId, path, method
            )

            val windowSeconds = when {
                method.equals("POST", ignoreCase = true) && BID_PATH_PATTERN.matches(path) ->
                    RateLimitService.BID_WINDOW_SECONDS
                path.startsWith(SEARCH_PATH_PREFIX) ->
                    RateLimitService.SEARCH_WINDOW_SECONDS
                else ->
                    RateLimitService.GENERAL_WINDOW_SECONDS
            }

            requestContext.abortWith(
                Response.status(429)
                    .header("Retry-After", windowSeconds)
                    .header("X-RateLimit-Limit", getApplicableLimit(path, method))
                    .header("X-RateLimit-Remaining", 0)
                    .header("X-RateLimit-Reset", System.currentTimeMillis() / 1000 + windowSeconds)
                    .entity(mapOf(
                        "error" to "RATE_LIMIT_EXCEEDED",
                        "message" to "Too many requests. Please retry after $windowSeconds seconds.",
                        "retryAfter" to windowSeconds
                    ))
                    .build()
            )
        }
    }

    /**
     * Returns the applicable rate limit value for the given path and method,
     * used in response headers.
     */
    private fun getApplicableLimit(path: String, method: String): Int = when {
        method.equals("POST", ignoreCase = true) && BID_PATH_PATTERN.matches(path) ->
            RateLimitService.BID_LIMIT
        path.startsWith(SEARCH_PATH_PREFIX) ->
            RateLimitService.SEARCH_LIMIT
        else ->
            RateLimitService.GENERAL_LIMIT
    }
}
