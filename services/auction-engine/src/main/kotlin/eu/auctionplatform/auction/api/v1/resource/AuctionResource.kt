package eu.auctionplatform.auction.api.v1.resource

import eu.auctionplatform.auction.api.dto.AuctionDetailResponse
import eu.auctionplatform.auction.api.dto.AuctionSummaryResponse
import eu.auctionplatform.auction.api.dto.AutoBidConfirmationResponse
import eu.auctionplatform.auction.api.dto.BidConfirmationResponse
import eu.auctionplatform.auction.api.dto.BidResponse
import eu.auctionplatform.auction.api.dto.CreateAuctionRequest
import eu.auctionplatform.auction.api.dto.PlaceBidRequest
import eu.auctionplatform.auction.api.dto.SetAutoBidRequest
import eu.auctionplatform.auction.application.service.AuctionLifecycleService
import eu.auctionplatform.auction.application.service.BidService
import eu.auctionplatform.auction.domain.command.CreateAuctionCommand
import eu.auctionplatform.auction.domain.command.PlaceBidCommand
import eu.auctionplatform.auction.domain.command.SetAutoBidCommand
import eu.auctionplatform.auction.infrastructure.persistence.entity.AuctionEventEntity
import eu.auctionplatform.auction.infrastructure.persistence.repository.AuctionReadModel
import eu.auctionplatform.auction.infrastructure.persistence.repository.AuctionReadModelRepository
import eu.auctionplatform.auction.infrastructure.persistence.repository.AuctionEventRepository
import eu.auctionplatform.commons.domain.AuctionId
import eu.auctionplatform.commons.domain.Brand
import eu.auctionplatform.commons.domain.LotId
import eu.auctionplatform.commons.domain.Money
import eu.auctionplatform.commons.domain.UserId
import eu.auctionplatform.commons.dto.ApiResponse
import eu.auctionplatform.commons.dto.PagedResponse
import eu.auctionplatform.commons.exception.ForbiddenException
import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.commons.util.JsonMapper
import io.agroal.api.AgroalDataSource
import jakarta.annotation.security.PermitAll
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.net.URI
import java.sql.Timestamp
import java.util.Currency
import java.util.UUID

/**
 * REST resource for auction management and bidding operations.
 *
 * All endpoints are secured via Casbin-based RBAC policies. The authenticated
 * user's identity and roles are extracted from the JWT token by the Quarkus
 * security subsystem.
 *
 * Response envelope: all successful responses use [ApiResponse] or
 * [PagedResponse] from the shared commons library.
 *
 * ## Endpoints
 *
 * | Method | Path                       | Role              | Description              |
 * |--------|----------------------------|-------------------|--------------------------|
 * | POST   | /                          | admin_ops/super   | Create auction           |
 * | GET    | /{id}                      | (any)             | Get auction detail       |
 * | GET    | /{id}/bids                 | (any)             | Get bid history          |
 * | POST   | /{id}/bids                 | buyer_active      | Place a bid              |
 * | POST   | /{id}/auto-bids            | buyer_active      | Set auto-bid             |
 * | DELETE | /{id}/auto-bids            | buyer_active      | Cancel auto-bid          |
 * | GET    | /                          | (any)             | List auctions (paginated)|
 */
@Path("/api/v1/auctions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class AuctionResource @Inject constructor(
    private val bidService: BidService,
    private val lifecycleService: AuctionLifecycleService,
    private val readModelRepository: AuctionReadModelRepository,
    private val eventRepository: AuctionEventRepository,
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(AuctionResource::class.java)
    }

    // -----------------------------------------------------------------------
    // Auction CRUD
    // -----------------------------------------------------------------------

    /**
     * Creates a new auction.
     *
     * Restricted to admin_ops and admin_super roles.
     *
     * **POST /api/v1/auctions**
     *
     * @param request The auction creation request body.
     * @return 201 Created with the new auction's details.
     */
    @POST
    @RolesAllowed("admin_ops", "admin_super")
    fun createAuction(
        @Valid request: CreateAuctionRequest
    ): Response {
        LOG.infof("Creating auction for lot %s (brand=%s)", request.lotId, request.brand)

        val currency = Currency.getInstance(request.currency)
        val command = CreateAuctionCommand(
            lotId = LotId.fromString(request.lotId),
            brand = Brand.fromCode(request.brand),
            startTime = request.startTime,
            endTime = request.endTime,
            startingBid = Money.of(request.startingBid, currency),
            reservePrice = request.reservePrice?.let { Money.of(it, currency) },
            sellerId = UserId.fromString(request.sellerId)
        )

        val auctionId = lifecycleService.createAuction(command)

        val readModel = readModelRepository.findById(auctionId.value)
        val response = readModel?.let { toDetailResponse(it) }

        return Response
            .created(URI.create("/api/v1/auctions/${auctionId.value}"))
            .entity(ApiResponse.ok(response))
            .build()
    }

    /**
     * Retrieves auction details by ID.
     *
     * **GET /api/v1/auctions/{id}**
     *
     * @param id The auction UUID.
     * @return 200 OK with auction details or 404 if not found.
     */
    @GET
    @Path("/{id}")
    @PermitAll
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
     * Retrieves the bid history for an auction.
     *
     * Returns all BidPlacedEvent and ProxyBidTriggeredEvent entries from the
     * event store for the given auction, ordered chronologically.
     *
     * **GET /api/v1/auctions/{id}/bids**
     *
     * @param id The auction UUID.
     * @return 200 OK with the list of bids.
     */
    @GET
    @Path("/{id}/bids")
    @PermitAll
    fun getBidHistory(@PathParam("id") id: String): Response {
        val auctionId = UUID.fromString(id)

        // Verify auction exists
        readModelRepository.findById(auctionId)
            ?: throw NotFoundException(
                code = "AUCTION_NOT_FOUND",
                message = "Auction $id not found"
            )

        // Load bid events from the event store
        val events = eventRepository.findByAggregateId(auctionId)
        val bidEvents = events.filter {
            it.eventType == "BidPlacedEvent" || it.eventType == "ProxyBidTriggeredEvent"
        }

        val bids = bidEvents.map { toBidResponse(it) }

        return Response.ok(ApiResponse.ok(bids)).build()
    }

    /**
     * Places a bid on an auction.
     *
     * Restricted to buyer_active role. The bidder identity is extracted from
     * the JWT token's subject claim via the injected [SecurityContext].
     *
     * **POST /api/v1/auctions/{id}/bids**
     *
     * @param id The auction UUID.
     * @param request The bid request body.
     * @param securityContext Injected security context with the authenticated user.
     * @return 200 OK with bid confirmation.
     */
    @POST
    @Path("/{id}/bids")
    @RolesAllowed("buyer_active")
    fun placeBid(
        @PathParam("id") id: String,
        @Valid request: PlaceBidRequest,
        @Context securityContext: SecurityContext
    ): Response {
        val userId = extractUserId(securityContext)

        val currency = Currency.getInstance(request.currency)
        val command = PlaceBidCommand(
            auctionId = AuctionId.fromString(id),
            bidderId = UserId.fromString(userId),
            amount = Money.of(request.amount, currency)
        )

        val result = bidService.placeBid(command)

        val confirmation = BidConfirmationResponse(
            bidId = result.bidId,
            auctionId = result.auctionId,
            amount = request.amount,
            newHighBid = result.newHighBid,
            closingTime = result.closingTime,
            reserveStatus = result.reserveStatus,
            extensionApplied = result.extensionApplied
        )

        return Response.ok(ApiResponse.ok(confirmation)).build()
    }

    /**
     * Configures an automatic (proxy) bid on an auction.
     *
     * Restricted to buyer_active role.
     *
     * **POST /api/v1/auctions/{id}/auto-bids**
     *
     * @param id The auction UUID.
     * @param request The auto-bid configuration request body.
     * @param securityContext Injected security context.
     * @return 200 OK with the auto-bid confirmation.
     */
    @POST
    @Path("/{id}/auto-bids")
    @RolesAllowed("buyer_active")
    fun setAutoBid(
        @PathParam("id") id: String,
        @Valid request: SetAutoBidRequest,
        @Context securityContext: SecurityContext
    ): Response {
        val userId = extractUserId(securityContext)

        val currency = Currency.getInstance(request.currency)
        val command = SetAutoBidCommand(
            auctionId = AuctionId.fromString(id),
            bidderId = UserId.fromString(userId),
            maxAmount = Money.of(request.maxAmount, currency)
        )

        val result = bidService.setAutoBid(command)

        val response = AutoBidConfirmationResponse(
            auctionId = result.auctionId,
            maxAmount = result.maxAmount,
            currentBidAmount = result.currentBidAmount,
            active = true
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }

    /**
     * Cancels the authenticated user's automatic bid on an auction.
     *
     * Restricted to buyer_active role. This sets the auto-bid's max amount
     * to the minimum possible value, effectively deactivating it.
     *
     * **DELETE /api/v1/auctions/{id}/auto-bids**
     *
     * @param id The auction UUID.
     * @param securityContext Injected security context.
     * @return 204 No Content on success.
     */
    @DELETE
    @Path("/{id}/auto-bids")
    @RolesAllowed("buyer_active")
    fun cancelAutoBid(
        @PathParam("id") id: String,
        @Context securityContext: SecurityContext
    ): Response {
        val userId = extractUserId(securityContext)

        LOG.infof("Cancelling auto-bid for user %s on auction %s", userId, id)

        // Cancel by setting auto-bid to minimum amount -- the aggregate will
        // either deactivate it or it will be outbid immediately.
        val command = SetAutoBidCommand(
            auctionId = AuctionId.fromString(id),
            bidderId = UserId.fromString(userId),
            maxAmount = Money.of(BigDecimal("0.01"))
        )

        bidService.setAutoBid(command)

        return Response.noContent().build()
    }

    /**
     * Lists auctions with optional filtering by status and brand.
     *
     * Supports pagination via `page` and `size` query parameters.
     *
     * **GET /api/v1/auctions**
     *
     * @param status Optional auction status filter (e.g. "ACTIVE", "CLOSED").
     * @param brand Optional brand code filter (e.g. "troostwijk").
     * @param page Page number (1-based, default 1).
     * @param size Page size (default 20, max 100).
     * @return 200 OK with paginated auction summaries.
     */
    @GET
    @PermitAll
    fun listAuctions(
        @QueryParam("status") status: String?,
        @QueryParam("brand") brand: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int
    ): Response {
        val effectiveSize = size.coerceIn(1, 100)
        val effectivePage = page.coerceAtLeast(1)

        // Query the read model with JDBC for filtered, paginated results
        val results = queryAuctions(status, brand, effectivePage, effectiveSize)

        val summaries = results.first.map { toSummaryResponse(it) }
        val total = results.second

        val pagedResponse = PagedResponse(
            items = summaries,
            total = total,
            page = effectivePage,
            pageSize = effectiveSize
        )

        return Response.ok(pagedResponse).build()
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Extracts the authenticated user's identity from the security context.
     *
     * @throws ForbiddenException if the user identity is not available.
     */
    private fun extractUserId(securityContext: SecurityContext): String {
        return securityContext.userPrincipal?.name
            ?: throw ForbiddenException(
                code = "UNAUTHENTICATED",
                message = "User identity not available"
            )
    }

    /**
     * Queries the auction read model with optional filters and pagination.
     *
     * Executes two queries: a COUNT for the total and a SELECT with LIMIT/OFFSET
     * for the current page. Filters are applied via parameterised WHERE clauses
     * to prevent SQL injection.
     *
     * @return A pair of (results, total count).
     */
    private fun queryAuctions(
        status: String?,
        brand: String?,
        page: Int,
        size: Int
    ): Pair<List<AuctionReadModel>, Long> {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any>()

        if (!status.isNullOrBlank()) {
            conditions.add("status = ?")
            params.add(status.uppercase())
        }
        if (!brand.isNullOrBlank()) {
            conditions.add("brand = ?")
            params.add(brand)
        }

        val whereClause = if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"
        val offset = (page - 1) * size

        // Count total matching records
        val total: Long = dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT COUNT(*) FROM app.auction_read_model $whereClause").use { stmt ->
                params.forEachIndexed { index, param -> stmt.setString(index + 1, param.toString()) }
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getLong(1) else 0L
                }
            }
        }

        if (total == 0L) {
            return Pair(emptyList(), 0L)
        }

        // Fetch the current page
        val querySql = """
            SELECT auction_id, lot_id, brand, status, start_time, end_time,
                   original_end_time, starting_bid, current_high_bid,
                   current_high_bidder_id, bid_count, reserve_met,
                   extension_count, seller_id, created_at, updated_at
              FROM app.auction_read_model
              $whereClause
             ORDER BY end_time ASC
             LIMIT ? OFFSET ?
        """

        val models: List<AuctionReadModel> = dataSource.connection.use { conn ->
            conn.prepareStatement(querySql).use { stmt ->
                var paramIndex = 1
                params.forEach { param ->
                    stmt.setString(paramIndex++, param.toString())
                }
                stmt.setInt(paramIndex++, size)
                stmt.setInt(paramIndex, offset)

                stmt.executeQuery().use { rs ->
                    val results = mutableListOf<AuctionReadModel>()
                    while (rs.next()) {
                        results.add(
                            AuctionReadModel(
                                auctionId = rs.getObject("auction_id", UUID::class.java),
                                lotId = rs.getObject("lot_id", UUID::class.java),
                                brand = rs.getString("brand"),
                                status = rs.getString("status"),
                                startTime = rs.getTimestamp("start_time").toInstant(),
                                endTime = rs.getTimestamp("end_time").toInstant(),
                                originalEndTime = rs.getTimestamp("original_end_time").toInstant(),
                                startingBid = rs.getBigDecimal("starting_bid"),
                                currentHighBid = rs.getBigDecimal("current_high_bid"),
                                currentHighBidderId = rs.getObject("current_high_bidder_id", UUID::class.java),
                                bidCount = rs.getInt("bid_count"),
                                reserveMet = rs.getBoolean("reserve_met"),
                                extensionCount = rs.getInt("extension_count"),
                                sellerId = rs.getObject("seller_id", UUID::class.java),
                                createdAt = rs.getTimestamp("created_at").toInstant(),
                                updatedAt = rs.getTimestamp("updated_at").toInstant()
                            )
                        )
                    }
                    results
                }
            }
        }

        return Pair(models, total)
    }

    /**
     * Converts a read model to a detailed auction response DTO.
     */
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
            createdAt = model.createdAt,
            updatedAt = model.updatedAt
        )

    /**
     * Converts a read model to a summary auction response DTO.
     */
    private fun toSummaryResponse(model: AuctionReadModel): AuctionSummaryResponse =
        AuctionSummaryResponse(
            auctionId = model.auctionId.toString(),
            lotId = model.lotId.toString(),
            brand = model.brand,
            status = model.status,
            startTime = model.startTime,
            endTime = model.endTime,
            currentHighBid = model.currentHighBid,
            bidCount = model.bidCount,
            reserveMet = model.reserveMet
        )

    /**
     * Converts an event store entity with a bid event type into a [BidResponse]
     * by deserialising the JSON event data.
     */
    @Suppress("UNCHECKED_CAST")
    private fun toBidResponse(entity: AuctionEventEntity): BidResponse {
        val data = JsonMapper.instance.readValue(
            entity.eventData,
            Map::class.java
        ) as Map<String, Any?>

        return BidResponse(
            bidId = data["bidId"]?.toString() ?: entity.eventId.toString(),
            bidderId = data["bidderId"]?.toString() ?: "",
            amount = BigDecimal(data["bidAmount"]?.toString() ?: data["amount"]?.toString() ?: "0"),
            currency = data["bidCurrency"]?.toString() ?: data["currency"]?.toString() ?: "EUR",
            isProxy = data["isProxy"] as? Boolean ?: (entity.eventType == "ProxyBidTriggeredEvent"),
            timestamp = entity.createdAt,
            status = "PLACED"
        )
    }
}
