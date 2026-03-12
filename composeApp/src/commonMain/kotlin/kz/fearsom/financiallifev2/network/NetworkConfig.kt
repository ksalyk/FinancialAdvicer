package kz.fearsom.financiallifev2.network

/**
 * Central config for server base URL.
 *
 * Override [baseUrl] from your platform DI or build config:
 *   - Local dev Android emulator: "http://10.0.2.2:8081/api/v1"
 *   - Local dev iOS simulator:    "http://localhost:8081/api/v1"
 *   - Production:                 "https://api.yourapp.com/api/v1"
 *
 * Port 8081 is used to avoid conflict with the landing webpack dev server (port 8080).
 */
object NetworkConfig {
    // Defaults to Android emulator loopback — override via Koin parameters in androidMain.
    var baseUrl: String = "http://10.0.2.2:8081/api/v1"
}
