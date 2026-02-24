package eu.auctionplatform.catalog.infrastructure.persistence.repository

import eu.auctionplatform.catalog.infrastructure.persistence.entity.CategoryEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * Repository for [CategoryEntity] persistence operations.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 */
@ApplicationScoped
class CategoryRepository : PanacheRepositoryBase<CategoryEntity, UUID> {

    /**
     * Returns all active root-level categories, ordered by sort position.
     *
     * @return List of root categories (parentId is null).
     */
    fun findRootCategories(): List<CategoryEntity> =
        list("parentId is null and active = true order by sortOrder asc")

    /**
     * Returns all active child categories for a given parent, ordered by sort position.
     *
     * @param parentId The parent category identifier.
     * @return List of child categories.
     */
    fun findByParentId(parentId: UUID): List<CategoryEntity> =
        list("parentId = ?1 and active = true order by sortOrder asc", parentId)

    /**
     * Finds a category by its URL-friendly slug.
     *
     * @param slug The category slug.
     * @return The matching category entity, or `null` if not found.
     */
    fun findBySlug(slug: String): CategoryEntity? =
        find("slug", slug).firstResult()

    /**
     * Returns all active categories at a given hierarchy level.
     *
     * @param level The depth level (0 = root).
     * @return List of categories at the specified level.
     */
    fun findByLevel(level: Int): List<CategoryEntity> =
        list("level = ?1 and active = true order by sortOrder asc", level)

    /**
     * Returns all active categories, ordered by level and sort position.
     * Useful for building the complete category tree in a single query.
     *
     * @return List of all active categories.
     */
    fun findAllActive(): List<CategoryEntity> =
        list("active = true order by level asc, sortOrder asc")
}
