package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.admin.ScenarioGraphDto
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.analysis.GraphWarning
import kz.fearsom.financiallifev2.scenarios.analysis.analyzeScenario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [analyzeScenario] validation logic.
 * The analysis function is shared code used by both the admin SPA (live validation)
 * and the server (publish gating), so correctness here is critical.
 */
class ScenarioGraphAnalysisTest {

    // ── Fixtures ───────────────────────────────────────────────────────────────

    private fun opt(id: String, next: String) = GameOption(id = id, text = "Go", emoji = "▶", next = next)

    private fun event(
        id: String,
        opts: List<GameOption> = emptyList(),
        isEnding: Boolean = false,
        endingType: EndingType? = null
    ) = GameEvent(id = id, message = "msg", options = opts, isEnding = isEnding, endingType = endingType)

    private fun dto(
        events: List<GameEvent>,
        conditionalEvents: List<GameEvent> = emptyList(),
        pool: List<PoolEntry> = emptyList()
    ) = ScenarioGraphDto(
        initialPlayerState = PlayerState(),
        events = events,
        conditionalEvents = conditionalEvents,
        eventPool = pool
    )

    // ── Clean graph ────────────────────────────────────────────────────────────

    @Test
    fun clean_minimal_graph_has_no_warnings() {
        val graph = dto(listOf(
            event("intro", opts = listOf(opt("go", "end"))),
            event("end", isEnding = true, endingType = EndingType.FINANCIAL_STABILITY)
        ))

        val analysis = analyzeScenario(graph, selfContained = true)

        assertEquals(0, analysis.warnings.size, "Expected no warnings but got: ${analysis.warnings}")
        assertEquals("intro", analysis.rootId)
        assertEquals(2, analysis.stats.eventCount)
        assertEquals(1, analysis.stats.endingCount)
    }

    // ── Dead-end non-ending event ──────────────────────────────────────────────

    @Test
    fun non_ending_event_with_no_options_is_flagged_as_error() {
        val graph = dto(listOf(
            event("intro", opts = listOf(opt("go", "stuck"))),
            event("stuck") // no options, not an ending → dead-end
        ))

        val analysis = analyzeScenario(graph, selfContained = true)

        val errors = analysis.warnings.filter { it.severity == GraphWarning.Severity.ERROR }
        assertTrue(errors.any { it.eventId == "stuck" && it.message.contains("no options") },
            "Expected dead-end error for 'stuck', got: $errors")
    }

    // ── Duplicate event IDs ────────────────────────────────────────────────────

    @Test
    fun duplicate_event_id_is_flagged_as_error() {
        val graph = dto(listOf(
            event("intro", opts = listOf(opt("go", "end"))),
            event("end", isEnding = true, endingType = EndingType.FINANCIAL_STABILITY),
            event("end") // duplicate id
        ))

        val analysis = analyzeScenario(graph, selfContained = true)

        val errors = analysis.warnings.filter { it.severity == GraphWarning.Severity.ERROR }
        assertTrue(errors.any { it.eventId == "end" && it.message.contains("duplicate") },
            "Expected duplicate-id error, got: $errors")
    }

    // ── Orphan event ───────────────────────────────────────────────────────────

    @Test
    fun orphan_event_not_in_pool_is_flagged_as_warning() {
        val graph = dto(listOf(
            event("intro", opts = listOf(opt("go", "end"))),
            event("end", isEnding = true, endingType = EndingType.FINANCIAL_STABILITY),
            event("orphan", opts = listOf(opt("x", "end"))) // not reachable from intro, not in pool
        ))

        val analysis = analyzeScenario(graph, selfContained = false)

        val warns = analysis.warnings.filter { it.severity == GraphWarning.Severity.WARN }
        assertTrue(warns.any { it.eventId == "orphan" && it.message.contains("unreachable") },
            "Expected orphan warning for 'orphan', got: $warns")
    }

    @Test
    fun pool_entry_is_NOT_flagged_as_orphan() {
        // Events only reachable via the event pool are legitimate — the engine draws them
        // after a monthly tick. They must not be mis-classified as orphans.
        val poolEvent = event("pool_event", opts = listOf(opt("done", "end")))
        val graph = dto(
            events = listOf(
                event("intro", opts = listOf(opt("tick", MONTHLY_TICK))),
                event("end", isEnding = true, endingType = EndingType.FINANCIAL_STABILITY),
                poolEvent
            ),
            pool = listOf(PoolEntry(eventId = "pool_event", baseWeight = 1))
        )

        val analysis = analyzeScenario(graph, selfContained = true)

        assertFalse(
            analysis.warnings.any { it.eventId == "pool_event" && it.message.contains("unreachable") },
            "Pool entry should not be flagged as orphan"
        )
    }
}
