package kz.fearsom.financiallifev2.scenarios

/**
 * Single source of truth for the (characterId, eraId) combinations covered by
 * scenario tests.
 *
 * Add a new graph here exactly ONCE and every scenario suite picks it up
 * automatically:
 *   - [ScenarioGraphContentTest]    — structural integrity
 *   - [ScenarioSimulationTest]      — random-playthrough liveness + reachability
 *
 * Keep this list in sync with [ScenarioGraphFactory.buildGraph]/[forEra].
 */
object ScenarioTestCatalog {

    /** characterId to eraId, mirroring the predefined branches in ScenarioGraphFactory. */
    val combos: List<Pair<String, String>> = listOf(
        "aidar_90s" to "kz_90s",
        "aidar" to "kz_2005",
        "daniyar" to "kz_2005",
        "serik" to "kz_2005",
        "dana" to "kz_2015",
        "asan" to "kz_2024",
    )

    fun graphs(): List<ScenarioGraph> = combos.map { (characterId, eraId) ->
        ScenarioGraphFactory.forCharacter(characterId, eraId)
    }
}
