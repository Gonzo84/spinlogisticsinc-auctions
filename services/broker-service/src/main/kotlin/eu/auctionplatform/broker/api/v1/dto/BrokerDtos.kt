package eu.auctionplatform.broker.api.v1.dto

import eu.auctionplatform.broker.domain.model.IntakeStatus
import eu.auctionplatform.broker.domain.model.LeadStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// =============================================================================
// Request DTOs
// =============================================================================

/**
 * Request payload for scheduling a visit on a lead.
 */
data class VisitScheduleRequest(
    val scheduledDate: Instant
)

/**
 * Request payload for a single lot within a bulk intake operation.
 */
data class LotIntakeRequest(
    val leadId: UUID,
    val title: String,
    val categoryId: UUID,
    val description: String? = null,
    val specifications: Map<String, Any>? = null,
    val reservePrice: BigDecimal? = null,
    val locationAddress: String,
    val locationCountry: String,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val imageKeys: List<String> = emptyList()
)

/**
 * Wrapper request for bulk lot intake containing the seller identifier
 * and a list of lots.
 */
data class BulkLotIntakeRequest(
    val sellerId: UUID,
    val lots: List<LotIntakeRequest>
)

// =============================================================================
// Response DTOs
// =============================================================================

/**
 * Response representation of a lead.
 */
data class LeadResponse(
    val id: UUID,
    val sellerId: UUID,
    val brokerId: UUID,
    val companyName: String,
    val contactName: String,
    val contactEmail: String,
    val contactPhone: String?,
    val status: LeadStatus,
    val notes: String?,
    val scheduledVisitDate: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Response representation of a lot intake.
 */
data class LotIntakeResponse(
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
 * Response representation of the broker's dashboard summary.
 */
data class BrokerDashboardResponse(
    val totalLeads: Int,
    val newLeads: Int,
    val contactedLeads: Int,
    val scheduledVisits: Int,
    val completedVisits: Int,
    val closedLeads: Int,
    val totalIntakes: Int,
    val draftIntakes: Int,
    val submittedIntakes: Int,
    val approvedIntakes: Int,
    val upcomingVisits: List<LeadResponse>
)
