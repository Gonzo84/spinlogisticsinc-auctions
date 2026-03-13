package eu.auctionplatform.auction.domain.model

import eu.auctionplatform.auction.domain.command.CreateAuctionCommand
import eu.auctionplatform.auction.domain.command.PlaceBidCommand
import eu.auctionplatform.auction.domain.command.SetAutoBidCommand
import eu.auctionplatform.auction.domain.event.*
import eu.auctionplatform.auction.domain.exception.AuctionException
import eu.auctionplatform.commons.domain.*
import eu.auctionplatform.commons.util.IdGenerator
import java.time.Instant
import java.util.UUID

/**
 * Auction aggregate root — the central domain entity of the EU B2B auction platform.
 *
 * This class implements the event-sourcing pattern: all state mutations occur through
 * domain events. Commands are validated against the current state, and if valid,
 * produce one or more events which are then applied to update the aggregate's state.
 *
 * ## Invariants
 *
 * - Only one bid can be the "current high bid" at any point in time.
 * - The seller of the lot cannot bid on their own auction.
 * - Bids must meet the minimum increment for the current price tier.
 * - Anti-sniping rules extend the end time when bids arrive within the
 *   configured window, up to [AuctionConstants.MAX_EXTENSIONS].
 * - Proxy (auto) bids are processed atomically with the triggering bid.
 * - Reserve price status is tracked and the [ReserveMetEvent] is raised
 *   exactly once per auction.
 *
 * ## Thread safety
 *
 * This aggregate is **not** thread-safe. The application layer must ensure that
 * concurrent commands targeting the same auction are serialised (e.g. via
 * optimistic locking on the event stream version).
 */
class Auction private constructor() : AggregateRoot() {

    // -----------------------------------------------------------------------
    // State fields — mutated only via apply(event)
    // -----------------------------------------------------------------------

    var id: AuctionId = AuctionId(NIL_UUID)
        private set

    var lotId: LotId = LotId(NIL_UUID)
        private set

    lateinit var brand: Brand
        private set

    var status: AuctionStatus = AuctionStatus.SCHEDULED
        private set

    lateinit var startTime: Instant
        private set

    lateinit var endTime: Instant
        private set

    lateinit var originalEndTime: Instant
        private set

    lateinit var startingBid: Money
        private set

    var reservePrice: Money? = null
        private set

    var reserveMet: Boolean = false
        private set

    var currentHighBid: Money? = null
        private set

    var currentHighBidderId: UserId? = null
        private set

    var bidCount: Int = 0
        private set

    val bids: MutableList<Bid> = mutableListOf()

    val autoBids: MutableMap<UserId, AutoBid> = mutableMapOf()

    var extensionCount: Int = 0
        private set

    var sellerId: UserId = UserId(NIL_UUID)
        private set

    var featured: Boolean = false
        private set

    var featuredAt: Instant? = null
        private set

    /** Set of blocked user IDs — populated from external context before command handling. */
    private val blockedBidders: MutableSet<UserId> = mutableSetOf()

    /** Set of user IDs who have placed the required deposit. */
    private val depositHolders: MutableSet<UserId> = mutableSetOf()

    // -----------------------------------------------------------------------
    // Factory methods
    // -----------------------------------------------------------------------

    companion object {

        /** Sentinel UUID used as a default before the aggregate is hydrated from events. */
        private val NIL_UUID: UUID = UUID(0, 0)

        /**
         * Creates a new auction from the given [command].
         *
         * This is the only way to instantiate a new Auction. It validates
         * the command parameters, generates identifiers, and raises the
         * [AuctionCreatedEvent].
         *
         * @param command The creation command containing all required fields.
         * @return A new [Auction] instance with one uncommitted event.
         */
        fun create(command: CreateAuctionCommand): Auction {
            val auction = Auction()
            val auctionId = AuctionId.generate()

            val event = AuctionCreatedEvent(
                eventId = IdGenerator.generateString(),
                aggregateId = auctionId.toString(),
                brand = command.brand.code,
                timestamp = Instant.now(),
                version = 1L,
                lotId = command.lotId.toString(),
                startTime = command.startTime,
                endTime = command.endTime,
                startingBidAmount = command.startingBid.amount,
                startingBidCurrency = command.startingBid.currency.currencyCode,
                reservePriceAmount = command.reservePrice?.amount,
                reservePriceCurrency = command.reservePrice?.currency?.currencyCode,
                sellerId = command.sellerId.toString()
            )

            auction.raise(event)
            return auction
        }

        /**
         * Reconstitutes an auction aggregate from its event history.
         *
         * This is used when loading an auction from the event store for
         * command handling or read-model projection.
         *
         * @param events The full ordered list of persisted domain events
         *               for this auction aggregate.
         * @return A fully hydrated [Auction] instance with no uncommitted events.
         * @throws IllegalArgumentException if [events] is empty.
         */
        fun reconstitute(events: List<DomainEvent>): Auction {
            require(events.isNotEmpty()) {
                "Cannot reconstitute an auction from an empty event list"
            }
            val auction = Auction()
            auction.loadFromHistory(events)
            return auction
        }
    }

    // -----------------------------------------------------------------------
    // External context setters (called by the application layer)
    // -----------------------------------------------------------------------

    /**
     * Registers a user as blocked from bidding on this auction.
     * Must be called by the application layer before processing bid commands.
     */
    fun blockBidder(userId: UserId) {
        blockedBidders.add(userId)
    }

    /**
     * Registers that a user has placed the required deposit.
     * Must be called by the application layer before processing bid commands.
     */
    fun registerDeposit(userId: UserId) {
        depositHolders.add(userId)
    }

    // -----------------------------------------------------------------------
    // Command handlers
    // -----------------------------------------------------------------------

    /**
     * Places a bid on this auction.
     *
     * This method validates all business invariants, raises the appropriate
     * domain events, and processes any triggered auto-bids atomically.
     *
     * ## Event flow
     *
     * 1. [BidPlacedEvent] — the direct bid.
     * 2. [AuctionExtendedEvent] — if the bid falls within the anti-sniping window.
     * 3. [ProxyBidTriggeredEvent] (zero or more) — counter-bids from auto-bid engine.
     * 4. [ReserveMetEvent] — if the bid or any resulting proxy bid meets the reserve.
     * 5. [AutoBidExhaustedEvent] (zero or more) — for auto-bids that can no longer counter.
     *
     * @param command The bid command.
     * @return The list of domain events produced by this command.
     * @throws AuctionException if any validation fails.
     */
    fun placeBid(command: PlaceBidCommand): List<DomainEvent> {
        // --- Validations ---
        validateAuctionAcceptsBids()
        validateNotSeller(command.bidderId)
        validateNotBlocked(command.bidderId)
        validateDeposit(command.bidderId)
        validateBidAmount(command.amount)

        val events = mutableListOf<DomainEvent>()
        val bidId = BidId.generate()
        val now = Instant.now()

        // --- Create BidPlacedEvent ---
        val bidPlacedEvent = BidPlacedEvent(
            eventId = IdGenerator.generateString(),
            aggregateId = id.toString(),
            brand = brand.code,
            timestamp = now,
            version = version + 1,
            bidId = bidId.toString(),
            bidderId = command.bidderId.toString(),
            bidAmount = command.amount.amount,
            bidCurrency = command.amount.currency.currencyCode,
            isProxy = command.isProxy,
            previousHighBidAmount = currentHighBid?.amount,
            previousHighBidCurrency = currentHighBid?.currency?.currencyCode,
            previousHighBidderId = currentHighBidderId?.toString()
        )
        raise(bidPlacedEvent)
        events.add(bidPlacedEvent)

        // --- Anti-sniping rule ---
        val extensionEvent = applyAntiSnipingRule(now)
        if (extensionEvent != null) {
            raise(extensionEvent)
            events.add(extensionEvent)
        }

        // --- Process auto-bids ---
        val newBid = Bid(
            id = bidId,
            bidderId = command.bidderId,
            amount = command.amount,
            timestamp = now,
            isProxy = command.isProxy,
            status = BidStatus.ACTIVE
        )
        val autoBidEvents = processAutoBids(newBid)
        autoBidEvents.forEach { event ->
            raise(event)
            events.add(event)
        }

        // --- Check reserve ---
        // Check against the current high bid after all auto-bids have been processed
        if (!reserveMet && currentHighBid != null) {
            val reserveEvent = checkReserve(currentHighBid!!)
            if (reserveEvent != null) {
                raise(reserveEvent)
                events.add(reserveEvent)
            }
        }

        return events
    }

    /**
     * Configures or updates an automatic (proxy) bid for a user.
     *
     * If the auto-bid's max amount exceeds the current high bid, the engine
     * immediately places a bid at the minimum required amount.
     *
     * @param command The auto-bid configuration command.
     * @return The list of domain events produced.
     * @throws AuctionException if validation fails.
     */
    fun setAutoBid(command: SetAutoBidCommand): List<DomainEvent> {
        validateAuctionAcceptsBids()
        validateNotSeller(command.bidderId)
        validateNotBlocked(command.bidderId)
        validateDeposit(command.bidderId)

        val events = mutableListOf<DomainEvent>()
        val now = Instant.now()

        // Raise AutoBidSetEvent
        val autoBidSetEvent = AutoBidSetEvent(
            eventId = IdGenerator.generateString(),
            aggregateId = id.toString(),
            brand = brand.code,
            timestamp = now,
            version = version + 1,
            bidderId = command.bidderId.toString(),
            maxAmount = command.maxAmount.amount,
            maxAmountCurrency = command.maxAmount.currency.currencyCode
        )
        raise(autoBidSetEvent)
        events.add(autoBidSetEvent)

        // If there is a current high bid and the auto-bidder is not already winning,
        // immediately place a proxy bid at the minimum required amount.
        if (currentHighBid != null && currentHighBidderId != command.bidderId) {
            val increment = AuctionConstants.minimumIncrement(currentHighBid!!)
            val requiredAmount = currentHighBid!! + increment

            if (command.maxAmount >= requiredAmount) {
                // Place the minimum necessary proxy bid
                val proxyBidAmount = requiredAmount
                val bidCommand = PlaceBidCommand(
                    auctionId = id,
                    bidderId = command.bidderId,
                    amount = proxyBidAmount,
                    isProxy = true
                )
                val bidEvents = placeBid(bidCommand)
                events.addAll(bidEvents)
            }
        } else if (currentHighBid == null) {
            // No bids yet — place the starting bid as a proxy bid
            val bidCommand = PlaceBidCommand(
                auctionId = id,
                bidderId = command.bidderId,
                amount = startingBid,
                isProxy = true
            )
            val bidEvents = placeBid(bidCommand)
            events.addAll(bidEvents)
        }

        return events
    }

    /**
     * Closes the auction, transitioning it to [AuctionStatus.CLOSED].
     *
     * The winning bid (if any) is promoted to [BidStatus.WINNING].
     *
     * @return The list of domain events produced.
     * @throws AuctionException.AuctionNotActiveException if the auction is not
     *         in a closable state.
     */
    fun close(): List<DomainEvent> {
        if (status != AuctionStatus.ACTIVE && status != AuctionStatus.CLOSING) {
            throw AuctionException.AuctionNotActiveException(id, status.name)
        }

        val events = mutableListOf<DomainEvent>()
        val now = Instant.now()

        val closedEvent = AuctionClosedEvent(
            eventId = IdGenerator.generateString(),
            aggregateId = id.toString(),
            brand = brand.code,
            timestamp = now,
            version = version + 1,
            finalBidAmount = currentHighBid?.amount,
            finalBidCurrency = currentHighBid?.currency?.currencyCode,
            winnerId = currentHighBidderId?.toString(),
            totalBids = bidCount,
            reserveMet = reserveMet
        )
        raise(closedEvent)
        events.add(closedEvent)

        return events
    }

    /**
     * Awards the lot to the winning bidder.
     *
     * The auction must be in [AuctionStatus.CLOSED] state with a winning bid
     * and (if applicable) the reserve must have been met.
     *
     * @return The list of domain events produced.
     * @throws IllegalStateException if the auction cannot be awarded.
     */
    fun award(): List<DomainEvent> {
        check(status == AuctionStatus.CLOSED) {
            "Auction '$id' must be CLOSED to award, current status: $status"
        }
        check(currentHighBidderId != null && currentHighBid != null) {
            "Auction '$id' has no winning bid to award"
        }
        if (reservePrice != null) {
            check(reserveMet) {
                "Auction '$id' cannot be awarded: reserve price not met"
            }
        }

        val winningBid = bids.lastOrNull { it.bidderId == currentHighBidderId }
            ?: throw IllegalStateException("Winning bid not found in bid list for auction '$id'")

        val events = mutableListOf<DomainEvent>()
        val now = Instant.now()

        val awardedEvent = LotAwardedEvent(
            eventId = IdGenerator.generateString(),
            aggregateId = id.toString(),
            brand = brand.code,
            timestamp = now,
            version = version + 1,
            lotId = lotId.toString(),
            winnerId = currentHighBidderId!!.toString(),
            winningBidAmount = currentHighBid!!.amount,
            winningBidCurrency = currentHighBid!!.currency.currencyCode,
            winningBidId = winningBid.id.toString()
        )
        raise(awardedEvent)
        events.add(awardedEvent)

        return events
    }

    /**
     * Cancels the auction with the given [reason].
     *
     * An auction can be cancelled from any non-terminal state.
     *
     * @param reason Human-readable reason for cancellation.
     * @param cancelledBy Optional identifier of the user/system that initiated the cancellation.
     * @return The list of domain events produced.
     * @throws IllegalStateException if the auction is already in a terminal state.
     */
    fun cancel(reason: String, cancelledBy: String? = null): List<DomainEvent> {
        check(!status.isTerminal()) {
            "Auction '$id' is already in terminal status $status and cannot be cancelled"
        }

        val events = mutableListOf<DomainEvent>()
        val now = Instant.now()

        val cancelledEvent = AuctionCancelledEvent(
            eventId = IdGenerator.generateString(),
            aggregateId = id.toString(),
            brand = brand.code,
            timestamp = now,
            version = version + 1,
            reason = reason,
            cancelledBy = cancelledBy
        )
        raise(cancelledEvent)
        events.add(cancelledEvent)

        return events
    }

    /**
     * Marks this auction as "featured" for homepage promotion.
     *
     * Only active auctions can be featured. The total featured count is checked
     * against [maxFeatured] to prevent homepage overload.
     */
    fun markFeatured(adminId: UserId, currentFeaturedCount: Int, maxFeatured: Int): List<DomainEvent> {
        if (status != AuctionStatus.ACTIVE) {
            throw AuctionException.AuctionNotFeaturableException(id, status.name)
        }
        if (featured) {
            throw AuctionException.AuctionAlreadyFeaturedException(id)
        }
        if (currentFeaturedCount >= maxFeatured) {
            throw AuctionException.FeaturedLimitReachedException(maxFeatured)
        }

        val now = Instant.now()
        val event = AuctionFeaturedEvent(
            eventId = IdGenerator.generateString(),
            aggregateId = id.toString(),
            brand = brand.code,
            timestamp = now,
            version = version + 1,
            featuredBy = adminId.toString(),
            featuredAt = now,
        )
        raise(event)
        return listOf(event)
    }

    /**
     * Removes the "featured" flag from this auction.
     */
    fun unmarkFeatured(adminId: UserId): List<DomainEvent> {
        if (!featured) {
            throw AuctionException.AuctionNotFeaturedException(id)
        }

        val event = AuctionUnfeaturedEvent(
            eventId = IdGenerator.generateString(),
            aggregateId = id.toString(),
            brand = brand.code,
            timestamp = Instant.now(),
            version = version + 1,
            unfeaturedBy = adminId.toString(),
        )
        raise(event)
        return listOf(event)
    }

    // -----------------------------------------------------------------------
    // Private business rule methods
    // -----------------------------------------------------------------------

    /**
     * Applies the anti-sniping rule.
     *
     * **CRITICAL**: If a bid is placed within [AuctionConstants.ANTI_SNIPING_WINDOW]
     * of the current [endTime], the end time is extended by
     * [AuctionConstants.ANTI_SNIPING_EXTENSION] from the **bid timestamp** (not
     * from the original end time). This ensures that each successive sniping bid
     * pushes the deadline forward incrementally.
     *
     * Extensions are capped at [AuctionConstants.MAX_EXTENSIONS] to prevent
     * indefinite auction prolongation.
     *
     * @param bidTime The timestamp of the bid that may trigger the extension.
     * @return An [AuctionExtendedEvent] if the extension is triggered, or `null`.
     */
    private fun applyAntiSnipingRule(bidTime: Instant): AuctionExtendedEvent? {
        if (extensionCount >= AuctionConstants.MAX_EXTENSIONS) {
            return null
        }

        val windowStart = endTime.minus(AuctionConstants.ANTI_SNIPING_WINDOW)

        // Bid must be within the anti-sniping window: windowStart <= bidTime < endTime
        if (!bidTime.isBefore(windowStart) && bidTime.isBefore(endTime)) {
            val previousEndTime = endTime
            val newEndTime = bidTime.plus(AuctionConstants.ANTI_SNIPING_EXTENSION)

            // Only extend if the new end time is actually later than the current one
            if (newEndTime.isAfter(endTime)) {
                val lastBidId = bids.lastOrNull()?.id?.toString() ?: ""

                return AuctionExtendedEvent(
                    eventId = IdGenerator.generateString(),
                    aggregateId = id.toString(),
                    brand = brand.code,
                    timestamp = bidTime,
                    version = version + 1,
                    previousEndTime = previousEndTime,
                    newEndTime = newEndTime,
                    extensionCount = extensionCount + 1,
                    triggeringBidId = lastBidId
                )
            }
        }

        return null
    }

    /**
     * Processes all active auto-bids in response to a new bid.
     *
     * The algorithm:
     * 1. Collect all active auto-bids from users other than the new bidder
     *    whose max amount can cover at least one counter bid.
     * 2. If multiple auto-bids can counter:
     *    a. The auto-bid with the highest [AutoBid.maxAmount] wins.
     *    b. The winning proxy bid is set to the **second-highest max amount
     *       + minimum increment**, or the minimum counter if only one can respond.
     *    c. If two auto-bids have equal max amounts, the one with the earlier
     *       [AutoBid.createdAt] timestamp wins (first-come-first-served).
     * 3. Auto-bids that cannot counter are exhausted with [AutoBidExhaustedEvent].
     *
     * @param newBid The bid that was just placed and may trigger auto-bid responses.
     * @return A list of domain events (proxy bids, bid-placed, and exhaustion events).
     */
    private fun processAutoBids(newBid: Bid): List<DomainEvent> {
        val events = mutableListOf<DomainEvent>()
        val increment = AuctionConstants.minimumIncrement(newBid.amount)
        val minimumCounter = newBid.amount + increment

        // Find all competing auto-bids (not from the bidder who just bid)
        val competingAutoBids = autoBids.entries
            .filter { (userId, autoBid) ->
                userId != newBid.bidderId && autoBid.active
            }
            .map { it.value }
            .sortedWith(
                compareByDescending<AutoBid> { it.maxAmount }
                    .thenBy { it.createdAt }
            )

        if (competingAutoBids.isEmpty()) {
            return events
        }

        // Find which auto-bids can actually counter
        val canCounter = competingAutoBids.filter { it.canCounter(newBid.amount, increment) }
        val cannotCounter = competingAutoBids.filter { !it.canCounter(newBid.amount, increment) }

        // Exhaust auto-bids that cannot counter
        for (exhausted in cannotCounter) {
            val exhaustedEvent = AutoBidExhaustedEvent(
                eventId = IdGenerator.generateString(),
                aggregateId = id.toString(),
                brand = brand.code,
                timestamp = Instant.now(),
                version = version + 1,
                bidderId = exhausted.bidderId.toString(),
                maxAmount = exhausted.maxAmount.amount,
                maxAmountCurrency = exhausted.maxAmount.currency.currencyCode,
                competingBidAmount = newBid.amount.amount,
                competingBidCurrency = newBid.amount.currency.currencyCode
            )
            events.add(exhaustedEvent)
        }

        if (canCounter.isEmpty()) {
            return events
        }

        // Determine the winning auto-bid and the proxy bid amount
        val winner = canCounter.first() // Highest maxAmount, earliest timestamp (already sorted)
        val proxyBidAmount: Money

        if (canCounter.size >= 2) {
            // Second-price logic: winning proxy bid = second-highest max + increment
            val secondHighest = canCounter[1]
            val incrementForSecond = AuctionConstants.minimumIncrement(secondHighest.maxAmount)
            val secondPlusIncrement = secondHighest.maxAmount + incrementForSecond

            proxyBidAmount = if (secondPlusIncrement <= winner.maxAmount) {
                secondPlusIncrement
            } else {
                // Winner can only match, not exceed: bid at winner's max
                winner.maxAmount
            }

            // Exhaust the second-place auto-bid (and any others below it)
            for (i in 1 until canCounter.size) {
                val loser = canCounter[i]
                val loserExhaustedEvent = AutoBidExhaustedEvent(
                    eventId = IdGenerator.generateString(),
                    aggregateId = id.toString(),
                    brand = brand.code,
                    timestamp = Instant.now(),
                    version = version + 1,
                    bidderId = loser.bidderId.toString(),
                    maxAmount = loser.maxAmount.amount,
                    maxAmountCurrency = loser.maxAmount.currency.currencyCode,
                    competingBidAmount = proxyBidAmount.amount,
                    competingBidCurrency = proxyBidAmount.currency.currencyCode
                )
                events.add(loserExhaustedEvent)
            }
        } else {
            // Only one auto-bid can counter — bid the minimum counter
            proxyBidAmount = minimumCounter
        }

        // Create the proxy bid event
        val proxyBidId = BidId.generate()
        val now = Instant.now()

        val proxyEvent = ProxyBidTriggeredEvent(
            eventId = IdGenerator.generateString(),
            aggregateId = id.toString(),
            brand = brand.code,
            timestamp = now,
            version = version + 1,
            bidId = proxyBidId.toString(),
            bidderId = winner.bidderId.toString(),
            bidAmount = proxyBidAmount.amount,
            bidCurrency = proxyBidAmount.currency.currencyCode,
            triggeringBidId = newBid.id.toString(),
            maxAutoBidAmount = winner.maxAmount.amount,
            maxAutoBidCurrency = winner.maxAmount.currency.currencyCode
        )
        events.add(proxyEvent)

        return events
    }

    /**
     * Checks whether the given bid amount meets or exceeds the auction's
     * reserve price.
     *
     * @param bidAmount The bid amount to check against the reserve.
     * @return A [ReserveMetEvent] if the reserve is met for the first time, or `null`.
     */
    private fun checkReserve(bidAmount: Money): ReserveMetEvent? {
        val reserve = reservePrice ?: return null // No reserve set
        if (reserveMet) return null // Already met

        if (bidAmount >= reserve) {
            val lastBidId = bids.lastOrNull()?.id?.toString() ?: ""

            return ReserveMetEvent(
                eventId = IdGenerator.generateString(),
                aggregateId = id.toString(),
                brand = brand.code,
                timestamp = Instant.now(),
                version = version + 1,
                bidId = lastBidId,
                bidAmount = bidAmount.amount,
                bidCurrency = bidAmount.currency.currencyCode,
                reserveAmount = reserve.amount,
                reserveCurrency = reserve.currency.currencyCode
            )
        }

        return null
    }

    // -----------------------------------------------------------------------
    // Validation helpers
    // -----------------------------------------------------------------------

    private fun validateAuctionAcceptsBids() {
        if (!status.acceptsBids()) {
            throw AuctionException.AuctionNotActiveException(id, status.name)
        }
    }

    private fun validateNotSeller(bidderId: UserId) {
        if (bidderId == sellerId) {
            throw AuctionException.SellerCannotBidException(id, sellerId)
        }
    }

    private fun validateNotBlocked(bidderId: UserId) {
        if (bidderId in blockedBidders) {
            throw AuctionException.UserBlockedException(id, bidderId)
        }
    }

    private fun validateDeposit(bidderId: UserId) {
        val threshold = AuctionConstants.DEPOSIT_THRESHOLD
        if (currentHighBid != null && currentHighBid!! >= threshold) {
            if (bidderId !in depositHolders) {
                throw AuctionException.DepositRequiredException(
                    id, bidderId, AuctionConstants.DEPOSIT_AMOUNT
                )
            }
        }
    }

    private fun validateBidAmount(amount: Money) {
        if (!amount.isPositive()) {
            throw AuctionException.InvalidBidAmountException(id, "Bid amount must be positive")
        }

        if (currentHighBid == null) {
            // First bid must be at least the starting bid
            if (amount < startingBid) {
                throw AuctionException.BidBelowMinimumException(id, amount, startingBid)
            }
        } else {
            // Subsequent bids must meet the minimum increment
            val increment = AuctionConstants.minimumIncrement(currentHighBid!!)
            val minimumRequired = currentHighBid!! + increment
            if (amount < minimumRequired) {
                throw AuctionException.BidBelowMinimumException(id, amount, minimumRequired)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Event application (state mutation)
    // -----------------------------------------------------------------------

    /**
     * Applies a domain event to the aggregate's internal state.
     *
     * This method is a pure projection: it updates fields based on the event's
     * payload without side-effects. It is invoked both during event replay
     * (reconstitution) and when handling new commands (via [raise]).
     *
     * @param event The domain event to apply.
     */
    override fun apply(event: DomainEvent) {
        when (event) {
            is AuctionCreatedEvent -> applyAuctionCreated(event)
            is BidPlacedEvent -> applyBidPlaced(event)
            is ProxyBidTriggeredEvent -> applyProxyBidTriggered(event)
            is AuctionExtendedEvent -> applyAuctionExtended(event)
            is AuctionClosedEvent -> applyAuctionClosed(event)
            is LotAwardedEvent -> applyLotAwarded(event)
            is AuctionCancelledEvent -> applyAuctionCancelled(event)
            is ReserveMetEvent -> applyReserveMet(event)
            is BidRejectedEvent -> { /* No state change for rejected bids */ }
            is AutoBidSetEvent -> applyAutoBidSet(event)
            is AuctionFeaturedEvent -> applyAuctionFeatured(event)
            is AuctionUnfeaturedEvent -> applyAuctionUnfeatured(event)
            is AutoBidExhaustedEvent -> applyAutoBidExhausted(event)
            else -> throw IllegalArgumentException(
                "Auction aggregate does not know how to apply event of type '${event.eventType}'"
            )
        }
    }

    private fun applyAuctionCreated(event: AuctionCreatedEvent) {
        id = AuctionId.fromString(event.aggregateId)
        lotId = LotId.fromString(event.lotId)
        brand = Brand.fromCode(event.brand)
        status = AuctionStatus.ACTIVE
        startTime = event.startTime
        endTime = event.endTime
        originalEndTime = event.endTime
        startingBid = Money.of(
            event.startingBidAmount,
            java.util.Currency.getInstance(event.startingBidCurrency)
        )
        reservePrice = if (event.reservePriceAmount != null && event.reservePriceCurrency != null) {
            Money.of(
                event.reservePriceAmount,
                java.util.Currency.getInstance(event.reservePriceCurrency)
            )
        } else {
            null
        }
        reserveMet = reservePrice == null // No reserve means reserve is trivially met
        sellerId = UserId.fromString(event.sellerId)
        bidCount = 0
        extensionCount = 0
    }

    private fun applyBidPlaced(event: BidPlacedEvent) {
        val bidAmount = Money.of(
            event.bidAmount,
            java.util.Currency.getInstance(event.bidCurrency)
        )
        val bidderId = UserId.fromString(event.bidderId)

        // Mark previous high bid as outbid
        bids.lastOrNull { it.status == BidStatus.ACTIVE }?.let { previousHigh ->
            val index = bids.indexOf(previousHigh)
            if (index >= 0) {
                bids[index] = previousHigh.withStatus(BidStatus.OUTBID)
            }
        }

        // Add the new bid
        val bid = Bid(
            id = BidId.fromString(event.bidId),
            bidderId = bidderId,
            amount = bidAmount,
            timestamp = event.timestamp,
            isProxy = event.isProxy,
            status = BidStatus.ACTIVE
        )
        bids.add(bid)

        currentHighBid = bidAmount
        currentHighBidderId = bidderId
        bidCount++
    }

    private fun applyProxyBidTriggered(event: ProxyBidTriggeredEvent) {
        val bidAmount = Money.of(
            event.bidAmount,
            java.util.Currency.getInstance(event.bidCurrency)
        )
        val bidderId = UserId.fromString(event.bidderId)

        // Mark previous high bid as outbid
        bids.lastOrNull { it.status == BidStatus.ACTIVE }?.let { previousHigh ->
            val index = bids.indexOf(previousHigh)
            if (index >= 0) {
                bids[index] = previousHigh.withStatus(BidStatus.OUTBID)
            }
        }

        // Add the proxy bid
        val bid = Bid(
            id = BidId.fromString(event.bidId),
            bidderId = bidderId,
            amount = bidAmount,
            timestamp = event.timestamp,
            isProxy = true,
            status = BidStatus.ACTIVE
        )
        bids.add(bid)

        currentHighBid = bidAmount
        currentHighBidderId = bidderId
        bidCount++

        // Update the auto-bid's current bid amount
        autoBids[bidderId]?.let { autoBid ->
            autoBids[bidderId] = autoBid.withCurrentBid(bidAmount)
        }
    }

    private fun applyAuctionExtended(event: AuctionExtendedEvent) {
        endTime = event.newEndTime
        extensionCount = event.extensionCount
        // Transition to CLOSING if not already
        if (status == AuctionStatus.ACTIVE) {
            status = AuctionStatus.CLOSING
        }
    }

    private fun applyAuctionClosed(event: AuctionClosedEvent) {
        status = AuctionStatus.CLOSED
        featured = false
        featuredAt = null

        // Promote the current high bid to WINNING status
        bids.lastOrNull { it.status == BidStatus.ACTIVE }?.let { winningBid ->
            val index = bids.indexOf(winningBid)
            if (index >= 0) {
                bids[index] = winningBid.withStatus(BidStatus.WINNING)
            }
        }

        // Deactivate all remaining auto-bids
        autoBids.keys.forEach { userId ->
            autoBids[userId]?.let { autoBid ->
                if (autoBid.active) {
                    autoBids[userId] = autoBid.deactivate()
                }
            }
        }
    }

    private fun applyLotAwarded(event: LotAwardedEvent) {
        status = AuctionStatus.AWARDED
    }

    private fun applyAuctionCancelled(event: AuctionCancelledEvent) {
        status = AuctionStatus.CANCELLED

        // Deactivate all remaining auto-bids
        autoBids.keys.forEach { userId ->
            autoBids[userId]?.let { autoBid ->
                if (autoBid.active) {
                    autoBids[userId] = autoBid.deactivate()
                }
            }
        }
    }

    private fun applyReserveMet(event: ReserveMetEvent) {
        reserveMet = true
    }

    private fun applyAutoBidSet(event: AutoBidSetEvent) {
        val bidderId = UserId.fromString(event.bidderId)
        val maxAmount = Money.of(
            event.maxAmount,
            java.util.Currency.getInstance(event.maxAmountCurrency)
        )

        autoBids[bidderId] = AutoBid(
            bidderId = bidderId,
            maxAmount = maxAmount,
            currentBidAmount = currentHighBid ?: startingBid,
            createdAt = event.timestamp,
            active = true
        )
    }

    private fun applyAutoBidExhausted(event: AutoBidExhaustedEvent) {
        val bidderId = UserId.fromString(event.bidderId)
        autoBids[bidderId]?.let { autoBid ->
            autoBids[bidderId] = autoBid.deactivate()
        }
    }

    private fun applyAuctionFeatured(event: AuctionFeaturedEvent) {
        featured = true
        featuredAt = event.featuredAt
    }

    private fun applyAuctionUnfeatured(event: AuctionUnfeaturedEvent) {
        featured = false
        featuredAt = null
    }

    // -----------------------------------------------------------------------
    // toString / identity
    // -----------------------------------------------------------------------

    override fun toString(): String =
        "Auction(id=$id, lotId=$lotId, brand=$brand, status=$status, " +
            "currentHighBid=$currentHighBid, bidCount=$bidCount, " +
            "endTime=$endTime, extensionCount=$extensionCount)"
}
