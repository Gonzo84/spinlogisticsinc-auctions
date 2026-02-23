package eu.auctionplatform.catalog.domain.model

import java.util.UUID

/**
 * Immutable domain model representing a hierarchical category in the catalog.
 *
 * Categories form a tree structure via [parentId] references. Top-level
 * categories have a `null` [parentId]. The [level] indicates depth in the
 * tree (0 = root, 1 = child, etc.).
 *
 * The [slug] is a URL-friendly version of the category name used in
 * routing and SEO-optimised URLs.
 *
 * @property id        Unique identifier (UUIDv7).
 * @property parentId  Reference to the parent category (null for root categories).
 * @property name      Human-readable category name (e.g. "Construction Machinery").
 * @property slug      URL-friendly identifier (e.g. "construction-machinery").
 * @property icon      Optional icon identifier or URL for UI rendering.
 * @property level     Depth level in the hierarchy (0 = root).
 * @property sortOrder Display order among siblings at the same level.
 * @property active    Whether this category is visible and accepts new lots.
 */
data class Category(
    val id: UUID,
    val parentId: UUID? = null,
    val name: String,
    val slug: String,
    val icon: String? = null,
    val level: Int,
    val sortOrder: Int,
    val active: Boolean = true
) {

    /** Returns `true` if this is a root-level category. */
    fun isRoot(): Boolean = parentId == null
}
