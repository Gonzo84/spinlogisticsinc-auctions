package eu.auctionplatform.catalog.infrastructure.persistence.entity

import eu.auctionplatform.catalog.domain.model.LotImage
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

/**
 * JPA entity mapped to the `app.lot_images` table.
 *
 * Stores image references for lots, including the CDN URLs for both
 * full-resolution and thumbnail variants.
 */
@Entity
@Table(name = "lot_images", schema = "app")
class LotImageEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "lot_id", nullable = false, updatable = false)
    var lotId: UUID = UUID.randomUUID(),

    @Column(name = "image_url", nullable = false)
    var imageUrl: String = "",

    @Column(name = "thumbnail_url")
    var thumbnailUrl: String? = null,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false
) {

    /** Converts this entity to the domain model. */
    fun toDomain(): LotImage = LotImage(
        id = id,
        lotId = lotId,
        imageUrl = imageUrl,
        thumbnailUrl = thumbnailUrl,
        displayOrder = displayOrder,
        isPrimary = isPrimary
    )

    companion object {

        /** Creates an entity from the domain model. */
        fun fromDomain(image: LotImage): LotImageEntity = LotImageEntity(
            id = image.id,
            lotId = image.lotId,
            imageUrl = image.imageUrl,
            thumbnailUrl = image.thumbnailUrl,
            displayOrder = image.displayOrder,
            isPrimary = image.isPrimary
        )
    }
}
