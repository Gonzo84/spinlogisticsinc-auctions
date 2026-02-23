package eu.auctionplatform.notification.infrastructure.persistence.repository

import eu.auctionplatform.notification.domain.model.Notification
import eu.auctionplatform.notification.domain.model.NotificationChannel
import eu.auctionplatform.notification.domain.model.NotificationStatus
import eu.auctionplatform.notification.domain.model.NotificationType
import io.agroal.api.AgroalDataSource
import io.quarkus.agroal.DataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.util.UUID

/**
 * Repository for [Notification] persistence operations.
 *
 * Uses direct JDBC via an [AgroalDataSource] (named "system") for full
 * control over SQL queries and JSON column handling.
 */
@ApplicationScoped
class NotificationRepository @Inject constructor(
    @DataSource("system")
    private val dataSource: AgroalDataSource
) {

    private val logger = LoggerFactory.getLogger(NotificationRepository::class.java)

    companion object {
        private const val SELECT_COLUMNS = """
            id, user_id, type, channel, status, subject, body,
            template_data, locale, read_at, sent_at, created_at
        """

        private const val INSERT = """
            INSERT INTO app.notification_log
                (id, user_id, type, channel, status, subject, body,
                 template_data, locale, read_at, sent_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
        """

        private const val SELECT_BY_USER_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.notification_log
             WHERE user_id = ?
             ORDER BY created_at DESC
             LIMIT ? OFFSET ?
        """

        private const val COUNT_BY_USER_ID = """
            SELECT COUNT(*) FROM app.notification_log WHERE user_id = ?
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.notification_log
             WHERE id = ?
        """

        private const val SELECT_BY_STATUS = """
            SELECT $SELECT_COLUMNS
              FROM app.notification_log
             WHERE status = ?
             ORDER BY created_at ASC
             LIMIT ?
        """

        private const val COUNT_UNREAD = """
            SELECT COUNT(*)
              FROM app.notification_log
             WHERE user_id = ?
               AND channel = 'IN_APP'
               AND read_at IS NULL
        """

        private const val UPDATE_STATUS = """
            UPDATE app.notification_log
               SET status = ?, sent_at = ?
             WHERE id = ?
        """

        private const val MARK_AS_READ = """
            UPDATE app.notification_log
               SET read_at = ?
             WHERE id = ?
               AND read_at IS NULL
        """

        private const val MARK_ALL_AS_READ = """
            UPDATE app.notification_log
               SET read_at = ?
             WHERE user_id = ?
               AND channel = 'IN_APP'
               AND read_at IS NULL
        """
    }

    /**
     * Persists a new notification record.
     *
     * @param notification The notification to insert.
     */
    fun insert(notification: Notification) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT).use { stmt ->
                stmt.setObject(1, notification.id)
                stmt.setObject(2, notification.userId)
                stmt.setString(3, notification.type.name)
                stmt.setString(4, notification.channel.name)
                stmt.setString(5, notification.status.name)
                stmt.setString(6, notification.subject)
                stmt.setString(7, notification.body)
                stmt.setString(8, templateDataToJson(notification.templateData))
                stmt.setString(9, notification.locale)
                if (notification.readAt != null) {
                    stmt.setTimestamp(10, Timestamp.from(notification.readAt))
                } else {
                    stmt.setNull(10, Types.TIMESTAMP)
                }
                if (notification.sentAt != null) {
                    stmt.setTimestamp(11, Timestamp.from(notification.sentAt))
                } else {
                    stmt.setNull(11, Types.TIMESTAMP)
                }
                stmt.setTimestamp(12, Timestamp.from(notification.createdAt))
                stmt.executeUpdate()
            }
        }

        logger.debug(
            "Inserted notification: id={}, userId={}, type={}, channel={}",
            notification.id, notification.userId, notification.type, notification.channel
        )
    }

    /**
     * Finds a notification by its unique identifier.
     *
     * @param id The notification UUID.
     * @return The notification, or null if not found.
     */
    fun findById(id: UUID): Notification? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toNotification() else null
                }
            }
        }
    }

    /**
     * Retrieves paginated notifications for a given user, ordered by creation
     * time descending (newest first).
     *
     * @param userId   The user's UUID.
     * @param limit    Maximum number of notifications to return.
     * @param offset   Number of notifications to skip.
     * @return List of notifications for the current page.
     */
    fun findByUserId(userId: UUID, limit: Int, offset: Int): List<Notification> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_USER_ID).use { stmt ->
                stmt.setObject(1, userId)
                stmt.setInt(2, limit)
                stmt.setInt(3, offset)
                stmt.executeQuery().use { rs ->
                    return rs.toNotificationList()
                }
            }
        }
    }

    /**
     * Counts total notifications for a given user.
     *
     * @param userId The user's UUID.
     * @return Total notification count.
     */
    fun countByUserId(userId: UUID): Long {
        dataSource.connection.use { conn ->
            conn.prepareStatement(COUNT_BY_USER_ID).use { stmt ->
                stmt.setObject(1, userId)
                stmt.executeQuery().use { rs ->
                    rs.next()
                    return rs.getLong(1)
                }
            }
        }
    }

    /**
     * Retrieves notifications with the given status, ordered by creation time
     * ascending (oldest first).
     *
     * @param status The status to filter by.
     * @param limit  Maximum number of notifications to return.
     * @return List of matching notifications.
     */
    fun findByStatus(status: NotificationStatus, limit: Int = 100): List<Notification> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                stmt.setInt(2, limit)
                stmt.executeQuery().use { rs ->
                    return rs.toNotificationList()
                }
            }
        }
    }

    /**
     * Counts unread in-app notifications for a given user.
     *
     * @param userId The user's UUID.
     * @return Number of unread in-app notifications.
     */
    fun countUnread(userId: UUID): Int {
        dataSource.connection.use { conn ->
            conn.prepareStatement(COUNT_UNREAD).use { stmt ->
                stmt.setObject(1, userId)
                stmt.executeQuery().use { rs ->
                    rs.next()
                    return rs.getInt(1)
                }
            }
        }
    }

    /**
     * Updates the status and sentAt timestamp of a notification.
     *
     * @param id     The notification UUID.
     * @param status The new status.
     * @param sentAt The sent timestamp (null if not yet sent).
     */
    fun updateStatus(id: UUID, status: NotificationStatus, sentAt: Instant?) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                if (sentAt != null) {
                    stmt.setTimestamp(2, Timestamp.from(sentAt))
                } else {
                    stmt.setNull(2, Types.TIMESTAMP)
                }
                stmt.setObject(3, id)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Marks a single notification as read.
     *
     * @param id The notification UUID.
     * @return `true` if the notification was updated (was previously unread).
     */
    fun markAsRead(id: UUID): Boolean {
        dataSource.connection.use { conn ->
            conn.prepareStatement(MARK_AS_READ).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(Instant.now()))
                stmt.setObject(2, id)
                return stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * Marks all unread in-app notifications for a user as read.
     *
     * @param userId The user's UUID.
     * @return Number of notifications that were marked as read.
     */
    fun markAllAsRead(userId: UUID): Int {
        dataSource.connection.use { conn ->
            conn.prepareStatement(MARK_ALL_AS_READ).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(Instant.now()))
                stmt.setObject(2, userId)
                return stmt.executeUpdate()
            }
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun templateDataToJson(data: Map<String, Any>): String {
        if (data.isEmpty()) return "{}"
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        return mapper.writeValueAsString(data)
    }

    @Suppress("UNCHECKED_CAST")
    private fun jsonToTemplateData(json: String?): Map<String, Any> {
        if (json.isNullOrBlank() || json == "{}") return emptyMap()
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        return mapper.readValue(json, Map::class.java) as Map<String, Any>
    }

    private fun ResultSet.toNotificationList(): List<Notification> {
        val list = mutableListOf<Notification>()
        while (next()) {
            list.add(toNotification())
        }
        return list
    }

    private fun ResultSet.toNotification(): Notification = Notification(
        id = getObject("id", UUID::class.java),
        userId = getObject("user_id", UUID::class.java),
        type = NotificationType.valueOf(getString("type")),
        channel = NotificationChannel.valueOf(getString("channel")),
        status = NotificationStatus.valueOf(getString("status")),
        subject = getString("subject") ?: "",
        body = getString("body") ?: "",
        templateData = jsonToTemplateData(getString("template_data")),
        locale = getString("locale") ?: "en",
        readAt = getTimestamp("read_at")?.toInstant(),
        sentAt = getTimestamp("sent_at")?.toInstant(),
        createdAt = getTimestamp("created_at").toInstant()
    )
}
