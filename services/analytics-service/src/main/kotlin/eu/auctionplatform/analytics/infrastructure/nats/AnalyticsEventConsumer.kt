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
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// =============================================================================
// Analytics Event Consumer -- NATS JetStream subscribers for domain events
// =============================================================================

/**
 * Consumes domain events from AUCTION, PAYMENT, and USER streams and maintains
 * analytics counters and aggregate tables.
 *
 * Creates three inner [NatsConsumer] instances, one per upstream stream:
 * - **auctionConsumer** (AUCTION stream, `auction.>`) -- handles bid.placed,
 *   lot.extended, lot.closed events
 * - **paymentConsumer** (PAYMENT stream, `payment.>`) -- handles
 *   checkout.completed events
 * - **userConsumer** (USER stream, `user.>`) -- handles user.registered events
 *
 * Each consumer runs on its own daemon thread with a dedicated durable name
 * to survive restarts and ensure at-least-once delivery.
 */
@ApplicationScoped
class AnalyticsEventConsumer @Inject constructor(
    private val natsConnection: Connection,
    private val analyticsRepository: AnalyticsRepository
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(AnalyticsEventConsumer::class.java)
    }

    private val executor: ExecutorService = Executors.newFixedThreadPool(3)

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts the three consumer threads on application startup.
     */
    @PostConstruct
    fun init() {
        LOG.info("Starting analytics event consumers (AUCTION, PAYMENT, USER)")

        executor.submit { createAuctionConsumer().start() }
        executor.submit { createPaymentConsumer().start() }
        executor.submit { createUserConsumer().start() }
    }

    /**
     * Shuts down all consumer threads on application shutdown.
     */
    @PreDestroy
    fun shutdown() {
        LOG.info("Shutting down analytics event consumers")
        executor.shutdownNow()
    }

    // -------------------------------------------------------------------------
    // Consumer factories
    // -------------------------------------------------------------------------

    /**
     * Creates a consumer for the AUCTION stream that handles bid.placed,
     * lot.extended, and lot.closed events.
     */
    private fun createAuctionConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = natsConnection,
            streamName = "AUCTION",
            durableName = "analytics-auction-consumer",
            filterSubject = "auction.>",
            maxRedeliveries = 5,
            deadLetterSubject = "dlq.analytics.auction",
            batchSize = 50
        ) {
            override fun handleMessage(message: Message) {
                handleAuctionEvent(message)
            }
        }

    /**
     * Creates a consumer for the PAYMENT stream that handles
     * checkout.completed events.
     */
    private fun createPaymentConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = natsConnection,
            streamName = "PAYMENT",
            durableName = "analytics-payment-consumer",
            filterSubject = "payment.>",
            maxRedeliveries = 5,
            deadLetterSubject = "dlq.analytics.payment",
            batchSize = 50
        ) {
            override fun handleMessage(message: Message) {
                handlePaymentEvent(message)
            }
        }

    /**
     * Creates a consumer for the USER stream that handles
     * user.registered events.
     */
    private fun createUserConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = natsConnection,
            streamName = "USER",
            durableName = "analytics-user-consumer",
            filterSubject = "user.>",
            maxRedeliveries = 5,
            deadLetterSubject = "dlq.analytics.user",
            batchSize = 50
        ) {
            override fun handleMessage(message: Message) {
                handleUserEvent(message)
            }
        }

    // -------------------------------------------------------------------------
    // Stream-level routers
    // -------------------------------------------------------------------------

    /**
     * Routes an incoming AUCTION stream message to the appropriate handler
     * based on the subject pattern.
     */
    private fun handleAuctionEvent(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        LOG.debugf("Received auction analytics event on subject [%s], payload size=%s bytes",
            subject, message.data.size)

        try {
            when {
                subject.startsWith("auction.bid.placed") -> handleBidPlaced(payload)
                subject.startsWith("auction.lot.extended") -> handleLotExtended(payload)
                subject.startsWith("auction.lot.closed") -> handleLotClosed(payload)
                else -> LOG.debugf("Unhandled auction event on subject [%s] -- skipping", subject)
            }
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to process auction analytics event on [%s]: %s", subject, ex.message)
            throw ex // re-throw so the base class handles nak/dead-letter
        }
    }

    /**
     * Routes an incoming PAYMENT stream message to the appropriate handler.
     */
    private fun handlePaymentEvent(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        LOG.debugf("Received payment analytics event on subject [%s], payload size=%s bytes",
            subject, message.data.size)

        try {
            when {
                subject.startsWith("payment.settlement.settled") -> handleSettlementSettled(payload)
                subject.startsWith("payment.checkout.completed") -> handleCheckoutCompleted(payload)
                else -> LOG.debugf("Unhandled payment event on subject [%s] -- skipping", subject)
            }
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to process payment analytics event on [%s]: %s", subject, ex.message)
            throw ex
        }
    }

    /**
     * Routes an incoming USER stream message to the appropriate handler.
     */
    private fun handleUserEvent(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        LOG.debugf("Received user analytics event on subject [%s], payload size=%s bytes",
            subject, message.data.size)

        try {
            when {
                subject.startsWith("user.registered") -> handleUserRegistered(payload)
                else -> LOG.debugf("Unhandled user event on subject [%s] -- skipping", subject)
            }
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to process user analytics event on [%s]: %s", subject, ex.message)
            throw ex
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
            LOG.warn("Bid placed event missing auctionId -- skipping")
            return
        }

        analyticsRepository.incrementBidCount(auctionId)
        analyticsRepository.recordDailyBid(auctionId)
        LOG.debugf("Incremented bid count for auction [%s]", auctionId)
    }

    /**
     * Handles lot extension events by incrementing the extension counter.
     */
    private fun handleLotExtended(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val auctionId = node.uuidField("auctionId") ?: node.uuidField("aggregateId")

        if (auctionId == null) {
            LOG.warn("Lot extended event missing auctionId -- skipping")
            return
        }

        analyticsRepository.incrementExtensionCount(auctionId)
        LOG.debugf("Incremented extension count for auction [%s]", auctionId)
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
        LOG.infof("Updated auction metrics for auction [%s]: %s bids, max=%s", auctionId, metrics.totalBids, metrics.maxBid)
    }

    /**
     * Handles settlement completion events by recording commission revenue
     * in daily analytics.
     *
     * The commission amount represents platform revenue from each settled
     * transaction, separate from the total checkout amount already tracked
     * by [handleCheckoutCompleted].
     */
    private fun handleSettlementSettled(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val commission = node.decimalField("commission") ?: return

        val today = LocalDate.now(ZoneOffset.UTC)
        val entry = DailyRevenueEntry(
            reportDate = today,
            revenueEur = commission,
            transactionCount = 1,
            avgTransactionEur = commission
        )

        analyticsRepository.upsertDailyRevenue(entry)
        LOG.debugf("Recorded settlement commission revenue for %s: +%s EUR", today, commission)
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
        LOG.debugf("Updated daily revenue for %s: +%s EUR", today, amount)
    }

    /**
     * Handles user registration events by updating user growth.
     *
     * Note: The `accountType` field from user-service is BUSINESS or PRIVATE
     * (not BUYER/SELLER). We always increment `new_registrations` regardless
     * of account type.
     */
    private fun handleUserRegistered(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val accountType = node.textField("accountType") ?: ""

        val today = LocalDate.now(ZoneOffset.UTC)
        val entry = UserGrowthEntry(
            reportDate = today,
            newRegistrations = 1,
            totalUsers = 0, // will be updated by periodic aggregation
            newBuyers = 0,  // accountType is BUSINESS/PRIVATE, not BUYER/SELLER
            newSellers = 0
        )

        analyticsRepository.upsertUserGrowth(entry)
        LOG.debugf("Updated user growth for %s: +1 registration (type=%s)", today, accountType)
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
