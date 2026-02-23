package eu.auctionplatform.catalog.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Immutable domain model representing a lot in the auction catalog.
 *
 * A lot is a single item or group of items offered for sale in an auction.
 * Lots are created by sellers, reviewed by platform staff, and assigned
 * to auction events for bidding.
 *
 * The [specifications] field stores category-specific attributes as a
 * flexible key-value map (persisted as JSONB in PostgreSQL), enabling
 * rich filtering and search across heterogeneous lot types.
 *
 * @property id               Unique identifier (UUIDv7).
 * @property sellerId         The user who created this lot.
 * @property brand            Brand/tenant code this lot belongs to (e.g. "troostwijk").
 * @property title            Short descriptive title for the lot.
 * @property description      Full description with condition, provenance, etc.
 * @property categoryId       Reference to the category this lot belongs to.
 * @property specifications   Category-specific key-value attributes (JSONB).
 * @property locationLat      GPS latitude of the lot's physical location.
 * @property locationLng      GPS longitude of the lot's physical location.
 * @property locationAddress  Street address where the lot can be inspected/picked up.
 * @property locationCountry  ISO 3166-1 alpha-2 country code.
 * @property locationCity     City where the lot is located.
 * @property reservePrice     Minimum price the seller is willing to accept (optional).
 * @property startingBid      The opening bid amount for the auction.
 * @property auctionId        The auction event this lot is assigned to (null if unassigned).
 * @property status           Current lifecycle status.
 * @property co2AvoidedKg     Estimated CO2 savings from reuse vs. new manufacturing.
 * @property pickupInfo       Instructions for lot pickup after sale.
 * @property createdAt        UTC instant when the lot was created.
 * @property updatedAt        UTC instant when the lot was last modified.
 */
data class Lot(
    val id: UUID,
    val sellerId: UUID,
    val brand: String,
    val title: String,
    val description: String,
    val categoryId: UUID,
    val specifications: Map<String, Any> = emptyMap(),
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationAddress: String? = null,
    val locationCountry: String,
    val locationCity: String,
    val reservePrice: BigDecimal? = null,
    val startingBid: BigDecimal,
    val auctionId: UUID? = null,
    val status: LotStatus = LotStatus.DRAFT,
    val co2AvoidedKg: Double? = null,
    val pickupInfo: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {

    /** Returns `true` if this lot can be edited by the seller. */
    fun isEditable(): Boolean = status.isEditable()

    /** Returns `true` if this lot has been assigned to an auction event. */
    fun isAssignedToAuction(): Boolean = auctionId != null

    /** Returns a copy with the given [newStatus] and an updated timestamp. */
    fun withStatus(newStatus: LotStatus): Lot =
        copy(status = newStatus, updatedAt = Instant.now())
}
