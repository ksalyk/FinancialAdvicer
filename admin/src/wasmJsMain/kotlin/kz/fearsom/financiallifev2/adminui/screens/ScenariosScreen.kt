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
import kz.fearsom.financiallifev2.admin.ScenarioComboDto
import kz.fearsom.financiallifev2.admin.ScenarioGraphDto
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient

@Composable
fun ScenariosScreen(api: AdminApiClient) {
    var combos   by remember { mutableStateOf<List<ScenarioComboDto>>(emptyList()) }
    var loading  by remember { mutableStateOf(true) }
    var error    by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<ScenarioComboDto?>(null) }
    var graph    by remember { mutableStateOf<ScenarioGraphDto?>(null) }
    var graphLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try { combos = api.listScenarioCombos() }
        catch (e: Exception) { error = e.message }
        finally { loading = false }
    }

    Row(Modifier.fillMaxSize()) {
        // ── Combo list (left panel) ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .padding(12.dp)
        ) {
            Text("Scenarios", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            when {
                loading -> CircularProgressIndicator()
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(combos) { combo ->
                        val isSelected = selected == combo
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = combo
                                    graph    = null
                                    scope.launch {
                                        graphLoading = true
                                        try { graph = api.getScenarioGraph(combo.characterId, combo.eraId) }
                                        catch (e: Exception) { error = e.message }
                                        finally { graphLoading = false }
                                    }
                                },
                            colors = if (isSelected) CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) else CardDefaults.cardColors()
                        ) {
                            Text(
                                text     = combo.label,
                                modifier = Modifier.padding(10.dp),
                                style    = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        VerticalDivider()

        // ── Graph detail (right panel) ────────────────────────────────────────
        Box(
            modifier            = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment    = Alignment.TopStart
        ) {
            when {
                selected == null -> Text(
                    "Select a scenario combo on the left.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                graphLoading -> CircularProgressIndicator()
                graph != null -> ScenarioGraphDetail(graph!!)
            }
        }
    }
}

// ── Graph detail panel ────────────────────────────────────────────────────────

@Composable
private fun ScenarioGraphDetail(graph: ScenarioGraphDto) {
    Column(
        modifier            = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Initial state
        Text("Initial Player State", style = MaterialTheme.typography.titleSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                val s = graph.initialPlayerState
                StatLine("capital", s.capital)
                StatLine("income", s.income)
                StatLine("expenses", s.expenses)
                StatLine("debt", s.debt)
                StatLine("stress", s.stress)
                StatLine("financial_knowledge", s.financialKnowledge)
                StatLine("risk_level", s.riskLevel)
            }
        }

        // Story events
        Text(
            "Story Events (${graph.events.size})",
            style = MaterialTheme.typography.titleSmall
        )
        graph.events.forEach { event ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text  = "${event.flavor} [${event.id}]",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(event.message, style = MaterialTheme.typography.bodySmall)
                    if (event.options.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        event.options.forEach { opt ->
                            Text(
                                text  = "  ${opt.emoji} ${opt.text} → ${opt.next}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Conditional events
        if (graph.conditionalEvents.isNotEmpty()) {
            Text(
                "Conditional Events (${graph.conditionalEvents.size})",
                style = MaterialTheme.typography.titleSmall
            )
            graph.conditionalEvents.forEach { event ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text  = "${event.flavor} [${event.id}] priority=${event.priority}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(event.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Event pool summary
        if (graph.eventPool.isNotEmpty()) {
            Text(
                "Pool Entries (${graph.eventPool.size})",
                style = MaterialTheme.typography.titleSmall
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    graph.eventPool.forEach { entry ->
                        Text(
                            text  = "  eventId=${entry.eventId} weight=${entry.baseWeight}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatLine(name: String, value: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.toString(), style = MaterialTheme.typography.bodySmall)
    }
}
