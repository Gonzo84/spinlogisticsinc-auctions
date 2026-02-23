package eu.auctionplatform.broker.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Represents a lot intake record created by a broker during a site visit.
 *
 * An intake captures the initial details of an asset before it is formally
 * submitted to the catalog as a lot. The broker records specifications,
 * location, pricing guidance, and photographs during the visit.
 */
data class LotIntake(
    val id: UUID,
    val brokerId: UUID,
    val sellerId: UUID,
    val leadId: UUID,
    val title: String,
    val categoryId: UUID,
    val description: String?,
    val specifications: Map<String, Any>?,
    val reservePrice: BigDecimal?,
    val locationAddress: String,
    val locationCountry: String,
    val locationLat: Double?,
    val locationLng: Double?,
    val imageKeys: List<String>,
    val status: IntakeStatus,
    val createdAt: Instant
)

/**
 * Lifecycle status of a lot intake.
 */
enum class IntakeStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED
}
