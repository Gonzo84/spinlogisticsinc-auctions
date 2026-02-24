package eu.auctionplatform.compliance.application.service

import eu.auctionplatform.commons.dto.PagedResponse
import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.commons.exception.ValidationException
import eu.auctionplatform.commons.messaging.NatsPublisher
import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.domain.DomainEvent
import eu.auctionplatform.commons.util.IdGenerator
import eu.auctionplatform.compliance.domain.model.GdprRequest
import eu.auctionplatform.compliance.domain.model.GdprRequestStatus
import eu.auctionplatform.compliance.domain.model.GdprRequestType
import eu.auctionplatform.compliance.infrastructure.persistence.repository.GdprRequestRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Application service for GDPR data-subject request management.
 *
 * Handles the right to data portability (Art. 20) and the right to erasure
 * (Art. 17) by orchestrating request creation, processing, and event
 * publishing for cross-service data deletion.
 */
@ApplicationScoped
class GdprService {

    @Inject
    lateinit var gdprRequestRepository: GdprRequestRepository

    @Inject
    lateinit var natsPublisher: NatsPublisher

    companion object {
        private val logger = LoggerFactory.getLogger(GdprService::class.java)
    }

    /**
     * Creates a new data export request (GDPR Art. 20 -- right to data portability).
     *
     * The request is created in PENDING status and will be processed by a
     * background job that collects data from all services.
     *
     * @param userId The data subject's user identifier.
     * @return The newly created GDPR export request.
     */
    fun requestExport(userId: UUID): GdprRequest {
        val request = GdprRequest(
            id = IdGenerator.generateUUIDv7(),
            userId = userId,
            type = GdprRequestType.EXPORT,
            status = GdprRequestStatus.PENDING,
            reason = "Data portability request (Art. 20 GDPR)",
            requestedAt = Instant.now()
        )

        gdprRequestRepository.insert(request)

        logger.info("GDPR export request created: id={}, userId={}", request.id, userId)

        return request
    }

    /**
     * Creates a new data erasure request (GDPR Art. 17 -- right to be forgotten).
     *
     * The request is created in PENDING status. Actual erasure is triggered
     * separately via [processErasure] to allow for manual review if needed.
     *
     * @param userId The data subject's user identifier.
     * @param reason The reason for the erasure request.
     * @return The newly created GDPR erasure request.
     */
    fun requestErasure(userId: UUID, reason: String): GdprRequest {
        if (reason.isBlank()) {
            throw ValidationException(
                field = "reason",
                error = "Erasure request reason must not be blank."
            )
        }

        val request = GdprRequest(
            id = IdGenerator.generateUUIDv7(),
            userId = userId,
            type = GdprRequestType.ERASURE,
            status = GdprRequestStatus.PENDING,
            reason = reason,
            requestedAt = Instant.now()
        )

        gdprRequestRepository.insert(request)

        logger.info("GDPR erasure request created: id={}, userId={}", request.id, userId)

        return request
    }

    /**
     * Processes a pending erasure request by publishing a cross-service erasure
     * event via NATS.
     *
     * All services that hold personal data for the user must subscribe to the
     * `compliance.gdpr.erasure` subject and delete or anonymize their data.
     *
     * @param requestId The GDPR request identifier.
     * @throws NotFoundException if the request does not exist.
     * @throws ValidationException if the request is not an erasure request or not in PENDING status.
     */
    fun processErasure(requestId: UUID) {
        val request = gdprRequestRepository.findById(requestId)
            ?: throw NotFoundException(
                code = "GDPR_REQUEST_NOT_FOUND",
                message = "GDPR request with id '$requestId' not found."
            )

        if (request.type != GdprRequestType.ERASURE) {
            throw ValidationException(
                field = "type",
                error = "Only ERASURE requests can be processed for erasure. This is a ${request.type} request."
            )
        }

        if (request.status != GdprRequestStatus.PENDING && request.status != GdprRequestStatus.IN_PROGRESS) {
            throw ValidationException(
                field = "status",
                error = "Request is in ${request.status} status and cannot be processed."
            )
        }

        // Mark as in progress
        gdprRequestRepository.updateStatus(requestId, GdprRequestStatus.IN_PROGRESS, null, null)

        // Publish erasure event to NATS for cross-service data deletion
        val event = GdprErasureEvent(
            eventId = IdGenerator.generateString(),
            aggregateId = request.userId.toString(),
            aggregateType = "User",
            brand = "platform",
            timestamp = Instant.now(),
            version = 1L,
            requestId = requestId.toString(),
            userId = request.userId.toString(),
            reason = request.reason
        )

        natsPublisher.publish(NatsSubjects.COMPLIANCE_GDPR_ERASURE, event)

        // Status remains IN_PROGRESS until downstream services confirm data deletion.
        // A scheduled job or acknowledgment consumer should handle marking COMPLETED.

        logger.info("GDPR erasure processed: requestId={}, userId={}", requestId, request.userId)
    }

    /**
     * Returns a paginated list of GDPR requests, optionally filtered by status.
     *
     * @param status Optional status filter.
     * @param page   Page number (1-based).
     * @param size   Page size.
     * @return Paged response of GDPR requests.
     */
    fun getRequests(status: GdprRequestStatus?, page: Int, size: Int): PagedResponse<GdprRequest> {
        val effectivePage = page.coerceAtLeast(1)
        val effectiveSize = size.coerceIn(1, 100)

        return if (status != null) {
            val items = gdprRequestRepository.findByStatus(status, effectivePage, effectiveSize)
            val total = gdprRequestRepository.countByStatus(status)
            PagedResponse(items = items, total = total, page = effectivePage, pageSize = effectiveSize)
        } else {
            val items = gdprRequestRepository.findAllPaged(effectivePage, effectiveSize)
            val total = gdprRequestRepository.countAll()
            PagedResponse(items = items, total = total, page = effectivePage, pageSize = effectiveSize)
        }
    }
}

/**
 * Domain event published when a GDPR erasure is being executed.
 * All services holding personal data for the identified user must respond.
 */
data class GdprErasureEvent(
    override val eventId: String,
    override val aggregateId: String,
    override val aggregateType: String,
    override val brand: String,
    override val timestamp: Instant,
    override val version: Long,
    val requestId: String,
    val userId: String,
    val reason: String
) : DomainEvent {
    override val eventType: String = "GdprErasureEvent"
}
