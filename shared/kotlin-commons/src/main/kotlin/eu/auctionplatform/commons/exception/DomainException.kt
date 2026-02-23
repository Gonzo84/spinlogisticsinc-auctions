package eu.auctionplatform.commons.exception

/**
 * Base exception for all domain-level errors in the auction platform.
 *
 * Every domain exception carries a machine-readable [code] (e.g. "LOT_NOT_FOUND",
 * "BID_TOO_LOW") that API consumers can map to localised messages or UI behaviour.
 */
open class DomainException(
    val code: String,
    override val message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
