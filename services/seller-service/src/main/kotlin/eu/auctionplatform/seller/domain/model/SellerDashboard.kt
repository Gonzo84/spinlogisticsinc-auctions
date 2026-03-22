package eu.auctionplatform.seller.domain.model

import java.math.BigDecimal

/**
 * Dashboard KPI data for a seller.
 *
 * Aggregated from auction events and settlement records to provide
 * real-time performance metrics on the seller's dashboard.
 *
 * @property activeLots          Number of lots currently live in auction.
 * @property totalBids           Total number of bids received across all seller's lots.
 * @property totalHammerSales    Total hammer price (winning bid amounts) in USD.
 * @property pendingSettlements  Number of won auctions awaiting settlement/payment.
 * @property totalSettled        Total amount settled (paid out) to the seller in USD.
 */
data class SellerDashboard(
    val activeLots: Int = 0,
    val totalBids: Int = 0,
    val totalHammerSales: BigDecimal = BigDecimal.ZERO,
    val pendingSettlements: Int = 0,
    val totalSettled: BigDecimal = BigDecimal.ZERO
)
