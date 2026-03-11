package eu.auctionplatform.gateway.infrastructure.nats

import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.gateway.infrastructure.websocket.WebSocketHub
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.jboss.logging.Logger
import java.time.Instant

/**
 * Forwards catalog events from NATS JetStream to WebSocket clients.
 *
 * - Lot status changes → sent to the seller via [WebSocketHub.sendToUser]
 * - Lots pending review → broadcast to admin_ops via [WebSocketHub.broadcastToRole]
 */
@Singleton
class CatalogEventForwarder @Inject constructor(
    natsConnection: Connection,
    private val webSocketHub: WebSocketHub
) : NatsConsumer(
    connection = natsConnection,
    streamName = "CATALOG",
    durableName = DURABLE_NAME,
    filterSubject = FILTER_SUBJECT,
    maxRedeliveries = 3,
    deadLetterSubject = "gateway.dlq.catalog",
    batchSize = 50
) {

    private var consumerThread: Thread? = null

    companion object {
        private val LOG: Logger = Logger.getLogger(CatalogEventForwarder::class.java)
        const val DURABLE_NAME: String = "gateway-catalog-consumer"
        const val FILTER_SUBJECT: String = "catalog.>"
    }

    fun onStart(@Observes event: StartupEvent) {
        LOG.infof("Starting CatalogEventForwarder [durable=%s]", DURABLE_NAME)
        consumerThread = Thread({
            start()
        }, "catalog-event-forwarder").apply {
            isDaemon = true
            start()
        }
    }

    fun onStop(@Observes event: ShutdownEvent) {
        LOG.infof("Stopping CatalogEventForwarder [durable=%s]", DURABLE_NAME)
        stop()
        consumerThread?.interrupt()
    }

    override fun handleMessage(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        LOG.debugf("Received catalog event [%s], size=%s bytes", subject, message.data.size)

        try {
            when {
                subject.startsWith(NatsSubjects.CATALOG_LOT_STATUS_CHANGED) ->
                    forwardLotStatusChanged(payload)

                subject.startsWith(NatsSubjects.CATALOG_LOT_CREATED) ->
                    forwardLotCreated(payload)

                else ->
                    LOG.debugf("Ignoring non-forwardable catalog subject [%s]", subject)
            }
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to forward catalog event [%s]: %s", subject, ex.message)
            throw ex
        }
    }

    private fun forwardLotStatusChanged(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val sellerId = node.path("sellerId").asText(null)
            ?: node.path("userId").asText(null)

        val wsMessage = JsonMapper.toJson(mapOf(
            "type" to "lot_status_changed",
            "data" to mapOf(
                "lotId" to node.path("lotId").asText(null),
                "previousStatus" to node.path("previousStatus").asText(null),
                "newStatus" to node.path("newStatus").asText(null),
                "title" to node.path("title").asText(null)
            ),
            "serverTime" to Instant.now().toString()
        ))

        // Notify seller if we know who they are
        if (!sellerId.isNullOrBlank()) {
            webSocketHub.sendToUser(sellerId, wsMessage)
            LOG.debugf("Forwarded lot_status_changed to seller [%s]", sellerId)
        }

        // Also broadcast to admins
        webSocketHub.broadcastToRole("admin_ops", wsMessage)
    }

    private fun forwardLotCreated(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val status = node.path("status").asText(null)

        // Only notify admins about lots pending review
        if (status == "PENDING_REVIEW") {
            val wsMessage = JsonMapper.toJson(mapOf(
                "type" to "lot_pending_approval",
                "data" to mapOf(
                    "lotId" to node.path("lotId").asText(null),
                    "title" to node.path("title").asText(null),
                    "sellerId" to node.path("sellerId").asText(null),
                    "category" to node.path("category").asText(null)
                ),
                "serverTime" to Instant.now().toString()
            ))

            webSocketHub.broadcastToRole("admin_ops", wsMessage)
            LOG.debugf("Broadcast lot_pending_approval to admin_ops")
        }
    }
}
