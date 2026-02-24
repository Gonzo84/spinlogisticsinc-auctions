package eu.auctionplatform.user.infrastructure.persistence.repository

import eu.auctionplatform.user.infrastructure.persistence.entity.UserEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
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
}
