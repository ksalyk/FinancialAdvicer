package kz.fearsom.financiallifev2.server.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kz.fearsom.financiallifev2.server.repository.AchievementCatalogRepository
import kz.fearsom.financiallifev2.server.repository.AchievementsRepository
import kz.fearsom.financiallifev2.server.repository.CharactersRepository
import kz.fearsom.financiallifev2.server.repository.ErasRepository
import kz.fearsom.financiallifev2.server.repository.GameRepository
import kz.fearsom.financiallifev2.server.repository.StatisticsRepository
import kz.fearsom.financiallifev2.server.repository.StoriesRepository
import kz.fearsom.financiallifev2.server.repository.UserRepository
import kz.fearsom.financiallifev2.server.routes.achievementRoutes
import kz.fearsom.financiallifev2.server.routes.adminAchievementRoutes
import kz.fearsom.financiallifev2.server.routes.adminAuthRoutes
import kz.fearsom.financiallifev2.server.routes.adminRoutes
import kz.fearsom.financiallifev2.server.routes.adminScenarioRoutes
import kz.fearsom.financiallifev2.server.routes.adminStoryRoutes
import kz.fearsom.financiallifev2.server.routes.adminUserRoutes
import kz.fearsom.financiallifev2.server.routes.authRoutes
import kz.fearsom.financiallifev2.server.routes.catalogRoutes
import kz.fearsom.financiallifev2.server.routes.gameRoutes

fun Application.configureRouting(
    userRepository: UserRepository,
    gameRepository: GameRepository,
    statisticsRepository: StatisticsRepository,
    charactersRepository: CharactersRepository,
    erasRepository: ErasRepository,
    achievementsRepository: AchievementsRepository,
    achievementCatalogRepository: AchievementCatalogRepository,
    storiesRepository: StoriesRepository
) {
    routing {
        // Serve the :admin Compose/wasmJs SPA.
        // Bundle is copied to server/src/main/resources/admin-ui/ at build time.
        //
        // The exact-path redirect is required: at "/admin" (no trailing slash) the
        // browser resolves the relative <script src="admin.js"> against "/" and
        // requests "/admin.js" → 404 → blank dark page. "/admin/" resolves it
        // correctly to "/admin/admin.js".
        get("/admin") {
            call.respondRedirect("/admin/", permanent = true)
        }
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

            // Public: active character/era catalog + published stories +
            // active achievement definitions (clients overlay onto SeedData).
            catalogRoutes(charactersRepository, erasRepository, achievementCatalogRepository, storiesRepository)

            // Protected: all game endpoints require a valid access token.
            authenticate("auth-jwt") {
                gameRoutes(gameRepository, statisticsRepository)
                achievementRoutes(achievementsRepository, achievementCatalogRepository)
            }

            // Admin session auth (login/logout/me) — must stay OUTSIDE the guard.
            adminAuthRoutes()

            // Admin API — one guard for everything: session cookie (SPA) OR
            // ADMIN_KEY Bearer (programmatic). Adding a new admin route inside
            // this block is automatically protected.
            authenticate(ADMIN_COMBINED_AUTH) {
                adminRoutes(charactersRepository, erasRepository)
                adminUserRoutes(userRepository, statisticsRepository)
                adminScenarioRoutes(charactersRepository, erasRepository)
                adminStoryRoutes(storiesRepository)
                adminAchievementRoutes(achievementCatalogRepository, achievementsRepository, userRepository)
            }
        }
    }
}
