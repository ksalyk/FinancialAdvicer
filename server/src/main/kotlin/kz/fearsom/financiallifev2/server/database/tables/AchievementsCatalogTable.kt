package kz.fearsom.financiallifev2.server.database.tables

import org.jetbrains.exposed.v1.core.Table

/**
 * DB-backed achievement definitions.
 *
 * Seeded from the code catalog (`:shared` AchievementCatalog) on startup with
 * insert-only-when-missing semantics (admin edits survive restarts). The full
 * `AchievementDefinition` (including its sealed condition and optional inline
 * texts) is stored as JSON in [definitionJson]; frequently-filtered fields are
 * denormalized into columns.
 *
 * [catalogSource] — "SEEDED" (from code catalog; delete-protected, deactivate instead)
 *            or "CUSTOM" (created in the admin panel). DB column "source".
 */
object AchievementsCatalogTable : Table("achievements_catalog") {
    val id             = varchar("id", 64)
    val kind           = varchar("kind", 10)
    val isActive       = bool("is_active")
    val sortOrder      = integer("sort_order")
    // Named catalogSource (not source) — Exposed's Table/FieldSet already declares `source`.
    val catalogSource  = varchar("source", 10)
    val definitionJson = text("definition_json")
    val createdAt      = long("created_at")
    val updatedAt      = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
