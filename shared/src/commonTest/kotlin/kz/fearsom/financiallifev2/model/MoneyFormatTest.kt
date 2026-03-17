package kz.fearsom.financiallifev2.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyFormatTest {

    @Test
    fun `kzt values use tenge symbol once`() {
        assertEquals("80к ₸", 80_000L.moneyFormat())
        assertEquals("2 М ₸", 2_000_000L.moneyFormat())
    }
}
