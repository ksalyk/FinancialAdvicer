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
 * Данияр «В город» — `daniyar_90s` · era `kz_90s` · 1994 · KZT.
 *
 * REFERENCE graph: the first authored character. New characters should copy this
 * structure (arc builders → [buildEvents]; conditional + ending arcs → `conditionalEvents`).
 *
 * Lesson (Bible §6): money literacy from zero — a buffer lets you survive a bad month
 * and say "no"; record-keeping makes money visible; scams find the naive. Secondary:
 * the земляк network (Нурлан good advisor vs Руслан "шальной").
 *
 * Architecture (full rationale in `.claude/CHARACTER_STORY_DANIYAR.md`):
 *  - era globals are NOT wired in `EraRegistry`, and `Condition` has no YEAR field, so
 *    the historical timeline (МММ June '94 →…) is a *scheduled backbone*: Act I is a
 *    scripted chain (direct `next`), then each beat queues the next via `scheduleEvent`.
 *  - Between beats the engine fills months from `eventPool` (routine, scams, calls home)
 *    and `conditionalEvents` (debt crisis, burnout, wage arrears, bankruptcy).
 *  - 5 endings are single terminal-conditional nodes resolved by `capital` + flags.
 *
 * Numbers are the design starting point; final balance is tuned against the sim harness.
 */
class DaniyarScenarioGraph : ScenarioGraph() {

    override val initialPlayerState: PlayerState = PlayerState(
        capital = 3_000L,
        income = 6_000L,
        expenses = 5_000L,
        debt = 0L,
        debtPaymentMonthly = 0L,
        investments = 0L,
        investmentReturnRate = 0.05,
        stress = 45,
        financialKnowledge = 5,
        riskLevel = 25,
        month = 3,          // весна 1994 → МММ (scheduleEvent +3) ляжет на июнь '94
        year = 1994,
        characterId = "daniyar_90s",
        eraId = "kz_90s",
        currency = CurrencyCode.KZT,   // 1994: тенге уже введён, RUB/реформа не нужны
    )

    override val events: Map<String, kz.fearsom.financiallifev2.model.GameEvent> = listOf(
        arrivalArc(),
        mmmArc(),
        winterArc(),
        endgameArc(),
        regularLifeArc(),
    ).buildEvents()

    override val conditionalEvents: List<kz.fearsom.financiallifev2.model.GameEvent> = listOf(
        conditionalsArc(),
        endingsArc(),
    ).flattenEvents()

    override val eventPool: List<PoolEntry> = listOf(
        PoolEntry("normal_life", 10),       // routine dominates (Bible §5) + engine hard fallback
        PoolEntry("pool_call_home", 8),
        PoolEntry("pool_roommate", 6),
        PoolEntry("pool_side_work", 6),
        PoolEntry("pool_notebook", 6),
        PoolEntry("pool_bazaar", 5),
        PoolEntry("pool_kairat_tile", 4),
        PoolEntry("pool_banya", 4),
        PoolEntry("network_favor", 3),
        PoolEntry("pool_scam_goods", 3),    // scams stay rare; weights crushed once learned
        PoolEntry("pool_scam_fakejob", 2),
        PoolEntry("pool_scam_relative", 2),
    )
}

// ════════════════════════════════════════════════════════════════════════════
//  АКТ I — Прибытие  (scripted chain, direct `next`, NO monthly tick → Act I is safe)
// ════════════════════════════════════════════════════════════════════════════

private fun arrivalArc(): EventArc = arc(
    "arrival",
    event(
        id = "intro",
        flavor = "🌅",
        unique = true,
        tags = setOf("family"),
        message = story(
            "Ты выходишь из вагона на «Алматы-2» с клетчатой сумкой, узлом баурсаков от матери и " +
                "3 000 тенге в потайном кармане. Пахнет углём, мокрой газетой и чужой спешкой.",
            "Нурлан должен был встретить — его нет. На табло мигают буквы, будто город сам не уверен, " +
                "как теперь называется жизнь после рубля.",
        ),
        options = listOf(
            option("wait_warm", "Надеть отцовскую телогрейку и ждать", "🧥", "almaty_arrival"),
            option("ask_cop", "Спросить дорогу у милиционера", "👮", "almaty_arrival",
                Effect(knowledgeDelta = 1)),
            option("follow", "Пойти за толпой к остановке", "🚶", "almaty_arrival",
                Effect(stressDelta = -3)),
        ),
    ),
    event(
        id = "almaty_arrival",
        flavor = "🏙️",
        unique = true,
        message = story(
            "Нурлан находит тебя к вечеру — оранжевая жилетка, «Бонд» в зубах. Хлопает по плечу так, " +
                "что ты делаешь шаг назад. «Ну, аульный, доехал? Пошли».",
            "Общага на Райымбека: бывший техникум под рабочих. Четыре койки, шкаф, стол с трещиной. " +
                "Сосед Руслан, усатый, с золотой фиксой, пьёт «Nescafe» и крутит видеомагнитофон.",
            "«Оклад 6 000 в месяц, — говорит Нурлан. — Аванс через неделю. Жильё бесплатно. Еда — сами». " +
                "Шесть тысяч. Больше, чем отец берёт за полгода в колхозе.",
        ),
        options = listOf(
            option("shake_ruslan", "Пожать руку Руслану — почти земляк", "🤝", "obshaga",
                Effect(knowledgeDelta = 1, setFlags = setOf("net.t1"))),
            option("ask_nurlan", "Расспросить Нурлана про его путь в город", "🗣️", "obshaga",
                Effect(setFlags = setOf("net.t1"))),
            option("sleep", "Лечь спать — устал с дороги", "💤", "obshaga",
                Effect(stressDelta = -8)),
        ),
    ),
    event(
        id = "obshaga",
        flavor = "🔐",
        unique = true,
        message = story(
            "Руслан кивает на твою сумку: «Деньги под матрас не клади. Тут все хорошие — пока голодные не стали».",
            "У общей тумбочки замка нет. Койка верхняя, гвоздь в стене — твой.",
        ),
        options = listOf(
            option("lock", "Купить маленький замок — 80 ₸", "🔒", "first_day",
                Effect(capitalDelta = -80L, knowledgeDelta = 1)),
            option("sock", "Спрятать деньги в носок", "🧦", "first_day",
                Effect(stressDelta = 3)),
            option("trust_nurlan", "Попросить Нурлана подержать часть денег", "🙏", "first_day",
                Effect(setFlags = setOf("net.t1"))),
        ),
    ),
    event(
        id = "first_day",
        flavor = "🧱",
        unique = true,
        message = story(
            "Подъём в шесть. Чай с сушками на всех. Монолит, второй этаж заливают. " +
                "Прораб Тимур-ага командует коротко: «Нурлан, новенького на подсобку».",
            "Спина горит, ноги гудят. К обеду Нурлан суёт булку и плавленый сырок: «Первое время так. Втянешься».",
            "Вечером Руслан варит кашу на чайнике — на четверых пакет крупы и две банки тушёнки. " +
                "Ты впервые ешь с незнакомыми мужиками из одного котла.",
        ),
        options = listOf(
            option("share_food", "Скинуться с Русланом на хлеб и сахар", "🍞", "first_wage",
                Effect(capitalDelta = -150L, setFlags = setOf("net.t1"))),
            option("eat_quiet", "Поесть что дают и помолчать", "🤐", "first_wage"),
            option("ask_around", "Расспросить, как тут все устроились", "🗣️", "first_wage",
                Effect(knowledgeDelta = 1)),
        ),
    ),
    // Узел направления арки: запускает sim-фазу и планирует МММ на июнь '94 (+3).
    event(
        id = "first_wage",
        flavor = "🧧",
        unique = true,
        tags = setOf("family"),
        message = story(
            "Суббота. Нурлан выдаёт конверт: «3 000 аванс, остальное в конце месяца». " +
                "Стопка новых купюр в руке — столько ты не держал никогда.",
            "Руслан зовёт «отметить» — по 1 000 на стол: «Не отметить аванс — плохая примета». " +
                "По автомату дозвонилась мать: «Сынок, отец колено разбил, на таблетки полторы тысячи…» — " +
                "голос ровный, будто это мелочь, которую ты не заметишь.",
            "А Нурлан отзывает в сторону: «Скидываемся прорабу на той сына, 500 с носа. " +
                "Не скинешь — на хороший объект не позовут».",
        ),
        options = listOf(
            option("celebrate", "Отметить с ребятами — 1 000 ₸", "🥳", MONTHLY_TICK,
                Effect(capitalDelta = -1_000L, stressDelta = -8, riskDelta = 5,
                    setFlags = setOf("path.brother"),
                    scheduleEvent = ScheduledEvent("mmm_arrives", 3))),
            option("send_mother", "Послать матери на таблетки — 1 500 ₸", "📮", MONTHLY_TICK,
                Effect(capitalDelta = -1_500L, stressDelta = 8,
                    setFlags = setOf("path.family", "family.first"),
                    scheduleEvent = ScheduledEvent("mmm_arrives", 3))),
            option("pay_foreman", "Скинуться прорабу на той — 500 ₸", "🏗️", MONTHLY_TICK,
                Effect(capitalDelta = -500L, knowledgeDelta = 1,
                    setFlags = setOf("path.work", "net.t1"),
                    scheduleEvent = ScheduledEvent("mmm_arrives", 3))),
            option("keep_buffer", "Отложить почти всё в конверт «не трогать»", "✉️", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, setFlags = setOf("buffer.kept"),
                    scheduleEvent = ScheduledEvent("mmm_arrives", 3))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ II — backbone: МММ (июнь '94) → афтермат (авг '94)
// ════════════════════════════════════════════════════════════════════════════

private fun mmmArc(): EventArc = arc(
    "mmm",
    event(
        id = "mmm_arrives",
        flavor = "💸",
        unique = true,
        tags = setOf("scam", "scam.pyramid", "world"),
        schemeExplanation = "МММ платил «прибыль» старым вкладчикам из денег новых — это пирамида. " +
            "Пока приходят новые, кажется, что работает. Когда поток иссякает, теряют все, кто вошёл последним.",
        message = story(
            "Июнь. Третий вечер спора за столом. Руслан машет цветными билетами: «Вложил 2 000 — через месяц 4 000. Гарантия».",
            "Нурлан: «Это пирамида. Лёня Голубков в телевизоре — артист, ему платят за слова». " +
                "Сосед Аскар: «Я мать хочу втянуть, у неё 8 000 на книжке всё равно лежат».",
            "У тебя в кармане полторы тысячи. А где-то в Талдыкоргане мать копит на ремонт крыши — " +
                "и слушает, что скажет сын.",
        ),
        options = listOf(
            option("invest", "Вложить свои 2 000 ₸ — все поднимаются", "🎟️", MONTHLY_TICK,
                Effect(capitalDelta = -2_000L, riskDelta = 10, setFlags = setOf("scam.taken"),
                    scheduleEvent = ScheduledEvent("mmm_aftermath_lost", 2))),
            option("advise_mother", "Сказать матери вложить «книжку»", "📞", MONTHLY_TICK,
                Effect(riskDelta = 5, stressDelta = 5, setFlags = setOf("scam.taken", "scam.mother"),
                    scheduleEvent = ScheduledEvent("mmm_aftermath_mother", 2))),
            option("back_nurlan", "Поддержать Нурлана: «пирамида и есть»", "🛑", MONTHLY_TICK,
                Effect(knowledgeDelta = 3, setFlags = setOf("learned.scam.pyramid"),
                    scheduleEvent = ScheduledEvent("mmm_aftermath_safe", 2))),
            option("warn_mother", "Написать матери: «не лезь туда»", "✍️", MONTHLY_TICK,
                Effect(knowledgeDelta = 1, setFlags = setOf("learned.scam.pyramid", "family.trusted"),
                    scheduleEvent = ScheduledEvent("mmm_aftermath_safe", 2))),
        ),
    ),
    event(
        id = "mmm_aftermath_lost",
        flavor = "📉",
        unique = true,
        tags = setOf("scam", "scam.pyramid"),
        message = story(
            "Август. Руслана нет три дня. Койка пуста, видика нет. К офисам МММ — очереди, двери заперты.",
            "Твои 2 000 сгорели. На стройке мужики матерятся и считают, кто сколько потерял.",
        ),
        options = listOf(
            option("swallow", "Молча проглотить потерю", "😞", MONTHLY_TICK,
                Effect(stressDelta = 10, setFlags = setOf("learned.scam.pyramid", "lost_money_to_scam"),
                    scheduleEvent = ScheduledEvent("winter_gear", 2))),
            option("vow", "Сказать себе: больше никогда вслепую", "📓", MONTHLY_TICK,
                Effect(stressDelta = 5, knowledgeDelta = 3,
                    setFlags = setOf("learned.scam.pyramid", "lost_money_to_scam", "lesson.budget"),
                    scheduleEvent = ScheduledEvent("winter_gear", 2))),
        ),
    ),
    event(
        id = "mmm_aftermath_mother",
        flavor = "📉",
        unique = true,
        tags = setOf("scam", "scam.pyramid", "family"),
        message = story(
            "Август. Звонок. Мать — тихо, без слёз, и это хуже слёз: «Сынок, МММ закрыли. " +
                "Я вложила, как ты сказал… крыша теперь без денег. Прости».",
            "Ты держишь трубку и слушаешь гудки после.",
        ),
        options = listOf(
            option("repay_mother", "«Верну тебе, мать. Постепенно»", "🤝", MONTHLY_TICK,
                Effect(debtDelta = 3_000L, debtPaymentDelta = 500L, stressDelta = 10,
                    setFlags = setOf("debt.family", "learned.scam.pyramid", "lost_money_to_scam"),
                    scheduleEvent = ScheduledEvent("winter_gear", 2))),
            option("send_last", "Отвезти матери что осталось", "🚂", MONTHLY_TICK,
                Effect(capitalDelta = -800L, stressDelta = -5,
                    setFlags = setOf("learned.scam.pyramid", "lost_money_to_scam"),
                    scheduleEvent = ScheduledEvent("winter_gear", 2))),
        ),
    ),
    event(
        id = "mmm_aftermath_safe",
        flavor = "📉",
        unique = true,
        tags = setOf("scam", "scam.pyramid"),
        message = story(
            "Август. К офисам МММ — очереди, двери заперты. Руслан исчез куда-то в Оренбург. " +
                "На стройке мужики плачут — отдали последнее.",
            "Твоё — цело. Впервые город не сбил тебя с ног, потому что ты вовремя сказал «нет».",
        ),
        options = listOf(
            option("steady", "Работать дальше, без резких движений", "🧱", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, stressDelta = -3,
                    scheduleEvent = ScheduledEvent("winter_gear", 2))),
            option("help_men", "Объяснить молодым на стройке, как это устроено", "🗣️", MONTHLY_TICK,
                Effect(knowledgeDelta = 1, setFlags = setOf("net.t1"),
                    scheduleEvent = ScheduledEvent("winter_gear", 2))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ II — backbone: куртка (окт '94) → расплата зимой (дек '94)
// ════════════════════════════════════════════════════════════════════════════

private fun winterArc(): EventArc = arc(
    "winter",
    event(
        id = "winter_gear",
        flavor = "🧥",
        unique = true,
        tags = setOf("crisis"),
        message = story(
            "Октябрь. С гор тянет ледяным ветром. Нурлан: «Без нормального ватника на лесах застудишь почки».",
            "На барахолке среди контейнеров продавец трясёт чёрной кожаной косухой: «Брат, бери — 4 000, все девки твои». " +
                "Рядом висит брезентовый ватник и резиновые сапоги — уродливо, но 2 500.",
        ),
        options = listOf(
            option("kosukha", "Взять косуху — в ауле будут гордиться", "🧥", MONTHLY_TICK,
                Effect(capitalDelta = -4_000L, setFlags = setOf("gear.style"),
                    scheduleEvent = ScheduledEvent("cold_winter", 2))),
            option("vatnik", "Взять ватник и валенки — 2 500 ₸", "🧣", MONTHLY_TICK,
                Effect(capitalDelta = -2_500L, setFlags = setOf("gear.warm"),
                    scheduleEvent = ScheduledEvent("winter_steady", 2))),
            option("newspaper", "Утеплиться газетами под свитер", "📰", MONTHLY_TICK,
                Effect(setFlags = setOf("gear.none"),
                    scheduleEvent = ScheduledEvent("cold_winter", 2))),
        ),
    ),
    event(
        id = "cold_winter",
        flavor = "🤒",
        unique = true,
        tags = setOf("crisis"),
        message = story(
            "Декабрь. Морозы под −20. Просыпаешься с тяжёлым кашлем, грудь горит при каждом вдохе. " +
                "Косуха продувается, газеты не греют.",
            "Нурлан жёстко: «Не выйдешь завтра на бетон — на твоё место очередь с вокзала».",
        ),
        options = listOf(
            option("treat", "Отлежаться, купить лекарства — 2 000 ₸", "💊", MONTHLY_TICK,
                Effect(capitalDelta = -2_000L, stressDelta = 8,
                    scheduleEvent = ScheduledEvent("final_fork", 2))),
            option("push_sick", "Выйти больным на бетон", "🥶", MONTHLY_TICK,
                Effect(stressDelta = 15, riskDelta = 5,
                    scheduleEvent = ScheduledEvent("final_fork", 2))),
        ),
    ),
    event(
        id = "winter_steady",
        flavor = "💪",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Декабрь. В ватнике жарко, ты здоров, кладёшь кирпич шов в шов.",
            "Нурлан хвалит: «Бережёшь себя — сделаю старшим по инструменту». Это не деньги сразу, но это доверие.",
        ),
        options = listOf(
            option("promo", "Принять повышение — старший по инструменту", "🔧", MONTHLY_TICK,
                Effect(incomeDelta = 1_000L, knowledgeDelta = 1, setFlags = setOf("net.t2"),
                    scheduleEvent = ScheduledEvent("final_fork", 2))),
            option("stay_modest", "Поблагодарить, остаться как есть", "🙂", MONTHLY_TICK,
                Effect(stressDelta = -5,
                    scheduleEvent = ScheduledEvent("final_fork", 2))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ III — последняя развилка (фев '95) → расчёт (~май '95)
// ════════════════════════════════════════════════════════════════════════════

private fun endgameArc(): EventArc = arc(
    "endgame",
    event(
        id = "final_fork",
        flavor = "❄️",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Февраль. Стройку морозят на две недели. Нурлан: «Поехали в Алматы на разгрузку — " +
                "1 500 в сутки, десять дней, 15 000. Готов?»",
            "Руслан, объявившийся, тянет в другое: «Возят шубы из Оренбурга под реализацию. " +
                "Поставим на барахолке — наш процент. Не продадим — вернём». Звучит слишком гладко.",
        ),
        options = listOf(
            option("unload", "Поехать с Нурланом на разгрузку — 15 000 ₸", "👷", MONTHLY_TICK,
                Effect(capitalDelta = 15_000L, stressDelta = 6, setFlags = setOf("end.stable", "net.t2"),
                    scheduleEvent = ScheduledEvent("final_review", 3))),
            option("furs", "Вписаться с Русланом в шубы", "🧥", MONTHLY_TICK,
                Effect(debtDelta = 8_000L, debtPaymentDelta = 1_000L, riskDelta = 15,
                    setFlags = setOf("end.gamble"),
                    scheduleEvent = ScheduledEvent("shuby_result", 2))),
            option("wait_site", "Сидеть в общаге, ждать стройку", "⏳", MONTHLY_TICK,
                Effect(stressDelta = 5,
                    scheduleEvent = ScheduledEvent("final_review", 3))),
        ),
    ),
    event(
        id = "shuby_result",
        flavor = "🧥",
        unique = true,
        tags = setOf("crisis"),
        message = story(
            "Апрель. Шубы почти не пошли — барахолка завалена такими же. Половину Руслан вернул поставщику, " +
                "на вторую половину долг висит на тебе.",
            "Руслан разводит руками: «Не сезон, брат». Долг от этого меньше не становится.",
        ),
        options = listOf(
            option("dump_stock", "Дораспродать по себестоимости", "🏷️", MONTHLY_TICK,
                Effect(capitalDelta = 4_000L, debtDelta = -4_000L, stressDelta = 5,
                    scheduleEvent = ScheduledEvent("final_review", 3))),
            option("roll_debt", "Вернуть остаток, влезть в новый долг", "📉", MONTHLY_TICK,
                Effect(debtDelta = 2_000L, debtPaymentDelta = 500L, stressDelta = 8,
                    scheduleEvent = ScheduledEvent("final_review", 3))),
        ),
    ),
    event(
        id = "final_review",
        flavor = "📜",
        unique = true,
        message = story(
            "Прошёл год с лишним. Алматы изменился — и ты тоже. Ты раскладываешь записи на столе общаги.",
            "Нурлан зовёт прорабом, но просит вложиться в инструмент. Мать пишет: в ауле ждут твоего решения. " +
                "Сколько у тебя в конверте — столько у тебя и выбора.",
        ),
        options = listOf(
            option("settle", "Посчитать всё честно и решить", "🧾", MONTHLY_TICK,
                Effect(setFlags = setOf("arc.final_check"))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Рутина — пул (вес normal_life=10 доминирует; скамы редкие и гаснут после урока)
// ════════════════════════════════════════════════════════════════════════════

private fun regularLifeArc(): EventArc = arc(
    "regular_life",
    event(
        id = "normal_life",
        flavor = "🌥️",
        cooldownMonths = 1,
        message = story(
            "Обычный месяц на стройке. Подъём, бетон, чай, койка. Город не кормит бесплатно, но и не валит с ног.",
        ),
        options = listOf(
            option("work_on", "Просто работать дальше", "🧱", MONTHLY_TICK),
            option("put_aside", "Отложить немного в конверт", "✉️", MONTHLY_TICK,
                Effect(capitalDelta = 200L, setFlags = setOf("buffer.kept"))),
            option("rest", "Отдохнуть вечером, выспаться", "😴", MONTHLY_TICK,
                Effect(stressDelta = -2)),
        ),
    ),
    event(
        id = "pool_call_home",
        flavor = "📞",
        tags = setOf("family"),
        cooldownMonths = 2,
        message = story(
            "Звонит мать. Голос тёплый и осторожный: «Как ты там, сынок? Не голодаешь?»",
        ),
        options = listOf(
            option("send_1000", "Послать домой 1 000 ₸", "📮", MONTHLY_TICK,
                Effect(capitalDelta = -1_000L)),
            option("send_3000", "Послать домой 3 000 ₸", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -3_000L, stressDelta = -3)),
            option("honest", "Сказать честно: «коплю, скоро помогу»", "🗣️", MONTHLY_TICK,
                Effect(knowledgeDelta = 1)),
        ),
    ),
    event(
        id = "pool_notebook",
        flavor = "📒",
        maxOccurrences = 1,
        message = story(
            "Ночью Нурлан сидит на койке и пишет в школьной тетради: хлеб, дорога, чай, долг, отправил домой.",
            "«Я просто два раза уже оставался без денег за неделю до зарплаты», — говорит он, не отрываясь.",
        ),
        options = listOf(
            option("own_notebook", "Завести свою тетрадь учёта", "✍️", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, setFlags = setOf("lesson.budget"))),
            option("remember", "«И так всё помню»", "🤷", MONTHLY_TICK),
        ),
    ),
    event(
        id = "pool_side_work",
        flavor = "🔨",
        tags = setOf("career"),
        cooldownMonths = 2,
        message = story(
            "Нурлан подкидывает вечернюю халтуру — поставить пару дверей у знакомого. Немного, но живые деньги.",
        ),
        options = listOf(
            option("take_side", "Взять халтуру — +1 500 ₸", "🔧", MONTHLY_TICK,
                Effect(capitalDelta = 1_500L, stressDelta = 4)),
            option("rest_side", "Отдохнуть, силы не казённые", "😮‍💨", MONTHLY_TICK,
                Effect(stressDelta = -3)),
        ),
    ),
    event(
        id = "pool_kairat_tile",
        flavor = "🧰",
        tags = setOf("career"),
        cooldownMonths = 3,
        maxOccurrences = 2,
        message = story(
            "Старый мастер Кайрат зовёт помочь с ремонтом квартиры. Платит немного, зато показывает, как класть плитку.",
            "«Учись руками, не только спиной», — говорит Нурлан.",
        ),
        options = listOf(
            option("learn_tile", "Учиться ремеслу у Кайрата", "🧱", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, setFlags = setOf("skill.tile"))),
            option("skip_tile", "Не до того сейчас", "🚶", MONTHLY_TICK),
        ),
    ),
    event(
        id = "pool_bazaar",
        flavor = "🛍️",
        cooldownMonths = 3,
        message = story(
            "Воскресенье. Барахолка гудит: ряды одежды, фруктов, запах жареной картошки. " +
                "Ты ищешь, что привезти домой.",
        ),
        options = listOf(
            option("gift_mother", "Купить матери подарок — 800 ₸", "🎁", MONTHLY_TICK,
                Effect(capitalDelta = -800L, stressDelta = -3)),
            option("just_walk", "Пройтись без покупок", "👀", MONTHLY_TICK),
        ),
    ),
    event(
        id = "network_favor",
        flavor = "🤝",
        tags = setOf("family"),
        maxOccurrences = 1,
        message = story(
            "Знакомый парнишка из вашего аула попал в милицию на барахолке — придрались к прописке. " +
                "Нужен залог 1 500 и кто-то, кто поручится.",
        ),
        options = listOf(
            option("bail_out", "Выручить земляка — 1 500 ₸", "🫶", MONTHLY_TICK,
                Effect(capitalDelta = -1_500L, setFlags = setOf("net.t2"))),
            option("walk_past", "Пройти мимо — у тебя свои проблемы", "🙈", MONTHLY_TICK,
                Effect(stressDelta = 8)),
        ),
    ),
    event(
        id = "pool_banya",
        flavor = "🛁",
        cooldownMonths = 3,
        message = story(
            "Суббота, баня. Пар, веник, мужской разговор ни о чём и обо всём сразу.",
        ),
        options = listOf(
            option("banya_nurlan", "Пойти с Нурланом и бригадой", "🤝", MONTHLY_TICK,
                Effect(stressDelta = -5, setFlags = setOf("net.t1"))),
            option("banya_alone", "Сходить одному, отдохнуть", "🧘", MONTHLY_TICK,
                Effect(stressDelta = -4)),
        ),
    ),
    event(
        id = "pool_roommate",
        flavor = "🌃",
        cooldownMonths = 2,
        message = story(
            "Вечер в общаге. Телевизор бубнит, кто-то режется в нарды, кто-то рассказывает про «верную тему».",
        ),
        options = listOf(
            option("listen", "Слушать разговоры про подработки", "👂", MONTHLY_TICK,
                Effect(knowledgeDelta = 1)),
            option("sleep_early", "Лечь спать пораньше", "💤", MONTHLY_TICK,
                Effect(stressDelta = -3)),
        ),
    ),
    // ── Пул-скамы (single-use; рефуз ставит learned.scam.{type} → движок режет вес) ──
    event(
        id = "pool_scam_fakejob",
        flavor = "🪪",
        tags = setOf("scam", "scam.fake_job"),
        conditions = listOf(Condition.NotFlag("learned.scam.fake_job")),
        schemeExplanation = "Классический «развод на работника»: берут деньги «за оформление» вперёд " +
            "и исчезают. Настоящий работодатель не просит платить за то, чтобы тебя взяли.",
        message = story(
            "У автовокзала мужчина в кожаной кепке обещает тёплую работу при складе — не стройка, не грязь, платят больше. " +
                "«Только за оформление 1 800 сегодня». Говорит спокойно. Даже слишком спокойно.",
        ),
        options = listOf(
            option("pay_fee", "Отдать 1 800 ₸ за «оформление»", "💵", MONTHLY_TICK,
                Effect(capitalDelta = -1_800L, setFlags = setOf("lost_money_to_scam"))),
            option("ask_address", "Попросить адрес конторы и договор", "📋", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, setFlags = setOf("learned.scam.fake_job"))),
            option("walk_off", "Уйти, пока он не начал злиться", "🚶", MONTHLY_TICK,
                Effect(setFlags = setOf("learned.scam.fake_job"))),
        ),
    ),
    event(
        id = "pool_scam_goods",
        flavor = "🧥",
        tags = setOf("scam", "scam.fake_goods"),
        conditions = listOf(Condition.NotFlag("learned.scam.fake_goods")),
        message = story(
            "На барахолке продавец расхваливает «таджикскую дублёнку, натуральная кожа» за 4 000. " +
                "По виду — почти как в магазине, только дешевле. «Последняя, бери, брат».",
        ),
        options = listOf(
            option("buy_coat", "Купить дублёнку — 4 000 ₸", "🧥", MONTHLY_TICK,
                Effect(capitalDelta = -4_000L, setFlags = setOf("lost_money_to_scam"),
                    scheduleEvent = ScheduledEvent("goods_peeled", 3))),
            option("refuse_coat", "Отказаться — деньги в обрез", "🚶", MONTHLY_TICK,
                Effect(knowledgeDelta = 1, setFlags = setOf("learned.scam.fake_goods"))),
        ),
    ),
    event(
        id = "pool_scam_relative",
        flavor = "🧍",
        tags = setOf("scam", "scam.relative"),
        conditions = listOf(Condition.NotFlag("learned.scam.relative")),
        message = story(
            "В общагу заходит «двоюродный брат» земляка: складно говорит, называет общих родственников. " +
                "«Одолжи 2 000 до понедельника, кровь из носу надо, отдам с процентом».",
        ),
        options = listOf(
            option("lend_relative", "Дать в долг — 2 000 ₸", "🤲", MONTHLY_TICK,
                Effect(capitalDelta = -2_000L, setFlags = setOf("lost_money_to_scam"))),
            option("refuse_relative", "Отказать вежливо", "🙅", MONTHLY_TICK,
                Effect(knowledgeDelta = 1, setFlags = setOf("learned.scam.relative"))),
        ),
    ),
    // Отложенное последствие «дублёнки» (scheduled-only target, в пул НЕ входит).
    event(
        id = "goods_peeled",
        flavor = "😕",
        unique = true,
        tags = setOf("scam", "scam.fake_goods"),
        message = story(
            "Через пару месяцев «кожа» дублёнки облезает клочьями. Мать по телефону: «Ай, сынок, не надо было».",
            "Дёшево — не всегда дёшево.",
        ),
        options = listOf(
            option("note_lesson", "Запомнить урок", "📓", MONTHLY_TICK,
                Effect(knowledgeDelta = 1, stressDelta = 4, setFlags = setOf("learned.scam.fake_goods"))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Условные события (priority desc): давление без спойлеров
// ════════════════════════════════════════════════════════════════════════════

private fun conditionalsArc(): EventArc = arc(
    "conditionals",
    event(
        id = "debt_crisis",
        flavor = "⛓️",
        tags = setOf("crisis"),
        priority = 100,
        cooldownMonths = 3,
        conditions = listOf(cond(DEBT, GT, 8_000L)),
        message = story(
            "Долг растёт быстрее, чем ты успеваешь отдавать. Кредитор намекает на «проценты сверху», " +
                "а в ломбарде уже знают тебя по имени.",
        ),
        options = listOf(
            option("pay_down", "Затянуть пояс, гасить долг", "✂️", MONTHLY_TICK,
                Effect(capitalDelta = -2_000L, debtDelta = -2_000L, stressDelta = 3)),
            option("sell_watch", "Продать часы — 700 ₸", "⌚", MONTHLY_TICK,
                Effect(capitalDelta = 700L, stressDelta = 5)),
            option("borrow_more", "Перезанять, чтобы закрыть сейчас", "🕳️", MONTHLY_TICK,
                Effect(debtDelta = 2_000L, debtPaymentDelta = 400L, stressDelta = 8)),
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
            "Ты измотан. Руки дрожат к вечеру, спишь плохо, на бетоне ловишь себя на том, что смотришь в одну точку.",
        ),
        options = listOf(
            option("rest_home", "Съездить на пару дней в аул", "🏡", MONTHLY_TICK,
                Effect(capitalDelta = -800L, stressDelta = -20)),
            option("push_through", "Переть дальше, деньги нужны", "🥵", MONTHLY_TICK,
                Effect(capitalDelta = 800L, stressDelta = 5)),
        ),
    ),
    event(
        id = "wage_arrears",
        flavor = "📭",
        tags = setOf("crisis"),
        priority = 80,
        cooldownMonths = 4,
        conditions = listOf(cond(CAPITAL, LT, 1_500L)),
        message = story(
            "Бригадир разводит руками: заказчик не рассчитался, зарплаты не будет ещё две недели. " +
                "На столе у тебя хлеб, чай и два последних яйца.",
        ),
        options = listOf(
            option("borrow_brigadier", "Занять у Нурлана под отработку", "🤝", MONTHLY_TICK,
                Effect(debtDelta = 3_000L, debtPaymentDelta = 500L, stressDelta = 4,
                    setFlags = setOf("debt.brigadier"))),
            option("day_labor", "Взять любую дневную работу", "🧹", MONTHLY_TICK,
                Effect(capitalDelta = 1_200L, stressDelta = 6)),
            option("endure", "Дотерпеть на том, что осталось", "🍞", MONTHLY_TICK,
                Effect(stressDelta = 8)),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Концовки — терминальные условные узлы (priority desc; решаются по capital + флагам)
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
            "Ты возвращаешься в аул раньше, чем хотел. Пустая сумка, тяжёлое молчание в автобусе.",
            "Больнее всего не потерянные деньги — а то, что теперь ты точно знаешь, где именно ошибся.",
        ),
    ),
    ending(
        id = "ending_wealth",
        endingType = EndingType.WEALTH,
        flavor = "🤑",
        priority = 150,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, 22_000L),
            Condition.HasFlag("net.t2"),
            Condition.HasFlag("learned.scam.pyramid"),
        ),
        message = story(
            "Не империя. Просто редкая для 90-х победа: стабильная бригада, репутация, инструмент, резерв, " +
                "помощь дому без самоуничтожения.",
            "Земляки доверяют тебе, ты научился считать и не верить пустым обещаниям. " +
                "Для парня из аула это и есть большое богатство.",
        ),
    ),
    ending(
        id = "ending_freedom",
        endingType = EndingType.FINANCIAL_FREEDOM,
        flavor = "🎯",
        priority = 140,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, 14_000L),
            Condition.HasFlag("learned.scam.pyramid"),
        ),
        message = story(
            "Ты впервые отказываешься от сомнительной работы без страха — не из гордости, " +
                "а потому что можешь позволить себе сказать «нет».",
            "Подушка, ясная голова и год, который ничему не научил тебя зря.",
        ),
    ),
    ending(
        id = "ending_stability",
        endingType = EndingType.FINANCIAL_STABILITY,
        flavor = "😊",
        priority = 130,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, 7_000L),
        ),
        message = story(
            "Тетрадь, нормальные ботинки, свой инструмент, люди, которым можно звонить, " +
                "и конверт, который не трогают без причины.",
            "Ты ещё не богат. Но город больше не свалит тебя одним плохим месяцем.",
        ),
    ),
    ending(
        id = "ending_p2p",
        endingType = EndingType.PAYCHECK_TO_PAYCHECK,
        flavor = "😰",
        priority = 110,
        conditions = listOf(Condition.HasFlag("arc.final_check")),
        message = story(
            "Зарплата уходит в зарплату. Матери — сколько можешь, себе — сколько осталось. Запаса нет.",
            "Заболеешь — идти к Нурлану занимать. Ты работаешь честно. Но легче пока не становится.",
        ),
    ),
)
