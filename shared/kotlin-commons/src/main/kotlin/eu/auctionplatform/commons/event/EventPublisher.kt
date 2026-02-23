package eu.auctionplatform.commons.event

import eu.auctionplatform.commons.domain.DomainEvent

/**
 * Abstraction for publishing domain events to a messaging infrastructure.
 *
 * Implementations may target NATS JetStream, an in-memory bus (for testing),
 * or a transactional outbox.
 */
interface EventPublisher {

    /**
     * Publishes the [event] synchronously to the given NATS [subject].
     *
     * The call blocks until the broker acknowledges receipt or a timeout /
     * error occurs.
     */
    fun publish(subject: String, event: DomainEvent)

    /**
     * Publishes the [event] asynchronously to the given NATS [subject].
     *
     * The call returns immediately; delivery failures are handled by the
     * implementation (e.g. retry, dead-letter queue).
     */
    fun publishAsync(subject: String, event: DomainEvent)
}
