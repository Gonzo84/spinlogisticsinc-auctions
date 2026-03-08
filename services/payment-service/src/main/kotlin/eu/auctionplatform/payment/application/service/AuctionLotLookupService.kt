package eu.auctionplatform.payment.application.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

/**
 * Looks up auction results and lot details from the auction-engine
 * and catalog-service via their REST APIs.
 *
 * Used by the checkout flow to resolve the hammer price, winner,
 * seller ID, lot title, and seller country for payment creation.
 */
@ApplicationScoped
class AuctionLotLookupService(
    @ConfigProperty(name = "service.auction-engine.url")
    private val auctionEngineUrl: String,

    @ConfigProperty(name = "service.catalog-service.url")
    private val catalogServiceUrl: String
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(AuctionLotLookupService::class.java)
        private val mapper = jacksonObjectMapper()
        private val httpClient: HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
    }

    /**
     * Fetches auction details from the auction-engine for the given lot.
     *
     * Calls `GET /api/v1/auctions/by-lot/{lotId}` to find the auction
     * associated with a lot and extract the hammer price and winner.
     *
     * @return [AuctionResult] with auction details, or null if not found.
     */
    fun fetchAuctionResultByLot(lotId: UUID): AuctionResult? {
        return try {
            val url = "$auctionEngineUrl/api/v1/auctions/by-lot/$lotId"
            LOG.debugf("Fetching auction result for lot %s from %s", lotId, url)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
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
     * Calls `GET /api/v1/auctions/{auctionId}` to get the auction detail
     * including seller ID, current high bid, and winner.
     *
     * @return [AuctionResult] with auction details, or null if not found.
     */
    fun fetchAuctionResultByAuctionId(auctionId: UUID): AuctionResult? {
        return try {
            val url = "$auctionEngineUrl/api/v1/auctions/$auctionId"
            LOG.debugf("Fetching auction result for auction %s from %s", auctionId, url)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
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
     * Calls `GET /api/v1/lots/{lotId}` to get the lot title, seller ID,
     * and seller country.
     *
     * @return [LotInfo] with lot details, or null if not found.
     */
    fun fetchLotInfo(lotId: UUID): LotInfo? {
        return try {
            val url = "$catalogServiceUrl/api/v1/lots/$lotId"
            LOG.debugf("Fetching lot info for %s from %s", lotId, url)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
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
