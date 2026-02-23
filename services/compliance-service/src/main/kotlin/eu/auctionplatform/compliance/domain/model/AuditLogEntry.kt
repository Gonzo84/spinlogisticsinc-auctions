package eu.auctionplatform.compliance.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Immutable domain model representing a single entry in the platform audit log.
 *
 * The audit log is append-only and records significant actions performed by
 * users and system processes. It supports regulatory compliance (GDPR Art. 30,
 * AML record-keeping) and operational forensics.
 *
 * @property id         Unique entry identifier (UUIDv7).
 * @property timestamp  UTC instant when the action occurred.
 * @property userId     The user who performed the action (null for system-initiated actions).
 * @property action     Short action descriptor (e.g. "USER_REGISTERED", "BID_PLACED", "GDPR_ERASURE_REQUESTED").
 * @property entityType The type of entity affected (e.g. "User", "Auction", "Lot").
 * @property entityId   The identifier of the affected entity (null for aggregate actions).
 * @property details    Free-text details or JSON payload with additional context.
 * @property ipAddress  The originating IP address (null for system-initiated actions).
 * @property source     The originating service or subsystem (e.g. "user-service", "auction-engine").
 */
data class AuditLogEntry(
    val id: UUID,
    val timestamp: Instant = Instant.now(),
    val userId: UUID? = null,
    val action: String,
    val entityType: String,
    val entityId: String? = null,
    val details: String? = null,
    val ipAddress: String? = null,
    val source: String
)
