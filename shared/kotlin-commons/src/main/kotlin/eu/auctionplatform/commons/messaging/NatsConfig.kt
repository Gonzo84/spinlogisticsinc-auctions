package eu.auctionplatform.commons.messaging

import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import org.jboss.logging.Logger
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

    companion object {
        private val LOG: Logger = Logger.getLogger(NatsConfig::class.java)
    }

    /**
     * Creates and returns a connected [Connection] using the configured parameters.
     *
     * The connection is set up with automatic reconnect, a connection listener for
     * observability, and an error listener that logs unexpected errors.
     */
    fun connect(): Connection {
        LOG.infof("Connecting to NATS at %s (maxReconnects=%s, reconnectWait=%s)",
            url, maxReconnects, reconnectWait)

        val options = Options.Builder()
            .server(url)
            .maxReconnects(maxReconnects)
            .reconnectWait(reconnectWait)
            .connectionTimeout(connectionTimeout)
            .connectionListener { conn, type ->
                LOG.infof("NATS connection event: %s (server=%s)", type, conn.serverInfo?.host)
            }
            .errorListener(object : io.nats.client.ErrorListener {
                override fun errorOccurred(conn: Connection, error: String) {
                    LOG.errorf("NATS error: %s", error)
                }

                override fun exceptionOccurred(conn: Connection, exp: Exception) {
                    LOG.errorf(exp, "NATS exception")
                }

                override fun slowConsumerDetected(conn: Connection, consumer: io.nats.client.Consumer) {
                    LOG.warnf("NATS slow consumer detected on connection %s", conn.serverInfo?.host)
                }
            })
            .build()

        val connection = Nats.connect(options)
        LOG.infof("NATS connection established (status=%s)", connection.status)

        // Initialize JetStream streams so publishers and consumers can work immediately
        NatsStreamInitializer.initializeStreams(connection)

        return connection
    }
}
