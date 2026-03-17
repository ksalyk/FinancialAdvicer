package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.model.ChatMessage
import kz.fearsom.financiallifev2.model.CurrencyCode
import kz.fearsom.financiallifev2.model.GameState
import kz.fearsom.financiallifev2.model.MessageSender
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ScenarioNarrativeRegressionTest {

    @Test
    fun `aidar 90s starts before tenge in rubles`() {
        val graph = ScenarioGraphFactory.forCharacter("aidar_90s", "kz_90s")
        val ps = graph.initialPlayerState

        assertEquals(1993, ps.year)
        assertEquals(10, ps.month)
        assertEquals(CurrencyCode.RUB, ps.currency)
    }

    @Test
    fun `2015 devaluation still wins over pool events on August 2015 tick`() {
        val engine = GameEngine.forCharacterAndEra("aidar", "kz_2015")
        val graph = ScenarioGraphFactory.forCharacter("aidar", "kz_2015")
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState.copy(month = 7, year = 2015),
                currentEventId = "normal_life",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Айдар"
        )

        val next = engine.makeChoice("save_cash")
        assertEquals("era_devaluation_2015", next.currentEventId)
    }

    @Test
    fun `chechen war broadcast wins over pool events on December 1994 tick`() {
        val engine = GameEngine.forCharacterAndEra("aidar_90s", "kz_90s")
        val graph = ScenarioGraphFactory.forCharacter("aidar_90s", "kz_90s")
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState.copy(month = 11, year = 1994),
                currentEventId = "normal_life",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Айдар"
        )

        val next = engine.makeChoice("do_nothing")
        assertEquals("chechen_war_broadcast", next.currentEventId)
    }

    @Test
    fun `nuclear disarmament event wins over pool events on April 1995 tick`() {
        val engine = GameEngine.forCharacterAndEra("aidar_90s", "kz_90s")
        val graph = ScenarioGraphFactory.forCharacter("aidar_90s", "kz_90s")
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState.copy(month = 3, year = 1995),
                currentEventId = "normal_life",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Айдар"
        )

        val next = engine.makeChoice("do_nothing")
        assertEquals("nuclear_disarmament_reaction", next.currentEventId)
    }

    @Test
    fun `tenge reform converts ruble balances at 500 to 1`() {
        val engine = GameEngine.forCharacterAndEra("aidar_90s", "kz_90s")
        val graph = ScenarioGraphFactory.forCharacter("aidar_90s", "kz_90s")
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState.copy(month = 11, year = 1993),
                currentEventId = "era_tenge_introduced",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Айдар"
        )

        val next = engine.makeChoice("hold_tenge")
        assertEquals(CurrencyCode.KZT, next.playerState.currency)
        assertEquals(53_000L, next.playerState.capital)
        assertEquals(15_000L, next.playerState.income)
        assertEquals(12_000L, next.playerState.expenses)
    }

    @Test
    fun `unique conditional event does not repeat after it was triggered once`() {
        val engine = GameEngine.forCharacterAndEra("aidar", "kz_2024")
        val graph = ScenarioGraphFactory.forCharacter("aidar", "kz_2024")
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState.copy(
                    capital = 600_000L,
                    financialKnowledge = 45
                ),
                currentEventId = "normal_life",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Айдар"
        )

        val first = engine.makeChoice("save_cash")
        assertEquals("investment_unlock", first.currentEventId)

        val afterSkip = engine.makeChoice("skip_iis")
        assertTrue("investment_unlock" in afterSkip.playerState.triggeredUniqueEvents)

        val reloaded = GameEngine.forCharacterAndEra("aidar", "kz_2024")
        reloaded.loadState(
            afterSkip.copy(
                currentEventId = "normal_life",
                isWaitingForChoice = true,
                gameOver = false,
                endingType = null
            ),
            characterName = "Айдар"
        )

        val second = reloaded.makeChoice("save_cash")
        assertNotEquals("investment_unlock", second.currentEventId)
    }
}
