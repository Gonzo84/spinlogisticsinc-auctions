package eu.auctionplatform.compliance.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Type of GDPR data-subject request.
 */
enum class GdprRequestType {
    /** Right to data portability (Art. 20 GDPR). */
    EXPORT,
    /** Right to erasure / "right to be forgotten" (Art. 17 GDPR). */
    ERASURE
}

/**
 * Processing status of a GDPR request.
 */
enum class GdprRequestStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    REJECTED
}

/**
 * Immutable domain model representing a GDPR data-subject request.
 *
 * A request is created when a user exercises their rights under the GDPR,
 * either requesting a full data export (Art. 20 portability) or requesting
 * erasure of their personal data (Art. 17 "right to be forgotten").
 *
 * @property id              Unique request identifier (UUIDv7).
 * @property userId          The data subject's internal user identifier.
 * @property type            The type of GDPR request (EXPORT or ERASURE).
 * @property status          Current processing status.
 * @property reason          Free-text reason provided by the data subject.
 * @property requestedAt     UTC instant when the request was submitted.
 * @property completedAt     UTC instant when the request was fulfilled (null if pending).
 * @property rejectionReason Explanation if the request was rejected (null otherwise).
 */
data class GdprRequest(
    val id: UUID,
    val userId: UUID,
    val type: GdprRequestType,
    val status: GdprRequestStatus = GdprRequestStatus.PENDING,
    val reason: String,
    val requestedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val rejectionReason: String? = null
) {

    /** Returns a copy transitioned to IN_PROGRESS. */
    fun markInProgress(): GdprRequest =
        copy(status = GdprRequestStatus.IN_PROGRESS)

    /** Returns a copy transitioned to COMPLETED with a completion timestamp. */
    fun markCompleted(): GdprRequest =
        copy(status = GdprRequestStatus.COMPLETED, completedAt = Instant.now())

    /** Returns a copy transitioned to REJECTED with the given [reason]. */
    fun reject(reason: String): GdprRequest =
        copy(status = GdprRequestStatus.REJECTED, rejectionReason = reason)
}
