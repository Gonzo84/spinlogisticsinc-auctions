package eu.auctionplatform.co2.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Represents a CO2 avoidance calculation for a specific lot.
 *
 * Each calculation records the estimated kilograms of CO2 avoided by
 * reusing the asset through the auction platform instead of manufacturing
 * a new replacement.
 *
 * @property id           Unique calculation identifier.
 * @property lotId        The catalog lot for which CO2 was calculated.
 * @property categoryId   The category used for the emission factor lookup.
 * @property co2AvoidedKg The estimated CO2 avoided in kilograms.
 * @property calculatedAt Timestamp when the calculation was performed.
 * @property version      Version counter for recalculations (monotonically increasing).
 */
data class Co2Calculation(
    val id: UUID,
    val lotId: UUID,
    val categoryId: UUID,
    val co2AvoidedKg: BigDecimal,
    val calculatedAt: Instant,
    val version: Int
)
