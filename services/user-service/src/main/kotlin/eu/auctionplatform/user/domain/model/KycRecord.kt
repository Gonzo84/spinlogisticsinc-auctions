package eu.auctionplatform.user.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Immutable domain model representing a Know-Your-Customer verification record.
 *
 * Each KYC record captures a single verification attempt with an external
 * identity verification provider. A user may have multiple records if
 * earlier attempts were rejected and re-initiated.
 *
 * @property id          Unique identifier (UUIDv7).
 * @property userId      The user whose identity is being verified.
 * @property provider    Name of the KYC provider (e.g. "onfido", "sumsub").
 * @property status      Current verification status.
 * @property checkId     External reference identifier from the KYC provider.
 * @property completedAt UTC instant when the verification completed (null if pending).
 */
data class KycRecord(
    val id: UUID,
    val userId: UUID,
    val provider: String,
    val status: KycStatus,
    val checkId: String? = null,
    val completedAt: Instant? = null
)
