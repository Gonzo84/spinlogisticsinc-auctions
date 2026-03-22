package eu.auctionplatform.payment.application.service

import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.payment.domain.model.Payment
import eu.auctionplatform.payment.domain.model.PaymentStatus
import eu.auctionplatform.payment.infrastructure.persistence.repository.PaymentRepository
import io.agroal.api.AgroalDataSource
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
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
    private val paymentRepository: PaymentRepository,
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(NonPaymentService::class.java)

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

        LOG.infof("Found %s overdue payment(s) to process", overduePayments.size)

        for (payment in overduePayments) {
            try {
                val result = applyPenalty(payment.id)
                if (result != null) {
                    LOG.infof(
                        "Applied non-payment penalty to payment %s (buyer=%s, forfeit=%s)",
                        result.paymentId, result.buyerId, result.forfeitAmount
                    )
                }
            } catch (ex: Exception) {
                LOG.errorf(
                    ex, "Failed to apply penalty for payment %s: %s",
                    payment.id, ex.message
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
            LOG.warnf("Cannot apply penalty: payment %s not found", paymentId)
            return null
        }

        if (payment.status != PaymentStatus.PENDING) {
            LOG.warnf(
                "Cannot apply penalty: payment %s is in status %s (expected PENDING)",
                paymentId, payment.status
            )
            return null
        }

        LOG.infof(
            "Applying non-payment penalty to payment %s (buyer=%s, lot=%s, total=%s)",
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

        LOG.infof(
            "Non-payment penalty applied: payment=%s, forfeit=%s, blocked=%s, relisted=%s",
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
    // Outbox event publishing
    // -----------------------------------------------------------------------

    /**
     * Blocks a buyer from future bidding by writing a NonPaymentPenaltyEvent
     * to the transactional outbox.
     *
     * The user service consumes this event to set the buyer status to BLOCKED.
     *
     * @return true if the outbox entry was written successfully.
     */
    private fun blockBuyer(buyerId: UUID, paymentId: UUID, forfeitAmount: BigDecimal): Boolean {
        LOG.infof(
            "Publishing NonPaymentPenaltyEvent: buyerId=%s, paymentId=%s, forfeit=%s",
            buyerId, paymentId, forfeitAmount
        )

        val payment = paymentRepository.findById(paymentId)

        val eventPayload = mapOf(
            "eventId" to UUID.randomUUID().toString(),
            "eventType" to "payment.non-payment.penalty",
            "aggregateId" to paymentId.toString(),
            "aggregateType" to "Payment",
            "brand" to "platform",
            "timestamp" to Instant.now().toString(),
            "version" to 1,
            "paymentId" to paymentId.toString(),
            "buyerId" to buyerId.toString(),
            "lotId" to (payment?.lotId?.toString() ?: ""),
            "auctionId" to (payment?.auctionId?.toString() ?: ""),
            "forfeitAmount" to forfeitAmount,
            "currency" to (payment?.currency ?: "USD")
        )

        return try {
            writeOutboxEntry(
                aggregateId = paymentId,
                eventType = "payment.non-payment.penalty",
                natsSubject = "payment.non-payment.penalty",
                payload = JsonMapper.toJson(eventPayload)
            )
            true
        } catch (e: Exception) {
            LOG.errorf(e, "Failed to write NonPaymentPenaltyEvent to outbox: %s", e.message)
            false
        }
    }

    /**
     * Requests relisting of a lot after non-payment by writing a
     * LotRelistRequestedEvent to the transactional outbox.
     *
     * The auction engine or catalog service consumes this event to return
     * the lot to the seller for relisting.
     *
     * @return true if the outbox entry was written successfully.
     */
    private fun requestLotRelist(lotId: UUID, auctionId: UUID, paymentId: UUID): Boolean {
        LOG.infof(
            "Publishing LotRelistRequestedEvent: lotId=%s, auctionId=%s, paymentId=%s",
            lotId, auctionId, paymentId
        )

        val eventPayload = mapOf(
            "eventId" to UUID.randomUUID().toString(),
            "eventType" to "payment.lot.relist-requested",
            "aggregateId" to lotId.toString(),
            "aggregateType" to "Lot",
            "brand" to "platform",
            "timestamp" to Instant.now().toString(),
            "version" to 1,
            "lotId" to lotId.toString(),
            "auctionId" to auctionId.toString(),
            "paymentId" to paymentId.toString(),
            "reason" to "NON_PAYMENT"
        )

        return try {
            writeOutboxEntry(
                aggregateId = lotId,
                eventType = "payment.lot.relist-requested",
                natsSubject = "payment.lot.relist-requested",
                payload = JsonMapper.toJson(eventPayload)
            )
            true
        } catch (e: Exception) {
            LOG.errorf(e, "Failed to write LotRelistRequestedEvent to outbox: %s", e.message)
            false
        }
    }

    /**
     * Inserts a row into the `app.outbox` table.
     */
    private fun writeOutboxEntry(
        aggregateId: UUID,
        eventType: String,
        natsSubject: String,
        payload: String
    ) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO app.outbox (aggregate_id, event_type, payload, nats_subject, created_at)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, aggregateId)
                stmt.setString(2, eventType)
                stmt.setString(3, payload)
                stmt.setString(4, natsSubject)
                stmt.setTimestamp(5, Timestamp.from(Instant.now()))
                stmt.executeUpdate()
            }
        }
    }
}
