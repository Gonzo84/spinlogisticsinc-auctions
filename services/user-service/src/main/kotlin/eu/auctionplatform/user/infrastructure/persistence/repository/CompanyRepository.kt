package eu.auctionplatform.user.infrastructure.persistence.repository

import eu.auctionplatform.user.infrastructure.persistence.entity.CompanyEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * Repository for [CompanyEntity] persistence operations.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 */
@ApplicationScoped
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
     * Finds a company by its Employer Identification Number.
     *
     * @param ein The US EIN to search for.
     * @return The matching company entity, or `null` if not found.
     */
    fun findByEin(ein: String): CompanyEntity? =
        find("ein", ein).firstResult()

    /**
     * Checks whether a company with the given EIN already exists.
     *
     * @param ein The US EIN to check.
     * @return `true` if a company with this EIN exists.
     */
    fun existsByEin(ein: String): Boolean =
        count("ein", ein) > 0
}
