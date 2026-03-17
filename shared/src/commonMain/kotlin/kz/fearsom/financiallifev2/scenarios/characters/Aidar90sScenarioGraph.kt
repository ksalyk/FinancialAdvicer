// ── Пакеты ─────────────────────────────────────────────────────────────────
package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.CAPITAL
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.DEBT
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.KNOWLEDGE
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.MONTH
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

// ── Сценарий: Айдар (90-е) ────────────────────────────────────────────────
class Aidar90sScenarioGraph : ScenarioGraph() {

    override val initialPlayerState = PlayerState(
        capital = 50_000L,
        income = 15_000L,
        expenses = 12_000L,
        debt = 0L,
        debtPaymentMonthly = 0L,
        investments = 0L,
        investmentReturnRate = 0.05,
        stress = 60,
        financialKnowledge = 15,
        riskLevel = 40,
        month = 1,
        year = 1993,
        characterId = "aidar_90s",
        eraId = "kz_90s",
        flags = setOf()
    )

    override val events: Map<String, GameEvent> = buildMap {

        // ── ОБЯЗАТЕЛЬНО: Стартовое событие ───────────────────────────────
        put("intro", event(
            id = "intro",
            message = story(
                """
                1993 год. Алматы гудит не жизнью, а сломанным электричеством эпохи. Деньги меняют ценность быстрее, чем люди успевают к ним привыкнуть, зарплаты задерживают, знакомые исчезают в новых «схемах», а вчерашние правила уже никого не защищают.
                """,
                """
                Айдару двадцать два. Он ещё слишком молод, чтобы считать себя сломанным, и уже достаточно взрослый, чтобы понять: если семья сейчас не удержится, никто не придёт и не соберёт её обратно. Родители вложили всё в ваучеры, которые обещали будущее, а принесли только стыд и пыльные разговоры на кухне.
                """,
                """
                На руках {capital}. Доход {income}, если вообще заплатят. И прямо сейчас звонит отец: какие-то люди обещают вернуть потерянное, если вложить ещё. В 90-х почти каждая надежда приходит в дом в костюме спасения. Айдару нужно решить, поверить ли в неё ещё раз.
                """
            ),
            flavor = "📼",
            options = listOf(
                option(
                    id = "warn_parents",
                    text = "Предупредить отца — это скам!",
                    emoji = "🛑",
                    next = "parents_conflict",
                    fx = Effect(knowledgeDelta = 5, stressDelta = 10, setFlags = setOf("warned_parents"))
                ),
                option(
                    id = "send_money_help",
                    text = "Отправить им последние деньги",
                    emoji = "🤲",
                    next = "parents_lost_money",
                    fx = Effect(capitalDelta = -40_000L, stressDelta = 20, setFlags = setOf("helped_parents_scam"))
                ),
                option(
                    id = "ignore_focus_self",
                    text = "Игнорировать. Спасать себя.",
                    emoji = "🧊",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -5, knowledgeDelta = 2, setFlags = setOf("self_preservation"))
                )
            )
        ))

        // ── ГЛАВА 1: Выживание (Месяцы 1-3) ──────────────────────────────
        put("parents_conflict", event(
            id = "parents_conflict",
            message = story(
                """
                Отец бросает трубку почти с обидой. В 90-х люди защищают не только деньги, которых уже нет, но и право верить, что их всё-таки можно вернуть. Айдару тяжело слышать в родном голосе недоверие, но ещё тяжелее было бы промолчать.
                """,
                """
                На работе тоже нет ощущения почвы под ногами. Начальство предлагает зарплату не деньгами, а сахаром, маслом и чем-то ещё из того, что пока не обесценилось прямо по дороге домой. Время такое: каждый сам решает, что здесь на самом деле считается оплатой труда.
                """,
                """
                Можно взять товар и превратить его в шанс, ждать живые деньги или окончательно признать: выживание требует искать вторую жизнь вне основной работы.
                """
            ),
            flavor = "📞",
            tags = setOf("family", "career"),
            options = listOf(
                option(
                    id = "take_goods",
                    text = "Взять сахар и масло (можно продать)",
                    emoji = "🍬",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 5_000L, knowledgeDelta = 3, stressDelta = 2)
                ),
                option(
                    id = "wait_cash",
                    text = "Ждать живые деньги",
                    emoji = "💵",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 5)
                ),
                option(
                    id = "look_side_hustle",
                    text = "Искать подработку на рынке",
                    emoji = "🏪",
                    next = "market_opportunity",
                    fx = Effect(stressDelta = 10, knowledgeDelta = 5)
                )
            )
        ))

        put("parents_lost_money", event(
            id = "parents_lost_money",
            message = story(
                """
                Деньги исчезают так же быстро, как и люди, которые их обещали вернуть. Всё повторяется с пугающей точностью: новые лица, те же слова, та же воронка стыда после того, как дверь за мошенниками уже закрылась.
                """,
                """
                Отец не выдерживает. Сердце сдаёт раньше, чем он успевает сказать что-то полезное или попросить прощения. В один день Айдар перестаёт быть просто сыном и становится главным кормильцем семьи, хотя никто не спрашивал, готов ли он к такой роли.
                """,
                """
                Теперь каждый следующий выбор будет тяжелее прежнего. Потому что речь уже не о личной амбиции, а о том, кто вообще сможет удержать дом от окончательного развала.
                """
            ),
            flavor = "💔",
            tags = setOf("crisis", "family"),
            options = listOf(
                option(
                    id = "take_extra_job",
                    text = "Взять вторую смену (таксистом)",
                    emoji = "🚕",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = 10_000L, stressDelta = 25, knowledgeDelta = 5)
                ),
                option(
                    id = "sell_computer",
                    text = "Продать свой ПК для лечения",
                    emoji = "💻",
                    next = "no_computer_life",
                    fx = Effect(capitalDelta = 150_000L, incomeDelta = -5_000L, stressDelta = -10)
                )
            )
        ))

        put("market_opportunity", event(
            id = "market_opportunity",
            message = story(
                """
                На барахолке всё пахнет одинаково: пылью, дешёвым пластиком и шансом выскочить из бедности на один удачный оборот. Айдару предлагают партию китайских часов, и предложение звучит ровно так, как в 90-х звучат почти все опасные возможности: быстро, уверенно и почти правдоподобно.
                """,
                """
                Вокруг полно людей, которые уже перестали верить в долгую честную дорогу. Кто-то торгует обувью, кто-то сигаретами, кто-то чужими обещаниями. На этом фоне риск начинает выглядеть не исключением, а единственной нормой времени.
                """,
                """
                Чтобы зайти, нужно вложить 30 000 тг. Для кого-то это мелочь, для Айдара почти кусок будущего. Но если партия выстрелит, он впервые почувствует не просто выживание, а движение вверх.
                """
            ),
            flavor = "⌚",
            tags = setOf("investment", "adventure"),
            options = listOf(
                option(
                    id = "buy_watches",
                    text = "Вложить все в часы",
                    emoji = "🎲",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -30_000L, riskDelta = 20, scheduleEvent = ScheduledEvent("watches_result", 2))
                ),
                option(
                    id = "skip_risk",
                    text = "Слишком опасно. Работать грузчиком",
                    emoji = "📦",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 5_000L, stressDelta = 10, knowledgeDelta = 2)
                )
            )
        ))

        put("watches_result", event(
            id = "watches_result",
            message = story(
                """
                Партия приходит спустя недели нервного ожидания. В 90-х ожидание почти всегда хуже самого результата, потому что за это время человек уже успевает прожить и победу, и крах в голове по нескольку раз.
                """,
                """
                Коробки стоят перед Айдаром как немой приговор его дерзости. Либо это первый честный шанс заработать на собственной смелости, либо очередное напоминание, что рынок того времени любит не трудолюбивых, а тех, кому хотя бы раз улыбнулась удача.
                """,
                """
                Дальше придётся быстро решить, как именно прожить этот исход: с достоинством победителя или с грубым уроком человека, который попробовал залезть выше своей опоры.
                """
            ),
            flavor = "📦",
            tags = setOf("consequence"),
            options = listOf(
                option(
                    id = "sell_success",
                    text = "Продать с прибылью (Удача!)",
                    emoji = "🤑",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 90_000L, knowledgeDelta = 10, stressDelta = -10),
//                    conditions = listOf(cond(KNOWLEDGE, GTE, 25L))
                ),
                option(
                    id = "sell_loss",
                    text = "Продать за бессток (Провал)",
                    emoji = "📉",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 10_000L, stressDelta = 20),
//                    conditions = listOf(cond(KNOWLEDGE, LT, 25L))
                )
            )
        ))

        put("no_computer_life", event(
            id = "no_computer_life",
            message = """
                Компьютера нет. Подработки программистом больше нет.
                Ты спас отца, но отбросил карьеру.
                Теперь только физический труд.
            """.trimIndent(),
            flavor = "🕯️",
            options = listOf(
                option(
                    id = "hard_labor",
                    text = "Работать на заводе",
                    emoji = "🏭",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = 5_000L, stressDelta = 15, knowledgeDelta = -5)
                )
            )
        ))

        // ── ГЛАВА 2: Введение Тенге (Месяц 4-6) ───────────────────────────
        put("tenge_introduction", event(
            id = "tenge_introduction",
            message = story(
                """
                Ноябрь 1993-го. На улицах говорят только об одном: ввели тенге. Старые деньги уже почти как призрак прежней страны, а новые ещё не успели стать чем-то понятным и надёжным. Люди стоят в очередях с тревогой, которая давно стала общим выражением лица.
                """,
                """
                Для Айдара это не просто государственная новость. Это проверка на взрослость в эпоху, где взрослость каждый день выглядит по-новому. Если ошибиться с накоплениями сейчас, ошибка будет стоить не процентов, а целых месяцев жизни.
                """,
                """
                Инфляция пожирает время так же быстро, как и деньги. Нужно решить, что именно из его небольшого капитала имеет шанс пережить ближайшие месяцы.
                """
            ),
            flavor = "📰",
            tags = setOf("era", "crisis"),
            options = listOf(
                option(
                    id = "buy_usd_tenge",
                    text = "Купить доллары (спасение от инфляции)",
                    emoji = "💵",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -20_000L, investmentsDelta = 20_000L, knowledgeDelta = 5)
                ),
                option(
                    id = "hold_tenge",
                    text = "Оставить в тенге — верю в страну",
                    emoji = "🇰🇿",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 10, knowledgeDelta = 3)
                ),
                option(
                    id = "buy_gold",
                    text = "Купить золото (советские монеты)",
                    emoji = "🪙",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -25_000L, investmentsDelta = 25_000L, riskDelta = 10)
                )
            )
        ))

        put("inflation_crisis", event(
            id = "inflation_crisis",
            message = """
                📈 Цены в магазине выросли на 40% за месяц!
                Хлеб: 5 тг → 7 тг
                Молоко: 12 тг → 17 тг
                
                Твоей зарплаты хватает только на еду.
            """.trimIndent(),
            flavor = "📈",
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "cut_expenses",
                    text = "Сократить расходы до минимума",
                    emoji = "✂️",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = -3_000L, stressDelta = 15)
                ),
                option(
                    id = "borrow_money",
                    text = "Занять у друзей (под проценты)",
                    emoji = "🤝",
                    next = MONTHLY_TICK,
                    fx = Effect(debtDelta = 50_000L, capitalDelta = 50_000L, stressDelta = -5)
                ),
                option(
                    id = "find_second_job",
                    text = "Найти вторую работу",
                    emoji = "💼",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = 8_000L, stressDelta = 20)
                )
            )
        ))

        // ── ГЛАВА 3: Предпринимательство (Месяц 7-12) ────────────────────
        put("business_opportunity", event(
            id = "business_opportunity",
            message = """
                🏪 Друг предлагает открыть ларек с китайскими товарами.
                Вложение: 200 000 тг.
                Доход: до 50 000 тг/мес.
                
                Нужен партнер. Ты в деле?
            """.trimIndent(),
            flavor = "🏪",
            tags = setOf("investment", "career"),
            options = listOf(
                option(
                    id = "open_kiosk",
                    text = "Вложиться (50% доля)",
                    emoji = "🤝",
                    next = "kiosk_opened",
                    fx = Effect(capitalDelta = -100_000L, incomeDelta = 25_000L, knowledgeDelta = 15, setFlags = setOf("has_kiosk"))
                ),
                option(
                    id = "decline_business",
                    text = "Отказаться — слишком рискованно",
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5, stressDelta = -5)
                ),
                option(
                    id = "negotiate_better",
                    text = "Торговаться за лучшую долю",
                    emoji = "💬",
                    next = "kiosk_negotiation",
                    fx = Effect(knowledgeDelta = 8, riskDelta = 5)
                )
            )
        ))

        put("kiosk_opened", event(
            id = "kiosk_opened",
            message = """
                🎉 Ларек открыт!
                Первые покупатели пошли.
                
                Но налоговая уже интересуется...
            """.trimIndent(),
            flavor = "🎉",
            tags = setOf("consequence"),
            options = listOf(
                option(
                    id = "pay_taxes",
                    text = "Платить налоги честно",
                    emoji = "📋",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = -5_000L, stressDelta = -10, knowledgeDelta = 5)
                ),
                option(
                    id = "bribe_inspector",
                    text = "Дать взятку (5000 тг)",
                    emoji = "🤫",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -5_000L, stressDelta = 15, riskDelta = 20)
                )
            )
        ))

        put("kiosk_negotiation", event(
            id = "kiosk_negotiation",
            message = """
                Друг согласился на 60% долю тебе.
                Но он обиделся. Отношения натянуты.
            """.trimIndent(),
            flavor = "💬",
            tags = setOf("consequence"),
            options = listOf(
                option(
                    id = "accept_deal",
                    text = "Принять сделку",
                    emoji = "✅",
                    next = "kiosk_opened",
                    fx = Effect(capitalDelta = -120_000L, incomeDelta = 30_000L, setFlags = setOf("has_kiosk"))
                ),
                option(
                    id = "walk_away",
                    text = "Уйти из сделки",
                    emoji = "🚶",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -10, knowledgeDelta = 5)
                )
            )
        ))

        // ── ГЛАВА 4: Повторный Скам (Месяц 13-18) ────────────────────────
        put("parents_scam_again", event(
            id = "parents_scam_again",
            message = """
                📞 Отец снова звонит.
                «Ты был прав в прошлый раз. Но сейчас точно надежно! МММ-2».
                Они хотят взять кредит под залог квартиры.
            """.trimIndent(),
            flavor = "📞",
            tags = setOf("family", "scam"),
            options = listOf(
                option(
                    id = "stop_parents_hard",
                    text = "Заблокировать их счета (Жестко)",
                    emoji = "🛑",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 20, knowledgeDelta = 10, setFlags = setOf("parents_scam_stopped"))
                ),
                option(
                    id = "let_them_try",
                    text = "Пусть сами учатся (Опасно)",
                    emoji = "🎲",
                    next = "parents_lost_money_2",
                    fx = Effect(stressDelta = 10, setFlags = setOf("parents_scam_stopped"))
                ),
                option(
                    id = "educate_parents",
                    text = "Объяснить спокойно (требует время)",
                    emoji = "📚",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 15, stressDelta = 5, setFlags = setOf("parents_educated"))
                )
            )
        ))

        put("parents_lost_money_2", event(
            id = "parents_lost_money_2",
            message = """
                😱 Они взяли кредит 500 000 тг и вложили в МММ.
                Пирамида лопнула через 2 месяца.
                
                Теперь ты должен помочь выплатить кредит.
                Или они потеряют квартиру.
            """.trimIndent(),
            flavor = "💔",
            tags = setOf("crisis", "family"),
            options = listOf(
                option(
                    id = "pay_parents_debt",
                    text = "Выплатить их долг (спаси квартиру)",
                    emoji = "💳",
                    next = MONTHLY_TICK,
                    fx = Effect(debtDelta = 500_000L, stressDelta = 30, setFlags = setOf("saved_parents_home"))
                ),
                option(
                    id = "let_them_suffer",
                    text = "Пусть сами разбираются",
                    emoji = "🧊",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -20, knowledgeDelta = 10, setFlags = setOf("parents_lost_home"))
                )
            )
        ))

        // ── ГЛАВА 5: Конституция 1995 (Месяц 19-24) ──────────────────────
        put("constitution_1995", event(
            id = "constitution_1995",
            message = """
                📜 30 августа 1995 — принята новая Конституция!
                Частная собственность защищена законом.
                
                Землю можно брать в аренду на 99 лет.
                Интересует сельское хозяйство?
            """.trimIndent(),
            flavor = "📜",
            tags = setOf("era", "investment"),
            options = listOf(
                option(
                    id = "buy_land",
                    text = "Купить участок земли (долгосрок)",
                    emoji = "🌾",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -150_000L, investmentsDelta = 150_000L, knowledgeDelta = 20)
                ),
                option(
                    id = "skip_land",
                    text = "Не сейчас — нет опыта",
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5)
                )
            )
        ))

        // ── ГЛАВА 6: Кризис 1998 (Месяц 25-36) ───────────────────────────
        put("russia_crisis_1998", event(
            id = "russia_crisis_1998",
            message = """
                🚨 РОССИЯ ДЕВАЛЬВИРОВАЛА РУБЛЬ!
                Кризис ударил по Казахстану.
                
                Тенге падает. Импорт дорожает.
                Твой бизнес под угрозой.
            """.trimIndent(),
            flavor = "🚨",
            tags = setOf("era", "crisis"),
            options = listOf(
                option(
                    id = "hedge_currency",
                    text = "Срочно купить доллары",
                    emoji = "💵",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, investmentsDelta = 50_000L, stressDelta = -10)
                ),
                option(
                    id = "cut_business",
                    text = "Сократить бизнес, сохранить капитал",
                    emoji = "✂️",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = -20_000L, capitalDelta = 30_000L, stressDelta = 10)
                ),
                option(
                    id = "hold_and_pray",
                    text = "Держаться и молиться",
                    emoji = "🙏",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 25)
                )
            )
        ))

        // ── ГЛАВА 7: Финал (Месяц 37+) ───────────────────────────────────
        put("final_choice", event(
            id = "final_choice",
            message = """
                🎯 1999 год. Ты прошел через всё.
                
                Капитал: {capital} тг
                Доход: {income} тг/мес
                Знания: {knowledge}/100
                
                Что дальше? Эмиграция или остаться строить бизнес?
            """.trimIndent(),
            flavor = "🎯",
            tags = setOf("ending"),
            options = listOf(
                option(
                    id = "emigrate",
                    text = "Уехать в Россию/Германию",
                    emoji = "✈️",
                    next = "ending_emigration",
                    fx = Effect()
                ),
                option(
                    id = "stay_build",
                    text = "Остаться и строить империю",
                    emoji = "🏗️",
                    next = "ending_business",
                    fx = Effect()
                ),
                option(
                    id = "retire_early",
                    text = "Хватит — живу на пассивный доход",
                    emoji = "🏖️",
                    next = "ending_freedom",
                    fx = Effect()
                )
            )
        ))

        // ── КОНЦОВКИ ─────────────────────────────────────────────────────
        put("ending_freedom", event(
            id = "ending_freedom",
            message = """
                🏆 ФИНАНСОВАЯ НЕЗАВИСИМОСТЬ (90-е Style)
                
                Айдар смог ухватить волну. 
                Капитал: {capital} тг (или $10,000).
                Ты открыл свой магазин или уехал в Москву.
                
                Родители спасены. 90-е пройдены.
            """.trimIndent(),
            flavor = "🏆",
            isEnding = true,
            endingType = EndingType.FINANCIAL_FREEDOM,
            options = emptyList()
        ))

        put("ending_business", event(
            id = "ending_business",
            message = """
                🏢 БИЗНЕС-ИМПЕРИЯ
                
                Твой ларек вырос в сеть магазинов.
                Ты один из тех, кто построил новый Казахстан.
                
                Но сколько здоровья это стоило?
            """.trimIndent(),
            flavor = "🏢",
            isEnding = true,
            endingType = EndingType.WEALTH,
            options = emptyList()
        ))

        put("ending_emigration", event(
            id = "ending_emigration",
            message = """
                ✈️ ЭМИГРАЦИЯ
                
                Ты уехал в поисках лучшей жизни.
                В Германии/России ты начал с нуля.
                
                Но дети будут говорить по-немецки/русски.
                Родина осталась в прошлом.
            """.trimIndent(),
            flavor = "✈️",
            isEnding = true,
            endingType = EndingType.FINANCIAL_STABILITY,
            options = emptyList()
        ))

        put("ending_bankruptcy", event(
            id = "ending_bankruptcy",
            message = """
                💔 КРАХ.
                
                Инфляция съела остатки. 
                Родители в долгах. 
                Ты остался без жилья и средств.
                
                90-е не прощают ошибок.
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
                
                Ты выжил. Но не более.
                90-е закончились, а ты всё ещё считаешь каждый тенге.
                
                Жизнь продолжается...
            """.trimIndent(),
            flavor = "😐",
            isEnding = true,
            endingType = EndingType.PAYCHECK_TO_PAYCHECK,
            options = emptyList()
        ))

        // ── HUB: Обычная жизнь ───────────────────────────────────────────
        put("normal_life", event(
            id = "normal_life",
            message = """
                Месяц прошел. Свет дают по графику.
                Цены в магазине выросли на 10%.
                
                Что в приоритете?
            """.trimIndent(),
            flavor = "📺",
            poolWeight = 20,
            options = listOf(
                option(
                    id = "buy_usd",
                    text = "Купить доллары (спасение от инфляции)",
                    emoji = "💵",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -10_000L, investmentsDelta = 10_000L, knowledgeDelta = 2)
                ),
                option(
                    id = "study_books",
                    text = "Читать фин. книги (библиотека)",
                    emoji = "📚",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5, stressDelta = -2)
                ),
                option(
                    id = "do_nothing",
                    text = "Просто выжить",
                    emoji = "😐",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 2)
                ),
                option(
                    id = "network_friends",
                    text = "Встретиться с друзьями (нетворкинг)",
                    emoji = "🍺",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 3, capitalDelta = -3_000L, stressDelta = -5)
                )
            )
        ))

        // ── ДОПОЛНИТЕЛЬНЫЕ СОБЫТИЯ ПУЛА ──────────────────────────────────
        put("job_offer", event(
            id = "job_offer",
            message = """
                💼 Предложили работу в новой фирме.
                Зарплата: 25 000 тг/мес (vs текущие 15 000).
                
                Но фирма сомнительная — могут закрыть через месяц.
            """.trimIndent(),
            flavor = "💼",
            poolWeight = 10,
            tags = setOf("career"),
            options = listOf(
                option(
                    id = "take_job",
                    text = "Согласиться — деньги нужны",
                    emoji = "✅",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = 10_000L, stressDelta = 10)
                ),
                option(
                    id = "decline_job",
                    text = "Отказаться — стабильность важнее",
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 3, stressDelta = -5)
                )
            )
        ))

        put("health_issue", event(
            id = "health_issue",
            message = """
                🏥 Здоровье подводит.
                Стресс и плохое питание сказываются.
                
                Лечение: 20 000 тг. Или терпеть?
            """.trimIndent(),
            flavor = "🏥",
            poolWeight = 8,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "treat_health",
                    text = "Лечиться (20к тг)",
                    emoji = "💊",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -20_000L, stressDelta = -15)
                ),
                option(
                    id = "ignore_health",
                    text = "Само пройдет",
                    emoji = "🤷",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 10, knowledgeDelta = -5)
                )
            )
        ))

        put("friend_investment", event(
            id = "friend_investment",
            message = """
                🤝 Друг предлагает вложиться в общий бизнес.
                Нужно 50 000 тг. Обещают 30% годовых.
                
                Доверяешь ли ты ему?
            """.trimIndent(),
            flavor = "🤝",
            poolWeight = 12,
            tags = setOf("investment", "scam"),
            options = listOf(
                option(
                    id = "invest_friend",
                    text = "Вложиться (друг же!)",
                    emoji = "💰",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, investmentsDelta = 50_000L, riskDelta = 15)
                ),
                option(
                    id = "decline_friend",
                    text = "Отказать — деньги не пахнут",
                    emoji = "🛑",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5, stressDelta = 5)
                )
            )
        ))

        put("black_market", event(
            id = "black_market",
            message = """
                🌑 На черном рынке предлагают доллары по курсу ниже официального.
                Можно купить на 20% выгоднее.
                
                Но риск нарваться на фальшивки или милицию.
            """.trimIndent(),
            flavor = "🌑",
            poolWeight = 10,
            tags = setOf("investment", "adventure"),
            options = listOf(
                option(
                    id = "buy_black_usd",
                    text = "Купить на черном рынке",
                    emoji = "🤫",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -15_000L, investmentsDelta = 18_000L, riskDelta = 25)
                ),
                option(
                    id = "buy_official_usd",
                    text = "Купить в банке (официально)",
                    emoji = "🏦",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -18_000L, investmentsDelta = 18_000L, knowledgeDelta = 3)
                )
            )
        ))

        put("family_celebration", event(
            id = "family_celebration",
            message = """
                🎉 У родственника той (свадьба).
                Нужно подарить минимум 5 000 тг.
                
                Но у тебя сейчас туго с деньгами...
            """.trimIndent(),
            flavor = "🎉",
            poolWeight = 15,
            tags = setOf("family"),
            options = listOf(
                option(
                    id = "give_gift",
                    text = "Дарить (сохранить лицо)",
                    emoji = "🎁",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -5_000L, stressDelta = -5, knowledgeDelta = 2)
                ),
                option(
                    id = "skip_celebration",
                    text = "Не идти (сэкономить)",
                    emoji = "🏠",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 10, knowledgeDelta = -3)
                )
            )
        ))

        put("utility_bills", event(
            id = "utility_bills",
            message = """
                💡 Пришли счета за коммунальные услуги.
                Свет, вода, отопление: 8 000 тг.
                
                Свет отключают за неуплату...
            """.trimIndent(),
            flavor = "💡",
            poolWeight = 18,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "pay_bills",
                    text = "Оплатить полностью",
                    emoji = "💳",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 8_000L, stressDelta = -5)
                ),
                option(
                    id = "pay_partial",
                    text = "Оплатить половину",
                    emoji = "💰",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 4_000L, stressDelta = 5)
                ),
                option(
                    id = "skip_bills",
                    text = "Не платить (риск отключения)",
                    emoji = "⚠️",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 15, riskDelta = 10)
                )
            )
        ))

        put("education_opportunity", event(
            id = "education_opportunity",
            message = """
                🎓 Открылись курсы по бухгалтерии/праву.
                Стоимость: 30 000 тг.
                Длительность: 3 месяца.
                
                Это может открыть новые возможности.
            """.trimIndent(),
            flavor = "🎓",
            poolWeight = 10,
            tags = setOf("career"),
            options = listOf(
                option(
                    id = "take_course",
                    text = "Пройти курсы",
                    emoji = "📚",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -30_000L, knowledgeDelta = 25, incomeDelta = 5_000L)
                ),
                option(
                    id = "skip_course",
                    text = "Нет времени/денег",
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -3)
                )
            )
        ))

        put("car_purchase", event(
            id = "car_purchase",
            message = """
                🚗 Продают старую «девятку» за 150 000 тг.
                Машина нужна для бизнеса/такси.
                
                Но это все твои накопления...
            """.trimIndent(),
            flavor = "🚗",
            poolWeight = 8,
            tags = setOf("investment"),
            options = listOf(
                option(
                    id = "buy_car",
                    text = "Купить (для работы)",
                    emoji = "🔑",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -150_000L, incomeDelta = 15_000L, riskDelta = 10)
                ),
                option(
                    id = "skip_car",
                    text = "Обойдусь без машины",
                    emoji = "🚶",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 3)
                )
            )
        ))

        put("apartment_rent", event(
            id = "apartment_rent",
            message = """
                🏠 Хозяйка квартиры повышает аренду на 5 000 тг/мес.
                Или ищи новое жилье.
                
                Переезд стоит 10 000 тг и куча нервов.
            """.trimIndent(),
            flavor = "🏠",
            poolWeight = 15,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "accept_raise",
                    text = "Согласиться на повышение",
                    emoji = "😤",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 5_000L, stressDelta = 10)
                ),
                option(
                    id = "move_out",
                    text = "Искать новое жилье",
                    emoji = "📦",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -10_000L, expensesDelta = -2_000L, stressDelta = 15)
                )
            )
        ))

        put("winter_prep", event(
            id = "winter_prep",
            message = """
                ❄️ Зима близко. Нужно запастись углем/дровами.
                Стоимость: 15 000 тг.
                
                Без отопления не выжить в алматинской зиме.
            """.trimIndent(),
            flavor = "❄️",
            poolWeight = 12,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "buy_fuel",
                    text = "Закупить топливо",
                    emoji = "🪵",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -15_000L, stressDelta = -10)
                ),
                option(
                    id = "risk_cold",
                    text = "Рискнуть — зима будет теплой",
                    emoji = "🥶",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 20, riskDelta = 15)
                )
            )
        ))

        put("tax_inspection", event(
            id = "tax_inspection",
            message = """
                📋 Налоговая пришла с проверкой.
                Нашли нарушения на 50 000 тг.
                
                Платить или «договориться»?
            """.trimIndent(),
            flavor = "📋",
            poolWeight = 6,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "pay_fine",
                    text = "Заплатить штраф",
                    emoji = "💳",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, knowledgeDelta = 10)
                ),
                option(
                    id = "bribe_tax",
                    text = "Дать взятку (20к тг)",
                    emoji = "🤫",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -20_000L, riskDelta = 30, stressDelta = 10)
                )
            )
        ))

        put("lottery_win", event(
            id = "lottery_win",
            message = """
                🎰 Выиграл в лотерею 10 000 тг!
                Не богато, но приятно.
                
                Что сделаешь с выигрышем?
            """.trimIndent(),
            flavor = "🎰",
            poolWeight = 5,
            tags = setOf("windfall"),
            options = listOf(
                option(
                    id = "save_win",
                    text = "Отложить в копилку",
                    emoji = "💰",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 10_000L, knowledgeDelta = 3)
                ),
                option(
                    id = "spend_win",
                    text = "Потратить на себя",
                    emoji = "🎁",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -10)
                ),
                option(
                    id = "invest_win",
                    text = "Вложить в дело",
                    emoji = "📈",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 10_000L, knowledgeDelta = 5)
                )
            )
        ))

        put("old_friend_return", event(
            id = "old_friend_return",
            message = """
                👋 Вернулся старый друг из России.
                Привез деньги, предлагает совместный бизнес.
                
                Доверяешь ли ты ему после стольких лет?
            """.trimIndent(),
            flavor = "👋",
            poolWeight = 8,
            tags = setOf("career", "investment"),
            options = listOf(
                option(
                    id = "partner_friend",
                    text = "Стать партнером",
                    emoji = "🤝",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -30_000L, incomeDelta = 10_000L, riskDelta = 15)
                ),
                option(
                    id = "decline_friend",
                    text = "Отказаться politely",
                    emoji = "🙏",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5, stressDelta = -5)
                )
            )
        ))

        put("child_birth", event(
            id = "child_birth",
            message = """
                👶 У тебя родился ребенок!
                Поздравляем! 🎉
                
                Но расходы вырастут на 10 000 тг/мес...
            """.trimIndent(),
            flavor = "👶",
            poolWeight = 4,
            tags = setOf("family"),
            options = listOf(
                option(
                    id = "accept_child",
                    text = "Ради семьи всё отдам",
                    emoji = "💝",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 10_000L, stressDelta = 10, knowledgeDelta = 5)
                ),
                option(
                    id = "plan_budget",
                    text = "Срочно пересмотреть бюджет",
                    emoji = "📊",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 10_000L, knowledgeDelta = 10)
                )
            )
        ))

        put("theft_victim", event(
            id = "theft_victim",
            message = """
                🚨 Тебя обокрали!
                Украли 20 000 тг из кармана.
                
                Милиция не поможет — у них своих проблем хватает.
            """.trimIndent(),
            flavor = "🚨",
            poolWeight = 6,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "report_theft",
                    text = "Заявить в милицию (бесполезно)",
                    emoji = "👮",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -20_000L, stressDelta = 10)
                ),
                option(
                    id = "accept_loss",
                    text = "Смириться и жить дальше",
                    emoji = "😔",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -20_000L, knowledgeDelta = 5)
                )
            )
        ))

        put("currency_exchange", event(
            id = "currency_exchange",
            message = """
                💱 Курс доллара резко вырос!
                Те, кто купил раньше — в плюсе.
                
                Покупать сейчас или ждать падения?
            """.trimIndent(),
            flavor = "💱",
            poolWeight = 12,
            tags = setOf("investment"),
            options = listOf(
                option(
                    id = "buy_usd_now",
                    text = "Покупать сейчас",
                    emoji = "💵",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -20_000L, investmentsDelta = 20_000L)
                ),
                option(
                    id = "wait_dip",
                    text = "Ждать коррекции",
                    emoji = "⏳",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5, stressDelta = 5)
                )
            )
        ))

        put("business_partner_betray", event(
            id = "business_partner_betray",
            message = """
                😡 Партнер по бизнесу исчез с деньгами!
                Твоя доля: 50 000 тг.
                
                Искать его или принять потерю?
            """.trimIndent(),
            flavor = "😡",
            poolWeight = 5,
            tags = setOf("crisis", "scam"),
            options = listOf(
                option(
                    id = "hunt_partner",
                    text = "Искать и требовать деньги",
                    emoji = "🔍",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -10_000L, stressDelta = 25, riskDelta = 20)
                ),
                option(
                    id = "write_off",
                    text = "Списать как урок",
                    emoji = "📝",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, knowledgeDelta = 15)
                )
            )
        ))

        put("government_subsidy", event(
            id = "government_subsidy",
            message = """
                🏛️ Государство выделило субсидии для малого бизнеса.
                Можно получить 100 000 тг под 5% годовых.
                
                Но paperwork займет 2 месяца...
            """.trimIndent(),
            flavor = "🏛️",
            poolWeight = 8,
            tags = setOf("investment"),
            options = listOf(
                option(
                    id = "apply_subsidy",
                    text = "Подать заявку",
                    emoji = "📋",
                    next = MONTHLY_TICK,
                    fx = Effect(debtDelta = 100_000L, capitalDelta = 100_000L, knowledgeDelta = 10)
                ),
                option(
                    id = "skip_subsidy",
                    text = "Слишком сложно",
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -5)
                )
            )
        ))

        put("medical_emergency", event(
            id = "medical_emergency",
            message = """
                🚑 Срочная госпитализация родственника.
                Нужно 100 000 тг немедленно.
                
                Это всё твои накопления...
            """.trimIndent(),
            flavor = "🚑",
            poolWeight = 4,
            tags = setOf("crisis", "family"),
            options = listOf(
                option(
                    id = "pay_medical",
                    text = "Отдать все накопления",
                    emoji = "💝",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -100_000L, stressDelta = -20, setFlags = setOf("saved_relative"))
                ),
                option(
                    id = "partial_help",
                    text = "Дать половину (50к)",
                    emoji = "🤲",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, stressDelta = 10)
                )
            )
        ))

        put("new_year_bonus", event(
            id = "new_year_bonus",
            message = """
                🎄 Начислили новогодний бонус: 30 000 тг!
                Неожиданная удача.
                
                Как распорядишься?
            """.trimIndent(),
            flavor = "🎄",
            poolWeight = 10,
            tags = setOf("windfall"),
            options = listOf(
                option(
                    id = "save_bonus",
                    text = "Отложить в резерв",
                    emoji = "🛡️",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 30_000L, knowledgeDelta = 5)
                ),
                option(
                    id = "celebrate",
                    text = "Отметить с семьей",
                    emoji = "🎉",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -15_000L, stressDelta = -20)
                ),
                option(
                    id = "invest_bonus",
                    text = "Вложить в бизнес",
                    emoji = "📈",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 30_000L, knowledgeDelta = 8)
                )
            )
        ))

        put("apartment_purchase", event(
            id = "apartment_purchase",
            message = """
                🏠 Продается 1-комнатная квартира за 2 000 000 тг.
                Можно взять в рассрочку: 500к сразу + 50к/мес.
                
                Это твой шанс обзавестись жильем!
            """.trimIndent(),
            flavor = "🏠",
            poolWeight = 6,
            tags = setOf("investment"),
            options = listOf(
                option(
                    id = "buy_apartment",
                    text = "Купить в рассрочку",
                    emoji = "🔑",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -500_000L, debtDelta = 1_500_000L, debtPaymentDelta = 50_000L, investmentsDelta = 2_000_000L)
                ),
                option(
                    id = "wait_apartment",
                    text = "Подождать лучших времен",
                    emoji = "⏳",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5)
                )
            )
        ))

        put("political_change", event(
            id = "political_change",
            message = """
                🗳️ В стране политические изменения.
                Новые законы для бизнеса.
                
                Адаптироваться или уходить в тень?
            """.trimIndent(),
            flavor = "🗳️",
            poolWeight = 8,
            tags = setOf("era"),
            options = listOf(
                option(
                    id = "adapt_law",
                    text = "Работать по новым законам",
                    emoji = "📜",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 15, incomeDelta = -5_000L)
                ),
                option(
                    id = "go_shadow",
                    text = "Уйти в тень (риск!)",
                    emoji = "🌑",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = 10_000L, riskDelta = 30)
                )
            )
        ))

        put("child_education", event(
            id = "child_education",
            message = """
                🎓 Ребенок подрос. Нужна школа.
                Частная: 50 000 тг/мес. Государственная: бесплатно.
                
                Но в гос. школе нет будущего...
            """.trimIndent(),
            flavor = "🎓",
            poolWeight = 10,
            tags = setOf("family"),
            options = listOf(
                option(
                    id = "private_school",
                    text = "Частная школа (инвестиция в будущее)",
                    emoji = "🏫",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 50_000L, knowledgeDelta = 10)
                ),
                option(
                    id = "public_school",
                    text = "Государственная (экономия)",
                    emoji = "🏛️",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 5, knowledgeDelta = -5)
                )
            )
        ))

        put("retirement_planning", event(
            id = "retirement_planning",
            message = """
                👴 Пенсия в 90-е — миф.
                Но копить на старость нужно уже сейчас.
                
                Откладывать 10% дохода?
            """.trimIndent(),
            flavor = "👴",
            poolWeight = 8,
            tags = setOf("investment"),
            options = listOf(
                option(
                    id = "save_retirement",
                    text = "Откладывать 10%",
                    emoji = "🐖",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 1_500L, capitalDelta = -1_500L, knowledgeDelta = 10)
                ),
                option(
                    id = "no_retirement",
                    text = "Сейчас не до пенсии",
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -5)
                )
            )
        ))

        put("inflation_spike", event(
            id = "inflation_spike",
            message = """
                📈 Инфляция подскочила до 30% в месяц!
                Цены меняются каждый день.
                
                Твои сбережения тают на глазах.
            """.trimIndent(),
            flavor = "📈",
            poolWeight = 10,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "convert_all",
                    text = "Конвертировать всё в доллары",
                    emoji = "💵",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -30_000L, investmentsDelta = 30_000L)
                ),
                option(
                    id = "buy_goods",
                    text = "Скупить товары про запас",
                    emoji = "🛒",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -20_000L, stressDelta = -10)
                )
            )
        ))

        put("gang_protection", event(
            id = "gang_protection",
            message = """
                🚔 Местные «авторитеты» предлагают крышу.
                10 000 тг/мес за защиту.
                
                Откажешься — могут быть проблемы...
            """.trimIndent(),
            flavor = "🚔",
            poolWeight = 6,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "pay_protection",
                    text = "Платить за крышу",
                    emoji = "🤝",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 10_000L, stressDelta = -15, riskDelta = 10)
                ),
                option(
                    id = "refuse_protection",
                    text = "Отказаться (риск!)",
                    emoji = "🛑",
                    next = MONTHLY_TICK,
                    fx = Effect(riskDelta = 40, stressDelta = 20)
                )
            )
        ))

        put("export_opportunity", event(
            id = "export_opportunity",
            message = """
                🌍 Китайцы ищут поставщиков зерна из Казахстана.
                Контракт на 500 000 тг.
                
                Но нужна лицензия и связи...
            """.trimIndent(),
            flavor = "🌍",
            poolWeight = 5,
            tags = setOf("investment", "career"),
            options = listOf(
                option(
                    id = "pursue_export",
                    text = "Получить лицензию и работать",
                    emoji = "📋",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -100_000L, incomeDelta = 50_000L, knowledgeDelta = 20)
                ),
                option(
                    id = "skip_export",
                    text = "Слишком сложно для меня",
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -5)
                )
            )
        ))

        put("bank_collapse", event(
            id = "bank_collapse",
            message = """
                🏦 Банк, где ты хранил деньги, лопнул!
                Вклад 100 000 тг потерян.
                
                Государство не компенсирует...
            """.trimIndent(),
            flavor = "🏦",
            poolWeight = 4,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "accept_loss",
                    text = "Смириться (урок на будущее)",
                    emoji = "😔",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -100_000L, knowledgeDelta = 20)
                ),
                option(
                    id = "protest_bank",
                    text = "Участвовать в митингах",
                    emoji = "📢",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 15, knowledgeDelta = 5)
                )
            )
        ))

        put("wedding_expense", event(
            id = "wedding_expense",
            message = """
                💒 Твоя свадьба!
                Той стоит минимум 200 000 тг.
                
                Но это важно для семьи и статуса...
            """.trimIndent(),
            flavor = "💒",
            poolWeight = 3,
            tags = setOf("family"),
            options = listOf(
                option(
                    id = "big_wedding",
                    text = "Сделать большой той",
                    emoji = "🎉",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -200_000L, stressDelta = -20, knowledgeDelta = 5)
                ),
                option(
                    id = "small_wedding",
                    text = "Скромно (семья обидится)",
                    emoji = "🏠",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, stressDelta = 15)
                )
            )
        ))

        put("corruption_demand", event(
            id = "corruption_demand",
            message = """
                🤫 Чиновник требует «подарок» за разрешение.
                30 000 тг или дело не сдвинется.
                
                Платить или жаловаться?
            """.trimIndent(),
            flavor = "🤫",
            poolWeight = 6,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "pay_bribe",
                    text = "Заплатить (быстрее)",
                    emoji = "💰",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -30_000L, riskDelta = 20)
                ),
                option(
                    id = "report_corruption",
                    text = "Пожаловаться (долго и опасно)",
                    emoji = "📢",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 25, knowledgeDelta = 10, riskDelta = 30)
                )
            )
        ))

        put("stock_market_90s", event(
            id = "stock_market_90s",
            message = """
                📈 Появилась фондовая биржа в Алматы!
                Можно купить акции privatized предприятий.
                
                Риск огромный, но и доходность тоже...
            """.trimIndent(),
            flavor = "📈",
            poolWeight = 8,
            tags = setOf("investment"),
            options = listOf(
                option(
                    id = "buy_stocks",
                    text = "Купить акции (50к тг)",
                    emoji = "📊",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, investmentsDelta = 50_000L, riskDelta = 25)
                ),
                option(
                    id = "avoid_stocks",
                    text = "Это казино, не бизнес",
                    emoji = "🎰",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5)
                )
            )
        ))

        put("family_immigration", event(
            id = "family_immigration",
            message = """
                ✈️ Родственники уезжают в Германию.
                Предлагают забрать тебя с семьей.
                
                Но нужно 500 000 тг на оформление...
            """.trimIndent(),
            flavor = "✈️",
            poolWeight = 5,
            tags = setOf("family"),
            options = listOf(
                option(
                    id = "join_immigration",
                    text = "Поехать с ними",
                    emoji = "🧳",
                    next = "ending_emigration",
                    fx = Effect(capitalDelta = -500_000L)
                ),
                option(
                    id = "stay_kazakhstan",
                    text = "Остаться на родине",
                    emoji = "🇰🇿",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 10, knowledgeDelta = 5)
                )
            )
        ))

        put("millennium_eve", event(
            id = "millennium_eve",
            message = """
                🎆 31 декабря 1999 года.
                Ты пережил лихие 90-е.
                
                Что ты вынес из этого десятилетия?
            """.trimIndent(),
            flavor = "🎆",
            poolWeight = 3,
            tags = setOf("ending"),
            options = listOf(
                option(
                    id = "reflect_wisdom",
                    text = "Мудрость и опыт — главный капитал",
                    emoji = "📚",
                    next = "final_choice",
                    fx = Effect(knowledgeDelta = 20)
                ),
                option(
                    id = "reflect_money",
                    text = "Деньги — единственная правда",
                    emoji = "💰",
                    next = "final_choice",
                    fx = Effect(riskDelta = 20)
                )
            )
        ))
    }

    override val conditionalEvents: List<GameEvent> = listOf(
        // ОБЯЗАТЕЛЬНО: Триггер банкротства
        event(
            id = "bankruptcy_trigger",
            message = "Деньги кончились. Инфляция добила.",
            flavor = "💀",
            priority = 90,
            conditions = listOf(cond(CAPITAL, LTE, 0L), cond(STRESS, GTE, 90L)),
            options = listOf(
                option("accept_bankruptcy", "Признать поражение", "💔", next = "ending_bankruptcy", fx = Effect())
            )
        ),
        // Долговой кризис
        event(
            id = "debt_crisis",
            message = "⚠️ КРИЗИС! Тебе должны вернуть долг, но не отдают.",
            flavor = "🚨",
            priority = 100,
            conditions = listOf(cond(DEBT, GT, 100_000L)),
            tags = setOf("crisis"),
            options = listOf(
                option("debt_collect", "Нанять сборщиков (Риск!)", "🔫", next = MONTHLY_TICK, fx = Effect(debtDelta = -50_000L, stressDelta = 30, capitalDelta = -20_000L)),
                option("debt_wait", "Ждать лучших времен", "⏳", next = MONTHLY_TICK, fx = Effect(stressDelta = 10))
            )
        ),
        // Повторный скам на родителей
        event(
            id = "parents_scam_again_conditional",
            message = """
                📞 Отец снова звонит.
                «Ты был прав в прошлый раз. Но сейчас точно надежно! МММ-2».
                Они хотят взять кредит под залог квартиры.
            """.trimIndent(),
            flavor = "📞",
            priority = 80,
            conditions = listOf(
                cond(MONTH, GTE, 12L),
                Condition.NotFlag("parents_scam_stopped"),
                Condition.ForCharacter("aidar_90s")
            ),
            unique = true,
            tags = setOf("family", "scam"),
            options = listOf(
                option(
                    "stop_parents_hard",
                    "Заблокировать их счета (Жестко)",
                    "🛑",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 20, knowledgeDelta = 10, setFlags = setOf("parents_scam_stopped"))
                ),
                option(
                    "let_them_try",
                    "Пусть сами учатся (Опасно)",
                    "🎲",
                    next = "parents_lost_money_2",
                    fx = Effect(stressDelta = 10, setFlags = setOf("parents_scam_stopped"))
                )
            )
        ),
        // Выгорание
        event(
            id = "burnout_90s",
            message = "😮‍💨 Нервы на пределе. Постоянная тревога за будущее.",
            flavor = "🔥",
            priority = 60,
            conditions = listOf(cond(STRESS, GT, 75L)),
            cooldownMonths = 6,
            options = listOf(
                option("rest_home", "Отсидеться дома (экономия)", "🏠", next = MONTHLY_TICK, fx = Effect(expensesDelta = -5_000L, stressDelta = -15, incomeDelta = -5_000L)),
                option("drink_friends", "Встретиться с друзьями (алкоголь)", "🍺", next = MONTHLY_TICK, fx = Effect(capitalDelta = -5_000L, stressDelta = -10, knowledgeDelta = -2))
            )
        ),
        // Введение тенге (эра-событие)
        event(
            id = "era_tenge_introduced",
            message = """
                📰 15 ноября 1993 — ввели тенге!
                Рубли обменивают по курсу 1:500.
                
                Инфляция 20% в месяц. Цены растут каждый день.
            """.trimIndent(),
            flavor = "📰",
            priority = 95,
            conditions = listOf(
                cond(MONTH, GTE, 4L),
                cond(MONTH, LTE, 6L),
                Condition.InEra("kz_90s")
            ),
            unique = true,
            tags = setOf("era"),
            options = listOf(
                option("buy_usd_tenge", "Купить доллары", "💵", next = MONTHLY_TICK, fx = Effect(capitalDelta = -20_000L, investmentsDelta = 20_000L, knowledgeDelta = 5)),
                option("hold_tenge", "Оставить в тенге", "🇰🇿", next = MONTHLY_TICK, fx = Effect(stressDelta = 10, knowledgeDelta = 3))
            )
        ),
        // Конституция 1995
        event(
            id = "era_constitution_1995",
            message = """
                📜 30 августа 1995 — принята новая Конституция!
                Частная собственность защищена законом.
            """.trimIndent(),
            flavor = "📜",
            priority = 95,
            conditions = listOf(
                cond(MONTH, GTE, 20L),
                cond(MONTH, LTE, 24L),
                Condition.InEra("kz_90s")
            ),
            unique = true,
            tags = setOf("era"),
            options = listOf(
                option("buy_land", "Купить землю", "🌾", next = MONTHLY_TICK, fx = Effect(capitalDelta = -150_000L, investmentsDelta = 150_000L, knowledgeDelta = 20)),
                option("skip_land", "Не сейчас", "❌", next = MONTHLY_TICK, fx = Effect(knowledgeDelta = 5))
            )
        ),
        // Кризис 1998
        event(
            id = "era_russia_crisis_1998",
            message = """
                🚨 РОССИЯ ДЕВАЛЬВИРОВАЛА РУБЛЬ!
                Кризис ударил по Казахстану.
            """.trimIndent(),
            flavor = "🚨",
            priority = 95,
            conditions = listOf(
                cond(MONTH, GTE, 30L),
                cond(MONTH, LTE, 36L),
                Condition.InEra("kz_90s")
            ),
            unique = true,
            tags = setOf("era", "crisis"),
            options = listOf(
                option("hedge_currency", "Купить доллары", "💵", next = MONTHLY_TICK, fx = Effect(capitalDelta = -50_000L, investmentsDelta = 50_000L, stressDelta = -10)),
                option("cut_business", "Сократить бизнес", "✂️", next = MONTHLY_TICK, fx = Effect(incomeDelta = -20_000L, capitalDelta = 30_000L, stressDelta = 10))
            )
        ),
        // Ловушка зарплата-в-зарплату
        event(
            id = "trap_warning",
            message = "⚠️ Ты живешь от зарплаты до зарплаты уже 6 месяцев.",
            flavor = "⚠️",
            priority = 50,
            conditions = listOf(
                cond(CAPITAL, LTE, 50_000L),
                cond(KNOWLEDGE, LTE, 15L),
                cond(MONTH, GTE, 6L)
            ),
            tags = setOf("warning"),
            options = listOf(
                option("learn_more", "Начать учиться финансам", "📚", next = MONTHLY_TICK, fx = Effect(knowledgeDelta = 10, stressDelta = 5)),
                option("ignore_warning", "Игнорировать", "😶", next = MONTHLY_TICK, fx = Effect(stressDelta = 5))
            )
        ),
        // Бонус за успех
        event(
            id = "bonus_received",
            message = "🎉 Годовой бонус! Ты справляешься отлично.",
            flavor = "🎉",
            priority = 30,
            conditions = listOf(
                cond(CAPITAL, GTE, 500_000L),
                cond(KNOWLEDGE, GTE, 30L)
            ),
            cooldownMonths = 12,
            options = listOf(
                option("reinvest_bonus", "Реинвестировать", "📈", next = MONTHLY_TICK, fx = Effect(investmentsDelta = 100_000L, capitalDelta = -100_000L, knowledgeDelta = 5)),
                option("celebrate_bonus", "Отпраздновать", "🎊", next = MONTHLY_TICK, fx = Effect(capitalDelta = -50_000L, stressDelta = -20))
            )
        )
    )

    override val eventPool: List<PoolEntry> = buildList {
        add(PoolEntry("normal_life", 20))
        add(PoolEntry("job_offer", 10))
        add(PoolEntry("health_issue", 8))
        add(PoolEntry("friend_investment", 12))
        add(PoolEntry("black_market", 10))
        add(PoolEntry("family_celebration", 15))
        add(PoolEntry("utility_bills", 18))
        add(PoolEntry("education_opportunity", 10))
        add(PoolEntry("car_purchase", 8))
        add(PoolEntry("apartment_rent", 15))
        add(PoolEntry("winter_prep", 12))
        add(PoolEntry("tax_inspection", 6))
        add(PoolEntry("lottery_win", 5))
        add(PoolEntry("old_friend_return", 8))
        add(PoolEntry("child_birth", 4))
        add(PoolEntry("theft_victim", 6))
        add(PoolEntry("currency_exchange", 12))
        add(PoolEntry("business_partner_betray", 5))
        add(PoolEntry("government_subsidy", 8))
        add(PoolEntry("medical_emergency", 4))
        add(PoolEntry("new_year_bonus", 10))
        add(PoolEntry("apartment_purchase", 6))
        add(PoolEntry("political_change", 8))
        add(PoolEntry("child_education", 10))
        add(PoolEntry("retirement_planning", 8))
        add(PoolEntry("inflation_spike", 10))
        add(PoolEntry("gang_protection", 6))
        add(PoolEntry("export_opportunity", 5))
        add(PoolEntry("bank_collapse", 4))
        add(PoolEntry("wedding_expense", 3))
        add(PoolEntry("corruption_demand", 6))
        add(PoolEntry("stock_market_90s", 8))
        add(PoolEntry("family_immigration", 5))
        add(PoolEntry("millennium_eve", 3))
        add(PoolEntry("market_opportunity", 8))
        add(PoolEntry("business_opportunity", 8))
        add(PoolEntry("tenge_introduction", 6))
        add(PoolEntry("inflation_crisis", 10))
        add(PoolEntry("constitution_1995", 5))
        add(PoolEntry("russia_crisis_1998", 5))
        addAll(ScamEventLibrary.poolEntries)
    }
}
