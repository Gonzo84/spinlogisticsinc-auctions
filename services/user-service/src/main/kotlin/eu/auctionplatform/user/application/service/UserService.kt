package eu.auctionplatform.user.application.service

import eu.auctionplatform.commons.exception.ConflictException
import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.commons.exception.ValidationException
import eu.auctionplatform.commons.util.IdGenerator
import eu.auctionplatform.user.api.dto.AddCompanyRequest
import eu.auctionplatform.user.api.dto.InitiateDepositRequest
import eu.auctionplatform.user.api.dto.RegisterUserRequest
import eu.auctionplatform.user.api.dto.UpdateProfileRequest
import eu.auctionplatform.user.domain.model.AccountType
import eu.auctionplatform.user.domain.model.Company
import eu.auctionplatform.user.domain.model.Deposit
import eu.auctionplatform.user.domain.model.DepositStatus
import eu.auctionplatform.user.domain.model.User
import eu.auctionplatform.user.domain.model.UserStatus
import eu.auctionplatform.user.infrastructure.persistence.entity.CompanyEntity
import eu.auctionplatform.user.infrastructure.persistence.entity.DepositEntity
import eu.auctionplatform.user.infrastructure.persistence.entity.UserEntity
import eu.auctionplatform.user.infrastructure.persistence.repository.CompanyRepository
import eu.auctionplatform.user.infrastructure.persistence.repository.DepositRepository
import eu.auctionplatform.user.infrastructure.persistence.repository.KycRecordRepository
import eu.auctionplatform.user.infrastructure.persistence.repository.UserRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

/**
 * Application service that orchestrates user management operations.
 *
 * This service sits between the REST API layer and the domain/persistence
 * layers. It coordinates transactional boundaries, enforces business rules,
 * and translates between DTOs and domain models.
 *
 * All mutating methods are annotated with [@Transactional] to ensure
 * atomicity across multiple repository calls.
 */
@ApplicationScoped
class UserService {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var companyRepository: CompanyRepository

    @Inject
    lateinit var depositRepository: DepositRepository

    @Inject
    lateinit var kycRecordRepository: KycRecordRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(UserService::class.java)
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers a new user on the platform.
     *
     * Validates that neither the email nor the Keycloak ID is already in use,
     * then persists the new user entity.
     *
     * @param request Registration details.
     * @return The newly created [User] domain model.
     * @throws ConflictException if the email or Keycloak ID is already registered.
     */
    @Transactional
    fun registerUser(request: RegisterUserRequest): User {
        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException(
                code = "EMAIL_ALREADY_EXISTS",
                message = "A user with email '${request.email}' already exists."
            )
        }
        if (userRepository.existsByKeycloakId(request.keycloakId)) {
            throw ConflictException(
                code = "KEYCLOAK_ID_ALREADY_EXISTS",
                message = "A user with Keycloak ID '${request.keycloakId}' already exists."
            )
        }

        val user = User(
            id = IdGenerator.generateUUIDv7(),
            keycloakId = request.keycloakId,
            accountType = request.accountType,
            email = request.email,
            phone = request.phone,
            firstName = request.firstName,
            lastName = request.lastName,
            language = request.language,
            currency = request.currency,
            status = UserStatus.ACTIVE,
            depositStatus = DepositStatus.NONE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val entity = UserEntity.fromDomain(user)
        userRepository.persist(entity)

        LOG.infof("Registered new user: id=%s, email=%s, accountType=%s",
            user.id, user.email, user.accountType)

        return user
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Retrieves a user by their internal identifier.
     *
     * @param id The user's UUID.
     * @return The [User] domain model.
     * @throws NotFoundException if no user exists with the given ID.
     */
    fun getUserById(id: UUID): User {
        val entity = userRepository.findById(id)
            ?: throw NotFoundException(
                code = "USER_NOT_FOUND",
                message = "User with id '$id' not found."
            )
        return entity.toDomain()
    }

    /**
     * Retrieves a user by their Keycloak subject identifier.
     *
     * @param keycloakId The `sub` claim from the JWT.
     * @return The [User] domain model.
     * @throws NotFoundException if no user exists with the given Keycloak ID.
     */
    fun getUserByKeycloakId(keycloakId: String): User {
        val entity = userRepository.findByKeycloakId(keycloakId)
            ?: throw NotFoundException(
                code = "USER_NOT_FOUND",
                message = "User with Keycloak ID '$keycloakId' not found."
            )
        return entity.toDomain()
    }

    /**
     * Retrieves a user by their Keycloak ID, auto-creating the user profile
     * on first access using claims extracted from the JWT token.
     *
     * This ensures that any Keycloak-authenticated user automatically gets
     * a user-service profile without requiring a separate registration step.
     *
     * @param keycloakId The `sub` claim from the JWT.
     * @param email      The `email` claim from the JWT.
     * @param firstName  The `given_name` claim from the JWT.
     * @param lastName   The `family_name` claim from the JWT.
     * @return The existing or newly created [User] domain model.
     */
    @Transactional
    fun getOrCreateUser(keycloakId: String, email: String, firstName: String, lastName: String): User {
        val existing = userRepository.findByKeycloakId(keycloakId)
        if (existing != null) {
            return existing.toDomain()
        }

        LOG.infof("Auto-registering user on first access: keycloakId=%s, email=%s", keycloakId, email)

        val user = User(
            id = IdGenerator.generateUUIDv7(),
            keycloakId = keycloakId,
            accountType = AccountType.PRIVATE,
            email = email.ifEmpty { "$keycloakId@placeholder.local" },
            firstName = firstName.ifEmpty { "User" },
            lastName = lastName.ifEmpty { keycloakId.take(8) },
            language = "en",
            currency = "EUR",
            status = UserStatus.ACTIVE,
            depositStatus = DepositStatus.NONE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val entity = UserEntity.fromDomain(user)
        userRepository.persist(entity)

        LOG.infof("Auto-registered user: id=%s, email=%s", user.id, user.email)

        return user
    }

    /**
     * Retrieves the company profile for a given user.
     *
     * @param userId The user's UUID.
     * @return The [Company] domain model, or `null` if no company is associated.
     */
    fun getCompanyByUserId(userId: UUID): Company? =
        companyRepository.findByUserId(userId)?.toDomain()

    /**
     * Retrieves the latest deposit for a given user.
     *
     * @param userId The user's UUID.
     * @return The [Deposit] domain model, or `null` if no deposit exists.
     */
    fun getLatestDeposit(userId: UUID): Deposit? =
        depositRepository.findLatestByUserId(userId)?.toDomain()

    // -------------------------------------------------------------------------
    // Profile management
    // -------------------------------------------------------------------------

    /**
     * Updates mutable profile fields for the authenticated user.
     *
     * Only non-null fields in the request are applied. The [User.updatedAt]
     * timestamp is refreshed automatically.
     *
     * @param userId  The user's UUID.
     * @param request The fields to update.
     * @return The updated [User] domain model.
     * @throws NotFoundException if no user exists with the given ID.
     */
    @Transactional
    fun updateProfile(userId: UUID, request: UpdateProfileRequest): User {
        val entity = userRepository.findById(userId)
            ?: throw NotFoundException(
                code = "USER_NOT_FOUND",
                message = "User with id '$userId' not found."
            )

        request.phone?.let { entity.phone = it }
        request.firstName?.let { entity.firstName = it }
        request.lastName?.let { entity.lastName = it }
        request.language?.let { entity.language = it }
        request.currency?.let { entity.currency = it }
        entity.updatedAt = Instant.now()

        userRepository.persist(entity)

        LOG.infof("Updated profile for user: id=%s", userId)

        return entity.toDomain()
    }

    // -------------------------------------------------------------------------
    // Company management
    // -------------------------------------------------------------------------

    /**
     * Adds a company profile to a BUSINESS user account.
     *
     * Validates that the user has a BUSINESS account type and does not already
     * have a company profile, and that the VAT ID is not already registered.
     *
     * @param userId  The user's UUID.
     * @param request The company details.
     * @return The newly created [Company] domain model.
     * @throws ValidationException if the user is not a BUSINESS account.
     * @throws ConflictException   if a company profile already exists or the VAT ID is taken.
     */
    @Transactional
    fun addCompany(userId: UUID, request: AddCompanyRequest): Company {
        val user = getUserById(userId)

        if (user.accountType != AccountType.BUSINESS) {
            throw ValidationException(
                field = "accountType",
                error = "Company profiles can only be added to BUSINESS accounts."
            )
        }

        if (companyRepository.findByUserId(userId) != null) {
            throw ConflictException(
                code = "COMPANY_ALREADY_EXISTS",
                message = "User '$userId' already has a company profile."
            )
        }

        if (companyRepository.existsByVatId(request.vatId)) {
            throw ConflictException(
                code = "VAT_ID_ALREADY_EXISTS",
                message = "A company with VAT ID '${request.vatId}' is already registered."
            )
        }

        val company = Company(
            id = IdGenerator.generateUUIDv7(),
            userId = userId,
            companyName = request.companyName,
            registrationNo = request.registrationNo,
            vatId = request.vatId,
            country = request.country,
            address = request.address,
            city = request.city,
            postalCode = request.postalCode,
            verified = false
        )

        companyRepository.persist(CompanyEntity.fromDomain(company))

        LOG.infof("Added company for user: userId=%s, companyId=%s, vatId=%s",
            userId, company.id, company.vatId)

        return company
    }

    // -------------------------------------------------------------------------
    // Deposit management
    // -------------------------------------------------------------------------

    /**
     * Initiates a new security deposit for the user.
     *
     * Validates that the user does not already have an active deposit, then
     * creates a new deposit record and updates the user's deposit status.
     *
     * @param userId  The user's UUID.
     * @param request The deposit payment details.
     * @return The newly created [Deposit] domain model.
     * @throws ConflictException if an active deposit already exists.
     */
    @Transactional
    fun initiateDeposit(userId: UUID, request: InitiateDepositRequest): Deposit {
        val user = getUserById(userId)

        if (user.depositStatus == DepositStatus.ACTIVE) {
            throw ConflictException(
                code = "DEPOSIT_ALREADY_ACTIVE",
                message = "User '$userId' already has an active deposit."
            )
        }

        val deposit = Deposit(
            id = IdGenerator.generateUUIDv7(),
            userId = userId,
            amount = request.amount,
            currency = request.currency,
            paidAt = Instant.now(),
            pspReference = request.pspReference
        )

        depositRepository.persist(DepositEntity.fromDomain(deposit))

        // Update user deposit status
        val userEntity = userRepository.findById(userId)!!
        userEntity.depositStatus = DepositStatus.ACTIVE
        userEntity.updatedAt = Instant.now()
        userRepository.persist(userEntity)

        LOG.infof("Deposit initiated for user: userId=%s, depositId=%s, amount=%s %s",
            userId, deposit.id, deposit.amount, deposit.currency)

        return deposit
    }

    /**
     * Requests a refund for the user's active deposit.
     *
     * Sets the [Deposit.refundRequestedAt] timestamp and transitions the
     * user's deposit status to [DepositStatus.REFUND_REQUESTED].
     *
     * @param userId The user's UUID.
     * @return The updated [Deposit] domain model.
     * @throws NotFoundException if no active deposit exists.
     */
    @Transactional
    fun requestDepositRefund(userId: UUID): Deposit {
        val depositEntity = depositRepository.findActiveByUserId(userId)
            ?: throw NotFoundException(
                code = "NO_ACTIVE_DEPOSIT",
                message = "User '$userId' does not have an active deposit to refund."
            )

        depositEntity.refundRequestedAt = Instant.now()
        depositRepository.persist(depositEntity)

        // Update user deposit status
        val userEntity = userRepository.findById(userId)!!
        userEntity.depositStatus = DepositStatus.REFUND_REQUESTED
        userEntity.updatedAt = Instant.now()
        userRepository.persist(userEntity)

        LOG.infof("Deposit refund requested for user: userId=%s, depositId=%s",
            userId, depositEntity.id)

        return depositEntity.toDomain()
    }

    // -------------------------------------------------------------------------
    // Admin operations
    // -------------------------------------------------------------------------

    /**
     * Blocks a user account, preventing all platform interactions.
     *
     * @param userId The user's UUID.
     * @param reason Optional reason for the block (for audit logging).
     * @return The updated [User] domain model.
     * @throws NotFoundException if no user exists with the given ID.
     * @throws ConflictException if the user is already blocked.
     */
    @Transactional
    fun blockUser(userId: UUID, reason: String? = null): User {
        val entity = userRepository.findById(userId)
            ?: throw NotFoundException(
                code = "USER_NOT_FOUND",
                message = "User with id '$userId' not found."
            )

        if (entity.status == UserStatus.BLOCKED) {
            throw ConflictException(
                code = "USER_ALREADY_BLOCKED",
                message = "User '$userId' is already blocked."
            )
        }

        entity.status = UserStatus.BLOCKED
        entity.updatedAt = Instant.now()
        userRepository.persist(entity)

        LOG.warnf("User blocked: userId=%s, reason=%s", userId, reason ?: "N/A")

        return entity.toDomain()
    }

    /**
     * Unblocks a previously blocked user account, restoring it to ACTIVE status.
     *
     * @param userId The user's UUID.
     * @return The updated [User] domain model.
     * @throws NotFoundException if no user exists with the given ID.
     * @throws ConflictException if the user is not currently blocked.
     */
    @Transactional
    fun unblockUser(userId: UUID): User {
        val entity = userRepository.findById(userId)
            ?: throw NotFoundException(
                code = "USER_NOT_FOUND",
                message = "User with id '$userId' not found."
            )

        if (entity.status != UserStatus.BLOCKED) {
            throw ConflictException(
                code = "USER_NOT_BLOCKED",
                message = "User '$userId' is not blocked (current status: ${entity.status})."
            )
        }

        entity.status = UserStatus.ACTIVE
        entity.updatedAt = Instant.now()
        userRepository.persist(entity)

        LOG.infof("User unblocked: userId=%s", userId)

        return entity.toDomain()
    }

    /**
     * Updates a user's status to any valid [UserStatus] value.
     *
     * This is a general-purpose admin endpoint that allows transitions
     * to any status. Business rule validation (e.g. preventing transitions
     * from BLOCKED to PENDING_KYC) should be enforced at the caller level
     * or via additional domain logic as requirements evolve.
     *
     * @param userId    The user's UUID.
     * @param newStatus The target status.
     * @param reason    Optional reason for the status change (audit logging).
     * @return The updated [User] domain model.
     * @throws NotFoundException if no user exists with the given ID.
     */
    @Transactional
    fun updateUserStatus(userId: UUID, newStatus: UserStatus, reason: String? = null): User {
        val entity = userRepository.findById(userId)
            ?: throw NotFoundException(
                code = "USER_NOT_FOUND",
                message = "User with id '$userId' not found."
            )

        val previousStatus = entity.status
        entity.status = newStatus
        entity.updatedAt = Instant.now()
        userRepository.persist(entity)

        LOG.infof("User status changed: userId=%s, from=%s, to=%s, reason=%s",
            userId, previousStatus, newStatus, reason ?: "N/A")

        return entity.toDomain()
    }
}
