package eu.auctionplatform.compliance.application.service

import eu.auctionplatform.commons.dto.PagedResponse
import eu.auctionplatform.compliance.domain.model.AuditLogEntry
import eu.auctionplatform.compliance.infrastructure.persistence.repository.AuditLogRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

/**
 * Application service for querying the platform audit log.
 *
 * Provides filtered, paginated access to the append-only audit log for
 * administrators and compliance officers.
 */
@ApplicationScoped
class AuditService {

    @Inject
    lateinit var auditLogRepository: AuditLogRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(AuditService::class.java)
    }

    /**
     * Queries the audit log with optional filters.
     *
     * @param action     Optional filter by action type (e.g. "USER_REGISTERED", "BID_PLACED").
     * @param entityType Optional filter by entity type (e.g. "User", "Auction").
     * @param userId     Optional filter by the acting user.
     * @param source     Optional filter by originating service.
     * @param from       Optional lower bound for timestamp (inclusive).
     * @param to         Optional upper bound for timestamp (exclusive).
     * @param page       Page number (1-based).
     * @param size       Page size (max 100).
     * @return Paged response of audit log entries.
     */
    fun queryLog(
        action: String? = null,
        entityType: String? = null,
        userId: UUID? = null,
        source: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        page: Int = 1,
        size: Int = 50
    ): PagedResponse<AuditLogEntry> {
        val effectivePage = page.coerceAtLeast(1)
        val effectiveSize = size.coerceIn(1, 100)

        val (items, total) = auditLogRepository.query(
            action = action,
            entityType = entityType,
            userId = userId,
            source = source,
            from = from,
            to = to,
            page = effectivePage,
            size = effectiveSize
        )

        LOG.debugf(
            "Audit log queried: action=%s, entityType=%s, userId=%s, results=%s",
            action, entityType, userId, items.size
        )

        return PagedResponse(
            items = items,
            total = total,
            page = effectivePage,
            pageSize = effectiveSize
        )
    }
}
