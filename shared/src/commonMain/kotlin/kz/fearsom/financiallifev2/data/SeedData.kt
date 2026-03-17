package kz.fearsom.financiallifev2.data

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
    val eras: List<Era> = mutableListOf(
        Era(
            id = "kz_90s",
            name = "Казахстан 90-е",
            description = "Распад СССР. Гиперинфляция, ваучеры, пирамиды МММ и введение тенге. Выживи в хаосе.",
            startYear = 1991,
            endYear = 2000,
            baseInflationRate = 40.0,
            baseSalaryMin = 5_000L,
            baseSalaryMax = 50_000L,
            availableCharacterIds = listOf("aidar_90s"),
            keyEconomicEvents = listOf("Распад СССР", "Введение тенге 1993", "МММ 1994", "Кризис 1998"),
            emoji = "📼",
            isLocked = false
        ),
        Era(
            id = "kz_2005",
            name = "Казахстан 2005–2010",
            description = "Нефтяной бум. Экономика растёт, зарплаты увеличиваются, но инфляция высокая.",
            startYear = 2005,
            endYear = 2010,
            baseInflationRate = 8.5,
            baseSalaryMin = 80_000L,
            baseSalaryMax = 250_000L,
            availableCharacterIds = listOf("aidar", "dana"),
            keyEconomicEvents = listOf("Нефтяной бум", "Девальвация 2009"),
            emoji = "🏗️",
            isLocked = false
        ),
        Era(
            id = "kz_2015",
            name = "Казахстан 2015–2020",
            description = "Цифровизация и нестабильность тенге. Курс доллара скачет, начинается онлайн-бизнес.",
            startYear = 2015,
            endYear = 2020,
            baseInflationRate = 14.5,
            baseSalaryMin = 150_000L,
            baseSalaryMax = 500_000L,
            availableCharacterIds = listOf("aidar", "dana", "erbolat"),
            keyEconomicEvents = listOf("Девальвация 2015", "Пандемия 2020", "Цифровизация"),
            emoji = "📱",
            isLocked = false
        ),
        Era(
            id = "kz_2024",
            name = "Казахстан 2024–2030",
            description = "Новая реальность. Разблокируйте, пройдя любую эпоху.",
            startYear = 2024,
            endYear = 2030,
            baseInflationRate = 9.8,
            baseSalaryMin = 300_000L,
            baseSalaryMax = 1_500_000L,
            availableCharacterIds = listOf("aidar", "dana", "erbolat"),
            keyEconomicEvents = listOf("Технологический бум", "Финансовые реформы"),
            emoji = "🚀",
            isLocked = true
        )
    )

    // ── Predefined Characters ─────────────────────────────────────────────────

    val predefinedCharacters: List<PredefinedCharacter> = listOf(
        PredefinedCharacter(
            id = "aidar",
            name = "Айдар",
            age = 24,
            profession = "Junior разработчик",
            emoji = "🧑‍💻",
            backstory = "Закончил университет в Алматы, устроился junior разработчиком. Живёт на съёмной квартире с другом. Мама просит помогать младшему брату. Мечтает о своём стартапе, но пока боится рисковать — на карте всего 200 тысяч.",
            personality = "Осторожный, амбициозный, немного наивный в финансах",
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
            id = "dana",
            name = "Дана",
            age = 32,
            profession = "Учительница",
            emoji = "👩‍🏫",
            backstory = "Работает учителем математики в школе. Замужем, один ребёнок 5 лет. Муж работает механиком на заводе. Стабильный, но небольшой доход. Подрабатывает репетитором. Мечтает купить собственную квартиру, пока снимают.",
            personality = "Стабильная, ответственная, не любит риск",
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
            name = "Айдар (90-е)",
            age = 22,
            profession = "Студент",
            emoji = "🧑‍🎓",
            backstory = "Начало 90-х. Только что поступил в университет. В стране кризис, но есть мечты.",
            personality = "Оптимистичный, настойчивый",
            compatibleEraIds = listOf("kz_90s"),
            initialStats = CharacterStats(
                capital = 50_000L,
                income = 15_000L,
                debt = 0L,
                monthlyExpenses = 12_000L,
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
            name = "Ерболат",
            age = 38,
            profession = "Предприниматель",
            emoji = "💼",
            backstory = "Владеет небольшой сетью из 2 магазинов одежды в Астане. Бизнес на грани — аренда растёт, маржа падает из-за конкуренции. Жена ведёт бухгалтерию. Двое детей в частной школе.",
            personality = "Рискованный, предприимчивый, под давлением",
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

    val characterBundles: List<CharacterBundle> = listOf(
        CharacterBundle(
            id = "bundle_entrepreneur",
            label = "Рискованный предприниматель",
            description = "Высокий доход, но огромный долг. Один плохой месяц — банкрот.",
            emoji = "🎰",
            compatibleEraIds = listOf("kz_2005", "kz_2015", "kz_2024"),
            profession = "Владелец кафе",
            stats = CharacterStats(
                capital = 500_000L,
                income = 800_000L,
                debt = 2_000_000L,
                monthlyExpenses = 600_000L,
                stress = 70,
                financialKnowledge = 25,
                riskLevel = 80
            ),
            traits = listOf("импульсивный", "харизматичный"),
            difficulty = Difficulty.HARD
        ), CharacterBundle(
            id = "bundle_student",
            label = "Вечный студент",
            description = "Нестабильный доход — каждый месяц рандом ±50%. Учёба не гарантирует успех.",
            emoji = "🎓",
            compatibleEraIds = listOf("kz_2005", "kz_2015", "kz_2024"),
            profession = "Фрилансер-дизайнер",
            stats = CharacterStats(
                capital = 50_000L,
                income = 150_000L,
                debt = 500_000L,
                monthlyExpenses = 120_000L,
                stress = 45,
                financialKnowledge = 30,
                riskLevel = 20
            ),
            traits = listOf("тревожный", "tech-savvy"),
            difficulty = Difficulty.MEDIUM
        ), CharacterBundle(
            id = "bundle_family",
            label = "Молодая семья",
            description = "Ипотека программа 7-20-25. Высокие расходы — ребёнок генерирует случайные события.",
            emoji = "👨‍👩‍👧",
            compatibleEraIds = listOf("kz_2005", "kz_2015", "kz_2024"),
            profession = "Менеджер",
            stats = CharacterStats(
                capital = 1_200_000L,
                income = 450_000L,
                debt = 5_000_000L,
                monthlyExpenses = 380_000L,
                stress = 55,
                financialKnowledge = 15,
                riskLevel = 10
            ),
            traits = listOf("семейный", "консервативный"),
            difficulty = Difficulty.HARD
        ), CharacterBundle(
            id = "bundle_heir",
            label = "Наследник",
            description = "15 миллионов, но нулевые знания → главная мишень для мошенников.",
            emoji = "💎",
            compatibleEraIds = listOf("kz_2015", "kz_2024"),
            profession = "Рантье",
            stats = CharacterStats(
                capital = 15_000_000L,
                income = 200_000L,
                debt = 0L,
                monthlyExpenses = 300_000L,
                stress = 10,
                financialKnowledge = 5,
                riskLevel = 5
            ),
            traits = listOf("расточительный", "харизматичный"),
            difficulty = Difficulty.NIGHTMARE
        ), CharacterBundle(
            id = "bundle_burnout",
            label = "Корпоративный беглец",
            description = "Бывший банкир. Высокие знания, но выгорание и нет дохода. Начни с нуля.",
            emoji = "🏃",
            compatibleEraIds = listOf("kz_2015", "kz_2024"),
            profession = "Бывший банкир",
            stats = CharacterStats(
                capital = 3_000_000L,
                income = 0L,
                debt = 800_000L,
                monthlyExpenses = 250_000L,
                stress = 80,
                financialKnowledge = 65,
                riskLevel = 40
            ),
            traits = listOf("тревожный", "консервативный"),
            difficulty = Difficulty.MEDIUM
        ), CharacterBundle(
            id = "bundle_late_start",
            label = "Поздний старт",
            description = "50 лет. Мало времени до пенсии — нужно быстро копить на старость.",
            emoji = "👵",
            compatibleEraIds = listOf("kz_2005", "kz_2015", "kz_2024"),
            profession = "Бухгалтер",
            stats = CharacterStats(
                capital = 200_000L,
                income = 300_000L,
                debt = 0L,
                monthlyExpenses = 180_000L,
                stress = 60,
                financialKnowledge = 40,
                riskLevel = 5
            ),
            traits = listOf("консервативный", "тревожный"),
            difficulty = Difficulty.HARD
        ), CharacterBundle(
            id = "bundle_gray",
            label = "Серая схема",
            description = "Весь доход серый → уникальные события с налоговой и полицией. Риск тюрьмы.",
            emoji = "🤫",
            compatibleEraIds = listOf("kz_2005", "kz_2015", "kz_2024"),
            profession = "«Консультант»",
            stats = CharacterStats(
                capital = 2_000_000L,
                income = 600_000L,
                debt = 0L,
                monthlyExpenses = 200_000L,
                stress = 50,
                financialKnowledge = 55,
                riskLevel = 90
            ),
            traits = listOf("серый", "харизматичный"),
            difficulty = Difficulty.NIGHTMARE
        ), CharacterBundle(
            id = "bundle_crypto",
            label = "Криптобро",
            description = "5 млн в крипте. Капитал волатильный — каждый квартал ±60%.",
            emoji = "🎮",
            compatibleEraIds = listOf("kz_2015", "kz_2024"),
            profession = "Блогер/трейдер",
            stats = CharacterStats(
                capital = 5_000_000L,
                income = 300_000L,
                debt = 200_000L,
                monthlyExpenses = 250_000L,
                stress = 35,
                financialKnowledge = 20,
                riskLevel = 95
            ),
            traits = listOf("импульсивный", "tech-savvy"),
            difficulty = Difficulty.HARD,
            isLocked = true,
            unlockCondition = UnlockCondition.FinishGameWith(GameEnding.BANKRUPTCY)
        )
    )
}
