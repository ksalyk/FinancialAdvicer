package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.ui.components.AppScreen
import kz.fearsom.financiallifev2.ui.theme.GoldPrimary
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors
import kz.fearsom.financiallifev2.ui.theme.Spacing

@Composable
fun SettingsScreen(
    currentLocale: String,
    onLocaleSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalAppColors.current

    AppScreen(
        title = Strings.uiSettingsTitle,
        subtitle = Strings.uiSettingsSubtitle,
        onBack = onBack,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg)
        ) {
            Text(
                text = Strings.uiSettingsLanguage,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = Strings.uiSettingsLanguageSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(16.dp))

            LanguageRow(
                label = Strings.uiSettingsLanguageRussian,
                selected = currentLocale == "ru",
                onClick = { onLocaleSelected("ru") }
            )
            LanguageRow(
                label = Strings.uiSettingsLanguageKazakh,
                selected = currentLocale == "kk",
                onClick = { onLocaleSelected("kk") }
            )
            LanguageRow(
                label = Strings.uiSettingsLanguageEnglish,
                selected = currentLocale == "en",
                onClick = { onLocaleSelected("en") }
            )
        }
    }
}

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
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary
        )
    }
    Spacer(Modifier.height(8.dp))
}
