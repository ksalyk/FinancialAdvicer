package kz.fearsom.financiallifev2.scenarios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DaniyarScenarioBalanceTest {

    private val graph = DaniyarScenarioGraph()

    @Test
    fun `baseline cash flow stays narrow`() {
        assertEquals(500L, graph.initialPlayerState.netMonthlyFlow)
    }

    @Test
    fun `putting money aside does not create capital`() {
        val normalLife = assertNotNull(graph.findEvent("normal_life"))
        val putAside = assertNotNull(normalLife.options.find { it.id == "put_aside" })

        assertEquals(0L, putAside.effects.capitalDelta)
    }

    @Test
    fun `living costs and workwear remain real money sinks`() {
        val costOfLiving = assertNotNull(graph.findEvent("pool_cost_of_living"))
        assertTrue(costOfLiving.options.all { it.effects.expensesDelta > 0L })

        val workwear = assertNotNull(graph.findEvent("pool_workwear"))
        assertTrue(workwear.cooldownMonths > 0)
        assertTrue(workwear.options.all { it.effects.capitalDelta < 0L })
    }
}
