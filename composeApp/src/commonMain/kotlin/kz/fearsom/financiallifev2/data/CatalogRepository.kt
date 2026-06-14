package kz.fearsom.financiallifev2.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kz.fearsom.financiallifev2.admin.GameCatalogResponse
import kz.fearsom.financiallifev2.model.CharacterBundle
import kz.fearsom.financiallifev2.model.Era
import kz.fearsom.financiallifev2.model.PredefinedCharacter
import kz.fearsom.financiallifev2.network.GameApiService

/**
 * Overlays the server's admin-managed catalog (`/game/catalog`, active rows only)
 * onto the in-code [SeedData].
 *
 * Why an overlay rather than full server-side data: the admin tables only store
 * catalog *metadata* (id, name, emoji, type, era membership, active flag). The
 * gameplay-critical data — initialStats, profession, scenario graphs, and the
 * *localized* era name/description — lives in code ([SeedData] /
 * `ScenarioGraphFactory`). So the catalog decides what is visible and overrides
 * non-localized display fields; everything else is read from SeedData by id.
 *
 * Fail-safe: until a catalog is fetched (or if the server is unreachable),
 * [catalog] is null and every accessor returns the full SeedData — the game always
 * works offline.
 *
 * i18n: era `name`/`description` are intentionally NOT overridden from the catalog,
 * because in SeedData they come from the localized string table. Overriding them
 * would freeze era text to whatever locale the server seeded with. Character names
 * are plain (non-localized) strings in SeedData, so those ARE overridden — letting
 * an admin rename a character actually take effect in the app.
 */
class CatalogRepository(private val api: GameApiService) {

    private val _catalog = MutableStateFlow<GameCatalogResponse?>(null)
    val catalog: StateFlow<GameCatalogResponse?> = _catalog.asStateFlow()

    /** Last ETag seen, sent as If-None-Match so unchanged catalogs return 304 (no body). */
    private var etag: String? = null

    /**
     * Conditionally fetches the catalog. A `304 Not Modified` (or any failure) leaves
     * the current value untouched — fail-safe to the last value, or to SeedData if we
     * never successfully fetched.
     */
    suspend fun refresh() {
        api.getCatalog(etag).onSuccess { fetch ->
            if (!fetch.notModified && fetch.catalog != null) {
                _catalog.value = fetch.catalog
                etag = fetch.etag
            }
        }
    }

    // ── Overlay accessors — read fresh (localized) SeedData on every call ───────

    fun eras(): List<Era> {
        val rows = _catalog.value?.eras?.associateBy { it.id } ?: return SeedData.eras
        return SeedData.eras
            .filter { it.id in rows }                       // hide deactivated / removed
            .map { era ->
                val row = rows.getValue(era.id)
                era.copy(
                    // name & description stay localized from SeedData (see class KDoc)
                    emoji                 = row.emoji.ifBlank { era.emoji },
                    startYear             = row.startYear,
                    endYear               = row.endYear,
                    isLocked              = row.isLocked,
                    availableCharacterIds = if (row.availableCharacterIds.isNotEmpty())
                        row.availableCharacterIds else era.availableCharacterIds
                )
            }
    }

    fun predefinedCharacters(): List<PredefinedCharacter> {
        val rows = _catalog.value?.characters
            ?.filter { it.type == TYPE_PREDEFINED }
            ?.associateBy { it.id }
            ?: return SeedData.predefinedCharacters
        return SeedData.predefinedCharacters
            .filter { it.id in rows }
            .map { c ->
                val row = rows.getValue(c.id)
                c.copy(
                    name             = row.name.ifBlank { c.name },   // char names are not localized
                    emoji            = row.emoji.ifBlank { c.emoji },
                    compatibleEraIds = if (row.eraIds.isNotEmpty()) row.eraIds else c.compatibleEraIds
                )
            }
    }

    fun characterBundles(): List<CharacterBundle> {
        val rows = _catalog.value?.characters
            ?.filter { it.type == TYPE_BUNDLE }
            ?.associateBy { it.id }
            ?: return SeedData.characterBundles
        return SeedData.characterBundles
            .filter { it.id in rows }
            .map { b ->
                val row = rows.getValue(b.id)
                b.copy(
                    label            = row.name.ifBlank { b.label },
                    emoji            = row.emoji.ifBlank { b.emoji },
                    compatibleEraIds = if (row.eraIds.isNotEmpty()) row.eraIds else b.compatibleEraIds
                )
            }
    }

    private companion object {
        const val TYPE_PREDEFINED = "PREDEFINED"
        const val TYPE_BUNDLE = "BUNDLE"
    }
}
