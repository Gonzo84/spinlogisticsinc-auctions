package eu.auctionplatform.notification.api.v1.resource

import eu.auctionplatform.commons.dto.ApiResponse
import eu.auctionplatform.commons.util.IdGenerator
import eu.auctionplatform.notification.api.v1.dto.MarkAllReadResponse
import eu.auctionplatform.notification.api.v1.dto.RegisterDeviceTokenRequest
import eu.auctionplatform.notification.api.v1.dto.UnreadCountResponse
import eu.auctionplatform.notification.api.v1.dto.UpdatePreferenceRequest
import eu.auctionplatform.notification.api.v1.dto.UpdatePreferencesRequest
import eu.auctionplatform.notification.api.v1.dto.toDomain
import eu.auctionplatform.notification.api.v1.dto.toResponse
import eu.auctionplatform.notification.application.service.NotificationPreferenceService
import eu.auctionplatform.notification.application.service.NotificationService
import eu.auctionplatform.notification.domain.model.DeviceToken
import eu.auctionplatform.notification.infrastructure.persistence.repository.DeviceTokenRepository
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
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
import java.util.UUID

/**
 * REST resource for notification management.
 *
 * Provides endpoints for:
 * - Listing and managing notifications (paginated, mark as read)
 * - Querying unread notification counts
 * - Managing notification delivery preferences
 * - Registering and unregistering device tokens for push notifications
 *
 * All endpoints require authentication. The user identity is extracted
 * from the bearer token in the Authorization header.
 */
@Path("/api/v1/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class NotificationResource {

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var preferenceService: NotificationPreferenceService

    @Inject
    lateinit var deviceTokenRepository: DeviceTokenRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(NotificationResource::class.java)
    }

    // -------------------------------------------------------------------------
    // Notification list & read operations
    // -------------------------------------------------------------------------

    /**
     * Lists the authenticated user's notifications with pagination.
     *
     * **GET /api/v1/notifications?page=1&size=20**
     *
     * @param authorization The Bearer token from the Authorization header.
     * @param page          Page number (1-based, default 1).
     * @param size          Page size (default 20, max 100).
     * @return 200 OK with paginated notifications.
     */
    @GET
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun listNotifications(
        @Context securityContext: SecurityContext,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("size") @DefaultValue("20") size: Int
    ): Response {
        val userId = extractUserId(securityContext)
        val pagedResult = notificationService.getUserNotifications(userId, page, size)

        val responseItems = pagedResult.items.map { it.toResponse() }
        val pagedResponse = eu.auctionplatform.commons.dto.PagedResponse(
            items = responseItems,
            total = pagedResult.total,
            page = pagedResult.page,
            pageSize = pagedResult.pageSize
        )

        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }

    /**
     * Returns the unread in-app notification count for the authenticated user.
     *
     * **GET /api/v1/notifications/unread-count**
     *
     * @param authorization The Bearer token.
     * @return 200 OK with unread count.
     */
    @GET
    @Path("/unread-count")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getUnreadCount(
        @Context securityContext: SecurityContext
    ): Response {
        val userId = extractUserId(securityContext)
        val count = notificationService.getUnreadCount(userId)

        return Response.ok(ApiResponse.ok(UnreadCountResponse(unreadCount = count))).build()
    }

    /**
     * Marks a single notification as read.
     *
     * **PUT /api/v1/notifications/{id}/read**
     *
     * @param authorization The Bearer token.
     * @param id            The notification UUID.
     * @return 200 OK on success.
     */
    @PUT
    @Path("/{id}/read")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun markAsRead(
        @Context securityContext: SecurityContext,
        @PathParam("id") id: UUID
    ): Response {
        val userId = extractUserId(securityContext)
        LOG.debugf("Mark as read: notificationId=%s, userId=%s", id, userId)

        notificationService.markAsRead(id)

        return Response.ok(ApiResponse.ok(mapOf("id" to id, "read" to true))).build()
    }

    /**
     * Marks all unread in-app notifications as read for the authenticated user.
     *
     * **PUT /api/v1/notifications/read-all**
     *
     * @param authorization The Bearer token.
     * @return 200 OK with the number of notifications marked as read.
     */
    @PUT
    @Path("/read-all")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun markAllAsRead(
        @Context securityContext: SecurityContext
    ): Response {
        val userId = extractUserId(securityContext)
        val count = notificationService.markAllAsRead(userId)

        return Response.ok(ApiResponse.ok(MarkAllReadResponse(markedCount = count))).build()
    }

    // -------------------------------------------------------------------------
    // Notification preferences
    // -------------------------------------------------------------------------

    /**
     * Returns the authenticated user's notification delivery preferences.
     *
     * Returns preferences for all notification types, filling in defaults
     * for types the user hasn't explicitly configured.
     *
     * **GET /api/v1/notifications/preferences**
     *
     * @param authorization The Bearer token.
     * @return 200 OK with the full list of preferences.
     */
    @GET
    @Path("/preferences")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun getPreferences(
        @Context securityContext: SecurityContext
    ): Response {
        val userId = extractUserId(securityContext)
        val preferences = preferenceService.getPreferences(userId)
        val response = preferences.map { it.toResponse() }

        return Response.ok(ApiResponse.ok(response)).build()
    }

    /**
     * Updates the authenticated user's notification delivery preferences.
     *
     * Accepts a list of preference entries. Each entry specifies the
     * notification type and the enabled state for each channel.
     *
     * **PUT /api/v1/notifications/preferences**
     *
     * @param authorization The Bearer token.
     * @param request       The bulk update payload.
     * @return 200 OK with the updated preferences.
     */
    @PUT
    @Path("/preferences")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun updatePreferences(
        @Context securityContext: SecurityContext,
        request: UpdatePreferencesRequest
    ): Response {
        val userId = extractUserId(securityContext)

        val domainPreferences = request.preferences.map { it.toDomain(userId) }
        val updated = preferenceService.updatePreferences(userId, domainPreferences)
        val response = updated.map { it.toResponse() }

        return Response.ok(ApiResponse.ok(response)).build()
    }

    // -------------------------------------------------------------------------
    // Device tokens
    // -------------------------------------------------------------------------

    /**
     * Registers a device token for push notifications.
     *
     * If the token already exists for the user/platform combination, it is
     * re-activated.
     *
     * **POST /api/v1/notifications/device-token**
     *
     * @param authorization The Bearer token.
     * @param request       The device token registration payload.
     * @return 201 Created with the device token details.
     */
    @POST
    @Path("/device-token")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun registerDeviceToken(
        @Context securityContext: SecurityContext,
        request: RegisterDeviceTokenRequest
    ): Response {
        val userId = extractUserId(securityContext)

        val deviceToken = DeviceToken(
            id = IdGenerator.generateUUIDv7(),
            userId = userId,
            platform = request.platform,
            token = request.token,
            active = true
        )

        deviceTokenRepository.upsert(deviceToken)

        LOG.infof(
            "Registered device token: userId=%s, platform=%s, tokenId=%s",
            userId, request.platform, deviceToken.id
        )

        return Response
            .status(Response.Status.CREATED)
            .entity(ApiResponse.ok(deviceToken.toResponse()))
            .build()
    }

    /**
     * Unregisters (deactivates) a device token.
     *
     * **DELETE /api/v1/notifications/device-token/{id}**
     *
     * @param authorization The Bearer token.
     * @param id            The device token UUID.
     * @return 200 OK on success, 404 if the token is not found.
     */
    @DELETE
    @Path("/device-token/{id}")
    @RolesAllowed("buyer_active", "buyer_pending_kyc", "seller_verified", "seller_pending", "broker", "admin_ops", "admin_super")
    fun unregisterDeviceToken(
        @Context securityContext: SecurityContext,
        @PathParam("id") id: UUID
    ): Response {
        val userId = extractUserId(securityContext)
        LOG.debugf("Unregister device token: id=%s, userId=%s", id, userId)

        val deactivated = deviceTokenRepository.deactivate(id)

        return if (deactivated) {
            Response.ok(ApiResponse.ok(mapOf("id" to id, "active" to false))).build()
        } else {
            Response.status(Response.Status.NOT_FOUND)
                .entity(ApiResponse.ok(mapOf("error" to "Device token not found")))
                .build()
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the user UUID from the SecurityContext principal.
     *
     * The principal name is set by Quarkus OIDC from the JWT `sub` claim.
     */
    private fun extractUserId(securityContext: SecurityContext): UUID {
        val principal = securityContext.userPrincipal?.name
            ?: throw jakarta.ws.rs.ForbiddenException("User identity not available")
        return UUID.fromString(principal)
    }
}
