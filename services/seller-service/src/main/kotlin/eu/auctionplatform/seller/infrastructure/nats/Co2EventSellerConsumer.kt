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
// CO2 Event Seller Consumer -- NATS JetStream subscriber for CO2 events
// =============================================================================

/**
 * Consumes CO2 calculation events from NATS JetStream and populates the
 * seller_co2 table with per-lot CO2 savings data.
 *
 * Subscribed subjects (on the CO2 stream):
 * - `co2.calculated` -- a CO2 calculation has been completed for a lot.
 *
 * The sellerId is resolved from the seller_lots projection table using the
 * lotId from the event payload.
 *
 * Uses a durable pull consumer named "seller-co2-consumer" on the
 * "CO2" stream to ensure at-least-once delivery and survive restarts.
 */
@Singleton
class Co2EventSellerConsumer @Inject constructor(
    connection: Connection,
    private val sellerProfileRepository: SellerProfileRepository
) : NatsConsumer(
    connection = connection,
    streamName = STREAM_NAME,
    durableName = DURABLE_NAME,
    filterSubject = FILTER_SUBJECT,
    maxRedeliveries = 5,
    deadLetterSubject = "seller.dlq.co2",
    batchSize = 20
) {

    private var consumerThread: Thread? = null

    companion object {
        private val LOG: Logger = Logger.getLogger(Co2EventSellerConsumer::class.java)

        /** NATS JetStream stream for CO2 events. */
        const val STREAM_NAME: String = "CO2"

        /** Durable consumer name -- persists across restarts. */
        const val DURABLE_NAME: String = "seller-co2-consumer"

        /** Subject filter matching CO2 calculation events. */
        const val FILTER_SUBJECT: String = "co2.>"
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts the consumer loop in a dedicated daemon thread on application startup.
     */
    fun onStart(@Observes event: StartupEvent) {
        LOG.infof("Starting Co2EventSellerConsumer [durable=%s]", DURABLE_NAME)
        consumerThread = Thread({
            start()
        }, "seller-co2-consumer").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Signals the consumer loop to stop gracefully on application shutdown.
     */
    fun onStop(@Observes event: ShutdownEvent) {
        LOG.infof("Stopping Co2EventSellerConsumer [durable=%s]", DURABLE_NAME)
        stop()
        consumerThread?.interrupt()
    }

    // -------------------------------------------------------------------------
    // Message handling
    // -------------------------------------------------------------------------

    /**
     * Routes an incoming NATS message to the appropriate handler based on the
     * subject pattern.
     */
    override fun handleMessage(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        LOG.debugf("Received CO2 event on subject [%s], payload size=%s bytes",
            subject, message.data.size)

        try {
            when {
                subject.startsWith("co2.calculated") -> handleCo2Calculated(payload)
                else -> LOG.debugf("Ignoring unrelated subject [%s] on CO2 stream", subject)
            }
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to process CO2 event on [%s]: %s", subject, ex.message)
            throw ex // re-throw so the base class handles nak/dead-letter
        }
    }

    // -------------------------------------------------------------------------
    // Event handler
    // -------------------------------------------------------------------------

    /**
     * Handles `co2.calculated` events by inserting a CO2 savings record
     * into the seller_co2 table.
     *
     * Expected payload fields:
     * - `lotId` -- the lot for which CO2 was calculated
     * - `co2SavedKg` -- the CO2 savings in kilograms
     *
     * The sellerId is resolved by looking up `seller_lots` by lotId.
     */
    private fun handleCo2Calculated(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotId = extractLotId(node) ?: return
        val co2SavedKg = node.optionalDecimal("co2SavedKg")
            ?: node.optionalDecimal("co2_saved_kg")

        if (co2SavedKg == null || co2SavedKg <= BigDecimal.ZERO) {
            LOG.debugf("CO2 event for lot %s has no valid co2SavedKg -- skipping", lotId)
            return
        }

        val sellerId = sellerProfileRepository.findSellerIdByLotId(lotId)
        if (sellerId == null) {
            LOG.debugf("No seller found for lot %s -- skipping co2.calculated", lotId)
            return
        }

        sellerProfileRepository.insertCo2Record(sellerId, lotId, co2SavedKg)

        LOG.infof("Inserted CO2 record for seller %s (lot=%s, co2=%s kg)",
            sellerId, lotId, co2SavedKg)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the lot ID from the event payload.
     * Supports both `lotId` and `aggregateId` field names.
     */
    private fun extractLotId(node: JsonNode): UUID? {
        val lotIdStr = node.get("lotId")?.asText()
            ?: node.get("aggregateId")?.asText()
        if (lotIdStr == null) {
            LOG.warn("CO2 event payload missing both 'lotId' and 'aggregateId' fields")
            return null
        }
        return try {
            UUID.fromString(lotIdStr)
        } catch (ex: IllegalArgumentException) {
            LOG.warnf("Invalid lotId format in CO2 event: %s", lotIdStr)
            null
        }
    }

    private fun JsonNode.optionalDecimal(field: String): BigDecimal? =
        this.get(field)?.takeIf { !it.isNull }?.decimalValue()
}
