package kz.fearsom.financiallifev2.server.database.tables

import org.jetbrains.exposed.sql.Table

/**
 * Stores all playable characters (predefined + bundles).
 *
 * Characters are seeded from SeedData on server startup via CharactersRepository.upsertAll().
 * Admin API can create, update, or soft-delete characters.
 *
 * Deleting a character from this table will cascade-delete all rows in
 * [CompletedSessionsTable] that reference the same [id] via application-level cascade
 * inside CharactersRepository.deleteWithStatsCascade().
 *
 * [id]           — stable identifier, e.g. "aidar", "bundle_crypto"
 * [name]         — display name, e.g. "Айдар"
 * [emoji]        — display emoji, e.g. "🧑‍💻"
 * [type]         — "PREDEFINED" or "BUNDLE"
 * [eraIds]       — JSON array of compatible era IDs, e.g. ["kz_2005","kz_2015"]
 * [isActive]     — soft-delete flag; false = hidden from UI but stats preserved or deleteable
 * [createdAt]    — epoch millis of first insert
 * [updatedAt]    — epoch millis of last upsert
 */
object CharactersTable : Table("characters") {
    val id        = varchar("id", 100)
    val name      = varchar("name", 100)
    val emoji     = varchar("emoji", 20)
    val type      = varchar("type", 20)      // "PREDEFINED" | "BUNDLE"
    val eraIds    = text("era_ids")           // JSON array stored as text
    val isActive  = bool("is_active").default(true)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
