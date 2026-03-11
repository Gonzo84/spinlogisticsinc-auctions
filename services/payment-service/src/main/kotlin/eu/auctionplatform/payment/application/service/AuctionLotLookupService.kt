package eu.auctionplatform.payment.application.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Context
import io.quarkus.oidc.client.OidcClient
import io.quarkus.oidc.client.OidcClients
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.faulttolerance.CircuitBreaker
import org.eclipse.microprofile.faulttolerance.Retry
import org.eclipse.microprofile.faulttolerance.Timeout
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Looks up auction results and lot details from the auction-engine
 * and catalog-service via their REST APIs.
 *
 * Uses Keycloak client credentials flow for service-to-service
 * authentication and OpenTelemetry context propagation for
 * distributed tracing.
 *
 * Protected by circuit breaker, timeout, and retry fault tolerance
 * annotations to handle downstream service failures gracefully.
 */
@ApplicationScoped
class AuctionLotLookupService(
    @ConfigProperty(name = "service.auction-engine.url")
    private val auctionEngineUrl: String,

    @ConfigProperty(name = "service.catalog-service.url")
    private val catalogServiceUrl: String
) {

    @Inject
    lateinit var oidcClients: OidcClients

    private var oidcClient: OidcClient? = null

    companion object {
        private val LOG: Logger = Logger.getLogger(AuctionLotLookupService::class.java)
        private val mapper = jacksonObjectMapper()
        private val httpClient: HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
    }

    @PostConstruct
    fun init() {
        try {
            oidcClient = oidcClients.getClient("internal")
            LOG.info("OIDC client 'internal' initialized for service-to-service auth")
        } catch (e: Exception) {
            LOG.warnf("Failed to initialize OIDC client 'internal': %s. " +
                "Inter-service calls will proceed without authentication.", e.message)
        }
    }

    /**
     * Obtains a service access token from the OIDC client (client credentials flow).
     * Returns null if the client is not configured or token acquisition fails.
     */
    private fun getServiceToken(): String? {
        return try {
            oidcClient?.getTokens()?.await()?.indefinitely()?.accessToken
        } catch (e: Exception) {
            LOG.warnf("Failed to obtain service token: %s", e.message)
            null
        }
    }

    /**
     * Builds an [HttpRequest.Builder] with authentication and trace context headers.
     */
    private fun authenticatedRequestBuilder(uri: URI): HttpRequest.Builder {
        val builder = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofSeconds(10))

        // Add service-to-service auth token
        val token = getServiceToken()
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }

        // Propagate OpenTelemetry trace context
        try {
            GlobalOpenTelemetry.getPropagators().textMapPropagator.inject(
                Context.current(), builder
            ) { b, key, value -> b?.header(key, value) }
        } catch (e: Exception) {
            LOG.debugf("Failed to inject trace context: %s", e.message)
        }

        return builder
    }

    /**
     * Fetches auction details from the auction-engine for the given lot.
     *
     * Calls `GET /api/v1/internal/auctions/by-lot/{lotId}` (internal endpoint)
     * to find the auction associated with a lot and extract the hammer price
     * and winner.
     *
     * @return [AuctionResult] with auction details, or null if not found.
     */
    @CircuitBreaker(requestVolumeThreshold = 20, failureRatio = 0.5, delay = 10000)
    @Timeout(5000)
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS)
    fun fetchAuctionResultByLot(lotId: UUID): AuctionResult? {
        return try {
            val url = "$auctionEngineUrl/api/v1/internal/auctions/by-lot/$lotId"
            LOG.debugf("Fetching auction result for lot %s from %s", lotId, url)

            val request = authenticatedRequestBuilder(URI.create(url))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                LOG.warnf("Auction lookup for lot %s returned %d", lotId, response.statusCode())
                return null
            }

            val body = mapper.readValue<Map<String, Any?>>(response.body())
            // Response is wrapped in ApiResponse: { data: { ... } }
            @Suppress("UNCHECKED_CAST")
            val data = (body["data"] as? Map<String, Any?>) ?: body

            val auctionId = (data["auctionId"] as? String)?.let { UUID.fromString(it) }
            val currentHighBid = (data["currentHighBid"] as? Number)?.let { BigDecimal(it.toString()) }
            val currentHighBidderId = (data["currentHighBidderId"] as? String)?.let { UUID.fromString(it) }
            val sellerId = (data["sellerId"] as? String)?.let { UUID.fromString(it) }
            val status = data["status"] as? String

            AuctionResult(
                auctionId = auctionId ?: lotId,
                hammerPrice = currentHighBid ?: BigDecimal.ZERO,
                winnerId = currentHighBidderId,
                sellerId = sellerId,
                status = status
            )
        } catch (e: Exception) {
            LOG.warnf("Failed to fetch auction result for lot %s: %s", lotId, e.message)
            null
        }
    }

    /**
     * Fetches auction details from the auction-engine by auction ID.
     *
     * Calls `GET /api/v1/internal/auctions/{auctionId}` (internal endpoint)
     * to get the auction detail including seller ID, current high bid, and winner.
     *
     * @return [AuctionResult] with auction details, or null if not found.
     */
    @CircuitBreaker(requestVolumeThreshold = 20, failureRatio = 0.5, delay = 10000)
    @Timeout(5000)
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS)
    fun fetchAuctionResultByAuctionId(auctionId: UUID): AuctionResult? {
        return try {
            val url = "$auctionEngineUrl/api/v1/internal/auctions/$auctionId"
            LOG.debugf("Fetching auction result for auction %s from %s", auctionId, url)

            val request = authenticatedRequestBuilder(URI.create(url))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                LOG.warnf("Auction lookup for %s returned %d", auctionId, response.statusCode())
                return null
            }

            val body = mapper.readValue<Map<String, Any?>>(response.body())
            @Suppress("UNCHECKED_CAST")
            val data = (body["data"] as? Map<String, Any?>) ?: body

            val currentHighBid = (data["currentHighBid"] as? Number)?.let { BigDecimal(it.toString()) }
            val currentHighBidderId = (data["currentHighBidderId"] as? String)?.let { UUID.fromString(it) }
            val sellerId = (data["sellerId"] as? String)?.let { UUID.fromString(it) }
            val status = data["status"] as? String

            AuctionResult(
                auctionId = auctionId,
                hammerPrice = currentHighBid ?: BigDecimal.ZERO,
                winnerId = currentHighBidderId,
                sellerId = sellerId,
                status = status
            )
        } catch (e: Exception) {
            LOG.warnf("Failed to fetch auction result for auction %s: %s", auctionId, e.message)
            null
        }
    }

    /**
     * Fetches lot details from the catalog-service.
     *
     * Calls `GET /api/v1/internal/lots/{lotId}` (internal endpoint)
     * to get the lot title, seller ID, and seller country.
     *
     * @return [LotInfo] with lot details, or null if not found.
     */
    @CircuitBreaker(requestVolumeThreshold = 20, failureRatio = 0.5, delay = 10000)
    @Timeout(5000)
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS)
    fun fetchLotInfo(lotId: UUID): LotInfo? {
        return try {
            val url = "$catalogServiceUrl/api/v1/internal/lots/$lotId"
            LOG.debugf("Fetching lot info for %s from %s", lotId, url)

            val request = authenticatedRequestBuilder(URI.create(url))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                LOG.warnf("Lot lookup for %s returned %d", lotId, response.statusCode())
                return null
            }

            val body = mapper.readValue<Map<String, Any?>>(response.body())
            // Response may be wrapped in ApiResponse: { data: { ... } }
            @Suppress("UNCHECKED_CAST")
            val data = (body["data"] as? Map<String, Any?>) ?: body

            val title = data["title"] as? String
            val sellerId = (data["sellerId"] as? String)?.let { UUID.fromString(it) }
            val sellerCountry = data["locationCountry"] as? String

            LotInfo(
                lotId = lotId,
                title = title ?: "Lot $lotId",
                sellerId = sellerId,
                sellerCountry = sellerCountry ?: "NL"
            )
        } catch (e: Exception) {
            LOG.warnf("Failed to fetch lot info for %s: %s", lotId, e.message)
            null
        }
    }
}

/**
 * Auction result data resolved from the auction-engine.
 */
data class AuctionResult(
    val auctionId: UUID,
    val hammerPrice: BigDecimal,
    val winnerId: UUID?,
    val sellerId: UUID?,
    val status: String?
)

/**
 * Lot information resolved from the catalog-service.
 */
data class LotInfo(
    val lotId: UUID,
    val title: String,
    val sellerId: UUID?,
    val sellerCountry: String
)
