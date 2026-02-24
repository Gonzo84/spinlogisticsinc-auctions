package eu.auctionplatform.gateway.api.v1.resource

import eu.auctionplatform.commons.util.JsonMapper
import eu.auctionplatform.gateway.api.v1.dto.AdyenWebhookPayload
import eu.auctionplatform.gateway.api.v1.dto.OnfidoWebhookPayload
import eu.auctionplatform.gateway.api.v1.dto.WebhookAckResponse
import eu.auctionplatform.gateway.api.v1.dto.WebhookNatsEvent
import eu.auctionplatform.gateway.infrastructure.webhook.WebhookValidator
import io.agroal.api.AgroalDataSource
import io.nats.client.Connection
import jakarta.annotation.security.PermitAll
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * REST resource for processing incoming webhooks from third-party providers.
 *
 * Handles Adyen payment webhooks and Onfido identity verification webhooks.
 * Each webhook is validated (HMAC signature), deduplicated via Redis/DB,
 * and then published to NATS for downstream processing.
 *
 * Webhook endpoints are exempt from authentication (they use HMAC validation
 * instead) and rate limiting (handled at the provider level).
 */
@Path("/api/v1/webhooks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class WebhookResource {

    @Inject
    lateinit var webhookValidator: WebhookValidator

    @Inject
    lateinit var natsConnection: Connection

    @Inject
    lateinit var dataSource: AgroalDataSource

    @ConfigProperty(name = "webhook.adyen.hmac-key", defaultValue = "")
    lateinit var adyenHmacKey: String

    @ConfigProperty(name = "webhook.onfido.token", defaultValue = "")
    lateinit var onfidoToken: String

    private val logger = LoggerFactory.getLogger(WebhookResource::class.java)

    companion object {
        private const val NATS_SUBJECT_ADYEN = "webhook.adyen.received"
        private const val NATS_SUBJECT_ONFIDO = "webhook.onfido.received"

        private const val INSERT_DEDUP = """
            INSERT INTO app.webhook_dedup (event_id, source, received_at)
            VALUES (?, ?, NOW())
            ON CONFLICT (event_id) DO NOTHING
        """

        private const val CHECK_DEDUP = """
            SELECT COUNT(*) FROM app.webhook_dedup WHERE event_id = ?
        """
    }

    // -------------------------------------------------------------------------
    // Adyen Webhook
    // -------------------------------------------------------------------------

    /**
     * Receives and processes Adyen payment webhook notifications.
     *
     * **POST /api/v1/webhooks/adyen**
     *
     * Flow:
     * 1. Parse the Adyen notification payload.
     * 2. For each notification item, validate HMAC signature.
     * 3. Deduplicate using the PSP reference as event ID.
     * 4. Publish validated events to NATS for downstream processing.
     * 5. Return `[accepted]` response (Adyen requires this).
     *
     * @param body The raw Adyen webhook payload.
     * @return 200 OK with acceptance acknowledgement.
     */
    @POST
    @Path("/adyen")
    @PermitAll
    fun handleAdyenWebhook(body: String): Response {
        logger.info("Received Adyen webhook, payload size={} bytes", body.length)

        val payload: AdyenWebhookPayload = try {
            JsonMapper.fromJson(body)
        } catch (ex: Exception) {
            logger.error("Failed to parse Adyen webhook payload: {}", ex.message)
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(WebhookAckResponse(accepted = false, message = "Invalid payload"))
                .build()
        }

        var processedCount = 0

        for (wrapper in payload.notificationItems) {
            val item = wrapper.notificationRequestItem
            val eventId = "adyen:${item.pspReference}"
            val signature = item.hmacSignature()

            // Validate HMAC if key is configured
            if (adyenHmacKey.isNotBlank()) {
                if (signature.isNullOrBlank()) {
                    logger.warn("Adyen notification missing HMAC signature [psp={}]", item.pspReference)
                    continue
                }
                if (!webhookValidator.validateAdyenHmac(body, signature, adyenHmacKey)) {
                    logger.warn("Adyen HMAC validation failed [psp={}]", item.pspReference)
                    continue
                }
            }

            // Deduplicate
            if (isDuplicate(eventId)) {
                logger.debug("Adyen notification already processed [psp={}]", item.pspReference)
                continue
            }

            // Record the event for deduplication
            recordEvent(eventId, "adyen")

            // Publish to NATS
            val natsEvent = WebhookNatsEvent(
                eventId = eventId,
                source = "adyen",
                eventType = item.eventCode,
                payload = JsonMapper.toJson(item),
                receivedAt = Instant.now()
            )

            publishToNats(NATS_SUBJECT_ADYEN, natsEvent)
            processedCount++

            logger.info(
                "Processed Adyen notification [psp={}, eventCode={}, success={}]",
                item.pspReference, item.eventCode, item.success
            )
        }

        logger.info("Adyen webhook processed: {} of {} items", processedCount, payload.notificationItems.size)

        return Response.ok(WebhookAckResponse(accepted = true, message = "[accepted]")).build()
    }

    // -------------------------------------------------------------------------
    // Onfido Webhook
    // -------------------------------------------------------------------------

    /**
     * Receives and processes Onfido identity verification webhook events.
     *
     * **POST /api/v1/webhooks/onfido**
     *
     * Flow:
     * 1. Validate the HMAC signature from the `X-SHA2-Signature` header.
     * 2. Parse the Onfido payload.
     * 3. Deduplicate using the event object ID.
     * 4. Publish validated event to NATS.
     *
     * @param body            The raw Onfido webhook payload.
     * @param sha2Signature   The HMAC-SHA256 signature from the `X-SHA2-Signature` header.
     * @return 200 OK with acceptance acknowledgement.
     */
    @POST
    @Path("/onfido")
    @PermitAll
    fun handleOnfidoWebhook(
        body: String,
        @HeaderParam("X-SHA2-Signature") sha2Signature: String?
    ): Response {
        logger.info("Received Onfido webhook, payload size={} bytes", body.length)

        // Validate HMAC signature if token is configured
        if (onfidoToken.isNotBlank()) {
            if (sha2Signature.isNullOrBlank()) {
                logger.warn("Onfido webhook missing X-SHA2-Signature header")
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(WebhookAckResponse(accepted = false, message = "Missing signature"))
                    .build()
            }
            if (!webhookValidator.validateOnfidoHmac(body, sha2Signature, onfidoToken)) {
                logger.warn("Onfido HMAC validation failed")
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(WebhookAckResponse(accepted = false, message = "Invalid signature"))
                    .build()
            }
        }

        val payload: OnfidoWebhookPayload = try {
            JsonMapper.fromJson(body)
        } catch (ex: Exception) {
            logger.error("Failed to parse Onfido webhook payload: {}", ex.message)
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(WebhookAckResponse(accepted = false, message = "Invalid payload"))
                .build()
        }

        val eventObject = payload.payload.eventObject
        val eventId = "onfido:${eventObject.id}"

        // Deduplicate
        if (isDuplicate(eventId)) {
            logger.debug("Onfido webhook already processed [id={}]", eventObject.id)
            return Response.ok(
                WebhookAckResponse(accepted = true, eventId = eventId, message = "Duplicate event")
            ).build()
        }

        // Record the event for deduplication
        recordEvent(eventId, "onfido")

        // Publish to NATS
        val natsEvent = WebhookNatsEvent(
            eventId = eventId,
            source = "onfido",
            eventType = "${payload.payload.resourceType}.${payload.payload.action}",
            payload = body,
            receivedAt = Instant.now()
        )

        publishToNats(NATS_SUBJECT_ONFIDO, natsEvent)

        logger.info(
            "Processed Onfido webhook [id={}, type={}.{}, status={}]",
            eventObject.id, payload.payload.resourceType, payload.payload.action, eventObject.status
        )

        return Response.ok(WebhookAckResponse(accepted = true, eventId = eventId)).build()
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Checks whether an event has already been processed (deduplication).
     */
    private fun isDuplicate(eventId: String): Boolean {
        return try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(CHECK_DEDUP).use { stmt ->
                    stmt.setString(1, eventId)
                    stmt.executeQuery().use { rs ->
                        rs.next() && rs.getLong(1) > 0
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error("Dedup check failed for event {}: {}", eventId, ex.message)
            false
        }
    }

    /**
     * Records an event ID in the deduplication table.
     */
    private fun recordEvent(eventId: String, source: String) {
        try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(INSERT_DEDUP).use { stmt ->
                    stmt.setString(1, eventId)
                    stmt.setString(2, source)
                    stmt.executeUpdate()
                }
            }
        } catch (ex: Exception) {
            logger.error("Failed to record dedup event {}: {}", eventId, ex.message)
        }
    }

    /**
     * Publishes a validated webhook event to NATS JetStream.
     */
    private fun publishToNats(subject: String, event: WebhookNatsEvent) {
        try {
            val payload = JsonMapper.toJson(event).toByteArray(Charsets.UTF_8)
            natsConnection.publish(subject, payload)
            logger.debug("Published webhook event to NATS [subject={}, eventId={}]", subject, event.eventId)
        } catch (ex: Exception) {
            logger.error(
                "Failed to publish webhook event to NATS [subject={}, eventId={}]: {}",
                subject, event.eventId, ex.message, ex
            )
        }
    }
}
