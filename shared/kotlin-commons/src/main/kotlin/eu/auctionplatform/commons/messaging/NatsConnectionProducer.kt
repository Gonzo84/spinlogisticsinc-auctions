package eu.auctionplatform.commons.messaging

import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Disposes
import jakarta.enterprise.inject.Produces
import org.eclipse.microprofile.config.ConfigProvider
import org.jboss.logging.Logger
import java.time.Duration

/**
 * CDI producer for NATS [Connection].
 *
 * Reads the `nats.url` configuration property (defaulting to `nats://localhost:4222`)
 * and produces an application-scoped NATS connection with automatic reconnect.
 */
@ApplicationScoped
class NatsConnectionProducer {

    companion object {
        private val LOG: Logger = Logger.getLogger(NatsConnectionProducer::class.java)
    }

    @Produces
    @ApplicationScoped
    fun produceConnection(): Connection {
        val url = ConfigProvider.getConfig()
            .getOptionalValue("nats.url", String::class.java)
            .orElse("nats://localhost:4222")

        LOG.infof("Connecting to NATS at %s", url)

        val options = Options.Builder()
            .server(url)
            .maxReconnects(-1)
            .reconnectWait(Duration.ofSeconds(2))
            .connectionTimeout(Duration.ofSeconds(5))
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

    @Produces
    @ApplicationScoped
    fun producePublisher(connection: Connection): NatsPublisher {
        return NatsPublisher(connection)
    }

    fun close(@Disposes connection: Connection) {
        LOG.info("Closing NATS connection")
        connection.close()
    }
}
