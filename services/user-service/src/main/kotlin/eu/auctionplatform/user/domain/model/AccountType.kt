package eu.auctionplatform.user.domain.model

/**
 * The type of account a user holds on the auction platform.
 *
 * - **BUSINESS** – A registered business entity (company / sole proprietor).
 *   Business accounts require VAT identification and may be subject to
 *   additional KYC verification.
 * - **PRIVATE** – An individual consumer account. Private accounts have
 *   simplified onboarding but may face bidding restrictions on certain
 *   lots designated as B2B-only.
 */
enum class AccountType {
    BUSINESS,
    PRIVATE
}
