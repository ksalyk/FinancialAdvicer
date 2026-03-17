package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.*
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.*

// ════════════════════════════════════════════════════════════════════
//  ERA DATA STRUCTURES
// ════════════════════════════════════════════════════════════════════

/**
 * Defines a historical period with:
 * - Global crises that fire at specific real-world dates
 * - Pool weight modifiers that shift which events are more/less likely in this era
 *
 * Weight modifier keys can be either an event id or a tag (e.g. "scam.pyramid").
 * When both match, the higher multiplier wins.
 */
data class EraDefinition(
    val id: String,
    val name: String,
    val startYear: Int,
    val endYear: Int,
    /** Events that fire on a specific year/month in game time. */
    val globalEvents: List<EraGlobalEvent> = emptyList(),
    /**
     * Multipliers applied to pool event weights.
     * Key = eventId OR tag string (e.g. "scam.pyramid", "scam.crypto", "career").
     * Value = multiplier (1.0 = no change, 2.0 = double, 0.0 = disabled).
     */
    val poolWeightModifiers: Map<String, Float> = emptyMap()
)

/** A global crisis or world event scheduled for a specific in-game date. */
data class EraGlobalEvent(
    val eventId: String,
    val year: Int,
    val month: Int = 1,
    /** Probability 0.0–1.0. Use < 1.0 for events that don't hit every player. */
    val probability: Float = 1.0f
)

// ════════════════════════════════════════════════════════════════════
//  ERA EVENT LIBRARY  — global crises referenced by EraDefinition
// ════════════════════════════════════════════════════════════════════

object EraEventLibrary {

    val all: List<GameEvent> = listOf(

        // ── KZ 90s ────────────────────────────────────────────────────

        GameEvent(
            id = "era_ussr_collapse",
            message = """
                Декабрь 1991. СССР распался.

                Банки заморозили вклады. Гиперинфляция — деньги обесцениваются
                каждую неделю. Половина предприятий встала.

                Всё, что было в сберкассе — сгорело.
            """.trimIndent(),
            flavor = "🏚️",
            tags = setOf("crisis", "era.kz_90s"),
            options = listOf(
                GameOption("barter_survive", "Перейти на натуральный обмен — еда важнее денег", "🥖",
                    effects = Effect(capitalDelta = -50_000, stressDelta = 20, knowledgeDelta = 10),
                    next = MONTHLY_TICK),
                GameOption("hard_currency", "Срочно перевести остатки в доллары", "💵",
                    effects = Effect(capitalDelta = -30_000, stressDelta = 10, knowledgeDelta = 15),
                    next = MONTHLY_TICK)
            )
        ),

        GameEvent(
            id = "era_tenge_introduced",
            message = """
                Ноябрь 1993. Казахстан ввёл собственную валюту — тенге.

                Советские рубли обменивают по курсу 500:1.
                Очереди у обменников, у людей паника и усталость.

                У тебя есть наличные рубли — что делаешь?
            """.trimIndent(),
            flavor = "💴",
            tags = setOf("crisis", "era.kz_90s"),
            options = listOf(
                GameOption("exchange_all", "Обменять всё немедленно — пока есть лимит", "🏃",
                    effects = Effect(
                        stressDelta = -5,
                        knowledgeDelta = 8,
                        monetaryReform = MonetaryReform(
                            from = CurrencyCode.RUB,
                            to = CurrencyCode.KZT,
                            numerator = 1,
                            denominator = 500
                        )
                    ),
                    next = MONTHLY_TICK),
                GameOption("wait_see", "Подождать — может курс улучшится", "⏳",
                    effects = Effect(
                        capitalDelta = -30_000,
                        stressDelta = 15,
                        monetaryReform = MonetaryReform(
                            from = CurrencyCode.RUB,
                            to = CurrencyCode.KZT,
                            numerator = 1,
                            denominator = 500
                        )
                    ),
                    next = MONTHLY_TICK)
            )
        ),

        GameEvent(
            id = "era_mmm_wave_90s",
            message = """
                1994. По всему СНГ рекламируют МММ — Сергей Мавроди обещает 1000% в месяц.
                Телевизор, газеты — везде Лёня Голубков.
                «Я не халявщик, я партнёр!»

                Сосед вложил всю зарплату и уже «заработал» в 3 раза больше.
                Тебе предлагают вложить тоже.
            """.trimIndent(),
            flavor = "📺",
            tags = setOf("scam", "scam.pyramid", "era.kz_90s"),
            unique = true,
            poolWeight = 25,
            options = listOf(
                GameOption("invest_mmm", "Вложить — все вокруг зарабатывают", "💸",
                    effects = Effect(
                        capitalDelta = -100_000, stressDelta = 5, riskDelta = 25,
                        scheduleEvent = ScheduledEvent("era_mmm_collapse", afterMonths = 4)
                    ),
                    next = MONTHLY_TICK),
                GameOption("skeptical", "Отказаться — слишком хорошо, чтобы быть правдой", "🤔",
                    effects = Effect(
                        knowledgeDelta = 20, stressDelta = -5,
                        setFlags = setOf("learned.scam.pyramid")
                    ),
                    next = "era_mmm_skeptic_result")
            )
        ),

        GameEvent(
            id = "era_mmm_collapse",
            message = """
                МММ рухнула. Мавроди арестован.
                Акции стоят ноль. Деньги исчезли.

                Десятки миллионов людей потеряли сбережения.
                Ты один из них.

                Это был главный урок 90-х: «бесплатного сыра не бывает».
            """.trimIndent(),
            flavor = "💀",
            tags = setOf("scam.pyramid", "consequence", "era.kz_90s"),
            options = listOf(
                GameOption("accept_lesson", "Принять урок. Больше никаких пирамид.", "📚",
                    effects = Effect(
                        knowledgeDelta = 30, stressDelta = 15,
                        setFlags = setOf("learned.scam.pyramid", "lost_money_to_scam")
                    ),
                    next = MONTHLY_TICK)
            )
        ),

        GameEvent(
            id = "era_mmm_skeptic_result",
            message = """
                МММ рухнула через 4 месяца. Миллионы потеряли всё.

                Твой сосед в шоке — он вложил всё что было.
                Ты сохранил деньги, потому что не поддался ажиотажу.

                Осторожность стоила тебе упущенной прибыли — но спасла капитал.
            """.trimIndent(),
            flavor = "🛡️",
            tags = setOf("era.kz_90s"),
            options = listOf(
                GameOption("be_grateful", "Хорошо, что не вложился. Запомню этот урок.", "🙏",
                    effects = Effect(knowledgeDelta = 10, stressDelta = -5),
                    next = MONTHLY_TICK)
            )
        ),

        // ── KZ Devaluation 2015 ───────────────────────────────────────

        GameEvent(
            id = "era_devaluation_2015",
            message = """
                20 августа 2015. Нацбанк объявил о переходе на свободный курс тенге.

                За один день доллар вырос с 188 до 256 тенге.
                Тенге упал на 36%.

                У тебя сбережения в тенге. Они только что потеряли треть реальной стоимости.
            """.trimIndent(),
            flavor = "📉",
            tags = setOf("crisis", "era.kz_2015"),
            unique = true,
            options = listOf(
                GameOption("convert_to_dollar", "Срочно купить доллары — пока не поздно", "💵",
                    effects = Effect(stressDelta = 10, knowledgeDelta = 15),
                    next = MONTHLY_TICK),
                GameOption("keep_tenge", "Держать в тенге — может отыграется", "🤞",
                    effects = Effect(capitalDelta = -100_000, stressDelta = 20),
                    next = MONTHLY_TICK),
                GameOption("buy_property", "Купить что-то реальное — квартиру или технику", "🏠",
                    effects = Effect(capitalDelta = -200_000, stressDelta = 5, knowledgeDelta = 10),
                    next = MONTHLY_TICK)
            )
        ),

        // ── COVID 2020 ────────────────────────────────────────────────

        GameEvent(
            id = "era_covid_shock_2020",
            message = """
                Март 2020. Пандемия. Полный локдаун.

                Компании массово сокращают. Рестораны, магазины — всё закрыто.
                Тенге снова упал. Биржи рухнули на 30-40%.

                Те, у кого была подушка безопасности — пережили это спокойно.
                У тебя есть подушка?
            """.trimIndent(),
            flavor = "🦠",
            tags = setOf("crisis", "era.modern"),
            unique = true,
            options = listOf(
                GameOption("has_cushion_good", "Есть подушка — переживём спокойно", "🛡️",
                    effects = Effect(stressDelta = -10, knowledgeDelta = 15),
                    next = MONTHLY_TICK),
                GameOption("no_cushion_crisis", "Подушки нет — срочно ищу любую работу", "😱",
                    effects = Effect(incomeDelta = -100_000, stressDelta = 30, knowledgeDelta = 20),
                    next = MONTHLY_TICK),
                GameOption("buy_dip", "Рынок упал — покупаю ETF на просадке", "📈",
                    effects = Effect(
                        capitalDelta = -100_000, investmentsDelta = 100_000,
                        stressDelta = 5, knowledgeDelta = 20,
                        scheduleEvent = ScheduledEvent("era_covid_recovery_gain", afterMonths = 12)
                    ),
                    next = MONTHLY_TICK)
            )
        ),

        GameEvent(
            id = "era_covid_recovery_gain",
            message = """
                Год прошёл. Рынки отыграли падение и выросли.

                ETF, купленный на дне в марте 2020, сейчас стоит в 1.7 раза дороже.
                +70% за год — это лучший результат за последнее десятилетие.

                Кризис стал возможностью для тех, кто не запаниковал.
            """.trimIndent(),
            flavor = "🚀",
            tags = setOf("investment", "consequence"),
            options = listOf(
                GameOption("celebrate_wisdom", "Отличный результат. Держу дальше.", "🏆",
                    effects = Effect(
                        investmentsDelta = 70_000,
                        knowledgeDelta = 15, stressDelta = -10
                    ),
                    next = MONTHLY_TICK)
            )
        ),

        // ── KZ Devaluation 2022 ───────────────────────────────────────

        GameEvent(
            id = "era_kz_devaluation_2022",
            message = """
                Март 2022. Война в Украине. Санкции против России.
                Тенге упал на 20% за неделю.

                Всё импортное резко подорожало.
                Доллар теперь 470 тенге.

                Что делаешь с накоплениями?
            """.trimIndent(),
            flavor = "⚡",
            tags = setOf("crisis", "era.modern"),
            unique = true,
            options = listOf(
                GameOption("diversify_currency", "Разделить: 50% в долларах, 50% в тенге на расходы", "⚖️",
                    effects = Effect(stressDelta = -5, knowledgeDelta = 20),
                    next = MONTHLY_TICK),
                GameOption("panic_buy_dollar", "Купить все доллары по любому курсу", "😰",
                    effects = Effect(stressDelta = 15, knowledgeDelta = 5),
                    next = MONTHLY_TICK),
                GameOption("ignore_it", "Ничего не делать — разберётся само", "😶",
                    effects = Effect(capitalDelta = -80_000, stressDelta = 10),
                    next = MONTHLY_TICK)
            )
        )
    )

    fun findById(id: String): GameEvent? = all.find { it.id == id }
}

// ════════════════════════════════════════════════════════════════════
//  ERA REGISTRY — predefined eras used by ScenarioGraphFactory
// ════════════════════════════════════════════════════════════════════

object EraRegistry {

    val MODERN_KZ_2024 = EraDefinition(
        id = "modern_kz_2024",
        name = "Современный Казахстан",
        startYear = 2020,
        endYear = 2030,
        globalEvents = listOf(
            EraGlobalEvent("era_covid_shock_2020",    year = 2020, month = 3),
            EraGlobalEvent("era_kz_devaluation_2022", year = 2022, month = 3, probability = 0.85f)
        ),
        poolWeightModifiers = mapOf(
            "scam.crypto"     to 2.5f,   // crypto scams peak in this era
            "scam.pyramid"    to 1.5f,
            "scam.romance"    to 2.0f,   // pig butchering is modern
            "scam.betting"    to 1.8f,
            "scam.mlm"        to 1.3f,
            "career"          to 1.5f,
            "investment"      to 1.8f
        )
    )

    val KZ_90S = EraDefinition(
        id = "kz_90s",
        name = "Казахстан 90-х",
        startYear = 1991,
        endYear = 2000,
        globalEvents = listOf(
            EraGlobalEvent("era_ussr_collapse",  year = 1991, month = 12),
            EraGlobalEvent("era_tenge_introduced", year = 1993, month = 11),
            EraGlobalEvent("era_mmm_wave_90s",   year = 1994, month = 6, probability = 0.9f),
            EraGlobalEvent("chechen_war_broadcast", year = 1994, month = 12),
            EraGlobalEvent("nuclear_disarmament_reaction", year = 1995, month = 4),
            EraGlobalEvent("capital_move_debate", year = 1997, month = 12)
        ),
        poolWeightModifiers = mapOf(
            "scam.pyramid" to 4.0f,    // MMM era — rampant
            "scam.crypto"  to 0.0f,    // doesn't exist yet
            "scam.romance" to 0.0f,    // no internet yet
            "crisis"       to 3.0f,
            "scam.mlm"     to 2.0f     // Amway/Oriflame wave just starting
        )
    )

    val KZ_2015_DEVALUATION = EraDefinition(
        id = "kz_2015",
        name = "Казахстан 2015",
        startYear = 2014,
        endYear = 2019,
        globalEvents = listOf(
            EraGlobalEvent("era_devaluation_2015", year = 2015, month = 8)
        ),
        poolWeightModifiers = mapOf(
            "scam.pyramid" to 2.0f,
            "scam.mlm"     to 2.5f,
            "crisis"       to 2.0f,
            "scam.crypto"  to 0.5f  // early crypto, not mainstream yet
        )
    )

    private val all = listOf(MODERN_KZ_2024, KZ_90S, KZ_2015_DEVALUATION)

    fun findById(eraId: String): EraDefinition? = all.find { it.id == eraId }
}
