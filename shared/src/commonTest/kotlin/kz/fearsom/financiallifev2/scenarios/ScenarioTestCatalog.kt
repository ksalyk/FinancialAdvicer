package kz.fearsom.financiallifev2.scenarios

/**
 * Single source of truth for the empty era shells covered by scenario tests.
 */
object ScenarioTestCatalog {

    /** characterId to eraId. Character is a dummy because scenarios are era-level. */
    val combos: List<Pair<String, String>> = listOf(
        "empty" to "kz_90s",
        "empty" to "kz_2005",
        "empty" to "kz_2015",
        "empty" to "kz_2024",
    )

    fun graphs(): List<ScenarioGraph> = combos.map { (characterId, eraId) ->
        ScenarioGraphFactory.forCharacter(characterId, eraId)
    }
}
