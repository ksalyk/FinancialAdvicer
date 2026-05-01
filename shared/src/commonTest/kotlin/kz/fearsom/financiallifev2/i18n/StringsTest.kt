package kz.fearsom.financiallifev2.i18n

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Verifies:
 * 1. Correct locale returns translated string
 * 2. Unknown locale falls back to Russian
 * 3. Missing key returns the key itself (no crash)
 * 4. All critical UI keys are present in the Russian map
 * 5. Typed accessors work
 */
class StringsTest {

    private val originalLocale = Strings.currentLocale

    @BeforeTest
    fun setUp() {
        Strings.currentLocale = "ru"
    }

    @AfterTest
    fun tearDown() {
        Strings.currentLocale = originalLocale
    }

    // ── 1. Correct locale lookup ─────────────────────────────────────

    @Test
    fun `ru locale returns Russian string`() {
        Strings.currentLocale = "ru"
        val result = Strings["ui_login_subtitle"]
        assertEquals("Путь к финансовой свободе", result)
    }

    @Test
    fun `kk locale returns Kazakh string`() {
        Strings.currentLocale = "kk"
        val result = Strings["ui_login_subtitle"]
        assertEquals("Қаржылық бостандыққа жол", result)
    }

    @Test
    fun `en locale returns English string`() {
        Strings.currentLocale = "en"
        val result = Strings["ui_login_subtitle"]
        // English value should differ from the Russian one
        assertNotEquals("Путь к финансовой свободе", result)
        assertTrue(result.isNotEmpty())
    }

    // ── 2. Unknown locale falls back to Russian ──────────────────────

    @Test
    fun `unknown locale falls back to Russian`() {
        Strings.currentLocale = "zh"
        val result = Strings["ui_login_subtitle"]
        assertEquals("Путь к финансовой свободе", result)
    }

    @Test
    fun `empty locale falls back to Russian`() {
        Strings.currentLocale = ""
        val result = Strings["ui_login_subtitle"]
        assertEquals("Путь к финансовой свободе", result)
    }

    // ── 3. Missing key returns the key itself ────────────────────────

    @Test
    fun `missing key returns key string as fallback`() {
        Strings.currentLocale = "ru"
        val missingKey = "this_key_does_not_exist_xyz_999"
        val result = Strings[missingKey]
        assertEquals(missingKey, result, "Missing key should return the key itself")
    }

    @Test
    fun `missing key in kk locale returns key string`() {
        Strings.currentLocale = "kk"
        val missingKey = "totally_unknown_key_abc"
        val result = Strings[missingKey]
        assertEquals(missingKey, result)
    }

    // ── 4. Critical UI keys present in Russian map ───────────────────

    @Test
    fun `all required UI keys are present in ru`() {
        Strings.currentLocale = "ru"
        val requiredKeys = listOf(
            "ui_login_subtitle",
            "ui_login_tab_register",
            "ui_login_tab_login",
            "ui_login_field_username",
            "ui_login_field_password",
            "ui_login_btn_login",
            "ui_login_btn_register",
            "ui_main_tagline",
            "ui_main_settings",
            "ui_settings_title",
            "ui_settings_language",
            "ui_settings_language_english",
            "ui_chat_reset_title",
            "ui_stats_title",
            "ending_bankruptcy",
            "ending_paycheck",
            "ending_stability",
            "ending_freedom",
            "ending_wealth",
            "ending_prison",
            "currency_rub",
            "era_modern_kz_2024_name",
            "era_kz_90s_name",
            "era_kz_2015_name",
            "sys_monthly_title",
            "sys_monthly_income",
            "sys_monthly_expenses"
        )
        val missing = requiredKeys.filter { key -> Strings[key] == key }
        assertTrue(missing.isEmpty(), "Missing keys in ru map: $missing")
    }

    // ── 5. Typed accessors ───────────────────────────────────────────

    @Test
    fun `typed accessors return non-empty strings in ru`() {
        Strings.currentLocale = "ru"
        assertTrue(Strings.endingBankruptcy.isNotEmpty())
        assertTrue(Strings.endingWealth.isNotEmpty())
        assertTrue(Strings.endingPrison.isNotEmpty())
        assertTrue(Strings.currencyRub.isNotEmpty())
        assertTrue(Strings.eraModernKz2024Name.isNotEmpty())
        assertTrue(Strings.eraKz90sName.isNotEmpty())
        assertTrue(Strings.eraKz2015Name.isNotEmpty())
        assertTrue(Strings.sysMonthlyTitle.isNotEmpty())
        assertTrue(Strings.uiSettingsTitle.isNotEmpty())
        assertTrue(Strings.uiSettingsLanguageEnglish.isNotEmpty())
    }

    @Test
    fun `typed accessors return different values per locale`() {
        Strings.currentLocale = "ru"
        val ruBankruptcy = Strings.endingBankruptcy

        Strings.currentLocale = "kk"
        val kkBankruptcy = Strings.endingBankruptcy

        assertNotEquals(ruBankruptcy, kkBankruptcy,
            "Kazakh translation should differ from Russian")
    }

    // ── 6. Era event keys present ────────────────────────────────────

    @Test
    fun `era event keys are present in ru`() {
        Strings.currentLocale = "ru"
        val eraKeys = listOf(
            "evt_era_ussr_collapse_msg",
            "evt_era_tenge_introduced_msg",
            "evt_era_mmm_wave_90s_msg",
            "evt_era_devaluation_2015_msg",
            "evt_era_covid_shock_2020_msg"
        )
        val missing = eraKeys.filter { key -> Strings[key] == key }
        assertTrue(missing.isEmpty(), "Missing era event keys in ru map: $missing")
    }

    // ── 7. Locale switching is thread-safe at call site ──────────────

    @Test
    fun `switching locale immediately affects next lookup`() {
        Strings.currentLocale = "ru"
        val ruValue = Strings["ui_login_subtitle"]

        Strings.currentLocale = "kk"
        val kkValue = Strings["ui_login_subtitle"]

        assertNotEquals(ruValue, kkValue)

        Strings.currentLocale = "ru"
        assertEquals(ruValue, Strings["ui_login_subtitle"])
    }

    @Test
    fun `settings language labels switch by locale`() {
        Strings.currentLocale = "ru"
        assertEquals("Настройки", Strings.uiSettingsTitle)

        Strings.currentLocale = "en"
        assertEquals("Settings", Strings.uiSettingsTitle)

        Strings.currentLocale = "kk"
        assertEquals("Баптаулар", Strings.uiSettingsTitle)
    }
}
