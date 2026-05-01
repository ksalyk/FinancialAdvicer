package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kz.fearsom.financiallifev2.data.TypingPace
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.presentation.SettingsUiState
import kz.fearsom.financiallifev2.ui.components.AppScreen
import kz.fearsom.financiallifev2.ui.theme.GoldPrimary
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.Spacing

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onLocaleSelected: (String) -> Unit,
    onTypingAnimToggle: (Boolean) -> Unit,
    onTypingPaceChange: (TypingPace) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current

    AppScreen(
        title    = Strings.uiSettingsTitle,
        subtitle = Strings.uiSettingsSubtitle,
        onBack   = onBack,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg)
        ) {
            // ── Language ─────────────────────────────────────────────────────
            SettingsSectionHeader(Strings.uiSettingsLanguage)
            Spacer(Modifier.height(4.dp))
            Text(
                text  = Strings.uiSettingsLanguageSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(12.dp))

            LanguageRow(
                label    = Strings.uiSettingsLanguageRussian,
                selected = uiState.currentLocale == "ru",
                onClick  = { onLocaleSelected("ru") }
            )
            LanguageRow(
                label    = Strings.uiSettingsLanguageKazakh,
                selected = uiState.currentLocale == "kk",
                onClick  = { onLocaleSelected("kk") }
            )
            LanguageRow(
                label    = Strings.uiSettingsLanguageEnglish,
                selected = uiState.currentLocale == "en",
                onClick  = { onLocaleSelected("en") }
            )

            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.15f))
            Spacer(Modifier.height(24.dp))

            // ── Gameplay ─────────────────────────────────────────────────────
            SettingsSectionHeader(Strings.uiSettingsGameplay)
            Spacer(Modifier.height(16.dp))

            // Typing animation toggle
            SettingsToggleRow(
                title    = Strings.uiSettingsTypingAnim,
                subtitle = Strings.uiSettingsTypingAnimSub,
                checked  = uiState.typingAnimationEnabled,
                onCheckedChange = onTypingAnimToggle
            )

            // Pace selector — only shown when animation is enabled
            if (uiState.typingAnimationEnabled) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text       = Strings.uiSettingsTypingPace,
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = colors.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                TypingPaceSelector(
                    selected = uiState.typingAnimationPace,
                    onChange = onTypingPaceChange
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Section header ───────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    val colors = LocalAppColors.current
    Text(
        text       = title,
        style      = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color      = colors.textPrimary
    )
}

// ─── Language row ─────────────────────────────────────────────────────────────

@Composable
private fun LanguageRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) GoldPrimary.copy(alpha = 0.12f) else colors.backgroundCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(12.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary
        )
    }
    Spacer(Modifier.height(8.dp))
}

// ─── Toggle row ───────────────────────────────────────────────────────────────

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.backgroundCard)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color      = colors.textPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor  = GoldPrimary,
                checkedTrackColor  = GoldPrimary.copy(alpha = 0.35f)
            )
        )
    }
}

// ─── Typing pace selector ─────────────────────────────────────────────────────

@Composable
private fun TypingPaceSelector(
    selected: TypingPace,
    onChange: (TypingPace) -> Unit
) {
    val colors = LocalAppColors.current
    val paces = listOf(
        TypingPace.SLOW   to Strings.uiSettingsPaceSlow,
        TypingPace.NORMAL to Strings.uiSettingsPaceNormal,
        TypingPace.FAST   to Strings.uiSettingsPaceFast,
    )

    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        paces.forEach { (pace, label) ->
            val isSelected = pace == selected
            Surface(
                onClick      = { onChange(pace) },
                modifier     = Modifier.weight(1f),
                shape        = RoundedCornerShape(10.dp),
                color        = if (isSelected) GoldPrimary.copy(alpha = 0.18f) else colors.backgroundCard,
                border       = if (isSelected)
                    androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.6f))
                else
                    androidx.compose.foundation.BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier              = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally
                ) {
                    Text(
                        text       = label,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color      = if (isSelected) GoldPrimary else colors.textSecondary
                    )
                }
            }
        }
    }
}
