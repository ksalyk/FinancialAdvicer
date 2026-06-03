package kz.fearsom.financiallifev2.adminui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kz.fearsom.financiallifev2.admin.CharacterRow
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient

@Composable
fun CharactersScreen(api: AdminApiClient) {
    var characters by remember { mutableStateOf<List<CharacterRow>>(emptyList()) }
    var loading    by remember { mutableStateOf(true) }
    var error      by remember { mutableStateOf<String?>(null) }
    val scope      = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true; error = null
            try { characters = api.listCharacters() }
            catch (e: Exception) { error = e.message }
            finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Characters", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(characters) { char ->
                    CharacterCard(
                        character  = char,
                        onToggle   = {
                            scope.launch {
                                try {
                                    if (char.isActive) api.deactivateCharacter(char.id)
                                    else               api.activateCharacter(char.id)
                                    reload()
                                } catch (e: Exception) { error = e.message }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterCard(character: CharacterRow, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier            = Modifier.padding(12.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(character.emoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(character.name, style = MaterialTheme.typography.bodyLarge)
                    if (!character.isActive) {
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
                    text  = "eras: ${character.eraIds.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked         = character.isActive,
                onCheckedChange = { onToggle() }
            )
        }
    }
}
