package eu.auctionplatform.analytics.infrastructure.nats

import com.fasterxml.jackson.databind.JsonNode
import eu.auctionplatform.analytics.domain.model.AuctionMetrics
import eu.auctionplatform.analytics.infrastructure.persistence.repository.AnalyticsRepository
import eu.auctionplatform.analytics.infrastructure.persistence.repository.DailyRevenueEntry
import eu.auctionplatform.analytics.infrastructure.persistence.repository.UserGrowthEntry
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
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

// =============================================================================
// Analytics Event Consumer -- NATS JetStream subscriber for ALL domain events
// =============================================================================

/**
 * Consumes domain events from all bounded contexts and maintains analytics
 * counters and aggregate tables.
 *
 * Subscribed subject: `>` (all subjects on the ANALYTICS stream, which should
 * be configured to mirror relevant subjects from other streams).
 *
 * Handles the following event types:
 * - `auction.bid.placed.*`         -- increments bid counters
 * - `auction.lot.extended.*`       -- increments extension counters
 * - `auction.lot.closed.*`         -- updates auction metrics
 * - `payment.checkout.completed.*` -- updates daily revenue
 * - `user.registered.*`            -- updates user growth
 *
 * Uses a durable pull consumer named "analytics-all-consumer" to ensure
 * at-least-once delivery and survive restarts.
 */
@ApplicationScoped
class AnalyticsEventConsumer @Inject constructor(
    private val natsConnection: Connection,
    private val analyticsRepository: AnalyticsRepository
) : NatsConsumer(
    connection = natsConnection,
    streamName = "ANALYTICS",
    durableName = DURABLE_NAME,
    filterSubject = FILTER_SUBJECT,
    maxRedeliveries = 5,
    deadLetterSubject = "analytics.dlq",
    batchSize = 50
) {

    private val logger = LoggerFactory.getLogger(AnalyticsEventConsumer::class.java)

    private var consumerThread: Thread? = null

    companion object {
        /** Durable consumer name -- persists across restarts. */
        const val DURABLE_NAME: String = "analytics-all-consumer"

        /** Subject filter matching all domain events. */
        const val FILTER_SUBJECT: String = ">"
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts the consumer loop in a dedicated daemon thread on application startup.
     */
    fun onStart(@Observes event: StartupEvent) {
        logger.info("Starting AnalyticsEventConsumer [durable={}]", DURABLE_NAME)
        consumerThread = Thread({
            start()
        }, "analytics-event-consumer").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Signals the consumer loop to stop gracefully on application shutdown.
     */
    fun onStop(@Observes event: ShutdownEvent) {
        logger.info("Stopping AnalyticsEventConsumer [durable={}]", DURABLE_NAME)
        stop()
        consumerThread?.interrupt()
    }

    // -------------------------------------------------------------------------
    // Message handling
    // -------------------------------------------------------------------------

    /**
     * Routes an incoming NATS message to the appropriate handler based on
     * the subject pattern.
     */
    override fun handleMessage(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        logger.debug("Received analytics event on subject [{}], payload size={} bytes",
            subject, message.data.size)

        try {
            when {
                subject.startsWith("auction.bid.placed") -> handleBidPlaced(payload)
                subject.startsWith("auction.lot.extended") -> handleLotExtended(payload)
                subject.startsWith("auction.lot.closed") -> handleLotClosed(payload)
                subject.startsWith("payment.checkout.completed") -> handleCheckoutCompleted(payload)
                subject.startsWith("user.registered") -> handleUserRegistered(payload)
                else -> logger.debug("Unhandled analytics event on subject [{}] -- skipping", subject)
            }
        } catch (ex: Exception) {
            logger.error("Failed to process analytics event on [{}]: {}", subject, ex.message, ex)
            throw ex // re-throw so the base class handles nak/dead-letter
        }
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    /**
     * Handles bid placement events by incrementing the bid counter for the auction.
     */
    private fun handleBidPlaced(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val auctionId = node.uuidField("auctionId") ?: node.uuidField("aggregateId")

        if (auctionId == null) {
            logger.warn("Bid placed event missing auctionId -- skipping")
            return
        }

        analyticsRepository.incrementBidCount(auctionId)
        logger.debug("Incremented bid count for auction [{}]", auctionId)
    }

    /**
     * Handles lot extension events by incrementing the extension counter.
     */
    private fun handleLotExtended(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val auctionId = node.uuidField("auctionId") ?: node.uuidField("aggregateId")

        if (auctionId == null) {
            logger.warn("Lot extended event missing auctionId -- skipping")
            return
        }

        analyticsRepository.incrementExtensionCount(auctionId)
        logger.debug("Incremented extension count for auction [{}]", auctionId)
    }

    /**
     * Handles auction close events by upserting full auction metrics.
     */
    private fun handleLotClosed(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val auctionId = node.uuidField("auctionId") ?: node.uuidField("aggregateId") ?: return

        val metrics = AuctionMetrics(
            auctionId = auctionId,
            totalBids = node.longField("totalBids") ?: 0L,
            uniqueBidders = node.intField("uniqueBidders") ?: 0,
            avgBidAmount = node.decimalField("avgBidAmount") ?: BigDecimal.ZERO,
            maxBid = node.decimalField("maxBid") ?: node.decimalField("winningBid") ?: BigDecimal.ZERO,
            extensionCount = node.intField("extensionCount") ?: 0,
            durationSeconds = node.longField("durationSeconds") ?: 0L
        )

        analyticsRepository.upsertAuctionMetrics(metrics)
        logger.info("Updated auction metrics for auction [{}]: {} bids, max={}", auctionId, metrics.totalBids, metrics.maxBid)
    }

    /**
     * Handles checkout completion events by updating daily revenue.
     */
    private fun handleCheckoutCompleted(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val amount = node.decimalField("amount") ?: node.decimalField("totalAmount") ?: return

        val today = LocalDate.now(ZoneOffset.UTC)
        val entry = DailyRevenueEntry(
            reportDate = today,
            revenueEur = amount,
            transactionCount = 1,
            avgTransactionEur = amount
        )

        analyticsRepository.upsertDailyRevenue(entry)
        logger.debug("Updated daily revenue for {}: +{} EUR", today, amount)
    }

    /**
     * Handles user registration events by updating user growth.
     */
    private fun handleUserRegistered(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val accountType = node.textField("accountType") ?: ""

        val today = LocalDate.now(ZoneOffset.UTC)
        val entry = UserGrowthEntry(
            reportDate = today,
            newRegistrations = 1,
            totalUsers = 0, // will be updated by periodic aggregation
            newBuyers = if (accountType.equals("BUYER", ignoreCase = true)) 1 else 0,
            newSellers = if (accountType.equals("SELLER", ignoreCase = true)) 1 else 0
        )

        analyticsRepository.upsertUserGrowth(entry)
        logger.debug("Updated user growth for {}: +1 registration (type={})", today, accountType)
    }

    // -------------------------------------------------------------------------
    // JSON extraction helpers
    // -------------------------------------------------------------------------

    private fun JsonNode.textField(field: String): String? =
        this.get(field)?.takeIf { !it.isNull }?.asText()

    private fun JsonNode.intField(field: String): Int? =
        this.get(field)?.takeIf { !it.isNull }?.asInt()

    private fun JsonNode.longField(field: String): Long? =
        this.get(field)?.takeIf { !it.isNull }?.asLong()

    private fun JsonNode.decimalField(field: String): BigDecimal? =
        this.get(field)?.takeIf { !it.isNull }?.decimalValue()

    private fun JsonNode.uuidField(field: String): UUID? =
        this.get(field)?.takeIf { !it.isNull }?.asText()?.let {
            try { UUID.fromString(it) } catch (_: Exception) { null }
        }
}
