package kz.fearsom.financiallifev2.adminui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kz.fearsom.financiallifev2.admin.ScenarioComboDto
import kz.fearsom.financiallifev2.admin.ScenarioGraphDto
import kz.fearsom.financiallifev2.adminui.graph.GraphNode
import kz.fearsom.financiallifev2.adminui.graph.GraphWarning
import kz.fearsom.financiallifev2.adminui.graph.ScenarioAnalysis
import kz.fearsom.financiallifev2.adminui.graph.analyzeScenario
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient
import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.MONTHLY_TICK

// ── Canvas layout constants (world units) ───────────────────────────────────────
private const val NODE_W = 168f
private const val NODE_H = 56f
private const val H_GAP  = 28f
private const val V_GAP  = 44f
private const val PAD    = 28f

@Composable
fun ScenariosScreen(api: AdminApiClient, onMessage: (String) -> Unit) {
    var combos       by remember { mutableStateOf<List<ScenarioComboDto>>(emptyList()) }
    var loading      by remember { mutableStateOf(true) }
    var selected     by remember { mutableStateOf<ScenarioComboDto?>(null) }
    var graph        by remember { mutableStateOf<ScenarioGraphDto?>(null) }
    var graphLoading by remember { mutableStateOf(false) }
    val scope        = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            combos = api.listScenarioCombos()
        } catch (e: Exception) {
            onMessage("Failed to load scenarios: ${e.message}")
        } finally {
            loading = false
        }
    }

    Row(Modifier.fillMaxSize()) {
        // ── Combo list (left) ─────────────────────────────────────────────────
        Column(Modifier.width(240.dp).fillMaxHeight().padding(12.dp)) {
            Text("Scenarios", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (loading) {
                CircularProgressIndicator()
            } else if (combos.isEmpty()) {
                Text(
                    "No character×era combos. Add characters and assign eras first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(combos) { combo ->
                        val isSelected = selected == combo
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selected = combo
                                graph = null
                                scope.launch {
                                    graphLoading = true
                                    try {
                                        graph = api.getScenarioGraph(combo.characterId, combo.eraId)
                                    } catch (e: Exception) {
                                        onMessage("Failed to load graph: ${e.message}")
                                    } finally {
                                        graphLoading = false
                                    }
                                }
                            },
                            colors = if (isSelected) {
                                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                CardDefaults.cardColors()
                            }
                        ) {
                            Text(
                                text = combo.label,
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        VerticalDivider()

        // ── Graph + inspector (right) ─────────────────────────────────────────
        val currentGraph = graph
        when {
            selected == null -> CenterHint("Select a scenario combo on the left.")
            graphLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            currentGraph != null -> GraphAndInspector(currentGraph)
            else -> CenterHint("No graph loaded.")
        }
    }
}

@Composable
private fun RowScope.CenterHint(text: String) {
    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RowScope.GraphAndInspector(dto: ScenarioGraphDto) {
    // Re-analyze only when the graph instance changes.
    val analysis = remember(dto) { analyzeScenario(dto) }
    var selectedEventId by remember(dto) { mutableStateOf<String?>(null) }

    // Center: visual canvas graph.
    Box(Modifier.weight(1f).fillMaxHeight()) {
        ScenarioCanvas(
            analysis = analysis,
            selectedEventId = selectedEventId,
            onSelect = { selectedEventId = it }
        )
    }

    VerticalDivider()

    // Right: enriched inspector.
    Column(
        modifier = Modifier.width(360.dp).fillMaxHeight()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val node = selectedEventId?.let { analysis.node(it) }
        if (node != null) {
            TextButton(onClick = { selectedEventId = null }) { Text("← Back to summary") }
            EventInspector(
                event = node.event,
                analysis = analysis,
                onJump = { target -> if (analysis.node(target) != null) selectedEventId = target }
            )
        } else {
            GraphSummary(dto, analysis, onSelectEvent = { selectedEventId = it })
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  CANVAS
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun ScenarioCanvas(
    analysis: ScenarioAnalysis,
    selectedEventId: String?,
    onSelect: (String?) -> Unit
) {
    var viewScale by remember(analysis) { mutableStateOf(1f) }
    var offset    by remember(analysis) { mutableStateOf(Offset(PAD, PAD)) }

    val measurer = rememberTextMeasurer()

    // Theme colors captured for use inside the (non-composable) DrawScope.
    val cs = MaterialTheme.colorScheme
    val edgeColor       = cs.outline.copy(alpha = 0.7f)
    val normalFill      = cs.surfaceVariant
    val normalText      = cs.onSurfaceVariant
    val rootFill        = cs.primaryContainer
    val rootText        = cs.onPrimaryContainer
    val selectedBorder  = cs.primary
    val unreachBorder   = cs.error
    val canvasBg        = cs.surface

    fun nodeTopLeft(n: GraphNode) = Offset(
        x = PAD + n.column * (NODE_W + H_GAP),
        y = PAD + n.rank * (NODE_H + V_GAP)
    )

    Box(Modifier.fillMaxSize().clipToBounds().background(canvasBg)) {
        if (analysis.nodes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Empty graph.", color = normalText)
            }
        } else {
            Canvas(
                modifier = Modifier.fillMaxSize()
                    .pointerInput(analysis) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val newScale = (viewScale * zoom).coerceIn(0.3f, 3f)
                            offset = centroid - (centroid - offset) * (newScale / viewScale) + pan
                            viewScale = newScale
                        }
                    }
                    .pointerInput(analysis) {
                        detectTapGestures { tap ->
                            val world = (tap - offset) / viewScale
                            val hit = analysis.nodes.firstOrNull { n ->
                                val tl = nodeTopLeft(n)
                                world.x in tl.x..(tl.x + NODE_W) && world.y in tl.y..(tl.y + NODE_H)
                            }
                            onSelect(hit?.event?.id)
                        }
                    }
            ) {
                translate(left = offset.x, top = offset.y) {
                    scale(scaleX = viewScale, scaleY = viewScale, pivot = Offset.Zero) {
                        // Edges first (under nodes).
                        val posById = analysis.nodes.associate { it.event.id to nodeTopLeft(it) }
                        for (edge in analysis.edges) {
                            val from = posById[edge.fromId] ?: continue
                            val to   = posById[edge.toId] ?: continue
                            val start = Offset(from.x + NODE_W / 2f, from.y + NODE_H)
                            val end   = Offset(to.x + NODE_W / 2f, to.y)
                            drawLine(edgeColor, start, end, strokeWidth = 1.6f)
                            // Arrival chevron pointing into the target's top edge.
                            drawLine(edgeColor, end, Offset(end.x - 5f, end.y - 8f), strokeWidth = 1.6f)
                            drawLine(edgeColor, end, Offset(end.x + 5f, end.y - 8f), strokeWidth = 1.6f)
                        }

                        // Nodes.
                        for (n in analysis.nodes) {
                            val tl = nodeTopLeft(n)
                            val ending = n.event.isEnding
                            val fill = when {
                                ending -> endingColor(n.event.endingType).copy(alpha = 0.9f)
                                n.isRoot -> rootFill
                                else -> normalFill
                            }
                            val textColor = when {
                                ending -> Color.White
                                n.isRoot -> rootText
                                else -> normalText
                            }
                            drawRoundRect(
                                color = fill,
                                topLeft = tl,
                                size = Size(NODE_W, NODE_H),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                            )
                            // Border: selected > unreachable > none.
                            when {
                                n.event.id == selectedEventId -> drawRoundRect(
                                    color = selectedBorder, topLeft = tl, size = Size(NODE_W, NODE_H),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                                    style = Stroke(width = 3f)
                                )
                                !n.reachable -> drawRoundRect(
                                    color = unreachBorder, topLeft = tl, size = Size(NODE_W, NODE_H),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                                    style = Stroke(
                                        width = 1.5f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                                    )
                                )
                            }
                            val label = "${n.event.flavor} ${n.event.id}" +
                                if (ending) "\n🏁 ${n.event.endingType ?: "ending"}" else ""
                            val layout = measurer.measure(
                                text = label,
                                style = TextStyle(color = textColor, fontSize = 11.sp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                constraints = Constraints(maxWidth = (NODE_W - 16).toInt())
                            )
                            drawText(layout, topLeft = Offset(tl.x + 8f, tl.y + 8f))
                        }
                    }
                }
            }

            // Zoom / reset overlay.
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SmallControl("−") { viewScale = (viewScale * 0.8f).coerceIn(0.3f, 3f) }
                SmallControl("+") { viewScale = (viewScale * 1.25f).coerceIn(0.3f, 3f) }
                SmallControl("Reset") { viewScale = 1f; offset = Offset(PAD, PAD) }
            }
        }
    }
}

@Composable
private fun SmallControl(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  INSPECTOR
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun GraphSummary(
    dto: ScenarioGraphDto,
    analysis: ScenarioAnalysis,
    onSelectEvent: (String) -> Unit
) {
    Text("Graph summary", style = MaterialTheme.typography.titleSmall)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val s = analysis.stats
            KeyVal("Entry event", analysis.rootId ?: "—")
            KeyVal("Story events", s.eventCount.toString())
            KeyVal("Endings", s.endingCount.toString())
            KeyVal("Options (edges)", s.optionCount.toString())
            KeyVal("Conditional events", s.conditionalCount.toString())
            KeyVal("Pool entries", s.poolCount.toString())
        }
    }

    Text("Initial player state", style = MaterialTheme.typography.titleSmall)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val p = dto.initialPlayerState
            KeyVal("capital", formatMoney(p.capital))
            KeyVal("income", formatMoney(p.income))
            KeyVal("expenses", formatMoney(p.expenses))
            KeyVal("debt", formatMoney(p.debt))
            KeyVal("stress", p.stress.toString())
            KeyVal("knowledge", p.financialKnowledge.toString())
            KeyVal("risk", p.riskLevel.toString())
        }
    }

    Text(
        "Validation (${analysis.warnings.size})",
        style = MaterialTheme.typography.titleSmall
    )
    if (analysis.warnings.isEmpty()) {
        Text("No issues found.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        analysis.warnings.forEach { w ->
            Card(
                Modifier.fillMaxWidth().clickable { onSelectEvent(w.eventId) }
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                    Text(severityTag(w.severity), color = severityColor(w.severity),
                        style = MaterialTheme.typography.labelMedium)
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

    Text("Legend", style = MaterialTheme.typography.titleSmall)
    Text(
        "• Entry = highlighted  • 🏁 = ending (colored by type)  • dashed red = unreachable\n" +
            "Pinch / scroll to zoom, drag to pan, tap a node to inspect.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EventInspector(
    event: GameEvent,
    analysis: ScenarioAnalysis,
    onJump: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(event.flavor, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text(event.id, style = MaterialTheme.typography.titleSmall)
    }
    if (event.isEnding) {
        Badge(containerColor = endingColor(event.endingType)) {
            Text("ending: ${event.endingType ?: "—"}")
        }
    }

    Text(event.message, style = MaterialTheme.typography.bodyMedium)

    val meta = buildList {
        if (event.tags.isNotEmpty()) add("tags: ${event.tags.joinToString()}")
        add("priority: ${event.priority}")
        add("poolWeight: ${event.poolWeight}")
        if (event.unique) add("unique")
        if (event.cooldownMonths > 0) add("cooldown: ${event.cooldownMonths}mo")
    }
    Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)

    if (event.conditions.isNotEmpty()) {
        Text("Conditions (all must hold)", style = MaterialTheme.typography.labelLarge)
        event.conditions.forEach { c ->
            Text("• ${formatCondition(c)}", style = MaterialTheme.typography.bodySmall)
        }
    }

    Text("Options (${event.options.size})", style = MaterialTheme.typography.labelLarge)
    if (event.options.isEmpty()) {
        Text(
            if (event.isEnding) "Leaf ending — no options." else "⚠ No options (dead-end).",
            style = MaterialTheme.typography.bodySmall,
            color = if (event.isEnding) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error
        )
    } else {
        event.options.forEach { opt ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${opt.emoji} ${opt.text}", style = MaterialTheme.typography.bodyMedium)

                    val effects = formatEffect(opt.effects)
                    if (effects.isNotEmpty()) {
                        Text(effects.joinToString(", "), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    opt.effects.scheduleEvent?.let { se ->
                        Text("schedules '${se.eventId}' in ${se.afterMonths}mo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary)
                    }

                    // Next-link: jump if it's a story event, label otherwise.
                    when {
                        opt.next == MONTHLY_TICK ->
                            Text("→ monthly tick", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        analysis.node(opt.next) != null ->
                            TextButton(
                                onClick = { onJump(opt.next) },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) { Text("→ ${opt.next}") }
                        else ->
                            Text("→ ${opt.next} (external/library)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ── Small UI helpers ────────────────────────────────────────────────────────────

@Composable
private fun KeyVal(key: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun severityColor(s: GraphWarning.Severity): Color = when (s) {
    GraphWarning.Severity.ERROR -> MaterialTheme.colorScheme.error
    GraphWarning.Severity.WARN  -> MaterialTheme.colorScheme.tertiary
    GraphWarning.Severity.INFO  -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun severityTag(s: GraphWarning.Severity): String = when (s) {
    GraphWarning.Severity.ERROR -> "ERR"
    GraphWarning.Severity.WARN  -> "WARN"
    GraphWarning.Severity.INFO  -> "INFO"
}

// ── Pure formatting helpers ─────────────────────────────────────────────────────

private fun endingColor(type: EndingType?): Color = when (type) {
    EndingType.WEALTH               -> Color(0xFF2E7D32)
    EndingType.FINANCIAL_FREEDOM    -> Color(0xFF388E3C)
    EndingType.FINANCIAL_STABILITY  -> Color(0xFF1565C0)
    EndingType.PAYCHECK_TO_PAYCHECK -> Color(0xFFF9A825)
    EndingType.BANKRUPTCY           -> Color(0xFFC62828)
    null                            -> Color(0xFF6A6A6A)
}

private fun formatMoney(v: Long): String {
    val abs = kotlin.math.abs(v)
    return when {
        abs >= 1_000_000 -> "${v / 1_000_000}.${(abs % 1_000_000) / 100_000}M ₸"
        abs >= 1_000     -> "${v / 1_000}k ₸"
        else             -> "$v ₸"
    }
}

/** Compact list of the non-zero deltas in an [Effect]. */
private fun formatEffect(e: Effect): List<String> = buildList {
    if (e.capitalDelta != 0L)     add("capital ${signed(e.capitalDelta)}")
    if (e.incomeDelta != 0L)      add("income ${signed(e.incomeDelta)}")
    if (e.expensesDelta != 0L)    add("expenses ${signed(e.expensesDelta)}")
    if (e.debtDelta != 0L)        add("debt ${signed(e.debtDelta)}")
    if (e.debtPaymentDelta != 0L) add("debtPay ${signed(e.debtPaymentDelta)}")
    if (e.investmentsDelta != 0L) add("invest ${signed(e.investmentsDelta)}")
    if (e.stressDelta != 0)       add("stress ${signed(e.stressDelta.toLong())}")
    if (e.knowledgeDelta != 0)    add("knowledge ${signed(e.knowledgeDelta.toLong())}")
    if (e.riskDelta != 0)         add("risk ${signed(e.riskDelta.toLong())}")
    if (e.setFlags.isNotEmpty())  add("+flags ${e.setFlags.joinToString()}")
    if (e.clearFlags.isNotEmpty())add("-flags ${e.clearFlags.joinToString()}")
    e.monetaryReform?.let { add("reform ${it.from}→${it.to} ${it.numerator}:${it.denominator}") }
}

private fun signed(v: Long): String = if (v >= 0) "+$v" else "$v"

private fun formatCondition(c: Condition): String = when (c) {
    is Condition.Stat        -> "${c.field} ${c.op} ${c.value}"
    is Condition.HasFlag     -> "has flag '${c.flag}'"
    is Condition.NotFlag     -> "not flag '${c.flag}'"
    is Condition.InEra       -> "in era '${c.eraId}'"
    is Condition.ForCharacter -> "for character '${c.characterId}'"
}
