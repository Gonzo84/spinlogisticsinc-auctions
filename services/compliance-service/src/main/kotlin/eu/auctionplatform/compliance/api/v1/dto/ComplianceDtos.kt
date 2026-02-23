package eu.auctionplatform.compliance.api.v1.dto

import eu.auctionplatform.compliance.domain.model.AmlScreening
import eu.auctionplatform.compliance.domain.model.AmlScreeningStatus
import eu.auctionplatform.compliance.domain.model.AuditLogEntry
import eu.auctionplatform.compliance.domain.model.ContentReport
import eu.auctionplatform.compliance.domain.model.ContentReportStatus
import eu.auctionplatform.compliance.domain.model.GdprRequest
import eu.auctionplatform.compliance.domain.model.GdprRequestStatus
import eu.auctionplatform.compliance.domain.model.GdprRequestType
import eu.auctionplatform.compliance.application.service.TransparencyReport
import java.time.Instant
import java.util.UUID

// =============================================================================
// Request DTOs
// =============================================================================

/**
 * Request payload for GDPR data export.
 */
data class ExportRequest(
    val userId: UUID
)

/**
 * Request payload for GDPR data erasure.
 */
data class ErasureRequest(
    val userId: UUID,
    val reason: String
)

/**
 * Request payload for triggering an AML screening.
 */
data class AmlScreeningRequest(
    val userId: UUID
)

/**
 * Request payload for filing a suspicious activity report.
 */
data class AmlReportRequest(
    val userId: UUID,
    val details: String
)

/**
 * Request payload for filing a DSA content report.
 */
data class ContentReportRequest(
    val reporterId: UUID,
    val lotId: UUID? = null,
    val reason: String
)

// =============================================================================
// Response DTOs
// =============================================================================

/**
 * Response representation of a GDPR data-subject request.
 */
data class GdprRequestResponse(
    val id: UUID,
    val userId: UUID,
    val type: GdprRequestType,
    val status: GdprRequestStatus,
    val reason: String,
    val requestedAt: Instant,
    val completedAt: Instant?,
    val rejectionReason: String?
)

/**
 * Response representation of an AML screening.
 */
data class AmlScreeningResponse(
    val id: UUID,
    val userId: UUID,
    val provider: String,
    val status: AmlScreeningStatus,
    val checkId: String?,
    val completedAt: Instant?,
    val riskLevel: String?
)

/**
 * Response representation of a DSA content report.
 */
data class ContentReportResponse(
    val id: UUID,
    val reporterId: UUID,
    val lotId: UUID?,
    val reason: String,
    val status: ContentReportStatus,
    val createdAt: Instant,
    val resolvedAt: Instant?
)

/**
 * Response representation of a DSA transparency report.
 */
data class TransparencyReportResponse(
    val periodStart: Instant,
    val periodEnd: Instant,
    val totalReports: Long,
    val statusBreakdown: Map<String, Long>,
    val medianResolutionHours: Double?,
    val generatedAt: Instant
)

/**
 * Response representation of an audit log entry.
 */
data class AuditLogEntryResponse(
    val id: UUID,
    val timestamp: Instant,
    val userId: UUID?,
    val action: String,
    val entityType: String,
    val entityId: String?,
    val details: String?,
    val ipAddress: String?,
    val source: String
)

// =============================================================================
// Extension functions for domain -> response mapping
// =============================================================================

/** Converts a [GdprRequest] domain model to a [GdprRequestResponse] DTO. */
fun GdprRequest.toResponse(): GdprRequestResponse = GdprRequestResponse(
    id = id,
    userId = userId,
    type = type,
    status = status,
    reason = reason,
    requestedAt = requestedAt,
    completedAt = completedAt,
    rejectionReason = rejectionReason
)

/** Converts an [AmlScreening] domain model to an [AmlScreeningResponse] DTO. */
fun AmlScreening.toResponse(): AmlScreeningResponse = AmlScreeningResponse(
    id = id,
    userId = userId,
    provider = provider,
    status = status,
    checkId = checkId,
    completedAt = completedAt,
    riskLevel = riskLevel
)

/** Converts a [ContentReport] domain model to a [ContentReportResponse] DTO. */
fun ContentReport.toResponse(): ContentReportResponse = ContentReportResponse(
    id = id,
    reporterId = reporterId,
    lotId = lotId,
    reason = reason,
    status = status,
    createdAt = createdAt,
    resolvedAt = resolvedAt
)

/** Converts a [TransparencyReport] to a [TransparencyReportResponse] DTO. */
fun TransparencyReport.toResponse(): TransparencyReportResponse = TransparencyReportResponse(
    periodStart = periodStart,
    periodEnd = periodEnd,
    totalReports = totalReports,
    statusBreakdown = statusBreakdown,
    medianResolutionHours = medianResolutionHours,
    generatedAt = generatedAt
)

/** Converts an [AuditLogEntry] domain model to an [AuditLogEntryResponse] DTO. */
fun AuditLogEntry.toResponse(): AuditLogEntryResponse = AuditLogEntryResponse(
    id = id,
    timestamp = timestamp,
    userId = userId,
    action = action,
    entityType = entityType,
    entityId = entityId,
    details = details,
    ipAddress = ipAddress,
    source = source
)
