package eu.auctionplatform.payment

import java.math.BigDecimal
import java.util.UUID

object TestFixtures {
    val BUYER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val SELLER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")

    fun createCheckoutRequest(
        lotIds: List<UUID> = listOf(UUID.randomUUID()),
        buyerId: UUID = BUYER_ID
    ) = mapOf(
        "buyerId" to buyerId.toString(),
        "lotIds" to lotIds.map { it.toString() },
        "currency" to "USD",
        "brand" to "troostwijk"
    )
}
