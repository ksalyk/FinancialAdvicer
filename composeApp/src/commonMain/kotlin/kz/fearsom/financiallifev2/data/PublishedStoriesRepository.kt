package kz.fearsom.financiallifev2.data

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.admin.StoryDetail
import kz.fearsom.financiallifev2.admin.StoryRow
import kz.fearsom.financiallifev2.network.StoriesApiService
import kz.fearsom.financiallifev2.scenarios.DtoScenarioGraph
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph

private const val TAG = "PublishedStoriesRepository"
private const val KEY_STORIES = "published_stories_v1"

/**
 * Client cache of published DB stories.
 *
 * Fetches the published catalog + each story's graph, caches to [SecureStorage],
 * and exposes them to gameplay via [graphFor]: the GameEngine resolver overlays a
 * published story onto its (characterId, eraId), so publishing a story in the
 * admin panel replaces the built-in graph for that combo on the next fetch.
 *
 * Offline-first: without [api] or on failure the last cached stories stay usable.
 * Graph resolution is synchronous (the engine's startGame is), so story graphs are
 * prefetched into an in-memory map keyed by "characterId|eraId".
 */
class PublishedStoriesRepository(
    private val secureStorage: SecureStorage? = null,
    private val api: StoriesApiService? = null,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _published = MutableStateFlow<List<StoryRow>>(emptyList())
    /** Published story metadata (for a selection UI). */
    val published: StateFlow<List<StoryRow>> = _published.asStateFlow()

    // "characterId|eraId" → adapted graph. Last published story wins per combo.
    private val graphs = mutableMapOf<String, ScenarioGraph>()

    init { restore() }

    /** Override graph for a character+era if a published story exists, else null. */
    fun graphFor(characterId: String, eraId: String): ScenarioGraph? = graphs[key(characterId, eraId)]

    /** Pull the published catalog + graphs; keep the cache on any failure. */
    suspend fun refresh() {
        val service = api ?: return
        val rows = service.getPublishedStories().getOrNull() ?: return

        val details = rows.mapNotNull { row -> service.getStory(row.id).getOrNull() }
        if (details.isEmpty() && rows.isNotEmpty()) return   // network hiccup — keep cache

        apply(details)
        persist(details)
        Napier.d("Published stories refreshed: ${details.size}", tag = TAG)
    }

    private fun apply(details: List<StoryDetail>) {
        graphs.clear()
        details.forEach { graphs[key(it.row.characterId, it.row.eraId)] = DtoScenarioGraph(it.graph) }
        _published.value = details.map { it.row }
    }

    private fun restore() {
        val storage = secureStorage ?: return
        runCatching {
            storage.get(KEY_STORIES)?.takeIf { it.isNotBlank() }?.let {
                apply(json.decodeFromString<List<StoryDetail>>(it))
            }
        }
    }

    private fun persist(details: List<StoryDetail>) {
        runCatching { secureStorage?.save(KEY_STORIES, json.encodeToString(details)) }
    }

    private fun key(characterId: String, eraId: String) = "$characterId|$eraId"
}
