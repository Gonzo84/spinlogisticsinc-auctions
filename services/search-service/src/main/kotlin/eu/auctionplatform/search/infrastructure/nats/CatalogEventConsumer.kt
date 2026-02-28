package eu.auctionplatform.search.infrastructure.nats

import com.fasterxml.jackson.databind.JsonNode
import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.search.infrastructure.elasticsearch.GeoPoint
import eu.auctionplatform.search.infrastructure.elasticsearch.LotDocument
import eu.auctionplatform.search.infrastructure.elasticsearch.LotImage
import eu.auctionplatform.search.infrastructure.elasticsearch.LotIndexService
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.time.Instant

// =============================================================================
// Catalog Event Consumer – NATS JetStream subscriber for catalog events
// =============================================================================

/**
 * Consumes catalog domain events from NATS JetStream and updates the
 * Elasticsearch lots index accordingly.
 *
 * Subscribed subjects:
 * - `catalog.lot.created.>` -- indexes a new lot document.
 * - `catalog.lot.updated.>` -- partially updates an existing lot document.
 * - `catalog.lot.status.changed.>` -- updates only the status field.
 *
 * Uses a durable pull consumer named "search-catalog-consumer" on the
 * "CATALOG" stream to ensure at-least-once delivery and survive restarts.
 */
@Singleton
class CatalogEventConsumer @Inject constructor(
    connection: Connection,
    private val lotIndexService: LotIndexService
) : NatsConsumer(
    connection = connection,
    streamName = "CATALOG",
    durableName = DURABLE_NAME,
    filterSubject = FILTER_SUBJECT,
    maxRedeliveries = 5,
    deadLetterSubject = "search.dlq.catalog",
    batchSize = 25
) {

    private var consumerThread: Thread? = null

    companion object {
        private val LOG: Logger = Logger.getLogger(CatalogEventConsumer::class.java)

        /** Durable consumer name -- persists across restarts. */
        const val DURABLE_NAME: String = "search-catalog-consumer"

        /** Subject filter matching all catalog lot events with brand suffix. */
        const val FILTER_SUBJECT: String = "catalog.lot.>"
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts the consumer loop in a dedicated daemon thread on application startup.
     */
    fun onStart(@Observes event: StartupEvent) {
        LOG.infof("Starting CatalogEventConsumer [durable=%s]", DURABLE_NAME)
        consumerThread = Thread({
            start()
        }, "catalog-event-consumer").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Signals the consumer loop to stop gracefully on application shutdown.
     */
    fun onStop(@Observes event: ShutdownEvent) {
        LOG.infof("Stopping CatalogEventConsumer [durable=%s]", DURABLE_NAME)
        stop()
        consumerThread?.interrupt()
    }

    // -------------------------------------------------------------------------
    // Message handling
    // -------------------------------------------------------------------------

    /**
     * Routes an incoming NATS message to the appropriate handler based on the
     * subject pattern.
     *
     * Subject convention: `catalog.lot.<action>.<brand>`
     * - `catalog.lot.created.<brand>` -- full lot creation
     * - `catalog.lot.updated.<brand>` -- partial lot update
     * - `catalog.lot.status.changed.<brand>` -- status-only update
     */
    override fun handleMessage(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        LOG.debugf("Received catalog event on subject [%s], payload size=%s bytes",
            subject, message.data.size)

        try {
            when {
                subject.startsWith("catalog.lot.status.changed") -> handleStatusChanged(payload)
                subject.startsWith("catalog.lot.created") -> handleLotCreated(payload)
                subject.startsWith("catalog.lot.updated") -> handleLotUpdated(payload)
                else -> LOG.warnf("Unrecognised catalog subject [%s] -- skipping", subject)
            }
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to process catalog event on [%s]: %s", subject, ex.message)
            throw ex // re-throw so the base class handles nak/dead-letter
        }
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    /**
     * Handles `catalog.lot.created` events by indexing a complete new lot document.
     *
     * The event payload is expected to contain all fields required to build a
     * [LotDocument]. Missing optional fields default to their null/empty values.
     */
    private fun handleLotCreated(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotId = node.requiredText("lotId")

        LOG.infof("Indexing new lot [id=%s]", lotId)

        val document = LotDocument(
            id = lotId,
            title = node.requiredText("title"),
            description = node.optionalText("description"),
            categoryId = node.optionalText("categoryId"),
            categoryPath = node.optionalStringList("categoryPath"),
            brand = node.optionalText("brand"),
            country = node.optionalText("country"),
            city = node.optionalText("city"),
            location = node.optionalGeoPoint("location"),
            currentBid = node.optionalDecimal("currentBid"),
            startingBid = node.optionalDecimal("startingBid"),
            bidCount = node.optionalInt("bidCount") ?: 0,
            reserveStatus = node.optionalText("reserveStatus") ?: "no_reserve",
            auctionEndTime = node.optionalInstant("auctionEndTime"),
            status = node.optionalText("status") ?: "active",
            co2AvoidedKg = node.optionalFloat("co2AvoidedKg"),
            specifications = node.optionalMap("specifications"),
            images = node.optionalImageList("images"),
            createdAt = node.optionalInstant("createdAt") ?: Instant.now(),
            sellerId = node.optionalText("sellerId"),
            lotNumber = node.optionalText("lotNumber"),
            currency = node.optionalText("currency") ?: "EUR"
        )

        lotIndexService.indexDocument(document)
        LOG.infof("Successfully indexed lot [id=%s]", lotId)
    }

    /**
     * Handles `catalog.lot.updated` events by performing a partial update on the
     * existing lot document.
     *
     * Only the fields present in the event payload are updated; all other fields
     * in the document remain untouched.
     */
    private fun handleLotUpdated(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotId = node.requiredText("lotId")

        LOG.infof("Updating lot [id=%s]", lotId)

        val updates = mutableMapOf<String, Any?>()

        node.optionalText("title")?.let { updates["title"] = it }
        node.optionalText("description")?.let { updates["description"] = it }
        node.optionalText("categoryId")?.let { updates["categoryId"] = it }
        if (node.has("categoryPath")) updates["categoryPath"] = node.optionalStringList("categoryPath")
        node.optionalText("brand")?.let { updates["brand"] = it }
        node.optionalText("country")?.let { updates["country"] = it }
        node.optionalText("city")?.let { updates["city"] = it }
        node.optionalGeoPoint("location")?.let { updates["location"] = it }
        node.optionalDecimal("currentBid")?.let { updates["currentBid"] = it }
        node.optionalDecimal("startingBid")?.let { updates["startingBid"] = it }
        if (node.has("bidCount")) updates["bidCount"] = node.optionalInt("bidCount")
        node.optionalText("reserveStatus")?.let { updates["reserveStatus"] = it }
        node.optionalInstant("auctionEndTime")?.let { updates["auctionEndTime"] = it.toString() }
        node.optionalText("status")?.let { updates["status"] = it }
        node.optionalFloat("co2AvoidedKg")?.let { updates["co2AvoidedKg"] = it }
        if (node.has("specifications")) updates["specifications"] = node.optionalMap("specifications")
        if (node.has("images")) updates["images"] = node.optionalImageList("images")
        node.optionalText("sellerId")?.let { updates["sellerId"] = it }
        node.optionalText("lotNumber")?.let { updates["lotNumber"] = it }
        node.optionalText("currency")?.let { updates["currency"] = it }

        if (updates.isNotEmpty()) {
            lotIndexService.updateDocument(lotId, updates)
            LOG.infof("Successfully updated lot [id=%s] with %s fields", lotId, updates.size)
        } else {
            LOG.debugf("No updateable fields in event for lot [id=%s]", lotId)
        }
    }

    /**
     * Handles `catalog.lot.status.changed` events by updating only the status field.
     */
    private fun handleStatusChanged(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotId = node.requiredText("lotId")
        val newStatus = node.requiredText("status")

        LOG.infof("Updating status for lot [id=%s] to [%s]", lotId, newStatus)

        lotIndexService.updateDocument(lotId, mapOf("status" to newStatus))
        LOG.infof("Successfully updated status for lot [id=%s]", lotId)
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

    private fun JsonNode.optionalInt(field: String): Int? =
        this.get(field)?.takeIf { !it.isNull }?.asInt()

    private fun JsonNode.optionalFloat(field: String): Float? =
        this.get(field)?.takeIf { !it.isNull }?.floatValue()

    private fun JsonNode.optionalDecimal(field: String): BigDecimal? =
        this.get(field)?.takeIf { !it.isNull }?.decimalValue()

    private fun JsonNode.optionalInstant(field: String): Instant? =
        this.get(field)?.takeIf { !it.isNull }?.asText()?.let { Instant.parse(it) }

    private fun JsonNode.optionalStringList(field: String): List<String> {
        val arrayNode = this.get(field) ?: return emptyList()
        if (!arrayNode.isArray) return emptyList()
        return arrayNode.map { it.asText() }
    }

    private fun JsonNode.optionalGeoPoint(field: String): GeoPoint? {
        val obj = this.get(field) ?: return null
        if (obj.isNull) return null
        val lat = obj.get("lat")?.asDouble() ?: return null
        val lon = obj.get("lon")?.asDouble() ?: return null
        return GeoPoint(lat = lat, lon = lon)
    }

    @Suppress("UNCHECKED_CAST")
    private fun JsonNode.optionalMap(field: String): Map<String, Any>? {
        val obj = this.get(field) ?: return null
        if (obj.isNull) return null
        return JsonMapper.instance.convertValue(obj, Map::class.java) as? Map<String, Any>
    }

    private fun JsonNode.optionalImageList(field: String): List<LotImage> {
        val arrayNode = this.get(field) ?: return emptyList()
        if (!arrayNode.isArray) return emptyList()
        return arrayNode.map { imgNode ->
            LotImage(
                url = imgNode.get("url")?.asText() ?: "",
                thumbnailUrl = imgNode.get("thumbnailUrl")?.asText(),
                isPrimary = imgNode.get("isPrimary")?.asBoolean() ?: false
            )
        }
    }
}
