package kz.fearsom.financiallifev2.adminui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kz.fearsom.financiallifev2.admin.AdminUserDetailRow
import kz.fearsom.financiallifev2.admin.AdminUserListResponse
import kz.fearsom.financiallifev2.admin.AdminUserRow
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient

@Composable
fun UsersScreen(api: AdminApiClient) {
    var state    by remember { mutableStateOf<AdminUserListResponse?>(null) }
    var loading  by remember { mutableStateOf(true) }
    var error    by remember { mutableStateOf<String?>(null) }
    var search   by remember { mutableStateOf("") }
    var offset   by remember { mutableStateOf(0L) }
    val limit    = 50

    // Detail / delete / reset dialogs
    var detailUser   by remember { mutableStateOf<AdminUserDetailRow?>(null) }
    var resetTarget  by remember { mutableStateOf<AdminUserRow?>(null) }
    var newPassword  by remember { mutableStateOf("") }
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

    LaunchedEffect(offset, search) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        // ── Search bar ────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value         = search,
                onValueChange = { search = it; offset = 0 },
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
            error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
            state != null -> {
                Text(
                    text  = "Total: ${state!!.total}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(state!!.items) { user ->
                        UserRow(
                            user          = user,
                            onDetail      = {
                                scope.launch {
                                    try { detailUser = api.getUserDetail(user.id) }
                                    catch (e: Exception) { error = e.message }
                                }
                            },
                            onResetPw     = { resetTarget = user; newPassword = "" },
                            onDelete      = {
                                scope.launch {
                                    actionLoading = true
                                    try { if (api.deleteUser(user.id)) reload() }
                                    catch (e: Exception) { error = e.message }
                                    finally { actionLoading = false }
                                }
                            }
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

    // ── Detail dialog ─────────────────────────────────────────────────────────
    detailUser?.let { detail ->
        AlertDialog(
            onDismissRequest = { detailUser = null },
            title            = { Text(detail.username) },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                }
            },
            confirmButton = { TextButton(onClick = { detailUser = null }) { Text("Close") } }
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
                                resetTarget = null
                            } catch (e: Exception) {
                                error = e.message
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
}

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
