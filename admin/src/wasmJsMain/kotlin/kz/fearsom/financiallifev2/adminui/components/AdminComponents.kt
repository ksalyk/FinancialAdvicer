package kz.fearsom.financiallifev2.adminui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Reusable building blocks shared by the Characters and Eras admin screens.
 *
 * These deliberately use only `remember`/stateless composables — the admin SPA
 * keeps per-screen state in the screen composable (mirroring the existing
 * UsersScreen pattern) rather than introducing Koin/presenters for four screens.
 */

/** A single selectable option for [MultiSelectChips]: stable [id] + human [label]. */
data class ChipOption(val id: String, val label: String)

/**
 * A labeled, wrapping set of filter chips for multi-selecting ids (e.g. eraIds,
 * availableCharacterIds). Selection is owned by the caller; this is stateless.
 *
 * When [options] is empty an explanatory line is shown instead of an empty row,
 * so the admin understands *why* there is nothing to pick (e.g. no eras exist yet).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiSelectChips(
    label: String,
    options: List<ChipOption>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    emptyHint: String = "Nothing to select yet."
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (options.isEmpty()) {
            Text(
                text = emptyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { opt ->
                    val isSelected = opt.id in selected
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggle(opt.id) },
                        label = { Text(opt.label) },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }
        }
    }
}

/**
 * Outlined text field that also surfaces a validation error inline.
 * [errorText] non-null => the field renders in the error state with the message.
 */
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    singleLine: Boolean = true,
    numeric: Boolean = false,
    errorText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = singleLine,
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it) } },
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        modifier = modifier
    )
}

/**
 * Destructive confirmation dialog. The confirm button uses the error color so a
 * hard-delete (which cascades stats across ALL users) is never a one-tap accident.
 */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    body: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
