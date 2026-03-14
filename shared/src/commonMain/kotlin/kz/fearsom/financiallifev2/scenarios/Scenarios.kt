package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.model.Condition.Field.*
import kz.fearsom.financiallifev2.model.Condition.Op.*
import kz.fearsom.financiallifev2.model.moneyFormat

// ─── DSL helpers ─────────────────────────────────────────────────────────────

private fun event(
    id: String,
    message: String,
    flavor: String = "💬",
    priority: Int = 0,
    conditions: List<Condition> = emptyList(),
    isEnding: Boolean = false,
    endingType: EndingType? = null,
    options: List<GameOption>
) = GameEvent(id, message, flavor, options, conditions, priority, isEnding, endingType)

private fun option(
    id: String,
    text: String,
    emoji: String,
    next: String,
    fx: Effect = Effect()
) = GameOption(id, text, emoji, fx, next)

private fun cond(field: Condition.Field, op: Condition.Op, value: Long) =
    Condition(field, op, value)

// ─── ScenarioGraph ────────────────────────────────────────────────────────────

/**
 * The full narrative graph — character-agnostic story events.
 *
 * Event messages may contain {token} placeholders that are substituted at
 * render time by GameEngine.substituteTemplate() with actual PlayerState values:
 *   {income}        — monthly income
 *   {expenses}      — monthly fixed expenses
 *   {capital}       — current savings/capital
 *   {debt}          — total outstanding debt
 *   {debtPayment}   — monthly debt repayment amount
 *   {investments}   — total portfolio value
 *   {passiveIncome} — monthly investment return
 *   {netFlow}       — net monthly cash flow
 *   {income3x}      — 3× income (emergency fund target)
 *   {name}          — character name
 *
 * Structure:
 *   [events]             — narrative nodes keyed by id
 *   [conditionalEvents]  — injected by engine when PlayerState matches conditions
 *   [afterTickEventPool] — ids drawn from after a monthly tick (convergence hub)
 */
class ScenarioGraph {

    val initialPlayerState = PlayerState(
        capital            = 200_000L,
        income             = 450_000L,
        expenses           = 180_000L,   // rent 130k + food/transport 50k
        debt               = 120_000L,
        debtPaymentMonthly = 15_000L,    // ~15% APR on 120k credit card
        investments        = 0L,
        investmentReturnRate = 0.10,     // 10% annual (mixed ETF)
        stress             = 25,
        financialKnowledge = 10,
        riskLevel          = 15,
        month = 1, year = 2024
    )

    // ─────────────────────────────────────────────────────────────────────────
    // STORY EVENTS
    // ─────────────────────────────────────────────────────────────────────────

    val events: Map<String, GameEvent> = buildMap {

        // ── INTRO ─────────────────────────────────────────────────────────────
        // Message overridden at runtime by GameEngine.buildIntroMessage()
        put("intro", event(
            id = "intro",
            message = "Привет! Загружаем твою историю...",
            flavor = "😰",
            options = listOf(
                option("crypto_in", "Вложить 100 000 тг в крипту с другом", "🚀",
                    next = "crypto_result",
                    fx = Effect(capitalDelta = -100_000, riskDelta = 20, stressDelta = 10)),
                option("pay_debt", "Погасить долг по кредитке", "💳",
                    next = "debt_paid",
                    fx = Effect(capitalDelta = -120_000, debtDelta = -120_000,
                        debtPaymentDelta = -15_000, stressDelta = -8, knowledgeDelta = 5)),
                option("emergency_fund", "Отложить 60 000 тг на подушку безопасности", "🛡️",
                    next = "has_cushion",
                    fx = Effect(capitalDelta = -60_000, stressDelta = -4, knowledgeDelta = 6)),
                option("do_nothing", "Ничего не менять, потрачу как обычно", "😶",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 3))
            )
        ))

        // ── CRYPTO ────────────────────────────────────────────────────────────
        put("crypto_result", event(
            id = "crypto_result",
            message = """
                Прошёл месяц. Крипта упала на 40%.
                Друг не отвечает. Потерял 40 000 тг 😭
                Осталось 60 000 тг на бирже.

                Друг внезапно пишет: «Надо докупить на дне, потом x3».
            """.trimIndent(),
            flavor = "😬",
            options = listOf(
                option("double_down", "Докупить ещё на 50 000 тг", "📉",
                    next = "total_loss",
                    fx = Effect(capitalDelta = -50_000, stressDelta = 20, riskDelta = 15)),
                option("cut_losses", "Вывести остаток, зафиксировать −40 000", "✂️",
                    next = "lesson_learned",
                    fx = Effect(capitalDelta = 60_000, stressDelta = -10, knowledgeDelta = 18))
            )
        ))

        put("total_loss", event(
            id = "total_loss",
            message = """
                Биржа оказалась скамом. Друг исчез. Потерял всё — 150 000 тг.

                Это был урок ценой в часть накоплений.
                Никаких «быстрых денег» — только системная работа.

                Капитал сейчас: {capital}.
            """.trimIndent(),
            flavor = "💀",
            options = listOf(
                option("rebuild", "Начать восстанавливать финансы системно", "💪",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -5, knowledgeDelta = 20))
            )
        ))

        put("lesson_learned", event(
            id = "lesson_learned",
            message = """
                Потерял 40 000 тг, но усвоил урок на всю жизнь.

                Прочитал 3 книги по инвестициям. Понял:
                индексные ETF > хайп проекты.

                Капитал сейчас: {capital}.
                Готов начать инвестировать правильно?
            """.trimIndent(),
            flavor = "📚",
            options = listOf(
                option("start_etf", "Открыть брокерский счёт, купить ETF на KASE", "📈",
                    next = "first_etf_bought",
                    fx = Effect(capitalDelta = -50_000, investmentsDelta = 50_000,
                        knowledgeDelta = 10, riskDelta = -5))
            )
        ))

        // ── DEBT PAID ─────────────────────────────────────────────────────────
        put("debt_paid", event(
            id = "debt_paid",
            message = """
                Долг закрыт! 🎉

                Капитал: {capital}.
                Ежемесячные расходы: {expenses}/мес.

                Что делать с освободившимися деньгами?
            """.trimIndent(),
            flavor = "🎉",
            options = listOf(
                option("invest_freed", "Инвестировать освободившиеся деньги в ETF", "📈",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 15_000, knowledgeDelta = 8)),
                option("raise_cushion", "Нарастить подушку до 3 зарплат ({income3x})", "🛡️",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -5, knowledgeDelta = 5)),
                option("lifestyle_creep", "Переехать в квартиру подороже (+40к/мес)", "🏠",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 40_000, stressDelta = -3, riskDelta = 5))
            )
        ))

        // ── CUSHION ───────────────────────────────────────────────────────────
        put("has_cushion", event(
            id = "has_cushion",
            message = """
                Подушка создана — 60 000 тг лежат нетронутыми.

                Новость: сломался телефон. Нужно 35 000 тг.
                Подушка существует именно для этого.
            """.trimIndent(),
            flavor = "🛡️",
            options = listOf(
                option("use_cushion_correct", "Использовать подушку — для этого она и есть", "✅",
                    next = "cushion_worked",
                    fx = Effect(capitalDelta = -35_000, stressDelta = -10, knowledgeDelta = 12)),
                option("take_credit_instead", "Взять рассрочку, сохранить подушку нетронутой", "💳",
                    next = "new_credit",
                    fx = Effect(debtDelta = 38_000, debtPaymentDelta = 4_000, stressDelta = 5))
            )
        ))

        put("cushion_worked", event(
            id = "cushion_worked",
            message = """
                Именно так и работает подушка безопасности! ✅

                Купил телефон без стресса и без долгов.
                Теперь восполняю подушку — 15 000/мес.

                Капитал: {capital}.
            """.trimIndent(),
            flavor = "💪",
            options = listOf(
                option("rebuild_cushion", "Восполнять подушку + начать инвестировать", "📊",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5, stressDelta = -3))
            )
        ))

        put("new_credit", event(
            id = "new_credit",
            message = """
                Взял рассрочку 38 000 тг под 20% годовых.
                Подушка цела, но появился новый долг.

                Итого долг сейчас: {debt}.
                Ежемесячный платёж: {debtPayment}/мес.

                Стратегически — спорно. Подушка нужна была именно для этого.
            """.trimIndent(),
            flavor = "🤔",
            options = listOf(
                option("close_fast", "Закрыть рассрочку досрочно за 2 месяца", "⚡",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -38_000, debtDelta = -38_000,
                        debtPaymentDelta = -4_000, knowledgeDelta = 10)),
                option("pay_minimum", "Платить минимум, деньги пустить в инвестиции", "📈",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 20_000, riskDelta = 5))
            )
        ))

        // ── JOB OFFER — convergence from multiple paths ───────────────────────
        put("job_offer", event(
            id = "job_offer",
            message = """
                Получил оффер от стартапа: 700 000 тг vs текущие {income}.
                Стартапу 1 год, ещё не прибыльный.

                Текущий работодатель — крупный банк, стабильно.
                Разница: +250 000 тг/мес, но риск закрытия.
            """.trimIndent(),
            flavor = "💼",
            options = listOf(
                option("take_startup", "Принять оффер стартапа (+250к/мес, высокий риск)", "🚀",
                    next = "startup_joined",
                    fx = Effect(incomeDelta = 250_000, riskDelta = 20, stressDelta = 15)),
                option("negotiate_current", "Показать оффер текущему — попросить +150к", "🤝",
                    next = "negotiated_raise",
                    fx = Effect(incomeDelta = 150_000, stressDelta = -5, knowledgeDelta = 8)),
                option("stay_safe", "Остаться — стабильность важнее на текущем этапе", "🛡️",
                    next = "promotion_soon",
                    fx = Effect(stressDelta = -8, knowledgeDelta = 3))
            )
        ))

        put("startup_joined", event(
            id = "startup_joined",
            message = """
                Стартап крутой! Работа интересная.
                +250к/мес очень ощутимо — текущий доход: {income}/мес.

                Через 6 месяцев инвесторы не продлили финансирование.
                Нас сокращают. На счёте: {capital}.

                Что дальше?
            """.trimIndent(),
            flavor = "😰",
            options = listOf(
                option("freelance", "Фрилансить пока ищу новое место", "💻",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = -200_000, knowledgeDelta = 15, riskDelta = -5)),
                option("fast_job", "Взять первый попавшийся оффер быстро", "🏃",
                    next = "back_stable",
                    fx = Effect(stressDelta = -15))
            )
        ))

        put("negotiated_raise", event(
            id = "negotiated_raise",
            message = """
                Шеф согласился! 🎉

                Новая зарплата: {income}/мес.
                Стабильность + рост = идеально.

                Что делать с прибавкой?
            """.trimIndent(),
            flavor = "🎯",
            options = listOf(
                option("invest_raise", "80% прибавки → инвестиции, 20% → подушка", "📈",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 120_000, stressDelta = -5, knowledgeDelta = 5)),
                option("lifestyle_raise", "Переехать в лучшую квартиру (+50к/мес)", "🏠",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 50_000))
            )
        ))

        put("promotion_soon", event(
            id = "promotion_soon",
            message = """
                Остался — через месяц объявили повышение! 🎉
                Теперь мидл: +100к/мес к текущей {income}.

                Это первый шаг к senior.
            """.trimIndent(),
            flavor = "📈",
            options = listOf(
                option("skill_up", "Инвестировать в скиллы: курс за 150к", "🎓",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -150_000, incomeDelta = 100_000,
                        knowledgeDelta = 15, stressDelta = 5)),
                option("invest_extra", "Сразу пустить +100к/мес в ETF", "📊",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 100_000, knowledgeDelta = 5))
            )
        ))

        put("back_stable", event(
            id = "back_stable",
            message = """
                Вышел на новое место — {income}/мес.
                Опыт сокращения научил: подушка безопасности — это не опция.

                Теперь правило 50/30/20.
                Накопления: {capital}.
            """.trimIndent(),
            flavor = "💼",
            options = listOf(
                option("rule_50_30_20", "Применить правило 50/30/20 строго", "📊",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 20, stressDelta = -10,
                        investmentsDelta = 100_000))
            )
        ))

        // ── ETF & INVESTING ───────────────────────────────────────────────────
        put("first_etf_bought", event(
            id = "first_etf_bought",
            message = """
                Первые 50 000 тг в ETF куплены! 📈
                Тикер: KASE_IDX на KASE через Halyk Invest.

                Пришло уведомление через месяц: +3.2% → +1 600 тг.
                Немного, но сложный процент делает своё дело.

                Портфель: {investments}. Капитал: {capital}.
                Дальше — набирать позицию регулярно?
            """.trimIndent(),
            flavor = "📈",
            options = listOf(
                option("dca_strategy", "DCA: вкладывать 30 000 тг каждый месяц", "📅",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 30_000, knowledgeDelta = 8,
                        stressDelta = -3)),
                option("lump_sum", "Вложить ещё 100 000 тг сразу", "💰",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -100_000, investmentsDelta = 100_000,
                        riskDelta = 5))
            )
        ))

        // ── MORTGAGE ──────────────────────────────────────────────────────────
        put("mortgage_offer", event(
            id = "mortgage_offer",
            message = """
                Банк одобрил ипотеку по 7-20-25.
                Квартира 18 000 000 тг. Взнос 20% = 3 600 000 тг.
                Ипотека 14 400 000 тг под 7.5% на 20 лет.
                Платёж: ~115 000 тг/мес.

                Текущие расходы: {expenses}/мес.
                На счёте: {capital}.
                Родители готовы одолжить недостающее.
            """.trimIndent(),
            flavor = "🏠",
            options = listOf(
                option("mortgage_yes", "Взять ипотеку сейчас, занять у родителей", "🏠",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -3_600_000, debtDelta = 14_400_000,
                        debtPaymentDelta = 115_000, expensesDelta = -130_000,
                        stressDelta = 20, knowledgeDelta = 10)),
                option("save_more", "Накопить 3.6 млн самому — ещё ~10 месяцев", "⏳",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -5, knowledgeDelta = 8)),
                option("rent_invest", "Продолжать арендовать, разницу инвестировать", "📈",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 130_000, knowledgeDelta = 12,
                        riskDelta = -3))
            )
        ))

        // ── SENIOR / FINANCIAL FREEDOM ────────────────────────────────────────
        put("senior_offer", event(
            id = "senior_offer",
            message = """
                Получил оффер Senior — 900 000 тг/мес! 🎯
                Сейчас зарплата: {income}/мес.

                За 2 года прокачал скиллы.
                Это значительный рост дохода.
            """.trimIndent(),
            flavor = "🚀",
            options = listOf(
                option("accept_senior", "Принять — продолжать расти", "🎯",
                    next = "financial_freedom_path",
                    fx = Effect(incomeDelta = 450_000, stressDelta = 5,
                        knowledgeDelta = 10))
            )
        ))

        put("financial_freedom_path", event(
            id = "financial_freedom_path",
            message = """
                Доход: {income}/мес. Расходы: {expenses}/мес.
                Чистый поток: {netFlow}/мес.

                Портфель растёт. Пассивный доход через ETF + дивиденды —
                уже {passiveIncome}/мес.

                Финансовая свобода на горизонте. Ты на правильном пути!
            """.trimIndent(),
            flavor = "🎯",
            options = listOf(
                option("keep_going", "Продолжать — финансовая свобода неизбежна", "🏆",
                    next = "ending_freedom",
                    fx = Effect(knowledgeDelta = 10, stressDelta = -15))
            )
        ))

        // ── NORMAL LIFE (monthly tick convergence hub) ────────────────────────
        put("normal_life", event(
            id = "normal_life",
            message = """
                Жизнь идёт своим чередом.
                Работаешь, откладываешь, иногда балуешь себя.

                Доход: {income}/мес | Расходы: {expenses}/мес | Капитал: {capital}.

                Что в фокусе этого месяца?
            """.trimIndent(),
            flavor = "☀️",
            options = listOf(
                option("focus_savings", "Сократить расходы, увеличить накопления", "💰",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = -10_000, stressDelta = 2, knowledgeDelta = 2)),
                option("focus_invest", "Направить максимум в инвестиции", "📈",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 50_000, capitalDelta = -50_000,
                        knowledgeDelta = 3)),
                option("focus_skills", "Потратить время на развитие скиллов", "🎓",
                    next = "skill_check",
                    fx = Effect(knowledgeDelta = 8, stressDelta = 3)),
                option("check_career", "Посмотреть рынок — может пора менять работу?", "🔍",
                    next = "job_offer",
                    fx = Effect())
            )
        ))

        put("skill_check", event(
            id = "skill_check",
            message = """
                Изучил новую технологию. Написал статью.
                150 лайков, 2 рекрутера написали в личку.

                Один предлагает Senior-позицию прямо сейчас.
            """.trimIndent(),
            flavor = "📝",
            options = listOf(
                option("talk_recruiter", "Поговорить с рекрутером", "📞",
                    next = "senior_offer",
                    fx = Effect(knowledgeDelta = 5)),
                option("not_ready", "Пока не готов — ещё учиться", "📚",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 10, stressDelta = -2))
            )
        ))

        // ── ENDINGS ───────────────────────────────────────────────────────────
        put("ending_freedom", event(
            id = "ending_freedom",
            message = """
                🏆 ФИНАНСОВАЯ СВОБОДА ДОСТИГНУТА!

                {name} построил портфель {investments}.
                Пассивный доход: {passiveIncome}/мес.
                Расходы: {expenses}/мес — покрыты полностью.

                С твоими советами — на 3 года быстрее среднего.
                Спасибо! 🙏
            """.trimIndent(),
            flavor = "🏆",
            isEnding = true,
            endingType = EndingType.FINANCIAL_FREEDOM,
            options = emptyList()
        ))

        put("ending_bankruptcy", event(
            id = "ending_bankruptcy",
            message = """
                💔 Банкротство.

                Долги превысили все активы. Стресс 100/100.
                Нет подушки, нет накоплений.

                Но это не конец жизни — только финансовой главы.
                Попробуй снова — теперь ты знаешь, что не делать.
            """.trimIndent(),
            flavor = "💔",
            isEnding = true,
            endingType = EndingType.BANKRUPTCY,
            options = emptyList()
        ))

        put("ending_paycheck", event(
            id = "ending_paycheck",
            message = """
                😔 Белка в колесе.

                Деньги всегда куда-то уходят. Нет накоплений.
                Нет долгов, но и нет пути вперёд.

                Попробуй снова — сначала подушка, потом инвестиции.
            """.trimIndent(),
            flavor = "😔",
            isEnding = true,
            endingType = EndingType.PAYCHECK_TO_PAYCHECK,
            options = emptyList()
        ))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONDITIONAL EVENTS  (injected by engine when conditions match)
    // ─────────────────────────────────────────────────────────────────────────

    val conditionalEvents: List<GameEvent> = listOf(

        // CRISIS: debt > 1 000 000 — срочное событие, высший приоритет
        event(
            id = "debt_crisis",
            message = """
                ⚠️ ДОЛГОВОЙ КРИЗИС!

                Долг вырос до {debt}.
                Банк звонит. Коллекторы шлют письма.
                Стресс максимальный.

                Нужны срочные меры.
            """.trimIndent(),
            flavor = "🚨",
            priority = 100,
            conditions = listOf(cond(DEBT, GT, 1_000_000L)),
            options = listOf(
                option("debt_restructure", "Договориться о реструктуризации долга", "🤝",
                    next = MONTHLY_TICK,
                    fx = Effect(debtPaymentDelta = 20_000, stressDelta = -20,
                        knowledgeDelta = 15)),
                option("sell_assets", "Продать всё что можно, закрыть долг", "💸",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = -300_000, capitalDelta = -200_000,
                        debtDelta = -500_000, stressDelta = -25))
            )
        ),

        // BANKRUPTCY TRIGGER: capital = 0, stress = 100
        event(
            id = "bankruptcy_trigger",
            message = """
                Капитал обнулился. Платить нечем.
                Долг: {debt}. Расходы: {expenses}/мес.
            """.trimIndent(),
            flavor = "💀",
            priority = 90,
            conditions = listOf(
                cond(CAPITAL, LTE, 0L),
                cond(STRESS, GTE, 90L)
            ),
            options = listOf(
                option("accept_bankruptcy", "Признать банкротство", "💔",
                    next = "ending_bankruptcy",
                    fx = Effect())
            )
        ),

        // PAYCHECK TO PAYCHECK: no savings, low capital, low knowledge
        event(
            id = "trap_warning",
            message = """
                😰 Ловушка «от зарплаты до зарплаты»

                Уже 6 месяцев нет никаких накоплений.
                Доход: {income}/мес. Расходы: {expenses}/мес.
                Капитал: {capital}. Нужно что-то менять.
            """.trimIndent(),
            flavor = "⚠️",
            priority = 50,
            conditions = listOf(
                cond(CAPITAL, LTE, 50_000L),
                cond(KNOWLEDGE, LTE, 15L),
                cond(MONTH, GTE, 6L)
            ),
            options = listOf(
                option("budget_app", "Установить приложение для учёта расходов", "📱",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = -30_000, knowledgeDelta = 15,
                        stressDelta = -5)),
                option("second_income", "Найти подработку (+80к/мес)", "💼",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = 80_000, stressDelta = 8))
            )
        ),

        // WINDFALL: boss gives bonus — capital > 500k, knowledge > 30
        event(
            id = "bonus_received",
            message = """
                🎁 Неожиданно: годовой бонус!

                Компания показала рекордную прибыль.
                Тебе выплатили 200 000 тг сверху.
                Капитал теперь: {capital}.

                Куда направить?
            """.trimIndent(),
            flavor = "🎁",
            priority = 30,
            conditions = listOf(
                cond(CAPITAL, GTE, 500_000L),
                cond(KNOWLEDGE, GTE, 30L)
            ),
            options = listOf(
                option("bonus_invest", "Всё в инвестиции", "📈",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 200_000, capitalDelta = -200_000,
                        knowledgeDelta = 5)),
                option("bonus_cushion", "Пополнить подушку безопасности", "🛡️",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -8, knowledgeDelta = 3)),
                option("bonus_experience", "Потратить на себя — заслужил!", "🎉",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -12))
            )
        ),

        // MORTGAGE UNLOCK: capital >= 2.8M — ипотека становится доступна
        event(
            id = "mortgage_unlock",
            message = """
                🏠 Накопил на первый взнос!

                На счёте уже {capital}.
                Банк предварительно одобрил ипотеку по 7-20-25.

                Стоит рассмотреть?
            """.trimIndent(),
            flavor = "🏠",
            priority = 40,
            conditions = listOf(
                cond(CAPITAL, GTE, 2_800_000L),
                cond(DEBT, LTE, 200_000L)
            ),
            options = listOf(
                option("consider_mortgage", "Да, разобраться с ипотекой", "🏠",
                    next = "mortgage_offer",
                    fx = Effect()),
                option("keep_investing", "Нет, продолжать инвестировать — аренда дешевле", "📈",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 8))
            )
        ),

        // HIGH STRESS EVENT: stress > 70
        event(
            id = "burnout_risk",
            message = """
                😮‍💨 Выгорание на горизонте.

                Стресс очень высокий. Сон плохой.
                Продуктивность падает.

                Нужно замедлиться.
            """.trimIndent(),
            flavor = "🔥",
            priority = 60,
            conditions = listOf(cond(STRESS, GT, 70L)),
            options = listOf(
                option("take_vacation", "Взять отпуск — потратить 80к на отдых", "🌴",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -80_000, stressDelta = -30)),
                option("therapy", "Пойти к психологу (20к/мес)", "🧠",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 20_000, stressDelta = -15,
                        knowledgeDelta = 5)),
                option("push_through", "Продолжать — у меня цели", "💪",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 5, knowledgeDelta = 3))
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // AFTER-TICK POOL  (convergence hub after every monthly tick)
    // Multiple narrative branches converge here to continue the simulation.
    // ─────────────────────────────────────────────────────────────────────────

    val afterTickEventPool: List<String> = listOf(
        "normal_life",
        "job_offer",
        "mortgage_offer",
        "skill_check"
    )

    /**
     * Unified event lookup — checks both the narrative graph and the conditional
     * event pool. Required because conditional events become [currentEventId] after
     * injection, but are not in the [events] map.
     */
    fun findEvent(id: String): GameEvent? =
        events[id] ?: conditionalEvents.find { it.id == id }
}
