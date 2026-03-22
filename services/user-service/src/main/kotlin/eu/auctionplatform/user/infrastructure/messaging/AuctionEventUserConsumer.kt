package eu.auctionplatform.user.infrastructure.messaging

import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import io.agroal.api.AgroalDataSource
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * NATS JetStream consumer that listens for auction domain events and
 * projects them into user-service read model tables.
 *
 * ## Subscribed events
 *
 * - `auction.bid.placed` -- Inserts a row into `app.user_bids` so the buyer
 *   can see their bid history via `GET /api/v1/users/me/bids`.
 * - `auction.lot.awarded` -- Inserts a row into `app.user_purchases` so the
 *   winner can see their purchases via `GET /api/v1/users/me/purchases`.
 *
 * Uses the CDI-managed NATS [Connection] and inner [NatsConsumer] instances
 * for durable, at-least-once delivery.
 */
@ApplicationScoped
@Startup
class AuctionEventUserConsumer @Inject constructor(
    private val connection: Connection,
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(AuctionEventUserConsumer::class.java)

        private const val STREAM_NAME = "AUCTION"
        private const val DURABLE_NAME = "user-auction-consumer"
    }

    private val executor: ExecutorService = Executors.newFixedThreadPool(2)

    /**
     * Starts consumer threads for auction event subjects.
     */
    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.info("Starting auction event user consumers")

        executor.submit { createBidPlacedConsumer().start() }
        executor.submit { createLotAwardedConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.info("Shutting down auction event user consumers")
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
            filterSubject = NatsSubjects.AUCTION_BID_PLACED,
            deadLetterSubject = "dlq.user.auction.bid.placed"
        ) {
            override fun handleMessage(message: Message) {
                handleBidPlaced(message)
            }
        }

    private fun createLotAwardedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-lot-awarded",
            filterSubject = NatsSubjects.AUCTION_LOT_AWARDED,
            deadLetterSubject = "dlq.user.auction.lot.awarded"
        ) {
            override fun handleMessage(message: Message) {
                handleLotAwarded(message)
            }
        }

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    /**
     * Handles `auction.bid.placed` events.
     *
     * Inserts a row into `app.user_bids` with the bidder's user ID,
     * auction ID, lot ID, bid amount, and timestamp.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleBidPlaced(message: Message) {
        val payload = parsePayload(message) ?: return

        val bidderId = payload["bidderId"]?.toString() ?: run {
            LOG.warn("auction.bid.placed event missing bidderId -- skipping")
            return
        }
        val auctionId = payload["aggregateId"]?.toString() ?: payload["auctionId"]?.toString() ?: ""
        val lotId = payload["lotId"]?.toString()
        val bidAmount = payload["bidAmount"]?.toString() ?: payload["amount"]?.toString() ?: "0"
        val currency = payload["bidCurrency"]?.toString() ?: payload["currency"]?.toString() ?: "USD"
        val timestamp = payload["timestamp"]?.toString()?.let {
            try { Instant.parse(it) } catch (_: Exception) { Instant.now() }
        } ?: Instant.now()

        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO app.user_bids (user_id, auction_id, lot_id, amount, currency, bid_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, auction_id, bid_at) DO NOTHING
                """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, UUID.fromString(bidderId))
                stmt.setObject(2, UUID.fromString(auctionId))
                stmt.setObject(3, lotId?.let { UUID.fromString(it) })
                stmt.setBigDecimal(4, BigDecimal(bidAmount))
                stmt.setString(5, currency)
                stmt.setTimestamp(6, Timestamp.from(timestamp))
                stmt.executeUpdate()
            }
        }

        LOG.debugf("Projected bid for user=%s auction=%s amount=%s", bidderId, auctionId, bidAmount)
    }

    /**
     * Handles `auction.lot.awarded` events.
     *
     * Inserts a row into `app.user_purchases` with the winner's user ID,
     * lot ID, auction ID, hammer price, and award timestamp.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleLotAwarded(message: Message) {
        val payload = parsePayload(message) ?: return

        val winnerId = payload["winnerId"]?.toString() ?: run {
            LOG.warn("auction.lot.awarded event missing winnerId -- skipping")
            return
        }
        val lotId = payload["lotId"]?.toString() ?: run {
            LOG.warn("auction.lot.awarded event missing lotId -- skipping")
            return
        }
        val auctionId = payload["aggregateId"]?.toString() ?: payload["auctionId"]?.toString()
        val hammerPrice = payload["finalBidAmount"]?.toString() ?: payload["hammerPrice"]?.toString()
        val currency = payload["finalBidCurrency"]?.toString() ?: payload["currency"]?.toString() ?: "USD"
        val timestamp = payload["timestamp"]?.toString()?.let {
            try { Instant.parse(it) } catch (_: Exception) { Instant.now() }
        } ?: Instant.now()

        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO app.user_purchases (user_id, lot_id, auction_id, hammer_price, currency, status, awarded_at)
                VALUES (?, ?, ?, ?, ?, 'PENDING_PAYMENT', ?)
                ON CONFLICT (user_id, lot_id) DO NOTHING
                """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, UUID.fromString(winnerId))
                stmt.setObject(2, UUID.fromString(lotId))
                stmt.setObject(3, auctionId?.let { UUID.fromString(it) })
                stmt.setBigDecimal(4, hammerPrice?.let { BigDecimal(it) })
                stmt.setString(5, currency)
                stmt.setTimestamp(6, Timestamp.from(timestamp))
                stmt.executeUpdate()
            }
        }

        LOG.debugf("Projected purchase for user=%s lot=%s hammerPrice=%s", winnerId, lotId, hammerPrice)
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
            LOG.errorf(
                ex, "Failed to parse auction event payload on subject %s: %s",
                message.subject, ex.message
            )
            null
        }
    }
}
