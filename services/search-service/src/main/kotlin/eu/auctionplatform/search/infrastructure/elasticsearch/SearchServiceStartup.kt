package eu.auctionplatform.search.infrastructure.elasticsearch

import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.slf4j.LoggerFactory

/**
 * Initializes Elasticsearch indices on application startup.
 *
 * Observes the Quarkus [StartupEvent] and calls
 * [LotIndexService.createIndexIfNotExists] to ensure that the active
 * and archive lot indices exist before the first search request arrives.
 */
@ApplicationScoped
class SearchServiceStartup @Inject constructor(
    private val lotIndexService: LotIndexService
) {

    private val logger = LoggerFactory.getLogger(SearchServiceStartup::class.java)

    fun onApplicationStart(@Observes event: StartupEvent) {
        logger.info("Initializing Elasticsearch indices for search service")
        try {
            lotIndexService.createIndexIfNotExists()
            logger.info("Elasticsearch indices initialized successfully")
        } catch (ex: Exception) {
            logger.error("Failed to initialize Elasticsearch indices: {}", ex.message, ex)
        }
    }
}
