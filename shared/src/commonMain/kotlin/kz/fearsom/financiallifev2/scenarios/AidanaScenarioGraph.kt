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
 * Айдана «Первая зарплата» — `aidana_2024` · era `kz_2024` · 2024 · KZT.
 *
 * Logline: 26, Алматы, маркетолог. Первая «взрослая» зарплата 350 000 ₸. Снимает
 * квартиру, телефон в рассрочку, подписки, бранчи. Денег почему-то не остаётся.
 *
 * Lesson (Bible §8): the foundational loop — budget a real salary, build a 3–6 month
 * emergency fund, dodge Kaspi-рассрочка / credit-card traps, make a first investment.
 * Secondary: lifestyle creep, crypto hype scams.
 *
 * Pressure point: ~0 emergency fund + existing рассрочка — one shock (job wobble,
 * медрасход) and she's in the красная зона. Creep eats the surplus.
 *
 * 5 arcs, 26 story events + 3 conditional + 5 terminal endings.
 * Pattern mirrors [DaniyarScenarioGraph] (the reference).
 */
class AidanaScenarioGraph : ScenarioGraph() {

    override val initialPlayerState: PlayerState = PlayerState(
        capital = 150_000L,
        income = 350_000L,
        expenses = 280_000L,
        debt = 90_000L,
        debtPaymentMonthly = 15_000L,
        investments = 0L,
        investmentReturnRate = 0.10,
        stress = 35,
        financialKnowledge = 12,
        riskLevel = 20,
        month = 1,
        year = 2024,
        characterId = "aidana_2024",
        eraId = "kz_2024",
        currency = CurrencyCode.KZT,
    )

    override val events: Map<String, kz.fearsom.financiallifev2.model.GameEvent> = listOf(
        firstSalaryArc(),
        cryptoArc(),
        emergencyFundArc(),
        investmentArc(),
        endgameArc(),
        regularLifeArc(),
    ).buildEvents()

    override val conditionalEvents: List<kz.fearsom.financiallifev2.model.GameEvent> = listOf(
        conditionalsArc(),
        endingsArc(),
    ).flattenEvents()

    override val eventPool: List<PoolEntry> = listOf(
        PoolEntry("normal_life", 10),
        PoolEntry("pool_brunch", 8),
        PoolEntry("pool_subscription", 6),
        PoolEntry("pool_kaspi_banner", 6),
        PoolEntry("pool_colleague_advice", 5),
        PoolEntry("pool_mom_advice", 5),
        PoolEntry("pool_kamila_signal", 4),
        PoolEntry("pool_side_gig", 4),
        PoolEntry("pool_instagram_coach", 3),
        PoolEntry("pool_scam_forex", 2),
        PoolEntry("pool_scam_invest", 2),
    )
}

// ════════════════════════════════════════════════════════════════════════════
//  АКТ I — Первая зарплата (scripted chain, Act I is safe — direct `next`)
// ════════════════════════════════════════════════════════════════════════════

private fun firstSalaryArc(): EventArc = arc(
    "first_salary",
    event(
        id = "intro",
        flavor = "💼",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Первая зарплата упала на Kaspi — 350 000 ₸. Чувство — будто богатая. " +
                "Смотришь на баланс три раза подряд, не веришь.",
            "Сторис подруги Камилы: \"+40% на крипте за неделю, пишите, скину сигнал 🚀\". " +
                "В приложении баннер: «iPhone 16 в рассрочку 0-0-24, всего 35 000 ₸/мес». " +
                "Ещё есть долг за старый телефон — 15 000 ₸ в месяц.",
        ),
        options = listOf(
            option("save_cushion", "Отложить 100 000 ₸ на подушку, остальное — по плану", "💰",
                "first_month_review",
                Effect(capitalDelta = 100_000L, knowledgeDelta = 2, setFlags = setOf("choice.save"))),
            option("iphone_installment", "Взять iPhone в рассрочку — все берут", "📱",
                "first_month_review",
                Effect(debtDelta = 420_000L, debtPaymentDelta = 35_000L, stressDelta = -3,
                    setFlags = setOf("choice.iphone", "debt.kaspi"))),
            option("crypto_signal", "Закинуть 50 000 ₸ в крипту по сигналу Камилы", "🎰",
                "crypto_signal_first",
                Effect(capitalDelta = -50_000L, riskDelta = 10, setFlags = setOf("choice.crypto"))),
        ),
    ),
    event(
        id = "first_month_review",
        flavor = "📊",
        unique = true,
        message = story(
            "Конец первого месяца. Ты открываешь Kaspi и смотришь, куда ушли деньги. " +
                "Доставки — 45 000 ₸. Кафе и бранчи — 38 000 ₸. Подписки — 12 000 ₸. " +
                "Ещё аренда, коммуналка, продукты.",
            "Коллега Дина смотрит через плечо: «Я первые три месяца вела таблицу каждый тенге. " +
                "Скинула шаблон, если хочешь». Камила пишет: «Зря не взяла сигнал, ещё +18% сегодня».",
        ),
        options = listOf(
            option("start_budget", "Взять шаблон у Дины и начать вести бюджет", "📋",
                "budget_lesson",
                Effect(knowledgeDelta = 3, setFlags = setOf("habit.budget"))),
            option("cut_delivery", "Самой отрезать доставки и кафе", "✂️",
                "budget_lesson",
                Effect(expensesDelta = -25_000L, knowledgeDelta = 1, stressDelta = 4)),
            option("trust_feeling", "И так всё под контролем, примерно считаю в уме", "🤷",
                "budget_lesson"),
        ),
    ),
    event(
        id = "budget_lesson",
        flavor = "📱",
        unique = true,
        message = story(
            "Мама звонит в воскресенье: «Айдана, ты откладываешь на чёрный день? " +
                "Вдруг работу потеряешь — что будешь делать?»",
            "Ты молчишь секунду. На карте сейчас 87 000 ₸ — это меньше трети месячных расходов. " +
                "Мама продолжает: «Мы всегда держали три оклада в Сбербанке, это правило».",
        ),
        options = listOf(
            option("take_mom_advice", "Начать откладывать 50 000 ₸ каждый месяц", "🏦",
                MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, knowledgeDelta = 2,
                    setFlags = setOf("goal.cushion"),
                    scheduleEvent = ScheduledEvent("kaspi_temptation", 2))),
            option("small_step", "Отложить хотя бы 20 000 ₸ — с чего-то надо начать", "📦",
                MONTHLY_TICK,
                Effect(capitalDelta = -20_000L, knowledgeDelta = 1,
                    scheduleEvent = ScheduledEvent("kaspi_temptation", 2))),
            option("not_yet", "Сначала закрыть старый долг, потом буду копить", "💳",
                MONTHLY_TICK,
                Effect(debtDelta = -15_000L,
                    scheduleEvent = ScheduledEvent("kaspi_temptation", 2))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ I — Крипто-сигнал Камилы (ветка если выбрала крипту)
// ════════════════════════════════════════════════════════════════════════════

private fun cryptoArc(): EventArc = arc(
    "crypto",
    event(
        id = "crypto_signal_first",
        flavor = "📈",
        unique = true,
        tags = setOf("scam", "scam.crypto"),
        schemeExplanation = "«Сигналы» в крипте — это или угадывание, или pump-and-dump: " +
            "организатор покупает токен, рекламирует его тысячам подписчиков, продаёт на пике. " +
            "Подписчики остаются с упавшим токеном. Никакого «инсайда» не существует.",
        message = story(
            "Камила скидывает ссылку на Telegram-канал «Crypto by Mila 🔥» — 12 000 подписчиков. " +
                "«Захожу в MATIC, прогноз x3 за неделю. Я уже зашла».",
            "Ты вложила 50 000 ₸. Через три дня в приложении биржи — плюс 18%. " +
                "Пальцы тянутся вложить ещё.",
        ),
        options = listOf(
            option("double_down", "Добавить ещё 80 000 ₸ — momentum есть", "🚀",
                "crypto_pump_crash",
                Effect(capitalDelta = -80_000L, riskDelta = 15, stressDelta = -5)),
            option("take_profit", "Зафиксировать прибыль и выйти — 59 000 ₸ в кармане", "💵",
                "crypto_signal_lesson",
                Effect(capitalDelta = 59_000L, knowledgeDelta = 1,
                    setFlags = setOf("learned.scam.crypto"))),
            option("hold_curious", "Держать, смотреть что будет", "👀",
                "crypto_pump_crash",
                Effect(stressDelta = 5, riskDelta = 5)),
        ),
    ),
    event(
        id = "crypto_pump_crash",
        flavor = "📉",
        unique = true,
        tags = setOf("scam", "scam.crypto"),
        message = story(
            "Через неделю MATIC просел на 55% за ночь. Камила пишет в чате: " +
                "«Ребята, ничего страшного, это временно, держим 💎🙏». Организатор канала — в тишине.",
            "На бирже у тебя теперь осколки. Выводить или ждать «отскока»?",
        ),
        options = listOf(
            option("cut_loss", "Зафиксировать убыток и выйти — вернуть что осталось", "🩸",
                MONTHLY_TICK,
                Effect(capitalDelta = -65_000L, knowledgeDelta = 3, stressDelta = 10,
                    setFlags = setOf("learned.scam.crypto", "lost_money_to_scam"),
                    scheduleEvent = ScheduledEvent("kaspi_temptation", 2))),
            option("hodl_hope", "Держать — «всё вернётся»", "🤞",
                MONTHLY_TICK,
                Effect(capitalDelta = -130_000L, riskDelta = 5, stressDelta = 15,
                    setFlags = setOf("learned.scam.crypto", "lost_money_to_scam"),
                    scheduleEvent = ScheduledEvent("kaspi_temptation", 2))),
        ),
    ),
    event(
        id = "crypto_signal_lesson",
        flavor = "🎓",
        unique = true,
        tags = setOf("scam", "scam.crypto"),
        message = story(
            "Через месяц Камила пишет, что потеряла 200 000 ₸ — «держала до конца». " +
                "Её MATIC сейчас стоит 15% от цены входа.",
            "Ты вышла вовремя. Повезло — или почувствовала?",
        ),
        options = listOf(
            option("lesson_noted", "Запомнить: сигналы — не аналитика", "📓",
                MONTHLY_TICK,
                Effect(knowledgeDelta = 2, setFlags = setOf("learned.scam.crypto"),
                    scheduleEvent = ScheduledEvent("kaspi_temptation", 2))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ II — Рассрочки, подушка, шок (backbone)
// ════════════════════════════════════════════════════════════════════════════

private fun emergencyFundArc(): EventArc = arc(
    "emergency_fund",
    event(
        id = "kaspi_temptation",
        flavor = "🛒",
        unique = true,
        tags = setOf("lifestyle"),
        message = story(
            "Распродажа 11.11 на Kaspi. Смартвотч 89 000 ₸ — «0-0-12, 7 400 ₸/мес». " +
                "Новый диван 180 000 ₸ — «0-0-24». Ещё AirPods, кросовки, кухонный комбайн.",
            "Дина говорит: «Рассрочки бесплатные — только не набирай их до потолка. " +
                "Считай сумму всех ежемесячных платежей, не цену каждой вещи».",
        ),
        options = listOf(
            option("buy_watch_only", "Взять только смартвотч — 7 400 ₸/мес, хочу давно", "⌚",
                "medical_shock",
                Effect(debtDelta = 89_000L, debtPaymentDelta = 7_400L, stressDelta = -3,
                    setFlags = setOf("debt.kaspi"))),
            option("buy_several", "Диван + смартвотч + AirPods, всё равно рассрочка бесплатная", "🛍️",
                "medical_shock",
                Effect(debtDelta = 349_000L, debtPaymentDelta = 31_500L, stressDelta = -5,
                    setFlags = setOf("debt.kaspi", "debt.heavy"))),
            option("skip_sale", "Пройти мимо — распродажи бывают каждый месяц", "🚶",
                "medical_shock",
                Effect(knowledgeDelta = 2, setFlags = setOf("habit.nodebts"))),
        ),
    ),
    event(
        id = "medical_shock",
        flavor = "😷",
        unique = true,
        tags = setOf("crisis"),
        message = story(
            "Начало марта. Просыпаешься с высокой температурой, к вечеру — скорая. " +
                "Пневмония. Больничный неоплачиваемый — ты же на испытательном сроке.",
            "Лечение и анализы: 65 000 ₸. Работать нельзя 10 дней. " +
                "На карте — всё что есть. Звонит арендодатель: аренда через 5 дней.",
        ),
        options = listOf(
            option("use_cushion", "Оплатить из накоплений — для этого и копила", "💳",
                "after_shock",
                Effect(capitalDelta = -65_000L, stressDelta = 5)),
            option("ask_mom", "Попросить маму перевести — «верну с зарплаты»", "📞",
                "after_shock",
                Effect(debtDelta = 65_000L, stressDelta = 8, setFlags = setOf("debt.family"))),
            option("microfinance", "Взять в МФО «срочный займ» 80 000 под 2% в день", "🏦",
                "after_shock",
                Effect(capitalDelta = 80_000L, debtDelta = 80_000L, debtPaymentDelta = 18_000L,
                    stressDelta = 15, setFlags = setOf("debt.mfo"))),
        ),
    ),
    event(
        id = "after_shock",
        flavor = "💪",
        unique = true,
        message = story(
            "Две недели спустя. Ты снова на работе, баланс восстановился — немного.",
            "Дина: «Видишь теперь зачем подушка? Не для красоты. Один форс-мажор — и всё». " +
                "Она говорит, что держит 400 000 ₸ на депозите в Халык под 14% годовых: " +
                "«Деньги работают, и я сплю спокойно».",
        ),
        options = listOf(
            option("start_deposit", "Открыть депозит с целью 400 000 ₸ за год", "🏦",
                MONTHLY_TICK,
                Effect(knowledgeDelta = 3, setFlags = setOf("goal.deposit"),
                    scheduleEvent = ScheduledEvent("investment_fork", 3))),
            option("pay_debt_first", "Сначала закрыть MFO или Kaspi-долги", "💰",
                MONTHLY_TICK,
                Effect(debtDelta = -30_000L, debtPaymentDelta = -5_000L, knowledgeDelta = 1,
                    scheduleEvent = ScheduledEvent("investment_fork", 3))),
            option("just_keep_going", "Работать дальше, разберусь со временем", "🤷",
                MONTHLY_TICK,
                Effect(scheduleEvent = ScheduledEvent("investment_fork", 3))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ II — Первые инвестиции и рыночный тест
// ════════════════════════════════════════════════════════════════════════════

private fun investmentArc(): EventArc = arc(
    "investment",
    event(
        id = "investment_fork",
        flavor = "💹",
        unique = true,
        tags = setOf("investment"),
        message = story(
            "Лето. На карте — 280 000 ₸ после всех расходов. Ты думаешь о первых инвестициях.",
            "Варианты: депозит в Халык под 14% («скучно, но надёжно», говорит Дина), " +
                "ETF на Казахстанской бирже через Freedom Finance, " +
                "или снова крипта — Камила опять кричит про какой-то новый токен.",
        ),
        options = listOf(
            option("invest_deposit", "Открыть депозит — 200 000 ₸ под 14% годовых", "🏦",
                "invest_result_steady",
                Effect(capitalDelta = -200_000L, investmentsDelta = 200_000L,
                    knowledgeDelta = 2,
                    setFlags = setOf("invest.deposit"))),
            option("invest_etf", "Купить ETF — 150 000 ₸, диверсификация", "📊",
                "invest_result_etf",
                Effect(capitalDelta = -150_000L, investmentsDelta = 150_000L,
                    riskDelta = 5, knowledgeDelta = 3,
                    setFlags = setOf("invest.etf"))),
            option("invest_kamila_crypto", "Слушать Камилу — 100 000 ₸ в «новый токен»", "🎰",
                "invest_result_crypto",
                Effect(capitalDelta = -100_000L, riskDelta = 20, stressDelta = 5,
                    setFlags = setOf("invest.crypto"))),
            option("no_invest", "Ничего не делать — пока не разобралась", "🤷",
                MONTHLY_TICK,
                Effect(scheduleEvent = ScheduledEvent("market_dip", 3))),
        ),
    ),
    event(
        id = "invest_result_steady",
        flavor = "📈",
        unique = true,
        tags = setOf("investment"),
        message = story(
            "Депозит открыт. Каждый месяц — тихая строка «начислены проценты: +2 333 ₸». " +
                "Камила смеётся: «Это не инвестиции, это детский сад».",
            "Дина спокойна: «Камила потеряла 200 000 ₸. Ты — нет. Кто смеётся последней?»",
        ),
        options = listOf(
            option("add_monthly", "Пополнять депозит на 20 000 ₸ в месяц", "💰",
                MONTHLY_TICK,
                Effect(capitalDelta = -20_000L, investmentsDelta = 20_000L, knowledgeDelta = 1,
                    scheduleEvent = ScheduledEvent("market_dip", 3))),
            option("hold_deposit", "Оставить как есть, смотреть дальше", "👀",
                MONTHLY_TICK,
                Effect(scheduleEvent = ScheduledEvent("market_dip", 3))),
        ),
    ),
    event(
        id = "invest_result_etf",
        flavor = "📊",
        unique = true,
        tags = setOf("investment"),
        message = story(
            "ETF куплен через приложение Freedom Finance. Неделю ты каждое утро открываешь портфель — " +
                "то +2%, то −1.5%. Сердце немного сжимается при минусе.",
            "Статья попалась: «Долгосрочный инвестор не смотрит портфель каждый день». " +
                "Пробуешь закрыть приложение и не открывать неделю.",
        ),
        options = listOf(
            option("stay_calm", "Держать ETF без паники, докупать на просадках", "🧘",
                MONTHLY_TICK,
                Effect(knowledgeDelta = 2, stressDelta = -3,
                    scheduleEvent = ScheduledEvent("market_dip", 3))),
            option("half_out", "Продать половину — нервы не для этого", "😰",
                MONTHLY_TICK,
                Effect(capitalDelta = 75_000L, investmentsDelta = -75_000L, stressDelta = -5,
                    scheduleEvent = ScheduledEvent("market_dip", 3))),
        ),
    ),
    event(
        id = "invest_result_crypto",
        flavor = "🎲",
        unique = true,
        tags = setOf("scam", "scam.crypto"),
        schemeExplanation = "Большинство «новых токенов» — pump-and-dump или просто ничем не обеспеченные " +
            "проекты. Из тысяч альткоинов выжили единицы. Вход по сигналу Telegram-блогера — лотерея.",
        message = story(
            "Токен Камилы вырос на 30% за три дня. Ты уже считаешь прибыль. " +
                "На пятый день — минус 70% за ночь. «Технические работы», пишет организатор канала.",
            "Камила молчит три дня, потом пишет: «Я потеряла 350 000. Не говори маме».",
        ),
        options = listOf(
            option("exit_crypto_lesson", "Выйти с тем что осталось, записать урок", "📓",
                MONTHLY_TICK,
                Effect(capitalDelta = -70_000L, knowledgeDelta = 4, stressDelta = 10,
                    setFlags = setOf("learned.scam.crypto", "lost_money_to_scam"),
                    scheduleEvent = ScheduledEvent("market_dip", 3))),
        ),
    ),
    event(
        id = "market_dip",
        flavor = "📉",
        unique = true,
        tags = setOf("crisis", "investment"),
        message = story(
            "Осень. Тенге просел на 8% за две недели — курс доллара прыгнул. " +
                "Подруга пишет: «Ты в чём деньги держишь?». В Instagram — паника «всё в доллары».",
        ),
        options = listOf(
            option("stay_tenge", "Не паниковать — краткосрочные колебания нормальны", "🧘",
                MONTHLY_TICK,
                Effect(knowledgeDelta = 2,
                    scheduleEvent = ScheduledEvent("final_review", 2))),
            option("buy_usd", "Купить доллары на 100 000 ₸ — сохранить стоимость", "💵",
                MONTHLY_TICK,
                Effect(capitalDelta = -100_000L, investmentsDelta = 100_000L,
                    knowledgeDelta = 1, riskDelta = 5,
                    scheduleEvent = ScheduledEvent("final_review", 2))),
            option("panic_sell", "Вывести все инвестиции в наличные — «переждать»", "😱",
                MONTHLY_TICK,
                Effect(stressDelta = 12, knowledgeDelta = -1,
                    scheduleEvent = ScheduledEvent("final_review", 2))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ III — Итоги года (рекончиляция → финальный выбор → концовки)
// ════════════════════════════════════════════════════════════════════════════

private fun endgameArc(): EventArc = arc(
    "endgame",
    event(
        id = "final_review",
        flavor = "📜",
        unique = true,
        message = story(
            "Декабрь. Год подходит к концу. Ты смотришь на Kaspi — баланс, долги, инвестиции.",
            "Дина отправляет скрин своего портфеля: 600 000 ₸ на депозите. " +
                "«Скучно, зато в отпуск лечу без кредита». " +
                "Камила где-то читает про «лучший токен 2025».",
            "Пришло время честно посмотреть на цифры.",
        ),
        options = listOf(
            option("count_results", "Посчитать всё честно — доходы, расходы, долги, накопления", "🧾",
                MONTHLY_TICK,
                Effect(knowledgeDelta = 1, setFlags = setOf("arc.final_check"))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Рутина — пул (normal_life = 10, доминирует; скамы редкие)
// ════════════════════════════════════════════════════════════════════════════

private fun regularLifeArc(): EventArc = arc(
    "regular_life",
    event(
        id = "normal_life",
        flavor = "🌥️",
        cooldownMonths = 1,
        message = story(
            "Обычный месяц в офисе. Дедлайны, созвоны, аренда, продукты. " +
                "Алматы не дешевеет, зарплата та же. Каждый тенге на счету.",
        ),
        options = listOf(
            option("work_on", "Работать дальше, придерживаться бюджета", "💼", MONTHLY_TICK),
            option("save_extra", "Отложить лишние 10 000 ₸ в этом месяце", "💰", MONTHLY_TICK,
                Effect(capitalDelta = 10_000L, knowledgeDelta = 0)),
            option("rest_weekend", "Отдохнуть на выходных, погулять в парке", "🌳", MONTHLY_TICK,
                Effect(stressDelta = -3)),
        ),
    ),
    event(
        id = "pool_brunch",
        flavor = "☕",
        cooldownMonths = 2,
        message = story(
            "Камила зовёт на бранч в новое место. «Обязательно надо попробовать, " +
                "только 5 000 ₸ с человека, зато атмосфера 🌸».",
        ),
        options = listOf(
            option("go_brunch", "Пойти — хочется расслабиться", "🥂", MONTHLY_TICK,
                Effect(capitalDelta = -5_000L, stressDelta = -5)),
            option("home_food", "Предложить собраться дома — дешевле и уютнее", "🏠", MONTHLY_TICK,
                Effect(capitalDelta = -1_000L, stressDelta = -3, knowledgeDelta = 1)),
            option("skip_brunch", "Отказать — этот месяц не укладываюсь в бюджет", "🚶", MONTHLY_TICK,
                Effect(stressDelta = 3)),
        ),
    ),
    event(
        id = "pool_subscription",
        flavor = "📲",
        maxOccurrences = 1,
        message = story(
            "Ты замечаешь: Netflix, Spotify, iCloud, Kaspi Gold, " +
                "ещё какой-то сервис, который взяла по акции полгода назад.",
            "Итого — 8 подписок. Суммарно 14 200 ₸ в месяц. Треть используешь регулярно.",
        ),
        options = listOf(
            option("audit_subscriptions", "Отменить 5 ненужных — экономить 9 000 ₸/мес", "✂️",
                MONTHLY_TICK,
                Effect(expensesDelta = -9_000L, knowledgeDelta = 2,
                    setFlags = setOf("habit.subscription_audit"))),
            option("keep_all", "Оставить всё — зато не думаешь об этом", "📱", MONTHLY_TICK),
        ),
    ),
    event(
        id = "pool_kaspi_banner",
        flavor = "🛒",
        cooldownMonths = 3,
        conditions = listOf(Condition.NotFlag("habit.nodebts")),
        message = story(
            "В Kaspi — новая акция: «Оформи рассрочку сегодня, три месяца без переплат».",
        ),
        options = listOf(
            option("kaspi_buy", "Взять что-то на 60 000 ₸ в рассрочку", "💳", MONTHLY_TICK,
                Effect(debtDelta = 60_000L, debtPaymentDelta = 6_000L,
                    setFlags = setOf("debt.kaspi"))),
            option("kaspi_skip", "Не нужно — уже достаточно платежей", "🚶", MONTHLY_TICK,
                Effect(knowledgeDelta = 1)),
        ),
    ),
    event(
        id = "pool_colleague_advice",
        flavor = "🗣️",
        cooldownMonths = 3,
        message = story(
            "Дина за обедом: «Ты уже смотрела накопительные пенсионные? " +
                "ЕНПФ — скучно, но взносы идут автоматически. Можно добровольно добавлять».",
        ),
        options = listOf(
            option("check_pension", "Зайти в приложение и посмотреть пенсионный счёт", "🏛️",
                MONTHLY_TICK,
                Effect(knowledgeDelta = 2)),
            option("too_early", "Мне 26 лет, зачем думать о пенсии", "🤷", MONTHLY_TICK),
        ),
    ),
    event(
        id = "pool_mom_advice",
        flavor = "👩",
        tags = setOf("family"),
        cooldownMonths = 4,
        message = story(
            "Мама советует: «Купи квартиру по «7-20-25». " +
                "Первоначальный взнос 20% — это же всего 5 миллионов».",
            "«Всего» пять миллионов. У тебя на карте меньше 300 000 ₸.",
        ),
        options = listOf(
            option("research_mortgage", "Изучить условия и посчитать реально", "📋", MONTHLY_TICK,
                Effect(knowledgeDelta = 3)),
            option("dream_later", "Хочу, но пока нереально — буду копить", "🏠", MONTHLY_TICK,
                Effect(knowledgeDelta = 1)),
            option("borrow_first", "Занять на взнос у родителей — «сейчас шанс»", "📞", MONTHLY_TICK,
                Effect(debtDelta = 500_000L, debtPaymentDelta = 30_000L, stressDelta = 12,
                    setFlags = setOf("debt.family"))),
        ),
    ),
    event(
        id = "pool_kamila_signal",
        flavor = "📱",
        cooldownMonths = 3,
        conditions = listOf(Condition.NotFlag("learned.scam.crypto")),
        schemeExplanation = "«Сигналы» криптоблогеров — это почти всегда pump-and-dump или шум. " +
            "Настоящих инсайдеров на Telegram-каналах нет.",
        message = story(
            "Камила пишет: «Поймала новый сигнал — BNB x2 за месяц. " +
                "Серьёзно, мой аналитик никогда не ошибается 🚀🚀».",
        ),
        options = listOf(
            option("follow_signal", "Вложить 30 000 ₸", "🎰", MONTHLY_TICK,
                Effect(capitalDelta = -30_000L, riskDelta = 10,
                    setFlags = setOf("lost_money_to_scam"))),
            option("ask_mechanism", "Спросить: как именно он это предсказывает?", "🔍", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, setFlags = setOf("learned.scam.crypto"))),
            option("ignore_signal", "Игнорировать — уже была в этой игре", "🛑", MONTHLY_TICK,
                Effect(setFlags = setOf("learned.scam.crypto"))),
        ),
    ),
    event(
        id = "pool_side_gig",
        flavor = "💻",
        tags = setOf("career"),
        cooldownMonths = 3,
        message = story(
            "Знакомый предлагает проект — написать контент для сайта. " +
                "Неделя работы вечерами, 50 000 ₸.",
        ),
        options = listOf(
            option("take_gig", "Взяться — дополнительный доход", "✍️", MONTHLY_TICK,
                Effect(capitalDelta = 50_000L, stressDelta = 8,
                    setFlags = setOf("income.side"))),
            option("rest_instead", "Отказать — нужно восстановиться", "😴", MONTHLY_TICK,
                Effect(stressDelta = -5)),
        ),
    ),
    event(
        id = "pool_instagram_coach",
        flavor = "🤳",
        tags = setOf("scam", "scam.infocoach"),
        cooldownMonths = 4,
        conditions = listOf(Condition.NotFlag("learned.scam.infocoach")),
        schemeExplanation = "«Инфобизнес» — курсы личного роста и инвестиций часто продают " +
            "пустые обещания под видом «системы». Настоящие знания доступны бесплатно. " +
            "«Гарантированный пассивный доход» не существует.",
        message = story(
            "В Instagram реклама: «За 6 недель я зарабатываю пассивно 800 000 ₸. " +
                "Курс «Деньги и Я» — 120 000 ₸. Первые 10 мест уже заняты!»",
        ),
        options = listOf(
            option("buy_course", "Купить курс — 120 000 ₸", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -120_000L, stressDelta = 5,
                    setFlags = setOf("lost_money_to_scam"))),
            option("research_author", "Поискать автора в Интернете — кто он?", "🔍", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, setFlags = setOf("learned.scam.infocoach"))),
            option("free_books", "Найти бесплатные книги о финансах", "📚", MONTHLY_TICK,
                Effect(knowledgeDelta = 3, setFlags = setOf("learned.scam.infocoach"))),
        ),
    ),
    event(
        id = "pool_scam_forex",
        flavor = "📉",
        tags = setOf("scam", "scam.forex"),
        cooldownMonths = 4,
        conditions = listOf(Condition.NotFlag("learned.scam.forex")),
        schemeExplanation = "Нелицензированные форекс-брокеры не выводят деньги. " +
            "Требование «пополнить для вывода» — классическая схема вытягивания средств.",
        message = story(
            "В Telegram-чате: «Мой брокер +50% за месяц. Минимальный депозит 100$. " +
                "Приведи друга — бонус 20$».",
        ),
        options = listOf(
            option("try_forex", "Попробовать — 50 000 ₸", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, stressDelta = 8,
                    setFlags = setOf("lost_money_to_scam"))),
            option("check_license", "Проверить лицензию в Нацбанке", "📋", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, setFlags = setOf("learned.scam.forex"))),
            option("ignore_forex", "Не моя история, игнорировать", "🚶", MONTHLY_TICK,
                Effect(setFlags = setOf("learned.scam.forex"))),
        ),
    ),
    event(
        id = "pool_scam_invest",
        flavor = "🧲",
        tags = setOf("scam", "scam.invest"),
        cooldownMonths = 4,
        conditions = listOf(Condition.NotFlag("learned.scam.invest")),
        schemeExplanation = "Гарантированная доходность выше 20% годовых — признак пирамиды. " +
            "Настоящие инвестиции несут риск.",
        message = story(
            "Сосед Арман: «Вложил в компанию 500 000 ₸ — обещают 30% в месяц. " +
                "Уже получил первые выплаты. Вписывайся, пока места есть».",
        ),
        options = listOf(
            option("join_invest", "Вложить 150 000 ₸", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -150_000L, riskDelta = 20,
                    setFlags = setOf("lost_money_to_scam"),
                    scheduleEvent = ScheduledEvent("pyramid_crash", 3))),
            option("ask_mechanics", "Как именно они зарабатывают 30% в месяц?", "🔍", MONTHLY_TICK,
                Effect(knowledgeDelta = 3, setFlags = setOf("learned.scam.invest"))),
            option("say_no", "Поблагодарить и отказать — подозрительно", "🛑", MONTHLY_TICK,
                Effect(knowledgeDelta = 1, setFlags = setOf("learned.scam.invest"))),
        ),
    ),
    event(
        id = "pyramid_crash",
        flavor = "💥",
        unique = true,
        tags = setOf("scam", "scam.invest"),
        message = story(
            "Три месяца спустя. Арман пишет в ночи: «Всё — сайт не открывается. " +
                "Телефон не отвечает. Мы потеряли всё».",
            "150 000 ₸ испарились. Арман брал под это кредит.",
        ),
        options = listOf(
            option("accept_loss", "Принять потерю, больше не повторять", "📓", MONTHLY_TICK,
                Effect(knowledgeDelta = 3, stressDelta = 10,
                    setFlags = setOf("learned.scam.invest", "learned.scam.pyramid"))),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Условные события (priority desc)
// ════════════════════════════════════════════════════════════════════════════

private fun conditionalsArc(): EventArc = arc(
    "conditionals",
    event(
        id = "debt_spiral",
        flavor = "⛓️",
        tags = setOf("crisis"),
        priority = 100,
        cooldownMonths = 3,
        conditions = listOf(cond(DEBT, GT, 600_000L)),
        message = story(
            "Суммарный долг вырос больше 600 000 ₸. Ежемесячные платежи съедают больше половины дохода. " +
                "Kaspi присылает напоминания. Мама звонит: «Ты нормально живёшь?».",
        ),
        options = listOf(
            option("close_mfo", "Закрыть самый дорогой долг — приоритет МФО", "✂️", MONTHLY_TICK,
                Effect(capitalDelta = -80_000L, debtDelta = -80_000L, debtPaymentDelta = -18_000L,
                    stressDelta = 5)),
            option("refinance", "Попробовать рефинансировать в банке под меньший %", "🏦", MONTHLY_TICK,
                Effect(debtPaymentDelta = -10_000L, knowledgeDelta = 2, stressDelta = 3)),
            option("minimum_payment", "Платить только минимальные платежи, ждать", "⏳", MONTHLY_TICK,
                Effect(stressDelta = 10)),
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
            "Ты не можешь сосредоточиться. Открываешь одно приложение — сразу переключаешься на другое. " +
                "Ночью не спишь, думаешь о деньгах.",
        ),
        options = listOf(
            option("take_vacation", "Взять отпуск — 3 дня в Боровом, 40 000 ₸", "🌲", MONTHLY_TICK,
                Effect(capitalDelta = -40_000L, stressDelta = -25)),
            option("push_through", "Перетерпеть — нельзя сейчас тратить на отдых", "💪", MONTHLY_TICK,
                Effect(stressDelta = 8)),
        ),
    ),
    event(
        id = "low_capital_warning",
        flavor = "📭",
        tags = setOf("crisis"),
        priority = 80,
        cooldownMonths = 4,
        conditions = listOf(cond(CAPITAL, LT, 30_000L)),
        message = story(
            "На карте меньше 30 000 ₸. Аренда через неделю. " +
                "Если что-то сломается — нет резерва совсем.",
        ),
        options = listOf(
            option("freelance_urgently", "Срочно взять фриланс-заказ", "💻", MONTHLY_TICK,
                Effect(capitalDelta = 40_000L, stressDelta = 10)),
            option("ask_advance", "Попросить аванс у работодателя", "📋", MONTHLY_TICK,
                Effect(capitalDelta = 50_000L, stressDelta = 5,
                    scheduleEvent = ScheduledEvent("advance_deduction", 1))),
            option("borrow_kamila", "Занять у Камилы до зарплаты", "📞", MONTHLY_TICK,
                Effect(capitalDelta = 30_000L, debtDelta = 30_000L, stressDelta = 8)),
        ),
    ),
    event(
        id = "advance_deduction",
        flavor = "📋",
        unique = true,
        message = story(
            "В этом месяце аванс вычтут из зарплаты — придёт меньше чем обычно.",
        ),
        options = listOf(
            option("plan_reduced_salary", "Перестроить бюджет под меньшую сумму", "📊", MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, knowledgeDelta = 1)),
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
            "Баланс обнулился. Долги, рассрочки и один неудачный месяц — " +
                "и ты звонишь маме, объясняешь что не можешь заплатить аренду.",
            "Это не конец — просто точка остановки. " +
                "Больнее всего не цифры, а осознание: каждый маленький «всего 5 000» сложился в это.",
        ),
    ),
    ending(
        id = "ending_wealth",
        endingType = EndingType.WEALTH,
        flavor = "🤑",
        priority = 150,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, 700_000L),
            Condition.HasFlag("invest.deposit"),
            Condition.HasFlag("learned.scam.crypto"),
        ),
        message = story(
            "Год закончился лучше, чем начинался. Депозит растёт, долгов почти нет, " +
                "подушка — полгода расходов. Камила спрашивает у тебя совет.",
            "Это не богатство. Это foundation — то, с чего начинается настоящее.",
        ),
    ),
    ending(
        id = "ending_freedom",
        endingType = EndingType.FINANCIAL_FREEDOM,
        flavor = "🎯",
        priority = 140,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, 400_000L),
            cond(DEBT, LTE, 150_000L),
        ),
        message = story(
            "Ты впервые говоришь «нет» на рассрочку — не потому что нет денег, " +
                "а потому что не хочешь платить за это будущей свободой.",
            "Небольшой инвестиционный портфель, подушка, дисциплина. " +
                "Алматы всё такой же дорогой — но ты уже немного другая.",
        ),
    ),
    ending(
        id = "ending_stability",
        endingType = EndingType.FINANCIAL_STABILITY,
        flavor = "😊",
        priority = 130,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, 150_000L),
        ),
        message = story(
            "Бюджет есть, долги под контролем, небольшой резерв на непредвиденное.",
            "Ещё не мечта — но уже не паника от каждого оповещения банка.",
        ),
    ),
    ending(
        id = "ending_p2p",
        endingType = EndingType.PAYCHECK_TO_PAYCHECK,
        flavor = "😰",
        priority = 110,
        conditions = listOf(Condition.HasFlag("arc.final_check")),
        message = story(
            "Зарплата в зарплату. Рассрочки, подписки, доставки — " +
                "всё съедает чуть больше, чем приходит.",
            "Один плохой месяц — и снова звонок маме. " +
                "Изменить можно, просто нужно начать с одного конкретного шага.",
        ),
    ),
)
