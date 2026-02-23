package eu.auctionplatform.auction.domain.model

import eu.auctionplatform.commons.domain.Money
import java.math.BigDecimal
import java.time.Duration
import java.util.Currency
import java.util.TreeMap

/**
 * Business constants for auction bid processing.
 *
 * All monetary values default to EUR. The minimum increment rules follow a tiered
 * structure common in European B2B industrial auctions, ensuring bid increments
 * scale proportionally to the lot's current price level.
 *
 * These values are compiled into the domain model (rather than externalised to
 * configuration) because they represent core business invariants that require
 * version-controlled changes and domain-event replay consistency.
 */
object AuctionConstants {

    // -----------------------------------------------------------------------
    // Anti-sniping configuration
    // -----------------------------------------------------------------------

    /**
     * Duration before the auction's scheduled end time that constitutes the
     * anti-sniping window. Any bid placed within this window triggers an
     * automatic time extension.
     */
    val ANTI_SNIPING_WINDOW: Duration = Duration.ofMinutes(2)

    /**
     * Duration by which the auction end time is extended when a bid is placed
     * inside the [ANTI_SNIPING_WINDOW]. The extension is calculated from the
     * bid timestamp (not from the original end time), ensuring that rapid
     * successive bids each push the deadline forward.
     */
    val ANTI_SNIPING_EXTENSION: Duration = Duration.ofMinutes(2)

    /**
     * Maximum number of anti-sniping extensions allowed per auction. This is a
     * safety cap to prevent indefinite auction prolongation.
     */
    const val MAX_EXTENSIONS: Int = 100

    // -----------------------------------------------------------------------
    // Deposit thresholds
    // -----------------------------------------------------------------------

    private val EUR: Currency = Currency.getInstance("EUR")

    /**
     * If an auction's current high bid reaches or exceeds this threshold,
     * all subsequent bidders must have a deposit on file before they can
     * place a bid.
     */
    val DEPOSIT_THRESHOLD: Money = Money.of(BigDecimal("4000"), EUR)

    /**
     * The fixed deposit amount required from bidders when the [DEPOSIT_THRESHOLD]
     * is active.
     */
    val DEPOSIT_AMOUNT: Money = Money.of(BigDecimal("200"), EUR)

    // -----------------------------------------------------------------------
    // Minimum bid increment rules
    // -----------------------------------------------------------------------

    /**
     * Sorted map of price-range lower bounds (inclusive) to their corresponding
     * minimum bid increment. The key is the lower bound of the price range in
     * whole EUR. Lookup is performed via [TreeMap.floorEntry] to find the
     * applicable tier for any given bid amount.
     *
     * | Price range (EUR) | Minimum increment (EUR) |
     * |-------------------|-------------------------|
     * |        0 –     99 |                       1 |
     * |      100 –    499 |                       5 |
     * |      500 –    999 |                      10 |
     * |    1 000 –  4 999 |                      25 |
     * |    5 000 –  9 999 |                      50 |
     * |   10 000 – 49 999 |                     100 |
     * |   50 000 – 99 999 |                     250 |
     * |  100 000+         |                     500 |
     */
    val MIN_INCREMENT_RULES: TreeMap<BigDecimal, Money> = TreeMap<BigDecimal, Money>().apply {
        put(BigDecimal("0"), Money.of(BigDecimal("1"), EUR))
        put(BigDecimal("100"), Money.of(BigDecimal("5"), EUR))
        put(BigDecimal("500"), Money.of(BigDecimal("10"), EUR))
        put(BigDecimal("1000"), Money.of(BigDecimal("25"), EUR))
        put(BigDecimal("5000"), Money.of(BigDecimal("50"), EUR))
        put(BigDecimal("10000"), Money.of(BigDecimal("100"), EUR))
        put(BigDecimal("50000"), Money.of(BigDecimal("250"), EUR))
        put(BigDecimal("100000"), Money.of(BigDecimal("500"), EUR))
    }

    /**
     * Returns the minimum bid increment for the given [currentBid] amount.
     *
     * The increment is determined by locating the highest tier whose lower bound
     * does not exceed the current bid's amount. For example, a current bid of
     * EUR 750 falls in the 500–999 tier and requires a minimum increment of EUR 10.
     *
     * @param currentBid The current highest bid amount in the auction.
     * @return The minimum increment that must be added to [currentBid] for a new
     *         bid to be valid.
     * @throws IllegalStateException if the increment rules are misconfigured and
     *         no matching tier is found.
     */
    fun minimumIncrement(currentBid: Money): Money {
        val entry = MIN_INCREMENT_RULES.floorEntry(currentBid.amount)
            ?: throw IllegalStateException(
                "No minimum increment rule found for bid amount ${currentBid.amount}. " +
                    "This indicates a misconfiguration in MIN_INCREMENT_RULES."
            )
        return entry.value
    }
}
