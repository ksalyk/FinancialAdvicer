package kz.fearsom.financiallifev2.server.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.server.database.tables.CharactersTable
import kz.fearsom.financiallifev2.server.database.tables.CompletedSessionsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

// ── DTOs ─────────────────────────────────────────────────────────────────────

@Serializable
data class CharacterRow(
    val id: String,
    val name: String,
    val emoji: String,
    val type: String,       // "PREDEFINED" | "BUNDLE"
    val eraIds: List<String>,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class UpsertCharacterRequest(
    val id: String,
    val name: String,
    val emoji: String,
    val type: String,
    val eraIds: List<String>,
    val isActive: Boolean = true
)

// ── Repository ────────────────────────────────────────────────────────────────

class CharactersRepository {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Read ──────────────────────────────────────────────────────────────────

    suspend fun findById(id: String): CharacterRow? =
        newSuspendedTransaction {
            CharactersTable
                .selectAll()
                .where { CharactersTable.id eq id }
                .singleOrNull()
                ?.toRow()
        }

    /** Returns all characters; [activeOnly] = false returns soft-deleted ones too. */
    suspend fun listAll(activeOnly: Boolean = true): List<CharacterRow> =
        newSuspendedTransaction {
            val query = CharactersTable.selectAll()
            if (activeOnly) query.where { CharactersTable.isActive eq true }
            query.map { it.toRow() }
        }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Insert or update a character by [UpsertCharacterRequest.id].
     * Safe to call multiple times — idempotent (used for SeedData sync on startup).
     */
    suspend fun upsert(req: UpsertCharacterRequest): CharacterRow {
        val now = System.currentTimeMillis()
        val eraIdsJson = json.encodeToString(req.eraIds)

        newSuspendedTransaction {
            val exists = CharactersTable
                .selectAll()
                .where { CharactersTable.id eq req.id }
                .count() > 0

            if (exists) {
                CharactersTable.update({ CharactersTable.id eq req.id }) {
                    it[name]      = req.name
                    it[emoji]     = req.emoji
                    it[type]      = req.type
                    it[eraIds]    = eraIdsJson
                    it[isActive]  = req.isActive
                    it[updatedAt] = now
                }
            } else {
                CharactersTable.insert {
                    it[id]        = req.id
                    it[name]      = req.name
                    it[emoji]     = req.emoji
                    it[type]      = req.type
                    it[eraIds]    = eraIdsJson
                    it[isActive]  = req.isActive
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        }

        return findById(req.id)!!
    }

    /** Bulk upsert — used on startup to sync hardcoded SeedData to the DB. */
    suspend fun upsertAll(characters: List<UpsertCharacterRequest>) {
        characters.forEach { upsert(it) }
    }

    // ── Soft delete ───────────────────────────────────────────────────────────

    /**
     * Marks a character as inactive (soft-delete).
     * Statistics are preserved — use [deleteWithStatsCascade] to also wipe them.
     */
    suspend fun softDelete(characterId: String): Boolean {
        val now = System.currentTimeMillis()
        return newSuspendedTransaction {
            val updated = CharactersTable.update({ CharactersTable.id eq characterId }) {
                it[isActive]  = false
                it[updatedAt] = now
            }
            updated > 0
        }
    }

    // ── Hard delete with cascade ──────────────────────────────────────────────

    /**
     * Permanently deletes a character AND all [CompletedSessionsTable] rows
     * that reference this [characterId] — across ALL users.
     *
     * This is application-level cascade (no DB-level FK needed).
     * Both deletes happen in the same transaction so they're atomic.
     *
     * Returns the number of statistics rows deleted.
     */
    suspend fun deleteWithStatsCascade(characterId: String): DeleteResult =
        newSuspendedTransaction {
            // 1. Delete statistics first (child rows)
            val statsDeleted = CompletedSessionsTable.deleteWhere {
                CompletedSessionsTable.characterId eq characterId
            }

            // 2. Delete the character itself
            val charDeleted = CharactersTable.deleteWhere {
                CharactersTable.id eq characterId
            }

            DeleteResult(
                characterFound   = charDeleted > 0,
                statsRowsDeleted = statsDeleted
            )
        }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun ResultRow.toRow(): CharacterRow {
        val rawEraIds = this[CharactersTable.eraIds]
        val parsedEraIds = runCatching {
            json.decodeFromString<List<String>>(rawEraIds)
        }.getOrDefault(emptyList())

        return CharacterRow(
            id        = this[CharactersTable.id],
            name      = this[CharactersTable.name],
            emoji     = this[CharactersTable.emoji],
            type      = this[CharactersTable.type],
            eraIds    = parsedEraIds,
            isActive  = this[CharactersTable.isActive],
            createdAt = this[CharactersTable.createdAt],
            updatedAt = this[CharactersTable.updatedAt]
        )
    }
}

data class DeleteResult(
    val characterFound: Boolean,
    val statsRowsDeleted: Int
)
