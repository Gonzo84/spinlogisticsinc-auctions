package eu.auctionplatform.payment.application.service

import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.payment.domain.model.Payment
import eu.auctionplatform.payment.domain.model.PaymentStatus
import eu.auctionplatform.payment.domain.model.Settlement
import eu.auctionplatform.payment.domain.model.SettlementStatus
import eu.auctionplatform.payment.infrastructure.persistence.repository.PaymentRepository
import eu.auctionplatform.payment.infrastructure.persistence.repository.SettlementRepository
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
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
    private val paymentRepository: PaymentRepository,
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(SettlementService::class.java)

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
            LOG.warnf("Cannot create settlement: payment %s not found", paymentId)
            return null
        }

        if (payment.status != PaymentStatus.COMPLETED) {
            LOG.warnf(
                "Cannot create settlement: payment %s is in status %s, expected COMPLETED",
                paymentId, payment.status
            )
            return null
        }

        // Check if settlement already exists for this payment
        val existing = settlementRepository.findByPaymentId(paymentId)
        if (existing != null) {
            LOG.warnf("Settlement already exists for payment %s: %s", paymentId, existing.id)
            return existing
        }

        val commission = payment.hammerPrice
            .multiply(DEFAULT_COMMISSION_RATE)
            .setScale(2, RoundingMode.HALF_UP)

        val netAmount = payment.hammerPrice
            .subtract(commission)
            .setScale(2, RoundingMode.HALF_UP)

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

        LOG.infof(
            "Created settlement %s for seller %s (net=%s, commission=%s, payment=%s)",
            settlement.id, sellerId, netAmount, commission, paymentId
        )

        // Write SettlementReadyEvent to outbox so both webhook and admin
        // settle paths produce the event for downstream consumers.
        // Must not swallow exceptions — if outbox write fails, settlement
        // exists without an event, leaving seller-service out of sync.
        writeSettlementReadyToOutbox(settlement, payment)

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
            LOG.warnf("Cannot process settlement: %s not found", settlementId)
            return null
        }

        check(settlement.status == SettlementStatus.PENDING) {
            "Settlement $settlementId is in status ${settlement.status}, expected PENDING"
        }

        LOG.infof(
            "Processing settlement %s for seller %s (net=%s USD)",
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

        LOG.infof(
            "Settlement %s paid to seller %s (bankRef=%s)",
            settlementId, settlement.sellerId, bankReference
        )

        // Write PaymentSettledEvent to outbox for downstream consumers
        // (seller-service, notification-service, analytics-service).
        val settledSettlement = settlementRepository.findById(settlementId)
        if (settledSettlement != null) {
            writeSettlementSettledToOutbox(settledSettlement, bankReference)
        }

        return settledSettlement
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
     * The seller ID is stored directly on the payment record, populated
     * during checkout from the lot/auction context.
     */
    private fun resolveSellerId(payment: Payment): UUID {
        return payment.sellerId
    }

    // -----------------------------------------------------------------------
    // Outbox event writing
    // -----------------------------------------------------------------------

    /**
     * Writes a SettlementReadyEvent to the outbox table for reliable
     * publication to NATS via the PaymentOutboxPoller.
     *
     * This is called from [createSettlement] so both webhook-triggered
     * and admin-triggered settlement creation paths produce the event.
     */
    private fun writeSettlementReadyToOutbox(settlement: Settlement, payment: Payment) {
        val eventPayload = mapOf(
            "eventId" to UUID.randomUUID().toString(),
            "eventType" to "payment.settlement.ready",
            "aggregateId" to settlement.id.toString(),
            "aggregateType" to "Settlement",
            "brand" to "platform",
            "timestamp" to Instant.now().toString(),
            "version" to 1,
            "settlementId" to settlement.id.toString(),
            "sellerId" to settlement.sellerId.toString(),
            "paymentId" to settlement.paymentId.toString(),
            "netAmount" to settlement.netAmount,
            "commission" to settlement.commission,
            "commissionRate" to settlement.commissionRate,
            "hammerPrice" to payment.hammerPrice,
            "currency" to payment.currency,
            "lotId" to payment.lotId.toString(),
            "lotTitle" to (payment.lotTitle ?: "Lot ${payment.lotId}")
        )

        writeOutboxEntry(
            aggregateId = settlement.id,
            eventType = "payment.settlement.ready",
            natsSubject = "payment.settlement.ready",
            payload = JsonMapper.toJson(eventPayload)
        )

        LOG.infof("Wrote SettlementReadyEvent to outbox for settlement %s", settlement.id)
    }

    /**
     * Writes a PaymentSettledEvent to the outbox table for reliable
     * publication to NATS via the PaymentOutboxPoller.
     *
     * Downstream consumers:
     * - seller-service: updates settlement row status from READY to PAID
     * - notification-service: sends payout confirmation to seller
     * - analytics-service: records commission revenue
     */
    private fun writeSettlementSettledToOutbox(settlement: Settlement, bankReference: String) {
        val eventPayload = mapOf(
            "eventId" to UUID.randomUUID().toString(),
            "eventType" to "payment.settlement.settled",
            "aggregateId" to settlement.id.toString(),
            "aggregateType" to "Settlement",
            "brand" to "platform",
            "timestamp" to Instant.now().toString(),
            "version" to 1,
            "settlementId" to settlement.id.toString(),
            "sellerId" to settlement.sellerId.toString(),
            "paymentId" to settlement.paymentId.toString(),
            "netAmount" to settlement.netAmount,
            "commission" to settlement.commission,
            "commissionRate" to settlement.commissionRate,
            "hammerPrice" to settlement.netAmount.add(settlement.commission),
            "currency" to "USD",
            "bankReference" to bankReference,
            "settledAt" to (settlement.settledAt ?: Instant.now()).toString()
        )

        writeOutboxEntry(
            aggregateId = settlement.id,
            eventType = "payment.settlement.settled",
            natsSubject = "payment.settlement.settled",
            payload = JsonMapper.toJson(eventPayload)
        )

        LOG.infof("Wrote PaymentSettledEvent to outbox for settlement %s", settlement.id)
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
