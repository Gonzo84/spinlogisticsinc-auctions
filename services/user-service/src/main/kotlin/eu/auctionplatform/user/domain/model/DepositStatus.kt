package eu.auctionplatform.user.domain.model

/**
 * Tracks the state of a user's security deposit on the platform.
 *
 * A deposit is required to bid on lots above a certain price threshold.
 *
 * State transitions:
 * ```
 * NONE ──► ACTIVE ──► REFUND_REQUESTED ──► REFUNDED
 * ```
 *
 * - **NONE** – No deposit has been paid.
 * - **ACTIVE** – A deposit is on file and the user may bid on high-value lots.
 * - **REFUND_REQUESTED** – The user has requested a refund of their deposit.
 *   During this state the deposit is still considered "held" until the PSP
 *   confirms the refund.
 * - **REFUNDED** – The deposit has been returned to the user. They must pay
 *   a new deposit to bid on high-value lots again.
 */
enum class DepositStatus {
    NONE,
    ACTIVE,
    REFUND_REQUESTED,
    REFUNDED;

    /**
     * Returns `true` if the user currently has a deposit that enables
     * bidding on high-value lots.
     */
    fun hasActiveDeposit(): Boolean = this == ACTIVE
}
