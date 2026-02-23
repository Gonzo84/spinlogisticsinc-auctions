package eu.auctionplatform.catalog.api.v1.resource

import eu.auctionplatform.commons.dto.ApiResponse
import eu.auctionplatform.commons.dto.PagedResponse
import eu.auctionplatform.catalog.api.dto.toResponse
import eu.auctionplatform.catalog.api.dto.toSummaryResponse
import eu.auctionplatform.catalog.application.service.CategoryService
import eu.auctionplatform.catalog.infrastructure.persistence.repository.LotImageRepository
import jakarta.annotation.security.PermitAll
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import java.util.UUID

/**
 * REST resource for category browsing operations.
 *
 * Provides read-only endpoints for navigating the category hierarchy
 * and listing lots within categories. All endpoints are public (no
 * authentication required) to support anonymous browsing.
 */
@Path("/api/v1/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class CategoryResource {

    @Inject
    lateinit var categoryService: CategoryService

    @Inject
    lateinit var lotImageRepository: LotImageRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(CategoryResource::class.java)
    }

    /**
     * Returns the full category tree as a nested hierarchy.
     *
     * **GET /api/v1/categories/tree**
     *
     * @return 200 OK with the category tree structure.
     */
    @GET
    @Path("/tree")
    @PermitAll
    fun getCategoryTree(): Response {
        val tree = categoryService.getCategoryTree()

        return Response.ok(ApiResponse.ok(tree)).build()
    }

    /**
     * Returns all root-level categories as a flat list.
     *
     * **GET /api/v1/categories**
     *
     * @return 200 OK with a list of root categories.
     */
    @GET
    @PermitAll
    fun getRootCategories(): Response {
        val tree = categoryService.getCategoryTree()
        // Return only root-level nodes without children for a flat list
        val roots = tree.map { node ->
            eu.auctionplatform.catalog.api.dto.CategoryResponse(
                id = node.id,
                parentId = null,
                name = node.name,
                slug = node.slug,
                icon = node.icon,
                level = node.level,
                sortOrder = 0,
                active = true
            )
        }

        return Response.ok(ApiResponse.ok(roots)).build()
    }

    /**
     * Returns a single category by its identifier.
     *
     * **GET /api/v1/categories/{id}**
     *
     * @param id The category identifier.
     * @return 200 OK with the category details.
     */
    @GET
    @Path("/{id}")
    @PermitAll
    fun getCategoryById(@PathParam("id") id: UUID): Response {
        val category = categoryService.getCategoryById(id)

        return Response.ok(ApiResponse.ok(category.toResponse())).build()
    }

    /**
     * Returns a category by its URL-friendly slug.
     *
     * **GET /api/v1/categories/by-slug/{slug}**
     *
     * @param slug The category slug (e.g. "construction-machinery").
     * @return 200 OK with the category details.
     */
    @GET
    @Path("/by-slug/{slug}")
    @PermitAll
    fun getCategoryBySlug(@PathParam("slug") slug: String): Response {
        val category = categoryService.getCategoryBySlug(slug)

        return Response.ok(ApiResponse.ok(category.toResponse())).build()
    }

    /**
     * Returns child subcategories for a given parent category.
     *
     * **GET /api/v1/categories/{id}/children**
     *
     * @param id The parent category identifier.
     * @return 200 OK with a list of child categories.
     */
    @GET
    @Path("/{id}/children")
    @PermitAll
    fun getChildCategories(@PathParam("id") id: UUID): Response {
        val children = categoryService.getChildCategories(id)
            .map { it.toResponse() }

        return Response.ok(ApiResponse.ok(children)).build()
    }

    /**
     * Returns lots within a specific category, with pagination.
     *
     * **GET /api/v1/categories/{id}/lots**
     *
     * @param id       The category identifier.
     * @param page     Page number (0-based, default 0).
     * @param pageSize Items per page (default 20).
     * @return 200 OK with a paginated list of lot summaries.
     */
    @GET
    @Path("/{id}/lots")
    @PermitAll
    fun getLotsInCategory(
        @PathParam("id") id: UUID,
        @QueryParam("page") @DefaultValue("0") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int
    ): Response {
        val (lots, total) = categoryService.getLotsInCategory(id, page, pageSize)

        val summaries = lots.map { lot ->
            val primaryImage = lotImageRepository.findPrimaryByLotId(lot.id)
            lot.toSummaryResponse(primaryImage?.imageUrl)
        }

        val pagedResponse = PagedResponse(
            items = summaries,
            total = total,
            page = page,
            pageSize = pageSize
        )

        return Response.ok(ApiResponse.ok(pagedResponse)).build()
    }
}
