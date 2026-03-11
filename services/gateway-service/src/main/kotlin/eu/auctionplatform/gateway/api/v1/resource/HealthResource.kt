package eu.auctionplatform.gateway.api.v1.resource

import eu.auctionplatform.gateway.api.v1.dto.ComponentHealth
import eu.auctionplatform.gateway.api.v1.dto.HealthResponse
import eu.auctionplatform.gateway.infrastructure.websocket.WebSocketHub
import io.agroal.api.AgroalDataSource
import io.nats.client.Connection
import io.quarkus.redis.datasource.RedisDataSource
import jakarta.annotation.security.PermitAll
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import java.time.Instant

/**
 * Health check resource providing aggregated and component-level health status.
 *
 * Used by load balancers, Kubernetes probes, and monitoring systems to determine
 * the service's availability and readiness.
 *
 * - **GET /api/v1/health** -- Aggregated health including all dependencies.
 * - **GET /api/v1/health/ready** -- Readiness probe (checks critical dependencies).
 */
@Path("/api/v1/health")
@Produces(MediaType.APPLICATION_JSON)
class HealthResource {

    @Inject
    lateinit var dataSource: AgroalDataSource

    @Inject
    lateinit var redisDataSource: RedisDataSource

    @Inject
    lateinit var natsConnection: Connection

    @Inject
    lateinit var webSocketHub: WebSocketHub

    companion object {
        private val LOG: Logger = Logger.getLogger(HealthResource::class.java)

        private val KNOWN_SERVICES = setOf(
            "auction-engine", "catalog-service", "user-service",
            "payment-service", "notification-service", "media-service",
            "search-service", "seller-service", "broker-service",
            "analytics-service", "compliance-service", "co2-service"
        )
    }

    // -------------------------------------------------------------------------
    // Aggregated Health
    // -------------------------------------------------------------------------

    /**
     * Returns aggregated health status of the gateway and all its dependencies.
     *
     * **GET /api/v1/health**
     *
     * Checks: database, Redis, NATS, and reports WebSocket session metrics.
     * Returns 200 if the service is operational, 503 if any critical component is down.
     *
     * @return 200 OK or 503 Service Unavailable with component health details.
     */
    @GET
    @PermitAll
    fun health(): Response {
        val checks = mutableMapOf<String, ComponentHealth>()

        // Database check
        checks["database"] = checkDatabase()

        // Redis check
        checks["redis"] = checkRedis()

        // NATS check
        checks["nats"] = checkNats()

        // WebSocket Hub metrics
        checks["websocket"] = ComponentHealth(
            status = "UP",
            details = mapOf(
                "totalSessions" to webSocketHub.totalSessions(),
                "connectedUsers" to webSocketHub.connectedUsers(),
                "activeAuctions" to webSocketHub.activeAuctions()
            )
        )

        val allUp = checks.values.all { it.status == "UP" }
        val overallStatus = if (allUp) "UP" else "DEGRADED"

        val response = HealthResponse(
            status = overallStatus,
            timestamp = Instant.now(),
            checks = checks
        )

        val httpStatus = if (allUp) Response.Status.OK else Response.Status.SERVICE_UNAVAILABLE
        return Response.status(httpStatus).entity(response).build()
    }

    // -------------------------------------------------------------------------
    // Readiness Probe
    // -------------------------------------------------------------------------

    /**
     * Readiness probe for Kubernetes / load balancer health checks.
     *
     * **GET /api/v1/health/ready**
     *
     * Only checks critical dependencies (database, Redis). Returns 200 if the
     * service is ready to accept traffic, 503 otherwise.
     *
     * @return 200 OK or 503 Service Unavailable.
     */
    @GET
    @Path("/ready")
    @PermitAll
    fun readiness(): Response {
        val checks = mutableMapOf<String, ComponentHealth>()

        checks["database"] = checkDatabase()
        checks["redis"] = checkRedis()

        val allUp = checks.values.all { it.status == "UP" }
        val overallStatus = if (allUp) "UP" else "DOWN"

        val response = HealthResponse(
            status = overallStatus,
            timestamp = Instant.now(),
            checks = checks
        )

        val httpStatus = if (allUp) Response.Status.OK else Response.Status.SERVICE_UNAVAILABLE
        return Response.status(httpStatus).entity(response).build()
    }

    // -------------------------------------------------------------------------
    // Service Restart
    // -------------------------------------------------------------------------

    /**
     * Initiates a restart for a named downstream service (mock implementation).
     *
     * **POST /api/v1/health/services/{name}/restart**
     *
     * Validates the service name against the known services list and returns
     * a mock response indicating restart has been initiated.
     *
     * @param name the service name (e.g., "auction-engine", "catalog-service")
     * @return 200 OK with restart status, or 400 Bad Request if service name is unknown.
     */
    @POST
    @Path("/services/{name}/restart")
    @RolesAllowed("admin_ops", "admin_super")
    @Consumes(MediaType.APPLICATION_JSON)
    fun restartService(@PathParam("name") name: String): Response {
        LOG.infof("POST /services/%s/restart requested", name)

        if (name !in KNOWN_SERVICES) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(
                    mapOf(
                        "status" to 400,
                        "title" to "Bad Request",
                        "detail" to "Unknown service: $name. Known services: ${KNOWN_SERVICES.sorted().joinToString(", ")}",
                        "instance" to "/api/v1/health/services/$name/restart"
                    )
                )
                .build()
        }

        return Response.ok(
            mapOf(
                "service" to name,
                "status" to "RESTART_INITIATED",
                "message" to "Restart signal sent to $name. Service will be back online shortly.",
                "timestamp" to Instant.now().toString()
            )
        ).build()
    }

    // -------------------------------------------------------------------------
    // Component checks
    // -------------------------------------------------------------------------

    /**
     * Checks database connectivity by executing a simple query.
     */
    private fun checkDatabase(): ComponentHealth {
        return try {
            dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT 1").use { stmt ->
                    stmt.executeQuery().use { rs ->
                        rs.next()
                    }
                }
            }
            ComponentHealth(status = "UP")
        } catch (ex: Exception) {
            LOG.warnf("Database health check failed: %s", ex.message)
            ComponentHealth(
                status = "DOWN",
                details = mapOf("error" to (ex.message ?: "Unknown error"))
            )
        }
    }

    /**
     * Checks Redis connectivity by executing a PING command.
     */
    private fun checkRedis(): ComponentHealth {
        return try {
            val value = redisDataSource.value(String::class.java)
            value.set("health:ping", "pong")
            val result = value.get("health:ping")
            if (result == "pong") {
                ComponentHealth(status = "UP")
            } else {
                ComponentHealth(
                    status = "DOWN",
                    details = mapOf("error" to "Unexpected Redis response")
                )
            }
        } catch (ex: Exception) {
            LOG.warnf("Redis health check failed: %s", ex.message)
            ComponentHealth(
                status = "DOWN",
                details = mapOf("error" to (ex.message ?: "Unknown error"))
            )
        }
    }

    /**
     * Checks NATS connectivity by inspecting the connection status.
     */
    private fun checkNats(): ComponentHealth {
        return try {
            val status = natsConnection.status
            if (status == Connection.Status.CONNECTED) {
                ComponentHealth(
                    status = "UP",
                    details = mapOf("serverInfo" to (natsConnection.serverInfo?.serverId ?: "unknown"))
                )
            } else {
                ComponentHealth(
                    status = "DOWN",
                    details = mapOf("connectionStatus" to status.name)
                )
            }
        } catch (ex: Exception) {
            LOG.warnf("NATS health check failed: %s", ex.message)
            ComponentHealth(
                status = "DOWN",
                details = mapOf("error" to (ex.message ?: "Unknown error"))
            )
        }
    }
}
