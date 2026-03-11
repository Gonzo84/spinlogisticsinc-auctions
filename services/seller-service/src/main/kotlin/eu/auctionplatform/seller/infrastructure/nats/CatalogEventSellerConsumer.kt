package eu.auctionplatform.seller.infrastructure.nats

import com.fasterxml.jackson.databind.JsonNode
import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.seller.infrastructure.persistence.repository.SellerProfileRepository
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
@ApplicationScoped
@Startup
class CatalogEventSellerConsumer @Inject constructor(
    private val connection: Connection,
    private val sellerProfileRepository: SellerProfileRepository
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(CatalogEventSellerConsumer::class.java)

        /** NATS JetStream stream for catalog events. */
        private const val STREAM_NAME: String = "CATALOG"

        /** Durable consumer name -- persists across restarts. */
        private const val DURABLE_NAME: String = "seller-catalog-consumer"

        /** Subject filter matching all catalog lot events (including brand suffix). */
        private const val FILTER_SUBJECT: String = "catalog.lot.>"

        // Subject prefixes for routing
        private const val SUBJECT_LOT_CREATED = "catalog.lot.created"
        private const val SUBJECT_LOT_STATUS_CHANGED = "catalog.lot.status"
    }

    private val executor: ExecutorService = Executors.newFixedThreadPool(2)

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Starts consumer threads for catalog event subjects.
     * Called automatically at application startup via the [Startup] annotation.
     */
    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.infof("Starting CatalogEventSellerConsumer [durable=%s]", DURABLE_NAME)

        executor.submit { createLotCreatedConsumer().start() }
        executor.submit { createLotStatusChangedConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.infof("Stopping CatalogEventSellerConsumer [durable=%s]", DURABLE_NAME)
        executor.shutdownNow()
    }

    // -------------------------------------------------------------------------
    // Consumer factories
    // -------------------------------------------------------------------------

    private fun createLotCreatedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-lot-created",
            filterSubject = SUBJECT_LOT_CREATED,
            maxRedeliveries = 5,
            deadLetterSubject = "seller.dlq.catalog.lot.created",
            batchSize = 20
        ) {
            override fun handleMessage(message: Message) {
                handleLotCreated(message)
            }
        }

    private fun createLotStatusChangedConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = "$DURABLE_NAME-lot-status",
            filterSubject = SUBJECT_LOT_STATUS_CHANGED,
            maxRedeliveries = 5,
            deadLetterSubject = "seller.dlq.catalog.lot.status",
            batchSize = 20
        ) {
            override fun handleMessage(message: Message) {
                handleLotStatusChanged(message)
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
    private fun handleLotCreated(message: Message) {
        val payload = String(message.data, Charsets.UTF_8)
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
    private fun handleLotStatusChanged(message: Message) {
        val payload = String(message.data, Charsets.UTF_8)
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
