package eu.auctionplatform.catalog.domain.model

/**
 * Lifecycle status of a lot within the catalog.
 *
 * State transitions:
 * ```
 * DRAFT ──► PENDING_REVIEW ──► APPROVED ──► ACTIVE ──► SOLD
 *              │                    │          │
 *              ▼                    ▼          ▼
 *          WITHDRAWN           WITHDRAWN   UNSOLD
 * ```
 *
 * - **DRAFT** – The seller is still preparing the lot listing.
 * - **PENDING_REVIEW** – The lot has been submitted for review by platform staff.
 * - **APPROVED** – The lot has been approved and is ready to be assigned to an auction.
 * - **ACTIVE** – The lot is currently listed in a live or upcoming auction.
 * - **SOLD** – The lot was sold via auction and is awaiting pickup/delivery.
 * - **UNSOLD** – The auction closed without the reserve being met or without bids.
 * - **WITHDRAWN** – The seller or an admin withdrew the lot from the catalog.
 */
enum class LotStatus {
    DRAFT,
    PENDING_REVIEW,
    APPROVED,
    ACTIVE,
    SOLD,
    UNSOLD,
    WITHDRAWN;

    /**
     * Returns `true` if the lot can be edited by the seller.
     * Only drafts and lots pending review may be modified.
     */
    fun isEditable(): Boolean = this == DRAFT || this == PENDING_REVIEW

    /**
     * Returns `true` if the lot is in a terminal state (sold, unsold, or withdrawn).
     */
    fun isTerminal(): Boolean = this == SOLD || this == UNSOLD || this == WITHDRAWN

    /**
     * Returns `true` if the lot can be submitted for review.
     */
    fun canSubmitForReview(): Boolean = this == DRAFT

    /**
     * Returns `true` if the lot can be approved by a reviewer.
     */
    fun canApprove(): Boolean = this == PENDING_REVIEW
}
