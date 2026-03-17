package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.model.ChatMessage
import kz.fearsom.financiallifev2.model.GameState
import kz.fearsom.financiallifev2.model.MessageSender
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ScenarioNarrativeRegressionTest {

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
