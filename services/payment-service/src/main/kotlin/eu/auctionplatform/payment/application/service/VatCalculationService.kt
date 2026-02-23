package eu.auctionplatform.payment.application.service

import eu.auctionplatform.payment.domain.model.VatRates
import eu.auctionplatform.payment.domain.model.VatScheme
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Result of a VAT calculation.
 *
 * @property vatAmount The calculated VAT amount.
 * @property vatRate The effective VAT rate applied.
 * @property vatScheme The VAT scheme that was determined.
 */
data class VatCalculationResult(
    val vatAmount: BigDecimal,
    val vatRate: BigDecimal,
    val vatScheme: VatScheme
)

/**
 * Calculates VAT for EU auction transactions based on the buyer/seller
 * locations, account types, and applicable VAT regime.
 *
 * ## EU VAT rules for auctions (simplified)
 *
 * | Scenario                          | Scheme          | Rate              |
 * |-----------------------------------|-----------------|-------------------|
 * | Domestic B2B                      | STANDARD        | Seller country    |
 * | Intra-EU B2B (valid VAT ID)       | REVERSE_CHARGE  | 0%                |
 * | Domestic B2C                      | STANDARD        | Seller country    |
 * | Intra-EU B2C                      | OSS             | Destination (buyer) country |
 *
 * The margin scheme (Art. 312-325 VAT Directive) for second-hand goods,
 * art, and antiques is supported but must be explicitly requested by the
 * seller.
 */
@ApplicationScoped
class VatCalculationService {

    private val logger = LoggerFactory.getLogger(VatCalculationService::class.java)

    companion object {
        /** Account type constants. */
        const val ACCOUNT_TYPE_BUSINESS = "BUSINESS"
        const val ACCOUNT_TYPE_CONSUMER = "CONSUMER"
    }

    /**
     * Calculates the VAT for a transaction based on EU rules.
     *
     * The taxable base is the sum of [hammerPrice] and [buyerPremium].
     *
     * @param hammerPrice The final hammer (bid) price.
     * @param buyerPremium The buyer premium amount.
     * @param buyerCountry ISO 3166-1 alpha-2 country code of the buyer.
     * @param sellerCountry ISO 3166-1 alpha-2 country code of the seller.
     * @param buyerType Account type of the buyer ("BUSINESS" or "CONSUMER").
     * @param sellerType Account type of the seller ("BUSINESS" or "CONSUMER").
     * @param buyerVatId The buyer's VAT identification number (null if not provided).
     * @return [VatCalculationResult] containing the VAT amount, rate, and scheme.
     */
    fun calculateVat(
        hammerPrice: BigDecimal,
        buyerPremium: BigDecimal,
        buyerCountry: String,
        sellerCountry: String,
        buyerType: String,
        sellerType: String,
        buyerVatId: String?
    ): VatCalculationResult {
        val taxableBase = hammerPrice.add(buyerPremium)
        val isDomestic = buyerCountry.equals(sellerCountry, ignoreCase = true)
        val isBuyerBusiness = buyerType.equals(ACCOUNT_TYPE_BUSINESS, ignoreCase = true)
        val hasValidVatId = !buyerVatId.isNullOrBlank()

        logger.debug(
            "Calculating VAT: taxableBase={}, buyerCountry={}, sellerCountry={}, " +
                "buyerType={}, sellerType={}, buyerVatId={}, isDomestic={}",
            taxableBase, buyerCountry, sellerCountry, buyerType, sellerType,
            buyerVatId?.take(4)?.plus("***"), isDomestic
        )

        return when {
            // Intra-EU B2B with valid VAT ID -> Reverse charge
            !isDomestic && isBuyerBusiness && hasValidVatId -> {
                logger.info(
                    "Applying REVERSE_CHARGE: intra-EU B2B from {} to {} (VAT ID provided)",
                    sellerCountry, buyerCountry
                )
                VatCalculationResult(
                    vatAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    vatRate = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    vatScheme = VatScheme.REVERSE_CHARGE
                )
            }

            // Intra-EU B2C -> OSS regime (destination country rate)
            !isDomestic && !isBuyerBusiness -> {
                val rate = VatRates.getRate(buyerCountry.uppercase())
                val vatAmount = taxableBase.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                logger.info(
                    "Applying OSS: intra-EU B2C from {} to {} (rate={})",
                    sellerCountry, buyerCountry, rate
                )
                VatCalculationResult(
                    vatAmount = vatAmount,
                    vatRate = rate.setScale(4, RoundingMode.HALF_UP),
                    vatScheme = VatScheme.OSS
                )
            }

            // Domestic B2B -> Standard VAT rate of seller country
            isDomestic && isBuyerBusiness -> {
                val rate = VatRates.getRate(sellerCountry.uppercase())
                val vatAmount = taxableBase.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                logger.info(
                    "Applying STANDARD: domestic B2B in {} (rate={})",
                    sellerCountry, rate
                )
                VatCalculationResult(
                    vatAmount = vatAmount,
                    vatRate = rate.setScale(4, RoundingMode.HALF_UP),
                    vatScheme = VatScheme.STANDARD
                )
            }

            // Domestic B2C -> Standard VAT rate of seller country
            isDomestic && !isBuyerBusiness -> {
                val rate = VatRates.getRate(sellerCountry.uppercase())
                val vatAmount = taxableBase.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                logger.info(
                    "Applying STANDARD: domestic B2C in {} (rate={})",
                    sellerCountry, rate
                )
                VatCalculationResult(
                    vatAmount = vatAmount,
                    vatRate = rate.setScale(4, RoundingMode.HALF_UP),
                    vatScheme = VatScheme.STANDARD
                )
            }

            // Intra-EU B2B without valid VAT ID -> Standard rate of seller country
            // (buyer should provide VAT ID for reverse charge; without it, standard applies)
            else -> {
                val rate = VatRates.getRate(sellerCountry.uppercase())
                val vatAmount = taxableBase.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                logger.info(
                    "Applying STANDARD: intra-EU B2B without VAT ID, from {} to {} (rate={})",
                    sellerCountry, buyerCountry, rate
                )
                VatCalculationResult(
                    vatAmount = vatAmount,
                    vatRate = rate.setScale(4, RoundingMode.HALF_UP),
                    vatScheme = VatScheme.STANDARD
                )
            }
        }
    }
}
