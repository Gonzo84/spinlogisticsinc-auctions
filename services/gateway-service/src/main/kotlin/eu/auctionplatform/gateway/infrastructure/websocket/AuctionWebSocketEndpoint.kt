package eu.auctionplatform.gateway.infrastructure.websocket

import eu.auctionplatform.commons.auth.userId
import eu.auctionplatform.commons.util.JsonMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.websocket.CloseReason
import jakarta.websocket.OnClose
import jakarta.websocket.OnError
import jakarta.websocket.OnMessage
import jakarta.websocket.OnOpen
import jakarta.websocket.Session
import jakarta.websocket.server.PathParam
import jakarta.websocket.server.ServerEndpoint
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

// =============================================================================
// Auction WebSocket Endpoint – real-time auction event streaming
// =============================================================================

/**
 * WebSocket endpoint for real-time auction event streaming. Clients connect to
 * `/ws/auctions/{auctionId}` with a valid JWT token to receive live bid updates,
 * lot extensions, and closing notifications.
 *
 * **Authentication**: The JWT token must be provided as a query parameter `token`
 * (since WebSocket does not support custom headers in the browser). The token is
 * validated on connect; invalid or missing tokens result in immediate closure.
 *
 * **Heartbeat**: A server-sent heartbeat message is dispatched every 30 seconds
 * to keep the connection alive and provide clock synchronisation data for clients.
 */
@ServerEndpoint("/ws/auctions/{auctionId}")
@ApplicationScoped
class AuctionWebSocketEndpoint @Inject constructor(
    private val webSocketHub: WebSocketHub
) {

    private val logger = LoggerFactory.getLogger(AuctionWebSocketEndpoint::class.java)

    /** Scheduler for periodic heartbeat messages. */
    private val heartbeatScheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "ws-heartbeat").apply { isDaemon = true }
        }

    /** Tracks heartbeat tasks per session so they can be cancelled on disconnect. */
    private val heartbeatTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()

    companion object {
        /** Heartbeat interval in seconds. */
        const val HEARTBEAT_INTERVAL_SECONDS: Long = 30

        /** Query parameter name for the JWT token. */
        const val TOKEN_PARAM: String = "token"
    }

    // -------------------------------------------------------------------------
    // Lifecycle callbacks
    // -------------------------------------------------------------------------

    /**
     * Called when a new WebSocket connection is opened.
     *
     * Validates the JWT token from the `token` query parameter, extracts the
     * user identity, and registers the session with the [WebSocketHub].
     * Starts a periodic heartbeat for clock synchronisation.
     */
    @OnOpen
    fun onOpen(session: Session, @PathParam("auctionId") auctionId: String) {
        val token = extractToken(session)

        if (token.isNullOrBlank()) {
            logger.warn(
                "WebSocket connection rejected: missing or empty JWT token [sessionId={}, auctionId={}]",
                session.id, auctionId
            )
            closeSession(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Missing authentication token")
            return
        }

        val userId = try {
            token.userId()
        } catch (ex: Exception) {
            logger.warn(
                "WebSocket connection rejected: invalid JWT token [sessionId={}, auctionId={}]: {}",
                session.id, auctionId, ex.message
            )
            closeSession(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid authentication token")
            return
        }

        if (userId.isBlank()) {
            logger.warn(
                "WebSocket connection rejected: JWT token has no subject [sessionId={}, auctionId={}]",
                session.id, auctionId
            )
            closeSession(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Token missing user identity")
            return
        }

        // Register the session with the hub
        webSocketHub.register(session, userId, auctionId)

        // Send initial connection acknowledgement
        val ackMessage = JsonMapper.toJson(mapOf(
            "type" to "connected",
            "auctionId" to auctionId,
            "userId" to userId,
            "serverTime" to Instant.now().toString(),
            "heartbeatIntervalMs" to HEARTBEAT_INTERVAL_SECONDS * 1000
        ))
        session.asyncRemote.sendText(ackMessage)

        // Start heartbeat
        startHeartbeat(session, auctionId)

        logger.info(
            "WebSocket session opened [sessionId={}, userId={}, auctionId={}]",
            session.id, userId, auctionId
        )
    }

    /**
     * Called when a WebSocket connection is closed (by client or server).
     *
     * Unregisters the session from the [WebSocketHub] and cancels any
     * pending heartbeat task.
     */
    @OnClose
    fun onClose(session: Session, @PathParam("auctionId") auctionId: String, closeReason: CloseReason) {
        stopHeartbeat(session)
        webSocketHub.unregister(session)

        logger.info(
            "WebSocket session closed [sessionId={}, auctionId={}, reason={} ({})]",
            session.id, auctionId, closeReason.reasonPhrase, closeReason.closeCode
        )
    }

    /**
     * Called when a WebSocket error occurs.
     *
     * Logs the error and unregisters the session to prevent stale connections.
     */
    @OnError
    fun onError(session: Session, @PathParam("auctionId") auctionId: String, throwable: Throwable) {
        logger.error(
            "WebSocket error [sessionId={}, auctionId={}]: {}",
            session.id, auctionId, throwable.message, throwable
        )
        stopHeartbeat(session)
        webSocketHub.unregister(session)
    }

    /**
     * Called when a text message is received from the client.
     *
     * Currently, the server does not expect client messages beyond the initial
     * handshake. A "pong" response is sent if the client sends a "ping" frame
     * (application-level keep-alive).
     */
    @OnMessage
    fun onMessage(session: Session, message: String, @PathParam("auctionId") auctionId: String) {
        logger.debug(
            "Received client message [sessionId={}, auctionId={}]: {}",
            session.id, auctionId, message.take(200)
        )

        // Handle application-level ping/pong
        if (message.trim().equals("ping", ignoreCase = true)) {
            session.asyncRemote.sendText(JsonMapper.toJson(mapOf(
                "type" to "pong",
                "serverTime" to Instant.now().toString()
            )))
        }
    }

    // -------------------------------------------------------------------------
    // Heartbeat
    // -------------------------------------------------------------------------

    /**
     * Starts a periodic heartbeat message for the given session.
     *
     * The heartbeat includes the current server time (UTC) for client-side
     * clock synchronisation, which is critical for accurate countdown timers
     * in the auction UI.
     */
    private fun startHeartbeat(session: Session, auctionId: String) {
        val task = heartbeatScheduler.scheduleAtFixedRate(
            {
                try {
                    if (session.isOpen) {
                        val heartbeat = JsonMapper.toJson(mapOf(
                            "type" to "heartbeat",
                            "serverTime" to Instant.now().toString(),
                            "auctionId" to auctionId,
                            "activeSessions" to webSocketHub.sessionsForAuction(auctionId)
                        ))
                        session.asyncRemote.sendText(heartbeat)
                    } else {
                        stopHeartbeat(session)
                    }
                } catch (ex: Exception) {
                    logger.warn(
                        "Heartbeat failed for session [{}]: {}",
                        session.id, ex.message
                    )
                    stopHeartbeat(session)
                }
            },
            HEARTBEAT_INTERVAL_SECONDS,
            HEARTBEAT_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        )

        heartbeatTasks[session.id] = task
    }

    /**
     * Stops and removes the heartbeat task for the given session.
     */
    private fun stopHeartbeat(session: Session) {
        heartbeatTasks.remove(session.id)?.cancel(false)
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    /**
     * Extracts the JWT token from the `token` query parameter of the WebSocket
     * upgrade request.
     */
    private fun extractToken(session: Session): String? {
        val queryString = session.requestParameterMap[TOKEN_PARAM]
        return queryString?.firstOrNull()
    }

    /**
     * Closes a session with the given close code and reason phrase.
     */
    private fun closeSession(
        session: Session,
        closeCode: CloseReason.CloseCode,
        reason: String
    ) {
        try {
            session.close(CloseReason(closeCode, reason))
        } catch (ex: Exception) {
            logger.warn("Error closing session [{}]: {}", session.id, ex.message)
        }
    }
}
