package eu.auctionplatform.seller.infrastructure.nats

import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.seller.infrastructure.persistence.repository.SellerProfileRepository
import io.nats.client.Connection
import io.nats.client.JetStreamSubscription
import io.nats.client.Message
import io.nats.client.PullSubscribeOptions
import io.nats.client.api.ConsumerConfiguration
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.eclipse.microprofile.config.ConfigProvider
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * NATS JetStream consumer that listens for auction lifecycle events and
 * updates seller dashboard metrics accordingly.
 *
 * ## Subscribed events
 *
 * - `auction.bid.placed` -- increments the bid counter for the lot's seller.
 * - `auction.lot.closed` -- records hammer sale amount, decrements active lots.
 * - `auction.lot.awarded` -- marks the lot as awarded (used for settlement tracking).
 * - `payment.settlement.ready` -- records a completed settlement payment.
 * - `catalog.lot.created` -- increments active lots counter for the seller.
 *
 * The consumer uses a durable pull subscription for at-least-once delivery
 * guarantees across service restarts.
 */
@ApplicationScoped
class AuctionEventSellerConsumer @Inject constructor(
    private val sellerProfileRepository: SellerProfileRepository
) {

    private val logger = LoggerFactory.getLogger(AuctionEventSellerConsumer::class.java)

    private var connection: Connection? = null
    private val subscriptions = mutableListOf<JetStreamSubscription>()
    private var executor: ExecutorService? = null

    @Volatile
    private var running = false

    companion object {
        private const val STREAM_NAME = "AUCTION"
        private const val DURABLE_PREFIX = "seller-metrics"
        private const val BATCH_SIZE = 20
        private val POLL_TIMEOUT: Duration = Duration.ofSeconds(5)

        /** Subjects this consumer listens to. */
        private val SUBJECTS = listOf(
            NatsSubjects.AUCTION_BID_PLACED,
            NatsSubjects.AUCTION_LOT_CLOSED,
            NatsSubjects.AUCTION_LOT_AWARDED,
            NatsSubjects.PAYMENT_SETTLEMENT_READY,
            NatsSubjects.CATALOG_LOT_CREATED
        )
    }

    /**
     * Starts the consumer on application startup.
     */
    fun onStart(@Observes event: StartupEvent) {
        try {
            val natsUrl = ConfigProvider.getConfig()
                .getOptionalValue("nats.url", String::class.java)
                .orElse("nats://localhost:4222")
            connection = io.nats.client.Nats.connect(natsUrl)

            val jetStream = connection!!.jetStream()

            for (subject in SUBJECTS) {
                val durableName = "$DURABLE_PREFIX-${subject.replace(".", "-")}"
                val consumerConfig = ConsumerConfiguration.builder()
                    .durable(durableName)
                    .filterSubject(subject)
                    .maxDeliver(5)
                    .build()

                val pullOptions = PullSubscribeOptions.builder()
                    .stream(STREAM_NAME)
                    .configuration(consumerConfig)
                    .build()

                val subscription = jetStream.subscribe(subject, pullOptions)
                subscriptions.add(subscription)
            }

            running = true
            executor = Executors.newSingleThreadExecutor { r ->
                Thread(r, "seller-auction-consumer").apply { isDaemon = true }
            }
            executor!!.submit { consumeLoop() }

            logger.info("AuctionEventSellerConsumer started with {} subject(s)", SUBJECTS.size)
        } catch (ex: Exception) {
            logger.error("Failed to start AuctionEventSellerConsumer: {}", ex.message, ex)
        }
    }

    /**
     * Gracefully shuts down the consumer.
     */
    fun onStop(@Observes event: ShutdownEvent) {
        running = false
        try {
            subscriptions.forEach { it.drain(Duration.ofSeconds(10)) }
            connection?.close()
            executor?.shutdownNow()
            logger.info("AuctionEventSellerConsumer stopped")
        } catch (ex: Exception) {
            logger.warn("Error during AuctionEventSellerConsumer shutdown: {}", ex.message)
        }
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private fun consumeLoop() {
        while (running) {
            try {
                for (subscription in subscriptions) {
                    val messages = subscription.fetch(BATCH_SIZE, POLL_TIMEOUT)
                    for (msg in messages) {
                        processMessage(msg)
                    }
                }
            } catch (ex: InterruptedException) {
                Thread.currentThread().interrupt()
                running = false
            } catch (ex: Exception) {
                logger.error("Error in AuctionEventSellerConsumer loop: {}", ex.message, ex)
            }
        }
    }

    /**
     * Dispatches a single NATS message to the appropriate handler based on
     * the event type extracted from the payload.
     */
    @Suppress("UNCHECKED_CAST")
    private fun processMessage(message: Message) {
        try {
            val payload = String(message.data, Charsets.UTF_8)
            val eventData = JsonMapper.fromJson<Map<String, Any>>(payload)
            val eventType = eventData["eventType"]?.toString() ?: message.subject

            when {
                eventType.contains("bid.placed") -> handleBidPlaced(eventData)
                eventType.contains("lot.closed") -> handleLotClosed(eventData)
                eventType.contains("lot.awarded") -> handleLotAwarded(eventData)
                eventType.contains("settlement.ready") -> handleSettlementReady(eventData)
                eventType.contains("lot.created") -> handleLotCreated(eventData)
                else -> logger.debug("Unhandled event type: {}", eventType)
            }

            message.ack()
        } catch (ex: Exception) {
            logger.error("Failed to process auction event: {}", ex.message, ex)
            message.nak()
        }
    }

    private fun handleBidPlaced(data: Map<String, Any>) {
        val sellerId = extractSellerId(data) ?: return
        sellerProfileRepository.incrementBids(sellerId)
        logger.debug("Incremented bid count for seller {}", sellerId)
    }

    private fun handleLotClosed(data: Map<String, Any>) {
        val sellerId = extractSellerId(data) ?: return
        val hammerAmount = data["finalBidAmount"]?.toString()?.let { BigDecimal(it) } ?: return
        sellerProfileRepository.addHammerSale(sellerId, hammerAmount)
        logger.debug("Recorded hammer sale of {} for seller {}", hammerAmount, sellerId)
    }

    private fun handleLotAwarded(data: Map<String, Any>) {
        val sellerId = extractSellerId(data) ?: return
        logger.debug("Lot awarded for seller {} -- metrics already updated on close", sellerId)
    }

    private fun handleSettlementReady(data: Map<String, Any>) {
        val sellerId = extractSellerId(data) ?: return
        val amount = data["amount"]?.toString()?.let { BigDecimal(it) } ?: return
        sellerProfileRepository.settlePayment(sellerId, amount)
        logger.debug("Recorded settlement of {} for seller {}", amount, sellerId)
    }

    private fun handleLotCreated(data: Map<String, Any>) {
        val sellerId = extractSellerId(data) ?: return
        sellerProfileRepository.incrementActiveLots(sellerId)
        logger.debug("Incremented active lots for seller {}", sellerId)
    }

    private fun extractSellerId(data: Map<String, Any>): UUID? {
        val sellerIdStr = data["sellerId"]?.toString()
        if (sellerIdStr == null) {
            logger.warn("Event missing sellerId field")
            return null
        }
        return try {
            UUID.fromString(sellerIdStr)
        } catch (ex: IllegalArgumentException) {
            logger.warn("Invalid sellerId format: {}", sellerIdStr)
            null
        }
    }
}
