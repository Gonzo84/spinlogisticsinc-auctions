package eu.auctionplatform.catalog.infrastructure.persistence.entity

import eu.auctionplatform.catalog.domain.model.Category
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

/**
 * JPA entity mapped to the `app.categories` table.
 *
 * Represents a node in the hierarchical category tree. Parent-child
 * relationships are modelled via the [parentId] foreign key.
 */
@Entity
@Table(name = "categories", schema = "app")
class CategoryEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "parent_id")
    var parentId: UUID? = null,

    @Column(name = "name", nullable = false)
    var name: String = "",

    @Column(name = "slug", nullable = false, unique = true)
    var slug: String = "",

    @Column(name = "icon")
    var icon: String? = null,

    @Column(name = "level", nullable = false)
    var level: Int = 0,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "active", nullable = false)
    var active: Boolean = true
) {

    /** Converts this entity to the domain model. */
    fun toDomain(): Category = Category(
        id = id,
        parentId = parentId,
        name = name,
        slug = slug,
        icon = icon,
        level = level,
        sortOrder = sortOrder,
        active = active
    )

    companion object {

        /** Creates an entity from the domain model. */
        fun fromDomain(category: Category): CategoryEntity = CategoryEntity(
            id = category.id,
            parentId = category.parentId,
            name = category.name,
            slug = category.slug,
            icon = category.icon,
            level = category.level,
            sortOrder = category.sortOrder,
            active = category.active
        )
    }
}
