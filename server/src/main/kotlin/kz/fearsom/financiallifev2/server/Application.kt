package kz.fearsom.financiallifev2.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import kz.fearsom.financiallifev2.data.SeedData
import kz.fearsom.financiallifev2.server.database.DatabaseFactory
import kz.fearsom.financiallifev2.server.plugins.configureCORS
import kz.fearsom.financiallifev2.server.plugins.configureLogging
import kz.fearsom.financiallifev2.server.plugins.configureRouting
import kz.fearsom.financiallifev2.server.plugins.configureSecurity
import kz.fearsom.financiallifev2.server.plugins.configureSecurityHeaders
import kz.fearsom.financiallifev2.server.plugins.configureSerialization
import kz.fearsom.financiallifev2.server.plugins.configureStatusPages
import kz.fearsom.financiallifev2.server.repository.CharactersRepository
import kz.fearsom.financiallifev2.server.repository.ErasRepository
import kz.fearsom.financiallifev2.server.repository.GameRepository
import kz.fearsom.financiallifev2.server.repository.StatisticsRepository
import kz.fearsom.financiallifev2.server.repository.UpsertCharacterRequest
import kz.fearsom.financiallifev2.server.repository.UpsertEraRequest
import kz.fearsom.financiallifev2.server.repository.UserRepository

fun main() {
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 8082,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // 1. Connect to PostgreSQL and run DDL (creates missing tables automatically)
    DatabaseFactory.init()

    // 2. Repositories (singletons for server lifetime)
    val userRepository        = UserRepository()
    val gameRepository        = GameRepository()
    val statisticsRepository  = StatisticsRepository()
    val charactersRepository  = CharactersRepository()
    val erasRepository        = ErasRepository()

    // 3. Seed hardcoded characters into DB (upsert — safe to run on every restart)
    runBlocking {
        val seedRequests = buildList {
            SeedData.predefinedCharacters.forEach { c ->
                add(UpsertCharacterRequest(
                    id       = c.id,
                    name     = c.name,
                    emoji    = c.emoji,
                    type     = "PREDEFINED",
                    eraIds   = c.compatibleEraIds,
                    isActive = true
                ))
            }
            SeedData.characterBundles.forEach { b ->
                add(UpsertCharacterRequest(
                    id       = b.id,
                    name     = b.label,
                    emoji    = b.emoji,
                    type     = "BUNDLE",
                    eraIds   = b.compatibleEraIds,
                    isActive = !b.isLocked
                ))
            }
        }
        charactersRepository.upsertAll(seedRequests)

        erasRepository.upsertAll(SeedData.eras.map { e ->
            UpsertEraRequest(
                id                    = e.id,
                name                  = e.name,
                description           = e.description,
                emoji                 = e.emoji,
                startYear             = e.startYear,
                endYear               = e.endYear,
                availableCharacterIds = e.availableCharacterIds,
                isActive              = true,
                isLocked              = e.isLocked
            )
        })
    }

    // 4. Ktor plugins
    // SecurityHeaders must be first so every response gets the headers,
    // including error responses from other plugins.
    configureSecurityHeaders()
    configureLogging()
    configureSerialization()
    configureCORS()
    configureStatusPages()
    configureSecurity()           // JWT auth plugin
    configureRouting(userRepository, gameRepository, statisticsRepository, charactersRepository, erasRepository)
}
