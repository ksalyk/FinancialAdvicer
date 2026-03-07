package kz.fearsom.financiallifev2.network

/**
 * Central config for server base URL.
 *
 * Override [baseUrl] from your platform DI or build config:
 *   - Local dev Android emulator: "http://10.0.2.2:8080/api/v1"
 *   - Local dev iOS simulator:    "http://localhost:8080/api/v1"
 *   - Production:                 "https://api.yourapp.com/api/v1"
 */
object NetworkConfig {
    // Defaults to Android emulator loopback — override via Koin parameters in androidMain.
    var baseUrl: String = "http://10.0.2.2:8080/api/v1"
}
