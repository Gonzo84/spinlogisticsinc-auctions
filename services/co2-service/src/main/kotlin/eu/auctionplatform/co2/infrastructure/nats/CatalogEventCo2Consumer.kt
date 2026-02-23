package eu.auctionplatform.co2.infrastructure.nats

import com.fasterxml.jackson.databind.JsonNode
import eu.auctionplatform.co2.application.service.Co2CalculationService
import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.util.JsonMapper
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.util.UUID

// =============================================================================
// Catalog Event CO2 Consumer -- NATS JetStream subscriber for lot creation events
// =============================================================================

/**
 * Consumes catalog lot creation events from NATS JetStream and triggers
 * CO2 avoidance calculations.
 *
 * Subscribed subject: `catalog.lot.created.>`
 *
 * When a new lot is created in the catalog, this consumer:
 * 1. Extracts the lot ID and category ID from the event payload
 * 2. Looks up the emission factor for the category
 * 3. Calculates CO2 avoided = newManufacturingCo2Kg * reuseFactor
 * 4. Persists the calculation
 * 5. Publishes a `co2.calculated.{lotId}` event
 *
 * Uses a durable pull consumer named "co2-catalog-consumer" on the
 * "CATALOG" stream to ensure at-least-once delivery and survive restarts.
 */
@ApplicationScoped
class CatalogEventCo2Consumer @Inject constructor(
    private val natsConnection: Connection,
    private val co2CalculationService: Co2CalculationService
) : NatsConsumer(
    connection = natsConnection,
    streamName = "CATALOG",
    durableName = DURABLE_NAME,
    filterSubject = FILTER_SUBJECT,
    maxRedeliveries = 5,
    deadLetterSubject = "co2.dlq.catalog",
    batchSize = 25
) {

    private val logger = LoggerFactory.getLogger(CatalogEventCo2Consumer::class.java)

    private var consumerThread: Thread? = null

    companion object {
        /** Durable consumer name -- persists across restarts. */
        const val DURABLE_NAME: String = "co2-catalog-consumer"

        /** Subject filter matching all catalog lot creation events. */
        const val FILTER_SUBJECT: String = "catalog.lot.created.>"
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts the consumer loop in a dedicated daemon thread on application startup.
     */
    fun onStart(@Observes event: StartupEvent) {
        logger.info("Starting CatalogEventCo2Consumer [durable={}]", DURABLE_NAME)
        consumerThread = Thread({
            start()
        }, "co2-catalog-event-consumer").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Signals the consumer loop to stop gracefully on application shutdown.
     */
    fun onStop(@Observes event: ShutdownEvent) {
        logger.info("Stopping CatalogEventCo2Consumer [durable={}]", DURABLE_NAME)
        stop()
        consumerThread?.interrupt()
    }

    // -------------------------------------------------------------------------
    // Message handling
    // -------------------------------------------------------------------------

    /**
     * Processes a single catalog lot creation event.
     *
     * Extracts the lot ID, category ID, and optional seller ID from the event
     * payload, then delegates to the [Co2CalculationService] to perform the
     * calculation and publish the result.
     */
    override fun handleMessage(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        logger.debug("Received catalog event on subject [{}], payload size={} bytes",
            subject, message.data.size)

        try {
            val node = JsonMapper.instance.readTree(payload)

            val lotIdStr = node.requiredText("lotId")
            val categoryIdStr = node.optionalText("categoryId")
            val sellerIdStr = node.optionalText("sellerId")

            if (categoryIdStr == null) {
                logger.warn("Lot [{}] has no categoryId -- skipping CO2 calculation", lotIdStr)
                return
            }

            val lotId = UUID.fromString(lotIdStr)
            val categoryId = UUID.fromString(categoryIdStr)
            val sellerId = sellerIdStr?.let { UUID.fromString(it) }

            logger.info("Calculating CO2 for lot [id={}, category={}]", lotId, categoryId)

            co2CalculationService.calculateForLot(lotId, categoryId, sellerId)

            logger.info("Successfully calculated CO2 for lot [id={}]", lotId)
        } catch (ex: Exception) {
            logger.error("Failed to process catalog event on [{}]: {}", subject, ex.message, ex)
            throw ex // re-throw so the base class handles nak/dead-letter
        }
    }

    // -------------------------------------------------------------------------
    // JSON extraction helpers
    // -------------------------------------------------------------------------

    private fun JsonNode.requiredText(field: String): String {
        return this.get(field)?.asText()
            ?: throw IllegalArgumentException("Required field '$field' missing from event payload")
    }

    private fun JsonNode.optionalText(field: String): String? =
        this.get(field)?.takeIf { !it.isNull }?.asText()
}
