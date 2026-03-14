package eu.auctionplatform.search.application

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.GeoDistanceType
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.mapping.FieldType
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.SearchResponse
import eu.auctionplatform.search.infrastructure.elasticsearch.LotDocument
import eu.auctionplatform.search.infrastructure.elasticsearch.LotIndexService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.time.Instant

// =============================================================================
// Domain models for search
// =============================================================================

/**
 * Sort options for lot search results.
 */
enum class SortOption {
    /** Lots ending soonest first. */
    CLOSING_SOONEST,
    /** Lowest current bid first. */
    PRICE_ASC,
    /** Highest current bid first. */
    PRICE_DESC,
    /** Most recently created first. */
    NEWEST,
    /** Most bids first. */
    BID_COUNT_DESC,
    /** Elasticsearch relevance score (default for text queries). */
    RELEVANCE
}

/**
 * Encapsulates all search parameters for a lot search request.
 *
 * @property q               Free-text query string.
 * @property categoryId      Filter by leaf category ID.
 * @property country         Filter by ISO 3166-1 alpha-2 country code.
 * @property lat             Latitude for geo-distance filtering.
 * @property lng             Longitude for geo-distance filtering.
 * @property radiusKm        Radius in kilometres for geo-distance filtering.
 * @property minPrice        Minimum current bid price filter.
 * @property maxPrice        Maximum current bid price filter.
 * @property reserveStatus   Filter by reserve status (no_reserve, reserve_met, reserve_not_met).
 * @property sort            Sort order for results.
 * @property page            Page number (0-based).
 * @property size            Number of results per page.
 */
data class SearchQuery(
    val q: String? = null,
    val categoryId: String? = null,
    val country: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val radiusKm: Int? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val reserveStatus: String? = null,
    val sort: SortOption = SortOption.RELEVANCE,
    val page: Int = 0,
    val size: Int = 20
)

/**
 * A single lot item in search results.
 */
data class LotSearchItem(
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

/**
 * Search result container returned by [SearchService.search] and [SearchService.nearby].
 *
 * @property items       List of matching lot items for the current page.
 * @property totalCount  Total number of matching lots across all pages.
 * @property aggregations Optional aggregation buckets (populated when requested).
 */
data class SearchResult(
    val items: List<LotSearchItem>,
    val totalCount: Long,
    val aggregations: AggregationResult? = null
)

/**
 * Autocomplete suggestion returned by [SearchService.suggest].
 *
 * @property text     The suggested completion text.
 * @property lotId    The lot ID this suggestion refers to.
 * @property category The category name (for context in the suggestion dropdown).
 */
data class Suggestion(
    val text: String,
    val lotId: String?,
    val category: String?
)

/**
 * Aggregation (faceted count) results.
 *
 * @property categories List of category buckets with their document counts.
 * @property countries  List of country buckets with their document counts.
 * @property priceRanges List of price range buckets with their document counts.
 */
data class AggregationResult(
    val categories: List<BucketCount> = emptyList(),
    val countries: List<BucketCount> = emptyList(),
    val priceRanges: List<BucketCount> = emptyList()
)

/**
 * A single aggregation bucket with a name and document count.
 */
data class BucketCount(
    val name: String,
    val count: Long
)

// =============================================================================
// Search Service – Application layer
// =============================================================================

/**
 * Application service providing full-text search, autocomplete, aggregations,
 * and proximity search over the Elasticsearch lots index.
 *
 * This service translates [SearchQuery] parameters into Elasticsearch queries
 * using the Elasticsearch Java client, executes them against the active lots
 * index, and maps the results back into domain-level DTOs.
 */
@ApplicationScoped
class SearchService @Inject constructor(
    private val esClient: ElasticsearchClient,
    private val lotIndexService: LotIndexService
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(SearchService::class.java)

        /** Default number of suggestions returned by autocomplete. */
        private const val DEFAULT_SUGGEST_LIMIT = 10

        /** Maximum allowed page size to prevent abuse. */
        private const val MAX_PAGE_SIZE = 100

        /** Price range boundaries for aggregation buckets (in EUR). */
        private val PRICE_RANGE_BOUNDARIES = listOf(0.0, 100.0, 500.0, 1000.0, 5000.0, 10000.0, 50000.0)
    }

    // -------------------------------------------------------------------------
    // Full-text search
    // -------------------------------------------------------------------------

    /**
     * Executes a full-text search with optional filters, sorting, and pagination.
     *
     * The search query is built as a bool query combining:
     * - `must`: multi-match on title (all language sub-fields) and description
     * - `filter`: term filters for categoryId, country, reserveStatus, status
     * - `filter`: range filter for price (minPrice/maxPrice)
     * - `filter`: geo-distance filter when lat/lng/radius are provided
     *
     * @param query The search parameters.
     * @return [SearchResult] with matching lots, total count, and optional aggregations.
     */
    fun search(query: SearchQuery): SearchResult {
        val safeSize = query.size.coerceIn(1, MAX_PAGE_SIZE)
        val from = query.page * safeSize
        val indexName = lotIndexService.activeIndexName

        LOG.debugf("Executing search: q=[%s], category=[%s], country=[%s], page=%s, size=%s",
            query.q, query.categoryId, query.country, query.page, safeSize)

        val response = esClient.search({ s ->
            s.index(indexName)
                .query(buildSearchQuery(query))
                .from(from)
                .size(safeSize)
                .apply { applySorting(this, query.sort, query.lat, query.lng) }
                // Include source filtering to reduce payload
                .source { src ->
                    src.filter { f -> f.excludes("specifications") }
                }
        }, LotDocument::class.java)

        val items = response.hits().hits().mapNotNull { hit ->
            hit.source()?.toSearchItem()
        }
        val totalCount = response.hits().total()?.value() ?: 0L

        LOG.debugf("Search returned %s items (total=%s)", items.size, totalCount)
        return SearchResult(items = items, totalCount = totalCount)
    }

    // -------------------------------------------------------------------------
    // Autocomplete / Suggest
    // -------------------------------------------------------------------------

    /**
     * Provides autocomplete suggestions for a given prefix string.
     *
     * Uses a prefix query on the `title.keyword` field combined with a match
     * on the analysed `title` field, then extracts distinct title values from
     * the results.
     *
     * @param prefix The user's partial input.
     * @param limit  Maximum number of suggestions (default 10).
     * @return List of [Suggestion] objects.
     */
    fun suggest(prefix: String, limit: Int = DEFAULT_SUGGEST_LIMIT): List<Suggestion> {
        if (prefix.isBlank()) return emptyList()

        val safeLimit = limit.coerceIn(1, 50)
        val indexName = lotIndexService.activeIndexName

        LOG.debugf("Autocomplete: prefix=[%s], limit=%s", prefix, safeLimit)

        val response = esClient.search({ s ->
            s.index(indexName)
                .query { q ->
                    q.bool { b ->
                        b.must { m ->
                            m.bool { inner ->
                                inner.should { sh ->
                                    sh.matchPhrasePrefix { mp ->
                                        mp.field("title").query(prefix)
                                    }
                                }.should { sh ->
                                    sh.prefix { p ->
                                        p.field("title.keyword")
                                            .value(prefix)
                                            .caseInsensitive(true)
                                    }
                                }.minimumShouldMatch("1")
                            }
                        }.filter { f ->
                            f.term { t -> t.field("status").value("active") }
                        }
                    }
                }
                .size(safeLimit)
                .source { src ->
                    src.filter { f ->
                        f.includes("title", "id", "categoryPath")
                    }
                }
        }, LotDocument::class.java)

        val suggestions = response.hits().hits().mapNotNull { hit ->
            val source = hit.source() ?: return@mapNotNull null
            Suggestion(
                text = source.title,
                lotId = source.id,
                category = source.categoryPath.lastOrNull()
            )
        }

        LOG.debugf("Autocomplete returned %s suggestions for prefix [%s]", suggestions.size, prefix)
        return suggestions
    }

    // -------------------------------------------------------------------------
    // Aggregations (faceted counts)
    // -------------------------------------------------------------------------

    /**
     * Returns aggregation (faceted) counts for categories, countries, and price
     * ranges, optionally filtered by the same criteria as [search].
     *
     * @param query The search parameters (used to scope the aggregations).
     * @return [AggregationResult] with bucket counts.
     */
    fun aggregations(query: SearchQuery): AggregationResult {
        val indexName = lotIndexService.activeIndexName

        LOG.debugf("Computing aggregations for query: q=[%s], category=[%s]", query.q, query.categoryId)

        val response = esClient.search({ s ->
            s.index(indexName)
                .query(buildSearchQuery(query))
                .size(0) // we only need aggregations, no hits
                .aggregations("categories", Aggregation.Builder()
                    .terms { t -> t.field("categoryPath").size(50) }
                    .build())
                .aggregations("countries", Aggregation.Builder()
                    .terms { t -> t.field("country").size(50) }
                    .build())
                .aggregations("price_ranges", Aggregation.Builder()
                    .range { r ->
                        r.field("currentBid")
                            .ranges(
                                AggregationRange.of { rng -> rng.key("0-100").from(0.0).to(100.0) },
                                AggregationRange.of { rng -> rng.key("100-500").from(100.0).to(500.0) },
                                AggregationRange.of { rng -> rng.key("500-1000").from(500.0).to(1000.0) },
                                AggregationRange.of { rng -> rng.key("1000-5000").from(1000.0).to(5000.0) },
                                AggregationRange.of { rng -> rng.key("5000-10000").from(5000.0).to(10000.0) },
                                AggregationRange.of { rng -> rng.key("10000-50000").from(10000.0).to(50000.0) },
                                AggregationRange.of { rng -> rng.key("50000+").from(50000.0) }
                            )
                    }
                    .build())
        }, Void::class.java)

        val categories = extractTermBuckets(response, "categories")
        val countries = extractTermBuckets(response, "countries")
        val priceRanges = extractRangeBuckets(response, "price_ranges")

        LOG.debugf("Aggregations: %s categories, %s countries, %s price ranges",
            categories.size, countries.size, priceRanges.size)

        return AggregationResult(
            categories = categories,
            countries = countries,
            priceRanges = priceRanges
        )
    }

    // -------------------------------------------------------------------------
    // Nearby / Proximity search
    // -------------------------------------------------------------------------

    /**
     * Searches for lots within a given radius of a geographic point.
     *
     * Results are sorted by distance from the provided coordinates (closest first).
     *
     * @param lat      Latitude of the center point.
     * @param lng      Longitude of the center point.
     * @param radiusKm Search radius in kilometres.
     * @param category Optional category filter.
     * @return [SearchResult] with nearby lots.
     */
    fun nearby(lat: Double, lng: Double, radiusKm: Int, category: String?): SearchResult {
        val indexName = lotIndexService.activeIndexName

        LOG.debugf("Nearby search: lat=%s, lng=%s, radius=%skm, category=[%s]",
            lat, lng, radiusKm, category)

        val response = esClient.search({ s ->
            s.index(indexName)
                .query { q ->
                    q.bool { b ->
                        b.filter { f ->
                            f.geoDistance { geo ->
                                geo.field("location")
                                    .location { loc -> loc.latlon { ll -> ll.lat(lat).lon(lng) } }
                                    .distance("${radiusKm}km")
                                    .distanceType(GeoDistanceType.Arc)
                            }
                        }.filter { f ->
                            f.term { t -> t.field("status").value("active") }
                        }.apply {
                            if (category != null) {
                                filter { f ->
                                    f.term { t -> t.field("categoryId").value(category) }
                                }
                            }
                        }
                    }
                }
                .sort { so ->
                    so.geoDistance { gd ->
                        gd.field("location")
                            .location { loc -> loc.latlon { ll -> ll.lat(lat).lon(lng) } }
                            .order(SortOrder.Asc)
                            .unit(co.elastic.clients.elasticsearch._types.DistanceUnit.Kilometers)
                    }
                }
                .size(50)
        }, LotDocument::class.java)

        val items = response.hits().hits().mapNotNull { hit ->
            hit.source()?.toSearchItem()
        }
        val totalCount = response.hits().total()?.value() ?: 0L

        LOG.debugf("Nearby search returned %s items within %skm", items.size, radiusKm)
        return SearchResult(items = items, totalCount = totalCount)
    }

    // -------------------------------------------------------------------------
    // Query building
    // -------------------------------------------------------------------------

    /**
     * Builds the composite Elasticsearch bool query from a [SearchQuery].
     *
     * The bool query structure:
     * - `must`: full-text multi-match query on title fields + description (if q is provided)
     * - `filter`: exact-match term filters for keyword fields
     * - `filter`: range filter for price bounds
     * - `filter`: geo-distance filter for location-based queries
     * - `filter`: always filter on status=active
     */
    private fun buildSearchQuery(query: SearchQuery): Query {
        return Query.Builder().bool { bool ->
            // Always filter to active lots
            bool.filter { f ->
                f.term { t -> t.field("status").value("active") }
            }

            // Full-text query (if provided)
            if (!query.q.isNullOrBlank()) {
                bool.must { m ->
                    m.multiMatch { mm ->
                        mm.query(query.q)
                            .fields(
                                "title^3",
                                "title.en^2",
                                "title.de^2",
                                "title.nl^2",
                                "title.fr^2",
                                "description"
                            )
                            .fuzziness("AUTO")
                            .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                    }
                }
            }

            // Category filter
            if (!query.categoryId.isNullOrBlank()) {
                bool.filter { f ->
                    f.term { t -> t.field("categoryId").value(query.categoryId) }
                }
            }

            // Country filter
            if (!query.country.isNullOrBlank()) {
                bool.filter { f ->
                    f.term { t -> t.field("country").value(query.country) }
                }
            }

            // Reserve status filter
            if (!query.reserveStatus.isNullOrBlank()) {
                bool.filter { f ->
                    f.term { t -> t.field("reserveStatus").value(query.reserveStatus) }
                }
            }

            // Price range filter
            if (query.minPrice != null || query.maxPrice != null) {
                bool.filter { f ->
                    f.range { r ->
                        r.number { nr ->
                            nr.field("currentBid").apply {
                                if (query.minPrice != null) gte(query.minPrice.toDouble())
                                if (query.maxPrice != null) lte(query.maxPrice.toDouble())
                            }
                        }
                    }
                }
            }

            // Geo-distance filter
            if (query.lat != null && query.lng != null && query.radiusKm != null) {
                bool.filter { f ->
                    f.geoDistance { geo ->
                        geo.field("location")
                            .location { loc -> loc.latlon { ll -> ll.lat(query.lat).lon(query.lng) } }
                            .distance("${query.radiusKm}km")
                            .distanceType(GeoDistanceType.Arc)
                    }
                }
            }

            bool
        }.build()
    }

    /**
     * Applies sorting to the search request builder based on the [SortOption].
     */
    private fun applySorting(
        builder: SearchRequest.Builder,
        sort: SortOption,
        lat: Double?,
        lng: Double?
    ): SearchRequest.Builder {
        return when (sort) {
            SortOption.CLOSING_SOONEST -> builder.sort { s ->
                s.field { f -> f.field("auctionEndTime").order(SortOrder.Asc).missing(FieldValue.of("_last")).unmappedType(FieldType.Date) }
            }
            SortOption.PRICE_ASC -> builder.sort { s ->
                s.field { f -> f.field("currentBid").order(SortOrder.Asc).missing(FieldValue.of("_last")).unmappedType(FieldType.Float) }
            }
            SortOption.PRICE_DESC -> builder.sort { s ->
                s.field { f -> f.field("currentBid").order(SortOrder.Desc).missing(FieldValue.of("_last")).unmappedType(FieldType.Float) }
            }
            SortOption.NEWEST -> builder.sort { s ->
                s.field { f -> f.field("createdAt").order(SortOrder.Desc).missing(FieldValue.of("_last")).unmappedType(FieldType.Date) }
            }
            SortOption.BID_COUNT_DESC -> builder.sort { s ->
                s.field { f -> f.field("bidCount").order(SortOrder.Desc).missing(FieldValue.of("_last")).unmappedType(FieldType.Long) }
            }
            SortOption.RELEVANCE -> builder // Use default _score sorting
        }
    }

    // -------------------------------------------------------------------------
    // Result extraction
    // -------------------------------------------------------------------------

    /**
     * Extracts term aggregation buckets from the search response.
     */
    private fun extractTermBuckets(response: SearchResponse<*>, aggName: String): List<BucketCount> {
        val agg = response.aggregations()[aggName] ?: return emptyList()
        val termsAgg = agg.sterms()
        return termsAgg.buckets().array().map { bucket ->
            BucketCount(
                name = bucket.key()._toJsonString().trim('"'),
                count = bucket.docCount()
            )
        }
    }

    /**
     * Extracts range aggregation buckets from the search response.
     */
    private fun extractRangeBuckets(response: SearchResponse<*>, aggName: String): List<BucketCount> {
        val agg = response.aggregations()[aggName] ?: return emptyList()
        val rangeAgg = agg.range()
        return rangeAgg.buckets().array().map { bucket ->
            BucketCount(
                name = bucket.key() ?: "unknown",
                count = bucket.docCount()
            )
        }
    }

    /**
     * Maps a [LotDocument] to a [LotSearchItem], extracting the primary image
     * thumbnail URL for display in search results.
     */
    private fun LotDocument.toSearchItem(): LotSearchItem {
        val primaryImage = images.firstOrNull { it.isPrimary }
            ?: images.firstOrNull()
        return LotSearchItem(
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
            thumbnailUrl = primaryImage?.thumbnailUrl ?: primaryImage?.url,
            lotNumber = lotNumber,
            currency = currency
        )
    }
}
