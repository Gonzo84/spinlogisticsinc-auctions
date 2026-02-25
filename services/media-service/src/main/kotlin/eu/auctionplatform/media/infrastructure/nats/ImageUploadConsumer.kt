package eu.auctionplatform.media.infrastructure.nats

import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.media.application.service.ImageProcessingService
import io.nats.client.Connection
import io.nats.client.JetStreamSubscription
import io.nats.client.Message
import io.nats.client.PullSubscribeOptions
import io.nats.client.api.ConsumerConfiguration
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.eclipse.microprofile.config.ConfigProvider
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * NATS JetStream consumer that listens for `media.image.uploaded` events.
 *
 * These events are published by MinIO bucket notifications (or by the API
 * after a successful presigned upload). Upon receiving an event, this consumer
 * triggers the [ImageProcessingService] to run the full image processing pipeline.
 *
 * The consumer uses a durable pull subscription to ensure at-least-once delivery
 * and survives service restarts without losing messages.
 */
@ApplicationScoped
class ImageUploadConsumer @Inject constructor(
    private val imageProcessingService: ImageProcessingService
) {

    private val logger = LoggerFactory.getLogger(ImageUploadConsumer::class.java)

    private var connection: Connection? = null
    private var subscription: JetStreamSubscription? = null
    private var executor: ExecutorService? = null

    @Volatile
    private var running = false

    companion object {
        private const val STREAM_NAME = "MEDIA"
        private const val DURABLE_NAME = "media-image-upload-consumer"
        private const val BATCH_SIZE = 10
        private val POLL_TIMEOUT: Duration = Duration.ofSeconds(5)
    }

    /**
     * Initialises and starts the consumer on application startup.
     *
     * The consumer loop runs on a dedicated background thread to avoid
     * blocking the Quarkus startup sequence.
     */
    fun onStart(@Observes event: StartupEvent) {
        try {
            val natsUrl = ConfigProvider.getConfig()
                .getOptionalValue("nats.url", String::class.java)
                .orElse("nats://localhost:4222")
            connection = io.nats.client.Nats.connect(natsUrl)

            val jetStream = connection!!.jetStream()
            val consumerConfig = ConsumerConfiguration.builder()
                .durable(DURABLE_NAME)
                .filterSubject(NatsSubjects.MEDIA_IMAGE_UPLOADED)
                .maxDeliver(5)
                .build()

            val pullOptions = PullSubscribeOptions.builder()
                .stream(STREAM_NAME)
                .configuration(consumerConfig)
                .build()

            subscription = jetStream.subscribe(NatsSubjects.MEDIA_IMAGE_UPLOADED, pullOptions)
            running = true

            executor = Executors.newSingleThreadExecutor { r ->
                Thread(r, "image-upload-consumer").apply { isDaemon = true }
            }

            executor!!.submit { consumeLoop() }

            logger.info("ImageUploadConsumer started (stream={}, subject={})", STREAM_NAME, NatsSubjects.MEDIA_IMAGE_UPLOADED)
        } catch (ex: Exception) {
            logger.error("Failed to start ImageUploadConsumer: {}", ex.message, ex)
        }
    }

    /**
     * Gracefully shuts down the consumer on application stop.
     */
    fun onStop(@Observes event: ShutdownEvent) {
        running = false
        try {
            subscription?.drain(Duration.ofSeconds(10))
            connection?.close()
            executor?.shutdownNow()
            logger.info("ImageUploadConsumer stopped")
        } catch (ex: Exception) {
            logger.warn("Error during ImageUploadConsumer shutdown: {}", ex.message)
        }
    }

    // -----------------------------------------------------------------------
    // Internal
    // -----------------------------------------------------------------------

    private fun consumeLoop() {
        while (running) {
            try {
                val messages = subscription?.fetch(BATCH_SIZE, POLL_TIMEOUT) ?: emptyList()
                for (msg in messages) {
                    processMessage(msg)
                }
            } catch (ex: InterruptedException) {
                Thread.currentThread().interrupt()
                running = false
            } catch (ex: Exception) {
                logger.error("Error in ImageUploadConsumer loop: {}", ex.message, ex)
            }
        }
    }

    /**
     * Processes a single image upload notification.
     *
     * Extracts the image ID from the event payload and delegates to the
     * image processing pipeline.
     */
    private fun processMessage(message: Message) {
        try {
            val payload = String(message.data, Charsets.UTF_8)
            logger.debug("Received image upload event: {}", payload)

            val eventData = JsonMapper.fromJson<Map<String, Any>>(payload)
            val imageId = eventData["imageId"]?.toString()
                ?: throw IllegalArgumentException("Missing imageId in upload event")

            imageProcessingService.processImage(UUID.fromString(imageId))

            message.ack()
            logger.info("Successfully processed image upload event for imageId={}", imageId)
        } catch (ex: Exception) {
            logger.error("Failed to process image upload event: {}", ex.message, ex)
            message.nak()
        }
    }
}
