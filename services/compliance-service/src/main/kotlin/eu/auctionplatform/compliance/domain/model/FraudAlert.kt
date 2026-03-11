package eu.auctionplatform.compliance.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Type of detected fraud pattern.
 */
enum class FraudAlertType {
    SUSPICIOUS_BIDDING,
    SHILL_BIDDING,
    PRICE_MANIPULATION,
    ACCOUNT_TAKEOVER,
    VELOCITY_ANOMALY
}

/**
 * Severity level of a fraud alert.
 */
enum class FraudSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Processing status of a fraud alert.
 */
enum class FraudAlertStatus {
    NEW,
    INVESTIGATING,
    RESOLVED,
    DISMISSED
}

/**
 * Immutable domain model representing a fraud alert raised by the platform's
 * fraud detection heuristics.
 *
 * A fraud alert is created when suspicious activity is detected (e.g. shill
 * bidding, velocity anomalies, account takeover). Compliance officers triage
 * alerts by investigating, resolving, or dismissing them.
 *
 * @property id          Unique alert identifier (UUIDv7).
 * @property type        Category of detected fraud pattern.
 * @property severity    Severity level (LOW through CRITICAL).
 * @property status      Current triage status.
 * @property title       Short summary of the alert.
 * @property description Detailed description including evidence context.
 * @property userId      The user associated with the suspicious activity.
 * @property lotId       Optional lot involved in the alert.
 * @property auctionId   Optional auction involved in the alert.
 * @property riskScore   Machine-generated risk score between 0.0 and 1.0.
 * @property evidence    List of evidence references (IDs, descriptions, etc.).
 * @property resolution  Free-text resolution summary (set when resolved).
 * @property resolvedBy  Admin user who resolved/dismissed the alert.
 * @property resolvedAt  UTC instant when the alert was resolved/dismissed.
 * @property detectedAt  UTC instant when the fraud was initially detected.
 * @property createdAt   UTC instant when the alert record was created.
 * @property updatedAt   UTC instant when the alert was last updated.
 */
data class FraudAlert(
    val id: UUID,
    val type: FraudAlertType,
    val severity: FraudSeverity,
    val status: FraudAlertStatus = FraudAlertStatus.NEW,
    val title: String,
    val description: String,
    val userId: UUID,
    val lotId: UUID? = null,
    val auctionId: UUID? = null,
    val riskScore: Double,
    val evidence: List<String> = emptyList(),
    val resolution: String? = null,
    val resolvedBy: UUID? = null,
    val resolvedAt: Instant? = null,
    val detectedAt: Instant = Instant.now(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {

    /** Returns a copy transitioned to INVESTIGATING status. */
    fun markInvestigating(): FraudAlert =
        copy(status = FraudAlertStatus.INVESTIGATING, updatedAt = Instant.now())

    /** Returns a copy transitioned to RESOLVED with a resolution summary. */
    fun resolve(resolution: String, resolvedBy: UUID): FraudAlert =
        copy(
            status = FraudAlertStatus.RESOLVED,
            resolution = resolution,
            resolvedBy = resolvedBy,
            resolvedAt = Instant.now(),
            updatedAt = Instant.now()
        )

    /** Returns a copy transitioned to DISMISSED (false positive). */
    fun dismiss(resolvedBy: UUID): FraudAlert =
        copy(
            status = FraudAlertStatus.DISMISSED,
            resolvedBy = resolvedBy,
            resolvedAt = Instant.now(),
            updatedAt = Instant.now()
        )
}
