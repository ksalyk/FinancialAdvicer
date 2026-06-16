package kz.fearsom.financiallifev2.scenarios

/**
 * Single source of truth for the scenario graphs covered by the test suites.
 *
 * Split into two groups so a new authored character is enrolled in the simulation +
 * content suites by adding one line to [authoredCombos] — without tripping the
 * empty-shell structural test (which only applies to [emptyShellCombos]).
 */
object ScenarioTestCatalog {

    /** Era shells with no authored content yet (characterId is a dummy). */
    val emptyShellCombos: List<Pair<String, String>> = listOf(
        "empty" to "kz_90s",
        "empty" to "kz_2005",
        "empty" to "kz_2015",
        "empty" to "kz_2024",
    )

    /** Authored character graphs — real content. `DaniyarScenarioGraph` is the reference. */
    val authoredCombos: List<Pair<String, String>> = listOf(
        "daniyar_90s" to "kz_90s",
    )

    /** Everything the simulation + reachability suites run against. */
    val combos: List<Pair<String, String>> = emptyShellCombos + authoredCombos

    fun graphs(): List<ScenarioGraph> = combos.toGraphs()
    fun emptyShellGraphs(): List<ScenarioGraph> = emptyShellCombos.toGraphs()
    fun authoredGraphs(): List<ScenarioGraph> = authoredCombos.toGraphs()

    private fun List<Pair<String, String>>.toGraphs(): List<ScenarioGraph> =
        map { (characterId, eraId) -> ScenarioGraphFactory.forCharacter(characterId, eraId) }
}
