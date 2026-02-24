package eu.auctionplatform.commons.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Immutable value object representing a monetary amount in a specific currency.
 *
 * Arithmetic is only permitted between [Money] instances of the **same** currency;
 * mixing currencies throws [IllegalArgumentException].
 *
 * The [amount] is always stored with the number of fraction digits defined by the
 * currency (e.g. 2 for EUR, 0 for JPY).
 */
class Money private constructor(
    val amount: BigDecimal,
    val currency: Currency
) : ValueObject(), Comparable<Money> {

    init {
        require(amount.scale() >= 0) { "Amount scale must be non-negative" }
    }

    // ---------------------------------------------------------------------------
    // Equality (delegated to ValueObject)
    // ---------------------------------------------------------------------------

    override fun equalityComponents(): List<Any?> = listOf(amount, currency)

    // ---------------------------------------------------------------------------
    // Arithmetic
    // ---------------------------------------------------------------------------

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount.add(other.amount), currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(amount.subtract(other.amount), currency)
    }

    operator fun times(multiplier: BigDecimal): Money =
        Money(amount.multiply(multiplier).setScale(currency.defaultFractionDigits, RoundingMode.HALF_UP), currency)

    operator fun times(multiplier: Int): Money = times(BigDecimal(multiplier))

    operator fun times(multiplier: Long): Money = times(BigDecimal(multiplier))

    operator fun div(divisor: BigDecimal): Money {
        require(divisor.compareTo(BigDecimal.ZERO) != 0) { "Cannot divide by zero" }
        return Money(amount.divide(divisor, currency.defaultFractionDigits, RoundingMode.HALF_UP), currency)
    }

    operator fun div(divisor: Int): Money = div(BigDecimal(divisor))

    fun negate(): Money = Money(amount.negate(), currency)

    fun abs(): Money = Money(amount.abs(), currency)

    // ---------------------------------------------------------------------------
    // Comparison
    // ---------------------------------------------------------------------------

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return amount.compareTo(other.amount)
    }

    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0

    fun isPositive(): Boolean = amount > BigDecimal.ZERO

    fun isNegative(): Boolean = amount < BigDecimal.ZERO

    // ---------------------------------------------------------------------------
    // Formatting
    // ---------------------------------------------------------------------------

    /**
     * Formats the money value using the given [locale] (defaults to [Locale.GERMANY]
     * which uses comma as decimal separator and dot as grouping separator, matching
     * common EU conventions).
     */
    fun format(locale: Locale = Locale.GERMANY): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = currency
        formatter.minimumFractionDigits = currency.defaultFractionDigits
        formatter.maximumFractionDigits = currency.defaultFractionDigits
        return formatter.format(amount)
    }

    override fun toString(): String = "${amount.toPlainString()} ${currency.currencyCode}"

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "Currency mismatch: cannot combine ${currency.currencyCode} with ${other.currency.currencyCode}"
        }
    }

    // ---------------------------------------------------------------------------
    // Factory
    // ---------------------------------------------------------------------------

    companion object {

        /** Default currency for the EU auction platform. */
        val DEFAULT_CURRENCY: Currency = Currency.getInstance("EUR")

        /**
         * Creates a [Money] instance, scaling the [amount] to the currency's
         * default fraction digits.
         */
        fun of(amount: BigDecimal, currency: Currency = DEFAULT_CURRENCY): Money =
            Money(amount.setScale(currency.defaultFractionDigits, RoundingMode.HALF_UP), currency)

        fun of(amount: Double, currency: Currency = DEFAULT_CURRENCY): Money =
            of(BigDecimal.valueOf(amount), currency)

        fun of(amount: Long, currency: Currency = DEFAULT_CURRENCY): Money =
            of(BigDecimal.valueOf(amount), currency)

        fun of(amount: Int, currency: Currency = DEFAULT_CURRENCY): Money =
            of(BigDecimal(amount), currency)

        fun of(amount: String, currency: Currency = DEFAULT_CURRENCY): Money =
            of(BigDecimal(amount), currency)

        /** Returns a zero-value [Money] in the given [currency]. */
        fun zero(currency: Currency = DEFAULT_CURRENCY): Money =
            of(BigDecimal.ZERO, currency)
    }
}
