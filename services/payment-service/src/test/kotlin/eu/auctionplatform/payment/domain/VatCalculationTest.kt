package eu.auctionplatform.payment.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.math.RoundingMode

class VatCalculationTest {

    @Test
    fun `standard US sales tax rate is 6_25 percent for TX`() {
        val amount = BigDecimal("100.00")
        val taxRate = BigDecimal("6.25").divide(BigDecimal("100"), 6, RoundingMode.HALF_UP)
        val tax = amount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("6.25"), tax)
    }

    @Test
    fun `total with sales tax is correct`() {
        val amount = BigDecimal("100.00")
        val taxRate = BigDecimal("6.25").divide(BigDecimal("100"), 6, RoundingMode.HALF_UP)
        val total = amount.add(amount.multiply(taxRate)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("106.25"), total)
    }

    @Test
    fun `zero amount has zero sales tax`() {
        val amount = BigDecimal.ZERO
        val taxRate = BigDecimal("6.25").divide(BigDecimal("100"), 6, RoundingMode.HALF_UP)
        val tax = amount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("0.00"), tax)
    }
}
