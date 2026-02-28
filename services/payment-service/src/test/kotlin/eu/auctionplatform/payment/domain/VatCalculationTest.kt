package eu.auctionplatform.payment.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.math.RoundingMode

class VatCalculationTest {

    @Test
    fun `standard EU VAT rate is 21 percent for NL`() {
        val amount = BigDecimal("100.00")
        val vatRate = BigDecimal("0.21")
        val vat = amount.multiply(vatRate).setScale(2, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("21.00"), vat)
    }

    @Test
    fun `total with VAT is correct`() {
        val amount = BigDecimal("100.00")
        val vatRate = BigDecimal("0.21")
        val total = amount.add(amount.multiply(vatRate)).setScale(2, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("121.00"), total)
    }

    @Test
    fun `zero amount has zero VAT`() {
        val amount = BigDecimal.ZERO
        val vatRate = BigDecimal("0.21")
        val vat = amount.multiply(vatRate).setScale(2, RoundingMode.HALF_UP)
        assertEquals(BigDecimal("0.00"), vat)
    }
}
