package kz.fearsom.financiallifev2.adminui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kz.fearsom.financiallifev2.admin.EraRow
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient

@Composable
fun ErasScreen(api: AdminApiClient) {
    var eras    by remember { mutableStateOf<List<EraRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf<String?>(null) }
    val scope   = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true; error = null
            try { eras = api.listEras() }
            catch (e: Exception) { error = e.message }
            finally { loading = false }
        }
    }

    fun toggle(era: EraRow, onRevert: () -> Unit) {
        scope.launch {
            val ok = try {
                if (era.isActive) api.deactivateEra(era.id)
                else              api.activateEra(era.id)
            } catch (e: Exception) {
                error = e.message; false
            }
            if (ok) reload() else onRevert()
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Eras", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(eras, key = { it.id }) { era ->
                    EraCard(
                        era      = era,
                        onToggle = { onRevert -> toggle(era, onRevert) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EraCard(era: EraRow, onToggle: (onRevert: () -> Unit) -> Unit) {
    var checked by remember(era.id, era.isActive) { mutableStateOf(era.isActive) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(era.emoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(era.name, style = MaterialTheme.typography.bodyLarge)
                    if (!checked) {
                        Spacer(Modifier.width(8.dp))
                        Badge { Text("inactive") }
                    }
                    if (era.isLocked) {
                        Spacer(Modifier.width(8.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.tertiary) { Text("locked") }
                    }
                }
                Text(
                    text  = "${era.startYear}–${era.endYear} · id: ${era.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = era.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Switch(
                checked         = checked,
                onCheckedChange = { newValue ->
                    checked = newValue
                    onToggle { checked = !newValue }
                }
            )
        }
    }
}
