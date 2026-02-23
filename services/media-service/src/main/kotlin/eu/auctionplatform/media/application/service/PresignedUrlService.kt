package eu.auctionplatform.media.application.service

import eu.auctionplatform.media.domain.model.ImageStatus
import eu.auctionplatform.media.domain.model.MediaImage
import eu.auctionplatform.media.infrastructure.minio.MinioService
import eu.auctionplatform.media.infrastructure.persistence.repository.ImageRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID

/**
 * Result of generating a presigned upload URL.
 *
 * @property imageId    The identifier of the created image record.
 * @property uploadUrl  The presigned URL for the client to PUT the file to.
 * @property objectKey  The MinIO object key where the file will be stored.
 * @property expiresIn  Number of seconds until the presigned URL expires.
 */
data class PresignedUploadResult(
    val imageId: UUID,
    val uploadUrl: String,
    val objectKey: String,
    val expiresIn: Long
)

/**
 * Application service for generating presigned upload URLs for lot images.
 *
 * Workflow:
 * 1. Client requests a presigned URL for a specific lot and content type.
 * 2. This service creates an image record in UPLOADING status.
 * 3. Returns the presigned URL for the client to upload directly to MinIO.
 * 4. After upload, MinIO triggers a bucket notification that is consumed
 *    by [ImageUploadConsumer] to start the processing pipeline.
 */
@ApplicationScoped
class PresignedUrlService @Inject constructor(
    private val minioService: MinioService,
    private val imageRepository: ImageRepository,

    @ConfigProperty(name = "minio.bucket.media")
    private val mediaBucket: String
) {

    private val logger = LoggerFactory.getLogger(PresignedUrlService::class.java)

    companion object {
        /** Default presigned URL validity period. */
        private val DEFAULT_EXPIRY: Duration = Duration.ofMinutes(15)

        /** Allowed MIME types for image uploads. */
        val ALLOWED_CONTENT_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/tiff"
        )

        /** Maximum file size in bytes (50 MB). */
        const val MAX_FILE_SIZE: Long = 50 * 1024 * 1024
    }

    /**
     * Generates a presigned upload URL for a lot image.
     *
     * Creates an image record in the database with UPLOADING status and returns
     * a presigned URL that the client can use to upload the file directly to MinIO.
     *
     * @param lotId       The lot this image will belong to.
     * @param contentType The MIME type of the file to be uploaded.
     * @param fileName    Optional original filename (used for object key generation).
     * @return [PresignedUploadResult] containing the presigned URL and metadata.
     * @throws IllegalArgumentException if the content type is not allowed.
     */
    fun generatePresignedUploadUrl(
        lotId: UUID,
        contentType: String,
        fileName: String? = null
    ): PresignedUploadResult {
        require(contentType in ALLOWED_CONTENT_TYPES) {
            "Content type '$contentType' is not allowed. Allowed types: $ALLOWED_CONTENT_TYPES"
        }

        val imageId = UUID.randomUUID()
        val extension = extensionFromContentType(contentType)
        val objectKey = "uploads/$lotId/${imageId}.$extension"

        // Determine display order (append at end)
        val existingCount = imageRepository.countByLotId(lotId)
        val isPrimary = existingCount == 0L

        // Create image record in UPLOADING status
        val image = MediaImage(
            id = imageId,
            lotId = lotId,
            objectKey = objectKey,
            displayOrder = existingCount.toInt(),
            isPrimary = isPrimary,
            status = ImageStatus.UPLOADING,
            contentType = contentType
        )
        imageRepository.save(image)

        // Generate presigned URL for upload
        val uploadUrl = minioService.generatePresignedUploadUrl(
            bucket = mediaBucket,
            objectKey = objectKey,
            contentType = contentType,
            expiry = DEFAULT_EXPIRY
        )

        val expiresIn = DEFAULT_EXPIRY.toSeconds()

        logger.info(
            "Generated presigned upload URL for lot {} (imageId={}, type={}, expiresIn={}s)",
            lotId, imageId, contentType, expiresIn
        )

        return PresignedUploadResult(
            imageId = imageId,
            uploadUrl = uploadUrl,
            objectKey = objectKey,
            expiresIn = expiresIn
        )
    }

    /**
     * Derives a file extension from a MIME content type.
     */
    private fun extensionFromContentType(contentType: String): String = when (contentType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/tiff" -> "tiff"
        else -> "bin"
    }
}
