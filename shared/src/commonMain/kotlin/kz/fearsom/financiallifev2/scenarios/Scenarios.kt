package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.CAPITAL
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.DEBT
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.KNOWLEDGE
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.MONTH
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.STRESS
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GT
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GTE
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.LTE
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.moneyFormat
import kz.fearsom.financiallifev2.scenarios.characters.AidarScenarioGraph
import kz.fearsom.financiallifev2.scenarios.characters.AsanScenarioGraph
import kz.fearsom.financiallifev2.scenarios.characters.DanaScenarioGraph
import kz.fearsom.financiallifev2.scenarios.characters.ErbolatScenarioGraph

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
     * Returns the ScenarioGraph for the given character + era.
     *
     * Predefined characters → their own graph (era-aware via constructor param).
     * Custom bundles (unknown characterId) → era-based generic graph so the
     * correct historical narrative and event pool are used.
     */
    fun forCharacter(characterId: String, eraId: String): ScenarioGraph = when (characterId) {
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

