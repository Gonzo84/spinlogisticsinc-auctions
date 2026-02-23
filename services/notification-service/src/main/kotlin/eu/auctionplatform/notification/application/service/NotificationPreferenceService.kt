package eu.auctionplatform.notification.application.service

import eu.auctionplatform.notification.domain.model.NotificationChannel
import eu.auctionplatform.notification.domain.model.NotificationPreference
import eu.auctionplatform.notification.domain.model.NotificationType
import eu.auctionplatform.notification.infrastructure.persistence.repository.NotificationPreferenceRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Application service for managing user notification preferences.
 *
 * Users can configure which delivery channels (email, push, SMS) are
 * enabled for each notification type. In-app notifications are always
 * delivered and are not governed by user preferences.
 *
 * If a user has not configured preferences for a notification type,
 * sensible defaults are returned (email and push enabled, SMS disabled).
 */
@ApplicationScoped
class NotificationPreferenceService @Inject constructor(
    private val preferenceRepository: NotificationPreferenceRepository
) {

    private val logger = LoggerFactory.getLogger(NotificationPreferenceService::class.java)

    /**
     * Retrieves all notification preferences for a user, filling in
     * defaults for any notification types the user hasn't configured.
     *
     * @param userId The user's UUID.
     * @return Complete list of preferences for all notification types.
     */
    fun getPreferences(userId: UUID): List<NotificationPreference> {
        val stored = preferenceRepository.findByUserId(userId)
        val storedByType = stored.associateBy { it.notificationType }

        return NotificationType.entries.map { type ->
            storedByType[type] ?: defaultPreference(userId, type)
        }
    }

    /**
     * Updates a single channel preference for a specific notification type.
     *
     * @param userId  The user's UUID.
     * @param type    The notification type to configure.
     * @param channel The delivery channel to enable or disable.
     * @param enabled Whether the channel should be enabled.
     * @return The updated preference.
     */
    fun updatePreference(
        userId: UUID,
        type: NotificationType,
        channel: NotificationChannel,
        enabled: Boolean
    ): NotificationPreference {
        // Load existing or create default
        val existing = preferenceRepository.findByUserIdAndType(userId, type)
            ?: defaultPreference(userId, type)

        val updated = when (channel) {
            NotificationChannel.EMAIL -> existing.copy(emailEnabled = enabled)
            NotificationChannel.PUSH -> existing.copy(pushEnabled = enabled)
            NotificationChannel.SMS -> existing.copy(smsEnabled = enabled)
            NotificationChannel.IN_APP -> {
                logger.debug(
                    "IN_APP channel is always enabled and cannot be toggled. " +
                        "Ignoring preference update for userId={}, type={}",
                    userId, type
                )
                existing
            }
        }

        preferenceRepository.upsert(updated)

        logger.info(
            "Updated preference: userId={}, type={}, channel={}, enabled={}",
            userId, type, channel, enabled
        )

        return updated
    }

    /**
     * Bulk-updates preferences from a list.
     *
     * @param userId      The user's UUID.
     * @param preferences The preferences to upsert.
     * @return The updated list of all preferences.
     */
    fun updatePreferences(userId: UUID, preferences: List<NotificationPreference>): List<NotificationPreference> {
        for (preference in preferences) {
            require(preference.userId == userId) {
                "Preference userId (${preference.userId}) does not match the authenticated user ($userId)"
            }
            preferenceRepository.upsert(preference)
        }

        logger.info("Bulk-updated {} preferences for userId={}", preferences.size, userId)

        return getPreferences(userId)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the default preference for a notification type when the user
     * has not explicitly configured it.
     */
    private fun defaultPreference(userId: UUID, type: NotificationType): NotificationPreference =
        NotificationPreference(
            userId = userId,
            notificationType = type,
            emailEnabled = true,
            pushEnabled = true,
            smsEnabled = false
        )
}
