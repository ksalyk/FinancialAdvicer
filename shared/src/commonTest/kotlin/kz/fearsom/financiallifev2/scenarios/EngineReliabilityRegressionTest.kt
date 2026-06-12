package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.PendingEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.ScheduledEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngineReliabilityRegressionTest {

    @Test
    fun `era event does not delete due scheduled event`() {
        val engine = GameEngine(
            graph = ScheduledVsEraGraph(),
            eraDefinition = EraDefinition(
                id = "test",
                name = "Test",
                startYear = 2024,
                endYear = 2024,
                globalEvents = listOf(EraGlobalEvent("era_event", year = 2024, month = 2))
            )
        )

        engine.startGame(characterName = "Test")

        val era = engine.makeChoice("schedule")
        assertEquals("era_event", era.currentEventId)
        assertTrue(era.playerState.pendingScheduled.any { it.eventId == "scheduled_event" })

        val scheduled = engine.makeChoice("continue_after_era")
        assertEquals("scheduled_event", scheduled.currentEventId)
        assertTrue(scheduled.playerState.pendingScheduled.none { it.eventId == "scheduled_event" })
    }

    @Test
    fun `multiple due scheduled events fire one per tick`() {
        val engine = GameEngine(graph = MultipleScheduledGraph())
        engine.startGame(characterName = "Test")

        val first = engine.makeChoice("tick")
        assertEquals("scheduled_one", first.currentEventId)
        assertTrue(first.playerState.pendingScheduled.any { it.eventId == "scheduled_two" })

        val second = engine.makeChoice("tick_one")
        assertEquals("scheduled_two", second.currentEventId)
        assertTrue(second.playerState.pendingScheduled.none { it.eventId == "scheduled_two" })
    }

    @Test
    fun `conditional cooldown prevents immediate repeat`() {
        val engine = GameEngine(graph = ConditionalCooldownGraph())
        engine.startGame(characterName = "Test")

        val first = engine.makeChoice("tick")
        assertEquals("conditional_warning", first.currentEventId)

        val second = engine.makeChoice("tick_conditional")
        assertEquals("normal_life", second.currentEventId)
    }

    @Test
    fun `debt payment stops when debt is already zero`() {
        val engine = GameEngine(graph = DebtGraph(initialDebt = 0L, payment = 30_000L))
        engine.startGame(characterName = "Test")

        val next = engine.makeChoice("tick")

        assertEquals(100_000L, next.playerState.capital)
        assertEquals(0L, next.playerState.debt)
        assertEquals(0L, next.playerState.debtPaymentMonthly)
        assertEquals(0L, next.messages.last { it.monthlyReport != null }.monthlyReport?.debtPayment)
    }

    @Test
    fun `final debt payment is capped to remaining debt`() {
        val engine = GameEngine(graph = DebtGraph(initialDebt = 5_000L, payment = 30_000L))
        engine.startGame(characterName = "Test")

        val next = engine.makeChoice("tick")

        assertEquals(95_000L, next.playerState.capital)
        assertEquals(0L, next.playerState.debt)
        assertEquals(0L, next.playerState.debtPaymentMonthly)
        assertEquals(5_000L, next.messages.last { it.monthlyReport != null }.monthlyReport?.debtPayment)
    }

    private class ScheduledVsEraGraph : ScenarioGraph() {
        override val initialPlayerState = PlayerState(characterId = "test", eraId = "test", month = 1, year = 2024)

        override val events = mapOf(
            "intro" to GameEvent(
                id = "intro",
                message = "intro",
                options = listOf(GameOption("schedule", "Schedule", "", Effect(scheduleEvent = ScheduledEvent("scheduled_event", 1)), MONTHLY_TICK))
            ),
            "era_event" to GameEvent(
                id = "era_event",
                message = "era",
                options = listOf(GameOption("continue_after_era", "Continue", "", Effect(), MONTHLY_TICK))
            ),
            "scheduled_event" to GameEvent(
                id = "scheduled_event",
                message = "scheduled",
                options = listOf(GameOption("continue_scheduled", "Continue", "", Effect(), MONTHLY_TICK))
            ),
            "normal_life" to GameEvent(
                id = "normal_life",
                message = "normal",
                options = listOf(GameOption("tick", "Tick", "", Effect(), MONTHLY_TICK))
            )
        )

        override val conditionalEvents = emptyList<GameEvent>()
        override val eventPool = emptyList<PoolEntry>()
    }

    private class MultipleScheduledGraph : ScenarioGraph() {
        override val initialPlayerState = PlayerState(
            characterId = "test",
            eraId = "test",
            month = 1,
            year = 2024,
            pendingScheduled = listOf(
                PendingEvent("scheduled_one", 2024, 1),
                PendingEvent("scheduled_two", 2024, 1)
            )
        )

        override val events = mapOf(
            "intro" to GameEvent("intro", "intro", options = listOf(GameOption("tick", "Tick", "", Effect(), MONTHLY_TICK))),
            "normal_life" to GameEvent("normal_life", "normal", options = listOf(GameOption("tick", "Tick", "", Effect(), MONTHLY_TICK))),
            "scheduled_one" to GameEvent("scheduled_one", "one", options = listOf(GameOption("tick_one", "Tick", "", Effect(), MONTHLY_TICK))),
            "scheduled_two" to GameEvent("scheduled_two", "two", options = listOf(GameOption("tick_two", "Tick", "", Effect(), MONTHLY_TICK)))
        )

        override val conditionalEvents = emptyList<GameEvent>()
        override val eventPool = emptyList<PoolEntry>()
    }

    private class ConditionalCooldownGraph : ScenarioGraph() {
        override val initialPlayerState = PlayerState(
            characterId = "test",
            eraId = "test",
            month = 1,
            year = 2024,
            capital = 100L,
            income = 0L,
            expenses = 100L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            stress = 80
        )

        override val events = mapOf(
            "intro" to GameEvent("intro", "intro", options = listOf(GameOption("tick", "Tick", "", Effect(), MONTHLY_TICK))),
            "normal_life" to GameEvent("normal_life", "normal", options = listOf(GameOption("tick_normal", "Tick", "", Effect(), MONTHLY_TICK)))
        )

        override val conditionalEvents = listOf(
            GameEvent(
                id = "conditional_warning",
                message = "warning",
                options = listOf(GameOption("tick_conditional", "Tick", "", Effect(), MONTHLY_TICK)),
                conditions = listOf(Condition.Stat(Condition.Stat.Field.STRESS, Condition.Stat.Op.GTE, 80)),
                cooldownMonths = 2
            )
        )

        override val eventPool = emptyList<PoolEntry>()
    }

    private class DebtGraph(
        private val initialDebt: Long,
        private val payment: Long
    ) : ScenarioGraph() {
        override val initialPlayerState = PlayerState(
            characterId = "test",
            eraId = "test",
            month = 1,
            year = 2024,
            capital = 100_000L,
            income = 0L,
            expenses = 0L,
            debt = initialDebt,
            debtPaymentMonthly = payment
        )

        override val events = mapOf(
            "intro" to GameEvent("intro", "intro", options = listOf(GameOption("tick", "Tick", "", Effect(), MONTHLY_TICK))),
            "normal_life" to GameEvent("normal_life", "normal", options = listOf(GameOption("tick_normal", "Tick", "", Effect(), MONTHLY_TICK)))
        )

        override val conditionalEvents = emptyList<GameEvent>()
        override val eventPool = emptyList<PoolEntry>()
    }
}
