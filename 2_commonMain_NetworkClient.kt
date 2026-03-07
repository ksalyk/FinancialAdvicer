/**
 * File location: src/commonMain/kotlin/com/example/network/NetworkClient.kt
 */

package com.example.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.github.shreyashkore.inspektor.Inspektor
import io.github.shreyashkore.inspektor.InspektorConfig.LoggingLevel

/**
 * Creates and configures the shared Ktor HTTP client with Inspektor debugging.
 * Platform-specific engines are provided via expect/actual declarations.
 *
 * This is a singleton that should be used throughout the app for all HTTP requests.
 * The Inspektor plugin automatically intercepts and logs all network traffic.
 */
object NetworkClient {

    private var _httpClient: HttpClient? = null

    val httpClient: HttpClient
        get() {
            if (_httpClient == null) {
                _httpClient = createHttpClient()
            }
            return _httpClient!!
        }

    private fun createHttpClient(): HttpClient {
        return HttpClient(provideHttpClientEngine()) {
            // JSON serialization plugin
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }

            // Install Inspektor for network inspection (like Chucker)
            install(Inspektor) {
                // Logging level: NONE, INFO, HEADERS, or BODY
                loggingLevel = LoggingLevel.BODY

                // Maximum content length in bytes (250KB)
                maxContentLength = 250000L

                // Retention period in milliseconds (7 days)
                retentionPeriod = 7 * 24 * 60 * 60 * 1000L

                // Exclude sensitive endpoints from logging via regex patterns
                excludePatterns = listOf(
                    ".*oauth.*",
                    ".*password.*",
                    ".*token.*",
                    ".*secret.*"
                )

                // Enable mock responses for testing (recommended: only in debug builds)
                mockingEnabled = false

                // Enable HAR format export for Chrome DevTools
                enableHarExport = true
            }

            // Expect successful status codes (don't throw on 4xx, 5xx)
            expectSuccess = false

            // Additional timeout configuration
            engine {
                connectTimeout = 30000  // 30 seconds
                requestTimeout = 30000
                socketTimeout = 30000
            }
        }
    }

    /**
     * Close the HTTP client and release resources.
     * Call this when the app is shutting down or when you need to reset the client.
     */
    fun closeClient() {
        _httpClient?.close()
        _httpClient = null
    }

    /**
     * Reset the client (closes and recreates it).
     * Useful for testing or when you need to refresh configuration.
     */
    fun resetClient() {
        closeClient()
        // Next access to httpClient will recreate it
    }
}

/**
 * Platform-specific HTTP client engine provider.
 * Must be implemented in androidMain/IosMain with actual and expect keywords.
 *
 * - Android: Returns AndroidEngineConfig
 * - iOS: Returns DarwinEngineConfig
 */
expect fun provideHttpClientEngine(): HttpClientEngineConfig
