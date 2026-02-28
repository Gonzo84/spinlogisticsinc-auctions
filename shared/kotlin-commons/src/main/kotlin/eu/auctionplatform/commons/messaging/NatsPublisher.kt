package eu.auctionplatform.commons.messaging

import eu.auctionplatform.commons.domain.DomainEvent
import eu.auctionplatform.commons.event.EventPublisher
import eu.auctionplatform.commons.util.JsonMapper
import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.api.PublishAck
import io.nats.client.impl.Headers
import io.nats.client.impl.NatsMessage
import org.jboss.logging.Logger
import java.time.Duration

/**
 * NATS JetStream publisher that serialises [DomainEvent] instances to JSON and
 * publishes them with configurable retry logic.
 *
 * @property connection  An active NATS [Connection].
 * @property maxRetries  Maximum number of publish attempts before giving up.
 * @property retryDelay  Base delay between retry attempts (no exponential backoff
 *                       in this implementation -- extend if needed).
 */
class NatsPublisher(
    private val connection: Connection,
    private val maxRetries: Int = 3,
    private val retryDelay: Duration = Duration.ofMillis(500)
) : EventPublisher {

    companion object {
        private val LOG: Logger = Logger.getLogger(NatsPublisher::class.java)
    }
    private val jetStream: JetStream = connection.jetStream()

    // ---------------------------------------------------------------------------
    // EventPublisher contract
    // ---------------------------------------------------------------------------

    /**
     * Publishes the [event] synchronously to JetStream on the given [subject].
     *
     * Retries up to [maxRetries] times on transient failures. If all attempts fail,
     * the last exception is rethrown so callers can handle it (e.g. outbox fallback).
     */
    override fun publish(subject: String, event: DomainEvent) {
        val payload = serialise(event)
        val message = buildMessage(subject, payload, event)

        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                val ack: PublishAck = jetStream.publish(message)
                LOG.debugf(
                    "Published event %s to subject %s (stream=%s, seq=%s)",
                    event.eventId, subject, ack.stream, ack.seqno
                )
                return
            } catch (ex: Exception) {
                lastException = ex
                LOG.warnf(
                    "Publish attempt %s/%s failed for event %s on subject %s: %s",
                    attempt, maxRetries, event.eventId, subject, ex.message
                )
                if (attempt < maxRetries) {
                    Thread.sleep(retryDelay.toMillis())
                }
            }
        }
        throw IllegalStateException(
            "Failed to publish event ${event.eventId} to $subject after $maxRetries attempts",
            lastException
        )
    }

    /**
     * Publishes the [event] asynchronously (fire-and-forget with logging on failure).
     *
     * Uses [JetStream.publishAsync] -- the returned future completes when the
     * server acknowledges the message.
     */
    override fun publishAsync(subject: String, event: DomainEvent) {
        val payload = serialise(event)
        val message = buildMessage(subject, payload, event)

        val future = jetStream.publishAsync(message)
        future.whenComplete { ack, throwable ->
            if (throwable != null) {
                LOG.errorf(
                    throwable, "Async publish failed for event %s on subject %s: %s",
                    event.eventId, subject, throwable.message
                )
            } else {
                LOG.debugf(
                    "Async published event %s to subject %s (stream=%s, seq=%s)",
                    event.eventId, subject, ack.stream, ack.seqno
                )
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------

    private fun serialise(event: DomainEvent): ByteArray =
        JsonMapper.toJson(event).toByteArray(Charsets.UTF_8)

    private fun buildMessage(subject: String, payload: ByteArray, event: DomainEvent): NatsMessage {
        val headers = Headers()
        headers.add("event-id", event.eventId)
        headers.add("event-type", event.eventType)
        headers.add("aggregate-id", event.aggregateId)
        headers.add("aggregate-type", event.aggregateType)
        headers.add("brand", event.brand)

        // Propagate trace context if available
        event.metadata?.get("traceId")?.let { headers.add("trace-id", it) }
        event.metadata?.get("userId")?.let { headers.add("user-id", it) }

        return NatsMessage.builder()
            .subject(subject)
            .data(payload)
            .headers(headers)
            .build()
    }
}
