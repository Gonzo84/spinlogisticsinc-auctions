package eu.auctionplatform.seller.infrastructure.nats

import com.fasterxml.jackson.databind.JsonNode
import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.seller.infrastructure.persistence.repository.SellerProfileRepository
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
 * - `auction.lot.award-revoked` -- reverts lot status to CLOSED, removes READY settlements.
 *
 * Because auction events do not carry a `sellerId` field, this consumer
 * resolves the seller via a DB lookup on `seller_lots(id)` using the lotId
 * from the event payload.
 *
 * Uses durable consumers on the "AUCTION" stream to ensure at-least-once
 * delivery and survive restarts.
 */
@ApplicationScoped
@Startup
class AuctionEventSellerConsumer @Inject constructor(
    private val connection: Connection,
    private val sellerProfileRepository: SellerProfileRepository
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(AuctionEventSellerConsumer::class.java)

        private const val STREAM_NAME: String = "AUCTION"
        private const val DURABLE_NAME: String = "seller-auction-consumer"

        // Subject prefixes for routing
        private const val SUBJECT_BID_PLACED = "auction.bid.placed"
        private const val SUBJECT_LOT_CLOSED = "auction.lot.closed"
        private const val SUBJECT_LOT_AWARDED = "auction.lot.awarded"
        private const val SUBJECT_LOT_AWARD_REVOKED = "auction.lot.award-revoked"
    }

    private val executor: ExecutorService = Executors.newFixedThreadPool(4)

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts consumer threads for auction event subjects.
     * Called automatically at application startup via the [Startup] annotation.
     */
    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.infof("Starting AuctionEventSellerConsumer [durable=%s]", DURABLE_NAME)

        executor.submit { createBidPlacedConsumer().start() }
        executor.submit { createLotClosedConsumer().start() }
        executor.submit { createLotAwardedConsumer().start() }
        executor.submit { createLotAwardRevokedConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.infof("Stopping AuctionEventSellerConsumer [durable=%s]", DURABLE_NAME)
        executor.shutdownNow()
    }

    // -------------------------------------------------------------------------
    // Consumer factories
    // -------------------------------------------------------------------------

    private fun createBidPlacedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-bid-placed",
            filterSubject = SUBJECT_BID_PLACED,
            maxRedeliveries = 5,
            deadLetterSubject = "seller.dlq.auction.bid.placed",
            batchSize = 20
        ) {
            override fun handleMessage(message: Message) {
                handleBidPlaced(message)
            }
        }

    private fun createLotClosedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-lot-closed",
            filterSubject = SUBJECT_LOT_CLOSED,
            maxRedeliveries = 5,
            deadLetterSubject = "seller.dlq.auction.lot.closed",
            batchSize = 20
        ) {
            override fun handleMessage(message: Message) {
                handleLotClosed(message)
            }
        }

    private fun createLotAwardedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-lot-awarded",
            filterSubject = SUBJECT_LOT_AWARDED,
            maxRedeliveries = 5,
            deadLetterSubject = "seller.dlq.auction.lot.awarded",
            batchSize = 20
        ) {
            override fun handleMessage(message: Message) {
                handleLotAwarded(message)
            }
        }

    private fun createLotAwardRevokedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-lot-award-revoked",
            filterSubject = SUBJECT_LOT_AWARD_REVOKED,
            maxRedeliveries = 5,
            deadLetterSubject = "seller.dlq.auction.lot.award-revoked",
            batchSize = 20
        ) {
            override fun handleMessage(message: Message) {
                handleLotAwardRevoked(message)
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
    private fun handleBidPlaced(message: Message) {
        val payload = String(message.data, Charsets.UTF_8)
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
    private fun handleLotClosed(message: Message) {
        val payload = String(message.data, Charsets.UTF_8)
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
    private fun handleLotAwarded(message: Message) {
        val payload = String(message.data, Charsets.UTF_8)
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

    /**
     * Handles `auction.lot.award-revoked` events by reverting the lot status
     * from AWARDED back to CLOSED, removing any READY settlement records,
     * and reversing seller metrics.
     *
     * Uses `originalHammerPrice` from [AwardRevokedEvent] to subtract from
     * total_hammer_sales and decrement pending_settlements.
     */
    private fun handleLotAwardRevoked(message: Message) {
        val payload = String(message.data, Charsets.UTF_8)
        val node = JsonMapper.instance.readTree(payload)
        val lotId = extractLotId(node) ?: return

        val sellerId = sellerProfileRepository.findSellerIdByLotId(lotId)
        if (sellerId == null) {
            LOG.debugf("No seller found for lot %s -- skipping lot.award-revoked", lotId)
            return
        }

        // Revert lot status from AWARDED back to CLOSED
        sellerProfileRepository.updateLotStatus(lotId, "CLOSED")

        // Remove any READY (unpaid) settlement records for this lot
        val deletedSettlements = sellerProfileRepository.deleteSettlementByLotId(lotId)
        LOG.debugf("Deleted %d READY settlement(s) for lot %s", deletedSettlements, lotId)

        // Reverse seller metrics (undo addHammerSale from lot.closed handler)
        val hammerPrice = node.optionalDecimal("originalHammerPrice")
        if (hammerPrice != null && hammerPrice > BigDecimal.ZERO) {
            sellerProfileRepository.revertHammerSale(sellerId, hammerPrice)
        }

        val reason = node.get("reason")?.asText() ?: "unknown"
        LOG.infof("Reverted award for seller %s (lot=%s, reason=%s)", sellerId, lotId, reason)
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
