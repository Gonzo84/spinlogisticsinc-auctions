package eu.auctionplatform.catalog.domain.model

/**
 * Lifecycle status of an auction event (the grouping entity, not a domain event).
 *
 * An auction event groups multiple lots into a single branded auction session
 * with a defined time window.
 *
 * - **DRAFT** – The auction event is being configured by an admin.
 * - **SCHEDULED** – The auction event has been published and is visible to buyers.
 * - **ACTIVE** – The auction event is currently live; lots within it accept bids.
 * - **CLOSED** – The auction event has ended; all lots have been finalised.
 */
enum class AuctionEventStatus {
    DRAFT,
    SCHEDULED,
    ACTIVE,
    CLOSED;

    /**
     * Returns `true` if lots can still be added to this auction event.
     */
    fun acceptsLots(): Boolean = this == DRAFT || this == SCHEDULED

    /**
     * Returns `true` if the auction event is currently accepting bids.
     */
    fun isLive(): Boolean = this == ACTIVE
}
