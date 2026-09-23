package kz.fearsom.financiallifev2.data

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.achievements.AchievementDefinition
import kz.fearsom.financiallifev2.achievements.AchievementEvaluator
import kz.fearsom.financiallifev2.achievements.AchievementSessionFacts
import kz.fearsom.financiallifev2.achievements.AchievementUnlockDto
import kz.fearsom.financiallifev2.achievements.AchievementVote
import kz.fearsom.financiallifev2.model.GameState
import kz.fearsom.financiallifev2.network.AchievementApiService
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val TAG = "AchievementsRepository"

private const val KEY_UNLOCKS      = "achievements_unlocks"
private const val KEY_PENDING_SYNC = "achievements_pending_sync"
private const val KEY_VOTES        = "achievements_votes"

/**
 * Client-side achievement store: offline-first, server-synced.
 *
 * - Unlock detection runs locally ([onGameState]) via the shared
 *   [AchievementEvaluator]; unlocks persist to [SecureStorage] immediately, so
 *   guests keep their collection.
 * - Every locally earned unlock id also lands in a pending-sync set; [sync]
 *   pushes pending unlocks and reconciles with the server list (union — the
 *   server is additive-only, first unlock wins on both sides).
 * - Dossier votes are stored locally and mirrored to the server fire-and-forget.
 *
 * Threading: state mutations happen on the caller's dispatcher; SecureStorage
 * writes are best-effort synchronous, same trade-off as [GameSessionRepository].
 */
class AchievementsRepository(
    private val secureStorage: SecureStorage? = null,
    private val api: AchievementApiService? = null,
    private val catalogStore: AchievementCatalogStore = AchievementCatalogStore()
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _unlocks = MutableStateFlow<Map<String, AchievementUnlockDto>>(emptyMap())
    /** achievementId → unlock record. */
    val unlocks: StateFlow<Map<String, AchievementUnlockDto>> = _unlocks.asStateFlow()

    private val _votes = MutableStateFlow<Map<String, String>>(emptyMap())
    /** achievementId → "up" | "down". */
    val votes: StateFlow<Map<String, String>> = _votes.asStateFlow()

    /** Ids unlocked locally but not yet confirmed by the server. */
    private val _pendingSync = MutableStateFlow<Set<String>>(emptySet())

    // ── Per-session unlock tracking ───────────────────────────────────────────
    private var facts = AchievementSessionFacts()
    private var trackedSessionId: String? = null
    private var sessionCharacterId: String? = null
    private var sessionEraId: String? = null

    init {
        restoreFromStorage()
    }

    // ── Session tracking API (called by GamePresenter) ────────────────────────

    /**
     * (Re)arms unlock tracking for a game session.
     *
     * Facts reset when the session changes OR [freshRun] is true (new game /
     * restart of the same session); they are kept when re-entering the same
     * session ("Continue") so a crisis window survives menu navigation.
     */
    fun startSessionTracking(
        sessionId: String,
        characterId: String,
        eraId: String,
        freshRun: Boolean = false
    ) {
        if (freshRun || sessionId != trackedSessionId) {
            facts = AchievementSessionFacts()
        }
        trackedSessionId   = sessionId
        sessionCharacterId = characterId
        sessionEraId       = eraId
    }

    /**
     * Feed every engine state emission here. Evaluates unlock conditions and
     * records anything newly earned.
     *
     * @return ids unlocked by THIS state emission (for future toast/badge UI).
     */
    @OptIn(ExperimentalTime::class)
    fun onGameState(state: GameState): Set<String> {
        facts = facts.update(state)

        val newlyUnlocked = AchievementEvaluator.newlyUnlocked(
            state           = state,
            facts           = facts,
            alreadyUnlocked = _unlocks.value.keys,
            catalog         = catalogStore.current   // active (admin-curated) catalog
        )
        if (newlyUnlocked.isEmpty()) return emptySet()

        val now = Clock.System.now().toEpochMilliseconds()
        val added = newlyUnlocked.associateWith { id ->
            AchievementUnlockDto(
                achievementId     = id,
                unlockedAt        = now,
                sourceCharacterId = sessionCharacterId ?: state.playerState.characterId.ifEmpty { null },
                sourceEraId       = sessionEraId ?: state.playerState.eraId.ifEmpty { null }
            )
        }

        _unlocks.update { it + added }
        _pendingSync.update { it + newlyUnlocked }
        persist()

        Napier.i("Achievements unlocked locally: $newlyUnlocked", tag = TAG)
        return newlyUnlocked
    }

    // ── Server sync ───────────────────────────────────────────────────────────

    /**
     * Push pending unlocks, then reconcile with the server's list.
     * Silent no-op offline / unauthenticated — local state stays authoritative
     * until the next successful sync.
     */
    suspend fun sync() {
        val service = api ?: return

        val snapshot = _pendingSync.value
        val pending = snapshot.mapNotNull { _unlocks.value[it] }
        val result = if (pending.isNotEmpty()) {
            service.pushUnlocks(pending)
        } else {
            service.getUnlocks()
        }

        result.onSuccess { serverUnlocks ->
            // Union merge: server response already contains everything we pushed.
            _unlocks.update { current ->
                val merged = current.toMutableMap()
                serverUnlocks.forEach { dto -> merged[dto.achievementId] = dto }
                merged
            }
            val confirmedIds = serverUnlocks.map { it.achievementId }.toSet()
            _pendingSync.update { it - confirmedIds }
            persist()
            Napier.d("Achievements synced: ${_unlocks.value.size} total, ${_pendingSync.value.size} pending", tag = TAG)
        }
    }

    // ── Feedback votes ────────────────────────────────────────────────────────

    /**
     * Toggle semantics matching the design: voting the same value again clears it.
     * Returns the resulting vote ("up"/"down") or null when cleared.
     */
    suspend fun toggleVote(achievementId: String, vote: String): String? {
        require(vote in AchievementVote.VALID) { "vote must be 'up' or 'down'" }

        val next = if (_votes.value[achievementId] == vote) null else vote
        _votes.value = if (next == null) {
            _votes.value - achievementId
        } else {
            _votes.value + (achievementId to next)
        }
        persist()

        api?.sendFeedback(achievementId, next)   // fire-and-forget, logged inside
        return next
    }

    // ── Counters (main-menu subtitle) ─────────────────────────────────────────

    val unlockedCount: Int get() = _unlocks.value.size
    val totalCount: Int get() = catalogStore.current.size

    // ── Active catalog (admin-curated) ─────────────────────────────────────────

    /** Active definitions (compile-time fallback → server overlay). */
    val catalogDefinitions: StateFlow<List<AchievementDefinition>> get() = catalogStore.definitions

    /** Pull the server's active catalog. Safe no-op offline / without api. */
    suspend fun refreshCatalog() = catalogStore.refresh()

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun restoreFromStorage() {
        val storage = secureStorage ?: return
        runCatching {
            storage.get(KEY_UNLOCKS)?.takeIf { it.isNotBlank() }?.let {
                val restored = json.decodeFromString<List<AchievementUnlockDto>>(it)
                    .filter { dto -> catalogStore.isValidId(dto.achievementId) }
                _unlocks.value = restored.associateBy { dto -> dto.achievementId }
            }
        }
        runCatching {
            storage.get(KEY_PENDING_SYNC)?.takeIf { it.isNotBlank() }?.let {
                _pendingSync.value = json.decodeFromString<Set<String>>(it)
            }
        }
        runCatching {
            storage.get(KEY_VOTES)?.takeIf { it.isNotBlank() }?.let {
                _votes.value = json.decodeFromString<Map<String, String>>(it)
            }
        }
    }

    private fun persist() {
        val storage = secureStorage ?: return
        runCatching { storage.save(KEY_UNLOCKS, json.encodeToString(_unlocks.value.values.toList())) }
        runCatching { storage.save(KEY_PENDING_SYNC, json.encodeToString(_pendingSync.value)) }
        runCatching { storage.save(KEY_VOTES, json.encodeToString(_votes.value)) }
    }
}
