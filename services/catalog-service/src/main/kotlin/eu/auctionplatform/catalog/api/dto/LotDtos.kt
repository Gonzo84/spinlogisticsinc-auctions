package eu.auctionplatform.catalog.api.dto

import com.fasterxml.jackson.annotation.JsonAlias
import eu.auctionplatform.catalog.domain.model.LotStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// =============================================================================
// Request DTOs
// =============================================================================

/**
 * Request payload for creating a new lot.
 */
data class CreateLotRequest(
    val brand: String,
    val title: String,
    val description: String,
    val categoryId: UUID,
    val specifications: Map<String, Any> = emptyMap(),
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationAddress: String? = null,
    val locationCountry: String,
    val locationCity: String,
    @JsonAlias("reservePriceAmount")
    val reservePrice: BigDecimal? = null,
    @JsonAlias("startingBidAmount")
    val startingBid: BigDecimal = BigDecimal.ONE,
    val co2AvoidedKg: Double? = null,
    val pickupInfo: String? = null,
    val imageIds: List<String>? = null,
    val images: List<CreateLotImageRequest>? = null
)

/**
 * Image data sent during lot creation (id + URL from presigned upload).
 */
data class CreateLotImageRequest(
    val id: String,
    val url: String
)

/**
 * Request payload for updating an existing lot.
 * Only non-null fields will be applied.
 */
data class UpdateLotRequest(
    val title: String? = null,
    val description: String? = null,
    val categoryId: UUID? = null,
    val specifications: Map<String, Any>? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationAddress: String? = null,
    val locationCountry: String? = null,
    val locationCity: String? = null,
    val reservePrice: BigDecimal? = null,
    val startingBid: BigDecimal? = null,
    val co2AvoidedKg: Double? = null,
    val pickupInfo: String? = null
)

/**
 * Request payload for assigning lots to an auction event.
 */
data class AssignToAuctionRequest(
    val auctionId: UUID
)

/**
 * Request payload for combining multiple lots into a single lot.
 */
data class CombineLotsRequest(
    val lotIds: List<UUID>,
    val title: String,
    val description: String
)

/**
 * Query parameters for listing lots with filtering.
 */
data class LotListFilter(
    val brand: String? = null,
    val categoryId: UUID? = null,
    val country: String? = null,
    val status: LotStatus? = null,
    val sellerId: UUID? = null,
    val auctionId: UUID? = null,
    val search: String? = null,
    val sortBy: String? = null,
    val sortDir: String? = null,
    val page: Int = 0,
    val pageSize: Int = 20
)

// =============================================================================
// Response DTOs
// =============================================================================

/**
 * Response representation of a lot.
 */
data class LotResponse(
    val id: UUID,
    val sellerId: UUID,
    val brand: String,
    val title: String,
    val description: String,
    val categoryId: UUID,
    val specifications: Map<String, Any>,
    val locationLat: Double?,
    val locationLng: Double?,
    val locationAddress: String?,
    val locationCountry: String,
    val locationCity: String,
    val reservePrice: BigDecimal?,
    val startingBid: BigDecimal,
    val auctionId: UUID?,
    val status: LotStatus,
    val co2AvoidedKg: Double?,
    val pickupInfo: String?,
    val images: List<LotImageResponse> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Response representation of a lot image.
 */
data class LotImageResponse(
    val id: UUID,
    val imageUrl: String,
    val thumbnailUrl: String?,
    val displayOrder: Int,
    val isPrimary: Boolean
)

/**
 * Compact lot summary for list views and search results.
 */
data class LotSummaryResponse(
    val id: UUID,
    val sellerId: UUID,
    val categoryId: UUID?,
    val title: String,
    val brand: String,
    val locationCountry: String,
    val locationCity: String,
    val startingBid: BigDecimal,
    val status: LotStatus,
    val primaryImageUrl: String?,
    val co2AvoidedKg: Double?,
    val createdAt: Instant
)
