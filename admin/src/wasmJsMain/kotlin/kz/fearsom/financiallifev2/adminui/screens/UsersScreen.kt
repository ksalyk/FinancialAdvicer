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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kz.fearsom.financiallifev2.achievements.AchievementUnlockDto
import kz.fearsom.financiallifev2.admin.AchievementAdminRow
import kz.fearsom.financiallifev2.admin.AdminUserDetailRow
import kz.fearsom.financiallifev2.admin.AdminUserListResponse
import kz.fearsom.financiallifev2.admin.AdminUserRow
import kz.fearsom.financiallifev2.adminui.components.ConfirmDeleteDialog
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient

private const val SEARCH_DEBOUNCE_MS = 350L

@Composable
fun UsersScreen(api: AdminApiClient, onMessage: (String) -> Unit) {
    var state    by remember { mutableStateOf<AdminUserListResponse?>(null) }
    var loading  by remember { mutableStateOf(true) }
    var error    by remember { mutableStateOf<String?>(null) }
    var search   by remember { mutableStateOf("") }
    var offset   by remember { mutableStateOf(0L) }
    val limit    = 50

    // Detail / delete / reset dialogs
    var detailUser    by remember { mutableStateOf<AdminUserDetailRow?>(null) }
    var resetTarget   by remember { mutableStateOf<AdminUserRow?>(null) }
    var deleteTarget  by remember { mutableStateOf<AdminUserRow?>(null) }
    var newPassword   by remember { mutableStateOf("") }
    var actionLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true; error = null
            try { state = api.listUsers(limit, offset, search.takeIf { it.isNotBlank() }) }
            catch (e: Exception) { error = e.message }
            finally { loading = false }
        }
    }

    // Debounced search: one request per pause in typing, not one per keystroke.
    LaunchedEffect(search) {
        if (state != null) delay(SEARCH_DEBOUNCE_MS)
        offset = 0
        reload()
    }
    LaunchedEffect(offset) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        // ── Search bar ────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value         = search,
                onValueChange = { search = it },
                label         = { Text("Search username") },
                singleLine    = true,
                modifier      = Modifier.weight(1f)
            )
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
            state != null -> {
                Text(
                    text  = "Total: ${state!!.total}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state!!.items, key = { it.id }) { user ->
                        UserRow(
                            user      = user,
                            onDetail  = {
                                scope.launch {
                                    try { detailUser = api.getUserDetail(user.id) }
                                    catch (e: Exception) { onMessage("Load failed: ${e.message}") }
                                }
                            },
                            onResetPw = { resetTarget = user; newPassword = "" },
                            onDelete  = { deleteTarget = user }
                        )
                    }
                }

                // ── Pagination ────────────────────────────────────────────────
                Row(
                    modifier            = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick  = { offset = (offset - limit).coerceAtLeast(0) },
                        enabled  = offset > 0
                    ) { Text("← Prev") }
                    Text(
                        text     = "Page ${offset / limit + 1}",
                        modifier = Modifier.padding(horizontal = 16.dp).align(Alignment.CenterVertically)
                    )
                    TextButton(
                        onClick  = { offset += limit },
                        enabled  = offset + limit < (state!!.total)
                    ) { Text("Next →") }
                }
            }
        }
    }

    // ── Detail dialog (stats + achievements management) ───────────────────────
    detailUser?.let { detail ->
        UserDetailDialog(
            api       = api,
            detail    = detail,
            onMessage = onMessage,
            onDismiss = { detailUser = null }
        )
    }

    // ── Reset-password dialog ─────────────────────────────────────────────────
    resetTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { resetTarget = null },
            title            = { Text("Reset password — ${target.username}") },
            text             = {
                OutlinedTextField(
                    value               = newPassword,
                    onValueChange       = { newPassword = it },
                    label               = { Text("New password (min 6 chars)") },
                    singleLine          = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            },
            confirmButton = {
                TextButton(
                    onClick  = {
                        scope.launch {
                            actionLoading = true
                            try {
                                api.resetPassword(target.id, newPassword)
                                onMessage("Password reset for ${target.username}")
                                resetTarget = null
                            } catch (e: Exception) {
                                onMessage("Reset failed: ${e.message}")
                            } finally {
                                actionLoading = false
                            }
                        }
                    },
                    enabled = newPassword.length >= 6 && !actionLoading
                ) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { resetTarget = null }) { Text("Cancel") } }
        )
    }

    // ── Delete confirmation (was a single-tap hard delete before) ─────────────
    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete user?",
            body  = "Permanently deletes '${target.username}' with all sessions, statistics " +
                    "and achievements. This cannot be undone.",
            onConfirm = {
                scope.launch {
                    actionLoading = true
                    try {
                        api.deleteUser(target.id)
                        onMessage("Deleted user '${target.username}'")
                        reload()
                    } catch (e: Exception) {
                        onMessage("Delete failed: ${e.message}")
                    } finally {
                        actionLoading = false
                        deleteTarget = null
                    }
                }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

// ── User detail dialog with achievements section ──────────────────────────────

@Composable
private fun UserDetailDialog(
    api: AdminApiClient,
    detail: AdminUserDetailRow,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var unlocks    by remember { mutableStateOf<List<AchievementUnlockDto>>(emptyList()) }
    var catalog    by remember { mutableStateOf<List<AchievementAdminRow>>(emptyList()) }
    var achLoading by remember { mutableStateOf(true) }
    var grantOpen  by remember { mutableStateOf(false) }
    val scope      = rememberCoroutineScope()

    fun reloadAchievements() {
        scope.launch {
            achLoading = true
            try {
                unlocks = api.listUserAchievements(detail.id)
                if (catalog.isEmpty()) catalog = api.listAchievements()
            } catch (e: Exception) {
                onMessage("Achievements load failed: ${e.message}")
            } finally {
                achLoading = false
            }
        }
    }

    LaunchedEffect(detail.id) { reloadAchievements() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text(detail.username) },
        text             = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DetailRow("ID", detail.id)
                DetailRow("Created", formatTs(detail.createdAt))
                DetailRow("Games played", detail.gamesPlayed.toString())
                DetailRow("Best ending", detail.bestEnding ?: "—")
                DetailRow("Avg capital at end", formatMoney(detail.averageCapitalAtEnd))
                if (detail.endingDistribution.isNotEmpty()) {
                    Text("Ending distribution:", style = MaterialTheme.typography.labelMedium)
                    detail.endingDistribution.forEach { (ending, count) ->
                        Text("  $ending: $count", style = MaterialTheme.typography.bodySmall)
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Achievements (${unlocks.size})", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { grantOpen = !grantOpen }) {
                        Text(if (grantOpen) "Close" else "Grant…")
                    }
                }

                if (achLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                } else {
                    if (unlocks.isEmpty()) {
                        Text("No achievements yet.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    unlocks.forEach { u ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${achievementEmoji(catalog, u.achievementId)} ${u.achievementId}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(formatTs(u.unlockedAt), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        api.revokeAchievement(detail.id, u.achievementId)
                                        onMessage("Revoked ${u.achievementId}")
                                        reloadAchievements()
                                    } catch (e: Exception) {
                                        onMessage("Revoke failed: ${e.message}")
                                    }
                                }
                            }) { Text("Revoke") }
                        }
                    }

                    if (grantOpen) {
                        val unlocked = unlocks.map { it.achievementId }.toSet()
                        val grantable = catalog.filter { it.definition.id !in unlocked }
                        if (grantable.isEmpty()) {
                            Text("Everything already unlocked.", style = MaterialTheme.typography.bodySmall)
                        }
                        grantable.forEach { row ->
                            TextButton(onClick = {
                                scope.launch {
                                    try {
                                        api.grantAchievement(detail.id, row.definition.id)
                                        onMessage("Granted ${row.definition.id}")
                                        reloadAchievements()
                                    } catch (e: Exception) {
                                        onMessage("Grant failed: ${e.message}")
                                    }
                                }
                            }) {
                                Text("+ ${row.definition.emoji} ${row.definition.id}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun achievementEmoji(catalog: List<AchievementAdminRow>, id: String): String =
    catalog.firstOrNull { it.definition.id == id }?.definition?.emoji ?: "🏅"

// ── User row ──────────────────────────────────────────────────────────────────

@Composable
private fun UserRow(
    user:     AdminUserRow,
    onDetail: () -> Unit,
    onResetPw: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onDetail)) {
        Row(
            modifier            = Modifier.padding(12.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.username, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text  = "games: ${user.gamesPlayed} · ${formatTs(user.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                TextButton(onClick = onResetPw) { Text("Reset PW") }
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )) { Text("Delete") }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun DetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text     = "$label:",
            style    = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(140.dp)
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsDateToLocaleString(ms: Double): String =
    js("new Date(ms).toLocaleDateString()")

// Simple epoch → date string via JS Date (good enough for an admin panel).
// Kotlin/Wasm requires `js("…")` to be a single constant string literal with no
// Kotlin-side interpolation, so we can't inline `$ms` directly. Instead we use a
// helper function with a typed parameter; the compiler passes `ms` as a proper
// JS Number and the JS snippet just references the parameter name.
private fun formatTs(ms: Long): String = jsDateToLocaleString(ms.toDouble())

private fun formatMoney(tg: Long): String = "${tg / 1000}k ₸"
