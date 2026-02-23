package eu.auctionplatform.catalog.infrastructure.persistence.entity

import eu.auctionplatform.catalog.domain.model.AuctionEvent
import eu.auctionplatform.catalog.domain.model.AuctionEventStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * JPA entity mapped to the `app.auction_events` table.
 *
 * Represents a time-bounded auction session that groups lots for bidding.
 */
@Entity
@Table(name = "auction_events", schema = "app")
class AuctionEventEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "title", nullable = false)
    var title: String = "",

    @Column(name = "brand", nullable = false)
    var brand: String = "",

    @Column(name = "start_date", nullable = false)
    var startDate: Instant = Instant.now(),

    @Column(name = "end_date", nullable = false)
    var endDate: Instant = Instant.now(),

    @Column(name = "country", nullable = false)
    var country: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: AuctionEventStatus = AuctionEventStatus.DRAFT,

    @Column(name = "buyer_premium_percent", nullable = false, precision = 5, scale = 2)
    var buyerPremiumPercent: BigDecimal = BigDecimal("18"),

    @Column(name = "total_lots", nullable = false)
    var totalLots: Int = 0
) {

    /** Converts this entity to the domain model. */
    fun toDomain(): AuctionEvent = AuctionEvent(
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

    companion object {

        /** Creates an entity from the domain model. */
        fun fromDomain(event: AuctionEvent): AuctionEventEntity = AuctionEventEntity(
            id = event.id,
            title = event.title,
            brand = event.brand,
            startDate = event.startDate,
            endDate = event.endDate,
            country = event.country,
            status = event.status,
            buyerPremiumPercent = event.buyerPremiumPercent,
            totalLots = event.totalLots
        )
    }
}
