package kz.fearsom.financiallifev2.scenarios.arcs

import kz.fearsom.financiallifev2.model.GameEvent

/**
 * A composable unit of events. Call [buildInto] to merge this arc's events
 * into an existing mutable map, or use [List<EventArc>.buildEvents] to assemble
 * a complete events map from multiple arcs.
 *
 * Usage:
 *   override val events = listOf(
 *       myMainArc(eraId, amounts),
 *       myPoolArc(),
 *       myEndingsArc()
 *   ).buildEvents()
 */
fun interface EventArc {
    fun buildInto(map: MutableMap<String, GameEvent>)
}

fun List<EventArc>.buildEvents(): Map<String, GameEvent> = buildMap {
    for (arc in this@buildEvents) arc.buildInto(this)
}

/** "kz_2005" → "2005", "kz_2015" → "2015", "kz_2024" → "2024" */
internal fun String.eraLabel(): String = removePrefix("kz_")
