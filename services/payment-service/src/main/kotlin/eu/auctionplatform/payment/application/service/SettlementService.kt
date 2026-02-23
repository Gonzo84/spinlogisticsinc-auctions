package eu.auctionplatform.payment.application.service

import eu.auctionplatform.payment.domain.model.Payment
import eu.auctionplatform.payment.domain.model.PaymentStatus
import eu.auctionplatform.payment.domain.model.Settlement
import eu.auctionplatform.payment.domain.model.SettlementStatus
import eu.auctionplatform.payment.infrastructure.persistence.repository.PaymentRepository
import eu.auctionplatform.payment.infrastructure.persistence.repository.SettlementRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

/**
 * Manages seller settlements — the payouts to sellers after successful
 * buyer payments.
 *
 * ## Settlement flow
 *
 * 1. Buyer payment is confirmed (COMPLETED).
 * 2. [createSettlement] calculates the seller payout (hammer price minus
 *    platform commission) and creates a PENDING settlement.
 * 3. After any applicable escrow holding period, [processSettlement]
 *    initiates the bank transfer to the seller.
 * 4. Once the bank confirms the transfer, the settlement is marked as PAID.
 *
 * ## Commission structure
 *
 * The platform takes a commission on the hammer price. The default rate
 * is 10%, configurable per seller agreement.
 */
@ApplicationScoped
class SettlementService @Inject constructor(
    private val settlementRepository: SettlementRepository,
    private val paymentRepository: PaymentRepository
) {

    private val logger = LoggerFactory.getLogger(SettlementService::class.java)

    companion object {
        /** Default platform commission rate (10% of hammer price). */
        val DEFAULT_COMMISSION_RATE: BigDecimal = BigDecimal("0.10")
    }

    /**
     * Creates a settlement record for the seller after a buyer payment
     * is confirmed.
     *
     * The net amount paid to the seller is:
     *   netAmount = hammerPrice - (hammerPrice * commissionRate)
     *
     * @param paymentId The completed payment UUID.
     * @return The created settlement, or null if the payment was not found
     *         or is not in COMPLETED status.
     */
    fun createSettlement(paymentId: UUID): Settlement? {
        val payment = paymentRepository.findById(paymentId)
        if (payment == null) {
            logger.warn("Cannot create settlement: payment {} not found", paymentId)
            return null
        }

        if (payment.status != PaymentStatus.COMPLETED) {
            logger.warn(
                "Cannot create settlement: payment {} is in status {}, expected COMPLETED",
                paymentId, payment.status
            )
            return null
        }

        // Check if settlement already exists for this payment
        val existing = settlementRepository.findByPaymentId(paymentId)
        if (existing != null) {
            logger.warn("Settlement already exists for payment {}: {}", paymentId, existing.id)
            return existing
        }

        val commission = payment.hammerPrice
            .multiply(DEFAULT_COMMISSION_RATE)
            .setScale(2, RoundingMode.HALF_UP)

        val netAmount = payment.hammerPrice
            .subtract(commission)
            .setScale(2, RoundingMode.HALF_UP)

        // The sellerId is not directly on the payment — in a real system, we would
        // look it up from the auction/lot service. For now, we use the auctionId
        // as a placeholder lookup key. In production, an event-driven approach
        // would carry sellerId in the payment creation context.
        val sellerId = resolveSellerId(payment)

        val settlement = Settlement(
            id = UUID.randomUUID(),
            sellerId = sellerId,
            paymentId = paymentId,
            netAmount = netAmount,
            commission = commission,
            commissionRate = DEFAULT_COMMISSION_RATE,
            status = SettlementStatus.PENDING,
            settledAt = null,
            bankReference = null,
            createdAt = Instant.now()
        )

        settlementRepository.save(settlement)

        logger.info(
            "Created settlement {} for seller {} (net={}, commission={}, payment={})",
            settlement.id, sellerId, netAmount, commission, paymentId
        )

        return settlement
    }

    /**
     * Initiates a bank transfer for a pending settlement.
     *
     * Transitions the settlement from PENDING to PROCESSING, then calls
     * the banking integration to initiate the wire transfer.
     *
     * @param settlementId The settlement UUID.
     * @return The updated settlement, or null if not found.
     * @throws IllegalStateException if the settlement is not in PENDING status.
     */
    fun processSettlement(settlementId: UUID): Settlement? {
        val settlement = settlementRepository.findById(settlementId)
        if (settlement == null) {
            logger.warn("Cannot process settlement: {} not found", settlementId)
            return null
        }

        check(settlement.status == SettlementStatus.PENDING) {
            "Settlement $settlementId is in status ${settlement.status}, expected PENDING"
        }

        logger.info(
            "Processing settlement {} for seller {} (net={} EUR)",
            settlementId, settlement.sellerId, settlement.netAmount
        )

        // Transition to PROCESSING
        settlementRepository.updateStatus(settlementId, SettlementStatus.PROCESSING)

        // In a real implementation, this would:
        // 1. Call the banking API to initiate SEPA credit transfer
        // 2. The bank confirmation would arrive via callback or polling
        // 3. markSettled() would be called with the bank reference
        //
        // For now, we simulate immediate settlement for development.
        val bankReference = "SEPA-${UUID.randomUUID().toString().take(8).uppercase()}"
        settlementRepository.markSettled(
            id = settlementId,
            settledAt = Instant.now(),
            bankReference = bankReference
        )

        logger.info(
            "Settlement {} paid to seller {} (bankRef={})",
            settlementId, settlement.sellerId, bankReference
        )

        return settlementRepository.findById(settlementId)
    }

    /**
     * Returns all settlements for a given seller.
     *
     * @param sellerId The seller's UUID.
     * @return List of settlements ordered by creation time descending.
     */
    fun getSellerSettlements(sellerId: UUID): List<Settlement> {
        return settlementRepository.findBySellerId(sellerId)
    }

    /**
     * Returns all pending settlements that are ready for processing.
     *
     * @return List of PENDING settlements.
     */
    fun getPendingSettlements(): List<Settlement> {
        return settlementRepository.findByStatus(SettlementStatus.PENDING)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Resolves the seller UUID for a payment.
     *
     * In a full implementation, this would query the lot/auction service
     * to find the seller for the given lot. For the current implementation,
     * we use a deterministic UUID derived from the auction ID as a placeholder.
     */
    private fun resolveSellerId(payment: Payment): UUID {
        // In production: call lot-service or read from a local projection
        // For now, use the auctionId as a namespace to derive a stable seller UUID.
        return payment.auctionId
    }
}
