package eu.auctionplatform.media.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Domain model for a media image associated with an auction lot.
 *
 * Each lot can have multiple images. One image per lot is designated as
 * the primary image (used for listing thumbnails, search results, etc.).
 *
 * Images go through a processing pipeline: upload -> EXIF stripping ->
 * auto-orient -> WebP conversion -> thumbnail generation (200, 400, 800px).
 *
 * @property id           Unique image identifier.
 * @property lotId        The lot this image belongs to.
 * @property objectKey    The MinIO/S3 object key for the original upload.
 * @property originalUrl  Presigned or CDN URL to the original image.
 * @property processedUrl URL to the processed (WebP, EXIF-stripped) image.
 * @property thumbnailUrl URL to the primary thumbnail (400px default).
 * @property displayOrder Ordering position within the lot's image gallery.
 * @property isPrimary    Whether this is the primary/hero image for the lot.
 * @property status       Current lifecycle status in the processing pipeline.
 * @property contentType  MIME type of the original upload (e.g. "image/jpeg").
 * @property fileSize     Size of the original file in bytes.
 * @property createdAt    Timestamp when the image record was created.
 */
data class MediaImage(
    val id: UUID = UUID.randomUUID(),
    val lotId: UUID,
    val objectKey: String,
    val originalUrl: String? = null,
    val processedUrl: String? = null,
    val thumbnailUrl: String? = null,
    val displayOrder: Int = 0,
    val isPrimary: Boolean = false,
    val status: ImageStatus = ImageStatus.UPLOADING,
    val contentType: String,
    val fileSize: Long = 0,
    val createdAt: Instant = Instant.now()
)
