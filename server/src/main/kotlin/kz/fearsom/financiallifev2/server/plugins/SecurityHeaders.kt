package kz.fearsom.financiallifev2.server.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * Adds security response headers to every outgoing response.
 *
 * When deployed behind a reverse proxy that terminates TLS (Caddy / Nginx),
 * the proxy should also set these — but having them in the app is defence-in-depth.
 *
 * Headers applied:
 *  HSTS                — enforce HTTPS for 1 year
 *  X-Frame-Options     — block clickjacking
 *  X-Content-Type-Options — disable MIME sniffing
 *  Referrer-Policy     — prevent URL leakage
 *  Content-Security-Policy — restrictive API policy
 *  Permissions-Policy  — opt-out of browser APIs
 */
fun Application.configureSecurityHeaders() {
    // TODO: Ktor 3.x API — implement via custom plugin or middleware
    // For now, security headers can be added at reverse proxy level (Caddy/Nginx)
}
