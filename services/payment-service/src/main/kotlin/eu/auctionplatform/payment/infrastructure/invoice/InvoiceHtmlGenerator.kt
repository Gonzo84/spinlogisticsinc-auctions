package eu.auctionplatform.payment.infrastructure.invoice

import eu.auctionplatform.payment.domain.model.Invoice
import eu.auctionplatform.payment.domain.model.InvoiceType
import eu.auctionplatform.payment.domain.model.Payment
import jakarta.enterprise.context.ApplicationScoped
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Generates server-rendered HTML invoices for completed payments.
 *
 * Produces a self-contained HTML document with inline CSS that includes:
 * - Invoice header with number, date, and type
 * - Buyer and seller information
 * - Line item breakdown (hammer price, buyer premium, sales tax)
 * - Total amount due
 *
 * The generated HTML can be served directly via the REST API or
 * converted to PDF using a headless browser or wkhtmltopdf.
 */
@ApplicationScoped
class InvoiceHtmlGenerator {

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy").withZone(ZoneOffset.UTC)

        private val USD_FORMAT: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)
    }

    /** Formats a BigDecimal as US dollar currency (e.g. "$1,234.56"). */
    private fun formatUsd(amount: BigDecimal): String = USD_FORMAT.format(amount)

    /**
     * Generates an HTML invoice for the given payment and invoice.
     *
     * @param invoice The invoice metadata (number, type, issue date).
     * @param payment The payment details (amounts, sales tax, buyer/seller IDs).
     * @param buyerName Display name of the buyer (or buyer ID if unavailable).
     * @param sellerName Display name of the seller (or seller ID if unavailable).
     * @param lotTitle Title of the purchased lot (or lot ID if unavailable).
     * @return A complete HTML document string.
     */
    fun generate(
        invoice: Invoice,
        payment: Payment,
        buyerName: String = payment.buyerId.toString(),
        sellerName: String = payment.sellerId.toString(),
        lotTitle: String = "Lot ${payment.lotId}"
    ): String {
        val invoiceTypeLabel = when (invoice.type) {
            InvoiceType.BUYER -> "Buyer Invoice"
            InvoiceType.SELLER -> "Seller Credit Note"
        }

        val issuedDate = DATE_FORMATTER.format(invoice.issuedAt)
        val paidDate = payment.paidAt?.let { DATE_FORMATTER.format(it) } ?: "Pending"

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>${invoice.invoiceNumber} - $invoiceTypeLabel</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #333; line-height: 1.6; padding: 40px; max-width: 800px; margin: 0 auto; }
                    .header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 40px; border-bottom: 3px solid #388E3C; padding-bottom: 20px; }
                    .header h1 { font-size: 28px; color: #388E3C; }
                    .header .invoice-meta { text-align: right; }
                    .header .invoice-meta p { margin-bottom: 4px; }
                    .header .invoice-number { font-size: 18px; font-weight: bold; color: #2E7D32; }
                    .parties { display: flex; justify-content: space-between; margin-bottom: 30px; }
                    .parties .party { width: 48%; }
                    .parties .party h3 { color: #388E3C; border-bottom: 1px solid #ddd; padding-bottom: 6px; margin-bottom: 10px; }
                    .parties .party p { margin-bottom: 4px; font-size: 14px; }
                    .lot-info { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 16px; margin-bottom: 30px; }
                    .lot-info h3 { color: #388E3C; margin-bottom: 8px; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }
                    th { background: #388E3C; color: white; padding: 12px; text-align: left; font-size: 14px; }
                    th:last-child { text-align: right; }
                    td { padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 14px; }
                    td:last-child { text-align: right; font-family: 'Courier New', monospace; }
                    tr:nth-child(even) { background: #f8fafc; }
                    .total-row td { font-weight: bold; font-size: 16px; border-top: 2px solid #388E3C; background: #E8F5E9; }
                    .tax-info { background: #fef3c7; border: 1px solid #fcd34d; border-radius: 6px; padding: 16px; margin-bottom: 30px; }
                    .tax-info h3 { color: #92400e; margin-bottom: 8px; }
                    .footer { text-align: center; color: #94a3b8; font-size: 12px; border-top: 1px solid #e2e8f0; padding-top: 20px; margin-top: 40px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div>
                        <h1>Spin Logistics Inc.</h1>
                        <p>$invoiceTypeLabel</p>
                    </div>
                    <div class="invoice-meta">
                        <p class="invoice-number">${invoice.invoiceNumber}</p>
                        <p>Date: $issuedDate</p>
                        <p>Payment: ${payment.id}</p>
                        <p>Status: ${payment.status.name}</p>
                    </div>
                </div>

                <div class="parties">
                    <div class="party">
                        <h3>Buyer</h3>
                        <p><strong>$buyerName</strong></p>
                        <p>ID: ${payment.buyerId}</p>
                        <p>State: ${payment.state}</p>
                    </div>
                    <div class="party">
                        <h3>Seller</h3>
                        <p><strong>$sellerName</strong></p>
                        <p>ID: ${payment.sellerId}</p>
                    </div>
                </div>

                <div class="lot-info">
                    <h3>Lot Details</h3>
                    <p><strong>$lotTitle</strong></p>
                    <p>Lot ID: ${payment.lotId}</p>
                    <p>Auction ID: ${payment.auctionId}</p>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>Description</th>
                            <th>Amount (USD)</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Hammer Price</td>
                            <td>${formatUsd(payment.hammerPrice)}</td>
                        </tr>
                        <tr>
                            <td>Buyer Premium (${payment.buyerPremiumRate.multiply(BigDecimal(100)).stripTrailingZeros()}%)</td>
                            <td>${formatUsd(payment.buyerPremium)}</td>
                        </tr>
                        <tr>
                            <td>Sales Tax (${payment.taxRate.multiply(BigDecimal(100)).stripTrailingZeros()}%)</td>
                            <td>${formatUsd(payment.taxAmount)}</td>
                        </tr>
                        <tr class="total-row">
                            <td>Total Amount Due</td>
                            <td>${formatUsd(payment.totalAmount)}</td>
                        </tr>
                    </tbody>
                </table>

                <div class="tax-info">
                    <h3>Sales Tax</h3>
                    <p><strong>Tax Rate:</strong> ${payment.taxRate.multiply(BigDecimal(100)).stripTrailingZeros()}%</p>
                    <p><strong>Tax Amount:</strong> ${formatUsd(payment.taxAmount)}</p>
                </div>

                <p><strong>Payment Method:</strong> ${payment.paymentMethod ?: "N/A"}</p>
                <p><strong>PSP Reference:</strong> ${payment.pspReference ?: "N/A"}</p>
                <p><strong>Date Paid:</strong> $paidDate</p>
                <p><strong>Due Date:</strong> ${DATE_FORMATTER.format(payment.dueDate)}</p>

                <div class="footer">
                    <p>Spin Logistics Inc. &mdash; This is a computer-generated invoice.</p>
                    <p>Invoice ${invoice.invoiceNumber} issued on $issuedDate</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
