package kz.fearsom.financiallifev2

import kz.fearsom.financiallifev2.data.GameSessionRepository
import kz.fearsom.financiallifev2.data.SeedData
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.CharacterType
import kz.fearsom.financiallifev2.model.GameEnding
import kz.fearsom.financiallifev2.model.GameState
import kz.fearsom.financiallifev2.scenarios.ScenarioGraphFactory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ComposeAppCommonTest {

    private val originalLocale = Strings.currentLocale

    @AfterTest
    fun tearDown() {
        Strings.currentLocale = originalLocale
    }

    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }

    @Test
    fun `session start date follows scenario graph instead of raw era start`() {
        // secureStorage defaults to null → in-memory only, no platform deps needed in tests
        val repo = GameSessionRepository()
        val era = SeedData.eras.first { it.id == "kz_90s" }
        val character = SeedData.predefinedCharacters.first { it.id == "aidar_90s" }

        val session = repo.createSession(
            era = era,
            characterType = CharacterType.PREDEFINED,
            characterId = character.id,
            characterName = character.name,
            characterEmoji = character.emoji,
            characterTitle = character.profession,
            initialStats = character.initialStats
        )

        assertEquals(1993, session.currentGameYear)
        assertEquals(1, session.currentGameMonth)
    }

    @Test
    fun `session labels resolve using current locale when read`() {
        Strings.currentLocale = "ru"
        val repo = GameSessionRepository()
        val era = SeedData.eras.first { it.id == "kz_2024" }
        val character = SeedData.predefinedCharacters.first { it.id == "aidar" }

        val session = repo.createSession(
            era = era,
            characterType = CharacterType.PREDEFINED,
            characterId = character.id,
            characterName = character.name,
            characterEmoji = character.emoji,
            characterTitle = character.profession,
            initialStats = character.initialStats
        )
        val ruSession = repo.getSession(session.id)!!

        Strings.currentLocale = "en"
        val enSession = repo.getSession(session.id)!!

        assertNotEquals(ruSession.eraName, enSession.eraName)
        assertEquals(ruSession.characterTitle, enSession.characterTitle)
    }

    @Test
    fun `completed statistics include final investments`() {
        val repo = GameSessionRepository()
        val era = SeedData.eras.first { it.id == "kz_2024" }
        val character = SeedData.predefinedCharacters.first { it.id == "asan" }
        val session = repo.createSession(
            era = era,
            characterType = CharacterType.PREDEFINED,
            characterId = character.id,
            characterName = character.name,
            characterEmoji = character.emoji,
            characterTitle = character.profession,
            initialStats = character.initialStats
        )
        val finalState = ScenarioGraphFactory
            .forCharacter(character.id, era.id)
            .initialPlayerState
            .copy(capital = 100_000L, investments = 250_000L)

        repo.saveGameState(
            session.id,
            GameState(
                playerState = finalState,
                currentEventId = "ending_regular",
                characterName = character.name,
                gameOver = true
            )
        )
        repo.completeSession(session.id, GameEnding.FINANCIAL_STABILITY)

        val stats = repo.getPlayerStatistics()
        assertEquals(350_000L, stats.averageCapitalAtEnd)
        assertEquals(250_000L, repo.getSession(session.id)?.currentStats?.investments)
    }
}
