package kz.fearsom.financiallifev2.server.repository

import kotlinx.coroutines.runBlocking
import kz.fearsom.financiallifev2.server.database.DatabaseTestFixture
import kz.fearsom.financiallifev2.server.database.tables.UsersTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class DatabaseStatisticsRepositoryTest {

    private val db = DatabaseTestFixture.database

    @Before
    fun reset() {
        DatabaseTestFixture.reset()
    }

    @Test
    fun `player statistics average includes final investments`() = runBlocking {
        val userId = "user-investments-001"
        transaction(db) {
            UsersTable.insert {
                it[id] = userId
                it[username] = "investments_user"
                it[passwordHash] = "0".repeat(64)
                it[createdAt] = 1L
            }
        }

        val repo = DatabaseStatisticsRepository(db)
        repo.recordSession(
            userId,
            RecordSessionRequest(
                characterId = "asan",
                characterName = "Asan",
                characterEmoji = "A",
                eraId = "kz_2024",
                eraName = "Modern Kazakhstan",
                ending = "FINANCIAL_STABILITY",
                finalCapital = 100_000L,
                finalInvestments = 250_000L,
                finalDebt = 0L,
                finalStress = 30,
                finalKnowledge = 50,
                finalRiskLevel = 20,
                gameYear = 2024,
                gameMonth = 12
            )
        )

        val stats = repo.getPlayerStatistics(userId)

        assertEquals(350_000L, stats.averageCapitalAtEnd)
        assertEquals(350_000L, stats.perCharacter.single().averageCapital)
    }
}
