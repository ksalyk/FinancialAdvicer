package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kotlin.random.Random

/**
 * Selects the next event from a weighted pool after a monthly tick.
 *
 * Selection pipeline:
 *   1. Filter: conditions pass, not in cooldown, not already triggered
 *   2. Apply weight modifiers: era multipliers + flag-based knowledge modifiers
 *   3. Weighted random sample
 *
 * Flag-based weight reduction (financial education effect):
 *   - "learned.scam.X" flag → that scam category gets 0.15× weight
 *   - financialKnowledge > 50 → all scam events get 0.6× weight
 *   - "lost_money_to_scam" flag → same scam type gets 0.3× weight
 */
object EventPoolSelector {

    fun selectNext(
        pool: List<PoolEntry>,
        allEvents: (String) -> GameEvent?,
        state: PlayerState,
        eraWeightModifiers: Map<String, Float> = emptyMap()
    ): String? {
        val candidates = pool
            .mapNotNull { entry ->
                val event = allEvents(entry.eventId) ?: return@mapNotNull null

                // Pool events are single-use by default. Explicit cooldownMonths makes them repeatable.
                val singleUsePoolEvent = event.unique || event.cooldownMonths <= 0
                if (singleUsePoolEvent && entry.eventId in state.triggeredUniqueEvents) return@mapNotNull null

                // Skip events in cooldown
                val coolsOffAt = state.eventCooldowns[entry.eventId] ?: 0
                if (coolsOffAt > state.absoluteMonth) return@mapNotNull null

                // All conditions must pass
                if (!event.conditions.all { it.check(state) }) return@mapNotNull null

                val weight = computeWeight(entry.baseWeight, event, state, eraWeightModifiers)
                if (weight <= 0) return@mapNotNull null

                entry.eventId to weight
            }

        if (candidates.isEmpty()) return null

        val totalWeight = candidates.sumOf { it.second }
        var roll = Random.nextInt(totalWeight)
        for ((id, weight) in candidates) {
            roll -= weight
            if (roll < 0) return id
        }
        return candidates.last().first
    }

    private fun computeWeight(
        base: Int,
        event: GameEvent,
        state: PlayerState,
        eraModifiers: Map<String, Float>
    ): Int {
        var weight = base.toFloat()

        // Era-specific modifier keyed by event id or tag
        val eraById  = eraModifiers[event.id] ?: 1.0f
        val eraByTag = event.tags.maxOfOrNull { eraModifiers[it] ?: 1.0f } ?: 1.0f
        weight *= maxOf(eraById, eraByTag)

        // Knowledge reduces susceptibility to all scams
        if ("scam" in event.tags && state.financialKnowledge > 50) {
            weight *= 0.6f
        }

        // Each learned scam flag crushes that specific scam subtype
        for (tag in event.tags) {
            if (tag.startsWith("scam.") && "learned.$tag" in state.flags) {
                weight *= 0.15f
            }
        }

        // Having lost money to a scam doubles awareness of the same subtype
        if ("lost_money_to_scam" in state.flags) {
            for (tag in event.tags) {
                if (tag.startsWith("scam.")) weight *= 0.3f
            }
        }

        return weight.toInt().coerceAtLeast(0)
    }
}
