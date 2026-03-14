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
import org.eclipse.microprofile.config.inject.ConfigProperty
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
import jakarta.ws.rs.PUT
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
    private val dataSource: AgroalDataSource,
    @ConfigProperty(name = "auction.featured.max-count", defaultValue = "12")
    private val featuredMaxCount: String,
    @ConfigProperty(name = "auction.auto-award.revoke-window-minutes", defaultValue = "30")
    private val revokeWindowMinutes: String
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

        // Fields are guaranteed non-null by Bean Validation (@Valid + @NotNull/@NotBlank)
        val currency = Currency.getInstance(request.currency)
        val command = CreateAuctionCommand(
            lotId = LotId.fromString(request.lotId!!),
            brand = Brand.fromCode(request.brand!!),
            startTime = request.startTime!!,
            endTime = request.endTime!!,
            startingBid = Money.of(request.startingBid!!, currency),
            reservePrice = request.reservePrice?.let { Money.of(it, currency) },
            sellerId = UserId.fromString(request.sellerId!!)
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
     * Awards a closed auction to the winning bidder.
     *
     * Transitions the auction from CLOSED to AWARDED, recording the
     * hammer price and winner. Restricted to admin roles.
     *
     * **POST /api/v1/auctions/{id}/award**
     *
     * @param id The auction UUID.
     * @return 200 OK with award details.
     */
    @POST
    @Path("/{id}/award")
    @RolesAllowed("admin_ops", "admin_super")
    fun awardAuction(@PathParam("id") id: String): Response {
        LOG.infof("Awarding auction %s", id)

        val auctionId = AuctionId.fromString(id)
        val result = lifecycleService.awardLot(auctionId)

        val response = mapOf(
            "auctionId" to result.auctionId,
            "winnerId" to result.winnerId,
            "hammerPrice" to result.hammerPrice
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }

    /**
     * Revokes an award, reverting the auction to CLOSED status.
     *
     * Must be called within the configurable revoke window (default 30 min).
     *
     * **POST /api/v1/auctions/{id}/revoke-award**
     */
    @POST
    @Path("/{id}/revoke-award")
    @RolesAllowed("admin_ops", "admin_super")
    fun revokeAward(
        @PathParam("id") id: String,
        request: Map<String, String>?,
        @Context securityContext: SecurityContext
    ): Response {
        val auctionId = AuctionId.fromString(id)
        val adminId = extractUserId(securityContext)
        val reason = request?.get("reason")?.takeIf { it.isNotBlank() } ?: "Admin revoked award"

        val result = lifecycleService.revokeAward(
            auctionId = auctionId,
            adminId = adminId,
            reason = reason,
            revokeWindowMinutes = revokeWindowMinutes.toInt()
        )

        return Response.ok(ApiResponse.ok(result)).build()
    }

    /**
     * Marks an active auction as featured for homepage promotion.
     *
     * **POST /api/v1/auctions/{id}/feature**
     */
    @POST
    @Path("/{id}/feature")
    @RolesAllowed("admin_ops", "admin_super")
    fun featureAuction(
        @PathParam("id") id: String,
        @Context securityContext: SecurityContext
    ): Response {
        val auctionId = AuctionId.fromString(id)
        val adminId = UserId.fromString(extractUserId(securityContext))
        val maxFeatured = featuredMaxCount.toInt()
        val result = lifecycleService.featureAuction(auctionId, adminId, maxFeatured)
        return Response.ok(ApiResponse.ok(mapOf("auctionId" to result.auctionId, "featured" to result.featured))).build()
    }

    /**
     * Removes the featured flag from an auction.
     *
     * **DELETE /api/v1/auctions/{id}/feature**
     */
    @DELETE
    @Path("/{id}/feature")
    @RolesAllowed("admin_ops", "admin_super")
    fun unfeatureAuction(
        @PathParam("id") id: String,
        @Context securityContext: SecurityContext
    ): Response {
        val auctionId = AuctionId.fromString(id)
        val adminId = UserId.fromString(extractUserId(securityContext))
        val result = lifecycleService.unfeatureAuction(auctionId, adminId)
        return Response.ok(ApiResponse.ok(mapOf("auctionId" to result.auctionId, "featured" to result.featured))).build()
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
     * Retrieves auction details by lot ID.
     *
     * **GET /api/v1/auctions/by-lot/{lotId}**
     *
     * @param lotId The lot UUID.
     * @return 200 OK with auction details or 404 if not found.
     */
    @GET
    @Path("/by-lot/{lotId}")
    @PermitAll
    fun getAuctionByLot(@PathParam("lotId") lotId: String): Response {
        val lotUuid = UUID.fromString(lotId)
        val readModel = readModelRepository.findByLotId(lotUuid)
            ?: throw NotFoundException(
                code = "AUCTION_NOT_FOUND",
                message = "No auction found for lot $lotId"
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
        @QueryParam("lotId") lotId: String?,
        @QueryParam("featured") featuredParam: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int
    ): Response {
        val effectiveSize = size.coerceIn(1, 100)
        val effectivePage = page.coerceAtLeast(1)
        val featured = featuredParam?.toBooleanStrictOrNull()

        // Query the read model with JDBC for filtered, paginated results
        val results = queryAuctions(status, brand, lotId, featured, effectivePage, effectiveSize)

        val summaries = results.first.map { toSummaryResponse(it) }
        val total = results.second

        val pagedResponse = PagedResponse(
            items = summaries,
            total = total,
            page = effectivePage,
            pageSize = effectiveSize
        )

        return Response.ok(ApiResponse.ok(pagedResponse)).build()
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
        lotId: String?,
        featured: Boolean?,
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
        if (!lotId.isNullOrBlank()) {
            conditions.add("lot_id = ?::uuid")
            params.add(lotId)
        }
        if (featured != null) {
            conditions.add("featured = ?")
            params.add(featured)
        }

        val whereClause = if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"
        val offset = (page - 1) * size

        // Count total matching records
        val total: Long = dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT COUNT(*) FROM app.auction_read_model $whereClause").use { stmt ->
                params.forEachIndexed { index, param -> setTypedParam(stmt, index + 1, param) }
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
                   extension_count, seller_id, featured, featured_at,
                   awarded_at, auto_awarded,
                   created_at, updated_at
              FROM app.auction_read_model
              $whereClause
             ORDER BY end_time ASC
             LIMIT ? OFFSET ?
        """

        val models: List<AuctionReadModel> = dataSource.connection.use { conn ->
            conn.prepareStatement(querySql).use { stmt ->
                var paramIndex = 1
                params.forEach { param ->
                    setTypedParam(stmt, paramIndex++, param)
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
                                featured = rs.getBoolean("featured"),
                                featuredAt = rs.getTimestamp("featured_at")?.toInstant(),
                                awardedAt = rs.getTimestamp("awarded_at")?.toInstant(),
                                autoAwarded = rs.getBoolean("auto_awarded"),
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
    private fun toDetailResponse(model: AuctionReadModel): AuctionDetailResponse {
        val isAwarded = model.status == "AWARDED"
        return AuctionDetailResponse(
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
            winnerId = if (isAwarded) model.currentHighBidderId?.toString() else null,
            hammerPrice = if (isAwarded) model.currentHighBid else null,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt
        )
    }

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
            reserveMet = model.reserveMet,
            featured = model.featured,
            autoAwarded = model.autoAwarded
        )

    /**
     * Sets a prepared-statement parameter using the correct JDBC type.
     *
     * Boolean values use [java.sql.PreparedStatement.setBoolean] to avoid
     * PostgreSQL type-mismatch errors (`boolean = character varying`).
     * All other types fall back to [java.sql.PreparedStatement.setString].
     */
    private fun setTypedParam(stmt: java.sql.PreparedStatement, index: Int, value: Any) {
        when (value) {
            is Boolean -> stmt.setBoolean(index, value)
            else -> stmt.setString(index, value.toString())
        }
    }

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
