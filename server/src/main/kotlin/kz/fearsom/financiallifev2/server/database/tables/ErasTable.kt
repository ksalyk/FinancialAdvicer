package kz.fearsom.financiallifev2.server.database.tables

import org.jetbrains.exposed.sql.Table

/**
 * Stores all playable eras.
 *
 * Eras are seeded from SeedData on server startup via ErasRepository.upsertAll().
 * Admin API can create, update, or soft-delete eras.
 *
 * Deleting an era from this table cascades to all [CompletedSessionsTable] rows
 * with the same [id] via application-level cascade in ErasRepository.deleteWithStatsCascade().
 *
 * [id]                  — stable identifier, e.g. "kz_2005"
 * [name]                — display name, e.g. "Казахстан 2005–2010"
 * [description]         — short description shown in UI
 * [emoji]               — display emoji, e.g. "🏗️"
 * [startYear]           — in-game start year
 * [endYear]             — in-game end year
 * [availableCharacterIds] — JSON array of character IDs playable in this era
 * [isActive]            — soft-delete flag; false = hidden from UI
 * [isLocked]            — requires unlock condition to play
 * [createdAt]           — epoch millis of first insert
 * [updatedAt]           — epoch millis of last upsert
 */
object ErasTable : Table("eras") {
    val id                    = varchar("id", 100)
    val name                  = varchar("name", 100)
    val description           = text("description")
    val emoji                 = varchar("emoji", 20)
    val startYear             = integer("start_year")
    val endYear               = integer("end_year")
    val availableCharacterIds = text("available_character_ids")  // JSON array
    val isActive              = bool("is_active").default(true)
    val isLocked              = bool("is_locked").default(false)
    val createdAt             = long("created_at")
    val updatedAt             = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
