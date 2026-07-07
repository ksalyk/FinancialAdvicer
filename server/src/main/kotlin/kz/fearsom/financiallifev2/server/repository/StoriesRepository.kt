package kz.fearsom.financiallifev2.server.repository

import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.admin.ScenarioGraphDto
import kz.fearsom.financiallifev2.admin.StoryDetail
import kz.fearsom.financiallifev2.admin.StoryRow
import kz.fearsom.financiallifev2.admin.StorySource
import kz.fearsom.financiallifev2.admin.StoryStatus
import kz.fearsom.financiallifev2.admin.UpsertStoryRequest
import kz.fearsom.financiallifev2.server.database.tables.StoriesTable
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Persistence for DB-backed stories (scenario graphs with a moderation lifecycle).
 *
 * Status transition rules are enforced here — routes decide *when* to attempt a
 * transition (and gate PUBLISH on graph validation), this class decides *whether*
 * the transition is legal at all.
 */
class StoriesRepository(private val db: Database) {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Transition table ──────────────────────────────────────────────────────

    private val allowedTransitions: Map<StoryStatus, Set<StoryStatus>> = mapOf(
        StoryStatus.DRAFT          to setOf(StoryStatus.PENDING_REVIEW, StoryStatus.PUBLISHED),
        StoryStatus.PENDING_REVIEW to setOf(StoryStatus.PUBLISHED, StoryStatus.REJECTED, StoryStatus.DRAFT),
        StoryStatus.REJECTED       to setOf(StoryStatus.DRAFT, StoryStatus.PENDING_REVIEW),
        StoryStatus.PUBLISHED      to setOf(StoryStatus.ARCHIVED),
        StoryStatus.ARCHIVED       to setOf(StoryStatus.DRAFT, StoryStatus.PUBLISHED)
    )

    fun isTransitionAllowed(from: StoryStatus, to: StoryStatus): Boolean =
        to in allowedTransitions[from].orEmpty()

    // ── Read ──────────────────────────────────────────────────────────────────

    suspend fun list(
        status: StoryStatus? = null,
        characterId: String? = null,
        eraId: String? = null
    ): List<StoryRow> =
        newSuspendedTransaction(db = db) {
            val conditions = buildList<Op<Boolean>> {
                status?.let      { add(StoriesTable.status eq it.name) }
                characterId?.let { add(StoriesTable.characterId eq it) }
                eraId?.let       { add(StoriesTable.eraId eq it) }
            }
            val query = StoriesTable.selectAll()
            if (conditions.isNotEmpty()) query.where { conditions.reduce { a, b -> a and b } }
            query.orderBy(StoriesTable.updatedAt, SortOrder.DESC).map { it.toRow() }
        }

    suspend fun findRow(id: String): StoryRow? =
        newSuspendedTransaction(db = db) {
            StoriesTable.selectAll().where { StoriesTable.id eq id }.singleOrNull()?.toRow()
        }

    suspend fun findDetail(id: String): StoryDetail? =
        newSuspendedTransaction(db = db) {
            val row = StoriesTable.selectAll().where { StoriesTable.id eq id }.singleOrNull()
                ?: return@newSuspendedTransaction null
            StoryDetail(
                row   = row.toRow(),
                graph = json.decodeFromString<ScenarioGraphDto>(row[StoriesTable.graphJson])
            )
        }

    // ── Write ─────────────────────────────────────────────────────────────────

    /** Creates a new DRAFT story. Returns null if [UpsertStoryRequest.id] already exists. */
    suspend fun create(
        req: UpsertStoryRequest,
        source: StorySource,
        authorId: String? = null
    ): StoryDetail? {
        val now = System.currentTimeMillis()
        val graphJson = json.encodeToString(req.graph)

        val created = newSuspendedTransaction(db = db) {
            val exists = StoriesTable.selectAll()
                .where { StoriesTable.id eq req.id }.count() > 0
            if (exists) return@newSuspendedTransaction false

            StoriesTable.insert {
                it[id]          = req.id
                it[title]       = req.title
                it[description] = req.description
                it[StoriesTable.characterId] = req.characterId
                it[StoriesTable.eraId]       = req.eraId
                it[status]      = StoryStatus.DRAFT.name
                it[StoriesTable.storySource] = source.name
                it[StoriesTable.authorId]    = authorId
                it[reviewNote]  = null
                it[StoriesTable.graphJson]   = graphJson
                it[eventCount]  = req.graph.events.size
                it[endingCount] = req.graph.events.count { e -> e.isEnding }
                it[createdAt]   = now
                it[updatedAt]   = now
                it[publishedAt] = null
            }
            true
        }
        return if (created) findDetail(req.id) else null
    }

    /**
     * Updates metadata + graph of an existing story. Status is NOT touched here —
     * use [transition]. Returns null when the story does not exist.
     */
    suspend fun update(req: UpsertStoryRequest): StoryDetail? {
        val now = System.currentTimeMillis()
        val graphJson = json.encodeToString(req.graph)

        val updated = newSuspendedTransaction(db = db) {
            StoriesTable.update({ StoriesTable.id eq req.id }) {
                it[title]       = req.title
                it[description] = req.description
                it[characterId] = req.characterId
                it[eraId]       = req.eraId
                it[StoriesTable.graphJson] = graphJson
                it[eventCount]  = req.graph.events.size
                it[endingCount] = req.graph.events.count { e -> e.isEnding }
                it[updatedAt]   = now
            } > 0
        }
        return if (updated) findDetail(req.id) else null
    }

    sealed class TransitionResult {
        data class Success(val row: StoryRow) : TransitionResult()
        data object NotFound : TransitionResult()
        data class Illegal(val from: StoryStatus, val to: StoryStatus) : TransitionResult()
    }

    /**
     * Atomically moves a story to [to] if the transition is legal.
     * [reviewNote] is stored on REJECTED and cleared when leaving REJECTED.
     */
    suspend fun transition(id: String, to: StoryStatus, reviewNote: String? = null): TransitionResult =
        newSuspendedTransaction(db = db) {
            val row = StoriesTable.selectAll().where { StoriesTable.id eq id }.singleOrNull()
                ?: return@newSuspendedTransaction TransitionResult.NotFound

            val from = StoryStatus.valueOf(row[StoriesTable.status])
            if (from == to) return@newSuspendedTransaction TransitionResult.Success(row.toRow())
            if (!isTransitionAllowed(from, to)) {
                return@newSuspendedTransaction TransitionResult.Illegal(from, to)
            }

            val now = System.currentTimeMillis()
            // Optimistic concurrency: only move the row while it is STILL in [from].
            // A concurrent transition that already changed the status matches 0 rows,
            // so two approve/reject calls racing on the same old state can't both win.
            val moved = StoriesTable.update({
                (StoriesTable.id eq id) and (StoriesTable.status eq from.name)
            }) {
                it[status]    = to.name
                it[updatedAt] = now
                when (to) {
                    StoryStatus.PUBLISHED -> it[publishedAt] = now
                    StoryStatus.REJECTED  -> it[StoriesTable.reviewNote] = reviewNote
                    else                  -> it[StoriesTable.reviewNote] = null
                }
            }

            if (moved == 0) {
                // Lost the race: someone transitioned it between our read and write.
                // Report the current status as a conflicting (illegal) transition.
                val current = StoriesTable.selectAll().where { StoriesTable.id eq id }.singleOrNull()
                    ?: return@newSuspendedTransaction TransitionResult.NotFound
                return@newSuspendedTransaction TransitionResult.Illegal(
                    StoryStatus.valueOf(current[StoriesTable.status]), to
                )
            }

            val fresh = StoriesTable.selectAll().where { StoriesTable.id eq id }.single().toRow()
            TransitionResult.Success(fresh)
        }

    suspend fun delete(id: String): Boolean =
        newSuspendedTransaction(db = db) {
            StoriesTable.deleteWhere { StoriesTable.id eq id } > 0
        }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun ResultRow.toRow() = StoryRow(
        id          = this[StoriesTable.id],
        title       = this[StoriesTable.title],
        description = this[StoriesTable.description],
        characterId = this[StoriesTable.characterId],
        eraId       = this[StoriesTable.eraId],
        status      = StoryStatus.valueOf(this[StoriesTable.status]),
        source      = StorySource.valueOf(this[StoriesTable.storySource]),
        authorId    = this[StoriesTable.authorId],
        reviewNote  = this[StoriesTable.reviewNote],
        eventCount  = this[StoriesTable.eventCount],
        endingCount = this[StoriesTable.endingCount],
        createdAt   = this[StoriesTable.createdAt],
        updatedAt   = this[StoriesTable.updatedAt],
        publishedAt = this[StoriesTable.publishedAt]
    )
}
