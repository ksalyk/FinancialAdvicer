/**
 * File location: src/androidMain/kotlin/com/example/MainActivity.kt
 * This is an example Compose Activity showing how to use the NetworkClient and Inspektor.
 */

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.network.ApiClient
import com.example.network.User
import com.example.network.ui.DebugPanel
import com.example.network.ui.InspektorDebugButton

/**
 * Main Activity demonstrating NetworkClient + Inspektor integration.
 *
 * Features shown:
 * - Loading users via NetworkClient (which auto-logs with Inspektor)
 * - Error handling
 * - Debug button to open Inspektor UI
 * - Pull-to-refresh functionality
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiClient = remember { ApiClient() }

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDebugPanel by remember { mutableStateOf(false) }

    // Load users on initial composition
    LaunchedEffect(Unit) {
        loadUsers(apiClient, onSuccess = { users = it }, onError = { error = it })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KMP + Inspektor Demo") },
                backgroundColor = Color(0xFF1976D2),
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Debug button at top
            InspektorDebugButton(context = context)

            Divider()

            // Toggle debug panel
            Button(
                onClick = { showDebugPanel = !showDebugPanel },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp)
            ) {
                Text(if (showDebugPanel) "Hide Debug Panel" else "Show Debug Panel")
            }

            // Debug panel
            if (showDebugPanel) {
                DebugPanel(
                    context = context,
                    onClearLogs = { /* Implement log clearing */ },
                    onExportLogs = { /* Implement log export */ }
                )
                Divider()
            }

            // Content area
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    ErrorState(
                        error = error!!,
                        onRetry = {
                            error = null
                            loadUsers(apiClient, onSuccess = { users = it }, onError = { error = it })
                        }
                    )
                }

                users.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                    ) {
                        Text("No users found")
                    }
                }

                else -> {
                    UsersListContent(users) {
                        // Refresh button action
                        scope.launch {
                            loadUsers(apiClient, onSuccess = { users = it }, onError = { error = it })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsersListContent(
    users: List<User>,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Refresh button
        Button(
            onClick = onRefresh,
            modifier = Modifier
                .align(Alignment.End)
                .padding(8.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            Spacer(modifier = Modifier.width(4.dp))
            Text("Refresh")
        }

        // Users list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users) { user ->
                UserCard(user)
            }
        }
    }
}

@Composable
private fun UserCard(user: User) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.h6,
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ID: ${user.id}",
                style = MaterialTheme.typography.body2,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.email,
                style = MaterialTheme.typography.body2,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "❌ Error Loading Users",
            style = MaterialTheme.typography.h6,
            color = Color.Red
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.body2,
            color = Color.Gray,
            modifier = Modifier.padding(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

// Helper function to load users
private suspend fun loadUsers(
    apiClient: ApiClient,
    onSuccess: (List<User>) -> Unit,
    onError: (String) -> Unit
) {
    apiClient.getUsers()
        .onSuccess { onSuccess(it) }
        .onFailure { onError(it.message ?: "Unknown error") }
}
