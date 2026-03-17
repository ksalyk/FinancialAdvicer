package kz.fearsom.financiallifev2.scenarios.characters

import kz.fearsom.financiallifev2.model.Condition.Stat.Field.CAPITAL
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.DEBT
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.KNOWLEDGE
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.STRESS
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GT
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GTE
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.LTE
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.ScheduledEvent
import kz.fearsom.financiallifev2.scenarios.ScamEventLibrary
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.cond
import kz.fearsom.financiallifev2.scenarios.event
import kz.fearsom.financiallifev2.scenarios.option

// ─── DanaScenarioGraph ────────────────────────────────────────────────────────
//
// Character: Дана — 32 года, учительница математики, Алматы.
// Замужем, один ребёнок 5 лет. Муж — механик на заводе.
// Подрабатывает репетитором. Мечтает о своей квартире.
// Совместима с эрами: kz_2005, kz_2015, kz_2024.
//
// Нарратив: Стабильность vs мечта о квартире.
// Консервативный профиль — каждый тенге на счету.

class DanaScenarioGraph(private val eraId: String = "kz_2024") : ScenarioGraph() {

    override val initialPlayerState = PlayerState(
        capital            = 800_000L,
        income             = 280_000L,
        expenses           = 250_000L,
        debt               = 0L,
        debtPaymentMonthly = 0L,
        investments        = 0L,
        investmentReturnRate = 0.06,
        stress             = 40,
        financialKnowledge = 20,
        riskLevel          = 10,
        month              = 1,
        year               = when (eraId) { "kz_2005" -> 2005; "kz_2015" -> 2015; else -> 2024 },
        characterId        = "dana",
        eraId              = eraId
    )

    // ─────────────────────────────────────────────────────────────────────────
    // STORY EVENTS
    // ─────────────────────────────────────────────────────────────────────────

    override val events: Map<String, GameEvent> = buildMap {

        // ── INTRO ─────────────────────────────────────────────────────────────
        put("intro", event(
            id = "intro",
            message = """
                Привет! Меня зовут Дана. 👩‍🏫

                Я учительница математики, 32 года.
                Муж Сейткали работает механиком — вместе зарабатываем 430к/мес.
                Снимаем квартиру за 130к. Дочери Айгерим 5 лет.

                Накопления: {capital} тг (копила 3 года)
                Мой доход: {income} тг/мес
                Расходы семьи: {expenses} тг/мес

                Коллега рассказала о репетиторской платформе — можно подрабатывать онлайн.
                А соседка говорит: «Дана, бери ипотеку пока молодая!»

                Что важнее сейчас?
            """.trimIndent(),
            flavor = "👩‍🏫",
            options = listOf(
                option(
                    id    = "start_tutoring",
                    text  = "Зарегистрироваться на платформе репетиторов",
                    emoji = "💻",
                    next  = "tutoring_platform",
                    fx    = Effect(knowledgeDelta = 3)
                ),
                option(
                    id    = "explore_mortgage",
                    text  = "Изучить условия ипотеки",
                    emoji = "🏠",
                    next  = "mortgage_decision",
                    fx    = Effect(knowledgeDelta = 5)
                )
            )
        ))

        // ── РЕПЕТИТОРСКАЯ ПЛАТФОРМА ───────────────────────────────────────────
        put("tutoring_platform", event(
            id = "tutoring_platform",
            message = """
                💻 Платформа онлайн-репетиторов.

                Нужно пройти верификацию (5 000 тг за документы)
                и сдать методический тест.

                Потенциальный доход: 50–120к/мес дополнительно.
                Но придётся работать по вечерам — меньше времени с дочкой.
            """.trimIndent(),
            flavor = "💻",
            options = listOf(
                option(
                    id    = "join_platform",
                    text  = "Зарегистрироваться — стоит попробовать",
                    emoji = "✅",
                    next  = "tutoring_growth",
                    fx    = Effect(capitalDelta = -5_000L, incomeDelta = 60_000L, stressDelta = 15, knowledgeDelta = 5)
                ),
                option(
                    id    = "skip_platform",
                    text  = "Не сейчас — дочка важнее",
                    emoji = "👧",
                    next  = "mortgage_decision",
                    fx    = Effect(stressDelta = -5)
                )
            )
        ))

        // ── РОСТ РЕПЕТИТОРСТВА ────────────────────────────────────────────────
        put("tutoring_growth", event(
            id = "tutoring_growth",
            message = """
                📈 Репетиторство набирает обороты!

                Уже 8 учеников. Один родитель предлагает:
                А) Создать своё мини-онлайн-школу (вложить 200к на рекламу)
                Б) Продолжать тихо — без рисков, стабильный доп. доход

                Капитал: {capital} тг. Доход уже {income} тг/мес.
            """.trimIndent(),
            flavor = "📈",
            options = listOf(
                option(
                    id    = "launch_school",
                    text  = "Открыть мини-школу (вложить 200к)",
                    emoji = "🏫",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -200_000L, incomeDelta = 100_000L, stressDelta = 20, knowledgeDelta = 10)
                ),
                option(
                    id    = "stay_quiet",
                    text  = "Остаться в тени — стабильность важнее",
                    emoji = "🛡️",
                    next  = "mortgage_decision",
                    fx    = Effect(stressDelta = -5, knowledgeDelta = 3)
                )
            )
        ))

        // ── ИПОТЕЧНОЕ РЕШЕНИЕ ─────────────────────────────────────────────────
        put("mortgage_decision", event(
            id = "mortgage_decision",
            message = """
                🏠 Ипотека или продолжать снимать?

                Программа «7-20-25»:
                Квартира 2-комнатная: 15 000 000 тг.
                Первый взнос 20% = 3 000 000 тг.
                Платёж: ~95 000 тг/мес на 25 лет.

                У вас с Сейткали: {capital} тг накоплений.
                Если взять ипотеку — накопления почти обнулятся.
            """.trimIndent(),
            flavor = "🏠",
            options = listOf(
                option(
                    id    = "take_mortgage",
                    text  = "Взять ипотеку — своё жильё важно",
                    emoji = "🔑",
                    next  = "child_education",
                    fx    = Effect(capitalDelta = -3_000_000L, debtDelta = 12_000_000L, expensesDelta = 95_000L, stressDelta = 25, knowledgeDelta = 8)
                ),
                option(
                    id    = "save_more",
                    text  = "Копить ещё 2 года — взнос 30%",
                    emoji = "⏳",
                    next  = "child_education",
                    fx    = Effect(knowledgeDelta = 5, stressDelta = -5)
                ),
                option(
                    id    = "rent_forever",
                    text  = "Снимать и инвестировать разницу",
                    emoji = "📊",
                    next  = "child_education",
                    fx    = Effect(investmentsDelta = 50_000L, capitalDelta = -50_000L, knowledgeDelta = 8)
                )
            )
        ))

        // ── ОБРАЗОВАНИЕ РЕБЁНКА ───────────────────────────────────────────────
        put("child_education", event(
            id = "child_education",
            message = """
                👧 Айгерим идёт в школу через год.

                Варианты:
                А) Государственная школа — бесплатно, но качество разное.
                Б) Частная школа — 80к/мес, но гарантированный уровень.
                В) Нанять репетитора с 1-го класса — 30к/мес.

                Семейный бюджет: {income} тг/мес доход, {expenses} тг/мес расходы.
            """.trimIndent(),
            flavor = "📚",
            options = listOf(
                option(
                    id    = "private_school",
                    text  = "Частная школа — лучшее для дочки",
                    emoji = "🎓",
                    next  = MONTHLY_TICK,
                    fx    = Effect(expensesDelta = 80_000L, stressDelta = 10, knowledgeDelta = 3)
                ),
                option(
                    id    = "state_tutor",
                    text  = "Государственная + репетитор",
                    emoji = "✏️",
                    next  = MONTHLY_TICK,
                    fx    = Effect(expensesDelta = 30_000L, stressDelta = 5, knowledgeDelta = 2)
                ),
                option(
                    id    = "state_school",
                    text  = "Государственная школа — разберёмся",
                    emoji = "🏫",
                    next  = MONTHLY_TICK,
                    fx    = Effect(stressDelta = -3)
                )
            )
        ))

        // ── МУЖ ПОТЕРЯЛ РАБОТУ ────────────────────────────────────────────────
        put("husband_layoff", event(
            id = "husband_layoff",
            message = """
                😰 ЧП. Завод закрылся.

                Сейткали потерял работу — его цех ликвидировали.
                Его зарплата была 150к/мес. Ищет новую работу уже месяц.

                Пособие по безработице: 42к/мес (3 месяца).
                Твой доход теперь единственный источник.

                Капитал: {capital} тг. Расходы: {expenses} тг/мес.
            """.trimIndent(),
            flavor = "😰",
            poolWeight = 10,
            tags = setOf("family", "crisis"),
            options = listOf(
                option(
                    id    = "cut_expenses",
                    text  = "Срочно урезать расходы на 30%",
                    emoji = "✂️",
                    next  = MONTHLY_TICK,
                    fx    = Effect(expensesDelta = -75_000L, stressDelta = 20, knowledgeDelta = 5)
                ),
                option(
                    id    = "use_savings",
                    text  = "Жить на накопления пока ищет работу",
                    emoji = "🐷",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -150_000L, stressDelta = 10)
                ),
                option(
                    id    = "more_tutoring",
                    text  = "Увеличить нагрузку — больше учеников",
                    emoji = "💪",
                    next  = MONTHLY_TICK,
                    fx    = Effect(incomeDelta = 50_000L, stressDelta = 25)
                )
            )
        ))

        // ── КОНЦОВКИ ──────────────────────────────────────────────────────────
        put("ending_own_school", event(
            id = "ending_own_school",
            message = """
                🏫 СВОЯ ОНЛАЙН-ШКОЛА

                Школа Даны — топ-3 в рейтинге казахстанских онлайн-школ.
                500 учеников. Команда из 8 человек.

                Квартира куплена. Дочь в лучшей школе города.
                Сейткали стал операционным директором.

                Капитал: {capital} тг
            """.trimIndent(),
            flavor = "🏆",
            isEnding = true,
            endingType = EndingType.WEALTH,
            options = emptyList()
        ))

        put("ending_stable_family", event(
            id = "ending_stable_family",
            message = """
                🏠 СТАБИЛЬНАЯ СЕМЬЯ

                Своя квартира. Дочь растёт здоровой.
                Репетиторство приносит +60к/мес.

                Не миллионер, но всё своё.
                Семья в безопасности.

                Капитал: {capital} тг
            """.trimIndent(),
            flavor = "❤️",
            isEnding = true,
            endingType = EndingType.FINANCIAL_STABILITY,
            options = emptyList()
        ))

        put("ending_freedom", event(
            id = "ending_freedom",
            message = """
                🎯 ФИНАНСОВАЯ НЕЗАВИСИМОСТЬ

                Инвестиции + пассивный доход от онлайн-школы
                покрывают все расходы семьи.

                Дана работает потому что любит, не потому что нужно.

                Капитал: {capital} тг
            """.trimIndent(),
            flavor = "🌅",
            isEnding = true,
            endingType = EndingType.FINANCIAL_FREEDOM,
            options = emptyList()
        ))

        put("ending_debt_trap", event(
            id = "ending_debt_trap",
            message = """
                💔 ДОЛГОВАЯ ЛОВУШКА

                Ипотека + кредит на ремонт + расходы на ребёнка.
                Каждый месяц — выплата долгов.

                Работаешь на банк, а не на семью.
                До пенсии ещё 30 лет таких платежей.

                Долг: {debt} тг
            """.trimIndent(),
            flavor = "😞",
            isEnding = true,
            endingType = EndingType.PAYCHECK_TO_PAYCHECK,
            options = emptyList()
        ))

        put("ending_bankruptcy", event(
            id = "ending_bankruptcy",
            message = """
                💀 БАНКРОТСТВО

                Ипотека просрочена. Квартиру забирает банк.
                Муж без работы, накопления на нуле.

                Придётся снова снимать и начинать с нуля.
                Но знания остались — следующий раз будет иначе.
            """.trimIndent(),
            flavor = "💀",
            isEnding = true,
            endingType = EndingType.BANKRUPTCY,
            options = emptyList()
        ))

        // ── HUB ──────────────────────────────────────────────────────────────
        put("normal_life", event(
            id = "normal_life",
            message = """
                Обычный месяц. Школа, ученики, дочка.

                Капитал: {capital} тг
                Доход: {income} тг/мес

                Семейный совет — куда направить лишние деньги?
            """.trimIndent(),
            flavor = "🍵",
            poolWeight = 20,
            options = listOf(
                option(
                    id    = "family_savings",
                    text  = "Пополнить семейную подушку",
                    emoji = "🐷",
                    next  = MONTHLY_TICK,
                    fx    = Effect(stressDelta = -3, knowledgeDelta = 1)
                ),
                option(
                    id    = "read_finance",
                    text  = "Читать книги по личным финансам",
                    emoji = "📖",
                    next  = MONTHLY_TICK,
                    fx    = Effect(knowledgeDelta = 6, stressDelta = -2)
                ),
                option(
                    id    = "invest_conservative",
                    text  = "Вложить в консервативный фонд",
                    emoji = "🏦",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -30_000L, investmentsDelta = 30_000L, knowledgeDelta = 4)
                ),
                option(
                    id    = "family_trip",
                    text  = "Поехать с семьёй на природу",
                    emoji = "🌿",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -20_000L, stressDelta = -15)
                )
            )
        ))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONDITIONAL EVENTS
    // ─────────────────────────────────────────────────────────────────────────

    override val conditionalEvents: List<GameEvent> = listOf(

        // Ипотечный кризис — если долг большой и денег мало
        event(
            id = "debt_crisis",
            message = """
                🚨 ИПОТЕКА ПОД УГРОЗОЙ

                Долг: {debt} тг. Капитал: {capital} тг.
                Банк прислал уведомление о просроченном платеже.

                Если не заплатить в течение 30 дней —
                банк начнёт процедуру изъятия квартиры.
            """.trimIndent(),
            flavor = "🚨",
            priority = 10,
            conditions = listOf(cond(DEBT, GT, 0L), cond(CAPITAL, LTE, 100_000L)),
            options = listOf(
                option(
                    id    = "sell_investments",
                    text  = "Продать инвестиции для погашения",
                    emoji = "📉",
                    next  = MONTHLY_TICK,
                    fx    = Effect(debtDelta = -200_000L, investmentsDelta = -200_000L, stressDelta = 20)
                ),
                option(
                    id    = "ask_parents",
                    text  = "Попросить помощи у родителей",
                    emoji = "👵",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = 150_000L, stressDelta = 25)
                )
            )
        ),

        // Выгорание учителя
        event(
            id = "burnout_warning",
            message = """
                😮‍💨 ПЕДАГОГИЧЕСКОЕ ВЫГОРАНИЕ

                Стресс: {stress}/100.
                30 учеников в классе + 8 репетиторских + домашние дела.

                Директор предлагает взять отпуск за свой счёт.
                Или уменьшить репетиторскую нагрузку.
            """.trimIndent(),
            flavor = "😮‍💨",
            priority = 8,
            conditions = listOf(cond(STRESS, GTE, 70L)),
            options = listOf(
                option(
                    id    = "take_break",
                    text  = "Взять отпуск — здоровье дороже",
                    emoji = "🌿",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -100_000L, incomeDelta = -60_000L, stressDelta = -35)
                ),
                option(
                    id    = "reduce_tutoring",
                    text  = "Уменьшить репетиторство наполовину",
                    emoji = "📉",
                    next  = MONTHLY_TICK,
                    fx    = Effect(incomeDelta = -30_000L, stressDelta = -20)
                )
            )
        ),

        // Разблокировка консервативных инвестиций
        event(
            id = "investment_unlock",
            message = """
                💡 ОСОЗНАННЫЙ ИНВЕСТОР

                Знания: {knowledge}/100.
                Ты поняла разницу между депозитом, облигациями и акциями.

                Банк предлагает государственные облигации КазМунайГаза:
                7% годовых, минимальный риск.
            """.trimIndent(),
            flavor = "💡",
            priority = 5,
            conditions = listOf(cond(KNOWLEDGE, GTE, 35L)),
            unique = true,
            options = listOf(
                option(
                    id    = "buy_bonds",
                    text  = "Купить облигации на 200к",
                    emoji = "📊",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -200_000L, investmentsDelta = 214_000L, knowledgeDelta = 5)
                ),
                option(
                    id    = "skip_bonds",
                    text  = "Нет — лучше держать в депозите",
                    emoji = "🏦",
                    next  = MONTHLY_TICK,
                    fx    = Effect()
                )
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // EVENT POOL
    // ─────────────────────────────────────────────────────────────────────────

    override val eventPool: List<PoolEntry> = listOf(
        PoolEntry("normal_life",     baseWeight = 25),
        PoolEntry("husband_layoff",  baseWeight = 8),
    ) + ScamEventLibrary.poolEntries
}
