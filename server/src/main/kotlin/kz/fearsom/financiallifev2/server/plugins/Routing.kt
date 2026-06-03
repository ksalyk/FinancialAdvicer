package kz.fearsom.financiallifev2.server.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kz.fearsom.financiallifev2.server.repository.CharactersRepository
import kz.fearsom.financiallifev2.server.repository.ErasRepository
import kz.fearsom.financiallifev2.server.repository.GameRepository
import kz.fearsom.financiallifev2.server.repository.StatisticsRepository
import kz.fearsom.financiallifev2.server.repository.UserRepository
import kz.fearsom.financiallifev2.server.routes.adminAuthRoutes
import kz.fearsom.financiallifev2.server.routes.adminRoutes
import kz.fearsom.financiallifev2.server.routes.adminScenarioRoutes
import kz.fearsom.financiallifev2.server.routes.adminUserRoutes
import kz.fearsom.financiallifev2.server.routes.authRoutes
import kz.fearsom.financiallifev2.server.routes.gameRoutes

fun Application.configureRouting(
    userRepository: UserRepository,
    gameRepository: GameRepository,
    statisticsRepository: StatisticsRepository,
    charactersRepository: CharactersRepository,
    erasRepository: ErasRepository
) {
    routing {
        // Serve the :admin Compose/wasmJs SPA.
        // Bundle is copied to server/src/main/resources/admin-ui/ at build time.
        staticResources("/admin", "admin-ui") {
            default("index.html")
        }

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
                gameRoutes(gameRepository, statisticsRepository)
            }

            // Admin session auth (login/logout/me) — must be before the guarded admin routes.
            adminAuthRoutes()

            // Admin: character + era management (ADMIN_KEY Bearer or session cookie)
            adminRoutes(charactersRepository, erasRepository, statisticsRepository)

            // Admin: user management (paginated list, detail, reset-password, delete)
            adminUserRoutes(userRepository, statisticsRepository)

            // Admin: scenario graph viewer (list combos + full graph DTO)
            adminScenarioRoutes(charactersRepository, erasRepository)
        }
    }
}
