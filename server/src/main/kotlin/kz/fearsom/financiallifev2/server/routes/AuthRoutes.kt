package kz.fearsom.financiallifev2.server.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kz.fearsom.financiallifev2.server.auth.JwtConfig
import kz.fearsom.financiallifev2.server.models.AuthResponse
import kz.fearsom.financiallifev2.server.models.LoginRequest
import kz.fearsom.financiallifev2.server.models.RefreshRequest
import kz.fearsom.financiallifev2.server.models.RegisterRequest
import kz.fearsom.financiallifev2.server.plugins.authRateLimiter
import kz.fearsom.financiallifev2.server.plugins.checkRateLimit
import kz.fearsom.financiallifev2.server.plugins.refreshRateLimiter
import kz.fearsom.financiallifev2.server.repository.UserRepository
import org.slf4j.LoggerFactory
import java.util.*

private val log = LoggerFactory.getLogger("AuthRoutes")

fun Route.authRoutes(userRepository: UserRepository) {
    route("/auth") {

        // ── POST /auth/register ───────────────────────────────────────────────
        post("/register") {
            if (!call.checkRateLimit(authRateLimiter)) return@post
            val req = call.receive<RegisterRequest>()

            val username = req.username.trim()
            val password = req.password

            if (username.length < 3) {
                call.respond(HttpStatusCode.BadRequest,
                    AuthResponse(success = false, message = "Логин минимум 3 символа"))
                return@post
            }
            if (password.length < 6) {
                call.respond(HttpStatusCode.BadRequest,
                    AuthResponse(success = false, message = "Пароль минимум 6 символов"))
                return@post
            }
            if (userRepository.existsByUsername(username)) {
                call.respond(HttpStatusCode.Conflict,
                    AuthResponse(success = false, message = "Пользователь уже существует"))
                return@post
            }

            val user         = userRepository.create(username, password)
            val accessToken  = generateAccessJwt(user.id, user.username)
            val refreshToken = userRepository.createRefreshToken(user.id)

            log.info("Registered user={} id={}", user.username, user.id)

            call.respond(HttpStatusCode.Created,
                AuthResponse(
                    success      = true,
                    accessToken  = accessToken,
                    refreshToken = refreshToken,
                    userId       = user.id,
                    username     = user.username
                )
            )
        }

        // ── POST /auth/login ──────────────────────────────────────────────────
        post("/login") {
            if (!call.checkRateLimit(authRateLimiter)) return@post
            val req = call.receive<LoginRequest>()

            if (req.username.isBlank() || req.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest,
                    AuthResponse(success = false, message = "Заполните все поля"))
                return@post
            }

            val user = userRepository.findByUsername(req.username)
            if (user == null) {
                log.warn("Login failed: user not found username={}", req.username)
                call.respond(HttpStatusCode.Unauthorized,
                    AuthResponse(success = false, message = "Пользователь не найден"))
                return@post
            }

            if (!userRepository.verifyPassword(req.password, user.passwordHash)) {
                log.warn("Login failed: wrong password userId={}", user.id)
                call.respond(HttpStatusCode.Unauthorized,
                    AuthResponse(success = false, message = "Неверный пароль"))
                return@post
            }

            val accessToken  = generateAccessJwt(user.id, user.username)
            val refreshToken = userRepository.createRefreshToken(user.id)

            log.info("Login success userId={}", user.id)

            call.respond(
                AuthResponse(
                    success      = true,
                    accessToken  = accessToken,
                    refreshToken = refreshToken,
                    userId       = user.id,
                    username     = user.username
                )
            )
        }

        // ── POST /auth/refresh ────────────────────────────────────────────────
        // Accepts the long-lived refreshToken, rotates it, and issues a new pair.
        post("/refresh") {
            if (!call.checkRateLimit(refreshRateLimiter)) return@post
            val req = call.receive<RefreshRequest>()

            if (req.refreshToken.isBlank()) {
                call.respond(HttpStatusCode.BadRequest,
                    AuthResponse(success = false, message = "Missing refresh token"))
                return@post
            }

            // consumeRefreshToken deletes the old token and validates expiry.
            val user = userRepository.consumeRefreshToken(req.refreshToken)
            if (user == null) {
                log.warn("Token refresh failed: invalid or expired token")
                call.respond(HttpStatusCode.Unauthorized,
                    AuthResponse(success = false, message = "Invalid or expired refresh token"))
                return@post
            }

            val newAccessToken  = generateAccessJwt(user.id, user.username)
            val newRefreshToken = userRepository.createRefreshToken(user.id)

            log.info("Token rotated userId={}", user.id)

            call.respond(
                AuthResponse(
                    success      = true,
                    accessToken  = newAccessToken,
                    refreshToken = newRefreshToken,
                    userId       = user.id,
                    username     = user.username
                )
            )
        }

        // ── GET /auth/me ──────────────────────────────────────────────────────
        // Protected via Ktor JWT plugin — reads principal instead of manual verify.
        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId    = principal.payload.getClaim("userId").asString()

                val user = userRepository.findById(userId)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized,
                        AuthResponse(false, message = "User not found"))

                call.respond(AuthResponse(
                    success  = true,
                    userId   = user.id,
                    username = user.username
                ))
            }
        }

        // ── POST /auth/logout ─────────────────────────────────────────────────
        // Revokes all refresh tokens for this user (all devices).
        authenticate("auth-jwt") {
            post("/logout") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId    = principal.payload.getClaim("userId").asString()

                userRepository.revokeAllTokens(userId)
                log.info("Logout: all tokens revoked userId={}", userId)

                call.respond(mapOf("success" to true))
            }
        }
    }
}

// ── JWT helpers ───────────────────────────────────────────────────────────────

data class JwtPrincipal(val userId: String, val username: String)

fun generateAccessJwt(userId: String, username: String): String =
    JWT.create()
        .withIssuer(JwtConfig.ISSUER)
        .withAudience(JwtConfig.AUDIENCE)
        .withClaim("userId", userId)
        .withClaim("username", username)
        .withExpiresAt(Date(System.currentTimeMillis() + JwtConfig.ACCESS_TTL_MS))
        .sign(Algorithm.HMAC256(JwtConfig.secret))

// Kept for backward compatibility with any call sites that import this.
@Deprecated("Use generateAccessJwt", ReplaceWith("generateAccessJwt(userId, username)"))
fun generateJwt(userId: String, username: String): String = generateAccessJwt(userId, username)

fun verifyJwt(token: String): JwtPrincipal? = runCatching {
    val verifier = JWT.require(Algorithm.HMAC256(JwtConfig.secret))
        .withIssuer(JwtConfig.ISSUER)
        .withAudience(JwtConfig.AUDIENCE)
        .build()
    val decoded = verifier.verify(token)
    JwtPrincipal(
        userId   = decoded.getClaim("userId").asString(),
        username = decoded.getClaim("username").asString()
    )
}.getOrNull()
