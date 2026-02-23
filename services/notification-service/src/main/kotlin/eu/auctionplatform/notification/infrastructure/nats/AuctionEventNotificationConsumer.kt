package eu.auctionplatform.notification.infrastructure.nats

import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.notification.application.service.NotificationService
import eu.auctionplatform.notification.domain.model.NotificationType
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * NATS JetStream consumer that listens for auction-related domain events
 * and triggers the appropriate notifications.
 *
 * Subscribed subjects:
 * - `auction.bid.placed.>` -- Triggers [NotificationType.OVERBID] to the
 *   previous high bidder and [NotificationType.BID_CONFIRMED] to the current bidder.
 * - `auction.bid.proxy.>` -- Triggers [NotificationType.AUTO_BID_TRIGGERED]
 *   to the auto-bidder.
 * - `auction.lot.closed.>` -- Triggers [NotificationType.AUCTION_WON] to the winner.
 *
 * Uses the durable consumer name `notification-auction-consumer` to survive
 * restarts and ensure exactly-once processing semantics (via JetStream).
 */
@ApplicationScoped
@Startup
class AuctionEventNotificationConsumer @Inject constructor(
    private val connection: Connection,
    private val notificationService: NotificationService
) {

    private val logger = LoggerFactory.getLogger(AuctionEventNotificationConsumer::class.java)

    companion object {
        private const val STREAM_NAME = "AUCTION"
        private const val DURABLE_NAME = "notification-auction-consumer"

        /** Filter subjects for all auction events this consumer cares about. */
        private const val BID_PLACED_FILTER = "auction.bid.placed.>"
        private const val BID_PROXY_FILTER = "auction.bid.proxy.>"
        private const val LOT_CLOSED_FILTER = "auction.lot.closed.>"
    }

    private val executor: ExecutorService = Executors.newFixedThreadPool(3)

    /**
     * Starts three consumer threads, one for each auction event subject filter.
     * Called automatically at application startup via the [Startup] annotation.
     */
    @jakarta.annotation.PostConstruct
    fun init() {
        logger.info("Starting auction event notification consumers")

        executor.submit { createBidPlacedConsumer().start() }
        executor.submit { createBidProxyConsumer().start() }
        executor.submit { createLotClosedConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        logger.info("Shutting down auction event notification consumers")
        executor.shutdownNow()
    }

    // -----------------------------------------------------------------------
    // Consumer factories
    // -----------------------------------------------------------------------

    private fun createBidPlacedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-bid-placed",
            filterSubject = BID_PLACED_FILTER,
            deadLetterSubject = "dlq.notification.auction.bid.placed"
        ) {
            override fun handleMessage(message: Message) {
                handleBidPlaced(message)
            }
        }

    private fun createBidProxyConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-bid-proxy",
            filterSubject = BID_PROXY_FILTER,
            deadLetterSubject = "dlq.notification.auction.bid.proxy"
        ) {
            override fun handleMessage(message: Message) {
                handleBidProxy(message)
            }
        }

    private fun createLotClosedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-lot-closed",
            filterSubject = LOT_CLOSED_FILTER,
            deadLetterSubject = "dlq.notification.auction.lot.closed"
        ) {
            override fun handleMessage(message: Message) {
                handleLotClosed(message)
            }
        }

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    /**
     * Handles `auction.bid.placed.>` events.
     *
     * Sends [NotificationType.BID_CONFIRMED] to the bidder and, if there was
     * a previous high bidder, sends [NotificationType.OVERBID] to them.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleBidPlaced(message: Message) {
        val payload = parsePayload(message) ?: return

        val bidderId = payload["bidderId"]?.toString() ?: return
        val bidAmount = payload["bidAmount"]?.toString() ?: "0"
        val bidCurrency = payload["bidCurrency"]?.toString() ?: "EUR"
        val aggregateId = payload["aggregateId"]?.toString() ?: ""
        val previousHighBidderId = payload["previousHighBidderId"]?.toString()

        // Notify the bidder that their bid was confirmed
        val bidConfirmData = mapOf(
            "auctionId" to aggregateId,
            "bidAmount" to bidAmount,
            "bidCurrency" to bidCurrency
        )
        notificationService.sendNotification(
            userId = UUID.fromString(bidderId),
            type = NotificationType.BID_CONFIRMED,
            data = bidConfirmData
        )

        logger.debug("Sent BID_CONFIRMED to bidder={} for auction={}", bidderId, aggregateId)

        // Notify the previous high bidder that they've been outbid
        if (!previousHighBidderId.isNullOrBlank() && previousHighBidderId != bidderId) {
            val overbidData = mapOf(
                "auctionId" to aggregateId,
                "newBidAmount" to bidAmount,
                "newBidCurrency" to bidCurrency
            )
            notificationService.sendNotification(
                userId = UUID.fromString(previousHighBidderId),
                type = NotificationType.OVERBID,
                data = overbidData
            )

            logger.debug(
                "Sent OVERBID to previousBidder={} for auction={}",
                previousHighBidderId, aggregateId
            )
        }
    }

    /**
     * Handles `auction.bid.proxy.>` events.
     *
     * Sends [NotificationType.AUTO_BID_TRIGGERED] to the auto-bidder.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleBidProxy(message: Message) {
        val payload = parsePayload(message) ?: return

        val bidderId = payload["bidderId"]?.toString() ?: return
        val bidAmount = payload["bidAmount"]?.toString() ?: "0"
        val bidCurrency = payload["bidCurrency"]?.toString() ?: "EUR"
        val aggregateId = payload["aggregateId"]?.toString() ?: ""
        val maxAutoBidAmount = payload["maxAutoBidAmount"]?.toString() ?: "0"
        val maxAutoBidCurrency = payload["maxAutoBidCurrency"]?.toString() ?: "EUR"

        val data = mapOf(
            "auctionId" to aggregateId,
            "bidAmount" to bidAmount,
            "bidCurrency" to bidCurrency,
            "maxAutoBidAmount" to maxAutoBidAmount,
            "maxAutoBidCurrency" to maxAutoBidCurrency
        )

        notificationService.sendNotification(
            userId = UUID.fromString(bidderId),
            type = NotificationType.AUTO_BID_TRIGGERED,
            data = data
        )

        logger.debug("Sent AUTO_BID_TRIGGERED to bidder={} for auction={}", bidderId, aggregateId)
    }

    /**
     * Handles `auction.lot.closed.>` events.
     *
     * Sends [NotificationType.AUCTION_WON] to the winner if the auction
     * had a winner and the reserve was met.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleLotClosed(message: Message) {
        val payload = parsePayload(message) ?: return

        val winnerId = payload["winnerId"]?.toString()
        val reserveMet = payload["reserveMet"]?.toString()?.toBoolean() ?: false
        val aggregateId = payload["aggregateId"]?.toString() ?: ""
        val finalBidAmount = payload["finalBidAmount"]?.toString() ?: "0"
        val finalBidCurrency = payload["finalBidCurrency"]?.toString() ?: "EUR"

        if (winnerId.isNullOrBlank() || !reserveMet) {
            logger.debug(
                "Auction {} closed without a winner or reserve not met (winnerId={}, reserveMet={})",
                aggregateId, winnerId, reserveMet
            )
            return
        }

        val data = mapOf(
            "auctionId" to aggregateId,
            "finalBidAmount" to finalBidAmount,
            "finalBidCurrency" to finalBidCurrency
        )

        notificationService.sendNotification(
            userId = UUID.fromString(winnerId),
            type = NotificationType.AUCTION_WON,
            data = data
        )

        logger.debug("Sent AUCTION_WON to winner={} for auction={}", winnerId, aggregateId)
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun parsePayload(message: Message): Map<String, Any>? {
        return try {
            val json = String(message.data, Charsets.UTF_8)
            JsonMapper.instance.readValue(json, Map::class.java) as Map<String, Any>
        } catch (ex: Exception) {
            logger.error(
                "Failed to parse message payload on subject {}: {}",
                message.subject, ex.message, ex
            )
            null
        }
    }
}
