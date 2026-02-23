package eu.auctionplatform.search.api.v1.resource

import eu.auctionplatform.commons.dto.ApiResponse
import eu.auctionplatform.search.api.v1.dto.AggregationResponse
import eu.auctionplatform.search.api.v1.dto.SearchResultResponse
import eu.auctionplatform.search.api.v1.dto.SuggestionResponse
import eu.auctionplatform.search.api.v1.dto.toResponse
import eu.auctionplatform.search.application.SearchQuery
import eu.auctionplatform.search.application.SearchService
import eu.auctionplatform.search.application.SortOption
import jakarta.annotation.security.PermitAll
import jakarta.inject.Inject
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import java.math.BigDecimal

// =============================================================================
// Search Resource – REST API v1
// =============================================================================

/**
 * JAX-RS resource exposing the lot search, autocomplete, aggregation, and
 * proximity APIs.
 *
 * All endpoints are publicly accessible (`@PermitAll`) since search is a
 * read-only operation that does not require authentication. Rate limiting
 * and abuse prevention are handled at the API gateway layer.
 *
 * Base path: `/api/v1/search`
 */
@Path("/api/v1/search")
@Produces(MediaType.APPLICATION_JSON)
class SearchResource {

    @Inject
    lateinit var searchService: SearchService

    companion object {
        private val LOG: Logger = Logger.getLogger(SearchResource::class.java)
    }

    // -------------------------------------------------------------------------
    // GET /lots – Full-text search with filters
    // -------------------------------------------------------------------------

    /**
     * Searches for active lots using full-text query, faceted filters, and
     * optional geo-distance constraints.
     *
     * **GET /api/v1/search/lots**
     *
     * Query parameters:
     * - `q`             – Free-text search query (optional).
     * - `category`      – Category ID filter (optional).
     * - `country`       – ISO 3166-1 alpha-2 country code filter (optional).
     * - `lat` / `lng`   – Latitude / longitude for geo-distance filter (optional).
     * - `radius`        – Search radius in kilometres (requires lat/lng, optional).
     * - `minPrice`      – Minimum current bid filter (optional).
     * - `maxPrice`      – Maximum current bid filter (optional).
     * - `reserveStatus` – Reserve status filter: no_reserve, reserve_met, reserve_not_met (optional).
     * - `sort`          – Sort order: RELEVANCE, CLOSING_SOONEST, PRICE_ASC, PRICE_DESC, NEWEST, BID_COUNT_DESC.
     * - `page`          – Page number, 0-based (default 0).
     * - `size`          – Page size (default 20, max 100).
     *
     * @return 200 OK with [SearchResultResponse] wrapped in [ApiResponse].
     */
    @GET
    @Path("/lots")
    @PermitAll
    fun searchLots(
        @QueryParam("q") q: String?,
        @QueryParam("category") category: String?,
        @QueryParam("country") country: String?,
        @QueryParam("lat") lat: Double?,
        @QueryParam("lng") lng: Double?,
        @QueryParam("radius") radius: Int?,
        @QueryParam("minPrice") minPrice: BigDecimal?,
        @QueryParam("maxPrice") maxPrice: BigDecimal?,
        @QueryParam("reserveStatus") reserveStatus: String?,
        @QueryParam("sort") @DefaultValue("RELEVANCE") sortParam: String,
        @QueryParam("page") @DefaultValue("0") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int
    ): Response {
        LOG.debugf("GET /lots q=[%s] category=[%s] country=[%s] sort=[%s] page=%d size=%d",
            q, category, country, sortParam, page, size)

        val sort = parseSortOption(sortParam)

        val query = SearchQuery(
            q = q,
            categoryId = category,
            country = country,
            lat = lat,
            lng = lng,
            radiusKm = radius,
            minPrice = minPrice,
            maxPrice = maxPrice,
            reserveStatus = reserveStatus,
            sort = sort,
            page = page,
            size = size
        )

        val result = searchService.search(query)
        val responseDto = result.toResponse(page, size)

        return Response.ok(ApiResponse.ok(responseDto)).build()
    }

    // -------------------------------------------------------------------------
    // GET /lots/suggest – Autocomplete
    // -------------------------------------------------------------------------

    /**
     * Provides autocomplete suggestions based on a partial text input.
     *
     * **GET /api/v1/search/lots/suggest**
     *
     * Query parameters:
     * - `q` – The partial text input to complete (required).
     *
     * @return 200 OK with [SuggestionResponse] wrapped in [ApiResponse].
     */
    @GET
    @Path("/lots/suggest")
    @PermitAll
    fun suggestLots(
        @QueryParam("q") q: String?
    ): Response {
        LOG.debugf("GET /lots/suggest q=[%s]", q)

        if (q.isNullOrBlank()) {
            val empty = SuggestionResponse(suggestions = emptyList())
            return Response.ok(ApiResponse.ok(empty)).build()
        }

        val suggestions = searchService.suggest(prefix = q, limit = 10)
        val responseDto = suggestions.toResponse()

        return Response.ok(ApiResponse.ok(responseDto)).build()
    }

    // -------------------------------------------------------------------------
    // GET /lots/aggregations – Faceted counts
    // -------------------------------------------------------------------------

    /**
     * Returns faceted aggregation counts for categories, countries, and price
     * ranges, optionally scoped by the same filters as the search endpoint.
     *
     * **GET /api/v1/search/lots/aggregations**
     *
     * Query parameters:
     * - `q`             – Free-text query (optional, scopes aggregations).
     * - `category`      – Category ID filter (optional).
     * - `country`       – Country filter (optional).
     * - `minPrice`      – Minimum price filter (optional).
     * - `maxPrice`      – Maximum price filter (optional).
     * - `reserveStatus` – Reserve status filter (optional).
     *
     * @return 200 OK with [AggregationResponse] wrapped in [ApiResponse].
     */
    @GET
    @Path("/lots/aggregations")
    @PermitAll
    fun getAggregations(
        @QueryParam("q") q: String?,
        @QueryParam("category") category: String?,
        @QueryParam("country") country: String?,
        @QueryParam("minPrice") minPrice: BigDecimal?,
        @QueryParam("maxPrice") maxPrice: BigDecimal?,
        @QueryParam("reserveStatus") reserveStatus: String?
    ): Response {
        LOG.debugf("GET /lots/aggregations q=[%s] category=[%s] country=[%s]", q, category, country)

        val query = SearchQuery(
            q = q,
            categoryId = category,
            country = country,
            minPrice = minPrice,
            maxPrice = maxPrice,
            reserveStatus = reserveStatus
        )

        val aggResult = searchService.aggregations(query)
        val responseDto = aggResult.toResponse()

        return Response.ok(ApiResponse.ok(responseDto)).build()
    }

    // -------------------------------------------------------------------------
    // GET /lots/nearby – Proximity search
    // -------------------------------------------------------------------------

    /**
     * Searches for lots near a geographic point, sorted by distance.
     *
     * **GET /api/v1/search/lots/nearby**
     *
     * Query parameters:
     * - `lat`      – Latitude of the center point (required).
     * - `lng`      – Longitude of the center point (required).
     * - `radius`   – Search radius in kilometres (required).
     * - `category` – Optional category filter.
     *
     * @return 200 OK with [SearchResultResponse] wrapped in [ApiResponse].
     * @return 400 Bad Request if lat, lng, or radius are missing.
     */
    @GET
    @Path("/lots/nearby")
    @PermitAll
    fun nearbyLots(
        @QueryParam("lat") lat: Double?,
        @QueryParam("lng") lng: Double?,
        @QueryParam("radius") radius: Int?,
        @QueryParam("category") category: String?
    ): Response {
        LOG.debugf("GET /lots/nearby lat=%s lng=%s radius=%s category=[%s]", lat, lng, radius, category)

        if (lat == null || lng == null || radius == null) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(mapOf(
                    "error" to "MISSING_PARAMETERS",
                    "message" to "Parameters 'lat', 'lng', and 'radius' are required for nearby search"
                ))
                .build()
        }

        val result = searchService.nearby(
            lat = lat,
            lng = lng,
            radiusKm = radius,
            category = category
        )
        val responseDto = result.toResponse(page = 0, size = result.items.size)

        return Response.ok(ApiResponse.ok(responseDto)).build()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Parses a sort parameter string into a [SortOption] enum value.
     * Defaults to [SortOption.RELEVANCE] for unrecognised values.
     */
    private fun parseSortOption(value: String): SortOption {
        return try {
            SortOption.valueOf(value.uppercase())
        } catch (ex: IllegalArgumentException) {
            LOG.warnf("Unrecognised sort option [%s], defaulting to RELEVANCE", value)
            SortOption.RELEVANCE
        }
    }
}
