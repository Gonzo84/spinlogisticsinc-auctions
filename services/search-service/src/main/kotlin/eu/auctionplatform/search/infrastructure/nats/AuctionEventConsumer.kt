package eu.auctionplatform.search.infrastructure.nats

import com.fasterxml.jackson.databind.JsonNode
import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.util.JsonMapper
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
// Auction Event Consumer – NATS JetStream subscriber for auction events
// =============================================================================

/**
 * Consumes auction and CO2 domain events from NATS JetStream and updates the
 * Elasticsearch lots index accordingly.
 *
 * Subscribed subjects:
 * - `auction.bid.placed.>` -- updates currentBid and increments bidCount.
 * - `auction.lot.closed.>` -- removes the lot from the active index and moves
 *   it to the archive index.
 * - `auction.lot.extended.>` -- updates the auctionEndTime field.
 * - `co2.calculated.>` -- updates the co2AvoidedKg field.
 *
 * Uses a durable pull consumer named "search-auction-consumer" on the
 * "AUCTION" stream to ensure at-least-once delivery and survive restarts.
 *
 * Note: The filter subject uses `>` (NATS multi-level wildcard) to capture
 * brand-suffixed subjects, e.g. `auction.bid.placed.troostwijk`.
 */
@Singleton
class AuctionEventConsumer @Inject constructor(
    connection: Connection,
    private val lotIndexService: LotIndexService
) : NatsConsumer(
    connection = connection,
    streamName = "AUCTION",
    durableName = DURABLE_NAME,
    filterSubject = FILTER_SUBJECT,
    maxRedeliveries = 5,
    deadLetterSubject = "search.dlq.auction",
    batchSize = 50
) {

    private var consumerThread: Thread? = null

    companion object {
        private val LOG: Logger = Logger.getLogger(AuctionEventConsumer::class.java)

        /** Durable consumer name -- persists across restarts. */
        const val DURABLE_NAME: String = "search-auction-consumer"

        /**
         * Subject filter matching auction bid, lot, and CO2 events.
         *
         * Because the base [NatsConsumer] only supports a single filter subject,
         * we subscribe to `>` (all subjects in the stream) and route by prefix
         * in [handleMessage]. The JetStream stream itself is scoped to relevant
         * subjects.
         */
        const val FILTER_SUBJECT: String = ">"

        // Subject prefixes for routing
        private const val SUBJECT_BID_PLACED = "auction.bid.placed"
        private const val SUBJECT_LOT_CLOSED = "auction.lot.closed"
        private const val SUBJECT_LOT_EXTENDED = "auction.lot.extended"
        private const val SUBJECT_CO2_CALCULATED = "co2.calculated"
        private const val SUBJECT_FEATURED_MARKED = "auction.featured.marked"
        private const val SUBJECT_FEATURED_UNMARKED = "auction.featured.unmarked"
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts the consumer loop in a dedicated daemon thread on application startup.
     */
    fun onStart(@Observes event: StartupEvent) {
        LOG.infof("Starting AuctionEventConsumer [durable=%s]", DURABLE_NAME)
        consumerThread = Thread({
            start()
        }, "auction-event-consumer").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Signals the consumer loop to stop gracefully on application shutdown.
     */
    fun onStop(@Observes event: ShutdownEvent) {
        LOG.infof("Stopping AuctionEventConsumer [durable=%s]", DURABLE_NAME)
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
     * Subject convention: `<domain>.<entity>.<action>.<brand>`
     * - `auction.bid.placed.<brand>` -- bid placement
     * - `auction.lot.closed.<brand>` -- auction closing
     * - `auction.lot.extended.<brand>` -- anti-sniping extension
     * - `co2.calculated.<brand>` -- CO2 calculation result
     */
    override fun handleMessage(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        LOG.debugf("Received auction event on subject [%s], payload size=%s bytes",
            subject, message.data.size)

        try {
            when {
                subject.startsWith(SUBJECT_BID_PLACED) -> handleBidPlaced(payload)
                subject.startsWith(SUBJECT_LOT_CLOSED) -> handleLotClosed(payload)
                subject.startsWith(SUBJECT_LOT_EXTENDED) -> handleLotExtended(payload)
                subject.startsWith(SUBJECT_CO2_CALCULATED) -> handleCo2Calculated(payload)
                subject.startsWith(SUBJECT_FEATURED_MARKED) -> handleFeaturedMarked(payload)
                subject.startsWith(SUBJECT_FEATURED_UNMARKED) -> handleFeaturedUnmarked(payload)
                else -> LOG.debugf("Ignoring unrelated subject [%s] on auction stream", subject)
            }
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to process auction event on [%s]: %s", subject, ex.message)
            throw ex // re-throw so the base class handles nak/dead-letter
        }
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    /**
     * Handles `auction.bid.placed` events by updating the currentBid amount
     * and incrementing the bidCount on the lot document.
     *
     * Expected payload fields:
     * - `lotId` or `aggregateId` -- the lot identifier
     * - `bidAmount` -- the new highest bid amount
     * - `bidCurrency` -- currency of the bid (optional, for validation)
     *
     * The bidCount is retrieved from the existing document and incremented,
     * or derived from the event payload if provided.
     */
    private fun handleBidPlaced(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotId = extractLotId(node)
        val bidAmount = node.requiredDecimal("bidAmount")

        LOG.infof("Updating bid for lot [id=%s], newBid=%s", lotId, bidAmount)

        // Attempt to read current bid count and increment
        val currentDoc = lotIndexService.getDocument(lotId)
        val newBidCount = (currentDoc?.bidCount ?: 0) + 1

        val updates = mutableMapOf<String, Any?>(
            "currentBid" to bidAmount,
            "bidCount" to newBidCount
        )

        // Update reserve status if provided (e.g. reserve was met by this bid)
        node.optionalText("reserveStatus")?.let { updates["reserveStatus"] = it }

        lotIndexService.updateDocument(lotId, updates)
        LOG.infof("Successfully updated bid for lot [id=%s], bidCount=%s", lotId, newBidCount)
    }

    /**
     * Handles `auction.lot.closed` events by removing the lot from the active
     * index and archiving it.
     *
     * Expected payload fields:
     * - `lotId` or `aggregateId` -- the lot identifier
     * - `finalBidAmount` -- final closing bid amount (optional)
     * - `winnerId` -- winning bidder ID (optional)
     * - `reserveMet` -- whether the reserve price was met (optional)
     */
    private fun handleLotClosed(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotId = extractLotId(node)

        LOG.infof("Closing and archiving lot [id=%s]", lotId)

        // Update final state before archiving
        val updates = mutableMapOf<String, Any?>("status" to "closed")
        node.optionalDecimal("finalBidAmount")?.let { updates["currentBid"] = it }
        node.optionalText("winnerId")?.let { /* stored in archive but not in search index */ }

        val reserveMet = node.get("reserveMet")?.asBoolean()
        if (reserveMet != null) {
            updates["reserveStatus"] = if (reserveMet) "reserve_met" else "reserve_not_met"
        }

        try {
            lotIndexService.updateDocument(lotId, updates)
        } catch (ex: Exception) {
            LOG.warnf("Could not update lot [id=%s] before archiving: %s", lotId, ex.message)
        }

        // Move from active to archive index
        lotIndexService.archiveDocument(lotId)
        LOG.infof("Successfully archived lot [id=%s]", lotId)
    }

    /**
     * Handles `auction.lot.extended` events by updating the auctionEndTime field.
     *
     * This occurs when the anti-sniping rule extends the auction closing time
     * due to a late bid.
     *
     * Expected payload fields:
     * - `lotId` or `aggregateId` -- the lot identifier
     * - `newEndTime` -- the new auction end time (ISO-8601)
     */
    private fun handleLotExtended(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotId = extractLotId(node)
        val newEndTime = node.requiredText("newEndTime")

        LOG.infof("Extending auction end time for lot [id=%s] to [%s]", lotId, newEndTime)

        lotIndexService.updateDocument(lotId, mapOf(
            "auctionEndTime" to newEndTime
        ))
        LOG.infof("Successfully extended auction for lot [id=%s]", lotId)
    }

    /**
     * Handles `co2.calculated` events by updating the co2AvoidedKg field.
     *
     * This event is emitted by the CO2 service after calculating the
     * environmental impact of reselling the item.
     *
     * Expected payload fields:
     * - `lotId` -- the lot identifier
     * - `co2AvoidedKg` -- estimated CO2 savings in kilograms
     */
    private fun handleCo2Calculated(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotId = node.requiredText("lotId")
        val co2AvoidedKg = node.get("co2AvoidedKg")?.floatValue()
            ?: throw IllegalArgumentException("Required field 'co2AvoidedKg' missing from CO2 event")

        LOG.infof("Updating CO2 avoided for lot [id=%s] to %s kg", lotId, co2AvoidedKg)

        lotIndexService.updateDocument(lotId, mapOf(
            "co2AvoidedKg" to co2AvoidedKg
        ))
        LOG.infof("Successfully updated CO2 for lot [id=%s]", lotId)
    }

    /**
     * Handles `auction.featured.marked` events by setting featured=true on the lot document.
     */
    private fun handleFeaturedMarked(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotId = extractLotId(node)
        val featuredAt = node.optionalText("featuredAt")

        LOG.infof("Marking lot [id=%s] as featured", lotId)

        val updates = mutableMapOf<String, Any?>(
            "featured" to true,
        )
        if (featuredAt != null) {
            updates["featuredAt"] = featuredAt
        }

        lotIndexService.updateDocument(lotId, updates)
        LOG.infof("Successfully marked lot [id=%s] as featured", lotId)
    }

    /**
     * Handles `auction.featured.unmarked` events by setting featured=false on the lot document.
     */
    private fun handleFeaturedUnmarked(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotId = extractLotId(node)

        LOG.infof("Removing featured flag from lot [id=%s]", lotId)

        lotIndexService.updateDocument(lotId, mapOf(
            "featured" to false,
            "featuredAt" to null,
        ))
        LOG.infof("Successfully removed featured flag from lot [id=%s]", lotId)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the lot ID from the event payload.
     *
     * Supports both `lotId` (catalog-style events) and `aggregateId`
     * (auction-style events where the aggregate is the auction/lot).
     */
    private fun extractLotId(node: JsonNode): String {
        return node.get("lotId")?.asText()
            ?: node.get("aggregateId")?.asText()
            ?: throw IllegalArgumentException("Neither 'lotId' nor 'aggregateId' found in event payload")
    }

    private fun JsonNode.requiredText(field: String): String {
        return this.get(field)?.asText()
            ?: throw IllegalArgumentException("Required field '$field' missing from event payload")
    }

    private fun JsonNode.optionalText(field: String): String? =
        this.get(field)?.takeIf { !it.isNull }?.asText()

    private fun JsonNode.optionalDecimal(field: String): BigDecimal? =
        this.get(field)?.takeIf { !it.isNull }?.decimalValue()

    private fun JsonNode.requiredDecimal(field: String): BigDecimal {
        return this.get(field)?.decimalValue()
            ?: throw IllegalArgumentException("Required field '$field' missing from event payload")
    }
}
