/**
 * File location: src/iosMain/kotlin/com/example/network/IosHttpClientEngine.kt
 */

package com.example.network

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.darwin.DarwinEngineConfig

/**
 * iOS-specific implementation of HTTP client engine provider.
 * Uses Ktor's DarwinEngineConfig which uses native URLSession API.
 *
 * This function is called from the shared NetworkClient to create
 * the platform-specific engine for HTTP requests on iOS.
 *
 * Note: On iOS, Inspektor logs are output to the system console (Xcode debugger)
 * and can also be persisted locally for later access.
 */
actual fun provideHttpClientEngine(): HttpClientEngineConfig {
    return DarwinEngineConfig().apply {
        // Configure iOS-specific HTTP engine settings

        // URLSession configuration can be customized here if needed
        // The Darwin engine automatically uses the native URLSession API

        // Connect timeout
        // Note: Darwin doesn't have explicit connect timeout like Android,
        // but you can configure it through the URLSessionConfiguration if needed
    }
}

/**
 * Note on Inspektor on iOS:
 *
 * Unlike Android, iOS doesn't have an in-app UI popup for Inspektor.
 * Instead, Inspektor logs to:
 *
 * 1. Xcode Console (when running in debugger)
 *    - View in Xcode: Debug > Debug Navigator > Console (⌘⇧Y)
 *
 * 2. Local file system (can be accessed via device file browser)
 *    - Located in app's Documents directory
 *    - Can be exported via Files app or Document picker
 *
 * 3. System Logs
 *    - Check system logs via Console.app on macOS
 *    - Or use log stream command in Terminal
 *
 * To view detailed network logs on iOS:
 * 1. Connect device to Mac via Xcode
 * 2. Open Xcode > Window > Devices and Simulators
 * 3. Select your device/simulator
 * 4. Tap "View Device Logs"
 *
 * Alternative: Use Xcode's Network inspector
 * 1. Run your app in Xcode debugger
 * 2. Go to Xcode > Debug Navigator > Network (⌘6)
 * 3. See all network requests in real-time
 */
