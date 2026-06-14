package kz.fearsom.financiallifev2.adminui.graph

import kz.fearsom.financiallifev2.admin.ScenarioGraphDto
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.MONTHLY_TICK

/**
 * Pure, UI-free analysis of a [ScenarioGraphDto]. Computes a layered layout and a
 * set of validation warnings for the read-only scenario viewer.
 *
 * Engine facts this relies on (see GameEngine / ScenarioGraph):
 *  - The narrative entry point is the event whose id is "intro".
 *  - Story `events` are entered two ways: directly via `option.next`, OR after a
 *    MONTHLY_TICK the engine draws from `eventPool` / conditional events. So an
 *    event being unreachable via direct `next` edges is NOT a bug if it is a pool
 *    entry or a scheduled-consequence target — only true orphans are flagged.
 *  - `MONTHLY_TICK` is a sentinel `next`, not a concrete event id.
 *  - Endings are leaf nodes and should carry empty `options`.
 *
 * Kept deliberately framework-free so the logic can be reasoned about and tested
 * independently of Compose.
 */

/** Which visual band a node belongs to: the main narrative tree, or the pool/other grid. */
enum class Band { MAIN, SECONDARY }

/** A laid-out story event. [rank]/[column] are abstract grid coordinates (caller scales to px). */
data class GraphNode(
    val event: GameEvent,
    val rank: Int,
    val column: Int,
    val band: Band,
    val isRoot: Boolean,
    val reachable: Boolean
)

/** A directed edge between two story events (target resolved within `events`). */
data class GraphEdge(
    val fromId: String,
    val toId: String,
    val optionId: String
)

/** A validation finding surfaced in the inspector. */
data class GraphWarning(
    val severity: Severity,
    val eventId: String,
    val message: String
) {
    enum class Severity { ERROR, WARN, INFO }
}

data class ScenarioStats(
    val eventCount: Int,
    val endingCount: Int,
    val optionCount: Int,
    val conditionalCount: Int,
    val poolCount: Int
)

data class ScenarioAnalysis(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val warnings: List<GraphWarning>,
    val rootId: String?,
    val rankCount: Int,
    val columnCount: Int,
    val stats: ScenarioStats
) {
    fun node(id: String): GraphNode? = nodes.firstOrNull { it.event.id == id }
}

private const val SECONDARY_GRID_COLUMNS = 4

fun analyzeScenario(dto: ScenarioGraphDto): ScenarioAnalysis {
    val events = dto.events
    val byId = events.associateBy { it.id }

    if (events.isEmpty()) {
        return ScenarioAnalysis(
            nodes = emptyList(), edges = emptyList(), warnings = emptyList(),
            rootId = null, rankCount = 0, columnCount = 0,
            stats = ScenarioStats(0, 0, 0, dto.conditionalEvents.size, dto.eventPool.size)
        )
    }

    // ── Direct edges (only when the target is a story event) ──────────────────
    val edges = events.flatMap { e ->
        e.options.mapNotNull { o ->
            if (o.next in byId) GraphEdge(e.id, o.next, o.id) else null
        }
    }
    val adjacency: Map<String, List<String>> =
        edges.groupBy { it.fromId }.mapValues { (_, es) -> es.map { it.toId } }

    // ── Root: prefer "intro", else an event with no incoming edge, else first ─
    val targeted = edges.map { it.toId }.toSet()
    val rootId = when {
        "intro" in byId -> "intro"
        else -> events.firstOrNull { it.id !in targeted }?.id ?: events.first().id
    }

    // ── BFS rank from root over direct edges (main narrative tree) ────────────
    val rankOf = HashMap<String, Int>()
    run {
        val queue = ArrayDeque<String>()
        rankOf[rootId] = 0
        queue.add(rootId)
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            val curRank = rankOf.getValue(cur)
            for (next in adjacency[cur].orEmpty()) {
                if (next !in rankOf) {
                    rankOf[next] = curRank + 1
                    queue.add(next)
                }
            }
        }
    }
    val mainReachable = rankOf.keys.toSet()

    // ── Legitimate non-direct entry points: pool entries + scheduled targets ──
    val poolIds = dto.eventPool.map { it.eventId }.filter { it in byId }.toSet()
    val scheduledIds = events
        .flatMap { it.options }
        .mapNotNull { it.effects.scheduleEvent?.eventId }
        .filter { it in byId }
        .toSet()

    // ── Full reachability: BFS from root AND every legitimate non-direct entry,
    // over direct edges. An event reached only via a pool entry (after a tick) is
    // therefore NOT mis-flagged as an orphan. (Validated against a synthetic graph.)
    val fullReachable: Set<String> = run {
        val seen = HashSet<String>()
        val queue = ArrayDeque<String>()
        for (r in (setOf(rootId) + poolIds + scheduledIds)) {
            if (r in byId && seen.add(r)) queue.add(r)
        }
        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            for (next in adjacency[cur].orEmpty()) if (seen.add(next)) queue.add(next)
        }
        seen
    }

    // ── Main-band nodes, grouped by rank, columns by encounter order ──────────
    val mainNodes = ArrayList<GraphNode>()
    val maxMainRank = mainReachable.maxOfOrNull { rankOf.getValue(it) } ?: 0
    for (rank in 0..maxMainRank) {
        val atRank = events.filter { it.id in mainReachable && rankOf.getValue(it.id) == rank }
        atRank.forEachIndexed { col, e ->
            mainNodes += GraphNode(
                event = e, rank = rank, column = col,
                band = Band.MAIN, isRoot = e.id == rootId, reachable = true
            )
        }
    }

    // ── Secondary band: everything not in the main tree, pool entries first ───
    val secondaryEvents = run {
        val notMain = events.filter { it.id !in mainReachable }
        val pooledFirst = notMain.filter { it.id in poolIds }
        val rest = notMain.filter { it.id !in poolIds }
        pooledFirst + rest
    }
    val secondaryStartRank = maxMainRank + 2
    val secondaryNodes = secondaryEvents.mapIndexed { idx, e ->
        val reachable = e.id in fullReachable
        GraphNode(
            event = e,
            rank = secondaryStartRank + idx / SECONDARY_GRID_COLUMNS,
            column = idx % SECONDARY_GRID_COLUMNS,
            band = Band.SECONDARY,
            isRoot = false,
            reachable = reachable
        )
    }

    val nodes = mainNodes + secondaryNodes
    val columnCount = (nodes.maxOfOrNull { it.column } ?: 0) + 1
    val rankCount = (nodes.maxOfOrNull { it.rank } ?: 0) + 1

    // ── Warnings ──────────────────────────────────────────────────────────────
    val warnings = ArrayList<GraphWarning>()
    val knownIds = byId.keys + dto.conditionalEvents.map { it.id }.toSet()

    for (e in events) {
        // Unresolved option targets (could be a shared scam/era-library event — WARN not ERROR)
        for (o in e.options) {
            if (o.next != MONTHLY_TICK && o.next !in knownIds) {
                warnings += GraphWarning(
                    GraphWarning.Severity.WARN, e.id,
                    "option '${o.id}' → '${o.next}' not found in this graph " +
                        "(ok if it is a shared scam/era-library event)"
                )
            }
        }
        // Dead-end: a non-ending event the player cannot leave
        if (!e.isEnding && e.options.isEmpty()) {
            warnings += GraphWarning(
                GraphWarning.Severity.ERROR, e.id,
                "non-ending event has no options — player gets stuck"
            )
        }
        // Ending hygiene
        if (e.isEnding && e.options.isNotEmpty()) {
            warnings += GraphWarning(
                GraphWarning.Severity.WARN, e.id,
                "ending event has ${e.options.size} option(s) — endings should be leaves"
            )
        }
        if (e.isEnding && e.endingType == null) {
            warnings += GraphWarning(
                GraphWarning.Severity.INFO, e.id, "ending has no endingType set"
            )
        }
        // True orphan: unreachable from the root, the event pool, and any scheduled consequence
        if (e.id != rootId && e.id !in fullReachable) {
            warnings += GraphWarning(
                GraphWarning.Severity.WARN, e.id,
                "unreachable: not reachable from '$rootId', not in the event pool, " +
                    "and not a scheduled consequence"
            )
        }
    }

    val stats = ScenarioStats(
        eventCount = events.size,
        endingCount = events.count { it.isEnding },
        optionCount = events.sumOf { it.options.size },
        conditionalCount = dto.conditionalEvents.size,
        poolCount = dto.eventPool.size
    )

    return ScenarioAnalysis(
        nodes = nodes,
        edges = edges,
        warnings = warnings.sortedBy { it.severity.ordinal },
        rootId = rootId,
        rankCount = rankCount,
        columnCount = columnCount,
        stats = stats
    )
}
