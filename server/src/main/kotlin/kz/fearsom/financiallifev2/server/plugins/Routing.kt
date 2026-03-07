package kz.fearsom.financiallifev2.server.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kz.fearsom.financiallifev2.server.repository.GameRepository
import kz.fearsom.financiallifev2.server.repository.UserRepository
import kz.fearsom.financiallifev2.server.routes.authRoutes
import kz.fearsom.financiallifev2.server.routes.gameRoutes

fun Application.configureRouting(
    userRepository: UserRepository,
    gameRepository: GameRepository
) {
    routing {
        get("/") {
            call.respond(mapOf(
                "name"    to "Finance LifeLine API",
                "version" to "1.0.0",
                "status"  to "running"
            ))
        }

        route("/api/v1") {
            // Public: login, register, refresh, me
            authRoutes(userRepository)

            // Protected: all game endpoints require a valid access token.
            authenticate("auth-jwt") {
                gameRoutes(gameRepository)
            }
        }
    }
}
