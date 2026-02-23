package eu.auctionplatform.seller.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Domain model for a seller's profile on the auction platform.
 *
 * Sellers are B2B entities (companies) that list lots for auction. Each seller
 * must go through a verification process before their lots are published.
 *
 * @property id              Unique seller profile identifier.
 * @property userId          Reference to the user account in the user service.
 * @property companyName     Registered company name.
 * @property registrationNo  Business registration / chamber of commerce number.
 * @property vatId           EU VAT identification number (for cross-border B2B).
 * @property country         ISO 3166-1 alpha-2 country code.
 * @property status          Verification status of the seller profile.
 * @property verifiedAt      Timestamp when the seller was verified (null if pending).
 * @property createdAt       Timestamp when the profile was created.
 */
data class SellerProfile(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val companyName: String,
    val registrationNo: String? = null,
    val vatId: String? = null,
    val country: String,
    val status: SellerStatus = SellerStatus.PENDING,
    val verifiedAt: Instant? = null,
    val createdAt: Instant = Instant.now()
)

/**
 * Verification status of a seller profile.
 *
 * Transitions:
 * - PENDING -> VERIFIED (after manual review / automated checks)
 * - PENDING -> SUSPENDED (compliance issue detected during review)
 * - VERIFIED -> SUSPENDED (compliance issue detected post-verification)
 * - SUSPENDED -> VERIFIED (issue resolved, re-verified)
 */
enum class SellerStatus {

    /** Seller has registered but is awaiting verification. */
    PENDING,

    /** Seller has been verified and can create auction lots. */
    VERIFIED,

    /** Seller account has been suspended (compliance, fraud, etc.). */
    SUSPENDED
}
