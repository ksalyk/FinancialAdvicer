package kz.fearsom.financiallifev2.network

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kz.fearsom.financiallifev2.admin.PublishedStoriesResponse
import kz.fearsom.financiallifev2.admin.StoryDetail
import kz.fearsom.financiallifev2.admin.StoryRow

private const val TAG = "StoriesApiService"

/**
 * Public read-only client for the published-story catalog
 * (/game/catalog/stories). No auth — same as the character/era catalog.
 * Admin CRUD lives server-side under the guarded /admin/stories routes.
 */
class StoriesApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    /** Published stories, metadata only (no graph payload). */
    suspend fun getPublishedStories(): Result<List<StoryRow>> =
        runCatching {
            httpClient.get("$baseUrl/game/catalog/stories").body<PublishedStoriesResponse>().stories
        }.onFailure { e ->
            Napier.w("Failed to fetch published stories: ${e.message}", tag = TAG)
        }

    /** A single published story with its full graph. 404 for unpublished/missing. */
    suspend fun getStory(id: String): Result<StoryDetail> =
        runCatching {
            httpClient.get("$baseUrl/game/catalog/stories/$id").body<StoryDetail>()
        }.onFailure { e ->
            Napier.w("Failed to fetch story '$id': ${e.message}", tag = TAG)
        }
}
