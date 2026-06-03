package kz.fearsom.financiallifev2.adminui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient
import kz.fearsom.financiallifev2.adminui.screens.CharactersScreen
import kz.fearsom.financiallifev2.adminui.screens.ErasScreen
import kz.fearsom.financiallifev2.adminui.screens.LoginScreen
import kz.fearsom.financiallifev2.adminui.screens.ScenariosScreen
import kz.fearsom.financiallifev2.adminui.screens.UsersScreen

private enum class AdminTab(val label: String) {
    USERS("Users"),
    CHARACTERS("Characters"),
    ERAS("Eras"),
    SCENARIOS("Scenarios")
}

@Composable
fun AdminApp(api: AdminApiClient) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {

            // Auth state: null = loading, "" = not logged in, username = logged in
            var loggedInUser by remember { mutableStateOf<String?>(null) }
            var authChecked by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                loggedInUser = api.getMe()
                authChecked = true
            }

            when {
                !authChecked -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                loggedInUser == null -> LoginScreen(api) { username ->
                    loggedInUser = username
                }

                else -> MainScaffold(
                    api           = api,
                    loggedInUser  = loggedInUser!!,
                    onLogout      = { loggedInUser = null }
                )
            }
        }
    }
}

// ── Main scaffold with tab navigation ────────────────────────────────────────

@Composable
private fun MainScaffold(
    api:          AdminApiClient,
    loggedInUser: String,
    onLogout:     () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AdminTab.USERS) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AdminTopBar(
                loggedInUser = loggedInUser,
                onLogout     = {
                    scope.launch { runCatching { api.logout() } }
                    onLogout()
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AdminTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick  = { selectedTab = tab },
                        label    = { Text(tab.label) },
                        icon     = {}
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                AdminTab.USERS      -> UsersScreen(api)
                AdminTab.CHARACTERS -> CharactersScreen(api)
                AdminTab.ERAS       -> ErasScreen(api)
                AdminTab.SCENARIOS  -> ScenariosScreen(api)
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTopBar(loggedInUser: String, onLogout: () -> Unit) {
    TopAppBar(
        title = { Text("Finance LifeLine — Admin") },
        actions = {
            Text(
                text     = loggedInUser,
                modifier = Modifier.padding(end = 8.dp),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onLogout) { Text("Logout") }
        }
    )
}
