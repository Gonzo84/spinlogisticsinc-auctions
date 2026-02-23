package eu.auctionplatform.compliance.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Status of a content report under the Digital Services Act (DSA).
 */
enum class ContentReportStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED,
    DISMISSED
}

/**
 * Immutable domain model representing a content report submitted under the
 * EU Digital Services Act (DSA).
 *
 * Users may report lots or other content that they believe violates platform
 * rules or applicable legislation. Reports are triaged by moderators and
 * tracked for DSA transparency reporting.
 *
 * @property id         Unique report identifier (UUIDv7).
 * @property reporterId The user who submitted the report.
 * @property lotId      The lot being reported (null for non-lot content reports).
 * @property reason     Free-text description of the reason for reporting.
 * @property status     Current report status.
 * @property createdAt  UTC instant when the report was filed.
 * @property resolvedAt UTC instant when the report was resolved or dismissed (null if open).
 */
data class ContentReport(
    val id: UUID,
    val reporterId: UUID,
    val lotId: UUID? = null,
    val reason: String,
    val status: ContentReportStatus = ContentReportStatus.OPEN,
    val createdAt: Instant = Instant.now(),
    val resolvedAt: Instant? = null
) {

    /** Returns a copy transitioned to INVESTIGATING. */
    fun markInvestigating(): ContentReport =
        copy(status = ContentReportStatus.INVESTIGATING)

    /** Returns a copy transitioned to RESOLVED with a resolution timestamp. */
    fun resolve(): ContentReport =
        copy(status = ContentReportStatus.RESOLVED, resolvedAt = Instant.now())

    /** Returns a copy transitioned to DISMISSED with a resolution timestamp. */
    fun dismiss(): ContentReport =
        copy(status = ContentReportStatus.DISMISSED, resolvedAt = Instant.now())
}
