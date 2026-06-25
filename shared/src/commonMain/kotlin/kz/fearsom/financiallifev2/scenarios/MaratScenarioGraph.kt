package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.CAPITAL
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.DEBT
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.STRESS
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GT
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GTE
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.LTE
import kz.fearsom.financiallifev2.model.CurrencyCode
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.ScheduledEvent

/**
 * Марат «Нефть» — `marat_2015` · era `kz_2015` · Jan 2014 · KZT.
 *
 * Logline: 25, Атырау, вахтовик на нефтянке. Зарплата — мечта региона.
 * Чувствует себя непробиваемым. Ербол зовёт брать Prado в кредит. Динара
 * прислала ссылку на свадебный зал за 2 миллиона.
 *
 * Lesson (Bible §7): lifestyle inflation & concentration risk — rising income ≠ wealth;
 * 100% dependence on one sector + credit = fragility when oil turns.
 * Secondary: consumer credit, FX debt.
 *
 * Architecture:
 *  - Act I is a scripted chain (direct `next`) — hook → lifestyle fork → mortgage fork.
 *    Each scripted beat queues the next via `scheduleEvent`.
 *  - Signature mechanics: sticky `expensesDelta+` on every "ты заслужил" choice
 *    (lifestyle.creep.t1–t3 flags); FX mortgage flag `has.fx_mortgage`; oil price drop
 *    as Act II beat; `devaluation_arrives` fires ~9 months after oil drop (Aug 2015).
 *  - 5 endings gated on `arc.final_check` (except BANKRUPTCY on capital ≤ 0).
 *
 * Numbers are tuning-ready starting points; run `./gradlew :shared:test` to verify.
 */
class MaratScenarioGraph : ScenarioGraph() {

    override val initialPlayerState: PlayerState = PlayerState(
        capital = 500_000L,
        income = 700_000L,
        expenses = 220_000L,
        debt = 0L,
        debtPaymentMonthly = 0L,
        investments = 0L,
        investmentReturnRate = 0.08,
        stress = 15,
        financialKnowledge = 8,
        riskLevel = 45,
        month = 1,
        year = 2014,
        characterId = "marat_2015",
        eraId = "kz_2015",
        currency = CurrencyCode.KZT,
    )

    override val events: Map<String, kz.fearsom.financiallifev2.model.GameEvent> = listOf(
        introArc(),
        carArc(),
        mortgageArc(),
        weddingArc(),
        devaluationArc(),
        endgameArc(),
        regularLifeArc(),
    ).buildEvents()

    override val conditionalEvents: List<kz.fearsom.financiallifev2.model.GameEvent> = listOf(
        conditionalsArc(),
        endingsArc(),
    ).flattenEvents()

    override val eventPool: List<PoolEntry> = listOf(
        PoolEntry("normal_life", 10),
        PoolEntry("pool_gadget_upgrade", 8),
        PoolEntry("pool_toy_party", 7),
        PoolEntry("pool_relative_loan", 7),
        PoolEntry("pool_bank_limit_raise", 6),
        PoolEntry("pool_property_invest", 5),
        PoolEntry("pool_restaurant_corp", 5),
        PoolEntry("pool_crypto_tip", 4),
        PoolEntry("pool_forex_scam", 4),
        PoolEntry("pool_insurance_offer", 4),
        PoolEntry("pool_health_scare", 3),
    )
}

// ════════════════════════════════════════════════════════════════════════════
//  АКТ I — Первая большая премия (scripted chain)
// ════════════════════════════════════════════════════════════════════════════

private fun introArc(): EventArc = arc(
    "intro",
    event(
        id = "intro",
        flavor = "🛢️",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Премия за вахту — 700 000 тенге, и это не разовая. " +
                "Ербол скидывает фото нового Prado: «Бери в кредит, нефть же вечная».",
            "Динара прислала ссылку на свадебный зал за 2 миллиона. " +
                "На счёте полмиллиона. Впервые в жизни не думаешь, хватит ли до зарплаты.",
        ),
        options = listOf(
            option(
                "save_bonus", "Отложить премию — жить на прежнем уровне", "💰",
                "lifestyle_check",
                Effect(
                    capitalDelta = 700_000L,
                    knowledgeDelta = 3,
                    stressDelta = -2,
                    setFlags = setOf("choice.save"),
                    scheduleEvent = ScheduledEvent("car_offer", 3),
                ),
            ),
            option(
                "buy_prado", "Взять Prado в кредит — ты заслужил", "🚗",
                "lifestyle_check",
                Effect(
                    debtDelta = 2_500_000L,
                    debtPaymentDelta = 80_000L,
                    expensesDelta = 30_000L,
                    stressDelta = -5,
                    riskDelta = 10,
                    setFlags = setOf("has.car_credit", "lifestyle.creep.t1"),
                    scheduleEvent = ScheduledEvent("car_offer", 3),
                ),
            ),
            option(
                "split_bonus", "Половину на свадьбу мечты, половину на депозит", "💍",
                "lifestyle_check",
                Effect(
                    capitalDelta = 350_000L,
                    expensesDelta = 20_000L,
                    knowledgeDelta = 1,
                    setFlags = setOf("choice.split", "flag.wedding.big"),
                    scheduleEvent = ScheduledEvent("car_offer", 3),
                ),
            ),
        ),
    ),
    event(
        id = "lifestyle_check",
        flavor = "📊",
        unique = true,
        message = story(
            "Февраль. Ты садишься и впервые смотришь на цифры: " +
                "доход 700 000 — расходы 220 000. Разница огромная.",
            "Коллега Серик откладывает 40% с каждой вахты и уже купил квартиру без кредита. " +
                "«Нефть не вечная», — говорит он. Ты смеёшься. Пока.",
        ),
        options = listOf(
            option(
                "set_savings_plan", "Составить план: откладывать 40% каждый месяц", "📝",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = 50_000L,
                    knowledgeDelta = 4,
                    stressDelta = -3,
                    setFlags = setOf("has.savings_plan"),
                    scheduleEvent = ScheduledEvent("mortgage_offer", 6),
                ),
            ),
            option(
                "live_comfortably", "Жить нормально — зачем себе отказывать?", "🍾",
                MONTHLY_TICK,
                Effect(
                    expensesDelta = 60_000L,
                    stressDelta = -5,
                    riskDelta = 5,
                    setFlags = setOf("lifestyle.creep.t1"),
                    scheduleEvent = ScheduledEvent("mortgage_offer", 6),
                ),
            ),
            option(
                "invest_deposit", "Открыть депозит — хотя бы 10% годовых работают", "🏦",
                MONTHLY_TICK,
                Effect(
                    investmentsDelta = 200_000L,
                    capitalDelta = -200_000L,
                    knowledgeDelta = 3,
                    setFlags = setOf("has.deposit"),
                    scheduleEvent = ScheduledEvent("mortgage_offer", 6),
                ),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ I cont. — Машина Ербола (scheduled)
// ════════════════════════════════════════════════════════════════════════════

private fun carArc(): EventArc = arc(
    "car",
    event(
        id = "car_offer",
        flavor = "🚗",
        unique = true,
        tags = setOf("lifestyle"),
        message = story(
            "Ербол катает тебя на новом Prado. Рукоятка из натуральной кожи. " +
                "«Взял в кредит 2,5 миллиона — плачу 80 тысяч в месяц. Смешно при нашей зарплате».",
            "Дилер звонит: остался один такой же в наличии. Можно взять сегодня.",
        ),
        options = listOf(
            option(
                "take_prado_credit", "Взять тоже — 80 000 в месяц не проблема", "🤑",
                MONTHLY_TICK,
                Effect(
                    debtDelta = 2_500_000L,
                    debtPaymentDelta = 80_000L,
                    stressDelta = -3,
                    riskDelta = 10,
                    setFlags = setOf("has.car_credit", "lifestyle.creep.t1"),
                ),
            ),
            option(
                "buy_used_car", "Купить подержанную за 500 тысяч наличными", "🚙",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = -500_000L,
                    stressDelta = 2,
                    knowledgeDelta = 2,
                    setFlags = setOf("has.used_car"),
                ),
            ),
            option(
                "skip_car", "Отказаться — своя машина уже есть", "🚶",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = 20_000L,
                    knowledgeDelta = 3,
                    stressDelta = -2,
                ),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ II — Ипотека (scheduled ~month 7 from intro)
// ════════════════════════════════════════════════════════════════════════════

private fun mortgageArc(): EventArc = arc(
    "mortgage",
    event(
        id = "mortgage_offer",
        flavor = "🏠",
        unique = true,
        tags = setOf("housing"),
        message = story(
            "Банковский менеджер Асем приходит прямо на вахту. " +
                "«Квартира 70 м² в Атырау — 18 миллионов. " +
                "Ипотека в долларах под 7% — это 90 000 тенге в месяц. " +
                "Тенговая под 14% — 170 000».",
            "Коллеги берут валютную. «Доллар же стабильный», — говорит Ербол.",
        ),
        options = listOf(
            option(
                "fx_mortgage", "Взять в долларах — дешевле по платежу", "💵",
                "mortgage_signed",
                Effect(
                    debtDelta = 5_400_000L,
                    debtPaymentDelta = 90_000L,
                    knowledgeDelta = 1,
                    riskDelta = 15,
                    setFlags = setOf("has.fx_mortgage", "lifestyle.creep.t2"),
                    scheduleEvent = ScheduledEvent("wedding_decision", 2),
                ),
            ),
            option(
                "kzt_mortgage", "Взять в тенге — дороже, но без валютного риска", "🏦",
                "mortgage_signed",
                Effect(
                    debtDelta = 5_400_000L,
                    debtPaymentDelta = 170_000L,
                    knowledgeDelta = 4,
                    setFlags = setOf("has.kzt_mortgage"),
                    scheduleEvent = ScheduledEvent("wedding_decision", 2),
                ),
            ),
            option(
                "skip_mortgage", "Отказаться — арендовать пока", "🏢",
                MONTHLY_TICK,
                Effect(
                    expensesDelta = 80_000L,
                    knowledgeDelta = 3,
                    stressDelta = 3,
                    setFlags = setOf("choice.no_mortgage"),
                    scheduleEvent = ScheduledEvent("wedding_decision", 3),
                ),
            ),
        ),
    ),
    event(
        id = "mortgage_signed",
        flavor = "✍️",
        unique = true,
        message = story(
            "Подписал. Квартира твоя. Динара вешает шторы, ты ставишь огромный телевизор.",
            "Суммарный долг теперь ощутимый. Но зарплата перекрывает всё — пока нефть держится.",
        ),
        options = listOf(
            option(
                "continue_lifestyle", "Продолжать жить как привык", "😌",
                MONTHLY_TICK,
                Effect(
                    expensesDelta = 30_000L,
                    stressDelta = -3,
                    setFlags = setOf("lifestyle.creep.t3"),
                    scheduleEvent = ScheduledEvent("oil_price_drop", 5),
                ),
            ),
            option(
                "cut_extras", "Сократить лишние расходы — долги давят", "✂️",
                MONTHLY_TICK,
                Effect(
                    expensesDelta = -40_000L,
                    stressDelta = 5,
                    knowledgeDelta = 2,
                    scheduleEvent = ScheduledEvent("oil_price_drop", 5),
                ),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ II cont. — Свадьба (scheduled from mortgage arc)
// ════════════════════════════════════════════════════════════════════════════

private fun weddingArc(): EventArc = arc(
    "wedding",
    event(
        id = "wedding_decision",
        flavor = "💍",
        unique = true,
        tags = setOf("lifestyle"),
        message = story(
            "Динара нашла зал. 2 миллиона тенге — 300 гостей, живая музыка, ресторан «Астана». " +
                "«Один раз в жизни, Марат. Родители не поймут скромно».",
            "Можно взять потребительский кредит — банк одобрит мгновенно.",
        ),
        options = listOf(
            option(
                "big_wedding_credit", "Свадьба на 2 млн — взять кредит", "💒",
                MONTHLY_TICK,
                Effect(
                    debtDelta = 1_800_000L,
                    debtPaymentDelta = 55_000L,
                    stressDelta = -8,
                    riskDelta = 8,
                    setFlags = setOf("lifestyle.creep.t2", "had.wedding.big"),
                ),
            ),
            option(
                "modest_wedding", "Скромно — 400 тысяч наличными, только близкие", "🥂",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = -400_000L,
                    stressDelta = 5,
                    knowledgeDelta = 2,
                    setFlags = setOf("had.wedding.modest"),
                ),
            ),
            option(
                "postpone_wedding", "Отложить на год — накопить без кредита", "📅",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = 30_000L,
                    knowledgeDelta = 3,
                    stressDelta = 8,
                    setFlags = setOf("wedding.postponed"),
                ),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ II — Нефть падает + Девальвация (scheduled backbone)
// ════════════════════════════════════════════════════════════════════════════

private fun devaluationArc(): EventArc = arc(
    "devaluation",
    event(
        id = "oil_price_drop",
        flavor = "📉",
        unique = true,
        tags = setOf("career", "economy"),
        message = story(
            "Ноябрь 2014. Нефть упала с \$110 до \$80. Компания объявляет «оптимизацию». " +
                "Вахтовые надбавки урезали — доход упал на 150 000 тенге.",
            "Ербол шутит: «Ничего, это временно». " +
                "Серик уже перевёл часть сбережений в доллары.",
        ),
        options = listOf(
            option(
                "trust_recovery", "Потерпеть — нефть восстановится", "⏳",
                MONTHLY_TICK,
                Effect(
                    incomeDelta = -150_000L,
                    stressDelta = 10,
                    riskDelta = 5,
                    scheduleEvent = ScheduledEvent("devaluation_arrives", 9),
                ),
            ),
            option(
                "reduce_expenses_now", "Срочно урезать расходы — закрыть кредитки", "✂️",
                MONTHLY_TICK,
                Effect(
                    incomeDelta = -150_000L,
                    expensesDelta = -70_000L,
                    knowledgeDelta = 4,
                    stressDelta = 8,
                    scheduleEvent = ScheduledEvent("devaluation_arrives", 9),
                ),
            ),
            option(
                "side_income", "Найти подработку — таксовать в выходные", "🚕",
                MONTHLY_TICK,
                Effect(
                    incomeDelta = -90_000L,
                    stressDelta = 15,
                    knowledgeDelta = 2,
                    setFlags = setOf("has.side_income"),
                    scheduleEvent = ScheduledEvent("devaluation_arrives", 9),
                ),
            ),
        ),
    ),
    event(
        id = "devaluation_arrives",
        flavor = "💥",
        unique = true,
        tags = setOf("economy", "crisis"),
        message = story(
            "Август 2015. Нацбанк отпустил тенге. За одну ночь курс улетел с 188 до 255. " +
                "Тенге обесценился на 35%. Все новости кричат: «свободное плавание».",
            "Доход в тенге тот же, но реальная стоимость упала. " +
                "Если брал ипотеку в долларах — долг в тенге вырос на треть.",
        ),
        options = listOf(
            option(
                "panic_sell", "Продать всё что можно — перевести в доллары", "😱",
                "post_deval_reality",
                Effect(
                    incomeDelta = -200_000L,
                    capitalDelta = -300_000L,
                    stressDelta = 20,
                    riskDelta = 10,
                    setFlags = setOf("reacted.panic"),
                ),
            ),
            option(
                "hold_calm", "Не паниковать — держать план, сократить расходы", "🧘",
                "post_deval_reality",
                Effect(
                    incomeDelta = -200_000L,
                    expensesDelta = -80_000L,
                    knowledgeDelta = 5,
                    stressDelta = 12,
                    setFlags = setOf("reacted.calm"),
                ),
            ),
            option(
                "restructure_debt", "Подать заявку на реструктуризацию долга", "📋",
                "post_deval_reality",
                Effect(
                    incomeDelta = -200_000L,
                    debtPaymentDelta = -30_000L,
                    knowledgeDelta = 6,
                    stressDelta = 8,
                    setFlags = setOf("reacted.restructured"),
                ),
            ),
        ),
    ),
    event(
        id = "post_deval_reality",
        flavor = "🔍",
        unique = true,
        message = story(
            "Сентябрь 2015. Ты смотришь на цифры: " +
                "доходы упали, расходы и долги — те же. " +
                "Если брал валютную ипотеку — долг вырос на 35% в тенге.",
            "Ербол уже не смеётся. У него Prado в кредите и FX-ипотека. " +
                "Серик, который не брал валютных долгов, чувствует себя нормально.",
        ),
        options = listOf(
            option(
                "assess_damage", "Составить список долгов — понять реальную картину", "📊",
                "final_review",
                Effect(
                    knowledgeDelta = 5,
                    stressDelta = -5,
                    setFlags = setOf("has.assessed"),
                ),
            ),
            option(
                "keep_going", "Продолжать — авось нефть вырастет", "🙏",
                "final_review",
                Effect(stressDelta = 5, riskDelta = 5),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ III — Итог
// ════════════════════════════════════════════════════════════════════════════

private fun endgameArc(): EventArc = arc(
    "endgame",
    event(
        id = "final_review",
        flavor = "🏁",
        unique = true,
        message = story(
            "2016. Год прошёл после обвала. Нефть не вернулась. " +
                "Компания предлагает остаться — но на 30% меньше.",
            "Момент истины: что осталось от жирных лет? Подушка или долговая яма?",
        ),
        options = listOf(
            option(
                "accept_reduced", "Принять урезанные условия — перестроить бюджет", "💼",
                MONTHLY_TICK,
                Effect(
                    incomeDelta = -210_000L,
                    knowledgeDelta = 5,
                    stressDelta = 10,
                    setFlags = setOf("arc.final_check"),
                ),
            ),
            option(
                "look_for_new", "Уволиться — искать работу вне нефтянки", "🔍",
                MONTHLY_TICK,
                Effect(
                    incomeDelta = -350_000L,
                    stressDelta = 20,
                    knowledgeDelta = 4,
                    setFlags = setOf("arc.final_check", "left.oil"),
                ),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Обычная жизнь + пул событий
// ════════════════════════════════════════════════════════════════════════════

private fun regularLifeArc(): EventArc = arc(
    "regular_life",
    event(
        id = "normal_life",
        flavor = "📅",
        cooldownMonths = 1,
        message = story(
            "Обычный месяц на вахте. Смены, Атырау, переводы Динаре.",
        ),
        options = listOf(
            option("continue", "Продолжать", "➡️", MONTHLY_TICK),
        ),
    ),
    // ── Pool events ─────────────────────────────────────────────────────────
    event(
        id = "pool_gadget_upgrade",
        flavor = "📱",
        message = story(
            "Новый iPhone — 350 000 тенге в рассрочку. " +
                "«Всего 29 000 в месяц», — говорит продавец.",
        ),
        options = listOf(
            option(
                "buy_phone_credit", "Взять в рассрочку", "📲",
                MONTHLY_TICK,
                Effect(
                    debtDelta = 350_000L,
                    debtPaymentDelta = 29_000L,
                    stressDelta = -3,
                    setFlags = setOf("lifestyle.creep.t1"),
                ),
            ),
            option(
                "skip_phone", "Обойтись старым", "🚫",
                MONTHLY_TICK,
                Effect(capitalDelta = 10_000L, knowledgeDelta = 1),
            ),
        ),
        poolWeight = 8,
        cooldownMonths = 6,
    ),
    event(
        id = "pool_toy_party",
        flavor = "🎉",
        message = story(
            "Коллеги организуют корпоратив в ресторане. " +
                "«Ты угощаешь — ты же богатый» — традиция.",
        ),
        options = listOf(
            option(
                "pay_for_all", "Угостить всех — репутация дороже", "🥂",
                MONTHLY_TICK,
                Effect(capitalDelta = -150_000L, stressDelta = -5),
            ),
            option(
                "split_bill", "Предложить скинуться пополам", "🤝",
                MONTHLY_TICK,
                Effect(capitalDelta = -40_000L, stressDelta = 3, knowledgeDelta = 1),
            ),
        ),
        poolWeight = 7,
        cooldownMonths = 4,
    ),
    event(
        id = "pool_relative_loan",
        flavor = "👨‍👩‍👧",
        message = story(
            "Двоюродный брат из Семея просит 300 000 тенге — «срочно, верну через полгода».",
        ),
        options = listOf(
            option(
                "give_loan", "Дать — родня же", "❤️",
                MONTHLY_TICK,
                Effect(capitalDelta = -300_000L, stressDelta = 5),
            ),
            option(
                "give_gift", "Дать 50 тысяч как подарок — без ожиданий", "🎁",
                MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, stressDelta = 2, knowledgeDelta = 2),
            ),
            option(
                "decline_loan", "Отказать — сам в долгах", "🙅",
                MONTHLY_TICK,
                Effect(stressDelta = 8, knowledgeDelta = 2),
            ),
        ),
        poolWeight = 7,
        cooldownMonths = 5,
    ),
    event(
        id = "pool_bank_limit_raise",
        flavor = "💳",
        message = story(
            "Банк поднял лимит карты до 1 500 000 тенге. " +
                "«Поздравляем, вы — надёжный клиент».",
        ),
        options = listOf(
            option(
                "use_limit", "Потратить на ремонт квартиры", "🛠️",
                MONTHLY_TICK,
                Effect(
                    debtDelta = 500_000L,
                    debtPaymentDelta = 20_000L,
                    stressDelta = -3,
                    setFlags = setOf("lifestyle.creep.t2"),
                ),
            ),
            option(
                "ignore_limit", "Оставить как запасной вариант — не трогать", "🔒",
                MONTHLY_TICK,
                Effect(knowledgeDelta = 3, capitalDelta = 5_000L),
            ),
        ),
        poolWeight = 6,
        cooldownMonths = 8,
    ),
    event(
        id = "pool_property_invest",
        flavor = "🏗️",
        message = story(
            "Знакомый предлагает вложить в строящийся ЖК в Алматы. " +
                "«Долёвка — 5 миллионов сейчас, через 2 года продашь за 8».",
        ),
        options = listOf(
            option(
                "invest_dolev", "Вложить 5 млн — диверсификация", "🏢",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = -500_000L,
                    investmentsDelta = 4_500_000L,
                    riskDelta = 10,
                    knowledgeDelta = 2,
                    stressDelta = 8,
                ),
            ),
            option(
                "skip_dolev", "Слишком много рисков в долёвке", "🚫",
                MONTHLY_TICK,
                Effect(capitalDelta = 10_000L, knowledgeDelta = 3),
            ),
        ),
        poolWeight = 5,
        cooldownMonths = 12,
    ),
    event(
        id = "pool_restaurant_corp",
        flavor = "🍖",
        message = story(
            "День рождения начальника. Каждый скидывается по 40 000 тенге.",
        ),
        options = listOf(
            option(
                "pay_restaurant", "Скинуться как все", "🥩",
                MONTHLY_TICK,
                Effect(capitalDelta = -40_000L),
            ),
            option(
                "skip_event", "Сослаться на вахту — пропустить", "🤧",
                MONTHLY_TICK,
                Effect(capitalDelta = 5_000L, stressDelta = 3),
            ),
        ),
        poolWeight = 5,
        cooldownMonths = 3,
    ),
    event(
        id = "pool_crypto_tip",
        flavor = "₿",
        message = story(
            "Сосед по вахте показывает график биткоина: +500% за год. " +
                "«Я уже вложил 200 тысяч — удвоилось. Вписывайся».",
        ),
        options = listOf(
            option(
                "invest_crypto", "Вложить 300 000 — может, повезёт", "🎰",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = -300_000L,
                    riskDelta = 15,
                    stressDelta = 5,
                    setFlags = setOf("invested.crypto"),
                    scheduleEvent = ScheduledEvent("crypto_outcome", 4),
                ),
            ),
            option(
                "skip_crypto", "Пропустить — это казино", "🚫",
                MONTHLY_TICK,
                Effect(capitalDelta = 5_000L, knowledgeDelta = 3),
            ),
        ),
        poolWeight = 4,
        cooldownMonths = 8,
        schemeExplanation = "Криптовалюта — высокорисковый спекулятивный актив. " +
            "Прошлая доходность не гарантирует будущую.",
    ),
    event(
        id = "crypto_outcome",
        flavor = "📉",
        unique = true,
        message = story(
            "Биткоин упал на 70%. Твои 300 тысяч превратились в 90 тысяч.",
        ),
        options = listOf(
            option(
                "sell_crypto", "Продать — зафиксировать убыток", "💸",
                MONTHLY_TICK,
                Effect(capitalDelta = 90_000L, stressDelta = 15, knowledgeDelta = 5),
            ),
            option(
                "hold_crypto", "Держать — вдруг вырастет", "🤞",
                MONTHLY_TICK,
                Effect(stressDelta = 10, riskDelta = 5),
            ),
        ),
    ),
    event(
        id = "pool_forex_scam",
        flavor = "📈",
        message = story(
            "Telegram-канал «Форекс Профи Атырау»: +15% в месяц, " +
                "«торгую нефтяными фьючерсами». Скриншоты прибыли.",
        ),
        options = listOf(
            option(
                "join_forex", "Вложить 200 000 тенге", "💸",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = -200_000L,
                    riskDelta = 10,
                    stressDelta = 5,
                    setFlags = setOf("invested.forex_scam"),
                    scheduleEvent = ScheduledEvent("forex_outcome", 2),
                ),
            ),
            option(
                "skip_forex", "Слишком красиво для правды", "🔍",
                MONTHLY_TICK,
                Effect(capitalDelta = 5_000L, knowledgeDelta = 4),
            ),
        ),
        poolWeight = 4,
        cooldownMonths = 12,
        schemeExplanation = "Схема HYIP — обещание нереальной доходности. " +
            "Средства выводятся организаторами.",
    ),
    event(
        id = "forex_outcome",
        flavor = "🚨",
        unique = true,
        message = story(
            "Канал удалён. Вывод заблокирован. Твои 200 тысяч исчезли.",
        ),
        options = listOf(
            option(
                "accept_loss", "Принять потерю — урок получен", "📚",
                MONTHLY_TICK,
                Effect(stressDelta = 12, knowledgeDelta = 6),
            ),
            option(
                "chase_loss", "Искать другой канал — отыграться", "🎲",
                MONTHLY_TICK,
                Effect(stressDelta = 15, riskDelta = 10, knowledgeDelta = 1),
            ),
        ),
    ),
    event(
        id = "pool_insurance_offer",
        flavor = "📋",
        message = story(
            "Страховой агент предлагает накопительное страхование жизни: " +
                "50 000 тенге в месяц — через 10 лет получишь 8 миллионов.",
        ),
        options = listOf(
            option(
                "buy_insurance", "Оформить — звучит как депозит + страховка", "✅",
                MONTHLY_TICK,
                Effect(expensesDelta = 50_000L, knowledgeDelta = 1, stressDelta = -2),
            ),
            option(
                "skip_insurance", "Посчитать — депозит в банке выгоднее", "🔢",
                MONTHLY_TICK,
                Effect(capitalDelta = 10_000L, knowledgeDelta = 4),
            ),
        ),
        poolWeight = 4,
        cooldownMonths = 10,
    ),
    event(
        id = "pool_health_scare",
        flavor = "🏥",
        message = story(
            "После вахты — давление, усталость. " +
                "Врач советует обследование: 120 000 тенге в частной клинике.",
        ),
        options = listOf(
            option(
                "get_checkup", "Пройти обследование", "💊",
                MONTHLY_TICK,
                Effect(capitalDelta = -120_000L, stressDelta = -10, knowledgeDelta = 1),
            ),
            option(
                "skip_checkup", "Потерпеть — сойдёт", "😤",
                MONTHLY_TICK,
                Effect(stressDelta = 8),
            ),
        ),
        poolWeight = 3,
        cooldownMonths = 6,
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Условные события
// ════════════════════════════════════════════════════════════════════════════

private fun conditionalsArc(): EventArc = arc(
    "conditionals",
    event(
        id = "cond_debt_crisis",
        flavor = "🚨",
        conditions = listOf(cond(DEBT, GTE, 6_500_000L)),
        priority = 80,
        cooldownMonths = 4,
        message = story(
            "Банк звонит: общая долговая нагрузка превышает норматив. " +
                "Ежемесячные платежи поглощают больше половины дохода.",
            "Кредитный офицер предлагает консолидацию — но это ещё один договор.",
        ),
        options = listOf(
            option(
                "consolidate_debt", "Консолидировать долги под 18% на 5 лет", "📄",
                MONTHLY_TICK,
                Effect(
                    debtPaymentDelta = -20_000L,
                    stressDelta = -10,
                    knowledgeDelta = 3,
                    riskDelta = 5,
                ),
            ),
            option(
                "sell_car", "Продать машину — закрыть автокредит", "🔑",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = 1_200_000L,
                    debtDelta = -2_500_000L,
                    debtPaymentDelta = -80_000L,
                    stressDelta = -5,
                    knowledgeDelta = 4,
                ),
            ),
            option(
                "ignore_warning", "Тянуть — пока платишь", "😶",
                MONTHLY_TICK,
                Effect(stressDelta = 10, riskDelta = 8),
            ),
        ),
    ),
    event(
        id = "cond_burnout",
        flavor = "😰",
        conditions = listOf(cond(STRESS, GT, 80L)),
        priority = 70,
        cooldownMonths = 3,
        message = story(
            "Вахта за вахтой. Вечером смотришь в потолок: " +
                "зачем всё это, если живёшь в кредит?",
        ),
        options = listOf(
            option(
                "take_leave", "Взять отпуск без содержания — перезагрузиться", "🌴",
                MONTHLY_TICK,
                Effect(incomeDelta = -150_000L, stressDelta = -25, knowledgeDelta = 3),
            ),
            option(
                "push_through", "Держаться — долги не ждут", "💪",
                MONTHLY_TICK,
                Effect(stressDelta = -10, riskDelta = 5),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Концовки
// ════════════════════════════════════════════════════════════════════════════

private fun endingsArc(): EventArc = arc(
    "endings",
    ending(
        id = "ending_bankruptcy",
        flavor = "💀",
        endingType = EndingType.BANKRUPTCY,
        conditions = listOf(cond(CAPITAL, LTE, 0L)),
        priority = 100,
        message = story(
            "Счёт обнулился. Долги, которые казались управляемыми, стали горой.",
            "Банк инициирует изъятие квартиры. Машина продана. " +
                "Ербол в той же ситуации. Серик купил ещё одну квартиру наличными.",
            "Урок дорогой: нефть не вечная — и долг, взятый на нефтяные деньги, тоже.",
        ),
    ),
    ending(
        id = "ending_paycheck",
        flavor = "😓",
        endingType = EndingType.PAYCHECK_TO_PAYCHECK,
        conditions = listOf(
            cond(CAPITAL, LTE, 300_000L),
            Condition.HasFlag("arc.final_check"),
        ),
        priority = 30,
        message = story(
            "2016. Зарплата приходит и сразу уходит на долги и аренду. " +
                "Подушки нет. Один сбой — и всё рухнет.",
            "Prado, большой той, квартира в долг — стоят дороже, чем казалось тогда.",
        ),
    ),
    ending(
        id = "ending_stability",
        flavor = "🏠",
        endingType = EndingType.FINANCIAL_STABILITY,
        conditions = listOf(
            cond(CAPITAL, GTE, 800_000L),
            cond(DEBT, LTE, 3_000_000L),
            Condition.HasFlag("arc.final_check"),
        ),
        priority = 40,
        message = story(
            "Трудно, но устоял. Девальвация ударила — но не сломала.",
            "У тебя тенговый долг, небольшая подушка и опыт: " +
                "рост дохода — не повод поднимать расходы.",
            "Нефтяной бум закончился. Ты оказался лучше подготовлен, чем Ербол.",
        ),
    ),
    ending(
        id = "ending_freedom",
        flavor = "🌅",
        endingType = EndingType.FINANCIAL_FREEDOM,
        conditions = listOf(
            cond(CAPITAL, GTE, 1_500_000L),
            cond(DEBT, LTE, 1_500_000L),
            Condition.HasFlag("arc.final_check"),
        ),
        priority = 50,
        message = story(
            "Ты не попал в валютную ловушку. Долги — управляемые. Капитал есть.",
            "Коллеги удивлены: «Как ты не взял FX-ипотеку как все?» " +
                "«Считал», — отвечаешь ты.",
            "Концентрация в одном секторе — риск. Ты понял это до кризиса, а не после.",
        ),
    ),
    ending(
        id = "ending_wealth",
        flavor = "🏆",
        endingType = EndingType.WEALTH,
        conditions = listOf(
            cond(CAPITAL, GTE, 2_500_000L),
            Condition.HasFlag("arc.final_check"),
        ),
        priority = 60,
        message = story(
            "Жирные годы на нефтянке — ты использовал их правильно. " +
                "Откладывал, инвестировал, не раздул расходы.",
            "Девальвация ударила по доходу — но у тебя был буфер. " +
                "Ты купил квартиру дешевле после обвала. Диверсифицировал.",
            "Мало кто из вахтовиков сохранил деньги, когда нефть упала. " +
                "Ты — один из немногих.",
        ),
    ),
)
