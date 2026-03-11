package eu.auctionplatform.gateway.infrastructure.websocket

import eu.auctionplatform.commons.auth.roles
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
import jakarta.websocket.server.ServerEndpoint
import org.jboss.logging.Logger
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * User-scoped WebSocket endpoint for real-time notifications and updates.
 *
 * Unlike [AuctionWebSocketEndpoint] which is auction-scoped, this endpoint
 * provides user-targeted events such as settlement updates, lot status changes,
 * fraud alerts (admin), and payment notifications.
 *
 * **Authentication**: JWT token via `?token=<jwt>` query parameter.
 * User roles are extracted and stored for role-based broadcasting.
 */
@ServerEndpoint("/ws/user")
@ApplicationScoped
class UserWebSocketEndpoint @Inject constructor(
    private val webSocketHub: WebSocketHub
) {

    private val heartbeatScheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "ws-user-heartbeat").apply { isDaemon = true }
        }

    private val heartbeatTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()

    companion object {
        private val LOG: Logger = Logger.getLogger(UserWebSocketEndpoint::class.java)
        const val HEARTBEAT_INTERVAL_SECONDS: Long = 30
        const val TOKEN_PARAM: String = "token"
    }

    @OnOpen
    fun onOpen(session: Session) {
        val token = session.requestParameterMap[TOKEN_PARAM]?.firstOrNull()

        if (token.isNullOrBlank()) {
            LOG.warnf("User WebSocket rejected: missing JWT [sessionId=%s]", session.id)
            closeSession(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Missing authentication token")
            return
        }

        val userId = try {
            token.userId()
        } catch (ex: Exception) {
            LOG.warnf("User WebSocket rejected: invalid JWT [sessionId=%s]: %s", session.id, ex.message)
            closeSession(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid authentication token")
            return
        }

        if (userId.isBlank()) {
            LOG.warnf("User WebSocket rejected: no subject in JWT [sessionId=%s]", session.id)
            closeSession(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Token missing user identity")
            return
        }

        val roles = token.roles()

        webSocketHub.registerUser(session, userId, roles)

        val ackMessage = JsonMapper.toJson(mapOf(
            "type" to "connected",
            "userId" to userId,
            "serverTime" to Instant.now().toString(),
            "heartbeatIntervalMs" to HEARTBEAT_INTERVAL_SECONDS * 1000
        ))
        session.asyncRemote.sendText(ackMessage)

        startHeartbeat(session)

        LOG.infof("User WebSocket opened [sessionId=%s, userId=%s, roles=%s]", session.id, userId, roles)
    }

    @OnClose
    fun onClose(session: Session, closeReason: CloseReason) {
        stopHeartbeat(session)
        webSocketHub.unregister(session)
        LOG.infof("User WebSocket closed [sessionId=%s, reason=%s]", session.id, closeReason.reasonPhrase)
    }

    @OnError
    fun onError(session: Session, throwable: Throwable) {
        LOG.errorf(throwable, "User WebSocket error [sessionId=%s]: %s", session.id, throwable.message)
        stopHeartbeat(session)
        webSocketHub.unregister(session)
    }

    @OnMessage
    fun onMessage(session: Session, message: String) {
        if (message.trim().equals("ping", ignoreCase = true)) {
            session.asyncRemote.sendText(JsonMapper.toJson(mapOf(
                "type" to "pong",
                "serverTime" to Instant.now().toString()
            )))
        }
    }

    private fun startHeartbeat(session: Session) {
        val task = heartbeatScheduler.scheduleAtFixedRate(
            {
                try {
                    if (session.isOpen) {
                        val heartbeat = JsonMapper.toJson(mapOf(
                            "type" to "heartbeat",
                            "serverTime" to Instant.now().toString()
                        ))
                        session.asyncRemote.sendText(heartbeat)
                    } else {
                        stopHeartbeat(session)
                    }
                } catch (ex: Exception) {
                    LOG.warnf("User heartbeat failed [sessionId=%s]: %s", session.id, ex.message)
                    stopHeartbeat(session)
                }
            },
            HEARTBEAT_INTERVAL_SECONDS,
            HEARTBEAT_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        )
        heartbeatTasks[session.id] = task
    }

    private fun stopHeartbeat(session: Session) {
        heartbeatTasks.remove(session.id)?.cancel(false)
    }

    private fun closeSession(session: Session, closeCode: CloseReason.CloseCode, reason: String) {
        try {
            session.close(CloseReason(closeCode, reason))
        } catch (ex: Exception) {
            LOG.warnf("Error closing user session [%s]: %s", session.id, ex.message)
        }
    }
}
