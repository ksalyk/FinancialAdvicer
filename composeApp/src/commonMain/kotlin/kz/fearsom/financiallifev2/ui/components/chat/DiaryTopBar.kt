package kz.fearsom.financiallifev2.ui.components.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.RedDanger

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
fun DiaryTopBar(
    playerState: PlayerState?,
    characterName: String,
    characterEmoji: String,
    characterTitle: String,
    onStatsClick: () -> Unit,
    onRestartClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val colors = LocalAppColors.current
    var showMenu by remember { mutableStateOf(false) }
    var showConfirmRestart by remember { mutableStateOf(false) }

    // Confirmation dialog for restart (destructive action)
    if (showConfirmRestart) {
        AlertDialog(
            onDismissRequest = { showConfirmRestart = false },
            title = { Text(Strings.uiChatResetTitle) },
            text = { Text(Strings.uiChatResetMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmRestart = false
                        showMenu = false
                        onRestartClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedDanger
                    )
                ) {
                    Text(Strings.uiChatResetConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRestart = false }) {
                    Text(Strings.uiChatCancel)
                }
            }
        )
    }

    Surface(color = colors.backgroundDeep, shadowElevation = 6.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = Strings.uiChatCdHome,
                    tint = colors.textPrimary
                )
            }

            Text("📓", fontSize = 26.sp, modifier = Modifier.padding(end = 8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${Strings.uiChatDiary} · $characterName",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                val subtitle = if (playerState != null) {
                    "$characterTitle · ${monthName(playerState.month)} ${playerState.year}"
                } else {
                    characterTitle
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            IconButton(
                onClick = onStatsClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = Strings.uiChatCdStats,
                    tint = colors.textPrimary
                )
            }

            // Overflow menu (three-dot menu)
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = Strings.uiChatCdOptions,
                        tint = colors.textPrimary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(Strings.uiChatRestartGame) },
                        onClick = {
                            showMenu = false
                            showConfirmRestart = true
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

private fun monthName(month: Int): String =
    Strings.uiChatShortMonths.getOrNull(month) ?: "?"
