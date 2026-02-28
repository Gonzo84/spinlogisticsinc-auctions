package eu.auctionplatform.catalog

import eu.auctionplatform.catalog.domain.model.Category
import eu.auctionplatform.catalog.domain.model.Lot
import eu.auctionplatform.catalog.domain.model.LotStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object TestFixtures {
    val SELLER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    val CATEGORY_ID: UUID = UUID.fromString("00000000-0000-0000-0000-100000000001")

    fun createLot(
        id: UUID = UUID.randomUUID(),
        sellerId: UUID = SELLER_ID,
        title: String = "Industrial CNC Machine",
        status: LotStatus = LotStatus.DRAFT,
        startingBid: BigDecimal = BigDecimal("1000.00"),
        categoryId: UUID = CATEGORY_ID
    ) = Lot(
        id = id,
        sellerId = sellerId,
        brand = "troostwijk",
        title = title,
        description = "High-precision CNC milling machine, 2019 model, excellent condition.",
        categoryId = categoryId,
        specifications = mapOf("manufacturer" to "Haas", "year" to 2019, "hours" to 3200),
        locationCountry = "NL",
        locationCity = "Amsterdam",
        locationAddress = "Industrieweg 42",
        startingBid = startingBid,
        status = status,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    fun createCategory(
        id: UUID = CATEGORY_ID,
        name: String = "Construction Machinery",
        slug: String = "construction-machinery",
        parentId: UUID? = null,
        level: Int = 0,
        sortOrder: Int = 1
    ) = Category(
        id = id,
        parentId = parentId,
        name = name,
        slug = slug,
        level = level,
        sortOrder = sortOrder,
        active = true
    )

    fun createLotRequest(
        title: String = "Industrial CNC Machine",
        startingBid: BigDecimal = BigDecimal("1000.00"),
        categoryId: UUID = CATEGORY_ID
    ) = mapOf(
        "sellerId" to SELLER_ID.toString(),
        "brand" to "troostwijk",
        "title" to title,
        "description" to "High-precision CNC milling machine, 2019 model.",
        "categoryId" to categoryId.toString(),
        "locationCountry" to "NL",
        "locationCity" to "Amsterdam",
        "startingBid" to startingBid.toString()
    )
}
