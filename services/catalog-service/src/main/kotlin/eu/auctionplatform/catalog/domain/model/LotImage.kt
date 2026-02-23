package eu.auctionplatform.catalog.domain.model

import java.util.UUID

/**
 * Immutable domain model representing an image associated with a lot.
 *
 * Each lot can have multiple images with a defined display order. One image
 * may be marked as [isPrimary] to serve as the lot's thumbnail in listings.
 *
 * @property id            Unique identifier (UUIDv7).
 * @property lotId         The lot this image belongs to.
 * @property imageUrl      Full-resolution image URL (CDN path).
 * @property thumbnailUrl  Resized thumbnail URL (CDN path), null if not yet generated.
 * @property displayOrder  Ordering position in the lot's image gallery (0-based).
 * @property isPrimary     Whether this is the lot's primary / hero image.
 */
data class LotImage(
    val id: UUID,
    val lotId: UUID,
    val imageUrl: String,
    val thumbnailUrl: String? = null,
    val displayOrder: Int,
    val isPrimary: Boolean = false
)
