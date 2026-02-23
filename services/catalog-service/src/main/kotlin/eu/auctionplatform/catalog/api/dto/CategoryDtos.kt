package eu.auctionplatform.catalog.api.dto

import java.util.UUID

// =============================================================================
// Response DTOs
// =============================================================================

/**
 * Response representation of a category.
 */
data class CategoryResponse(
    val id: UUID,
    val parentId: UUID?,
    val name: String,
    val slug: String,
    val icon: String?,
    val level: Int,
    val sortOrder: Int,
    val active: Boolean
)

/**
 * Hierarchical category tree node for building nested category structures.
 */
data class CategoryTreeNode(
    val id: UUID,
    val name: String,
    val slug: String,
    val icon: String?,
    val level: Int,
    val children: List<CategoryTreeNode> = emptyList()
)
