package eu.auctionplatform.payment.domain.model

import java.math.BigDecimal

/**
 * Base state sales tax rates for all US states and DC.
 *
 * Rates are kept as a static lookup table and reflect the base state rates
 * as of 2025. These are BASE rates only — local jurisdictions (county, city,
 * special district) add additional tax on top.
 *
 * States with no general sales tax (AK, DE, MT, NH, OR) return 0.00.
 * Note: Alaska has no state sales tax but allows local jurisdictions to
 * levy their own sales tax.
 *
 * TODO: For production use with jurisdiction-level (county/city/district)
 *       rates, integrate Avalara AvaTax or TaxJar API. Static base rates
 *       are insufficient for accurate US sales tax compliance.
 *
 * Source: Tax Foundation, state revenue department publications.
 */
object VatRates {

  private val rates = mapOf(
    "AL" to 4.00,
    "AK" to 0.00,   // No state sales tax (local only)
    "AZ" to 5.60,
    "AR" to 6.50,
    "CA" to 7.25,
    "CO" to 2.90,
    "CT" to 6.35,
    "DE" to 0.00,   // No sales tax
    "FL" to 6.00,
    "GA" to 4.00,
    "HI" to 4.00,
    "ID" to 6.00,
    "IL" to 6.25,
    "IN" to 7.00,
    "IA" to 6.00,
    "KS" to 6.50,
    "KY" to 6.00,
    "LA" to 4.45,
    "ME" to 5.50,
    "MD" to 6.00,
    "MA" to 6.25,
    "MI" to 6.00,
    "MN" to 6.875,
    "MS" to 7.00,
    "MO" to 4.225,
    "MT" to 0.00,   // No sales tax
    "NE" to 5.50,
    "NV" to 6.85,
    "NH" to 0.00,   // No sales tax
    "NJ" to 6.625,
    "NM" to 4.875,
    "NY" to 4.00,
    "NC" to 4.75,
    "ND" to 5.00,
    "OH" to 5.75,
    "OK" to 4.50,
    "OR" to 0.00,   // No sales tax
    "PA" to 6.00,
    "RI" to 7.00,
    "SC" to 6.00,
    "SD" to 4.20,
    "TN" to 7.00,
    "TX" to 6.25,
    "UT" to 6.10,
    "VT" to 6.00,
    "VA" to 5.30,
    "WA" to 6.50,
    "WV" to 6.00,
    "WI" to 5.00,
    "WY" to 4.00,
    "DC" to 6.00,
  )

  /** State codes with no general sales tax. */
  private val noTaxStates = setOf("AK", "DE", "MT", "NH", "OR")

  /**
   * Returns the base state sales tax rate for the given US state.
   *
   * @param stateCode US state 2-letter code (e.g. "NY", "CA").
   * @return The base state sales tax rate as a [BigDecimal] percentage
   *         (e.g. 6.25 for 6.25%). Divide by 100 before multiplying.
   * @throws IllegalArgumentException if the state code is not recognised.
   */
  fun getRate(stateCode: String): BigDecimal {
    val code = stateCode.uppercase()
    val rate = rates[code]
    if (rate == null) {
      // Return 0% for unknown codes (non-US countries, territories, etc.)
      // rather than crashing the checkout flow with a 500.
      return BigDecimal.ZERO
    }
    return BigDecimal.valueOf(rate)
  }

  /**
   * Returns true if the given state has no general sales tax.
   *
   * @param stateCode US state 2-letter code.
   */
  fun isNoTaxState(stateCode: String): Boolean =
    noTaxStates.contains(stateCode.uppercase())

  /**
   * Returns true if the given code is a recognised US state (or DC).
   *
   * @param stateCode 2-letter code to check.
   */
  fun isValidUsState(stateCode: String): Boolean =
    rates.containsKey(stateCode.uppercase())
}
