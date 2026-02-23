package eu.auctionplatform.catalog.infrastructure.persistence.entity

import eu.auctionplatform.catalog.domain.model.Lot
import eu.auctionplatform.catalog.domain.model.LotStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * JPA entity mapped to the `app.lots` table.
 *
 * The [specifications] field is stored as JSONB in PostgreSQL, enabling
 * flexible category-specific attributes without schema changes.
 */
@Entity
@Table(name = "lots", schema = "app")
class LotEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "seller_id", nullable = false, updatable = false)
    var sellerId: UUID = UUID.randomUUID(),

    @Column(name = "brand", nullable = false)
    var brand: String = "",

    @Column(name = "title", nullable = false)
    var title: String = "",

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String = "",

    @Column(name = "category_id", nullable = false)
    var categoryId: UUID = UUID.randomUUID(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specifications", columnDefinition = "jsonb")
    var specifications: Map<String, Any> = emptyMap(),

    @Column(name = "location_lat")
    var locationLat: Double? = null,

    @Column(name = "location_lng")
    var locationLng: Double? = null,

    @Column(name = "location_address")
    var locationAddress: String? = null,

    @Column(name = "location_country", nullable = false)
    var locationCountry: String = "",

    @Column(name = "location_city", nullable = false)
    var locationCity: String = "",

    @Column(name = "reserve_price", precision = 12, scale = 2)
    var reservePrice: BigDecimal? = null,

    @Column(name = "starting_bid", nullable = false, precision = 12, scale = 2)
    var startingBid: BigDecimal = BigDecimal.ONE,

    @Column(name = "auction_id")
    var auctionId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: LotStatus = LotStatus.DRAFT,

    @Column(name = "co2_avoided_kg")
    var co2AvoidedKg: Double? = null,

    @Column(name = "pickup_info", columnDefinition = "TEXT")
    var pickupInfo: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {

    /** Converts this entity to the domain model. */
    fun toDomain(): Lot = Lot(
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
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {

        /** Creates an entity from the domain model. */
        fun fromDomain(lot: Lot): LotEntity = LotEntity(
            id = lot.id,
            sellerId = lot.sellerId,
            brand = lot.brand,
            title = lot.title,
            description = lot.description,
            categoryId = lot.categoryId,
            specifications = lot.specifications,
            locationLat = lot.locationLat,
            locationLng = lot.locationLng,
            locationAddress = lot.locationAddress,
            locationCountry = lot.locationCountry,
            locationCity = lot.locationCity,
            reservePrice = lot.reservePrice,
            startingBid = lot.startingBid,
            auctionId = lot.auctionId,
            status = lot.status,
            co2AvoidedKg = lot.co2AvoidedKg,
            pickupInfo = lot.pickupInfo,
            createdAt = lot.createdAt,
            updatedAt = lot.updatedAt
        )
    }
}
