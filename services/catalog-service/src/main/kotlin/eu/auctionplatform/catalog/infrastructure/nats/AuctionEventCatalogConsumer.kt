package eu.auctionplatform.catalog.infrastructure.nats

import eu.auctionplatform.commons.exception.ConflictException
import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.catalog.application.service.LotService
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

/**
 * NATS JetStream consumer that listens for auction and payment domain events
 * and updates lot lifecycle status in the catalog.
 *
 * Subscribed subjects:
 * - `auction.lot.awarded` -- Transitions lot from ACTIVE to SOLD.
 * - `auction.lot.closed` -- Assigns lot to auction (APPROVED to ACTIVE) if applicable.
 * - `payment.lot.relist-requested` -- Transitions lot from ACTIVE/SOLD back to APPROVED.
 *
 * Uses durable consumers to survive restarts and ensure at-least-once delivery.
 */
@ApplicationScoped
@Startup
class AuctionEventCatalogConsumer @Inject constructor(
    private val connection: Connection,
    private val lotService: LotService
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(AuctionEventCatalogConsumer::class.java)

        private const val AUCTION_STREAM = "AUCTION"
        private const val PAYMENT_STREAM = "PAYMENT"
        private const val DURABLE_NAME = "catalog-auction-consumer"
    }

    private val executor: ExecutorService = Executors.newFixedThreadPool(3)

    /**
     * Starts three consumer threads for auction and payment event subjects.
     * Called automatically at application startup via the [Startup] annotation.
     */
    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.info("Starting auction/payment event catalog consumers")

        executor.submit { createLotAwardedConsumer().start() }
        executor.submit { createLotClosedConsumer().start() }
        executor.submit { createRelistConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.info("Shutting down auction/payment event catalog consumers")
        executor.shutdownNow()
    }

    // -----------------------------------------------------------------------
    // Consumer factories
    // -----------------------------------------------------------------------

    private fun createLotAwardedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = AUCTION_STREAM,
            durableName = "$DURABLE_NAME-lot-awarded",
            filterSubject = NatsSubjects.AUCTION_LOT_AWARDED,
            deadLetterSubject = "dlq.catalog.auction.lot.awarded"
        ) {
            override fun handleMessage(message: Message) {
                handleLotAwarded(message)
            }
        }

    private fun createLotClosedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = AUCTION_STREAM,
            durableName = "$DURABLE_NAME-lot-closed",
            filterSubject = NatsSubjects.AUCTION_LOT_CLOSED,
            deadLetterSubject = "dlq.catalog.auction.lot.closed"
        ) {
            override fun handleMessage(message: Message) {
                handleLotClosed(message)
            }
        }

    private fun createRelistConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = PAYMENT_STREAM,
            durableName = "$DURABLE_NAME-relist",
            filterSubject = NatsSubjects.PAYMENT_LOT_RELIST,
            deadLetterSubject = "dlq.catalog.payment.lot.relist"
        ) {
            override fun handleMessage(message: Message) {
                handleRelistRequested(message)
            }
        }

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    /**
     * Handles `auction.lot.awarded` events.
     *
     * Transitions the lot from ACTIVE to SOLD via [LotService.markAsSold].
     * ConflictException is caught and logged as an idempotent skip (not retried).
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleLotAwarded(message: Message) {
        val payload = parsePayload(message) ?: return

        val lotId = payload["lotId"]?.toString() ?: run {
            LOG.warn("auction.lot.awarded event missing lotId -- skipping")
            return
        }
        val winnerId = payload["winnerId"]?.toString()
        val hammerPrice = payload["hammerPrice"]?.toString()?.let { BigDecimal(it) }

        LOG.infof("Processing auction.lot.awarded: lotId=%s, winnerId=%s", lotId, winnerId)

        try {
            val lotUuid = UUID.fromString(lotId)
            val winnerUuid = winnerId?.let { UUID.fromString(it) }
            lotService.markAsSold(lotUuid, winnerUuid, hammerPrice)
            LOG.infof("Lot %s transitioned to SOLD", lotId)
        } catch (ex: ConflictException) {
            LOG.infof("Idempotent skip for lot %s: %s", lotId, ex.message)
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to mark lot %s as sold: %s", lotId, ex.message)
            throw ex
        }
    }

    /**
     * Handles `auction.lot.closed` events.
     *
     * If the lot has a lotId and auctionId, assigns the lot to the auction
     * (APPROVED to ACTIVE). This handles the case where an auction is created
     * and the lot needs to be activated.
     *
     * ConflictException is caught and logged as an idempotent skip.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleLotClosed(message: Message) {
        val payload = parsePayload(message) ?: return

        val lotId = payload["lotId"]?.toString()
        val auctionId = payload["aggregateId"]?.toString() ?: payload["auctionId"]?.toString()

        if (lotId == null || auctionId == null) {
            LOG.debugf("auction.lot.closed missing lotId or auctionId -- skipping assignment")
            return
        }

        LOG.infof("Processing auction.lot.closed: lotId=%s, auctionId=%s", lotId, auctionId)

        try {
            val lotUuid = UUID.fromString(lotId)
            val auctionUuid = UUID.fromString(auctionId)
            lotService.assignToAuction(lotUuid, auctionUuid)
            LOG.infof("Lot %s assigned to auction %s (now ACTIVE)", lotId, auctionId)
        } catch (ex: ConflictException) {
            LOG.infof("Idempotent skip for lot %s assignment: %s", lotId, ex.message)
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to assign lot %s to auction %s: %s", lotId, auctionId, ex.message)
            throw ex
        }
    }

    /**
     * Handles `payment.lot.relist-requested` events.
     *
     * Transitions the lot from ACTIVE/SOLD back to APPROVED via [LotService.relistLot],
     * detaching it from the previous auction so it can be re-auctioned.
     *
     * ConflictException is caught and logged as an idempotent skip.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleRelistRequested(message: Message) {
        val payload = parsePayload(message) ?: return

        val lotId = payload["lotId"]?.toString() ?: run {
            LOG.warn("payment.lot.relist-requested event missing lotId -- skipping")
            return
        }
        val reason = payload["reason"]?.toString() ?: "NON_PAYMENT"

        LOG.infof("Processing payment.lot.relist-requested: lotId=%s, reason=%s", lotId, reason)

        try {
            val lotUuid = UUID.fromString(lotId)
            lotService.relistLot(lotUuid, reason)
            LOG.infof("Lot %s relisted (back to APPROVED)", lotId)
        } catch (ex: ConflictException) {
            LOG.infof("Idempotent skip for lot %s relist: %s", lotId, ex.message)
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to relist lot %s: %s", lotId, ex.message)
            throw ex
        }
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
                ex, "Failed to parse event payload on subject %s: %s",
                message.subject, ex.message
            )
            null
        }
    }
}
