package eu.auctionplatform.catalog.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Immutable domain model representing an auction event.
 *
 * An auction event is a time-bounded session that groups multiple lots
 * under a single brand. Buyers browse and bid on lots within the context
 * of an auction event.
 *
 * The [buyerPremiumPercent] defines the percentage surcharge applied to
 * the winning bid amount, which constitutes the platform's revenue from
 * the transaction. The standard EU B2B rate is 18%.
 *
 * @property id                   Unique identifier (UUIDv7).
 * @property title                Descriptive title (e.g. "Weekly Construction Auction - NL").
 * @property brand                Brand/tenant code (e.g. "troostwijk", "industrial-auctions").
 * @property startDate            Scheduled start time (UTC).
 * @property endDate              Scheduled end time (UTC).
 * @property country              ISO 3166-1 alpha-2 country code for the auction's jurisdiction.
 * @property status               Current lifecycle status.
 * @property buyerPremiumPercent  Buyer premium as a percentage (e.g. 18 = 18%).
 * @property totalLots            Number of lots assigned to this auction event.
 */
data class AuctionEvent(
    val id: UUID,
    val title: String,
    val brand: String,
    val startDate: Instant,
    val endDate: Instant,
    val country: String,
    val status: AuctionEventStatus = AuctionEventStatus.DRAFT,
    val buyerPremiumPercent: BigDecimal = BigDecimal("18"),
    val totalLots: Int = 0
) {

    /** Returns `true` if additional lots can be assigned to this event. */
    fun acceptsLots(): Boolean = status.acceptsLots()

    /** Returns `true` if the auction event is currently live. */
    fun isLive(): Boolean = status.isLive()

    /** Returns a copy with the given [newStatus]. */
    fun withStatus(newStatus: AuctionEventStatus): AuctionEvent =
        copy(status = newStatus)
}
