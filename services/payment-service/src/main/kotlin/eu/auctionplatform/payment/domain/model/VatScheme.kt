package eu.auctionplatform.payment.domain.model

/**
 * VAT scheme applied to a payment.
 *
 * - STANDARD: domestic sale, standard VAT rate of seller country.
 * - REVERSE_CHARGE: intra-EU B2B with valid buyer VAT ID; 0% VAT,
 *   buyer self-assesses.
 * - MARGIN_SCHEME: second-hand goods margin scheme (Art. 312-325 VAT Directive).
 * - OSS: One Stop Shop regime for intra-EU B2C sales; destination country rate.
 */
enum class VatScheme {
    STANDARD,
    REVERSE_CHARGE,
    MARGIN_SCHEME,
    OSS
}
