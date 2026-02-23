package eu.auctionplatform.payment.infrastructure.persistence.repository

import eu.auctionplatform.payment.domain.model.Settlement
import eu.auctionplatform.payment.domain.model.SettlementStatus
import io.agroal.api.AgroalDataSource
import io.quarkus.agroal.DataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Repository for the [Settlement] entity.
 *
 * Uses direct JDBC via an [AgroalDataSource] (the "SandBox" pattern) for
 * full SQL control over settlement CRUD operations.
 */
@ApplicationScoped
class SettlementRepository @Inject constructor(
    @DataSource("system")
    private val dataSource: AgroalDataSource
) {

    private val logger = LoggerFactory.getLogger(SettlementRepository::class.java)

    companion object {
        private const val SELECT_COLUMNS = """
            id, seller_id, payment_id, net_amount, commission,
            commission_rate, status, settled_at, bank_reference, created_at
        """

        private const val INSERT_SETTLEMENT = """
            INSERT INTO app.settlements
                (id, seller_id, payment_id, net_amount, commission,
                 commission_rate, status, settled_at, bank_reference,
                 created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_COLUMNS FROM app.settlements WHERE id = ?
        """

        private const val SELECT_BY_SELLER_ID = """
            SELECT $SELECT_COLUMNS FROM app.settlements
            WHERE seller_id = ? ORDER BY created_at DESC
        """

        private const val SELECT_BY_PAYMENT_ID = """
            SELECT $SELECT_COLUMNS FROM app.settlements
            WHERE payment_id = ?
        """

        private const val SELECT_BY_STATUS = """
            SELECT $SELECT_COLUMNS FROM app.settlements
            WHERE status = ? ORDER BY created_at ASC
        """

        private const val UPDATE_STATUS = """
            UPDATE app.settlements
               SET status = ?, updated_at = ?
             WHERE id = ?
        """

        private const val UPDATE_SETTLED = """
            UPDATE app.settlements
               SET status = 'PAID', settled_at = ?, bank_reference = ?, updated_at = ?
             WHERE id = ?
        """

        private const val DELETE_BY_ID = """
            DELETE FROM app.settlements WHERE id = ?
        """
    }

    /**
     * Persists a new settlement record.
     *
     * @param settlement The settlement to save.
     */
    fun save(settlement: Settlement) {
        val now = Instant.now()
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT_SETTLEMENT).use { stmt ->
                stmt.setObject(1, settlement.id)
                stmt.setObject(2, settlement.sellerId)
                stmt.setObject(3, settlement.paymentId)
                stmt.setBigDecimal(4, settlement.netAmount)
                stmt.setBigDecimal(5, settlement.commission)
                stmt.setBigDecimal(6, settlement.commissionRate)
                stmt.setString(7, settlement.status.name)
                stmt.setTimestamp(8, settlement.settledAt?.let { Timestamp.from(it) })
                stmt.setString(9, settlement.bankReference)
                stmt.setTimestamp(10, Timestamp.from(settlement.createdAt))
                stmt.setTimestamp(11, Timestamp.from(now))
                stmt.executeUpdate()
            }
        }
        logger.debug("Saved settlement {} for seller {} (payment={})",
            settlement.id, settlement.sellerId, settlement.paymentId)
    }

    /**
     * Finds a settlement by its unique identifier.
     *
     * @param id The settlement UUID.
     * @return The settlement, or null if not found.
     */
    fun findById(id: UUID): Settlement? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toSettlement() else null
                }
            }
        }
    }

    /**
     * Returns all settlements for a given seller, ordered by creation time descending.
     *
     * @param sellerId The seller's UUID.
     * @return List of settlements.
     */
    fun findBySellerId(sellerId: UUID): List<Settlement> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_SELLER_ID).use { stmt ->
                stmt.setObject(1, sellerId)
                stmt.executeQuery().use { rs ->
                    return rs.toSettlementList()
                }
            }
        }
    }

    /**
     * Finds the settlement associated with a given payment.
     *
     * @param paymentId The payment UUID.
     * @return The settlement, or null if not yet created.
     */
    fun findByPaymentId(paymentId: UUID): Settlement? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_PAYMENT_ID).use { stmt ->
                stmt.setObject(1, paymentId)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toSettlement() else null
                }
            }
        }
    }

    /**
     * Returns all settlements with the given status, ordered by creation time ascending.
     *
     * @param status The settlement status to filter by.
     * @return List of matching settlements.
     */
    fun findByStatus(status: SettlementStatus): List<Settlement> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                stmt.executeQuery().use { rs ->
                    return rs.toSettlementList()
                }
            }
        }
    }

    /**
     * Updates only the status of a settlement.
     *
     * @param id The settlement UUID.
     * @param status The new status.
     */
    fun updateStatus(id: UUID, status: SettlementStatus) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                stmt.setTimestamp(2, Timestamp.from(Instant.now()))
                stmt.setObject(3, id)
                stmt.executeUpdate()
            }
        }
        logger.debug("Updated settlement {} status to {}", id, status)
    }

    /**
     * Marks a settlement as paid with bank reference and settlement timestamp.
     *
     * @param id The settlement UUID.
     * @param settledAt Timestamp of the confirmed bank transfer.
     * @param bankReference The bank wire transfer reference.
     */
    fun markSettled(id: UUID, settledAt: Instant, bankReference: String) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_SETTLED).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(settledAt))
                stmt.setString(2, bankReference)
                stmt.setTimestamp(3, Timestamp.from(Instant.now()))
                stmt.setObject(4, id)
                stmt.executeUpdate()
            }
        }
        logger.debug("Marked settlement {} as PAID (bankRef={})", id, bankReference)
    }

    /**
     * Deletes a settlement by its identifier.
     *
     * @param id The settlement UUID.
     */
    fun deleteById(id: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(DELETE_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeUpdate()
            }
        }
        logger.debug("Deleted settlement {}", id)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toSettlementList(): List<Settlement> {
        val settlements = mutableListOf<Settlement>()
        while (next()) {
            settlements.add(toSettlement())
        }
        return settlements
    }

    private fun ResultSet.toSettlement(): Settlement = Settlement(
        id = getObject("id", UUID::class.java),
        sellerId = getObject("seller_id", UUID::class.java),
        paymentId = getObject("payment_id", UUID::class.java),
        netAmount = getBigDecimal("net_amount"),
        commission = getBigDecimal("commission"),
        commissionRate = getBigDecimal("commission_rate"),
        status = SettlementStatus.valueOf(getString("status")),
        settledAt = getTimestamp("settled_at")?.toInstant(),
        bankReference = getString("bank_reference"),
        createdAt = getTimestamp("created_at").toInstant()
    )
}
