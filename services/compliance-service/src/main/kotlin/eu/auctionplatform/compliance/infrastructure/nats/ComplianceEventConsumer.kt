package eu.auctionplatform.compliance.infrastructure.nats

import eu.auctionplatform.commons.messaging.NatsConsumer
import eu.auctionplatform.commons.util.IdGenerator
import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.compliance.domain.model.AuditLogEntry
import eu.auctionplatform.compliance.infrastructure.persistence.repository.AuditLogRepository
import io.nats.client.Connection
import io.nats.client.Message
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * NATS JetStream consumer that subscribes to all significant domain events
 * and logs them to the compliance audit log.
 *
 * Subscribed subjects (via wildcard):
 * - `user.registered.>` -- user registration events
 * - `auction.bid.placed.>` -- bid placement events
 * - `payment.*` -- all payment events
 * - `compliance.gdpr.erasure` -- GDPR erasure processing events
 *
 * The consumer uses a broad wildcard `>` to capture events across all brands
 * and sub-subjects. Each event is parsed and logged as an [AuditLogEntry]
 * for regulatory retention and forensic analysis.
 */
@Singleton
class ComplianceEventConsumer @Inject constructor(
    connection: Connection,
    private val auditLogRepository: AuditLogRepository
) : NatsConsumer(
    connection = connection,
    streamName = "PLATFORM",
    durableName = "compliance-audit-consumer",
    filterSubject = ">",
    maxRedeliveries = 5,
    deadLetterSubject = "compliance.dlq",
    batchSize = 25,
) {

    private val logger = LoggerFactory.getLogger(ComplianceEventConsumer::class.java)

    /**
     * Set of subject prefixes that are considered significant for audit logging.
     * Events not matching any of these prefixes are silently ignored.
     */
    private val auditableSubjects = listOf(
        "user.registered",
        "auction.bid.placed",
        "payment.",
        "compliance.gdpr.erasure"
    )

    override fun handleMessage(message: Message) {
        val subject = message.subject

        // Only process events matching auditable subject patterns
        if (!isAuditable(subject)) {
            return
        }

        try {
            val payload = String(message.data, Charsets.UTF_8)
            val eventData = parseEventData(payload)

            val entry = AuditLogEntry(
                id = IdGenerator.generateUUIDv7(),
                timestamp = Instant.now(),
                userId = extractUserId(eventData),
                action = mapSubjectToAction(subject),
                entityType = extractEntityType(subject),
                entityId = extractEntityId(eventData),
                details = payload,
                ipAddress = null,
                source = extractSource(subject)
            )

            auditLogRepository.insert(entry)

            logger.debug(
                "Audit log entry created from event: subject={}, action={}, entityType={}",
                subject, entry.action, entry.entityType
            )
        } catch (ex: Exception) {
            logger.error("Failed to process audit event on subject {}: {}", subject, ex.message, ex)
            throw ex
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun isAuditable(subject: String): Boolean =
        auditableSubjects.any { subject.startsWith(it) }

    @Suppress("UNCHECKED_CAST")
    private fun parseEventData(payload: String): Map<String, Any?> {
        return try {
            JsonMapper.instance.readValue(payload, Map::class.java) as Map<String, Any?>
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun extractUserId(eventData: Map<String, Any?>): UUID? {
        val userIdStr = eventData["userId"]?.toString()
            ?: eventData["user_id"]?.toString()
            ?: eventData["bidderId"]?.toString()
            ?: eventData["aggregateId"]?.toString()
        return try {
            userIdStr?.let { UUID.fromString(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun mapSubjectToAction(subject: String): String = when {
        subject.startsWith("user.registered") -> "USER_REGISTERED"
        subject.startsWith("auction.bid.placed") -> "BID_PLACED"
        subject.startsWith("payment.checkout.completed") -> "PAYMENT_CHECKOUT_COMPLETED"
        subject.startsWith("payment.settlement.ready") -> "PAYMENT_SETTLEMENT_READY"
        subject.startsWith("payment.deposit.paid") -> "PAYMENT_DEPOSIT_PAID"
        subject.startsWith("payment.") -> "PAYMENT_EVENT"
        subject.startsWith("compliance.gdpr.erasure") -> "GDPR_ERASURE_PROCESSED"
        else -> "UNKNOWN_EVENT"
    }

    private fun extractEntityType(subject: String): String = when {
        subject.startsWith("user.") -> "User"
        subject.startsWith("auction.") -> "Auction"
        subject.startsWith("payment.") -> "Payment"
        subject.startsWith("compliance.") -> "Compliance"
        else -> "Unknown"
    }

    private fun extractEntityId(eventData: Map<String, Any?>): String? =
        eventData["aggregateId"]?.toString()
            ?: eventData["entityId"]?.toString()
            ?: eventData["auctionId"]?.toString()
            ?: eventData["lotId"]?.toString()

    private fun extractSource(subject: String): String = when {
        subject.startsWith("user.") -> "user-service"
        subject.startsWith("auction.") -> "auction-engine"
        subject.startsWith("payment.") -> "payment-service"
        subject.startsWith("compliance.") -> "compliance-service"
        else -> "unknown"
    }
}
