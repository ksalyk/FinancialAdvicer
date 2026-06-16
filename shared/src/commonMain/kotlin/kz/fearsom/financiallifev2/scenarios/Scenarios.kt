package kz.fearsom.financiallifev2.scenarios

import kotlin.concurrent.Volatile
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.CurrencyCode
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry

/** Thrown when no scenario graph exists for a given characterId + eraId combination. */
class ScenarioNotFoundException(characterId: String, eraId: String) :
    NoSuchElementException("No scenario graph for characterId=$characterId eraId=$eraId")

abstract class ScenarioGraph {

    abstract val initialPlayerState: PlayerState

    /** Fixed story events keyed by id. */
    abstract val events: Map<String, GameEvent>

    /** Priority-checked after each monthly tick. */
    abstract val conditionalEvents: List<GameEvent>

    /** Weighted pool entries drawn after a monthly tick. */
    abstract val eventPool: List<PoolEntry>

    fun findEvent(id: String): GameEvent? =
        events[id] ?: conditionalEvents.find { it.id == id }
}

abstract class EmptyEraScenarioGraph(
    eraName: String,
    final override val initialPlayerState: PlayerState
) : ScenarioGraph() {

    final override val events: Map<String, GameEvent> = mapOf(
        "intro" to GameEvent(
            id = "intro",
            message = "Сценарий эпохи $eraName пока пуст.",
            flavor = "📝",
            options = emptyList(),
            isEnding = true
        )
    )

    final override val conditionalEvents: List<GameEvent> = emptyList()
    final override val eventPool: List<PoolEntry> = emptyList()
}

class Kz90sScenarioGraph : EmptyEraScenarioGraph(
    eraName = Strings.eraKz90sName,
    initialPlayerState = PlayerState(
        capital = 25_000_000L,
        income = 7_500_000L,
        expenses = 6_000_000L,
        debt = 0L,
        debtPaymentMonthly = 0L,
        investments = 0L,
        investmentReturnRate = 0.05,
        stress = 55,
        financialKnowledge = 18,
        riskLevel = 35,
        month = 1,
        year = 1993,
        characterId = "era_kz_90s",
        eraId = "kz_90s",
        currency = CurrencyCode.RUB
    )
)

class Kz2005ScenarioGraph : EmptyEraScenarioGraph(
    eraName = Strings.eraKz2005Name,
    initialPlayerState = PlayerState(
        capital = 450_000L,
        income = 170_000L,
        expenses = 105_000L,
        debt = 0L,
        debtPaymentMonthly = 0L,
        investments = 0L,
        investmentReturnRate = 0.07,
        stress = 34,
        financialKnowledge = 24,
        riskLevel = 26,
        month = 1,
        year = 2005,
        characterId = "era_kz_2005",
        eraId = "kz_2005"
    )
)

class Kz2015ScenarioGraph : EmptyEraScenarioGraph(
    eraName = Strings.eraKz2015Name,
    initialPlayerState = PlayerState(
        capital = 620_000L,
        income = 260_000L,
        expenses = 190_000L,
        debt = 0L,
        debtPaymentMonthly = 0L,
        investments = 0L,
        investmentReturnRate = 0.08,
        stress = 42,
        financialKnowledge = 28,
        riskLevel = 18,
        month = 6,
        year = 2015,
        characterId = "era_kz_2015",
        eraId = "kz_2015"
    )
)

class Kz2024ScenarioGraph : EmptyEraScenarioGraph(
    eraName = Strings.eraModernKz2024Name,
    initialPlayerState = PlayerState(
        capital = 260_000L,
        income = 520_000L,
        expenses = 255_000L,
        debt = 180_000L,
        debtPaymentMonthly = 30_000L,
        investments = 0L,
        investmentReturnRate = 0.10,
        stress = 38,
        financialKnowledge = 24,
        riskLevel = 22,
        month = 1,
        year = 2024,
        characterId = "era_kz_2024",
        eraId = "kz_2024"
    )
)

object ScenarioGraphFactory {

    @Volatile
    private var cache: Map<String, ScenarioGraph> = emptyMap()

    /**
     * Resolves the graph for a character + era. Authored character graphs take
     * priority (matched by [characterId]); any other character falls back to the
     * still-empty era shell. Results are cached per locale + character + era.
     */
    fun forCharacter(characterId: String, eraId: String): ScenarioGraph {
        val key = "${Strings.currentLocale}:$characterId:$eraId"
        return cache[key] ?: buildGraph(characterId, eraId).also { graph ->
            cache = cache + (key to graph)
        }
    }

    /** Authored characters first; the reference graph is [DaniyarScenarioGraph]. */
    private fun buildGraph(characterId: String, eraId: String): ScenarioGraph = when (characterId) {
        "daniyar_90s" -> DaniyarScenarioGraph()
        else -> forEra(eraId)
    }

    private fun forEra(eraId: String): ScenarioGraph = when (eraId) {
        "kz_90s" -> Kz90sScenarioGraph()
        "kz_2005" -> Kz2005ScenarioGraph()
        "kz_2015" -> Kz2015ScenarioGraph()
        "kz_2024", "modern_kz_2024" -> Kz2024ScenarioGraph()
        else -> throw ScenarioNotFoundException("bundle", eraId)
    }
}
