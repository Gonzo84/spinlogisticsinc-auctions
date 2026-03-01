package eu.auctionplatform.seller.infrastructure.nats

import com.fasterxml.jackson.databind.JsonNode
import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.seller.infrastructure.persistence.repository.SellerProfileRepository
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.util.UUID

// =============================================================================
// Auction Event Seller Consumer -- NATS JetStream subscriber for auction events
// =============================================================================

/**
 * Consumes auction domain events from NATS JetStream and updates seller
 * dashboard metrics and lot projections accordingly.
 *
 * Subscribed subjects (on the AUCTION stream):
 * - `auction.bid.placed` -- increments bid counter for the lot's seller.
 * - `auction.lot.closed` -- records hammer sale, updates lot status.
 * - `auction.lot.awarded` -- records award, updates lot status.
 *
 * Because auction events do not carry a `sellerId` field, this consumer
 * resolves the seller via a DB lookup on `seller_lots(id)` using the lotId
 * from the event payload.
 *
 * Uses a durable pull consumer named "seller-auction-consumer" on the
 * "AUCTION" stream to ensure at-least-once delivery and survive restarts.
 */
@Singleton
class AuctionEventSellerConsumer @Inject constructor(
    connection: Connection,
    private val sellerProfileRepository: SellerProfileRepository
) : NatsConsumer(
    connection = connection,
    streamName = STREAM_NAME,
    durableName = DURABLE_NAME,
    filterSubject = FILTER_SUBJECT,
    maxRedeliveries = 5,
    deadLetterSubject = "seller.dlq.auction",
    batchSize = 20
) {

    private var consumerThread: Thread? = null

    companion object {
        private val LOG: Logger = Logger.getLogger(AuctionEventSellerConsumer::class.java)

        /** NATS JetStream stream for auction events. */
        const val STREAM_NAME: String = "AUCTION"

        /** Durable consumer name -- persists across restarts. */
        const val DURABLE_NAME: String = "seller-auction-consumer"

        /** Subject filter matching all auction events. */
        const val FILTER_SUBJECT: String = "auction.>"

        // Subject prefixes for routing
        private const val SUBJECT_BID_PLACED = "auction.bid.placed"
        private const val SUBJECT_LOT_CLOSED = "auction.lot.closed"
        private const val SUBJECT_LOT_AWARDED = "auction.lot.awarded"
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts the consumer loop in a dedicated daemon thread on application startup.
     */
    fun onStart(@Observes event: StartupEvent) {
        LOG.infof("Starting AuctionEventSellerConsumer [durable=%s]", DURABLE_NAME)
        consumerThread = Thread({
            start()
        }, "seller-auction-consumer").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Signals the consumer loop to stop gracefully on application shutdown.
     */
    fun onStop(@Observes event: ShutdownEvent) {
        LOG.infof("Stopping AuctionEventSellerConsumer [durable=%s]", DURABLE_NAME)
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
     * Subject convention: `auction.<entity>.<action>` (optionally with brand suffix).
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
                subject.startsWith(SUBJECT_LOT_AWARDED) -> handleLotAwarded(payload)
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
     * Handles `auction.bid.placed` events by incrementing the bid counter
     * for the seller who owns the lot and updating current bid on seller_lots.
     *
     * The sellerId is resolved by looking up `seller_lots` by lotId.
     */
    private fun handleBidPlaced(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotId = extractLotId(node) ?: return
        val bidAmount = node.optionalDecimal("amount")

        val sellerId = sellerProfileRepository.findSellerIdByLotId(lotId)
        if (sellerId == null) {
            LOG.debugf("No seller found for lot %s -- skipping bid.placed", lotId)
            return
        }

        sellerProfileRepository.incrementBids(sellerId)

        // Update current bid and bid count on seller_lots projection
        if (bidAmount != null) {
            sellerProfileRepository.updateLotBid(lotId, bidAmount, node.optionalInt("bidCount"))
        }

        LOG.debugf("Incremented bid count for seller %s (lot=%s)", sellerId, lotId)
    }

    /**
     * Handles `auction.lot.closed` events by recording the hammer sale amount
     * and updating the lot status in seller_lots.
     *
     * Uses `finalBid` from [AuctionClosedEvent].
     */
    private fun handleLotClosed(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotId = extractLotId(node) ?: return

        val sellerId = sellerProfileRepository.findSellerIdByLotId(lotId)
        if (sellerId == null) {
            LOG.debugf("No seller found for lot %s -- skipping lot.closed", lotId)
            return
        }

        val hammerAmount = node.optionalDecimal("finalBid")
        if (hammerAmount != null && hammerAmount > BigDecimal.ZERO) {
            sellerProfileRepository.addHammerSale(sellerId, hammerAmount)
        }

        // Update lot status in seller_lots
        sellerProfileRepository.updateLotStatus(lotId, "CLOSED")

        LOG.debugf("Recorded lot closed for seller %s (lot=%s, hammer=%s)",
            sellerId, lotId, hammerAmount)
    }

    /**
     * Handles `auction.lot.awarded` events by updating the lot status
     * and recording the hammer price.
     *
     * Uses `hammerPrice` from [LotAwardedEvent].
     */
    private fun handleLotAwarded(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotId = extractLotId(node) ?: return

        val sellerId = sellerProfileRepository.findSellerIdByLotId(lotId)
        if (sellerId == null) {
            LOG.debugf("No seller found for lot %s -- skipping lot.awarded", lotId)
            return
        }

        // Update lot status to AWARDED
        sellerProfileRepository.updateLotStatus(lotId, "AWARDED")

        LOG.debugf("Recorded lot awarded for seller %s (lot=%s)", sellerId, lotId)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the lot ID from the event payload.
     * Supports both `lotId` (explicit) and `aggregateId` (auction event convention).
     */
    private fun extractLotId(node: JsonNode): UUID? {
        val lotIdStr = node.get("lotId")?.asText()
            ?: node.get("aggregateId")?.asText()
        if (lotIdStr == null) {
            LOG.warn("Event payload missing both 'lotId' and 'aggregateId' fields")
            return null
        }
        return try {
            UUID.fromString(lotIdStr)
        } catch (ex: IllegalArgumentException) {
            LOG.warnf("Invalid lotId format: %s", lotIdStr)
            null
        }
    }

    private fun JsonNode.optionalDecimal(field: String): BigDecimal? =
        this.get(field)?.takeIf { !it.isNull }?.decimalValue()

    private fun JsonNode.optionalInt(field: String): Int? =
        this.get(field)?.takeIf { !it.isNull }?.asInt()
}
