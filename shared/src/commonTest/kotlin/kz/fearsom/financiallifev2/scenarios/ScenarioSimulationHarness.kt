package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.GameState
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kotlin.random.Random

/** A single invariant violation found while validating a scenario. */
data class SimViolation(val combo: String, val kind: String, val detail: String) {
    override fun toString() = "[$combo] $kind: $detail"
}

/** Outcome of [ScenarioSimulationHarness.simulate]. */
data class SimReport(
    val combo: String,
    val playthroughs: Int,
    val endingsReached: Int,
    val violations: List<SimViolation>,
)

/** Outcome of [ScenarioSimulationHarness.validateReachability]. */
data class ReachReport(
    val combo: String,
    val danglingRefs: List<SimViolation>,
    val allEndings: List<String>,
    val reachableEndings: List<String>,
    val unreachableEndings: List<String>,
)

/**
 * Deterministic, engine-driven verification for scenario graphs.
 *
 * [simulate] plays a graph to many endings with random choices (seeded RNG) and
 * asserts liveness invariants: no crash, no dead-end, stats in bounds.
 *
 * [validateReachability] statically proves every reference resolves and every
 * ending is reachable from `intro`, modelling the engine's MONTHLY_TICK fan-out
 * (era globals + deferred + conditionals + weighted pool, with the hard-coded
 * `normal_life` fallback) as edges.
 */
object ScenarioSimulationHarness {

    fun simulate(
        characterId: String,
        eraId: String,
        seeds: Int = 40,
        maxStepsPerRun: Int = 800,
    ): SimReport {
        val combo = "$characterId/$eraId"
        val graph = ScenarioGraphFactory.forCharacter(characterId, eraId)
        val era = EraRegistry.findById(eraId)
        val violations = mutableListOf<SimViolation>()
        var endingsReached = 0

        for (seed in 0 until seeds) {
            val rng = Random(seed)
            val engine = GameEngine(graph, era, rng)

            var state: GameState = try {
                engine.startGame(characterName = "Sim")
            } catch (t: Throwable) {
                violations += SimViolation(combo, "START_CRASH", "seed=$seed ${t::class.simpleName}: ${t.message}")
                continue
            }

            var steps = 0
            while (!state.gameOver && steps < maxStepsPerRun) {
                checkInvariants(combo, seed, steps, state, violations)

                val options = graph.findEvent(state.currentEventId)?.options.orEmpty()
                if (options.isEmpty()) {
                    violations += SimViolation(
                        combo, "DEAD_END",
                        "seed=$seed step=$steps event='${state.currentEventId}' has no options but game is not over",
                    )
                    break
                }

                val option = options[rng.nextInt(options.size)]
                state = try {
                    engine.makeChoice(option.id)
                } catch (t: Throwable) {
                    violations += SimViolation(
                        combo, "CHOICE_CRASH",
                        "seed=$seed step=$steps event='${state.currentEventId}' option='${option.id}' " +
                            "${t::class.simpleName}: ${t.message}",
                    )
                    break
                }
                steps++
            }

            if (state.gameOver) {
                endingsReached++
                checkInvariants(combo, seed, steps, state, violations)
            }
        }
        return SimReport(combo, seeds, endingsReached, violations)
    }

    private fun checkInvariants(
        combo: String,
        seed: Int,
        step: Int,
        state: GameState,
        out: MutableList<SimViolation>,
    ) {
        val ps = state.playerState
        fun bad(field: String, value: Long) = out.add(
            SimViolation(
                combo, "STAT_OUT_OF_BOUNDS",
                "seed=$seed step=$step $field=$value event='${state.currentEventId}'",
            ),
        )
        if (ps.capital < 0) bad("capital", ps.capital)
        if (ps.income < 0) bad("income", ps.income)
        if (ps.expenses < 0) bad("expenses", ps.expenses)
        if (ps.debt < 0) bad("debt", ps.debt)
        if (ps.investments < 0) bad("investments", ps.investments)
        if (ps.stress !in 0..100) bad("stress", ps.stress.toLong())
        if (ps.financialKnowledge !in 0..100) bad("knowledge", ps.financialKnowledge.toLong())
        if (ps.riskLevel !in 0..100) bad("risk", ps.riskLevel.toLong())
    }

    fun validateReachability(characterId: String, eraId: String): ReachReport {
        val combo = "$characterId/$eraId"
        val graph = ScenarioGraphFactory.forCharacter(characterId, eraId)
        val era = EraRegistry.findById(eraId)
        val dangling = mutableListOf<SimViolation>()

        val conditionalIds = graph.conditionalEvents.map { it.id }
        val poolIds = graph.eventPool.map { it.eventId }
        val eraIds = era?.globalEvents?.map { it.eventId }.orEmpty()

        fun mustResolve(id: String, src: String) {
            if (id != MONTHLY_TICK && graph.findEvent(id) == null) {
                dangling += SimViolation(combo, "DANGLING_REF", "$src -> '$id' does not resolve")
            }
        }

        val authored = graph.events.values + graph.conditionalEvents
        authored.forEach { ev ->
            ev.options.forEach { opt ->
                mustResolve(opt.next, "option '${ev.id}/${opt.id}'.next")
                opt.effects.scheduleEvent?.let { mustResolve(it.eventId, "option '${ev.id}/${opt.id}'.scheduleEvent") }
            }
        }
        poolIds.forEach { mustResolve(it, "eventPool entry") }
        conditionalIds.forEach { mustResolve(it, "conditionalEvents id") }
        eraIds.forEach { mustResolve(it, "era globalEvent") }

        // Anything the engine can route to after a MONTHLY_TICK.
        val tickClosure = (conditionalIds + poolIds + eraIds + "normal_life").distinct()

        fun edgesOf(ev: GameEvent): Set<String> {
            val out = mutableSetOf<String>()
            var hasTick = false
            ev.options.forEach { opt ->
                if (opt.next == MONTHLY_TICK) hasTick = true else out += opt.next
                opt.effects.scheduleEvent?.let { out += it.eventId }
            }
            if (hasTick) out += tickClosure
            return out.filter { it != MONTHLY_TICK }.toSet()
        }

        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        if (graph.findEvent("intro") != null) {
            visited += "intro"
            queue.addLast("intro")
        }
        while (queue.isNotEmpty()) {
            val ev = graph.findEvent(queue.removeFirst()) ?: continue
            for (next in edgesOf(ev)) {
                if (graph.findEvent(next) != null && visited.add(next)) queue.addLast(next)
            }
        }

        val allKnownIds = (graph.events.keys + conditionalIds + poolIds + eraIds + tickClosure).distinct()
        val allEndings = allKnownIds
            .mapNotNull { graph.findEvent(it) }
            .filter { it.isEnding }
            .map { it.id }
            .distinct()
        val reachableEndings = allEndings.filter { it in visited }
        val unreachableEndings = allEndings.filterNot { it in visited }

        return ReachReport(combo, dangling, allEndings, reachableEndings, unreachableEndings)
    }
}
