package eu.auctionplatform.notification.infrastructure.nats

import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import java.math.BigDecimal
import eu.auctionplatform.notification.application.service.NotificationService
import eu.auctionplatform.notification.domain.model.NotificationType
import eu.auctionplatform.notification.infrastructure.UserEmailResolver
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
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
 * - `auction.reserve.met` -- Triggers [NotificationType.RESERVE_MET] to the bidder
 *   whose bid met the reserve price.
 *
 * Uses the durable consumer name `notification-auction-consumer` to survive
 * restarts and ensure exactly-once processing semantics (via JetStream).
 */
@ApplicationScoped
@Startup
class AuctionEventNotificationConsumer @Inject constructor(
    private val connection: Connection,
    private val notificationService: NotificationService,
    private val userEmailResolver: UserEmailResolver
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(AuctionEventNotificationConsumer::class.java)

        private const val STREAM_NAME = "AUCTION"
        private const val DURABLE_NAME = "notification-auction-consumer"

        /** Filter subjects for all auction events this consumer cares about. */
        private const val BID_PLACED_FILTER = "auction.bid.placed"
        private const val BID_PROXY_FILTER = "auction.bid.proxy"
        private const val LOT_CLOSED_FILTER = "auction.lot.closed"
        private const val RESERVE_MET_FILTER = "auction.reserve.met"
    }

    private val executor: ExecutorService = Executors.newFixedThreadPool(4)

    /**
     * Starts three consumer threads, one for each auction event subject filter.
     * Called automatically at application startup via the [Startup] annotation.
     */
    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.info("Starting auction event notification consumers")

        executor.submit { createBidPlacedConsumer().start() }
        executor.submit { createBidProxyConsumer().start() }
        executor.submit { createLotClosedConsumer().start() }
        executor.submit { createReserveMetConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.info("Shutting down auction event notification consumers")
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

    private fun createReserveMetConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "notification-reserve-met",
            filterSubject = RESERVE_MET_FILTER,
            deadLetterSubject = "dlq.notification.auction.reserve.met"
        ) {
            override fun handleMessage(message: Message) {
                handleReserveMet(message)
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

        // Resolve bidder email for email delivery
        val bidderUuid = UUID.fromString(bidderId)
        val bidderEmail = userEmailResolver.resolveEmail(bidderUuid)

        // Notify the bidder that their bid was confirmed
        val bidConfirmData = mutableMapOf(
            "auctionId" to aggregateId,
            "bidAmount" to bidAmount,
            "bidCurrency" to bidCurrency
        )
        if (bidderEmail != null) bidConfirmData["email"] = bidderEmail

        notificationService.sendNotification(
            userId = bidderUuid,
            type = NotificationType.BID_CONFIRMED,
            data = bidConfirmData
        )

        LOG.debugf("Sent BID_CONFIRMED to bidder=%s for auction=%s", bidderId, aggregateId)

        // Notify the previous high bidder that they've been outbid
        if (!previousHighBidderId.isNullOrBlank() && previousHighBidderId != bidderId) {
            val previousBidderUuid = UUID.fromString(previousHighBidderId)
            val previousBidderEmail = userEmailResolver.resolveEmail(previousBidderUuid)

            val overbidData = mutableMapOf(
                "auctionId" to aggregateId,
                "newBidAmount" to bidAmount,
                "newBidCurrency" to bidCurrency
            )
            if (previousBidderEmail != null) overbidData["email"] = previousBidderEmail

            notificationService.sendNotification(
                userId = previousBidderUuid,
                type = NotificationType.OVERBID,
                data = overbidData
            )

            LOG.debugf(
                "Sent OVERBID to previousBidder=%s for auction=%s",
                previousHighBidderId, aggregateId
            )
        }
    }

    /**
     * Handles `auction.bid.proxy` events.
     *
     * Sends [NotificationType.AUTO_BID_TRIGGERED] to the auto-bid owner whose
     * proxy bid was placed by the engine in response to a competing bid.
     *
     * Extracts fields from [eu.auctionplatform.events.auction.ProxyBidTriggeredEvent]:
     * `autoBidOwnerId`, `amount`, `maxAmount`, `currency`, `triggeringBidId`.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleBidProxy(message: Message) {
        val payload = parsePayload(message) ?: return

        // ProxyBidTriggeredEvent uses autoBidOwnerId; fall back to bidderId for compat
        val ownerId = payload["autoBidOwnerId"]?.toString()
            ?: payload["bidderId"]?.toString()
            ?: return
        val bidAmount = payload["amount"]?.toString()
            ?: payload["bidAmount"]?.toString()
            ?: "0"
        val bidCurrency = payload["currency"]?.toString()
            ?: payload["bidCurrency"]?.toString()
            ?: "EUR"
        val aggregateId = payload["aggregateId"]?.toString() ?: ""
        val maxAmount = payload["maxAmount"]?.toString()
            ?: payload["maxAutoBidAmount"]?.toString()
            ?: "0"
        val triggeringBidId = payload["triggeringBidId"]?.toString() ?: ""

        val ownerUuid = UUID.fromString(ownerId)
        val ownerEmail = userEmailResolver.resolveEmail(ownerUuid)

        val remainingBudget = try {
            BigDecimal(maxAmount).subtract(BigDecimal(bidAmount)).toPlainString()
        } catch (_: Exception) { "0" }

        val data = mutableMapOf(
            "auctionId" to aggregateId,
            "bidAmount" to bidAmount,
            "bidCurrency" to bidCurrency,
            "maxAmount" to maxAmount,
            "remainingBudget" to remainingBudget,
            "triggeringBidId" to triggeringBidId
        )
        if (ownerEmail != null) data["email"] = ownerEmail

        notificationService.sendNotification(
            userId = ownerUuid,
            type = NotificationType.AUTO_BID_TRIGGERED,
            data = data
        )

        LOG.debugf("Sent AUTO_BID_TRIGGERED to autoBidOwner=%s for auction=%s", ownerId, aggregateId)
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
            LOG.debugf(
                "Auction %s closed without a winner or reserve not met (winnerId=%s, reserveMet=%s)",
                aggregateId, winnerId, reserveMet
            )
            return
        }

        val winnerUuid = UUID.fromString(winnerId)
        val winnerEmail = userEmailResolver.resolveEmail(winnerUuid)

        val data = mutableMapOf(
            "auctionId" to aggregateId,
            "finalBidAmount" to finalBidAmount,
            "finalBidCurrency" to finalBidCurrency
        )
        if (winnerEmail != null) data["email"] = winnerEmail

        notificationService.sendNotification(
            userId = winnerUuid,
            type = NotificationType.AUCTION_WON,
            data = data
        )

        LOG.debugf("Sent AUCTION_WON to winner=%s for auction=%s", winnerId, aggregateId)
    }

    /**
     * Handles `auction.reserve.met` events.
     *
     * Sends [NotificationType.RESERVE_MET] to the bidder whose bid met the
     * seller's reserve price, informing them the lot is now eligible to sell.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleReserveMet(message: Message) {
        val payload = parsePayload(message) ?: return

        val auctionId = payload["aggregateId"]?.toString() ?: ""
        val lotId = payload["lotId"]?.toString() ?: ""
        val bidderId = payload["bidderId"]?.toString() ?: return
        val reservePrice = payload["reservePrice"]?.toString() ?: ""
        val currentBid = payload["currentBid"]?.toString() ?: ""

        val bidderUuid = UUID.fromString(bidderId)
        val bidderEmail = userEmailResolver.resolveEmail(bidderUuid)

        val data = mutableMapOf<String, Any>(
            "auctionId" to auctionId,
            "lotId" to lotId,
            "reservePrice" to reservePrice,
            "currentBid" to currentBid
        )
        if (bidderEmail != null) data["email"] = bidderEmail

        notificationService.sendNotification(
            userId = bidderUuid,
            type = NotificationType.RESERVE_MET,
            data = data
        )

        LOG.debugf(
            "Sent RESERVE_MET to bidder=%s for auction=%s (lot=%s)",
            bidderId, auctionId, lotId
        )
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
            LOG.errorf(ex,
                "Failed to parse message payload on subject %s: %s",
                message.subject, ex.message
            )
            null
        }
    }
}
