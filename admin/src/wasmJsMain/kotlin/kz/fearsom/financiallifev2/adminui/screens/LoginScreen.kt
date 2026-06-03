package kz.fearsom.financiallifev2.adminui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kz.fearsom.financiallifev2.adminui.net.AdminApiClient

@Composable
fun LoginScreen(api: AdminApiClient, onLoginSuccess: (username: String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading  by remember { mutableStateOf(false) }
    var error    by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.width(360.dp)) {
            Column(
                modifier            = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Admin Login", style = MaterialTheme.typography.headlineSmall)

                OutlinedTextField(
                    value         = username,
                    onValueChange = { username = it; error = null },
                    label         = { Text("Username") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value               = password,
                    onValueChange       = { password = it; error = null },
                    label               = { Text("Password") },
                    singleLine          = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier            = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(
                        text  = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick  = {
                        scope.launch {
                            loading = true
                            error   = null
                            try {
                                val ok = api.login(username.trim(), password)
                                if (ok) {
                                    val me = api.getMe()
                                    if (me != null) onLoginSuccess(me)
                                    else error = "Login succeeded but session check failed"
                                } else {
                                    error = "Invalid credentials"
                                }
                            } catch (e: Exception) {
                                error = "Network error: ${e.message}"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    enabled  = !loading && username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loading) CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    else Text("Login")
                }
            }
        }
    }
}
