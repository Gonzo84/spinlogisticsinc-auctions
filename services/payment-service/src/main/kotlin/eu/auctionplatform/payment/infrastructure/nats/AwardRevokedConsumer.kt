package eu.auctionplatform.payment.infrastructure.nats

import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.payment.domain.model.PaymentStatus
import eu.auctionplatform.payment.infrastructure.persistence.repository.PaymentRepository
import io.nats.client.Connection
import io.nats.client.Message
import io.quarkus.runtime.Startup
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * NATS JetStream consumer that listens for `auction.lot.award-revoked` events
 * and cancels pending payments for the revoked auction.
 *
 * When an award is revoked by an admin, this consumer:
 * 1. Finds all PENDING and PROCESSING payments for the auction.
 * 2. Marks them as FAILED with the revocation reason.
 *
 * Uses the durable consumer name `payment-award-revoked-consumer` to survive
 * restarts and ensure at-least-once delivery.
 */
@ApplicationScoped
@Startup
class AwardRevokedConsumer @Inject constructor(
    private val connection: Connection,
    private val paymentRepository: PaymentRepository
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(AwardRevokedConsumer::class.java)

        private const val STREAM_NAME = "AUCTION"
        private const val DURABLE_NAME = "payment-award-revoked-consumer"
        private const val FILTER_SUBJECT = "auction.lot.award-revoked"

        /** Payment statuses that should be cancelled when an award is revoked. */
        private val CANCELLABLE_STATUSES = listOf(PaymentStatus.PENDING, PaymentStatus.PROCESSING)
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "payment-award-revoked-consumer").apply { isDaemon = true }
    }

    @jakarta.annotation.PostConstruct
    fun init() {
        LOG.info("Starting AwardRevokedConsumer for payment cancellation")
        executor.submit { createConsumer().start() }
    }

    @jakarta.annotation.PreDestroy
    fun shutdown() {
        LOG.info("Shutting down AwardRevokedConsumer")
        executor.shutdownNow()
    }

    private fun createConsumer(): NatsConsumer =
        object : NatsConsumer(
            connection = connection,
            streamName = STREAM_NAME,
            durableName = DURABLE_NAME,
            filterSubject = FILTER_SUBJECT,
            deadLetterSubject = "dlq.payment.award-revoked"
        ) {
            override fun handleMessage(message: Message) {
                handleAwardRevoked(message)
            }
        }

    // -----------------------------------------------------------------------
    // Event handler
    // -----------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun handleAwardRevoked(message: Message) {
        val payload = try {
            val json = String(message.data, Charsets.UTF_8)
            JsonMapper.instance.readValue(json, Map::class.java) as Map<String, Any>
        } catch (ex: Exception) {
            LOG.errorf(ex, "Failed to parse AwardRevokedEvent payload: %s", ex.message)
            return
        }

        val auctionId = payload["aggregateId"]?.toString()
        val reason = payload["reason"]?.toString() ?: "Award revoked"
        val revokedBy = payload["revokedBy"]?.toString()
        val originalWinnerId = payload["originalWinnerId"]?.toString()

        if (auctionId == null) {
            LOG.warn("AwardRevokedEvent missing aggregateId -- skipping")
            return
        }

        LOG.infof(
            "Processing AwardRevokedEvent: auctionId=%s, revokedBy=%s, originalWinnerId=%s, reason=%s",
            auctionId, revokedBy, originalWinnerId, reason
        )

        try {
            val auctionUuid = UUID.fromString(auctionId)

            val payments = paymentRepository.findByAuctionIdAndStatuses(auctionUuid, CANCELLABLE_STATUSES)

            if (payments.isEmpty()) {
                LOG.infof("No cancellable payments found for auctionId=%s", auctionId)
                return
            }

            var cancelledCount = 0
            for (payment in payments) {
                try {
                    paymentRepository.updateStatus(payment.id, PaymentStatus.FAILED)
                    cancelledCount++
                    LOG.infof(
                        "Cancelled payment %s (was %s) for auctionId=%s -- reason: %s",
                        payment.id, payment.status, auctionId, reason
                    )
                } catch (ex: Exception) {
                    LOG.errorf(
                        ex, "Failed to cancel payment %s for auctionId=%s: %s",
                        payment.id, auctionId, ex.message
                    )
                }
            }

            LOG.infof(
                "Award revocation processed: auctionId=%s, payments cancelled=%d/%d",
                auctionId, cancelledCount, payments.size
            )
        } catch (ex: Exception) {
            LOG.errorf(
                ex, "Failed to process AwardRevokedEvent for auctionId=%s: %s",
                auctionId, ex.message
            )
            throw ex // Rethrow to trigger redelivery via NatsConsumer
        }
    }
}
