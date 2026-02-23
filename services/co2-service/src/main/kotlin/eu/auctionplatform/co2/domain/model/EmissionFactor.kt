package eu.auctionplatform.co2.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Represents a CO2 emission factor for a specific product category.
 *
 * Each factor captures the estimated CO2 in kilograms that would be emitted
 * to manufacture a new unit of the given product type, and a reuse factor
 * (typically between 0.70 and 0.95) representing the proportion of those
 * emissions avoided by reusing the existing unit through auction.
 *
 * @property id                    Unique factor identifier.
 * @property categoryId            Reference to the catalog category.
 * @property productType           Human-readable product type label (e.g. "Excavator").
 * @property newManufacturingCo2Kg Estimated CO2 (kg) emitted by manufacturing a new unit.
 * @property reuseFactor           Fraction of CO2 avoided through reuse (0.70 - 0.95).
 * @property source                Attribution / data source (e.g. "EU EF Database 3.1").
 * @property lastUpdated           Timestamp of the most recent update to this factor.
 */
data class EmissionFactor(
    val id: UUID,
    val categoryId: UUID,
    val productType: String,
    val newManufacturingCo2Kg: BigDecimal,
    val reuseFactor: BigDecimal,
    val source: String,
    val lastUpdated: Instant
)
