package eu.auctionplatform.commons.exception

/**
 * Thrown when input validation fails.
 *
 * Carries a map of field-level [errors] where keys are field names (or paths)
 * and values are human-readable error descriptions.
 *
 * Maps to HTTP 422 (Unprocessable Entity) or 400 (Bad Request).
 */
class ValidationException(
    val errors: Map<String, String>,
    code: String = "VALIDATION_ERROR",
    message: String = "Validation failed",
    cause: Throwable? = null
) : DomainException(code, message, cause) {

    /**
     * Convenience constructor for a single field error.
     */
    constructor(field: String, error: String) : this(
        errors = mapOf(field to error)
    )

    override fun toString(): String =
        "ValidationException(code=$code, message=$message, errors=$errors)"
}
