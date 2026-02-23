package eu.auctionplatform.commons.domain

/**
 * Base class for event-sourced aggregates.
 *
 * Subclasses implement [apply] to mutate internal state in response to domain
 * events and use [raise] to both apply and record new events.
 *
 * Typical lifecycle:
 * 1. **Rehydration** – call [loadFromHistory] with the persisted event stream.
 * 2. **Command handling** – business methods call [raise] for each state transition.
 * 3. **Persistence** – the repository reads [uncommittedEvents], appends them to
 *    the event store, then calls [markEventsAsCommitted].
 */
abstract class AggregateRoot {

    /** Events that have been raised but not yet persisted. */
    val uncommittedEvents: MutableList<DomainEvent> = mutableListOf()

    /** Current version of the aggregate (equals the version of the last applied event). */
    var version: Long = 0
        protected set

    /**
     * Raises a new domain event: applies it to the aggregate's state and adds it
     * to the list of uncommitted events.
     */
    protected fun raise(event: DomainEvent) {
        apply(event)
        uncommittedEvents.add(event)
        version = event.version
    }

    /**
     * Applies the [event] to the aggregate's internal state.
     *
     * Implementations must be **pure projections** – they must not have side-effects
     * beyond mutating the aggregate's own fields, because [apply] is invoked both
     * when replaying history and when handling new commands.
     */
    protected abstract fun apply(event: DomainEvent)

    /**
     * Clears the uncommitted events list after the events have been successfully
     * persisted by the repository / event store.
     */
    fun markEventsAsCommitted() {
        uncommittedEvents.clear()
    }

    /**
     * Rebuilds the aggregate's state by replaying a list of historical [events].
     *
     * After this call, [version] equals the version of the last event in the list
     * and [uncommittedEvents] remains empty.
     */
    fun loadFromHistory(events: List<DomainEvent>) {
        events.forEach { event ->
            apply(event)
            version = event.version
        }
    }
}
