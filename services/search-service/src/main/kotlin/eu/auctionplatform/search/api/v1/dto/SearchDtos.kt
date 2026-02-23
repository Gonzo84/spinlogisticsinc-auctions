package eu.auctionplatform.search.api.v1.dto

import eu.auctionplatform.search.application.AggregationResult
import eu.auctionplatform.search.application.BucketCount
import eu.auctionplatform.search.application.LotSearchItem
import eu.auctionplatform.search.application.SearchResult
import eu.auctionplatform.search.application.Suggestion
import java.math.BigDecimal
import java.time.Instant

// =============================================================================
// Search API DTOs – v1
// =============================================================================

// ---- Search Result Response -------------------------------------------------

/**
 * API response wrapper for lot search results.
 *
 * @property items      Lot search result items for the current page.
 * @property totalCount Total number of matching lots.
 * @property page       Current page number (0-based).
 * @property size       Page size used for this request.
 * @property totalPages Total number of pages.
 */
data class SearchResultResponse(
    val items: List<LotSearchResultDto>,
    val totalCount: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
)

/**
 * Individual lot result returned in search responses.
 *
 * Contains a subset of lot fields optimised for search result display
 * (list views, cards) rather than full detail pages.
 *
 * @property id             Unique lot identifier.
 * @property title          Lot title.
 * @property description    Truncated lot description.
 * @property categoryId     Leaf category identifier.
 * @property categoryPath   Full category breadcrumb (e.g. ["Transport", "Trucks", "Tipper Trucks"]).
 * @property brand          Platform brand / tenant.
 * @property country        ISO 3166-1 alpha-2 country code.
 * @property city           City where the lot is located.
 * @property currentBid     Current highest bid amount.
 * @property startingBid    Starting / opening bid amount.
 * @property bidCount       Total number of bids placed.
 * @property reserveStatus  Reserve price status (no_reserve, reserve_met, reserve_not_met).
 * @property auctionEndTime Auction closing time (ISO-8601).
 * @property status         Lot status (active, closed, etc.).
 * @property co2AvoidedKg   Estimated CO2 avoided in kilograms.
 * @property thumbnailUrl   Primary image thumbnail URL for display.
 * @property lotNumber      Human-readable lot number / reference code.
 * @property currency       Currency code for bid amounts.
 */
data class LotSearchResultDto(
    val id: String,
    val title: String,
    val description: String?,
    val categoryId: String?,
    val categoryPath: List<String>,
    val brand: String?,
    val country: String?,
    val city: String?,
    val currentBid: BigDecimal?,
    val startingBid: BigDecimal?,
    val bidCount: Int,
    val reserveStatus: String,
    val auctionEndTime: Instant?,
    val status: String,
    val co2AvoidedKg: Float?,
    val thumbnailUrl: String?,
    val lotNumber: String?,
    val currency: String
)

// ---- Suggestion Response ----------------------------------------------------

/**
 * API response for autocomplete suggestions.
 *
 * @property suggestions List of autocomplete suggestion entries.
 */
data class SuggestionResponse(
    val suggestions: List<SuggestionDto>
)

/**
 * A single autocomplete suggestion.
 *
 * @property text     The suggested completion text (lot title).
 * @property lotId    The lot ID for direct navigation.
 * @property category The leaf category name (displayed as context in the dropdown).
 */
data class SuggestionDto(
    val text: String,
    val lotId: String?,
    val category: String?
)

// ---- Aggregation Response ---------------------------------------------------

/**
 * API response for aggregation (faceted count) queries.
 *
 * @property categories List of category facets with document counts.
 * @property countries  List of country facets with document counts.
 * @property priceRanges List of price range facets with document counts.
 */
data class AggregationResponse(
    val categories: List<FacetBucketDto>,
    val countries: List<FacetBucketDto>,
    val priceRanges: List<FacetBucketDto>
)

/**
 * A single facet bucket in an aggregation response.
 *
 * @property name  The bucket key (category name, country code, price range label).
 * @property count Number of documents matching this bucket.
 */
data class FacetBucketDto(
    val name: String,
    val count: Long
)

// =============================================================================
// Mapping extensions – Domain → DTO
// =============================================================================

/**
 * Converts a domain [SearchResult] to a [SearchResultResponse] API DTO.
 */
fun SearchResult.toResponse(page: Int, size: Int): SearchResultResponse {
    val totalPages = if (size > 0) ((totalCount + size - 1) / size).toInt() else 0
    return SearchResultResponse(
        items = items.map { it.toDto() },
        totalCount = totalCount,
        page = page,
        size = size,
        totalPages = totalPages
    )
}

/**
 * Converts a domain [LotSearchItem] to a [LotSearchResultDto].
 */
fun LotSearchItem.toDto(): LotSearchResultDto = LotSearchResultDto(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    categoryPath = categoryPath,
    brand = brand,
    country = country,
    city = city,
    currentBid = currentBid,
    startingBid = startingBid,
    bidCount = bidCount,
    reserveStatus = reserveStatus,
    auctionEndTime = auctionEndTime,
    status = status,
    co2AvoidedKg = co2AvoidedKg,
    thumbnailUrl = thumbnailUrl,
    lotNumber = lotNumber,
    currency = currency
)

/**
 * Converts a list of domain [Suggestion] objects to a [SuggestionResponse].
 */
fun List<Suggestion>.toResponse(): SuggestionResponse = SuggestionResponse(
    suggestions = map { suggestion ->
        SuggestionDto(
            text = suggestion.text,
            lotId = suggestion.lotId,
            category = suggestion.category
        )
    }
)

/**
 * Converts a domain [AggregationResult] to an [AggregationResponse] API DTO.
 */
fun AggregationResult.toResponse(): AggregationResponse = AggregationResponse(
    categories = categories.map { it.toDto() },
    countries = countries.map { it.toDto() },
    priceRanges = priceRanges.map { it.toDto() }
)

/**
 * Converts a domain [BucketCount] to a [FacetBucketDto].
 */
fun BucketCount.toDto(): FacetBucketDto = FacetBucketDto(
    name = name,
    count = count
)
