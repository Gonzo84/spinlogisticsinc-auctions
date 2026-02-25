package eu.auctionplatform.catalog.api.v1.resource

import eu.auctionplatform.commons.auth.userId
import eu.auctionplatform.commons.dto.ApiResponse
import eu.auctionplatform.commons.dto.PagedResponse
import eu.auctionplatform.catalog.api.dto.AssignToAuctionRequest
import eu.auctionplatform.catalog.api.dto.CombineLotsRequest
import eu.auctionplatform.catalog.api.dto.CreateLotRequest
import eu.auctionplatform.catalog.api.dto.LotListFilter
import eu.auctionplatform.catalog.api.dto.UpdateLotRequest
import eu.auctionplatform.catalog.api.dto.toResponse
import eu.auctionplatform.catalog.api.dto.toSummaryResponse
import eu.auctionplatform.catalog.application.service.LotService
import eu.auctionplatform.catalog.domain.model.LotStatus
import eu.auctionplatform.catalog.infrastructure.persistence.repository.LotImageRepository
import jakarta.annotation.security.PermitAll
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import java.util.UUID

/**
 * REST resource for lot management operations.
 *
 * Provides CRUD endpoints for sellers to manage their lots, public
 * listing/search endpoints for buyers, and administrative endpoints
 * for platform staff to approve and manage lots.
 */
@Path("/api/v1/lots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class LotResource {

    @Inject
    lateinit var lotService: LotService

    @Inject
    lateinit var lotImageRepository: LotImageRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(LotResource::class.java)
    }

    // -------------------------------------------------------------------------
    // Public endpoints
    // -------------------------------------------------------------------------

    /**
     * Lists lots with optional filtering and pagination.
     *
     * **GET /api/v1/lots**
     *
     * Available filters: brand, categoryId, country, status, sellerId, auctionId.
     * Default page size is 20.
     *
     * @return 200 OK with a paginated list of lot summaries.
     */
    @GET
    @PermitAll
    fun listLots(
        @QueryParam("brand") brand: String?,
        @QueryParam("categoryId") categoryId: UUID?,
        @QueryParam("country") country: String?,
        @QueryParam("status") status: LotStatus?,
        @QueryParam("sellerId") sellerId: UUID?,
        @QueryParam("auctionId") auctionId: UUID?,
        @QueryParam("page") @DefaultValue("0") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int
    ): Response {
        val filter = LotListFilter(
            brand = brand,
            categoryId = categoryId,
            country = country,
            status = status,
            sellerId = sellerId,
            auctionId = auctionId,
            page = page,
            pageSize = pageSize
        )

        val (lots, total) = lotService.listLots(filter)

        val summaries = lots.map { lot ->
            val primaryImage = lotImageRepository.findPrimaryByLotId(lot.id)
            lot.toSummaryResponse(primaryImage?.imageUrl)
        }

        val pagedResponse = PagedResponse(
            items = summaries,
            total = total,
            page = page,
            pageSize = pageSize
        )

        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }

    /**
     * Retrieves a single lot by its identifier, including images.
     *
     * **GET /api/v1/lots/{id}**
     *
     * @param id The lot identifier.
     * @return 200 OK with the full lot details and images.
     */
    @GET
    @Path("/{id}")
    @PermitAll
    fun getLotById(@PathParam("id") id: UUID): Response {
        val (lot, images) = lotService.getLotById(id)

        return Response.ok(ApiResponse.ok(lot.toResponse(images))).build()
    }

    // -------------------------------------------------------------------------
    // Seller endpoints
    // -------------------------------------------------------------------------

    /**
     * Creates a new lot in DRAFT status.
     *
     * **POST /api/v1/lots**
     *
     * @param authorization The Bearer token.
     * @param request       The lot creation payload.
     * @return 201 Created with the new lot.
     */
    @POST
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun createLot(
        @HeaderParam("Authorization") authorization: String,
        request: CreateLotRequest
    ): Response {
        val sellerId = extractUserId(authorization)
        val lot = lotService.createLot(sellerId, request)
        val (savedLot, images) = lotService.getLotById(lot.id)

        return Response
            .status(Response.Status.CREATED)
            .entity(ApiResponse.ok(savedLot.toResponse(images)))
            .build()
    }

    /**
     * Updates an existing lot's fields.
     *
     * Only lots in DRAFT or PENDING_REVIEW status can be updated.
     * The caller must be the lot owner.
     *
     * **PUT /api/v1/lots/{id}**
     *
     * @param id            The lot identifier.
     * @param authorization The Bearer token.
     * @param request       The fields to update.
     * @return 200 OK with the updated lot.
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun updateLot(
        @PathParam("id") id: UUID,
        @HeaderParam("Authorization") authorization: String,
        request: UpdateLotRequest
    ): Response {
        val sellerId = extractUserId(authorization)
        val lot = lotService.updateLot(id, sellerId, request)
        val images = lotImageRepository.findByLotId(id).map { it.toDomain() }

        return Response.ok(ApiResponse.ok(lot.toResponse(images))).build()
    }

    /**
     * Submits a DRAFT lot for review.
     *
     * **POST /api/v1/lots/{id}/submit**
     *
     * @param id            The lot identifier.
     * @param authorization The Bearer token.
     * @return 200 OK with the updated lot.
     */
    @POST
    @Path("/{id}/submit")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun submitForReview(
        @PathParam("id") id: UUID,
        @HeaderParam("Authorization") authorization: String
    ): Response {
        val sellerId = extractUserId(authorization)
        val lot = lotService.submitForReview(id, sellerId)

        return Response.ok(ApiResponse.ok(lot.toResponse())).build()
    }

    /** PUT alias for [submitForReview]. */
    @PUT
    @Path("/{id}/submit")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun submitForReviewPut(
        @PathParam("id") id: UUID,
        @HeaderParam("Authorization") authorization: String
    ): Response = submitForReview(id, authorization)

    /**
     * Withdraws a lot from the catalog.
     *
     * **DELETE /api/v1/lots/{id}**
     *
     * @param id            The lot identifier.
     * @param authorization The Bearer token.
     * @return 200 OK with the withdrawn lot.
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun withdrawLot(
        @PathParam("id") id: UUID,
        @HeaderParam("Authorization") authorization: String
    ): Response {
        val sellerId = extractUserId(authorization)
        val lot = lotService.withdrawLot(id, sellerId)

        return Response.ok(ApiResponse.ok(lot.toResponse())).build()
    }

    /**
     * Combines multiple DRAFT lots into a single lot.
     *
     * **POST /api/v1/lots/combine**
     *
     * @param authorization The Bearer token.
     * @param request       The combination details.
     * @return 201 Created with the new combined lot.
     */
    @POST
    @Path("/combine")
    @RolesAllowed("seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun combineLots(
        @HeaderParam("Authorization") authorization: String,
        request: CombineLotsRequest
    ): Response {
        val sellerId = extractUserId(authorization)
        val lot = lotService.combineLots(sellerId, request)

        return Response
            .status(Response.Status.CREATED)
            .entity(ApiResponse.ok(lot.toResponse()))
            .build()
    }

    // -------------------------------------------------------------------------
    // Admin endpoints
    // -------------------------------------------------------------------------

    /**
     * Approves a lot that is pending review (admin/moderator only).
     *
     * **POST /api/v1/lots/{id}/approve**
     *
     * @param id The lot identifier.
     * @return 200 OK with the approved lot.
     */
    @POST
    @Path("/{id}/approve")
    @RolesAllowed("admin_ops", "admin_super")
    fun approveLot(@PathParam("id") id: UUID): Response {
        val lot = lotService.approveLot(id)

        return Response.ok(ApiResponse.ok(lot.toResponse())).build()
    }

    /** PUT alias for [approveLot]. */
    @PUT
    @Path("/{id}/approve")
    @RolesAllowed("admin_ops", "admin_super")
    fun approveLotPut(@PathParam("id") id: UUID): Response = approveLot(id)

    /**
     * Withdraws a lot by admin (no ownership check).
     *
     * **POST /api/v1/lots/{id}/admin-withdraw**
     *
     * @param id The lot identifier.
     * @return 200 OK with the withdrawn lot.
     */
    @POST
    @Path("/{id}/admin-withdraw")
    @RolesAllowed("admin_ops", "admin_super")
    fun adminWithdrawLot(@PathParam("id") id: UUID): Response {
        val lot = lotService.withdrawLot(id, sellerId = null)

        return Response.ok(ApiResponse.ok(lot.toResponse())).build()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun extractUserId(authorization: String): UUID {
        val token = authorization.removePrefix("Bearer ").trim()
        return UUID.fromString(token.userId())
    }
}
