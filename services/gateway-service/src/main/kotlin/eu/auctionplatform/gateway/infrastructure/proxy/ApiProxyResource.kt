package eu.auctionplatform.gateway.infrastructure.proxy

import jakarta.annotation.security.PermitAll
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// Reverse proxy that forwards unmatched /api/v1 requests to the
// appropriate upstream microservice based on the URL path prefix.
//
// Route resolution:
//   /api/v1/auctions     -> auction-engine
//   /api/v1/lots         -> catalog-service
//   /api/v1/users        -> user-service
//   /api/v1/payments     -> payment-service
//   /api/v1/notifications -> notification-service
//   /api/v1/media        -> media-service
//   /api/v1/search       -> search-service
//   /api/v1/sellers      -> seller-service
//   /api/v1/brokers      -> broker-service
//   /api/v1/compliance   -> compliance-service
//   /api/v1/co2          -> co2-service
//   /api/v1/analytics    -> analytics-service
@Path("/api/v1")
@ApplicationScoped
class ApiProxyResource {

    private val logger = LoggerFactory.getLogger(ApiProxyResource::class.java)

    @ConfigProperty(name = "gateway.routes.auction-engine.url")
    lateinit var auctionEngineUrl: String

    @ConfigProperty(name = "gateway.routes.catalog-service.url")
    lateinit var catalogServiceUrl: String

    @ConfigProperty(name = "gateway.routes.user-service.url")
    lateinit var userServiceUrl: String

    @ConfigProperty(name = "gateway.routes.payment-service.url")
    lateinit var paymentServiceUrl: String

    @ConfigProperty(name = "gateway.routes.notification-service.url")
    lateinit var notificationServiceUrl: String

    @ConfigProperty(name = "gateway.routes.media-service.url")
    lateinit var mediaServiceUrl: String

    @ConfigProperty(name = "gateway.routes.search-service.url")
    lateinit var searchServiceUrl: String

    @ConfigProperty(name = "gateway.routes.seller-service.url")
    lateinit var sellerServiceUrl: String

    @ConfigProperty(name = "gateway.routes.broker-service.url")
    lateinit var brokerServiceUrl: String

    @ConfigProperty(name = "gateway.routes.compliance-service.url")
    lateinit var complianceServiceUrl: String

    @ConfigProperty(name = "gateway.routes.co2-service.url")
    lateinit var co2ServiceUrl: String

    @ConfigProperty(name = "gateway.routes.analytics-service.url")
    lateinit var analyticsServiceUrl: String

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    // ----- Route mapping -----

    private fun resolveUpstream(path: String): String? {
        val segment = path.removePrefix("/api/v1/").split("/").firstOrNull() ?: return null
        return when (segment) {
            "auctions" -> auctionEngineUrl
            "lots", "categories" -> catalogServiceUrl
            "users" -> userServiceUrl
            "payments" -> paymentServiceUrl
            "notifications" -> notificationServiceUrl
            "media" -> mediaServiceUrl
            "search" -> searchServiceUrl
            "sellers" -> sellerServiceUrl
            "brokers" -> brokerServiceUrl
            "compliance" -> complianceServiceUrl
            "co2" -> co2ServiceUrl
            "analytics" -> analyticsServiceUrl
            else -> null
        }
    }

    // ----- Catch-all proxy endpoints -----

    @GET
    @Path("/{path: auctions.*|lots.*|categories.*|users.*|payments.*|notifications.*|media.*|search.*|sellers.*|brokers.*|compliance.*|co2.*|analytics.*}")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    fun proxyGet(
        @Context uriInfo: UriInfo,
        @Context headers: HttpHeaders
    ): Response = proxy("GET", uriInfo, headers, null)

    @POST
    @Path("/{path: auctions.*|lots.*|categories.*|users.*|payments.*|notifications.*|media.*|search.*|sellers.*|brokers.*|compliance.*|co2.*|analytics.*}")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun proxyPost(
        @Context uriInfo: UriInfo,
        @Context headers: HttpHeaders,
        body: String?
    ): Response = proxy("POST", uriInfo, headers, body)

    @PUT
    @Path("/{path: auctions.*|lots.*|categories.*|users.*|payments.*|notifications.*|media.*|search.*|sellers.*|brokers.*|compliance.*|co2.*|analytics.*}")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun proxyPut(
        @Context uriInfo: UriInfo,
        @Context headers: HttpHeaders,
        body: String?
    ): Response = proxy("PUT", uriInfo, headers, body)

    @DELETE
    @Path("/{path: auctions.*|lots.*|categories.*|users.*|payments.*|notifications.*|media.*|search.*|sellers.*|brokers.*|compliance.*|co2.*|analytics.*}")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    fun proxyDelete(
        @Context uriInfo: UriInfo,
        @Context headers: HttpHeaders
    ): Response = proxy("DELETE", uriInfo, headers, null)

    // ----- Core proxy logic -----

    private fun proxy(
        method: String,
        uriInfo: UriInfo,
        headers: HttpHeaders,
        body: String?
    ): Response {
        val requestPath = uriInfo.requestUri.rawPath
        val queryString = uriInfo.requestUri.rawQuery

        val upstreamBase = resolveUpstream(requestPath)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("status" to 404, "title" to "Not Found", "detail" to "No upstream service for path", "instance" to requestPath))
                .build()

        val targetUrl = buildString {
            append(upstreamBase)
            append(requestPath)
            if (!queryString.isNullOrBlank()) {
                append("?")
                append(queryString)
            }
        }

        logger.debug("Proxying {} {} -> {}", method, requestPath, targetUrl)

        return try {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(30))

            // Forward Authorization header
            val auth = headers.getHeaderString("Authorization")
            if (auth != null) {
                requestBuilder.header("Authorization", auth)
            }

            // Set method and body
            when (method) {
                "GET" -> requestBuilder.GET()
                "DELETE" -> requestBuilder.DELETE()
                "POST" -> {
                    requestBuilder.header("Content-Type", "application/json")
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body ?: ""))
                }
                "PUT" -> {
                    requestBuilder.header("Content-Type", "application/json")
                    requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body ?: ""))
                }
            }

            requestBuilder.header("Accept", "application/json")

            val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())

            val responseBuilder = Response.status(response.statusCode())
                .entity(response.body())
                .type(MediaType.APPLICATION_JSON)

            // Forward common response headers
            response.headers().firstValue("Location").ifPresent { loc ->
                responseBuilder.header("Location", loc)
            }

            responseBuilder.build()
        } catch (ex: Exception) {
            logger.error("Proxy error for {} {}: {}", method, targetUrl, ex.message)
            Response.status(Response.Status.BAD_GATEWAY)
                .entity(mapOf("status" to 502, "title" to "Bad Gateway", "detail" to "Upstream service unavailable", "instance" to requestPath))
                .build()
        }
    }
}
