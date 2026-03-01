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
     * Returns lots in a specific category with pagination.
     *
     * @param categoryId The category identifier.
     * @param page       The page to retrieve (0-based).
     * @param pageSize   The number of items per page.
     * @return List of lot entities in the category.
     */
    fun findByCategoryId(categoryId: UUID, page: Int, pageSize: Int): List<LotEntity> =
        find("categoryId = ?1 and status != ?2", Sort.descending("createdAt"), categoryId, LotStatus.WITHDRAWN)
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
     * Returns lots with a given status, paginated.
     *
     * @param status   The lot status to filter by.
     * @param page     The page to retrieve (0-based).
     * @param pageSize The number of items per page.
     * @return List of lot entities with the given status.
     */
    fun findByStatus(status: LotStatus, page: Int, pageSize: Int): List<LotEntity> =
        find("status", Sort.descending("createdAt"), status)
            .page(Page.of(page, pageSize))
            .list()

    /**
     * Counts lots with a given status.
     */
    fun countByStatus(status: LotStatus): Long =
        count("status", status)

    /**
     * Returns lots matching a brand, with pagination.
     *
     * @param brand    The brand/tenant code.
     * @param page     The page to retrieve (0-based).
     * @param pageSize The number of items per page.
     * @return List of lot entities for the brand.
     */
    fun findByBrand(brand: String, page: Int, pageSize: Int): List<LotEntity> =
        find("brand = ?1 and status != ?2", Sort.descending("createdAt"), brand, LotStatus.WITHDRAWN)
            .page(Page.of(page, pageSize))
            .list()

    /**
     * Counts lots for a given brand (excluding withdrawn).
     */
    fun countByBrand(brand: String): Long =
        count("brand = ?1 and status != ?2", brand, LotStatus.WITHDRAWN)

    /**
     * Returns lots matching a country and optional status filter, with pagination.
     *
     * @param country  ISO 3166-1 alpha-2 country code.
     * @param status   Optional status filter (null = all non-withdrawn).
     * @param page     The page to retrieve (0-based).
     * @param pageSize The number of items per page.
     * @return List of lot entities matching the criteria.
     */
    fun findByCountry(country: String, status: LotStatus?, page: Int, pageSize: Int): List<LotEntity> {
        return if (status != null) {
            find("locationCountry = ?1 and status = ?2", Sort.descending("createdAt"), country, status)
                .page(Page.of(page, pageSize))
                .list()
        } else {
            find("locationCountry = ?1 and status != ?2", Sort.descending("createdAt"), country, LotStatus.WITHDRAWN)
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
     * Returns lots for a given seller with pagination, ordered by creation time descending.
     *
     * @param sellerId The seller's user identifier.
     * @param page     The page to retrieve (0-based).
     * @param pageSize The number of items per page.
     * @return List of lot entities owned by the seller.
     */
    fun findBySellerId(sellerId: UUID, page: Int, pageSize: Int): List<LotEntity> =
        find("sellerId = ?1", Sort.descending("createdAt"), sellerId)
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
     * Returns lots assigned to a specific auction event with pagination.
     *
     * @param auctionId The auction event identifier.
     * @param page      The page to retrieve (0-based).
     * @param pageSize  The number of items per page.
     * @return List of lot entities in the auction.
     */
    fun findByAuctionId(auctionId: UUID, page: Int, pageSize: Int): List<LotEntity> =
        find("auctionId = ?1", Sort.ascending("title"), auctionId)
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
     * Full-text search on title and description (case-insensitive) with pagination.
     *
     * @param searchTerm The search keyword.
     * @param page       The page to retrieve (0-based).
     * @param pageSize   The number of items per page.
     * @return List of matching lot entities.
     */
    fun findBySearch(searchTerm: String, page: Int, pageSize: Int): List<LotEntity> {
        val pattern = "%${searchTerm.lowercase()}%"
        return find(
            "status != ?1 and (lower(title) like ?2 or lower(description) like ?2)",
            Sort.descending("createdAt"),
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
}
