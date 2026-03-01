package eu.auctionplatform.compliance.api.v1.resource

import eu.auctionplatform.commons.dto.ApiResponse
import eu.auctionplatform.commons.dto.PagedResponse
import eu.auctionplatform.compliance.api.v1.dto.AmlScreeningRequest
import eu.auctionplatform.compliance.api.v1.dto.AmlReportRequest
import eu.auctionplatform.compliance.api.v1.dto.ContentReportRequest
import eu.auctionplatform.compliance.api.v1.dto.ErasureRequest
import eu.auctionplatform.compliance.api.v1.dto.ExportRequest
import eu.auctionplatform.compliance.api.v1.dto.FraudAlertResponse
import eu.auctionplatform.compliance.api.v1.dto.toResponse
import eu.auctionplatform.compliance.application.service.AmlService
import eu.auctionplatform.compliance.application.service.AuditService
import eu.auctionplatform.compliance.application.service.DsaService
import eu.auctionplatform.compliance.application.service.GdprService
import eu.auctionplatform.compliance.domain.model.ContentReportStatus
import eu.auctionplatform.compliance.domain.model.GdprRequestStatus
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

/**
 * REST resource for compliance operations including GDPR, AML, DSA,
 * and audit log queries.
 *
 * All endpoints are secured. GDPR and AML endpoints require authenticated
 * users; audit log and admin-level queries require the `admin` role.
 */
@Path("/api/v1/compliance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ComplianceResource {

    @Inject
    lateinit var gdprService: GdprService

    @Inject
    lateinit var amlService: AmlService

    @Inject
    lateinit var dsaService: DsaService

    @Inject
    lateinit var auditService: AuditService

    companion object {
        private val LOG: Logger = Logger.getLogger(ComplianceResource::class.java)
    }

    // -----------------------------------------------------------------------
    // GDPR endpoints
    // -----------------------------------------------------------------------

    /**
     * Creates a GDPR data export request (Art. 20 right to data portability).
     *
     * **POST /api/v1/compliance/gdpr/export-request**
     */
    @POST
    @Path("/gdpr/export-request")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun requestExport(request: ExportRequest): Response {
        LOG.infof("GDPR export request from userId=%s", request.userId)

        val gdprRequest = gdprService.requestExport(request.userId)

        return Response
            .status(Response.Status.CREATED)
            .entity(ApiResponse.ok(gdprRequest.toResponse()))
            .build()
    }

    /**
     * Creates a GDPR data erasure request (Art. 17 right to erasure).
     *
     * **POST /api/v1/compliance/gdpr/erasure-request**
     */
    @POST
    @Path("/gdpr/erasure-request")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun requestErasure(request: ErasureRequest): Response {
        LOG.infof("GDPR erasure request from userId=%s", request.userId)

        val gdprRequest = gdprService.requestErasure(request.userId, request.reason)

        return Response
            .status(Response.Status.CREATED)
            .entity(ApiResponse.ok(gdprRequest.toResponse()))
            .build()
    }

    /**
     * Lists GDPR requests with optional status filter (admin only).
     *
     * **GET /api/v1/compliance/gdpr/requests**
     */
    @GET
    @Path("/gdpr/requests")
    @RolesAllowed("admin_ops", "admin_super")
    fun getGdprRequests(
        @QueryParam("status") status: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int
    ): Response {
        val statusEnum = status?.let {
            try { GdprRequestStatus.valueOf(it.uppercase()) } catch (_: Exception) { null }
        }

        val pagedResult = gdprService.getRequests(statusEnum, page, size)
        val responseItems = pagedResult.items.map { it.toResponse() }
        val pagedResponse = PagedResponse(
            items = responseItems,
            total = pagedResult.total,
            page = pagedResult.page,
            pageSize = pagedResult.pageSize
        )

        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }

    // -----------------------------------------------------------------------
    // Fraud detection endpoints
    // -----------------------------------------------------------------------

    /**
     * Lists fraud alerts with optional severity, status and type filters (admin only).
     *
     * **GET /api/v1/compliance/fraud/alerts**
     *
     * Returns a paginated list of mock fraud alerts with realistic data
     * until the fraud detection engine is fully integrated.
     */
    @GET
    @Path("/fraud/alerts")
    @RolesAllowed("admin_ops", "admin_super")
    fun getFraudAlerts(
        @QueryParam("severity") severity: String?,
        @QueryParam("status") status: String?,
        @QueryParam("type") type: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int
    ): Response {
        LOG.debugf("GET /fraud/alerts severity=%s status=%s type=%s page=%d size=%d", severity, status, type, page, size)

        // Mock fraud alerts until full fraud detection engine is integrated
        val mockAlerts = listOf(
            FraudAlertResponse(
                id = UUID.fromString("00000000-0000-0000-0000-000000000f01"),
                type = "SHILL_BIDDING",
                severity = "HIGH",
                status = "OPEN",
                userId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                description = "Suspicious bidding pattern detected: same IP address placed 12 bids across 3 related lots within 2 minutes",
                detectedAt = Instant.now().minusSeconds(3600),
                lotId = UUID.fromString("00000000-0000-0000-0000-00000000aa01"),
                riskScore = 0.92
            ),
            FraudAlertResponse(
                id = UUID.fromString("00000000-0000-0000-0000-000000000f02"),
                type = "VELOCITY_ANOMALY",
                severity = "MEDIUM",
                status = "UNDER_REVIEW",
                userId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
                description = "Unusual account activity: 47 bids placed in the last hour, exceeding the 99th percentile for this user segment",
                detectedAt = Instant.now().minusSeconds(7200),
                lotId = null,
                riskScore = 0.71
            ),
            FraudAlertResponse(
                id = UUID.fromString("00000000-0000-0000-0000-000000000f03"),
                type = "PAYMENT_FRAUD",
                severity = "HIGH",
                status = "OPEN",
                userId = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                description = "Multiple failed payment attempts with different card numbers from the same device fingerprint",
                detectedAt = Instant.now().minusSeconds(1800),
                lotId = UUID.fromString("00000000-0000-0000-0000-00000000aa02"),
                riskScore = 0.88
            ),
            FraudAlertResponse(
                id = UUID.fromString("00000000-0000-0000-0000-000000000f04"),
                type = "ACCOUNT_TAKEOVER",
                severity = "LOW",
                status = "RESOLVED",
                userId = UUID.fromString("00000000-0000-0000-0000-000000000004"),
                description = "Login from new geographic region (Romania) after previously only accessing from Netherlands. User verified via 2FA",
                detectedAt = Instant.now().minusSeconds(86400),
                lotId = null,
                riskScore = 0.35
            )
        )

        // Apply filters
        var filtered = mockAlerts
        if (severity != null) {
            filtered = filtered.filter { it.severity.equals(severity, ignoreCase = true) }
        }
        if (status != null) {
            filtered = filtered.filter { it.status.equals(status, ignoreCase = true) }
        }
        if (type != null) {
            filtered = filtered.filter { it.type.equals(type, ignoreCase = true) }
        }

        // Paginate
        val total = filtered.size.toLong()
        val offset = ((page.coerceAtLeast(1)) - 1) * size
        val pagedItems = filtered.drop(offset).take(size)

        val pagedResponse = PagedResponse(
            items = pagedItems,
            total = total,
            page = page,
            pageSize = size
        )

        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }

    // -----------------------------------------------------------------------
    // AML endpoints
    // -----------------------------------------------------------------------

    /**
     * Triggers a new AML screening check.
     *
     * **POST /api/v1/compliance/aml/screening**
     */
    @POST
    @Path("/aml/screening")
    @RolesAllowed("admin_ops", "admin_super")
    fun triggerScreening(request: AmlScreeningRequest): Response {
        LOG.infof("AML screening triggered for userId=%s", request.userId)

        val screening = amlService.triggerScreening(request.userId)

        return Response
            .status(Response.Status.CREATED)
            .entity(ApiResponse.ok(screening.toResponse()))
            .build()
    }

    /**
     * Retrieves the result of an AML screening.
     *
     * **GET /api/v1/compliance/aml/screening/{id}**
     */
    @GET
    @Path("/aml/screening/{id}")
    @RolesAllowed("admin_ops", "admin_super")
    fun getScreeningResult(@PathParam("id") id: UUID): Response {
        val screening = amlService.getScreeningResult(id)

        return Response.ok(ApiResponse.ok(screening.toResponse())).build()
    }

    // -----------------------------------------------------------------------
    // DSA endpoints
    // -----------------------------------------------------------------------

    /**
     * Files a content report under the DSA notice-and-action mechanism.
     *
     * **POST /api/v1/compliance/dsa/content-report**
     */
    @POST
    @Path("/dsa/content-report")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun reportContent(request: ContentReportRequest): Response {
        LOG.infof("DSA content report from reporterId=%s, lotId=%s", request.reporterId, request.lotId)

        val report = dsaService.reportContent(request.reporterId, request.lotId, request.reason)

        return Response
            .status(Response.Status.CREATED)
            .entity(ApiResponse.ok(report.toResponse()))
            .build()
    }

    /**
     * Lists content reports with optional status filter.
     *
     * **GET /api/v1/compliance/dsa/content-reports**
     */
    @GET
    @Path("/dsa/content-reports")
    @RolesAllowed("admin_ops", "admin_super")
    fun getContentReports(
        @QueryParam("status") status: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int
    ): Response {
        val statusEnum = status?.let {
            try { ContentReportStatus.valueOf(it.uppercase()) } catch (_: Exception) { null }
        }

        val pagedResult = dsaService.getReports(statusEnum, page, size)
        val responseItems = pagedResult.items.map { it.toResponse() }
        val pagedResponse = PagedResponse(
            items = responseItems,
            total = pagedResult.total,
            page = pagedResult.page,
            pageSize = pagedResult.pageSize
        )

        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }

    /**
     * Generates a DSA transparency report for the last 6 months.
     *
     * **GET /api/v1/compliance/dsa/transparency-report**
     */
    @GET
    @Path("/dsa/transparency-report")
    @RolesAllowed("admin_ops", "admin_super")
    fun getTransparencyReport(): Response {
        val report = dsaService.generateTransparencyReport()

        return Response.ok(ApiResponse.ok(report.toResponse())).build()
    }

    // -----------------------------------------------------------------------
    // Audit log endpoints
    // -----------------------------------------------------------------------

    /**
     * Queries the platform audit log with optional filters (admin only).
     *
     * **GET /api/v1/compliance/audit/log**
     */
    @GET
    @Path("/audit/log")
    @RolesAllowed("admin_ops", "admin_super")
    fun queryAuditLog(
        @QueryParam("action") action: String?,
        @QueryParam("entityType") entityType: String?,
        @QueryParam("userId") userId: UUID?,
        @QueryParam("source") source: String?,
        @QueryParam("from") from: String?,
        @QueryParam("to") to: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("size") @DefaultValue("50") size: Int
    ): Response {
        val fromInstant = from?.let { Instant.parse(it) }
        val toInstant = to?.let { Instant.parse(it) }

        val pagedResult = auditService.queryLog(
            action = action,
            entityType = entityType,
            userId = userId,
            source = source,
            from = fromInstant,
            to = toInstant,
            page = page,
            size = size
        )

        val responseItems = pagedResult.items.map { it.toResponse() }
        val pagedResponse = PagedResponse(
            items = responseItems,
            total = pagedResult.total,
            page = pagedResult.page,
            pageSize = pagedResult.pageSize
        )

        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }
}
