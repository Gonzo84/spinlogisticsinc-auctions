package eu.auctionplatform.co2.api.v1.resource

import eu.auctionplatform.co2.api.v1.dto.Co2CalculationResponse
import eu.auctionplatform.co2.api.v1.dto.Co2SummaryResponse
import eu.auctionplatform.co2.api.v1.dto.EmissionFactorResponse
import eu.auctionplatform.co2.api.v1.dto.UpdateEmissionFactorRequest
import eu.auctionplatform.co2.application.service.Co2CalculationService
import eu.auctionplatform.co2.domain.model.Co2Calculation
import eu.auctionplatform.co2.domain.model.EmissionFactor
import eu.auctionplatform.co2.infrastructure.persistence.repository.Co2CalculationRepository
import eu.auctionplatform.commons.dto.ApiResponse
import eu.auctionplatform.commons.exception.NotFoundException
import jakarta.annotation.security.PermitAll
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * REST resource for CO2 avoidance calculations and emission factor management.
 *
 * Provides endpoints for querying lot-level CO2 calculations, platform and
 * seller summaries, and managing the emission factor reference data.
 */
@Path("/api/v1/co2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class Co2Resource {

    @Inject
    lateinit var co2CalculationService: Co2CalculationService

    @Inject
    lateinit var co2CalculationRepository: Co2CalculationRepository

    companion object {
        private val logger = LoggerFactory.getLogger(Co2Resource::class.java)
    }

    // -------------------------------------------------------------------------
    // Lot-level calculations
    // -------------------------------------------------------------------------

    /**
     * Returns the CO2 avoidance calculation for a specific lot.
     *
     * **GET /api/v1/co2/lots/{lotId}**
     *
     * @param lotId The catalog lot identifier.
     * @return 200 OK with the calculation, or 404 if not found.
     */
    @GET
    @Path("/lots/{lotId}")
    @PermitAll
    fun getLotCo2(@PathParam("lotId") lotId: UUID): Response {
        logger.debug("GET /lots/{} CO2 calculation", lotId)

        val calculation = co2CalculationRepository.findByLotId(lotId)
            ?: throw NotFoundException(
                code = "CO2_CALCULATION_NOT_FOUND",
                message = "CO2 calculation for lot '$lotId' not found."
            )

        return Response.ok(ApiResponse.ok(calculation.toResponse())).build()
    }

    // -------------------------------------------------------------------------
    // Summary endpoints
    // -------------------------------------------------------------------------

    /**
     * Returns the platform-wide CO2 avoidance summary.
     *
     * **GET /api/v1/co2/summary**
     *
     * @return 200 OK with the platform summary.
     */
    @GET
    @Path("/summary")
    @PermitAll
    fun getPlatformSummary(): Response {
        logger.debug("GET /summary platform CO2")

        val summary = co2CalculationService.getPlatformSummary()

        val response = Co2SummaryResponse(
            totalCo2AvoidedKg = summary.totalCo2AvoidedKg,
            totalLots = summary.totalLots,
            equivalentTreesPlanted = summary.equivalentTreesPlanted,
            equivalentCarKmAvoided = summary.equivalentCarKmAvoided
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }

    /**
     * Returns the CO2 avoidance summary for a specific seller.
     *
     * **GET /api/v1/co2/sellers/{sellerId}**
     *
     * @param sellerId The seller's user identifier.
     * @return 200 OK with the seller summary.
     */
    @GET
    @Path("/sellers/{sellerId}")
    @PermitAll
    fun getSellerSummary(@PathParam("sellerId") sellerId: UUID): Response {
        logger.debug("GET /sellers/{} CO2 summary", sellerId)

        val summary = co2CalculationService.getSellerSummary(sellerId)

        val response = Co2SummaryResponse(
            totalCo2AvoidedKg = summary.totalCo2AvoidedKg,
            totalLots = summary.totalLots,
            equivalentTreesPlanted = summary.equivalentTreesPlanted,
            equivalentCarKmAvoided = summary.equivalentCarKmAvoided
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }

    // -------------------------------------------------------------------------
    // Emission factor endpoints
    // -------------------------------------------------------------------------

    /**
     * Returns all emission factors.
     *
     * **GET /api/v1/co2/emission-factors**
     *
     * @return 200 OK with the list of emission factors.
     */
    @GET
    @Path("/emission-factors")
    @PermitAll
    fun getEmissionFactors(): Response {
        logger.debug("GET /emission-factors")

        val factors = co2CalculationService.getAllEmissionFactors()
        val response = factors.map { it.toResponse() }

        return Response.ok(ApiResponse.ok(response)).build()
    }

    /**
     * Updates an existing emission factor (admin only).
     *
     * **PUT /api/v1/co2/emission-factors/{id}**
     *
     * @param id      The emission factor identifier.
     * @param request The updated emission factor data.
     * @return 200 OK with the updated emission factor.
     */
    @PUT
    @Path("/emission-factors/{id}")
    @RolesAllowed("admin")
    fun updateEmissionFactor(
        @PathParam("id") id: UUID,
        request: UpdateEmissionFactorRequest
    ): Response {
        logger.info("PUT /emission-factors/{}", id)

        val existing = co2CalculationService.getEmissionFactor(id)
            ?: throw NotFoundException(
                code = "EMISSION_FACTOR_NOT_FOUND",
                message = "Emission factor '$id' not found"
            )

        val factor = EmissionFactor(
            id = id,
            categoryId = existing.categoryId,
            productType = request.productType,
            newManufacturingCo2Kg = request.newManufacturingCo2Kg,
            reuseFactor = request.reuseFactor,
            source = request.source,
            lastUpdated = Instant.now()
        )

        val updated = co2CalculationService.updateEmissionFactor(id, factor)

        return Response.ok(ApiResponse.ok(updated.toResponse())).build()
    }

    // -------------------------------------------------------------------------
    // Mappers
    // -------------------------------------------------------------------------

    private fun Co2Calculation.toResponse(): Co2CalculationResponse = Co2CalculationResponse(
        id = id,
        lotId = lotId,
        categoryId = categoryId,
        co2AvoidedKg = co2AvoidedKg,
        calculatedAt = calculatedAt,
        version = version
    )

    private fun EmissionFactor.toResponse(): EmissionFactorResponse = EmissionFactorResponse(
        id = id,
        categoryId = categoryId,
        productType = productType,
        newManufacturingCo2Kg = newManufacturingCo2Kg,
        reuseFactor = reuseFactor,
        source = source,
        lastUpdated = lastUpdated
    )
}
