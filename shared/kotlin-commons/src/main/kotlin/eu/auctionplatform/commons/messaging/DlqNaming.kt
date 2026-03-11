package eu.auctionplatform.commons.messaging

/**
 * Naming convention for NATS dead-letter queue subjects.
 *
 * All dead-letter messages are published under the `DLQ.` prefix, followed by
 * the originating stream name and a sanitized version of the original subject
 * (dots replaced with underscores to avoid subject hierarchy conflicts).
 *
 * Example: `DLQ.PAYMENT.payment_settlement_settled`
 */
object DlqNaming {
    fun forSubject(streamName: String, subject: String): String =
        "DLQ.$streamName.${subject.replace('.', '_')}"
}
