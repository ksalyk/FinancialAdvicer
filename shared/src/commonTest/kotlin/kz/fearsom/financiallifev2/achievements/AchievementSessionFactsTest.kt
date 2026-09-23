package kz.fearsom.financiallifev2.achievements

import kz.fearsom.financiallifev2.model.ChatMessage
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameState
import kz.fearsom.financiallifev2.model.MessageSender
import kz.fearsom.financiallifev2.model.PlayerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Direct unit tests for [AchievementSessionFacts.update] — the cross-turn
 * accumulator that backs unlock conditions like DebtFree and CalmThroughCrisis.
 *
 * The evaluator tests use facts indirectly; these tests verify the accumulator's
 * own invariants in isolation.
 */
class AchievementSessionFactsTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun state(
        ps: PlayerState = PlayerState(),
        messages: List<ChatMessage> = emptyList(),
        gameOver: Boolean = false
    ) = GameState(
        playerState = ps,
        currentEventId = "evt",
        characterName = "Test",
        messages = messages,
        gameOver = gameOver
    )

    /**
     * Creates a crisis message. [absoluteMonth] = year * 12 + month.
     * Inverse: month = if (abs % 12 == 0) 12 else abs % 12; year = (abs - month) / 12.
     */
    private fun crisisMsg(id: String, absoluteMonth: Int = 1) = ChatMessage(
        id = id,
        sender = MessageSender.CHARACTER,
        sceneTag = AchievementSessionFacts.SCENE_TAG_CRISIS,
        sourcePlayerState = run {
            val m = if (absoluteMonth % 12 == 0) 12 else absoluteMonth % 12
            PlayerState(year = (absoluteMonth - m) / 12, month = m)
        }
    )

    // ── everHadDebt ───────────────────────────────────────────────────────────

    @Test
    fun everHadDebt_becomes_true_when_debt_is_positive() {
        val facts = AchievementSessionFacts()
        assertFalse(facts.everHadDebt)

        val updated = facts.update(state(PlayerState(debt = 500_000L)))
        assertTrue(updated.everHadDebt)
    }

    @Test
    fun everHadDebt_stays_true_after_debt_is_cleared() {
        var facts = AchievementSessionFacts()
        facts = facts.update(state(PlayerState(debt = 500_000L)))
        assertTrue(facts.everHadDebt)

        // Debt paid off — flag must remain true so DebtFree can fire.
        facts = facts.update(state(PlayerState(debt = 0L)))
        assertTrue(facts.everHadDebt, "everHadDebt must never revert to false")
    }

    @Test
    fun everHadDebt_stays_false_when_debt_never_occurs() {
        var facts = AchievementSessionFacts()
        repeat(5) { facts = facts.update(state(PlayerState(debt = 0L))) }
        assertFalse(facts.everHadDebt)
    }

    // ── Crisis window ─────────────────────────────────────────────────────────

    @Test
    fun crisis_window_opens_on_first_crisis_message() {
        val facts = AchievementSessionFacts()
        assertNull(facts.crisisStartAbsMonth)

        // absoluteMonth = year * 12 + month; use month=12, year=1 → absoluteMonth=24
        val updated = facts.update(state(
            ps = PlayerState(stress = 40, year = 1, month = 12),
            messages = listOf(crisisMsg("crisis_1", absoluteMonth = 24))
        ))

        assertEquals(24, updated.crisisStartAbsMonth)
        assertEquals("crisis_1", updated.crisisMessageId)
        assertEquals(40, updated.maxStressSinceCrisis)
    }

    @Test
    fun maxStressSinceCrisis_tracks_peak_stress_after_crisis_opens() {
        var facts = AchievementSessionFacts()
        val crisis = crisisMsg("crisis_1", absoluteMonth = 1)

        // Month 1: crisis opens, stress = 30
        facts = facts.update(state(ps = PlayerState(stress = 30), messages = listOf(crisis)))
        assertEquals(30, facts.maxStressSinceCrisis)

        // Month 2: stress rises to 55 — max should update
        facts = facts.update(state(ps = PlayerState(stress = 55), messages = listOf(crisis)))
        assertEquals(55, facts.maxStressSinceCrisis)

        // Month 3: stress drops to 20 — max must NOT decrease
        facts = facts.update(state(ps = PlayerState(stress = 20), messages = listOf(crisis)))
        assertEquals(55, facts.maxStressSinceCrisis, "maxStressSinceCrisis should only ever increase")
    }

    @Test
    fun new_crisis_resets_window_and_starts_fresh() {
        var facts = AchievementSessionFacts()
        val firstCrisis = crisisMsg("crisis_1", absoluteMonth = 10)
        facts = facts.update(state(
            ps = PlayerState(stress = 70, year = 0, month = 10),
            messages = listOf(firstCrisis)
        ))
        assertEquals(10, facts.crisisStartAbsMonth)
        assertEquals(70, facts.maxStressSinceCrisis)

        // New crisis with different id at absolute month 25 (year=2, month=1)
        val secondCrisis = crisisMsg("crisis_2", absoluteMonth = 25)
        facts = facts.update(state(
            ps = PlayerState(stress = 15, year = 2, month = 1),
            messages = listOf(firstCrisis, secondCrisis)
        ))

        assertEquals(25, facts.crisisStartAbsMonth, "Crisis window should reset to new crisis start")
        assertEquals(15, facts.maxStressSinceCrisis, "Max stress should reset to current stress for new crisis")
        assertEquals("crisis_2", facts.crisisMessageId)
    }
}
