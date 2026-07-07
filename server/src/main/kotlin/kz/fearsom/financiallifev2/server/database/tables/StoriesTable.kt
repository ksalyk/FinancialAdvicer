package kz.fearsom.financiallifev2.server.database.tables

import org.jetbrains.exposed.v1.core.Table

/**
 * DB-backed scenario/story storage.
 *
 * The narrative graph is stored as JSON ([graphJson], a serialized
 * `ScenarioGraphDto` from :shared) — the same wire shape the admin scenario
 * viewer already consumes. Code-authored graphs (`ScenarioGraphFactory`)
 * remain built-in and are NOT mirrored here; they can be cloned into a row
 * as an editable draft via the admin panel.
 *
 * [status]  — StoryStatus name: DRAFT / PENDING_REVIEW / PUBLISHED / REJECTED / ARCHIVED
 * [storySource] — StorySource name: BUILT_IN (clone) / ADMIN / USER (DB column "source")
 * [authorId] — users.id for USER-submitted stories; no FK on purpose so
 *              deleting a user never destroys published content.
 * [reviewNote] — moderator feedback shown to the author on rejection.
 */
object StoriesTable : Table("stories") {
    val id          = varchar("id", 64)
    val title       = varchar("title", 200)
    val description = text("description")
    val characterId = varchar("character_id", 50)
    val eraId       = varchar("era_id", 50)
    val status      = varchar("status", 20)
    // Named storySource (not source) — Exposed's Table/FieldSet already declares `source`.
    val storySource = varchar("source", 20)
    val authorId    = varchar("author_id", 36).nullable()
    val reviewNote  = text("review_note").nullable()
    val graphJson   = text("graph_json")
    /** Denormalized from the graph on every write — keeps list queries JSON-free. */
    val eventCount  = integer("event_count")
    val endingCount = integer("ending_count")
    val createdAt   = long("created_at")
    val updatedAt   = long("updated_at")
    val publishedAt = long("published_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
