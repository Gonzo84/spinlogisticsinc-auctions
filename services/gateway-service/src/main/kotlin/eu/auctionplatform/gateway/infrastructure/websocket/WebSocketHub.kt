package eu.auctionplatform.gateway.infrastructure.websocket

import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.Session
import org.jboss.logging.Logger
import java.util.concurrent.ConcurrentHashMap

// =============================================================================
// WebSocket Hub – centralised connection manager for real-time auction events
// =============================================================================

/**
 * Manages WebSocket sessions, maintaining mappings from users and auctions to
 * their active sessions. Provides broadcast capabilities for pushing real-time
 * auction events (bids, extensions, closings) to connected clients.
 *
 * Thread-safety is ensured via [ConcurrentHashMap] with copy-on-write sets for
 * session collections. This avoids locking during broadcasts while keeping
 * registration/unregistration safe under concurrent access.
 */
@ApplicationScoped
class WebSocketHub {

    companion object {
        private val LOG: Logger = Logger.getLogger(WebSocketHub::class.java)
    }

    /** userId -> set of active sessions for that user. */
    private val userSessions = ConcurrentHashMap<String, MutableSet<Session>>()

    /** auctionId -> set of active sessions watching that auction. */
    private val auctionSessions = ConcurrentHashMap<String, MutableSet<Session>>()

    /** sessionId -> metadata (userId, auctionId, roles) for reverse lookup on disconnect. */
    private val sessionMetadata = ConcurrentHashMap<String, SessionMetadata>()

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers a WebSocket [session] for the given [userId] and [auctionId].
     *
     * After registration, the session will receive broadcast messages for the
     * specified auction and any user-targeted messages.
     */
    fun register(session: Session, userId: String, auctionId: String) {
        registerInternal(session, userId, auctionId, emptyList())
    }

    /**
     * Registers a user-scoped WebSocket [session] (no auction context).
     *
     * Used by [UserWebSocketEndpoint] for user-targeted real-time events
     * such as settlement updates, lot status changes, and admin alerts.
     */
    fun registerUser(session: Session, userId: String, roles: List<String> = emptyList()) {
        registerInternal(session, userId, null, roles)
    }

    private fun registerInternal(session: Session, userId: String, auctionId: String?, roles: List<String>) {
        val sessionId = session.id

        // Store metadata for reverse lookup
        sessionMetadata[sessionId] = SessionMetadata(userId, auctionId, roles)

        // Add to user sessions
        userSessions.computeIfAbsent(userId) {
            ConcurrentHashMap.newKeySet()
        }.add(session)

        // Add to auction sessions (only if auction-scoped)
        if (auctionId != null) {
            auctionSessions.computeIfAbsent(auctionId) {
                ConcurrentHashMap.newKeySet()
            }.add(session)
        }

        LOG.infof(
            "WebSocket session registered [sessionId=%s, userId=%s, auctionId=%s, roles=%s]. " +
                "Active: users=%s, auctions=%s, total sessions=%s",
            sessionId, userId, auctionId ?: "none", roles.joinToString(","),
            userSessions.size, auctionSessions.size, sessionMetadata.size
        )
    }

    /**
     * Unregisters a WebSocket [session], removing it from all user and auction
     * mappings. Safe to call multiple times for the same session.
     */
    fun unregister(session: Session) {
        val sessionId = session.id
        val metadata = sessionMetadata.remove(sessionId) ?: return

        // Remove from user sessions
        userSessions[metadata.userId]?.let { sessions ->
            sessions.remove(session)
            if (sessions.isEmpty()) {
                userSessions.remove(metadata.userId)
            }
        }

        // Remove from auction sessions (only if auction-scoped)
        if (metadata.auctionId != null) {
            auctionSessions[metadata.auctionId]?.let { sessions ->
                sessions.remove(session)
                if (sessions.isEmpty()) {
                    auctionSessions.remove(metadata.auctionId)
                }
            }
        }

        LOG.infof(
            "WebSocket session unregistered [sessionId=%s, userId=%s, auctionId=%s]. " +
                "Active: users=%s, auctions=%s, total sessions=%s",
            sessionId, metadata.userId, metadata.auctionId ?: "none",
            userSessions.size, auctionSessions.size, sessionMetadata.size
        )
    }

    // -------------------------------------------------------------------------
    // Broadcasting
    // -------------------------------------------------------------------------

    /**
     * Broadcasts a [message] to all sessions watching the given [auctionId].
     *
     * Sessions that fail to receive the message are silently removed to prevent
     * stale connections from accumulating.
     */
    fun broadcast(auctionId: String, message: String) {
        val sessions = auctionSessions[auctionId]
        if (sessions.isNullOrEmpty()) {
            LOG.debugf("No active sessions for auction [%s] -- skipping broadcast", auctionId)
            return
        }

        LOG.debugf(
            "Broadcasting to %s sessions for auction [%s], message size=%s chars",
            sessions.size, auctionId, message.length
        )

        val stale = mutableListOf<Session>()
        for (session in sessions) {
            try {
                if (session.isOpen) {
                    session.asyncRemote.sendText(message)
                } else {
                    stale.add(session)
                }
            } catch (ex: Exception) {
                LOG.warnf(
                    "Failed to send message to session [%s]: %s",
                    session.id, ex.message
                )
                stale.add(session)
            }
        }

        // Clean up stale sessions
        for (session in stale) {
            unregister(session)
        }
    }

    /**
     * Sends a [message] to all sessions belonging to the given [userId].
     *
     * Useful for user-specific notifications (e.g. "you have been outbid").
     */
    fun sendToUser(userId: String, message: String) {
        val sessions = userSessions[userId]
        if (sessions.isNullOrEmpty()) {
            LOG.debugf("No active sessions for user [%s] -- skipping send", userId)
            return
        }

        val stale = mutableListOf<Session>()
        for (session in sessions) {
            try {
                if (session.isOpen) {
                    session.asyncRemote.sendText(message)
                } else {
                    stale.add(session)
                }
            } catch (ex: Exception) {
                LOG.warnf(
                    "Failed to send message to session [%s] for user [%s]: %s",
                    session.id, userId, ex.message
                )
                stale.add(session)
            }
        }

        for (session in stale) {
            unregister(session)
        }
    }

    /**
     * Broadcasts a [message] to all sessions belonging to users with the
     * specified [role]. Used for admin-wide alerts and notifications.
     */
    fun broadcastToRole(role: String, message: String) {
        val stale = mutableListOf<Session>()
        var sent = 0

        for ((sessionId, metadata) in sessionMetadata) {
            if (role !in metadata.roles) continue
            val session = findSessionById(sessionId) ?: continue

            try {
                if (session.isOpen) {
                    session.asyncRemote.sendText(message)
                    sent++
                } else {
                    stale.add(session)
                }
            } catch (ex: Exception) {
                LOG.warnf("Failed to send role broadcast to session [%s]: %s", sessionId, ex.message)
                stale.add(session)
            }
        }

        for (session in stale) {
            unregister(session)
        }

        LOG.debugf("Role broadcast [%s]: sent to %s sessions", role, sent)
    }

    /**
     * Finds a session by its ID across all user session sets.
     */
    private fun findSessionById(sessionId: String): Session? {
        for (sessions in userSessions.values) {
            for (session in sessions) {
                if (session.id == sessionId) return session
            }
        }
        return null
    }

    // -------------------------------------------------------------------------
    // Metrics / Diagnostics
    // -------------------------------------------------------------------------

    /** Returns the total number of active sessions across all users. */
    fun totalSessions(): Int = sessionMetadata.size

    /** Returns the number of distinct connected users. */
    fun connectedUsers(): Int = userSessions.size

    /** Returns the number of auctions with at least one active session. */
    fun activeAuctions(): Int = auctionSessions.size

    /** Returns the number of active sessions for a specific auction. */
    fun sessionsForAuction(auctionId: String): Int =
        auctionSessions[auctionId]?.size ?: 0

    // -------------------------------------------------------------------------
    // Internal data
    // -------------------------------------------------------------------------

    /**
     * Metadata stored per session for efficient reverse lookups during
     * unregistration.
     */
    private data class SessionMetadata(
        val userId: String,
        val auctionId: String?,
        val roles: List<String> = emptyList()
    )
}
