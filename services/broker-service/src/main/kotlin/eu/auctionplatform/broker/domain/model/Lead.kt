package eu.auctionplatform.broker.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Represents a sales lead assigned to a broker.
 *
 * A lead tracks the progression of a potential seller from initial contact
 * through site visit to lot submission. The lifecycle follows:
 * NEW -> CONTACTED -> VISIT_SCHEDULED -> VISIT_COMPLETED -> LOTS_SUBMITTED -> CLOSED
 */
data class Lead(
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
 * Lifecycle status of a broker lead.
 */
enum class LeadStatus {
    NEW,
    CONTACTED,
    VISIT_SCHEDULED,
    VISIT_COMPLETED,
    LOTS_SUBMITTED,
    CLOSED
}
