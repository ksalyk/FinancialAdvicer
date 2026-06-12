package kz.fearsom.financiallifev2.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.scenarios.ScenarioGraphFactory

private const val KEY_SESSIONS     = "game_session_repo_sessions"
private const val KEY_SAVED_STATES = "game_session_repo_saved_states"

/**
 * Repository for [GameSession] objects and their associated engine states.
 *
 * Responsibilities:
 *  - Create / list / complete sessions
 *  - Store serialised [GameState] so "Continue" can restore exactly where the player left off
 *  - Keep a reactive [sessions] flow so UIs observe changes automatically
 *  - **Persist** sessions and saved states to [SecureStorage] so state survives process death
 *
 * ## Persistence strategy
 * Both [sessions] and [savedStates] are serialised to JSON and written to [SecureStorage]
 * on every mutative operation. On construction the repository attempts to restore from
 * storage so a cold restart resumes from the last known state.
 *
 * [SecureStorage] operations are synchronous by design — call from a non-main dispatcher
 * (e.g. `Dispatchers.IO`) if the repo is constructed off the main thread, or rely on
 * Koin's lazy singleton creation which happens on the first background access.
 *
 * @param secureStorage  Platform-specific encrypted key-value store.
 *                       Defaults to `null` — when null, persistence is skipped (useful in
 *                       unit tests where no platform context is available). In production,
 *                       Koin injects the `actual` [SecureStorage] instance.
 */
class GameSessionRepository(
    private val secureStorage: SecureStorage? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults    = true
    }

    private val _sessions = MutableStateFlow<List<GameSession>>(emptyList())
    val sessions: StateFlow<List<GameSession>> = _sessions.asStateFlow()

    // Saved engine states keyed by sessionId — populated on navigate-away from ChatScreen
    private val savedStates = mutableMapOf<String, GameState>()

    private var sessionCounter = 0

    init {
        restoreFromStorage()
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun restoreFromStorage() {
        if (secureStorage == null) return
        try {
            val sessionsJson = secureStorage.get(KEY_SESSIONS)
            if (!sessionsJson.isNullOrBlank()) {
                val restored = json.decodeFromString<List<GameSession>>(sessionsJson)
                _sessions.value = restored
                // Advance counter past the highest existing session number to avoid ID collisions
                sessionCounter = restored
                    .mapNotNull { it.id.removePrefix("session_").toIntOrNull() }
                    .maxOrNull() ?: 0
            }
        } catch (_: Exception) {
            // Corrupt or incompatible storage — start fresh; old data is discarded.
        }

        try {
            val statesJson = secureStorage.get(KEY_SAVED_STATES)
            if (!statesJson.isNullOrBlank()) {
                val restored = json.decodeFromString<Map<String, GameState>>(statesJson)
                savedStates.putAll(restored)
            }
        } catch (_: Exception) {
            // Same: start with empty map on corruption.
        }
    }

    private fun persistSessions() {
        secureStorage ?: return
        try {
            secureStorage.save(KEY_SESSIONS, json.encodeToString(_sessions.value))
        } catch (_: Exception) { /* best-effort; UI state is unaffected */ }
    }

    private fun persistSavedStates() {
        secureStorage ?: return
        try {
            secureStorage.save(KEY_SAVED_STATES, json.encodeToString(savedStates.toMap()))
        } catch (_: Exception) { /* best-effort */ }
    }

    // ── Create ────────────────────────────────────────────────────────────────

    fun createSession(
        userId: String = "local_user",
        era: Era,
        characterType: CharacterType,
        characterId: String,
        characterName: String,
        characterEmoji: String,
        characterTitle: String,
        initialStats: CharacterStats
    ): GameSession {
        val scenarioStart = ScenarioGraphFactory
            .forCharacter(characterId, era.id)
            .initialPlayerState

        // Abandon any currently active sessions before starting a new one
        _sessions.update { list ->
            list.map { s ->
                if (s.status == SessionStatus.ACTIVE)
                    s.copy(status = SessionStatus.ABANDONED)
                else s
            }
        }

        val session = GameSession(
            id               = "session_${++sessionCounter}",
            userId           = userId,
            eraId            = era.id,
            eraName          = era.name,
            characterType    = characterType,
            characterId      = characterId,
            characterName    = characterName,
            characterEmoji   = characterEmoji,
            characterTitle   = characterTitle,
            initialStats     = initialStats,
            currentStats     = initialStats,
            currentGameYear  = scenarioStart.year,
            currentGameMonth = scenarioStart.month
        )
        _sessions.update { it + session }
        persistSessions()
        return session
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    fun getActiveSession(): GameSession? =
        _sessions.value.lastOrNull { it.status == SessionStatus.ACTIVE }?.localized()

    fun getSession(sessionId: String): GameSession? =
        _sessions.value.find { it.id == sessionId }?.localized()

    fun getAllSessions(): List<GameSession> = _sessions.value.map { it.localized() }

    // ── Saved Engine State ────────────────────────────────────────────────────

    fun saveGameState(sessionId: String, gameState: GameState) {
        savedStates[sessionId] = gameState
        persistSavedStates()

        val ps = gameState.playerState
        _sessions.update { list ->
            list.map { s ->
                if (s.id == sessionId)
                    s.copy(
                        currentGameYear  = ps.year,
                        currentGameMonth = ps.month,
                        currentStats     = CharacterStats(
                            capital            = ps.capital,
                            investments        = ps.investments,
                            income             = ps.income,
                            debt               = ps.debt,
                            monthlyExpenses    = ps.expenses,
                            stress             = ps.stress,
                            financialKnowledge = ps.financialKnowledge,
                            riskLevel          = ps.riskLevel
                        )
                    )
                else s
            }
        }
        persistSessions()
    }

    fun getSavedGameState(sessionId: String): GameState? =
        savedStates[sessionId]

    // ── Complete ──────────────────────────────────────────────────────────────

    fun completeSession(sessionId: String, ending: GameEnding) {
        _sessions.update { list ->
            list.map { s ->
                if (s.id == sessionId)
                    s.copy(status = SessionStatus.COMPLETED, ending = ending)
                else s
            }
        }
        persistSessions()
    }

    // ── Statistics helpers ────────────────────────────────────────────────────

    fun getQuickStats(): QuickStats? {
        val all = _sessions.value
        if (all.isEmpty()) return null
        val completed  = all.filter { it.status == SessionStatus.COMPLETED }
        val bestEnding = completed.mapNotNull { it.ending }.maxByOrNull { it.ordinal }
        return QuickStats(
            totalGames          = all.size,
            bestEnding          = bestEnding,
            lastPlayedCharacter = all.lastOrNull()?.localized()?.characterName
        )
    }

    fun getPlayerStatistics(): PlayerStatistics {
        val all       = _sessions.value
        val completed = all.filter { it.status == SessionStatus.COMPLETED }
        val bestEnding = completed.mapNotNull { it.ending }.maxByOrNull { it.ordinal }
        val avgCapital = if (completed.isNotEmpty())
            completed.sumOf { it.currentStats.capital + it.currentStats.investments } / completed.size
        else 0L
        val endingDist = GameEnding.entries.associateWith { ending ->
            completed.count { it.ending == ending }
        }
        val mostPlayedEra = all.groupBy { it.eraId }.maxByOrNull { it.value.size }?.key
        val perCharacter  = all.groupBy { it.characterId }.map { (charId, sessions) ->
            CharacterStatistics(
                characterId    = charId,
                characterName  = sessions.first().localized().characterName,
                characterEmoji = sessions.first().characterEmoji,
                timesPlayed    = sessions.size,
                bestEnding     = sessions.mapNotNull { it.ending }.maxByOrNull { it.ordinal },
                averageCapital = if (sessions.isNotEmpty())
                    sessions.sumOf { it.currentStats.capital + it.currentStats.investments } / sessions.size
                else 0L
            )
        }
        val perEra = all.groupBy { it.eraId }.map { (eraId, sessions) ->
            EraStatistics(
                eraId      = eraId,
                eraName    = sessions.first().localized().eraName,
                timesPlayed = sessions.size,
                bestEnding  = sessions.mapNotNull { it.ending }.maxByOrNull { it.ordinal }
            )
        }
        return PlayerStatistics(
            totalGamesPlayed    = all.size,
            gamesCompleted      = completed.size,
            bestEnding          = bestEnding,
            averageCapitalAtEnd = avgCapital,
            mostPlayedEraId     = mostPlayedEra,
            endingDistribution  = endingDist,
            perCharacter        = perCharacter,
            perEra              = perEra
        )
    }

    // ── Localization helper ───────────────────────────────────────────────────

    private fun GameSession.localized(): GameSession {
        val era        = SeedData.eras.find { it.id == eraId }
        val predefined = SeedData.predefinedCharacters.find { it.id == characterId }
        val bundle     = SeedData.characterBundles.find { it.id == characterId }
        return copy(
            eraName       = era?.name ?: eraName,
            characterName = predefined?.name ?: bundle?.label ?: characterName,
            characterTitle = predefined?.profession ?: bundle?.profession ?: characterTitle
        )
    }
}
