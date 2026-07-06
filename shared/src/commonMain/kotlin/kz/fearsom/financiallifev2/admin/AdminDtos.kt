package kz.fearsom.financiallifev2.admin

import kotlinx.serialization.Serializable
import kz.fearsom.financiallifev2.achievements.AchievementDefinition
import kz.fearsom.financiallifev2.achievements.AchievementUnlockDto
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

// ── Story DTOs (DB-backed scenario CRUD) ──────────────────────────────────────

/**
 * Lifecycle of a DB-backed story.
 *
 * Admin flow:   DRAFT → PUBLISHED → ARCHIVED (unpublish) → DRAFT (revise)
 * User flow:    DRAFT → PENDING_REVIEW → PUBLISHED | REJECTED → DRAFT (revise)
 *
 * Transitions are validated server-side (see StoriesRepository.transition).
 */
@Serializable
enum class StoryStatus { DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED, ARCHIVED }

/** Who authored the story. BUILT_IN snapshots are clones of code graphs. */
@Serializable
enum class StorySource { BUILT_IN, ADMIN, USER }

/** Story list row — metadata only, no graph payload. */
@Serializable
data class StoryRow(
    val id: String,
    val title: String,
    val description: String,
    val characterId: String,
    val eraId: String,
    val status: StoryStatus,
    val source: StorySource,
    /** Registered user who authored it (USER source); null for admin/built-in. */
    val authorId: String? = null,
    /** Moderator note shown to the author on REJECTED. */
    val reviewNote: String? = null,
    val eventCount: Int,
    val endingCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val publishedAt: Long? = null
)

/** Full story: metadata + graph. */
@Serializable
data class StoryDetail(
    val row: StoryRow,
    val graph: ScenarioGraphDto
)

@Serializable
data class UpsertStoryRequest(
    val id: String,
    val title: String,
    val description: String = "",
    val characterId: String,
    val eraId: String,
    val graph: ScenarioGraphDto
)

/** Body of POST /admin/stories/{id}/reject. */
@Serializable
data class StoryReviewRequest(val note: String? = null)

/** One validation finding for a story graph (serializable mirror of GraphWarning). */
@Serializable
data class StoryValidationIssue(
    /** "ERROR" | "WARN" | "INFO" */
    val severity: String,
    val eventId: String,
    val message: String
)

@Serializable
data class StoryValidationReport(
    val errors: Int,
    val warnings: Int,
    val infos: Int,
    val issues: List<StoryValidationIssue>
) {
    val publishable: Boolean get() = errors == 0
}

/** Published stories catalog for the game client / public frontend. */
@Serializable
data class PublishedStoriesResponse(val stories: List<StoryRow>)

// ── Achievement admin DTOs ────────────────────────────────────────────────────

/**
 * One catalog entry as seen by the admin panel: the full definition plus
 * DB metadata and aggregate usage stats.
 */
@Serializable
data class AchievementAdminRow(
    val definition: AchievementDefinition,
    val isActive: Boolean,
    val sortOrder: Int,
    /** "SEEDED" (from code catalog) | "CUSTOM" (created in admin). */
    val source: String,
    val unlockCount: Long,
    val upVotes: Long,
    val downVotes: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class UpsertAchievementRequest(
    val definition: AchievementDefinition,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

/** Unlock state of one user, for the admin user-detail view. */
@Serializable
data class UserAchievementsAdminResponse(
    val userId: String,
    val unlocks: List<AchievementUnlockDto>
)

/** Active achievement definitions served to game clients. */
@Serializable
data class AchievementCatalogResponse(val definitions: List<AchievementDefinition>)
