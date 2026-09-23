package kz.fearsom.financiallifev2.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kz.fearsom.financiallifev2.achievements.AchievementFeedbackRequest
import kz.fearsom.financiallifev2.achievements.AchievementVote
import kz.fearsom.financiallifev2.achievements.UnlockAchievementsRequest
import kz.fearsom.financiallifev2.achievements.UnlockAchievementsResponse
import kz.fearsom.financiallifev2.achievements.UserAchievementsResponse
import kz.fearsom.financiallifev2.server.repository.AchievementCatalogRepository
import kz.fearsom.financiallifev2.server.repository.AchievementsRepository
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AchievementRoutes")

/** Hard cap on a single unlock batch — the whole catalog fits many times over. */
private const val MAX_UNLOCK_BATCH = 100

@Serializable
data class AchievementFeedbackResponse(val feedback: Map<String, String>)

// ── Routes ────────────────────────────────────────────────────────────────────
// Mounted inside authenticate("auth-jwt") {} in Routing.kt.

fun Route.achievementRoutes(
    achievementsRepository: AchievementsRepository,
    catalogRepository: AchievementCatalogRepository
) {

    route("/achievements") {

        // ── GET /achievements/feedback ────────────────────────────────────────
        // Use an explicit sub-route so Ktor 3.x creates a dedicated path node
        // in the routing tree. A bare get("/feedback") can still lose to get { }
        // in Ktor 3.x because method selectors can match before path selectors
        // consume remaining segments. An explicit route("/feedback") guarantees
        // the path node is evaluated first by the quality-based routing engine.
        route("/feedback") {
            get {
                val userId = call.jwtUserId()
                call.respond(AchievementFeedbackResponse(achievementsRepository.listFeedback(userId)))
            }
        }

        // ── GET /achievements ─────────────────────────────────────────────────
        get {
            val userId = call.jwtUserId()
            call.respond(UserAchievementsResponse(achievementsRepository.listUnlocks(userId)))
        }

        // ── POST /achievements/unlock ─────────────────────────────────────────
        post("/unlock") {
            val userId = call.jwtUserId()
            val req    = call.receive<UnlockAchievementsRequest>()

            if (req.unlocks.size > MAX_UNLOCK_BATCH) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Batch exceeds $MAX_UNLOCK_BATCH unlocks")
                )
            }

            // Validate against the DB catalog (seeded from code + admin-created ids).
            val knownIds = catalogRepository.allIds()
            val unknown = req.unlocks.map { it.achievementId }.filterNot { it in knownIds }
            if (unknown.isNotEmpty()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Unknown achievement ids: ${unknown.distinct().joinToString()}")
                )
            }

            val inserted = achievementsRepository.unlock(userId, req.unlocks)
            if (inserted > 0) {
                log.info("Achievements unlocked userId={} inserted={}", userId, inserted)
            }

            call.respond(UnlockAchievementsResponse(
                inserted = inserted,
                unlocks  = achievementsRepository.listUnlocks(userId)
            ))
        }

        // ── POST /achievements/{id}/feedback ──────────────────────────────────
        // Body: {"vote": "up" | "down" | null} — null clears the vote.
        post("/{id}/feedback") {
            val userId        = call.jwtUserId()
            val achievementId = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing achievement id"))

            if (achievementId !in catalogRepository.allIds()) {
                return@post call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Unknown achievement id '$achievementId'")
                )
            }

            val req = call.receive<AchievementFeedbackRequest>()
            if (req.vote != null && req.vote !in AchievementVote.VALID) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "vote must be one of ${AchievementVote.VALID} or null")
                )
            }

            achievementsRepository.setFeedback(userId, achievementId, req.vote)
            call.respond(mapOf("ok" to true))
        }
    }
}

/**
 * Same JWT extraction as GameRoutes — valid only inside authenticate("auth-jwt").
 * Returns null if the principal is missing (misconfigured route or malformed token).
 */
private fun ApplicationCall.jwtUserIdOrNull(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()?.takeIf { it.isNotBlank() }

private fun ApplicationCall.jwtUserId(): String =
    jwtUserIdOrNull() ?: error("JWT principal missing — route must be inside authenticate(\"auth-jwt\")")
