package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.*
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.*

/**
 * Shared library of financial scam and fraud events.
 * Each scam type has ERA VARIANTS — trigger events differ per era,
 * but follow-up/consequence events are shared.
 *
 * Era mapping:
 *   kz_90s  — 1991-2000: no internet, personal visits, МММ wave
 *   kz_2005 — 2000-2009: early internet, dial-up, SMS, ICQ
 *   kz_2015 — 2014-2019: VKontakte, early Instagram, early WhatsApp
 *   kz_2024 — 2020+:     Telegram, Instagram, TikTok, Crypto, Pig Butchering
 *
 * Flag conventions:
 *   "learned.scam.X"     — player understands this scam type → weight drops 85%
 *   "lost_money_to_scam" — any scam weight drops further
 *   "in_mlm"             — player is in MLM now
 */
object ScamEventLibrary {

    // ── DSL helpers ───────────────────────────────────────────────────────────

    private fun event(
        id: String,
        message: String,
        flavor: String = "💬",
        priority: Int = 0,
        conditions: List<Condition> = emptyList(),
        tags: Set<String> = emptySet(),
        poolWeight: Int = 10,
        unique: Boolean = false,
        cooldownMonths: Int = 0,
        isEnding: Boolean = false,
        endingType: EndingType? = null,
        options: List<GameOption>
    ) = GameEvent(id, message, flavor, options, conditions, priority, isEnding, endingType,
                  tags, poolWeight, unique, cooldownMonths)

    private fun option(
        id: String, text: String, emoji: String, next: String, fx: Effect = Effect()
    ) = GameOption(id, text, emoji, fx, next)

    private fun stat(field: Condition.Stat.Field, op: Condition.Stat.Op, value: Long) =
        Condition.Stat(field, op, value)

    private fun inEra(eraId: String) = Condition.InEra(eraId)
    private fun notFlag(flag: String) = Condition.NotFlag(flag)

    // ════════════════════════════════════════════════════════════════════
    //  1. PYRAMID SCHEME — Пирамиды
    //  Общие follow-up: pyramidAvoided, pyramidCollapse, pyramidSmallLoss
    // ════════════════════════════════════════════════════════════════════

    /** kz_90s — сосед приходит домой с предложением */
    private val pyramidNeighbor90s = event(
        id = "scam_pyramid_neighbor_90s",
        message = """
            Сосед Марат пришёл прямо домой, в руках — стопка бумаг.

            «Слушай, тут такое дело. Вступительный взнос 100 тысяч тенге,
            и каждый месяц тебе идут деньги — пока ты привлекаешь людей.
            Я уже две недели внутри, мне уже пришло 40 тысяч!»

            Газеты пишут о таких схемах каждую неделю. Но Марат — нормальный мужик.
        """.trimIndent(),
        flavor = "🏚️",
        tags = setOf("scam", "scam.pyramid", "social.friend"),
        poolWeight = 18,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 100_000L),
            notFlag("learned.scam.pyramid"),
            inEra("kz_90s")
        ),
        options = listOf(
            option("pyramid_invest_full", "Вложить 100 000 тг — раз уже платит", "💸",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, stressDelta = 10, riskDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_pyramid_collapse", afterMonths = 2)
                )),
            option("pyramid_ask_docs", "Попросить документы — как работает схема?", "🔍",
                next = "scam_pyramid_avoided",
                fx = Effect(knowledgeDelta = 8, stressDelta = -5)),
            option("pyramid_small_bet", "Вложить 20 000 тг — небольшая сумма, не страшно", "🎲",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -20_000, stressDelta = 10, riskDelta = 10,
                    scheduleEvent = ScheduledEvent("scam_pyramid_small_loss", afterMonths = 1)
                )),
            option("pyramid_decline", "Отказать — «не моя тема»", "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5, stressDelta = -8,
                    setFlags = setOf("learned.scam.pyramid")
                ))
        )
    )

    /** kz_2005 — звонок по стационарному телефону + письмо по email */
    private val pyramidEmail2005 = event(
        id = "scam_pyramid_email_2005",
        message = """
            Пришло письмо на email — адрес дал кто-то из знакомых.

            «Уважаемый, приглашаю в финансовый клуб. Взнос 100 000 тг,
            гарантированный доход 40% в месяц. Работаем официально с 2003 года.
            Позвоните для записи на встречу».

            Потом позвонил сам Бауыржан — говорит, уже полгода в системе,
            всё прозрачно, никакого риска. Предлагает встретиться в кафе.
        """.trimIndent(),
        flavor = "📧",
        tags = setOf("scam", "scam.pyramid", "social.friend"),
        poolWeight = 18,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 100_000L),
            notFlag("learned.scam.pyramid"),
            inEra("kz_2005")
        ),
        options = listOf(
            option("pyramid_invest_full", "Вложить 100 000 тг — Бауыржан давно знакомый", "💸",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, stressDelta = 10, riskDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_pyramid_collapse", afterMonths = 2)
                )),
            option("pyramid_ask_docs", "Попросить официальные документы компании", "🔍",
                next = "scam_pyramid_avoided",
                fx = Effect(knowledgeDelta = 8, stressDelta = -5)),
            option("pyramid_small_bet", "Вложить 20 000 тг — «попробую»", "🎲",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -20_000, stressDelta = 10, riskDelta = 10,
                    scheduleEvent = ScheduledEvent("scam_pyramid_small_loss", afterMonths = 1)
                )),
            option("pyramid_decline", "Отказать — «занят, потом перезвоню» (и не перезвонить)", "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5, stressDelta = -8,
                    setFlags = setOf("learned.scam.pyramid")
                ))
        )
    )

    /** kz_2015 — звонок в WhatsApp + пост в VKontakte */
    private val pyramidVk2015 = event(
        id = "scam_pyramid_vk_2015",
        message = """
            Асан написал в VKontakte: «Зайди, есть тема».

            «Слушай, я нашёл кое-что. Мне уже пришло 80 000 тг за месяц.
            Мама тоже вложила. Гарантия 100%. Нужно минимум 100 000 тг.
            Пришли мне в WhatsApp — объясню схему, там всё легально».

            Асан — старый друг. Но что-то в голосе звучит слишком... продающе.
        """.trimIndent(),
        flavor = "📱",
        tags = setOf("scam", "scam.pyramid", "social.friend"),
        poolWeight = 18,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 100_000L),
            notFlag("learned.scam.pyramid"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("pyramid_invest_full", "Вложить 100 000 тг — Асан же друг детства", "💸",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, stressDelta = 10, riskDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_pyramid_collapse", afterMonths = 2)
                )),
            option("pyramid_ask_docs", "Попросить документы — как именно зарабатываются деньги?", "🔍",
                next = "scam_pyramid_avoided",
                fx = Effect(knowledgeDelta = 8, stressDelta = -5)),
            option("pyramid_small_bet", "Вложить 20 000 тг — «не жалко потерять»", "🎲",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -20_000, stressDelta = 10, riskDelta = 10,
                    scheduleEvent = ScheduledEvent("scam_pyramid_small_loss", afterMonths = 1)
                )),
            option("pyramid_decline", "Отказать — «не моя тема, извини»", "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5, stressDelta = -8,
                    setFlags = setOf("learned.scam.pyramid")
                ))
        )
    )

    /** kz_2024 — Telegram/WhatsApp */
    private val pyramidFriendCall = event(
        id = "scam_pyramid_friend",
        message = """
            Асан позвонил поздно вечером, голос взволнованный.

            «Слушай, я нашёл кое-что. Мне уже пришло 80 000 тг за месяц.
            Мама тоже вложила. Гарантия 100%. Нужно минимум 100 000 тг.
            Я за тебя отвечаю, ты же мне доверяешь?»

            Асан — старый друг. Но что-то в голосе звучит слишком... продающе.
        """.trimIndent(),
        flavor = "😰",
        tags = setOf("scam", "scam.pyramid", "social.friend"),
        poolWeight = 18,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 100_000L),
            notFlag("learned.scam.pyramid"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("pyramid_invest_full", "Вложить 100 000 тг — Асан же друг детства", "💸",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, stressDelta = 10, riskDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_pyramid_collapse", afterMonths = 2)
                )),
            option("pyramid_ask_docs", "Попросить документы — как именно зарабатываются деньги?", "🔍",
                next = "scam_pyramid_avoided",
                fx = Effect(knowledgeDelta = 8, stressDelta = -5)),
            option("pyramid_small_bet", "Вложить 20 000 тг — «не жалко потерять, интересно попробовать»", "🎲",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -20_000, stressDelta = 10, riskDelta = 10,
                    scheduleEvent = ScheduledEvent("scam_pyramid_small_loss", afterMonths = 1)
                )),
            option("pyramid_decline", "Отказать — «не моя тема, извини»", "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5, stressDelta = -8,
                    setFlags = setOf("learned.scam.pyramid")
                ))
        )
    )

    // ── Pyramid shared follow-ups ─────────────────────────────────────────────

    private val pyramidAvoided = event(
        id = "scam_pyramid_avoided",
        message = """
            Не смог объяснить, откуда берётся доход.
            «Ну... там система, умные люди занимаются...»

            Погуглил — классическая финансовая пирамида.
            Первые участники реально получают деньги — от новых вкладчиков.
            Математика неизбежно ведёт к коллапсу.

            Через 3 месяца пришло сообщение: «Ты был прав. Всё рухнуло.»
        """.trimIndent(),
        flavor = "📚",
        tags = setOf("scam.pyramid", "educational"),
        options = listOf(
            option("pyramid_lesson_learned", "Хорошо, что разобрался. Запомню признаки пирамиды.", "✅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 20, stressDelta = -10,
                    setFlags = setOf("learned.scam.pyramid")
                ))
        )
    )

    private val pyramidCollapse = event(
        id = "scam_pyramid_collapse",
        message = """
            Пирамида рухнула. Телефон недоступен.
            100 000 тенге потеряны навсегда.

            Организаторы исчезли. Полиция ведёт расследование, но шансов нет.
            Настоящие инвестиции не требуют привлекать новых людей.
        """.trimIndent(),
        flavor = "💀",
        tags = setOf("scam.pyramid", "consequence"),
        options = listOf(
            option("pyramid_rebuild", "Принять потерю. Никогда больше.", "📖",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 25, stressDelta = 20,
                    setFlags = setOf("learned.scam.pyramid", "lost_money_to_scam")
                ))
        )
    )

    private val pyramidSmallLoss = event(
        id = "scam_pyramid_small_loss",
        message = """
            Сначала пришло 8 000 тг — казалось всё реально.
            Потом — тишина. Выплаты остановились.

            Потерял 12 000 тг чистых. Небольшая сумма, но важный урок.
        """.trimIndent(),
        flavor = "😕",
        tags = setOf("scam.pyramid", "consequence"),
        options = listOf(
            option("pyramid_small_lesson", "Дёшево отделался. Теперь знаю признаки пирамид.", "📚",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 20, stressDelta = 5,
                    setFlags = setOf("learned.scam.pyramid")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  2. MLM — Сетевой маркетинг
    // ════════════════════════════════════════════════════════════════════

    /** kz_90s — дверь в дверь, Гербалайф/Амвэй */
    private val mlmDoor90s = event(
        id = "scam_mlm_door_90s",
        message = """
            Позвонили в дверь. Незнакомая женщина с большой сумкой.

            «Здравствуйте! Я представляю компанию Herbalife.
            Эти продукты изменят вашу жизнь! И вы можете сами зарабатывать —
            нужно только купить стартовый набор за 50 000 тг и пригласить друзей».

            Она показала журнал с историями успеха. Все счастливы, все худые.
        """.trimIndent(),
        flavor = "🚪",
        tags = setOf("scam", "scam.mlm", "social.colleague"),
        poolWeight = 14,
        unique = true,
        conditions = listOf(
            notFlag("learned.scam.mlm"),
            inEra("kz_90s")
        ),
        options = listOf(
            option("mlm_go_meeting", "Купить набор — «попробую продавать соседям»", "💳",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -50_000, stressDelta = 5, riskDelta = 10,
                    setFlags = setOf("in_mlm"),
                    scheduleEvent = ScheduledEvent("scam_mlm_month_later", afterMonths = 3)
                )),
            option("mlm_ask_directly", "Спросить: «А сколько реально зарабатывают?»", "🔍",
                next = "scam_mlm_confronted",
                fx = Effect(knowledgeDelta = 5)),
            option("mlm_decline", "Вежливо закрыть дверь", "🙂",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 3,
                    setFlags = setOf("learned.scam.mlm")
                ))
        )
    )

    /** kz_2005 — звонок + физическая встреча, ранний интернет-маркетинг */
    private val mlmPhone2005 = event(
        id = "scam_mlm_phone_2005",
        message = """
            Позвонил знакомый — давно не виделись.

            «Слушай, я сейчас в интересном бизнесе. Косметика, витамины.
            Заработал уже 80 000 тг за 2 месяца. Давай встретимся в субботу,
            я познакомлю тебя с командой. Просто посмотришь».

            На встрече — человек 15, красивые буклеты, рассказы об успехе.
            В конце: «Стартовый набор — 35 000 тг. Регистрация сегодня».
        """.trimIndent(),
        flavor = "📞",
        tags = setOf("scam", "scam.mlm", "social.colleague"),
        poolWeight = 14,
        unique = true,
        conditions = listOf(
            notFlag("learned.scam.mlm"),
            inEra("kz_2005")
        ),
        options = listOf(
            option("mlm_go_meeting", "Купить набор — «попробую месяц»", "💳",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -35_000, stressDelta = 5, riskDelta = 10,
                    setFlags = setOf("in_mlm"),
                    scheduleEvent = ScheduledEvent("scam_mlm_month_later", afterMonths = 3)
                )),
            option("mlm_ask_directly", "Спросить напрямую: «Сколько реально зарабатывает большинство?»", "🔍",
                next = "scam_mlm_confronted",
                fx = Effect(knowledgeDelta = 5)),
            option("mlm_decline", "Отказать — «подумаю»", "🙂",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 3,
                    setFlags = setOf("learned.scam.mlm")
                ))
        )
    )

    /** kz_2015 — Instagram + WhatsApp группа */
    private val mlmInstagram2015 = event(
        id = "scam_mlm_instagram_2015",
        message = """
            Коллега Айгуль изменилась — похудела, новые вещи, счастливая.
            Выложила в Instagram фото с тренингом: «Я теперь предприниматель!»

            «Приходи в пятницу на встречу — просто посмотришь.
            Добавлю тебя в наш WhatsApp-чат, там видео с результатами».

            Раньше она жаловалась на зарплату. Интересно, что случилось.
        """.trimIndent(),
        flavor = "✨",
        tags = setOf("scam", "scam.mlm", "social.colleague"),
        poolWeight = 14,
        unique = true,
        conditions = listOf(
            notFlag("learned.scam.mlm"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("mlm_go_meeting", "Сходить — «просто посмотрю»", "👀",
                next = "scam_mlm_presentation",
                fx = Effect(stressDelta = 3)),
            option("mlm_ask_directly", "Спросить напрямую: «Это МЛМ?»", "🔍",
                next = "scam_mlm_confronted",
                fx = Effect(knowledgeDelta = 5)),
            option("mlm_decline", "Вежливо отказать — занят", "🙂",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 3,
                    setFlags = setOf("learned.scam.mlm")
                ))
        )
    )

    /** kz_2024 — Telegram/Instagram */
    private val mlmColleague = event(
        id = "scam_mlm_colleague",
        message = """
            Коллега Айгуль в последнее время изменилась — светится, новая одежда.
            Говорит: «Нашла удивительные продукты! И я теперь предприниматель.
            Приходи в пятницу на встречу — просто посмотришь, ничего не подписываешь.»

            Раньше она жаловалась на зарплату. Теперь — счастлива. Интересно, что случилось.
        """.trimIndent(),
        flavor = "✨",
        tags = setOf("scam", "scam.mlm", "social.colleague"),
        poolWeight = 14,
        unique = true,
        conditions = listOf(
            notFlag("learned.scam.mlm"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("mlm_go_meeting", "Сходить — «просто посмотрю»", "👀",
                next = "scam_mlm_presentation",
                fx = Effect(stressDelta = 3)),
            option("mlm_ask_directly", "Спросить напрямую: «Это МЛМ?»", "🔍",
                next = "scam_mlm_confronted",
                fx = Effect(knowledgeDelta = 5)),
            option("mlm_decline", "Вежливо отказать — занят", "🙂",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 3,
                    setFlags = setOf("learned.scam.mlm")
                ))
        )
    )

    // ── MLM shared follow-ups ─────────────────────────────────────────────────

    private val mlmPresentation = event(
        id = "scam_mlm_presentation",
        message = """
            Встреча в кафе. Красивые слайды, истории успеха.
            «Пригласи двух — они пригласят по два — через год пассивный доход!»

            В конце: «Стартовый набор продукции — 35 000 тг. Только сегодня со скидкой.»
            Смотрят с надеждой. Ты им важен как «партнёр».
        """.trimIndent(),
        flavor = "📊",
        tags = setOf("scam.mlm"),
        options = listOf(
            option("mlm_join", "Купить стартовый набор — «попробую месяц»", "💳",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -35_000, stressDelta = 5, riskDelta = 10,
                    setFlags = setOf("in_mlm"),
                    scheduleEvent = ScheduledEvent("scam_mlm_month_later", afterMonths = 3)
                )),
            option("mlm_decline_after", "Отказать после презентации — «не моё»", "❌",
                next = "scam_mlm_confronted",
                fx = Effect(knowledgeDelta = 12))
        )
    )

    private val mlmConfronted = event(
        id = "scam_mlm_confronted",
        message = """
            95% дистрибьюторов МЛМ не окупают стартовые вложения — это факт из исследований FTC.

            Настоящий бизнес строится на продаже продукта, а не на вербовке.
            Если основной доход от привлечения новых людей — это пирамида с продуктом.

            Возможно, обидятся. Но ты сберёг деньги и время.
        """.trimIndent(),
        flavor = "📖",
        tags = setOf("scam.mlm", "educational"),
        options = listOf(
            option("mlm_understand", "Понял. Спасибо за урок.", "✅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 18, stressDelta = -5,
                    setFlags = setOf("learned.scam.mlm")
                ))
        )
    )

    private val mlmMonthLater = event(
        id = "scam_mlm_month_later",
        message = """
            3 месяца в МЛМ. Реальность такова:

            Продал продукции на 8 000 тг.
            Потрачено на «обязательные закупки»: 105 000 тг (3 месяца × 35 000).
            Склад дома забит нераспроданным товаром.

            Нужно снова закупить на 35 000, чтобы удержать «ранг директора».
            Продолжать или выйти?
        """.trimIndent(),
        flavor = "📦",
        tags = setOf("scam.mlm", "consequence"),
        options = listOf(
            option("mlm_exit", "Выйти — убытки зафиксированы, дальше будет хуже", "🚪",
                next = MONTHLY_TICK,
                fx = Effect(
                    stressDelta = -15, knowledgeDelta = 25,
                    clearFlags = setOf("in_mlm"),
                    setFlags = setOf("learned.scam.mlm")
                )),
            option("mlm_continue", "Продолжить — «ещё один месяц и всё пойдёт»", "😤",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -35_000, stressDelta = 10, riskDelta = 5
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  3. BETTING / CAPPER
    //  kz_90s/2005 — нет, kz_2015 — ранний, kz_2024 — Telegram
    // ════════════════════════════════════════════════════════════════════

    /** kz_2005 — SMS-типстер, ранние букмекеры */
    private val capperSms2005 = event(
        id = "scam_capper_sms_2005",
        message = """
            Пришло SMS с незнакомого номера:
            «Даю 3 бесплатных прогноза на спорт. 1й: ЦСКА победит сегодня».

            ЦСКА выиграл 2:0. Потом второй прогноз — тоже верный.
            Потом звонок: «Видишь точность? Платный пакет — 15 000 тг в месяц.
            Работаю с букмекерами напрямую».
        """.trimIndent(),
        flavor = "📟",
        tags = setOf("scam", "scam.betting", "scam.capper"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            notFlag("learned.scam.betting"),
            inEra("kz_2005")
        ),
        options = listOf(
            option("capper_buy", "Заплатить 15 000 тг — три из трёх верных!", "💰",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -15_000, riskDelta = 15,
                    scheduleEvent = ScheduledEvent("scam_capper_loses", afterMonths = 1)
                )),
            option("capper_research", "Поискать в интернете про «схему вилки прогнозов»", "🔍",
                next = "scam_capper_explained",
                fx = Effect(knowledgeDelta = 10)),
            option("capper_ignore", "Удалить SMS — слишком подозрительно", "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.betting")
                ))
        )
    )

    /** kz_2015 — VKontakte канал + личное сообщение */
    private val capperVk2015 = event(
        id = "scam_capper_vk_2015",
        message = """
            В VKontakte пришло личное сообщение от незнакомого:
            «Дам 3 бесплатных прогноза. Убедись в уровне.
            Первый: Барселона сегодня победит».

            Барселона выиграла. Второй прогноз сошёлся. Третий — тоже.

            «Видишь уровень? Платный доступ на месяц — 25 000 тг.
            Телеграм-канал пока не завёл, работаю напрямую».
        """.trimIndent(),
        flavor = "⚽",
        tags = setOf("scam", "scam.betting", "scam.capper"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            notFlag("learned.scam.betting"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("capper_buy", "Купить доступ за 25 000 тг — три из трёх верных!", "💰",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -25_000, riskDelta = 15,
                    scheduleEvent = ScheduledEvent("scam_capper_loses", afterMonths = 1)
                )),
            option("capper_research", "Погуглить «схема вилки прогнозов каппер»", "🔍",
                next = "scam_capper_explained",
                fx = Effect(knowledgeDelta = 10)),
            option("capper_ignore", "Заблокировать — слишком подозрительно", "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.betting")
                ))
        )
    )

    /** kz_2024 — Telegram канал */
    private val capperTelegram = event(
        id = "scam_capper_telegram",
        message = """
            В Telegram написал незнакомый аккаунт: «Дам 3 бесплатных прогноза».

            Первый: Реал Мадрид победит Барсу. Выиграл 2:1 — верно.
            Второй сошёлся. Третий тоже. Три из трёх — это впечатляет.

            «Видишь уровень? Платный доступ на месяц — 30 000 тг.
            Первый месяц возвращаю деньги если не заработаешь.»

            Интересно. Но откуда незнакомец знает мой номер?
        """.trimIndent(),
        flavor = "⚽",
        tags = setOf("scam", "scam.betting", "scam.capper"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            notFlag("learned.scam.betting"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("capper_buy", "Купить доступ за 30 000 тг — три из трёх верных!", "💰",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -30_000, riskDelta = 15,
                    scheduleEvent = ScheduledEvent("scam_capper_loses", afterMonths = 1)
                )),
            option("capper_research", "Погуглить «схема вилки прогнозов каппер»", "🔍",
                next = "scam_capper_explained",
                fx = Effect(knowledgeDelta = 10)),
            option("capper_ignore", "Заблокировать — слишком подозрительно", "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.betting")
                ))
        )
    )

    // ── Capper shared follow-ups ──────────────────────────────────────────────

    private val capperExplained = event(
        id = "scam_capper_explained",
        message = """
            Схема называется «вилка прогнозов»:

            1000 человек → 500 получают «А выиграет», 500 — «Б выиграет».
            500 оказались правы → 500 отписались.
            После 5 туров — 30 человек с «магической» серией 5/5.
            Им продают платный доступ.

            Это чистая математика, а не аналитика.
        """.trimIndent(),
        flavor = "🔢",
        tags = setOf("scam.betting", "educational"),
        options = listOf(
            option("capper_lesson", "Понял механику. Не куплюсь на такое.", "📖",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 20, stressDelta = -5,
                    setFlags = setOf("learned.scam.betting")
                ))
        )
    )

    private val capperLoses = event(
        id = "scam_capper_loses",
        message = """
            Прогнозы начали проигрывать. 3 из 5 — в минус.

            Каппер объясняет: «Полоса неудач, рынок непредсказуем.
            Купи годовой пакет — там закрытые матчи с гарантией.»

            Деньги потеряны. Дополнительно предлагают вложить ещё 100 000.
        """.trimIndent(),
        flavor = "😤",
        tags = setOf("scam.betting", "consequence"),
        options = listOf(
            option("capper_stop", "Остановиться. Это схема, а не аналитика.", "🛑",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 18, stressDelta = 5,
                    setFlags = setOf("learned.scam.betting", "lost_money_to_scam")
                )),
            option("capper_escalate", "Купить «годовой пакет» — отыграть потери", "😰",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, stressDelta = 20, riskDelta = 15,
                    scheduleEvent = ScheduledEvent("scam_betting_deep_loss", afterMonths = 2)
                ))
        )
    )

    private val bettingDeepLoss = event(
        id = "scam_betting_deep_loss",
        message = """
            Каппер исчез. Канал удалён. Номер заблокирован.

            Погоня за проигрышем — «chase losses» — это ловушка.
            Чем больше теряешь, тем сильнее желание «отыграться».
            Это называется игровая зависимость.
        """.trimIndent(),
        flavor = "💀",
        tags = setOf("scam.betting", "consequence"),
        options = listOf(
            option("betting_final_lesson", "Дорого. Никогда — ставки на деньги которые нужны.", "📖",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 30, stressDelta = 15,
                    setFlags = setOf("learned.scam.betting", "lost_money_to_scam")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  4. ROMANCE SCAM / PIG BUTCHERING
    //  kz_2015 — ранняя версия (ВКонтакте/Mamba), kz_2024 — WhatsApp
    //  kz_90s / kz_2005 — не существует (нет интернета / слишком рано)
    // ════════════════════════════════════════════════════════════════════

    /** kz_2015 — VKontakte / Mamba / ранний dating */
    private val romanceMamba2015 = event(
        id = "scam_romance_mamba_2015",
        message = """
            В VKontakte написала незнакомая — сказала, «нашла через общих друзей».

            Красивый профиль. Говорит, работает в Алматы, переехала из Шымкента.
            Умная, понимает шутки. Пишет каждый день уже две недели.

            Сегодня впервые упомянула: «Мой дядя научил меня торговать на Форексе.
            Хочешь покажу? Я уже заработала 200 000 тг за месяц».
        """.trimIndent(),
        flavor = "💌",
        tags = setOf("scam", "scam.romance", "scam.crypto"),
        poolWeight = 8,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 200_000L),
            notFlag("learned.scam.romance"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("romance_respond", "Ответить — интересно про Форекс", "💬",
                next = "scam_romance_buildup",
                fx = Effect(stressDelta = -3)),
            option("romance_suspicious", "Проверить профиль — аккаунт создан месяц назад", "🔍",
                next = "scam_romance_caught",
                fx = Effect(knowledgeDelta = 12)),
            option("romance_ignore", "Не отвечать — незнакомый человек", "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.romance")
                ))
        )
    )

    /** kz_2024 — WhatsApp "ошиблась номером" */
    private val romanceFirstContact = event(
        id = "scam_romance_contact",
        message = """
            Сегодня пришло странное сообщение в WhatsApp с незнакомого номера.
            «Привет, кажется ошиблась номером 😊 Ты знаешь кафе Арай на Достык?»

            Красивый профиль, фото из разных стран. Говорит, работает консультантом в Дубае.
            Написала уже три раза — интересуется, задаёт вопросы, очень внимательна.

            Наверное, просто одинокий человек. Или нет?
        """.trimIndent(),
        flavor = "💌",
        tags = setOf("scam", "scam.romance", "scam.crypto"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 200_000L),
            notFlag("learned.scam.romance"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("romance_respond", "Ответить — интересно, кто это", "💬",
                next = "scam_romance_buildup",
                fx = Effect(stressDelta = -3)),
            option("romance_suspicious", "Проверить номер и профиль на мошенничество", "🔍",
                next = "scam_romance_caught",
                fx = Effect(knowledgeDelta = 12)),
            option("romance_ignore", "Не отвечать — незнакомый номер", "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.romance")
                ))
        )
    )

    // ── Romance shared follow-ups ─────────────────────────────────────────────

    private val romanceBuildup = event(
        id = "scam_romance_buildup",
        message = """
            2 месяца общения. Понимает как никто.
            Умная, красивая по фото, интересно говорит о деньгах.

            «Мой дядя научил меня зарабатывать на платформе.
            Не предлагаю — просто посмотри скрин моего счёта.»

            На скрине — 4 800 000 тг. «Так и работает, если знать куда смотреть».
        """.trimIndent(),
        flavor = "💕",
        tags = setOf("scam.romance", "scam.crypto"),
        options = listOf(
            option("romance_interested", "«Расскажи подробнее» — интересно попробовать", "🤔",
                next = "scam_romance_crypto_intro",
                fx = Effect(stressDelta = 2, riskDelta = 5)),
            option("romance_red_flag", "Стоп — знакомый сразу переходит к деньгам?", "🚩",
                next = "scam_romance_caught",
                fx = Effect(knowledgeDelta = 15))
        )
    )

    private val romanceCryptoIntro = event(
        id = "scam_romance_crypto_intro",
        message = """
            Помогла зарегистрироваться на «платформе».
            Вложил 50 000 тг — в интерфейсе они выросли до 74 000 тг за неделю!

            «Видишь? Можно вывести в любой момент. Но лучше подержать —
            через месяц будет 150 000.» Предлагает вложить больше.
        """.trimIndent(),
        flavor = "📈",
        tags = setOf("scam.romance", "scam.crypto"),
        options = listOf(
            option("romance_more_money", "Вложить ещё 200 000 тг — цифры реально растут!", "💸",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -200_000, riskDelta = 20, stressDelta = 5,
                    scheduleEvent = ScheduledEvent("scam_romance_freeze", afterMonths = 2)
                )),
            option("romance_withdraw_test", "Попробовать вывести — проверить что деньги реальные", "🧪",
                next = "scam_romance_withdrawal_blocked",
                fx = Effect(knowledgeDelta = 15))
        )
    )

    private val romanceWithdrawalBlocked = event(
        id = "scam_romance_withdrawal_blocked",
        message = """
            При попытке вывода появилось сообщение:
            «Для верификации счёта необходимо уплатить налог 15% — 7 500 тг.»

            После уплаты — новое: «Для снятия более 50 000 требуется международная верификация — 50 000 тг».

            Объясняет: «Это временно, обязательные процедуры платформы».
        """.trimIndent(),
        flavor = "🚨",
        tags = setOf("scam.romance", "scam.crypto"),
        options = listOf(
            option("romance_pay_tax", "Заплатить «налог» — деньги ведь видны в интерфейсе", "😰",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -57_500, stressDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_romance_final", afterMonths = 1)
                )),
            option("romance_realize", "Подождать — это классический признак скама", "💡",
                next = "scam_romance_caught",
                fx = Effect(knowledgeDelta = 25))
        )
    )

    private val romanceFreeze = event(
        id = "scam_romance_freeze",
        message = """
            «Счёт заморожен регуляторными проверками. Временно.
            Для разморозки нужно пополнить до 500 000 тг — это требование платформы.»

            Уже 250 000 тг внутри системы. Вывести невозможно.
            Продолжает писать — ласково, с объяснениями.
        """.trimIndent(),
        flavor = "🔒",
        tags = setOf("scam.romance", "consequence"),
        options = listOf(
            option("romance_pay_more", "Пополнить ещё 250 000 — иначе потеряю всё вложенное", "😱",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -250_000, stressDelta = 25,
                    scheduleEvent = ScheduledEvent("scam_romance_final", afterMonths = 1)
                )),
            option("romance_stop_loss", "Остановиться. 250 000 — это потери. Не добавлять.", "✋",
                next = "scam_romance_caught",
                fx = Effect(knowledgeDelta = 20, stressDelta = 10))
        )
    )

    private val romanceFinal = event(
        id = "scam_romance_final",
        message = """
            Профиль удалён. Платформа недоступна. Номер заблокирован.

            Всё что было внесено — исчезло. Это «Pig Butchering» (Разделка свиньи):
            сначала «откармливают» доверием, потом «режут».

            Мошенники работают из организованных центров, у них скрипты и обучение.
        """.trimIndent(),
        flavor = "💔",
        tags = setOf("scam.romance", "consequence"),
        options = listOf(
            option("romance_report", "Сообщить в полицию. Изучить как работает схема.", "📖",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 35, stressDelta = 20,
                    setFlags = setOf("learned.scam.romance", "learned.scam.crypto", "lost_money_to_scam")
                ))
        )
    )

    private val romanceCaught = event(
        id = "scam_romance_caught",
        message = """
            Проверил профиль в базах мошенников — жалобы есть.
            Фото взяты со стоков. Номер не берёт трубку.

            Схема «романтический скам»: 2-3 месяца строят доверие,
            потом вводят «инвестиционную платформу».

            Тебя спасла осторожность.
        """.trimIndent(),
        flavor = "🔍",
        tags = setOf("scam.romance", "educational"),
        options = listOf(
            option("romance_safe", "Хорошо, что проверил. Буду внимательнее к незнакомцам онлайн.", "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 25, stressDelta = -5,
                    setFlags = setOf("learned.scam.romance")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  5. CRYPTO SCAM — фейковые биржи
    //  kz_2015 — ранний Bitcoin (не полностью crypto scam, скорее Forex)
    //  kz_2024 — Telegram, полноценный крипто-скам
    // ════════════════════════════════════════════════════════════════════

    /** kz_2015 — ранний Bitcoin / Forex-лохотрон */
    private val cryptoForex2015 = event(
        id = "scam_crypto_forex_2015",
        message = """
            На форуме прочитал про Bitcoin — говорят, за год вырос в 10 раз.
            Написал незнакомый в личку: «Помогу зайти в Forex — там легальный заработок».

            «Регистрируйся на платформе, закидывай 100 000 тг.
            Автоматическая торговля, доход 5% в неделю. Вывод когда угодно».

            Платформа выглядит серьёзно — графики, аналитика, английский интерфейс.
        """.trimIndent(),
        flavor = "📊",
        tags = setOf("scam", "scam.crypto"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            notFlag("learned.scam.crypto"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("crypto_exchange_invest", "Зарегистрироваться и завести 100 000 тг", "🚀",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, riskDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_crypto_withdrawal_trap", afterMonths = 2)
                )),
            option("crypto_exchange_check", "Проверить лицензию в реестре финрегулятора", "🔎",
                next = "scam_crypto_no_license",
                fx = Effect(knowledgeDelta = 10)),
            option("crypto_exchange_ignore", "Игнорировать — нелицензированные платформы опасны", "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.crypto")
                ))
        )
    )

    /** kz_2024 — Telegram канал с 52k подписчиков */
    private val cryptoFakeExchange = event(
        id = "scam_crypto_exchange",
        message = """
            Подписан на Telegram-канал с 52 000 подписчиками — аналитик даёт «сигналы».
            Первые 3 оказались верными. Теперь рекомендует новую биржу:

            «Регистрируйся, завози USDT — автоторговля даёт 8% в неделю.
            Вывод в любой момент. Минимум 50 000 тг. Проверено лично».

            52 тысячи людей читают — не может же всё это быть обманом?
        """.trimIndent(),
        flavor = "📊",
        tags = setOf("scam", "scam.crypto"),
        poolWeight = 15,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            notFlag("learned.scam.crypto"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("crypto_exchange_invest", "Зарегистрироваться и завести 100 000 тг", "🚀",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, riskDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_crypto_withdrawal_trap", afterMonths = 2)
                )),
            option("crypto_exchange_check", "Проверить лицензию биржи в реестре Нацбанка", "🔎",
                next = "scam_crypto_no_license",
                fx = Effect(knowledgeDelta = 10)),
            option("crypto_exchange_ignore", "Игнорировать — нелицензированные биржи опасны", "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.crypto")
                ))
        )
    )

    // ── Crypto shared follow-ups ──────────────────────────────────────────────

    private val cryptoNoLicense = event(
        id = "scam_crypto_no_license",
        message = """
            В реестре Нацбанка РК эта платформа отсутствует.
            В интернете — форумы с отзывами «не дают вывести деньги».

            Подписчиков в канале можно купить за 5 000 рублей.
            Три верных прогноза — «схема вилки», простая математика.

            Легальные площадки работают с реальными лицензиями.
        """.trimIndent(),
        flavor = "🔍",
        tags = setOf("scam.crypto", "educational"),
        options = listOf(
            option("crypto_lesson", "Понял. Проверять лицензию — обязательный шаг.", "✅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 22, stressDelta = -5,
                    setFlags = setOf("learned.scam.crypto")
                ))
        )
    )

    private val cryptoWithdrawalTrap = event(
        id = "scam_crypto_withdrawal_trap",
        message = """
            В интерфейсе баланс вырос — выглядит отлично.
            При попытке вывода: «Уплатите верификационный сбор — 15 000 тг».

            После уплаты: «Счёт помечен как подозрительный.
            Требуется депозит 200 000 тг для подтверждения личности».
        """.trimIndent(),
        flavor = "🚨",
        tags = setOf("scam.crypto", "consequence"),
        options = listOf(
            option("crypto_pay_more", "Заплатить 15 000 — может разблокируют", "😰",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -15_000, stressDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_crypto_final_disappear", afterMonths = 1)
                )),
            option("crypto_stop_now", "Остановиться. Это классическая ловушка.", "🛑",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 25, stressDelta = 15,
                    setFlags = setOf("learned.scam.crypto", "lost_money_to_scam")
                ))
        )
    )

    private val cryptoFinalDisappear = event(
        id = "scam_crypto_final_disappear",
        message = """
            Сайт исчез. Канал заблокирован.

            Схема — «фейковая биржа»:
            красивый интерфейс с растущими цифрами, но реальные деньги уходят мошенникам.
            При выводе появляются «налоги» и «верификации» без конца.

            Лицензия регулятора — первое что нужно проверять.
        """.trimIndent(),
        flavor = "💀",
        tags = setOf("scam.crypto", "consequence"),
        options = listOf(
            option("crypto_final_lesson", "Горький урок. Теперь знаю как проверять.", "📖",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 30, stressDelta = 15,
                    setFlags = setOf("learned.scam.crypto", "lost_money_to_scam")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  6. MFO — Микрофинансовые организации
    // ════════════════════════════════════════════════════════════════════

    /** kz_90s / kz_2005 — неформальный «ростовщик», сосед или знакомый */
    private val mfoNeighborLender90s = event(
        id = "scam_mfo_neighbor_90s",
        message = """
            Холодильник сломался. Нужно 50 000 тг срочно, а банки не дают без залога.

            Знакомый Рустем говорит: «Дам под 10% в месяц. Без бумаг, по-соседски.
            Через месяц вернёшь 55 000 — и всё, никаких проблем».

            10% в месяц — это 120% годовых. Но деньги нужны сегодня.
        """.trimIndent(),
        flavor = "🏚️",
        tags = setOf("scam", "scam.mfo", "debt"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, LTE, 100_000L),
            stat(STRESS, GTE, 40L),
            notFlag("learned.scam.mfo"),
            inEra("kz_90s")
        ),
        options = listOf(
            option("mfo_take_quick", "Взять у Рустема — быстро и удобно", "⚡",
                next = "scam_mfo_signed",
                fx = Effect(capitalDelta = 50_000, debtDelta = 50_000, stressDelta = -5)),
            option("mfo_read_contract", "Посчитать: 10% в месяц = 120% в год. Это очень много.", "📋",
                next = "scam_mfo_contract_revealed",
                fx = Effect(knowledgeDelta = 10)),
            option("mfo_call_bank", "Попробовать занять у родственников", "📞",
                next = MONTHLY_TICK,
                fx = Effect(
                    stressDelta = 5, knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.mfo")
                ))
        )
    )

    /** kz_2005 — первые МФО с физическими офисами */
    private val mfoOffice2005 = event(
        id = "scam_mfo_office_2005",
        message = """
            Холодильник сломался. Нужно 80 000 тг срочно.
            Банк рассматривает неделю. На улице вижу вывеску: «Микрокредит — 15 минут».

            Захожу. Менеджер улыбается: «Без справки, без залога.
            0% первый месяц! Потом 3% в день».

            3% в день = 1000% годовых. Но написано мелко.
        """.trimIndent(),
        flavor = "🏦",
        tags = setOf("scam", "scam.mfo", "debt"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, LTE, 100_000L),
            stat(STRESS, GTE, 40L),
            notFlag("learned.scam.mfo"),
            inEra("kz_2005")
        ),
        options = listOf(
            option("mfo_take_quick", "Взять в МФО — быстро и удобно", "⚡",
                next = "scam_mfo_signed",
                fx = Effect(capitalDelta = 80_000, debtDelta = 80_000, stressDelta = -5)),
            option("mfo_read_contract", "Прочитать договор полностью перед подписанием", "📋",
                next = "scam_mfo_contract_revealed",
                fx = Effect(knowledgeDelta = 10)),
            option("mfo_call_bank", "Подождать банк — МФО слишком дорогие", "📞",
                next = MONTHLY_TICK,
                fx = Effect(
                    stressDelta = 5, knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.mfo")
                ))
        )
    )

    /** kz_2015 / kz_2024 — онлайн МФО */
    private val mfoUrgentOnline = event(
        id = "scam_mfo_urgent",
        message = """
            Холодильник сломался. Всё содержимое пропало — 80 000 тг нужно срочно.

            В банке говорят 3-5 дней рассмотрения. МФО онлайн — одобрение за 15 минут.
            На сайте написано: «0% первые 30 дней!» — звучит неплохо.
            В самом низу мелким шрифтом: «после 30 дней — 2% в день».

            Мне сейчас не до мелкого шрифта. Или стоит прочитать?
        """.trimIndent(),
        flavor = "🏦",
        tags = setOf("scam", "scam.mfo", "debt"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, LTE, 100_000L),
            stat(STRESS, GTE, 40L),
            notFlag("learned.scam.mfo"),
            Condition.InEra("kz_2015")
        ),
        options = listOf(
            option("mfo_take_quick", "Взять в МФО — быстро и удобно", "⚡",
                next = "scam_mfo_signed",
                fx = Effect(capitalDelta = 80_000, debtDelta = 80_000, stressDelta = -5)),
            option("mfo_read_contract", "Прочитать договор полностью перед подписанием", "📋",
                next = "scam_mfo_contract_revealed",
                fx = Effect(knowledgeDelta = 10)),
            option("mfo_call_bank", "Позвонить в банк — попросить срочное рассмотрение", "📞",
                next = MONTHLY_TICK,
                fx = Effect(
                    stressDelta = 5, knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.mfo")
                ))
        )
    )

    private val mfoUrgent2024 = event(
        id = "scam_mfo_urgent_2024",
        message = """
            Холодильник сломался. Всё содержимое пропало — 80 000 тг нужно срочно.

            В банке говорят 3-5 дней рассмотрения. МФО онлайн — одобрение за 15 минут.
            На сайте написано: «0% первые 30 дней!» — звучит неплохо.
            В самом низу мелким шрифтом: «после 30 дней — 2% в день».

            Мне сейчас не до мелкого шрифта. Или стоит прочитать?
        """.trimIndent(),
        flavor = "🏦",
        tags = setOf("scam", "scam.mfo", "debt"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, LTE, 100_000L),
            stat(STRESS, GTE, 40L),
            notFlag("learned.scam.mfo"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("mfo_take_quick", "Взять в МФО — быстро и удобно", "⚡",
                next = "scam_mfo_signed",
                fx = Effect(capitalDelta = 80_000, debtDelta = 80_000, stressDelta = -5)),
            option("mfo_read_contract", "Прочитать договор полностью перед подписанием", "📋",
                next = "scam_mfo_contract_revealed",
                fx = Effect(knowledgeDelta = 10)),
            option("mfo_call_bank", "Позвонить в банк — попросить срочное рассмотрение", "📞",
                next = MONTHLY_TICK,
                fx = Effect(
                    stressDelta = 5, knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.mfo")
                ))
        )
    )

    // ── MFO shared follow-ups ─────────────────────────────────────────────────

    private val mfoContractRevealed = event(
        id = "scam_mfo_contract_revealed",
        message = """
            Прочитал договор. Вот что там на самом деле:

            • Ставка: 2% в день = 730% годовых (это легально!)
            • Штраф за просрочку: 0.5% от суммы в день дополнительно
            • После «бесплатного» периода: долг растёт как снежный ком

            Пример: взял 80 000 → не вернул вовремя → через 3 месяца долг 200 000+.

            МФО — только для крайних случаев с гарантированным быстрым возвратом.
        """.trimIndent(),
        flavor = "⚠️",
        tags = setOf("scam.mfo", "educational"),
        options = listOf(
            option("mfo_understand", "Понял. Лучше подождать банк или занять у родных.", "✅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 20,
                    setFlags = setOf("learned.scam.mfo")
                ))
        )
    )

    private val mfoSigned = event(
        id = "scam_mfo_signed",
        message = """
            Взял деньги. Проблему решил.

            Через месяц нет нужной суммы чтобы вернуть.
            Ставка переходит на 2% в день.
            Через месяц долг уже вырос в полтора раза.

            МФО звонит каждый день. Стресс растёт.
        """.trimIndent(),
        flavor = "😰",
        tags = setOf("scam.mfo", "consequence"),
        options = listOf(
            option("mfo_pay_urgently", "Срочно занять у родных и закрыть долг", "🚨",
                next = MONTHLY_TICK,
                fx = Effect(
                    debtDelta = -128_000, capitalDelta = -128_000,
                    stressDelta = -10, knowledgeDelta = 20,
                    setFlags = setOf("learned.scam.mfo")
                )),
            option("mfo_rollover", "Взять ещё в другом МФО чтобы закрыть этот", "💳",
                next = MONTHLY_TICK,
                fx = Effect(
                    debtDelta = 128_000, debtPaymentDelta = 15_000,
                    stressDelta = 20, riskDelta = 15
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  7. MIDDLEMAN / CHINA GOODS — Схема посредника
    // ════════════════════════════════════════════════════════════════════

    /** kz_90s — «челнок» из Турции */
    private val middlemanTurkey90s = event(
        id = "scam_middleman_turkey_90s",
        message = """
            Сосед Рустем возвращается из Турции раз в квартал с товаром.
            «Слушай, давай скинемся — я привезу одежду и технику.
            В 4 раза дешевле чем в магазине. Нужно 200 000 тг.
            Через 2 месяца привезу товар на 400 000».

            Никаких бумаг. «Зачем договор между соседями?»
            Рустем действительно ездит — вот его фото из Стамбула.
        """.trimIndent(),
        flavor = "✈️",
        tags = setOf("scam", "scam.middleman"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 200_000L),
            notFlag("learned.scam.middleman"),
            inEra("kz_90s")
        ),
        options = listOf(
            option("middleman_invest", "Вложить 200 000 тг — хорошая маржа!", "🏭",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -200_000, riskDelta = 20, stressDelta = 5,
                    scheduleEvent = ScheduledEvent("scam_middleman_result", afterMonths = 2)
                )),
            option("middleman_ask_contract", "Только с распиской — без бумаг не работаю", "📋",
                next = "scam_middleman_contract_refused",
                fx = Effect(knowledgeDelta = 10)),
            option("middleman_decline", "Отказать — слишком мутно без документов", "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.middleman")
                ))
        )
    )

    /** kz_2005 — ранний Alibaba + физический посредник */
    private val middlemanAlibaba2005 = event(
        id = "scam_middleman_alibaba_2005",
        message = """
            Знакомый говорит: «Нашёл сайт Alibaba — там товары с фабрик Китая.
            Давай скинемся, я организую доставку. 500 000 тг —
            через 2 месяца получишь товар на 800 000. Моя маржа 10%».

            Никакого договора. «Зачем договор, мы же знакомые?»
            Alibaba реальный сайт — это же не мошенничество?
        """.trimIndent(),
        flavor = "📦",
        tags = setOf("scam", "scam.middleman"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 300_000L),
            notFlag("learned.scam.middleman"),
            inEra("kz_2005")
        ),
        options = listOf(
            option("middleman_invest", "Вложить 500 000 тг — хорошая маржа!", "🏭",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -500_000, riskDelta = 20, stressDelta = 5,
                    scheduleEvent = ScheduledEvent("scam_middleman_result", afterMonths = 2)
                )),
            option("middleman_ask_contract", "Только с договором — без бумаг не работаю", "📋",
                next = "scam_middleman_contract_refused",
                fx = Effect(knowledgeDelta = 10)),
            option("middleman_decline", "Отказать — слишком мутно без документов", "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.middleman")
                ))
        )
    )

    /** kz_2015 — AliExpress / популярный e-commerce */
    private val middlemanAli2015 = event(
        id = "scam_middleman_ali_2015",
        message = """
            Знакомый Канат написал в WhatsApp:
            «Закупаю напрямую с фабрик AliExpress — в 3 раза дешевле магазина.
            Вложи 500 000 тг — через 2 месяца получишь товар на 800 000. Моя маржа 10%.»

            Я спросил про договор. «Зачем договор, мы же знакомые? Обидел».
            Хм. Мы знакомые — но не настолько близкие.
        """.trimIndent(),
        flavor = "📦",
        tags = setOf("scam", "scam.middleman"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 300_000L),
            notFlag("learned.scam.middleman"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("middleman_invest", "Вложить 500 000 тг — хорошая маржа!", "🏭",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -500_000, riskDelta = 20, stressDelta = 5,
                    scheduleEvent = ScheduledEvent("scam_middleman_result", afterMonths = 2)
                )),
            option("middleman_ask_contract", "Только с договором — без бумаг не работаю", "📋",
                next = "scam_middleman_contract_refused",
                fx = Effect(knowledgeDelta = 10)),
            option("middleman_decline", "Отказать — слишком мутно без документов", "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.middleman")
                ))
        )
    )

    /** kz_2024 — Telegram/Instagram перекуп */
    private val middlemanChina2024 = event(
        id = "scam_middleman_china",
        message = """
            Знакомый Канат написал с предложением «партнёрства».

            «Закупаю товары прямо с китайских фабрик — в 3 раза дешевле магазина.
            Вложи 500 000 тг — через 2 месяца получишь товар на 800 000. Моя маржа 10%.»

            Я спросил про договор. «Зачем договор, мы же знакомые? Обидел».
            Хм. Мы знакомые — но не настолько близкие.
        """.trimIndent(),
        flavor = "📦",
        tags = setOf("scam", "scam.middleman"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 300_000L),
            notFlag("learned.scam.middleman"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("middleman_invest", "Вложить 500 000 тг — хорошая маржа!", "🏭",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -500_000, riskDelta = 20, stressDelta = 5,
                    scheduleEvent = ScheduledEvent("scam_middleman_result", afterMonths = 2)
                )),
            option("middleman_ask_contract", "Только с договором — без бумаг не работаю", "📋",
                next = "scam_middleman_contract_refused",
                fx = Effect(knowledgeDelta = 10)),
            option("middleman_decline", "Отказать — слишком мутно без документов", "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.middleman")
                ))
        )
    )

    // ── Middleman shared follow-ups ───────────────────────────────────────────

    private val middlemanContractRefused = event(
        id = "scam_middleman_contract_refused",
        message = """
            «Зачем договор между друзьями? Не доверяешь что ли?»

            Давление через доверие — классический признак мошенничества.
            Любая легальная сделка имеет документальное подтверждение.

            Если человек отказывается от договора — он скрывает что-то важное.
        """.trimIndent(),
        flavor = "🚩",
        tags = setOf("scam.middleman", "educational"),
        options = listOf(
            option("middleman_lesson", "Правильно поступил. Договор защищает обе стороны.", "✅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 18, stressDelta = -5,
                    setFlags = setOf("learned.scam.middleman")
                ))
        )
    )

    private val middlemanResult = event(
        id = "scam_middleman_result",
        message = """
            Прошло 2 месяца. Товар не пришёл.
            Сначала отвечал: «Задержка на таможне».
            Потом: «Форс-мажор, нужно ещё 100 000 на доп. расходы».
            Теперь не берёт трубку.

            Деньги потеряны. Договора нет — суд не поможет.
        """.trimIndent(),
        flavor = "💀",
        tags = setOf("scam.middleman", "consequence"),
        options = listOf(
            option("middleman_loss_lesson", "Деньги потеряны. Урок: любая сделка — только с договором.", "📖",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 28, stressDelta = 20,
                    setFlags = setOf("learned.scam.middleman", "lost_money_to_scam")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  8. TRAINING CULT — Тренинги-секты
    // ════════════════════════════════════════════════════════════════════

    /** kz_90s — семинар по книге Кийосаки / начало тренинговой культуры */
    private val trainingKiyosaki90s = event(
        id = "scam_training_kiyosaki_90s",
        message = """
            На улице дали листовку: «Семинар Роберта Кийосаки в Алматы.
            Бесплатно. Научим зарабатывать как богатые!»

            Зал на 200 человек. Харизматичный ведущий, переводчик.
            Три часа про активы, пассивы, мышление богатых.

            В конце: «Двухдневный интенсив — 50 000 тг. Только сегодня скидка 50%.
            Завтра цена удвоится».
        """.trimIndent(),
        flavor = "📚",
        tags = setOf("scam", "scam.training"),
        poolWeight = 8,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            notFlag("learned.scam.training"),
            inEra("kz_90s")
        ),
        options = listOf(
            option("training_pay", "Заплатить 50 000 тг — «инвестиция в себя»", "💳",
                next = "scam_training_first_level",
                fx = Effect(capitalDelta = -50_000, stressDelta = -5, knowledgeDelta = 3)),
            option("training_research", "Поискать отзывы о компании в газетах", "🔍",
                next = "scam_training_reviews",
                fx = Effect(knowledgeDelta = 8)),
            option("training_decline", "Отказать — решения под давлением опасны", "🙅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.training")
                ))
        )
    )

    /** kz_2005 — «Бизнес Молодость» прообраз, корпоративные тренинги */
    private val trainingBusiness2005 = event(
        id = "scam_training_business_2005",
        message = """
            Коллега рассказал про тренинг «Деньги и Успех».
            «Я там изменился! Харизматичный тренер, практики, командная работа.
            Бесплатное вводное занятие в субботу. Просто сходи».

            Зашёл. Три часа энергии. В конце:
            «Базовый курс — 80 000 тг. Только для записавшихся сегодня — скидка 30%.
            Завтра такой цены не будет».
        """.trimIndent(),
        flavor = "🧘",
        tags = setOf("scam", "scam.training"),
        poolWeight = 8,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 80_000L),
            notFlag("learned.scam.training"),
            inEra("kz_2005")
        ),
        options = listOf(
            option("training_pay", "Заплатить 80 000 тг — «инвестиция в себя»", "💳",
                next = "scam_training_first_level",
                fx = Effect(capitalDelta = -80_000, stressDelta = -5, knowledgeDelta = 3)),
            option("training_research", "Погуглить компанию и отзывы перед оплатой", "🔍",
                next = "scam_training_reviews",
                fx = Effect(knowledgeDelta = 8)),
            option("training_decline", "Отказать — решение под давлением опасно", "🙅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.training")
                ))
        )
    )

    /** kz_2015 — «Бизнес Молодость», Instagram-коучи */
    private val trainingBm2015 = event(
        id = "scam_training_bm_2015",
        message = """
            В Instagram увидел рекламу: «Бизнес Молодость — Алматы».
            Друг в WhatsApp: «Я там был, реально огонь! Сходи на бесплатный вебинар».

            Вебинар онлайн — час мотивации, истории успеха.
            В конце: «Живой интенсив — 80 000 тг. Только сегодня со скидкой 40%».

            Я почувствовал подъём. Но почему именно сегодня?
        """.trimIndent(),
        flavor = "🔥",
        tags = setOf("scam", "scam.training"),
        poolWeight = 11,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 80_000L),
            notFlag("learned.scam.training"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("training_pay", "Заплатить 80 000 тг — «инвестиция в себя»", "💳",
                next = "scam_training_first_level",
                fx = Effect(capitalDelta = -80_000, stressDelta = -5, knowledgeDelta = 3)),
            option("training_research", "Погуглить компанию и отзывы перед оплатой", "🔍",
                next = "scam_training_reviews",
                fx = Effect(knowledgeDelta = 8)),
            option("training_decline", "Отказать — хорошего бесплатно не бывает", "🙅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.training")
                ))
        )
    )

    /** kz_2024 — Telegram/TikTok коучи */
    private val trainingCult2024 = event(
        id = "scam_training_cult",
        message = """
            Руслан позвонил и долго уговаривал сходить на тренинг «Деньги и Ты».

            «Я там так изменился! Харизматичный спикер, мощная атмосфера. Бесплатно, просто сходи».
            Пошёл. Три часа энергии, упражнений, истории успеха — реально захватывает.

            В самом конце спикер говорит: «Следующий уровень — 80 000 тг.
            Но только сегодня со скидкой 40%. Завтра такой цены не будет».

            Я почувствовал эйфорию. Но почему решение нужно принять прямо сейчас?
        """.trimIndent(),
        flavor = "🧘",
        tags = setOf("scam", "scam.training"),
        poolWeight = 11,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 80_000L),
            notFlag("learned.scam.training"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("training_pay", "Заплатить 80 000 тг — «инвестиция в себя»", "💳",
                next = "scam_training_first_level",
                fx = Effect(capitalDelta = -80_000, stressDelta = -5, knowledgeDelta = 3)),
            option("training_research", "Погуглить компанию и отзывы перед оплатой", "🔍",
                next = "scam_training_reviews",
                fx = Effect(knowledgeDelta = 8)),
            option("training_decline", "Отказать — хорошего бесплатно не бывает", "🙅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.training")
                ))
        )
    )

    // ── Training shared follow-ups ────────────────────────────────────────────

    private val trainingReviews = event(
        id = "scam_training_reviews",
        message = """
            Нашёл форум с отзывами. Структура у всех одинаковая:
            — Бесплатный вводный
            — Платный базовый (80к)
            — «Продвинутый» (200к)
            — «Мастер» (500к+)
            — Волонтёрство на организацию новых тренингов

            Реального навыка не дают. Техники НЛП создают зависимость от группы.
        """.trimIndent(),
        flavor = "🔍",
        tags = setOf("scam.training", "educational"),
        options = listOf(
            option("training_research_lesson", "Спасибо интернету. Сначала проверяй — потом плати.", "✅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 20, stressDelta = -5,
                    setFlags = setOf("learned.scam.training")
                ))
        )
    )

    private val trainingFirstLevel = event(
        id = "scam_training_first_level",
        message = """
            3 дня интенсива. Слёзы, объятия, «прорывы».
            Ощущение: «Я другой человек!»

            В конце: «Базовый уровень — только первый шаг.
            Продвинутый модуль — 200 000 тг. Скидка только для тех, кто решит сейчас.»
        """.trimIndent(),
        flavor = "🔥",
        tags = setOf("scam.training"),
        options = listOf(
            option("training_escalate", "Заплатить 200 000 тг — чувствую что нужно продолжать", "💸",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -200_000, stressDelta = -5, knowledgeDelta = 2,
                    scheduleEvent = ScheduledEvent("scam_training_deeper", afterMonths = 2)
                )),
            option("training_stop", "Остановиться. Эмоциональный подъём ≠ реальная ценность.", "🛑",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 20, stressDelta = 5,
                    setFlags = setOf("learned.scam.training")
                ))
        )
    )

    private val trainingDeeper = event(
        id = "scam_training_deeper",
        message = """
            Прошло 2 месяца. Суммарные траты на тренинги значительные.
            Практических навыков — минимум.

            Зато теперь просят «помочь» — волонтёром на следующем тренинге.
            И конечно — привести друзей.

            Организация зарабатывает на обучении тебя быть их рекрутёром.
        """.trimIndent(),
        flavor = "💡",
        tags = setOf("scam.training", "consequence"),
        options = listOf(
            option("training_exit", "Выйти. Достаточно дорогой урок.", "🚪",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 25, stressDelta = 10,
                    setFlags = setOf("learned.scam.training", "lost_money_to_scam")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  EXPORT
    // ════════════════════════════════════════════════════════════════════

    val all: List<GameEvent> = listOf(
        // Pyramid — 4 era variants + shared follow-ups
        pyramidNeighbor90s, pyramidEmail2005, pyramidVk2015, pyramidFriendCall,
        pyramidAvoided, pyramidCollapse, pyramidSmallLoss,
        // MLM — 4 era variants + shared follow-ups
        mlmDoor90s, mlmPhone2005, mlmInstagram2015, mlmColleague,
        mlmPresentation, mlmConfronted, mlmMonthLater,
        // Capper — 3 era variants (no 90s) + shared follow-ups
        capperSms2005, capperVk2015, capperTelegram,
        capperExplained, capperLoses, bettingDeepLoss,
        // Romance — 2 era variants (2015+) + shared follow-ups
        romanceMamba2015, romanceFirstContact,
        romanceBuildup, romanceCryptoIntro, romanceWithdrawalBlocked,
        romanceFreeze, romanceFinal, romanceCaught,
        // Crypto — 2 era variants (2015+) + shared follow-ups
        cryptoForex2015, cryptoFakeExchange,
        cryptoNoLicense, cryptoWithdrawalTrap, cryptoFinalDisappear,
        // MFO — 4 era variants + shared follow-ups
        mfoNeighborLender90s, mfoOffice2005, mfoUrgentOnline, mfoUrgent2024,
        mfoContractRevealed, mfoSigned,
        // Middleman — 4 era variants + shared follow-ups
        middlemanTurkey90s, middlemanAlibaba2005, middlemanAli2015, middlemanChina2024,
        middlemanContractRefused, middlemanResult,
        // Training — 4 era variants + shared follow-ups
        trainingKiyosaki90s, trainingBusiness2005, trainingBm2015, trainingCult2024,
        trainingReviews, trainingFirstLevel, trainingDeeper
    )

    /**
     * Pool entries — all era variants included.
     * Era conditions on each event act as the gate;
     * EraDefinition.poolWeightModifiers provide further suppression (e.g. crypto=0 in 90s).
     */
    val poolEntries: List<PoolEntry> = listOf(
        // Pyramid
        PoolEntry("scam_pyramid_neighbor_90s", baseWeight = 18),
        PoolEntry("scam_pyramid_email_2005",   baseWeight = 18),
        PoolEntry("scam_pyramid_vk_2015",      baseWeight = 18),
        PoolEntry("scam_pyramid_friend",       baseWeight = 18),
        // MLM
        PoolEntry("scam_mlm_door_90s",         baseWeight = 14),
        PoolEntry("scam_mlm_phone_2005",       baseWeight = 14),
        PoolEntry("scam_mlm_instagram_2015",   baseWeight = 14),
        PoolEntry("scam_mlm_colleague",        baseWeight = 14),
        // Capper (no 90s)
        PoolEntry("scam_capper_sms_2005",      baseWeight = 10),
        PoolEntry("scam_capper_vk_2015",       baseWeight = 12),
        PoolEntry("scam_capper_telegram",      baseWeight = 12),
        // Romance (2015+)
        PoolEntry("scam_romance_mamba_2015",   baseWeight = 8),
        PoolEntry("scam_romance_contact",      baseWeight = 10),
        // Crypto (2015+)
        PoolEntry("scam_crypto_forex_2015",    baseWeight = 10),
        PoolEntry("scam_crypto_exchange",      baseWeight = 15),
        // MFO
        PoolEntry("scam_mfo_neighbor_90s",     baseWeight = 12),
        PoolEntry("scam_mfo_office_2005",      baseWeight = 12),
        PoolEntry("scam_mfo_urgent",           baseWeight = 12),
        PoolEntry("scam_mfo_urgent_2024",      baseWeight = 12),
        // Middleman
        PoolEntry("scam_middleman_turkey_90s", baseWeight = 10),
        PoolEntry("scam_middleman_alibaba_2005", baseWeight = 10),
        PoolEntry("scam_middleman_ali_2015",   baseWeight = 10),
        PoolEntry("scam_middleman_china",      baseWeight = 10),
        // Training
        PoolEntry("scam_training_kiyosaki_90s", baseWeight = 8),
        PoolEntry("scam_training_business_2005", baseWeight = 8),
        PoolEntry("scam_training_bm_2015",     baseWeight = 11),
        PoolEntry("scam_training_cult",        baseWeight = 11)
    )

    fun findById(id: String): GameEvent? = all.find { it.id == id }
}
