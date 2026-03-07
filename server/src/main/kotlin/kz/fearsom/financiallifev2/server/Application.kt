package kz.fearsom.financiallifev2.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kz.fearsom.financiallifev2.server.database.DatabaseFactory
import kz.fearsom.financiallifev2.server.plugins.configureCORS
import kz.fearsom.financiallifev2.server.plugins.configureLogging
import kz.fearsom.financiallifev2.server.plugins.configureRouting
import kz.fearsom.financiallifev2.server.plugins.configureSecurity
import kz.fearsom.financiallifev2.server.plugins.configureSecurityHeaders
import kz.fearsom.financiallifev2.server.plugins.configureSerialization
import kz.fearsom.financiallifev2.server.plugins.configureStatusPages
import kz.fearsom.financiallifev2.server.repository.GameRepository
import kz.fearsom.financiallifev2.server.repository.UserRepository

fun main() {
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // 1. Connect to PostgreSQL and run DDL (creates missing tables automatically)
    DatabaseFactory.init()

    // 2. Repositories (singletons for server lifetime)
    val userRepository = UserRepository()
    val gameRepository = GameRepository()

    // 3. Ktor plugins
    // SecurityHeaders must be first so every response gets the headers,
    // including error responses from other plugins.
    configureSecurityHeaders()
    configureLogging()
    configureSerialization()
    configureCORS()
    configureStatusPages()
    configureSecurity()           // JWT auth plugin
    configureRouting(userRepository, gameRepository)
}
