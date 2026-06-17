package kz.fearsom.financiallifev2.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

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
    private val windowMs: Long   = 60_000L,    // 1 minute
    private val pruneIntervalMs: Long = 5L * 60 * 1000   // sweep stale IP buckets at most this often
) {
    // IP → list of request timestamps within the current window.
    private val store = ConcurrentHashMap<String, ArrayDeque<Long>>()

    // Timestamp of the last opportunistic sweep (see [isAllowed]).
    private val lastPruneAt = AtomicLong(System.currentTimeMillis())

    fun isAllowed(ip: String): Boolean {
        val now  = System.currentTimeMillis()
        val floor = now - windowMs

        val timestamps = store.getOrPut(ip) { ArrayDeque() }

        val allowed = synchronized(timestamps) {
            // Evict timestamps outside the window.
            while (timestamps.isNotEmpty() && timestamps.first() < floor) {
                timestamps.removeFirst()
            }

            if (timestamps.size >= maxRequests) {
                false
            } else {
                timestamps.addLast(now)
                true
            }
        }

        // Opportunistic prune of empty/stale buckets — bounds memory at O(active IPs)
        // without a dedicated GC thread (mirrors ChoiceLockManager). Runs at most once
        // per [pruneIntervalMs] regardless of request volume.
        if (now - lastPruneAt.get() >= pruneIntervalMs) {
            lastPruneAt.set(now)
            purgeExpired()
        }

        return allowed
    }

    /**
     * Removes IP buckets whose timestamps have all aged out of the window.
     * Invoked automatically (and throttled) from [isAllowed]; also safe to call manually.
     */
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
 * When true, X-Forwarded-For is trusted. Set this ONLY when a reverse proxy you control
 * overwrites the header. Defaults to false so a directly-exposed server cannot have its
 * rate limiting bypassed by a spoofed header.
 */
private val TRUST_PROXY: Boolean = System.getenv("TRUST_PROXY")?.lowercase() == "true"

/**
 * Extracts the client IP used as the rate-limit key.
 *
 * Behind a trusted proxy ([TRUST_PROXY] = true) the left-most X-Forwarded-For entry is the
 * originating client. Otherwise the header is attacker-controlled and is ignored in favour
 * of the actual socket peer, so an attacker cannot dodge limits by forging the header.
 */
fun ApplicationCall.clientIp(): String =
    if (TRUST_PROXY) {
        request.headers["X-Forwarded-For"]
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: request.local.remoteHost
    } else {
        request.local.remoteHost
    }

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
