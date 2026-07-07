package kz.fearsom.financiallifev2.data

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.achievements.AchievementCatalog
import kz.fearsom.financiallifev2.achievements.AchievementDefinition
import kz.fearsom.financiallifev2.network.AchievementApiService

private const val TAG = "AchievementCatalogStore"
private const val KEY_CATALOG = "achievements_catalog_v1"

/**
 * Client-side active achievement catalog.
 *
 * Seeded with the compile-time [AchievementCatalog] so the UI and the unlock
 * evaluator are never empty — even offline or before the first fetch. [refresh]
 * overlays the server's active catalog (admin edits, custom entries, and
 * deactivations from the admin panel) and caches it to [SecureStorage] so the
 * last-known catalog survives restarts.
 *
 * Offline-first, same trade-off as [AchievementsRepository]: without [api] or on
 * any network failure the current definitions stay authoritative.
 */
class AchievementCatalogStore(
    private val secureStorage: SecureStorage? = null,
    private val api: AchievementApiService? = null,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _definitions = MutableStateFlow(AchievementCatalog.all)
    /** Active definitions the UI renders and the evaluator checks against. */
    val definitions: StateFlow<List<AchievementDefinition>> = _definitions.asStateFlow()

    /** Snapshot for synchronous callers (unlock evaluator, counters). */
    val current: List<AchievementDefinition> get() = _definitions.value

    fun isValidId(id: String): Boolean = _definitions.value.any { it.id == id }

    init { restore() }

    /** Pull the server's active catalog; keep current definitions on any failure. */
    suspend fun refresh() {
        val service = api ?: return
        service.getCatalog().onSuccess { defs ->
            // An empty payload almost certainly means "not fetched", not "no
            // achievements" — never blank the collection over it.
            if (defs.isNotEmpty()) {
                _definitions.value = defs
                runCatching { secureStorage?.save(KEY_CATALOG, json.encodeToString(defs)) }
                Napier.d("Achievement catalog refreshed: ${defs.size} definitions", tag = TAG)
            }
        }
    }

    private fun restore() {
        val storage = secureStorage ?: return
        runCatching {
            storage.get(KEY_CATALOG)?.takeIf { it.isNotBlank() }?.let {
                val cached = json.decodeFromString<List<AchievementDefinition>>(it)
                if (cached.isNotEmpty()) _definitions.value = cached
            }
        }
    }
}
