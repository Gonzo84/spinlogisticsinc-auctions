package eu.auctionplatform.user.infrastructure.persistence.repository

import eu.auctionplatform.user.infrastructure.persistence.entity.DepositEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * Repository for [DepositEntity] persistence operations.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 */
@ApplicationScoped
class DepositRepository : PanacheRepositoryBase<DepositEntity, UUID> {

    /**
     * Finds the most recent deposit for a given user.
     *
     * @param userId The user identifier.
     * @return The latest deposit entity, or `null` if none exist.
     */
    fun findLatestByUserId(userId: UUID): DepositEntity? =
        find("userId = ?1 order by createdAt desc", userId).firstResult()

    /**
     * Returns all deposits for a given user, ordered by creation time descending.
     *
     * @param userId The user identifier.
     * @return List of deposit entities, most recent first.
     */
    fun findAllByUserId(userId: UUID): List<DepositEntity> =
        list("userId = ?1 order by createdAt desc", userId)

    /**
     * Finds the currently active deposit for a user (paid, not refunded).
     *
     * @param userId The user identifier.
     * @return The active deposit entity, or `null` if no active deposit exists.
     */
    fun findActiveByUserId(userId: UUID): DepositEntity? =
        find("userId = ?1 and paidAt is not null and refundedAt is null", userId).firstResult()
}
