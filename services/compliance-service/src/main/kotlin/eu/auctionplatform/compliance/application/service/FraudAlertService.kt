package eu.auctionplatform.compliance.application.service

import eu.auctionplatform.commons.dto.PagedResponse
import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.commons.exception.ValidationException
import eu.auctionplatform.commons.util.IdGenerator
import eu.auctionplatform.compliance.domain.model.FraudAlert
import eu.auctionplatform.compliance.domain.model.FraudAlertStatus
import eu.auctionplatform.compliance.domain.model.FraudAlertType
import eu.auctionplatform.compliance.domain.model.FraudSeverity
import eu.auctionplatform.compliance.infrastructure.persistence.repository.FraudAlertRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

/**
 * Application service for fraud alert management.
 *
 * Provides CRUD operations and lifecycle transitions for fraud alerts
 * raised by the platform's fraud detection heuristics. Compliance officers
 * use these operations to triage, investigate, resolve, or dismiss alerts.
 */
@ApplicationScoped
class FraudAlertService {

    @Inject
    lateinit var fraudAlertRepository: FraudAlertRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(FraudAlertService::class.java)
    }

    /**
     * Returns a paginated list of fraud alerts with optional filters.
     *
     * @param severity Optional severity filter.
     * @param status   Optional status filter.
     * @param type     Optional type filter.
     * @param page     Page number (1-based).
     * @param size     Page size.
     * @return Paged response of fraud alerts.
     */
    fun listAlerts(
        severity: FraudSeverity?,
        status: FraudAlertStatus?,
        type: FraudAlertType?,
        page: Int,
        size: Int
    ): PagedResponse<FraudAlert> {
        val effectivePage = page.coerceAtLeast(1)
        val effectiveSize = size.coerceIn(1, 100)

        val items = fraudAlertRepository.findAll(severity, status, type, effectivePage, effectiveSize)
        val total = fraudAlertRepository.count(severity, status, type)

        return PagedResponse(
            items = items,
            total = total,
            page = effectivePage,
            pageSize = effectiveSize
        )
    }

    /**
     * Retrieves a single fraud alert by ID.
     *
     * @param id The fraud alert identifier.
     * @return The fraud alert.
     * @throws NotFoundException if the alert does not exist.
     */
    fun getAlert(id: UUID): FraudAlert {
        return fraudAlertRepository.findById(id)
            ?: throw NotFoundException(
                code = "FRAUD_ALERT_NOT_FOUND",
                message = "Fraud alert with id '$id' not found."
            )
    }

    /**
     * Creates a new fraud alert.
     *
     * @param type        Category of detected fraud pattern.
     * @param severity    Severity level.
     * @param title       Short summary.
     * @param description Detailed description.
     * @param userId      User associated with the suspicious activity.
     * @param lotId       Optional lot involved.
     * @param auctionId   Optional auction involved.
     * @param riskScore   Risk score between 0.0 and 1.0.
     * @param evidence    List of evidence references.
     * @return The newly created fraud alert.
     */
    fun createAlert(
        type: FraudAlertType,
        severity: FraudSeverity,
        title: String,
        description: String,
        userId: UUID,
        lotId: UUID?,
        auctionId: UUID?,
        riskScore: Double,
        evidence: List<String>
    ): FraudAlert {
        if (title.isBlank()) {
            throw ValidationException(field = "title", error = "Fraud alert title must not be blank.")
        }
        if (riskScore < 0.0 || riskScore > 1.0) {
            throw ValidationException(field = "riskScore", error = "Risk score must be between 0.0 and 1.0.")
        }

        val now = Instant.now()
        val alert = FraudAlert(
            id = IdGenerator.generateUUIDv7(),
            type = type,
            severity = severity,
            status = FraudAlertStatus.NEW,
            title = title,
            description = description,
            userId = userId,
            lotId = lotId,
            auctionId = auctionId,
            riskScore = riskScore,
            evidence = evidence,
            detectedAt = now,
            createdAt = now,
            updatedAt = now
        )

        fraudAlertRepository.save(alert)

        LOG.infof("Fraud alert created: id=%s, type=%s, severity=%s, userId=%s", alert.id, type, severity, userId)

        return alert
    }

    /**
     * Transitions a fraud alert to INVESTIGATING status.
     *
     * @param id The fraud alert identifier.
     * @return The updated fraud alert.
     * @throws NotFoundException if the alert does not exist.
     * @throws ValidationException if the alert is not in NEW status.
     */
    fun investigate(id: UUID): FraudAlert {
        val alert = getAlert(id)

        if (alert.status != FraudAlertStatus.NEW) {
            throw ValidationException(
                field = "status",
                error = "Only NEW alerts can be moved to INVESTIGATING. Current status: ${alert.status}."
            )
        }

        fraudAlertRepository.updateStatus(id, FraudAlertStatus.INVESTIGATING, null, null, null)

        LOG.infof("Fraud alert marked as investigating: id=%s", id)

        return alert.markInvestigating()
    }

    /**
     * Resolves a fraud alert with a resolution summary.
     *
     * @param id         The fraud alert identifier.
     * @param resolution Free-text resolution summary.
     * @param resolvedBy The admin user who resolved the alert.
     * @return The updated fraud alert.
     * @throws NotFoundException if the alert does not exist.
     * @throws ValidationException if the alert is already resolved or dismissed.
     */
    fun resolve(id: UUID, resolution: String, resolvedBy: UUID): FraudAlert {
        val alert = getAlert(id)

        if (alert.status == FraudAlertStatus.RESOLVED || alert.status == FraudAlertStatus.DISMISSED) {
            throw ValidationException(
                field = "status",
                error = "Alert is already ${alert.status} and cannot be resolved again."
            )
        }

        if (resolution.isBlank()) {
            throw ValidationException(field = "resolution", error = "Resolution summary must not be blank.")
        }

        val now = Instant.now()
        fraudAlertRepository.updateStatus(id, FraudAlertStatus.RESOLVED, resolution, resolvedBy, now)

        LOG.infof("Fraud alert resolved: id=%s, resolvedBy=%s", id, resolvedBy)

        return alert.resolve(resolution, resolvedBy)
    }

    /**
     * Dismisses a fraud alert as a false positive.
     *
     * @param id         The fraud alert identifier.
     * @param dismissedBy The admin user who dismissed the alert.
     * @return The updated fraud alert.
     * @throws NotFoundException if the alert does not exist.
     * @throws ValidationException if the alert is already resolved or dismissed.
     */
    fun dismiss(id: UUID, dismissedBy: UUID): FraudAlert {
        val alert = getAlert(id)

        if (alert.status == FraudAlertStatus.RESOLVED || alert.status == FraudAlertStatus.DISMISSED) {
            throw ValidationException(
                field = "status",
                error = "Alert is already ${alert.status} and cannot be dismissed."
            )
        }

        val now = Instant.now()
        fraudAlertRepository.updateStatus(id, FraudAlertStatus.DISMISSED, null, dismissedBy, now)

        LOG.infof("Fraud alert dismissed: id=%s, dismissedBy=%s", id, dismissedBy)

        return alert.dismiss(dismissedBy)
    }
}
