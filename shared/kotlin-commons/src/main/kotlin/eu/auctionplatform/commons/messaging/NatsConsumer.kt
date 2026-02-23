package eu.auctionplatform.commons.messaging

import io.nats.client.Connection
import io.nats.client.JetStreamSubscription
import io.nats.client.Message
import io.nats.client.PullSubscribeOptions
import io.nats.client.api.ConsumerConfiguration
import io.nats.client.impl.Headers
import io.nats.client.impl.NatsMessage
import org.slf4j.LoggerFactory
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
 */
abstract class NatsConsumer(
    protected val connection: Connection,
    protected val streamName: String,
    protected val durableName: String,
    protected val filterSubject: String,
    protected val maxRedeliveries: Int = 5,
    protected val deadLetterSubject: String? = null,
    protected val batchSize: Int = 10,
    protected val pollTimeout: Duration = Duration.ofSeconds(5)
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

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
        logger.info(
            "Starting NATS consumer [durable={}, filter={}, stream={}]",
            durableName, filterSubject, streamName
        )

        val jetStream = connection.jetStream()

        val consumerConfig = ConsumerConfiguration.builder()
            .durable(durableName)
            .filterSubject(filterSubject)
            .maxDeliver(maxRedeliveries.toLong())
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
                logger.info("Consumer [{}] interrupted -- stopping", durableName)
                running = false
            } catch (ex: Exception) {
                logger.error("Unexpected error in consumer [{}]: {}", durableName, ex.message, ex)
            }
        }

        logger.info("NATS consumer [{}] stopped", durableName)
    }

    /**
     * Signals the consumer loop to stop after the current batch completes.
     */
    fun stop() {
        running = false
        subscription?.drain(Duration.ofSeconds(10))
        logger.info("Stop requested for NATS consumer [{}]", durableName)
    }

    // ---------------------------------------------------------------------------
    // Internal processing
    // ---------------------------------------------------------------------------

    private fun processMessage(message: Message) {
        try {
            handleMessage(message)
            message.ack()
        } catch (ex: Exception) {
            val metadata = message.metaData()
            val deliveryCount = metadata?.deliveredCount() ?: 0

            logger.error(
                "Error processing message on subject {} (delivery #{}): {}",
                message.subject, deliveryCount, ex.message, ex
            )

            if (deliveryCount >= maxRedeliveries) {
                sendToDeadLetterQueue(message, ex)
                message.ack() // ack to prevent further redelivery
            } else {
                message.nak()
            }
        }
    }

    private fun sendToDeadLetterQueue(message: Message, cause: Exception) {
        val dlqSubject = deadLetterSubject
        if (dlqSubject == null) {
            logger.warn(
                "No dead-letter subject configured -- dropping message on {} after {} attempts",
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
            logger.warn(
                "Message forwarded to dead-letter subject {} (original={})",
                dlqSubject, message.subject
            )
        } catch (dlqEx: Exception) {
            logger.error(
                "Failed to send message to dead-letter subject {}: {}",
                dlqSubject, dlqEx.message, dlqEx
            )
        }
    }
}
