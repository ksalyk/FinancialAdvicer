package kz.fearsom.financiallifev2.engine

import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.scenarios.AsanScenarioGraph
import kz.fearsom.financiallifev2.scenarios.EraDefinition
import kz.fearsom.financiallifev2.scenarios.EraRegistry
import kz.fearsom.financiallifev2.scenarios.EventPoolSelector
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.ScenarioGraphFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * Core game FSM — 3-layer architecture:
 *
 * 1. NARRATIVE GRAPH  — events + options + branches, convergent paths
 * 2. STATE SYSTEM     — PlayerState (capital, income, debt, stress, knowledge, risk, flags)
 * 3. ECONOMIC SIM     — monthly tick: income − expenses − debt + investments ± random
 *
 * Turn flow:
 *   makeChoice(optionId)
 *     → applyEffects(option.effects)      ← applies deltas AND flag mutations
 *     → scheduleEvent if present          ← queues deferred consequences
 *     → if option.next == MONTHLY_TICK:
 *         runMonthlyTick()                ← economic simulation
 *         PRIORITY 1: era global event    ← world crises on specific dates
 *         PRIORITY 2: deferred scheduled  ← consequences from prior choices
 *         PRIORITY 3: conditional event   ← state-triggered (debt crisis, burnout…)
 *         PRIORITY 4: weighted pool       ← scam events, career events, normal life
 *     → else: navigate directly to option.next
 *     → emit new GameState via StateFlow
 */
class GameEngine(
    private val graph: ScenarioGraph = AsanScenarioGraph(),
    private val eraDefinition: EraDefinition? = null
) {

    private val _state = MutableStateFlow<GameState?>(null)
    val state: StateFlow<GameState?> = _state.asStateFlow()

    // Tracks the current character name for template substitution in event messages
    private var currentCharacterName: String = ""

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Start a new game. If [initialState] is provided (character-selection flow),
     * it overrides the default [ScenarioGraph.initialPlayerState].
     * If [characterName] is provided, it is used in messages and template substitution.
     */
    fun startGame(
        initialState: PlayerState? = null,
        characterName: String = "Асан"
    ): GameState {
        currentCharacterName = characterName
        val ps    = initialState ?: graph.initialPlayerState
        val intro = graph.findEvent("intro") ?: error("No 'intro' event in graph")
        val personalizedIntro = intro.copy(message = buildIntroMessage(characterName, ps))
        return GameState(
            playerState        = ps,
            currentEventId     = intro.id,
            messages           = listOf(
                systemMsg("🎮 Финансовое приключение началось! Помогай $characterName строить финансовое будущее."),
                characterMsg(personalizedIntro, ps)
            ),
            isWaitingForChoice = true
        ).also { _state.value = it }
    }

    /**
     * Create a GameEngine wired to the correct ScenarioGraph and EraDefinition
     * for the given character + era combination.
     */
    companion object {
        fun forCharacterAndEra(characterId: String, eraId: String): GameEngine {
            val graph = ScenarioGraphFactory.forCharacter(characterId, eraId)
            val era   = EraRegistry.findById(eraId)
            return GameEngine(graph, era)
        }
    }

    private fun buildIntroMessage(characterName: String, ps: PlayerState): String = buildString {
        appendLine("Привет! 👋 Я $characterName.")
        appendLine()
        appendLine("Зарплата: ${ps.income.moneyFormat()}/мес")
        appendLine("Расходы: ${ps.expenses.moneyFormat()}/мес")
        if (ps.debt > 0) {
            appendLine("Долг: ${ps.debt.moneyFormat()} (−${ps.debtPaymentMonthly.moneyFormat()}/мес)")
        }
        appendLine("Накопления: ${ps.capital.moneyFormat()}")
        appendLine()
        append("Звонит друг — зовёт вложиться в крипту. Говорит, можно x2 за месяц. Что делаешь?")
    }.trimEnd()

    /**
     * Process a player choice.
     * Handles MONTHLY_TICK sentinel, the full 4-tier event priority queue, and endings.
     */
    fun makeChoice(optionId: String): GameState {
        val current = _state.value ?: return startGame()
        val event   = graph.findEvent(current.currentEventId) ?: return current
        val option  = event.options.find { it.id == optionId } ?: return current

        // Apply stat effects and flag mutations
        var ps = applyEffects(current.playerState, option.effects)

        // Queue deferred consequence if the option requests one
        option.effects.scheduleEvent?.let { scheduled ->
            ps = addScheduledEvent(ps, scheduled)
        }

        val newMessages = mutableListOf(playerMsg(option))
        val nextEventId: String

        if (option.next == MONTHLY_TICK) {
            // ── Layer 3: monthly economic simulation ─────────────────────
            val (updatedPs, report) = monthlyTick(ps)
            ps = updatedPs
            newMessages += monthlyReportMsg(report)


            // ── 4-tier event priority queue ───────────────────────────────

            // PRIORITY 1: Era-scheduled global event (world crisis on this date)
            val eraEvent = findEraEvent(ps)

            // PRIORITY 2: Deferred consequence scheduled by a previous choice
            val scheduled = findScheduledEvent(ps)
                ?.also { ps = removeScheduledEvent(ps, it) }

            // PRIORITY 3: Conditional event (debt crisis, burnout, mortgage unlock…)
            val conditional = if (eraEvent == null && scheduled == null)
                findConditionalEvent(ps, current.currentEventId)
            else null

            // PRIORITY 4: Weighted pool (scam events, career, normal life)
            val poolPick = if (eraEvent == null && scheduled == null && conditional == null)
                EventPoolSelector.selectNext(
                    pool                = graph.eventPool,
                    allEvents           = graph::findEvent,
                    state               = ps,
                    eraWeightModifiers  = eraDefinition?.poolWeightModifiers ?: emptyMap()
                )
            else null

            val nextEvent = eraEvent ?: scheduled ?: conditional
            nextEventId   = nextEvent?.id ?: poolPick ?: "normal_life"

            // Mark unique events triggered
            val winner = graph.findEvent(nextEventId)
            if (winner?.unique == true) {
                ps = ps.copy(triggeredUniqueEvents = ps.triggeredUniqueEvents + nextEventId)
            }

            // Apply cooldown
            if (winner != null && winner.cooldownMonths > 0) {
                ps = ps.copy(
                    eventCooldowns = ps.eventCooldowns +
                        (nextEventId to ps.absoluteMonth + winner.cooldownMonths)
                )
            }

            // Mark era event triggered so it doesn't fire again
            if (eraEvent != null) {
                ps = ps.copy(triggeredUniqueEvents = ps.triggeredUniqueEvents + nextEventId)
            }

            graph.findEvent(nextEventId)?.let { newMessages += characterMsg(it, ps) }

        } else {
            // ── Direct graph navigation ──────────────────────────────────
            nextEventId = option.next
            graph.findEvent(nextEventId)?.let { newMessages += characterMsg(it, ps) }
        }

        val nextEvent = graph.findEvent(nextEventId)
        val ending    = nextEvent?.isEnding == true

        return current.copy(
            playerState        = ps,
            currentEventId     = nextEventId,
            messages           = current.messages + newMessages,
            isWaitingForChoice = !ending,
            gameOver           = ending,
            endingType         = nextEvent?.endingType
        ).also { _state.value = it }
    }

    /** Rehydrate engine from a previously serialized [GameState] (e.g., loaded from DB). */
    fun loadState(state: GameState, characterName: String = "") {
        if (characterName.isNotEmpty()) currentCharacterName = characterName
        _state.value = state
    }

    fun currentOptions(): List<GameOption> {
        val id = _state.value?.currentEventId ?: return emptyList()
        return graph.findEvent(id)?.options ?: emptyList()
    }

    fun reset() { _state.value = null }

    // ── Layer 3: Monthly Economic Simulator ───────────────────────────────────

    private fun monthlyTick(ps: PlayerState): Pair<PlayerState, MonthlyReport> {
        val investGain  = ps.monthlyInvestmentReturn
        val netFlow     = ps.income - ps.expenses - ps.debtPaymentMonthly + investGain
        val capitalAfter = (ps.capital + netFlow).coerceAtLeast(0L)

        // 30% of monthly payment reduces principal
        val principalPaid = (ps.debtPaymentMonthly * 0.30).toLong()
        val debtAfter     = (ps.debt - principalPaid).coerceAtLeast(0L)

        // Stress dynamics
        val rawDelta = when {
            capitalAfter == 0L           -> 15   // bankrupt stress spike
            netFlow < 0                  ->  6   // negative cash flow
            ps.debt > ps.income * 3      ->  3   // heavy debt burden
            netFlow > ps.expenses        -> -3   // comfortable surplus
            capitalAfter > ps.income * 6 -> -2   // 6-month cushion
            else                         ->  0
        }
        // Each 25 points of knowledge absorbs 1 stress point/month
        val stressDelta = rawDelta - (ps.financialKnowledge / 25)

        val report = MonthlyReport(
            month          = ps.month,
            year           = ps.year,
            incomeReceived = ps.income,
            expensesPaid   = ps.expenses,
            debtPayment    = ps.debtPaymentMonthly,
            investmentGain = investGain,
            netFlow        = netFlow,
            capitalBefore  = ps.capital,
            capitalAfter   = capitalAfter,
            debtAfter      = debtAfter,
            stressDelta    = stressDelta
        )

        val next = ps.copy(
            capital  = capitalAfter,
            debt     = debtAfter,
            stress   = (ps.stress + stressDelta).coerceIn(0, 100),
            month    = if (ps.month == 12) 1 else ps.month + 1,
            year     = if (ps.month == 12) ps.year + 1 else ps.year
        )

        return next to report
    }

    // ── Priority 1: Era global events ─────────────────────────────────────────

    private fun findEraEvent(ps: PlayerState): GameEvent? {
        val era = eraDefinition ?: return null
        return era.globalEvents
            .filter { it.year == ps.year && it.month == ps.month }
            .filter { it.eventId !in ps.triggeredUniqueEvents }
            .filter { it.probability >= 1.0f || Random.nextFloat() < it.probability }
            .firstNotNullOfOrNull { graph.findEvent(it.eventId) }
    }

    // ── Priority 2: Deferred scheduled events ────────────────────────────────

    private fun findScheduledEvent(ps: PlayerState): GameEvent? =
        ps.pendingScheduled
            .filter { it.fireAtYear == ps.year && it.fireAtMonth == ps.month }
            .firstNotNullOfOrNull { graph.findEvent(it.eventId) }

    private fun addScheduledEvent(ps: PlayerState, scheduled: ScheduledEvent): PlayerState {
        val totalMonths = ps.year * 12 + ps.month + scheduled.afterMonths
        val targetYear  = (totalMonths - 1) / 12
        val targetMonth = ((totalMonths - 1) % 12) + 1
        return ps.copy(
            pendingScheduled = ps.pendingScheduled + PendingEvent(
                eventId    = scheduled.eventId,
                fireAtYear  = targetYear,
                fireAtMonth = targetMonth
            )
        )
    }

    private fun removeScheduledEvent(ps: PlayerState, event: GameEvent): PlayerState =
        ps.copy(
            pendingScheduled = ps.pendingScheduled.filter {
                !(it.fireAtYear == ps.year && it.fireAtMonth == ps.month && it.eventId == event.id)
            }
        )

    // ── Priority 3: Conditional event injection ──────────────────────────────

    private fun findConditionalEvent(ps: PlayerState, excludeId: String): GameEvent? =
        graph.conditionalEvents
            .filter { it.id != excludeId && it.conditions.isNotEmpty() }
            .filter { it.id !in ps.triggeredUniqueEvents || !it.unique }
            .sortedByDescending { it.priority }
            .firstOrNull { event -> event.conditions.all { it.check(ps) } }

    // ── Effect application ────────────────────────────────────────────────────

    private fun applyEffects(ps: PlayerState, e: Effect): PlayerState {
        val newFlags = (ps.flags + e.setFlags) - e.clearFlags
        return ps.copy(
            capital            = (ps.capital            + e.capitalDelta).coerceAtLeast(0L),
            income             = (ps.income             + e.incomeDelta).coerceAtLeast(0L),
            expenses           = (ps.expenses           + e.expensesDelta).coerceAtLeast(0L),
            debt               = (ps.debt               + e.debtDelta).coerceAtLeast(0L),
            debtPaymentMonthly = (ps.debtPaymentMonthly + e.debtPaymentDelta).coerceAtLeast(0L),
            investments        = (ps.investments        + e.investmentsDelta).coerceAtLeast(0L),
            stress             = (ps.stress             + e.stressDelta).coerceIn(0, 100),
            financialKnowledge = (ps.financialKnowledge + e.knowledgeDelta).coerceIn(0, 100),
            riskLevel          = (ps.riskLevel          + e.riskDelta).coerceIn(0, 100),
            flags              = newFlags
        )
    }

    // ── Message factories ─────────────────────────────────────────────────────

    /**
     * Create a chat message for a character event, substituting {token} placeholders
     * with actual values from the current [PlayerState].
     */
    private fun characterMsg(event: GameEvent, ps: PlayerState) = ChatMessage(
        id     = "char_${event.id}_${ts()}",
        sender = MessageSender.CHARACTER,
        text   = substituteTemplate(event.message, ps),
        emoji  = event.flavor
    )

    /**
     * Replaces {token} placeholders in event messages with live PlayerState values.
     *
     * Supported tokens:
     *   {income}       — monthly income
     *   {expenses}     — monthly fixed expenses
     *   {capital}      — current savings/capital
     *   {debt}         — total outstanding debt
     *   {debtPayment}  — monthly debt repayment
     *   {investments}  — total portfolio value
     *   {passiveIncome}— monthly investment return
     *   {netFlow}      — net monthly cash flow
     *   {income3x}     — 3× income (emergency fund target)
     *   {name}         — character name
     */
    private fun substituteTemplate(text: String, ps: PlayerState): String =
        text
            .replace("{income}",        ps.income.moneyFormat())
            .replace("{expenses}",      ps.expenses.moneyFormat())
            .replace("{capital}",       ps.capital.moneyFormat())
            .replace("{debt}",          ps.debt.moneyFormat())
            .replace("{debtPayment}",   ps.debtPaymentMonthly.moneyFormat())
            .replace("{investments}",   ps.investments.moneyFormat())
            .replace("{passiveIncome}", ps.monthlyInvestmentReturn.moneyFormat())
            .replace("{netFlow}",       ps.netMonthlyFlow.moneyFormat())
            .replace("{income3x}",      (ps.income * 3).moneyFormat())
            .replace("{name}",          currentCharacterName)

    fun playerMsg(option: GameOption) = ChatMessage(
        id     = "player_${option.id}_${ts()}",
        sender = MessageSender.PLAYER,
        text   = "${option.emoji} ${option.text}",
        emoji  = option.emoji
    )

    fun systemMsg(text: String) = ChatMessage(
        id     = "sys_${ts()}",
        sender = MessageSender.SYSTEM,
        text   = text
    )

    fun monthlyReportMsg(report: MonthlyReport) = ChatMessage(
        id     = "report_${report.year}_${report.month}_${ts()}",
        sender = MessageSender.MONTHLY_REPORT,
        text   = report.toMessage(),
        emoji  = "📊"
    )

    private var msgSeq = 0
    private fun ts() = (++msgSeq).toString()
}
