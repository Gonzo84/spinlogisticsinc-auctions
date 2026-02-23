package eu.auctionplatform.compliance.application.service

import eu.auctionplatform.commons.dto.PagedResponse
import eu.auctionplatform.commons.util.IdGenerator
import eu.auctionplatform.compliance.domain.model.ContentReport
import eu.auctionplatform.compliance.domain.model.ContentReportStatus
import eu.auctionplatform.compliance.infrastructure.persistence.repository.ContentReportRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
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
 */
@ApplicationScoped
class DsaService {

    @Inject
    lateinit var contentReportRepository: ContentReportRepository

    companion object {
        private val logger = LoggerFactory.getLogger(DsaService::class.java)
    }

    /**
     * Creates a new content report under the DSA notice-and-action mechanism.
     *
     * Any user may report content (e.g. a lot listing) they believe violates
     * platform policies or applicable legislation.
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

        logger.info(
            "Content report filed: id={}, reporterId={}, lotId={}, reason={}",
            report.id, reporterId, lotId, reason.take(80)
        )

        return report
    }

    /**
     * Returns a paginated list of content reports, optionally filtered by status.
     *
     * @param status Optional status filter.
     * @param page   Page number (1-based).
     * @param size   Page size.
     * @return Paged response of content reports.
     */
    fun getReports(status: ContentReportStatus?, page: Int, size: Int): PagedResponse<ContentReport> {
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
     * @return The generated transparency report.
     */
    fun generateTransparencyReport(): TransparencyReport {
        val periodEnd = Instant.now()
        val periodStart = periodEnd.minus(180, ChronoUnit.DAYS)

        val totalReports = contentReportRepository.countInRange(periodStart, periodEnd)
        val statusBreakdown = contentReportRepository.countByStatusInRange(periodStart, periodEnd)

        logger.info(
            "DSA transparency report generated: period={} to {}, totalReports={}",
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
