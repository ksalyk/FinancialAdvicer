package kz.fearsom.financiallifev2.data

import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.scenarios.ScenarioGraphFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory repository for [GameSession] objects and their associated engine states.
 *
 * Responsibilities:
 *  - Create / list / complete sessions
 *  - Store serialised [GameState] so "Continue" can restore exactly where the player left off
 *  - Keep a reactive [sessions] flow so UIs observe changes automatically
 *
 * No persistence yet — state survives app-foreground usage but not process death.
 * Replace with SQLDelight + server sync in a future sprint.
 */
class GameSessionRepository {

    private val _sessions = MutableStateFlow<List<GameSession>>(emptyList())
    val sessions: StateFlow<List<GameSession>> = _sessions.asStateFlow()

    // Saved engine states keyed by sessionId — populated on navigate-away from ChatScreen
    private val savedStates = mutableMapOf<String, GameState>()

    private var sessionCounter = 0

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
            id              = "session_${++sessionCounter}",
            userId          = userId,
            eraId           = era.id,
            eraName         = era.name,
            characterType   = characterType,
            characterId     = characterId,
            characterName   = characterName,
            characterEmoji  = characterEmoji,
            characterTitle  = characterTitle,
            initialStats    = initialStats,
            currentStats    = initialStats,
            currentGameYear = scenarioStart.year,
            currentGameMonth = scenarioStart.month
        )
        _sessions.update { it + session }
        return session
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    fun getActiveSession(): GameSession? =
        _sessions.value.lastOrNull { it.status == SessionStatus.ACTIVE }

    fun getSession(sessionId: String): GameSession? =
        _sessions.value.find { it.id == sessionId }

    fun getAllSessions(): List<GameSession> = _sessions.value

    // ── Saved Engine State ────────────────────────────────────────────────────

    fun saveGameState(sessionId: String, gameState: GameState) {
        savedStates[sessionId] = gameState
        val ps = gameState.playerState
        _sessions.update { list ->
            list.map { s ->
                if (s.id == sessionId)
                    s.copy(
                        currentGameYear  = ps.year,
                        currentGameMonth = ps.month,
                        currentStats     = CharacterStats(
                            capital            = ps.capital,
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
    }

    // ── Statistics helpers ────────────────────────────────────────────────────

    fun getQuickStats(): QuickStats? {
        val all = _sessions.value
        if (all.isEmpty()) return null
        val completed = all.filter { it.status == SessionStatus.COMPLETED }
        val bestEnding = completed
            .mapNotNull { it.ending }
            .maxByOrNull { it.ordinal }
        return QuickStats(
            totalGames           = all.size,
            bestEnding           = bestEnding,
            lastPlayedCharacter  = all.lastOrNull()?.characterName
        )
    }

    fun getPlayerStatistics(): PlayerStatistics {
        val all = _sessions.value
        val completed = all.filter { it.status == SessionStatus.COMPLETED }
        val bestEnding = completed.mapNotNull { it.ending }.maxByOrNull { it.ordinal }
        val avgCapital = if (completed.isNotEmpty())
            completed.sumOf { it.currentStats.capital } / completed.size
        else 0L
        val endingDist = GameEnding.entries.associateWith { ending ->
            completed.count { it.ending == ending }
        }
        val mostPlayedEra = all.groupBy { it.eraId }
            .maxByOrNull { it.value.size }?.key
        val perCharacter = all.groupBy { it.characterId }.map { (charId, sessions) ->
            CharacterStatistics(
                characterId    = charId,
                characterName  = sessions.first().characterName,
                characterEmoji = sessions.first().characterEmoji,
                timesPlayed    = sessions.size,
                bestEnding     = sessions.mapNotNull { it.ending }.maxByOrNull { it.ordinal },
                averageCapital = if (sessions.isNotEmpty())
                    sessions.sumOf { it.currentStats.capital } / sessions.size
                else 0L
            )
        }
        val perEra = all.groupBy { it.eraId }.map { (eraId, sessions) ->
            EraStatistics(
                eraId      = eraId,
                eraName    = sessions.first().eraName,
                timesPlayed = sessions.size,
                bestEnding = sessions.mapNotNull { it.ending }.maxByOrNull { it.ordinal }
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
}
