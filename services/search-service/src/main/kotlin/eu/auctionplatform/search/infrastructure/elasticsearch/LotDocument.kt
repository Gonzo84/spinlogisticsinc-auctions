package eu.auctionplatform.search.infrastructure.elasticsearch

import java.math.BigDecimal
import java.time.Instant

// =============================================================================
// Lot Document – Elasticsearch document representation
// =============================================================================

/**
 * Data class representing a lot document stored in the "lots_active" Elasticsearch
 * index.
 *
 * All fields mirror the index mapping defined in [LotIndexService]. This class is
 * used both when indexing documents (serialised to JSON via Jackson) and when
 * reading search hits back from Elasticsearch.
 *
 * @property id              Unique lot identifier (used as the ES document _id).
 * @property title           Multi-language analysed title of the lot.
 * @property description     Full-text description of the lot.
 * @property categoryId      Leaf category identifier (keyword).
 * @property categoryPath    Full category breadcrumb path, e.g. ["Transport", "Trucks", "Tipper Trucks"].
 * @property brand           Platform brand / tenant code (e.g. "troostwijk").
 * @property country         ISO 3166-1 alpha-2 country code where the lot is located.
 * @property city            City name of the lot location.
 * @property location        Geo-point for proximity searches ([lat, lon]).
 * @property currentBid      Current highest bid amount (scaled_float, scaling_factor=100).
 * @property startingBid     Starting bid / opening price.
 * @property bidCount        Total number of bids placed on this lot.
 * @property reserveStatus   Reserve price status: "no_reserve", "reserve_met", or "reserve_not_met".
 * @property auctionEndTime  Scheduled (or extended) auction closing time.
 * @property status          Lot lifecycle status (e.g. "active", "closed", "withdrawn").
 * @property co2AvoidedKg    Estimated CO2 avoided by reselling this item (kg).
 * @property specifications  Dynamic key-value map of lot-specific specs (weight, mileage, etc.).
 * @property images          Nested list of image objects.
 * @property createdAt       Timestamp when the lot was first created.
 * @property sellerId        Identifier of the seller who listed this lot.
 * @property lotNumber       Human-readable lot number / reference code.
 * @property currency        ISO 4217 currency code for bid amounts.
 */
data class LotDocument(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val categoryId: String? = null,
    val categoryPath: List<String> = emptyList(),
    val brand: String? = null,
    val country: String? = null,
    val city: String? = null,
    val location: GeoPoint? = null,
    val currentBid: BigDecimal? = null,
    val startingBid: BigDecimal? = null,
    val bidCount: Int = 0,
    val reserveStatus: String = "no_reserve",
    val auctionEndTime: Instant? = null,
    val status: String = "active",
    val co2AvoidedKg: Float? = null,
    val specifications: Map<String, Any>? = null,
    val images: List<LotImage> = emptyList(),
    val createdAt: Instant? = null,
    val sellerId: String? = null,
    val lotNumber: String? = null,
    val currency: String = "EUR"
)

/**
 * Geo-point representation compatible with Elasticsearch's `geo_point` type.
 *
 * @property lat Latitude in decimal degrees.
 * @property lon Longitude in decimal degrees.
 */
data class GeoPoint(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

/**
 * Nested image object stored within a [LotDocument].
 *
 * @property url          Full-resolution image URL.
 * @property thumbnailUrl Thumbnail image URL.
 * @property isPrimary    Whether this is the primary / hero image for the lot.
 */
data class LotImage(
    val url: String = "",
    val thumbnailUrl: String? = null,
    val isPrimary: Boolean = false
)
