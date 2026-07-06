package kz.fearsom.financiallifev2.server.repository

import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.achievements.AchievementDefinition
import kz.fearsom.financiallifev2.admin.AchievementAdminRow
import kz.fearsom.financiallifev2.admin.UpsertAchievementRequest
import kz.fearsom.financiallifev2.server.database.tables.AchievementFeedbackTable
import kz.fearsom.financiallifev2.server.database.tables.AchievementsCatalogTable
import kz.fearsom.financiallifev2.server.database.tables.UserAchievementsTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

/** Source markers for catalog rows. */
object AchievementSource {
    const val SEEDED = "SEEDED"
    const val CUSTOM = "CUSTOM"
}

/**
 * DB-backed achievement definition catalog.
 *
 * Seeded from the :shared code catalog on startup (insert-only-when-missing so
 * admin edits survive restarts). The whole [AchievementDefinition] round-trips
 * through [AchievementsCatalogTable.definitionJson]; kind/isActive/sortOrder are
 * denormalized for filtering.
 */
class AchievementCatalogRepository(private val db: Database) {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** All rows with aggregate unlock + feedback stats, ordered by sortOrder. */
    suspend fun listAll(activeOnly: Boolean = false): List<AchievementAdminRow> =
        newSuspendedTransaction(db = db) {
            // Aggregates: achievementId → unlock count
            val unlockCountExpr = UserAchievementsTable.achievementId.count()
            val unlockCounts: Map<String, Long> = UserAchievementsTable
                .select(UserAchievementsTable.achievementId, unlockCountExpr)
                .groupBy(UserAchievementsTable.achievementId)
                .associate { it[UserAchievementsTable.achievementId] to it[unlockCountExpr] }

            // Aggregates: (achievementId, vote) → count
            val voteCountExpr = AchievementFeedbackTable.achievementId.count()
            val voteCounts: Map<Pair<String, String>, Long> = AchievementFeedbackTable
                .select(AchievementFeedbackTable.achievementId, AchievementFeedbackTable.vote, voteCountExpr)
                .groupBy(AchievementFeedbackTable.achievementId, AchievementFeedbackTable.vote)
                .associate {
                    (it[AchievementFeedbackTable.achievementId] to it[AchievementFeedbackTable.vote]) to it[voteCountExpr]
                }

            val query = AchievementsCatalogTable.selectAll()
            if (activeOnly) query.where { AchievementsCatalogTable.isActive eq true }

            query.map { row ->
                val id = row[AchievementsCatalogTable.id]
                row.toAdminRow(
                    unlockCount = unlockCounts[id] ?: 0L,
                    upVotes     = voteCounts[id to "up"] ?: 0L,
                    downVotes   = voteCounts[id to "down"] ?: 0L
                )
            }.sortedWith(compareBy({ it.sortOrder }, { it.definition.id }))
        }

    /** Active definitions only — the shape game clients consume. */
    suspend fun listActiveDefinitions(): List<AchievementDefinition> =
        newSuspendedTransaction(db = db) {
            AchievementsCatalogTable.selectAll()
                .where { AchievementsCatalogTable.isActive eq true }
                .map { it.decodeDefinition() to it[AchievementsCatalogTable.sortOrder] }
                .sortedWith(compareBy({ it.second }, { it.first.id }))
                .map { it.first }
        }

    suspend fun findById(id: String): AchievementAdminRow? =
        newSuspendedTransaction(db = db) {
            AchievementsCatalogTable.selectAll()
                .where { AchievementsCatalogTable.id eq id }
                .singleOrNull()
                ?.toAdminRow(unlockCount = 0, upVotes = 0, downVotes = 0)
        }

    /** Set of ids accepted for unlock sync (active AND inactive — an unlock earned
     *  while an achievement was active must not be rejected after deactivation). */
    suspend fun allIds(): Set<String> =
        newSuspendedTransaction(db = db) {
            AchievementsCatalogTable.selectAll()
                .map { it[AchievementsCatalogTable.id] }
                .toSet()
        }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Insert or update by definition id. New rows get source=CUSTOM; existing
     * rows keep their original source (SEEDED stays SEEDED after edits).
     */
    suspend fun upsert(req: UpsertAchievementRequest): AchievementAdminRow {
        val now = System.currentTimeMillis()
        val defJson = json.encodeToString(req.definition)
        val defId = req.definition.id

        newSuspendedTransaction(db = db) {
            val exists = AchievementsCatalogTable.selectAll()
                .where { AchievementsCatalogTable.id eq defId }
                .count() > 0

            if (exists) {
                AchievementsCatalogTable.update({ AchievementsCatalogTable.id eq defId }) {
                    it[kind]           = req.definition.kind.name
                    it[isActive]       = req.isActive
                    it[sortOrder]      = req.sortOrder
                    it[definitionJson] = defJson
                    it[updatedAt]      = now
                }
            } else {
                AchievementsCatalogTable.insert {
                    it[id]             = defId
                    it[kind]           = req.definition.kind.name
                    it[isActive]       = req.isActive
                    it[sortOrder]      = req.sortOrder
                    it[source]         = AchievementSource.CUSTOM
                    it[definitionJson] = defJson
                    it[createdAt]      = now
                    it[updatedAt]      = now
                }
            }
        }
        return findById(defId)!!
    }

    /** Atomic active toggle. Returns false when the id does not exist. */
    suspend fun setActive(id: String, active: Boolean): Boolean =
        newSuspendedTransaction(db = db) {
            AchievementsCatalogTable.update({ AchievementsCatalogTable.id eq id }) {
                it[isActive]  = active
                it[updatedAt] = System.currentTimeMillis()
            } > 0
        }

    sealed class DeleteOutcome {
        data object Deleted : DeleteOutcome()
        data object NotFound : DeleteOutcome()
        /** SEEDED rows would be re-created by startup seeding — deactivate instead. */
        data object SeededProtected : DeleteOutcome()
    }

    /** Hard-delete a CUSTOM definition. SEEDED rows are protected (would resurrect on restart). */
    suspend fun delete(id: String): DeleteOutcome =
        newSuspendedTransaction(db = db) {
            val row = AchievementsCatalogTable.selectAll()
                .where { AchievementsCatalogTable.id eq id }
                .singleOrNull()
                ?: return@newSuspendedTransaction DeleteOutcome.NotFound

            if (row[AchievementsCatalogTable.source] == AchievementSource.SEEDED) {
                return@newSuspendedTransaction DeleteOutcome.SeededProtected
            }

            // Clean up unlock + feedback rows so users don't keep ghosts of a removed custom achievement.
            UserAchievementsTable.deleteWhere { UserAchievementsTable.achievementId eq id }
            AchievementFeedbackTable.deleteWhere { AchievementFeedbackTable.achievementId eq id }
            AchievementsCatalogTable.deleteWhere { AchievementsCatalogTable.id eq id }
            DeleteOutcome.Deleted
        }

    /**
     * Startup seed from the code catalog: inserts only missing ids, preserving
     * every admin edit. sortOrder follows catalog declaration order.
     */
    suspend fun seedMissing(definitions: List<AchievementDefinition>) {
        val now = System.currentTimeMillis()
        newSuspendedTransaction(db = db) {
            val existing = AchievementsCatalogTable.selectAll()
                .map { it[AchievementsCatalogTable.id] }
                .toSet()

            definitions.forEachIndexed { index, def ->
                if (def.id in existing) return@forEachIndexed
                AchievementsCatalogTable.insert {
                    it[id]             = def.id
                    it[kind]           = def.kind.name
                    it[isActive]       = true
                    it[sortOrder]      = index
                    it[source]         = AchievementSource.SEEDED
                    it[definitionJson] = json.encodeToString(def)
                    it[createdAt]      = now
                    it[updatedAt]      = now
                }
            }
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun ResultRow.decodeDefinition(): AchievementDefinition =
        json.decodeFromString(this[AchievementsCatalogTable.definitionJson])

    private fun ResultRow.toAdminRow(unlockCount: Long, upVotes: Long, downVotes: Long) =
        AchievementAdminRow(
            definition  = decodeDefinition(),
            isActive    = this[AchievementsCatalogTable.isActive],
            sortOrder   = this[AchievementsCatalogTable.sortOrder],
            source      = this[AchievementsCatalogTable.source],
            unlockCount = unlockCount,
            upVotes     = upVotes,
            downVotes   = downVotes,
            createdAt   = this[AchievementsCatalogTable.createdAt],
            updatedAt   = this[AchievementsCatalogTable.updatedAt]
        )
}
