package eu.auctionplatform.user.infrastructure.persistence.repository

import eu.auctionplatform.user.infrastructure.persistence.entity.CompanyEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.hibernate.orm.PersistenceUnit
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * Repository for [CompanyEntity] persistence operations.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 */
@ApplicationScoped
@PersistenceUnit("system")
class CompanyRepository : PanacheRepositoryBase<CompanyEntity, UUID> {

    /**
     * Finds a company profile by the owning user's identifier.
     *
     * @param userId The user identifier.
     * @return The matching company entity, or `null` if the user has no company profile.
     */
    fun findByUserId(userId: UUID): CompanyEntity? =
        find("userId", userId).firstResult()

    /**
     * Finds a company by its VAT identification number.
     *
     * @param vatId The EU VAT ID to search for.
     * @return The matching company entity, or `null` if not found.
     */
    fun findByVatId(vatId: String): CompanyEntity? =
        find("vatId", vatId).firstResult()

    /**
     * Checks whether a company with the given VAT ID already exists.
     *
     * @param vatId The EU VAT ID to check.
     * @return `true` if a company with this VAT ID exists.
     */
    fun existsByVatId(vatId: String): Boolean =
        count("vatId", vatId) > 0
}
