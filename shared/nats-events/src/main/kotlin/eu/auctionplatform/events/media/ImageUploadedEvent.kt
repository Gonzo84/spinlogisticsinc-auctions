package eu.auctionplatform.events.media

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.time.Instant

/**
 * Emitted when a raw image file is uploaded to object storage for a lot.
 *
 * The image processing pipeline listens for this event to generate
 * optimised WebP variants and thumbnails.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ImageUploadedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "media.image.uploaded",

    @JsonProperty("aggregateId")
    override val aggregateId: String,

    @JsonProperty("aggregateType")
    override val aggregateType: String = "Image",

    @JsonProperty("brand")
    override val brand: String,

    @JsonProperty("timestamp")
    override val timestamp: Instant,

    @JsonProperty("version")
    override val version: Long,

    @JsonProperty("metadata")
    override val metadata: EventMetadata? = null,

    /** Unique image identifier (same as aggregateId). */
    @JsonProperty("imageId")
    val imageId: String,

    /** Lot the image belongs to. */
    @JsonProperty("lotId")
    val lotId: String,

    /** Object storage key (e.g., S3 key / MinIO path). */
    @JsonProperty("objectKey")
    val objectKey: String,

    /** MIME content type of the uploaded file (e.g., "image/jpeg"). */
    @JsonProperty("contentType")
    val contentType: String,

    /** File size in bytes. */
    @JsonProperty("sizeBytes")
    val sizeBytes: Long,

    /** User ID of the uploader. */
    @JsonProperty("uploadedBy")
    val uploadedBy: String
) : BaseEvent()
