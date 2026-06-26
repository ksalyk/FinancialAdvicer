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
 * Тимур «Свой продукт» — `timur_2024` · era `kz_2024` · 2024 · KZT.
 *
 * Logline: 20, Астана, студент-айтишник. Первый зарубежный клиент на Upwork — $500
 * за лендинг. Скилл есть, буфера нет, мир открыт. Одногруппник зовёт на госпрактику.
 *
 * Lesson (Bible §7): income diversification & human-capital investment — resilience =
 * multiple streams (freelance + product + FX) + skill-investment fork + earning hard
 * currency as FX insurance. Secondary: feast/famine cash flow, entrepreneurship,
 * crypto hype, client non-payment.
 *
 * Architecture:
 *  - Act I is a scripted chain (direct `next`). Each scripted beat schedules the next
 *    via `scheduleEvent` so routine pool months fill the gaps between backbone events.
 *  - Signature mechanics: stackable `incomeDelta` streams via flags; skill-course
 *    human-capital fork (capital− now → incomeDelta+ later via scheduled event);
 *    USD income → `capitalDelta+` windfall when tenge weakens (FX lesson).
 *  - 5 endings gated on `arc.final_check` (except BANKRUPTCY on capital ≤ 0).
 *
 * Numbers are tuning-ready starting points; run `./gradlew :shared:test` to verify.
 */
class TimurScenarioGraph : ScenarioGraph() {

    override val initialPlayerState: PlayerState = PlayerState(
        capital = 80_000L,
        income = 200_000L,
        expenses = 120_000L,
        debt = 0L,
        debtPaymentMonthly = 0L,
        investments = 0L,
        investmentReturnRate = 0.08,
        stress = 30,
        financialKnowledge = 20,
        riskLevel = 30,
        month = 1,
        year = 2024,
        characterId = "timur_2024",
        eraId = "kz_2024",
        currency = CurrencyCode.KZT,
        flags = setOf("skill.tech"),
    )

    override val events: Map<String, kz.fearsom.financiallifev2.model.GameEvent> = listOf(
        introArc(),
        streamArc(),
        drySpellArc(),
        productArc(),
        endgameArc(),
        regularLifeArc(),
    ).buildEvents()

    override val conditionalEvents: List<kz.fearsom.financiallifev2.model.GameEvent> = listOf(
        conditionalsArc(),
        endingsArc(),
    ).flattenEvents()

    override val eventPool: List<PoolEntry> = listOf(
        PoolEntry("normal_life", 10),
        PoolEntry("pool_coworking", 8),
        PoolEntry("pool_mentor_call", 7),
        PoolEntry("pool_side_rush", 6),
        PoolEntry("pool_hackathon", 6),
        PoolEntry("pool_flat_rent_raise", 5),
        PoolEntry("pool_friend_loan", 5),
        PoolEntry("pool_equipment_fail", 4),
        PoolEntry("pool_telegram_scam", 4),
        PoolEntry("pool_infocoach", 3),
        PoolEntry("pool_forex_ad", 3),
    )
}

// ════════════════════════════════════════════════════════════════════════════
//  АКТ I — Первый клиент (scripted chain, direct `next`, no tick until setup_finances)
// ════════════════════════════════════════════════════════════════════════════

private fun introArc(): EventArc = arc(
    "intro",
    event(
        id = "intro",
        flavor = "💻",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Первый зарубежный заказ: $500 за лендинг — это три твоих стипендии. " +
                "Клиент пишет на ломаном английском, дедлайн жёсткий.",
            "Одногруппник Нурбек зовёт на стабильную практику в министерство: " +
                "«Там хоть оклад, страховка, и в резюме красиво». " +
                "Ещё вариант — брать всё подряд на Kwork и Upwork, лишь бы деньги шли.",
        ),
        options = listOf(
            option(
                "take_usd_client", "Взять заказ и вложить часть в React-курс", "📈",
                "client_onboard",
                Effect(
                    capitalDelta = 10_000L,
                    knowledgeDelta = 3,
                    stressDelta = 5,
                    setFlags = setOf("choice.usd_client"),
                    scheduleEvent = ScheduledEvent("course_payoff", 4),
                ),
            ),
            option(
                "gov_job", "Пойти на госпрактику ради стабильности", "🏢",
                "client_onboard",
                Effect(
                    incomeDelta = 60_000L,
                    stressDelta = -5,
                    setFlags = setOf("choice.gov_job", "stream.local"),
                ),
            ),
            option(
                "hustle_all", "Брать всё подряд — лишь бы платили", "🔥",
                "client_onboard",
                Effect(
                    incomeDelta = 30_000L,
                    stressDelta = 10,
                    riskDelta = 5,
                    setFlags = setOf("choice.hustle"),
                ),
            ),
        ),
    ),
    event(
        id = "client_onboard",
        flavor = "📨",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Клиент принял работу. 500 долларов по курсу ~450 ₸ — 225 000 ₸ на Kaspi. " +
                "Это твой первый доллар, и он ощущается иначе, чем тенге.",
            "Нурбек пишет: «Ну как? Я уже второй месяц получаю. Стабильно». " +
                "Ты смотришь на свой перевод и думаешь об обменнике.",
        ),
        options = listOf(
            option(
                "convert_all", "Конвертировать всё в тенге — деньги нужны сейчас", "💸",
                "setup_finances",
                Effect(capitalDelta = 225_000L, knowledgeDelta = 1),
            ),
            option(
                "keep_usd", "Оставить часть в долларах — пусть лежит как подушка", "🛡️",
                "setup_finances",
                Effect(
                    capitalDelta = 150_000L,
                    knowledgeDelta = 3,
                    setFlags = setOf("stream.usd"),
                ),
            ),
            option(
                "invest_equipment", "Купить хороший монитор — работа пойдёт быстрее", "🖥️",
                "setup_finances",
                Effect(
                    capitalDelta = 175_000L,
                    incomeDelta = 15_000L,
                    knowledgeDelta = 2,
                    setFlags = setOf("has.equipment"),
                ),
            ),
        ),
    ),
    event(
        id = "setup_finances",
        flavor = "📊",
        unique = true,
        message = story(
            "Февраль. Первый месяц «как взрослый» — аренда, еда, интернет, Upwork Pro. " +
                "Ты садишься и впервые считаешь: сколько ушло и сколько осталось.",
            "Расходы: 120 000 ₸. Доход нестабильный — в этом месяце 200 000, " +
                "но в прошлом было 80 000. Нурбек получает ровно 130 000 каждый месяц.",
        ),
        options = listOf(
            option(
                "make_budget", "Составить бюджет и отложить 30% до трат", "📝",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = 20_000L,
                    knowledgeDelta = 3,
                    stressDelta = -3,
                    setFlags = setOf("has.budget"),
                    scheduleEvent = ScheduledEvent("stream_build", 2),
                ),
            ),
            option(
                "spend_freely", "Не считать — деньги есть, потом разберусь", "🎮",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = -15_000L,
                    stressDelta = 3,
                    riskDelta = 5,
                    scheduleEvent = ScheduledEvent("stream_build", 2),
                ),
            ),
            option(
                "ask_mentor", "Написать ментору Асету — как он ведёт финансы?", "💬",
                MONTHLY_TICK,
                Effect(
                    knowledgeDelta = 4,
                    stressDelta = -2,
                    setFlags = setOf("has.mentor", "has.budget"),
                    scheduleEvent = ScheduledEvent("stream_build", 2),
                ),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ II — Строим потоки (scheduled backbone)
// ════════════════════════════════════════════════════════════════════════════

private fun streamArc(): EventArc = arc(
    "streams",
    event(
        id = "stream_build",
        flavor = "🌊",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Апрель. Ментор Асет говорит: «Одна точка дохода — это ловушка. " +
                "Клиент уходит — и всё. Тебе нужны минимум два потока».",
            "Варианты: добавить локальный заказ через Kwork параллельно Upwork, " +
                "запустить телеграм-канал с туториалами (долго, но растёт), " +
                "или взять курс по AI-интеграции — спрос высокий, ценник выше.",
        ),
        options = listOf(
            option(
                "add_kwork", "Добавить Kwork — быстрые деньги, другая аудитория", "🛒",
                MONTHLY_TICK,
                Effect(
                    incomeDelta = 40_000L,
                    stressDelta = 8,
                    setFlags = setOf("stream.local"),
                    scheduleEvent = ScheduledEvent("fx_swing", 3),
                ),
            ),
            option(
                "build_channel", "Запустить Telegram-канал — инвестиция в будущее", "📢",
                MONTHLY_TICK,
                Effect(
                    knowledgeDelta = 4,
                    stressDelta = 5,
                    setFlags = setOf("stream.content"),
                    scheduleEvent = ScheduledEvent("channel_payoff", 5),
                ),
            ),
            option(
                "take_ai_course", "Пройти курс по AI — $200 из кармана", "🤖",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = -90_000L,
                    knowledgeDelta = 6,
                    setFlags = setOf("has.ai_skill"),
                    scheduleEvent = ScheduledEvent("ai_income_boost", 3),
                ),
            ),
        ),
    ),
    event(
        id = "course_payoff",
        flavor = "🎓",
        unique = true,
        tags = setOf("career"),
        message = story(
            "React-курс окончен. Первый проект на нём — одностраничное приложение " +
                "для малого бизнеса. Заказчик доволен, платит $200 сверху.",
            "Ты замечаешь: с новым скиллом оценки выросли. " +
                "Upwork-профиль поднялся в поиске, пишут чаще.",
        ),
        options = listOf(
            option(
                "raise_rate", "Поднять ставку на Upwork до $25/час", "💵",
                MONTHLY_TICK,
                Effect(incomeDelta = 30_000L, knowledgeDelta = 2, stressDelta = -3),
            ),
            option(
                "more_volume", "Брать больше заказов по старой цене", "⚡",
                MONTHLY_TICK,
                Effect(incomeDelta = 20_000L, stressDelta = 10),
            ),
        ),
    ),
    event(
        id = "fx_swing",
        flavor = "📉",
        unique = true,
        tags = setOf("crisis", "economy"),
        message = story(
            "Тенге слабеет — доллар вырос на 12% за месяц. Новости кричат про инфляцию. " +
                "Нурбек паникует: «Мне зарплату в тенге платят, всё дорожает!»",
            "Ты смотришь на свои долларовые поступления. Тот же труд — " +
                "а тенге на счёте стало на 27 000 больше просто от курса.",
        ),
        options = listOf(
            option(
                "understand_fx", "Осмыслить: жёсткая валюта = защита от инфляции", "💡",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = 27_000L,
                    knowledgeDelta = 5,
                    stressDelta = -5,
                    setFlags = setOf("learned.fx", "stream.usd"),
                ),
            ),
            option(
                "convert_usd_panic", "Срочно конвертировать всё в тенге — вдруг откатится", "😰",
                MONTHLY_TICK,
                Effect(capitalDelta = 27_000L, stressDelta = 5, riskDelta = -5),
            ),
            option(
                "buy_more_usd", "Купить ещё долларов на свои тенге — ловить момент", "🎯",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = -30_000L,
                    knowledgeDelta = 3,
                    setFlags = setOf("stream.usd", "learned.fx"),
                ),
            ),
        ),
    ),
    event(
        id = "channel_payoff",
        flavor = "📊",
        unique = true,
        message = story(
            "Telegram-канал набрал 1 200 подписчиков. Рекламодатель пишет: " +
                "«Хочу разместить пост — 15 000 ₸». Первые деньги от контента.",
            "Это не большие деньги, но это пассивный поток — " +
                "ты спал, а они пришли. Асет кивает: «Вот так начинается диверсификация».",
        ),
        options = listOf(
            option(
                "accept_ads", "Принять рекламу — стабильный доход", "✅",
                MONTHLY_TICK,
                Effect(
                    incomeDelta = 15_000L,
                    knowledgeDelta = 2,
                    setFlags = setOf("stream.content.monetized"),
                ),
            ),
            option(
                "grow_first", "Отказаться — сначала дорасти до 5 000 и ценить выше", "📈",
                MONTHLY_TICK,
                Effect(knowledgeDelta = 3, stressDelta = -2),
            ),
        ),
    ),
    event(
        id = "ai_income_boost",
        flavor = "🚀",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Первый AI-проект: автоматизация отчётов для небольшой компании. " +
                "Клиент заплатил $800 — почти вдвое больше обычного проекта.",
            "AI-скилл превращает стандартные задачи в premium-продукт. " +
                "В профиле появился тег «AI Integration» — запросы идут сами.",
        ),
        options = listOf(
            option(
                "specialize_ai", "Специализироваться на AI — ниша пока не занята", "🎯",
                MONTHLY_TICK,
                Effect(
                    incomeDelta = 60_000L,
                    knowledgeDelta = 4,
                    setFlags = setOf("stream.usd", "has.niche"),
                    scheduleEvent = ScheduledEvent("dry_spell_start", 4),
                ),
            ),
            option(
                "diversify_skills", "Не зацикливаться — AI плюс обычная разработка", "⚖️",
                MONTHLY_TICK,
                Effect(
                    incomeDelta = 30_000L,
                    knowledgeDelta = 3,
                    setFlags = setOf("stream.local"),
                    scheduleEvent = ScheduledEvent("dry_spell_start", 4),
                ),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ II — Засуха (dry spell: feast/famine lesson)
// ════════════════════════════════════════════════════════════════════════════

private fun drySpellArc(): EventArc = arc(
    "dry_spell",
    event(
        id = "dry_spell_start",
        flavor = "🌵",
        unique = true,
        tags = setOf("crisis"),
        message = story(
            "Основной клиент пишет: «Проект заморожен, свяжемся через месяц-два». " +
                "Upwork тихо. Октябрь, новых заявок нет.",
            "Нурбек: «Видишь? А у меня стабильно». " +
                "Ты смотришь на баланс. Расходы никуда не делись: 120 000 ₸ в месяц.",
        ),
        options = listOf(
            option(
                "use_buffer", "Переждать на буфере — у меня есть подушка", "🛡️",
                "dry_spell_resolve",
                Effect(stressDelta = 5, knowledgeDelta = 2),
            ),
            option(
                "panic_gig", "Хвататься за всё подряд — любой заказ за любые деньги", "😰",
                "dry_spell_resolve",
                Effect(capitalDelta = 40_000L, stressDelta = 20, knowledgeDelta = 1),
            ),
            option(
                "gov_job_temp", "Временно взять тот самый «стабильный» вариант Нурбека", "🏢",
                "dry_spell_resolve",
                Effect(
                    incomeDelta = 80_000L,
                    stressDelta = -5,
                    knowledgeDelta = 1,
                    setFlags = setOf("took.gov_temp"),
                ),
            ),
        ),
    ),
    event(
        id = "dry_spell_resolve",
        flavor = "🌤️",
        unique = true,
        message = story(
            "Клиент вернулся через семь недель. Заказ возобновлён, проект живёт.",
            "Засуха закончилась — но урок остался: " +
                "без буфера на 3 месяца расходов один молчащий клиент = кризис.",
        ),
        options = listOf(
            option(
                "build_buffer", "Первым делом накопить 360 000 ₸ — 3 месяца расходов", "💰",
                MONTHLY_TICK,
                Effect(
                    knowledgeDelta = 4,
                    stressDelta = -8,
                    setFlags = setOf("has.buffer", "learned.buffer"),
                    scheduleEvent = ScheduledEvent("client_dispute", 3),
                ),
            ),
            option(
                "diversify_clients", "Найти ещё двух клиентов — не зависеть от одного", "🕸️",
                MONTHLY_TICK,
                Effect(
                    incomeDelta = 25_000L,
                    knowledgeDelta = 3,
                    setFlags = setOf("learned.buffer"),
                    scheduleEvent = ScheduledEvent("client_dispute", 3),
                ),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  АКТ III — Свой продукт (product idea + client dispute + endgame)
// ════════════════════════════════════════════════════════════════════════════

private fun productArc(): EventArc = arc(
    "product",
    event(
        id = "client_dispute",
        flavor = "⚖️",
        unique = true,
        tags = setOf("crisis"),
        message = story(
            "Клиент исчез после сдачи проекта. Скачал код, перестал отвечать. " +
                "$600 завис в воздухе. Upwork говорит: «Контракт закрыт, оспорить сложно».",
            "Ментор Асет: «Это случается. Поэтому не начинай работу без депозита " +
                "и не отдавай исходники до окончательной оплаты».",
        ),
        options = listOf(
            option(
                "dispute_platform", "Открыть спор на платформе — хоть что-то вернуть", "⚡",
                "product_idea",
                Effect(
                    capitalDelta = 100_000L,
                    stressDelta = 10,
                    knowledgeDelta = 3,
                    setFlags = setOf("learned.contract"),
                ),
            ),
            option(
                "accept_loss", "Принять потерю, изменить контрактный процесс навсегда", "📋",
                "product_idea",
                Effect(
                    stressDelta = 5,
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.contract"),
                ),
            ),
        ),
    ),
    event(
        id = "product_idea",
        flavor = "💡",
        unique = true,
        tags = setOf("career"),
        message = story(
            "На хакатоне ты встречаешь Данияра — он строит SaaS для казахстанских " +
                "бухгалтеров. «Все хотят работать на иностранцев, а рядом пустой рынок».",
            "Идея: простой инструмент для микро-бизнеса — договора, акты, счета. " +
                "Никто не делает на кириллице и с Kaspi-интеграцией. " +
                "Потенциал — абонентская плата, а не разовые заказы.",
        ),
        options = listOf(
            option(
                "build_mvp", "Взяться за MVP — месяц работы плюс заказы", "🏗️",
                "product_mvp",
                Effect(
                    capitalDelta = -50_000L,
                    stressDelta = 10,
                    knowledgeDelta = 3,
                    setFlags = setOf("building.product"),
                ),
            ),
            option(
                "validate_first", "Сначала поговорить с 10 бухгалтерами — нужно ли это им?", "🔍",
                "product_mvp",
                Effect(
                    knowledgeDelta = 5,
                    stressDelta = -3,
                    setFlags = setOf("building.product", "has.validated"),
                ),
            ),
            option(
                "skip_product", "Оставить идею — сейчас важнее доходы от фриланса", "💼",
                MONTHLY_TICK,
                Effect(
                    incomeDelta = 20_000L,
                    knowledgeDelta = 1,
                    scheduleEvent = ScheduledEvent("relocation_choice", 3),
                ),
            ),
        ),
    ),
    event(
        id = "product_mvp",
        flavor = "🚀",
        unique = true,
        tags = setOf("career"),
        message = story(
            "MVP запущен. Первые три бесплатных пользователя — одна реально пользуется. " +
                "Данияр говорит: «Конверсия 33% на холодной аудитории — это хорошо».",
            "Первый платящий: 5 000 ₸/мес, маленькая бухгалтерская фирма. " +
                "Маленькие деньги — но это уже продукт, а не просто фриланс.",
        ),
        options = listOf(
            option(
                "grow_product", "Вкладывать 30% дохода в развитие продукта", "📈",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = -30_000L,
                    incomeDelta = 20_000L,
                    knowledgeDelta = 4,
                    setFlags = setOf("stream.product"),
                    scheduleEvent = ScheduledEvent("relocation_choice", 3),
                ),
            ),
            option(
                "sell_product", "Предложить Данияру: объединить продукты или продать идею", "🤝",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = 200_000L,
                    knowledgeDelta = 3,
                    setFlags = setOf("stream.product"),
                    scheduleEvent = ScheduledEvent("relocation_choice", 3),
                ),
            ),
        ),
    ),
    event(
        id = "relocation_choice",
        flavor = "✈️",
        unique = true,
        tags = setOf("career"),
        message = story(
            "Linkedin. Оффер из Варшавы: Junior Dev, €2 000/мес, релокационный пакет.",
            "Асет: «Уехать — не сдаться. Но строить здесь тоже реально, " +
                "особенно если у тебя есть продукт и клиенты в USD». " +
                "Нурбек: «Я б уехал, но жена, ипотека...»",
        ),
        options = listOf(
            option(
                "relocate", "Принять оффер — новая жизнь, опыт, евро", "🌍",
                MONTHLY_TICK,
                Effect(
                    capitalDelta = 150_000L,
                    incomeDelta = 80_000L,
                    stressDelta = 10,
                    knowledgeDelta = 3,
                    setFlags = setOf("relocated"),
                    scheduleEvent = ScheduledEvent("final_reflection", 2),
                ),
            ),
            option(
                "stay_build", "Остаться — Казахстан пустой рынок, своё интереснее", "🏠",
                MONTHLY_TICK,
                Effect(
                    knowledgeDelta = 3,
                    stressDelta = -5,
                    setFlags = setOf("chose.stay"),
                    scheduleEvent = ScheduledEvent("final_reflection", 2),
                ),
            ),
            option(
                "remote_hybrid", "Взять оффер как ремоут — доход в евро, жизнь в Астане", "💻",
                MONTHLY_TICK,
                Effect(
                    incomeDelta = 50_000L,
                    knowledgeDelta = 3,
                    setFlags = setOf("stream.usd", "stream.eu_remote"),
                    scheduleEvent = ScheduledEvent("final_reflection", 2),
                ),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Финал — итоговая рефлексия + триггер концовок
// ════════════════════════════════════════════════════════════════════════════

private fun endgameArc(): EventArc = arc(
    "endgame",
    event(
        id = "final_reflection",
        flavor = "📜",
        unique = true,
        message = story(
            "Конец года. Ты открываешь таблицу, которую начал вести с февраля — " +
                "доходы, расходы, клиенты, потоки.",
            "12 месяцев назад был один клиент и 80 000 ₸ на счету. " +
                "Что изменилось? Сколько потоков? Есть ли буфер? " +
                "Нурбек звонит: «Нам повысили на 5% — ты рад за меня?»",
        ),
        options = listOf(
            option(
                "count_results", "Честно посчитать всё — доходы, потоки, буфер, знания", "🧾",
                MONTHLY_TICK,
                Effect(knowledgeDelta = 2, setFlags = setOf("arc.final_check")),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Рутина — пул событий (pool events, unique = false)
// ════════════════════════════════════════════════════════════════════════════

private fun regularLifeArc(): EventArc = arc(
    "regular_life",
    event(
        id = "normal_life",
        flavor = "🌤️",
        cooldownMonths = 1,
        message = story(
            "Обычный месяц: заказы, дедлайны, аренда, продукты, коворкинг. " +
                "Астана не дешевеет — но скилл растёт, и ставка тоже.",
        ),
        options = listOf(
            option("keep_grinding", "Работать дальше, придерживаться плана", "💼", MONTHLY_TICK),
            option(
                "save_extra", "Отложить 20 000 ₸ сверх обычного в буфер", "💰", MONTHLY_TICK,
                Effect(capitalDelta = 20_000L),
            ),
            option(
                "rest_weekend", "Взять уикенд без ноутбука — перезагрузка", "🌳", MONTHLY_TICK,
                Effect(stressDelta = -5),
            ),
        ),
    ),
    event(
        id = "pool_coworking",
        flavor = "☕",
        cooldownMonths = 2,
        message = story(
            "Коворкинг предлагает месячный абонемент: 25 000 ₸. " +
                "Или можно продолжать дома — сэкономить, но скучновато.",
            "В коворкинге — контакты, иногда заказы через знакомых.",
        ),
        options = listOf(
            option(
                "take_coworking", "Взять абонемент — сеть и атмосфера важны", "🏢", MONTHLY_TICK,
                Effect(capitalDelta = -25_000L, knowledgeDelta = 2, stressDelta = -3),
            ),
            option(
                "home_work", "Работать дома — сэкономить 25 000 ₸", "🏠", MONTHLY_TICK,
                Effect(stressDelta = 2),
            ),
        ),
    ),
    event(
        id = "pool_mentor_call",
        flavor = "📞",
        cooldownMonths = 3,
        message = story(
            "Асет зовёт на созвон. Спрашивает: «Сколько у тебя источников дохода? " +
                "Сколько месяцев протянешь без клиентов?»",
        ),
        options = listOf(
            option(
                "honest_answer", "Ответить честно и составить план диверсификации", "📋", MONTHLY_TICK,
                Effect(knowledgeDelta = 3, stressDelta = -3),
            ),
            option(
                "deflect", "Пообещать подумать — сейчас дедлайн", "⏰", MONTHLY_TICK,
                Effect(stressDelta = 2),
            ),
        ),
    ),
    event(
        id = "pool_side_rush",
        flavor = "⚡",
        cooldownMonths = 2,
        message = story(
            "Срочный заказ: лендинг за 72 часа, 80 000 ₸. " +
                "Придётся работать без выходных, но деньги хорошие.",
        ),
        options = listOf(
            option(
                "take_rush", "Взяться — 80 000 за три дня это сильно", "💪", MONTHLY_TICK,
                Effect(capitalDelta = 80_000L, stressDelta = 15),
            ),
            option(
                "pass_rush", "Отказать — нет смысла гробить здоровье", "🚫", MONTHLY_TICK,
                Effect(stressDelta = -5),
            ),
            option(
                "negotiate_price", "Согласиться, но поднять цену до 120 000 ₸", "💬", MONTHLY_TICK,
                Effect(capitalDelta = 120_000L, stressDelta = 10, knowledgeDelta = 2),
            ),
        ),
    ),
    event(
        id = "pool_hackathon",
        flavor = "🏆",
        cooldownMonths = 4,
        message = story(
            "Местный хакатон: 48 часов, тема — финтех. Призовой фонд: 500 000 ₸ за первое место.",
        ),
        options = listOf(
            option(
                "join_hackathon", "Участвовать — опыт и шанс выиграть", "🎯", MONTHLY_TICK,
                Effect(capitalDelta = 50_000L, knowledgeDelta = 4, stressDelta = 8),
            ),
            option(
                "skip_hackathon", "Пропустить — дедлайны важнее", "💼", MONTHLY_TICK,
                Effect(capitalDelta = 10_000L),
            ),
        ),
    ),
    event(
        id = "pool_flat_rent_raise",
        flavor = "🏠",
        cooldownMonths = 6,
        maxOccurrences = 2,
        message = story(
            "Хозяин квартиры поднимает аренду на 15 000 ₸. " +
                "«Всё дорожает, сам понимаешь». Сейчас платишь 90 000, будет 105 000.",
        ),
        options = listOf(
            option(
                "accept_raise", "Согласиться — искать новое жильё времени нет", "📝", MONTHLY_TICK,
                Effect(expensesDelta = 15_000L, stressDelta = 5),
            ),
            option(
                "negotiate_raise", "Поторговаться: «Готов подписать на год за 98 000»", "🤝", MONTHLY_TICK,
                Effect(expensesDelta = 8_000L, stressDelta = 2, knowledgeDelta = 2),
            ),
            option(
                "move_cheaper", "Найти другую квартиру дешевле — потратить неделю", "🔍", MONTHLY_TICK,
                Effect(capitalDelta = -10_000L, stressDelta = 8),
            ),
        ),
    ),
    event(
        id = "pool_friend_loan",
        flavor = "👥",
        cooldownMonths = 4,
        maxOccurrences = 1,
        message = story(
            "Нурбек просит взаймы 50 000 ₸ до зарплаты: «Холодильник сломался, жена ругается».",
            "Ты помнишь: одолжить другу — хороший способ потерять деньги и друга.",
        ),
        options = listOf(
            option(
                "lend_friend", "Дать — он вернёт, мы же друзья", "🤝", MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, stressDelta = 3),
            ),
            option(
                "gift_small", "Подарить 10 000 — без ожидания возврата", "🎁", MONTHLY_TICK,
                Effect(capitalDelta = -10_000L, stressDelta = -2, knowledgeDelta = 2),
            ),
            option(
                "decline_politely", "Отказать мягко: «Самому сейчас туго»", "🙏", MONTHLY_TICK,
                Effect(stressDelta = 5, knowledgeDelta = 2),
            ),
        ),
    ),
    event(
        id = "pool_equipment_fail",
        flavor = "💀",
        cooldownMonths = 6,
        maxOccurrences = 1,
        message = story(
            "Ноутбук умер в воскресенье вечером. Клиент ждёт дедлайн в среду.",
            "Ремонт: 45 000 ₸ и 4 дня. Новый б/у: 120 000 ₸ и можно работать завтра. " +
                "Аренда в коворкинге: 3 000 ₸/день.",
        ),
        options = listOf(
            option(
                "repair_laptop", "Сдать в ремонт и попросить отсрочку у клиента", "🔧", MONTHLY_TICK,
                Effect(capitalDelta = -45_000L, stressDelta = 12),
            ),
            option(
                "buy_used", "Купить б/у — деньги из буфера", "🛒", MONTHLY_TICK,
                Effect(capitalDelta = -120_000L, stressDelta = 5, knowledgeDelta = 2),
            ),
            option(
                "coworking_rent", "Арендовать ПК в коворкинге на неделю", "🏢", MONTHLY_TICK,
                Effect(capitalDelta = -21_000L, stressDelta = 7),
            ),
        ),
    ),
    event(
        id = "pool_telegram_scam",
        flavor = "⚠️",
        maxOccurrences = 1,
        message = story(
            "В Telegram-канале «Крипто сигналы 🚀»: «Binance листит новый токен, " +
                "у нас инсайд — X5 за 72 часа. Заходи сейчас, выходи с прибылью».",
            "Автор канала — 47 000 подписчиков, значки «проверено». " +
                "Одна строчка мелким: «инвестиции несут риски».",
        ),
        options = listOf(
            option(
                "invest_scam", "Вложить 30 000 ₸ — звучит как шанс", "🎰", MONTHLY_TICK,
                Effect(
                    capitalDelta = -30_000L,
                    stressDelta = 15,
                    knowledgeDelta = 4,
                    setFlags = setOf("learned.crypto.scam", "lost_money_to_scam"),
                ),
            ),
            option(
                "research_scam", "Загуглить канал — найти жалобы, понять схему", "🔍", MONTHLY_TICK,
                Effect(
                    knowledgeDelta = 5,
                    stressDelta = -2,
                    setFlags = setOf("learned.crypto.scam"),
                ),
            ),
            option(
                "ignore_scam", "Скипнуть без раздумий — это классический памп", "🚫", MONTHLY_TICK,
                Effect(knowledgeDelta = 2, setFlags = setOf("learned.crypto.scam")),
            ),
        ),
    ),
    event(
        id = "pool_infocoach",
        flavor = "📱",
        maxOccurrences = 1,
        message = story(
            "Instagram-реклама: «Как я зарабатываю 2 000 000 ₸/мес, работая 4 часа в день. " +
                "Мой курс — 150 000 ₸. Осталось 3 места. Пиши сейчас».",
            "Лендинг красивый. Отзывы есть. Но конкретики — ноль.",
        ),
        options = listOf(
            option(
                "buy_course", "Купить — вдруг реально работает?", "💳", MONTHLY_TICK,
                Effect(
                    capitalDelta = -150_000L,
                    stressDelta = 10,
                    knowledgeDelta = 2,
                    setFlags = setOf("learned.infocoach.scam"),
                ),
            ),
            option(
                "ask_mentor_course", "Спросить Асета — он поможет разобраться", "👨‍💼", MONTHLY_TICK,
                Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.infocoach.scam"),
                ),
            ),
            option(
                "ignore_ad", "Закрыть — потраченные деньги сделают умнее, но не богаче", "❌", MONTHLY_TICK,
                Effect(knowledgeDelta = 3, setFlags = setOf("learned.infocoach.scam")),
            ),
        ),
    ),
    event(
        id = "pool_forex_ad",
        flavor = "📉",
        maxOccurrences = 1,
        message = story(
            "YouTube-реклама: казахстанский брокер предлагает торговать Форекс с плечом 1:100. " +
                "«Начни с 10 000 ₸, зарабатывай как трейдер».",
        ),
        options = listOf(
            option(
                "try_forex", "Попробовать — небольшая сумма, интересно как работает", "📊", MONTHLY_TICK,
                Effect(
                    capitalDelta = -20_000L,
                    stressDelta = 8,
                    knowledgeDelta = 3,
                    riskDelta = 10,
                    setFlags = setOf("learned.forex.risk"),
                ),
            ),
            option(
                "skip_forex", "Пропустить — высокое плечо = почти гарантированные потери", "🚫", MONTHLY_TICK,
                Effect(knowledgeDelta = 4, setFlags = setOf("learned.forex.risk")),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Условные события (priority desc): давление, кризис, буфер
// ════════════════════════════════════════════════════════════════════════════

private fun conditionalsArc(): EventArc = arc(
    "conditionals",
    event(
        id = "burnout",
        flavor = "😵",
        tags = setOf("crisis"),
        priority = 100,
        cooldownMonths = 4,
        conditions = listOf(cond(STRESS, GT, 75L)),
        message = story(
            "Ты открываешь ноутбук — и не можешь начать. Пустота. " +
                "Часами смотришь в экран, пишешь три строки кода и удаляешь.",
            "Асет был прав: перегруз — это тоже стоимость. " +
                "Без отдыха производительность падает быстрее, чем растёт объём работы.",
        ),
        options = listOf(
            option(
                "take_rest", "Взять неделю без заказов — восстановиться", "🏖️", MONTHLY_TICK,
                Effect(capitalDelta = -30_000L, stressDelta = -25),
            ),
            option(
                "push_through_burnout", "Переть дальше — дедлайны не ждут", "🥵", MONTHLY_TICK,
                Effect(stressDelta = 10, riskDelta = 5),
            ),
        ),
    ),
    event(
        id = "buffer_alert",
        flavor = "🚨",
        tags = setOf("crisis"),
        priority = 90,
        cooldownMonths = 3,
        conditions = listOf(cond(CAPITAL, LT, 50_000L)),
        message = story(
            "Баланс: 48 000 ₸. Аренда через три дня: 90 000 ₸. " +
                "Это не кризис — это предупреждение. Без заказа в этом месяце — уже кризис.",
        ),
        options = listOf(
            option(
                "emergency_freelance", "Срочно найти любой заказ — Upwork, Kwork, знакомые", "⚡", MONTHLY_TICK,
                Effect(capitalDelta = 60_000L, stressDelta = 15),
            ),
            option(
                "ask_help", "Попросить родителей помочь в этот месяц", "👨‍👩‍👦", MONTHLY_TICK,
                Effect(capitalDelta = 50_000L, stressDelta = 10, knowledgeDelta = 1),
            ),
            option(
                "cut_costs", "Порезать всё лишнее — отменить подписки, перейти на домашнюю еду", "✂️", MONTHLY_TICK,
                Effect(expensesDelta = -20_000L, stressDelta = 5, knowledgeDelta = 2),
            ),
        ),
    ),
    event(
        id = "fx_bonus",
        flavor = "💹",
        tags = setOf("economy"),
        priority = 80,
        cooldownMonths = 6,
        conditions = listOf(
            cond(CAPITAL, GTE, 200_000L),
            Condition.HasFlag("stream.usd"),
            Condition.NotFlag("learned.fx"),
        ),
        message = story(
            "Тенге ослаб ещё раз. Твои USD-клиенты превращаются в большее количество тенге " +
                "без какого-либо дополнительного труда.",
            "Ментор Асет: «Видишь? Это и есть FX-диверсификация. " +
                "Не спекуляция — просто зарабатывать в сильной валюте».",
        ),
        options = listOf(
            option(
                "understand_fx_deep", "Разобраться глубже: как ещё защитить доходы от инфляции?", "📚", MONTHLY_TICK,
                Effect(capitalDelta = 35_000L, knowledgeDelta = 5, setFlags = setOf("learned.fx")),
            ),
            option(
                "just_enjoy_bonus", "Взять прибыль — на этот раз просто повезло", "😊", MONTHLY_TICK,
                Effect(capitalDelta = 35_000L, knowledgeDelta = 2, setFlags = setOf("learned.fx")),
            ),
        ),
    ),
)

// ════════════════════════════════════════════════════════════════════════════
//  Концовки — терминальные условные узлы (priority desc; capital + флаги)
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
            "Клиент ушёл. Аренда просрочена. Телефон звонит — номера незнакомые.",
            "Ты возвращаешься домой к родителям. Не провал — перезагрузка. " +
                "Но разница между тем, кем мог стать, и тем, кем стал — " +
                "отсутствие буфера на три плохих месяца.",
        ),
    ),
    ending(
        id = "ending_wealth",
        endingType = EndingType.WEALTH,
        flavor = "🤑",
        priority = 150,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, 1_000_000L),
            Condition.HasFlag("stream.product"),
            Condition.HasFlag("stream.usd"),
        ),
        message = story(
            "Три потока дохода. Продукт с платящими пользователями. " +
                "USD-клиенты, которых тенге не касается. Буфер на полгода.",
            "Нурбек спрашивает: «Как ты это сделал?» Ты думаешь: " +
                "не один большой прыжок — куча маленьких решений в правильную сторону.",
        ),
    ),
    ending(
        id = "ending_freedom",
        endingType = EndingType.FINANCIAL_FREEDOM,
        flavor = "🎯",
        priority = 140,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, 600_000L),
            Condition.HasFlag("has.buffer"),
            Condition.HasFlag("stream.usd"),
        ),
        message = story(
            "Ты впервые отказываешься от токсичного клиента без тревоги — " +
                "есть буфер, есть другие потоки.",
            "Свобода — не когда всё отлично, а когда одна точка отказа " +
                "тебя больше не убивает. Год назад это было не так.",
        ),
    ),
    ending(
        id = "ending_stability",
        endingType = EndingType.FINANCIAL_STABILITY,
        flavor = "😊",
        priority = 130,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, 300_000L),
            Condition.HasFlag("has.buffer"),
        ),
        message = story(
            "Буфер есть. Несколько клиентов. Ставка выросла. " +
                "Ноябрьская засуха прошла — и ты устоял.",
            "Стабильность — это не скучно. Это когда плохой месяц " +
                "не рушит всё, что строил.",
        ),
    ),
    ending(
        id = "ending_p2p",
        endingType = EndingType.PAYCHECK_TO_PAYCHECK,
        flavor = "😰",
        priority = 110,
        conditions = listOf(Condition.HasFlag("arc.final_check")),
        message = story(
            "Заказы есть, но деньги заканчиваются раньше, чем приходят следующие. " +
                "Один поток, нет буфера, стресс от каждого молчания клиента.",
            "Ты умеешь писать код. Но финансовая система пока не выстроена. " +
                "Следующий год — это исправить.",
        ),
    ),
)
