package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.GameOption

/**
 * Authoring DSL for scenario graphs — the reference layer used by every character.
 *
 * Goals:
 *  - keep scenario files readable (no hand-rolled data-class construction);
 *  - encode the engine contract that AI/humans get wrong (endings = `emptyList()`,
 *    story events are `unique`, `next` is `MONTHLY_TICK` or a literal id);
 *  - compose large graphs out of small, named [EventArc]s via [buildEvents].
 *
 * Used by `DaniyarScenarioGraph` (the first authored graph). New characters should
 * follow the same shape: a handful of arc builders flattened into `events`, plus
 * conditional + ending arcs for `conditionalEvents`.
 */

// ── Text ────────────────────────────────────────────────────────────────────

/** Joins paragraphs with a blank line — the shipped multi-paragraph "story" style. */
fun story(vararg paragraphs: String): String = paragraphs.joinToString("\n\n")

// ── Conditions ────────────────────────────────────────────────────────────────

/** Terse numeric condition, e.g. `cond(CAPITAL, LTE, 0L)`. Value is always `Long`. */
fun cond(field: Condition.Stat.Field, op: Condition.Stat.Op, value: Long): Condition.Stat =
    Condition.Stat(field, op, value)

// ── Options ───────────────────────────────────────────────────────────────────

/**
 * A player choice. [next] is either [kz.fearsom.financiallifev2.model.MONTHLY_TICK]
 * (advance time and let the priority queue pick) or a literal event id (immediate branch).
 * [fx] defaults to a no-op effect.
 */
fun option(
    id: String,
    text: String,
    emoji: String,
    next: String,
    fx: Effect = Effect(),
): GameOption = GameOption(id = id, text = text, emoji = emoji, effects = fx, next = next)

// ── Events ──────────────────────────────────────────────────────────────────

/**
 * A narrative or pool/conditional event.
 *
 * NOTE on [unique]: defaults to `false` so pool/conditional events repeat correctly.
 * Scripted story + backbone beats must pass `unique = true` (fire once per session).
 * Endings must use [ending] instead — they are terminal with no options.
 */
fun event(
    id: String,
    message: String,
    options: List<GameOption>,
    flavor: String = "💬",
    priority: Int = 0,
    conditions: List<Condition> = emptyList(),
    tags: Set<String> = emptySet(),
    poolWeight: Int = 10,
    unique: Boolean = false,
    cooldownMonths: Int = 0,
    maxOccurrences: Int = 0,
    schemeExplanation: String? = null,
): GameEvent = GameEvent(
    id = id,
    message = message,
    flavor = flavor,
    options = options,
    conditions = conditions,
    priority = priority,
    isEnding = false,
    endingType = null,
    tags = tags,
    poolWeight = poolWeight,
    unique = unique,
    cooldownMonths = cooldownMonths,
    schemeExplanation = schemeExplanation,
    maxOccurrences = maxOccurrences,
)

/**
 * A terminal ending node: `options = emptyList()`, `isEnding = true`, [endingType] set.
 * When used as a conditional ending (with [conditions] + [priority]) it both *triggers*
 * and *resolves* in one node — verified to pass `ScenarioGraphContentTest` and the
 * simulation/reachability harness.
 */
fun ending(
    id: String,
    message: String,
    endingType: EndingType,
    flavor: String,
    priority: Int = 0,
    conditions: List<Condition> = emptyList(),
): GameEvent = GameEvent(
    id = id,
    message = message,
    flavor = flavor,
    options = emptyList(),
    conditions = conditions,
    priority = priority,
    isEnding = true,
    endingType = endingType,
)

// ── Arc composition ───────────────────────────────────────────────────────────

/** A named bundle of events. Lets a graph be assembled from readable chapters. */
class EventArc(val name: String, val events: List<GameEvent>)

/** Build an [EventArc] from inline events. */
fun arc(name: String, vararg events: GameEvent): EventArc = EventArc(name, events.toList())

/**
 * Flatten arcs into the `events` map the engine consumes, failing loudly on duplicate
 * ids (a common authoring mistake the static tests would otherwise surface late).
 */
fun List<EventArc>.buildEvents(): Map<String, GameEvent> = buildMap {
    for (a in this@buildEvents) {
        for (e in a.events) {
            require(e.id !in this) { "Duplicate event id '${e.id}' (arc '${a.name}')" }
            put(e.id, e)
        }
    }
}

/** Flatten arcs into a flat event list — for `conditionalEvents`. */
fun List<EventArc>.flattenEvents(): List<GameEvent> = flatMap { it.events }
