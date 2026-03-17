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

// ─── ErbolatScenarioGraph ─────────────────────────────────────────────────────
//
// Character: Ерболат — 38 лет, владелец 2 магазинов одежды, Астана.
// Жена ведёт бухгалтерию. Двое детей в частной школе.
// Бизнес на грани: аренда растёт, маржа падает из-за конкуренции.
// Совместим с эрами: kz_2015, kz_2024.
//
// Нарратив: Удержать бизнес или pivot в e-commerce?
// Высокий доход, но огромный долг — один плохой месяц грозит банкротством.

class ErbolatScenarioGraph(private val eraId: String = "kz_2024") : ScenarioGraph() {

    override val initialPlayerState = PlayerState(
        capital            = 3_500_000L,
        income             = 900_000L,
        expenses           = 750_000L,
        debt               = 4_000_000L,
        debtPaymentMonthly = 111_111L,  // 4M / 36 мес
        investments        = 0L,
        investmentReturnRate = 0.12,
        stress             = 65,
        financialKnowledge = 35,
        riskLevel          = 50,
        month              = 1,
        year               = when (eraId) { "kz_2015" -> 2015; else -> 2024 },
        characterId        = "erbolat",
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
                Ерболат. 38 лет. Предприниматель. 💼

                Два магазина одежды в Астане — «BestStyle» на Хан Шатыре
                и «BestStyle 2» в ТРЦ Мега. Открывал 5 лет назад на кредит.

                Капитал: {capital} тг
                Выручка: ~{income} тг/мес
                Расходы (аренда, зарплаты, товар): {expenses} тг/мес
                Долг банку: {debt} тг (платёж ~111к/мес)

                Жена Гульнара говорит: «Конкуренты из AliExpress убивают розницу.
                Надо что-то менять. Может, закроем второй магазин?»

                Что делаешь?
            """.trimIndent(),
            flavor = "💼",
            options = listOf(
                option(
                    id    = "close_second_store",
                    text  = "Закрыть второй магазин — снизить риски",
                    emoji = "🔒",
                    next  = "ecommerce_pivot",
                    fx    = Effect(incomeDelta = -300_000L, expensesDelta = -250_000L, stressDelta = -10, knowledgeDelta = 3)
                ),
                option(
                    id    = "fight_competition",
                    text  = "Бороться — снизить цены, усилить маркетинг",
                    emoji = "⚡",
                    next  = "franchise_offer",
                    fx    = Effect(capitalDelta = -200_000L, incomeDelta = 50_000L, stressDelta = 15, knowledgeDelta = 5)
                ),
                option(
                    id    = "wait_and_see",
                    text  = "Подождать — рынок сам разберётся",
                    emoji = "⏳",
                    next  = "supplier_scam",
                    fx    = Effect(stressDelta = 10)
                )
            )
        ))

        // ── ФРАНШИЗНЫЙ ОФФЕР ──────────────────────────────────────────────────
        put("franchise_offer", event(
            id = "franchise_offer",
            message = """
                🤝 Представитель российской сети «FashionMart» предлагает франшизу.

                Условия:
                — Паушальный взнос: 2 000 000 тг
                — Роялти: 5% от выручки ежемесячно
                — Бренд, логистика, маркетинг — всё готово
                — Прогнозируемый рост: +40% к обороту за год

                Но придётся взять ещё один кредит на 2М.
                Долг вырастет до 6М тг.

                Капитал: {capital} тг. Текущий долг: {debt} тг.
            """.trimIndent(),
            flavor = "🤝",
            options = listOf(
                option(
                    id    = "take_franchise",
                    text  = "Взять франшизу — масштабирование",
                    emoji = "🚀",
                    next  = "franchise_result",
                    fx    = Effect(capitalDelta = -2_000_000L, debtDelta = 2_000_000L, incomeDelta = 200_000L, expensesDelta = 50_000L, stressDelta = 20, knowledgeDelta = 10)
                ),
                option(
                    id    = "skip_franchise",
                    text  = "Отказаться — слишком много долгов",
                    emoji = "🛡️",
                    next  = "ecommerce_pivot",
                    fx    = Effect(knowledgeDelta = 5)
                ),
                option(
                    id    = "negotiate_franchise",
                    text  = "Торговаться — снизить паушальный взнос",
                    emoji = "🗣️",
                    next  = "ecommerce_pivot",
                    fx    = Effect(capitalDelta = -1_200_000L, debtDelta = 1_200_000L, incomeDelta = 150_000L, stressDelta = 10, knowledgeDelta = 8)
                )
            )
        ))

        // ── РЕЗУЛЬТАТ ФРАНШИЗЫ ────────────────────────────────────────────────
        put("franchise_result", event(
            id = "franchise_result",
            message = """
                📊 6 месяцев с франшизой.

                Оборот вырос, но и расходы тоже.
                Роялти 5% «съедает» маржу.

                Представитель сети предлагает открыть третий магазин.
                Или выкупить мастер-франшизу по Казахстану за 10М тг.

                Долг сейчас: {debt} тг. Капитал: {capital} тг.
            """.trimIndent(),
            flavor = "📊",
            options = listOf(
                option(
                    id    = "third_store",
                    text  = "Открыть третий магазин (кредит 3М)",
                    emoji = "🏬",
                    next  = MONTHLY_TICK,
                    fx    = Effect(debtDelta = 3_000_000L, incomeDelta = 250_000L, expensesDelta = 200_000L, stressDelta = 25)
                ),
                option(
                    id    = "master_franchise",
                    text  = "Выкупить мастер-франшизу (кредит 10М)",
                    emoji = "🌐",
                    next  = MONTHLY_TICK,
                    fx    = Effect(debtDelta = 10_000_000L, incomeDelta = 500_000L, stressDelta = 30, knowledgeDelta = 15)
                ),
                option(
                    id    = "hold_position",
                    text  = "Держать текущее — гасить долг",
                    emoji = "🎯",
                    next  = MONTHLY_TICK,
                    fx    = Effect(stressDelta = -5, knowledgeDelta = 3)
                )
            )
        ))

        // ── ПИВОТ В E-COMMERCE ────────────────────────────────────────────────
        put("ecommerce_pivot", event(
            id = "ecommerce_pivot",
            message = """
                💻 Гульнара нашла решение.

                «Ерболат, смотри — на Kaspi.kz тысячи продавцов одежды.
                Мы можем стать одним из топ-продавцов за полгода.
                Нужно: фотосессия товара (150к), подключение к платформе.»

                Альтернатива: создать свой интернет-магазин (500к + 3 месяца).

                Выручка от офлайна: {income} тг/мес.
            """.trimIndent(),
            flavor = "💻",
            options = listOf(
                option(
                    id    = "kaspi_marketplace",
                    text  = "Выйти на Kaspi.kz — быстрый старт",
                    emoji = "🛍️",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -150_000L, incomeDelta = 150_000L, stressDelta = 5, knowledgeDelta = 10)
                ),
                option(
                    id    = "own_website",
                    text  = "Создать свой сайт — независимость",
                    emoji = "🌐",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -500_000L, incomeDelta = 80_000L, stressDelta = 20, knowledgeDelta = 15)
                ),
                option(
                    id    = "both_channels",
                    text  = "Оба канала сразу — максимальный охват",
                    emoji = "⚡",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -650_000L, incomeDelta = 220_000L, stressDelta = 30, knowledgeDelta = 18)
                )
            )
        ))

        // ── МОШЕННИК-ПОСТАВЩИК ────────────────────────────────────────────────
        put("supplier_scam", event(
            id = "supplier_scam",
            message = """
                ⚠️ Новый поставщик из Китая.

                Предлагает коллекцию сезона на 40% дешевле обычных поставщиков.
                Предоплата 100%: 800 000 тг. Доставка через 4 недели.

                Другой бизнесмен говорит, что работал с ними — «нормальные».
                Но контракт на русском, а юридический адрес в Гонконге.

                Капитал: {capital} тг.
            """.trimIndent(),
            flavor = "⚠️",
            options = listOf(
                option(
                    id    = "pay_supplier",
                    text  = "Оплатить — выгода очевидна",
                    emoji = "💸",
                    next  = "supplier_result",
                    fx    = Effect(capitalDelta = -800_000L, stressDelta = 10)
                ),
                option(
                    id    = "check_supplier",
                    text  = "Проверить через юриста (30к, 2 недели)",
                    emoji = "🔍",
                    next  = "supplier_safe",
                    fx    = Effect(capitalDelta = -30_000L, knowledgeDelta = 8)
                ),
                option(
                    id    = "skip_supplier",
                    text  = "Отказаться — слишком рискованно",
                    emoji = "🛡️",
                    next  = MONTHLY_TICK,
                    fx    = Effect(knowledgeDelta = 5)
                )
            )
        ))

        // ── МОШЕННИК РАСКРЫТ ──────────────────────────────────────────────────
        put("supplier_result", event(
            id = "supplier_result",
            message = """
                💀 Товар не пришёл. Телефон отключён.

                800 000 тг потеряны. Поставщик-мошенник.
                Полиция принимает заявление, но шансы вернуть деньги минимальны.

                Сезонная коллекция сорвана. Конкуренты получили преимущество.

                Капитал: {capital} тг. Долг: {debt} тг.
            """.trimIndent(),
            flavor = "💀",
            options = listOf(
                option(
                    id    = "cut_losses",
                    text  = "Принять потерю — работать дальше",
                    emoji = "💪",
                    next  = "ecommerce_pivot",
                    fx    = Effect(stressDelta = 25, knowledgeDelta = 10)
                ),
                option(
                    id    = "sue_supplier",
                    text  = "Подать в суд (50к на юриста)",
                    emoji = "⚖️",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -50_000L, stressDelta = 15, knowledgeDelta = 5)
                )
            )
        ))

        put("supplier_safe", event(
            id = "supplier_safe",
            message = """
                ✅ Юрист проверил поставщика.

                «Ерболат, это мошенники. Такая же схема была в Бишкеке год назад.
                Они исчезают после предоплаты.»

                Ты сэкономил 800 000 тг.
                Стоит найти надёжного поставщика — юрист знает проверенных.
            """.trimIndent(),
            flavor = "✅",
            options = listOf(
                option(
                    id    = "find_reliable",
                    text  = "Попросить юриста познакомить с поставщиком",
                    emoji = "🤝",
                    next  = MONTHLY_TICK,
                    fx    = Effect(incomeDelta = 50_000L, stressDelta = -10, knowledgeDelta = 5)
                ),
                option(
                    id    = "search_yourself",
                    text  = "Найти самому через проверенные платформы",
                    emoji = "🔍",
                    next  = MONTHLY_TICK,
                    fx    = Effect(knowledgeDelta = 8)
                )
            )
        ))

        // ── КОНЦОВКИ ──────────────────────────────────────────────────────────
        put("ending_empire", event(
            id = "ending_empire",
            message = """
                🏆 БИЗНЕС-ИМПЕРИЯ

                5 магазинов + онлайн. Долг погашен.
                Ерболат открыл обучение для начинающих предпринимателей.

                Дети учатся за рубежом.
                Гульнара — операционный директор группы компаний.

                Капитал: {capital} тг
            """.trimIndent(),
            flavor = "🏆",
            isEnding = true,
            endingType = EndingType.WEALTH,
            options = emptyList()
        ))

        put("ending_pivot_success", event(
            id = "ending_pivot_success",
            message = """
                🌐 DIGITAL FIRST

                Офлайн-магазины закрыты. Онлайн-продажи х5.
                Меньше стресса, больше маржа.

                Долг погашен досрочно.
                Ерболат консультирует других предпринимателей.

                Капитал: {capital} тг
            """.trimIndent(),
            flavor = "💻",
            isEnding = true,
            endingType = EndingType.FINANCIAL_FREEDOM,
            options = emptyList()
        ))

        put("ending_stable_business", event(
            id = "ending_stable_business",
            message = """
                😊 СТАБИЛЬНЫЙ БИЗНЕС

                Один магазин. Онлайн-канал. Долг под контролем.
                Не миллиардер, но спокойно.

                Дети в хорошей школе.
                Гульнара занялась своим проектом.

                Капитал: {capital} тг
            """.trimIndent(),
            flavor = "🏪",
            isEnding = true,
            endingType = EndingType.FINANCIAL_STABILITY,
            options = emptyList()
        ))

        put("ending_bankruptcy", event(
            id = "ending_bankruptcy",
            message = """
                💔 БАНКРОТСТВО

                Долг вырос до критического уровня.
                Банк забрал имущество. Бизнес закрыт.

                Гульнара нашла работу бухгалтером.
                Ерболат начинает консультантом в чужой компании.

                Это не конец — это урок.
            """.trimIndent(),
            flavor = "💀",
            isEnding = true,
            endingType = EndingType.BANKRUPTCY,
            options = emptyList()
        ))

        put("ending_paycheck", event(
            id = "ending_paycheck",
            message = """
                😰 ВЫЖИВАНИЕ

                Бизнес еле держится. Каждый месяц — борьба.
                Долг выплачивается, но накоплений нет.

                Дети переведены в государственную школу.
                Отпуск — воспоминание из прошлого.

                Долг: {debt} тг. Капитал: {capital} тг.
            """.trimIndent(),
            flavor = "😰",
            isEnding = true,
            endingType = EndingType.PAYCHECK_TO_PAYCHECK,
            options = emptyList()
        ))

        // ── HUB ──────────────────────────────────────────────────────────────
        put("normal_life", event(
            id = "normal_life",
            message = """
                Операционный месяц. Зарплаты выплачены, аренда оплачена.

                Выручка: {income} тг
                Расходы бизнеса: {expenses} тг
                Долг: {debt} тг

                Гульнара спрашивает: на что направить свободные деньги?
            """.trimIndent(),
            flavor = "📋",
            poolWeight = 20,
            options = listOf(
                option(
                    id    = "pay_debt",
                    text  = "Досрочно погасить часть долга",
                    emoji = "💳",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -200_000L, debtDelta = -200_000L, stressDelta = -5, knowledgeDelta = 2)
                ),
                option(
                    id    = "marketing",
                    text  = "Вложить в маркетинг (+трафик)",
                    emoji = "📣",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -100_000L, incomeDelta = 80_000L, knowledgeDelta = 5)
                ),
                option(
                    id    = "inventory",
                    text  = "Закупить больший ассортимент",
                    emoji = "📦",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -300_000L, incomeDelta = 120_000L, stressDelta = 5)
                ),
                option(
                    id    = "finance_course",
                    text  = "Пройти курс по управлению бизнесом",
                    emoji = "🎓",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -50_000L, knowledgeDelta = 12, stressDelta = -5)
                )
            )
        ))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONDITIONAL EVENTS
    // ─────────────────────────────────────────────────────────────────────────

    override val conditionalEvents: List<GameEvent> = listOf(

        // Долговой кризис — бизнес-банкротство
        event(
            id = "debt_crisis",
            message = """
                🚨 БИЗНЕС НА ГРАНИ БАНКРОТСТВА

                Долг: {debt} тг. Капитал: {capital} тг.
                Банк требует досрочного погашения части долга или дополнительного залога.

                Налоговая прислала проверку.
                У тебя 2 недели на решение.
            """.trimIndent(),
            flavor = "🚨",
            priority = 10,
            conditions = listOf(cond(DEBT, GT, 3_000_000L), cond(CAPITAL, LTE, 500_000L)),
            options = listOf(
                option(
                    id    = "sell_store",
                    text  = "Продать один магазин — погасить долг",
                    emoji = "🔒",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = 1_500_000L, debtDelta = -1_500_000L, incomeDelta = -300_000L, expensesDelta = -200_000L, stressDelta = 10)
                ),
                option(
                    id    = "bank_restructure",
                    text  = "Реструктурировать долг в банке",
                    emoji = "🏦",
                    next  = MONTHLY_TICK,
                    fx    = Effect(expensesDelta = -50_000L, stressDelta = 20, knowledgeDelta = 8)
                ),
                option(
                    id    = "find_investor",
                    text  = "Найти инвестора — продать 30% доли",
                    emoji = "🤝",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = 2_000_000L, stressDelta = 15, knowledgeDelta = 10)
                )
            )
        ),

        // Предпринимательское выгорание
        event(
            id = "burnout_warning",
            message = """
                😮‍💨 ПРЕДПРИНИМАТЕЛЬСКОЕ ВЫГОРАНИЕ

                Стресс: {stress}/100.
                5 лет без нормального отпуска.
                Гульнара говорит: «Ерболат, ты стал раздражительным. Дети тебя боятся.»

                Врач рекомендует 2 недели отдыха.
                Но сейчас — сезон перед Наурызом.
            """.trimIndent(),
            flavor = "😮‍💨",
            priority = 8,
            conditions = listOf(cond(STRESS, GTE, 80L)),
            options = listOf(
                option(
                    id    = "family_vacation",
                    text  = "Семейный отпуск в Дубай (300к)",
                    emoji = "✈️",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -300_000L, stressDelta = -40)
                ),
                option(
                    id    = "delegate",
                    text  = "Нанять управляющего — делегировать",
                    emoji = "👤",
                    next  = MONTHLY_TICK,
                    fx    = Effect(expensesDelta = 150_000L, stressDelta = -25, knowledgeDelta = 8)
                )
            )
        ),

        // Финансовая грамотность открывает инструменты
        event(
            id = "investment_unlock",
            message = """
                💡 ФИНАНСОВЫЕ ИНСТРУМЕНТЫ ДЛЯ БИЗНЕСА

                Знания: {knowledge}/100.
                Консультант рассказал о факторинге и хеджировании валютных рисков.

                Банк предлагает факторинг — получить деньги от дебиторов сразу
                за комиссию 2%. Это освободит 500к оборотных средств.
            """.trimIndent(),
            flavor = "💡",
            priority = 5,
            conditions = listOf(cond(KNOWLEDGE, GTE, 50L)),
            unique = true,
            options = listOf(
                option(
                    id    = "use_factoring",
                    text  = "Подключить факторинг",
                    emoji = "⚡",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = 500_000L, expensesDelta = 10_000L, knowledgeDelta = 5)
                ),
                option(
                    id    = "skip_factoring",
                    text  = "Не нужно — справляемся сами",
                    emoji = "🛡️",
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
        PoolEntry("normal_life",    baseWeight = 20),
        PoolEntry("supplier_scam",  baseWeight = 12),
        PoolEntry("franchise_offer", baseWeight = 6),
    ) + ScamEventLibrary.poolEntries
}
