package eu.auctionplatform.catalog.application.service

import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.catalog.api.dto.CategoryTreeNode
import eu.auctionplatform.catalog.api.dto.toTreeNode
import eu.auctionplatform.catalog.domain.model.Category
import eu.auctionplatform.catalog.domain.model.Lot
import eu.auctionplatform.catalog.infrastructure.persistence.repository.CategoryRepository
import eu.auctionplatform.catalog.infrastructure.persistence.repository.LotRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.util.UUID

/**
 * Application service for category-related operations.
 *
 * Provides methods for retrieving the category hierarchy and querying
 * lots within specific categories. Categories are seeded at migration
 * time and are considered relatively static reference data.
 */
@ApplicationScoped
class CategoryService {

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var lotRepository: LotRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(CategoryService::class.java)
    }

    /**
     * Builds and returns the complete category tree.
     *
     * Fetches all active categories in a single query and assembles them
     * into a hierarchical tree structure in memory. Root categories (level 0)
     * form the top-level nodes, with children nested recursively.
     *
     * @return List of root-level [CategoryTreeNode]s with nested children.
     */
    fun getCategoryTree(): List<CategoryTreeNode> {
        val allCategories = categoryRepository.findAllActive().map { it.toDomain() }

        if (allCategories.isEmpty()) {
            return emptyList()
        }

        // Group categories by parent ID for efficient tree construction
        val childrenByParent: Map<UUID?, List<Category>> = allCategories.groupBy { it.parentId }

        // Recursively build the tree starting from root categories
        fun buildChildren(parentId: UUID?): List<CategoryTreeNode> {
            return childrenByParent[parentId]
                ?.sortedBy { it.sortOrder }
                ?.map { category ->
                    category.toTreeNode(
                        children = buildChildren(category.id)
                    )
                }
                ?: emptyList()
        }

        return buildChildren(null)
    }

    /**
     * Returns a single category by its identifier.
     *
     * @param categoryId The category identifier.
     * @return The [Category] domain model.
     * @throws NotFoundException if the category does not exist.
     */
    fun getCategoryById(categoryId: UUID): Category {
        val entity = categoryRepository.findById(categoryId)
            ?: throw NotFoundException(
                code = "CATEGORY_NOT_FOUND",
                message = "Category with id '$categoryId' not found."
            )
        return entity.toDomain()
    }

    /**
     * Returns a single category by its URL-friendly slug.
     *
     * @param slug The category slug (e.g. "construction-machinery").
     * @return The [Category] domain model.
     * @throws NotFoundException if the category does not exist.
     */
    fun getCategoryBySlug(slug: String): Category {
        val entity = categoryRepository.findBySlug(slug)
            ?: throw NotFoundException(
                code = "CATEGORY_NOT_FOUND",
                message = "Category with slug '$slug' not found."
            )
        return entity.toDomain()
    }

    /**
     * Returns lots within a specific category, with pagination.
     *
     * @param categoryId The category identifier.
     * @param page       The page number (0-based).
     * @param pageSize   The number of items per page.
     * @return A pair of the lot list and the total count.
     * @throws NotFoundException if the category does not exist.
     */
    fun getLotsInCategory(categoryId: UUID, page: Int = 0, pageSize: Int = 20): Pair<List<Lot>, Long> {
        // Validate category exists
        categoryRepository.findById(categoryId)
            ?: throw NotFoundException(
                code = "CATEGORY_NOT_FOUND",
                message = "Category with id '$categoryId' not found."
            )

        val lots = lotRepository.findByCategoryId(categoryId, page, pageSize).map { it.toDomain() }
        val total = lotRepository.countByCategoryId(categoryId)

        return Pair(lots, total)
    }

    /**
     * Returns child categories for a given parent.
     *
     * @param parentId The parent category identifier.
     * @return List of child [Category] domain models.
     */
    fun getChildCategories(parentId: UUID): List<Category> {
        return categoryRepository.findByParentId(parentId).map { it.toDomain() }
    }
}
