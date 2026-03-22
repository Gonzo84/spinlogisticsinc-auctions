package eu.auctionplatform.user.domain.model

import java.util.UUID

/**
 * Immutable domain model representing a company profile associated with
 * a [AccountType.BUSINESS] user account.
 *
 * Company data is used for invoicing, tax validation, and compliance
 * reporting. The [verified] flag indicates whether an administrator has
 * confirmed the company registration details against official registries.
 *
 * @property id              Unique identifier (UUIDv7).
 * @property userId          The owning user's identifier.
 * @property companyName     Legal name of the company.
 * @property registrationNo  National business registration number.
 * @property ein             US Employer Identification Number (format XX-XXXXXXX).
 * @property state           US state code (2-letter, e.g. "NY", "CA", "TX").
 * @property address         Street address.
 * @property city            City name.
 * @property postalCode      Postal / ZIP code.
 * @property entityType      US business entity type (LLC, C-Corp, S-Corp, LP, Sole Prop).
 * @property verified        Whether the company has been verified by platform staff.
 */
data class Company(
    val id: UUID,
    val userId: UUID,
    val companyName: String,
    val registrationNo: String,
    val ein: String,
    val state: String,
    val address: String,
    val city: String,
    val postalCode: String,
    val entityType: String = "",
    val verified: Boolean = false
)
