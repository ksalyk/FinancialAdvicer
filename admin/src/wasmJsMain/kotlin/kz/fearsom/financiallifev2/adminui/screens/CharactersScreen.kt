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
import kz.fearsom.financiallifev2.admin.UpsertCharacterRequest
import kz.fearsom.financiallifev2.adminui.components.ChipOption
import kz.fearsom.financiallifev2.adminui.components.ConfirmDeleteDialog
import kz.fearsom.financiallifev2.adminui.components.FormField
import kz.fearsom.financiallifev2.adminui.components.MultiSelectChips
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient

private val CHARACTER_TYPES = listOf("PREDEFINED", "BUNDLE")

@Composable
fun CharactersScreen(api: AdminApiClient, onMessage: (String) -> Unit) {
    var characters by remember { mutableStateOf<List<CharacterRow>>(emptyList()) }
    var eras       by remember { mutableStateOf<List<EraRow>>(emptyList()) }
    var loading    by remember { mutableStateOf(true) }
    var error      by remember { mutableStateOf<String?>(null) }
    val scope      = rememberCoroutineScope()

    // Dialog state: editorInitial == null while editor closed; an empty-id row means "create".
    var editorOpen    by remember { mutableStateOf(false) }
    var editorInitial by remember { mutableStateOf<CharacterRow?>(null) } // null = create
    var deleteTarget  by remember { mutableStateOf<CharacterRow?>(null) }
    var busy          by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            loading = true; error = null
            try {
                characters = api.listCharacters()
                eras       = api.listEras()
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun toggle(char: CharacterRow, onRevert: () -> Unit) {
        scope.launch {
            val ok = try {
                if (char.isActive) api.deactivateCharacter(char.id)
                else               api.activateCharacter(char.id)
            } catch (e: Exception) {
                onMessage("Toggle failed: ${e.message}"); false
            }
            if (ok) reload() else onRevert()
        }
    }

    fun save(req: UpsertCharacterRequest) {
        scope.launch {
            busy = true
            try {
                api.upsertCharacter(req)
                editorOpen = false
                onMessage("Saved character '${req.id}'")
                reload()
            } catch (e: Exception) {
                onMessage("Save failed: ${e.message}")
            } finally {
                busy = false
            }
        }
    }

    fun delete(char: CharacterRow) {
        scope.launch {
            busy = true
            try {
                if (api.deleteCharacter(char.id)) {
                    onMessage("Deleted character '${char.id}'")
                    reload()
                } else {
                    onMessage("Delete failed for '${char.id}'")
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
            Text("Characters", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Button(onClick = { editorInitial = null; editorOpen = true }) { Text("+ Add character") }
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
            characters.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No characters yet. Use “+ Add character” to create one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(characters, key = { it.id }) { char ->
                    CharacterCard(
                        character = char,
                        onToggle  = { onRevert -> toggle(char, onRevert) },
                        onEdit    = { editorInitial = char; editorOpen = true },
                        onDelete  = { deleteTarget = char }
                    )
                }
            }
        }
    }

    if (editorOpen) {
        CharacterEditorDialog(
            initial = editorInitial,
            eras    = eras,
            busy    = busy,
            onDismiss = { editorOpen = false },
            onSave  = ::save
        )
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete character?",
            body  = "Hard-deletes '${target.name}' (${target.id}) and cascades all " +
                    "completed-session stats for this character across every user. " +
                    "This cannot be undone. Prefer Deactivate if you only want to hide it.",
            onConfirm = { delete(target) },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun CharacterCard(
    character: CharacterRow,
    onToggle:  (onRevert: () -> Unit) -> Unit,
    onEdit:    () -> Unit,
    onDelete:  () -> Unit
) {
    // Optimistic switch state; remember key includes isActive so a reload resets it
    // to the server's ground truth.
    var checked by remember(character.id, character.isActive) { mutableStateOf(character.isActive) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(character.emoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(character.name, style = MaterialTheme.typography.bodyLarge)
                    if (!checked) {
                        Spacer(Modifier.width(8.dp))
                        Badge { Text("inactive") }
                    }
                }
                Text(
                    text  = "id: ${character.id} · type: ${character.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = "eras: ${character.eraIds.joinToString().ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked         = checked,
                    onCheckedChange = { newValue ->
                        checked = newValue              // immediate visual response
                        onToggle { checked = !newValue }  // revert if server call fails
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
private fun CharacterEditorDialog(
    initial:   CharacterRow?,
    eras:      List<EraRow>,
    busy:      Boolean,
    onDismiss: () -> Unit,
    onSave:    (UpsertCharacterRequest) -> Unit
) {
    val isNew = initial == null

    var id       by remember(initial) { mutableStateOf(initial?.id ?: "") }
    var name     by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var emoji    by remember(initial) { mutableStateOf(initial?.emoji ?: "🧑") }
    var type     by remember(initial) { mutableStateOf(initial?.type ?: CHARACTER_TYPES.first()) }
    var eraIds   by remember(initial) { mutableStateOf(initial?.eraIds?.toSet() ?: emptySet()) }
    var isActive by remember(initial) { mutableStateOf(initial?.isActive ?: true) }

    val idError   = if (id.isBlank()) "Required" else null
    val nameError = if (name.isBlank()) "Required" else null
    val valid     = idError == null && nameError == null && !busy

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New character" else "Edit ${initial?.name}") },
        text = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FormField(
                    value         = id,
                    onValueChange = { id = it.trim() },
                    label         = "ID (immutable)",
                    enabled       = isNew,           // never rename — that would create a duplicate
                    errorText     = if (isNew) idError else null
                )
                FormField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = "Name",
                    errorText     = nameError
                )
                FormField(
                    value         = emoji,
                    onValueChange = { emoji = it },
                    label         = "Emoji"
                )

                Text(
                    "Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CHARACTER_TYPES.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick  = { type = t },
                            label    = { Text(t) }
                        )
                    }
                }

                MultiSelectChips(
                    label     = "Eras (eraIds)",
                    options   = eras.map { ChipOption(it.id, "${it.emoji} ${it.name}") },
                    selected  = eraIds,
                    onToggle  = { eId -> eraIds = eraIds.toggle(eId) },
                    emptyHint = "No eras exist yet — create one in the Eras tab first."
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Active", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        UpsertCharacterRequest(
                            id       = id.trim(),
                            name     = name.trim(),
                            emoji    = emoji.trim(),
                            type     = type,
                            eraIds   = eraIds.toList(),
                            isActive = isActive
                        )
                    )
                }
            ) { Text(if (busy) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value
