package eu.auctionplatform.user.api.v1.resource

import eu.auctionplatform.commons.auth.email
import eu.auctionplatform.commons.auth.familyName
import eu.auctionplatform.commons.auth.givenName
import eu.auctionplatform.commons.auth.userId
import eu.auctionplatform.commons.dto.ApiResponse
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
import jakarta.annotation.security.PermitAll
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
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
     * @return 200 OK with paginated purchase list (empty until auction-engine integration).
     */
    @GET
    @Path("/me/purchases")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getMyPurchases(@HeaderParam("Authorization") authorization: String): Response {
        val pagedResponse = eu.auctionplatform.commons.dto.PagedResponse(
            items = emptyList<Any>(),
            total = 0,
            page = 1,
            pageSize = 20
        )
        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }

    /**
     * Returns the authenticated user's bid history.
     *
     * **GET /api/v1/users/me/bids**
     *
     * @param authorization The Bearer token.
     * @return 200 OK with paginated bid list (empty until auction-engine integration).
     */
    @GET
    @Path("/me/bids")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getMyBids(@HeaderParam("Authorization") authorization: String): Response {
        val pagedResponse = eu.auctionplatform.commons.dto.PagedResponse(
            items = emptyList<Any>(),
            total = 0,
            page = 1,
            pageSize = 20
        )
        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }

    /**
     * Returns the authenticated user's watchlist.
     *
     * **GET /api/v1/users/me/watchlist**
     *
     * @param authorization The Bearer token.
     * @return 200 OK with paginated watchlist (empty until watchlist feature integration).
     */
    @GET
    @Path("/me/watchlist")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getMyWatchlist(@HeaderParam("Authorization") authorization: String): Response {
        val pagedResponse = eu.auctionplatform.commons.dto.PagedResponse(
            items = emptyList<Any>(),
            total = 0,
            page = 1,
            pageSize = 20
        )
        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }

    // -------------------------------------------------------------------------
    // Admin endpoints
    // -------------------------------------------------------------------------

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
