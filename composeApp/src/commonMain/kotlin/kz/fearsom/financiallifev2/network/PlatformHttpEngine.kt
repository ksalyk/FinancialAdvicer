package kz.fearsom.financiallifev2.network

import io.ktor.client.engine.*

/**
 * Returns a platform-specific [HttpClientEngine].
 *
 * Android  → OkHttp engine with certificate pinning (see androidMain actual).
 * iOS      → Darwin engine (NSURLSession) — see iosMain actual.
 *
 * TLS pinning pins the **public key** of the server certificate, not the
 * certificate itself, so it survives cert renewal as long as the key pair
 * doesn't change. Always include a backup pin for your CA's intermediate.
 */
expect fun createPlatformEngine(): HttpClientEngine
