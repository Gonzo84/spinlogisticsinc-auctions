package eu.auctionplatform.gateway.infrastructure.nats

import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.gateway.infrastructure.websocket.WebSocketHub
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.slf4j.LoggerFactory

// =============================================================================
// Bid Event Forwarder – NATS-to-WebSocket bridge for auction events
// =============================================================================

/**
 * Subscribes to real-time auction events from NATS JetStream and forwards them
 * to connected WebSocket clients via the [WebSocketHub].
 *
 * Subscribed subjects:
 * - `auction.bid.placed.>` -- new bid placed on a lot
 * - `auction.lot.extended.>` -- lot closing time extended (anti-sniping)
 * - `auction.lot.closed.>` -- lot closed / bidding ended
 *
 * The `>` wildcard matches the trailing brand segment, e.g.
 * `auction.bid.placed.troostwijk`.
 *
 * Uses a durable pull consumer named "gateway-auction-consumer" on the
 * "AUCTION" stream to ensure at-least-once delivery and survive restarts.
 */
@ApplicationScoped
class BidEventForwarder @Inject constructor(
    private val connection: Connection,
    private val webSocketHub: WebSocketHub
) : NatsConsumer(
    connection = connection,
    streamName = "AUCTION",
    durableName = DURABLE_NAME,
    filterSubject = FILTER_SUBJECT,
    maxRedeliveries = 3,
    deadLetterSubject = "gateway.dlq.auction",
    batchSize = 50
) {

    private val logger = LoggerFactory.getLogger(BidEventForwarder::class.java)

    private var consumerThread: Thread? = null

    companion object {
        /** Durable consumer name -- persists across restarts. */
        const val DURABLE_NAME: String = "gateway-auction-consumer"

        /**
         * Subject filter matching all auction events with brand suffix.
         * The NATS `>` wildcard matches one or more trailing tokens.
         */
        const val FILTER_SUBJECT: String = "auction.>"
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts the consumer loop in a dedicated daemon thread on application startup.
     */
    fun onStart(@Observes event: StartupEvent) {
        logger.info("Starting BidEventForwarder [durable={}]", DURABLE_NAME)
        consumerThread = Thread({
            start()
        }, "bid-event-forwarder").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Signals the consumer loop to stop gracefully on application shutdown.
     */
    fun onStop(@Observes event: ShutdownEvent) {
        logger.info("Stopping BidEventForwarder [durable={}]", DURABLE_NAME)
        stop()
        consumerThread?.interrupt()
    }

    // -------------------------------------------------------------------------
    // Message handling
    // -------------------------------------------------------------------------

    /**
     * Routes an incoming NATS message to the appropriate WebSocket broadcast
     * based on the subject pattern.
     *
     * Subject convention: `auction.<entity>.<action>.<brand>`
     * - `auction.bid.placed.<brand>` -- forward as "bid_placed" event
     * - `auction.lot.extended.<brand>` -- forward as "lot_extended" event
     * - `auction.lot.closed.<brand>` -- forward as "lot_closed" event
     */
    override fun handleMessage(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        logger.debug(
            "Received auction event on subject [{}], payload size={} bytes",
            subject, message.data.size
        )

        try {
            when {
                subject.startsWith(NatsSubjects.AUCTION_BID_PLACED) ->
                    forwardBidPlaced(payload)

                subject.startsWith(NatsSubjects.AUCTION_LOT_EXTENDED) ->
                    forwardLotExtended(payload)

                subject.startsWith(NatsSubjects.AUCTION_LOT_CLOSED) ->
                    forwardLotClosed(payload)

                subject.startsWith(NatsSubjects.AUCTION_LOT_AWARDED) ->
                    forwardLotAwarded(payload)

                else ->
                    logger.debug("Ignoring non-forwardable auction subject [{}]", subject)
            }
        } catch (ex: Exception) {
            logger.error(
                "Failed to forward auction event on [{}]: {}",
                subject, ex.message, ex
            )
            throw ex // re-throw so the base class handles nak/dead-letter
        }
    }

    // -------------------------------------------------------------------------
    // Event forwarders
    // -------------------------------------------------------------------------

    /**
     * Forwards a bid-placed event to all WebSocket clients watching the auction.
     *
     * The broadcast message includes the event type, auction ID, bid details,
     * and the server timestamp for clock synchronisation.
     */
    private fun forwardBidPlaced(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val auctionId = extractAuctionId(node) ?: return

        val wsMessage = JsonMapper.toJson(mapOf(
            "type" to "bid_placed",
            "auctionId" to auctionId,
            "data" to mapOf(
                "bidId" to node.path("bidId").asText(null),
                "bidderId" to maskBidderId(node.path("bidderId").asText(null)),
                "amount" to node.path("amount").asText(null),
                "currency" to node.path("currency").asText("EUR"),
                "isProxy" to node.path("isProxy").asBoolean(false),
                "bidCount" to node.path("bidCount").asInt(0),
                "timestamp" to node.path("timestamp").asText(null)
            ),
            "serverTime" to java.time.Instant.now().toString()
        ))

        webSocketHub.broadcast(auctionId, wsMessage)
        logger.debug("Forwarded bid_placed to auction [{}]", auctionId)
    }

    /**
     * Forwards a lot-extended event (anti-sniping) to all WebSocket clients.
     *
     * This is critical for UI countdown timers -- clients must update their
     * displayed closing time immediately.
     */
    private fun forwardLotExtended(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val auctionId = extractAuctionId(node) ?: return

        val wsMessage = JsonMapper.toJson(mapOf(
            "type" to "lot_extended",
            "auctionId" to auctionId,
            "data" to mapOf(
                "newEndTime" to node.path("newEndTime").asText(null),
                "previousEndTime" to node.path("previousEndTime").asText(null),
                "extensionSeconds" to node.path("extensionSeconds").asInt(0),
                "extensionCount" to node.path("extensionCount").asInt(0),
                "reason" to node.path("reason").asText("anti_sniping")
            ),
            "serverTime" to java.time.Instant.now().toString()
        ))

        webSocketHub.broadcast(auctionId, wsMessage)
        logger.debug("Forwarded lot_extended to auction [{}]", auctionId)
    }

    /**
     * Forwards a lot-closed event to all WebSocket clients watching the auction.
     *
     * After receiving this event, clients should transition the auction UI to
     * a "closed" state and stop accepting bid input.
     */
    private fun forwardLotClosed(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val auctionId = extractAuctionId(node) ?: return

        val wsMessage = JsonMapper.toJson(mapOf(
            "type" to "lot_closed",
            "auctionId" to auctionId,
            "data" to mapOf(
                "winningBidId" to node.path("winningBidId").asText(null),
                "winningAmount" to node.path("winningAmount").asText(null),
                "currency" to node.path("currency").asText("EUR"),
                "bidCount" to node.path("bidCount").asInt(0),
                "closedAt" to node.path("closedAt").asText(null),
                "reserveMet" to node.path("reserveMet").asBoolean(true)
            ),
            "serverTime" to java.time.Instant.now().toString()
        ))

        webSocketHub.broadcast(auctionId, wsMessage)
        logger.debug("Forwarded lot_closed to auction [{}]", auctionId)
    }

    /**
     * Forwards a lot-awarded event to all WebSocket clients.
     */
    private fun forwardLotAwarded(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val auctionId = extractAuctionId(node) ?: return

        val wsMessage = JsonMapper.toJson(mapOf(
            "type" to "lot_awarded",
            "auctionId" to auctionId,
            "data" to mapOf(
                "winnerId" to maskBidderId(node.path("winnerId").asText(null)),
                "amount" to node.path("amount").asText(null),
                "currency" to node.path("currency").asText("EUR"),
                "awardedAt" to node.path("awardedAt").asText(null)
            ),
            "serverTime" to java.time.Instant.now().toString()
        ))

        webSocketHub.broadcast(auctionId, wsMessage)
        logger.debug("Forwarded lot_awarded to auction [{}]", auctionId)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the auction ID from a JSON event payload, checking multiple
     * possible field names.
     */
    private fun extractAuctionId(node: com.fasterxml.jackson.databind.JsonNode): String? {
        val auctionId = node.path("auctionId").asText(null)
            ?: node.path("aggregateId").asText(null)

        if (auctionId.isNullOrBlank()) {
            logger.warn("Auction event missing auctionId/aggregateId -- cannot forward")
            return null
        }

        return auctionId
    }

    /**
     * Masks a bidder ID for broadcast to prevent revealing full user
     * identities to other participants. Shows only the first 4 and last 4
     * characters of the UUID.
     */
    private fun maskBidderId(bidderId: String?): String? {
        if (bidderId == null || bidderId.length <= 8) return bidderId
        return "${bidderId.take(4)}****${bidderId.takeLast(4)}"
    }
}
