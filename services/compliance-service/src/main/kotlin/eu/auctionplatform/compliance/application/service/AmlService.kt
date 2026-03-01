package eu.auctionplatform.compliance.application.service

import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.commons.util.IdGenerator
import eu.auctionplatform.compliance.domain.model.AmlScreening
import eu.auctionplatform.compliance.domain.model.AmlScreeningStatus
import eu.auctionplatform.compliance.infrastructure.persistence.repository.AmlScreeningRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

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
        private val LOG: Logger = Logger.getLogger(AmlService::class.java)

        /** Default AML screening provider name. */
        private const val DEFAULT_PROVIDER = "ComplyAdvantage"

        /** Delay in seconds before async screening completion (simulates provider latency). */
        private const val ASYNC_COMPLETION_DELAY_SECONDS = 3L
    }

    /**
     * Triggers a new AML screening check for the given user.
     *
     * The screening is created in IN_PROGRESS status and an asynchronous
     * completion is scheduled after a simulated 3-second provider delay.
     * The result will be CLEAR (90% probability) or FLAGGED (10%).
     *
     * @param userId The user to screen.
     * @return The newly created AML screening record (status = IN_PROGRESS).
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

        LOG.infof(
            "AML screening triggered: id=%s, userId=%s, provider=%s, checkId=%s -- scheduling async completion",
            screening.id, userId, screening.provider, screening.checkId
        )

        // Schedule async completion after 3-second simulated provider delay
        scheduleAsyncCompletion(screening.id)

        return screening
    }

    /**
     * Schedules an asynchronous screening completion after a simulated delay.
     *
     * After 3 seconds, the screening is updated to CLEAR (90% probability)
     * or FLAGGED (10% probability) with an appropriate risk level.
     *
     * Uses [CompletableFuture.delayedExecutor] to avoid blocking threads.
     */
    private fun scheduleAsyncCompletion(screeningId: UUID) {
        CompletableFuture.runAsync(
            {
                try {
                    val existing = amlScreeningRepository.findById(screeningId)
                    if (existing == null) {
                        LOG.warnf("AML screening %s not found during async completion", screeningId)
                        return@runAsync
                    }

                    // 90% CLEAR, 10% FLAGGED
                    val isClear = Math.random() < 0.9
                    val updated = if (isClear) {
                        existing.markClear(riskLevel = "LOW")
                    } else {
                        existing.markFlagged(riskLevel = "HIGH")
                    }

                    amlScreeningRepository.update(updated)

                    LOG.infof(
                        "AML screening %s completed asynchronously: status=%s, riskLevel=%s",
                        screeningId, updated.status, updated.riskLevel
                    )
                } catch (ex: Exception) {
                    LOG.errorf(
                        ex, "Failed to complete AML screening %s asynchronously: %s",
                        screeningId, ex.message
                    )
                }
            },
            CompletableFuture.delayedExecutor(ASYNC_COMPLETION_DELAY_SECONDS, TimeUnit.SECONDS)
        )
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

        LOG.warnf(
            "Suspicious Activity Report filed: id=%s, userId=%s, details=%s",
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
