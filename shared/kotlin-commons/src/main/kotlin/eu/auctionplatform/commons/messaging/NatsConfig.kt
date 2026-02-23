package eu.auctionplatform.commons.messaging

import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Configuration holder and factory for NATS [Connection] instances.
 *
 * Typical usage in a CDI / Spring environment:
 * ```kotlin
 * val config = NatsConfig(url = "nats://localhost:4222")
 * val connection = config.connect()
 * ```
 *
 * @property url              NATS server URL (e.g. "nats://localhost:4222").
 * @property maxReconnects    Maximum number of reconnect attempts (-1 = unlimited).
 * @property reconnectWait    Duration to wait between reconnect attempts.
 * @property connectionTimeout Duration to wait for the initial connection to succeed.
 */
data class NatsConfig(
    val url: String = "nats://localhost:4222",
    val maxReconnects: Int = -1,
    val reconnectWait: Duration = Duration.ofSeconds(2),
    val connectionTimeout: Duration = Duration.ofSeconds(5)
) {

    private val logger = LoggerFactory.getLogger(NatsConfig::class.java)

    /**
     * Creates and returns a connected [Connection] using the configured parameters.
     *
     * The connection is set up with automatic reconnect, a connection listener for
     * observability, and an error listener that logs unexpected errors.
     */
    fun connect(): Connection {
        logger.info("Connecting to NATS at {} (maxReconnects={}, reconnectWait={})",
            url, maxReconnects, reconnectWait)

        val options = Options.Builder()
            .server(url)
            .maxReconnects(maxReconnects)
            .reconnectWait(reconnectWait)
            .connectionTimeout(connectionTimeout)
            .connectionListener { conn, type ->
                logger.info("NATS connection event: {} (server={})", type, conn.serverInfo?.host)
            }
            .errorListener(object : io.nats.client.ErrorListener {
                override fun errorOccurred(conn: Connection, error: String) {
                    logger.error("NATS error: {}", error)
                }

                override fun exceptionOccurred(conn: Connection, exp: Exception) {
                    logger.error("NATS exception", exp)
                }

                override fun slowConsumerDetected(conn: Connection, consumer: io.nats.client.Consumer) {
                    logger.warn("NATS slow consumer detected on connection {}", conn.serverInfo?.host)
                }
            })
            .build()

        val connection = Nats.connect(options)
        logger.info("NATS connection established (status={})", connection.status)
        return connection
    }
}
