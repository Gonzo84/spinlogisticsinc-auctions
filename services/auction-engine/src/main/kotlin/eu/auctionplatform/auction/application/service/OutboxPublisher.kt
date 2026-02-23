package eu.auctionplatform.auction.application.service

import eu.auctionplatform.auction.infrastructure.persistence.repository.OutboxRepository
import eu.auctionplatform.commons.domain.DomainEvent
import eu.auctionplatform.commons.messaging.NatsPublisher
import eu.auctionplatform.commons.util.JsonMapper
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

/**
 * Background poller that reads unpublished entries from the transactional
 * outbox and publishes them to NATS JetStream.
 *
 * Runs every 100ms to minimize latency between event persistence and
 * external publication. The outbox pattern guarantees at-least-once
 * delivery: events are persisted in the same database transaction as
 * the aggregate state change, and this poller forwards them to NATS
 * independently.
 *
 * ## Retry strategy
 *
 * Failed publish attempts increment the retry counter on the outbox entry.
 * Entries are eligible for retry with exponential backoff based on their
 * retry count:
 * - Attempt 1: immediate retry
 * - Attempt 2: ~1 second delay
 * - Attempt 3: ~2 seconds delay
 * - Attempt 4: ~4 seconds delay
 * - ...up to [MAX_RETRIES]
 *
 * After exceeding [MAX_RETRIES] failed attempts, entries are moved to the
 * dead letter queue (DLQ) for manual investigation by platform operators.
 * DLQ entries are logged at ERROR level for alerting.
 */
@ApplicationScoped
class OutboxPublisher @Inject constructor(
    private val outboxRepository: OutboxRepository,
    private val natsPublisher: NatsPublisher
) {

    private val logger = LoggerFactory.getLogger(OutboxPublisher::class.java)

    companion object {
        /** Maximum number of publish retries before moving to the dead letter queue. */
        private const val MAX_RETRIES = 10

        /** Number of outbox entries to process per poll cycle. */
        private const val BATCH_SIZE = 100

        /** Base delay for exponential backoff (in milliseconds). */
        private const val BASE_BACKOFF_MS = 1000L

        /** Maximum backoff delay (in milliseconds). */
        private const val MAX_BACKOFF_MS = 60_000L
    }

    /**
     * Polls the outbox for unpublished entries and publishes them to NATS.
     *
     * Each entry is processed independently. Successful entries are marked
     * as published; failed entries have their retry counter incremented.
     * Entries that exceed [MAX_RETRIES] are moved to the dead letter queue.
     *
     * Entries with a non-zero retry count are subject to exponential backoff:
     * they are skipped if insufficient time has elapsed since their last
     * modification (approximated by creation time + backoff delay).
     */
    @Scheduled(every = "0.1s", identity = "outbox-publisher")
    fun publishPendingEvents() {
        val pendingEntries = outboxRepository.findPendingEntries(BATCH_SIZE)
        if (pendingEntries.isEmpty()) {
            return
        }

        logger.debug("Processing {} pending outbox entries", pendingEntries.size)

        val now = Instant.now()

        for (entry in pendingEntries) {
            val entryId = entry.id ?: continue

            // Apply exponential backoff for retried entries
            if (entry.retryCount > 0) {
                val backoffMs = calculateBackoff(entry.retryCount)
                val eligibleAfter = entry.createdAt.plusMillis(backoffMs)
                if (now.isBefore(eligibleAfter)) {
                    logger.debug(
                        "Skipping outbox entry {} (retryCount={}, eligible after {})",
                        entryId, entry.retryCount, eligibleAfter
                    )
                    continue
                }
            }

            try {
                // Deserialize the payload back to a DomainEvent for the NatsPublisher
                val event = JsonMapper.instance.readValue(
                    entry.payload,
                    DomainEvent::class.java
                )

                natsPublisher.publish(entry.natsSubject, event)
                outboxRepository.markAsPublished(entryId)

                logger.debug(
                    "Published outbox entry {} (eventType={}, subject={})",
                    entryId, entry.eventType, entry.natsSubject
                )
            } catch (ex: Exception) {
                val newRetryCount = entry.retryCount + 1

                logger.error(
                    "Failed to publish outbox entry {} (eventType={}, retryCount={}/{}): {}",
                    entryId, entry.eventType, newRetryCount, MAX_RETRIES, ex.message, ex
                )

                if (newRetryCount >= MAX_RETRIES) {
                    logger.error(
                        "OUTBOX DLQ: Entry {} exceeded max retries ({}) -- moving to dead letter queue. " +
                            "Event type: {}, subject: {}, aggregate: {}. Manual intervention required.",
                        entryId, MAX_RETRIES, entry.eventType, entry.natsSubject, entry.aggregateId
                    )
                    outboxRepository.moveToDeadLetterQueue(entryId)
                } else {
                    outboxRepository.incrementRetryCount(entryId)
                }
            }
        }
    }

    /**
     * Calculates the backoff delay in milliseconds using exponential backoff
     * with jitter. The formula is: min(BASE * 2^(retryCount-1), MAX_BACKOFF).
     *
     * @param retryCount The current retry count (1-based).
     * @return The delay in milliseconds before the next retry attempt.
     */
    private fun calculateBackoff(retryCount: Int): Long {
        val exponentialDelay = BASE_BACKOFF_MS * (1L shl (retryCount - 1).coerceAtMost(20))
        return exponentialDelay.coerceAtMost(MAX_BACKOFF_MS)
    }
}
