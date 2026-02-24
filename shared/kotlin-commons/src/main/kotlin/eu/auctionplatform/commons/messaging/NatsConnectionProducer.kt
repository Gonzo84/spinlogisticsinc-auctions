package eu.auctionplatform.commons.messaging

import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Disposes
import jakarta.enterprise.inject.Produces
import org.eclipse.microprofile.config.ConfigProvider
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * CDI producer for NATS [Connection].
 *
 * Reads the `nats.url` configuration property (defaulting to `nats://localhost:4222`)
 * and produces an application-scoped NATS connection with automatic reconnect.
 */
@ApplicationScoped
class NatsConnectionProducer {

    private val logger = LoggerFactory.getLogger(NatsConnectionProducer::class.java)

    @Produces
    @ApplicationScoped
    fun produceConnection(): Connection {
        val url = ConfigProvider.getConfig()
            .getOptionalValue("nats.url", String::class.java)
            .orElse("nats://localhost:4222")

        logger.info("Connecting to NATS at {}", url)

        val options = Options.Builder()
            .server(url)
            .maxReconnects(-1)
            .reconnectWait(Duration.ofSeconds(2))
            .connectionTimeout(Duration.ofSeconds(5))
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

    @Produces
    @ApplicationScoped
    fun producePublisher(connection: Connection): NatsPublisher {
        return NatsPublisher(connection)
    }

    fun close(@Disposes connection: Connection) {
        logger.info("Closing NATS connection")
        connection.close()
    }
}
