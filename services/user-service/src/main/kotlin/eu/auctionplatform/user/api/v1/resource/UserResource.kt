package eu.auctionplatform.user.api.v1.resource

import eu.auctionplatform.commons.auth.email
import eu.auctionplatform.commons.auth.familyName
import eu.auctionplatform.commons.auth.givenName
import eu.auctionplatform.commons.auth.userId
import eu.auctionplatform.commons.dto.ApiResponse
import eu.auctionplatform.commons.dto.PagedResponse
import eu.auctionplatform.user.api.dto.AddCompanyRequest
import eu.auctionplatform.user.api.dto.InitiateDepositRequest
import eu.auctionplatform.user.api.dto.RegisterUserRequest
import eu.auctionplatform.user.api.dto.UpdateProfileRequest
import eu.auctionplatform.user.api.dto.UpdateUserStatusRequest
import eu.auctionplatform.user.api.dto.UserProfileResponse
import eu.auctionplatform.user.api.dto.UserResponse
import eu.auctionplatform.user.api.dto.CompanyResponse
import eu.auctionplatform.user.api.dto.DepositResponse
import eu.auctionplatform.user.api.dto.toResponse
import eu.auctionplatform.user.application.service.UserService
import io.agroal.api.AgroalDataSource
import jakarta.annotation.security.PermitAll
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
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
import eu.auctionplatform.user.domain.model.UserStatus
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * REST resource for user management operations.
 *
 * Provides self-service endpoints for authenticated users (/me) and
 * administrative endpoints for platform operators (/{id}).
 *
 * Authentication is handled by Quarkus OIDC; the bearer token is expected
 * in the `Authorization` header. The Keycloak `sub` claim is used to
 * resolve the internal user identity.
 */
@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class UserResource {

    @Inject
    lateinit var userService: UserService

    @Inject
    lateinit var dataSource: AgroalDataSource

    companion object {
        private val LOG: Logger = Logger.getLogger(UserResource::class.java)
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers a new user account.
     *
     * This endpoint is called by the frontend after successful Keycloak
     * authentication to create the platform-side user record.
     *
     * **POST /api/v1/users/register**
     *
     * @param request The registration payload.
     * @return 201 Created with the new user profile.
     */
    @POST
    @Path("/register")
    @PermitAll
    fun register(request: RegisterUserRequest): Response {
        LOG.infof("Registration request for email=%s", request.email)

        val user = userService.registerUser(request)

        return Response
            .status(Response.Status.CREATED)
            .entity(ApiResponse.ok(user.toResponse()))
            .build()
    }

    // -------------------------------------------------------------------------
    // Self-service endpoints (/me)
    // -------------------------------------------------------------------------

    /**
     * Returns the authenticated user's full profile, including company
     * and deposit information.
     *
     * **GET /api/v1/users/me**
     *
     * @param authorization The Bearer token from the Authorization header.
     * @return 200 OK with the user's profile.
     */
    @GET
    @Path("/me")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getMe(@HeaderParam("Authorization") authorization: String): Response {
        val user = resolveCurrentUser(authorization)
        val company = userService.getCompanyByUserId(user.id)
        val deposit = userService.getLatestDeposit(user.id)

        val profile = UserProfileResponse(
            user = user.toResponse(),
            company = company?.toResponse(),
            deposit = deposit?.toResponse()
        )

        return Response.ok(ApiResponse.ok(profile)).build()
    }

    /**
     * Updates the authenticated user's profile fields.
     *
     * **PUT /api/v1/users/me**
     *
     * @param authorization The Bearer token.
     * @param request       The fields to update (only non-null fields are applied).
     * @return 200 OK with the updated user profile.
     */
    @PUT
    @Path("/me")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun updateMe(
        @HeaderParam("Authorization") authorization: String,
        request: UpdateProfileRequest
    ): Response {
        val user = resolveCurrentUser(authorization)
        val updated = userService.updateProfile(user.id, request)

        return Response.ok(ApiResponse.ok(updated.toResponse())).build()
    }

    /**
     * Adds a company profile to the authenticated user's BUSINESS account.
     *
     * **POST /api/v1/users/me/company**
     *
     * @param authorization The Bearer token.
     * @param request       The company details.
     * @return 201 Created with the new company profile.
     */
    @POST
    @Path("/me/company")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun addCompany(
        @HeaderParam("Authorization") authorization: String,
        request: AddCompanyRequest
    ): Response {
        val user = resolveCurrentUser(authorization)
        val company = userService.addCompany(user.id, request)

        return Response
            .status(Response.Status.CREATED)
            .entity(ApiResponse.ok(company.toResponse()))
            .build()
    }

    /**
     * Returns the authenticated user's current deposit status.
     *
     * **GET /api/v1/users/me/deposit**
     *
     * @param authorization The Bearer token.
     * @return 200 OK with the deposit details, or 204 No Content if none exists.
     */
    @GET
    @Path("/me/deposit")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getDeposit(@HeaderParam("Authorization") authorization: String): Response {
        val user = resolveCurrentUser(authorization)
        val deposit = userService.getLatestDeposit(user.id)

        return if (deposit != null) {
            Response.ok(ApiResponse.ok(deposit.toResponse())).build()
        } else {
            Response.noContent().build()
        }
    }

    /**
     * Initiates a new security deposit payment.
     *
     * **POST /api/v1/users/me/deposit**
     *
     * @param authorization The Bearer token.
     * @param request       The deposit payment details.
     * @return 201 Created with the new deposit record.
     */
    @POST
    @Path("/me/deposit")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun initiateDeposit(
        @HeaderParam("Authorization") authorization: String,
        request: InitiateDepositRequest
    ): Response {
        val user = resolveCurrentUser(authorization)
        val deposit = userService.initiateDeposit(user.id, request)

        return Response
            .status(Response.Status.CREATED)
            .entity(ApiResponse.ok(deposit.toResponse()))
            .build()
    }

    /**
     * Requests a refund for the authenticated user's active deposit.
     *
     * **POST /api/v1/users/me/deposit/refund**
     *
     * @param authorization The Bearer token.
     * @return 200 OK with the updated deposit record.
     */
    @POST
    @Path("/me/deposit/refund")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun requestDepositRefund(
        @HeaderParam("Authorization") authorization: String
    ): Response {
        val user = resolveCurrentUser(authorization)
        val deposit = userService.requestDepositRefund(user.id)

        return Response.ok(ApiResponse.ok(deposit.toResponse())).build()
    }

    /**
     * Returns the authenticated user's purchases (won auction lots).
     *
     * **GET /api/v1/users/me/purchases**
     *
     * @param authorization The Bearer token.
     * @param page          Page number (1-based, defaults to 1).
     * @param pageSize      Items per page (defaults to 20).
     * @return 200 OK with paginated purchase list.
     */
    @GET
    @Path("/me/purchases")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getMyPurchases(
        @HeaderParam("Authorization") authorization: String,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Response {
        val user = resolveCurrentUser(authorization)
        val pageNum = (page ?: 1).coerceAtLeast(1)
        val size = (pageSize ?: 20).coerceIn(1, 100)
        val offset = (pageNum - 1) * size

        val (items, total) = dataSource.connection.use { conn ->
            val countSql = "SELECT COUNT(*) FROM app.user_purchases WHERE user_id = ?"
            val totalCount = conn.prepareStatement(countSql).use { stmt ->
                stmt.setObject(1, user.id)
                stmt.executeQuery().use { rs ->
                    rs.next()
                    rs.getLong(1)
                }
            }

            val querySql = """
                SELECT lot_id, auction_id, hammer_price, currency, status, awarded_at
                FROM app.user_purchases
                WHERE user_id = ?
                ORDER BY awarded_at DESC
                LIMIT ? OFFSET ?
            """.trimIndent()
            val rows = conn.prepareStatement(querySql).use { stmt ->
                stmt.setObject(1, user.id)
                stmt.setInt(2, size)
                stmt.setInt(3, offset)
                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<Map<String, Any?>>()
                    while (rs.next()) {
                        result.add(mapOf(
                            "lotId" to rs.getObject("lot_id")?.toString(),
                            "auctionId" to rs.getObject("auction_id")?.toString(),
                            "hammerPrice" to rs.getBigDecimal("hammer_price"),
                            "currency" to rs.getString("currency"),
                            "status" to rs.getString("status"),
                            "awardedAt" to rs.getTimestamp("awarded_at")?.toInstant()?.toString()
                        ))
                    }
                    result
                }
            }

            rows to totalCount
        }

        val pagedResponse = PagedResponse(
            items = items,
            total = total,
            page = pageNum,
            pageSize = size
        )
        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }

    /**
     * Returns the authenticated user's bid history.
     *
     * **GET /api/v1/users/me/bids**
     *
     * @param authorization The Bearer token.
     * @param page          Page number (1-based, defaults to 1).
     * @param pageSize      Items per page (defaults to 20).
     * @return 200 OK with paginated bid list.
     */
    @GET
    @Path("/me/bids")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getMyBids(
        @HeaderParam("Authorization") authorization: String,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Response {
        val user = resolveCurrentUser(authorization)
        val pageNum = (page ?: 1).coerceAtLeast(1)
        val size = (pageSize ?: 20).coerceIn(1, 100)
        val offset = (pageNum - 1) * size

        val (items, total) = dataSource.connection.use { conn ->
            val countSql = "SELECT COUNT(*) FROM app.user_bids WHERE user_id = ?"
            val totalCount = conn.prepareStatement(countSql).use { stmt ->
                stmt.setObject(1, user.id)
                stmt.executeQuery().use { rs ->
                    rs.next()
                    rs.getLong(1)
                }
            }

            val querySql = """
                SELECT auction_id, lot_id, amount, currency, bid_at
                FROM app.user_bids
                WHERE user_id = ?
                ORDER BY bid_at DESC
                LIMIT ? OFFSET ?
            """.trimIndent()
            val rows = conn.prepareStatement(querySql).use { stmt ->
                stmt.setObject(1, user.id)
                stmt.setInt(2, size)
                stmt.setInt(3, offset)
                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<Map<String, Any?>>()
                    while (rs.next()) {
                        result.add(mapOf(
                            "auctionId" to rs.getObject("auction_id")?.toString(),
                            "lotId" to rs.getObject("lot_id")?.toString(),
                            "amount" to rs.getBigDecimal("amount"),
                            "currency" to rs.getString("currency"),
                            "bidAt" to rs.getTimestamp("bid_at")?.toInstant()?.toString()
                        ))
                    }
                    result
                }
            }

            rows to totalCount
        }

        val pagedResponse = PagedResponse(
            items = items,
            total = total,
            page = pageNum,
            pageSize = size
        )
        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }

    /**
     * Returns the authenticated user's watchlist.
     *
     * **GET /api/v1/users/me/watchlist**
     *
     * @param authorization The Bearer token.
     * @param page          Page number (1-based, defaults to 1).
     * @param pageSize      Items per page (defaults to 20).
     * @return 200 OK with paginated watchlist.
     */
    @GET
    @Path("/me/watchlist")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getMyWatchlist(
        @HeaderParam("Authorization") authorization: String,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Response {
        val user = resolveCurrentUser(authorization)
        val pageNum = (page ?: 1).coerceAtLeast(1)
        val size = (pageSize ?: 20).coerceIn(1, 100)
        val offset = (pageNum - 1) * size

        val (items, total) = dataSource.connection.use { conn ->
            val countSql = "SELECT COUNT(*) FROM app.user_watchlist WHERE user_id = ?"
            val totalCount = conn.prepareStatement(countSql).use { stmt ->
                stmt.setObject(1, user.id)
                stmt.executeQuery().use { rs ->
                    rs.next()
                    rs.getLong(1)
                }
            }

            val querySql = """
                SELECT lot_id, added_at
                FROM app.user_watchlist
                WHERE user_id = ?
                ORDER BY added_at DESC
                LIMIT ? OFFSET ?
            """.trimIndent()
            val rows = conn.prepareStatement(querySql).use { stmt ->
                stmt.setObject(1, user.id)
                stmt.setInt(2, size)
                stmt.setInt(3, offset)
                stmt.executeQuery().use { rs ->
                    val result = mutableListOf<Map<String, Any?>>()
                    while (rs.next()) {
                        result.add(mapOf(
                            "lotId" to rs.getObject("lot_id")?.toString(),
                            "addedAt" to rs.getTimestamp("added_at")?.toInstant()?.toString()
                        ))
                    }
                    result
                }
            }

            rows to totalCount
        }

        val pagedResponse = PagedResponse(
            items = items,
            total = total,
            page = pageNum,
            pageSize = size
        )
        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }

    /**
     * Adds a lot to the authenticated user's watchlist.
     *
     * **POST /api/v1/users/me/watchlist/{lotId}**
     *
     * @param authorization The Bearer token.
     * @param lotId         The lot UUID to add to the watchlist.
     * @return 201 Created if added, 200 OK if already present.
     */
    @POST
    @Path("/me/watchlist/{lotId}")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun addToWatchlist(
        @HeaderParam("Authorization") authorization: String,
        @PathParam("lotId") lotId: UUID
    ): Response {
        val user = resolveCurrentUser(authorization)

        val inserted = dataSource.connection.use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO app.user_watchlist (user_id, lot_id)
                VALUES (?, ?)
                ON CONFLICT (user_id, lot_id) DO NOTHING
                """.trimIndent()
            ).use { stmt ->
                stmt.setObject(1, user.id)
                stmt.setObject(2, lotId)
                stmt.executeUpdate()
            }
        }

        val status = if (inserted > 0) Response.Status.CREATED else Response.Status.OK
        val result = mapOf("lotId" to lotId.toString(), "watched" to true)
        return Response.status(status).entity(ApiResponse.ok(result)).build()
    }

    /**
     * Removes a lot from the authenticated user's watchlist.
     *
     * **DELETE /api/v1/users/me/watchlist/{lotId}**
     *
     * @param authorization The Bearer token.
     * @param lotId         The lot UUID to remove from the watchlist.
     * @return 200 OK with the removal result.
     */
    @DELETE
    @Path("/me/watchlist/{lotId}")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun removeFromWatchlist(
        @HeaderParam("Authorization") authorization: String,
        @PathParam("lotId") lotId: UUID
    ): Response {
        val user = resolveCurrentUser(authorization)

        dataSource.connection.use { conn ->
            conn.prepareStatement(
                "DELETE FROM app.user_watchlist WHERE user_id = ? AND lot_id = ?"
            ).use { stmt ->
                stmt.setObject(1, user.id)
                stmt.setObject(2, lotId)
                stmt.executeUpdate()
            }
        }

        val result = mapOf("lotId" to lotId.toString(), "watched" to false)
        return Response.ok(ApiResponse.ok(result)).build()
    }

    // -------------------------------------------------------------------------
    // Admin endpoints
    // -------------------------------------------------------------------------

    /**
     * Lists users with optional search and filter parameters (admin only).
     *
     * **GET /api/v1/users?search=&status=&page=1&pageSize=20**
     *
     * @param search   Optional search text (matches first name, last name, or email).
     * @param status   Optional status filter (e.g., ACTIVE, BLOCKED).
     * @param page     Page number (1-based, defaults to 1).
     * @param pageSize Items per page (defaults to 20).
     * @return 200 OK with paginated user list.
     */
    @GET
    @RolesAllowed("admin_ops", "admin_super")
    fun listUsers(
        @QueryParam("search") search: String?,
        @QueryParam("status") status: String?,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Response {
        val pageNum = (page ?: 1).coerceAtLeast(1)
        val size = (pageSize ?: 20).coerceIn(1, 100)

        val userStatus = status?.let {
            try {
                UserStatus.valueOf(it.uppercase())
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        val (users, total) = userService.listUsers(
            search = search,
            status = userStatus,
            page = pageNum - 1, // Convert to 0-based for repository
            pageSize = size
        )

        val pagedResponse = PagedResponse(
            items = users.map { user ->
                val company = userService.getCompanyByUserId(user.id)
                mapOf(
                    "id" to user.id,
                    "keycloakId" to user.keycloakId,
                    "email" to user.email,
                    "firstName" to user.firstName,
                    "lastName" to user.lastName,
                    "fullName" to user.fullName,
                    "companyName" to (company?.companyName ?: ""),
                    "accountType" to user.accountType.name.lowercase(),
                    "status" to user.status.name.lowercase(),
                    "kycStatus" to "not_started",
                    "depositStatus" to user.depositStatus.name.lowercase(),
                    "registeredAt" to user.createdAt.toString(),
                    "lastLoginAt" to user.updatedAt.toString()
                )
            },
            total = total,
            page = pageNum,
            pageSize = size
        )

        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }

    /**
     * Retrieves a user by their Keycloak subject identifier (admin/moderator only).
     *
     * This endpoint is used by the admin dashboard to resolve seller names
     * from the Keycloak UUID stored in catalog lots.
     *
     * **GET /api/v1/users/by-keycloak-id/{keycloakId}**
     *
     * @param keycloakId The Keycloak `sub` claim UUID.
     * @return 200 OK with the user profile.
     */
    @GET
    @Path("/by-keycloak-id/{keycloakId}")
    @RolesAllowed("admin_ops", "admin_super")
    fun getUserByKeycloakId(@PathParam("keycloakId") keycloakId: String): Response {
        val user = userService.getUserByKeycloakId(keycloakId)
        val company = userService.getCompanyByUserId(user.id)
        val deposit = userService.getLatestDeposit(user.id)

        val profile = UserProfileResponse(
            user = user.toResponse(),
            company = company?.toResponse(),
            deposit = deposit?.toResponse()
        )

        return Response.ok(ApiResponse.ok(profile)).build()
    }

    /**
     * Retrieves any user by their internal identifier (admin/moderator only).
     *
     * **GET /api/v1/users/{id}**
     *
     * @param id The user's UUID.
     * @return 200 OK with the user profile.
     */
    @GET
    @Path("/{id}")
    @RolesAllowed("admin_ops", "admin_super")
    fun getUserById(@PathParam("id") id: UUID): Response {
        val user = userService.getUserById(id)
        val company = userService.getCompanyByUserId(user.id)
        val deposit = userService.getLatestDeposit(user.id)

        val profile = UserProfileResponse(
            user = user.toResponse(),
            company = company?.toResponse(),
            deposit = deposit?.toResponse()
        )

        return Response.ok(ApiResponse.ok(profile)).build()
    }

    /**
     * Updates a user's account status (admin only).
     *
     * Supports transitions to any [UserStatus] value, including block,
     * unblock, and suspend operations.
     *
     * **PUT /api/v1/users/{id}/status**
     *
     * @param id      The user's UUID.
     * @param request The new status and optional reason.
     * @return 200 OK with the updated user profile.
     */
    @PUT
    @Path("/{id}/status")
    @RolesAllowed("admin_ops", "admin_super")
    fun updateUserStatus(
        @PathParam("id") id: UUID,
        request: UpdateUserStatusRequest
    ): Response {
        val updated = userService.updateUserStatus(id, request.status, request.reason)

        return Response.ok(ApiResponse.ok(updated.toResponse())).build()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the Keycloak subject identifier from the Authorization header.
     *
     * The header is expected in the format "Bearer <JWT>". The `sub` claim
     * is extracted from the JWT payload without full signature verification
     * (signature is validated at the framework level by Quarkus OIDC).
     */
    private fun extractKeycloakId(authorization: String): String {
        val token = authorization.removePrefix("Bearer ").trim()
        return token.userId()
    }

    /**
     * Resolves the current user from the JWT, auto-creating a user profile
     * if one does not yet exist. This ensures first-time Keycloak users
     * automatically get a user-service record.
     */
    private fun resolveCurrentUser(authorization: String): eu.auctionplatform.user.domain.model.User {
        val token = authorization.removePrefix("Bearer ").trim()
        val keycloakId = token.userId()
        val email = token.email()
        val firstName = token.givenName()
        val lastName = token.familyName()
        return userService.getOrCreateUser(keycloakId, email, firstName, lastName)
    }
}
