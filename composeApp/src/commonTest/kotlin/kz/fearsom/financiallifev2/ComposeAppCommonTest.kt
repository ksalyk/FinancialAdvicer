package kz.fearsom.financiallifev2

import kz.fearsom.financiallifev2.data.GameSessionRepository
import kz.fearsom.financiallifev2.data.SeedData
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.CharacterType
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
        assertEquals(10, session.currentGameMonth)
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
        assertNotEquals(ruSession.characterTitle, enSession.characterTitle)
    }
}
