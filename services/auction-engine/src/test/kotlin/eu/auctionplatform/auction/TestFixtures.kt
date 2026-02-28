package eu.auctionplatform.auction

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object TestFixtures {
    val SELLER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    val BUYER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val LOT_ID: UUID = UUID.randomUUID()

    fun createAuctionRequest(
        lotId: UUID = LOT_ID,
        startingBid: BigDecimal = BigDecimal("100.00"),
        currency: String = "EUR"
    ) = mapOf(
        "lotId" to lotId.toString(),
        "title" to "Test Auction Lot",
        "startingBid" to startingBid.toString(),
        "currency" to currency,
        "startTime" to Instant.now().plusSeconds(60).toString(),
        "endTime" to Instant.now().plusSeconds(3660).toString(),
        "sellerId" to SELLER_ID.toString(),
        "brand" to "troostwijk"
    )

    fun createBidRequest(
        amount: BigDecimal = BigDecimal("150.00"),
        bidderId: UUID = BUYER_ID
    ) = mapOf(
        "amount" to amount.toString(),
        "bidderId" to bidderId.toString(),
        "currency" to "EUR"
    )
}
