package eu.auctionplatform.payment.domain.model

/**
 * Lifecycle status of a payment.
 *
 * Transitions:
 * - PENDING -> PROCESSING (payment submitted to PSP)
 * - PROCESSING -> COMPLETED (PSP confirms success)
 * - PROCESSING -> FAILED (PSP reports failure)
 * - COMPLETED -> REFUNDED (admin-initiated refund)
 */
enum class PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED
}
