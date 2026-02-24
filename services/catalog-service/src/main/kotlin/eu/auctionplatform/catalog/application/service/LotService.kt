package eu.auctionplatform.catalog.application.service

import eu.auctionplatform.commons.exception.ConflictException
import eu.auctionplatform.commons.exception.ForbiddenException
import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.commons.exception.ValidationException
import eu.auctionplatform.commons.util.IdGenerator
import eu.auctionplatform.catalog.api.dto.CombineLotsRequest
import eu.auctionplatform.catalog.api.dto.CreateLotRequest
import eu.auctionplatform.catalog.api.dto.LotListFilter
import eu.auctionplatform.catalog.api.dto.UpdateLotRequest
import eu.auctionplatform.catalog.domain.model.Lot
import eu.auctionplatform.catalog.domain.model.LotImage
import eu.auctionplatform.catalog.domain.model.LotStatus
import eu.auctionplatform.catalog.infrastructure.persistence.entity.LotEntity
import eu.auctionplatform.catalog.infrastructure.persistence.repository.AuctionEventRepository
import eu.auctionplatform.catalog.infrastructure.persistence.repository.CategoryRepository
import eu.auctionplatform.catalog.infrastructure.persistence.repository.LotImageRepository
import eu.auctionplatform.catalog.infrastructure.persistence.repository.LotRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

/**
 * Application service that orchestrates lot management operations.
 *
 * Coordinates between the REST API layer and the domain/persistence layers,
 * enforcing business rules around lot lifecycle transitions, ownership
 * validation, and catalog integrity.
 */
@ApplicationScoped
class LotService {

    @Inject
    lateinit var lotRepository: LotRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var lotImageRepository: LotImageRepository

    @Inject
    lateinit var auctionEventRepository: AuctionEventRepository

    companion object {
        private val LOG: Logger = Logger.getLogger(LotService::class.java)
    }

    // -------------------------------------------------------------------------
    // Creation
    // -------------------------------------------------------------------------

    /**
     * Creates a new lot in DRAFT status.
     *
     * Validates that the referenced category exists, then persists the lot.
     *
     * @param sellerId The authenticated seller's user identifier.
     * @param request  The lot creation details.
     * @return The newly created [Lot] domain model.
     * @throws NotFoundException if the referenced category does not exist.
     */
    @Transactional
    fun createLot(sellerId: UUID, request: CreateLotRequest): Lot {
        // Validate category exists
        categoryRepository.findById(request.categoryId)
            ?: throw NotFoundException(
                code = "CATEGORY_NOT_FOUND",
                message = "Category with id '${request.categoryId}' not found."
            )

        val lot = Lot(
            id = IdGenerator.generateUUIDv7(),
            sellerId = sellerId,
            brand = request.brand,
            title = request.title,
            description = request.description,
            categoryId = request.categoryId,
            specifications = request.specifications,
            locationLat = request.locationLat,
            locationLng = request.locationLng,
            locationAddress = request.locationAddress,
            locationCountry = request.locationCountry,
            locationCity = request.locationCity,
            reservePrice = request.reservePrice,
            startingBid = request.startingBid,
            status = LotStatus.DRAFT,
            co2AvoidedKg = request.co2AvoidedKg,
            pickupInfo = request.pickupInfo,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        lotRepository.persist(LotEntity.fromDomain(lot))

        LOG.infof("Created lot: id=%s, seller=%s, brand=%s, title=%s",
            lot.id, sellerId, lot.brand, lot.title)

        return lot
    }

    // -------------------------------------------------------------------------
    // Updates
    // -------------------------------------------------------------------------

    /**
     * Updates mutable fields on an existing lot.
     *
     * Only lots in an editable state (DRAFT or PENDING_REVIEW) can be updated.
     * The caller must be the lot's owner.
     *
     * @param lotId    The lot identifier.
     * @param sellerId The authenticated seller's user identifier (for ownership check).
     * @param request  The fields to update (only non-null fields are applied).
     * @return The updated [Lot] domain model.
     * @throws NotFoundException   if the lot does not exist.
     * @throws ForbiddenException  if the caller is not the lot owner.
     * @throws ConflictException   if the lot is not in an editable state.
     */
    @Transactional
    fun updateLot(lotId: UUID, sellerId: UUID, request: UpdateLotRequest): Lot {
        val entity = findLotEntityOrThrow(lotId)
        validateOwnership(entity, sellerId)
        validateEditable(entity)

        // Validate category if changing
        request.categoryId?.let { newCategoryId ->
            categoryRepository.findById(newCategoryId)
                ?: throw NotFoundException(
                    code = "CATEGORY_NOT_FOUND",
                    message = "Category with id '$newCategoryId' not found."
                )
        }

        request.title?.let { entity.title = it }
        request.description?.let { entity.description = it }
        request.categoryId?.let { entity.categoryId = it }
        request.specifications?.let { entity.specifications = it }
        request.locationLat?.let { entity.locationLat = it }
        request.locationLng?.let { entity.locationLng = it }
        request.locationAddress?.let { entity.locationAddress = it }
        request.locationCountry?.let { entity.locationCountry = it }
        request.locationCity?.let { entity.locationCity = it }
        request.reservePrice?.let { entity.reservePrice = it }
        request.startingBid?.let { entity.startingBid = it }
        request.co2AvoidedKg?.let { entity.co2AvoidedKg = it }
        request.pickupInfo?.let { entity.pickupInfo = it }
        entity.updatedAt = Instant.now()

        lotRepository.persist(entity)

        LOG.infof("Updated lot: id=%s", lotId)

        return entity.toDomain()
    }

    // -------------------------------------------------------------------------
    // Lifecycle transitions
    // -------------------------------------------------------------------------

    /**
     * Submits a DRAFT lot for review by platform staff.
     *
     * Transitions the lot from DRAFT to PENDING_REVIEW.
     *
     * @param lotId    The lot identifier.
     * @param sellerId The authenticated seller's user identifier.
     * @return The updated [Lot] domain model.
     * @throws ConflictException if the lot is not in DRAFT status.
     */
    @Transactional
    fun submitForReview(lotId: UUID, sellerId: UUID): Lot {
        val entity = findLotEntityOrThrow(lotId)
        validateOwnership(entity, sellerId)

        if (!LotStatus.valueOf(entity.status.name).canSubmitForReview()) {
            throw ConflictException(
                code = "INVALID_LOT_STATUS",
                message = "Lot '$lotId' cannot be submitted for review (current status: ${entity.status})."
            )
        }

        entity.status = LotStatus.PENDING_REVIEW
        entity.updatedAt = Instant.now()
        lotRepository.persist(entity)

        LOG.infof("Lot submitted for review: id=%s", lotId)

        return entity.toDomain()
    }

    /**
     * Approves a lot that is pending review.
     *
     * Transitions the lot from PENDING_REVIEW to APPROVED. This is an
     * admin/moderator operation.
     *
     * @param lotId The lot identifier.
     * @return The updated [Lot] domain model.
     * @throws ConflictException if the lot is not in PENDING_REVIEW status.
     */
    @Transactional
    fun approveLot(lotId: UUID): Lot {
        val entity = findLotEntityOrThrow(lotId)

        if (!LotStatus.valueOf(entity.status.name).canApprove()) {
            throw ConflictException(
                code = "INVALID_LOT_STATUS",
                message = "Lot '$lotId' cannot be approved (current status: ${entity.status})."
            )
        }

        entity.status = LotStatus.APPROVED
        entity.updatedAt = Instant.now()
        lotRepository.persist(entity)

        LOG.infof("Lot approved: id=%s", lotId)

        return entity.toDomain()
    }

    /**
     * Withdraws a lot from the catalog.
     *
     * The lot can be withdrawn from any non-terminal state. Once withdrawn,
     * the lot cannot be reactivated.
     *
     * @param lotId    The lot identifier.
     * @param sellerId The authenticated seller's user identifier (null for admin withdrawal).
     * @return The updated [Lot] domain model.
     * @throws ConflictException if the lot is already in a terminal state.
     */
    @Transactional
    fun withdrawLot(lotId: UUID, sellerId: UUID? = null): Lot {
        val entity = findLotEntityOrThrow(lotId)

        if (sellerId != null) {
            validateOwnership(entity, sellerId)
        }

        if (entity.status.isTerminal()) {
            throw ConflictException(
                code = "LOT_ALREADY_TERMINAL",
                message = "Lot '$lotId' is already in terminal state: ${entity.status}."
            )
        }

        entity.status = LotStatus.WITHDRAWN
        entity.updatedAt = Instant.now()
        lotRepository.persist(entity)

        LOG.infof("Lot withdrawn: id=%s, by=%s", lotId, sellerId ?: "admin")

        return entity.toDomain()
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Retrieves a lot by its identifier, including its images.
     *
     * @param lotId The lot identifier.
     * @return A pair of the [Lot] domain model and its associated [LotImage] list.
     * @throws NotFoundException if the lot does not exist.
     */
    fun getLotById(lotId: UUID): Pair<Lot, List<LotImage>> {
        val entity = findLotEntityOrThrow(lotId)
        val images = lotImageRepository.findByLotId(lotId).map { it.toDomain() }
        return Pair(entity.toDomain(), images)
    }

    /**
     * Lists lots with optional filtering and pagination.
     *
     * @param filter The filtering and pagination parameters.
     * @return A pair of the lot list and the total count matching the filter.
     */
    fun listLots(filter: LotListFilter): Pair<List<Lot>, Long> {
        return when {
            filter.sellerId != null -> {
                val lots = lotRepository.findBySellerId(filter.sellerId, filter.page, filter.pageSize)
                    .map { it.toDomain() }
                val total = lotRepository.countBySellerId(filter.sellerId)
                Pair(lots, total)
            }
            filter.auctionId != null -> {
                val lots = lotRepository.findByAuctionId(filter.auctionId, filter.page, filter.pageSize)
                    .map { it.toDomain() }
                val total = lotRepository.countByAuctionId(filter.auctionId)
                Pair(lots, total)
            }
            filter.categoryId != null -> {
                val lots = lotRepository.findByCategoryId(filter.categoryId, filter.page, filter.pageSize)
                    .map { it.toDomain() }
                val total = lotRepository.countByCategoryId(filter.categoryId)
                Pair(lots, total)
            }
            filter.brand != null -> {
                val lots = lotRepository.findByBrand(filter.brand, filter.page, filter.pageSize)
                    .map { it.toDomain() }
                val total = lotRepository.countByBrand(filter.brand)
                Pair(lots, total)
            }
            filter.status != null -> {
                val lots = lotRepository.findByStatus(filter.status, filter.page, filter.pageSize)
                    .map { it.toDomain() }
                val total = lotRepository.countByStatus(filter.status)
                Pair(lots, total)
            }
            filter.country != null -> {
                val lots = lotRepository.findByCountry(filter.country, null, filter.page, filter.pageSize)
                    .map { it.toDomain() }
                val total = lotRepository.countByCountry(filter.country)
                Pair(lots, total)
            }
            else -> {
                val lots = lotRepository.findAll()
                    .page(io.quarkus.panache.common.Page.of(filter.page, filter.pageSize))
                    .list()
                    .map { it.toDomain() }
                val total = lotRepository.count()
                Pair(lots, total)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Combine lots
    // -------------------------------------------------------------------------

    /**
     * Combines multiple DRAFT lots into a single lot.
     *
     * All source lots must belong to the same seller and be in DRAFT status.
     * The combined lot inherits the category, location, and brand from the
     * first source lot. Source lots are marked as WITHDRAWN after combination.
     *
     * @param sellerId The authenticated seller's user identifier.
     * @param request  The combination details (source lot IDs, new title/description).
     * @return The newly created combined [Lot] domain model.
     * @throws ValidationException if fewer than 2 lots are provided.
     * @throws ForbiddenException  if any source lot is not owned by the seller.
     * @throws ConflictException   if any source lot is not in DRAFT status.
     */
    @Transactional
    fun combineLots(sellerId: UUID, request: CombineLotsRequest): Lot {
        if (request.lotIds.size < 2) {
            throw ValidationException(
                field = "lotIds",
                error = "At least 2 lots are required for combination."
            )
        }

        val sourceEntities = request.lotIds.map { lotId ->
            val entity = findLotEntityOrThrow(lotId)
            validateOwnership(entity, sellerId)
            if (entity.status != LotStatus.DRAFT) {
                throw ConflictException(
                    code = "LOT_NOT_DRAFT",
                    message = "Lot '$lotId' must be in DRAFT status to be combined (current: ${entity.status})."
                )
            }
            entity
        }

        val firstLot = sourceEntities.first()

        // Aggregate starting bids from all source lots
        val combinedStartingBid = sourceEntities
            .map { it.startingBid }
            .reduce { acc, bid -> acc.add(bid) }

        // Create the combined lot
        val combinedLot = Lot(
            id = IdGenerator.generateUUIDv7(),
            sellerId = sellerId,
            brand = firstLot.brand,
            title = request.title,
            description = request.description,
            categoryId = firstLot.categoryId,
            specifications = firstLot.specifications,
            locationLat = firstLot.locationLat,
            locationLng = firstLot.locationLng,
            locationAddress = firstLot.locationAddress,
            locationCountry = firstLot.locationCountry,
            locationCity = firstLot.locationCity,
            startingBid = combinedStartingBid,
            status = LotStatus.DRAFT,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        lotRepository.persist(LotEntity.fromDomain(combinedLot))

        // Withdraw source lots
        sourceEntities.forEach { entity ->
            entity.status = LotStatus.WITHDRAWN
            entity.updatedAt = Instant.now()
            lotRepository.persist(entity)
        }

        LOG.infof("Combined %d lots into new lot: id=%s, title=%s",
            request.lotIds.size, combinedLot.id, combinedLot.title)

        return combinedLot
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun findLotEntityOrThrow(lotId: UUID): LotEntity {
        return lotRepository.findById(lotId)
            ?: throw NotFoundException(
                code = "LOT_NOT_FOUND",
                message = "Lot with id '$lotId' not found."
            )
    }

    private fun validateOwnership(entity: LotEntity, sellerId: UUID) {
        if (entity.sellerId != sellerId) {
            throw ForbiddenException(
                code = "NOT_LOT_OWNER",
                message = "You do not have permission to modify this lot."
            )
        }
    }

    private fun validateEditable(entity: LotEntity) {
        if (!entity.status.isEditable()) {
            throw ConflictException(
                code = "LOT_NOT_EDITABLE",
                message = "Lot '${entity.id}' is not editable (current status: ${entity.status})."
            )
        }
    }
}
