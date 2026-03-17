package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class RandomEventCooldownRegressionTest {

    @Test
    fun `pool event without explicit cooldown becomes single-use`() {
        val engine = GameEngine(graph = SingleUsePoolEventGraph())

        engine.startGame(characterName = "Тест")

        val first = engine.makeChoice("tick")
        assertEquals("random_single_use", first.currentEventId)
        assertEquals(true, "random_single_use" in first.playerState.triggeredUniqueEvents)
        assertEquals(null, first.playerState.eventCooldowns["random_single_use"])

        val second = engine.makeChoice("tick_again")
        assertEquals("normal_life", second.currentEventId)
    }

    @Test
    fun `pool event with explicit cooldown can return after cooldown expires`() {
        val engine = GameEngine(graph = CooldownPoolEventGraph())

        engine.startGame(characterName = "Тест")

        val first = engine.makeChoice("tick")
        assertEquals("random_with_cooldown", first.currentEventId)
        assertEquals(first.playerState.absoluteMonth + 2, first.playerState.eventCooldowns["random_with_cooldown"])

        val second = engine.makeChoice("tick_again")
        assertEquals("normal_life", second.currentEventId)

        val third = engine.makeChoice("tick_fallback")
        assertEquals("random_with_cooldown", third.currentEventId)
    }

    private class SingleUsePoolEventGraph : ScenarioGraph() {
        override val initialPlayerState: PlayerState = PlayerState(
            characterId = "test",
            eraId = "kz_2024",
            month = 1,
            year = 2024
        )

        override val events: Map<String, GameEvent> = mapOf(
            "intro" to GameEvent(
                id = "intro",
                message = "intro",
                options = listOf(
                    GameOption("tick", "Tick", "⏭️", Effect(), MONTHLY_TICK)
                )
            ),
            "random_single_use" to GameEvent(
                id = "random_single_use",
                message = "random",
                options = listOf(
                    GameOption("tick_again", "Tick again", "⏭️", Effect(), MONTHLY_TICK)
                )
            ),
            "normal_life" to GameEvent(
                id = "normal_life",
                message = "fallback",
                options = listOf(
                    GameOption("tick_fallback", "Tick fallback", "⏭️", Effect(), MONTHLY_TICK)
                )
            )
        )

        override val conditionalEvents: List<GameEvent> = emptyList()

        override val eventPool: List<PoolEntry> = listOf(
            PoolEntry("random_single_use", 1)
        )
    }

    private class CooldownPoolEventGraph : ScenarioGraph() {
        override val initialPlayerState: PlayerState = PlayerState(
            characterId = "test",
            eraId = "kz_2024",
            month = 1,
            year = 2024
        )

        override val events: Map<String, GameEvent> = mapOf(
            "intro" to GameEvent(
                id = "intro",
                message = "intro",
                options = listOf(
                    GameOption("tick", "Tick", "⏭️", Effect(), MONTHLY_TICK)
                )
            ),
            "random_with_cooldown" to GameEvent(
                id = "random_with_cooldown",
                message = "random",
                options = listOf(
                    GameOption("tick_again", "Tick again", "⏭️", Effect(), MONTHLY_TICK)
                ),
                cooldownMonths = 2
            ),
            "normal_life" to GameEvent(
                id = "normal_life",
                message = "fallback",
                options = listOf(
                    GameOption("tick_fallback", "Tick fallback", "⏭️", Effect(), MONTHLY_TICK)
                )
            )
        )

        override val conditionalEvents: List<GameEvent> = emptyList()

        override val eventPool: List<PoolEntry> = listOf(
            PoolEntry("random_with_cooldown", 1)
        )
    }
}
