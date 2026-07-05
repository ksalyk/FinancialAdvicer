package kz.fearsom.financiallifev2.achievements

import kz.fearsom.financiallifev2.i18n.Strings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Structural integrity of the achievement catalog: unique ids, complete
 * content wiring, and every referenced i18n key resolvable in the Russian
 * source-of-truth map (Strings[key] returns the key itself when missing).
 */
class AchievementCatalogTest {

    @Test
    fun catalog_has_expected_size_and_split() {
        assertEquals(15, AchievementCatalog.all.size)
        assertEquals(5, AchievementCatalog.gameAchievements.size)
        assertEquals(10, AchievementCatalog.scamAchievements.size)
    }

    @Test
    fun ids_are_unique() {
        val ids = AchievementCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Duplicate achievement ids found")
    }

    @Test
    fun isValidId_accepts_known_and_rejects_unknown() {
        assertTrue(AchievementCatalog.isValidId(AchievementCatalog.SCAM_PONZI))
        assertTrue(!AchievementCatalog.isValidId("scam.nonexistent"))
    }

    @Test
    fun game_achievements_have_desc_and_hint_and_no_dossier() {
        AchievementCatalog.gameAchievements.forEach { def ->
            assertTrue(def.descKey != null, "${def.id} missing descKey")
            assertTrue(def.hintKey != null, "${def.id} missing hintKey")
            assertTrue(def.dossier == null, "${def.id} must not carry a dossier")
        }
    }

    @Test
    fun scam_achievements_have_dossier_with_flags_condition() {
        AchievementCatalog.scamAchievements.forEach { def ->
            val dossier = def.dossier
            assertTrue(dossier != null, "${def.id} missing dossier")
            assertTrue(dossier.signKeys.isNotEmpty(), "${def.id} has no recognition signs")
            val condition = def.condition
            assertTrue(condition is AchievementCondition.AnyFlag, "${def.id} must unlock via flags")
            assertTrue(condition.flags.isNotEmpty(), "${def.id} has empty flag set")
        }
    }

    @Test
    fun every_content_key_resolves_in_russian() {
        val previousLocale = Strings.currentLocale
        Strings.currentLocale = "ru"
        try {
            AchievementCatalog.all.forEach { def ->
                val keys = buildList {
                    add(def.titleKey)
                    def.descKey?.let { add(it) }
                    def.hintKey?.let { add(it) }
                    def.dossier?.let { d ->
                        add(d.ageKey)
                        (d.variants + d.origin + d.modern).forEach { entry ->
                            add(entry.yearKey); add(entry.titleKey); add(entry.textKey)
                        }
                        addAll(d.signKeys)
                    }
                }
                keys.forEach { key ->
                    // Strings.get() falls back to returning the raw key on a miss.
                    assertNotEquals(key, Strings[key], "Missing ru translation for '$key' (${def.id})")
                }
            }
        } finally {
            Strings.currentLocale = previousLocale
        }
    }

    @Test
    fun unlockable_scams_map_to_flags_actually_set_by_scenarios() {
        // Regression guard for the flag spelling used across scenario graphs —
        // these are set by existing content and must stay recognized.
        val wiredFlags = mapOf(
            AchievementCatalog.SCAM_PONZI       to "learned.scam.pyramid",
            AchievementCatalog.SCAM_CRYPTO_MOON to "learned.scam.crypto",
            AchievementCatalog.SCAM_GURU        to "learned.scam.infocoach",
            AchievementCatalog.SCAM_FINE_PRINT  to "learned.contract",
            AchievementCatalog.SCAM_DREAM_VISA  to "learned.scam.fake_job",
            AchievementCatalog.SCAM_BRIDGE_SALE to "learned.scam.fake_goods"
        )
        wiredFlags.forEach { (id, flag) ->
            val condition = AchievementCatalog.byId.getValue(id).condition
            assertTrue(
                condition is AchievementCondition.AnyFlag && flag in condition.flags,
                "$id must be unlockable via flag '$flag'"
            )
        }
    }
}
