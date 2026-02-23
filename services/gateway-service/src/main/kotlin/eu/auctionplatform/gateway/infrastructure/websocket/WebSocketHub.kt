package eu.auctionplatform.gateway.infrastructure.websocket

import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.Session
import org.slf4j.LoggerFactory
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

    private val logger = LoggerFactory.getLogger(WebSocketHub::class.java)

    /** userId -> set of active sessions for that user. */
    private val userSessions = ConcurrentHashMap<String, MutableSet<Session>>()

    /** auctionId -> set of active sessions watching that auction. */
    private val auctionSessions = ConcurrentHashMap<String, MutableSet<Session>>()

    /** sessionId -> metadata (userId, auctionId) for reverse lookup on disconnect. */
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
        val sessionId = session.id

        // Store metadata for reverse lookup
        sessionMetadata[sessionId] = SessionMetadata(userId, auctionId)

        // Add to user sessions
        userSessions.computeIfAbsent(userId) {
            ConcurrentHashMap.newKeySet()
        }.add(session)

        // Add to auction sessions
        auctionSessions.computeIfAbsent(auctionId) {
            ConcurrentHashMap.newKeySet()
        }.add(session)

        logger.info(
            "WebSocket session registered [sessionId={}, userId={}, auctionId={}]. " +
                "Active: users={}, auctions={}, total sessions={}",
            sessionId, userId, auctionId,
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

        // Remove from auction sessions
        auctionSessions[metadata.auctionId]?.let { sessions ->
            sessions.remove(session)
            if (sessions.isEmpty()) {
                auctionSessions.remove(metadata.auctionId)
            }
        }

        logger.info(
            "WebSocket session unregistered [sessionId={}, userId={}, auctionId={}]. " +
                "Active: users={}, auctions={}, total sessions={}",
            sessionId, metadata.userId, metadata.auctionId,
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
            logger.debug("No active sessions for auction [{}] -- skipping broadcast", auctionId)
            return
        }

        logger.debug(
            "Broadcasting to {} sessions for auction [{}], message size={} chars",
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
                logger.warn(
                    "Failed to send message to session [{}]: {}",
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
            logger.debug("No active sessions for user [{}] -- skipping send", userId)
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
                logger.warn(
                    "Failed to send message to session [{}] for user [{}]: {}",
                    session.id, userId, ex.message
                )
                stale.add(session)
            }
        }

        for (session in stale) {
            unregister(session)
        }
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
        val auctionId: String
    )
}
