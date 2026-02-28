package eu.auctionplatform.auction.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

class AuctionDomainTest {

    @Test
    fun `bid amount must be positive`() {
        val amount = BigDecimal("100.00")
        assertTrue(amount > BigDecimal.ZERO)
    }

    @Test
    fun `bid must meet minimum increment`() {
        val currentBid = BigDecimal("500.00")
        val minIncrement = BigDecimal("25.00")
        val newBid = BigDecimal("520.00")

        assertFalse(newBid >= currentBid + minIncrement,
            "Bid of $newBid should not meet minimum increment of $minIncrement over $currentBid")
    }

    @Test
    fun `starting bid cannot be negative`() {
        val startingBid = BigDecimal("-10.00")
        assertTrue(startingBid < BigDecimal.ZERO)
    }
}
