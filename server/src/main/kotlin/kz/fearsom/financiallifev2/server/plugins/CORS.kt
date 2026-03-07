package kz.fearsom.financiallifev2.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("CORS")

/**
 * CORS configuration.
 *
 * In production, set the ALLOWED_ORIGINS environment variable to a
 * comma-separated list of trusted origins:
 *
 *   ALLOWED_ORIGINS=https://app.yourapp.com,https://www.yourapp.com
 *
 * If ALLOWED_ORIGINS is not set, the server falls back to allowing any host
 * so that local dev (Android emulator, iOS simulator) works without extra config.
 *
 * WARNING: leaving anyHost() in production lets any website make credentialed
 * cross-origin requests to your API on behalf of logged-in users.
 */
fun Application.configureCORS() {
    val allowedOrigins = System.getenv("ALLOWED_ORIGINS")
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    if (allowedOrigins.isEmpty()) {
        log.warn("ALLOWED_ORIGINS not set — CORS is open to any host. Set this env var in production!")
    } else {
        log.info("CORS restricted to: {}", allowedOrigins)
    }

    install(CORS) {
        if (allowedOrigins.isEmpty()) {
            anyHost()
        } else {
            allowedOrigins.forEach { origin ->
                val url = Url(origin)
                allowHost(url.host, schemes = listOf(url.protocol.name))
            }
        }

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
    }
}
