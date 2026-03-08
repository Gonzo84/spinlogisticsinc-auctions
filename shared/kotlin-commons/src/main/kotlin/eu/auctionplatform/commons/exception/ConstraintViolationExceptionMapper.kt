package eu.auctionplatform.commons.exception

import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.Logger

/**
 * JAX-RS exception mapper for Bean Validation [ConstraintViolationException].
 *
 * When a request DTO annotated with `@Valid` fails Bean Validation,
 * Quarkus throws a [ConstraintViolationException]. Without this mapper
 * the response is a bare 400 with an empty body. This mapper returns
 * a 400 Bad Request with field-level validation error details.
 */
@Provider
class ConstraintViolationExceptionMapper : ExceptionMapper<ConstraintViolationException> {

    companion object {
        private val LOG: Logger = Logger.getLogger(ConstraintViolationExceptionMapper::class.java)
    }

    override fun toResponse(exception: ConstraintViolationException): Response {
        val violations = exception.constraintViolations.map { violation ->
            val field = violation.propertyPath.toString()
                .substringAfterLast('.')
            mapOf(
                "field" to field,
                "message" to violation.message,
                "rejectedValue" to violation.invalidValue?.toString()
            )
        }

        LOG.debugf("Bean Validation failed: %d violation(s)", violations.size)

        val body = mapOf(
            "status" to 400,
            "title" to "Bad Request",
            "code" to "VALIDATION_FAILED",
            "message" to "Request validation failed",
            "errors" to violations
        )

        return Response
            .status(Response.Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON)
            .entity(body)
            .build()
    }
}
