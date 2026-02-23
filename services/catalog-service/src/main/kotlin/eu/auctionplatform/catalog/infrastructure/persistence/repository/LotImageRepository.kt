package eu.auctionplatform.catalog.infrastructure.persistence.repository

import eu.auctionplatform.catalog.infrastructure.persistence.entity.LotImageEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.hibernate.orm.PersistenceUnit
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * Repository for [LotImageEntity] persistence operations.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 */
@ApplicationScoped
@PersistenceUnit("system")
class LotImageRepository : PanacheRepositoryBase<LotImageEntity, UUID> {

    /**
     * Returns all images for a given lot, ordered by display position.
     *
     * @param lotId The lot identifier.
     * @return List of image entities for the lot.
     */
    fun findByLotId(lotId: UUID): List<LotImageEntity> =
        list("lotId = ?1 order by displayOrder asc", lotId)

    /**
     * Finds the primary image for a given lot.
     *
     * @param lotId The lot identifier.
     * @return The primary image entity, or `null` if no primary is set.
     */
    fun findPrimaryByLotId(lotId: UUID): LotImageEntity? =
        find("lotId = ?1 and isPrimary = true", lotId).firstResult()

    /**
     * Deletes all images for a given lot.
     *
     * @param lotId The lot identifier.
     * @return The number of deleted image records.
     */
    fun deleteByLotId(lotId: UUID): Long =
        delete("lotId", lotId)
}
