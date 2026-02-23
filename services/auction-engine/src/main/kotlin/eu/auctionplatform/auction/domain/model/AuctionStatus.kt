package eu.auctionplatform.auction.domain.model

/**
 * Lifecycle states of an auction aggregate.
 *
 * State transitions:
 * ```
 * SCHEDULED ─► ACTIVE ─► CLOSING ─► CLOSED ─► AWARDED
 *                │                      │
 *                └────► CANCELLED ◄─────┘
 * ```
 *
 * - **SCHEDULED** – The auction has been created but the start time has not yet arrived.
 * - **ACTIVE** – The auction is open and accepting bids.
 * - **CLOSING** – The auction has entered its final phase (anti-sniping window active);
 *                 bids are still accepted but each bid may extend the end time.
 * - **CLOSED** – Bidding has ended; the auction is awaiting award processing.
 * - **AWARDED** – The winning bidder has been confirmed and the lot has been awarded.
 * - **CANCELLED** – The auction was cancelled by an administrator or the seller
 *                   before award completion.
 */
enum class AuctionStatus {

    SCHEDULED,
    ACTIVE,
    CLOSING,
    CLOSED,
    AWARDED,
    CANCELLED;

    /**
     * Returns `true` if the auction is in a state that accepts new bids.
     */
    fun acceptsBids(): Boolean = this == ACTIVE || this == CLOSING

    /**
     * Returns `true` if the auction has reached a terminal state.
     */
    fun isTerminal(): Boolean = this == AWARDED || this == CANCELLED
}
