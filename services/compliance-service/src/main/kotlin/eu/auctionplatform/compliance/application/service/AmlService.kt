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
 *
 * US compliance context:
 * - BSA (Bank Secrecy Act): requires financial institutions to assist in
 *   detecting and preventing money laundering.
 * - FinCEN (Financial Crimes Enforcement Network): administers BSA, receives
 *   Suspicious Activity Reports (SARs) and Currency Transaction Reports (CTRs).
 * - OFAC (Office of Foreign Assets Control): maintains the SDN (Specially
 *   Designated Nationals) list. All transactions must be screened against the
 *   SDN list before processing.
 */
@ApplicationScoped
class AmlService {

    @Inject
    lateinit var amlScreeningRepository: AmlScreeningRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(AmlService::class.java)

        /** Default AML screening provider — OFAC SDN list (US requirement). */
        private const val DEFAULT_PROVIDER = "OFAC_SDN"

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

    // --- US-specific: OFAC / FinCEN ---

    /**
     * Screens a user against the OFAC Specially Designated Nationals (SDN) list.
     *
     * Required by US sanctions regulations — all transactions must be screened
     * against the SDN list before processing. The screening uses the OFAC_SDN
     * provider and follows the same async completion pattern as [triggerScreening].
     *
     * @param userId The user to screen against the OFAC SDN list.
     * @return The newly created AML screening record (status = PENDING).
     */
    fun screenAgainstOfac(userId: UUID): AmlScreening {
        val screening = AmlScreening(
            id = IdGenerator.generateUUIDv7(),
            userId = userId,
            provider = "OFAC_SDN",
            status = AmlScreeningStatus.PENDING,
            checkId = "ofac_${IdGenerator.generateString().take(12)}"
        )

        amlScreeningRepository.insert(screening)

        LOG.infof(
            "OFAC SDN screening triggered: id=%s, userId=%s, checkId=%s",
            screening.id, userId, screening.checkId
        )

        // Schedule async completion (simulates OFAC list lookup latency)
        scheduleAsyncCompletion(screening.id)

        return screening
    }

    /**
     * Files a Suspicious Activity Report (SAR) with FinCEN for a user.
     *
     * As required by the Bank Secrecy Act (BSA), financial institutions must
     * file SARs with FinCEN when they detect suspicious activity. This creates
     * an AML screening record in FLAGGED status. In production, this would also
     * submit a BSA e-filing to FinCEN.
     *
     * This is functionally equivalent to [fileReport] but uses FinCEN-specific
     * naming and provider code for US regulatory compliance.
     *
     * @param userId  The user being reported.
     * @param details Description of the suspicious activity for the SAR narrative.
     * @return The AML screening record representing the FinCEN SAR.
     */
    fun fileSar(userId: UUID, details: String): AmlScreening {
        val screening = AmlScreening(
            id = IdGenerator.generateUUIDv7(),
            userId = userId,
            provider = "FINCEN_SAR",
            status = AmlScreeningStatus.FLAGGED,
            checkId = "sar_${IdGenerator.generateString().take(12)}",
            completedAt = java.time.Instant.now(),
            riskLevel = "HIGH"
        )

        amlScreeningRepository.insert(screening)

        LOG.warnf(
            "FinCEN SAR filed: id=%s, userId=%s, details=%s",
            screening.id, userId, details.take(100)
        )

        return screening
    }
}
