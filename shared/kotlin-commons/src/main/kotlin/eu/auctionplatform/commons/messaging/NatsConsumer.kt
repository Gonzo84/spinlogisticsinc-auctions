package eu.auctionplatform.commons.messaging

import io.micrometer.core.instrument.MeterRegistry
import io.nats.client.Connection
import io.nats.client.JetStreamSubscription
import io.nats.client.Message
import io.nats.client.PullSubscribeOptions
import io.nats.client.api.ConsumerConfiguration
import io.nats.client.impl.Headers
import io.nats.client.impl.NatsMessage
import org.jboss.logging.Logger
import org.slf4j.MDC
import java.time.Duration

/**
 * Abstract base class for durable NATS JetStream pull consumers.
 *
 * Subclasses specify the [streamName], [durableName], and [filterSubject], then
 * implement [handleMessage] to process each delivered message.
 *
 * Messages that fail processing are negative-acknowledged so that the server can
 * redeliver them. After [maxRedeliveries] the message is forwarded to a dead
 * letter subject if configured.
 *
 * @property connection        An active NATS [Connection].
 * @property streamName        The JetStream stream to consume from.
 * @property durableName       Durable consumer name (survives restarts).
 * @property filterSubject     Subject filter applied to the consumer.
 * @property maxRedeliveries   Maximum redelivery attempts before dead-lettering.
 * @property deadLetterSubject Subject to publish to when max redeliveries is exceeded.
 *                             Null disables dead-letter routing.
 * @property batchSize         Number of messages to pull per iteration.
 * @property pollTimeout       How long to wait for messages in each pull.
 * @property meterRegistry     Optional Micrometer registry for publishing custom metrics.
 *                             When null, no metrics are recorded. Injected by Quarkus CDI at runtime.
 */
abstract class NatsConsumer(
    protected val connection: Connection,
    protected val streamName: String,
    protected val durableName: String,
    protected val filterSubject: String,
    protected val maxRedeliveries: Int = 5,
    protected val deadLetterSubject: String? = null,
    protected val batchSize: Int = 10,
    protected val pollTimeout: Duration = Duration.ofSeconds(5),
    protected val meterRegistry: MeterRegistry? = null
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(NatsConsumer::class.java)
    }

    @Volatile
    private var running: Boolean = false

    private var subscription: JetStreamSubscription? = null

    /**
     * Processes a single message.
     *
     * Implementations must **not** acknowledge the message themselves -- the base
     * class takes care of ack / nak based on the outcome of this method.
     *
     * @throws Exception if processing fails (triggers redelivery or dead-letter).
     */
    protected abstract fun handleMessage(message: Message)

    /**
     * Starts the consumer loop on the current thread (blocking).
     *
     * Call [stop] from another thread to break the loop gracefully.
     */
    fun start() {
        LOG.infof(
            "Starting NATS consumer [durable=%s, filter=%s, stream=%s]",
            durableName, filterSubject, streamName
        )

        // Wait for the stream to be available (may be created by NatsStreamInitializer)
        waitForStream()

        val jetStream = connection.jetStream()

        val consumerConfig = ConsumerConfiguration.builder()
            .durable(durableName)
            .filterSubject(filterSubject)
            .maxDeliver(maxRedeliveries.toLong())
            .backoff(
                Duration.ofSeconds(5),
                Duration.ofSeconds(15),
                Duration.ofSeconds(60),
                Duration.ofSeconds(300),
                Duration.ofSeconds(900)
            )
            .build()

        val pullOptions = PullSubscribeOptions.builder()
            .stream(streamName)
            .configuration(consumerConfig)
            .build()

        subscription = jetStream.subscribe(filterSubject, pullOptions)
        running = true

        while (running) {
            try {
                val messages = subscription!!.fetch(batchSize, pollTimeout)
                for (msg in messages) {
                    processMessage(msg)
                }
            } catch (ex: InterruptedException) {
                Thread.currentThread().interrupt()
                LOG.infof("Consumer [%s] interrupted -- stopping", durableName)
                running = false
            } catch (ex: Exception) {
                LOG.errorf(ex, "Unexpected error in consumer [%s]: %s", durableName, ex.message)
            }
        }

        LOG.infof("NATS consumer [%s] stopped", durableName)
    }

    /**
     * Signals the consumer loop to stop after the current batch completes.
     */
    fun stop() {
        running = false
        subscription?.drain(Duration.ofSeconds(10))
        LOG.infof("Stop requested for NATS consumer [%s]", durableName)
    }

    // ---------------------------------------------------------------------------
    // Trace context extraction
    // ---------------------------------------------------------------------------

    /**
     * Extracts trace context (traceId, userId) from NATS message headers.
     *
     * Consumers can call this in [handleMessage] to restore distributed trace
     * context for logging, MDC, or OpenTelemetry propagation.
     */
    protected fun extractTraceContext(message: Message): Map<String, String> {
        val headers = message.headers ?: return emptyMap()
        return buildMap {
            headers.getFirst("trace-id")?.let { put("traceId", it) }
            headers.getFirst("user-id")?.let { put("userId", it) }
        }
    }

    // ---------------------------------------------------------------------------
    // Stream readiness check
    // ---------------------------------------------------------------------------

    /**
     * Waits for the JetStream stream to become available using exponential backoff.
     *
     * Starts with a 2-second delay and doubles on each retry, capped at 30 seconds.
     * Throws [IllegalStateException] if the stream is still unavailable after
     * [maxAttempts], which prevents the consumer from silently running without
     * a valid subscription.
     */
    protected fun waitForStream(streamName: String = this.streamName, maxAttempts: Int = 10) {
        var delay = 2000L // Start at 2 seconds
        val maxDelay = 30000L
        for (attempt in 1..maxAttempts) {
            try {
                connection.jetStreamManagement().getStreamInfo(streamName)
                LOG.infof("Stream '%s' is available (attempt %s)", streamName, attempt)
                return
            } catch (e: Exception) {
                LOG.warnf(
                    "Stream '%s' not available (attempt %s/%s), retrying in %sms",
                    streamName, attempt, maxAttempts, delay
                )
                Thread.sleep(delay)
                delay = (delay * 2).coerceAtMost(maxDelay)
            }
        }
        throw IllegalStateException("Stream '$streamName' not available after $maxAttempts attempts")
    }

    // ---------------------------------------------------------------------------
    // Internal processing
    // ---------------------------------------------------------------------------

    private fun processMessage(message: Message) {
        val metadata = message.metaData()
        val deliveryCount = metadata?.deliveredCount() ?: 0
        val eventType = message.subject
        val aggregateId = message.headers?.getFirst("aggregate-id") ?: "unknown"

        // Set MDC context for structured JSON logging
        MDC.put("eventType", eventType)
        MDC.put("aggregateId", aggregateId)
        MDC.put("consumer", durableName)
        MDC.put("deliveryCount", deliveryCount.toString())

        try {
            handleMessage(message)
            message.ack()
            meterRegistry?.counter(
                "nats.consumer.processed",
                "consumer", durableName,
                "stream", streamName
            )?.increment()

            if (deliveryCount > 1) {
                meterRegistry?.counter(
                    "nats.consumer.redeliveries",
                    "consumer", durableName,
                    "stream", streamName
                )?.increment()
            }
        } catch (ex: Exception) {
            MDC.put("errorClass", ex.javaClass.simpleName)

            LOG.errorf(
                ex, "Error processing message on subject %s (delivery #%s): %s",
                message.subject, deliveryCount, ex.message
            )

            meterRegistry?.counter(
                "nats.consumer.failures",
                "consumer", durableName,
                "stream", streamName
            )?.increment()

            if (deliveryCount >= maxRedeliveries) {
                sendToDeadLetterQueue(message, ex)
                message.ack() // ack to prevent further redelivery
            } else {
                message.nak()
            }
        } finally {
            MDC.remove("eventType")
            MDC.remove("aggregateId")
            MDC.remove("consumer")
            MDC.remove("deliveryCount")
            MDC.remove("errorClass")
        }
    }

    private fun sendToDeadLetterQueue(message: Message, cause: Exception) {
        val dlqSubject = deadLetterSubject
        if (dlqSubject == null) {
            LOG.warnf(
                "No dead-letter subject configured -- dropping message on %s after %s attempts",
                message.subject, maxRedeliveries
            )
            return
        }

        try {
            val headers = Headers()
            headers.add("original-subject", message.subject)
            headers.add("error-message", cause.message ?: "unknown")
            headers.add("consumer", durableName)

            val dlqMessage = NatsMessage.builder()
                .subject(dlqSubject)
                .headers(headers)
                .data(message.data)
                .build()

            connection.publish(dlqMessage)

            meterRegistry?.counter(
                "nats.consumer.dlq",
                "consumer", durableName,
                "stream", streamName
            )?.increment()

            LOG.warnf(
                "Message forwarded to dead-letter subject %s (original=%s)",
                dlqSubject, message.subject
            )
        } catch (dlqEx: Exception) {
            LOG.errorf(
                dlqEx, "Failed to send message to dead-letter subject %s: %s",
                dlqSubject, dlqEx.message
            )
        }
    }
}
