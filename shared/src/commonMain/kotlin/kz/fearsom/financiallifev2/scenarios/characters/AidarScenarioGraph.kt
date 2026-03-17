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
                Сегодня первый рабочий день в новой компании.

                Я — Айдар, 24 года, junior разработчик в Алматы.
                Снимаю квартиру с другом Сержаном, платим пополам — по 45к каждый.
                На счету {capital} тг — откладывал год, не позволял себе лишнего.

                Зарплата {income} тг/мес. Расходы {expenses} тг/мес.
                Остаток — моя маленькая свобода.

                Вечером позвонила мама. Брат-студент просит помочь финансово.
                И тут же — сообщение от Данияра, однокурсника: «Есть идея, поговорим?»

                Надо решить, что важнее прямо сейчас.
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
                Встретился с Данияром в кофейне. Он возбуждённый, глаза горят.

                «Слушай, давай сделаем маркетплейс для казахстанских мастеров.
                Гончарка, ювелирка, войлок — всё это хотят покупать за рубежом.
                Нужно 150 тысяч и три месяца работы по вечерам. Делим 50/50.»

                Данияр надёжный. Но {capital} тг — это всё, что у меня есть.
                Если не получится, придётся начинать с нуля.
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
                Три месяца пролетели как один длинный вечер.

                Кодил после работы, иногда до двух ночи. Сержан жаловался на шум клавиш.
                Но MVP готов — 50 мастеров уже зарегистрировались, первые заказы идут.

                Данияр говорит: «Надо питчить акселераторы, пока есть импульс».
                Но деньги заканчиваются. Если я выйду — он вернёт 80к из 150к.

                Я устал. Но бросить сейчас — значит потерять 70к и три месяца жизни.
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
                День питча в акселераторе «Astana Hub».

                Зал на 40 человек. Жюри — три инвестора в дорогих пиджаках.
                У меня 7 минут, чтобы объяснить, почему наш маркетплейс изменит рынок.

                Мои финансовые знания: {knowledge}/100.
                Я понимаю цифры — но умею ли я их объяснить незнакомым людям?

                Два возможных исхода: грант 500к или полный отказ.
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
                Они сказали «да».

                500 000 тг и стол в коворкинге «The Hive». Данияр обнял меня прямо в лифте.
                За неделю число мастеров выросло до 300 — сарафанное радио работает.

                Но я всё ещё хожу на основную работу. Не сплю нормально уже две недели.
                Стартап пока не платит мне ни тенге — только обещания.

                Пора выбирать: рискнуть и уйти с работы, или тянуть оба фронта дальше.
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
                Полгода в компании. Тимлид Бекзат вызвал на разговор.

                «Айдар, мы тебя ценим. Есть вариант стать Middle — +80к к зарплате,
                будешь вести джуна. Но знаю, что тебе пишут рекрутеры».

                Действительно — три дня назад написали из Астаны. Senior сразу, +150к,
                но придётся переехать. Там дороже жильё, но карьера — как ракета.

                Сейчас зарабатываю {income} тг/мес. Что важнее — стабильность или рост?
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
                Мама позвонила поздно вечером. Голос дрожал.

                Брат провалил две сессии — нужны репетиторы, иначе отчислят. Это 50к.
                И самое страшное — папа попал в больницу. Операция нужна срочно, стоит 200к.
                «Ты один у нас, сынок. Больше не к кому».

                У меня {capital} тг. Это мой запас, который я строил месяцами.
                Семья — это святое. Но если я отдам всё — что будет дальше со мной?
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
                Сегодня пришло письмо из банка. Одобрили ипотеку по программе «7-20-25».

                Студия в Алматы — 12 000 000 тг. Взнос 20%, это 2 400 000 тг.
                Платёж — около 80 000 тг в месяц на 20 лет.

                У меня {capital} тг накоплений.
                Надоело снимать и зависеть от хозяйки. Но 20 лет кредита — это серьёзно.
                Мама говорит: «Своё жильё — это главное». Сержан говорит: «Не торопись».
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
                Три года назад я писал код в 2 ночи и боялся потерять 150 тысяч.

                Сегодня в команде 18 человек. Маркетплейс — 5 000 мастеров, продажи в 12 стран.
                Привлекли раунд A: $500k от казахстанского венчурного фонда.

                Брат работает у нас в поддержке. Маме купил квартиру.
                Я — CEO в 27 лет. Это звучит странно, но это правда.

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
                Сегодня подписал оффер Lead Developer. Зарплата {income} тг/мес.

                Ипотека почти закрыта. Квартира моя — маленькая, но своя.
                Коллеги уважают, джуны приходят за советом. Это приятно.

                Иногда думаю про стартап. Но потом смотрю на остаток {capital} тг
                и понимаю — я сделал правильный выбор для себя.
                Не для всех. Для себя.
            """.trimIndent(),
            flavor = "💼",
            isEnding = true,
            endingType = EndingType.FINANCIAL_STABILITY,
            options = emptyList()
        ))

        put("ending_freedom", event(
            id = "ending_freedom",
            message = """
                Закрыл ноутбук и понял — сегодня я работал не потому что должен.

                Пассивный доход от инвестиций перекрыл расходы ещё в прошлом квартале.
                Капитал {capital} тг. Инвестиции приносят больше, чем я трачу.

                Можно работать. Можно не работать. Выбор — мой.
                Это и есть финансовая свобода. Не яхта. Просто выбор.
            """.trimIndent(),
            flavor = "🏖️",
            isEnding = true,
            endingType = EndingType.FINANCIAL_FREEDOM,
            options = emptyList()
        ))

        put("ending_broke", event(
            id = "ending_broke",
            message = """
                Сижу в пустой квартире. Сержан уехал — не мог платить аренду.

                Стартап не взлетел. Долги накопились. Помогал семье — опустошил всё.
                Банк звонит каждый день. На счету почти ноль.

                Нужно начинать с нуля. Это больно и стыдно.
                Но я записал всё в этот дневник — чтобы не забыть, чего больше не делать.
            """.trimIndent(),
            flavor = "💀",
            isEnding = true,
            endingType = EndingType.BANKRUPTCY,
            options = emptyList()
        ))

        put("ending_paycheck", event(
            id = "ending_paycheck",
            message = """
                Зарплата пришла. Всё как всегда.

                Квартплата, продукты, кредит за телефон — всё уходит.
                До следующей зарплаты остаётся немного. Мечты о стартапе — где-то в прошлом.

                Долгов нет. Работа есть. Это что-то, да.
                Может, в следующем месяце начну откладывать хотя бы 10%.
                Может.
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
                Обычный месяц. Код, стенд-апы, обеды в офисной столовке.

                На счету {capital} тг. Зарплата {income} тг/мес — приходит стабильно.
                Ничего особенного не случилось. Но это тоже выбор — что делать с остатком.

                Инвестировать? Учиться? Просто копить?
                Каждый месяц — это маленькое решение, которое складывается в большое будущее.
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
                Сегодня банк прислал уведомление о просрочке.

                Ипотека + расходы давно съедают больше, чем я зарабатываю.
                Я знал, что так будет — но надеялся, что вырулю.

                Долг: {debt} тг. На счету только {capital} тг.
                Нужно что-то сделать прямо сейчас — иначе будет хуже.
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
                Сегодня утром не мог встать с кровати. Просто лежал и смотрел в потолок.

                Стресс: {stress}/100. Три месяца по 12 часов — это много.
                Марат сказал: «Ты выглядишь как зомби, возьми отпуск».

                Дедлайн через две недели. Если уйду — подведу команду.
                Если не уйду — скоро сломаюсь окончательно.
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
                Наконец-то дочитал книгу про инвестиции. Финансовые знания: {knowledge}/100.

                Написал брокер — предложил открыть ИИС (индивидуальный инвестиционный счёт).
                Налоговый вычет 10% от суммы — это реальные деньги, не рекламная ерунда.

                Можно вложить 100к и сразу вернуть 10к налогами.
                Риски есть, но знания снижают страх. Может, время начать?
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
        PoolEntry("normal_life",    baseWeight = 25),
        PoolEntry("family_pressure", baseWeight = 8),
        PoolEntry("mortgage_offer",  baseWeight = 6),
    ) + ScamEventLibrary.poolEntries
}
