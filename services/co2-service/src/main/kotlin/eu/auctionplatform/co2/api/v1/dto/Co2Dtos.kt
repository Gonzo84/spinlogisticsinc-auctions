package eu.auctionplatform.co2.api.v1.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// =============================================================================
// Request DTOs
// =============================================================================

/**
 * Request payload for updating an emission factor.
 */
data class UpdateEmissionFactorRequest(
    val productType: String,
    val newManufacturingCo2Kg: BigDecimal,
    val reuseFactor: BigDecimal,
    val source: String
)

// =============================================================================
// Response DTOs
// =============================================================================

/**
 * Response representation of a CO2 calculation for a specific lot.
 */
data class Co2CalculationResponse(
    val id: UUID,
    val lotId: UUID,
    val categoryId: UUID,
    val co2AvoidedKg: BigDecimal,
    val calculatedAt: Instant,
    val version: Int
)

/**
 * Response representation of an emission factor.
 */
data class EmissionFactorResponse(
    val id: UUID,
    val categoryId: UUID,
    val productType: String,
    val newManufacturingCo2Kg: BigDecimal,
    val reuseFactor: BigDecimal,
    val source: String,
    val lastUpdated: Instant
)

/**
 * Response representation of a CO2 summary with equivalence metrics.
 */
data class Co2SummaryResponse(
    val totalCo2AvoidedKg: BigDecimal,
    val totalLots: Long,
    val equivalentTreesPlanted: BigDecimal,
    val equivalentCarKmAvoided: BigDecimal
)
