package eu.auctionplatform.events.media

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.time.Instant

/**
 * Emitted when the image processing pipeline has finished generating
 * optimised variants (WebP, thumbnails) from a raw uploaded image.
 *
 * The catalog service and CDN cache listen for this event to update
 * the lot's image URLs for front-end consumption.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ImageProcessedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "media.image.processed",

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

    /** Image identifier (same as aggregateId). */
    @JsonProperty("imageId")
    val imageId: String,

    /** Lot the image belongs to. */
    @JsonProperty("lotId")
    val lotId: String,

    /** CDN URL of the original full-resolution image. */
    @JsonProperty("originalUrl")
    val originalUrl: String,

    /** CDN URL of the WebP-optimised variant. */
    @JsonProperty("webpUrl")
    val webpUrl: String,

    /** Map of thumbnail size label to CDN URL (e.g., "240x180" -> "https://..."). */
    @JsonProperty("thumbnails")
    val thumbnails: Map<String, String>
) : BaseEvent()
