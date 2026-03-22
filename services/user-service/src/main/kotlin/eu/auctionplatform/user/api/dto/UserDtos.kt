package eu.auctionplatform.user.api.dto

import eu.auctionplatform.user.domain.model.AccountType
import eu.auctionplatform.user.domain.model.DepositStatus
import eu.auctionplatform.user.domain.model.UserStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// =============================================================================
// Request DTOs
// =============================================================================

/**
 * Request payload for user registration.
 */
data class RegisterUserRequest(
    val keycloakId: String,
    val accountType: AccountType,
    val email: String,
    val phone: String? = null,
    val firstName: String,
    val lastName: String,
    val language: String = "en",
    val currency: String = "USD"
)

/**
 * Request payload for updating user profile fields.
 * Only non-null fields will be applied.
 */
data class UpdateProfileRequest(
    val phone: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val language: String? = null,
    val currency: String? = null
)

/**
 * Request payload for adding a company profile to a BUSINESS account.
 */
data class AddCompanyRequest(
    val companyName: String,
    val registrationNo: String,
    val ein: String,
    val state: String,
    val address: String,
    val city: String,
    val postalCode: String,
    val entityType: String? = null
)

/**
 * Request payload for initiating a security deposit payment.
 */
data class InitiateDepositRequest(
    val amount: BigDecimal = BigDecimal("200.00"),
    val currency: String = "USD",
    val pspReference: String? = null
)

/**
 * Request payload for changing a user's status (admin endpoint).
 */
data class UpdateUserStatusRequest(
    val status: UserStatus,
    val reason: String? = null
)

// =============================================================================
// Response DTOs
// =============================================================================

/**
 * Response representation of a user profile.
 */
data class UserResponse(
    val id: UUID,
    val keycloakId: String,
    val accountType: AccountType,
    val email: String,
    val phone: String?,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val language: String,
    val currency: String,
    val status: UserStatus,
    val depositStatus: DepositStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Response representation of a company profile.
 */
data class CompanyResponse(
    val id: UUID,
    val userId: UUID,
    val companyName: String,
    val registrationNo: String,
    val ein: String,
    val state: String,
    val address: String,
    val city: String,
    val postalCode: String,
    val entityType: String,
    val verified: Boolean
)

/**
 * Response representation of a deposit record.
 */
data class DepositResponse(
    val id: UUID,
    val userId: UUID,
    val amount: BigDecimal,
    val currency: String,
    val paidAt: Instant?,
    val refundRequestedAt: Instant?,
    val refundedAt: Instant?,
    val pspReference: String?,
    val isActive: Boolean
)

/**
 * Composite response for the /me endpoint including user, company, and deposit.
 */
data class UserProfileResponse(
    val user: UserResponse,
    val company: CompanyResponse? = null,
    val deposit: DepositResponse? = null
)
