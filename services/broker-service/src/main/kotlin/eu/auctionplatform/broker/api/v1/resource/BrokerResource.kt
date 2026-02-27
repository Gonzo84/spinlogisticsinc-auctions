package eu.auctionplatform.broker.api.v1.resource

import eu.auctionplatform.broker.api.v1.dto.BrokerDashboardResponse
import eu.auctionplatform.broker.api.v1.dto.BulkLotIntakeRequest
import eu.auctionplatform.broker.api.v1.dto.LeadResponse
import eu.auctionplatform.broker.api.v1.dto.LotIntakeRequest
import eu.auctionplatform.broker.api.v1.dto.LotIntakeResponse
import eu.auctionplatform.broker.api.v1.dto.VisitScheduleRequest
import eu.auctionplatform.broker.application.service.BrokerService
import eu.auctionplatform.broker.application.service.LotIntakeInput
import eu.auctionplatform.broker.domain.model.Lead
import eu.auctionplatform.broker.domain.model.LotIntake
import eu.auctionplatform.commons.auth.userId
import eu.auctionplatform.commons.dto.ApiResponse
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * REST resource for broker operations.
 *
 * Provides endpoints for lead management, visit scheduling, bulk lot intake,
 * and broker dashboard retrieval.
 *
 * Authentication is handled by Quarkus OIDC; the bearer token is expected
 * in the `Authorization` header. The Keycloak `sub` claim is used to
 * resolve the broker identity.
 */
@Path("/api/v1/brokers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class BrokerResource {

    @Inject
    lateinit var brokerService: BrokerService

    companion object {
        private val logger = LoggerFactory.getLogger(BrokerResource::class.java)
    }

    // -------------------------------------------------------------------------
    // Lead endpoints
    // -------------------------------------------------------------------------

    /**
     * Returns all leads assigned to the authenticated broker.
     *
     * **GET /api/v1/brokers/leads**
     *
     * @param authorization The Bearer token from the Authorization header.
     * @return 200 OK with the list of leads.
     */
    @GET
    @Path("/leads")
    @RolesAllowed("broker", "admin_ops", "admin_super")
    fun getLeads(@HeaderParam("Authorization") authorization: String): Response {
        val brokerId = extractBrokerId(authorization)
        logger.info("GET /leads for broker={}", brokerId)

        val leads = brokerService.getLeads(brokerId)
        val response = leads.map { it.toResponse() }

        return Response.ok(ApiResponse.ok(response)).build()
    }

    /**
     * Schedules a visit for a specific lead.
     *
     * **POST /api/v1/brokers/leads/{id}/visit**
     *
     * @param id            The lead identifier.
     * @param authorization The Bearer token.
     * @param request       The visit scheduling details.
     * @return 200 OK with the updated lead.
     */
    @POST
    @Path("/leads/{id}/visit")
    @RolesAllowed("broker", "admin_ops", "admin_super")
    fun scheduleVisit(
        @PathParam("id") id: UUID,
        @HeaderParam("Authorization") authorization: String,
        request: VisitScheduleRequest
    ): Response {
        val brokerId = extractBrokerId(authorization)
        logger.info("POST /leads/{}/visit for broker={}, date={}", id, brokerId, request.scheduledDate)

        val updatedLead = brokerService.scheduleVisit(id, request.scheduledDate)

        return Response.ok(ApiResponse.ok(updatedLead.toResponse())).build()
    }

    // -------------------------------------------------------------------------
    // Lot intake endpoints
    // -------------------------------------------------------------------------

    /**
     * Submits a single lot intake for the authenticated broker.
     *
     * **POST /api/v1/brokers/lots/intake**
     *
     * Accepts a single [LotIntakeRequest] without requiring a wrapping object.
     * The broker is used as both the broker and seller (self-intake).
     * The `leadId` field is optional — when omitted, the lot is created
     * without a lead reference (standalone intake).
     *
     * @param authorization The Bearer token.
     * @param request       The lot intake payload.
     * @return 201 Created with the created lot intake.
     */
    @POST
    @Path("/lots/intake")
    @RolesAllowed("broker", "admin_ops", "admin_super")
    fun singleLotIntake(
        @HeaderParam("Authorization") authorization: String,
        request: LotIntakeRequest
    ): Response {
        val brokerId = extractBrokerId(authorization)
        logger.info("POST /lots/intake (single) for broker={}, title={}", brokerId, request.title)

        val input = request.toInput()
        val intakes = brokerService.bulkLotIntake(brokerId, brokerId, listOf(input))
        val response = intakes.first().toResponse()

        return Response
            .status(Response.Status.CREATED)
            .entity(ApiResponse.ok(response))
            .build()
    }

    /**
     * Performs a bulk lot intake operation for the authenticated broker.
     *
     * **POST /api/v1/brokers/lots/bulk-intake**
     *
     * @param authorization The Bearer token.
     * @param request       The bulk intake payload containing seller ID and lot details.
     * @return 201 Created with the list of created lot intakes.
     */
    @POST
    @Path("/lots/bulk-intake")
    @RolesAllowed("broker", "admin_ops", "admin_super")
    fun bulkLotIntake(
        @HeaderParam("Authorization") authorization: String,
        request: BulkLotIntakeRequest
    ): Response {
        val brokerId = extractBrokerId(authorization)
        logger.info("POST /lots/bulk-intake for broker={}, seller={}, count={}",
            brokerId, request.sellerId, request.lots.size)

        val inputs = request.lots.map { it.toInput() }
        val intakes = brokerService.bulkLotIntake(brokerId, request.sellerId, inputs)
        val response = intakes.map { it.toResponse() }

        return Response
            .status(Response.Status.CREATED)
            .entity(ApiResponse.ok(response))
            .build()
    }

    // -------------------------------------------------------------------------
    // Dashboard endpoint
    // -------------------------------------------------------------------------

    /**
     * Returns the authenticated broker's dashboard summary.
     *
     * **GET /api/v1/brokers/me/dashboard**
     *
     * @param authorization The Bearer token.
     * @return 200 OK with the broker dashboard.
     */
    @GET
    @Path("/me/dashboard")
    @RolesAllowed("broker", "admin_ops", "admin_super")
    fun getDashboard(@HeaderParam("Authorization") authorization: String): Response {
        val brokerId = extractBrokerId(authorization)
        logger.info("GET /me/dashboard for broker={}", brokerId)

        val dashboard = brokerService.getDashboard(brokerId)

        val response = BrokerDashboardResponse(
            totalLeads = dashboard.totalLeads,
            newLeads = dashboard.newLeads,
            contactedLeads = dashboard.contactedLeads,
            scheduledVisits = dashboard.scheduledVisits,
            completedVisits = dashboard.completedVisits,
            closedLeads = dashboard.closedLeads,
            totalIntakes = dashboard.totalIntakes,
            draftIntakes = dashboard.draftIntakes,
            submittedIntakes = dashboard.submittedIntakes,
            approvedIntakes = dashboard.approvedIntakes,
            upcomingVisits = dashboard.upcomingVisits.map { it.toResponse() }
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun LotIntakeRequest.toInput(): LotIntakeInput = LotIntakeInput(
        leadId = leadId,
        title = title,
        categoryId = categoryId,
        description = description,
        specifications = specifications,
        reservePrice = reservePrice ?: startingBid,
        locationAddress = resolvedAddress(),
        locationCountry = resolvedCountry(),
        locationLat = locationLat,
        locationLng = locationLng,
        imageKeys = imageKeys
    )

    private fun extractBrokerId(authorization: String): UUID {
        val token = authorization.removePrefix("Bearer ").trim()
        return UUID.fromString(token.userId())
    }

    private fun Lead.toResponse(): LeadResponse = LeadResponse(
        id = id,
        sellerId = sellerId,
        brokerId = brokerId,
        companyName = companyName,
        contactName = contactName,
        contactEmail = contactEmail,
        contactPhone = contactPhone,
        status = status,
        notes = notes,
        scheduledVisitDate = scheduledVisitDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun LotIntake.toResponse(): LotIntakeResponse = LotIntakeResponse(
        id = id,
        brokerId = brokerId,
        sellerId = sellerId,
        leadId = leadId,
        title = title,
        categoryId = categoryId,
        description = description,
        specifications = specifications,
        reservePrice = reservePrice,
        locationAddress = locationAddress,
        locationCountry = locationCountry,
        locationLat = locationLat,
        locationLng = locationLng,
        imageKeys = imageKeys,
        status = status,
        createdAt = createdAt
    )
}
