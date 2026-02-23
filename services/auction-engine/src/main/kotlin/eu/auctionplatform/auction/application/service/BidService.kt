package eu.auctionplatform.auction.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import eu.auctionplatform.auction.domain.command.PlaceBidCommand
import eu.auctionplatform.auction.domain.command.SetAutoBidCommand
import eu.auctionplatform.auction.domain.event.AuctionExtendedEvent
import eu.auctionplatform.auction.domain.event.BidPlacedEvent
import eu.auctionplatform.auction.domain.model.Auction
import eu.auctionplatform.auction.infrastructure.persistence.entity.AuctionEventEntity
import eu.auctionplatform.auction.infrastructure.persistence.entity.OutboxEntity
import eu.auctionplatform.auction.infrastructure.persistence.repository.AuctionEventRepository
import eu.auctionplatform.auction.infrastructure.persistence.repository.AuctionReadModelRepository
import eu.auctionplatform.auction.infrastructure.persistence.repository.OutboxRepository
import eu.auctionplatform.commons.domain.DomainEvent
import eu.auctionplatform.commons.exception.ConflictException
import eu.auctionplatform.commons.exception.NotFoundException
import eu.auctionplatform.commons.messaging.NatsSubjects
import eu.auctionplatform.commons.util.JsonMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant

/**
 * Result of a successful bid placement.
 *
 * @property bidId Unique identifier of the newly placed bid.
 * @property auctionId Identifier of the auction the bid was placed on.
 * @property newHighBid The new highest bid amount after this bid.
 * @property closingTime The current closing time (may have been extended by anti-sniping).
 * @property reserveStatus Whether the reserve price has been met ("MET", "NOT_MET", "NO_RESERVE").
 * @property extensionApplied Whether an anti-sniping extension was triggered by this bid.
 */
data class BidResult(
    val bidId: String,
    val auctionId: String,
    val newHighBid: BigDecimal,
    val closingTime: Instant,
    val reserveStatus: String,
    val extensionApplied: Boolean
)

/**
 * Result of setting an automatic (proxy) bid.
 *
 * @property auctionId Identifier of the auction.
 * @property maxAmount The configured maximum auto-bid amount.
 * @property currentBidAmount The current effective bid amount from this auto-bid.
 */
data class AutoBidResult(
    val auctionId: String,
    val maxAmount: BigDecimal,
    val currentBidAmount: BigDecimal
)

/**
 * Application service responsible for bid placement and auto-bid management.
 *
 * Orchestrates the command-handling lifecycle:
 * 1. Reconstitute the auction aggregate from the event store.
 * 2. Execute the domain command (producing new events).
 * 3. Persist events with optimistic locking (with retry on conflict).
 * 4. Write to the outbox in the same transaction.
 * 5. Update the read model projection.
 */
@ApplicationScoped
class BidService @Inject constructor(
    private val eventRepository: AuctionEventRepository,
    private val outboxRepository: OutboxRepository,
    private val readModelRepository: AuctionReadModelRepository,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(BidService::class.java)

    companion object {
        /** Maximum number of retry attempts on optimistic concurrency conflict. */
        private const val MAX_RETRIES = 3
    }

    /**
     * Places a bid on an auction.
     *
     * Loads the auction aggregate from the event store, executes the bid
     * command, and persists the resulting events. Retries up to [MAX_RETRIES]
     * times on optimistic concurrency conflicts.
     *
     * @param command The bid placement command.
     * @return [BidResult] with the outcome of the bid.
     * @throws NotFoundException if the auction does not exist.
     * @throws ConflictException if all retry attempts are exhausted.
     */
    @Transactional
    fun placeBid(command: PlaceBidCommand): BidResult {
        var attempt = 0

        while (attempt < MAX_RETRIES) {
            attempt++

            // 1. Load auction from event store
            val eventEntities = eventRepository.findByAggregateId(command.auctionId.value)
            if (eventEntities.isEmpty()) {
                throw NotFoundException(
                    code = "AUCTION_NOT_FOUND",
                    message = "Auction ${command.auctionId} not found"
                )
            }

            // 2. Reconstitute aggregate from events
            val domainEvents = eventEntities.map { it.toDomainEvent() }
            val auction = Auction.reconstitute(domainEvents)
            val expectedVersion = auction.version

            // 3. Execute domain command -- produces new events via raise()
            val newEvents = auction.placeBid(command)

            if (newEvents.isEmpty()) {
                throw ConflictException(
                    code = "BID_REJECTED",
                    message = "Bid was rejected by domain rules"
                )
            }

            // 4. Persist events with optimistic locking
            val eventEntitiesNew = newEvents.map { AuctionEventEntity.fromDomainEvent(it) }
            val success = eventRepository.appendEvents(
                command.auctionId.value,
                eventEntitiesNew,
                expectedVersion
            )

            if (!success) {
                logger.warn(
                    "Optimistic concurrency conflict on attempt {}/{} for auction {}",
                    attempt, MAX_RETRIES, command.auctionId
                )
                if (attempt >= MAX_RETRIES) {
                    throw ConflictException(
                        code = "CONCURRENCY_CONFLICT",
                        message = "Failed to place bid after $MAX_RETRIES attempts due to concurrent modifications"
                    )
                }
                continue
            }

            // 5. Write to outbox (same transaction)
            for (event in newEvents) {
                val subject = resolveNatsSubject(event)
                outboxRepository.save(OutboxEntity.fromDomainEvent(event, subject))
            }

            // 6. Update read model
            for (event in newEvents) {
                readModelRepository.updateFromEvent(event)
            }

            auction.markEventsAsCommitted()

            // 7. Build result from the domain events
            val bidPlacedEvent = newEvents.filterIsInstance<BidPlacedEvent>().firstOrNull()
            val extensionApplied = newEvents.any { it is AuctionExtendedEvent }

            val reserveStatus = when {
                auction.reservePrice == null -> "NO_RESERVE"
                auction.reserveMet -> "MET"
                else -> "NOT_MET"
            }

            return BidResult(
                bidId = bidPlacedEvent?.bidId ?: "",
                auctionId = command.auctionId.toString(),
                newHighBid = auction.currentHighBid?.amount ?: command.amount.amount,
                closingTime = auction.endTime,
                reserveStatus = reserveStatus,
                extensionApplied = extensionApplied
            )
        }

        throw ConflictException(
            code = "CONCURRENCY_CONFLICT",
            message = "Failed to place bid after $MAX_RETRIES attempts"
        )
    }

    /**
     * Configures or updates an automatic (proxy) bid for a user on an auction.
     *
     * @param command The auto-bid configuration command.
     * @return [AutoBidResult] confirming the auto-bid setup.
     * @throws NotFoundException if the auction does not exist.
     */
    @Transactional
    fun setAutoBid(command: SetAutoBidCommand): AutoBidResult {
        var attempt = 0

        while (attempt < MAX_RETRIES) {
            attempt++

            val eventEntities = eventRepository.findByAggregateId(command.auctionId.value)
            if (eventEntities.isEmpty()) {
                throw NotFoundException(
                    code = "AUCTION_NOT_FOUND",
                    message = "Auction ${command.auctionId} not found"
                )
            }

            val domainEvents = eventEntities.map { it.toDomainEvent() }
            val auction = Auction.reconstitute(domainEvents)
            val expectedVersion = auction.version

            // Execute auto-bid command on the aggregate
            val newEvents = auction.setAutoBid(command)

            if (newEvents.isEmpty()) {
                // Auto-bid was set without producing new events -- return current state
                return AutoBidResult(
                    auctionId = command.auctionId.toString(),
                    maxAmount = command.maxAmount.amount,
                    currentBidAmount = auction.currentHighBid?.amount ?: command.maxAmount.amount
                )
            }

            val eventEntitiesNew = newEvents.map { AuctionEventEntity.fromDomainEvent(it) }
            val success = eventRepository.appendEvents(
                command.auctionId.value,
                eventEntitiesNew,
                expectedVersion
            )

            if (!success) {
                logger.warn(
                    "Optimistic concurrency conflict on auto-bid attempt {}/{} for auction {}",
                    attempt, MAX_RETRIES, command.auctionId
                )
                if (attempt >= MAX_RETRIES) {
                    throw ConflictException(
                        code = "CONCURRENCY_CONFLICT",
                        message = "Failed to set auto-bid after $MAX_RETRIES attempts"
                    )
                }
                continue
            }

            // Write to outbox and update read model
            for (event in newEvents) {
                val subject = resolveNatsSubject(event)
                outboxRepository.save(OutboxEntity.fromDomainEvent(event, subject))
                readModelRepository.updateFromEvent(event)
            }

            auction.markEventsAsCommitted()

            return AutoBidResult(
                auctionId = command.auctionId.toString(),
                maxAmount = command.maxAmount.amount,
                currentBidAmount = auction.currentHighBid?.amount ?: command.maxAmount.amount
            )
        }

        throw ConflictException(
            code = "CONCURRENCY_CONFLICT",
            message = "Failed to set auto-bid after $MAX_RETRIES attempts"
        )
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Resolves the NATS subject for a given domain event, including brand prefix.
     */
    private fun resolveNatsSubject(event: DomainEvent): String {
        val baseSubject = when (event.eventType) {
            "BidPlacedEvent" -> NatsSubjects.AUCTION_BID_PLACED
            "ProxyBidTriggeredEvent" -> NatsSubjects.AUCTION_BID_PROXY
            "AuctionExtendedEvent" -> NatsSubjects.AUCTION_LOT_EXTENDED
            "AuctionClosedEvent" -> NatsSubjects.AUCTION_LOT_CLOSED
            "LotAwardedEvent" -> NatsSubjects.AUCTION_LOT_AWARDED
            else -> "auction.events.${event.eventType}"
        }
        return NatsSubjects.withBrand(baseSubject, event.brand)
    }
}
