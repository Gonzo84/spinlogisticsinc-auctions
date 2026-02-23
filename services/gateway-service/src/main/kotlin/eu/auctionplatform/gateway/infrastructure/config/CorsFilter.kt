package eu.auctionplatform.gateway.infrastructure.config

import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory

// =============================================================================
// CORS Filter – Cross-Origin Resource Sharing response headers
// =============================================================================

/**
 * JAX-RS [ContainerResponseFilter] that adds CORS headers to all responses.
 *
 * While Quarkus provides built-in CORS support via `quarkus.http.cors.*`
 * configuration, this filter provides additional control for WebSocket upgrade
 * requests and custom headers that the built-in filter may not fully cover.
 *
 * The filter also handles CORS preflight (OPTIONS) responses, ensuring that
 * browsers receive the correct `Access-Control-Allow-*` headers before sending
 * the actual request.
 *
 * Allowed origins are configured via the `cors.allowed-origins` property,
 * defaulting to `*` in development.
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
class CorsFilter : ContainerResponseFilter {

    private val logger = LoggerFactory.getLogger(CorsFilter::class.java)

    @ConfigProperty(name = "cors.allowed-origins", defaultValue = "*")
    lateinit var allowedOrigins: String

    @ConfigProperty(name = "cors.allowed-methods", defaultValue = "GET,POST,PUT,DELETE,PATCH,OPTIONS")
    lateinit var allowedMethods: String

    @ConfigProperty(name = "cors.allowed-headers", defaultValue = "accept,authorization,content-type,x-requested-with,x-correlation-id")
    lateinit var allowedHeaders: String

    @ConfigProperty(name = "cors.exposed-headers", defaultValue = "location,x-ratelimit-limit,x-ratelimit-remaining,x-ratelimit-reset,retry-after")
    lateinit var exposedHeaders: String

    @ConfigProperty(name = "cors.max-age-seconds", defaultValue = "3600")
    var maxAgeSeconds: Int = 3600

    @ConfigProperty(name = "cors.allow-credentials", defaultValue = "true")
    var allowCredentials: Boolean = true

    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext
    ) {
        val origin = requestContext.getHeaderString("Origin")

        // Only add CORS headers if an Origin header is present
        if (origin.isNullOrBlank()) {
            return
        }

        val headers = responseContext.headers

        // Determine the allowed origin to return
        val effectiveOrigin = if (allowedOrigins == "*") {
            origin
        } else {
            val allowedList = allowedOrigins.split(",").map { it.trim() }
            if (allowedList.contains(origin)) origin else null
        }

        if (effectiveOrigin == null) {
            logger.debug("CORS: rejecting origin [{}] -- not in allowed list", origin)
            return
        }

        headers.putSingle("Access-Control-Allow-Origin", effectiveOrigin)
        headers.putSingle("Access-Control-Allow-Methods", allowedMethods)
        headers.putSingle("Access-Control-Allow-Headers", allowedHeaders)
        headers.putSingle("Access-Control-Expose-Headers", exposedHeaders)
        headers.putSingle("Access-Control-Max-Age", maxAgeSeconds.toString())

        if (allowCredentials) {
            headers.putSingle("Access-Control-Allow-Credentials", "true")
        }

        // Vary header to ensure correct caching per origin
        headers.add("Vary", "Origin")
    }
}
