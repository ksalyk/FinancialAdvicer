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
import kz.fearsom.financiallifev2.achievements.AchievementCondition
import kz.fearsom.financiallifev2.achievements.AchievementDefinition
import kz.fearsom.financiallifev2.achievements.AchievementKind
import kz.fearsom.financiallifev2.achievements.AchievementRarity
import kz.fearsom.financiallifev2.achievements.LocalizedText
import kz.fearsom.financiallifev2.admin.AchievementAdminRow
import kz.fearsom.financiallifev2.admin.UpsertAchievementRequest
import kz.fearsom.financiallifev2.adminui.components.ConfirmDeleteDialog
import kz.fearsom.financiallifev2.adminui.components.FormField
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient

// ── Condition editor model ───────────────────────────────────────────────────

private enum class ConditionType(val label: String) {
    ANY_FLAG("Any flag set"),
    FIRST_MONTHLY_REPORT("First monthly report"),
    EMERGENCY_FUND("Emergency fund (months)"),
    DEBT_FREE("Debt-free after debt"),
    CALM_THROUGH_CRISIS("Calm through crisis"),
    STORY_COMPLETED("Story completed");

    companion object {
        fun of(c: AchievementCondition): ConditionType = when (c) {
            is AchievementCondition.AnyFlag            -> ANY_FLAG
            is AchievementCondition.FirstMonthlyReport -> FIRST_MONTHLY_REPORT
            is AchievementCondition.EmergencyFund      -> EMERGENCY_FUND
            is AchievementCondition.DebtFree           -> DEBT_FREE
            is AchievementCondition.CalmThroughCrisis  -> CALM_THROUGH_CRISIS
            is AchievementCondition.StoryCompleted     -> STORY_COMPLETED
        }
    }
}

@Composable
fun AchievementsScreen(api: AdminApiClient, onMessage: (String) -> Unit) {
    var rows    by remember { mutableStateOf<List<AchievementAdminRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf<String?>(null) }
    val scope   = rememberCoroutineScope()

    var editorOpen    by remember { mutableStateOf(false) }
    var editorInitial by remember { mutableStateOf<AchievementAdminRow?>(null) }
    var deleteTarget  by remember { mutableStateOf<AchievementAdminRow?>(null) }
    var busy          by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            loading = true; error = null
            try { rows = api.listAchievements() }
            catch (e: Exception) { error = e.message }
            finally { loading = false }
        }
    }

    fun toggle(row: AchievementAdminRow, onRevert: () -> Unit) {
        scope.launch {
            try {
                if (row.isActive) api.deactivateAchievement(row.definition.id)
                else              api.activateAchievement(row.definition.id)
                reload()
            } catch (e: Exception) {
                onMessage("Toggle failed: ${e.message}")
                onRevert()
            }
        }
    }

    fun save(req: UpsertAchievementRequest) {
        scope.launch {
            busy = true
            try {
                api.upsertAchievement(req)
                editorOpen = false
                onMessage("Saved achievement '${req.definition.id}'")
                reload()
            } catch (e: Exception) {
                onMessage("Save failed: ${e.message}")
            } finally {
                busy = false
            }
        }
    }

    fun delete(row: AchievementAdminRow) {
        scope.launch {
            busy = true
            try {
                api.deleteAchievement(row.definition.id)
                onMessage("Deleted achievement '${row.definition.id}'")
                reload()
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
            Text("Achievements", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Button(onClick = { editorInitial = null; editorOpen = true }) { Text("+ Add achievement") }
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
            rows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Catalog is empty — the server seeds it from code on startup.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(rows, key = { it.definition.id }) { row ->
                    AchievementCard(
                        row      = row,
                        onToggle = { onRevert -> toggle(row, onRevert) },
                        onEdit   = { editorInitial = row; editorOpen = true },
                        onDelete = { deleteTarget = row }
                    )
                }
            }
        }
    }

    if (editorOpen) {
        AchievementEditorDialog(
            initial   = editorInitial,
            busy      = busy,
            onDismiss = { editorOpen = false },
            onSave    = ::save
        )
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete achievement?",
            body  = "Hard-deletes '${target.definition.id}' together with every user's unlock " +
                    "and feedback rows. Only CUSTOM achievements can be deleted; " +
                    "seeded ones can only be deactivated.",
            onConfirm = { delete(target) },
            onDismiss = { deleteTarget = null }
        )
    }
}

// ── Card ──────────────────────────────────────────────────────────────────────

@Composable
private fun AchievementCard(
    row:      AchievementAdminRow,
    onToggle: (onRevert: () -> Unit) -> Unit,
    onEdit:   () -> Unit,
    onDelete: () -> Unit
) {
    val def = row.definition
    var checked by remember(def.id, row.isActive) { mutableStateOf(row.isActive) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(def.emoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(displayTitle(def), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    Badge { Text(def.kind.name.lowercase()) }
                    Spacer(Modifier.width(4.dp))
                    Badge(containerColor = rarityColor(def.rarity)) { Text(def.rarity.name.lowercase()) }
                    if (row.source == "CUSTOM") {
                        Spacer(Modifier.width(4.dp))
                        Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text("custom", color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                    if (!checked) {
                        Spacer(Modifier.width(4.dp))
                        Badge { Text("inactive") }
                    }
                }
                Text(
                    text  = "id: ${def.id} · ${conditionSummary(def.condition)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = "unlocked by ${row.unlockCount} · 👍 ${row.upVotes} · 👎 ${row.downVotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        enabled = row.source == "CUSTOM",
                        colors  = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                }
            }
        }
    }
}

// ── Editor dialog ─────────────────────────────────────────────────────────────

@Composable
private fun AchievementEditorDialog(
    initial:   AchievementAdminRow?,
    busy:      Boolean,
    onDismiss: () -> Unit,
    onSave:    (UpsertAchievementRequest) -> Unit
) {
    val isNew = initial == null
    val def   = initial?.definition

    var id        by remember(initial) { mutableStateOf(def?.id ?: "") }
    var kind      by remember(initial) { mutableStateOf(def?.kind ?: AchievementKind.GAME) }
    var emoji     by remember(initial) { mutableStateOf(def?.emoji ?: "🏆") }
    var rarity    by remember(initial) { mutableStateOf(def?.rarity ?: AchievementRarity.COMMON) }
    var sortOrder by remember(initial) { mutableStateOf((initial?.sortOrder ?: 0).toString()) }
    var isActive  by remember(initial) { mutableStateOf(initial?.isActive ?: true) }

    // Inline texts (win over i18n keys for display).
    var titleRu by remember(initial) { mutableStateOf(def?.titleText?.ru ?: "") }
    var titleEn by remember(initial) { mutableStateOf(def?.titleText?.en ?: "") }
    var titleKk by remember(initial) { mutableStateOf(def?.titleText?.kk ?: "") }
    var descRu  by remember(initial) { mutableStateOf(def?.descText?.ru ?: "") }
    var descEn  by remember(initial) { mutableStateOf(def?.descText?.en ?: "") }
    var descKk  by remember(initial) { mutableStateOf(def?.descText?.kk ?: "") }
    var hintRu  by remember(initial) { mutableStateOf(def?.hintText?.ru ?: "") }

    // Condition editor state.
    var conditionType by remember(initial) {
        mutableStateOf(def?.condition?.let(ConditionType::of) ?: ConditionType.ANY_FLAG)
    }
    var flagsText  by remember(initial) {
        mutableStateOf((def?.condition as? AchievementCondition.AnyFlag)?.flags?.joinToString(", ") ?: "")
    }
    var monthsText by remember(initial) {
        mutableStateOf(when (val c = def?.condition) {
            is AchievementCondition.EmergencyFund     -> c.months.toString()
            is AchievementCondition.CalmThroughCrisis -> c.months.toString()
            else -> "6"
        })
    }
    var stressText by remember(initial) {
        mutableStateOf(((def?.condition as? AchievementCondition.CalmThroughCrisis)?.stressBelow ?: 30).toString())
    }

    val idError = when {
        id.isBlank()                                    -> "Required"
        !Regex("^[a-z0-9][a-z0-9._\\-]{2,63}$").matches(id) -> "lowercase, digits, . _ - (3–64)"
        else -> null
    }
    // Seeded definitions may rely on i18n keys — inline title only required for new/custom.
    val hasKeyTitle  = def?.titleKey?.isNotBlank() == true
    val titleError   = if (titleRu.isBlank() && !hasKeyTitle) "ru title required" else null
    val monthsError  = monthsText.toIntOrNull()?.takeIf { it <= 0 }?.let { "must be > 0" }
        ?: if (monthsText.toIntOrNull() == null) "number" else null
    val monthsNeeded = conditionType == ConditionType.EMERGENCY_FUND ||
        conditionType == ConditionType.CALM_THROUGH_CRISIS
    val flagsError   = if (conditionType == ConditionType.ANY_FLAG &&
        flagsText.split(',').none { it.trim().isNotEmpty() }) "at least one flag" else null

    val valid = idError == null && titleError == null && flagsError == null &&
        (!monthsNeeded || monthsError == null) && !busy

    fun buildCondition(): AchievementCondition = when (conditionType) {
        ConditionType.ANY_FLAG -> AchievementCondition.AnyFlag(
            flagsText.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        )
        ConditionType.FIRST_MONTHLY_REPORT -> AchievementCondition.FirstMonthlyReport
        ConditionType.EMERGENCY_FUND       -> AchievementCondition.EmergencyFund(monthsText.toIntOrNull() ?: 6)
        ConditionType.DEBT_FREE            -> AchievementCondition.DebtFree
        ConditionType.CALM_THROUGH_CRISIS  -> AchievementCondition.CalmThroughCrisis(
            months      = monthsText.toIntOrNull() ?: 12,
            stressBelow = stressText.toIntOrNull() ?: 30
        )
        ConditionType.STORY_COMPLETED      -> AchievementCondition.StoryCompleted
    }

    fun localized(ru: String, en: String = "", kk: String = ""): LocalizedText? =
        if (ru.isBlank() && en.isBlank() && kk.isBlank()) null else LocalizedText(ru, en, kk)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "New achievement" else "Edit ${def?.id}") },
        text = {
            Column(
                modifier            = Modifier.verticalScroll(rememberScrollState()).width(480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FormField(id, { id = it.trim() }, "ID (immutable)", enabled = isNew,
                    errorText = if (isNew) idError else null)
                FormField(emoji, { emoji = it }, "Emoji")

                Text("Kind", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AchievementKind.entries.forEach { k ->
                        FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.name) })
                    }
                }

                Text("Rarity", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AchievementRarity.entries.forEach { r ->
                        FilterChip(selected = rarity == r, onClick = { rarity = r }, label = { Text(r.name) })
                    }
                }

                FormField(sortOrder, { sortOrder = it }, "Sort order (list position)")

                // ── Unlock condition ─────────────────────────────────────────
                Text("Unlock condition", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ConditionType.entries.forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = conditionType == t, onClick = { conditionType = t })
                            Text(t.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                when (conditionType) {
                    ConditionType.ANY_FLAG ->
                        FormField(flagsText, { flagsText = it },
                            "Flags (comma-separated, e.g. learned.scam.pyramid)", errorText = flagsError)
                    ConditionType.EMERGENCY_FUND ->
                        FormField(monthsText, { monthsText = it }, "Months of expenses", errorText = monthsError)
                    ConditionType.CALM_THROUGH_CRISIS -> {
                        FormField(monthsText, { monthsText = it }, "Months after crisis", errorText = monthsError)
                        FormField(stressText, { stressText = it }, "Stress stays below")
                    }
                    else -> {}
                }

                // ── Texts ────────────────────────────────────────────────────
                if (hasKeyTitle) {
                    Text(
                        "Seeded texts come from code i18n (key: ${def?.titleKey}). " +
                            "Inline texts below override them when filled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("Title", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FormField(titleRu, { titleRu = it }, "Title (ru — source of truth)", errorText = titleError)
                FormField(titleEn, { titleEn = it }, "Title (en, optional)")
                FormField(titleKk, { titleKk = it }, "Title (kk, optional)")

                Text("Description", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FormField(descRu, { descRu = it }, "Description (ru)", singleLine = false)
                FormField(descEn, { descEn = it }, "Description (en, optional)", singleLine = false)
                FormField(descKk, { descKk = it }, "Description (kk, optional)", singleLine = false)

                FormField(hintRu, { hintRu = it }, "Hint (ru, GAME only, optional)", singleLine = false)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Active", style = MaterialTheme.typography.bodyMedium)
                }

                if (def?.dossier != null) {
                    Text(
                        "This SCAM achievement has a code-defined dossier — it is preserved on save.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        UpsertAchievementRequest(
                            definition = AchievementDefinition(
                                id        = id.trim(),
                                kind      = kind,
                                emoji     = emoji.trim(),
                                rarity    = rarity,
                                titleKey  = def?.titleKey ?: "",
                                descKey   = def?.descKey,
                                hintKey   = def?.hintKey,
                                dossier   = def?.dossier,   // preserved; dossier authoring stays in code
                                condition = buildCondition(),
                                titleText = localized(titleRu, titleEn, titleKk),
                                descText  = localized(descRu, descEn, descKk),
                                hintText  = localized(hintRu)
                            ),
                            isActive  = isActive,
                            sortOrder = sortOrder.toIntOrNull() ?: 0
                        )
                    )
                }
            ) { Text(if (busy) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun displayTitle(def: AchievementDefinition): String =
    def.titleText?.ru?.takeIf { it.isNotBlank() }
        ?: def.titleKey.ifBlank { def.id }

private fun conditionSummary(c: AchievementCondition): String = when (c) {
    is AchievementCondition.AnyFlag            -> "any flag: ${c.flags.joinToString()}"
    is AchievementCondition.FirstMonthlyReport -> "first monthly report"
    is AchievementCondition.EmergencyFund      -> "emergency fund ≥ ${c.months} mo"
    is AchievementCondition.DebtFree           -> "debt-free after debt"
    is AchievementCondition.CalmThroughCrisis  -> "calm ${c.months} mo, stress < ${c.stressBelow}"
    is AchievementCondition.StoryCompleted     -> "story completed"
}

@Composable
private fun rarityColor(r: AchievementRarity) = when (r) {
    AchievementRarity.COMMON    -> MaterialTheme.colorScheme.surfaceVariant
    AchievementRarity.RARE      -> MaterialTheme.colorScheme.primaryContainer
    AchievementRarity.LEGENDARY -> MaterialTheme.colorScheme.tertiaryContainer
}
