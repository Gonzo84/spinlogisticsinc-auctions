package eu.auctionplatform.payment.infrastructure.persistence.repository

import eu.auctionplatform.payment.domain.model.Invoice
import eu.auctionplatform.payment.domain.model.InvoiceType
import io.agroal.api.AgroalDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Repository for the [Invoice] entity.
 *
 * Uses direct JDBC via an [AgroalDataSource] (the "SandBox" pattern) for
 * full SQL control over invoice CRUD operations.
 */
@ApplicationScoped
class InvoiceRepository @Inject constructor(
    private val dataSource: AgroalDataSource
) {

    private val logger = LoggerFactory.getLogger(InvoiceRepository::class.java)

    companion object {
        private const val SELECT_COLUMNS = """
            id, payment_id, invoice_number, type, pdf_url, issued_at
        """

        private const val INSERT_INVOICE = """
            INSERT INTO app.invoices
                (id, payment_id, invoice_number, type, pdf_url, issued_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """

        private const val SELECT_BY_ID = """
            SELECT $SELECT_COLUMNS FROM app.invoices WHERE id = ?
        """

        private const val SELECT_BY_PAYMENT_ID = """
            SELECT $SELECT_COLUMNS FROM app.invoices
            WHERE payment_id = ? ORDER BY issued_at ASC
        """

        private const val SELECT_BY_INVOICE_NUMBER = """
            SELECT $SELECT_COLUMNS FROM app.invoices
            WHERE invoice_number = ?
        """

        private const val SELECT_BY_PAYMENT_ID_AND_TYPE = """
            SELECT $SELECT_COLUMNS FROM app.invoices
            WHERE payment_id = ? AND type = ?
        """

        private const val SELECT_ALL = """
            SELECT $SELECT_COLUMNS FROM app.invoices
            ORDER BY issued_at DESC LIMIT ? OFFSET ?
        """

        private const val COUNT_ALL = """
            SELECT COUNT(*) FROM app.invoices
        """

        private const val UPDATE_PDF_URL = """
            UPDATE app.invoices SET pdf_url = ? WHERE id = ?
        """

        private const val DELETE_BY_ID = """
            DELETE FROM app.invoices WHERE id = ?
        """
    }

    /**
     * Persists a new invoice record.
     *
     * @param invoice The invoice to save.
     */
    fun save(invoice: Invoice) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(INSERT_INVOICE).use { stmt ->
                stmt.setObject(1, invoice.id)
                stmt.setObject(2, invoice.paymentId)
                stmt.setString(3, invoice.invoiceNumber)
                stmt.setString(4, invoice.type.name)
                stmt.setString(5, invoice.pdfUrl)
                stmt.setTimestamp(6, Timestamp.from(invoice.issuedAt))
                stmt.executeUpdate()
            }
        }
        logger.debug("Saved invoice {} (number={}, type={})",
            invoice.id, invoice.invoiceNumber, invoice.type)
    }

    /**
     * Finds an invoice by its unique identifier.
     *
     * @param id The invoice UUID.
     * @return The invoice, or null if not found.
     */
    fun findById(id: UUID): Invoice? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toInvoice() else null
                }
            }
        }
    }

    /**
     * Returns all invoices for a given payment, ordered by issued date ascending.
     *
     * @param paymentId The payment UUID.
     * @return List of invoices (typically buyer + seller).
     */
    fun findByPaymentId(paymentId: UUID): List<Invoice> {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_PAYMENT_ID).use { stmt ->
                stmt.setObject(1, paymentId)
                stmt.executeQuery().use { rs ->
                    return rs.toInvoiceList()
                }
            }
        }
    }

    /**
     * Finds an invoice by its human-readable invoice number.
     *
     * @param invoiceNumber The invoice number (e.g. "INV-2026-000123").
     * @return The invoice, or null if not found.
     */
    fun findByInvoiceNumber(invoiceNumber: String): Invoice? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_INVOICE_NUMBER).use { stmt ->
                stmt.setString(1, invoiceNumber)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toInvoice() else null
                }
            }
        }
    }

    /**
     * Finds an invoice for a specific payment and type (BUYER or SELLER).
     *
     * @param paymentId The payment UUID.
     * @param type The invoice type.
     * @return The invoice, or null if not found.
     */
    fun findByPaymentIdAndType(paymentId: UUID, type: InvoiceType): Invoice? {
        dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_BY_PAYMENT_ID_AND_TYPE).use { stmt ->
                stmt.setObject(1, paymentId)
                stmt.setString(2, type.name)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.toInvoice() else null
                }
            }
        }
    }

    /**
     * Returns all invoices with pagination.
     *
     * @param limit Maximum number of results.
     * @param offset Number of results to skip.
     * @return Pair of (invoice list, total count).
     */
    fun findAll(limit: Int, offset: Int): Pair<List<Invoice>, Long> {
        val invoices = dataSource.connection.use { conn ->
            conn.prepareStatement(SELECT_ALL).use { stmt ->
                stmt.setInt(1, limit)
                stmt.setInt(2, offset)
                stmt.executeQuery().use { rs ->
                    rs.toInvoiceList()
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

        return Pair(invoices, total)
    }

    /**
     * Updates the PDF URL for an invoice after generation.
     *
     * @param id The invoice UUID.
     * @param pdfUrl The URL to the generated PDF.
     */
    fun updatePdfUrl(id: UUID, pdfUrl: String) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(UPDATE_PDF_URL).use { stmt ->
                stmt.setString(1, pdfUrl)
                stmt.setObject(2, id)
                stmt.executeUpdate()
            }
        }
        logger.debug("Updated PDF URL for invoice {}", id)
    }

    /**
     * Deletes an invoice by its identifier.
     *
     * @param id The invoice UUID.
     */
    fun deleteById(id: UUID) {
        dataSource.connection.use { conn ->
            conn.prepareStatement(DELETE_BY_ID).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeUpdate()
            }
        }
        logger.debug("Deleted invoice {}", id)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun ResultSet.toInvoiceList(): List<Invoice> {
        val invoices = mutableListOf<Invoice>()
        while (next()) {
            invoices.add(toInvoice())
        }
        return invoices
    }

    private fun ResultSet.toInvoice(): Invoice = Invoice(
        id = getObject("id", UUID::class.java),
        paymentId = getObject("payment_id", UUID::class.java),
        invoiceNumber = getString("invoice_number"),
        type = InvoiceType.valueOf(getString("type")),
        pdfUrl = getString("pdf_url"),
        issuedAt = getTimestamp("issued_at").toInstant()
    )
}
