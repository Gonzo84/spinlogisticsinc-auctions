package eu.auctionplatform.user.api.dto

import eu.auctionplatform.user.domain.model.Company
import eu.auctionplatform.user.domain.model.Deposit
import eu.auctionplatform.user.domain.model.User

/**
 * Extension functions for converting domain models to response DTOs.
 *
 * These mappers live in the API layer to keep the domain model free
 * of serialisation concerns.
 */

/** Converts a [User] domain model to a [UserResponse] DTO. */
fun User.toResponse(): UserResponse = UserResponse(
    id = id,
    keycloakId = keycloakId,
    accountType = accountType,
    email = email,
    phone = phone,
    firstName = firstName,
    lastName = lastName,
    fullName = fullName,
    language = language,
    currency = currency,
    status = status,
    depositStatus = depositStatus,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** Converts a [Company] domain model to a [CompanyResponse] DTO. */
fun Company.toResponse(): CompanyResponse = CompanyResponse(
    id = id,
    userId = userId,
    companyName = companyName,
    registrationNo = registrationNo,
    vatId = vatId,
    country = country,
    address = address,
    city = city,
    postalCode = postalCode,
    verified = verified
)

/** Converts a [Deposit] domain model to a [DepositResponse] DTO. */
fun Deposit.toResponse(): DepositResponse = DepositResponse(
    id = id,
    userId = userId,
    amount = amount,
    currency = currency,
    paidAt = paidAt,
    refundRequestedAt = refundRequestedAt,
    refundedAt = refundedAt,
    pspReference = pspReference,
    isActive = isActive()
)
