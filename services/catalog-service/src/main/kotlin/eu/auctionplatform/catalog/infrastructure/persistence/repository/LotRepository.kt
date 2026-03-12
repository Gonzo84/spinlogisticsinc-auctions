package eu.auctionplatform.catalog.infrastructure.persistence.repository

import eu.auctionplatform.catalog.domain.model.LotStatus
import eu.auctionplatform.catalog.infrastructure.persistence.entity.LotEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * Repository for [LotEntity] persistence operations.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 */
@ApplicationScoped
class LotRepository : PanacheRepositoryBase<LotEntity, UUID> {

    companion object {
        /** Allowed sort field names (whitelist to prevent SQL injection). */
        private val ALLOWED_SORT_FIELDS = setOf("createdAt", "title", "startingBid", "updatedAt")
    }

    /**
     * Builds a [Sort] from optional sort parameters.
     *
     * Only allows whitelisted field names to prevent SQL injection.
     * Defaults to `createdAt` descending when no valid sort is provided.
     *
     * @param sortBy  The field name to sort by (must be in [ALLOWED_SORT_FIELDS]).
     * @param sortDir The sort direction (`"asc"` or `"desc"`). Defaults to descending.
     * @return A Panache [Sort] instance.
     */
    fun buildSort(sortBy: String?, sortDir: String?): Sort {
        val field = if (sortBy != null && sortBy in ALLOWED_SORT_FIELDS) sortBy else "createdAt"
        val direction = if (sortDir?.lowercase() == "asc") Sort.Direction.Ascending else Sort.Direction.Descending
        return Sort.by(field, direction)
    }

    /**
     * Returns lots for a given seller, ordered by creation time descending.
     *
     * @param sellerId The seller's user identifier.
     * @return List of lot entities owned by the seller.
     */
    fun findBySellerId(sellerId: UUID): List<LotEntity> =
        list("sellerId = ?1 order by createdAt desc", sellerId)

    /**
     * Returns lots assigned to a specific auction event.
     *
     * @param auctionId The auction event identifier.
     * @return List of lot entities in the auction.
     */
    fun findByAuctionId(auctionId: UUID): List<LotEntity> =
        list("auctionId = ?1 order by title asc", auctionId)

    /**
     * Returns lots in a specific category with pagination and sorting.
     *
     * @param categoryId The category identifier.
     * @param sort       The sort order to apply.
     * @param page       The page to retrieve (0-based).
     * @param pageSize   The number of items per page.
     * @return List of lot entities in the category.
     */
    fun findByCategoryId(categoryId: UUID, sort: Sort, page: Int, pageSize: Int): List<LotEntity> =
        find("categoryId = ?1 and status != ?2", sort, categoryId, LotStatus.WITHDRAWN)
            .page(Page.of(page, pageSize))
            .list()

    /**
     * Counts lots in a specific category (excluding withdrawn lots).
     *
     * @param categoryId The category identifier.
     * @return Total number of non-withdrawn lots in the category.
     */
    fun countByCategoryId(categoryId: UUID): Long =
        count("categoryId = ?1 and status != ?2", categoryId, LotStatus.WITHDRAWN)

    /**
     * Returns lots with a given status, paginated and sorted.
     *
     * @param status   The lot status to filter by.
     * @param sort     The sort order to apply.
     * @param page     The page to retrieve (0-based).
     * @param pageSize The number of items per page.
     * @return List of lot entities with the given status.
     */
    fun findByStatus(status: LotStatus, sort: Sort, page: Int, pageSize: Int): List<LotEntity> =
        find("status", sort, status)
            .page(Page.of(page, pageSize))
            .list()

    /**
     * Counts lots with a given status.
     */
    fun countByStatus(status: LotStatus): Long =
        count("status", status)

    /**
     * Returns lots matching a brand, with pagination and sorting.
     *
     * @param brand    The brand/tenant code.
     * @param sort     The sort order to apply.
     * @param page     The page to retrieve (0-based).
     * @param pageSize The number of items per page.
     * @return List of lot entities for the brand.
     */
    fun findByBrand(brand: String, sort: Sort, page: Int, pageSize: Int): List<LotEntity> =
        find("brand = ?1 and status != ?2", sort, brand, LotStatus.WITHDRAWN)
            .page(Page.of(page, pageSize))
            .list()

    /**
     * Counts lots for a given brand (excluding withdrawn).
     */
    fun countByBrand(brand: String): Long =
        count("brand = ?1 and status != ?2", brand, LotStatus.WITHDRAWN)

    /**
     * Returns lots matching a country and optional status filter, with pagination and sorting.
     *
     * @param country  ISO 3166-1 alpha-2 country code.
     * @param status   Optional status filter (null = all non-withdrawn).
     * @param sort     The sort order to apply.
     * @param page     The page to retrieve (0-based).
     * @param pageSize The number of items per page.
     * @return List of lot entities matching the criteria.
     */
    fun findByCountry(country: String, status: LotStatus?, sort: Sort, page: Int, pageSize: Int): List<LotEntity> {
        return if (status != null) {
            find("locationCountry = ?1 and status = ?2", sort, country, status)
                .page(Page.of(page, pageSize))
                .list()
        } else {
            find("locationCountry = ?1 and status != ?2", sort, country, LotStatus.WITHDRAWN)
                .page(Page.of(page, pageSize))
                .list()
        }
    }

    /**
     * Counts lots matching a country (excluding withdrawn lots).
     *
     * @param country ISO 3166-1 alpha-2 country code.
     * @return Total number of non-withdrawn lots in the country.
     */
    fun countByCountry(country: String): Long =
        count("locationCountry = ?1 and status != ?2", country, LotStatus.WITHDRAWN)

    /**
     * Returns lots for a given seller with pagination and sorting.
     *
     * @param sellerId The seller's user identifier.
     * @param sort     The sort order to apply.
     * @param page     The page to retrieve (0-based).
     * @param pageSize The number of items per page.
     * @return List of lot entities owned by the seller.
     */
    fun findBySellerId(sellerId: UUID, sort: Sort, page: Int, pageSize: Int): List<LotEntity> =
        find("sellerId = ?1", sort, sellerId)
            .page(Page.of(page, pageSize))
            .list()

    /**
     * Counts lots for a given seller.
     *
     * @param sellerId The seller's user identifier.
     * @return Total number of lots owned by the seller.
     */
    fun countBySellerId(sellerId: UUID): Long =
        count("sellerId", sellerId)

    /**
     * Returns lots assigned to a specific auction event with pagination and sorting.
     *
     * @param auctionId The auction event identifier.
     * @param sort      The sort order to apply.
     * @param page      The page to retrieve (0-based).
     * @param pageSize  The number of items per page.
     * @return List of lot entities in the auction.
     */
    fun findByAuctionId(auctionId: UUID, sort: Sort, page: Int, pageSize: Int): List<LotEntity> =
        find("auctionId = ?1", sort, auctionId)
            .page(Page.of(page, pageSize))
            .list()

    /**
     * Counts lots assigned to a specific auction event.
     *
     * @param auctionId The auction event identifier.
     * @return Total number of lots in the auction.
     */
    fun countByAuctionId(auctionId: UUID): Long =
        count("auctionId", auctionId)

    /**
     * Counts non-withdrawn lots grouped by category, returning a map of
     * category slug to lot count. Joins with the categories table to
     * resolve slugs.
     *
     * @return Map of category slug to lot count.
     */
    fun countsByCategorySlug(): Map<String, Long> {
        @Suppress("UNCHECKED_CAST")
        val results = getEntityManager()
            .createNativeQuery(
                """
                SELECT c.slug, COUNT(l.id) as cnt
                FROM app.lots l
                JOIN app.categories c ON l.category_id = c.id
                WHERE l.status != 'WITHDRAWN'
                GROUP BY c.slug
                """.trimIndent()
            )
            .resultList as List<Array<Any>>

        return results.associate { row ->
            (row[0] as String) to (row[1] as Number).toLong()
        }
    }

    /**
     * Full-text search on title and description (case-insensitive) with pagination and sorting.
     *
     * @param searchTerm The search keyword.
     * @param sort       The sort order to apply.
     * @param page       The page to retrieve (0-based).
     * @param pageSize   The number of items per page.
     * @return List of matching lot entities.
     */
    fun findBySearch(searchTerm: String, sort: Sort, page: Int, pageSize: Int): List<LotEntity> {
        val pattern = "%${searchTerm.lowercase()}%"
        return find(
            "status != ?1 and (lower(title) like ?2 or lower(description) like ?2)",
            sort,
            LotStatus.WITHDRAWN,
            pattern
        )
            .page(Page.of(page, pageSize))
            .list()
    }

    /**
     * Counts lots matching a text search on title and description.
     *
     * @param searchTerm The search keyword.
     * @return Total number of matching lots.
     */
    fun countBySearch(searchTerm: String): Long {
        val pattern = "%${searchTerm.lowercase()}%"
        return count(
            "status != ?1 and (lower(title) like ?2 or lower(description) like ?2)",
            LotStatus.WITHDRAWN,
            pattern
        )
    }

    /**
     * Dynamic query builder that supports combining multiple filter criteria.
     *
     * Builds a JPQL WHERE clause from all non-null filter parameters, allowing
     * queries like "lots for seller X with status Y matching search Z".
     *
     * @param sellerId   Optional seller filter.
     * @param status     Optional status filter.
     * @param search     Optional text search on title/description.
     * @param auctionId  Optional auction filter.
     * @param categoryId Optional category filter.
     * @param brand      Optional brand filter.
     * @param country    Optional country filter.
     * @param sort       The sort order to apply.
     * @param page       The page to retrieve (0-based).
     * @param pageSize   The number of items per page.
     * @return List of matching lot entities.
     */
    fun findByFilters(
        sellerId: UUID?,
        status: LotStatus?,
        search: String?,
        auctionId: UUID?,
        categoryId: UUID?,
        brand: String?,
        country: String?,
        sort: Sort,
        page: Int,
        pageSize: Int
    ): List<LotEntity> {
        val (query, params) = buildFilterQuery(sellerId, status, search, auctionId, categoryId, brand, country)
        return find(query, sort, *params.toTypedArray())
            .page(Page.of(page, pageSize))
            .list()
    }

    /**
     * Counts lots matching the combined filter criteria.
     */
    fun countByFilters(
        sellerId: UUID?,
        status: LotStatus?,
        search: String?,
        auctionId: UUID?,
        categoryId: UUID?,
        brand: String?,
        country: String?
    ): Long {
        val (query, params) = buildFilterQuery(sellerId, status, search, auctionId, categoryId, brand, country)
        return count(query, *params.toTypedArray())
    }

    /**
     * Builds a JPQL WHERE clause and parameter list from non-null filters.
     */
    private fun buildFilterQuery(
        sellerId: UUID?,
        status: LotStatus?,
        search: String?,
        auctionId: UUID?,
        categoryId: UUID?,
        brand: String?,
        country: String?
    ): Pair<String, List<Any>> {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()
        var paramIndex = 1

        if (sellerId != null) {
            conditions.add("sellerId = ?$paramIndex")
            params.add(sellerId)
            paramIndex++
        }
        if (status != null) {
            conditions.add("status = ?$paramIndex")
            params.add(status)
            paramIndex++
        }
        if (auctionId != null) {
            conditions.add("auctionId = ?$paramIndex")
            params.add(auctionId)
            paramIndex++
        }
        if (categoryId != null) {
            conditions.add("categoryId = ?$paramIndex")
            params.add(categoryId)
            paramIndex++
        }
        if (brand != null) {
            conditions.add("brand = ?$paramIndex")
            params.add(brand)
            paramIndex++
        }
        if (country != null) {
            conditions.add("locationCountry = ?$paramIndex")
            params.add(country)
            paramIndex++
        }
        if (!search.isNullOrBlank()) {
            val pattern = "%${search.lowercase()}%"
            conditions.add("(lower(title) like ?$paramIndex or lower(description) like ?$paramIndex)")
            params.add(pattern)
            paramIndex++
        }

        val query = if (conditions.isEmpty()) {
            "status != ?1".also {
                params.clear()
                params.add(LotStatus.WITHDRAWN)
            }
        } else {
            conditions.joinToString(" and ")
        }

        return Pair(query, params)
    }
}
