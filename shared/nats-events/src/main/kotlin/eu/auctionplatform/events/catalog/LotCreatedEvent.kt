package eu.auctionplatform.events.catalog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import eu.auctionplatform.events.BaseEvent
import eu.auctionplatform.events.EventMetadata
import java.math.BigDecimal
import java.time.Instant

/**
 * Geographic location of a lot, used for logistics estimation and
 * regional search filtering.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class LotLocation(
    @JsonProperty("lat")
    val lat: Double,

    @JsonProperty("lng")
    val lng: Double,

    @JsonProperty("country")
    val country: String,

    @JsonProperty("city")
    val city: String
)

/**
 * Emitted when a new lot is created in the catalog by a seller.
 *
 * The lot starts in a draft/pending status and must pass moderation
 * before it becomes visible to bidders.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class LotCreatedEvent(
    @JsonProperty("eventId")
    override val eventId: String,

    @JsonProperty("eventType")
    override val eventType: String = "catalog.lot.created",

    @JsonProperty("aggregateId")
    override val aggregateId: String,

    @JsonProperty("aggregateType")
    override val aggregateType: String = "Lot",

    @JsonProperty("brand")
    override val brand: String,

    @JsonProperty("timestamp")
    override val timestamp: Instant,

    @JsonProperty("version")
    override val version: Long,

    @JsonProperty("metadata")
    override val metadata: EventMetadata? = null,

    /** Unique lot identifier (same as aggregateId). */
    @JsonProperty("lotId")
    val lotId: String,

    /** User ID of the seller who created the lot. */
    @JsonProperty("sellerId")
    val sellerId: String,

    /** Human-readable lot title. */
    @JsonProperty("title")
    val title: String,

    /** Catalog category identifier for classification. */
    @JsonProperty("categoryId")
    val categoryId: String,

    /** Arbitrary key-value specifications (e.g., make, model, year, mileage). */
    @JsonProperty("specifications")
    val specifications: Map<String, Any> = emptyMap(),

    /** Physical location of the lot. */
    @JsonProperty("location")
    val location: LotLocation,

    /** Seller's minimum acceptable price; not disclosed to bidders. */
    @JsonProperty("reservePrice")
    val reservePrice: BigDecimal,

    /** Starting bid amount shown to bidders. */
    @JsonProperty("startingBid")
    val startingBid: BigDecimal,

    /** Initial lot status upon creation (e.g., "DRAFT", "PENDING_REVIEW"). */
    @JsonProperty("status")
    val status: String
) : BaseEvent()
