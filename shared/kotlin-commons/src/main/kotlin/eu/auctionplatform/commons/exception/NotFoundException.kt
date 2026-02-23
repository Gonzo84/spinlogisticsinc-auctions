package eu.auctionplatform.commons.exception

/**
 * Thrown when a requested resource (aggregate, entity, projection) cannot be found.
 *
 * Maps to HTTP 404.
 */
class NotFoundException(
    code: String = "NOT_FOUND",
    message: String,
    cause: Throwable? = null
) : DomainException(code, message, cause)
