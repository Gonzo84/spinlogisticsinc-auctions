package eu.auctionplatform.payment.application.service

import eu.auctionplatform.payment.domain.model.Payment
import eu.auctionplatform.payment.domain.model.PaymentStatus
import eu.auctionplatform.payment.infrastructure.persistence.repository.PaymentRepository
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

/**
 * Result of applying a non-payment penalty.
 *
 * @property paymentId The payment that was penalised.
 * @property buyerId The buyer who failed to pay.
 * @property forfeitAmount The penalty amount (25% of total amount).
 * @property userBlocked Whether the user was blocked from further bidding.
 * @property lotRelisted Whether the lot was sent for relisting.
 */
data class PenaltyResult(
    val paymentId: UUID,
    val buyerId: UUID,
    val forfeitAmount: BigDecimal,
    val userBlocked: Boolean,
    val lotRelisted: Boolean
)

/**
 * Handles non-payment detection and penalty enforcement.
 *
 * ## Non-payment policy
 *
 * Buyers are expected to complete payment within the due date (typically
 * 5 business days after winning). If payment is not received by the
 * deadline, the following penalties are applied:
 *
 * 1. **Forfeit**: A 25% penalty of the total amount due.
 * 2. **User block**: The buyer is blocked from bidding on future auctions
 *    until the penalty is resolved.
 * 3. **Lot relist**: The lot is returned to the seller for relisting.
 *
 * A scheduled job ([checkOverduePayments]) runs periodically to detect
 * and process overdue payments automatically.
 */
@ApplicationScoped
class NonPaymentService @Inject constructor(
    private val paymentRepository: PaymentRepository
) {

    private val logger = LoggerFactory.getLogger(NonPaymentService::class.java)

    companion object {
        /** Forfeit rate: 25% of total amount. */
        val FORFEIT_RATE: BigDecimal = BigDecimal("0.25")
    }

    /**
     * Scheduled task that checks for overdue payments and applies penalties.
     *
     * Runs every 15 minutes. Finds all PENDING payments whose due date
     * has passed and applies the non-payment penalty to each.
     */
    @Scheduled(every = "15m", identity = "overdue-payment-checker")
    fun checkOverduePayments() {
        val now = Instant.now()
        val overduePayments = paymentRepository.findOverdue(now)

        if (overduePayments.isEmpty()) {
            return
        }

        logger.info("Found {} overdue payment(s) to process", overduePayments.size)

        for (payment in overduePayments) {
            try {
                val result = applyPenalty(payment.id)
                if (result != null) {
                    logger.info(
                        "Applied non-payment penalty to payment {} (buyer={}, forfeit={})",
                        result.paymentId, result.buyerId, result.forfeitAmount
                    )
                }
            } catch (ex: Exception) {
                logger.error(
                    "Failed to apply penalty for payment {}: {}",
                    payment.id, ex.message, ex
                )
            }
        }
    }

    /**
     * Applies a non-payment penalty to a specific payment.
     *
     * Steps:
     * 1. Mark the payment as FAILED.
     * 2. Calculate the 25% forfeit amount.
     * 3. Block the buyer from future bidding (via event/API call).
     * 4. Request lot relisting (via event/API call).
     *
     * @param paymentId The overdue payment UUID.
     * @return [PenaltyResult] with details of the penalty, or null if
     *         the payment was not found or is not in PENDING status.
     */
    fun applyPenalty(paymentId: UUID): PenaltyResult? {
        val payment = paymentRepository.findById(paymentId)
        if (payment == null) {
            logger.warn("Cannot apply penalty: payment {} not found", paymentId)
            return null
        }

        if (payment.status != PaymentStatus.PENDING) {
            logger.warn(
                "Cannot apply penalty: payment {} is in status {} (expected PENDING)",
                paymentId, payment.status
            )
            return null
        }

        logger.info(
            "Applying non-payment penalty to payment {} (buyer={}, lot={}, total={})",
            paymentId, payment.buyerId, payment.lotId, payment.totalAmount
        )

        // 1. Mark payment as FAILED
        paymentRepository.updateStatus(paymentId, PaymentStatus.FAILED)

        // 2. Calculate forfeit
        val forfeitAmount = payment.totalAmount
            .multiply(FORFEIT_RATE)
            .setScale(2, RoundingMode.HALF_UP)

        // 3. Block buyer from future bidding
        // In production, this would publish an event to the user service:
        //   EventBus.publish("payment.non-payment.penalty", NonPaymentPenaltyEvent(...))
        // The user service would then set the user status to BLOCKED.
        val userBlocked = blockBuyer(payment.buyerId, paymentId, forfeitAmount)

        // 4. Request lot relisting
        // In production, this would publish an event to the auction/lot service:
        //   EventBus.publish("payment.lot.relist-requested", LotRelistRequestedEvent(...))
        val lotRelisted = requestLotRelist(payment.lotId, payment.auctionId, paymentId)

        logger.info(
            "Non-payment penalty applied: payment={}, forfeit={}, blocked={}, relisted={}",
            paymentId, forfeitAmount, userBlocked, lotRelisted
        )

        return PenaltyResult(
            paymentId = paymentId,
            buyerId = payment.buyerId,
            forfeitAmount = forfeitAmount,
            userBlocked = userBlocked,
            lotRelisted = lotRelisted
        )
    }

    // -----------------------------------------------------------------------
    // Integration stubs (to be replaced with event publishing / REST calls)
    // -----------------------------------------------------------------------

    /**
     * Blocks a buyer from future bidding.
     *
     * In production, publishes a "user.blocked" event to the user service
     * via the transactional outbox.
     *
     * @return true if the block request was sent successfully.
     */
    private fun blockBuyer(buyerId: UUID, paymentId: UUID, forfeitAmount: BigDecimal): Boolean {
        logger.info(
            "Requesting buyer block: buyerId={}, paymentId={}, forfeit={}",
            buyerId, paymentId, forfeitAmount
        )
        // TODO: Publish NonPaymentPenaltyEvent to NATS via outbox
        return true
    }

    /**
     * Requests relisting of a lot after non-payment.
     *
     * In production, publishes a "lot.relist-requested" event to the
     * auction engine via the transactional outbox.
     *
     * @return true if the relist request was sent successfully.
     */
    private fun requestLotRelist(lotId: UUID, auctionId: UUID, paymentId: UUID): Boolean {
        logger.info(
            "Requesting lot relist: lotId={}, auctionId={}, paymentId={}",
            lotId, auctionId, paymentId
        )
        // TODO: Publish LotRelistRequestedEvent to NATS via outbox
        return true
    }
}
