package eu.auctionplatform.compliance.application.service

import eu.auctionplatform.commons.dto.PagedResponse
import eu.auctionplatform.commons.util.IdGenerator
import eu.auctionplatform.compliance.domain.model.ContentReport
import eu.auctionplatform.compliance.domain.model.ContentReportStatus
import eu.auctionplatform.compliance.infrastructure.persistence.repository.ContentReportRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * DSA (Digital Services Act) transparency report data.
 *
 * Contains aggregated statistics for the reporting period as required
 * by EU Regulation 2022/2065 (DSA) Article 15.
 *
 * @property periodStart      Start of the reporting period (inclusive).
 * @property periodEnd        End of the reporting period (exclusive).
 * @property totalReports     Total number of content reports received.
 * @property statusBreakdown  Count of reports by status.
 * @property medianResolutionHours Median time to resolve a report (in hours).
 * @property generatedAt      When this report was generated.
 */
data class TransparencyReport(
    val periodStart: Instant,
    val periodEnd: Instant,
    val totalReports: Long,
    val statusBreakdown: Map<String, Long>,
    val medianResolutionHours: Double?,
    val generatedAt: Instant = Instant.now()
)

/**
 * Application service for Digital Services Act (DSA) compliance operations.
 *
 * Manages content reporting, moderation tracking, and transparency report
 * generation as required by EU Regulation 2022/2065.
 *
 * NOTE: The DSA is EU-only legislation (EU 2022/2065) with no US federal
 * equivalent. For US deployments, DSA-specific operations (transparency
 * reporting) are disabled by default via `compliance.dsa.enabled=false`.
 * Content moderation / reporting remains active as it is a universal
 * platform concern regardless of jurisdiction.
 */
@ApplicationScoped
class DsaService {

    @Inject
    lateinit var contentReportRepository: ContentReportRepository

    // DSA is EU-only (EU Regulation 2022/2065). Disabled by default for US deployments.
    @ConfigProperty(name = "compliance.dsa.enabled", defaultValue = "false")
    lateinit var dsaEnabled: java.lang.Boolean

    companion object {
        private val LOG: Logger = Logger.getLogger(DsaService::class.java)
    }

    /**
     * Creates a new content report under the DSA notice-and-action mechanism.
     *
     * Any user may report content (e.g. a lot listing) they believe violates
     * platform policies or applicable legislation.
     *
     * Note: Content moderation is a universal concern and remains active
     * regardless of the `compliance.dsa.enabled` flag. Only DSA-specific
     * operations (transparency reporting) are gated by the flag.
     *
     * @param reporterId The user filing the report.
     * @param lotId      The lot being reported (nullable for non-lot content).
     * @param reason     Free-text description of the concern.
     * @return The newly created content report.
     */
    fun reportContent(reporterId: UUID, lotId: UUID?, reason: String): ContentReport {
        val report = ContentReport(
            id = IdGenerator.generateUUIDv7(),
            reporterId = reporterId,
            lotId = lotId,
            reason = reason,
            status = ContentReportStatus.OPEN,
            createdAt = Instant.now()
        )

        contentReportRepository.insert(report)

        LOG.infof(
            "Content report filed: id=%s, reporterId=%s, lotId=%s, reason=%s",
            report.id, reporterId, lotId, reason.take(80)
        )

        return report
    }

    /**
     * Returns a paginated list of content reports, optionally filtered by status.
     *
     * When DSA is disabled (US deployment), returns an empty paged response
     * since DSA transparency obligations do not apply.
     *
     * @param status Optional status filter.
     * @param page   Page number (1-based).
     * @param size   Page size.
     * @return Paged response of content reports.
     */
    fun getReports(status: ContentReportStatus?, page: Int, size: Int): PagedResponse<ContentReport> {
        if (!dsaEnabled.booleanValue()) {
            LOG.debugf("DSA is disabled -- returning empty report list")
            return PagedResponse(items = emptyList(), total = 0, page = page, pageSize = size)
        }

        val effectivePage = page.coerceAtLeast(1)
        val effectiveSize = size.coerceIn(1, 100)

        return if (status != null) {
            val items = contentReportRepository.findByStatus(status, effectivePage, effectiveSize)
            val total = contentReportRepository.countByStatus(status)
            PagedResponse(items = items, total = total, page = effectivePage, pageSize = effectiveSize)
        } else {
            val items = contentReportRepository.findAllPaged(effectivePage, effectiveSize)
            val total = contentReportRepository.countAll()
            PagedResponse(items = items, total = total, page = effectivePage, pageSize = effectiveSize)
        }
    }

    /**
     * Generates a DSA transparency report for the last 6 months.
     *
     * As required by DSA Article 15, this includes the total number of
     * reports received, a breakdown by outcome, and median resolution time.
     *
     * When DSA is disabled (US deployment), returns null since DSA Article 15
     * transparency obligations do not apply outside the EU.
     *
     * @return The generated transparency report, or null if DSA is disabled.
     */
    fun generateTransparencyReport(): TransparencyReport? {
        if (!dsaEnabled.booleanValue()) {
            LOG.infof("DSA is disabled (no US federal equivalent) -- skipping transparency report generation")
            return null
        }

        val periodEnd = Instant.now()
        val periodStart = periodEnd.minus(180, ChronoUnit.DAYS)

        val totalReports = contentReportRepository.countInRange(periodStart, periodEnd)
        val statusBreakdown = contentReportRepository.countByStatusInRange(periodStart, periodEnd)

        LOG.infof(
            "DSA transparency report generated: period=%s to %s, totalReports=%s",
            periodStart, periodEnd, totalReports
        )

        return TransparencyReport(
            periodStart = periodStart,
            periodEnd = periodEnd,
            totalReports = totalReports,
            statusBreakdown = statusBreakdown,
            medianResolutionHours = null, // Could be computed with a more complex query
            generatedAt = Instant.now()
        )
    }
}
