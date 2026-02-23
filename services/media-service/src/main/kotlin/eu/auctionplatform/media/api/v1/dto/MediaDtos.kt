package eu.auctionplatform.media.api.v1.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

/**
 * Request body for generating a presigned upload URL.
 *
 * @property lotId       The lot to associate the image with.
 * @property contentType MIME type of the file to be uploaded (e.g. "image/jpeg").
 * @property fileName    Optional original filename for reference.
 */
data class PresignedUploadRequest(

    @field:NotBlank(message = "Lot ID is required")
    @JsonProperty("lotId")
    val lotId: String,

    @field:NotBlank(message = "Content type is required")
    @JsonProperty("contentType")
    val contentType: String,

    @JsonProperty("fileName")
    val fileName: String? = null
)

/**
 * Response containing a presigned upload URL and associated metadata.
 *
 * @property imageId    The identifier of the pre-created image record.
 * @property uploadUrl  The presigned URL for the client to PUT the file to.
 * @property objectKey  The MinIO object key where the file will be stored.
 * @property expiresIn  Number of seconds until the presigned URL expires.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PresignedUploadResponse(

    @JsonProperty("imageId")
    val imageId: String,

    @JsonProperty("uploadUrl")
    val uploadUrl: String,

    @JsonProperty("objectKey")
    val objectKey: String,

    @JsonProperty("expiresIn")
    val expiresIn: Long
)

/**
 * Response representing a single image and its processing status.
 *
 * @property id           Unique image identifier.
 * @property lotId        The lot this image belongs to.
 * @property originalUrl  URL to the original uploaded image.
 * @property processedUrl URL to the processed (optimised) image.
 * @property thumbnailUrl URL to the default thumbnail (400px).
 * @property displayOrder Ordering position within the lot gallery.
 * @property isPrimary    Whether this is the hero image for the lot.
 * @property status       Current processing status.
 * @property contentType  MIME type of the original upload.
 * @property fileSize     Size of the original file in bytes.
 * @property createdAt    Timestamp when the image record was created.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ImageResponse(

    @JsonProperty("id")
    val id: String,

    @JsonProperty("lotId")
    val lotId: String,

    @JsonProperty("originalUrl")
    val originalUrl: String?,

    @JsonProperty("processedUrl")
    val processedUrl: String?,

    @JsonProperty("thumbnailUrl")
    val thumbnailUrl: String?,

    @JsonProperty("displayOrder")
    val displayOrder: Int,

    @JsonProperty("isPrimary")
    val isPrimary: Boolean,

    @JsonProperty("status")
    val status: String,

    @JsonProperty("contentType")
    val contentType: String,

    @JsonProperty("fileSize")
    val fileSize: Long,

    @JsonProperty("createdAt")
    val createdAt: Instant
)

/**
 * Request body for updating the display order of an image.
 *
 * @property displayOrder The new display order position (0-based).
 */
data class UpdateOrderRequest(

    @field:NotNull(message = "Display order is required")
    @JsonProperty("displayOrder")
    val displayOrder: Int
)
