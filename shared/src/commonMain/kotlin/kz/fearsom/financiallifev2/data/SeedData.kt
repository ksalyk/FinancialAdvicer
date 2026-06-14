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
            keyEconomicEvents = listOf(
                Strings["seed_era_kz_90s_event_1"],
                Strings["seed_era_kz_90s_event_2"],
                Strings["seed_era_kz_90s_event_3"],
                Strings["seed_era_kz_90s_event_4"]
            ),
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
            availableCharacterIds = listOf("aidar", "daniyar", "serik"),
            keyEconomicEvents = listOf(
                Strings["seed_era_kz_2005_event_1"],
                Strings["seed_era_kz_2005_event_2"]
            ),
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
            availableCharacterIds = listOf("dana"),
            keyEconomicEvents = listOf(
                Strings["seed_era_kz_2015_event_1"],
                Strings["seed_era_kz_2015_event_2"],
                Strings["seed_era_kz_2015_event_3"]
            ),
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
            availableCharacterIds = listOf("asan"),
            keyEconomicEvents = listOf(
                Strings["seed_era_kz_2024_event_1"],
                Strings["seed_era_kz_2024_event_2"]
            ),
            emoji = "🚀",
            isLocked = false
        )
    )

    // ── Predefined Characters ─────────────────────────────────────────────────

    val predefinedCharacters: List<PredefinedCharacter> get() = listOf(
        PredefinedCharacter(
            id = "aidar",
            name = "Руслан Темирбаев",
            age = 27,
            profession = "Кредитный специалист в банке",
            emoji = "🏦",
            backstory = "Астана, 2005. Руслан продает ипотеку в годы строительного бума, встречается с Айгуль и помогает брату с университетом. Его арка про кредиты, свадьбу, долевое строительство и кризис доверия к красивым договорам.",
            personality = "Аккуратный, но амбициозный. Умеет считать платежи, пока статус и семейное давление не начинают шуметь громче калькулятора.",
            compatibleEraIds = listOf("kz_2005"),
            initialStats = CharacterStats(
                capital = 450_000L,
                income = 170_000L,
                debt = 0L,
                monthlyExpenses = 105_000L,
                stress = 34,
                financialKnowledge = 24,
                riskLevel = 26
            ),
            uniqueEventIds = listOf("ruslan_presale_flat", "ruslan_wedding_credit", "era_mortgage_freeze_2008"),
            difficulty = Difficulty.MEDIUM,
            isUnlocked = true
        ), PredefinedCharacter(
            id = "daniyar",
            name = "Данияр Ахметов",
            age = 31,
            profession = "Автомеханик в гараже на окраине Шымкента",
            emoji = "🔧",
            backstory = "Шымкент, весна 2005. У Данияра золотые руки и привычка верить людям чуть больше, чем стоит. Старый друг зовёт в схему с «серым импортом» машин, постоянные клиенты просят договор, из аула давят с просьбами, а друг из Астаны привозит «квартиру мечты» на котловане. Его арка — про кредитный бум 2005-2010, формализацию бизнеса, семейное давление и соблазн быстрых денег.",
            personality = "Надёжный и практичный, но привычка доверять «своим» и оставлять всё «на словах» регулярно толкает его к рискованным решениям.",
            compatibleEraIds = listOf("kz_2005"),
            initialStats = CharacterStats(
                capital = 400_000L,
                income = 200_000L,
                debt = 0L,
                monthlyExpenses = 130_000L,
                stress = 38,
                financialKnowledge = 18,
                riskLevel = 30
            ),
            uniqueEventIds = listOf("daniyar_bolat_import", "daniyar_garage_formalize", "daniyar_presale_flat", "era_mortgage_freeze_2008"),
            difficulty = Difficulty.MEDIUM,
            isUnlocked = true
        ), PredefinedCharacter(
            id = "serik",
            name = "Серік Жұмабеков",
            age = 35,
            profession = "Учитель физики в школе",
            emoji = "👨‍🏫",
            backstory = "Караганда, 2005. Серік — обычный школьный учитель на маленькой зарплате, а вокруг ревёт нефтяной бум: соседи перепродают квартиры, берут машины в кредит, всюду краны. По вечерам он репетиторствует. Его арка — про то, как использовать хорошие времена: превратить подработку в учебный центр, купить актив, а не «воздух» на котловане, и не утонуть в долларовой ипотеке и понтах к кризису 2008-2009.",
            personality = "Спокойный и методичный, привык считать. Но скромная зарплата на фоне всеобщего богатства давит на самолюбие и подталкивает догонять соседей в кредит.",
            compatibleEraIds = listOf("kz_2005"),
            initialStats = CharacterStats(
                capital = 150_000L,
                income = 50_000L,
                debt = 0L,
                monthlyExpenses = 32_000L,
                stress = 44,
                financialKnowledge = 16,
                riskLevel = 18
            ),
            uniqueEventIds = listOf("serik_tutoring", "serik_presale_flat", "serik_buy_home", "era_mortgage_freeze_2008"),
            difficulty = Difficulty.HARD,
            isUnlocked = true
        ), PredefinedCharacter(
            id = "asan",
            name = "Амир Нурланов",
            age = 26,
            profession = "Менеджер маркетплейса",
            emoji = "🛒",
            backstory = "Алматы, 2024. Амир ведет кабинет продавца, закрывает небольшой долг, помогает родителям и пытается не спутать инвестиции с Telegram-шумом. Его арка про fake escrow, dating scam, крипту, side-product и выгорание.",
            personality = "Быстро учится, любит технологии и иногда принимает скорость за качество решения.",
            compatibleEraIds = listOf("kz_2024"),
            initialStats = CharacterStats(
                capital = 260_000L,
                income = 520_000L,
                debt = 180_000L,
                monthlyExpenses = 255_000L,
                stress = 38,
                financialKnowledge = 24,
                riskLevel = 22
            ),
            uniqueEventIds = listOf("amir_marketplace", "amir_dating_app", "amir_side_product"),
            difficulty = Difficulty.MEDIUM,
            isUnlocked = true
        ), PredefinedCharacter(
            id = "dana",
            name = "Жанар Сейтова",
            age = 34,
            profession = "Учитель английского",
            emoji = "👩‍🏫",
            backstory = "Караганда, 2015. Жанар держит семью, запускает маленькие курсы английского и проходит через девальвацию, школьные расходы, WhatsApp-давление и форекс-воронки.",
            personality = "Теплая и рациональная, но семейная ответственность часто заставляет ее брать на себя больше, чем выдерживает бюджет.",
            compatibleEraIds = listOf("kz_2015"),
            initialStats = CharacterStats(
                capital = 620_000L,
                income = 260_000L,
                debt = 0L,
                monthlyExpenses = 190_000L,
                stress = 42,
                financialKnowledge = 28,
                riskLevel = 18
            ),
            uniqueEventIds = listOf("zhanar_course_launch", "zhanar_forex_relative", "zhanar_school_choice"),
            difficulty = Difficulty.MEDIUM,
            isUnlocked = true
        ), PredefinedCharacter(
            id = "aidar_90s",
            name = "Марат Сатыбалдин",
            age = 23,
            profession = "Мастер по ремонту техники на барахолке",
            emoji = "📼",
            backstory = "Алматы, 1993. Марат чинит магнитофоны, помогает родителям пережить задержки зарплаты и пытается построить маленький торговый бизнес до и после введения тенге.",
            personality = "Недоверчивый к красивым обещаниям, но семья и шанс быстро подняться постоянно проверяют его осторожность.",
            compatibleEraIds = listOf("kz_90s"),
            initialStats = CharacterStats(
                capital = 25_000_000L,
                income = 7_500_000L,
                debt = 0L,
                monthlyExpenses = 6_000_000L,
                stress = 55,
                financialKnowledge = 18,
                riskLevel = 35
            ),
            uniqueEventIds = listOf("marat_family_pyramid", "marat_supplier_prepay", "marat_wedding"),
            difficulty = Difficulty.MEDIUM,
            isUnlocked = true
        )

    )

    // ── Character Bundles ─────────────────────────────────────────────────────
    // Bundles are removed — they lack their own story arcs and reuse predefined
    // character narratives with mismatched text. Only predefined characters are playable.

    val characterBundles: List<CharacterBundle> get() = emptyList()

    @Suppress("unused")
    private val _removedBundles: List<CharacterBundle> get() = listOf(
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
