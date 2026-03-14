package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.*
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.*

/**
 * Shared library of financial scam and fraud events.
 * Included in every character's event pool.
 *
 * Each scam has:
 *  - A trigger event (gated by capital + NotFlag so it fires when player has money to lose)
 *  - One or more follow-up events (if player engages)
 *  - Deferred consequence events (bad outcomes that fire months later via ScheduledEvent)
 *  - An "avoided" branch that sets a learned flag, suppressing future similar scams
 *
 * Flag conventions:
 *  - "learned.scam.X"   — player understands this scam type, pool weight drops 85%
 *  - "lost_money_to_scam" — generic flag, any scam weight drops further
 *  - "in_mlm"           — player is currently in an MLM, enables escalation events
 *  - "in_betting_debt"  — player owes money from betting losses
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

    // ════════════════════════════════════════════════════════════════════
    //  1. PYRAMID SCHEME (Финансовые пирамиды)
    // ════════════════════════════════════════════════════════════════════

    private val pyramidFriendCall = event(
        id = "scam_pyramid_friend",
        message = """
            Звонок от Асана в 10 вечера.

            «Слушай, я нашёл кое-что. Мне уже пришло 80 000 тг за месяц.
            Мама тоже вложила. Гарантия 100%. Нужно минимум 100 000 тг.
            Я за тебя отвечаю, ты же мне доверяешь? 🤑»
        """.trimIndent(),
        flavor = "😰",
        tags = setOf("scam", "scam.pyramid", "social.friend"),
        poolWeight = 18,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 100_000L),
            Condition.NotFlag("learned.scam.pyramid")
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

    private val pyramidAvoided = event(
        id = "scam_pyramid_avoided",
        message = """
            Асан не смог объяснить, откуда берётся доход.
            «Ну... там система, там умные люди занимаются...»

            Ты погуглил: это классическая финансовая пирамида.
            Первые участники реально получают деньги — от новых вкладчиков.
            Математика неизбежно ведёт к коллапсу.

            Через 3 месяца Асан написал: «Ты был прав. Всё рухнуло.»
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
            Пирамида рухнула. Асан не берёт трубку.
            100 000 тенге потеряны — навсегда.

            Организаторы уже в другом городе.
            Ты понял: настоящие инвестиции не требуют привлекать новых людей.
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
    //  2. MLM (Сетевой маркетинг)
    // ════════════════════════════════════════════════════════════════════

    private val mlmColleague = event(
        id = "scam_mlm_colleague",
        message = """
            Коллега по работе изменилась — светится, похудела.
            «Я нашла удивительные продукты! И ещё я теперь предприниматель.
            Приходи на встречу в пятницу — просто посмотришь, ничего не подписываешь.»
        """.trimIndent(),
        flavor = "✨",
        tags = setOf("scam", "scam.mlm", "social.colleague"),
        poolWeight = 14,
        unique = true,
        conditions = listOf(
            Condition.NotFlag("learned.scam.mlm")
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

    private val mlmPresentation = event(
        id = "scam_mlm_presentation",
        message = """
            Встреча в кафе. Красивые слайды, истории успеха.
            «Пригласи двух — они пригласят по два — через год пассивный доход!»

            В конце: «Стартовый набор продукции — 35 000 тг. Только сегодня со скидкой.»
            Коллега смотрит с надеждой. Ты ей важен как «партнёр».
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

            Коллега, возможно, обидится. Но ты сберёг деньги и время.
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

            Продать удалось продукции на 8 000 тг.
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
    //  3. BETTING / CAPPER (Беттинг и капперы)
    // ════════════════════════════════════════════════════════════════════

    private val capperTelegram = event(
        id = "scam_capper_telegram",
        message = """
            В Telegram пишет незнакомец:
            «Дам 3 бесплатных прогноза. Убедись сам в моём уровне.
            Первый: Реал Мадрид сегодня победит Барсу.»

            Реал выиграл 2:1.
            Второй прогноз тоже сошёлся. Третий — тоже.

            «Видишь уровень? Платный доступ на месяц — 30 000 тг.»
        """.trimIndent(),
        flavor = "⚽",
        tags = setOf("scam", "scam.betting", "scam.capper"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            Condition.NotFlag("learned.scam.betting")
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

    private val capperExplained = event(
        id = "scam_capper_explained",
        message = """
            Схема называется «вилка прогнозов»:

            1000 человек → 500 получают «А выиграет», 500 — «Б выиграет».
            500 оказались правы → 500 отписались.
            Из 500 оставшихся снова делят пополам.
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

            30 000 тг потеряно. Дополнительно предлагают вложить ещё 100 000.
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
            Каппер исчез. Телеграм-канал удалён.
            130 000 тенге потеряно.

            Погоня за проигрышем — «chase losses» — это ловушка.
            Чем больше теряешь, тем сильнее желание «отыграться».
            Это называется игровая зависимость.
        """.trimIndent(),
        flavor = "💀",
        tags = setOf("scam.betting", "consequence"),
        options = listOf(
            option("betting_final_lesson", "Это было дорого. Никогда — ставки на деньги которые нужны.", "📖",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 30, stressDelta = 15,
                    setFlags = setOf("learned.scam.betting", "lost_money_to_scam")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  4. ROMANCE SCAM / PIG BUTCHERING (Романтический скам)
    // ════════════════════════════════════════════════════════════════════

    private val romanceFirstContact = event(
        id = "scam_romance_contact",
        message = """
            В WhatsApp сообщение от незнакомого номера:
            «Привет, кажется ошиблась номером 😊
            Ты знаешь кафе Арай на Достык?»

            Красивый профиль. Говорит, работает консультантом в Дубае.
            Очень внимательна. Пишет каждый день.
        """.trimIndent(),
        flavor = "💌",
        tags = setOf("scam", "scam.romance", "scam.crypto"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 200_000L),
            Condition.NotFlag("learned.scam.romance")
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

    private val romanceBuildup = event(
        id = "scam_romance_buildup",
        message = """
            2 месяца общения. Она понимает тебя как никто.
            Работает в консалтинге, умная, красивая по фото.

            «Мой дядя научил меня зарабатывать на крипте.
            Не предлагаю — просто посмотри скрин моего счёта.»

            На скрине — 4 800 000 тг. «Так и работает, если знать куда смотреть».
        """.trimIndent(),
        flavor = "💕",
        tags = setOf("scam.romance", "scam.crypto"),
        options = listOf(
            option("romance_interested", "«Расскажи подробнее» — интересно попробовать", "🤔",
                next = "scam_romance_crypto_intro",
                fx = Effect(stressDelta = 2, riskDelta = 5)),
            option("romance_red_flag", "Стоп — знакомый человек сразу переходит к деньгам?", "🚩",
                next = "scam_romance_caught",
                fx = Effect(knowledgeDelta = 15))
        )
    )

    private val romanceCryptoIntro = event(
        id = "scam_romance_crypto_intro",
        message = """
            Она показала «платформу» и помогла зарегистрироваться.
            Вложил 50 000 тг — в интерфейсе они выросли до 74 000 тг за неделю!

            «Видишь? Можно вывести в любой момент. Но лучше подержать — через месяц будет 150 000.»
            Предлагает вложить больше.
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

            Она объясняет: «Это временно, обязательные процедуры платформы».
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

            250 000 тг уже внутри системы. Вывести невозможно.
            Она продолжает писать — ласково, с объяснениями.
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

            Всё что было внесено — исчезло. Это называется «Pig Butchering» (Разделка свиньи):
            сначала «откармливают» доверием, потом «режут».

            Мошенники работают из организованных центров в Юго-Восточной Азии,
            у них есть скрипты, менеджеры и обучение.
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
            Проверил номер в базах мошенников — там 47 жалоб.
            Обратный звонок не берут. Фото взяты со стоков.

            Схема называется «романтический скам» или Pig Butchering.
            Мошенники 2-3 месяца строят доверие, потом вводят «инвестиционную платформу».

            Тебя спасла осторожность.
        """.trimIndent(),
        flavor = "🔍",
        tags = setOf("scam.romance", "educational"),
        options = listOf(
            option("romance_safe", "Хорошо что проверил. Буду внимательнее к незнакомцам онлайн.", "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 25, stressDelta = -5,
                    setFlags = setOf("learned.scam.romance")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  5. CRYPTO SCAM (фейковые биржи, rug pull)
    // ════════════════════════════════════════════════════════════════════

    private val cryptoFakeExchange = event(
        id = "scam_crypto_exchange",
        message = """
            В Telegram-канале (52 000 подписчиков) — аналитик даёт «сигналы».
            Первые 3 были верными. Теперь рекомендует новую биржу:

            «Регистрируйся, завози USDT — там автоторговля даёт 8% в неделю.
            Вывод в любой момент. Минимум — 50 000 тг».
        """.trimIndent(),
        flavor = "📊",
        tags = setOf("scam", "scam.crypto"),
        poolWeight = 15,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            Condition.NotFlag("learned.scam.crypto")
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

    private val cryptoNoLicense = event(
        id = "scam_crypto_no_license",
        message = """
            В реестре Нацбанка РК эта биржа отсутствует.
            В Google — несколько форумов с отзывами «не дают вывести деньги».

            Telegram-канал с 52 000 подписчиков? Их можно купить за 5 000 рублей.
            Три верных прогноза? Это называется «схема вилки» — простая математика.

            Легальные биржи: Binance, Bybit, KASE — с реальными лицензиями.
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
            В интерфейсе баланс вырос до 147 000 тг — отлично!
            При попытке вывода: «Для разблокировки вывода необходимо уплатить
            верификационный сбор — 15 000 тг».

            После уплаты: «Ваш счёт помечен как подозрительный.
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
            Сайт исчез. Telegram заблокирован. 115 000 тенге потеряно.

            Схема называется «фейковая биржа»:
            — Красивый интерфейс с растущими цифрами
            — Реальные деньги идут прямо мошенникам
            — При выводе появляются «налоги» и «верификации»

            Лицензия Нацбанка — первое что нужно проверять.
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
    //  6. MFO DEBT TRAP (Микрофинансовые организации)
    // ════════════════════════════════════════════════════════════════════

    private val mfoUrgentLoan = event(
        id = "scam_mfo_urgent",
        message = """
            Экстренная ситуация: сломался холодильник. Нужно 80 000 тг срочно.
            Банк рассматривает 3-5 дней. МФО онлайн — одобрение за 15 минут.

            Условия на сайте: «0% первые 30 дней!»
            Мелкий шрифт: после 30 дней — 2% в день.
        """.trimIndent(),
        flavor = "🏦",
        tags = setOf("scam", "scam.mfo", "debt"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, LTE, 100_000L),
            stat(STRESS, GTE, 40L),
            Condition.NotFlag("learned.scam.mfo")
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

    private val mfoContractRevealed = event(
        id = "scam_mfo_contract_revealed",
        message = """
            Прочитал договор. Вот что там на самом деле:

            • Ставка: 2% в день = 730% годовых (это легально!)
            • Штраф за просрочку: 0.5% от суммы в день дополнительно
            • Через 30 дней «бесплатного» периода: долг растёт как снежный ком

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
            Взял 80 000 тг. Холодильник куплен.

            Через месяц нет 80 000 чтобы вернуть.
            Ставка переходит на 2% в день.
            Через месяц долг уже 128 000 тг.

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
    //  7. MIDDLEMAN / CHINA GOODS (Схема посредника)
    // ════════════════════════════════════════════════════════════════════

    private val middlemanChinaGoods = event(
        id = "scam_middleman_china",
        message = """
            Знакомый предлагает бизнес:
            «Помогу закупить товар с китайской фабрики.
            В 3 раза дешевле чем в магазине! Вложи 500 000 тг —
            через 2 месяца получишь товар на 800 000 тг.»

            Никакого договора. «Зачем договор, мы же знакомые?»
        """.trimIndent(),
        flavor = "📦",
        tags = setOf("scam", "scam.middleman"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 300_000L),
            Condition.NotFlag("learned.scam.middleman")
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
            Знакомый сначала отвечал: «Задержка на таможне».
            Потом: «Форс-мажор, нужно ещё 100 000 на доп. расходы».
            Теперь не берёт трубку.

            500 000 тенге потеряно. Договора нет — суд не поможет.
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
    //  8. TRAINING CULT / PERSONAL GROWTH SECT (Тренинги-секты)
    // ════════════════════════════════════════════════════════════════════

    private val trainingCultIntro = event(
        id = "scam_training_cult",
        message = """
            Друг зовёт на бесплатный тренинг:
            «"Деньги и Ты" — всё изменит. Я там так изменился!
            Харизматичный спикер, мощная атмосфера. Просто сходи.»

            Бесплатно. Отличная атмосфера. В конце:
            «Следующий уровень — только 80 000 тг. Но только сегодня со скидкой 40%.»
        """.trimIndent(),
        flavor = "🧘",
        tags = setOf("scam", "scam.training"),
        poolWeight = 11,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 80_000L),
            Condition.NotFlag("learned.scam.training")
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

    private val trainingReviews = event(
        id = "scam_training_reviews",
        message = """
            Нашёл форум с отзывами. Структура у всех одинаковая:
            — Бесплатный вводный
            — Платный базовый (80к)
            — «Продвинутый» (200к)
            — «Мастер» (500к+)
            — Волонтёрство на организацию новых тренингов

            Реального навыка или знания не дают.
            Техники НЛП создают эмоциональную зависимость от группы.
            Судебные иски в России 2022-2024.
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
            Продвинутый модуль — 200 000 тг. Без него ты остановишься на полпути.
            Скидка только для тех, кто решит сейчас.»
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
            Прошло 2 месяца. Суммарные траты на тренинги: 280 000 тг.
            Практических навыков — минимум.

            Зато теперь тебя просят «помочь» — волонтёром на следующем тренинге.
            И конечно — привести друзей, «им это так поможет».

            Организация зарабатывает на обучении ТЕБЯ быть их рекрутёром.
        """.trimIndent(),
        flavor = "💡",
        tags = setOf("scam.training", "consequence"),
        options = listOf(
            option("training_exit", "Выйти. 280 000 тг — достаточно дорогой урок.", "🚪",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 25, stressDelta = 10,
                    setFlags = setOf("learned.scam.training", "lost_money_to_scam")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  EXPORT — all events flat list for ScenarioGraph.findEvent()
    // ════════════════════════════════════════════════════════════════════

    val all: List<GameEvent> = listOf(
        // Pyramid
        pyramidFriendCall, pyramidAvoided, pyramidCollapse, pyramidSmallLoss,
        // MLM
        mlmColleague, mlmPresentation, mlmConfronted, mlmMonthLater,
        // Betting / Capper
        capperTelegram, capperExplained, capperLoses, bettingDeepLoss,
        // Romance scam
        romanceFirstContact, romanceBuildup, romanceCryptoIntro,
        romanceWithdrawalBlocked, romanceFreeze, romanceFinal, romanceCaught,
        // Crypto fake exchange
        cryptoFakeExchange, cryptoNoLicense, cryptoWithdrawalTrap, cryptoFinalDisappear,
        // MFO
        mfoUrgentLoan, mfoContractRevealed, mfoSigned,
        // Middleman
        middlemanChinaGoods, middlemanContractRefused, middlemanResult,
        // Training cult
        trainingCultIntro, trainingReviews, trainingFirstLevel, trainingDeeper
    )

    /** Events eligible for the random pool (trigger events only — not follow-ups). */
    val poolEntries: List<PoolEntry> = listOf(
        PoolEntry("scam_pyramid_friend",    18),
        PoolEntry("scam_mlm_colleague",     14),
        PoolEntry("scam_capper_telegram",   12),
        PoolEntry("scam_romance_contact",   10),
        PoolEntry("scam_crypto_exchange",   15),
        PoolEntry("scam_mfo_urgent",        12),
        PoolEntry("scam_middleman_china",   10),
        PoolEntry("scam_training_cult",     11)
    )

    fun findById(id: String): GameEvent? = all.find { it.id == id }
}
