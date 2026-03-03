package eu.auctionplatform.search.infrastructure.elasticsearch

import eu.auctionplatform.commons.util.JsonMapper
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

/**
 * Initializes Elasticsearch indices on application startup and performs
 * an initial sync of lots from the catalog-service to ensure the search
 * index is populated even when NATS events were missed.
 *
 * Observes the Quarkus [StartupEvent] and calls
 * [LotIndexService.createIndexIfNotExists] to ensure that the active
 * and archive lot indices exist before the first search request arrives.
 * Then triggers a full reindex from the catalog-service.
 */
@ApplicationScoped
class SearchServiceStartup @Inject constructor(
    private val lotIndexService: LotIndexService,
    @ConfigProperty(name = "search.catalog-service.url", defaultValue = "http://localhost:8082")
    private val catalogServiceUrl: String
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(SearchServiceStartup::class.java)
    }

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun onApplicationStart(@Observes event: StartupEvent) {
        LOG.info("Initializing Elasticsearch indices for search service")
        try {
            lotIndexService.createIndexIfNotExists()
            LOG.info("Elasticsearch indices initialized successfully")
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to initialize Elasticsearch indices: %s", ex.message)
        }

        // Sync lots from catalog-service in a background thread to avoid
        // blocking the startup sequence.
        Thread({
            try {
                // Brief delay to allow catalog-service to fully start
                Thread.sleep(5000)
                syncLotsFromCatalog()
            } catch (ex: Exception) {
                LOG.warnf("Background lot sync from catalog-service failed: %s", ex.message)
            }
        }, "search-catalog-sync").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Fetches all lots from the catalog-service REST API and indexes them
     * into Elasticsearch. This is an idempotent operation -- existing documents
     * are overwritten (upserted) with the latest data from catalog.
     *
     * Only lots with status APPROVED or ACTIVE are indexed with status "active"
     * to match the search query filter. Other lot statuses are indexed with
     * their actual status for potential admin searches.
     *
     * @return The number of lots indexed.
     */
    fun syncLotsFromCatalog(): Int {
        LOG.info("Starting lot sync from catalog-service")

        var totalIndexed = 0
        var page = 0
        val pageSize = 100

        while (true) {
            val url = "$catalogServiceUrl/api/v1/lots?page=$page&pageSize=$pageSize"
            LOG.debugf("Fetching lots page %d from catalog-service: %s", page, url)

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build()

            val response = try {
                httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            } catch (ex: Exception) {
                LOG.warnf("Failed to fetch lots from catalog-service (page %d): %s", page, ex.message)
                break
            }

            if (response.statusCode() != 200) {
                LOG.warnf("Catalog-service returned status %d for page %d", response.statusCode(), page)
                break
            }

            val rootNode = JsonMapper.instance.readTree(response.body())
            // Handle ApiResponse wrapper: { "data": { "items": [...], "total": N } }
            val dataNode = rootNode.get("data") ?: rootNode
            val itemsNode = dataNode.get("items")

            if (itemsNode == null || !itemsNode.isArray || itemsNode.size() == 0) {
                LOG.debugf("No more lots on page %d -- sync complete", page)
                break
            }

            for (item in itemsNode) {
                try {
                    val lotId = item.get("id")?.asText() ?: continue
                    val rawStatus = (item.get("status")?.asText() ?: "DRAFT").uppercase()

                    // Map backend enum status to search index status
                    val indexStatus = when (rawStatus) {
                        "APPROVED", "ACTIVE" -> "active"
                        else -> rawStatus.lowercase()
                    }

                    val document = LotDocument(
                        id = lotId,
                        title = item.get("title")?.asText() ?: "",
                        description = item.get("description")?.asText(),
                        categoryId = item.get("categoryId")?.asText(),
                        brand = item.get("brand")?.asText(),
                        country = item.get("locationCountry")?.asText(),
                        city = item.get("locationCity")?.asText(),
                        startingBid = item.get("startingBid")?.decimalValue(),
                        status = indexStatus,
                        co2AvoidedKg = item.get("co2AvoidedKg")?.floatValue(),
                        sellerId = item.get("sellerId")?.asText(),
                        createdAt = item.get("createdAt")?.asText()?.let {
                            try { Instant.parse(it) } catch (_: Exception) { null }
                        } ?: Instant.now(),
                        currency = item.get("currency")?.asText() ?: "EUR"
                    )

                    lotIndexService.indexDocument(document)
                    totalIndexed++
                } catch (ex: Exception) {
                    LOG.warnf("Failed to index lot from catalog sync: %s", ex.message)
                }
            }

            val total = dataNode.get("total")?.asLong() ?: 0
            val fetched = ((page + 1) * pageSize).toLong()
            if (fetched >= total) {
                break
            }

            page++
        }

        LOG.infof("Lot sync from catalog-service complete: %d lots indexed", totalIndexed)
        return totalIndexed
    }
}
