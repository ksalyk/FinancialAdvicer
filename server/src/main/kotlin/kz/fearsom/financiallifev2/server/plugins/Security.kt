package kz.fearsom.financiallifev2.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kz.fearsom.financiallifev2.server.auth.JwtConfig

fun Application.configureSecurity() {
    install(Authentication) {
        // ── Game API: JWT bearer tokens ──────────────────────────────────────
        jwt("auth-jwt") {
            realm = "Finance LifeLine"
            verifier(
                JWT.require(Algorithm.HMAC256(JwtConfig.secret))
                    .withIssuer(JwtConfig.ISSUER)
                    .withAudience(JwtConfig.AUDIENCE)
                    .build()
            )
            validate { credential ->
                val userId   = credential.payload.getClaim("userId").asString()
                val username = credential.payload.getClaim("username").asString()
                if (!userId.isNullOrBlank() && !username.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }

        // ── Admin SPA: cookie session ────────────────────────────────────────
        // Used by the browser admin panel. API clients continue to use ADMIN_KEY Bearer.
        // Note: AdminRoutes.isAdminAuthorized() checks both mechanisms manually, so
        // this provider is available for future authenticate("admin-auth") blocks.
        session<AdminSession>("admin-auth") {
            validate { session -> session }
            challenge {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Admin session required"))
            }
        }
    }
}
