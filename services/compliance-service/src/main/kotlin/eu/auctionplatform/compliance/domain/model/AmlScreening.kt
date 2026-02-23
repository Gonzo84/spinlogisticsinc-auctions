package eu.auctionplatform.compliance.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Status of an Anti-Money Laundering screening check.
 */
enum class AmlScreeningStatus {
    PENDING,
    CLEAR,
    FLAGGED,
    REJECTED
}

/**
 * Immutable domain model representing an AML (Anti-Money Laundering) screening
 * check performed against a user.
 *
 * Screenings are triggered during onboarding or when suspicious activity is
 * detected. They are delegated to an external AML provider (e.g. ComplyAdvantage,
 * Refinitiv) and the result is stored for regulatory retention.
 *
 * @property id          Unique screening identifier (UUIDv7).
 * @property userId      The user being screened.
 * @property provider    Name of the AML screening provider.
 * @property status      Current screening status.
 * @property checkId     External reference ID returned by the AML provider (null until submitted).
 * @property completedAt UTC instant when the screening completed (null if pending).
 * @property riskLevel   Risk classification assigned by the provider (e.g. "LOW", "MEDIUM", "HIGH").
 */
data class AmlScreening(
    val id: UUID,
    val userId: UUID,
    val provider: String,
    val status: AmlScreeningStatus = AmlScreeningStatus.PENDING,
    val checkId: String? = null,
    val completedAt: Instant? = null,
    val riskLevel: String? = null
) {

    /** Returns a copy marked as CLEAR with the given risk level. */
    fun markClear(riskLevel: String): AmlScreening =
        copy(status = AmlScreeningStatus.CLEAR, completedAt = Instant.now(), riskLevel = riskLevel)

    /** Returns a copy marked as FLAGGED with the given risk level. */
    fun markFlagged(riskLevel: String): AmlScreening =
        copy(status = AmlScreeningStatus.FLAGGED, completedAt = Instant.now(), riskLevel = riskLevel)

    /** Returns a copy marked as REJECTED. */
    fun markRejected(riskLevel: String): AmlScreening =
        copy(status = AmlScreeningStatus.REJECTED, completedAt = Instant.now(), riskLevel = riskLevel)
}
