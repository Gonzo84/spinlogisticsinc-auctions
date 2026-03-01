package eu.auctionplatform.auction.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import eu.auctionplatform.auction.domain.command.CreateAuctionCommand
import eu.auctionplatform.auction.domain.event.AuctionClosedEvent
import eu.auctionplatform.auction.domain.event.LotAwardedEvent
import eu.auctionplatform.auction.domain.model.Auction
import eu.auctionplatform.auction.infrastructure.persistence.entity.AuctionEventEntity
import eu.auctionplatform.auction.infrastructure.persistence.entity.OutboxEntity
import eu.auctionplatform.auction.infrastructure.persistence.repository.AuctionEventRepository
import eu.auctionplatform.auction.infrastructure.persistence.repository.AuctionReadModel
import eu.auctionplatform.auction.infrastructure.persistence.repository.AuctionReadModelRepository
import eu.auctionplatform.auction.infrastructure.persistence.repository.OutboxRepository
import eu.auctionplatform.commons.domain.AuctionId
import eu.auctionplatform.commons.domain.DomainEvent
import eu.auctionplatform.commons.exception.ConflictException
import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.commons.messaging.NatsSubjects
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.Instant

/**
 * Result of closing an auction.
 *
 * @property auctionId Identifier of the closed auction.
 * @property finalBid The final highest bid, or null if no bids were placed.
 * @property winnerId The winning bidder's identifier, or null if unsold.
 * @property reserveMet Whether the reserve price was met at closing.
 */
data class AuctionCloseResult(
    val auctionId: String,
    val finalBid: java.math.BigDecimal?,
    val winnerId: String?,
    val reserveMet: Boolean
)

/**
 * Result of awarding a lot to the winning bidder.
 *
 * @property auctionId Identifier of the awarded auction.
 * @property winnerId The winning bidder's identifier.
 * @property hammerPrice The final hammer price.
 */
data class AwardResult(
    val auctionId: String,
    val winnerId: String,
    val hammerPrice: java.math.BigDecimal
)

/**
 * Application service managing the auction lifecycle: creation, closing,
 * awarding, and cancellation.
 *
 * Each operation follows the event-sourcing pattern:
 * 1. Reconstitute the aggregate from the event store.
 * 2. Execute the domain command.
 * 3. Persist new events with optimistic locking.
 * 4. Write to the outbox for NATS publication.
 * 5. Update the denormalized read model.
 */
@ApplicationScoped
class AuctionLifecycleService @Inject constructor(
    private val eventRepository: AuctionEventRepository,
    private val outboxRepository: OutboxRepository,
    private val readModelRepository: AuctionReadModelRepository,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(AuctionLifecycleService::class.java)
    }

    /**
     * Creates a new auction from the given command.
     *
     * Instantiates an Auction aggregate via its factory method, persists the
     * resulting creation event(s), creates the initial read model projection,
     * and writes to the outbox.
     *
     * @param command The auction creation command (validated at construction).
     * @return The generated [AuctionId].
     */
    @Transactional
    fun createAuction(command: CreateAuctionCommand): AuctionId {
        // Create the aggregate via its companion factory method
        val auction = Auction.create(command)
        val newEvents = auction.uncommittedEvents

        if (newEvents.isEmpty()) {
            throw ConflictException(
                code = "AUCTION_CREATION_FAILED",
                message = "Auction creation produced no events"
            )
        }

        val auctionId = AuctionId.fromString(newEvents.first().aggregateId)
        val eventEntities = newEvents.map { AuctionEventEntity.fromDomainEvent(it) }

        val success = eventRepository.appendEvents(auctionId.value, eventEntities, 0)
        if (!success) {
            throw ConflictException(
                code = "AUCTION_ALREADY_EXISTS",
                message = "Auction $auctionId already exists"
            )
        }

        // Write to outbox
        for (event in newEvents) {
            val subject = resolveNatsSubject(event)
            outboxRepository.save(OutboxEntity.fromDomainEvent(event, subject))
        }

        // Create initial read model
        val readModel = AuctionReadModel(
            auctionId = auctionId.value,
            lotId = command.lotId.value,
            brand = command.brand.code,
            status = "ACTIVE",
            startTime = command.startTime,
            endTime = command.endTime,
            originalEndTime = command.endTime,
            startingBid = command.startingBid.amount,
            currentHighBid = null,
            currentHighBidderId = null,
            bidCount = 0,
            reserveMet = command.reservePrice == null,
            extensionCount = 0,
            sellerId = command.sellerId.value,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        readModelRepository.save(readModel)

        auction.markEventsAsCommitted()

        LOG.infof("Created auction %s for lot %s (brand=%s)",
            auctionId, command.lotId, command.brand.code)

        return auctionId
    }

    /**
     * Closes an auction that has reached its end time.
     *
     * Reconstitutes the auction aggregate, executes the close command, persists
     * the resulting events, and updates the read model.
     *
     * @param auctionId The auction to close.
     * @return [AuctionCloseResult] with the closing summary.
     * @throws NotFoundException if the auction does not exist.
     */
    @Transactional
    fun closeAuction(auctionId: AuctionId): AuctionCloseResult {
        val auction = loadAuction(auctionId)
        val expectedVersion = auction.version

        // Execute close command on the aggregate
        val newEvents = auction.close()

        if (newEvents.isEmpty()) {
            LOG.warnf("Close command produced no events for auction %s", auctionId)
            return AuctionCloseResult(
                auctionId = auctionId.toString(),
                finalBid = null,
                winnerId = null,
                reserveMet = false
            )
        }

        val eventEntities = newEvents.map { AuctionEventEntity.fromDomainEvent(it) }
        val success = eventRepository.appendEvents(auctionId.value, eventEntities, expectedVersion)
        if (!success) {
            throw ConflictException(
                code = "CONCURRENCY_CONFLICT",
                message = "Failed to close auction $auctionId due to concurrent modification"
            )
        }

        for (event in newEvents) {
            val subject = resolveNatsSubject(event)
            outboxRepository.save(OutboxEntity.fromDomainEvent(event, subject))
            readModelRepository.updateFromEvent(event)
        }

        auction.markEventsAsCommitted()

        LOG.infof("Closed auction %s", auctionId)

        // Extract close result from the AuctionClosedEvent
        val closedEvent = newEvents.filterIsInstance<AuctionClosedEvent>().firstOrNull()

        return AuctionCloseResult(
            auctionId = auctionId.toString(),
            finalBid = closedEvent?.finalBidAmount,
            winnerId = closedEvent?.winnerId,
            reserveMet = closedEvent?.reserveMet ?: false
        )
    }

    /**
     * Awards a lot to the winning bidder after the auction has closed.
     *
     * The auction must be in CLOSED status, have a winning bid, and (if
     * applicable) the reserve price must have been met.
     *
     * @param auctionId The auction to award.
     * @return [AwardResult] with the award details.
     * @throws NotFoundException if the auction does not exist.
     */
    @Transactional
    fun awardLot(auctionId: AuctionId): AwardResult {
        val auction = loadAuction(auctionId)
        val expectedVersion = auction.version

        // Execute award command on the aggregate
        val newEvents = auction.award()

        if (newEvents.isEmpty()) {
            throw ConflictException(
                code = "AWARD_FAILED",
                message = "Award command produced no events for auction $auctionId"
            )
        }

        val eventEntities = newEvents.map { AuctionEventEntity.fromDomainEvent(it) }
        val success = eventRepository.appendEvents(auctionId.value, eventEntities, expectedVersion)
        if (!success) {
            throw ConflictException(
                code = "CONCURRENCY_CONFLICT",
                message = "Failed to award lot for auction $auctionId due to concurrent modification"
            )
        }

        for (event in newEvents) {
            val subject = resolveNatsSubject(event)
            outboxRepository.save(OutboxEntity.fromDomainEvent(event, subject))
            readModelRepository.updateFromEvent(event)
        }

        auction.markEventsAsCommitted()

        LOG.infof("Awarded lot for auction %s", auctionId)

        // Extract award details from the LotAwardedEvent
        val awardedEvent = newEvents.filterIsInstance<LotAwardedEvent>().firstOrNull()

        return AwardResult(
            auctionId = auctionId.toString(),
            winnerId = awardedEvent?.winnerId ?: "",
            hammerPrice = awardedEvent?.winningBidAmount ?: java.math.BigDecimal.ZERO
        )
    }

    /**
     * Cancels an auction with the given reason.
     *
     * An auction can be cancelled from any non-terminal state. This is an
     * administrative operation typically performed by ops or super admins.
     *
     * @param auctionId The auction to cancel.
     * @param reason Human-readable cancellation reason.
     * @param cancelledBy Optional identifier of the user initiating cancellation.
     * @throws NotFoundException if the auction does not exist.
     */
    @Transactional
    fun cancelAuction(auctionId: AuctionId, reason: String, cancelledBy: String? = null) {
        val auction = loadAuction(auctionId)
        val expectedVersion = auction.version

        // Execute cancel command on the aggregate
        val newEvents = auction.cancel(reason, cancelledBy)

        if (newEvents.isNotEmpty()) {
            val eventEntities = newEvents.map { AuctionEventEntity.fromDomainEvent(it) }
            val success = eventRepository.appendEvents(auctionId.value, eventEntities, expectedVersion)
            if (!success) {
                throw ConflictException(
                    code = "CONCURRENCY_CONFLICT",
                    message = "Failed to cancel auction $auctionId due to concurrent modification"
                )
            }

            for (event in newEvents) {
                val subject = resolveNatsSubject(event)
                outboxRepository.save(OutboxEntity.fromDomainEvent(event, subject))
                readModelRepository.updateFromEvent(event)
            }
        }

        auction.markEventsAsCommitted()

        LOG.infof("Cancelled auction %s (reason=%s)", auctionId, reason)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Loads and reconstitutes an Auction aggregate from the event store.
     *
     * @param auctionId The auction identifier.
     * @return The reconstituted [Auction] aggregate.
     * @throws NotFoundException if no events exist for the given aggregate.
     */
    private fun loadAuction(auctionId: AuctionId): Auction {
        val eventEntities = eventRepository.findByAggregateId(auctionId.value)
        if (eventEntities.isEmpty()) {
            throw NotFoundException(
                code = "AUCTION_NOT_FOUND",
                message = "Auction $auctionId not found"
            )
        }
        val domainEvents = eventEntities.map { it.toDomainEvent() }
        return Auction.reconstitute(domainEvents)
    }

    /**
     * Resolves the NATS subject for a given domain event.
     * Brand is propagated via NATS message headers (set by OutboxPublisher), not subject prefix.
     */
    private fun resolveNatsSubject(event: DomainEvent): String {
        return when (event.eventType) {
            "AuctionCreatedEvent" -> "auction.lot.created"
            "BidPlacedEvent" -> NatsSubjects.AUCTION_BID_PLACED
            "ProxyBidTriggeredEvent" -> NatsSubjects.AUCTION_BID_PROXY
            "AuctionExtendedEvent" -> NatsSubjects.AUCTION_LOT_EXTENDED
            "AuctionClosedEvent" -> NatsSubjects.AUCTION_LOT_CLOSED
            "LotAwardedEvent" -> NatsSubjects.AUCTION_LOT_AWARDED
            "AuctionCancelledEvent" -> "auction.lot.cancelled"
            "ReserveMetEvent" -> "auction.reserve.met"
            "AutoBidSetEvent" -> "auction.autobid.set"
            "AutoBidExhaustedEvent" -> "auction.autobid.exhausted"
            else -> "auction.events.${event.eventType}"
        }
    }
}
