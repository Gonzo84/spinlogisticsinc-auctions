package eu.auctionplatform.catalog.api.v1.resource

import eu.auctionplatform.catalog.api.dto.toResponse
import eu.auctionplatform.catalog.application.service.LotService
import eu.auctionplatform.catalog.infrastructure.persistence.repository.LotImageRepository
import eu.auctionplatform.commons.dto.ApiResponse
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import java.util.UUID

/**
 * Internal REST resource for service-to-service lot lookups.
 *
 * These endpoints are intended for inter-service communication only
 * (e.g., payment-service resolving lot details for checkout).
 * Access is restricted to the `service_account` role obtained via
 * Keycloak client credentials flow.
 */
@Path("/api/v1/internal/lots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class InternalLotResource {

    @Inject
    lateinit var lotService: LotService

    @Inject
    lateinit var lotImageRepository: LotImageRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(InternalLotResource::class.java)
    }

    /**
     * Gets lot detail by lot ID (internal).
     *
     * **GET /api/v1/internal/lots/{id}**
     */
    @GET
    @Path("/{id}")
    @RolesAllowed("service_account")
    fun getLotById(@PathParam("id") id: UUID): Response {
        val (lot, images) = lotService.getLotById(id)
        return Response.ok(ApiResponse.ok(lot.toResponse(images))).build()
    }
}
