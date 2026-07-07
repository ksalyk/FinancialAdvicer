package kz.fearsom.financiallifev2.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kz.fearsom.financiallifev2.admin.StoryDetail
import kz.fearsom.financiallifev2.admin.StoryReviewRequest
import kz.fearsom.financiallifev2.admin.StorySource
import kz.fearsom.financiallifev2.admin.StoryStatus
import kz.fearsom.financiallifev2.admin.UpsertStoryRequest
import kz.fearsom.financiallifev2.admin.toDto
import kz.fearsom.financiallifev2.scenarios.ScenarioGraphFactory
import kz.fearsom.financiallifev2.scenarios.ScenarioNotFoundException
import kz.fearsom.financiallifev2.scenarios.analysis.analyzeScenario
import kz.fearsom.financiallifev2.scenarios.analysis.toValidationReport
import kz.fearsom.financiallifev2.server.repository.StoriesRepository
import kz.fearsom.financiallifev2.server.repository.StoriesRepository.TransitionResult
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AdminStoryRoutes")

private val STORY_ID_REGEX = Regex("^[a-z0-9][a-z0-9_\\-]{2,63}$")

// Typed responses — kotlinx cannot serialize Map<String, Any>.
@kotlinx.serialization.Serializable
private data class StoryDeleteResponse(val deleted: Boolean, val storyId: String)

@kotlinx.serialization.Serializable
private data class StoryValidationErrorResponse(
    val error: String,
    val report: kz.fearsom.financiallifev2.admin.StoryValidationReport
)

/**
 * Admin CRUD + moderation for DB-backed stories.
 * Guarded at mount point by `authenticate(ADMIN_SESSION_AUTH, ADMIN_KEY_AUTH)`.
 *
 *   GET    /admin/stories?status=&characterId=&eraId=   — list (metadata only)
 *   GET    /admin/stories/{id}                          — metadata + full graph
 *   POST   /admin/stories                               — create DRAFT
 *   PUT    /admin/stories/{id}                          — update metadata + graph
 *   DELETE /admin/stories/{id}                          — hard delete
 *   POST   /admin/stories/{id}/validate                 — dry-run validation report
 *   POST   /admin/stories/{id}/submit                   — DRAFT/REJECTED → PENDING_REVIEW
 *   POST   /admin/stories/{id}/approve                  — PENDING_REVIEW → PUBLISHED (validation-gated)
 *   POST   /admin/stories/{id}/reject   {note}          — PENDING_REVIEW → REJECTED
 *   POST   /admin/stories/{id}/publish                  — DRAFT/ARCHIVED → PUBLISHED (validation-gated)
 *   POST   /admin/stories/{id}/unpublish                — PUBLISHED → ARCHIVED
 *   POST   /admin/stories/clone/{characterId}/{eraId}   — snapshot a built-in code graph as a DRAFT
 *
 * Publishing (approve/publish) is gated on the shared graph validator: a story
 * with ERROR-severity findings can never go live. The same validator runs live
 * in the admin editor, so admins see problems before the server rejects them.
 */
fun Route.adminStoryRoutes(storiesRepository: StoriesRepository) {
    route("/admin/stories") {

        // ── GET /admin/stories ────────────────────────────────────────────────
        get {
            val status = call.request.queryParameters["status"]?.let { raw ->
                runCatching { StoryStatus.valueOf(raw.uppercase()) }.getOrNull()
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Unknown status '$raw' — expected one of ${StoryStatus.entries}")
                    )
            }
            val characterId = call.request.queryParameters["characterId"]
            val eraId       = call.request.queryParameters["eraId"]
            call.respond(storiesRepository.list(status, characterId, eraId))
        }

        // ── GET /admin/stories/{id} ───────────────────────────────────────────
        get("/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            val detail = storiesRepository.findDetail(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Story '$id' not found"))
            call.respond(detail)
        }

        // ── POST /admin/stories ───────────────────────────────────────────────
        post {
            val req = call.receive<UpsertStoryRequest>()
            validateUpsert(req)?.let {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to it))
            }

            val created = storiesRepository.create(req, source = StorySource.ADMIN)
                ?: return@post call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "Story '${req.id}' already exists")
                )

            log.info("Story created id={} title={}", created.row.id, created.row.title)
            call.respond(HttpStatusCode.Created, created)
        }

        // ── PUT /admin/stories/{id} ───────────────────────────────────────────
        put("/{id}") {
            val id = call.parameters["id"]
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            val req = call.receive<UpsertStoryRequest>()
            if (req.id != id) {
                return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Body id '${req.id}' does not match path id '$id' — story ids are immutable")
                )
            }
            validateUpsert(req)?.let {
                return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to it))
            }

            val existing = storiesRepository.findRow(id)
                ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Story '$id' not found"))

            // Editing a live story must never break it: gate on validation.
            if (existing.status == StoryStatus.PUBLISHED) {
                val report = analyzeScenario(req.graph, selfContained = true).toValidationReport()
                if (!report.publishable) {
                    return@put call.respond(
                        HttpStatusCode.UnprocessableEntity,
                        StoryValidationErrorResponse(
                            error  = "Story is PUBLISHED and the new graph has ${report.errors} error(s). " +
                                "Fix them or unpublish first.",
                            report = report
                        )
                    )
                }
            }

            val updated: StoryDetail = storiesRepository.update(req)!!
            log.info("Story updated id={} status={}", id, updated.row.status)
            call.respond(updated)
        }

        // ── DELETE /admin/stories/{id} ────────────────────────────────────────
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            if (!storiesRepository.delete(id)) {
                return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Story '$id' not found"))
            }
            log.info("Story deleted id={}", id)
            call.respond(StoryDeleteResponse(deleted = true, storyId = id))
        }

        // ── POST /admin/stories/{id}/validate ─────────────────────────────────
        post("/{id}/validate") {
            val id = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            val detail = storiesRepository.findDetail(id)
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Story '$id' not found"))
            call.respond(analyzeScenario(detail.graph, selfContained = true).toValidationReport())
        }

        // ── Status transitions ────────────────────────────────────────────────

        post("/{id}/submit")    { call.transitionStory(storiesRepository, StoryStatus.PENDING_REVIEW) }
        post("/{id}/reject")    {
            val note = runCatching { call.receive<StoryReviewRequest>().note }.getOrNull()
            call.transitionStory(storiesRepository, StoryStatus.REJECTED, note)
        }
        post("/{id}/unpublish") { call.transitionStory(storiesRepository, StoryStatus.ARCHIVED) }

        // approve + publish share the validation gate.
        post("/{id}/approve")   { call.publishStory(storiesRepository) }
        post("/{id}/publish")   { call.publishStory(storiesRepository) }

        // ── POST /admin/stories/clone/{characterId}/{eraId} ───────────────────
        // Snapshots a built-in code graph into an editable DRAFT row. The snapshot
        // captures the server's current locale rendering of all texts.
        post("/clone/{characterId}/{eraId}") {
            val characterId = call.parameters["characterId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing characterId"))
            val eraId = call.parameters["eraId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing eraId"))

            val graph = try {
                ScenarioGraphFactory.forCharacter(characterId, eraId).toDto()
            } catch (e: ScenarioNotFoundException) {
                return@post call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "No built-in graph for characterId=$characterId eraId=$eraId")
                )
            }

            // Find a free id: <char>_<era>_copy, then _copy2, _copy3, …
            val base = "${characterId}_${eraId}_copy".take(58)
            var candidate = base
            var n = 1
            while (storiesRepository.findRow(candidate) != null) {
                n += 1
                candidate = "$base$n"
            }

            val created = storiesRepository.create(
                UpsertStoryRequest(
                    id          = candidate,
                    title       = "Clone: $characterId × $eraId",
                    description = "Snapshot of the built-in graph (server locale at clone time).",
                    characterId = characterId,
                    eraId       = eraId,
                    graph       = graph
                ),
                source = StorySource.BUILT_IN
            )!!

            log.info("Built-in graph cloned to story id={}", created.row.id)
            call.respond(HttpStatusCode.Created, created)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Shared metadata validation for create/update. Returns an error message or null. */
private fun validateUpsert(req: UpsertStoryRequest): String? = when {
    !STORY_ID_REGEX.matches(req.id) ->
        "id must match ${STORY_ID_REGEX.pattern} (lowercase latin, digits, '_' or '-', 3–64 chars)"
    req.title.isBlank()          -> "title is required"
    req.title.length > 200       -> "title is longer than 200 chars"
    req.characterId.isBlank()    -> "characterId is required"
    req.eraId.isBlank()          -> "eraId is required"
    req.graph.events.isEmpty()   -> "graph must contain at least one event"
    else -> null
}

private suspend fun ApplicationCall.transitionStory(
    repo: StoriesRepository,
    to: StoryStatus,
    reviewNote: String? = null
) {
    val id = parameters["id"]
        ?: return respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

    when (val result = repo.transition(id, to, reviewNote)) {
        is TransitionResult.NotFound ->
            respond(HttpStatusCode.NotFound, mapOf("error" to "Story '$id' not found"))
        is TransitionResult.Illegal ->
            respond(
                HttpStatusCode.Conflict,
                mapOf("error" to "Illegal transition ${result.from} → ${result.to}")
            )
        is TransitionResult.Success -> {
            log.info("Story {} → {} id={}", result.row.status, to, id)
            respond(result.row)
        }
    }
}

/** PUBLISH/APPROVE: validation-gated transition to PUBLISHED. */
private suspend fun ApplicationCall.publishStory(repo: StoriesRepository) {
    val id = parameters["id"]
        ?: return respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

    val detail = repo.findDetail(id)
        ?: return respond(HttpStatusCode.NotFound, mapOf("error" to "Story '$id' not found"))

    val report = analyzeScenario(detail.graph, selfContained = true).toValidationReport()
    if (!report.publishable) {
        return respond(
            HttpStatusCode.UnprocessableEntity,
            StoryValidationErrorResponse(
                error  = "Story has ${report.errors} validation error(s) — cannot publish",
                report = report
            )
        )
    }

    when (val result = repo.transition(id, StoryStatus.PUBLISHED)) {
        is TransitionResult.NotFound ->
            respond(HttpStatusCode.NotFound, mapOf("error" to "Story '$id' not found"))
        is TransitionResult.Illegal ->
            respond(
                HttpStatusCode.Conflict,
                mapOf("error" to "Illegal transition ${result.from} → ${result.to}")
            )
        is TransitionResult.Success -> {
            respond(result.row)
        }
    }
}
