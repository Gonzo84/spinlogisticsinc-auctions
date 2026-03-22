package eu.auctionplatform.payment.application.service

import eu.auctionplatform.payment.domain.model.VatRates
import eu.auctionplatform.payment.domain.model.VatScheme
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Result of a US sales tax calculation.
 *
 * @property taxAmount The calculated sales tax amount.
 * @property taxRate The effective sales tax rate applied (percentage, e.g. 6.25).
 * @property taxScheme The tax scheme that was determined.
 */
data class TaxCalculationResult(
  val taxAmount: BigDecimal,
  val taxRate: BigDecimal,
  val taxScheme: VatScheme,
)

/**
 * Calculates US sales tax for auction transactions based on buyer/seller
 * locations and exemption status.
 *
 * ## US sales tax rules for auctions (simplified)
 *
 * | Scenario                                  | Scheme               | Rate                |
 * |-------------------------------------------|----------------------|---------------------|
 * | Buyer is outside the US (non-US state)     | EXPORT               | 0%                  |
 * | Buyer has resale exemption certificate     | EXEMPT_RESALE        | 0%                  |
 * | Buyer has manufacturing exemption cert     | EXEMPT_MANUFACTURING | 0%                  |
 * | Buyer is government/nonprofit              | EXEMPT_GOVERNMENT    | 0%                  |
 * | Destination state has no sales tax         | NO_TAX_STATE         | 0%                  |
 * | Standard taxable sale                      | TAXABLE              | Destination state   |
 *
 * Sales tax is destination-based: the buyer's state determines the rate.
 *
 * TODO: Integrate Avalara AvaTax or TaxJar for production jurisdiction-level
 *       (county/city/district) rate calculation. Base state rates alone are
 *       insufficient for full US sales tax compliance.
 */
@ApplicationScoped
class VatCalculationService {

  companion object {
    private val LOG: Logger = Logger.getLogger(VatCalculationService::class.java)

    /** Percentage divisor for converting rate to multiplier. */
    private val HUNDRED: BigDecimal = BigDecimal.valueOf(100)
  }

  /**
   * Calculates the sales tax for a transaction based on US rules.
   *
   * The taxable base is the sum of [hammerPrice] and [buyerPremium].
   *
   * @param hammerPrice The final hammer (bid) price.
   * @param buyerPremium The buyer premium amount.
   * @param buyerState US state 2-letter code of the buyer (destination).
   * @param sellerState US state 2-letter code of the seller (origin).
   * @param exemptionCertificateId The buyer's exemption certificate ID
   *        (resale, manufacturing, or government), null if none.
   * @return [TaxCalculationResult] containing the tax amount, rate, and scheme.
   */
  fun calculateTax(
    hammerPrice: BigDecimal,
    buyerPremium: BigDecimal,
    buyerState: String,
    sellerState: String,
    exemptionCertificateId: String?,
  ): TaxCalculationResult {
    val taxableBase = hammerPrice.add(buyerPremium)
    val hasExemptionCert = !exemptionCertificateId.isNullOrBlank()
    val isNoTaxState = VatRates.isNoTaxState(buyerState)

    LOG.debugf(
      "Calculating sales tax: taxableBase=%s, buyerState=%s, sellerState=%s, " +
        "exemptionCertId=%s, isNoTaxState=%s",
      taxableBase, buyerState, sellerState,
      exemptionCertificateId?.take(4)?.plus("***"), isNoTaxState,
    )

    val isValidUsState = VatRates.isValidUsState(buyerState)

    return when {
      // Non-US buyer (buyerState is not a recognised US state code) — export, 0% tax
      !isValidUsState -> {
        LOG.infof(
          "Applying EXPORT: buyerState=%s is not a recognised US state, 0%% tax",
          buyerState,
        )
        TaxCalculationResult(
          taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
          taxRate = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
          taxScheme = VatScheme.EXPORT,
        )
      }

      // Buyer holds a valid exemption certificate (resale/manufacturing/gov)
      hasExemptionCert -> {
        LOG.infof(
          "Applying EXEMPT_RESALE: buyer has exemption certificate, " +
            "buyerState=%s, sellerState=%s",
          buyerState, sellerState,
        )
        TaxCalculationResult(
          taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
          taxRate = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
          taxScheme = VatScheme.EXEMPT_RESALE,
        )
      }

      // Destination state has no general sales tax
      isNoTaxState -> {
        LOG.infof(
          "Applying NO_TAX_STATE: destination state %s has no sales tax",
          buyerState,
        )
        TaxCalculationResult(
          taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
          taxRate = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
          taxScheme = VatScheme.NO_TAX_STATE,
        )
      }

      // Standard taxable sale — apply destination state base rate
      else -> {
        val rate = VatRates.getRate(buyerState.uppercase())
        val rateMultiplier = rate.divide(HUNDRED, 6, RoundingMode.HALF_UP)
        val taxAmount = taxableBase.multiply(rateMultiplier).setScale(2, RoundingMode.HALF_UP)
        LOG.infof(
          "Applying TAXABLE: standard sale to %s (rate=%s%%)",
          buyerState, rate,
        )
        TaxCalculationResult(
          taxAmount = taxAmount,
          taxRate = rate.setScale(4, RoundingMode.HALF_UP),
          taxScheme = VatScheme.TAXABLE,
        )
      }
    }
  }
}
