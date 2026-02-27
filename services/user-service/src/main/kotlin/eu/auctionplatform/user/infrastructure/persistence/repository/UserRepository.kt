package eu.auctionplatform.user.infrastructure.persistence.repository

import eu.auctionplatform.user.domain.model.UserStatus
import eu.auctionplatform.user.infrastructure.persistence.entity.UserEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * Repository for [UserEntity] persistence operations.
 *
 * Uses the named `system` datasource configured in `application.yml`.
 * Extends Panache's repository base to inherit standard CRUD operations
 * while providing custom query methods for domain-specific lookups.
 */
@ApplicationScoped
class UserRepository : PanacheRepositoryBase<UserEntity, UUID> {

    /**
     * Finds a user by their Keycloak subject identifier.
     *
     * @param keycloakId The `sub` claim from the Keycloak JWT.
     * @return The matching user entity, or `null` if not found.
     */
    fun findByKeycloakId(keycloakId: String): UserEntity? =
        find("keycloakId", keycloakId).firstResult()

    /**
     * Finds a user by their email address.
     *
     * @param email The email address to search for (case-sensitive).
     * @return The matching user entity, or `null` if not found.
     */
    fun findByEmail(email: String): UserEntity? =
        find("email", email).firstResult()

    /**
     * Checks whether a user with the given email address already exists.
     *
     * @param email The email address to check.
     * @return `true` if a user with this email exists.
     */
    fun existsByEmail(email: String): Boolean =
        count("email", email) > 0

    /**
     * Checks whether a user with the given Keycloak ID already exists.
     *
     * @param keycloakId The Keycloak subject identifier to check.
     * @return `true` if a user with this Keycloak ID exists.
     */
    fun existsByKeycloakId(keycloakId: String): Boolean =
        count("keycloakId", keycloakId) > 0

    /**
     * Searches and lists users with optional filters and pagination.
     *
     * @param search Optional search text to match against first name, last name, or email.
     * @param status Optional status filter.
     * @param page   0-based page number.
     * @param pageSize Number of items per page.
     * @return A pair of (list of entities, total count).
     */
    fun searchUsers(
        search: String?,
        status: UserStatus?,
        page: Int,
        pageSize: Int
    ): Pair<List<UserEntity>, Long> {
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any>()

        if (!search.isNullOrBlank()) {
            conditions.add("(lower(firstName) like :search or lower(lastName) like :search or lower(email) like :search)")
            params["search"] = "%${search.lowercase()}%"
        }
        if (status != null) {
            conditions.add("status = :status")
            params["status"] = status
        }

        val whereClause = if (conditions.isNotEmpty()) conditions.joinToString(" and ") else "1=1"

        val query = find(whereClause, Sort.descending("createdAt"), params)
        val total = query.count()
        val items = query.page(Page.of(page, pageSize)).list()

        return Pair(items, total)
    }
}
