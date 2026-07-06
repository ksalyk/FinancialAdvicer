package kz.fearsom.financiallifev2.server.database.migrations

/**
 * Canonical registry of every migration version.
 *
 * Add a new entry here whenever a new migration file is added under `versions/`.
 * The ordinal/declaration order does not determine execution order — [MigrationRunner]
 * sorts by [version] numerically. Keep entries in ascending order by convention.
 */
enum class MigrationVersion(val version: Int) {
    V001_INITIAL_SCHEMA(1),
    V002_ADD_STATISTICS_INDEX(2),
    V003_HASH_REFRESH_TOKENS(3),
    V004_ADD_ACHIEVEMENTS(4),
    V005_ADD_STORIES_AND_ACHIEVEMENT_CATALOG(5);

    companion object {
        /** Returns the highest registered version number. */
        fun currentVersion(): Int = entries.maxOf { it.version }
    }
}
