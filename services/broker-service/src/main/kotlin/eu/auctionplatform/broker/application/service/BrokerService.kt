package eu.auctionplatform.broker.application.service

import eu.auctionplatform.broker.domain.model.IntakeStatus
import eu.auctionplatform.broker.domain.model.Lead
import eu.auctionplatform.broker.domain.model.LeadStatus
import eu.auctionplatform.broker.domain.model.LotIntake
import eu.auctionplatform.broker.infrastructure.persistence.repository.LeadRepository
import eu.auctionplatform.broker.infrastructure.persistence.repository.LotIntakeRepository
import eu.auctionplatform.commons.exception.ConflictException
import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.commons.exception.ValidationException
import eu.auctionplatform.commons.util.IdGenerator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Application service that orchestrates broker operations.
 *
 * Coordinates between the REST API layer and the domain/persistence layers,
 * enforcing business rules around lead management, visit scheduling, and
 * bulk lot intake.
 */
@ApplicationScoped
class BrokerService {

    @Inject
    lateinit var leadRepository: LeadRepository

    @Inject
    lateinit var lotIntakeRepository: LotIntakeRepository

    companion object {
        private val logger = LoggerFactory.getLogger(BrokerService::class.java)
    }

    // -------------------------------------------------------------------------
    // Lead management
    // -------------------------------------------------------------------------

    /**
     * Returns all leads assigned to the given broker.
     *
     * @param brokerId The broker's user identifier.
     * @return List of leads belonging to the broker.
     */
    fun getLeads(brokerId: UUID): List<Lead> {
        logger.debug("Fetching leads for broker: {}", brokerId)
        return leadRepository.findByBrokerId(brokerId)
    }

    /**
     * Schedules a visit for a lead, transitioning it to VISIT_SCHEDULED status.
     *
     * The lead must be in NEW or CONTACTED status to schedule a visit.
     *
     * @param leadId The lead identifier.
     * @param date   The scheduled visit date/time.
     * @return The updated lead.
     * @throws NotFoundException if the lead does not exist.
     * @throws ConflictException if the lead is not in a valid state for scheduling.
     */
    fun scheduleVisit(leadId: UUID, date: Instant): Lead {
        val lead = leadRepository.findById(leadId)
            ?: throw NotFoundException(
                code = "LEAD_NOT_FOUND",
                message = "Lead with id '$leadId' not found."
            )

        if (lead.status != LeadStatus.NEW && lead.status != LeadStatus.CONTACTED) {
            throw ConflictException(
                code = "INVALID_LEAD_STATUS",
                message = "Lead '$leadId' cannot be scheduled for a visit (current status: ${lead.status}). " +
                    "Only leads in NEW or CONTACTED status can be scheduled."
            )
        }

        val now = Instant.now()
        leadRepository.updateStatus(leadId, LeadStatus.VISIT_SCHEDULED, date, now)

        logger.info("Visit scheduled for lead: id={}, date={}", leadId, date)

        return lead.copy(
            status = LeadStatus.VISIT_SCHEDULED,
            scheduledVisitDate = date,
            updatedAt = now
        )
    }

    // -------------------------------------------------------------------------
    // Lot intake
    // -------------------------------------------------------------------------

    /**
     * Creates multiple lot intakes in a single batch operation.
     *
     * All intakes are associated with the given broker, seller, and an
     * existing lead. The intakes are created in DRAFT status.
     *
     * @param brokerId The broker's user identifier.
     * @param sellerId The seller's user identifier.
     * @param lots     The list of lot intake data to create.
     * @return The list of created lot intakes.
     */
    fun bulkLotIntake(
        brokerId: UUID,
        sellerId: UUID,
        lots: List<LotIntakeInput>
    ): List<LotIntake> {
        logger.info("Bulk lot intake: broker={}, seller={}, count={}", brokerId, sellerId, lots.size)

        // Validate all referenced lead IDs exist before processing (skip nulls for standalone intakes)
        val uniqueLeadIds = lots.mapNotNull { it.leadId }.distinct()
        val invalidLeadIds = uniqueLeadIds.filter { leadRepository.findById(it) == null }
        if (invalidLeadIds.isNotEmpty()) {
            throw ValidationException(
                field = "leadId",
                error = "Invalid lead IDs: ${invalidLeadIds.joinToString(", ")}"
            )
        }

        val now = Instant.now()
        val intakes = lots.map { input ->
            LotIntake(
                id = IdGenerator.generateUUIDv7(),
                brokerId = brokerId,
                sellerId = sellerId,
                leadId = input.leadId,
                title = input.title,
                categoryId = input.categoryId,
                description = input.description,
                specifications = input.specifications,
                reservePrice = input.reservePrice,
                locationAddress = input.locationAddress,
                locationCountry = input.locationCountry,
                locationLat = input.locationLat,
                locationLng = input.locationLng,
                imageKeys = input.imageKeys,
                status = IntakeStatus.DRAFT,
                createdAt = now
            )
        }

        lotIntakeRepository.bulkInsert(intakes)

        // Update the lead status to LOTS_SUBMITTED if all intakes share the same lead
        for (leadId in uniqueLeadIds) {
            val lead = leadRepository.findById(leadId)
            if (lead != null && lead.status == LeadStatus.VISIT_COMPLETED) {
                leadRepository.updateStatus(leadId, LeadStatus.LOTS_SUBMITTED, lead.scheduledVisitDate, now)
                logger.info("Lead status updated to LOTS_SUBMITTED: id={}", leadId)
            }
        }

        return intakes
    }

    // -------------------------------------------------------------------------
    // Dashboard
    // -------------------------------------------------------------------------

    /**
     * Returns a summary dashboard for the given broker.
     *
     * @param brokerId The broker's user identifier.
     * @return A dashboard containing lead and intake statistics.
     */
    fun getDashboard(brokerId: UUID): BrokerDashboard {
        val leads = leadRepository.findByBrokerId(brokerId)
        val intakes = lotIntakeRepository.findByBrokerId(brokerId)

        val newLeads = leads.count { it.status == LeadStatus.NEW }
        val contactedLeads = leads.count { it.status == LeadStatus.CONTACTED }
        val scheduledVisits = leads.count { it.status == LeadStatus.VISIT_SCHEDULED }
        val completedVisits = leads.count { it.status == LeadStatus.VISIT_COMPLETED }
        val closedLeads = leads.count { it.status == LeadStatus.CLOSED }
        val totalIntakes = intakes.size
        val draftIntakes = intakes.count { it.status == IntakeStatus.DRAFT }
        val submittedIntakes = intakes.count { it.status == IntakeStatus.SUBMITTED }
        val approvedIntakes = intakes.count { it.status == IntakeStatus.APPROVED }

        // Upcoming visits sorted by date
        val upcomingVisits = leads
            .filter { it.status == LeadStatus.VISIT_SCHEDULED && it.scheduledVisitDate != null }
            .sortedBy { it.scheduledVisitDate }

        logger.debug("Dashboard for broker {}: {} leads, {} intakes", brokerId, leads.size, intakes.size)

        return BrokerDashboard(
            totalLeads = leads.size,
            newLeads = newLeads,
            contactedLeads = contactedLeads,
            scheduledVisits = scheduledVisits,
            completedVisits = completedVisits,
            closedLeads = closedLeads,
            totalIntakes = totalIntakes,
            draftIntakes = draftIntakes,
            submittedIntakes = submittedIntakes,
            approvedIntakes = approvedIntakes,
            upcomingVisits = upcomingVisits
        )
    }
}

/**
 * Input data for creating a single lot intake within a bulk operation.
 */
data class LotIntakeInput(
    val leadId: UUID? = null,
    val title: String,
    val categoryId: UUID,
    val description: String? = null,
    val specifications: Map<String, Any>? = null,
    val reservePrice: java.math.BigDecimal? = null,
    val locationAddress: String,
    val locationCountry: String,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val imageKeys: List<String> = emptyList()
)

/**
 * Aggregated dashboard data for a broker.
 */
data class BrokerDashboard(
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
    val upcomingVisits: List<Lead>
)
