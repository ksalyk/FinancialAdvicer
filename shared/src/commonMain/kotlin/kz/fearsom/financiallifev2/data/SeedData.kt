package kz.fearsom.financiallifev2.data

import kz.fearsom.financiallifev2.i18n.Strings

import kz.fearsom.financiallifev2.model.CharacterBundle
import kz.fearsom.financiallifev2.model.CharacterStats
import kz.fearsom.financiallifev2.model.Difficulty
import kz.fearsom.financiallifev2.model.Era
import kz.fearsom.financiallifev2.model.GameEnding
import kz.fearsom.financiallifev2.model.PredefinedCharacter
import kz.fearsom.financiallifev2.model.UnlockCondition

/**
 * Hardcoded seed data for eras, predefined characters, and character bundles.
 * In production these would come from a remote API + local SQLite cache.
 */
object SeedData {

    // ── Eras ──────────────────────────────────────────────────────────────────
    val eras: List<Era> get() = listOf(
        Era(
            id = "kz_90s",
            name = Strings["seed_era_kz_90s_name"],
            description = Strings["seed_era_kz_90s_desc"],
            startYear = 1991,
            endYear = 2000,
            baseInflationRate = 40.0,
            baseSalaryMin = 5_000L,
            baseSalaryMax = 50_000L,
            availableCharacterIds = listOf("aidar_90s"),
            keyEconomicEvents = listOf(Strings["seed_era_kz_90s_event_1"], Strings["seed_era_kz_90s_event_2"], Strings["seed_era_kz_90s_event_3"], Strings["seed_era_kz_90s_event_4"]),
            emoji = "📼",
            isLocked = false
        ),
        Era(
            id = "kz_2005",
            name = Strings["seed_era_kz_2005_name"],
            description = Strings["seed_era_kz_2005_desc"],
            startYear = 2005,
            endYear = 2010,
            baseInflationRate = 8.5,
            baseSalaryMin = 80_000L,
            baseSalaryMax = 250_000L,
            availableCharacterIds = listOf("aidar", "dana"),
            keyEconomicEvents = listOf(Strings["seed_era_kz_2005_event_1"], Strings["seed_era_kz_2005_event_2"]),
            emoji = "🏗️",
            isLocked = false
        ),
        Era(
            id = "kz_2015",
            name = Strings["seed_era_kz_2015_name"],
            description = Strings["seed_era_kz_2015_desc"],
            startYear = 2015,
            endYear = 2020,
            baseInflationRate = 14.5,
            baseSalaryMin = 150_000L,
            baseSalaryMax = 500_000L,
            availableCharacterIds = listOf("aidar", "dana", "erbolat"),
            keyEconomicEvents = listOf(Strings["seed_era_kz_2015_event_1"], Strings["seed_era_kz_2015_event_2"], Strings["seed_era_kz_2015_event_3"]),
            emoji = "📱",
            isLocked = false
        ),
        Era(
            id = "kz_2024",
            name = Strings["seed_era_kz_2024_name"],
            description = Strings["seed_era_kz_2024_desc"],
            startYear = 2024,
            endYear = 2030,
            baseInflationRate = 9.8,
            baseSalaryMin = 300_000L,
            baseSalaryMax = 1_500_000L,
            availableCharacterIds = listOf("aidar", "asan", "dana", "erbolat"),
            keyEconomicEvents = listOf(Strings["seed_era_kz_2024_event_1"], Strings["seed_era_kz_2024_event_2"]),
            emoji = "🚀",
            isLocked = true
        )
    )

    // ── Predefined Characters ─────────────────────────────────────────────────

    val predefinedCharacters: List<PredefinedCharacter> get() = listOf(
        PredefinedCharacter(
            id = "aidar",
            name = Strings["seed_char_aidar_name"],
            age = 24,
            profession = Strings["seed_char_aidar_profession"],
            emoji = "🧑‍💻",
            backstory = Strings["seed_char_aidar_backstory"],
            personality = Strings["seed_char_aidar_personality"],
            compatibleEraIds = listOf("kz_2005", "kz_2015", "kz_2024"),
            initialStats = CharacterStats(
                capital = 200_000L,
                income = 350_000L,
                debt = 0L,
                monthlyExpenses = 230_000L,
                stress = 25,
                financialKnowledge = 15,
                riskLevel = 20
            ),
            uniqueEventIds = listOf("startup_pitch", "senior_promotion"),
            difficulty = Difficulty.EASY,
            isUnlocked = true
        ), PredefinedCharacter(
            id = "asan",
            name = Strings["seed_char_asan_name"],
            age = 28,
            profession = Strings["seed_char_asan_profession"],
            emoji = "📱",
            backstory = Strings["seed_char_asan_backstory"],
            personality = Strings["seed_char_asan_personality"],
            compatibleEraIds = listOf("kz_2024"),
            initialStats = CharacterStats(
                capital = 200_000L,
                income = 450_000L,
                debt = 120_000L,
                monthlyExpenses = 180_000L,
                stress = 25,
                financialKnowledge = 10,
                riskLevel = 15
            ),
            uniqueEventIds = listOf("job_offer", "mortgage_offer", "senior_offer"),
            difficulty = Difficulty.MEDIUM,
            isUnlocked = true
        ), PredefinedCharacter(
            id = "dana",
            name = Strings["seed_char_dana_name"],
            age = 32,
            profession = Strings["seed_char_dana_profession"],
            emoji = "👩‍🏫",
            backstory = Strings["seed_char_dana_backstory"],
            personality = Strings["seed_char_dana_personality"],
            compatibleEraIds = listOf("kz_2005", "kz_2015", "kz_2024"),
            initialStats = CharacterStats(
                capital = 800_000L,
                income = 280_000L,
                debt = 0L,
                monthlyExpenses = 250_000L,
                stress = 40,
                financialKnowledge = 20,
                riskLevel = 10
            ),
            uniqueEventIds = listOf("tutoring_platform", "child_education", "husband_layoff"),
            difficulty = Difficulty.EASY,
            isUnlocked = true
        ), PredefinedCharacter(
            id = "aidar_90s",
            name = Strings["seed_char_aidar_90s_name"],
            age = 22,
            profession = Strings["seed_char_aidar_90s_profession"],
            emoji = "🧑‍🎓",
            backstory = Strings["seed_char_aidar_90s_backstory"],
            personality = Strings["seed_char_aidar_90s_personality"],
            compatibleEraIds = listOf("kz_90s"),
            initialStats = CharacterStats(
                capital = 25_000_000L,
                income = 7_500_000L,
                debt = 0L,
                monthlyExpenses = 6_000_000L,
                stress = 60,
                financialKnowledge = 15,
                riskLevel = 40
            ),
            uniqueEventIds = listOf("supplier_scam", "ecommerce_pivot"),
            difficulty = Difficulty.MEDIUM,
            isUnlocked = true
        ),
        PredefinedCharacter(
            id = "erbolat",
            name = Strings["seed_char_erbolat_name"],
            age = 38,
            profession = Strings["seed_char_erbolat_profession"],
            emoji = "💼",
            backstory = Strings["seed_char_erbolat_backstory"],
            personality = Strings["seed_char_erbolat_personality"],
            compatibleEraIds = listOf("kz_2015", "kz_2024"),
            initialStats = CharacterStats(
                capital = 3_500_000L,
                income = 900_000L,
                debt = 4_000_000L,
                monthlyExpenses = 750_000L,
                stress = 65,
                financialKnowledge = 35,
                riskLevel = 50
            ),
            uniqueEventIds = listOf("franchise_offer", "supplier_scam", "ecommerce_pivot"),
            difficulty = Difficulty.HARD,
            isUnlocked = true
        )

    )

    // ── Character Bundles ─────────────────────────────────────────────────────

    val characterBundles: List<CharacterBundle> get() = listOf(
        CharacterBundle(
            id = "bundle_entrepreneur",
            label = Strings["seed_bundle_entrepreneur_label"],
            description = Strings["seed_bundle_entrepreneur_desc"],
            emoji = "🎰",
            compatibleEraIds = listOf("kz_2005", "kz_2015", "kz_2024"),
            profession = Strings["seed_bundle_entrepreneur_profession"],
            stats = CharacterStats(
                capital = 500_000L,
                income = 800_000L,
                debt = 2_000_000L,
                monthlyExpenses = 600_000L,
                stress = 70,
                financialKnowledge = 25,
                riskLevel = 80
            ),
            traits = listOf(Strings["seed_bundle_entrepreneur_trait_1"], Strings["seed_bundle_entrepreneur_trait_2"]),
            difficulty = Difficulty.HARD
        ), CharacterBundle(
            id = "bundle_student",
            label = Strings["seed_bundle_student_label"],
            description = Strings["seed_bundle_student_desc"],
            emoji = "🎓",
            compatibleEraIds = listOf("kz_2005", "kz_2015", "kz_2024"),
            profession = Strings["seed_bundle_student_profession"],
            stats = CharacterStats(
                capital = 50_000L,
                income = 150_000L,
                debt = 500_000L,
                monthlyExpenses = 120_000L,
                stress = 45,
                financialKnowledge = 30,
                riskLevel = 20
            ),
            traits = listOf(Strings["seed_bundle_student_trait_1"], "tech-savvy"),
            difficulty = Difficulty.MEDIUM
        ), CharacterBundle(
            id = "bundle_family",
            label = Strings["seed_bundle_family_label"],
            description = Strings["seed_bundle_family_desc"],
            emoji = "👨‍👩‍👧",
            compatibleEraIds = listOf("kz_2005", "kz_2015", "kz_2024"),
            profession = Strings["seed_bundle_family_profession"],
            stats = CharacterStats(
                capital = 1_200_000L,
                income = 450_000L,
                debt = 5_000_000L,
                monthlyExpenses = 380_000L,
                stress = 55,
                financialKnowledge = 15,
                riskLevel = 10
            ),
            traits = listOf(Strings["seed_bundle_family_trait_1"], Strings["seed_bundle_family_trait_2"]),
            difficulty = Difficulty.HARD
        ), CharacterBundle(
            id = "bundle_heir",
            label = Strings["seed_bundle_heir_label"],
            description = Strings["seed_bundle_heir_desc"],
            emoji = "💎",
            compatibleEraIds = listOf("kz_2015", "kz_2024"),
            profession = Strings["seed_bundle_heir_profession"],
            stats = CharacterStats(
                capital = 15_000_000L,
                income = 200_000L,
                debt = 0L,
                monthlyExpenses = 300_000L,
                stress = 10,
                financialKnowledge = 5,
                riskLevel = 5
            ),
            traits = listOf(Strings["seed_bundle_heir_trait_1"], Strings["seed_bundle_entrepreneur_trait_2"]),
            difficulty = Difficulty.NIGHTMARE
        ), CharacterBundle(
            id = "bundle_burnout",
            label = Strings["seed_bundle_burnout_label"],
            description = Strings["seed_bundle_burnout_desc"],
            emoji = "🏃",
            compatibleEraIds = listOf("kz_2015", "kz_2024"),
            profession = Strings["seed_bundle_burnout_profession"],
            stats = CharacterStats(
                capital = 3_000_000L,
                income = 0L,
                debt = 800_000L,
                monthlyExpenses = 250_000L,
                stress = 80,
                financialKnowledge = 65,
                riskLevel = 40
            ),
            traits = listOf(Strings["seed_bundle_student_trait_1"], Strings["seed_bundle_family_trait_2"]),
            difficulty = Difficulty.MEDIUM
        ), CharacterBundle(
            id = "bundle_late_start",
            label = Strings["seed_bundle_late_start_label"],
            description = Strings["seed_bundle_late_start_desc"],
            emoji = "👵",
            compatibleEraIds = listOf("kz_2005", "kz_2015", "kz_2024"),
            profession = Strings["seed_bundle_late_start_profession"],
            stats = CharacterStats(
                capital = 200_000L,
                income = 300_000L,
                debt = 0L,
                monthlyExpenses = 180_000L,
                stress = 60,
                financialKnowledge = 40,
                riskLevel = 5
            ),
            traits = listOf(Strings["seed_bundle_family_trait_2"], Strings["seed_bundle_student_trait_1"]),
            difficulty = Difficulty.HARD
        ), CharacterBundle(
            id = "bundle_gray",
            label = Strings["seed_bundle_gray_label"],
            description = Strings["seed_bundle_gray_desc"],
            emoji = "🤫",
            compatibleEraIds = listOf("kz_2005", "kz_2015", "kz_2024"),
            profession = Strings["seed_bundle_gray_profession"],
            stats = CharacterStats(
                capital = 2_000_000L,
                income = 600_000L,
                debt = 0L,
                monthlyExpenses = 200_000L,
                stress = 50,
                financialKnowledge = 55,
                riskLevel = 90
            ),
            traits = listOf(Strings["seed_bundle_gray_trait_1"], Strings["seed_bundle_entrepreneur_trait_2"]),
            difficulty = Difficulty.NIGHTMARE
        ), CharacterBundle(
            id = "bundle_crypto",
            label = Strings["seed_bundle_crypto_label"],
            description = Strings["seed_bundle_crypto_desc"],
            emoji = "🎮",
            compatibleEraIds = listOf("kz_2015", "kz_2024"),
            profession = Strings["seed_bundle_crypto_profession"],
            stats = CharacterStats(
                capital = 5_000_000L,
                income = 300_000L,
                debt = 200_000L,
                monthlyExpenses = 250_000L,
                stress = 35,
                financialKnowledge = 20,
                riskLevel = 95
            ),
            traits = listOf(Strings["seed_bundle_entrepreneur_trait_1"], "tech-savvy"),
            difficulty = Difficulty.HARD,
            isLocked = true,
            unlockCondition = UnlockCondition.FinishGameWith(GameEnding.BANKRUPTCY)
        )
    )
}
