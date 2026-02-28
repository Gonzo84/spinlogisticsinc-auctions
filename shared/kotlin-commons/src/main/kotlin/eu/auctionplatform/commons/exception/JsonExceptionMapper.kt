package eu.auctionplatform.commons.exception

import com.fasterxml.jackson.core.JsonProcessingException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.Logger

/**
 * JAX-RS exception mapper for Jackson [JsonProcessingException].
 *
 * When the client sends a request body with invalid JSON or missing/wrong
 * fields, Jackson throws a [JsonProcessingException] during deserialization.
 * Without this mapper, Quarkus returns a bare 400 with an empty body.
 *
 * This mapper returns a 400 Bad Request with a descriptive error body.
 */
@Provider
class JsonExceptionMapper : ExceptionMapper<JsonProcessingException> {

    companion object {
        private val LOG: Logger = Logger.getLogger(JsonExceptionMapper::class.java)
    }

    override fun toResponse(exception: JsonProcessingException): Response {
        LOG.debugf("JSON deserialization error: %s", exception.originalMessage)

        val body = mapOf(
            "status" to 400,
            "title" to "Bad Request",
            "code" to "INVALID_REQUEST_BODY",
            "message" to "Invalid request body: ${exception.originalMessage ?: "JSON parsing failed"}"
        )

        return Response
            .status(Response.Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON)
            .entity(body)
            .build()
    }
}
