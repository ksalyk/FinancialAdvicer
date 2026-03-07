package kz.fearsom.financiallifev2.server.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.Level

/**
 * Configures Ktor's built-in CallLogging plugin.
 *
 * Every HTTP request/response is logged at INFO level with:
 *  - method + URI
 *  - response status code
 *  - request duration
 *
 * Health-check endpoints (/game/health, /) are excluded to reduce noise.
 */
fun Application.configureLogging() {
    install(CallLogging) {
        level = Level.INFO

        // Log: method, path, status, duration
        format { call ->
            val method   = call.request.httpMethod.value
            val path     = call.request.path()
            val status   = call.response.status()?.value ?: "???"
            val duration = call.processingTimeMillis()
            "$method $path → $status (${duration}ms)"
        }

        // Skip noisy health/root endpoints
        filter { call ->
            val path = call.request.path()
            path != "/" && !path.endsWith("/health")
        }
    }
}
