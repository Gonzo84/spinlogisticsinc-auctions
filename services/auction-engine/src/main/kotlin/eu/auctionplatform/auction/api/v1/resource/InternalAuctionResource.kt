package eu.auctionplatform.auction.api.v1.resource

import eu.auctionplatform.auction.api.dto.AuctionDetailResponse
import eu.auctionplatform.auction.infrastructure.persistence.repository.AuctionReadModel
import eu.auctionplatform.auction.infrastructure.persistence.repository.AuctionReadModelRepository
import eu.auctionplatform.commons.dto.ApiResponse
import eu.auctionplatform.commons.exception.NotFoundException
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
 * Internal REST resource for service-to-service auction lookups.
 *
 * These endpoints are intended for inter-service communication only
 * (e.g., payment-service resolving auction details for checkout).
 * Access is restricted to the `service_account` role obtained via
 * Keycloak client credentials flow.
 */
@Path("/api/v1/internal/auctions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class InternalAuctionResource {

    @Inject
    lateinit var readModelRepository: AuctionReadModelRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(InternalAuctionResource::class.java)
    }

    /**
     * Gets auction detail by auction ID (internal).
     *
     * **GET /api/v1/internal/auctions/{id}**
     */
    @GET
    @Path("/{id}")
    @RolesAllowed("service_account")
    fun getAuction(@PathParam("id") id: String): Response {
        val auctionId = UUID.fromString(id)
        val readModel = readModelRepository.findById(auctionId)
            ?: throw NotFoundException(
                code = "AUCTION_NOT_FOUND",
                message = "Auction $id not found"
            )

        return Response.ok(ApiResponse.ok(toDetailResponse(readModel))).build()
    }

    /**
     * Gets auction detail by lot ID (internal).
     *
     * **GET /api/v1/internal/auctions/by-lot/{lotId}**
     */
    @GET
    @Path("/by-lot/{lotId}")
    @RolesAllowed("service_account")
    fun getAuctionByLot(@PathParam("lotId") lotId: String): Response {
        val lotUuid = UUID.fromString(lotId)
        val readModel = readModelRepository.findByLotId(lotUuid)
            ?: throw NotFoundException(
                code = "AUCTION_NOT_FOUND",
                message = "No auction found for lot $lotId"
            )

        return Response.ok(ApiResponse.ok(toDetailResponse(readModel))).build()
    }

    private fun toDetailResponse(model: AuctionReadModel): AuctionDetailResponse =
        AuctionDetailResponse(
            auctionId = model.auctionId.toString(),
            lotId = model.lotId.toString(),
            brand = model.brand,
            status = model.status,
            startTime = model.startTime,
            endTime = model.endTime,
            originalEndTime = model.originalEndTime,
            startingBid = model.startingBid,
            currentHighBid = model.currentHighBid,
            currentHighBidderId = model.currentHighBidderId?.toString(),
            bidCount = model.bidCount,
            reserveMet = model.reserveMet,
            extensionCount = model.extensionCount,
            sellerId = model.sellerId.toString(),
            featured = model.featured,
            featuredAt = model.featuredAt,
            awardedAt = model.awardedAt,
            autoAwarded = model.autoAwarded,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt
        )
}
