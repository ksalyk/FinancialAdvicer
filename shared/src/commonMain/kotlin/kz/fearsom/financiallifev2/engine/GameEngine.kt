package kz.fearsom.financiallifev2.engine

import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Core game FSM — 3-layer architecture:
 *
 * 1. NARRATIVE GRAPH  — events + options + branches, convergent paths
 * 2. STATE SYSTEM     — PlayerState (capital, income, debt, stress, knowledge, risk)
 * 3. ECONOMIC SIM     — monthly tick: income − expenses − debt + investments ± random
 *
 * Turn flow:
 *   makeChoice(optionId)
 *     → applyEffects(option.effects)
 *     → if option.next == MONTHLY_TICK:
 *         runMonthlyTick() → emit MonthlyReport message
 *         findConditionalEvent() → inject if conditions match
 *         else → pick next story event
 *     → else: navigate directly to option.next
 *     → emit new GameState via StateFlow
 */
class GameEngine(private val graph: ScenarioGraph = ScenarioGraph()) {

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
        val intro = graph.events["intro"] ?: error("No 'intro' event in graph")
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
     * Process a player choice. Called from GamePresenter (which adds typing delays).
     * Handles MONTHLY_TICK sentinel, conditional event injection, and endings.
     */
    fun makeChoice(optionId: String): GameState {
        val current = _state.value ?: return startGame()
        val event   = graph.findEvent(current.currentEventId) ?: return current
        val option  = event.options.find { it.id == optionId } ?: return current

        var ps = applyEffects(current.playerState, option.effects)
        val newMessages = mutableListOf(playerMsg(option))

        val nextEventId: String

        if (option.next == MONTHLY_TICK) {
            // ── Layer 3: monthly economic simulation ─────────────────────
            val (updatedPs, report) = monthlyTick(ps)
            ps = updatedPs
            newMessages += monthlyReportMsg(report)

            // ── Conditional event injection ──────────────────────────────
            val conditional = findConditionalEvent(ps, current.currentEventId)
            if (conditional != null) {
                newMessages += systemMsg("⚡ Внеплановое событие!")
                newMessages += characterMsg(conditional, ps)
                nextEventId = conditional.id
            } else {
                // Rotate through the after-tick story pool
                val pool = graph.afterTickEventPool.filter { it != current.currentEventId }
                nextEventId = pool.randomOrNull() ?: "normal_life"
                graph.findEvent(nextEventId)?.let { newMessages += characterMsg(it, ps) }
            }
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
        val investGain = ps.monthlyInvestmentReturn
        val netFlow    = ps.income - ps.expenses - ps.debtPaymentMonthly + investGain
        val capitalAfter = (ps.capital + netFlow).coerceAtLeast(0L)

        // 30% of monthly payment goes to principal
        val principalPaid = (ps.debtPaymentMonthly * 0.30).toLong()
        val debtAfter = (ps.debt - principalPaid).coerceAtLeast(0L)

        // Stress dynamics
        val rawDelta = when {
            capitalAfter == 0L                -> 15  // bankrupt stress spike
            netFlow < 0                       ->  6  // negative cash flow
            ps.debt > ps.income * 3           ->  3  // heavy debt burden
            netFlow > ps.expenses             -> -3  // comfortable surplus
            capitalAfter > ps.income * 6      -> -2  // 6-month cushion
            else                              ->  0
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

    // ── Conditional event injection ───────────────────────────────────────────

    /**
     * Returns the highest-priority conditional event whose ALL conditions are
     * satisfied by the current [PlayerState]. Skips the event already shown.
     */
    private fun findConditionalEvent(ps: PlayerState, excludeId: String): GameEvent? =
        graph.conditionalEvents
            .filter { it.id != excludeId && it.conditions.isNotEmpty() }
            .sortedByDescending { it.priority }
            .firstOrNull { event -> event.conditions.all { it.check(ps) } }

    // ── Effect application ────────────────────────────────────────────────────

    private fun applyEffects(ps: PlayerState, e: Effect): PlayerState = ps.copy(
        capital            = (ps.capital            + e.capitalDelta).coerceAtLeast(0L),
        income             = (ps.income             + e.incomeDelta).coerceAtLeast(0L),
        expenses           = (ps.expenses           + e.expensesDelta).coerceAtLeast(0L),
        debt               = (ps.debt               + e.debtDelta).coerceAtLeast(0L),
        debtPaymentMonthly = (ps.debtPaymentMonthly + e.debtPaymentDelta).coerceAtLeast(0L),
        investments        = (ps.investments        + e.investmentsDelta).coerceAtLeast(0L),
        stress             = (ps.stress             + e.stressDelta).coerceIn(0, 100),
        financialKnowledge = (ps.financialKnowledge + e.knowledgeDelta).coerceIn(0, 100),
        riskLevel          = (ps.riskLevel          + e.riskDelta).coerceIn(0, 100)
    )

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

    private fun playerMsg(option: GameOption) = ChatMessage(
        id     = "player_${option.id}_${ts()}",
        sender = MessageSender.PLAYER,
        text   = "${option.emoji} ${option.text}",
        emoji  = option.emoji
    )

    private fun systemMsg(text: String) = ChatMessage(
        id     = "sys_${ts()}",
        sender = MessageSender.SYSTEM,
        text   = text
    )

    private fun monthlyReportMsg(report: MonthlyReport) = ChatMessage(
        id     = "report_${report.year}_${report.month}_${ts()}",
        sender = MessageSender.MONTHLY_REPORT,
        text   = report.toMessage(),
        emoji  = "📊"
    )

    // Monotonically increasing sequence — no JVM APIs, safe in commonMain.
    private var msgSeq = 0
    private fun ts() = (++msgSeq).toString()
}
