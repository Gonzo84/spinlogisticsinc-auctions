package eu.auctionplatform.user.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Immutable domain model representing a registered user on the auction platform.
 *
 * A user is identified by their internal [id] (UUIDv7) and linked to the
 * external identity provider via [keycloakId]. Users may hold either a
 * [AccountType.BUSINESS] or [AccountType.PRIVATE] account, which determines
 * their onboarding flow and eligible lot categories.
 *
 * The [status] governs what actions the user can perform (bidding, selling),
 * while [depositStatus] tracks whether the user has a security deposit on
 * file for high-value lot bidding.
 *
 * @property id            Unique internal identifier (UUIDv7).
 * @property keycloakId    External subject identifier from Keycloak.
 * @property accountType   Whether this is a business or private account.
 * @property email         Primary email address (unique across the platform).
 * @property phone         Optional phone number in E.164 format.
 * @property firstName     User's given name.
 * @property lastName      User's family name.
 * @property language      Preferred UI language (BCP-47 tag, e.g. "en", "de").
 * @property currency      Preferred display currency (ISO 4217 code).
 * @property status        Current lifecycle status of the account.
 * @property depositStatus Current state of the user's security deposit.
 * @property createdAt     UTC instant when the account was created.
 * @property updatedAt     UTC instant when the account was last modified.
 */
data class User(
    val id: UUID,
    val keycloakId: String,
    val accountType: AccountType,
    val email: String,
    val phone: String? = null,
    val firstName: String,
    val lastName: String,
    val language: String = "en",
    val currency: String = "EUR",
    val status: UserStatus = UserStatus.ACTIVE,
    val depositStatus: DepositStatus = DepositStatus.NONE,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {

    /** The user's full display name. */
    val fullName: String
        get() = "$firstName $lastName"

    /** Returns `true` if the user is allowed to place bids and create lots. */
    fun canTrade(): Boolean = status.canTrade()

    /** Returns `true` if the user has a deposit enabling high-value bidding. */
    fun hasActiveDeposit(): Boolean = depositStatus.hasActiveDeposit()

    /** Returns a copy with the given [newStatus] and an updated timestamp. */
    fun withStatus(newStatus: UserStatus): User =
        copy(status = newStatus, updatedAt = Instant.now())

    /** Returns a copy with the given [newDepositStatus] and an updated timestamp. */
    fun withDepositStatus(newDepositStatus: DepositStatus): User =
        copy(depositStatus = newDepositStatus, updatedAt = Instant.now())
}
