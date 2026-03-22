package eu.auctionplatform.payment.infrastructure.persistence.repository

import eu.auctionplatform.payment.domain.model.Payment
import eu.auctionplatform.payment.domain.model.PaymentStatus
import eu.auctionplatform.payment.domain.model.VatScheme
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
 * Repository for the [Payment] aggregate.
 *
 * Uses direct JDBC via an [AgroalDataSource] (the "SandBox" pattern) rather
 * than Panache, giving full control over SQL, parameter binding, and
 * connection lifecycle.
 */
@ApplicationScoped
class PaymentRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    companion object {
        private val LOG: Logger = Logger.getLogger(PaymentRepository::class.java)

        private const val SELECT_COLUMNS = """
            id, buyer_id, seller_id, auction_id, lot_id, hammer_price, buyer_premium,
            buyer_premium_rate, vat_amount, vat_rate, vat_scheme, total_amount,
            currency, country, payment_method, psp_reference, status,
            due_date, paid_at, created_at, lot_title, buyer_name, seller_name
        """

        private const val INSERT_PAYMENT = """
            INSERT INTO app.payments
                (id, buyer_id, seller_id, auction_id, lot_id, hammer_price, buyer_premium,
                 buyer_premium_rate, vat_amount, vat_rate, vat_scheme, total_amount,
                 currency, country, payment_method, psp_reference, status,
                 due_date, paid_at, created_at, updated_at,
                 lot_title, buyer_name, seller_name)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_COLUMNS FROM app.payments WHERE id = ?
        """

        private const val SELECT_BY_BUYER_ID = """
            SELECT $SELECT_COLUMNS FROM app.payments
            WHERE buyer_id = ? ORDER BY created_at DESC
        """

        private const val SELECT_BY_LOT_ID = """
            SELECT $SELECT_COLUMNS FROM app.payments
            WHERE lot_id = ? ORDER BY created_at DESC
        """

        private const val SELECT_BY_STATUS = """
            SELECT $SELECT_COLUMNS FROM app.payments
            WHERE status = ? ORDER BY created_at ASC
        """

        private const val SELECT_ALL = """
            SELECT $SELECT_COLUMNS FROM app.payments
            ORDER BY created_at DESC LIMIT ? OFFSET ?
        """

        private const val COUNT_ALL = """
            SELECT COUNT(*) FROM app.payments
        """

        private const val UPDATE_STATUS = """
            UPDATE app.payments
               SET status = ?, updated_at = ?
             WHERE id = ?
        """

        private const val UPDATE_PAYMENT_COMPLETED = """
            UPDATE app.payments
               SET status = ?, payment_method = ?, psp_reference = ?,
                   paid_at = ?, updated_at = ?
             WHERE id = ?
        """

        private const val SELECT_BY_AUCTION_ID_AND_STATUSES = """
            SELECT $SELECT_COLUMNS FROM app.payments
            WHERE auction_id = ? AND status = ANY(?)
            ORDER BY created_at DESC
        """

        private const val SELECT_OVERDUE = """
            SELECT $SELECT_COLUMNS FROM app.payments
            WHERE status = 'PENDING' AND due_date < ?
            ORDER BY due_date ASC
        """

        private const val DELETE_BY_ID = """
            DELETE FROM app.payments WHERE id = ?
        """
    }

    /**
     * Persists a new payment record.
     *
     * @param payment The payment to save.
     */
    fun save(payment: Payment) {
        val now = Instant.now()
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT_PAYMENT).use { stmt ->
                stmt.setObject(1, payment.id)
                stmt.setObject(2, payment.buyerId)
                stmt.setObject(3, payment.sellerId)
                stmt.setObject(4, payment.auctionId)
                stmt.setObject(5, payment.lotId)
                stmt.setBigDecimal(6, payment.hammerPrice)
                stmt.setBigDecimal(7, payment.buyerPremium)
                stmt.setBigDecimal(8, payment.buyerPremiumRate)
                stmt.setBigDecimal(9, payment.taxAmount)
                stmt.setBigDecimal(10, payment.taxRate)
                stmt.setString(11, payment.taxScheme.name)
                stmt.setBigDecimal(12, payment.totalAmount)
                stmt.setString(13, payment.currency)
                stmt.setString(14, payment.state)
                stmt.setString(15, payment.paymentMethod)
                stmt.setString(16, payment.pspReference)
                stmt.setString(17, payment.status.name)
                stmt.setTimestamp(18, Timestamp.from(payment.dueDate))
                stmt.setTimestamp(19, payment.paidAt?.let { Timestamp.from(it) })
                stmt.setTimestamp(20, Timestamp.from(payment.createdAt))
                stmt.setTimestamp(21, Timestamp.from(now))
                stmt.setString(22, payment.lotTitle)
                stmt.setString(23, payment.buyerName)
                stmt.setString(24, payment.sellerName)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Saved payment %s for buyer %s (lot=%s)", payment.id, payment.buyerId, payment.lotId)
    }

    /**
     * Finds a payment by its unique identifier.
     *
     * @param id The payment UUID.
     * @return The payment, or null if not found.
     */
    fun findById(id: UUID): Payment? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toPayment() else null
                }
            }
        }
    }

    /**
     * Returns all payments for a given buyer, ordered by creation time descending.
     *
     * @param buyerId The buyer's UUID.
     * @return List of payments.
     */
    fun findByBuyerId(buyerId: UUID): List<Payment> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_BUYER_ID).use { stmt ->
                stmt.setObject(1, buyerId)
                stmt.executeQuery().use { rs ->
                    return rs.toPaymentList()
                }
            }
        }
    }

    /**
     * Returns all payments for a given lot, ordered by creation time descending.
     *
     * @param lotId The lot UUID.
     * @return List of payments.
     */
    fun findByLotId(lotId: UUID): List<Payment> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_LOT_ID).use { stmt ->
                stmt.setObject(1, lotId)
                stmt.executeQuery().use { rs ->
                    return rs.toPaymentList()
                }
            }
        }
    }

    /**
     * Returns all payments with the given status, ordered by creation time ascending.
     *
     * @param status The payment status to filter by.
     * @return List of matching payments.
     */
    fun findByStatus(status: PaymentStatus): List<Payment> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                stmt.executeQuery().use { rs ->
                    return rs.toPaymentList()
                }
            }
        }
    }

    /**
     * Returns all payments with paginated results.
     *
     * @param limit Maximum number of results.
     * @param offset Number of results to skip.
     * @return Pair of (payment list, total count).
     */
    fun findAll(limit: Int, offset: Int): Pair<List<Payment>, Long> {
        val payments = dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_ALL).use { stmt ->
                stmt.setInt(1, limit)
                stmt.setInt(2, offset)
                stmt.executeQuery().use { rs ->
                    rs.toPaymentList()
                }
            }
        }

        val total = dataSource.connection.use { conn ->
            conn.prepareStatement(COUNT_ALL).use { stmt ->
                stmt.executeQuery().use { rs ->
                    if (rs.next()) rs.getLong(1) else 0L
                }
            }
        }

        return Pair(payments, total)
    }

    /**
     * Returns all payments for a given auction ID matching any of the specified statuses.
     *
     * @param auctionId The auction UUID.
     * @param statuses The set of statuses to match.
     * @return List of matching payments.
     */
    fun findByAuctionIdAndStatuses(auctionId: UUID, statuses: List<PaymentStatus>): List<Payment> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_AUCTION_ID_AND_STATUSES).use { stmt ->
                stmt.setObject(1, auctionId)
                stmt.setArray(2, conn.createArrayOf("varchar", statuses.map { it.name }.toTypedArray()))
                stmt.executeQuery().use { rs ->
                    return rs.toPaymentList()
                }
            }
        }
    }

    /**
     * Returns payments that are overdue (PENDING with due_date before the given instant).
     *
     * @param now The current instant to compare against.
     * @return List of overdue payments.
     */
    fun findOverdue(now: Instant): List<Payment> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_OVERDUE).use { stmt ->
                stmt.setTimestamp(1, Timestamp.from(now))
                stmt.executeQuery().use { rs ->
                    return rs.toPaymentList()
                }
            }
        }
    }

    /**
     * Updates only the status of a payment.
     *
     * @param id The payment UUID.
     * @param status The new status.
     */
    fun updateStatus(id: UUID, status: PaymentStatus) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_STATUS).use { stmt ->
                stmt.setString(1, status.name)
                stmt.setTimestamp(2, Timestamp.from(Instant.now()))
                stmt.setObject(3, id)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Updated payment %s status to %s", id, status)
    }

    /**
     * Marks a payment as completed with payment method, PSP reference, and paid timestamp.
     *
     * @param id The payment UUID.
     * @param paymentMethod The payment method used.
     * @param pspReference The PSP (Adyen) reference.
     * @param paidAt The timestamp of payment confirmation.
     */
    fun markCompleted(id: UUID, paymentMethod: String, pspReference: String, paidAt: Instant) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_PAYMENT_COMPLETED).use { stmt ->
                stmt.setString(1, PaymentStatus.COMPLETED.name)
                stmt.setString(2, paymentMethod)
                stmt.setString(3, pspReference)
                stmt.setTimestamp(4, Timestamp.from(paidAt))
                stmt.setTimestamp(5, Timestamp.from(Instant.now()))
                stmt.setObject(6, id)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Marked payment %s as COMPLETED (psp=%s)", id, pspReference)
    }

    /**
     * Deletes a payment by its identifier. Used for cleanup or test purposes.
     *
     * @param id The payment UUID.
     */
    fun deleteById(id: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(DELETE_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeUpdate()
            }
        }
        LOG.debugf("Deleted payment %s", id)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toPaymentList(): List<Payment> {
        val payments = mutableListOf<Payment>()
        while (next()) {
            try {
                payments.add(toPayment())
            } catch (e: Exception) {
                LOG.warnf("Skipping unmappable payment row: %s", e.message)
            }
        }
        return payments
    }

    private fun ResultSet.toPayment(): Payment = Payment(
        id = getObject("id", UUID::class.java),
        buyerId = getObject("buyer_id", UUID::class.java),
        sellerId = getObject("seller_id", UUID::class.java) ?: UUID(0L, 0L),
        auctionId = getObject("auction_id", UUID::class.java),
        lotId = getObject("lot_id", UUID::class.java),
        hammerPrice = getBigDecimal("hammer_price") ?: BigDecimal.ZERO,
        buyerPremium = getBigDecimal("buyer_premium") ?: BigDecimal.ZERO,
        buyerPremiumRate = getBigDecimal("buyer_premium_rate") ?: BigDecimal.ZERO,
        taxAmount = getBigDecimal("vat_amount") ?: BigDecimal.ZERO,
        taxRate = getBigDecimal("vat_rate") ?: BigDecimal.ZERO,
        taxScheme = try { VatScheme.valueOf(getString("vat_scheme") ?: "TAXABLE") } catch (_: IllegalArgumentException) { VatScheme.TAXABLE },
        totalAmount = getBigDecimal("total_amount") ?: BigDecimal.ZERO,
        currency = getString("currency") ?: "USD",
        state = getString("country") ?: "",
        paymentMethod = getString("payment_method"),
        pspReference = getString("psp_reference"),
        status = try { PaymentStatus.valueOf(getString("status") ?: "PENDING") } catch (_: IllegalArgumentException) { PaymentStatus.PENDING },
        dueDate = getTimestamp("due_date")?.toInstant() ?: Instant.now(),
        paidAt = getTimestamp("paid_at")?.toInstant(),
        createdAt = getTimestamp("created_at")?.toInstant() ?: Instant.now(),
        lotTitle = getString("lot_title"),
        buyerName = getString("buyer_name"),
        sellerName = getString("seller_name")
    )
}
