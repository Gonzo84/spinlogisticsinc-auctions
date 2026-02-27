package eu.auctionplatform.catalog.api.dto

import eu.auctionplatform.catalog.domain.model.AuctionEvent
import eu.auctionplatform.catalog.domain.model.Category
import eu.auctionplatform.catalog.domain.model.Lot
import eu.auctionplatform.catalog.domain.model.LotImage

/**
 * Extension functions for converting domain models to response DTOs.
 *
 * These mappers live in the API layer to keep the domain model free
 * of serialisation concerns.
 */

/** Converts a [Lot] domain model to a [LotResponse] DTO. */
fun Lot.toResponse(images: List<LotImage> = emptyList()): LotResponse = LotResponse(
    id = id,
    sellerId = sellerId,
    brand = brand,
    title = title,
    description = description,
    categoryId = categoryId,
    specifications = specifications,
    locationLat = locationLat,
    locationLng = locationLng,
    locationAddress = locationAddress,
    locationCountry = locationCountry,
    locationCity = locationCity,
    reservePrice = reservePrice,
    startingBid = startingBid,
    auctionId = auctionId,
    status = status,
    co2AvoidedKg = co2AvoidedKg,
    pickupInfo = pickupInfo,
    images = images.map { it.toResponse() },
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** Converts a [Lot] domain model to a [LotSummaryResponse] DTO. */
fun Lot.toSummaryResponse(primaryImageUrl: String? = null): LotSummaryResponse = LotSummaryResponse(
    id = id,
    sellerId = sellerId,
    categoryId = categoryId,
    title = title,
    brand = brand,
    locationCountry = locationCountry,
    locationCity = locationCity,
    startingBid = startingBid,
    status = status,
    primaryImageUrl = primaryImageUrl,
    co2AvoidedKg = co2AvoidedKg,
    createdAt = createdAt
)

/** Converts a [LotImage] domain model to a [LotImageResponse] DTO. */
fun LotImage.toResponse(): LotImageResponse = LotImageResponse(
    id = id,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    displayOrder = displayOrder,
    isPrimary = isPrimary
)

/** Converts a [Category] domain model to a [CategoryResponse] DTO. */
fun Category.toResponse(): CategoryResponse = CategoryResponse(
    id = id,
    parentId = parentId,
    name = name,
    slug = slug,
    icon = icon,
    level = level,
    sortOrder = sortOrder,
    active = active
)

/** Converts a [Category] domain model to a [CategoryTreeNode] DTO. */
fun Category.toTreeNode(children: List<CategoryTreeNode> = emptyList()): CategoryTreeNode = CategoryTreeNode(
    id = id,
    name = name,
    slug = slug,
    icon = icon,
    level = level,
    children = children
)

/** Converts an [AuctionEvent] domain model to an [AuctionEventResponse] DTO. */
fun AuctionEvent.toResponse(): AuctionEventResponse = AuctionEventResponse(
    id = id,
    title = title,
    brand = brand,
    startDate = startDate,
    endDate = endDate,
    country = country,
    status = status,
    buyerPremiumPercent = buyerPremiumPercent,
    totalLots = totalLots
)
