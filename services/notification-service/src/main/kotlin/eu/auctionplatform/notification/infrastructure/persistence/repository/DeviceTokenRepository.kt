package eu.auctionplatform.notification.infrastructure.persistence.repository

import eu.auctionplatform.notification.domain.model.DeviceToken
import io.agroal.api.AgroalDataSource
import io.quarkus.agroal.DataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

/**
 * Repository for [DeviceToken] persistence operations.
 *
 * Manages device tokens used for push notification delivery (FCM, APNs, Web Push).
 * Uses direct JDBC via the named "system" [AgroalDataSource].
 */
@ApplicationScoped
class DeviceTokenRepository @Inject constructor(
    @DataSource("system")
    private val dataSource: AgroalDataSource
) {

    private val logger = LoggerFactory.getLogger(DeviceTokenRepository::class.java)

    companion object {
        private const val SELECT_COLUMNS = """
            id, user_id, platform, token, active, created_at
        """

        private const val SELECT_BY_USER_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.device_tokens
             WHERE user_id = ?
             ORDER BY created_at DESC
        """

        private const val SELECT_ACTIVE_BY_USER_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.device_tokens
             WHERE user_id = ?
               AND active = true
             ORDER BY created_at DESC
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.device_tokens
             WHERE id = ?
        """

        private const val UPSERT = """
            INSERT INTO app.device_tokens
                (id, user_id, platform, token, active, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id, platform, token)
            DO UPDATE SET
                active     = EXCLUDED.active,
                updated_at = NOW()
        """

        private const val DEACTIVATE = """
            UPDATE app.device_tokens
               SET active = false, updated_at = NOW()
             WHERE id = ?
        """

        private const val DEACTIVATE_BY_USER_ID = """
            UPDATE app.device_tokens
               SET active = false, updated_at = NOW()
             WHERE user_id = ?
               AND active = true
        """
    }

    /**
     * Retrieves all device tokens (active and inactive) for a given user.
     *
     * @param userId The user's UUID.
     * @return List of device tokens, ordered by creation time descending.
     */
    fun findByUserId(userId: UUID): List<DeviceToken> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_USER_ID).use { stmt ->
                stmt.setObject(1, userId)
                stmt.executeQuery().use { rs ->
                    return rs.toDeviceTokenList()
                }
            }
        }
    }

    /**
     * Retrieves only active device tokens for a given user.
     *
     * @param userId The user's UUID.
     * @return List of active device tokens.
     */
    fun findActiveByUserId(userId: UUID): List<DeviceToken> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_ACTIVE_BY_USER_ID).use { stmt ->
                stmt.setObject(1, userId)
                stmt.executeQuery().use { rs ->
                    return rs.toDeviceTokenList()
                }
            }
        }
    }

    /**
     * Finds a device token by its unique identifier.
     *
     * @param id The device token UUID.
     * @return The device token, or null if not found.
     */
    fun findById(id: UUID): DeviceToken? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toDeviceToken() else null
                }
            }
        }
    }

    /**
     * Inserts or updates a device token.
     *
     * Uses PostgreSQL's `ON CONFLICT (user_id, platform, token) DO UPDATE` to
     * re-activate an existing token or insert a new one.
     *
     * @param deviceToken The device token to upsert.
     */
    fun upsert(deviceToken: DeviceToken) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPSERT).use { stmt ->
                stmt.setObject(1, deviceToken.id)
                stmt.setObject(2, deviceToken.userId)
                stmt.setString(3, deviceToken.platform)
                stmt.setString(4, deviceToken.token)
                stmt.setBoolean(5, deviceToken.active)
                stmt.setTimestamp(6, Timestamp.from(deviceToken.createdAt))
                stmt.executeUpdate()
            }
        }

        logger.debug(
            "Upserted device token: id={}, userId={}, platform={}",
            deviceToken.id, deviceToken.userId, deviceToken.platform
        )
    }

    /**
     * Deactivates a device token by its identifier.
     *
     * @param id The device token UUID.
     * @return `true` if the token was found and deactivated.
     */
    fun deactivate(id: UUID): Boolean {
        dataSource.connection.use { conn ->
            conn.prepareStatement(DEACTIVATE).use { stmt ->
                stmt.setObject(1, id)
                val updated = stmt.executeUpdate()
                if (updated > 0) {
                    logger.debug("Deactivated device token: id={}", id)
                }
                return updated > 0
            }
        }
    }

    /**
     * Deactivates all active device tokens for a given user.
     *
     * @param userId The user's UUID.
     * @return Number of tokens that were deactivated.
     */
    fun deactivateAllByUserId(userId: UUID): Int {
        dataSource.connection.use { conn ->
            conn.prepareStatement(DEACTIVATE_BY_USER_ID).use { stmt ->
                stmt.setObject(1, userId)
                return stmt.executeUpdate()
            }
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toDeviceTokenList(): List<DeviceToken> {
        val list = mutableListOf<DeviceToken>()
        while (next()) {
            list.add(toDeviceToken())
        }
        return list
    }

    private fun ResultSet.toDeviceToken(): DeviceToken = DeviceToken(
        id = getObject("id", UUID::class.java),
        userId = getObject("user_id", UUID::class.java),
        platform = getString("platform"),
        token = getString("token"),
        active = getBoolean("active"),
        createdAt = getTimestamp("created_at").toInstant()
    )
}
