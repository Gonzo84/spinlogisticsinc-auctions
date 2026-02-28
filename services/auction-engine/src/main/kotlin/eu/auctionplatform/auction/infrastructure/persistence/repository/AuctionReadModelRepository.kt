package eu.auctionplatform.auction.infrastructure.persistence.repository

import eu.auctionplatform.auction.domain.event.AuctionClosedEvent
import eu.auctionplatform.auction.domain.event.AuctionExtendedEvent
import eu.auctionplatform.auction.domain.event.BidPlacedEvent
import eu.auctionplatform.auction.domain.event.LotAwardedEvent
import eu.auctionplatform.auction.domain.event.ReserveMetEvent
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Denormalized read model for auction queries.
 *
 * Fields mirror the `app.auction_read_model` table and are optimised for
 * the most common query patterns: active auctions closing soon, lookup by
 * auction ID or lot ID, and filtered listing by status/brand.
 */
data class AuctionReadModel(
    val auctionId: UUID,
    val lotId: UUID,
    val brand: String,
    val status: String,
    val startTime: Instant,
    val endTime: Instant,
    val originalEndTime: Instant,
    val startingBid: BigDecimal?,
    val currentHighBid: BigDecimal?,
    val currentHighBidderId: UUID?,
    val bidCount: Int,
    val reserveMet: Boolean,
    val extensionCount: Int,
    val sellerId: UUID,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

/**
 * Repository for the auction read model (CQRS query side).
 *
 * Maintains a denormalized projection of auction state, updated from domain
 * events. This projection is optimised for read-heavy query patterns such
 * as listing active auctions, looking up auction details, and finding
 * auctions that are about to close.
 */
@ApplicationScoped
class AuctionReadModelRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(AuctionReadModelRepository::class.java)
        private const val SELECT_COLUMNS = """
            auction_id, lot_id, brand, status, start_time, end_time,
            original_end_time, starting_bid, current_high_bid,
            current_high_bidder_id, bid_count, reserve_met,
            extension_count, seller_id, created_at, updated_at
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.auction_read_model
             WHERE auction_id = ?
        """

        private const val SELECT_BY_LOT_ID = """
            SELECT $SELECT_COLUMNS
              FROM app.auction_read_model
             WHERE lot_id = ?
        """

        private const val SELECT_ACTIVE_CLOSING_BEFORE = """
            SELECT $SELECT_COLUMNS
              FROM app.auction_read_model
             WHERE status IN ('ACTIVE', 'CLOSING')
               AND end_time <= ?
             ORDER BY end_time ASC
        """

        private const val UPSERT = """
            INSERT INTO app.auction_read_model
                (auction_id, lot_id, brand, status, start_time, end_time,
                 original_end_time, starting_bid, current_high_bid,
                 current_high_bidder_id, bid_count, reserve_met,
                 extension_count, seller_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (auction_id) DO UPDATE SET
                lot_id = EXCLUDED.lot_id,
                brand = EXCLUDED.brand,
                status = EXCLUDED.status,
                start_time = EXCLUDED.start_time,
                end_time = EXCLUDED.end_time,
                original_end_time = EXCLUDED.original_end_time,
                starting_bid = EXCLUDED.starting_bid,
                current_high_bid = EXCLUDED.current_high_bid,
                current_high_bidder_id = EXCLUDED.current_high_bidder_id,
                bid_count = EXCLUDED.bid_count,
                reserve_met = EXCLUDED.reserve_met,
                extension_count = EXCLUDED.extension_count,
                seller_id = EXCLUDED.seller_id,
                updated_at = EXCLUDED.updated_at
        """

        private const val UPDATE_BID = """
            UPDATE app.auction_read_model
               SET current_high_bid = ?,
                   current_high_bidder_id = ?,
                   bid_count = bid_count + 1,
                   updated_at = ?
             WHERE auction_id = ?
        """

        private const val UPDATE_EXTENSION = """
            UPDATE app.auction_read_model
               SET end_time = ?,
                   extension_count = ?,
                   updated_at = ?
             WHERE auction_id = ?
        """

        private const val UPDATE_STATUS = """
            UPDATE app.auction_read_model
               SET status = ?,
                   updated_at = ?
             WHERE auction_id = ?
        """

        private const val UPDATE_RESERVE_MET = """
            UPDATE app.auction_read_model
               SET reserve_met = TRUE,
                   updated_at = ?
             WHERE auction_id = ?
        """
    }

    /**
     * Finds all active auctions whose end time is at or before the given [time].
     *
     * Used by the [AuctionClosingScheduler] to identify auctions that should
     * transition to CLOSED status.
     *
     * @param time The cutoff time (inclusive).
     * @return List of auctions closing at or before the specified time.
     */
    fun findActiveAuctionsClosingBefore(time: Instant): List<AuctionReadModel> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_ACTIVE_CLOSING_BEFORE).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(time))
                stmt.executeQuery().use { rs ->
                    return rs.toModelList()
                }
            }
        }
    }

    /**
     * Finds an auction read model by its auction identifier.
     *
     * @param auctionId The auction identifier.
     * @return The read model, or `null` if no auction exists with that ID.
     */
    fun findById(auctionId: UUID): AuctionReadModel? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_ID).use { stmt ->
                stmt.setObject(1, auctionId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toModel() else null
                }
            }
        }
    }

    /**
     * Finds an auction read model by its associated lot identifier.
     *
     * @param lotId The lot identifier.
     * @return The read model, or `null` if no auction exists for that lot.
     */
    fun findByLotId(lotId: UUID): AuctionReadModel? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_LOT_ID).use { stmt ->
                stmt.setObject(1, lotId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toModel() else null
                }
            }
        }
    }

    /**
     * Persists or updates an auction read model (upsert semantics).
     *
     * @param readModel The read model to save.
     */
    fun save(readModel: AuctionReadModel) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPSERT).use { stmt ->
                stmt.setObject(1, readModel.auctionId)
                stmt.setObject(2, readModel.lotId)
                stmt.setString(3, readModel.brand)
                stmt.setString(4, readModel.status)
                stmt.setTimestamp(5, Timestamp.from(readModel.startTime))
                stmt.setTimestamp(6, Timestamp.from(readModel.endTime))
                stmt.setTimestamp(7, Timestamp.from(readModel.originalEndTime))
                stmt.setBigDecimal(8, readModel.startingBid)
                stmt.setBigDecimal(9, readModel.currentHighBid)
                stmt.setObject(10, readModel.currentHighBidderId)
                stmt.setInt(11, readModel.bidCount)
                stmt.setBoolean(12, readModel.reserveMet)
                stmt.setInt(13, readModel.extensionCount)
                stmt.setObject(14, readModel.sellerId)
                stmt.setTimestamp(15, Timestamp.from(readModel.createdAt))
                stmt.setTimestamp(16, Timestamp.from(readModel.updatedAt))
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Saved read model for auction %s", readModel.auctionId)
    }

    /**
     * Updates the read model based on a domain event.
     *
     * Dispatches to the appropriate update method based on the event type.
     * Unknown event types are silently ignored since not all domain events
     * affect the read model.
     *
     * @param event The domain event to project onto the read model.
     */
    fun updateFromEvent(event: Any) {
        val now = Instant.now()
        when (event) {
            is BidPlacedEvent -> {
                dataSource.connection.use { conn ->
                    conn.prepareStatement(UPDATE_BID).use { stmt ->
                        stmt.setBigDecimal(1, event.bidAmount)
                        stmt.setObject(2, UUID.fromString(event.bidderId))
                        stmt.setTimestamp(3, Timestamp.from(now))
                        stmt.setObject(4, UUID.fromString(event.aggregateId))
                        stmt.executeUpdate()
                    }
                }
                LOG.debugf("Updated read model for bid on auction %s", event.aggregateId)
            }

            is AuctionExtendedEvent -> {
                dataSource.connection.use { conn ->
                    conn.prepareStatement(UPDATE_EXTENSION).use { stmt ->
                        stmt.setTimestamp(1, Timestamp.from(event.newEndTime))
                        stmt.setInt(2, event.extensionCount)
                        stmt.setTimestamp(3, Timestamp.from(now))
                        stmt.setObject(4, UUID.fromString(event.aggregateId))
                        stmt.executeUpdate()
                    }
                }
                LOG.debugf("Updated read model extension for auction %s", event.aggregateId)
            }

            is AuctionClosedEvent -> {
                dataSource.connection.use { conn ->
                    conn.prepareStatement(UPDATE_STATUS).use { stmt ->
                        stmt.setString(1, "CLOSED")
                        stmt.setTimestamp(2, Timestamp.from(now))
                        stmt.setObject(3, UUID.fromString(event.aggregateId))
                        stmt.executeUpdate()
                    }
                }
                LOG.debugf("Updated read model status to CLOSED for auction %s", event.aggregateId)
            }

            is LotAwardedEvent -> {
                dataSource.connection.use { conn ->
                    conn.prepareStatement(UPDATE_STATUS).use { stmt ->
                        stmt.setString(1, "AWARDED")
                        stmt.setTimestamp(2, Timestamp.from(now))
                        stmt.setObject(3, UUID.fromString(event.aggregateId))
                        stmt.executeUpdate()
                    }
                }
                LOG.debugf("Updated read model status to AWARDED for auction %s", event.aggregateId)
            }

            is ReserveMetEvent -> {
                dataSource.connection.use { conn ->
                    conn.prepareStatement(UPDATE_RESERVE_MET).use { stmt ->
                        stmt.setTimestamp(1, Timestamp.from(now))
                        stmt.setObject(2, UUID.fromString(event.aggregateId))
                        stmt.executeUpdate()
                    }
                }
                LOG.debugf("Updated read model reserve met for auction %s", event.aggregateId)
            }

            else -> {
                LOG.tracef("Ignoring event type %s for read model projection", event::class.simpleName)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toModelList(): List<AuctionReadModel> {
        val models = mutableListOf<AuctionReadModel>()
        while (next()) {
            models.add(toModel())
        }
        return models
    }

    private fun ResultSet.toModel(): AuctionReadModel = AuctionReadModel(
        auctionId = getObject("auction_id", UUID::class.java),
        lotId = getObject("lot_id", UUID::class.java),
        brand = getString("brand"),
        status = getString("status"),
        startTime = getTimestamp("start_time").toInstant(),
        endTime = getTimestamp("end_time").toInstant(),
        originalEndTime = getTimestamp("original_end_time").toInstant(),
        startingBid = getBigDecimal("starting_bid"),
        currentHighBid = getBigDecimal("current_high_bid"),
        currentHighBidderId = getObject("current_high_bidder_id", UUID::class.java),
        bidCount = getInt("bid_count"),
        reserveMet = getBoolean("reserve_met"),
        extensionCount = getInt("extension_count"),
        sellerId = getObject("seller_id", UUID::class.java),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant()
    )
}
