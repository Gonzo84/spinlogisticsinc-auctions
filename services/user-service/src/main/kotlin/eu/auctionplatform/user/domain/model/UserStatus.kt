package eu.auctionplatform.user.domain.model

/**
 * Lifecycle status of a user account on the auction platform.
 *
 * State transitions:
 * ```
 * PENDING_KYC ──► ACTIVE ──► BLOCKED
 *                   │  ▲        │
 *                   ▼  │        │
 *              SUSPENDED ◄──────┘
 * ```
 *
 * - **ACTIVE** – The user has completed onboarding and may fully participate
 *   in auctions (bidding, selling, managing deposits).
 * - **BLOCKED** – An administrator has permanently blocked the account due
 *   to policy violations. The user cannot log in or interact with the platform.
 * - **PENDING_KYC** – The user has registered but has not yet completed the
 *   mandatory Know-Your-Customer verification. Limited platform access.
 * - **SUSPENDED** – The account is temporarily suspended (e.g. pending
 *   investigation). The user can log in but cannot place bids or create lots.
 */
enum class UserStatus {
    ACTIVE,
    BLOCKED,
    PENDING_KYC,
    SUSPENDED;

    /**
     * Returns `true` if the user is allowed to place bids and create lots.
     */
    fun canTrade(): Boolean = this == ACTIVE

    /**
     * Returns `true` if the account has reached a terminal blocked state.
     */
    fun isBlocked(): Boolean = this == BLOCKED
}
