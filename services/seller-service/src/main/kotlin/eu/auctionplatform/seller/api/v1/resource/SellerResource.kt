package eu.auctionplatform.seller.api.v1.resource

import eu.auctionplatform.commons.auth.company
import eu.auctionplatform.commons.auth.country
import eu.auctionplatform.commons.auth.email
import eu.auctionplatform.commons.auth.userId
import eu.auctionplatform.commons.dto.ApiResponse
import eu.auctionplatform.seller.api.v1.dto.Co2ReportResponse
import eu.auctionplatform.seller.api.v1.dto.DashboardResponse
import eu.auctionplatform.seller.api.v1.dto.LotSummaryResponse
import eu.auctionplatform.seller.api.v1.dto.SellerAnalyticsResponse
import eu.auctionplatform.seller.api.v1.dto.SellerProfileResponse
import eu.auctionplatform.seller.api.v1.dto.SellerRegistrationRequest
import eu.auctionplatform.seller.api.v1.dto.SettlementSummaryResponse
import eu.auctionplatform.seller.application.service.SellerService
import eu.auctionplatform.seller.domain.model.SellerProfile
import eu.auctionplatform.seller.infrastructure.persistence.repository.SellerProfileRepository
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import java.util.UUID

/**
 * REST resource for seller operations.
 *
 * Provides endpoints for seller registration, dashboard access, lot management,
 * settlement queries, analytics, and CO2 reporting. All `/me` endpoints resolve
 * the seller identity from the authenticated user's JWT token.
 */
@Path("/api/v1/sellers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class SellerResource {

    @Inject
    lateinit var sellerService: SellerService

    @Inject
    lateinit var sellerProfileRepository: SellerProfileRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(SellerResource::class.java)
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers a new seller profile for the authenticated user.
     *
     * **POST /api/v1/sellers/register**
     *
     * @param authorization The Bearer JWT token.
     * @param request       The seller registration payload.
     * @return 201 Created with the new seller profile.
     */
    @POST
    @Path("/register")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun register(
        @HeaderParam("Authorization") authorization: String,
        request: SellerRegistrationRequest
    ): Response {
        val userId = extractUserId(authorization)
        val profile = sellerService.register(userId, request)

        return Response
            .status(Response.Status.CREATED)
            .entity(ApiResponse.ok(profile.toResponse()))
            .build()
    }

    // -------------------------------------------------------------------------
    // Dashboard
    // -------------------------------------------------------------------------

    /**
     * Retrieves the dashboard KPIs for the authenticated seller.
     *
     * **GET /api/v1/sellers/me/dashboard**
     *
     * @param authorization The Bearer JWT token.
     * @return 200 OK with the dashboard metrics.
     */
    @GET
    @Path("/me/dashboard")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getDashboard(
        @HeaderParam("Authorization") authorization: String
    ): Response {
        val sellerId = resolveSellerIdFromToken(authorization)
        val dashboard = sellerService.getDashboard(sellerId)

        val response = DashboardResponse(
            activeLots = dashboard.activeLots,
            totalBids = dashboard.totalBids,
            totalHammerSales = dashboard.totalHammerSales,
            pendingSettlements = dashboard.pendingSettlements,
            totalSettled = dashboard.totalSettled
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }

    // -------------------------------------------------------------------------
    // Lots
    // -------------------------------------------------------------------------

    /**
     * Retrieves a paginated list of the authenticated seller's lots.
     *
     * **GET /api/v1/sellers/me/lots**
     *
     * @param authorization The Bearer JWT token.
     * @param page          Page number (1-based, default 1).
     * @param size          Page size (default 20).
     * @return 200 OK with a paginated list of lot summaries.
     */
    @GET
    @Path("/me/lots")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getMyLots(
        @HeaderParam("Authorization") authorization: String,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int
    ): Response {
        val sellerId = resolveSellerIdFromToken(authorization)
        val pagedLots = sellerService.getMyLots(sellerId, page, size)

        val responsePage = eu.auctionplatform.commons.dto.PagedResponse(
            items = pagedLots.items.map { lot ->
                LotSummaryResponse(
                    id = lot.id,
                    title = lot.title,
                    status = lot.status,
                    currentBid = lot.currentBid,
                    reservePrice = lot.reservePrice,
                    bidCount = lot.bidCount,
                    closingAt = lot.closingAt,
                    createdAt = lot.createdAt
                )
            },
            total = pagedLots.total,
            page = pagedLots.page,
            pageSize = pagedLots.pageSize
        )

        return Response.ok(ApiResponse.ok(responsePage)).build()
    }

    /**
     * Retrieves a single lot by ID for the authenticated seller.
     *
     * **GET /api/v1/sellers/me/lots/{id}**
     *
     * @param authorization The Bearer JWT token.
     * @param id            The lot identifier.
     * @return 200 OK with the lot details.
     */
    @GET
    @Path("/me/lots/{id}")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getLotById(
        @HeaderParam("Authorization") authorization: String,
        @PathParam("id") id: UUID
    ): Response {
        val sellerId = resolveSellerIdFromToken(authorization)
        val lot = sellerService.getLotById(sellerId, id)

        val response = LotSummaryResponse(
            id = lot.id,
            title = lot.title,
            status = lot.status,
            currentBid = lot.currentBid,
            reservePrice = lot.reservePrice,
            bidCount = lot.bidCount,
            closingAt = lot.closingAt,
            createdAt = lot.createdAt
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }

    // -------------------------------------------------------------------------
    // Settlements
    // -------------------------------------------------------------------------

    /**
     * Retrieves all settlements for the authenticated seller.
     *
     * **GET /api/v1/sellers/me/settlements**
     *
     * @param authorization The Bearer JWT token.
     * @return 200 OK with a list of settlement summaries.
     */
    @GET
    @Path("/me/settlements")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getSettlements(
        @HeaderParam("Authorization") authorization: String
    ): Response {
        val sellerId = resolveSellerIdFromToken(authorization)
        val settlements = sellerService.getSettlements(sellerId)

        val responses = settlements.map { s ->
            SettlementSummaryResponse(
                id = s.id,
                lotId = s.lotId,
                lotTitle = s.lotTitle,
                hammerPrice = s.hammerPrice,
                commission = s.commission,
                netAmount = s.netAmount,
                currency = s.currency,
                status = s.status,
                settledAt = s.settledAt
            )
        }

        return Response.ok(ApiResponse.ok(responses)).build()
    }

    /**
     * Retrieves the invoice URL for a specific settlement.
     *
     * Redirects to the payment-service invoice endpoint. If the payment-service
     * is not available, returns a placeholder URL.
     *
     * **GET /api/v1/sellers/me/settlements/{id}/invoice**
     *
     * @param authorization The Bearer JWT token.
     * @param id            The settlement identifier.
     * @return 200 OK with the invoice URL.
     */
    @GET
    @Path("/me/settlements/{id}/invoice")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getSettlementInvoice(
        @HeaderParam("Authorization") authorization: String,
        @PathParam("id") id: UUID
    ): Response {
        resolveSellerIdFromToken(authorization) // Verify seller exists

        // Redirect to payment-service invoice endpoint
        val invoiceUrl = "http://localhost:8084/api/v1/payments/settlements/$id/invoice"
        val body = mapOf("url" to invoiceUrl, "settlementId" to id.toString())

        return Response.ok(ApiResponse.ok(body)).build()
    }

    /**
     * Retrieves monthly settlement aggregations for the authenticated seller.
     *
     * **GET /api/v1/sellers/me/settlements/monthly**
     *
     * @param authorization The Bearer JWT token.
     * @return 200 OK with monthly settlement aggregations (up to 12 months).
     */
    @GET
    @Path("/me/settlements/monthly")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getMonthlySettlements(
        @HeaderParam("Authorization") authorization: String
    ): Response {
        val sellerId = resolveSellerIdFromToken(authorization)
        val monthlyData = sellerService.getMonthlySettlements(sellerId)

        return Response.ok(ApiResponse.ok(monthlyData)).build()
    }

    /**
     * Retrieves lot status counts for the authenticated seller.
     *
     * Returns a map of status to count, more efficient than fetching
     * all lots and counting client-side.
     *
     * **GET /api/v1/sellers/me/lots/status-counts**
     *
     * @param authorization The Bearer JWT token.
     * @return 200 OK with a map of status to count.
     */
    @GET
    @Path("/me/lots/status-counts")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getLotStatusCounts(
        @HeaderParam("Authorization") authorization: String
    ): Response {
        val sellerId = resolveSellerIdFromToken(authorization)
        val counts = sellerService.getLotStatusCounts(sellerId)

        return Response.ok(ApiResponse.ok(counts)).build()
    }

    // -------------------------------------------------------------------------
    // Analytics
    // -------------------------------------------------------------------------

    /**
     * Retrieves aggregated analytics for the authenticated seller.
     *
     * **GET /api/v1/sellers/me/analytics**
     *
     * @param authorization The Bearer JWT token.
     * @return 200 OK with the seller analytics.
     */
    @GET
    @Path("/me/analytics")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getAnalytics(
        @HeaderParam("Authorization") authorization: String
    ): Response {
        val sellerId = resolveSellerIdFromToken(authorization)
        val analytics = sellerService.getAnalytics(sellerId)

        val response = SellerAnalyticsResponse(
            totalLots = analytics.totalLots,
            totalSold = analytics.totalSold,
            sellThroughRate = analytics.sellThroughRate,
            averageHammerPrice = analytics.averageHammerPrice,
            totalRevenue = analytics.totalRevenue,
            totalCommissionPaid = analytics.totalCommissionPaid,
            topCategories = analytics.topCategories,
            monthlyRevenue = analytics.monthlyRevenue
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }

    // -------------------------------------------------------------------------
    // CO2 Report
    // -------------------------------------------------------------------------

    /**
     * Retrieves the CO2 sustainability report for the authenticated seller.
     *
     * **GET /api/v1/sellers/me/co2-report**
     *
     * @param authorization The Bearer JWT token.
     * @return 200 OK with the CO2 report.
     */
    @GET
    @Path("/me/co2-report")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getCo2Report(
        @HeaderParam("Authorization") authorization: String
    ): Response {
        val sellerId = resolveSellerIdFromToken(authorization)
        val report = sellerService.getCo2Report(sellerId)

        val response = Co2ReportResponse(
            sellerId = report.sellerId,
            totalCo2SavedKg = report.totalCo2SavedKg,
            totalLotsContributed = report.totalLotsContributed,
            averageCo2PerLotKg = report.averageCo2PerLotKg,
            equivalentTreesPlanted = report.equivalentTreesPlanted,
            reportPeriod = report.reportPeriod,
            generatedAt = report.generatedAt
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }

    // -------------------------------------------------------------------------
    // Lot Actions
    // -------------------------------------------------------------------------

    /**
     * Accepts a lot that closed below its reserve price.
     *
     * **POST /api/v1/sellers/me/lots/{id}/accept-below-reserve**
     *
     * @param authorization The Bearer JWT token.
     * @param id            The lot identifier.
     * @return 204 No Content on success.
     */
    @POST
    @Path("/me/lots/{id}/accept-below-reserve")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun acceptBelowReserve(
        @HeaderParam("Authorization") authorization: String,
        @PathParam("id") id: UUID
    ): Response {
        val sellerId = resolveSellerIdFromToken(authorization)
        sellerService.acceptBelowReserve(sellerId, id)

        return Response.noContent().build()
    }

    /**
     * Relists a lot for a new auction.
     *
     * **POST /api/v1/sellers/me/lots/{id}/relist**
     *
     * @param authorization The Bearer JWT token.
     * @param id            The lot identifier.
     * @return 204 No Content on success.
     */
    @POST
    @Path("/me/lots/{id}/relist")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun relistLot(
        @HeaderParam("Authorization") authorization: String,
        @PathParam("id") id: UUID
    ): Response {
        val sellerId = resolveSellerIdFromToken(authorization)
        sellerService.relistLot(sellerId, id)

        return Response.noContent().build()
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the user ID from the Authorization header JWT token.
     */
    private fun extractUserId(authorization: String): UUID {
        val token = authorization.removePrefix("Bearer ").trim()
        return UUID.fromString(token.userId())
    }

    /**
     * Resolves the seller profile ID from the authenticated user's JWT token.
     *
     * Auto-creates a seller profile if one does not yet exist, using JWT claims
     * for company name and country (mirrors user-service's auto-registration).
     */
    private fun resolveSellerIdFromToken(authorization: String): UUID {
        val token = authorization.removePrefix("Bearer ").trim()
        val userId = UUID.fromString(token.userId())
        val companyName = token.company()
        val country = token.country()
        val email = token.email()
        val profile = sellerService.getOrCreateSeller(userId, companyName, country, email)
        return profile.id
    }

    /**
     * Maps a [SellerProfile] domain model to a [SellerProfileResponse] DTO.
     */
    private fun SellerProfile.toResponse(): SellerProfileResponse {
        return SellerProfileResponse(
            id = this.id,
            userId = this.userId,
            companyName = this.companyName,
            registrationNo = this.registrationNo,
            vatId = this.vatId,
            country = this.country,
            status = this.status.name,
            commissionRate = null,
            verifiedAt = this.verifiedAt,
            createdAt = this.createdAt
        )
    }
}
