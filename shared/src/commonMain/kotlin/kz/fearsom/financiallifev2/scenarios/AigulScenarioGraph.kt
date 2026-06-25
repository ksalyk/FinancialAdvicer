package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.CAPITAL
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.DEBT
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.STRESS
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GT
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GTE
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.LT
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.LTE
import kz.fearsom.financiallifev2.model.CurrencyCode
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.ScheduledEvent

/**
 * Айгуль «Базар» — `aigul_90s` · era `kz_90s` · 1994 · KZT.
 *
 * Lesson (Bible §6): entrepreneurship as cash-flow timing — buy stock (working capital at risk),
 * sell, reinvest vs draw; survive the month goods don't move; supplier credit, рэкет, competition.
 * Secondary: МММ pyramid temptation after first taste of profit, family asks.
 *
 * Architecture:
 *  - Opening choice (баул size) takes on debt/capital risk and schedules the first sale
 *    outcome (goods_sold_*, +1 month). Income = 0 — she earns only via scheduled sales.
 *  - Each sale outcome schedules the МММ wave (+2 months) — pyramid arrives after she
 *    has something to potentially lose or invest.
 *  - Backbone continues: racket_intro → supplier_squeeze → kiosk_offer → final_review.
 *  - Pool events cover routine market life, family asks, scam temptations.
 *  - 5 endings resolve via conditionals once arc.final_check is set.
 *
 * Numbers calibrated for 1994 KZT (scale ~10× Daniyar; wholesale goods trade).
 */
class AigulScenarioGraph : ScenarioGraph() {

    override val initialPlayerState: PlayerState = PlayerState(
        capital = 30_000L,
        income = 0L,                   // ⚠ intentional: earns via scheduled sale events only
        expenses = 18_000L,
        debt = 12_000L,
        debtPaymentMonthly = 4_000L,
        investments = 0L,
        investmentReturnRate = 0.05,
        stress = 65,
        financialKnowledge = 20,
        riskLevel = 40,
        month = 3,                     // March 1994 → MMM backbone fires June '94 (+3)
        year = 1994,
        characterId = "aigul_90s",
        eraId = "kz_90s",
        currency = CurrencyCode.KZT,
        flags = setOf("single_mother"),
    )

    override val events: Map<String, kz.fearsom.financiallifev2.model.GameEvent> = listOf(
        marketArc(),
        mmmArc(),
        pressureArc(),
        endgameArc(),
        regularLifeArc(),
    ).buildEvents()

    override val conditionalEvents: List<kz.fearsom.financiallifev2.model.GameEvent> = listOf(
        conditionalsArc(),
        endingsArc(),
    ).flattenEvents()

    override val eventPool: List<PoolEntry> = listOf(
        PoolEntry("normal_life", 10),
        PoolEntry("pool_market_busy", 7),
        PoolEntry("pool_market_quiet", 6),
        PoolEntry("pool_amina_school", 5),
        PoolEntry("pool_relative_ask", 5),
        PoolEntry("pool_competitor", 4),
        PoolEntry("pool_spoiled_goods", 4),
        PoolEntry("pool_tax_raid", 3),
        PoolEntry("pool_scam_pyramid_neighbor", 2),
        PoolEntry("pool_scam_forex", 2),
    )
}

// ════════════════════════════════════════════════════════════════════════════
//  АКТ I — Первый баул (scripted chain + scheduled sale outcomes)
// ════════════════════════════════════════════════════════════════════════════

private fun marketArc(): EventArc = arc(
    "market",
    event(
        id = "intro",
        flavor = "🧳",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Ваня отгружает баул турецкого трикотажа у ворот базара: «Бери на 30 000, отдашь с выручки. " +
                "Не продашь — долг твой». У него таких должников половина ряда.",
            "Амина кашляет вторую неделю. Аптека — это ещё 4 000. В кармане 30 000 — всё, что есть. " +
                "Ваня смотрит и ждёт ответа.",
        ),
        options = listOf(
            option("full_baul", "Взять полный баул — риск на максимум", "📦", MONTHLY_TICK,
                Effect(debtDelta = 30_000L, debtPaymentDelta = 5_000L, stressDelta = 8, riskDelta = 10,
                    setFlags = setOf("baul.full"),
                    scheduleEvent = ScheduledEvent("goods_sold_full", 1))),
            option("half_baul", "Взять половину — остаток в подушку", "🧶", MONTHLY_TICK,
                Effect(debtDelta = 15_000L, debtPaymentDelta = 2_500L, stressDelta = 3,
                    setFlags = setOf("baul.half"),
                    scheduleEvent = ScheduledEvent("goods_sold_half", 1))),
            option("clinic_first", "Сначала к врачу — товар потом", "🏥", "amina_clinic",
                Effect(capitalDelta = -4_000L, stressDelta = -8, setFlags = setOf("child.first"))),
        ),
    ),
    event(
        id = "amina_clinic",
        flavor = "💊",
        unique = true,
        tags = setOf("family"),
        message = story(
            "В детской поликлинике очередь на два часа. Врач выписывает антибиотик: «Ещё 3 000 на лекарства. " +
                "Недели через две поправится».",
            "Ваня ждёт. Ряд не ждёт. Каждый день без товара — это день без выручки.",
        ),
        options = listOf(
            option("small_batch_now", "Взять маленькую партию на свои — 8 000 ₸", "🛍️", MONTHLY_TICK,
                Effect(capitalDelta = -8_000L, stressDelta = 5,
                    setFlags = setOf("baul.small"),
                    scheduleEvent = ScheduledEvent("goods_sold_small", 1))),
            option("wait_amina", "Побыть с Аминой, выйти на рынок позже", "🤍", MONTHLY_TICK,
                Effect(capitalDelta = -3_000L, stressDelta = -5, knowledgeDelta = 1,
                    setFlags = setOf("baul.small", "child.cared"),
                    scheduleEvent = ScheduledEvent("goods_sold_small", 2))),
        ),
    ),
    // ── Итоги первого баула (scheduled, unique) ──────────────────────────────
    event(
        id = "goods_sold_full",
        flavor = "💰",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Неделя на ногах с шести утра до заката. Полный баул турецкого трикотажа разошёлся за пять дней. " +
                "Выручка — 52 000 тенге наличными. Ваня доволен. Ты тоже — почти.",
            "После расчёта с Ваней останется примерно 22 000. Но через месяц нужен следующий баул. " +
                "Капитал работает только пока он в обороте.",
        ),
        options = listOf(
            option("reinvest_repay", "Вернуть долг Ване, сразу взять новый баул", "🔄", MONTHLY_TICK,
                Effect(capitalDelta = 52_000L, debtDelta = -30_000L, debtPaymentDelta = -5_000L,
                    knowledgeDelta = 2, setFlags = setOf("cashflow.reinvest"),
                    scheduleEvent = ScheduledEvent("mmm_wave", 2))),
            option("repay_keep_buffer", "Вернуть долг и оставить 15 000 в резерве", "✉️", MONTHLY_TICK,
                Effect(capitalDelta = 52_000L, debtDelta = -30_000L, debtPaymentDelta = -5_000L,
                    knowledgeDelta = 3, setFlags = setOf("cashflow.buffer"),
                    scheduleEvent = ScheduledEvent("mmm_wave", 2))),
            option("take_profit", "Оставить часть выручки — семья важнее", "👧", MONTHLY_TICK,
                Effect(capitalDelta = 52_000L, debtDelta = -20_000L, debtPaymentDelta = -3_000L,
                    stressDelta = -10, setFlags = setOf("child.cared"),
                    scheduleEvent = ScheduledEvent("mmm_wave", 2))),
        ),
    ),
    event(
        id = "goods_sold_half",
        flavor = "🧾",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Меньший баул — меньше риска, меньше выручки. За неделю продала почти всё: 27 000 тенге в кармане. " +
                "Долг Ваниному кредиту закрыт — и это уже победа.",
            "Ваня: «Маловато берёшь — место пустует, конкуренты занимают». Может, он и прав. А может, удобно так говорить.",
        ),
        options = listOf(
            option("repay_expand", "Закрыть долг и взять новый, больший баул", "📦", MONTHLY_TICK,
                Effect(capitalDelta = 27_000L, debtDelta = 10_000L, debtPaymentDelta = 1_500L,
                    setFlags = setOf("cashflow.reinvest", "baul.expanded"),
                    scheduleEvent = ScheduledEvent("mmm_wave", 2))),
            option("repay_stay_half", "Закрыть долг, держать тот же объём", "🧶", MONTHLY_TICK,
                Effect(capitalDelta = 27_000L, debtDelta = -15_000L, debtPaymentDelta = -2_500L,
                    knowledgeDelta = 2, setFlags = setOf("cashflow.steady"),
                    scheduleEvent = ScheduledEvent("mmm_wave", 2))),
        ),
    ),
    event(
        id = "goods_sold_small",
        flavor = "🧾",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Маленькая партия, маленькая выручка — 14 000 тенге. Амина выздоравливает. " +
                "Долгов от этой партии нет — товар был на свои. Но запаса почти не прибавилось.",
            "Ваня: «Слушай, я подожду — но долго ждать не буду. Ряд — деньги, не место жалеть».",
        ),
        options = listOf(
            option("try_full_now", "Взять у Вани полный баул — пора рисковать", "📦", MONTHLY_TICK,
                Effect(capitalDelta = 14_000L, debtDelta = 30_000L, debtPaymentDelta = 5_000L,
                    stressDelta = 8, setFlags = setOf("baul.full"),
                    scheduleEvent = ScheduledEvent("mmm_wave", 3))),
            option("build_slowly", "Продолжать на свои, копить постепенно", "🐢", MONTHLY_TICK,
                Effect(capitalDelta = 14_000L, knowledgeDelta = 2,
                    setFlags = setOf("cashflow.steady"),
                    scheduleEvent = ScheduledEvent("mmm_wave", 3))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  МММ (июнь '94) — pyramid temptation after first profit
// ════════════════════════════════════════════════════════════════════════════

private fun mmmArc(): EventArc = arc(
    "mmm",
    event(
        id = "mmm_wave",
        flavor = "💸",
        unique = true,
        tags = setOf("scam", "scam.pyramid", "world"),
        schemeExplanation = "МММ выплачивал «доходность» старым вкладчикам деньгами новых — классическая пирамида. " +
            "Пока приходят новые участники, она держится. Когда поток иссякает — теряют все, кто вошёл последним.",
        message = story(
            "Июнь. Соседка по ряду Лена машет жёлтыми билетами МММ: «Вложила 10 000 в апреле — в прошлую пятницу " +
                "получила 18 000. Говорят, в июле опять поднимут».",
            "Ваня тоже слышал: «Айгуль, у тебя выручка есть — зачем в ткань вкладывать? " +
                "Положи в МММ, за месяц вернётся с процентом». Амина рядом — рисует что-то в тетрадке.",
        ),
        options = listOf(
            option("invest_mmm", "Вложить 20 000 ₸ в МММ — все поднимаются", "🎟️", MONTHLY_TICK,
                Effect(capitalDelta = -20_000L, riskDelta = 15, stressDelta = 3,
                    setFlags = setOf("scam.taken"),
                    scheduleEvent = ScheduledEvent("mmm_lost", 2))),
            option("invest_all_in", "Вложить всё — 40 000 ₸", "💥", MONTHLY_TICK,
                Effect(capitalDelta = -40_000L, riskDelta = 20, stressDelta = 8,
                    setFlags = setOf("scam.taken", "scam.all_in"),
                    scheduleEvent = ScheduledEvent("mmm_lost", 2))),
            option("say_no_to_lena", "Отказать: деньги нужны в обороте", "🛑", MONTHLY_TICK,
                Effect(knowledgeDelta = 3, setFlags = setOf("learned.scam.pyramid"),
                    scheduleEvent = ScheduledEvent("mmm_safe", 2))),
            option("ask_vanya_advice", "Спросить совета у Вани ещё раз", "🤔", MONTHLY_TICK,
                Effect(knowledgeDelta = 1, setFlags = setOf("learned.scam.pyramid"),
                    scheduleEvent = ScheduledEvent("mmm_safe", 2))),
        ),
    ),
    event(
        id = "mmm_lost",
        flavor = "📉",
        unique = true,
        tags = setOf("scam", "scam.pyramid"),
        message = story(
            "Август. Офисы МММ закрыты. Лена ходит с красными глазами. Очереди к входу, двери с замком.",
            "Деньги сгорели. Ряд стоит. Амина смотрит на тебя.",
        ),
        options = listOf(
            option("face_it", "Проглотить потерю и работать дальше", "😤", MONTHLY_TICK,
                Effect(stressDelta = 15, setFlags = setOf("learned.scam.pyramid", "lost_money_to_scam"),
                    scheduleEvent = ScheduledEvent("racket_intro", 2))),
            option("vow_lesson", "Поклясться больше никогда не вкладывать вслепую", "📓", MONTHLY_TICK,
                Effect(stressDelta = 8, knowledgeDelta = 4,
                    setFlags = setOf("learned.scam.pyramid", "lost_money_to_scam", "lesson.scam"),
                    scheduleEvent = ScheduledEvent("racket_intro", 2))),
        ),
    ),
    event(
        id = "mmm_safe",
        flavor = "📈",
        unique = true,
        tags = setOf("scam", "scam.pyramid"),
        message = story(
            "Август. Лена рыдает в углу ряда: вложила всё — ничего не вернули. По базару ходят люди с пустыми глазами.",
            "Твои деньги целы. Ваня: «Умная баба. Жаль, не все такие».",
        ),
        options = listOf(
            option("expand_stock", "Купить новый баул — рынок освободился", "📦", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, stressDelta = -5,
                    setFlags = setOf("cashflow.reinvest"),
                    scheduleEvent = ScheduledEvent("racket_intro", 2))),
            option("keep_steady", "Работать дальше без изменений", "🧱", MONTHLY_TICK,
                Effect(stressDelta = -3,
                    scheduleEvent = ScheduledEvent("racket_intro", 2))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ II — рэкет → поставщик → киоск
// ════════════════════════════════════════════════════════════════════════════

private fun pressureArc(): EventArc = arc(
    "pressure",
    // ── Рэкет (scheduled, unique) ────────────────────────────────────────────
    event(
        id = "racket_intro",
        flavor = "🪓",
        unique = true,
        tags = setOf("crisis"),
        message = story(
            "Осень. К ряду подходит широкий парень в кожаной куртке — Серый. Без здравствуй.",
            "«За место на рынке платят все. Три тысячи в месяц — и вопросов нет. Подумай до пятницы».",
            "Ваня тихо: «Не платишь — товар портят, ряд ломают. Таких уже было». " +
                "Участковый приходит раз в квартал и ни во что не вмешивается.",
        ),
        options = listOf(
            option("pay_racket", "Платить 3 000 ₸ в месяц — работать спокойно", "💰", MONTHLY_TICK,
                Effect(expensesDelta = 3_000L, stressDelta = -5, setFlags = setOf("racket.paying"),
                    scheduleEvent = ScheduledEvent("supplier_squeeze", 3))),
            option("refuse_racket", "Отказать — не платить за воздух", "✊", MONTHLY_TICK,
                Effect(riskDelta = 20, stressDelta = 15, setFlags = setOf("racket.refused"),
                    scheduleEvent = ScheduledEvent("racket_retaliation", 1))),
            option("move_spot", "Перенести ряд на другое место — 5 000 ₸ за переоформление", "🔄", MONTHLY_TICK,
                Effect(capitalDelta = -5_000L, stressDelta = 5, knowledgeDelta = 1,
                    setFlags = setOf("racket.moved"),
                    scheduleEvent = ScheduledEvent("supplier_squeeze", 3))),
        ),
    ),
    event(
        id = "racket_retaliation",
        flavor = "💔",
        unique = true,
        tags = setOf("crisis"),
        message = story(
            "Утром треть товара разрезана, лоток опрокинут. Охранник ничего не видел.",
            "Убыток — 12 000 тенге. Серый проходит мимо и смотрит мимо тебя.",
        ),
        options = listOf(
            option("pay_now", "Заплатить Серому — хватит воевать", "😔", MONTHLY_TICK,
                Effect(capitalDelta = -12_000L, expensesDelta = 3_000L, stressDelta = 10,
                    setFlags = setOf("racket.paying"),
                    scheduleEvent = ScheduledEvent("supplier_squeeze", 3))),
            option("document_losses", "Записать всё и найти других пострадавших", "📋", MONTHLY_TICK,
                Effect(capitalDelta = -12_000L, knowledgeDelta = 2, stressDelta = 8,
                    setFlags = setOf("racket.fighting"),
                    scheduleEvent = ScheduledEvent("supplier_squeeze", 3))),
        ),
    ),
    // ── Давление поставщика (scheduled, unique) ──────────────────────────────
    event(
        id = "supplier_squeeze",
        flavor = "🏦",
        unique = true,
        tags = setOf("career", "crisis"),
        message = story(
            "Зима. Ваня меняет условия: «Рынок вырос, цены на товар тоже. Теперь кредит под 15% в месяц. " +
                "Или закрывай долг до Нового года».",
            "Долг перед ним — сколько есть. Процент отъедает маржу. Другой оптовик берёт дороже, зато без процентов.",
        ),
        options = listOf(
            option("pay_vanya_off", "Закрыть весь долг у Вани сейчас", "✅", MONTHLY_TICK,
                Effect(capitalDelta = -20_000L, knowledgeDelta = 2,
                    setFlags = setOf("supplier.clean"),
                    scheduleEvent = ScheduledEvent("kiosk_offer", 3))),
            option("negotiate_vanya", "Договориться на фиксированный долг без роста", "🤝", MONTHLY_TICK,
                Effect(knowledgeDelta = 3, stressDelta = -3,
                    setFlags = setOf("supplier.negotiated"),
                    scheduleEvent = ScheduledEvent("kiosk_offer", 3))),
            option("switch_supplier", "Найти нового поставщика, уйти от Вани", "🔍", MONTHLY_TICK,
                Effect(capitalDelta = -3_000L, stressDelta = 8, knowledgeDelta = 1,
                    setFlags = setOf("supplier.switched"),
                    scheduleEvent = ScheduledEvent("kiosk_offer", 3))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ III — киоск → финал → концовки
// ════════════════════════════════════════════════════════════════════════════

private fun endgameArc(): EventArc = arc(
    "endgame",
    event(
        id = "kiosk_offer",
        flavor = "🏪",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Весна 1995-го. Администрация рынка предлагает аренду небольшого киоска — 3 000 ₸ в месяц плюс залог 15 000. " +
                "Постоянное место, крыша, полки, замок на ночь. Никакого Серого.",
            "Сосед-предприниматель: «С киоска выручка стабильнее. Но каждый месяц аренда — прибыль или не прибыль, всё равно плати».",
        ),
        options = listOf(
            option("rent_kiosk_own", "Взять киоск на свои — залог 15 000 ₸", "🏪", MONTHLY_TICK,
                Effect(capitalDelta = -15_000L, expensesDelta = 3_000L, incomeDelta = 10_000L,
                    knowledgeDelta = 2, setFlags = setOf("kiosk.opened"),
                    scheduleEvent = ScheduledEvent("final_review", 4))),
            option("rent_kiosk_credit", "Взять киоск в кредит — занять 20 000 ₸", "🏦", MONTHLY_TICK,
                Effect(debtDelta = 20_000L, debtPaymentDelta = 3_500L, expensesDelta = 3_000L, incomeDelta = 10_000L,
                    stressDelta = 8, setFlags = setOf("kiosk.opened"),
                    scheduleEvent = ScheduledEvent("final_review", 4))),
            option("stay_mobile", "Остаться мобильной — без аренды и обязательств", "🎒", MONTHLY_TICK,
                Effect(knowledgeDelta = 1, stressDelta = -5,
                    scheduleEvent = ScheduledEvent("final_review", 4))),
        ),
    ),
    event(
        id = "final_review",
        flavor = "📊",
        unique = true,
        message = story(
            "Осень 1995-го. Ты сидишь за столом на кухне, Амина делает уроки рядом. " +
                "На листке бумаги — выручка, долги, аренда, расходы на Амину и то, что осталось.",
            "Полтора года на базаре. Ты знаешь цену товара, цену долга и цену дня, когда покупателей нет. " +
                "Это не написано ни в одном учебнике.",
        ),
        options = listOf(
            option("count_everything", "Свести все цифры — выручка, долги, что осталось", "🧮", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, setFlags = setOf("arc.final_check"))),
            option("plan_next_season", "Написать план на следующий сезон", "📝", MONTHLY_TICK,
                Effect(stressDelta = -5, knowledgeDelta = 1, setFlags = setOf("arc.final_check"))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Рутина — пул (normal_life + рыночная жизнь + семья + скамы)
// ════════════════════════════════════════════════════════════════════════════

private fun regularLifeArc(): EventArc = arc(
    "regular_life",
    event(
        id = "normal_life",
        flavor = "🌤️",
        cooldownMonths = 1,
        message = story(
            "Обычная неделя на рынке. Встаёшь до рассвета, раскладываешь товар, торгуешь до темноты. " +
                "Амина у соседки. Деньги считаешь вечером.",
        ),
        options = listOf(
            option("trade_on", "Работать дальше — обычный день", "🛍️", MONTHLY_TICK),
            option("reinvest_rest", "Вложить часть выручки в новый товар", "🔄", MONTHLY_TICK,
                Effect(knowledgeDelta = 1, setFlags = setOf("cashflow.reinvest"))),
            option("rest_sunday", "Взять воскресенье выходным", "😴", MONTHLY_TICK,
                Effect(capitalDelta = -2_000L, stressDelta = -6)),
        ),
    ),
    event(
        id = "pool_market_busy",
        flavor = "🎉",
        tags = setOf("career"),
        cooldownMonths = 2,
        message = story(
            "Перед праздником рынок гудит. Покупатели идут сплошным потоком, товар уходит быстро. " +
                "За два дня выручка — как за обычную неделю.",
        ),
        options = listOf(
            option("push_sales", "Торговать до последнего покупателя", "💪", MONTHLY_TICK,
                Effect(capitalDelta = 8_000L, stressDelta = 5)),
            option("reserve_stock", "Придержать часть товара — цена вырастет", "📦", MONTHLY_TICK,
                Effect(capitalDelta = 5_000L, knowledgeDelta = 1)),
        ),
    ),
    event(
        id = "pool_market_quiet",
        flavor = "🌧️",
        tags = setOf("crisis"),
        cooldownMonths = 2,
        maxOccurrences = 3,
        message = story(
            "Дождь с утра. На рынке пусто. К обеду продала три вещи. Затраты на аренду ряда идут в любую погоду.",
        ),
        options = listOf(
            option("discount_price", "Сделать скидку 20% — хоть что-то продать", "🏷️", MONTHLY_TICK,
                Effect(capitalDelta = 2_000L, stressDelta = 3)),
            option("hold_price", "Держать цену — плохие дни бывают у всех", "🧱", MONTHLY_TICK,
                Effect(stressDelta = 6)),
            option("use_slow_day", "Потратить день на поиск нового поставщика", "🔍", MONTHLY_TICK,
                Effect(capitalDelta = -500L, knowledgeDelta = 2)),
        ),
    ),
    event(
        id = "pool_amina_school",
        flavor = "📚",
        tags = setOf("family"),
        cooldownMonths = 3,
        conditions = listOf(Condition.HasFlag("single_mother")),
        message = story(
            "Амина приходит с запиской от учителя: нужны учебники и форма к новому году. Ещё денег на экскурсию. " +
                "«Мама, все поедут, только я нет?»",
        ),
        options = listOf(
            option("pay_all", "Купить всё — 5 000 ₸", "📚", MONTHLY_TICK,
                Effect(capitalDelta = -5_000L, stressDelta = -5, setFlags = setOf("child.cared"))),
            option("pay_basics", "Учебники и форму — 3 000 ₸, без экскурсии", "📗", MONTHLY_TICK,
                Effect(capitalDelta = -3_000L, stressDelta = 3)),
            option("borrow_books", "Попросить книги у соседей", "🤝", MONTHLY_TICK,
                Effect(capitalDelta = -1_000L, knowledgeDelta = 1)),
        ),
    ),
    event(
        id = "pool_relative_ask",
        flavor = "📞",
        tags = setOf("family"),
        cooldownMonths = 3,
        message = story(
            "Звонит сестра из Шымкента: «Ты же в Алматы, там зарабатываешь. Дай взаймы 10 000 — отдадим летом».",
        ),
        options = listOf(
            option("lend_sister", "Дать 10 000 ₸ в долг", "🤝", MONTHLY_TICK,
                Effect(capitalDelta = -10_000L, stressDelta = 3)),
            option("give_partial", "Дать 3 000 ₸ в подарок, больше нет", "💌", MONTHLY_TICK,
                Effect(capitalDelta = -3_000L, stressDelta = 5)),
            option("refuse_relative", "Объяснить: у меня самой долги", "🗣️", MONTHLY_TICK,
                Effect(stressDelta = 8, knowledgeDelta = 1)),
        ),
    ),
    event(
        id = "pool_competitor",
        flavor = "⚔️",
        tags = setOf("career"),
        cooldownMonths = 3,
        maxOccurrences = 2,
        message = story(
            "Рядом открылся новый ряд — похожий товар, цены на 10% ниже. Покупатели останавливаются у них. " +
                "За тобой — постоянные. Но их меньше, чем хотелось бы.",
        ),
        options = listOf(
            option("cut_price", "Снизить цены — удержать поток", "🏷️", MONTHLY_TICK,
                Effect(capitalDelta = -3_000L, stressDelta = 5)),
            option("change_assortment", "Поменять ассортимент — другой товар", "🔄", MONTHLY_TICK,
                Effect(capitalDelta = -5_000L, knowledgeDelta = 2)),
            option("hold_quality", "Держаться на качестве и постоянных", "🧱", MONTHLY_TICK,
                Effect(knowledgeDelta = 1)),
        ),
    ),
    event(
        id = "pool_spoiled_goods",
        flavor = "📦",
        tags = setOf("crisis"),
        cooldownMonths = 4,
        maxOccurrences = 2,
        message = story(
            "После дождя часть трикотажа намокла под навесом — пятна, залом. Продать по нормальной цене нельзя.",
        ),
        options = listOf(
            option("sell_discount", "Распродать по себестоимости — 4 000 ₸ убыток", "🏷️", MONTHLY_TICK,
                Effect(capitalDelta = -4_000L, stressDelta = 4)),
            option("store_hope", "Высушить и попробовать продать позже", "🤞", MONTHLY_TICK,
                Effect(stressDelta = 6, riskDelta = 3)),
            option("protect_next_time", "Купить хороший навес — 2 000 ₸", "🌂", MONTHLY_TICK,
                Effect(capitalDelta = -6_000L, knowledgeDelta = 2,
                    setFlags = setOf("business.protected"))),
        ),
    ),
    event(
        id = "pool_tax_raid",
        flavor = "🚔",
        tags = setOf("crisis"),
        cooldownMonths = 4,
        maxOccurrences = 2,
        message = story(
            "Налоговая инспекция и ОБЭП ходят по рядам. Проверяют документы, накладные, кассовый аппарат.",
            "У тебя документы в порядке — частично. Инспектор листает с выражением «найдём».",
        ),
        options = listOf(
            option("cooperate_official", "Показать всё — заплатить штраф по акту", "📋", MONTHLY_TICK,
                Effect(capitalDelta = -5_000L, knowledgeDelta = 2, stressDelta = 5)),
            option("settle_quietly", "«Договориться» на месте — 2 000 ₸", "🤫", MONTHLY_TICK,
                Effect(capitalDelta = -2_000L, stressDelta = 3, riskDelta = 5)),
            option("have_papers_ready", "Все документы в порядке — пройти без проблем", "✅", MONTHLY_TICK,
                Effect(knowledgeDelta = 1)),
        ),
    ),
    // ── Пул-скамы (single-use; рефуз ставит learned.scam → движок снижает вес) ──
    event(
        id = "pool_scam_pyramid_neighbor",
        flavor = "🎟️",
        tags = setOf("scam", "scam.pyramid"),
        conditions = listOf(Condition.NotFlag("learned.scam.pyramid")),
        schemeExplanation = "Соседская пирамида работает так же, как МММ — пока есть новые участники. " +
            "Ранние вкладчики видят прибыль, поздние теряют всё. Нет ни лицензии, ни гарантий.",
        message = story(
            "Соседка по ряду Нурия шёпотом: «Есть надёжный человек, собирает деньги — 50% за два месяца. " +
                "Я уже вложила, жду выплату».",
        ),
        options = listOf(
            option("join_pyramid", "Вложить 15 000 ₸ — Нурия заработала", "🎰", MONTHLY_TICK,
                Effect(capitalDelta = -15_000L, riskDelta = 10,
                    setFlags = setOf("lost_money_to_scam"),
                    scheduleEvent = ScheduledEvent("pyramid_result_lost", 2))),
            option("refuse_pyramid", "Отказать: схема не объяснена", "🛑", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, setFlags = setOf("learned.scam.pyramid"))),
        ),
    ),
    event(
        id = "pyramid_result_lost",
        flavor = "📉",
        unique = true,
        tags = setOf("scam", "scam.pyramid"),
        message = story(
            "«Надёжный человек» пропал. Нурия плачет. 15 000 сгорели.",
        ),
        options = listOf(
            option("lesson_learned", "Запомнить навсегда", "📓", MONTHLY_TICK,
                Effect(knowledgeDelta = 3, stressDelta = 8,
                    setFlags = setOf("learned.scam.pyramid"))),
        ),
    ),
    event(
        id = "pool_scam_forex",
        flavor = "💱",
        tags = setOf("scam", "scam.forex"),
        conditions = listOf(Condition.NotFlag("learned.scam.forex")),
        schemeExplanation = "Форекс-кухни показывают нереальную доходность на демо-счёте. " +
            "На реальном депозите правила меняются, вывод средств блокируется.",
        message = story(
            "Листовка у входа на рынок: «ФОРЕКС — доходность 200% в год. Обучение бесплатно, торгуй из дома». " +
                "Телефон. Молодой менеджер встречает у офиса с кофе.",
        ),
        options = listOf(
            option("invest_forex", "Открыть счёт — 10 000 ₸ стартовый депозит", "💱", MONTHLY_TICK,
                Effect(capitalDelta = -10_000L, riskDelta = 10,
                    setFlags = setOf("lost_money_to_scam"),
                    scheduleEvent = ScheduledEvent("forex_result_lost", 2))),
            option("skip_forex", "Слишком красиво звучит — пройти мимо", "🚶", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, setFlags = setOf("learned.scam.forex"))),
        ),
    ),
    event(
        id = "forex_result_lost",
        flavor = "📉",
        unique = true,
        tags = setOf("scam", "scam.forex"),
        message = story(
            "Счёт «слит» за две недели. Менеджер не отвечает. Офис закрыт.",
        ),
        options = listOf(
            option("accept_loss", "Принять. Больше ни в какие «офисы»", "😞", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, stressDelta = 6,
                    setFlags = setOf("learned.scam.forex"))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Условные события — давление без спойлеров
// ════════════════════════════════════════════════════════════════════════════

private fun conditionalsArc(): EventArc = arc(
    "conditionals",
    event(
        id = "debt_crisis",
        flavor = "⛓️",
        tags = setOf("crisis"),
        priority = 100,
        cooldownMonths = 3,
        conditions = listOf(cond(DEBT, GT, 35_000L)),
        message = story(
            "Долг Ваниному кредиту и аренде перевесил выручку. Каждый новый баул — это новый долг поверх старого.",
            "Кредитор намекает на «проценты сверху», а другие продавцы советуют взять кредит в банке под твёрдый процент.",
        ),
        options = listOf(
            option("cut_stock", "Сократить партию вдвое — гасить долг", "✂️", MONTHLY_TICK,
                Effect(capitalDelta = -5_000L, debtDelta = -5_000L, stressDelta = 5)),
            option("sell_personal", "Продать личные вещи — 3 000 ₸", "👜", MONTHLY_TICK,
                Effect(capitalDelta = 3_000L, stressDelta = 8)),
            option("debt_consolidation", "Взять кредит в банке — закрыть Ваню разом", "🏦", MONTHLY_TICK,
                Effect(debtDelta = 10_000L, debtPaymentDelta = 2_000L, stressDelta = 5,
                    setFlags = setOf("supplier.clean"))),
        ),
    ),
    event(
        id = "burnout",
        flavor = "😵",
        tags = setOf("crisis"),
        priority = 90,
        cooldownMonths = 6,
        conditions = listOf(cond(STRESS, GT, 75L)),
        message = story(
            "Ты не помнишь, когда последний раз сидела просто так. Встаёшь ночью и проверяешь замок на лотке. " +
                "Амина спрашивает, почему мама всегда устала.",
        ),
        options = listOf(
            option("take_weekend", "Закрыть ряд на выходные — Амина и парк", "🌿", MONTHLY_TICK,
                Effect(capitalDelta = -4_000L, stressDelta = -22, setFlags = setOf("child.cared"))),
            option("push_through_burnout", "Некогда отдыхать, долги не ждут", "🥵", MONTHLY_TICK,
                Effect(capitalDelta = 2_000L, stressDelta = 8)),
        ),
    ),
    event(
        id = "amina_sick_crisis",
        flavor = "🤒",
        tags = setOf("family", "crisis"),
        priority = 80,
        cooldownMonths = 6,
        conditions = listOf(
            Condition.HasFlag("single_mother"),
            cond(STRESS, GT, 60L),
        ),
        message = story(
            "Амина не встаёт утром — температура 39. Детская скорая говорит «не критично», но нужно к педиатру, " +
                "лекарства и три дня дома. Три дня — это три дня без рынка.",
        ),
        options = listOf(
            option("stay_amina", "Остаться с Аминой дома три дня", "🤍", MONTHLY_TICK,
                Effect(capitalDelta = -6_000L, stressDelta = -10, setFlags = setOf("child.cared"))),
            option("leave_to_neighbour", "Попросить соседку — идти на рынок", "👧", MONTHLY_TICK,
                Effect(capitalDelta = -2_000L, stressDelta = 10)),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Концовки — терминальные условные узлы (priority desc)
// ════════════════════════════════════════════════════════════════════════════

private fun endingsArc(): EventArc = arc(
    "endings",
    ending(
        id = "ending_bankruptcy",
        endingType = EndingType.BANKRUPTCY,
        flavor = "💔",
        priority = 200,
        conditions = listOf(cond(CAPITAL, LTE, 0L)),
        message = story(
            "Долги съели всё. Ряд закрыт. Ваня забрал последнюю партию в счёт долга.",
            "Ты везёшь Амину обратно в Шымкент. Это не конец — но пока так. " +
                "Ты знаешь теперь точно: оборотный капитал не терпит ошибок.",
        ),
    ),
    ending(
        id = "ending_wealth",
        endingType = EndingType.WEALTH,
        flavor = "🏪",
        priority = 150,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, 200_000L),
            cond(DEBT, LTE, 10_000L),
            Condition.HasFlag("kiosk.opened"),
            Condition.HasFlag("supplier.clean"),
            Condition.HasFlag("learned.scam.pyramid"),
            Condition.HasFlag("cashflow.reinvest"),
        ),
        message = story(
            "Киоск, своя накладная, Ваня — в прошлом. Амина учится в нормальной школе. " +
                "Ты открываешь второй лоток с другой партнёршей.",
            "Ты поняла: деньги в обороте работают. Деньги в кармане просто лежат. " +
                "Это и есть предпринимательская грамота — никакой школы не нужно.",
        ),
    ),
    ending(
        id = "ending_freedom",
        endingType = EndingType.FINANCIAL_FREEDOM,
        flavor = "🎯",
        priority = 140,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, 100_000L),
            cond(DEBT, LTE, 25_000L),
            Condition.HasFlag("kiosk.opened"),
        ),
        message = story(
            "Киоск работает, Амина здорова, долги под контролем. Впервые ты отказываешься от выгодного " +
                "предложения — не потому что нет денег, а потому что не хочешь.",
            "Свобода выбора — это тоже результат. И за неё ты заплатила каждым ранним утром на рынке.",
        ),
    ),
    ending(
        id = "ending_stability",
        endingType = EndingType.FINANCIAL_STABILITY,
        flavor = "😊",
        priority = 130,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, 50_000L),
            cond(DEBT, LTE, 40_000L),
        ),
        message = story(
            "Не богатство — но стабильность. Платёж Ваниному кредиту идёт вовремя, Амина учится, " +
                "ряд стоит на своём месте.",
            "Ты научилась считать оборот, держать выручку отдельно от кармана и говорить «нет» " +
                "красивым предложениям. Для начала — это очень много.",
        ),
    ),
    ending(
        id = "ending_p2p",
        endingType = EndingType.PAYCHECK_TO_PAYCHECK,
        flavor = "😰",
        priority = 110,
        conditions = listOf(Condition.HasFlag("arc.final_check")),
        message = story(
            "Выручка уходит в долг, долг уходит в товар, товар уходит в выручку. Круг замкнулся, " +
                "просвета пока нет.",
            "Амина выросла на рынке. Ты работаешь честно и много. Но денег как не было, так и нет. " +
                "Что-то в цепочке сломано — и ты ещё не нашла где.",
        ),
    ),
)
