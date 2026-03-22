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
 * Forwards payment events from NATS JetStream to WebSocket clients.
 *
 * - Settlement events → sent to the seller via [WebSocketHub.sendToUser]
 * - Checkout/payment events → broadcast to admin_ops via [WebSocketHub.broadcastToRole]
 */
@Singleton
class PaymentEventForwarder @Inject constructor(
    natsConnection: Connection,
    private val webSocketHub: WebSocketHub
) : NatsConsumer(
    connection = natsConnection,
    streamName = "PAYMENT",
    durableName = DURABLE_NAME,
    filterSubject = FILTER_SUBJECT,
    maxRedeliveries = 3,
    deadLetterSubject = "gateway.dlq.payment",
    batchSize = 50
) {

    private var consumerThread: Thread? = null

    companion object {
        private val LOG: Logger = Logger.getLogger(PaymentEventForwarder::class.java)
        const val DURABLE_NAME: String = "gateway-payment-consumer"
        const val FILTER_SUBJECT: String = "payment.>"
    }

    fun onStart(@Observes event: StartupEvent) {
        LOG.infof("Starting PaymentEventForwarder [durable=%s]", DURABLE_NAME)
        consumerThread = Thread({
            start()
        }, "payment-event-forwarder").apply {
            isDaemon = true
            start()
        }
    }

    fun onStop(@Observes event: ShutdownEvent) {
        LOG.infof("Stopping PaymentEventForwarder [durable=%s]", DURABLE_NAME)
        stop()
        consumerThread?.interrupt()
    }

    override fun handleMessage(message: Message) {
        val subject = message.subject
        val payload = String(message.data, Charsets.UTF_8)

        LOG.debugf("Received payment event [%s], size=%s bytes", subject, message.data.size)

        try {
            when {
                subject.startsWith(NatsSubjects.PAYMENT_SETTLEMENT_SETTLED) ->
                    forwardSettlementSettled(payload)

                subject.startsWith(NatsSubjects.PAYMENT_SETTLEMENT_READY) ->
                    forwardSettlementReady(payload)

                subject.startsWith(NatsSubjects.PAYMENT_CHECKOUT_COMPLETED) ->
                    forwardCheckoutCompleted(payload)

                else ->
                    LOG.debugf("Ignoring non-forwardable payment subject [%s]", subject)
            }
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to forward payment event [%s]: %s", subject, ex.message)
            throw ex
        }
    }

    private fun forwardSettlementSettled(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val sellerId = node.path("sellerId").asText(null)
            ?: node.path("userId").asText(null) ?: return

        val wsMessage = JsonMapper.toJson(mapOf(
            "type" to "settlement_updated",
            "data" to mapOf(
                "settlementId" to node.path("settlementId").asText(null),
                "status" to "SETTLED",
                "amount" to node.path("amount").asText(null),
                "currency" to node.path("currency").asText("USD"),
                "settledAt" to node.path("settledAt").asText(null)
            ),
            "serverTime" to Instant.now().toString()
        ))

        webSocketHub.sendToUser(sellerId, wsMessage)
        LOG.debugf("Forwarded settlement_updated to seller [%s]", sellerId)
    }

    private fun forwardSettlementReady(payload: String) {
        val node = JsonMapper.instance.readTree(payload)
        val sellerId = node.path("sellerId").asText(null)
            ?: node.path("userId").asText(null) ?: return

        val wsMessage = JsonMapper.toJson(mapOf(
            "type" to "settlement_ready",
            "data" to mapOf(
                "settlementId" to node.path("settlementId").asText(null),
                "amount" to node.path("amount").asText(null),
                "currency" to node.path("currency").asText("USD"),
                "lotId" to node.path("lotId").asText(null)
            ),
            "serverTime" to Instant.now().toString()
        ))

        webSocketHub.sendToUser(sellerId, wsMessage)
        LOG.debugf("Forwarded settlement_ready to seller [%s]", sellerId)
    }

    private fun forwardCheckoutCompleted(payload: String) {
        val node = JsonMapper.instance.readTree(payload)

        val wsMessage = JsonMapper.toJson(mapOf(
            "type" to "payment_updated",
            "data" to mapOf(
                "paymentId" to node.path("paymentId").asText(null),
                "buyerId" to node.path("buyerId").asText(null),
                "amount" to node.path("totalAmount").asText(null),
                "currency" to node.path("currency").asText("USD"),
                "status" to "COMPLETED"
            ),
            "serverTime" to Instant.now().toString()
        ))

        webSocketHub.broadcastToRole("admin_ops", wsMessage)
        LOG.debugf("Broadcast payment_updated to admin_ops")
    }
}
