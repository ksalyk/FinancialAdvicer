package kz.fearsom.financiallifev2.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

private val log = LoggerFactory.getLogger("RateLimiter")

/**
 * Sliding-window in-memory rate limiter.
 *
 * Suitable for single-instance deployments. For horizontally-scaled setups,
 * replace the in-memory store with Redis (e.g. Lettuce or Redisson).
 *
 * @param maxRequests Maximum number of requests allowed in [windowMs].
 * @param windowMs    Length of the sliding window in milliseconds.
 */
class RateLimiter(
    private val maxRequests: Int = 10,
    private val windowMs: Long   = 60_000L    // 1 minute
) {
    // IP → list of request timestamps within the current window.
    private val store = ConcurrentHashMap<String, ArrayDeque<Long>>()

    fun isAllowed(ip: String): Boolean {
        val now  = System.currentTimeMillis()
        val floor = now - windowMs

        val timestamps = store.getOrPut(ip) { ArrayDeque() }

        synchronized(timestamps) {
            // Evict timestamps outside the window.
            while (timestamps.isNotEmpty() && timestamps.first() < floor) {
                timestamps.removeFirst()
            }

            if (timestamps.size >= maxRequests) return false

            timestamps.addLast(now)
            return true
        }
    }

    /** Periodically call this to avoid unbounded memory growth on large deployments. */
    fun purgeExpired() {
        val floor = System.currentTimeMillis() - windowMs
        store.entries.removeIf { (_, timestamps) ->
            synchronized(timestamps) {
                timestamps.removeIf { it < floor }
                timestamps.isEmpty()
            }
        }
    }
}

// Shared instances — one per sensitivity level.
val authRateLimiter   = RateLimiter(maxRequests = 10, windowMs = 60_000L)   // 10 req/min
val refreshRateLimiter = RateLimiter(maxRequests = 30, windowMs = 60_000L)  // 30 req/min

/**
 * Extracts the real client IP, respecting X-Forwarded-For when behind a proxy.
 * Only trust X-Forwarded-For if you control the proxy that sets it.
 */
fun ApplicationCall.clientIp(): String =
    request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
        ?: request.local.remoteHost

/**
 * Applies [limiter] to the current call. Returns true if the request is allowed,
 * false if it was rejected (and automatically responds with 429).
 */
suspend fun ApplicationCall.checkRateLimit(limiter: RateLimiter): Boolean {
    val ip = clientIp()
    if (!limiter.isAllowed(ip)) {
        log.warn("Rate limit exceeded ip={} path={}", ip, request.path())
        response.header("Retry-After", "60")
        respond(
            HttpStatusCode.TooManyRequests,
            mapOf("error" to "Too many requests. Try again in 60 seconds.")
        )
        return false
    }
    return true
}
