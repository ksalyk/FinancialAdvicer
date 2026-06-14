package kz.fearsom.financiallifev2.adminui.screens

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
import kz.fearsom.financiallifev2.admin.CharacterRow
import kz.fearsom.financiallifev2.admin.EraRow
import kz.fearsom.financiallifev2.admin.UpsertEraRequest
import kz.fearsom.financiallifev2.adminui.components.ChipOption
import kz.fearsom.financiallifev2.adminui.components.ConfirmDeleteDialog
import kz.fearsom.financiallifev2.adminui.components.FormField
import kz.fearsom.financiallifev2.adminui.components.MultiSelectChips
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient

@Composable
fun ErasScreen(api: AdminApiClient, onMessage: (String) -> Unit) {
    var eras       by remember { mutableStateOf<List<EraRow>>(emptyList()) }
    var characters by remember { mutableStateOf<List<CharacterRow>>(emptyList()) }
    var loading    by remember { mutableStateOf(true) }
    var error      by remember { mutableStateOf<String?>(null) }
    val scope      = rememberCoroutineScope()

    var editorOpen    by remember { mutableStateOf(false) }
    var editorInitial by remember { mutableStateOf<EraRow?>(null) } // null = create
    var deleteTarget  by remember { mutableStateOf<EraRow?>(null) }
    var busy          by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            loading = true; error = null
            try {
                eras       = api.listEras()
                characters = api.listCharacters()
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun toggle(era: EraRow, onRevert: () -> Unit) {
        scope.launch {
            val ok = try {
                if (era.isActive) api.deactivateEra(era.id)
                else              api.activateEra(era.id)
            } catch (e: Exception) {
                onMessage("Toggle failed: ${e.message}"); false
            }
            if (ok) reload() else onRevert()
        }
    }

    fun save(req: UpsertEraRequest) {
        scope.launch {
            busy = true
            try {
                api.upsertEra(req)
                editorOpen = false
                onMessage("Saved era '${req.id}'")
                reload()
            } catch (e: Exception) {
                onMessage("Save failed: ${e.message}")
            } finally {
                busy = false
            }
        }
    }

    fun delete(era: EraRow) {
        scope.launch {
            busy = true
            try {
                if (api.deleteEra(era.id)) {
                    onMessage("Deleted era '${era.id}'")
                    reload()
                } else {
                    onMessage("Delete failed for '${era.id}'")
                }
            } catch (e: Exception) {
                onMessage("Delete failed: ${e.message}")
            } finally {
                busy = false
                deleteTarget = null
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Eras", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Button(onClick = { editorInitial = null; editorOpen = true }) { Text("+ Add era") }
        }
        Spacer(Modifier.height(12.dp))

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Column {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { reload() }) { Text("Retry") }
            }
            eras.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No eras yet. Use “+ Add era” to create one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(eras, key = { it.id }) { era ->
                    EraCard(
                        era      = era,
                        onToggle = { onRevert -> toggle(era, onRevert) },
                        onEdit   = { editorInitial = era; editorOpen = true },
                        onDelete = { deleteTarget = era }
                    )
                }
            }
        }
    }

    if (editorOpen) {
        EraEditorDialog(
            initial    = editorInitial,
            characters = characters,
            busy       = busy,
            onDismiss  = { editorOpen = false },
            onSave     = ::save
        )
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete era?",
            body  = "Hard-deletes '${target.name}' (${target.id}) and cascades all " +
                    "completed-session stats for this era across every user. " +
                    "This cannot be undone. Prefer Deactivate if you only want to hide it.",
            onConfirm = { delete(target) },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun EraCard(
    era:      EraRow,
    onToggle: (onRevert: () -> Unit) -> Unit,
    onEdit:   () -> Unit,
    onDelete: () -> Unit
) {
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
                    text     = era.description,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked         = checked,
                    onCheckedChange = { newValue ->
                        checked = newValue
                        onToggle { checked = !newValue }
                    }
                )
                Row {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    TextButton(
                        onClick = onDelete,
                        colors  = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun EraEditorDialog(
    initial:    EraRow?,
    characters: List<CharacterRow>,
    busy:       Boolean,
    onDismiss:  () -> Unit,
    onSave:     (UpsertEraRequest) -> Unit
) {
    val isNew = initial == null

    var id          by remember(initial) { mutableStateOf(initial?.id ?: "") }
    var name        by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
    var emoji       by remember(initial) { mutableStateOf(initial?.emoji ?: "📅") }
    var startYear   by remember(initial) { mutableStateOf(initial?.startYear?.toString() ?: "") }
    var endYear     by remember(initial) { mutableStateOf(initial?.endYear?.toString() ?: "") }
    var charIds     by remember(initial) { mutableStateOf(initial?.availableCharacterIds?.toSet() ?: emptySet()) }
    var isActive    by remember(initial) { mutableStateOf(initial?.isActive ?: true) }
    var isLocked    by remember(initial) { mutableStateOf(initial?.isLocked ?: false) }

    val start = startYear.trim().toIntOrNull()
    val end   = endYear.trim().toIntOrNull()

    val idError    = if (id.isBlank()) "Required" else null
    val nameError  = if (name.isBlank()) "Required" else null
    val startError = if (start == null) "Whole number" else null
    val endError   = when {
        end == null -> "Whole number"
        start != null && end < start -> "Must be ≥ start year"
        else -> null
    }
    val valid = idError == null && nameError == null && startError == null && endError == null && !busy

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New era" else "Edit ${initial?.name}") },
        text = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FormField(
                    value         = id,
                    onValueChange = { id = it.trim() },
                    label         = "ID (immutable)",
                    enabled       = isNew,
                    errorText     = if (isNew) idError else null
                )
                FormField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = "Name",
                    errorText     = nameError
                )
                FormField(
                    value         = description,
                    onValueChange = { description = it },
                    label         = "Description",
                    singleLine    = false
                )
                FormField(
                    value         = emoji,
                    onValueChange = { emoji = it },
                    label         = "Emoji"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        value         = startYear,
                        onValueChange = { startYear = it.filter(Char::isDigit) },
                        label         = "Start year",
                        numeric       = true,
                        errorText     = startError,
                        modifier      = Modifier.weight(1f)
                    )
                    FormField(
                        value         = endYear,
                        onValueChange = { endYear = it.filter(Char::isDigit) },
                        label         = "End year",
                        numeric       = true,
                        errorText     = endError,
                        modifier      = Modifier.weight(1f)
                    )
                }

                MultiSelectChips(
                    label     = "Available characters",
                    options   = characters.map { ChipOption(it.id, "${it.emoji} ${it.name}") },
                    selected  = charIds,
                    onToggle  = { cId -> charIds = charIds.toggleId(cId) },
                    emptyHint = "No characters exist yet — create one in the Characters tab first."
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Active", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(24.dp))
                    Switch(checked = isLocked, onCheckedChange = { isLocked = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Locked", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        UpsertEraRequest(
                            id                    = id.trim(),
                            name                  = name.trim(),
                            description           = description.trim(),
                            emoji                 = emoji.trim(),
                            startYear             = start ?: 0,
                            endYear               = end ?: 0,
                            availableCharacterIds = charIds.toList(),
                            isActive              = isActive,
                            isLocked              = isLocked
                        )
                    )
                }
            ) { Text(if (busy) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun Set<String>.toggleId(value: String): Set<String> =
    if (value in this) this - value else this + value
