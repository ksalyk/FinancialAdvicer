package kz.fearsom.financiallifev2.network

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*

/**
 * iOS HTTP engine: Darwin (NSURLSession-backed).
 *
 * Ktor's Darwin engine doesn't expose NSURLSession's pinning API directly.
 * For certificate pinning on iOS, the recommended options are:
 *
 *  Option A — TrustKit (recommended for production):
 *    https://github.com/datatheorem/TrustKit
 *    Configure in AppDelegate, works transparently across all NSURLSession traffic.
 *
 *  Option B — App Transport Security (ATS) in Info.plist:
 *    Enforces HTTPS-only but does NOT do public-key pinning.
 *    Sufficient for most apps if TLS is properly set up on the server.
 *
 *  Option C — Custom NSURLSessionDelegate via Darwin engine challenge handler:
 *    Ktor Darwin engine accepts `handleChallenge` block for custom cert validation,
 *    but requires writing a Kotlin/Native bridge to NSURLAuthenticationChallenge.
 *
 * The Darwin engine already enforces HTTPS for non-localhost URLs in line with
 * ATS defaults. No additional config needed for dev (localhost is ATS-exempt).
 */
actual fun createPlatformEngine(): HttpClientEngine = Darwin.create {
    configureRequest {
        // NSURLRequest configuration if needed (timeouts, cache policy, etc.)
        setTimeoutInterval(30.0)
    }
}
