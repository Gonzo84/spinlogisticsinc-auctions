package eu.auctionplatform.payment.domain.model

/**
 * Lifecycle status of a seller settlement.
 *
 * Transitions:
 * - PENDING -> PROCESSING (bank transfer initiated)
 * - PROCESSING -> PAID (bank confirms receipt)
 */
enum class SettlementStatus {
    PENDING,
    PROCESSING,
    PAID
}
