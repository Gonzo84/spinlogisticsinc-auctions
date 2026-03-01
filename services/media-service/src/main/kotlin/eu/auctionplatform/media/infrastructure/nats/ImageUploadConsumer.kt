package eu.auctionplatform.media.infrastructure.nats

import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.media.application.service.ImageProcessingService
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
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
 * Uses the CDI-managed NATS [Connection] and an inner [NatsConsumer] for
 * durable pull subscription with at-least-once delivery guarantees.
 */
@ApplicationScoped
@Startup
class ImageUploadConsumer @Inject constructor(
    private val connection: Connection,
    private val imageProcessingService: ImageProcessingService
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(ImageUploadConsumer::class.java)

        private const val STREAM_NAME = "MEDIA"
        private const val DURABLE_NAME = "media-image-upload-consumer"
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "image-upload-consumer").apply { isDaemon = true }
    }

    /**
     * Starts the consumer thread on application startup.
     */
    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.info("Starting ImageUploadConsumer")
        executor.submit { createImageUploadConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.info("Shutting down ImageUploadConsumer")
        executor.shutdownNow()
    }

    // -----------------------------------------------------------------------
    // Consumer factory
    // -----------------------------------------------------------------------

    private fun createImageUploadConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = DURABLE_NAME,
            filterSubject = NatsSubjects.MEDIA_IMAGE_UPLOADED,
            deadLetterSubject = "dlq.media.image.uploaded"
        ) {
            override fun handleMessage(message: Message) {
                handleImageUpload(message)
            }
        }

    // -----------------------------------------------------------------------
    // Event handler
    // -----------------------------------------------------------------------

    /**
     * Processes a single image upload notification.
     *
     * Extracts the image ID from the event payload and delegates to the
     * image processing pipeline.
     */
    @Suppress("UNCHECKED_CAST")
    private fun handleImageUpload(message: Message) {
        val payload = String(message.data, Charsets.UTF_8)
        LOG.debugf("Received image upload event: %s", payload)

        val eventData = JsonMapper.instance.readValue(payload, Map::class.java) as Map<String, Any>
        val imageId = eventData["imageId"]?.toString()
            ?: throw IllegalArgumentException("Missing imageId in upload event")

        imageProcessingService.processImage(UUID.fromString(imageId))

        LOG.infof("Successfully processed image upload event for imageId=%s", imageId)
    }
}
