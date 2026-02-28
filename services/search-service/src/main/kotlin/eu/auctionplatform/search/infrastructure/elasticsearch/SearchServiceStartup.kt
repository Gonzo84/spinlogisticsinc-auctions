package eu.auctionplatform.search.infrastructure.elasticsearch

import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.jboss.logging.Logger

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

    companion object {
        private val LOG: Logger = Logger.getLogger(SearchServiceStartup::class.java)
    }

    fun onApplicationStart(@Observes event: StartupEvent) {
        LOG.info("Initializing Elasticsearch indices for search service")
        try {
            lotIndexService.createIndexIfNotExists()
            LOG.info("Elasticsearch indices initialized successfully")
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to initialize Elasticsearch indices: %s", ex.message)
        }
    }
}
