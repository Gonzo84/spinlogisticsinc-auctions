package eu.auctionplatform.user.infrastructure.persistence.repository

import eu.auctionplatform.user.domain.model.KycStatus
import eu.auctionplatform.user.infrastructure.persistence.entity.KycRecordEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * Repository for [KycRecordEntity] persistence operations.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 */
@ApplicationScoped
class KycRecordRepository : PanacheRepositoryBase<KycRecordEntity, UUID> {

    /**
     * Returns all KYC records for a given user, ordered by creation time descending.
     *
     * @param userId The user identifier.
     * @return List of KYC records, most recent first.
     */
    fun findByUserId(userId: UUID): List<KycRecordEntity> =
        list("userId = ?1 order by createdAt desc", userId)

    /**
     * Finds the most recent KYC record for a user.
     *
     * @param userId The user identifier.
     * @return The latest KYC record, or `null` if none exist.
     */
    fun findLatestByUserId(userId: UUID): KycRecordEntity? =
        find("userId = ?1 order by createdAt desc", userId).firstResult()

    /**
     * Checks whether a user has at least one verified KYC record.
     *
     * @param userId The user identifier.
     * @return `true` if the user has a verified KYC record.
     */
    fun hasVerifiedRecord(userId: UUID): Boolean =
        count("userId = ?1 and status = ?2", userId, KycStatus.VERIFIED) > 0
}
