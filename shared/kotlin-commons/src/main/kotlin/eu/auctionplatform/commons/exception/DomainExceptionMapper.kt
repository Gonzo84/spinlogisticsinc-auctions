package eu.auctionplatform.commons.exception

import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.slf4j.LoggerFactory

/**
 * JAX-RS exception mapper for [DomainException] and its subtypes.
 *
 * Maps domain exceptions to the appropriate HTTP status codes with
 * RFC 7807 Problem Details-style response bodies.
 *
 * - [ValidationException] -> 422 Unprocessable Entity
 * - [NotFoundException]   -> 404 Not Found
 * - [ConflictException]   -> 409 Conflict
 * - [ForbiddenException]  -> 403 Forbidden
 * - [DomainException]     -> 400 Bad Request (fallback)
 */
@Provider
class DomainExceptionMapper : ExceptionMapper<DomainException> {

    companion object {
        private val LOG = LoggerFactory.getLogger(DomainExceptionMapper::class.java)
    }

    override fun toResponse(exception: DomainException): Response {
        val status = when (exception) {
            is ValidationException -> Response.Status.BAD_REQUEST
            is NotFoundException -> Response.Status.NOT_FOUND
            is ConflictException -> Response.Status.CONFLICT
            is ForbiddenException -> Response.Status.FORBIDDEN
            else -> Response.Status.BAD_REQUEST
        }

        LOG.debug("DomainException mapped: type={}, code={}, status={}, message={}",
            exception.javaClass.simpleName, exception.code, status.statusCode, exception.message)

        val body = mutableMapOf<String, Any>(
            "status" to status.statusCode,
            "title" to status.reasonPhrase,
            "code" to exception.code,
            "message" to (exception.message ?: "An error occurred")
        )

        if (exception is ValidationException && exception.errors.isNotEmpty()) {
            body["errors"] = exception.errors
        }

        return Response
            .status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(body)
            .build()
    }
}
