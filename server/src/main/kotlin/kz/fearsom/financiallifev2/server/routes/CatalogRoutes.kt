package kz.fearsom.financiallifev2.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kz.fearsom.financiallifev2.admin.CharacterRow
import kz.fearsom.financiallifev2.admin.EraRow
import kz.fearsom.financiallifev2.admin.GameCatalogResponse
import kz.fearsom.financiallifev2.server.repository.CharactersRepository
import kz.fearsom.financiallifev2.server.repository.ErasRepository

/**
 * Public game catalog endpoint.
 *
 *   GET /game/catalog → active characters + eras the client overlays onto SeedData.
 *
 * Unauthenticated on purpose: non-sensitive reference data. Only ACTIVE rows are
 * returned, so deactivating a character/era in the admin panel hides it from the app.
 *
 * Supports conditional GET: the response carries an [HttpHeaders.ETag] derived from
 * the active rows' ids + updatedAt. A client that sends a matching
 * [HttpHeaders.IfNoneMatch] gets `304 Not Modified` with no body, so the catalog is
 * only re-downloaded when it actually changes.
 */
fun Route.catalogRoutes(
    charactersRepository: CharactersRepository,
    erasRepository: ErasRepository
) {
    get("/game/catalog") {
        val characters = charactersRepository.listAll(activeOnly = true)
        val eras       = erasRepository.listAll(activeOnly = true)
        val version    = catalogEtag(characters, eras)

        val ifNoneMatch = call.request.header(HttpHeaders.IfNoneMatch)?.trim()?.removeSurrounding("\"")
        if (ifNoneMatch == version) {
            call.respond(HttpStatusCode.NotModified)
            return@get
        }

        call.response.header(HttpHeaders.ETag, "\"$version\"")
        call.respond(GameCatalogResponse(characters, eras))
    }
}

/**
 * Content version for the catalog. `updatedAt` changes on every admin edit (it captures
 * renames/era-membership/reactivation); the id set captures adds and deletes. Sorted so
 * the value is order-independent. Hash is stable within a running server, which is all an
 * ETag needs (the client only holds it in memory for the session).
 */
private fun catalogEtag(characters: List<CharacterRow>, eras: List<EraRow>): String {
    val basis = buildString {
        characters.sortedBy { it.id }.forEach { append(it.id).append(':').append(it.updatedAt).append('|') }
        append('#')
        eras.sortedBy { it.id }.forEach { append(it.id).append(':').append(it.updatedAt).append('|') }
    }
    return basis.hashCode().toString()
}
