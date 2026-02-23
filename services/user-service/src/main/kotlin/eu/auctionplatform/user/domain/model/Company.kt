package eu.auctionplatform.user.domain.model

import java.util.UUID

/**
 * Immutable domain model representing a company profile associated with
 * a [AccountType.BUSINESS] user account.
 *
 * Company data is used for invoicing, VAT validation, and compliance
 * reporting. The [verified] flag indicates whether an administrator has
 * confirmed the company registration details against official registries.
 *
 * @property id              Unique identifier (UUIDv7).
 * @property userId          The owning user's identifier.
 * @property companyName     Legal name of the company.
 * @property registrationNo  National business registration / chamber of commerce number.
 * @property vatId           EU VAT identification number (e.g. "NL123456789B01").
 * @property country         ISO 3166-1 alpha-2 country code of registration.
 * @property address         Street address.
 * @property city            City name.
 * @property postalCode      Postal / ZIP code.
 * @property verified        Whether the company has been verified by platform staff.
 */
data class Company(
    val id: UUID,
    val userId: UUID,
    val companyName: String,
    val registrationNo: String,
    val vatId: String,
    val country: String,
    val address: String,
    val city: String,
    val postalCode: String,
    val verified: Boolean = false
)
