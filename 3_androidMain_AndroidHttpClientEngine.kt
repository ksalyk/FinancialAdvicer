/**
 * File location: src/androidMain/kotlin/com/example/network/AndroidHttpClientEngine.kt
 */

package com.example.network

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.android.AndroidEngineConfig

/**
 * Android-specific implementation of HTTP client engine provider.
 * Uses Ktor's AndroidEngineConfig which internally uses OkHttp.
 *
 * This function is called from the shared NetworkClient to create
 * the platform-specific engine for HTTP requests on Android.
 */
actual fun provideHttpClientEngine(): HttpClientEngineConfig {
    return AndroidEngineConfig().apply {
        // Configure Android-specific HTTP engine settings
        connectTimeout = 30000  // 30 seconds
        socketTimeout = 30000   // 30 seconds
        requestTimeout = 30000  // 30 seconds

        // Enable HTTP/2 (usually enabled by default on modern Android)
        // no explicit config needed for this

        // You can add custom OkHttp interceptors here if needed:
        // interceptors.add(customInterceptor)
        // networkInterceptors.add(customNetworkInterceptor)
    }
}
