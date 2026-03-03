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
// Catalog Event Seller Consumer -- NATS JetStream subscriber for catalog events
// =============================================================================

/**
 * Consumes catalog domain events from NATS JetStream and populates the
 * seller_lots projection table used by the seller dashboard.
 *
 * Subscribed subjects (on the CATALOG stream):
 * - `catalog.lot.created` -- INSERT into seller_lots.
 * - `catalog.lot.status_changed` -- UPDATE seller_lots status.
 *
 * Uses a durable pull consumer named "seller-catalog-consumer" on the
 * "CATALOG" stream to ensure at-least-once delivery and survive restarts.
 */
@Singleton
class CatalogEventSellerConsumer @Inject constructor(
    connection: Connection,
    private val sellerProfileRepository: SellerProfileRepository
) : NatsConsumer(
    connection = connection,
    streamName = STREAM_NAME,
    durableName = DURABLE_NAME,
    filterSubject = FILTER_SUBJECT,
    maxRedeliveries = 5,
    deadLetterSubject = "seller.dlq.catalog",
    batchSize = 20
) {

    private var consumerThread: Thread? = null

    companion object {
        private val LOG: Logger = Logger.getLogger(CatalogEventSellerConsumer::class.java)

        /** NATS JetStream stream for catalog events. */
        const val STREAM_NAME: String = "CATALOG"

        /** Durable consumer name -- persists across restarts. */
        const val DURABLE_NAME: String = "seller-catalog-consumer"

        /** Subject filter matching all catalog lot events (including brand suffix). */
        const val FILTER_SUBJECT: String = "catalog.lot.>"

        // Subject prefixes for routing
        private const val SUBJECT_LOT_CREATED = "catalog.lot.created"
        private const val SUBJECT_LOT_STATUS_CHANGED = "catalog.lot.status"
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts the consumer loop in a dedicated daemon thread on application startup.
     */
    fun onStart(@Observes event: StartupEvent) {
        LOG.infof("Starting CatalogEventSellerConsumer [durable=%s]", DURABLE_NAME)
        consumerThread = Thread({
            start()
        }, "seller-catalog-consumer").apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Signals the consumer loop to stop gracefully on application shutdown.
     */
    fun onStop(@Observes event: ShutdownEvent) {
        LOG.infof("Stopping CatalogEventSellerConsumer [durable=%s]", DURABLE_NAME)
        stop()
        consumerThread?.interrupt()
    }

    // -------------------------------------------------------------------------
    // Message handling
    // -------------------------------------------------------------------------

    /**
     * Routes an incoming NATS message to the appropriate handler based on the
     * subject pattern.
     *
     * Subject convention: `catalog.lot.<action>.<brand>`
     * - `catalog.lot.created.<brand>` -- new lot
     * - `catalog.lot.status_changed.<brand>` -- status update
     */
    override fun handleMessage(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        LOG.debugf("Received catalog event on subject [%s], payload size=%s bytes",
            subject, message.data.size)

        try {
            when {
                subject.startsWith(SUBJECT_LOT_STATUS_CHANGED) -> handleLotStatusChanged(payload)
                subject.startsWith(SUBJECT_LOT_CREATED) -> handleLotCreated(payload)
                else -> LOG.debugf("Ignoring unrelated subject [%s] on catalog stream", subject)
            }
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to process catalog event on [%s]: %s", subject, ex.message)
            throw ex // re-throw so the base class handles nak/dead-letter
        }
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    /**
     * Handles `catalog.lot.created` events by inserting a new row into the
     * seller_lots projection table and incrementing the seller's active lots
     * counter.
     *
     * Expected payload fields (from [LotCreatedEvent]):
     * - `lotId` -- unique lot identifier (used as seller_lots.id)
     * - `sellerId` -- the seller who created the lot
     * - `title` -- lot title
     * - `status` -- initial lot status (e.g. "DRAFT", "PENDING_REVIEW")
     * - `reservePrice` -- seller's reserve price
     * - `startingBid` -- starting bid amount
     */
    private fun handleLotCreated(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotIdStr = node.requiredText("lotId")
        val sellerIdStr = node.requiredText("sellerId")
        val title = node.optionalText("title") ?: ""
        val status = node.optionalText("status") ?: "DRAFT"
        val reservePrice = node.optionalDecimal("reservePrice")

        val lotId = UUID.fromString(lotIdStr)
        val sellerId = UUID.fromString(sellerIdStr)

        LOG.infof("Creating seller lot projection [lotId=%s, sellerId=%s]", lotId, sellerId)

        // Look up the seller profile by userId to find the seller_profiles.id
        // The sellerId in catalog events is the user_id, but seller_lots.seller_id
        // references seller_profiles.id. We need to resolve this.
        val sellerProfileId = sellerProfileRepository.findSellerProfileIdByUserId(sellerId)
        if (sellerProfileId == null) {
            LOG.warnf("No seller profile found for userId %s -- cannot create lot projection", sellerId)
            return
        }

        sellerProfileRepository.insertSellerLot(
            lotId = lotId,
            sellerId = sellerProfileId,
            title = title,
            status = status,
            reservePrice = reservePrice
        )

        sellerProfileRepository.incrementActiveLots(sellerProfileId)

        LOG.infof("Successfully created seller lot projection [lotId=%s, sellerProfile=%s]",
            lotId, sellerProfileId)
    }

    /**
     * Handles `catalog.lot.status.changed` events by updating the status
     * in the seller_lots projection table.
     *
     * Expected payload fields (from catalog-service publishLotEvent):
     * - `lotId` -- lot whose status changed
     * - `status` -- new lifecycle status
     */
    private fun handleLotStatusChanged(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val lotIdStr = node.requiredText("lotId")
        // catalog-service publishes the field as "status", not "newStatus"
        val newStatus = node.optionalText("status")
            ?: node.optionalText("newStatus")
            ?: throw IllegalArgumentException("Required field 'status' missing from catalog event payload")

        val lotId = UUID.fromString(lotIdStr)

        LOG.infof("Updating lot status [lotId=%s, newStatus=%s]", lotId, newStatus)

        sellerProfileRepository.updateLotStatus(lotId, newStatus)

        LOG.infof("Successfully updated lot status [lotId=%s, newStatus=%s]", lotId, newStatus)
    }

    // -------------------------------------------------------------------------
    // JSON extraction helpers
    // -------------------------------------------------------------------------

    private fun JsonNode.requiredText(field: String): String {
        return this.get(field)?.asText()
            ?: throw IllegalArgumentException("Required field '$field' missing from catalog event payload")
    }

    private fun JsonNode.optionalText(field: String): String? =
        this.get(field)?.takeIf { !it.isNull }?.asText()

    private fun JsonNode.optionalDecimal(field: String): BigDecimal? =
        this.get(field)?.takeIf { !it.isNull }?.decimalValue()
}
