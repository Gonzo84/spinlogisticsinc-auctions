package eu.auctionplatform.compliance.application.service

import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.commons.util.IdGenerator
import eu.auctionplatform.compliance.domain.model.AmlScreening
import eu.auctionplatform.compliance.domain.model.AmlScreeningStatus
import eu.auctionplatform.compliance.infrastructure.persistence.repository.AmlScreeningRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Application service for Anti-Money Laundering (AML) screening operations.
 *
 * Orchestrates the creation of AML screening checks, integration with external
 * AML providers, and the filing of suspicious activity reports (SARs).
 */
@ApplicationScoped
class AmlService {

    @Inject
    lateinit var amlScreeningRepository: AmlScreeningRepository

    companion object {
        private val logger = LoggerFactory.getLogger(AmlService::class.java)

        /** Default AML screening provider name. */
        private const val DEFAULT_PROVIDER = "ComplyAdvantage"
    }

    /**
     * Triggers a new AML screening check for the given user.
     *
     * The screening is created in PENDING status. In a production environment,
     * this would initiate an asynchronous call to the AML provider API.
     * The result would be updated via a webhook or polling mechanism.
     *
     * @param userId The user to screen.
     * @return The newly created AML screening record.
     */
    fun triggerScreening(userId: UUID): AmlScreening {
        val screening = AmlScreening(
            id = IdGenerator.generateUUIDv7(),
            userId = userId,
            provider = DEFAULT_PROVIDER,
            status = AmlScreeningStatus.PENDING,
            checkId = "chk_${IdGenerator.generateString().take(12)}"
        )

        amlScreeningRepository.insert(screening)

        logger.info(
            "AML screening triggered: id={}, userId={}, provider={}, checkId={}",
            screening.id, userId, screening.provider, screening.checkId
        )

        return screening
    }

    /**
     * Retrieves the result of an AML screening by its identifier.
     *
     * @param screeningId The screening identifier.
     * @return The AML screening record.
     * @throws NotFoundException if no screening exists with the given ID.
     */
    fun getScreeningResult(screeningId: UUID): AmlScreening {
        return amlScreeningRepository.findById(screeningId)
            ?: throw NotFoundException(
                code = "AML_SCREENING_NOT_FOUND",
                message = "AML screening with id '$screeningId' not found."
            )
    }

    /**
     * Files a Suspicious Activity Report (SAR) for a user.
     *
     * Creates an AML screening record in FLAGGED status with the provided
     * details. In a production system, this would also notify the compliance
     * team and potentially the relevant Financial Intelligence Unit (FIU).
     *
     * @param userId  The user being reported.
     * @param details Description of the suspicious activity.
     * @return The AML screening record representing the SAR.
     */
    fun fileReport(userId: UUID, details: String): AmlScreening {
        val screening = AmlScreening(
            id = IdGenerator.generateUUIDv7(),
            userId = userId,
            provider = "INTERNAL_SAR",
            status = AmlScreeningStatus.FLAGGED,
            checkId = "sar_${IdGenerator.generateString().take(12)}",
            completedAt = java.time.Instant.now(),
            riskLevel = "HIGH"
        )

        amlScreeningRepository.insert(screening)

        logger.warn(
            "Suspicious Activity Report filed: id={}, userId={}, details={}",
            screening.id, userId, details.take(100)
        )

        return screening
    }

    /**
     * Retrieves all AML screenings for a given user.
     *
     * @param userId The user identifier.
     * @return List of AML screening records.
     */
    fun getScreeningsByUser(userId: UUID): List<AmlScreening> =
        amlScreeningRepository.findByUserId(userId)
}
