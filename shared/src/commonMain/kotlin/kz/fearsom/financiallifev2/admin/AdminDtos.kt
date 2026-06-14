package kz.fearsom.financiallifev2.admin

import kotlinx.serialization.Serializable
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph

// ── Character DTOs ────────────────────────────────────────────────────────────

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

// ── Era DTOs ──────────────────────────────────────────────────────────────────

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

// ── User DTOs ─────────────────────────────────────────────────────────────────

/** Never includes passwordHash — safe to send to admin client. */
@Serializable
data class AdminUserRow(
    val id: String,
    val username: String,
    val createdAt: Long,
    val gamesPlayed: Int
)

@Serializable
data class AdminUserListResponse(val items: List<AdminUserRow>, val total: Long)

@Serializable
data class AdminUserDetailRow(
    val id: String,
    val username: String,
    val createdAt: Long,
    val gamesPlayed: Int,
    val bestEnding: String?,
    val averageCapitalAtEnd: Long,
    val endingDistribution: Map<String, Int>
)

// ── Game catalog DTO ──────────────────────────────────────────────────────────

/**
 * Active-only character + era catalog served to the game client at `/game/catalog`.
 *
 * Reuses [CharacterRow]/[EraRow] (already shared + serializable). The client overlays
 * this onto its in-code [kz.fearsom.financiallifev2.data.SeedData] so admin toggles
 * (active/name/emoji/era-membership) take effect, while gameplay-only data
 * (initialStats, profession, localized era text, scenario graphs) stays in code.
 */
@Serializable
data class GameCatalogResponse(
    val characters: List<CharacterRow>,
    val eras: List<EraRow>
)

// ── Scenario DTOs ─────────────────────────────────────────────────────────────

@Serializable
data class ScenarioComboDto(
    val characterId: String,
    val eraId: String,
    val label: String
)

@Serializable
data class ScenarioGraphDto(
    val initialPlayerState: PlayerState,
    val events: List<GameEvent>,
    val conditionalEvents: List<GameEvent>,
    val eventPool: List<PoolEntry>
)

fun ScenarioGraph.toDto() = ScenarioGraphDto(
    initialPlayerState  = initialPlayerState,
    events              = events.values.toList(),
    conditionalEvents   = conditionalEvents,
    eventPool           = eventPool
)
