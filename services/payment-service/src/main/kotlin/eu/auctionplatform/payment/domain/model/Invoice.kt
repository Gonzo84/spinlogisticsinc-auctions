package eu.auctionplatform.payment.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Type of invoice issued for a transaction.
 */
enum class InvoiceType {
    /** Invoice issued to the buyer for their purchase. */
    BUYER,
    /** Self-billing invoice / credit note issued to the seller for the payout. */
    SELLER
}

/**
 * Represents a tax invoice generated for a payment.
 *
 * Both buyer and seller invoices are generated for each completed payment.
 * The buyer invoice details the purchase price, premium, and VAT. The seller
 * invoice (self-billing) details the hammer price minus commission.
 *
 * @property id Unique invoice identifier.
 * @property paymentId The payment this invoice relates to.
 * @property invoiceNumber Sequential, human-readable invoice number (e.g. "INV-2026-000123").
 * @property type Whether this is a BUYER or SELLER invoice.
 * @property pdfUrl URL to the generated PDF document (null until generated).
 * @property issuedAt Timestamp when the invoice was issued.
 */
data class Invoice(
    val id: UUID,
    val paymentId: UUID,
    val invoiceNumber: String,
    val type: InvoiceType,
    val pdfUrl: String?,
    val issuedAt: Instant
)
