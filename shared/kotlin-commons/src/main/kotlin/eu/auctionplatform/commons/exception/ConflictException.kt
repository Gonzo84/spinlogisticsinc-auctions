package eu.auctionplatform.commons.exception

/**
 * Thrown when an operation conflicts with the current state of a resource
 * (e.g. optimistic concurrency violation, duplicate key).
 *
 * Maps to HTTP 409.
 */
class ConflictException(
    code: String = "CONFLICT",
    message: String,
    cause: Throwable? = null
) : DomainException(code, message, cause)
