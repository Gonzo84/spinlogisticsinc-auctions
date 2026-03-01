package eu.auctionplatform.notification.infrastructure

import eu.auctionplatform.commons.util.JsonMapper
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

/**
 * Resolves a user's email address by calling the user-service REST API.
 *
 * Used by NATS event consumers to populate the "email" key in notification
 * data maps, enabling email delivery for notifications triggered by
 * auction and payment events.
 *
 * The user-service URL is configured via `services.user-service.url`.
 */
@ApplicationScoped
class UserEmailResolver(
    @ConfigProperty(name = "services.user-service.url")
    private val userServiceUrl: String
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(UserEmailResolver::class.java)
        private val HTTP_CLIENT: HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
    }

    /**
     * Resolves the email address for the given user ID by calling
     * `GET {userServiceUrl}/users/{userId}`.
     *
     * @param userId The user's UUID.
     * @return The user's email address, or `null` if the lookup fails.
     */
    @Suppress("UNCHECKED_CAST")
    fun resolveEmail(userId: UUID): String? {
        return try {
            val url = "${userServiceUrl.trimEnd('/')}/users/$userId"
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build()

            val response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                LOG.warnf(
                    "User-service returned status %d for userId=%s",
                    response.statusCode(), userId
                )
                return null
            }

            val body = JsonMapper.instance.readValue(response.body(), Map::class.java) as Map<String, Any>
            // Response may be wrapped in ApiResponse { data: { ... } }
            val userData = if (body.containsKey("data") && body["data"] is Map<*, *>) {
                body["data"] as Map<String, Any>
            } else {
                body
            }

            userData["email"]?.toString()
        } catch (ex: Exception) {
            LOG.warnf(
                ex, "Failed to resolve email for userId=%s: %s",
                userId, ex.message
            )
            null
        }
    }
}
