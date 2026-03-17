package kz.fearsom.financiallifev2

import kz.fearsom.financiallifev2.data.GameSessionRepository
import kz.fearsom.financiallifev2.data.SeedData
import kz.fearsom.financiallifev2.model.CharacterType
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeAppCommonTest {

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
}
