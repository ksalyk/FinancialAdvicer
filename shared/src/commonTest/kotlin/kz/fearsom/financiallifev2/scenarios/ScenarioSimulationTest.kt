package kz.fearsom.financiallifev2.scenarios

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Engine-driven safety net for every graph in [ScenarioTestCatalog].
 *
 * These two tests are the "solid" guarantee for AI-authored scenarios: a graph
 * that compiles but traps the player, crashes, or strands an ending fails here
 * before it can ship.
 */
class ScenarioSimulationTest {

    private val seedsPerGraph = 40
    private val maxStepsPerRun = 800

    @Test
    fun random_playthroughs_never_crash_dead_end_or_break_stat_bounds() {
        val problems = StringBuilder()

        ScenarioTestCatalog.combos.forEach { (characterId, eraId) ->
            val report = ScenarioSimulationHarness.simulate(
                characterId = characterId,
                eraId = eraId,
                seeds = seedsPerGraph,
                maxStepsPerRun = maxStepsPerRun,
            )
            println(
                "SIM ${report.combo}: ${report.playthroughs} runs, " +
                    "${report.endingsReached} reached an ending, ${report.violations.size} violation(s)",
            )
            if (report.violations.isNotEmpty()) {
                problems.appendLine("[$characterId/$eraId] ${report.violations.size} violation(s):")
                report.violations.take(20).forEach { problems.appendLine("    ${it.kind}: ${it.detail}") }
            }
        }

        assertTrue(problems.isEmpty(), "Scenario simulation found problems:\n$problems")
    }

    @Test
    fun every_reference_resolves_and_all_endings_are_reachable() {
        val problems = StringBuilder()

        ScenarioTestCatalog.combos.forEach { (characterId, eraId) ->
            val report = ScenarioSimulationHarness.validateReachability(characterId, eraId)
            println(
                "REACH ${report.combo}: ${report.reachableEndings.size}/${report.allEndings.size} endings reachable, " +
                    "${report.danglingRefs.size} dangling ref(s)",
            )

            report.danglingRefs.forEach { problems.appendLine("[$characterId/$eraId] ${it.kind}: ${it.detail}") }

            if (report.allEndings.isEmpty()) {
                problems.appendLine("[$characterId/$eraId] no ending events defined")
            } else if (report.reachableEndings.isEmpty()) {
                problems.appendLine("[$characterId/$eraId] no ending reachable from 'intro'")
            }

            if (report.unreachableEndings.isNotEmpty()) {
                problems.appendLine(
                    "[$characterId/$eraId] unreachable ending(s): ${report.unreachableEndings.joinToString()}",
                )
            }
        }

        assertTrue(problems.isEmpty(), "Scenario reachability problems:\n$problems")
    }
}
