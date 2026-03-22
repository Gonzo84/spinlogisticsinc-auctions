package eu.auctionplatform.payment.domain.model

/**
 * US sales tax scheme applied to a payment.
 *
 * - TAXABLE: Standard taxable sale; destination state base rate applies.
 * - EXEMPT_RESALE: B2B resale certificate exemption; buyer holds a valid
 *   resale/exemption certificate, 0% sales tax.
 * - EXEMPT_MANUFACTURING: Manufacturing equipment exemption; varies by state,
 *   buyer holds a manufacturing exemption certificate.
 * - EXEMPT_GOVERNMENT: Government or nonprofit exemption; buyer is a
 *   government entity or qualified nonprofit organization.
 * - NO_TAX_STATE: Destination state has no general sales tax
 *   (AK local only, DE, MT, NH, OR).
 * - EXPORT: Non-US buyer; no US sales tax applies (0%).
 */
enum class VatScheme {
  TAXABLE,
  EXEMPT_RESALE,
  EXEMPT_MANUFACTURING,
  EXEMPT_GOVERNMENT,
  NO_TAX_STATE,
  EXPORT,
}
