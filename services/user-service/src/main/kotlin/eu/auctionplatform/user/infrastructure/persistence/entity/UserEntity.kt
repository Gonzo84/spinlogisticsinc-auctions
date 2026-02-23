package eu.auctionplatform.user.infrastructure.persistence.entity

import eu.auctionplatform.user.domain.model.AccountType
import eu.auctionplatform.user.domain.model.DepositStatus
import eu.auctionplatform.user.domain.model.User
import eu.auctionplatform.user.domain.model.UserStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity mapped to the `app.users` table.
 *
 * Serves as the persistence representation of the [User] domain model.
 * Conversion between the entity and domain model is handled via the
 * [toDomain] and [fromDomain] functions to keep the domain layer free
 * of persistence concerns.
 */
@Entity
@Table(name = "users", schema = "app")
class UserEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "keycloak_id", nullable = false, unique = true, updatable = false)
    var keycloakId: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    var accountType: AccountType = AccountType.PRIVATE,

    @Column(name = "email", nullable = false, unique = true)
    var email: String = "",

    @Column(name = "phone")
    var phone: String? = null,

    @Column(name = "first_name", nullable = false)
    var firstName: String = "",

    @Column(name = "last_name", nullable = false)
    var lastName: String = "",

    @Column(name = "language", nullable = false)
    var language: String = "en",

    @Column(name = "currency", nullable = false)
    var currency: String = "EUR",

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: UserStatus = UserStatus.ACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_status", nullable = false)
    var depositStatus: DepositStatus = DepositStatus.NONE,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {

    /** Converts this entity to the domain model. */
    fun toDomain(): User = User(
        id = id,
        keycloakId = keycloakId,
        accountType = accountType,
        email = email,
        phone = phone,
        firstName = firstName,
        lastName = lastName,
        language = language,
        currency = currency,
        status = status,
        depositStatus = depositStatus,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {

        /** Creates an entity from the domain model. */
        fun fromDomain(user: User): UserEntity = UserEntity(
            id = user.id,
            keycloakId = user.keycloakId,
            accountType = user.accountType,
            email = user.email,
            phone = user.phone,
            firstName = user.firstName,
            lastName = user.lastName,
            language = user.language,
            currency = user.currency,
            status = user.status,
            depositStatus = user.depositStatus,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }
}
