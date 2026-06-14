package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.i18n.StringKeys
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.MessageSender
import kz.fearsom.financiallifev2.model.PendingEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression for the "filler floods the late game" bug (reported on character "serik").
 *
 * Two failure modes are pinned here:
 *  1. A repeatable pool event ([GameEvent.maxOccurrences]) must never be drawn more
 *     than its cap, no matter how many empty months elapse.
 *  2. When the pool is exhausted but a dated beat is still ahead, the engine must
 *     fast-forward to it (emitting a single [StringKeys.SYS_TIME_ADVANCED] note)
 *     instead of looping the hard-coded `normal_life` fallback forever.
 */
class FillerEventCapRegressionTest {

    @Test
    fun `capped filler stops at its limit and time fast-forwards to the next beat`() {
        val engine = GameEngine(graph = CappedFillerGraph())
        var state = engine.startGame(characterName = "Test")

        var guard = 0
        while (!state.gameOver && guard++ < 200) {
            state = engine.makeChoice("tick")
        }

        val fillerShown = state.messages.count {
            it.sender == MessageSender.CHARACTER && it.sourceEventId == "filler"
        }
        assertEquals(2, fillerShown, "filler has maxOccurrences=2; it must not be drawn more than twice")

        assertTrue(state.gameOver, "engine should fast-forward to the scheduled finale, not loop filler")
        assertEquals(EndingType.FINANCIAL_STABILITY, state.endingType)

        val skipNote = state.messages.any {
            it.sender == MessageSender.SYSTEM && it.textKey == StringKeys.SYS_TIME_ADVANCED
        }
        assertTrue(skipNote, "empty months should be condensed into a single time-advanced note")
    }

    /**
     * Minimal graph: one repeatable-but-capped pool event plus a dated finale far in
     * the future. After the cap is hit, every remaining month is "empty", forcing the
     * fast-forward path to carry the player to the finale.
     */
    private class CappedFillerGraph : ScenarioGraph() {
        override val initialPlayerState = PlayerState(
            characterId = "test",
            eraId = "test",
            month = 1,
            year = 2024,
            capital = 1_000_000L,
            income = 100_000L,
            expenses = 50_000L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            // A finale five years out keeps a dated beat permanently ahead.
            pendingScheduled = listOf(PendingEvent("finale", fireAtYear = 2029, fireAtMonth = 1))
        )

        override val events = mapOf(
            "intro" to GameEvent(
                id = "intro",
                message = "intro",
                options = listOf(GameOption("tick", "Tick", "", Effect(), MONTHLY_TICK))
            ),
            "filler" to GameEvent(
                id = "filler",
                message = "filler",
                options = listOf(GameOption("tick", "Tick", "", Effect(), MONTHLY_TICK)),
                cooldownMonths = 2,
                maxOccurrences = 2
            ),
            "finale" to GameEvent(
                id = "finale",
                message = "finale",
                isEnding = true,
                endingType = EndingType.FINANCIAL_STABILITY,
                options = emptyList()
            ),
            // Last-resort sink; must NOT be reached while the finale is still pending.
            "normal_life" to GameEvent(
                id = "normal_life",
                message = "normal",
                options = listOf(GameOption("tick", "Tick", "", Effect(), MONTHLY_TICK))
            )
        )

        override val conditionalEvents = emptyList<GameEvent>()

        override val eventPool = listOf(PoolEntry("filler", 100))
    }
}
