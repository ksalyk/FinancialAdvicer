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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kz.fearsom.financiallifev2.admin.ScenarioComboDto
import kz.fearsom.financiallifev2.admin.ScenarioGraphDto
import kz.fearsom.financiallifev2.admin.StoryDetail
import kz.fearsom.financiallifev2.admin.StoryRow
import kz.fearsom.financiallifev2.admin.StorySource
import kz.fearsom.financiallifev2.admin.StoryStatus
import kz.fearsom.financiallifev2.admin.UpsertStoryRequest
import kz.fearsom.financiallifev2.adminui.components.ConfirmDeleteDialog
import kz.fearsom.financiallifev2.adminui.components.FormField
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.model.PlayerState

/**
 * DB-backed story management: list + status filter + moderation actions.
 * Selecting "Edit" opens the full-screen [StoryEditorScreen].
 */
@Composable
fun StoriesScreen(api: AdminApiClient, onMessage: (String) -> Unit) {
    var stories      by remember { mutableStateOf<List<StoryRow>>(emptyList()) }
    var filter       by remember { mutableStateOf<StoryStatus?>(null) }
    var loading      by remember { mutableStateOf(true) }
    var error        by remember { mutableStateOf<String?>(null) }
    var editing      by remember { mutableStateOf<StoryDetail?>(null) }
    var createOpen   by remember { mutableStateOf(false) }
    var cloneOpen    by remember { mutableStateOf(false) }
    var rejectTarget by remember { mutableStateOf<StoryRow?>(null) }
    var deleteTarget by remember { mutableStateOf<StoryRow?>(null) }
    var busy         by remember { mutableStateOf(false) }
    val scope        = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true; error = null
            try { stories = api.listStories(filter) }
            catch (e: Exception) { error = e.message }
            finally { loading = false }
        }
    }

    LaunchedEffect(filter) { reload() }

    /** Runs a moderation action, reports errors via snackbar, reloads on success. */
    fun action(label: String, block: suspend () -> Unit) {
        scope.launch {
            busy = true
            try {
                block()
                onMessage("$label — ok")
                reload()
            } catch (e: Exception) {
                onMessage("$label failed: ${e.message}")
            } finally {
                busy = false
            }
        }
    }

    // ── Full-screen editor takes over the tab ─────────────────────────────────
    val current = editing
    if (current != null) {
        StoryEditorScreen(
            api       = api,
            initial   = current,
            onMessage = onMessage,
            onClose   = { editing = null; reload() }
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Stories", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { cloneOpen = true }) { Text("Clone built-in") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { createOpen = true }) { Text("+ New story") }
        }
        Spacer(Modifier.height(8.dp))

        // ── Status filter chips ───────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
            StoryStatus.entries.forEach { s ->
                FilterChip(
                    selected = filter == s,
                    onClick  = { filter = if (filter == s) null else s },
                    label    = { Text(s.name.lowercase().replace('_', ' ')) }
                )
            }
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
            stories.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (filter == null) "No stories yet. Create one or clone a built-in graph."
                    else "No stories with status ${filter!!.name}.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(stories, key = { it.id }) { story ->
                    StoryCard(
                        story    = story,
                        busy     = busy,
                        onEdit   = {
                            scope.launch {
                                try { editing = api.getStory(story.id) }
                                catch (e: Exception) { onMessage("Load failed: ${e.message}") }
                            }
                        },
                        onPublish   = { action("Publish '${story.id}'")   { api.publishStory(story.id) } },
                        onUnpublish = { action("Unpublish '${story.id}'") { api.unpublishStory(story.id) } },
                        onApprove   = { action("Approve '${story.id}'")   { api.approveStory(story.id) } },
                        onReject    = { rejectTarget = story },
                        onSubmit    = { action("Submit '${story.id}'")    { api.submitStory(story.id) } },
                        onDelete    = { deleteTarget = story }
                    )
                }
            }
        }
    }

    if (createOpen) {
        NewStoryDialog(
            busy      = busy,
            onDismiss = { createOpen = false },
            onCreate  = { req ->
                scope.launch {
                    busy = true
                    try {
                        val created = api.createStory(req)
                        createOpen = false
                        editing = created           // jump straight into the editor
                    } catch (e: Exception) {
                        onMessage("Create failed: ${e.message}")
                    } finally {
                        busy = false
                    }
                }
            }
        )
    }

    if (cloneOpen) {
        CloneBuiltInDialog(
            api       = api,
            onDismiss = { cloneOpen = false },
            onCloned  = { detail ->
                cloneOpen = false
                onMessage("Cloned to '${detail.row.id}'")
                editing = detail
            },
            onError   = { onMessage(it) }
        )
    }

    rejectTarget?.let { target ->
        RejectDialog(
            story     = target,
            busy      = busy,
            onDismiss = { rejectTarget = null },
            onReject  = { note ->
                rejectTarget = null
                action("Reject '${target.id}'") { api.rejectStory(target.id, note) }
            }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete story?",
            body  = "Permanently deletes '${target.title}' (${target.id}). " +
                    if (target.status == StoryStatus.PUBLISHED)
                        "It is PUBLISHED — players will lose access immediately. Prefer Unpublish."
                    else "This cannot be undone.",
            onConfirm = {
                deleteTarget = null
                action("Delete '${target.id}'") { api.deleteStory(target.id) }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

// ── Story card ────────────────────────────────────────────────────────────────

@Composable
private fun StoryCard(
    story: StoryRow,
    busy: Boolean,
    onEdit: () -> Unit,
    onPublish: () -> Unit,
    onUnpublish: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onSubmit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(story.title, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(8.dp))
                StatusBadge(story.status)
                if (story.source != StorySource.ADMIN) {
                    Spacer(Modifier.width(4.dp))
                    Badge { Text(story.source.name.lowercase().replace('_', ' ')) }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${story.eventCount} events · ${story.endingCount} endings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "id: ${story.id} · ${story.characterId} × ${story.eraId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (story.status == StoryStatus.REJECTED && !story.reviewNote.isNullOrBlank()) {
                Text(
                    "review: ${story.reviewNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit, enabled = !busy) { Text("Edit") }
                when (story.status) {
                    StoryStatus.DRAFT -> {
                        TextButton(onClick = onPublish, enabled = !busy) { Text("Publish") }
                        TextButton(onClick = onSubmit, enabled = !busy) { Text("To review") }
                    }
                    StoryStatus.PENDING_REVIEW -> {
                        TextButton(onClick = onApprove, enabled = !busy) { Text("Approve") }
                        TextButton(
                            onClick = onReject, enabled = !busy,
                            colors  = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Reject") }
                    }
                    StoryStatus.PUBLISHED -> {
                        TextButton(onClick = onUnpublish, enabled = !busy) { Text("Unpublish") }
                    }
                    StoryStatus.REJECTED -> {
                        TextButton(onClick = onSubmit, enabled = !busy) { Text("Resubmit") }
                    }
                    StoryStatus.ARCHIVED -> {
                        TextButton(onClick = onPublish, enabled = !busy) { Text("Republish") }
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onDelete, enabled = !busy,
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            }
        }
    }
}

@Composable
internal fun StatusBadge(status: StoryStatus) {
    val (bg, label) = when (status) {
        StoryStatus.DRAFT          -> MaterialTheme.colorScheme.surfaceVariant to "draft"
        StoryStatus.PENDING_REVIEW -> Color(0xFFF9A825) to "review"
        StoryStatus.PUBLISHED      -> Color(0xFF2E7D32) to "published"
        StoryStatus.REJECTED       -> MaterialTheme.colorScheme.errorContainer to "rejected"
        StoryStatus.ARCHIVED       -> MaterialTheme.colorScheme.outline to "archived"
    }
    Badge(containerColor = bg) { Text(label) }
}

// ── New story dialog ──────────────────────────────────────────────────────────

/** Skeleton graph every new story starts from: intro → happy ending. */
internal fun skeletonGraph(characterId: String, eraId: String) = ScenarioGraphDto(
    initialPlayerState = PlayerState(characterId = characterId, eraId = eraId),
    events = listOf(
        GameEvent(
            id      = "intro",
            message = "Привет! Это начало новой истории. Отредактируй меня.",
            flavor  = "👋",
            options = listOf(
                GameOption(id = "start", text = "Начать", emoji = "🚀", next = "ending_good")
            )
        ),
        GameEvent(
            id         = "ending_good",
            message    = "Конец истории. Отредактируй меня.",
            flavor     = "🏁",
            options    = emptyList(),
            isEnding   = true,
            endingType = EndingType.FINANCIAL_STABILITY
        )
    ),
    conditionalEvents = emptyList(),
    eventPool         = emptyList()
)

@Composable
private fun NewStoryDialog(
    busy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (UpsertStoryRequest) -> Unit
) {
    var id          by remember { mutableStateOf("") }
    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var characterId by remember { mutableStateOf("") }
    var eraId       by remember { mutableStateOf("") }

    val idError = when {
        id.isBlank() -> "Required"
        !Regex("^[a-z0-9][a-z0-9_\\-]{2,63}$").matches(id) -> "lowercase, digits, _ or - (3–64)"
        else -> null
    }
    val valid = idError == null && title.isNotBlank() &&
        characterId.isNotBlank() && eraId.isNotBlank() && !busy

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New story") },
        text  = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FormField(id, { id = it.trim() }, "ID (immutable)", errorText = idError)
                FormField(title, { title = it }, "Title",
                    errorText = if (title.isBlank()) "Required" else null)
                FormField(description, { description = it }, "Description", singleLine = false)
                FormField(characterId, { characterId = it.trim() }, "Character id",
                    errorText = if (characterId.isBlank()) "Required" else null)
                FormField(eraId, { eraId = it.trim() }, "Era id",
                    errorText = if (eraId.isBlank()) "Required" else null)
                Text(
                    "Starts as a DRAFT with a 2-event skeleton (intro → ending).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onCreate(
                        UpsertStoryRequest(
                            id          = id.trim(),
                            title       = title.trim(),
                            description = description.trim(),
                            characterId = characterId.trim(),
                            eraId       = eraId.trim(),
                            graph       = skeletonGraph(characterId.trim(), eraId.trim())
                        )
                    )
                }
            ) { Text(if (busy) "Creating…" else "Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Clone built-in dialog ─────────────────────────────────────────────────────

@Composable
private fun CloneBuiltInDialog(
    api: AdminApiClient,
    onDismiss: () -> Unit,
    onCloned: (StoryDetail) -> Unit,
    onError: (String) -> Unit
) {
    var combos  by remember { mutableStateOf<List<ScenarioComboDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy    by remember { mutableStateOf(false) }
    val scope   = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try { combos = api.listScenarioCombos() }
        catch (e: Exception) { onError("Failed to load combos: ${e.message}") }
        finally { loading = false }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clone built-in graph") },
        text  = {
            when {
                loading -> CircularProgressIndicator()
                combos.isEmpty() -> Text("No character × era combos found.")
                else -> Column(
                    modifier            = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Snapshots the code-authored graph into an editable DRAFT.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    combos.forEach { combo ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled  = !busy,
                            onClick  = {
                                scope.launch {
                                    busy = true
                                    try {
                                        onCloned(api.cloneBuiltIn(combo.characterId, combo.eraId))
                                    } catch (e: Exception) {
                                        onError("Clone failed: ${e.message}")
                                    } finally {
                                        busy = false
                                    }
                                }
                            }
                        ) { Text(combo.label) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Reject dialog ─────────────────────────────────────────────────────────────

@Composable
private fun RejectDialog(
    story: StoryRow,
    busy: Boolean,
    onDismiss: () -> Unit,
    onReject: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject '${story.title}'") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "The note is shown to the author so they can fix the story.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FormField(note, { note = it }, "Review note", singleLine = false)
            }
        },
        confirmButton = {
            TextButton(
                enabled = note.isNotBlank() && !busy,
                onClick = { onReject(note.trim()) }
            ) { Text("Reject") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
