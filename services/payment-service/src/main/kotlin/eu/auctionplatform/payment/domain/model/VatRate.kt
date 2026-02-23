package eu.auctionplatform.payment.domain.model

import java.math.BigDecimal

/**
 * Standard VAT rates for all EU member states.
 *
 * Rates are kept as a static lookup table and reflect the standard rates
 * as of 2025. Reduced rates for specific goods categories (e.g. art,
 * antiques) are not modelled here and would require a more granular
 * lookup by product category.
 *
 * Source: European Commission VAT rates database.
 */
object VatRates {

    private val rates = mapOf(
        "AT" to 0.20,   // Austria
        "BE" to 0.21,   // Belgium
        "BG" to 0.20,   // Bulgaria
        "HR" to 0.25,   // Croatia
        "CY" to 0.19,   // Cyprus
        "CZ" to 0.21,   // Czech Republic
        "DK" to 0.25,   // Denmark
        "EE" to 0.22,   // Estonia
        "FI" to 0.255,  // Finland
        "FR" to 0.20,   // France
        "DE" to 0.19,   // Germany
        "GR" to 0.24,   // Greece
        "HU" to 0.27,   // Hungary
        "IE" to 0.23,   // Ireland
        "IT" to 0.22,   // Italy
        "LV" to 0.21,   // Latvia
        "LT" to 0.21,   // Lithuania
        "LU" to 0.17,   // Luxembourg
        "MT" to 0.18,   // Malta
        "NL" to 0.21,   // Netherlands
        "PL" to 0.23,   // Poland
        "PT" to 0.23,   // Portugal
        "RO" to 0.19,   // Romania
        "SK" to 0.23,   // Slovakia
        "SI" to 0.22,   // Slovenia
        "ES" to 0.21,   // Spain
        "SE" to 0.25    // Sweden
    )

    /**
     * Returns the standard VAT rate for the given EU member state.
     *
     * @param countryCode ISO 3166-1 alpha-2 country code (e.g. "DE", "NL").
     * @return The standard VAT rate as a [BigDecimal] (e.g. 0.21 for 21%).
     * @throws IllegalArgumentException if the country code is not a recognised EU member state.
     */
    fun getRate(countryCode: String): BigDecimal {
        val rate = rates[countryCode.uppercase()]
            ?: throw IllegalArgumentException(
                "Unknown EU country code: '$countryCode'. " +
                    "Supported codes: ${rates.keys.sorted().joinToString()}"
            )
        return BigDecimal.valueOf(rate)
    }

    /**
     * Returns true if the given country code is a recognised EU member state.
     *
     * @param countryCode ISO 3166-1 alpha-2 country code.
     */
    fun isEuCountry(countryCode: String): Boolean =
        rates.containsKey(countryCode.uppercase())
}
