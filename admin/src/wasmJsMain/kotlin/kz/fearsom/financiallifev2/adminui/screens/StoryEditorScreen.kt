package kz.fearsom.financiallifev2.adminui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.admin.ScenarioGraphDto
import kz.fearsom.financiallifev2.admin.StoryDetail
import kz.fearsom.financiallifev2.admin.UpsertStoryRequest
import kz.fearsom.financiallifev2.adminui.components.FormField
import kz.fearsom.financiallifev2.adminui.components.ScenarioCanvas
import kz.fearsom.financiallifev2.adminui.components.rememberScenarioCanvasState
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient
import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.CurrencyCode
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.ScheduledEvent
import kz.fearsom.financiallifev2.scenarios.analysis.GraphWarning
import kz.fearsom.financiallifev2.scenarios.analysis.analyzeScenario

// ════════════════════════════════════════════════════════════════════════════
//  DRAFT MODELS — string-backed so every field maps 1:1 to a text input
// ════════════════════════════════════════════════════════════════════════════

private data class ScheduleDraft(val eventId: String = "", val months: String = "1")

private data class OptionDraft(
    val id: String          = "",
    val text: String        = "",
    val emoji: String       = "👉",
    val next: String        = MONTHLY_TICK,
    val capital: String     = "0",
    val income: String      = "0",
    val expenses: String    = "0",
    val debt: String        = "0",
    val debtPayment: String = "0",
    val investments: String = "0",
    val stress: String      = "0",
    val knowledge: String   = "0",
    val risk: String        = "0",
    val setFlags: String    = "",
    val clearFlags: String  = "",
    val schedule: ScheduleDraft? = null
)

private enum class ConditionKind(val label: String) {
    STAT("Stat"), HAS_FLAG("Has flag"), NOT_FLAG("No flag"),
    IN_ERA("In era"), FOR_CHARACTER("For character")
}

private data class ConditionDraft(
    val kind: ConditionKind             = ConditionKind.STAT,
    val statField: Condition.Stat.Field = Condition.Stat.Field.CAPITAL,
    val statOp: Condition.Stat.Op       = Condition.Stat.Op.GTE,
    val statValue: String               = "0",
    /** flag / eraId / characterId depending on [kind]. */
    val text: String                    = ""
)

private data class EventDraft(
    val id: String,
    val message: String,
    val flavor: String,
    val isEnding: Boolean,
    val endingType: EndingType?,
    val tags: String,
    val priority: String,
    val poolWeight: String,
    val unique: Boolean,
    val cooldownMonths: String,
    val maxOccurrences: String,
    val schemeExplanation: String,
    val conditions: List<ConditionDraft>,
    val options: List<OptionDraft>,
    /** true → lives in dto.conditionalEvents instead of dto.events. */
    val isConditional: Boolean
)

private data class PoolEntryDraft(val eventId: String = "", val weight: String = "10")

private data class InitialStateDraft(
    val capital: String, val income: String, val expenses: String, val debt: String,
    val debtPayment: String, val investments: String, val returnRate: String,
    val stress: String, val knowledge: String, val risk: String,
    val month: String, val year: String,
    val characterId: String, val eraId: String, val currency: CurrencyCode
)

// ── Model ↔ draft conversions ────────────────────────────────────────────────

private fun GameEvent.toDraft(isConditional: Boolean) = EventDraft(
    id = id, message = message, flavor = flavor,
    isEnding = isEnding, endingType = endingType,
    tags = tags.joinToString(", "),
    priority = priority.toString(), poolWeight = poolWeight.toString(),
    unique = unique, cooldownMonths = cooldownMonths.toString(),
    maxOccurrences = maxOccurrences.toString(),
    schemeExplanation = schemeExplanation ?: "",
    conditions = conditions.map { it.toDraft() },
    options = options.map { it.toDraft() },
    isConditional = isConditional
)

private fun Condition.toDraft(): ConditionDraft = when (this) {
    is Condition.Stat         -> ConditionDraft(ConditionKind.STAT, field, op, value.toString())
    is Condition.HasFlag      -> ConditionDraft(ConditionKind.HAS_FLAG, text = flag)
    is Condition.NotFlag      -> ConditionDraft(ConditionKind.NOT_FLAG, text = flag)
    is Condition.InEra        -> ConditionDraft(ConditionKind.IN_ERA, text = eraId)
    is Condition.ForCharacter -> ConditionDraft(ConditionKind.FOR_CHARACTER, text = characterId)
}

private fun GameOption.toDraft() = OptionDraft(
    id = id, text = text, emoji = emoji, next = next,
    capital = effects.capitalDelta.toString(),
    income = effects.incomeDelta.toString(),
    expenses = effects.expensesDelta.toString(),
    debt = effects.debtDelta.toString(),
    debtPayment = effects.debtPaymentDelta.toString(),
    investments = effects.investmentsDelta.toString(),
    stress = effects.stressDelta.toString(),
    knowledge = effects.knowledgeDelta.toString(),
    risk = effects.riskDelta.toString(),
    setFlags = effects.setFlags.joinToString(", "),
    clearFlags = effects.clearFlags.joinToString(", "),
    schedule = effects.scheduleEvent?.let { ScheduleDraft(it.eventId, it.afterMonths.toString()) }
)

private fun PlayerState.toDraft() = InitialStateDraft(
    capital = capital.toString(), income = income.toString(),
    expenses = expenses.toString(), debt = debt.toString(),
    debtPayment = debtPaymentMonthly.toString(), investments = investments.toString(),
    returnRate = investmentReturnRate.toString(),
    stress = stress.toString(), knowledge = financialKnowledge.toString(),
    risk = riskLevel.toString(), month = month.toString(), year = year.toString(),
    characterId = characterId, eraId = eraId, currency = currency
)

private fun String.longOr0()   = trim().toLongOrNull() ?: 0L
private fun String.intOr0()    = trim().toIntOrNull() ?: 0
private fun String.csvSet()    = split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

private fun ConditionDraft.toCondition(): Condition = when (kind) {
    ConditionKind.STAT          -> Condition.Stat(statField, statOp, statValue.longOr0())
    ConditionKind.HAS_FLAG      -> Condition.HasFlag(text.trim())
    ConditionKind.NOT_FLAG      -> Condition.NotFlag(text.trim())
    ConditionKind.IN_ERA        -> Condition.InEra(text.trim())
    ConditionKind.FOR_CHARACTER -> Condition.ForCharacter(text.trim())
}

private fun OptionDraft.toOption() = GameOption(
    id = id.trim(), text = text, emoji = emoji.trim(), next = next.trim(),
    effects = Effect(
        capitalDelta = capital.longOr0(), incomeDelta = income.longOr0(),
        expensesDelta = expenses.longOr0(), debtDelta = debt.longOr0(),
        debtPaymentDelta = debtPayment.longOr0(), investmentsDelta = investments.longOr0(),
        stressDelta = stress.intOr0(), knowledgeDelta = knowledge.intOr0(),
        riskDelta = risk.intOr0(),
        setFlags = setFlags.csvSet(), clearFlags = clearFlags.csvSet(),
        scheduleEvent = schedule?.takeIf { it.eventId.isNotBlank() }
            ?.let { ScheduledEvent(it.eventId.trim(), it.months.intOr0().coerceAtLeast(1)) }
    )
)

private fun EventDraft.toEvent() = GameEvent(
    id = id.trim(), message = message, flavor = flavor.ifBlank { "💬" },
    options = options.map { it.toOption() },
    conditions = conditions.map { it.toCondition() },
    priority = priority.intOr0(),
    isEnding = isEnding,
    endingType = if (isEnding) endingType else null,
    tags = tags.csvSet(),
    poolWeight = poolWeight.intOr0().takeIf { it > 0 } ?: 10,
    unique = unique,
    cooldownMonths = cooldownMonths.intOr0(),
    schemeExplanation = schemeExplanation.trim().ifBlank { null },
    maxOccurrences = maxOccurrences.intOr0()
)

private fun InitialStateDraft.toPlayerState() = PlayerState(
    capital = capital.longOr0(), income = income.longOr0(),
    expenses = expenses.longOr0(), debt = debt.longOr0(),
    debtPaymentMonthly = debtPayment.longOr0(), investments = investments.longOr0(),
    investmentReturnRate = returnRate.trim().toDoubleOrNull() ?: 0.08,
    stress = stress.intOr0(), financialKnowledge = knowledge.intOr0(),
    riskLevel = risk.intOr0(),
    month = month.intOr0().coerceIn(1, 12), year = year.intOr0(),
    characterId = characterId.trim(), eraId = eraId.trim(), currency = currency
)

// ════════════════════════════════════════════════════════════════════════════
//  EDITOR SCREEN
// ════════════════════════════════════════════════════════════════════════════

private val prettyJson = Json { prettyPrint = true; ignoreUnknownKeys = true }

/** What the center pane shows: the selected event's form, or the full-size graph. */
private enum class CenterMode { FORM, GRAPH }

@Composable
fun StoryEditorScreen(
    api: AdminApiClient,
    initial: StoryDetail,
    onMessage: (String) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // ── Metadata ──────────────────────────────────────────────────────────────
    var title       by remember { mutableStateOf(initial.row.title) }
    var description by remember { mutableStateOf(initial.row.description) }
    var characterId by remember { mutableStateOf(initial.row.characterId) }
    var eraId       by remember { mutableStateOf(initial.row.eraId) }

    // ── Graph drafts ──────────────────────────────────────────────────────────
    var initialState by remember { mutableStateOf(initial.graph.initialPlayerState.toDraft()) }
    var events by remember {
        mutableStateOf(
            initial.graph.events.map { it.toDraft(isConditional = false) } +
                initial.graph.conditionalEvents.map { it.toDraft(isConditional = true) }
        )
    }
    var pool by remember {
        mutableStateOf(initial.graph.eventPool.map { PoolEntryDraft(it.eventId, it.baseWeight.toString()) })
    }

    var selectedIndex by remember { mutableStateOf(if (events.isEmpty()) null else 0) }
    var centerMode    by remember { mutableStateOf(CenterMode.FORM) }
    var jsonOpen      by remember { mutableStateOf(false) }
    var busy          by remember { mutableStateOf(false) }
    // Hoisted so pan/zoom survives Form ⇄ Graph toggles.
    val canvasState   = rememberScenarioCanvasState()

    // ── Live graph + validation ───────────────────────────────────────────────
    val builtGraph: ScenarioGraphDto = remember(events, pool, initialState) {
        ScenarioGraphDto(
            initialPlayerState = initialState.toPlayerState(),
            events             = events.filter { !it.isConditional }.map { it.toEvent() },
            conditionalEvents  = events.filter { it.isConditional }.map { it.toEvent() },
            eventPool          = pool.filter { it.eventId.isNotBlank() }
                .map { PoolEntry(it.eventId.trim(), it.weight.intOr0().takeIf { w -> w > 0 } ?: 10) }
        )
    }
    val analysis = remember(builtGraph) { analyzeScenario(builtGraph, selfContained = true) }
    val errorCount = analysis.warnings.count { it.severity == GraphWarning.Severity.ERROR }

    val allEventIds = remember(events) { events.map { it.id.trim() }.filter { it.isNotBlank() } }

    fun updateSelected(transform: (EventDraft) -> EventDraft) {
        val idx = selectedIndex ?: return
        events = events.mapIndexed { i, e -> if (i == idx) transform(e) else e }
    }

    /** Canvas selection: unknown id or empty-space tap (null) clears the selection. */
    fun selectById(id: String?) {
        selectedIndex = id?.let { events.indexOfFirst { e -> e.id == it }.takeIf { i -> i >= 0 } }
    }

    /** Validation jump: no-op when the id isn't a real event (e.g. "-" for graph-level errors). */
    fun jumpToEvent(id: String) {
        events.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { selectedIndex = it }
    }

    /**
     * Creates a new (non-ending) event with a unique id derived from [base] and a
     * default "Continue → tick" option, WITHOUT stealing the current selection.
     * Returns the actual id so callers can link to it (option.next, schedules).
     */
    fun createLinkedEvent(base: String): String {
        val id = uniqueEventId(base, events.map { it.id.trim() })
        events = events + emptyEventDraft(id, isEnding = false, options = listOf(defaultOption(1)))
        return id
    }

    fun save() {
        scope.launch {
            busy = true
            try {
                api.updateStory(
                    UpsertStoryRequest(
                        id          = initial.row.id,
                        title       = title.trim(),
                        description = description.trim(),
                        characterId = characterId.trim(),
                        eraId       = eraId.trim(),
                        graph       = builtGraph
                    )
                )
                onMessage("Saved story '${initial.row.id}'")
            } catch (e: Exception) {
                onMessage("Save failed: ${e.message}")
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize()) {

        // ── Toolbar ───────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose) { Text("← Stories") }
            Spacer(Modifier.width(8.dp))
            Text("Editing: ${initial.row.id}", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(12.dp))
            StatusBadge(initial.row.status)
            Spacer(Modifier.width(16.dp))
            FilterChip(
                selected = centerMode == CenterMode.FORM,
                onClick  = { centerMode = CenterMode.FORM },
                label    = { Text("📝 Form") }
            )
            Spacer(Modifier.width(6.dp))
            FilterChip(
                selected = centerMode == CenterMode.GRAPH,
                onClick  = { centerMode = CenterMode.GRAPH },
                label    = { Text("🕸 Graph") }
            )
            Spacer(Modifier.weight(1f))
            if (errorCount > 0) {
                Text(
                    "$errorCount error(s)",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.width(12.dp))
            }
            OutlinedButton(onClick = { jsonOpen = true }) { Text("JSON") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = ::save, enabled = !busy) { Text(if (busy) "Saving…" else "Save") }
        }
        HorizontalDivider()

        Row(Modifier.fillMaxSize()) {

            // ── Left: metadata + event list + pool ────────────────────────────
            Column(
                modifier = Modifier.width(280.dp).fillMaxHeight()
                    .verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Story", style = MaterialTheme.typography.titleSmall)
                FormField(title, { title = it }, "Title")
                FormField(description, { description = it }, "Description", singleLine = false)
                FormField(characterId, { characterId = it }, "Character id")
                FormField(eraId, { eraId = it }, "Era id")

                HorizontalDivider()
                Text("Initial player state", style = MaterialTheme.typography.titleSmall)
                InitialStateForm(initialState) { initialState = it }

                HorizontalDivider()
                Text("Events (${events.size})", style = MaterialTheme.typography.titleSmall)
                Row {
                    // Templates: every new event is immediately playable (no dead-end errors).
                    TextButton(
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        onClick = {
                            val newId = uniqueEventId("event", allEventIds)
                            events = events + emptyEventDraft(newId, isEnding = false, options = listOf(defaultOption(1)))
                            selectedIndex = events.size - 1
                        }
                    ) { Text("+ Event") }
                    TextButton(
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        onClick = {
                            val newId = uniqueEventId("choice", allEventIds)
                            events = events + emptyEventDraft(
                                newId, isEnding = false,
                                options = listOf(
                                    defaultOption(1).copy(text = "Choice A", emoji = "✅"),
                                    defaultOption(2).copy(text = "Choice B", emoji = "❌")
                                )
                            )
                            selectedIndex = events.size - 1
                        }
                    ) { Text("+ Choice") }
                    TextButton(
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        onClick = {
                            val newId = uniqueEventId("ending", allEventIds)
                            events = events + emptyEventDraft(newId, isEnding = true)
                            selectedIndex = events.size - 1
                        }
                    ) { Text("+ Ending") }
                }

                events.forEachIndexed { idx, e ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedIndex = idx },
                        colors = if (selectedIndex == idx)
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        else CardDefaults.cardColors()
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(e.flavor, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    e.id.ifBlank { "(no id)" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (e.isEnding) Badge { Text("🏁") }
                                if (e.isConditional) Badge { Text("cond") }
                            }
                            Text(
                                "${e.options.size} options",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Event pool (${pool.size})", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { pool = pool + PoolEntryDraft() }) { Text("+ Entry") }
                }
                pool.forEachIndexed { idx, entry ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.weight(1f)) {
                            EventIdSelector(
                                value    = entry.eventId,
                                label    = "event id",
                                options  = allEventIds,
                                allowTick = false,
                                onSelect = { v -> pool = pool.mapIndexed { i, p -> if (i == idx) p.copy(eventId = v) else p } }
                            )
                        }
                        OutlinedTextField(
                            value = entry.weight,
                            onValueChange = { v -> pool = pool.mapIndexed { i, p -> if (i == idx) p.copy(weight = v) else p } },
                            label = { Text("w") },
                            singleLine = true,
                            modifier = Modifier.width(72.dp)
                        )
                        TextButton(onClick = { pool = pool.filterIndexed { i, _ -> i != idx } }) { Text("✕") }
                    }
                }
            }

            VerticalDivider()

            // ── Center: selected event form ⇄ full-size graph ──────────────────
            Box(Modifier.weight(1f).fillMaxHeight()) {
                val idx = selectedIndex
                when {
                    centerMode == CenterMode.GRAPH -> {
                        ScenarioCanvas(
                            analysis        = analysis,
                            selectedEventId = idx?.let { events.getOrNull(it)?.id },
                            onSelect        = { id -> selectById(id) },
                            state           = canvasState
                        )
                        // Floating summary of the selected node with a jump-to-form action.
                        val sel = idx?.let { events.getOrNull(it) }
                        if (sel != null) {
                            Card(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                                elevation = CardDefaults.elevatedCardElevation()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        .widthIn(max = 520.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f, fill = false)) {
                                        Text("${sel.flavor} ${sel.id}",
                                            style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${sel.options.size} option(s) · " +
                                                sel.message.replace('\n', ' ').take(60)
                                                    .ifBlank { "(no message)" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Button(onClick = { centerMode = CenterMode.FORM }) { Text("Edit") }
                                }
                            }
                        } else {
                            Text(
                                "Click a node to select · wheel to zoom · drag to pan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                            )
                        }
                    }
                    idx == null || idx !in events.indices ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Select an event on the left (or switch to 🕸 Graph).",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    else -> EventForm(
                        draft         = events[idx],
                        allEventIds   = allEventIds,
                        onChange      = { updateSelected { _ -> it } },
                        onCreateEvent = ::createLinkedEvent,
                        onDuplicate = {
                            val copy = events[idx].copy(id = uniqueEventId(events[idx].id, allEventIds))
                            events = events + copy
                            selectedIndex = events.size - 1
                        },
                        onDelete = {
                            events = events.filterIndexed { i, _ -> i != idx }
                            selectedIndex = if (events.isEmpty()) null else (idx - 1).coerceAtLeast(0)
                        }
                    )
                }
            }

            VerticalDivider()

            // ── Right: validation ──────────────────────────────────────────────
            Column(Modifier.width(340.dp).fillMaxHeight()) {
                Text(
                    "Validation (${analysis.warnings.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(12.dp)
                )
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (analysis.warnings.isEmpty()) {
                        item {
                            Text("No issues found — publishable.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(analysis.warnings) { w ->
                        Card(Modifier.fillMaxWidth().clickable { jumpToEvent(w.eventId) }) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                                Text(
                                    when (w.severity) {
                                        GraphWarning.Severity.ERROR -> "ERR"
                                        GraphWarning.Severity.WARN  -> "WARN"
                                        GraphWarning.Severity.INFO  -> "INFO"
                                    },
                                    color = when (w.severity) {
                                        GraphWarning.Severity.ERROR -> MaterialTheme.colorScheme.error
                                        GraphWarning.Severity.WARN  -> MaterialTheme.colorScheme.tertiary
                                        GraphWarning.Severity.INFO  -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(w.eventId, style = MaterialTheme.typography.labelMedium)
                                    Text(w.message, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (jsonOpen) {
        JsonDialog(
            graph     = builtGraph,
            onDismiss = { jsonOpen = false },
            onImport  = { imported ->
                initialState = imported.initialPlayerState.toDraft()
                events = imported.events.map { it.toDraft(false) } +
                    imported.conditionalEvents.map { it.toDraft(true) }
                pool = imported.eventPool.map { PoolEntryDraft(it.eventId, it.baseWeight.toString()) }
                selectedIndex = if (imported.events.isEmpty()) null else 0
                jsonOpen = false
                onMessage("Graph imported (${imported.events.size} events)")
            },
            onError = { onMessage(it) }
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Default "keep playing" option — a new event is never an instant dead-end error. */
private fun defaultOption(n: Int) =
    OptionDraft(id = "opt_$n", text = "Continue", emoji = "👉", next = MONTHLY_TICK)

private fun emptyEventDraft(
    id: String,
    isEnding: Boolean,
    options: List<OptionDraft> = emptyList()
) = EventDraft(
    id = id, message = "", flavor = if (isEnding) "🏁" else "💬",
    isEnding = isEnding,
    endingType = if (isEnding) EndingType.FINANCIAL_STABILITY else null,
    tags = "", priority = "0", poolWeight = "10",
    unique = false, cooldownMonths = "0", maxOccurrences = "0",
    schemeExplanation = "",
    conditions = emptyList(),
    options = if (isEnding) emptyList() else options,
    isConditional = false
)

private fun uniqueEventId(base: String, existing: List<String>): String {
    val clean = base.ifBlank { "event" }
    if (clean !in existing) return clean
    var n = 2
    while ("${clean}_$n" in existing) n++
    return "${clean}_$n"
}

// ════════════════════════════════════════════════════════════════════════════
//  SUB-FORMS
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun InitialStateForm(draft: InitialStateDraft, onChange: (InitialStateDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PairRow("Capital", draft.capital, { onChange(draft.copy(capital = it)) },
            "Income", draft.income, { onChange(draft.copy(income = it)) })
        PairRow("Expenses", draft.expenses, { onChange(draft.copy(expenses = it)) },
            "Debt", draft.debt, { onChange(draft.copy(debt = it)) })
        PairRow("Debt pay/mo", draft.debtPayment, { onChange(draft.copy(debtPayment = it)) },
            "Investments", draft.investments, { onChange(draft.copy(investments = it)) })
        PairRow("Stress", draft.stress, { onChange(draft.copy(stress = it)) },
            "Knowledge", draft.knowledge, { onChange(draft.copy(knowledge = it)) })
        PairRow("Risk", draft.risk, { onChange(draft.copy(risk = it)) },
            "Return rate", draft.returnRate, { onChange(draft.copy(returnRate = it)) })
        PairRow("Month", draft.month, { onChange(draft.copy(month = it)) },
            "Year", draft.year, { onChange(draft.copy(year = it)) })
        Text("Currency", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CurrencyCode.entries.forEach { c ->
                FilterChip(selected = draft.currency == c,
                    onClick = { onChange(draft.copy(currency = c)) },
                    label = { Text(c.name) })
            }
        }
    }
}

@Composable
private fun PairRow(
    l1: String, v1: String, c1: (String) -> Unit,
    l2: String, v2: String, c2: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(v1, c1, label = { Text(l1) }, singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(v2, c2, label = { Text(l2) }, singleLine = true, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventIdSelector(
    value: String,
    label: String,
    options: List<String>,
    allowTick: Boolean,
    onSelect: (String) -> Unit,
    /** When set, an unknown typed id offers "➕ create event" — [onCreate] must create it and link. */
    onCreate: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val typed = value.trim()
    val isUnknown = typed.isNotEmpty() && typed != MONTHLY_TICK && typed !in options

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = onSelect,      // free text allowed (external/library events)
            label = { Text(label) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (onCreate != null && isUnknown) {
                DropdownMenuItem(
                    text = { Text("➕ Create event '$typed'") },
                    onClick = { onCreate(typed); expanded = false }
                )
            }
            if (allowTick) {
                DropdownMenuItem(
                    text = { Text("MONTHLY_TICK (monthly sim)") },
                    onClick = { onSelect(MONTHLY_TICK); expanded = false }
                )
            }
            options.forEach { id ->
                DropdownMenuItem(text = { Text(id) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}

@Composable
private fun EventForm(
    draft: EventDraft,
    allEventIds: List<String>,
    onChange: (EventDraft) -> Unit,
    /** Creates a new event with a unique id derived from the base; returns the actual id. */
    onCreateEvent: (String) -> String,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Event", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDuplicate) { Text("Duplicate") }
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete event") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(draft.id, { onChange(draft.copy(id = it)) },
                label = { Text("id") }, singleLine = true, modifier = Modifier.weight(2f))
            OutlinedTextField(draft.flavor, { onChange(draft.copy(flavor = it)) },
                label = { Text("emoji") }, singleLine = true, modifier = Modifier.weight(1f))
        }

        OutlinedTextField(
            value = draft.message,
            onValueChange = { onChange(draft.copy(message = it)) },
            label = { Text("Message (chat text)") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
        )

        // ── Flags row: ending / conditional / unique ──────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(draft.isEnding, { onChange(draft.copy(isEnding = it)) })
                Spacer(Modifier.width(4.dp)); Text("Ending")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(draft.isConditional, { onChange(draft.copy(isConditional = it)) })
                Spacer(Modifier.width(4.dp)); Text("Conditional")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(draft.unique, { onChange(draft.copy(unique = it)) })
                Spacer(Modifier.width(4.dp)); Text("Unique")
            }
        }

        if (draft.isEnding) {
            Text("Ending type", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EndingType.entries.forEach { t ->
                    FilterChip(
                        selected = draft.endingType == t,
                        onClick  = { onChange(draft.copy(endingType = t)) },
                        label    = { Text(t.name.take(12)) }
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(draft.tags, { onChange(draft.copy(tags = it)) },
                label = { Text("tags (csv)") }, singleLine = true, modifier = Modifier.weight(2f))
            OutlinedTextField(draft.priority, { onChange(draft.copy(priority = it)) },
                label = { Text("priority") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(draft.poolWeight, { onChange(draft.copy(poolWeight = it)) },
                label = { Text("pool weight") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(draft.cooldownMonths, { onChange(draft.copy(cooldownMonths = it)) },
                label = { Text("cooldown mo") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(draft.maxOccurrences, { onChange(draft.copy(maxOccurrences = it)) },
                label = { Text("max occur.") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(
            value = draft.schemeExplanation,
            onValueChange = { onChange(draft.copy(schemeExplanation = it)) },
            label = { Text("Scheme explanation (educational, optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        // ── Conditions ────────────────────────────────────────────────────────
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Conditions (${draft.conditions.size}) — all must hold",
                style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                onChange(draft.copy(conditions = draft.conditions + ConditionDraft()))
            }) { Text("+ Condition") }
        }
        draft.conditions.forEachIndexed { ci, cond ->
            ConditionEditor(
                cond     = cond,
                onChange = { new ->
                    onChange(draft.copy(conditions = draft.conditions.mapIndexed { i, c ->
                        if (i == ci) new else c
                    }))
                },
                onRemove = {
                    onChange(draft.copy(conditions = draft.conditions.filterIndexed { i, _ -> i != ci }))
                }
            )
        }

        // ── Options ───────────────────────────────────────────────────────────
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Options (${draft.options.size})", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                val existing = draft.options.map { it.id }
                var n = draft.options.size + 1
                while ("opt_$n" in existing) n++
                onChange(draft.copy(options = draft.options + OptionDraft(id = "opt_$n")))
            }) { Text("+ Option") }
        }
        if (draft.isEnding && draft.options.isNotEmpty()) {
            Text("⚠ Endings should have no options.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        draft.options.forEachIndexed { oi, opt ->
            OptionEditor(
                opt           = opt,
                allEventIds   = allEventIds,
                newIdBase     = listOf(draft.id.trim(), opt.id.trim())
                    .filter { it.isNotBlank() }.joinToString("_").ifBlank { "event" },
                onCreateEvent = onCreateEvent,
                onChange      = { new ->
                    onChange(draft.copy(options = draft.options.mapIndexed { i, o ->
                        if (i == oi) new else o
                    }))
                },
                onRemove = {
                    onChange(draft.copy(options = draft.options.filterIndexed { i, _ -> i != oi }))
                }
            )
        }
    }
}

@Composable
private fun ConditionEditor(
    cond: ConditionDraft,
    onChange: (ConditionDraft) -> Unit,
    onRemove: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ConditionKind.entries.forEach { k ->
                        FilterChip(
                            selected = cond.kind == k,
                            onClick  = { onChange(cond.copy(kind = k)) },
                            label    = { Text(k.label) }
                        )
                    }
                }
                TextButton(onClick = onRemove) { Text("✕") }
            }
            when (cond.kind) {
                ConditionKind.STAT -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Column(Modifier.weight(2f)) {
                            Text("field", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Condition.Stat.Field.entries.forEach { f ->
                                    FilterChip(
                                        selected = cond.statField == f,
                                        onClick  = { onChange(cond.copy(statField = f)) },
                                        label    = { Text(f.name.take(4), style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Condition.Stat.Op.entries.forEach { op ->
                            FilterChip(
                                selected = cond.statOp == op,
                                onClick  = { onChange(cond.copy(statOp = op)) },
                                label    = { Text(opSymbol(op)) }
                            )
                        }
                        OutlinedTextField(
                            value = cond.statValue,
                            onValueChange = { onChange(cond.copy(statValue = it)) },
                            label = { Text("value") },
                            singleLine = true,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
                ConditionKind.HAS_FLAG, ConditionKind.NOT_FLAG ->
                    OutlinedTextField(cond.text, { onChange(cond.copy(text = it)) },
                        label = { Text("flag") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ConditionKind.IN_ERA ->
                    OutlinedTextField(cond.text, { onChange(cond.copy(text = it)) },
                        label = { Text("eraId") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ConditionKind.FOR_CHARACTER ->
                    OutlinedTextField(cond.text, { onChange(cond.copy(text = it)) },
                        label = { Text("characterId") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun opSymbol(op: Condition.Stat.Op): String = when (op) {
    Condition.Stat.Op.GT -> ">"; Condition.Stat.Op.LT -> "<"
    Condition.Stat.Op.GTE -> "≥"; Condition.Stat.Op.LTE -> "≤"
    Condition.Stat.Op.EQ -> "="; Condition.Stat.Op.NEQ -> "≠"
}

@Composable
private fun OptionEditor(
    opt: OptionDraft,
    allEventIds: List<String>,
    /** Base for auto-generated ids of "create & link" events, e.g. "intro_opt_1". */
    newIdBase: String,
    /** Creates a new event with a unique id derived from the base; returns the actual id. */
    onCreateEvent: (String) -> String,
    onChange: (OptionDraft) -> Unit,
    onRemove: () -> Unit
) {
    var effectsOpen by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(opt.id, { onChange(opt.copy(id = it)) },
                    label = { Text("id") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(opt.emoji, { onChange(opt.copy(emoji = it)) },
                    label = { Text("emoji") }, singleLine = true, modifier = Modifier.width(90.dp))
                TextButton(onClick = onRemove) { Text("✕") }
            }
            OutlinedTextField(opt.text, { onChange(opt.copy(text = it)) },
                label = { Text("Button text") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    EventIdSelector(
                        value     = opt.next,
                        label     = "next →",
                        options   = allEventIds,
                        allowTick = true,
                        onSelect  = { onChange(opt.copy(next = it)) },
                        onCreate  = { typed -> onChange(opt.copy(next = onCreateEvent(typed))) }
                    )
                }
                // One click: create "<eventId>_<optId>" and point this option at it.
                TextButton(onClick = { onChange(opt.copy(next = onCreateEvent(newIdBase))) }) {
                    Text("＋→ new")
                }
            }

            TextButton(onClick = { effectsOpen = !effectsOpen }) {
                Text(if (effectsOpen) "▾ Effects" else "▸ Effects (${effectSummary(opt)})")
            }
            if (effectsOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(opt.capital, { onChange(opt.copy(capital = it)) },
                            label = { Text("Δcapital") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(opt.income, { onChange(opt.copy(income = it)) },
                            label = { Text("Δincome") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(opt.expenses, { onChange(opt.copy(expenses = it)) },
                            label = { Text("Δexpenses") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(opt.debt, { onChange(opt.copy(debt = it)) },
                            label = { Text("Δdebt") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(opt.debtPayment, { onChange(opt.copy(debtPayment = it)) },
                            label = { Text("Δdebt pay") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(opt.investments, { onChange(opt.copy(investments = it)) },
                            label = { Text("Δinvest") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(opt.stress, { onChange(opt.copy(stress = it)) },
                            label = { Text("Δstress") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(opt.knowledge, { onChange(opt.copy(knowledge = it)) },
                            label = { Text("Δknowl.") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(opt.risk, { onChange(opt.copy(risk = it)) },
                            label = { Text("Δrisk") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(opt.setFlags, { onChange(opt.copy(setFlags = it)) },
                        label = { Text("set flags (csv)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(opt.clearFlags, { onChange(opt.copy(clearFlags = it)) },
                        label = { Text("clear flags (csv)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = opt.schedule != null,
                            onCheckedChange = { on ->
                                onChange(opt.copy(schedule = if (on) ScheduleDraft() else null))
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Schedule deferred event", style = MaterialTheme.typography.bodySmall)
                    }
                    opt.schedule?.let { sched ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.weight(2f)) {
                                EventIdSelector(
                                    value     = sched.eventId,
                                    label     = "scheduled event",
                                    options   = allEventIds,
                                    allowTick = false,
                                    onSelect  = { onChange(opt.copy(schedule = sched.copy(eventId = it))) },
                                    onCreate  = { typed ->
                                        onChange(opt.copy(schedule = sched.copy(eventId = onCreateEvent(typed))))
                                    }
                                )
                            }
                            OutlinedTextField(
                                value = sched.months,
                                onValueChange = { onChange(opt.copy(schedule = sched.copy(months = it))) },
                                label = { Text("after mo") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun effectSummary(o: OptionDraft): String {
    val n = listOf(o.capital, o.income, o.expenses, o.debt, o.debtPayment,
        o.investments, o.stress, o.knowledge, o.risk)
        .count { (it.toLongOrNull() ?: 0L) != 0L }
    val f = o.setFlags.csvSet().size + o.clearFlags.csvSet().size
    val s = if (o.schedule != null) 1 else 0
    return "$n deltas, $f flags${if (s > 0) ", 1 scheduled" else ""}"
}

// ════════════════════════════════════════════════════════════════════════════
//  JSON IMPORT / EXPORT
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun JsonDialog(
    graph: ScenarioGraphDto,
    onDismiss: () -> Unit,
    onImport: (ScenarioGraphDto) -> Unit,
    onError: (String) -> Unit
) {
    var text by remember { mutableStateOf(prettyJson.encodeToString(ScenarioGraphDto.serializer(), graph)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Graph JSON") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Copy for backup, or paste a graph and press Import. " +
                        "The editor state is replaced entirely on import.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.width(560.dp).heightIn(min = 300.dp, max = 420.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    onImport(prettyJson.decodeFromString(ScenarioGraphDto.serializer(), text))
                } catch (e: Exception) {
                    onError("JSON parse failed: ${e.message?.take(200)}")
                }
            }) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
