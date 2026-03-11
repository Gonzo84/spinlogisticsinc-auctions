package eu.auctionplatform.media.application.service

import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.media.domain.model.ImageStatus
import eu.auctionplatform.media.domain.model.MediaImage
import eu.auctionplatform.media.infrastructure.minio.MinioService
import eu.auctionplatform.media.infrastructure.persistence.repository.ImageRepository
import io.nats.client.Connection
import io.nats.client.impl.NatsMessage
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.coobird.thumbnailator.Thumbnails
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.UUID

/**
 * Orchestrates the image processing pipeline for uploaded lot images.
 *
 * ## Pipeline stages
 *
 * 1. **Validate** -- confirm the image exists in the database and is in UPLOADING status.
 * 2. **Download** -- fetch the original image from MinIO.
 * 3. **Strip EXIF** -- remove metadata (GPS, camera info) for privacy compliance (GDPR).
 * 4. **Auto-orient** -- apply EXIF orientation tag, then discard the tag.
 * 5. **Generate WebP** -- create an optimised WebP version for web delivery.
 * 6. **Create thumbnails** -- generate 200px, 400px, and 800px width variants.
 * 7. **Store processed** -- upload all derivatives to the thumbnails bucket.
 * 8. **Update database** -- set URLs and transition status to READY.
 * 9. **Publish event** -- emit `media.image.processed` NATS event for downstream consumers.
 *
 * If any stage fails, the image status is set to FAILED.
 */
@ApplicationScoped
class ImageProcessingService @Inject constructor(
    private val imageRepository: ImageRepository,
    private val minioService: MinioService,
    private val natsConnection: Connection,

    @ConfigProperty(name = "minio.endpoint")
    private val minioEndpoint: String,

    @ConfigProperty(name = "minio.access-key")
    private val accessKey: String,

    @ConfigProperty(name = "minio.secret-key")
    private val secretKey: String,

    @ConfigProperty(name = "minio.region", defaultValue = "eu-west-1")
    private val region: String,

    @ConfigProperty(name = "minio.bucket.media")
    private val mediaBucket: String,

    @ConfigProperty(name = "minio.bucket.thumbnails")
    private val thumbnailsBucket: String
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(ImageProcessingService::class.java)

        /** Thumbnail widths in pixels. */
        val THUMBNAIL_SIZES = listOf(200, 400, 800)
        private const val WEBP_OUTPUT_FORMAT = "png" // Thumbnailator does not natively support WebP; use PNG as fallback
    }

    private val s3Client: S3Client by lazy {
        S3Client.builder()
            .endpointOverride(URI.create(minioEndpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
            )
            .region(Region.of(region))
            .forcePathStyle(true)
            .build()
    }

    /**
     * Runs the full processing pipeline for the given image.
     *
     * This method is idempotent -- if the image is already in READY or PROCESSING
     * state, it will skip or re-process as appropriate.
     *
     * @param imageId The unique identifier of the image to process.
     */
    fun processImage(imageId: UUID) {
        val image = imageRepository.findById(imageId)
        if (image == null) {
            LOG.warnf("Image %s not found -- skipping processing", imageId)
            return
        }

        if (image.status == ImageStatus.READY) {
            LOG.infof("Image %s already processed -- skipping", imageId)
            return
        }

        LOG.infof("Starting image processing pipeline for image %s (lot=%s)", imageId, image.lotId)

        // Transition to PROCESSING
        imageRepository.updateStatus(imageId, ImageStatus.PROCESSING)

        try {
            // Step 1: Download original from MinIO
            val originalBytes = downloadOriginal(image)

            // Step 2 & 3: Strip EXIF and auto-orient (handled by Thumbnailator)
            val processedBytes = stripExifAndOrient(originalBytes)

            // Step 4: Upload processed (full-size) image
            val processedKey = "processed/${image.lotId}/${imageId}.png"
            uploadToMinio(thumbnailsBucket, processedKey, processedBytes, "image/png")

            // Step 5: Generate and upload thumbnails at 200, 400, 800px widths
            var primaryThumbnailKey = ""
            for (size in THUMBNAIL_SIZES) {
                val thumbnailBytes = generateThumbnail(originalBytes, size)
                val thumbnailKey = "thumbnails/${image.lotId}/${imageId}_${size}w.png"
                uploadToMinio(thumbnailsBucket, thumbnailKey, thumbnailBytes, "image/png")

                // Use 400px as the default thumbnail
                if (size == 400) {
                    primaryThumbnailKey = thumbnailKey
                }
            }

            // Step 6: Generate public URLs for the stored objects (non-expiring)
            val originalUrl = minioService.getPublicUrl(mediaBucket, image.objectKey)
            val processedUrl = minioService.getPublicUrl(thumbnailsBucket, processedKey)
            val thumbnailUrl = minioService.getPublicUrl(thumbnailsBucket, primaryThumbnailKey)

            // Step 7: Update database with URLs and READY status
            imageRepository.updateUrls(imageId, originalUrl, processedUrl, thumbnailUrl, ImageStatus.READY)

            LOG.infof(
                "Image processing complete for image %s (lot=%s). Generated %s thumbnails.",
                imageId, image.lotId, THUMBNAIL_SIZES.size
            )

            // Step 8: Publish processed event (fire-and-forget via NATS core publish)
            publishProcessedEvent(image)

        } catch (ex: Exception) {
            LOG.errorf(ex, "Image processing failed for image %s: %s", imageId, ex.message)
            imageRepository.updateStatus(imageId, ImageStatus.FAILED)
        }
    }

    // -----------------------------------------------------------------------
    // Pipeline stages
    // -----------------------------------------------------------------------

    /**
     * Downloads the original image bytes from MinIO.
     */
    private fun downloadOriginal(image: MediaImage): ByteArray {
        val getRequest = GetObjectRequest.builder()
            .bucket(mediaBucket)
            .key(image.objectKey)
            .build()

        return s3Client.getObject(getRequest).use { response ->
            response.readAllBytes()
        }
    }

    /**
     * Strips EXIF metadata and applies auto-orientation.
     *
     * Thumbnailator automatically reads EXIF orientation and rotates
     * the image accordingly, then outputs without EXIF data.
     */
    private fun stripExifAndOrient(imageBytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        Thumbnails.of(ByteArrayInputStream(imageBytes))
            .scale(1.0) // Keep original size
            .outputFormat(WEBP_OUTPUT_FORMAT)
            .toOutputStream(output)
        return output.toByteArray()
    }

    /**
     * Generates a thumbnail at the specified width, maintaining aspect ratio.
     *
     * @param imageBytes The source image bytes.
     * @param width      Target width in pixels.
     * @return The thumbnail image bytes.
     */
    private fun generateThumbnail(imageBytes: ByteArray, width: Int): ByteArray {
        val output = ByteArrayOutputStream()
        Thumbnails.of(ByteArrayInputStream(imageBytes))
            .width(width)
            .outputFormat(WEBP_OUTPUT_FORMAT)
            .toOutputStream(output)
        return output.toByteArray()
    }

    /**
     * Uploads a byte array to MinIO.
     */
    private fun uploadToMinio(bucket: String, key: String, data: ByteArray, contentType: String) {
        val putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .contentLength(data.size.toLong())
            .build()

        s3Client.putObject(putRequest, RequestBody.fromBytes(data))
    }

    /**
     * Publishes a `media.image.processed` event to NATS JetStream.
     *
     * Downstream consumers (catalog service, search indexer, etc.) can react
     * to this event to update their projections with the new image URLs.
     */
    private fun publishProcessedEvent(image: MediaImage) {
        try {
            val eventPayload = mapOf(
                "eventType" to NatsSubjects.MEDIA_IMAGE_PROCESSED,
                "imageId" to image.id.toString(),
                "lotId" to image.lotId.toString(),
                "status" to ImageStatus.READY.name,
                "timestamp" to java.time.Instant.now().toString()
            )
            val json = JsonMapper.toJson(eventPayload)
            LOG.debugf("Publishing image processed event: %s", json)

            val message = NatsMessage.builder()
                .subject(NatsSubjects.MEDIA_IMAGE_PROCESSED)
                .data(json.toByteArray(Charsets.UTF_8))
                .build()

            val ack = natsConnection.jetStream().publish(message)
            LOG.infof(
                "Published image processed event for image %s (stream=%s, seq=%s)",
                image.id, ack.stream, ack.seqno
            )
        } catch (ex: Exception) {
            LOG.warnf("Failed to publish image processed event for image %s: %s", image.id, ex.message)
        }
    }
}
