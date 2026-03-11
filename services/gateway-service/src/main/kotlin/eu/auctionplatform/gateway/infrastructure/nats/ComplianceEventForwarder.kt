package eu.auctionplatform.gateway.infrastructure.nats

import eu.auctionplatform.commons.messaging.NatsConsumer
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
 * Forwards compliance events from NATS JetStream to WebSocket clients.
 *
 * Currently handles fraud alert detection events, broadcasting them to
 * admin users via [WebSocketHub.broadcastToRole].
 */
@Singleton
class ComplianceEventForwarder @Inject constructor(
    natsConnection: Connection,
    private val webSocketHub: WebSocketHub
) : NatsConsumer(
    connection = natsConnection,
    streamName = "COMPLIANCE",
    durableName = DURABLE_NAME,
    filterSubject = FILTER_SUBJECT,
    maxRedeliveries = 3,
    deadLetterSubject = "gateway.dlq.compliance",
    batchSize = 50
) {

    private var consumerThread: Thread? = null

    companion object {
        private val LOG: Logger = Logger.getLogger(ComplianceEventForwarder::class.java)
        const val DURABLE_NAME: String = "gateway-compliance-consumer"
        const val FILTER_SUBJECT: String = "compliance.>"
    }

    fun onStart(@Observes event: StartupEvent) {
        LOG.infof("Starting ComplianceEventForwarder [durable=%s]", DURABLE_NAME)
        consumerThread = Thread({
            start()
        }, "compliance-event-forwarder").apply {
            isDaemon = true
            start()
        }
    }

    fun onStop(@Observes event: ShutdownEvent) {
        LOG.infof("Stopping ComplianceEventForwarder [durable=%s]", DURABLE_NAME)
        stop()
        consumerThread?.interrupt()
    }

    override fun handleMessage(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        LOG.debugf("Received compliance event [%s], size=%s bytes", subject, message.data.size)

        try {
            // Forward all compliance events as fraud alerts to admins
            forwardFraudAlert(payload)
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to forward compliance event [%s]: %s", subject, ex.message)
            throw ex
        }
    }

    private fun forwardFraudAlert(payload: String) {
        val node = JsonMapper.instance.readTree(payload)

        val wsMessage = JsonMapper.toJson(mapOf(
            "type" to "fraud_alert",
            "data" to mapOf(
                "alertId" to node.path("alertId").asText(null),
                "type" to node.path("type").asText(null),
                "severity" to node.path("severity").asText(null),
                "title" to node.path("title").asText(null),
                "userId" to node.path("userId").asText(null),
                "riskScore" to node.path("riskScore").asDouble(0.0)
            ),
            "serverTime" to Instant.now().toString()
        ))

        webSocketHub.broadcastToRole("admin_ops", wsMessage)
        LOG.debugf("Broadcast fraud_alert to admin_ops")
    }
}
