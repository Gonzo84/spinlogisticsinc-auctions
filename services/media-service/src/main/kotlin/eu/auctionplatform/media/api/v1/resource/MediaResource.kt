package eu.auctionplatform.media.api.v1.resource

import eu.auctionplatform.commons.dto.ApiResponse
import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.media.api.v1.dto.ImageResponse
import eu.auctionplatform.media.api.v1.dto.PresignedUploadRequest
import eu.auctionplatform.media.api.v1.dto.PresignedUploadResponse
import eu.auctionplatform.media.api.v1.dto.UpdateOrderRequest
import eu.auctionplatform.media.application.service.PresignedUrlService
import eu.auctionplatform.media.domain.model.MediaImage
import eu.auctionplatform.media.infrastructure.minio.MinioService
import eu.auctionplatform.media.infrastructure.persistence.repository.ImageRepository
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * REST resource for media (image) management operations.
 *
 * Provides endpoints for presigned URL generation, image listing, deletion,
 * reordering, and primary image selection.
 *
 * Authentication is handled by Quarkus OIDC; authorisation is enforced
 * via Casbin RBAC roles.
 */
@Path("/api/v1/media")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class MediaResource @Inject constructor(
    private val presignedUrlService: PresignedUrlService,
    private val imageRepository: ImageRepository,
    private val minioService: MinioService,

    @ConfigProperty(name = "minio.bucket.media")
    private val mediaBucket: String
) {

    private val logger = LoggerFactory.getLogger(MediaResource::class.java)

    // -----------------------------------------------------------------------
    // Upload
    // -----------------------------------------------------------------------

    /**
     * Generates a presigned URL for direct image upload to MinIO.
     *
     * The client uses the returned URL to PUT the image file directly to
     * object storage. An image record is created in UPLOADING status.
     *
     * **POST /api/v1/media/upload/presigned**
     *
     * @param request The upload request containing lot ID, content type, and optional filename.
     * @return 200 OK with the presigned URL and image metadata.
     */
    @POST
    @Path("/upload/presigned")
    @RolesAllowed("seller_verified", "seller_pending", "admin_ops", "admin_super", "broker")
    fun generatePresignedUrl(@Valid request: PresignedUploadRequest): Response {
        logger.info("Generating presigned upload URL for lot {}", request.lotId)

        val result = presignedUrlService.generatePresignedUploadUrl(
            lotId = UUID.fromString(request.lotId),
            contentType = request.contentType,
            fileName = request.fileName
        )

        val response = PresignedUploadResponse(
            imageId = result.imageId.toString(),
            uploadUrl = result.uploadUrl,
            objectKey = result.objectKey,
            expiresIn = result.expiresIn
        )

        return Response.ok(ApiResponse.ok(response)).build()
    }

    // -----------------------------------------------------------------------
    // Listing
    // -----------------------------------------------------------------------

    /**
     * Lists all images for a given lot, ordered by display order.
     *
     * **GET /api/v1/media/lots/{lotId}/images**
     *
     * @param lotId The lot identifier.
     * @return 200 OK with the list of images.
     * @return 400 Bad Request if lotId is not a valid UUID.
     */
    @GET
    @Path("/lots/{lotId}/images")
    fun listImages(@PathParam("lotId") lotId: String): Response {
        val uuid = parseUuid(lotId, "lotId") ?: return badRequestResponse("lotId", lotId)
        val images = imageRepository.findByLotId(uuid)
        val responses = images.map { it.toResponse() }

        return Response.ok(ApiResponse.ok(responses)).build()
    }

    // -----------------------------------------------------------------------
    // Deletion
    // -----------------------------------------------------------------------

    /**
     * Deletes an image and its associated objects from MinIO.
     *
     * **DELETE /api/v1/media/images/{imageId}**
     *
     * @param imageId The image identifier.
     * @return 204 No Content on success.
     * @return 400 Bad Request if imageId is not a valid UUID.
     */
    @DELETE
    @Path("/images/{imageId}")
    @RolesAllowed("seller_verified", "seller_pending", "admin_ops", "admin_super", "broker")
    fun deleteImage(@PathParam("imageId") imageId: String): Response {
        val id = parseUuid(imageId, "imageId") ?: return badRequestResponse("imageId", imageId)
        val image = imageRepository.findById(id)
            ?: throw NotFoundException(
                code = "IMAGE_NOT_FOUND",
                message = "Image $imageId not found"
            )

        logger.info("Deleting image {} (lot={}, key={})", imageId, image.lotId, image.objectKey)

        // Delete from MinIO (best-effort; object may not exist if upload never completed)
        try {
            minioService.deleteObject(mediaBucket, image.objectKey)
        } catch (ex: Exception) {
            logger.warn("Failed to delete object {} from MinIO: {}", image.objectKey, ex.message)
        }

        // Delete from database
        imageRepository.deleteById(id)

        return Response.noContent().build()
    }

    // -----------------------------------------------------------------------
    // Reordering
    // -----------------------------------------------------------------------

    /**
     * Updates the display order of an image within its lot gallery.
     *
     * **PUT /api/v1/media/images/{imageId}/order**
     *
     * @param imageId The image identifier.
     * @param request The new display order.
     * @return 200 OK with the updated image.
     * @return 400 Bad Request if imageId is not a valid UUID.
     */
    @PUT
    @Path("/images/{imageId}/order")
    @RolesAllowed("seller_verified", "seller_pending", "admin_ops", "admin_super", "broker")
    fun updateOrder(
        @PathParam("imageId") imageId: String,
        @Valid request: UpdateOrderRequest
    ): Response {
        val id = parseUuid(imageId, "imageId") ?: return badRequestResponse("imageId", imageId)
        val image = imageRepository.findById(id)
            ?: throw NotFoundException(
                code = "IMAGE_NOT_FOUND",
                message = "Image $imageId not found"
            )

        logger.info("Updating display order for image {} to {}", imageId, request.displayOrder)

        imageRepository.updateOrder(id, request.displayOrder)

        val updated = image.copy(displayOrder = request.displayOrder)
        return Response.ok(ApiResponse.ok(updated.toResponse())).build()
    }

    // -----------------------------------------------------------------------
    // Primary selection
    // -----------------------------------------------------------------------

    /**
     * Sets an image as the primary (hero) image for its lot.
     *
     * Clears the primary flag on all other images for the same lot.
     *
     * **PUT /api/v1/media/images/{imageId}/primary**
     *
     * @param imageId The image identifier.
     * @return 200 OK with the updated image.
     * @return 400 Bad Request if imageId is not a valid UUID.
     */
    @PUT
    @Path("/images/{imageId}/primary")
    @RolesAllowed("seller_verified", "seller_pending", "admin_ops", "admin_super", "broker")
    fun setPrimary(@PathParam("imageId") imageId: String): Response {
        val id = parseUuid(imageId, "imageId") ?: return badRequestResponse("imageId", imageId)
        val image = imageRepository.findById(id)
            ?: throw NotFoundException(
                code = "IMAGE_NOT_FOUND",
                message = "Image $imageId not found"
            )

        logger.info("Setting image {} as primary for lot {}", imageId, image.lotId)

        imageRepository.setPrimary(id, image.lotId)

        val updated = image.copy(isPrimary = true)
        return Response.ok(ApiResponse.ok(updated.toResponse())).build()
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Safely parses a string into a [UUID], returning `null` if the string
     * is not a valid UUID format.
     *
     * @param value     The string to parse.
     * @param paramName The parameter name (for logging).
     * @return The parsed [UUID], or `null` if invalid.
     */
    private fun parseUuid(value: String, paramName: String): UUID? {
        return try {
            UUID.fromString(value)
        } catch (ex: IllegalArgumentException) {
            logger.warn("Invalid UUID for parameter '{}': {}", paramName, value)
            null
        }
    }

    /**
     * Builds a 400 Bad Request response for an invalid UUID path parameter.
     */
    private fun badRequestResponse(paramName: String, value: String): Response {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(mapOf(
                "error" to "INVALID_UUID",
                "message" to "Parameter '$paramName' must be a valid UUID, got: $value"
            ))
            .build()
    }

    /**
     * Converts a [MediaImage] domain model to an [ImageResponse] DTO.
     */
    private fun MediaImage.toResponse(): ImageResponse = ImageResponse(
        id = id.toString(),
        lotId = lotId.toString(),
        originalUrl = originalUrl,
        processedUrl = processedUrl,
        thumbnailUrl = thumbnailUrl,
        displayOrder = displayOrder,
        isPrimary = isPrimary,
        status = status.name,
        contentType = contentType,
        fileSize = fileSize,
        createdAt = createdAt
    )
}
