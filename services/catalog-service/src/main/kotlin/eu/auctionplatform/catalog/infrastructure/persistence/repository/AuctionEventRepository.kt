package eu.auctionplatform.catalog.infrastructure.persistence.repository

import eu.auctionplatform.catalog.domain.model.AuctionEventStatus
import eu.auctionplatform.catalog.infrastructure.persistence.entity.AuctionEventEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * Repository for [AuctionEventEntity] persistence operations.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 */
@ApplicationScoped
class AuctionEventRepository : PanacheRepositoryBase<AuctionEventEntity, UUID> {

    /**
     * Returns auction events with a given status, ordered by start date.
     *
     * @param status The auction event status to filter by.
     * @return List of matching auction event entities.
     */
    fun findByStatus(status: AuctionEventStatus): List<AuctionEventEntity> =
        list("status", Sort.ascending("startDate"), status)

    /**
     * Returns auction events for a given brand, ordered by start date descending.
     *
     * @param brand The brand/tenant code.
     * @return List of matching auction event entities.
     */
    fun findByBrand(brand: String): List<AuctionEventEntity> =
        list("brand = ?1 order by startDate desc", brand)

    /**
     * Returns upcoming and active auction events for a given country.
     *
     * @param country ISO 3166-1 alpha-2 country code.
     * @return List of matching auction event entities.
     */
    fun findUpcomingByCountry(country: String): List<AuctionEventEntity> =
        list(
            "country = ?1 and (status = ?2 or status = ?3) order by startDate asc",
            country,
            AuctionEventStatus.SCHEDULED,
            AuctionEventStatus.ACTIVE
        )
}
