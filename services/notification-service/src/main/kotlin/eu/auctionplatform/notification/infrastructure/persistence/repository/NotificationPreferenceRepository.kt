package eu.auctionplatform.notification.infrastructure.persistence.repository

import eu.auctionplatform.notification.domain.model.NotificationPreference
import eu.auctionplatform.notification.domain.model.NotificationType
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.sql.ResultSet
import java.util.UUID

/**
 * Repository for [NotificationPreference] persistence operations.
 *
 * Uses direct JDBC via the named "system" [AgroalDataSource] for full
 * control over the upsert (INSERT ... ON CONFLICT) logic.
 */
@ApplicationScoped
class NotificationPreferenceRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(NotificationPreferenceRepository::class.java)

        private const val SELECT_BY_USER_ID = """
            SELECT user_id, notification_type, email_enabled, push_enabled, sms_enabled
              FROM app.notification_preferences
             WHERE user_id = ?
             ORDER BY notification_type ASC
        """

        private const val SELECT_BY_USER_AND_TYPE = """
            SELECT user_id, notification_type, email_enabled, push_enabled, sms_enabled
              FROM app.notification_preferences
             WHERE user_id = ?
               AND notification_type = ?
        """

        private const val UPSERT = """
            INSERT INTO app.notification_preferences
                (user_id, notification_type, email_enabled, push_enabled, sms_enabled)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (user_id, notification_type)
            DO UPDATE SET
                email_enabled = EXCLUDED.email_enabled,
                push_enabled  = EXCLUDED.push_enabled,
                sms_enabled   = EXCLUDED.sms_enabled,
                updated_at    = NOW()
        """
    }

    /**
     * Retrieves all notification preferences for a given user.
     *
     * If the user has not configured preferences for some notification types,
     * those types will not appear in the result. The caller should apply
     * defaults for missing types.
     *
     * @param userId The user's UUID.
     * @return List of preferences, ordered by notification type.
     */
    fun findByUserId(userId: UUID): List<NotificationPreference> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_USER_ID).use { stmt ->
                stmt.setObject(1, userId)
                stmt.executeQuery().use { rs ->
                    return rs.toPreferenceList()
                }
            }
        }
    }

    /**
     * Retrieves a single preference for a specific user and notification type.
     *
     * @param userId The user's UUID.
     * @param type   The notification type.
     * @return The preference, or null if not configured (use defaults).
     */
    fun findByUserIdAndType(userId: UUID, type: NotificationType): NotificationPreference? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_USER_AND_TYPE).use { stmt ->
                stmt.setObject(1, userId)
                stmt.setString(2, type.name)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toPreference() else null
                }
            }
        }
    }

    /**
     * Inserts or updates a notification preference.
     *
     * Uses PostgreSQL's `ON CONFLICT ... DO UPDATE` to atomically handle
     * both insert and update cases.
     *
     * @param preference The preference to upsert.
     */
    fun upsert(preference: NotificationPreference) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPSERT).use { stmt ->
                stmt.setObject(1, preference.userId)
                stmt.setString(2, preference.notificationType.name)
                stmt.setBoolean(3, preference.emailEnabled)
                stmt.setBoolean(4, preference.pushEnabled)
                stmt.setBoolean(5, preference.smsEnabled)
                stmt.executeUpdate()
            }
        }

        LOG.debugf(
            "Upserted preference: userId=%s, type=%s, email=%s, push=%s, sms=%s",
            preference.userId, preference.notificationType,
            preference.emailEnabled, preference.pushEnabled, preference.smsEnabled
        )
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toPreferenceList(): List<NotificationPreference> {
        val list = mutableListOf<NotificationPreference>()
        while (next()) {
            list.add(toPreference())
        }
        return list
    }

    private fun ResultSet.toPreference(): NotificationPreference = NotificationPreference(
        userId = getObject("user_id", UUID::class.java),
        notificationType = NotificationType.valueOf(getString("notification_type")),
        emailEnabled = getBoolean("email_enabled"),
        pushEnabled = getBoolean("push_enabled"),
        smsEnabled = getBoolean("sms_enabled")
    )
}
