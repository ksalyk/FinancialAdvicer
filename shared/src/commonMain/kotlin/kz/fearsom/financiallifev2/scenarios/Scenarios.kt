package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.scenarios.characters.AidarScenarioGraph
import kz.fearsom.financiallifev2.scenarios.characters.AsanScenarioGraph
import kz.fearsom.financiallifev2.scenarios.characters.DanaScenarioGraph
import kz.fearsom.financiallifev2.scenarios.characters.ErbolatScenarioGraph
import kotlin.concurrent.Volatile

// ─── DSL helpers ─────────────────────────────────────────────────────────────

internal fun event(
    id: String,
    message: String,
    flavor: String = "💬",
    priority: Int = 0,
    conditions: List<Condition> = emptyList(),
    tags: Set<String> = emptySet(),
    poolWeight: Int = 10,
    unique: Boolean = false,
    cooldownMonths: Int = 0,
    isEnding: Boolean = false,
    endingType: EndingType? = null,
    options: List<GameOption>
) = GameEvent(
    id, message, flavor, options, conditions, priority, isEnding, endingType,
    tags, poolWeight, unique, cooldownMonths
)

internal fun option(
    id: String,
    text: String,
    emoji: String,
    next: String,
    fx: Effect = Effect()
) = GameOption(id, text, emoji, fx, next)

internal fun cond(field: Condition.Stat.Field, op: Condition.Stat.Op, value: Long) =
    Condition.Stat(field, op, value)

// ─── Abstract ScenarioGraph ───────────────────────────────────────────────────

/**
 * Base class for all character scenario graphs.
 *
 * Structure:
 *   [events]            — fixed narrative story events keyed by id
 *   [conditionalEvents] — injected by engine when PlayerState conditions match
 *   [eventPool]         — weighted pool entries drawn after a monthly tick
 *
 * [findEvent] checks story events → conditional events → scam library → era library.
 * Subclasses override [events], [conditionalEvents], and [eventPool].
 */
abstract class ScenarioGraph {

    abstract val initialPlayerState: PlayerState

    /** Fixed story events — narrative anchors and branches. */
    abstract val events: Map<String, GameEvent>

    /** Priority-checked after each monthly tick (highest priority wins). */
    abstract val conditionalEvents: List<GameEvent>

    /**
     * Weighted pool entries for random event selection after a monthly tick.
     * Include your character-specific events AND ScamEventLibrary.poolEntries.
     */
    abstract val eventPool: List<PoolEntry>

    /**
     * Unified event lookup. Checks (in order):
     *   1. Character story events
     *   2. Character conditional events
     *   3. Scam event library
     *   4. Era event library
     */
    fun findEvent(id: String): GameEvent? =
        events[id]
            ?: conditionalEvents.find { it.id == id }
            ?: ScamEventLibrary.findById(id)
            ?: EraEventLibrary.findById(id)
}

// ─── Factory ──────────────────────────────────────────────────────────────────

object ScenarioGraphFactory {

    /**
     * Copy-on-write cache of immutable graph instances, keyed by "$characterId:$eraId".
     *
     * Why @Volatile + copy-on-write instead of a mutable map with a lock:
     *   - kotlin.concurrent.locks.ReentrantLock lives in stdlib/jvm, not commonMain.
     *   - @Volatile is available in commonMain since Kotlin 1.8.20 and guarantees that
     *     every thread always reads the latest reference.
     *   - Each write replaces the reference with a brand-new immutable map (+), so readers
     *     always see either the old or the new snapshot — never a partially-modified one.
     *   - Worst case under concurrent first-access: two threads both miss, both build the
     *     same deterministic graph, and one write is lost. The next call re-populates it.
     *     No data corruption, no infinite loops — just an extra build on cold start.
     *   - After warmup (≤ 20 character+era combos) every call is a read-only map lookup.
     */
    @Volatile
    private var cache: Map<String, ScenarioGraph> = emptyMap()

    /**
     * Returns the ScenarioGraph for the given character + era.
     * Result is cached — subsequent calls with the same arguments are O(1).
     *
     * Predefined characters → their own graph (era-aware via constructor param).
     * Custom bundles (unknown characterId) → era-based generic graph so the
     * correct historical narrative and event pool are used.
     */
    fun forCharacter(characterId: String, eraId: String): ScenarioGraph {
        val key = "${Strings.currentLocale}:$characterId:$eraId"
        return cache[key] ?: buildGraph(characterId, eraId).also { graph ->
            cache = cache + (key to graph)
        }
    }

    private fun buildGraph(characterId: String, eraId: String): ScenarioGraph = when (characterId) {
        "aidar_90s" -> Aidar90sScenarioGraph()
        "aidar"     -> AidarScenarioGraph(eraId)
        "asan"      -> AsanScenarioGraph()
        "dana"      -> DanaScenarioGraph(eraId)
        "erbolat"   -> ErbolatScenarioGraph(eraId)
        else        -> forEra(eraId)  // bundles: fall back to era-specific graph
    }

    /** Era-based graph used for custom bundle characters. */
    private fun forEra(eraId: String): ScenarioGraph = when (eraId) {
        "kz_90s"  -> Aidar90sScenarioGraph()
        "kz_2005" -> AidarScenarioGraph(eraId)
        "kz_2015" -> AidarScenarioGraph(eraId)
        "kz_2024" -> AidarScenarioGraph(eraId)
        else      -> error("No scenario graph for eraId=$eraId")
    }
}
