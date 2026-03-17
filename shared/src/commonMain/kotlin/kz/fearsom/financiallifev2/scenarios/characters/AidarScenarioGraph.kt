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

// ─── AidarScenarioGraph ───────────────────────────────────────────────────────
//
// Character: Айдар — 24-летний junior разработчик, Алматы.
// Совместим с эрами: kz_2005, kz_2015, kz_2024.
// Уникальные события: startup_pitch, senior_promotion.
//
// Нарратив: Амбиции vs стабильность. Карьерный рост или собственный стартап?
// Мама просит помогать брату. На карте 200 тысяч — маленькая, но реальная основа.

class AidarScenarioGraph(private val eraId: String = "kz_2024") : ScenarioGraph() {

    override val initialPlayerState = PlayerState(
        capital            = 200_000L,
        income             = 350_000L,
        expenses           = 230_000L,
        debt               = 0L,
        debtPaymentMonthly = 0L,
        investments        = 0L,
        investmentReturnRate = 0.08,
        stress             = 25,
        financialKnowledge = 15,
        riskLevel          = 20,
        month              = 1,
        year               = when (eraId) { "kz_2005" -> 2005; "kz_2015" -> 2015; else -> 2024 },
        characterId        = "aidar",
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
                Привет! Я Айдар, 24 года. 👋

                Только что устроился junior разработчиком в Алматы.
                Снимаю квартиру с другом — 90к аренды пополам.

                Накопления: {capital} тг
                Зарплата: {income} тг/мес
                Расходы: {expenses} тг/мес

                Мама звонит — просит помочь брату-студенту.
                А тут ещё старый однокурсник зовёт вместе запустить стартап.

                С чего начнёшь?
            """.trimIndent(),
            flavor = "🧑‍💻",
            options = listOf(
                option(
                    id    = "help_brother",
                    text  = "Помочь брату (10к/мес семье)",
                    emoji = "👨‍👩‍👧",
                    next  = "startup_pitch",
                    fx    = Effect(expensesDelta = 10_000L, stressDelta = -5, knowledgeDelta = 2)
                ),
                option(
                    id    = "save_first",
                    text  = "Сначала стабилизировать финансы",
                    emoji = "💰",
                    next  = "startup_pitch",
                    fx    = Effect(knowledgeDelta = 3)
                )
            )
        ))

        // ── СТАРТАП-ПИТЧ ──────────────────────────────────────────────────────
        put("startup_pitch", event(
            id = "startup_pitch",
            message = """
                💡 Однокурсник Данияр пришёл с идеей.

                «Айдар, давай сделаем маркетплейс для казахстанских ремесленников.
                Нужно 150 тысяч и 3 месяца работы по вечерам.
                Если взлетит — делим 50/50.»

                У тебя {capital} тг. Данияр надёжный, но стартап — это всегда риск.
            """.trimIndent(),
            flavor = "💡",
            options = listOf(
                option(
                    id    = "join_startup",
                    text  = "Вложить 150к и войти в стартап",
                    emoji = "🚀",
                    next  = "startup_3months",
                    fx    = Effect(capitalDelta = -150_000L, stressDelta = 15, knowledgeDelta = 5)
                ),
                option(
                    id    = "skip_startup",
                    text  = "Отказаться — слишком рискованно",
                    emoji = "🛡️",
                    next  = "senior_promotion",
                    fx    = Effect(knowledgeDelta = 2)
                ),
                option(
                    id    = "partial_startup",
                    text  = "Помочь кодом, но без денег",
                    emoji = "💻",
                    next  = "senior_promotion",
                    fx    = Effect(stressDelta = 10, knowledgeDelta = 8)
                )
            )
        ))

        // ── 3 МЕСЯЦА СТАРТАПА ─────────────────────────────────────────────────
        put("startup_3months", event(
            id = "startup_3months",
            message = """
                ⏰ 3 месяца разработки по ночам.

                MVP готов. Первые 50 продавцов зарегистрировались.
                Но инвестора пока нет, а деньги заканчиваются.

                Данияр предлагает питчить акселераторы.
                Или выйти из проекта — он вернёт 80к из вложенных 150к.
            """.trimIndent(),
            flavor = "⏰",
            options = listOf(
                option(
                    id    = "pitch_investors",
                    text  = "Питчить инвесторов (идти до конца)",
                    emoji = "🎤",
                    next  = "startup_result",
                    fx    = Effect(stressDelta = 20, knowledgeDelta = 10)
                ),
                option(
                    id    = "exit_startup",
                    text  = "Выйти — забрать 80к обратно",
                    emoji = "🚪",
                    next  = "senior_promotion",
                    fx    = Effect(capitalDelta = 80_000L, stressDelta = -10)
                )
            )
        ))

        // ── РЕЗУЛЬТАТ СТАРТАПА ────────────────────────────────────────────────
        put("startup_result", event(
            id = "startup_result",
            message = """
                🎯 Питч в акселераторе!

                Жюри оценило идею. Два исхода возможны:
                — Получить грант 500к тг и продолжить
                — Получить отказ и потерять вложения

                Твои знания: {knowledge}/100.
                Чем выше знания, тем убедительнее питч.
            """.trimIndent(),
            flavor = "🎯",
            options = listOf(
                option(
                    id    = "strong_pitch",
                    text  = "Уверенно питчить (риск, но шанс)",
                    emoji = "💪",
                    next  = "startup_success",
                    fx    = Effect(knowledgeDelta = 5)
                ),
                option(
                    id    = "safe_exit",
                    text  = "Передумать — лучше стабильность",
                    emoji = "🔄",
                    next  = "senior_promotion",
                    fx    = Effect(stressDelta = -15)
                )
            )
        ))

        // ── СТАРТАП ВЫСТРЕЛИЛ ─────────────────────────────────────────────────
        put("startup_success", event(
            id = "startup_success",
            message = """
                🎉 Грант получен!

                Акселератор выдал 500 000 тг и место в коворкинге.
                Маркетплейс растёт — уже 300 продавцов.

                Но теперь нужно выбирать: оставить основную работу или уйти в стартап полностью?
                Доход в стартапе пока 0, но потенциал огромный.
            """.trimIndent(),
            flavor = "🎉",
            options = listOf(
                option(
                    id    = "quit_job",
                    text  = "Уйти с работы — стартап важнее",
                    emoji = "🦅",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = 500_000L, incomeDelta = -350_000L, stressDelta = 25, knowledgeDelta = 15)
                ),
                option(
                    id    = "keep_job",
                    text  = "Совмещать — работа + стартап",
                    emoji = "⚖️",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = 500_000L, stressDelta = 30, knowledgeDelta = 10)
                )
            )
        ))

        // ── ПОВЫШЕНИЕ ─────────────────────────────────────────────────────────
        put("senior_promotion", event(
            id = "senior_promotion",
            message = """
                📈 Полгода прошло. Тебя заметили.

                Тимлид предлагает два варианта:
                А) Повышение до Middle — +80к к зарплате, больше ответственности.
                Б) Перейти в другую компанию — они предлагают Senior сразу, +150к, но нужно переехать в Астану.

                Текущая зарплата: {income} тг/мес.
            """.trimIndent(),
            flavor = "📈",
            options = listOf(
                option(
                    id    = "middle_promo",
                    text  = "Стать Middle — остаться в Алматы",
                    emoji = "🧗",
                    next  = MONTHLY_TICK,
                    fx    = Effect(incomeDelta = 80_000L, stressDelta = 5, knowledgeDelta = 8)
                ),
                option(
                    id    = "senior_astana",
                    text  = "Переехать в Астану — Senior сразу",
                    emoji = "🏙️",
                    next  = MONTHLY_TICK,
                    fx    = Effect(incomeDelta = 150_000L, expensesDelta = 40_000L, stressDelta = 20, knowledgeDelta = 12)
                ),
                option(
                    id    = "stay_same",
                    text  = "Подождать — искать лучшее предложение",
                    emoji = "🔍",
                    next  = MONTHLY_TICK,
                    fx    = Effect(knowledgeDelta = 3)
                )
            )
        ))

        // ── СЕМЬЯ ПРОСИТ ПОМОЩИ ───────────────────────────────────────────────
        put("family_pressure", event(
            id = "family_pressure",
            message = """
                📱 Мама звонит снова.

                «Брат провалил сессию — нужно 50к на репетиторов.
                Папа попал в больницу — операция стоит 200к.
                Ты единственный, кто зарабатывает.»

                Капитал: {capital} тг. Что делаешь?
            """.trimIndent(),
            flavor = "👨‍👩‍👧",
            poolWeight = 8,
            tags = setOf("family"),
            options = listOf(
                option(
                    id    = "help_fully",
                    text  = "Помочь всем — 250к семье",
                    emoji = "❤️",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -250_000L, stressDelta = 10)
                ),
                option(
                    id    = "help_partial",
                    text  = "Помочь только с операцией (200к)",
                    emoji = "🏥",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -200_000L, stressDelta = 5)
                ),
                option(
                    id    = "help_minimum",
                    text  = "Дать 50к — больше нет возможности",
                    emoji = "💸",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -50_000L, stressDelta = 20)
                )
            )
        ))

        // ── ИПОТЕКА ────────────────────────────────────────────────────────────
        put("mortgage_offer", event(
            id = "mortgage_offer",
            message = """
                🏠 Банк одобрил ипотеку по программе «7-20-25».

                Квартира-студия в Алматы: 12 000 000 тг.
                Первоначальный взнос: 20% = 2 400 000 тг.
                Ежемесячный платёж: ~80 000 тг/мес на 20 лет.

                Накопления: {capital} тг.
                Это твой шанс перестать снимать жильё.
            """.trimIndent(),
            flavor = "🏠",
            poolWeight = 6,
            tags = setOf("mortgage"),
            options = listOf(
                option(
                    id    = "take_mortgage",
                    text  = "Взять ипотеку (нужен первый взнос)",
                    emoji = "🔑",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -2_400_000L, debtDelta = 9_600_000L, expensesDelta = 80_000L, stressDelta = 15, knowledgeDelta = 5)
                ),
                option(
                    id    = "skip_mortgage",
                    text  = "Продолжать снимать — копить дальше",
                    emoji = "⏳",
                    next  = MONTHLY_TICK,
                    fx    = Effect(knowledgeDelta = 3)
                )
            )
        ))

        // ── КОНЦОВКИ ──────────────────────────────────────────────────────────
        put("ending_startup_king", event(
            id = "ending_startup_king",
            message = """
                🚀 СТАРТАП ВЫСТРЕЛИЛ

                Маркетплейс вырос до 5 000 продавцов.
                Привлекли раунд A — $500k от казахстанского фонда.

                Айдар — CEO собственной компании в 27 лет.
                Мама гордится. Брат работает в команде.

                Капитал: {capital} тг
            """.trimIndent(),
            flavor = "🚀",
            isEnding = true,
            endingType = EndingType.WEALTH,
            options = emptyList()
        ))

        put("ending_senior_dev", event(
            id = "ending_senior_dev",
            message = """
                👨‍💻 SENIOR DEVELOPER

                Ты вырос до Senior, потом до Lead.
                Зарплата: {income} тг/мес. Ипотека закрыта.

                Может, не миллиардер — но стабильность и
                уважение коллег дорогого стоят.

                Капитал: {capital} тг
            """.trimIndent(),
            flavor = "💼",
            isEnding = true,
            endingType = EndingType.FINANCIAL_STABILITY,
            options = emptyList()
        ))

        put("ending_freedom", event(
            id = "ending_freedom",
            message = """
                🎯 ФИНАНСОВАЯ СВОБОДА

                Инвестиции + пассивный доход перекрыли расходы.
                Айдар работает по выбору, а не по необходимости.

                Капитал: {capital} тг
                Доход от инвестиций: {investments} тг
            """.trimIndent(),
            flavor = "🏖️",
            isEnding = true,
            endingType = EndingType.FINANCIAL_FREEDOM,
            options = emptyList()
        ))

        put("ending_broke", event(
            id = "ending_broke",
            message = """
                💔 ФИНАНСОВЫЙ КРАХ

                Стартап не взлетел, долги накопились,
                помощь семье опустошила накопления.

                Придётся начинать с нуля.
                Зато теперь ты знаешь, чего не делать.
            """.trimIndent(),
            flavor = "💀",
            isEnding = true,
            endingType = EndingType.BANKRUPTCY,
            options = emptyList()
        ))

        put("ending_paycheck", event(
            id = "ending_paycheck",
            message = """
                😐 ОТ ЗАРПЛАТЫ ДО ЗАРПЛАТЫ

                Работа стабильная, но мечты о стартапе
                так и остались мечтами.

                Каждый месяц в ноль. Но и долгов нет.
                Жизнь продолжается.
            """.trimIndent(),
            flavor = "😐",
            isEnding = true,
            endingType = EndingType.PAYCHECK_TO_PAYCHECK,
            options = emptyList()
        ))

        // ── HUB ──────────────────────────────────────────────────────────────
        put("normal_life", event(
            id = "normal_life",
            message = """
                Месяц прошёл в обычном режиме.
                Код, стенд-апы, обеды в офисе.

                Капитал: {capital} тг
                Зарплата: {income} тг/мес

                Что в приоритете этого месяца?
            """.trimIndent(),
            flavor = "☕",
            poolWeight = 20,
            options = listOf(
                option(
                    id    = "invest_etf",
                    text  = "Инвестировать в ETF (5% капитала)",
                    emoji = "📊",
                    next  = MONTHLY_TICK,
                    fx    = Effect(investmentsDelta = 10_000L, capitalDelta = -10_000L, knowledgeDelta = 3)
                ),
                option(
                    id    = "online_course",
                    text  = "Пройти онлайн-курс по финансам",
                    emoji = "📚",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -15_000L, knowledgeDelta = 8, stressDelta = -3)
                ),
                option(
                    id    = "save_cash",
                    text  = "Просто откладывать в подушку",
                    emoji = "🐷",
                    next  = MONTHLY_TICK,
                    fx    = Effect(stressDelta = -2)
                ),
                option(
                    id    = "network_it",
                    text  = "Пойти на IT-митап (нетворкинг)",
                    emoji = "🤝",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -5_000L, knowledgeDelta = 5, stressDelta = -5)
                )
            )
        ))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONDITIONAL EVENTS
    // ─────────────────────────────────────────────────────────────────────────

    override val conditionalEvents: List<GameEvent> = listOf(

        // Долговой кризис — если взял ипотеку и не справляется
        event(
            id = "debt_crisis",
            message = """
                🚨 ДОЛГОВОЙ КРИЗИС

                Ипотека + расходы > доход.
                Банк звонит по поводу просроченного платежа.

                Долг: {debt} тг. Капитал: {capital} тг.
                Нужно срочно что-то менять.
            """.trimIndent(),
            flavor = "🚨",
            priority = 10,
            conditions = listOf(cond(DEBT, GT, 0L), cond(CAPITAL, LTE, 50_000L)),
            options = listOf(
                option(
                    id    = "sell_investments",
                    text  = "Продать инвестиции для погашения",
                    emoji = "📉",
                    next  = MONTHLY_TICK,
                    fx    = Effect(debtDelta = -200_000L, investmentsDelta = -200_000L, stressDelta = 15)
                ),
                option(
                    id    = "debt_restructure",
                    text  = "Реструктурировать долг в банке",
                    emoji = "🏦",
                    next  = MONTHLY_TICK,
                    fx    = Effect(expensesDelta = -20_000L, stressDelta = 20, knowledgeDelta = 5)
                )
            )
        ),

        // Выгорание — высокий стресс
        event(
            id = "burnout_warning",
            message = """
                😮‍💨 ВЫГОРАНИЕ ПРИБЛИЖАЕТСЯ

                Стресс: {stress}/100.
                Ты работаешь 12 часов в день уже 3 месяца.

                Коллега советует взять отпуск.
                Но дедлайн через 2 недели.
            """.trimIndent(),
            flavor = "😮‍💨",
            priority = 8,
            conditions = listOf(cond(STRESS, GTE, 75L)),
            options = listOf(
                option(
                    id    = "take_vacation",
                    text  = "Взять отпуск (расходы без дохода)",
                    emoji = "🏖️",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -50_000L, stressDelta = -30, knowledgeDelta = 3)
                ),
                option(
                    id    = "push_through",
                    text  = "Дотерпеть до дедлайна",
                    emoji = "😤",
                    next  = MONTHLY_TICK,
                    fx    = Effect(stressDelta = 10)
                )
            )
        ),

        // Разблокировка инвестиций — при высоких знаниях
        event(
            id = "investment_unlock",
            message = """
                💡 ФИНАНСОВЫЕ ЗНАНИЯ ОТКРЫВАЮТ ВОЗМОЖНОСТИ

                Знания: {knowledge}/100.
                Ты понял основы инвестирования.

                Брокер предлагает открыть ИИС (индивидуальный инвестиционный счёт) —
                налоговый вычет 10% от вложенных денег.
            """.trimIndent(),
            flavor = "💡",
            priority = 5,
            conditions = listOf(cond(KNOWLEDGE, GTE, 40L)),
            unique = true,
            options = listOf(
                option(
                    id    = "open_iis",
                    text  = "Открыть ИИС и вложить 100к",
                    emoji = "📈",
                    next  = MONTHLY_TICK,
                    fx    = Effect(capitalDelta = -100_000L, investmentsDelta = 110_000L, knowledgeDelta = 5)
                ),
                option(
                    id    = "skip_iis",
                    text  = "Пока не готов — подожду",
                    emoji = "⏸️",
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
        PoolEntry("normal_life",    weight = 25),
        PoolEntry("family_pressure", weight = 8),
        PoolEntry("mortgage_offer",  weight = 6),
    ) + ScamEventLibrary.poolEntries
}
