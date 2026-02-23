package eu.auctionplatform.catalog.api.dto

import eu.auctionplatform.catalog.domain.model.AuctionEventStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

// =============================================================================
// Request DTOs
// =============================================================================

/**
 * Request payload for creating an auction event.
 */
data class CreateAuctionEventRequest(
    val title: String,
    val brand: String,
    val startDate: Instant,
    val endDate: Instant,
    val country: String,
    val buyerPremiumPercent: BigDecimal = BigDecimal("18")
)

// =============================================================================
// Response DTOs
// =============================================================================

/**
 * Response representation of an auction event.
 */
data class AuctionEventResponse(
    val id: UUID,
    val title: String,
    val brand: String,
    val startDate: Instant,
    val endDate: Instant,
    val country: String,
    val status: AuctionEventStatus,
    val buyerPremiumPercent: BigDecimal,
    val totalLots: Int
)
