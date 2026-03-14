package kz.fearsom.financiallifev2.server.database.tables

import org.jetbrains.exposed.sql.Table

/**
 * Append-only log of every completed (or abandoned) game session.
 *
 * One row per finished game — never updated after insert.
 * This is the source of truth for all statistics queries.
 *
 * [id]              — UUID, PK
 * [userId]          — FK to users
 * [characterId]     — e.g. "asan"
 * [characterName]   — display name, e.g. "Асан"
 * [characterEmoji]  — e.g. "👨‍💻"
 * [eraId]           — e.g. "modern_kz_2024"
 * [eraName]         — display name
 * [ending]          — GameEnding.name()
 * [finalCapital]    — liquid savings at game end (₸)
 * [finalInvestments]— portfolio value at game end (₸)
 * [finalDebt]       — outstanding debt at game end (₸)
 * [finalStress]     — stress metric 0–100
 * [finalKnowledge]  — financial knowledge 0–100
 * [finalRiskLevel]  — risk level 0–100
 * [gameYear]        — in-game year when game ended
 * [gameMonth]       — in-game month when game ended
 * [completedAt]     — wall-clock epoch millis of completion
 */
object CompletedSessionsTable : Table("completed_sessions") {
    val id               = varchar("id", 36)
    val userId           = varchar("user_id", 36).references(UsersTable.id)
    val characterId      = varchar("character_id", 100)
    val characterName    = varchar("character_name", 100)
    val characterEmoji   = varchar("character_emoji", 20)
    val eraId            = varchar("era_id", 100)
    val eraName          = varchar("era_name", 100)
    val ending           = varchar("ending", 50)          // GameEnding.name
    val finalCapital     = long("final_capital")
    val finalInvestments = long("final_investments")
    val finalDebt        = long("final_debt")
    val finalStress      = integer("final_stress")
    val finalKnowledge   = integer("final_knowledge")
    val finalRiskLevel   = integer("final_risk_level")
    val gameYear         = integer("game_year")
    val gameMonth        = integer("game_month")
    val completedAt      = long("completed_at")

    override val primaryKey = PrimaryKey(id)

    // Fast per-user queries (most common access pattern)
    val userIdx = index("idx_completed_sessions_user", isUnique = false, userId)
}
