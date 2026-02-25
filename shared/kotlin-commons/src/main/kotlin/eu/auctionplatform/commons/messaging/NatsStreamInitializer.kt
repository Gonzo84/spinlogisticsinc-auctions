package eu.auctionplatform.commons.messaging

import io.nats.client.Connection
import io.nats.client.api.RetentionPolicy
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import io.nats.client.api.StreamInfo
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Initialises NATS JetStream streams.
 *
 * Each stream is created with sensible defaults if it does not already exist.
 * This ensures that publishers and consumers always have their required streams
 * available, eliminating the "stream not found" errors that occur when consumers
 * try to subscribe before streams are created.
 *
 * Stream definitions are derived from [NatsSubjects] — each top-level domain
 * (auction, catalog, payment, user, media, compliance, co2, notify) gets its
 * own stream capturing all subjects under that domain prefix.
 *
 * Call [initializeStreams] after establishing a NATS connection.
 */
object NatsStreamInitializer {

    private val logger = LoggerFactory.getLogger(NatsStreamInitializer::class.java)

    /**
     * Stream definitions: stream name to list of subject patterns.
     *
     * Each stream captures all subjects that match ANY of its patterns.
     * The `>` wildcard matches one or more tokens (e.g. `auction.>` matches
     * `auction.bid.placed`, `auction.lot.closed`, etc.).
     *
     * Brand-prefixed subjects (e.g. `troostwijk.auction.bid.placed`) are also
     * captured via `*.auction.>`.
     */
    val STREAM_DEFINITIONS: Map<String, List<String>> = mapOf(
        "AUCTION" to listOf("auction.>"),
        "CATALOG" to listOf("catalog.>"),
        "PAYMENT" to listOf("payment.>"),
        "USER" to listOf("user.>"),
        "MEDIA" to listOf("media.>"),
        "NOTIFY" to listOf("notify.>"),
        "COMPLIANCE" to listOf("compliance.>"),
        "CO2" to listOf("co2.>"),
    )

    /**
     * Creates all required JetStream streams if they do not already exist.
     *
     * @param connection An active NATS [Connection] with JetStream enabled.
     */
    fun initializeStreams(connection: Connection) {
        try {
            doInitialize(connection)
        } catch (ex: Exception) {
            logger.error("Failed to initialize NATS JetStream streams: {}", ex.message, ex)
        }
    }

    private fun doInitialize(connection: Connection) {
        val jsm = connection.jetStreamManagement()

        val existingStreams = try {
            jsm.streamNames.toSet()
        } catch (_: Exception) {
            emptySet()
        }

        for ((streamName, subjects) in STREAM_DEFINITIONS) {
            try {
                if (streamName in existingStreams) {
                    logger.debug("NATS stream '{}' already exists — skipping", streamName)
                    continue
                }

                val config = StreamConfiguration.builder()
                    .name(streamName)
                    .subjects(subjects)
                    .retentionPolicy(RetentionPolicy.Limits)
                    .storageType(StorageType.File)
                    .replicas(1)
                    .maxAge(Duration.ofDays(7))
                    .build()

                val info: StreamInfo = jsm.addStream(config)
                logger.info(
                    "Created NATS stream '{}' (subjects={}, messages={})",
                    streamName, subjects, info.streamState.msgCount
                )
            } catch (ex: Exception) {
                logger.warn(
                    "Could not create NATS stream '{}': {} — consumers may fail",
                    streamName, ex.message
                )
            }
        }

        logger.info("NATS JetStream stream initialization complete")
    }
}
