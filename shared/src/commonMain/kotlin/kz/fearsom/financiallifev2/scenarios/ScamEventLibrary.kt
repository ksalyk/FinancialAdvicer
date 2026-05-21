package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.CAPITAL
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.STRESS
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GTE
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.LTE
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.ScheduledEvent

/**
 * Era-aware scam library.
 *
 * Same scam mechanic can exist in different decades, but the channel must match
 * the era: paper receipts and landline calls in the 90s/2005, social networks in
 * 2015, Telegram/apps/crypto in 2024.
 */
object ScamEventLibrary {

    private fun e(
        id: String,
        message: String,
        flavor: String = "💬",
        conditions: List<Condition> = emptyList(),
        tags: Set<String> = emptySet(),
        poolWeight: Int = 10,
        unique: Boolean = false,
        cooldownMonths: Int = 0,
        schemeExplanation: String? = null,
        isEnding: Boolean = false,
        endingType: EndingType? = null,
        options: List<GameOption>
    ) = GameEvent(
        id = id,
        message = message,
        flavor = flavor,
        options = options,
        conditions = conditions,
        priority = 0,
        isEnding = isEnding,
        endingType = endingType,
        tags = tags,
        poolWeight = poolWeight,
        unique = unique,
        cooldownMonths = cooldownMonths,
        schemeExplanation = schemeExplanation
    )

    private fun o(id: String, text: String, emoji: String, next: String, fx: Effect = Effect()) =
        GameOption(id, text, emoji, fx, next)

    private fun capitalAtLeast(amount: Long) = Condition.Stat(CAPITAL, GTE, amount)
    private fun capitalAtMost(amount: Long) = Condition.Stat(CAPITAL, LTE, amount)
    private fun stressAtLeast(value: Long) = Condition.Stat(STRESS, GTE, value)
    private fun era(id: String) = Condition.InEra(id)
    private fun notLearned(tag: String) = Condition.NotFlag("learned.$tag")

    private val pyramidExplanation = story(
        "Пирамида не зарабатывает на продукте. Выплаты ранним участникам идут из денег новых людей.",
        "Проверка: откуда реальная прибыль, есть ли лицензия, можно ли вернуть деньги без привлечения новых участников, почему доходность обещают заранее."
    )

    private val mlmExplanation = story(
        "MLM становится опасной схемой, когда главный заработок не от продаж клиентам, а от покупки стартовых наборов и привлечения новых участников.",
        "Проверка: кто конечный покупатель, можно ли вернуть товар, сколько реально зарабатывает средний участник, есть ли обязательные закупки."
    )

    private val bettingExplanation = story(
        "Каппер продает уверенность, а не преимущество. Если прогнозы были бы стабильной прибылью, их не продавали бы массово за небольшую плату.",
        "Красные флаги: гарантия прохода, VIP-чат, просьба отыграться, скрины без полной истории ставок."
    )

    private val mfoExplanation = story(
        "Быстрый заем опасен не суммой, а полной стоимостью: ежедневная ставка, штрафы, пролонгации и страховки превращают маленький долг в постоянный платеж.",
        "Проверка: годовая эффективная ставка, штрафы, график платежей, право досрочного погашения, сколько будет уплачено всего."
    )

    private val middlemanExplanation = story(
        "Схема посредника держится на предоплате и срочности: товар, поставщик или склад существуют только со слов человека, который уже просит деньги.",
        "Защита: тестовая партия, договор, паспортные данные, проверка склада/юрлица, оплата частями после фактической приемки."
    )

    private val trainingExplanation = story(
        "Тренинг-секта продает не навык, а эмоциональный подъем и следующий уровень доступа. После первой оплаты появляется более дорогой пакет.",
        "Проверка: конкретный результат, программа, квалификация, отзывы вне сайта продавца, договор возврата, отсутствие давления «только сегодня»."
    )

    private val romanceExplanation = story(
        "Romance scam сначала строит доверие и эмоциональную близость, потом переводит разговор к инвестициям или срочной помощи.",
        "Защита: не переводить деньги человеку из переписки, проверять личность вне чата, не ставить приложения по ссылке, тестировать вывод до вложений."
    )

    private val forexCryptoExplanation = story(
        "Фейковый брокер или биржа показывает прибыль в интерфейсе, но деньги могут не попадать на реальный рынок. Вывод блокируют под видом налога, комиссии или верификации.",
        "Проверка: лицензия, юрлицо, независимые отзывы, домен, хранение клиентских средств, возможность вывести маленькую сумму без дополнительных платежей."
    )

    private val escrowExplanation = story(
        "Фейковый escrow копирует знакомую страницу оплаты или доставки. Деньги уходят не в сервис, а на поддельный сайт.",
        "Проверка: только официальный кабинет/приложение, правильный домен, номер заказа внутри сервиса, отсутствие просьбы перейти в мессенджер для оплаты."
    )

    private val presaleExplanation = story(
        "Риск долевого строительства возникает, когда покупатель платит за будущую квартиру без защиты денег и контроля этапов стройки.",
        "Проверка: земля, разрешение, история застройщика, судебные дела, отдельный счет, штрафы за задержку, запрет платить наличными посреднику."
    )

    private fun pyramid90s() = e(
        id = "scam_pyramid_90s",
        flavor = "📺",
        tags = setOf("scam", "scam.pyramid"),
        unique = true,
        poolWeight = 18,
        conditions = listOf(era("kz_90s"), capitalAtLeast(50_000L), notLearned("scam.pyramid")),
        schemeExplanation = pyramidExplanation,
        message = story(
            "Вечером сосед включает телевизор: там люди улыбаются и рассказывают, как «кооператив» уже выплатил им двойную сумму.",
            "Он предлагает зайти завтра в офис и внести наличные. Договор обещают дать потом, когда очередь станет меньше."
        ),
        options = listOf(
            o("pyramid_pay_90s", "Внести деньги, пока очередь растет", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -80_000L, stressDelta = 8, riskDelta = 18, scheduleEvent = ScheduledEvent("scam_pyramid_collapse", 2))),
            o("pyramid_questions_90s", "Спросить, откуда берется прибыль", "🔍", "scam_pyramid_avoided",
                Effect(knowledgeDelta = 10)),
            o("pyramid_decline_90s", "Не участвовать", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 6, stressDelta = -5, setFlags = setOf("learned.scam.pyramid")))
        )
    )

    private fun pyramid2005() = e(
        id = "scam_pyramid_2005",
        flavor = "📞",
        tags = setOf("scam", "scam.pyramid"),
        unique = true,
        poolWeight = 15,
        conditions = listOf(era("kz_2005"), capitalAtLeast(80_000L), notLearned("scam.pyramid")),
        schemeExplanation = pyramidExplanation,
        message = "Знакомый звонит на стационарный телефон и зовет в «инвестиционный клуб». Встреча в офисе, доходность 7% в месяц, вход наличными.",
        options = listOf(
            o("pyramid_pay_2005", "Съездить и внести стартовую сумму", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -120_000L, stressDelta = 8, riskDelta = 18, scheduleEvent = ScheduledEvent("scam_pyramid_collapse", 2))),
            o("pyramid_docs_2005", "Попросить лицензию и договор заранее", "📋", "scam_pyramid_avoided",
                Effect(knowledgeDelta = 10)),
            o("pyramid_decline_2005", "Отказаться по телефону", "☎️", MONTHLY_TICK,
                Effect(knowledgeDelta = 6, stressDelta = -4, setFlags = setOf("learned.scam.pyramid")))
        )
    )

    private fun pyramid2015() = e(
        id = "scam_pyramid_2015",
        flavor = "📱",
        tags = setOf("scam", "scam.pyramid"),
        unique = true,
        poolWeight = 16,
        conditions = listOf(era("kz_2015"), capitalAtLeast(80_000L), notLearned("scam.pyramid")),
        schemeExplanation = pyramidExplanation,
        message = "В WhatsApp-группе бывший одноклассник пишет про «кассу взаимопомощи». Нужно внести сумму и привести двоих, тогда выплаты якобы пойдут автоматически.",
        options = listOf(
            o("pyramid_pay_2015", "Внести и не отставать от всех", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -100_000L, stressDelta = 8, riskDelta = 18, scheduleEvent = ScheduledEvent("scam_pyramid_collapse", 2))),
            o("pyramid_math_2015", "Посчитать, сколько людей нужно на 5 уровней", "🔢", "scam_pyramid_avoided",
                Effect(knowledgeDelta = 12)),
            o("pyramid_decline_2015", "Выйти из группы", "🚪", MONTHLY_TICK,
                Effect(knowledgeDelta = 6, stressDelta = -4, setFlags = setOf("learned.scam.pyramid")))
        )
    )

    private fun pyramid2024() = e(
        id = "scam_pyramid_2024",
        flavor = "📲",
        tags = setOf("scam", "scam.pyramid"),
        unique = true,
        poolWeight = 16,
        conditions = listOf(era("kz_2024"), capitalAtLeast(80_000L), notLearned("scam.pyramid")),
        schemeExplanation = pyramidExplanation,
        message = "В Telegram зовут в закрытый клуб: «AI арбитраж, доход каждый день». В чате много скринов выплат, но все ссылки ведут на один и тот же сайт.",
        options = listOf(
            o("pyramid_pay_2024", "Попробовать небольшой вход", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -120_000L, stressDelta = 6, riskDelta = 18, scheduleEvent = ScheduledEvent("scam_pyramid_collapse", 2))),
            o("pyramid_check_2024", "Проверить юрлицо, домен и источник дохода", "🔍", "scam_pyramid_avoided",
                Effect(knowledgeDelta = 12)),
            o("pyramid_decline_2024", "Заблокировать чат", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 6, stressDelta = -4, setFlags = setOf("learned.scam.pyramid")))
        )
    )

    private fun pyramidAvoided() = e(
        id = "scam_pyramid_avoided",
        flavor = "📚",
        tags = setOf("scam.pyramid", "reflection"),
        schemeExplanation = pyramidExplanation,
        message = "Вопросы ломают красивую картинку. Как только разговор доходит до источника прибыли и лицензии, собеседник начинает торопить или обижаться.",
        options = listOf(
            o("pyramid_lesson", "Запомнить признаки пирамиды", "✅", MONTHLY_TICK,
                Effect(knowledgeDelta = 18, stressDelta = -6, setFlags = setOf("learned.scam.pyramid")))
        )
    )

    private fun pyramidCollapse() = e(
        id = "scam_pyramid_collapse",
        flavor = "💀",
        tags = setOf("scam.pyramid", "consequence"),
        schemeExplanation = pyramidExplanation,
        message = "Выплаты остановились. Офис закрыт, телефон не отвечает, в чате обещают «техническую паузу». Деньги ушли к тем, кто был выше.",
        options = listOf(
            o("pyramid_rebuild", "Принять потерю и разобрать ошибку", "📖", MONTHLY_TICK,
                Effect(knowledgeDelta = 24, stressDelta = 14, setFlags = setOf("learned.scam.pyramid", "lost_money_to_scam")))
        )
    )

    private fun mlm90s() = e(
        id = "scam_mlm_90s",
        flavor = "🚪",
        tags = setOf("scam", "scam.mlm"),
        unique = true,
        poolWeight = 12,
        conditions = listOf(era("kz_90s"), notLearned("scam.mlm")),
        schemeExplanation = mlmExplanation,
        message = "Знакомая приходит домой с каталогом банок и порошков. Говорит, что настоящие деньги не в продаже, а в своей команде.",
        options = listOf(
            o("mlm_buy_90s", "Купить стартовый набор", "📦", MONTHLY_TICK,
                Effect(capitalDelta = -45_000L, riskDelta = 10, scheduleEvent = ScheduledEvent("scam_mlm_stock", 3))),
            o("mlm_margin_90s", "Спросить маржу без привлечения людей", "🧮", "scam_mlm_exposed",
                Effect(knowledgeDelta = 10)),
            o("mlm_no_90s", "Отказаться", "🚪", MONTHLY_TICK,
                Effect(knowledgeDelta = 4, setFlags = setOf("learned.scam.mlm")))
        )
    )

    private fun mlm2005() = e(
        id = "scam_mlm_2005",
        flavor = "📞",
        tags = setOf("scam", "scam.mlm"),
        unique = true,
        poolWeight = 12,
        conditions = listOf(era("kz_2005"), notLearned("scam.mlm")),
        schemeExplanation = mlmExplanation,
        message = "Коллега зовет на презентацию в конференц-зал гостиницы. Обещают бизнес без начальника: косметика, БАДы, обучение и «лидерская линия».",
        options = listOf(
            o("mlm_buy_2005", "Войти стартовым пакетом", "💳", MONTHLY_TICK,
                Effect(capitalDelta = -70_000L, riskDelta = 10, scheduleEvent = ScheduledEvent("scam_mlm_stock", 3))),
            o("mlm_question_2005", "Попросить средний доход участника", "🔍", "scam_mlm_exposed",
                Effect(knowledgeDelta = 10)),
            o("mlm_no_2005", "Уйти после презентации", "🚶", MONTHLY_TICK,
                Effect(knowledgeDelta = 4, setFlags = setOf("learned.scam.mlm")))
        )
    )

    private fun mlm2015() = e(
        id = "scam_mlm_2015",
        flavor = "✨",
        tags = setOf("scam", "scam.mlm"),
        unique = true,
        poolWeight = 14,
        conditions = listOf(era("kz_2015"), notLearned("scam.mlm")),
        schemeExplanation = mlmExplanation,
        message = "В Instagram знакомая пишет: «Ты сильная, тебе подойдет наш женский бизнес». Потом добавляет в WhatsApp-чат с мотивацией и планами закупок.",
        options = listOf(
            o("mlm_buy_2015", "Взять стартовый пакет", "🛍️", MONTHLY_TICK,
                Effect(capitalDelta = -90_000L, riskDelta = 12, scheduleEvent = ScheduledEvent("scam_mlm_stock", 3))),
            o("mlm_question_2015", "Спросить про возврат товара", "🔍", "scam_mlm_exposed",
                Effect(knowledgeDelta = 10)),
            o("mlm_no_2015", "Вежливо выйти из чата", "📵", MONTHLY_TICK,
                Effect(knowledgeDelta = 4, stressDelta = -3, setFlags = setOf("learned.scam.mlm")))
        )
    )

    private fun mlm2024() = e(
        id = "scam_mlm_2024",
        flavor = "📲",
        tags = setOf("scam", "scam.mlm"),
        unique = true,
        poolWeight = 12,
        conditions = listOf(era("kz_2024"), notLearned("scam.mlm")),
        schemeExplanation = mlmExplanation,
        message = "В Telegram предлагают «комьюнити предпринимателей»: подписка, закупка товара и бонус за каждого приведенного участника.",
        options = listOf(
            o("mlm_buy_2024", "Оплатить вход", "💳", MONTHLY_TICK,
                Effect(capitalDelta = -110_000L, riskDelta = 12, scheduleEvent = ScheduledEvent("scam_mlm_stock", 3))),
            o("mlm_question_2024", "Попросить юнит-экономику без рефералов", "🧮", "scam_mlm_exposed",
                Effect(knowledgeDelta = 10)),
            o("mlm_no_2024", "Не входить", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 4, stressDelta = -3, setFlags = setOf("learned.scam.mlm")))
        )
    )

    private fun mlmExposed() = e(
        id = "scam_mlm_exposed",
        flavor = "📦",
        tags = setOf("scam.mlm", "reflection"),
        schemeExplanation = mlmExplanation,
        message = "Как только убрать рекрутинг, бизнес становится обычной продажей товара с низкой маржой и высоким риском остаться со складом дома.",
        options = listOf(
            o("mlm_lesson", "Не путать продажи и вербовку", "✅", MONTHLY_TICK,
                Effect(knowledgeDelta = 18, stressDelta = -4, setFlags = setOf("learned.scam.mlm")))
        )
    )

    private fun mlmStock() = e(
        id = "scam_mlm_stock",
        flavor = "📦",
        tags = setOf("scam.mlm", "consequence"),
        schemeExplanation = mlmExplanation,
        message = "Через три месяца дома лежат коробки. Продажи идут медленно, зато куратор предлагает купить следующий пакет, чтобы «не терять статус».",
        options = listOf(
            o("mlm_exit", "Остановиться и списать урок", "🚪", MONTHLY_TICK,
                Effect(knowledgeDelta = 22, stressDelta = 8, setFlags = setOf("learned.scam.mlm", "lost_money_to_scam"))),
            o("mlm_continue", "Докупить ради статуса", "😤", MONTHLY_TICK,
                Effect(capitalDelta = -120_000L, stressDelta = 14, riskDelta = 8))
        )
    )

    private fun betting2005() = e(
        id = "scam_betting_2005",
        flavor = "📟",
        tags = setOf("scam", "scam.betting"),
        unique = true,
        poolWeight = 7,
        conditions = listOf(era("kz_2005"), capitalAtLeast(40_000L), notLearned("scam.betting")),
        schemeExplanation = bettingExplanation,
        message = "После футбола знакомый дает номер SMS-типстера: «Он знает договорные матчи. Прогноз стоит копейки, банк поднимешь быстро».",
        options = listOf(
            o("betting_buy_2005", "Купить прогноз", "⚽", MONTHLY_TICK,
                Effect(capitalDelta = -20_000L, riskDelta = 12, scheduleEvent = ScheduledEvent("scam_betting_loss", 1))),
            o("betting_math_2005", "Посчитать комиссию букмекера и вероятность", "🔢", "scam_betting_explained",
                Effect(knowledgeDelta = 10)),
            o("betting_ignore_2005", "Не связываться", "🚫", MONTHLY_TICK,
                Effect(knowledgeDelta = 4, setFlags = setOf("learned.scam.betting")))
        )
    )

    private fun betting2015() = e(
        id = "scam_betting_2015",
        flavor = "⚽",
        tags = setOf("scam", "scam.betting"),
        unique = true,
        poolWeight = 10,
        conditions = listOf(era("kz_2015"), capitalAtLeast(40_000L), notLearned("scam.betting")),
        schemeExplanation = bettingExplanation,
        message = "Во VK появляется каппер с бесплатным прогнозом и платным VIP. В комментариях только победы, проигрыши удалены.",
        options = listOf(
            o("betting_buy_2015", "Купить VIP на неделю", "💰", MONTHLY_TICK,
                Effect(capitalDelta = -35_000L, riskDelta = 14, scheduleEvent = ScheduledEvent("scam_betting_loss", 1))),
            o("betting_audit_2015", "Попросить полную историю ставок", "🔍", "scam_betting_explained",
                Effect(knowledgeDelta = 10)),
            o("betting_ignore_2015", "Не играть", "🚫", MONTHLY_TICK,
                Effect(knowledgeDelta = 4, setFlags = setOf("learned.scam.betting")))
        )
    )

    private fun betting2024() = e(
        id = "scam_betting_2024",
        flavor = "🏟️",
        tags = setOf("scam", "scam.betting"),
        unique = true,
        poolWeight = 10,
        conditions = listOf(era("kz_2024"), capitalAtLeast(40_000L), notLearned("scam.betting")),
        schemeExplanation = bettingExplanation,
        message = "Telegram-каппер показывает «лесенку»: поставить 20 тысяч, потом удвоить, потом забрать прибыль. В закрепе партнерская ссылка букмекера.",
        options = listOf(
            o("betting_buy_2024", "Зайти по лесенке", "💰", MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, riskDelta = 14, scheduleEvent = ScheduledEvent("scam_betting_loss", 1))),
            o("betting_check_2024", "Проверить конфликт интересов", "🔍", "scam_betting_explained",
                Effect(knowledgeDelta = 10)),
            o("betting_ignore_2024", "Не открывать букмекера", "🚫", MONTHLY_TICK,
                Effect(knowledgeDelta = 4, setFlags = setOf("learned.scam.betting")))
        )
    )

    private fun bettingExplained() = e(
        id = "scam_betting_explained",
        flavor = "🔢",
        tags = setOf("scam.betting", "reflection"),
        schemeExplanation = bettingExplanation,
        message = "Когда смотреть полную историю, магия исчезает: серия побед продается, серия проигрышей скрывается, а партнерская ссылка платит капперу за проигрыши аудитории.",
        options = listOf(
            o("betting_lesson", "Не покупать уверенность", "✅", MONTHLY_TICK,
                Effect(knowledgeDelta = 18, stressDelta = -4, setFlags = setOf("learned.scam.betting")))
        )
    )

    private fun bettingLoss() = e(
        id = "scam_betting_loss",
        flavor = "😤",
        tags = setOf("scam.betting", "consequence"),
        schemeExplanation = bettingExplanation,
        message = "Прогноз проиграл. Каппер пишет: «Так бывает, следующий железный. Главное отбить». Это и есть ловушка отыгрыша.",
        options = listOf(
            o("betting_stop", "Остановиться", "🛑", MONTHLY_TICK,
                Effect(knowledgeDelta = 20, stressDelta = 5, setFlags = setOf("learned.scam.betting", "lost_money_to_scam"))),
            o("betting_chase", "Отыграться", "😰", MONTHLY_TICK,
                Effect(capitalDelta = -120_000L, stressDelta = 18, riskDelta = 12))
        )
    )

    private fun mfo90s() = e(
        id = "scam_mfo_90s",
        flavor = "🏚️",
        tags = setOf("scam", "scam.mfo", "debt"),
        unique = true,
        poolWeight = 10,
        conditions = listOf(era("kz_90s"), capitalAtMost(80_000L), stressAtLeast(35L), notLearned("scam.mfo")),
        schemeExplanation = mfoExplanation,
        message = "Знакомый предлагает занять наличные «до зарплаты». Проценты считает на словах: если задержишь, сумма просто вырастет.",
        options = listOf(
            o("mfo_take_90s", "Взять деньги сейчас", "⚡", "scam_mfo_signed",
                Effect(capitalDelta = 50_000L, debtDelta = 70_000L, stressDelta = -4)),
            o("mfo_terms_90s", "Записать срок, сумму и штрафы", "📋", "scam_mfo_contract",
                Effect(knowledgeDelta = 10)),
            o("mfo_skip_90s", "Урезать расходы вместо долга", "✂️", MONTHLY_TICK,
                Effect(expensesDelta = -15_000L, stressDelta = 5, knowledgeDelta = 8, setFlags = setOf("learned.scam.mfo")))
        )
    )

    private fun mfo2005() = e(
        id = "scam_mfo_2005",
        flavor = "🏦",
        tags = setOf("scam", "scam.mfo", "debt"),
        unique = true,
        poolWeight = 10,
        conditions = listOf(era("kz_2005"), capitalAtMost(120_000L), stressAtLeast(35L), notLearned("scam.mfo")),
        schemeExplanation = mfoExplanation,
        message = "В офисе возле остановки выдают быстрый заем по удостоверению. Менеджер улыбается: «Переплата маленькая, просто подпишите здесь и здесь».",
        options = listOf(
            o("mfo_take_2005", "Подписать быстро", "⚡", "scam_mfo_signed",
                Effect(capitalDelta = 90_000L, debtDelta = 140_000L, stressDelta = -4)),
            o("mfo_read_2005", "Прочитать полную стоимость", "📋", "scam_mfo_contract",
                Effect(knowledgeDelta = 10)),
            o("mfo_skip_2005", "Не брать", "✂️", MONTHLY_TICK,
                Effect(expensesDelta = -20_000L, stressDelta = 5, knowledgeDelta = 8, setFlags = setOf("learned.scam.mfo")))
        )
    )

    private fun mfoOnline2015() = e(
        id = "scam_mfo_2015",
        flavor = "💳",
        tags = setOf("scam", "scam.mfo", "debt"),
        unique = true,
        poolWeight = 10,
        conditions = listOf(era("kz_2015"), capitalAtMost(120_000L), stressAtLeast(35L), notLearned("scam.mfo")),
        schemeExplanation = mfoExplanation,
        message = "На сайте МФО обещают перевод на карту за 15 минут. Большая кнопка «получить», а полная стоимость спрятана в PDF.",
        options = listOf(
            o("mfo_take_2015", "Взять онлайн-заем", "⚡", "scam_mfo_signed",
                Effect(capitalDelta = 90_000L, debtDelta = 150_000L, stressDelta = -4)),
            o("mfo_read_2015", "Открыть PDF и посчитать переплату", "📋", "scam_mfo_contract",
                Effect(knowledgeDelta = 10)),
            o("mfo_skip_2015", "Искать другой вариант", "✂️", MONTHLY_TICK,
                Effect(expensesDelta = -20_000L, stressDelta = 5, knowledgeDelta = 8, setFlags = setOf("learned.scam.mfo")))
        )
    )

    private fun mfoOnline2024() = e(
        id = "scam_mfo_2024",
        flavor = "📲",
        tags = setOf("scam", "scam.mfo", "debt"),
        unique = true,
        poolWeight = 10,
        conditions = listOf(era("kz_2024"), capitalAtMost(120_000L), stressAtLeast(35L), notLearned("scam.mfo")),
        schemeExplanation = mfoExplanation,
        message = "Приложение предлагает деньги до зарплаты. Push пишет: «Одобрено 120 000, заберите сегодня». Ниже мелко: комиссия, страховка, штраф за каждый день.",
        options = listOf(
            o("mfo_take_2024", "Забрать деньги в приложении", "⚡", "scam_mfo_signed",
                Effect(capitalDelta = 120_000L, debtDelta = 190_000L, stressDelta = -4)),
            o("mfo_read_2024", "Посчитать полную стоимость", "📋", "scam_mfo_contract",
                Effect(knowledgeDelta = 10)),
            o("mfo_skip_2024", "Закрыть приложение", "✂️", MONTHLY_TICK,
                Effect(expensesDelta = -25_000L, stressDelta = 5, knowledgeDelta = 8, setFlags = setOf("learned.scam.mfo")))
        )
    )

    private fun mfoContract() = e(
        id = "scam_mfo_contract",
        flavor = "⚠️",
        tags = setOf("scam.mfo", "reflection"),
        schemeExplanation = mfoExplanation,
        message = "После расчета видно: быстрые деньги стоят дороже, чем казалось. Проблема не в займе, а в том, что он покупает один спокойный день ценой следующих месяцев.",
        options = listOf(
            o("mfo_lesson", "Не брать долг без полной стоимости", "✅", MONTHLY_TICK,
                Effect(knowledgeDelta = 18, stressDelta = -4, setFlags = setOf("learned.scam.mfo")))
        )
    )

    private fun mfoSigned() = e(
        id = "scam_mfo_signed",
        flavor = "😰",
        tags = setOf("scam.mfo", "consequence"),
        schemeExplanation = mfoExplanation,
        message = "Срок платежа пришел быстрее, чем зарплата. Приложение предлагает продлить заем, но продление платное и тело долга почти не уменьшается.",
        options = listOf(
            o("mfo_pay", "Закрыть долг любой ценой", "🧾", MONTHLY_TICK,
                Effect(capitalDelta = -120_000L, debtDelta = -120_000L, stressDelta = 8, knowledgeDelta = 18, setFlags = setOf("learned.scam.mfo"))),
            o("mfo_roll", "Продлить еще раз", "💳", MONTHLY_TICK,
                Effect(debtDelta = 90_000L, debtPaymentDelta = 20_000L, stressDelta = 18, riskDelta = 12))
        )
    )

    private fun middleman90s() = e(
        id = "scam_middleman_90s",
        flavor = "🧳",
        tags = setOf("scam", "scam.middleman"),
        unique = true,
        poolWeight = 10,
        conditions = listOf(era("kz_90s"), capitalAtLeast(120_000L), notLearned("scam.middleman")),
        schemeExplanation = middlemanExplanation,
        message = "Перекуп обещает привезти товар из Турции дешевле рынка. Просит наличные сейчас, расписку напишет «когда вернется».",
        options = listOf(
            o("middleman_pay_90s", "Отдать предоплату", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -160_000L, riskDelta = 18, scheduleEvent = ScheduledEvent("scam_middleman_result", 2))),
            o("middleman_docs_90s", "Попросить расписку и свидетелей", "📋", "scam_middleman_contract_refused",
                Effect(knowledgeDelta = 10)),
            o("middleman_skip_90s", "Не входить без товара", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 8, setFlags = setOf("learned.scam.middleman")))
        )
    )

    private fun middleman2005() = e(
        id = "scam_middleman_2005",
        flavor = "📦",
        tags = setOf("scam", "scam.middleman"),
        unique = true,
        poolWeight = 10,
        conditions = listOf(era("kz_2005"), capitalAtLeast(180_000L), notLearned("scam.middleman")),
        schemeExplanation = middlemanExplanation,
        message = "Посредник показывает распечатку с Alibaba и предлагает заказать партию техники. Интернет медленный, проверить поставщика трудно, зато скидка «только сегодня».",
        options = listOf(
            o("middleman_pay_2005", "Внести предоплату", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -250_000L, riskDelta = 18, scheduleEvent = ScheduledEvent("scam_middleman_result", 2))),
            o("middleman_docs_2005", "Попросить контракт и тестовую партию", "📋", "scam_middleman_contract_refused",
                Effect(knowledgeDelta = 10)),
            o("middleman_skip_2005", "Не входить", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 8, setFlags = setOf("learned.scam.middleman")))
        )
    )

    private fun middleman2015() = e(
        id = "scam_middleman_2015",
        flavor = "📦",
        tags = setOf("scam", "scam.middleman"),
        unique = true,
        poolWeight = 10,
        conditions = listOf(era("kz_2015"), capitalAtLeast(180_000L), notLearned("scam.middleman")),
        schemeExplanation = middlemanExplanation,
        message = "В Instagram продают «оптовый доступ» к поставщику с AliExpress. Нужно оплатить карту и получить закрытую таблицу.",
        options = listOf(
            o("middleman_pay_2015", "Оплатить доступ", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -180_000L, riskDelta = 18, scheduleEvent = ScheduledEvent("scam_middleman_result", 2))),
            o("middleman_docs_2015", "Попросить договор и реальные инвойсы", "📋", "scam_middleman_contract_refused",
                Effect(knowledgeDelta = 10)),
            o("middleman_skip_2015", "Не платить за воздух", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 8, setFlags = setOf("learned.scam.middleman")))
        )
    )

    private fun middleman2024() = e(
        id = "scam_middleman_2024",
        flavor = "🚚",
        tags = setOf("scam", "scam.middleman"),
        unique = true,
        poolWeight = 10,
        conditions = listOf(era("kz_2024"), capitalAtLeast(180_000L), notLearned("scam.middleman")),
        schemeExplanation = middlemanExplanation,
        message = "Telegram-перекуп предлагает товар из Китая с маржой 40%. Он присылает видео склада, но не дает название компании и просит перевод на карту.",
        options = listOf(
            o("middleman_pay_2024", "Перевести предоплату", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -250_000L, riskDelta = 18, scheduleEvent = ScheduledEvent("scam_middleman_result", 2))),
            o("middleman_docs_2024", "Попросить БИН, договор и оплату через счет", "📋", "scam_middleman_contract_refused",
                Effect(knowledgeDelta = 10)),
            o("middleman_skip_2024", "Не платить на карту", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 8, setFlags = setOf("learned.scam.middleman")))
        )
    )

    private fun middlemanRefused() = e(
        id = "scam_middleman_contract_refused",
        flavor = "🚩",
        tags = setOf("scam.middleman", "reflection"),
        schemeExplanation = middlemanExplanation,
        message = "На просьбе о договоре и проверке поставщика человек раздражается. Хорошая сделка не исчезает от одного нормального вопроса.",
        options = listOf(
            o("middleman_lesson", "Доверять проверке, а не срочности", "✅", MONTHLY_TICK,
                Effect(knowledgeDelta = 18, stressDelta = -4, setFlags = setOf("learned.scam.middleman")))
        )
    )

    private fun middlemanResult() = e(
        id = "scam_middleman_result",
        flavor = "💀",
        tags = setOf("scam.middleman", "consequence"),
        schemeExplanation = middlemanExplanation,
        message = "Срок поставки прошел. Сначала были обещания, потом болезнь родственника, потом телефон выключился. Товара нет.",
        options = listOf(
            o("middleman_loss_lesson", "Зафиксировать потерю и правило предоплаты", "📖", MONTHLY_TICK,
                Effect(knowledgeDelta = 24, stressDelta = 16, setFlags = setOf("learned.scam.middleman", "lost_money_to_scam")))
        )
    )

    private fun training90s() = e(
        id = "scam_training_90s",
        flavor = "📚",
        tags = setOf("scam", "scam.training"),
        unique = true,
        poolWeight = 7,
        conditions = listOf(era("kz_90s"), capitalAtLeast(40_000L), notLearned("scam.training")),
        schemeExplanation = trainingExplanation,
        message = "В ДК проводят семинар «как стать богатым за год». Первый билет недорогой, но на сцене уже продают закрытый курс.",
        options = trainingOptions(40_000L)
    )

    private fun training2005() = e(
        id = "scam_training_2005",
        flavor = "🧘",
        tags = setOf("scam", "scam.training"),
        unique = true,
        poolWeight = 7,
        conditions = listOf(era("kz_2005"), capitalAtLeast(60_000L), notLearned("scam.training")),
        schemeExplanation = trainingExplanation,
        message = "В гостинице проходит бизнес-тренинг: «мышление миллионера», «пакет VIP только сегодня», оплата в кассу у выхода.",
        options = trainingOptions(70_000L)
    )

    private fun training2015() = e(
        id = "scam_training_2015",
        flavor = "🔥",
        tags = setOf("scam", "scam.training"),
        unique = true,
        poolWeight = 9,
        conditions = listOf(era("kz_2015"), capitalAtLeast(60_000L), notLearned("scam.training")),
        schemeExplanation = trainingExplanation,
        message = "Instagram-коуч обещает запустить онлайн-школу за 21 день. Вебинар бесплатный, но «настоящая работа» начинается после оплаты тарифа.",
        options = trainingOptions(90_000L)
    )

    private fun training2024() = e(
        id = "scam_training_2024",
        flavor = "📲",
        tags = setOf("scam", "scam.training"),
        unique = true,
        poolWeight = 9,
        conditions = listOf(era("kz_2024"), capitalAtLeast(60_000L), notLearned("scam.training")),
        schemeExplanation = trainingExplanation,
        message = "TikTok ведет на марафон по «финансовому мышлению». В чате сотни сообщений, куратор пишет: «Если не оплатишь сегодня, значит выбираешь бедность».",
        options = trainingOptions(120_000L)
    )

    private fun trainingOptions(price: Long) = listOf(
        o("training_pay", "Оплатить первый уровень", "💳", "scam_training_level_two",
            Effect(capitalDelta = -price, stressDelta = -4, knowledgeDelta = 2)),
        o("training_check", "Проверить программу и договор возврата", "🔍", "scam_training_checked",
            Effect(knowledgeDelta = 10)),
        o("training_skip", "Не покупать эмоцию", "🛡️", MONTHLY_TICK,
            Effect(knowledgeDelta = 6, setFlags = setOf("learned.scam.training")))
    )

    private fun trainingChecked() = e(
        id = "scam_training_checked",
        flavor = "🔍",
        tags = setOf("scam.training", "reflection"),
        schemeExplanation = trainingExplanation,
        message = "В программе много мотивации и мало проверяемого навыка. Договор возврата туманный, а отзывы на независимых площадках совсем другие.",
        options = listOf(
            o("training_lesson", "Учиться по измеримому результату", "✅", MONTHLY_TICK,
                Effect(knowledgeDelta = 18, stressDelta = -3, setFlags = setOf("learned.scam.training")))
        )
    )

    private fun trainingLevelTwo() = e(
        id = "scam_training_level_two",
        flavor = "💡",
        tags = setOf("scam.training", "consequence"),
        schemeExplanation = trainingExplanation,
        message = "После первого уровня говорят, что настоящий доступ откроется в VIP. Цена выше, давление сильнее: «Иначе ты сольешь свой потенциал».",
        options = listOf(
            o("training_exit", "Остановиться", "🛑", MONTHLY_TICK,
                Effect(knowledgeDelta = 20, stressDelta = 6, setFlags = setOf("learned.scam.training", "lost_money_to_scam"))),
            o("training_pay_more", "Оплатить VIP", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -220_000L, stressDelta = -4, riskDelta = 12))
        )
    )

    private fun romance2015() = e(
        id = "scam_romance_2015",
        flavor = "💌",
        tags = setOf("scam", "scam.romance", "scam.forex"),
        unique = true,
        poolWeight = 7,
        conditions = listOf(era("kz_2015"), capitalAtLeast(120_000L), notLearned("scam.romance")),
        schemeExplanation = romanceExplanation,
        message = "На Mamba/VK начинается теплая переписка. Через пару недель собеседник рассказывает про наставника на валютном рынке и предлагает попробовать вместе.",
        options = listOf(
            o("romance_invest_2015", "Довериться и попробовать", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -120_000L, riskDelta = 18, scheduleEvent = ScheduledEvent("scam_forex_withdrawal", 2))),
            o("romance_check_2015", "Попросить видеозвонок и не смешивать деньги", "📹", "scam_romance_caught",
                Effect(knowledgeDelta = 12)),
            o("romance_skip_2015", "Закрыть тему денег", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 6, setFlags = setOf("learned.scam.romance")))
        )
    )

    private fun romance2024() = e(
        id = "scam_romance_2024",
        flavor = "💌",
        tags = setOf("scam", "scam.romance", "scam.crypto"),
        unique = true,
        poolWeight = 10,
        conditions = listOf(era("kz_2024"), capitalAtLeast(120_000L), notLearned("scam.romance")),
        schemeExplanation = romanceExplanation,
        message = "В WhatsApp пишет человек «ошиблась номером». Разговор становится личным, потом появляется тема криптобиржи, где у нее якобы стабильно получается.",
        options = listOf(
            o("romance_invest_2024", "Перевести небольшую сумму", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -150_000L, riskDelta = 20, scheduleEvent = ScheduledEvent("scam_romance_freeze", 2))),
            o("romance_check_2024", "Попросить видеозвонок и тестовый вывод", "🧪", "scam_romance_caught",
                Effect(knowledgeDelta = 12)),
            o("romance_skip_2024", "Не обсуждать деньги с незнакомцем", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 6, setFlags = setOf("learned.scam.romance")))
        )
    )

    private fun romanceCaught() = e(
        id = "scam_romance_caught",
        flavor = "🚩",
        tags = setOf("scam.romance", "reflection"),
        schemeExplanation = romanceExplanation,
        message = "На просьбе о видеозвонке и выводе маленькой суммы история начинает сыпаться. Теплая переписка была частью воронки.",
        options = listOf(
            o("romance_lesson", "Держать отношения отдельно от инвестиций", "✅", MONTHLY_TICK,
                Effect(knowledgeDelta = 22, stressDelta = -4, setFlags = setOf("learned.scam.romance")))
        )
    )

    private fun romanceFreeze() = e(
        id = "scam_romance_freeze",
        flavor = "🔒",
        tags = setOf("scam.romance", "scam.crypto", "consequence"),
        schemeExplanation = romanceExplanation,
        message = "В кабинете нарисована прибыль, но вывод заблокирован: нужно оплатить налог/комиссию. Собеседник давит мягко: «Я же тоже так делала».",
        options = listOf(
            o("romance_pay_tax", "Оплатить комиссию", "😰", MONTHLY_TICK,
                Effect(capitalDelta = -90_000L, stressDelta = 18, scheduleEvent = ScheduledEvent("scam_romance_final", 1))),
            o("romance_stop", "Остановиться", "🛑", "scam_romance_caught",
                Effect(knowledgeDelta = 18, stressDelta = 8))
        )
    )

    private fun romanceFinal() = e(
        id = "scam_romance_final",
        flavor = "💔",
        tags = setOf("scam.romance", "consequence"),
        schemeExplanation = romanceExplanation,
        message = "Аккаунт исчез. Поддержка молчит. Теперь видно, что вся история вела к одному моменту: заставить доплатить после первой потери.",
        options = listOf(
            o("romance_final_lesson", "Зафиксировать урок", "📖", MONTHLY_TICK,
                Effect(knowledgeDelta = 28, stressDelta = 18, setFlags = setOf("learned.scam.romance", "learned.scam.crypto", "lost_money_to_scam")))
        )
    )

    private fun forex2015() = e(
        id = "scam_forex_2015",
        flavor = "📊",
        tags = setOf("scam", "scam.forex", "scam.crypto"),
        unique = true,
        poolWeight = 9,
        conditions = listOf(era("kz_2015"), capitalAtLeast(80_000L), notLearned("scam.forex")),
        schemeExplanation = forexCryptoExplanation,
        message = "Баннер в интернете ведет на Forex/Bitcoin-платформу. Менеджер звонит и обещает заработать на девальвации, если пополнить счет сегодня.",
        options = listOf(
            o("forex_pay_2015", "Пополнить счет", "🚀", MONTHLY_TICK,
                Effect(capitalDelta = -100_000L, riskDelta = 18, scheduleEvent = ScheduledEvent("scam_forex_withdrawal", 2))),
            o("forex_check_2015", "Проверить лицензию и вывод", "🔍", "scam_forex_checked",
                Effect(knowledgeDelta = 12)),
            o("forex_skip_2015", "Не торговать в панике", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 8, setFlags = setOf("learned.scam.forex", "learned.scam.crypto")))
        )
    )

    private fun crypto2024() = e(
        id = "scam_crypto_2024",
        flavor = "📈",
        tags = setOf("scam", "scam.crypto"),
        unique = true,
        poolWeight = 13,
        conditions = listOf(era("kz_2024"), capitalAtLeast(80_000L), notLearned("scam.crypto")),
        schemeExplanation = forexCryptoExplanation,
        message = "Telegram-канал рекламирует новую биржу с бонусом за депозит. В интерфейсе красивые графики, но приложение ставится не из официального магазина.",
        options = listOf(
            o("crypto_pay_2024", "Закинуть депозит", "🚀", MONTHLY_TICK,
                Effect(capitalDelta = -140_000L, riskDelta = 20, scheduleEvent = ScheduledEvent("scam_forex_withdrawal", 2))),
            o("crypto_check_2024", "Проверить домен, лицензию и вывод", "🔍", "scam_forex_checked",
                Effect(knowledgeDelta = 12)),
            o("crypto_skip_2024", "Не ставить приложение по ссылке", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 8, setFlags = setOf("learned.scam.crypto")))
        )
    )

    private fun forexChecked() = e(
        id = "scam_forex_checked",
        flavor = "🔍",
        tags = setOf("scam.forex", "scam.crypto", "reflection"),
        schemeExplanation = forexCryptoExplanation,
        message = "Проверка показывает несостыковки: юрлицо в офшоре, лицензия не бьется, вывод маленькой суммы требует «активации счета».",
        options = listOf(
            o("forex_lesson", "Не путать интерфейс с реальным рынком", "✅", MONTHLY_TICK,
                Effect(knowledgeDelta = 22, stressDelta = -4, setFlags = setOf("learned.scam.forex", "learned.scam.crypto")))
        )
    )

    private fun forexWithdrawal() = e(
        id = "scam_forex_withdrawal",
        flavor = "🚨",
        tags = setOf("scam.forex", "scam.crypto", "consequence"),
        schemeExplanation = forexCryptoExplanation,
        message = "В кабинете прибыль, но вывести ее нельзя: менеджер просит оплатить комиссию, налог или верификацию. Без этого счет якобы заморозят.",
        options = listOf(
            o("forex_pay_fee", "Заплатить комиссию", "😰", MONTHLY_TICK,
                Effect(capitalDelta = -60_000L, stressDelta = 18, scheduleEvent = ScheduledEvent("scam_forex_final", 1))),
            o("forex_stop", "Остановиться и не доплачивать", "🛑", MONTHLY_TICK,
                Effect(knowledgeDelta = 24, stressDelta = 10, setFlags = setOf("learned.scam.forex", "learned.scam.crypto", "lost_money_to_scam")))
        )
    )

    private fun forexFinal() = e(
        id = "scam_forex_final",
        flavor = "💀",
        tags = setOf("scam.forex", "scam.crypto", "consequence"),
        schemeExplanation = forexCryptoExplanation,
        message = "После доплаты появляется новая причина блокировки. Это не рынок, а воронка дополнительных платежей.",
        options = listOf(
            o("forex_final_lesson", "Закрыть тему и записать правила проверки", "📖", MONTHLY_TICK,
                Effect(knowledgeDelta = 28, stressDelta = 16, setFlags = setOf("learned.scam.forex", "learned.scam.crypto", "lost_money_to_scam")))
        )
    )

    private fun escrowLoss() = e(
        id = "scam_escrow_loss",
        flavor = "💳",
        tags = setOf("scam.escrow", "consequence"),
        schemeExplanation = escrowExplanation,
        message = "Официальный кабинет не видит заказа. Поддержка сервиса говорит: такой сделки нет. Ссылка была копией, а деньги ушли напрямую мошеннику.",
        options = listOf(
            o("escrow_lesson_loss", "Проверять оплату только внутри сервиса", "📖", MONTHLY_TICK,
                Effect(knowledgeDelta = 24, stressDelta = 14, setFlags = setOf("learned.scam.escrow", "lost_money_to_scam")))
        )
    )

    private fun escrowChecked() = e(
        id = "scam_escrow_checked",
        flavor = "🔍",
        tags = setOf("scam.escrow", "reflection"),
        schemeExplanation = escrowExplanation,
        message = "В официальном приложении заказа нет. Домен в ссылке отличается одной буквой. Сделка развалилась до оплаты, как и должна развалиться плохая схема.",
        options = listOf(
            o("escrow_lesson", "Не платить вне официального сервиса", "✅", MONTHLY_TICK,
                Effect(knowledgeDelta = 20, stressDelta = -4, setFlags = setOf("learned.scam.escrow")))
        )
    )

    private fun presaleDelay() = e(
        id = "scam_presale_delay",
        flavor = "🏚️",
        tags = setOf("scam.presale", "mortgage", "consequence"),
        schemeExplanation = presaleExplanation,
        message = "Стройка встала. Менеджеры говорят про перенос сроков, потом про смену подрядчика. Деньги внесены, а рычагов почти нет.",
        options = listOf(
            o("presale_lesson_loss", "Разобрать риск долевого строительства", "📖", MONTHLY_TICK,
                Effect(knowledgeDelta = 26, stressDelta = 18, setFlags = setOf("learned.scam.presale", "lost_money_to_scam")))
        )
    )

    private fun presaleChecked() = e(
        id = "scam_presale_checked",
        flavor = "📋",
        tags = setOf("scam.presale", "mortgage", "reflection"),
        schemeExplanation = presaleExplanation,
        message = "Проверка показывает судебные дела и странного посредника в договоре. Скидка была платой за риск, который тебе пытались не показать.",
        options = listOf(
            o("presale_lesson", "Покупать только понятный риск", "✅", MONTHLY_TICK,
                Effect(knowledgeDelta = 22, stressDelta = -4, setFlags = setOf("learned.scam.presale")))
        )
    )

    val all: List<GameEvent> get() = listOf(
        pyramid90s(), pyramid2005(), pyramid2015(), pyramid2024(), pyramidAvoided(), pyramidCollapse(),
        mlm90s(), mlm2005(), mlm2015(), mlm2024(), mlmExposed(), mlmStock(),
        betting2005(), betting2015(), betting2024(), bettingExplained(), bettingLoss(),
        mfo90s(), mfo2005(), mfoOnline2015(), mfoOnline2024(), mfoContract(), mfoSigned(),
        middleman90s(), middleman2005(), middleman2015(), middleman2024(), middlemanRefused(), middlemanResult(),
        training90s(), training2005(), training2015(), training2024(), trainingChecked(), trainingLevelTwo(),
        romance2015(), romance2024(), romanceCaught(), romanceFreeze(), romanceFinal(),
        forex2015(), crypto2024(), forexChecked(), forexWithdrawal(), forexFinal(),
        escrowLoss(), escrowChecked(), presaleDelay(), presaleChecked()
    )

    val poolEntries: List<PoolEntry> get() = listOf(
        PoolEntry("scam_pyramid_90s", 18),
        PoolEntry("scam_pyramid_2005", 15),
        PoolEntry("scam_pyramid_2015", 16),
        PoolEntry("scam_pyramid_2024", 16),
        PoolEntry("scam_mlm_90s", 12),
        PoolEntry("scam_mlm_2005", 12),
        PoolEntry("scam_mlm_2015", 14),
        PoolEntry("scam_mlm_2024", 12),
        PoolEntry("scam_betting_2005", 7),
        PoolEntry("scam_betting_2015", 10),
        PoolEntry("scam_betting_2024", 10),
        PoolEntry("scam_mfo_90s", 10),
        PoolEntry("scam_mfo_2005", 10),
        PoolEntry("scam_mfo_2015", 10),
        PoolEntry("scam_mfo_2024", 10),
        PoolEntry("scam_middleman_90s", 10),
        PoolEntry("scam_middleman_2005", 10),
        PoolEntry("scam_middleman_2015", 10),
        PoolEntry("scam_middleman_2024", 10),
        PoolEntry("scam_training_90s", 7),
        PoolEntry("scam_training_2005", 7),
        PoolEntry("scam_training_2015", 9),
        PoolEntry("scam_training_2024", 9),
        PoolEntry("scam_romance_2015", 7),
        PoolEntry("scam_romance_2024", 10),
        PoolEntry("scam_forex_2015", 9),
        PoolEntry("scam_crypto_2024", 13)
    )

    fun findById(id: String): GameEvent? = all.find { it.id == id }
}
