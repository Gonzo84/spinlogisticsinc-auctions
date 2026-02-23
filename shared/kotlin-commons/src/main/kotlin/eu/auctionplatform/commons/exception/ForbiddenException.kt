package eu.auctionplatform.commons.exception

/**
 * Thrown when the authenticated user does not have permission to perform the
 * requested operation.
 *
 * Maps to HTTP 403.
 */
class ForbiddenException(
    code: String = "FORBIDDEN",
    message: String,
    cause: Throwable? = null
) : DomainException(code, message, cause)
