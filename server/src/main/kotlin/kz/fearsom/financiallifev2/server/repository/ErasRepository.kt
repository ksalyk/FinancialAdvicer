package kz.fearsom.financiallifev2.server.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.server.database.tables.CompletedSessionsTable
import kz.fearsom.financiallifev2.server.database.tables.ErasTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

// ── DTOs ─────────────────────────────────────────────────────────────────────

@Serializable
data class EraRow(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val startYear: Int,
    val endYear: Int,
    val availableCharacterIds: List<String>,
    val isActive: Boolean,
    val isLocked: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class UpsertEraRequest(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val startYear: Int,
    val endYear: Int,
    val availableCharacterIds: List<String>,
    val isActive: Boolean = true,
    val isLocked: Boolean = false
)

// ── Repository ────────────────────────────────────────────────────────────────

class ErasRepository {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Read ──────────────────────────────────────────────────────────────────

    suspend fun findById(id: String): EraRow? =
        newSuspendedTransaction {
            ErasTable
                .selectAll()
                .where { ErasTable.id eq id }
                .singleOrNull()
                ?.toRow()
        }

    /** Returns all eras; [activeOnly] = false returns soft-deleted ones too. */
    suspend fun listAll(activeOnly: Boolean = true): List<EraRow> =
        newSuspendedTransaction {
            val query = ErasTable.selectAll()
            if (activeOnly) query.where { ErasTable.isActive eq true }
            query.map { it.toRow() }
        }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Insert or update an era by [UpsertEraRequest.id].
     * Safe to call multiple times — idempotent (used for SeedData sync on startup).
     */
    suspend fun upsert(req: UpsertEraRequest): EraRow {
        val now = System.currentTimeMillis()
        val charIdsJson = json.encodeToString(req.availableCharacterIds)

        newSuspendedTransaction {
            val exists = ErasTable
                .selectAll()
                .where { ErasTable.id eq req.id }
                .count() > 0

            if (exists) {
                ErasTable.update({ ErasTable.id eq req.id }) {
                    it[name]                    = req.name
                    it[description]             = req.description
                    it[emoji]                   = req.emoji
                    it[startYear]               = req.startYear
                    it[endYear]                 = req.endYear
                    it[availableCharacterIds]   = charIdsJson
                    it[isActive]                = req.isActive
                    it[isLocked]                = req.isLocked
                    it[updatedAt]               = now
                }
            } else {
                ErasTable.insert {
                    it[id]                      = req.id
                    it[name]                    = req.name
                    it[description]             = req.description
                    it[emoji]                   = req.emoji
                    it[startYear]               = req.startYear
                    it[endYear]                 = req.endYear
                    it[availableCharacterIds]   = charIdsJson
                    it[isActive]                = req.isActive
                    it[isLocked]                = req.isLocked
                    it[createdAt]               = now
                    it[updatedAt]               = now
                }
            }
        }

        return findById(req.id)!!
    }

    /** Bulk upsert — used on startup to sync hardcoded SeedData to the DB. */
    suspend fun upsertAll(eras: List<UpsertEraRequest>) {
        eras.forEach { upsert(it) }
    }

    // ── Soft delete ───────────────────────────────────────────────────────────

    /**
     * Marks an era as inactive (soft-delete).
     * Statistics are preserved — use [deleteWithStatsCascade] to also wipe them.
     */
    suspend fun softDelete(eraId: String): Boolean {
        val now = System.currentTimeMillis()
        return newSuspendedTransaction {
            val updated = ErasTable.update({ ErasTable.id eq eraId }) {
                it[isActive]  = false
                it[updatedAt] = now
            }
            updated > 0
        }
    }

    // ── Hard delete with cascade ──────────────────────────────────────────────

    /**
     * Permanently deletes an era AND all [CompletedSessionsTable] rows
     * that reference this [eraId] — across ALL users.
     *
     * Application-level cascade (atomic — both deletes in one transaction).
     * Returns how many statistics rows were deleted.
     */
    suspend fun deleteWithStatsCascade(eraId: String): EraDeleteResult =
        newSuspendedTransaction {
            // 1. Delete statistics first (child rows)
            val statsDeleted = CompletedSessionsTable.deleteWhere {
                CompletedSessionsTable.eraId eq eraId
            }

            // 2. Delete the era itself
            val eraDeleted = ErasTable.deleteWhere {
                ErasTable.id eq eraId
            }

            EraDeleteResult(
                eraFound         = eraDeleted > 0,
                statsRowsDeleted = statsDeleted
            )
        }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun ResultRow.toRow(): EraRow {
        val rawCharIds = this[ErasTable.availableCharacterIds]
        val parsedCharIds = runCatching {
            json.decodeFromString<List<String>>(rawCharIds)
        }.getOrDefault(emptyList())

        return EraRow(
            id                    = this[ErasTable.id],
            name                  = this[ErasTable.name],
            description           = this[ErasTable.description],
            emoji                 = this[ErasTable.emoji],
            startYear             = this[ErasTable.startYear],
            endYear               = this[ErasTable.endYear],
            availableCharacterIds = parsedCharIds,
            isActive              = this[ErasTable.isActive],
            isLocked              = this[ErasTable.isLocked],
            createdAt             = this[ErasTable.createdAt],
            updatedAt             = this[ErasTable.updatedAt]
        )
    }
}

data class EraDeleteResult(
    val eraFound: Boolean,
    val statsRowsDeleted: Int
)
