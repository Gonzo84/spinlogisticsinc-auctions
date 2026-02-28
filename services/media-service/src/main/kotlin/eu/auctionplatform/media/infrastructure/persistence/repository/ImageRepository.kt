package eu.auctionplatform.media.infrastructure.persistence.repository

import eu.auctionplatform.media.domain.model.ImageStatus
import eu.auctionplatform.media.domain.model.MediaImage
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Direct JDBC repository for [MediaImage] persistence.
 *
 * Uses the named "system" datasource via Agroal, providing full SQL control
 * without ORM overhead -- consistent with the platform's repository pattern.
 */
@ApplicationScoped
class ImageRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(ImageRepository::class.java)

        private const val SELECT_COLUMNS = """
            id, lot_id, object_key, original_url, processed_url, thumbnail_url,
            display_order, is_primary, status, content_type, file_size, created_at
        """

        private const val INSERT = """
            INSERT INTO app.images
                (id, lot_id, object_key, original_url, processed_url, thumbnail_url,
                 display_order, is_primary, status, content_type, file_size, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_COLUMNS FROM app.images WHERE id = ?
        """

        private const val SELECT_BY_LOT_ID = """
            SELECT $SELECT_COLUMNS FROM app.images
             WHERE lot_id = ?
             ORDER BY display_order ASC, created_at ASC
        """

        private const val SELECT_PRIMARY_BY_LOT_ID = """
            SELECT $SELECT_COLUMNS FROM app.images
             WHERE lot_id = ? AND is_primary = TRUE
             LIMIT 1
        """

        private const val UPDATE = """
            UPDATE app.images
               SET original_url = ?, processed_url = ?, thumbnail_url = ?,
                   display_order = ?, is_primary = ?, status = ?,
                   content_type = ?, file_size = ?
             WHERE id = ?
        """

        private const val UPDATE_STATUS = """
            UPDATE app.images SET status = ? WHERE id = ?
        """

        private const val UPDATE_URLS = """
            UPDATE app.images
               SET original_url = ?, processed_url = ?, thumbnail_url = ?, status = ?
             WHERE id = ?
        """

        private const val UPDATE_ORDER = """
            UPDATE app.images SET display_order = ? WHERE id = ?
        """

        private const val UPDATE_PRIMARY = """
            UPDATE app.images SET is_primary = ? WHERE id = ?
        """

        private const val CLEAR_PRIMARY_FOR_LOT = """
            UPDATE app.images SET is_primary = FALSE WHERE lot_id = ? AND is_primary = TRUE
        """

        private const val DELETE_BY_ID = """
            DELETE FROM app.images WHERE id = ?
        """

        private const val COUNT_BY_LOT_ID = """
            SELECT COUNT(*) FROM app.images WHERE lot_id = ?
        """
    }

    /**
     * Persists a new image record.
     *
     * @param image The image domain model to insert.
     * @return The persisted image (same reference).
     */
    fun save(image: MediaImage): MediaImage {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT).use { stmt ->
                stmt.setObject(1, image.id)
                stmt.setObject(2, image.lotId)
                stmt.setString(3, image.objectKey)
                stmt.setString(4, image.originalUrl)
                stmt.setString(5, image.processedUrl)
                stmt.setString(6, image.thumbnailUrl)
                stmt.setInt(7, image.displayOrder)
                stmt.setBoolean(8, image.isPrimary)
                stmt.setString(9, image.status.name)
                stmt.setString(10, image.contentType)
                stmt.setLong(11, image.fileSize)
                stmt.setTimestamp(12, Timestamp.from(image.createdAt))
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Saved image %s for lot %s", image.id, image.lotId)
        return image
    }

    /**
     * Finds an image by its unique identifier.
     *
     * @param id The image UUID.
     * @return The image, or `null` if not found.
     */
    fun findById(id: UUID): MediaImage? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toMediaImage() else null
                }
            }
        }
    }

    /**
     * Returns all images for a lot, ordered by display order ascending.
     *
     * @param lotId The lot identifier.
     * @return Ordered list of images; empty if none exist.
     */
    fun findByLotId(lotId: UUID): List<MediaImage> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_LOT_ID).use { stmt ->
                stmt.setObject(1, lotId)
                stmt.executeQuery().use { rs ->
                    return rs.toList()
                }
            }
        }
    }

    /**
     * Returns the primary (hero) image for a lot.
     *
     * @param lotId The lot identifier.
     * @return The primary image, or `null` if no primary is set.
     */
    fun findPrimaryByLotId(lotId: UUID): MediaImage? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_PRIMARY_BY_LOT_ID).use { stmt ->
                stmt.setObject(1, lotId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toMediaImage() else null
                }
            }
        }
    }

    /**
     * Updates all mutable fields of an image record.
     *
     * @param image The image with updated fields.
     */
    fun update(image: MediaImage) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE).use { stmt ->
                stmt.setString(1, image.originalUrl)
                stmt.setString(2, image.processedUrl)
                stmt.setString(3, image.thumbnailUrl)
                stmt.setInt(4, image.displayOrder)
                stmt.setBoolean(5, image.isPrimary)
                stmt.setString(6, image.status.name)
                stmt.setString(7, image.contentType)
                stmt.setLong(8, image.fileSize)
                stmt.setObject(9, image.id)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Updates the status of an image.
     *
     * @param id     The image UUID.
     * @param status The new status.
     */
    fun updateStatus(id: UUID, status: ImageStatus) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                stmt.setObject(2, id)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Updates the URLs and status after processing completes.
     *
     * @param id           The image UUID.
     * @param originalUrl  URL to the original image.
     * @param processedUrl URL to the processed image.
     * @param thumbnailUrl URL to the thumbnail.
     * @param status       The new status (typically READY).
     */
    fun updateUrls(id: UUID, originalUrl: String, processedUrl: String, thumbnailUrl: String, status: ImageStatus) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_URLS).use { stmt ->
                stmt.setString(1, originalUrl)
                stmt.setString(2, processedUrl)
                stmt.setString(3, thumbnailUrl)
                stmt.setString(4, status.name)
                stmt.setObject(5, id)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Updates the display order of an image.
     *
     * @param id           The image UUID.
     * @param displayOrder The new display order position.
     */
    fun updateOrder(id: UUID, displayOrder: Int) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_ORDER).use { stmt ->
                stmt.setInt(1, displayOrder)
                stmt.setObject(2, id)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * Sets an image as the primary image for its lot.
     *
     * First clears the primary flag on all other images for the same lot,
     * then sets this image as primary.
     *
     * @param imageId The image to set as primary.
     * @param lotId   The lot that owns this image.
     */
    fun setPrimary(imageId: UUID, lotId: UUID) {
        dataSource.connection.use { conn ->
            val originalAutoCommit = conn.autoCommit
            conn.autoCommit = false
            try {
                // Clear existing primary
                conn.prepareStatement(CLEAR_PRIMARY_FOR_LOT).use { stmt ->
                    stmt.setObject(1, lotId)
                    stmt.executeUpdate()
                }
                // Set new primary
                conn.prepareStatement(UPDATE_PRIMARY).use { stmt ->
                    stmt.setBoolean(1, true)
                    stmt.setObject(2, imageId)
                    stmt.executeUpdate()
                }
                conn.commit()
            } catch (ex: Exception) {
                conn.rollback()
                throw ex
            } finally {
                conn.autoCommit = originalAutoCommit
            }
        }
    }

    /**
     * Deletes an image record by its identifier.
     *
     * @param id The image UUID to delete.
     * @return `true` if a record was deleted, `false` if not found.
     */
    fun deleteById(id: UUID): Boolean {
        dataSource.connection.use { conn ->
            conn.prepareStatement(DELETE_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                return stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * Returns the count of images for a lot.
     *
     * @param lotId The lot identifier.
     * @return Number of images.
     */
    fun countByLotId(lotId: UUID): Long {
        dataSource.connection.use { conn ->
            conn.prepareStatement(COUNT_BY_LOT_ID).use { stmt ->
                stmt.setObject(1, lotId)
                stmt.executeQuery().use { rs ->
                    rs.next()
                    return rs.getLong(1)
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toList(): List<MediaImage> {
        val images = mutableListOf<MediaImage>()
        while (next()) {
            images.add(toMediaImage())
        }
        return images
    }

    private fun ResultSet.toMediaImage(): MediaImage = MediaImage(
        id = getObject("id", UUID::class.java),
        lotId = getObject("lot_id", UUID::class.java),
        objectKey = getString("object_key"),
        originalUrl = getString("original_url"),
        processedUrl = getString("processed_url"),
        thumbnailUrl = getString("thumbnail_url"),
        displayOrder = getInt("display_order"),
        isPrimary = getBoolean("is_primary"),
        status = ImageStatus.valueOf(getString("status")),
        contentType = getString("content_type"),
        fileSize = getLong("file_size"),
        createdAt = getTimestamp("created_at").toInstant()
    )
}
