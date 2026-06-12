package kz.fearsom.financiallifev2.scenarios.arcs

import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.CAPITAL
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.DEBT
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.KNOWLEDGE
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.STRESS
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GT
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GTE
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.LTE
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.ScheduledEvent
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.scenarios.ScamEventLibrary
import kz.fearsom.financiallifev2.scenarios.cond
import kz.fearsom.financiallifev2.scenarios.event
import kz.fearsom.financiallifev2.scenarios.option
import kz.fearsom.financiallifev2.scenarios.story

data class StoryBalance(
    val stabilityCapital: Long,
    val freedomCapital: Long,
    val wealthCapital: Long,
    val debtCrisisDebt: Long,
    val debtCrisisCapital: Long,
    val investmentTicket: Long
)

fun marat90sStoryArc(): EventArc = EventArc { map ->
    map["intro"] = event(
        id = "intro",
        flavor = "📼",
        tags = setOf("family", "career"),
        message = story(
            "Алматы, октябрь 1993. Марат Сатыбалдин чинит магнитофоны на барахолке, по вечерам перепаивает платы в общаге и копит на первый нормальный товар.",
            "Отец принес домой бумажку: кооператив обещает «удвоить вклад до Нового года». Мама верит, потому что зарплату снова задержали. Младшая сестра хочет поступать в университет, а дома уже считают, что учебу придется отложить.",
            "У Марата есть {capital}, доход {income}/мес и очень мало времени до введения тенге."
        ),
        options = listOf(
            option("marat_stop_family", "Сесть с родителями и разобрать бумажку", "🛑", "marat_family_pyramid",
                Effect(knowledgeDelta = 8, stressDelta = 8, setFlags = setOf("family.warned"))),
            option("marat_bazaar_first", "Взять место на барахолке под кассеты", "📦", "marat_bazaar_counter",
                Effect(capitalDelta = -6_000_000L, incomeDelta = 2_000_000L, stressDelta = 8, riskDelta = 10, setFlags = setOf("business.started"))),
            option("marat_keep_cash", "Не спорить и держать деньги в наличных", "💵", MONTHLY_TICK,
                Effect(stressDelta = -2, knowledgeDelta = 2, scheduleEvent = ScheduledEvent("marat_family_pyramid", 1)))
        )
    )

    map["marat_family_pyramid"] = event(
        id = "marat_family_pyramid",
        flavor = "🧾",
        tags = setOf("scam", "scam.pyramid", "family"),
        schemeExplanation = story(
            "Пирамида платит ранним вкладчикам деньгами новых участников. Реальной прибыли нет: когда приток людей замедляется, выплаты останавливаются.",
            "Красные флаги: доходность «без риска», давление срочностью, нет понятного бизнеса, договор заменен распиской или рекламной бумажкой."
        ),
        message = story(
            "Отец злится: «Все соседи уже внесли, ты один умный?» На кухне пахнет жареной картошкой, радио шумит, а бумажка лежит между тарелками как приговор.",
            "Марат видит главное: там нет товара, нет лицензии, нет понятной выручки. Только обещание привести еще людей."
        ),
        options = listOf(
            option("marat_explain_pyramid", "Объяснить жестко, но без унижения", "📚", MONTHLY_TICK,
                Effect(knowledgeDelta = 18, stressDelta = 6, setFlags = setOf("learned.scam.pyramid", "family.protected"), scheduleEvent = ScheduledEvent("marat_bazaar_counter", 1))),
            option("marat_pay_for_peace", "Дать семье немного и принять риск", "🤲", MONTHLY_TICK,
                Effect(capitalDelta = -4_000_000L, stressDelta = 12, riskDelta = 12, scheduleEvent = ScheduledEvent("scam_pyramid_collapse", 2))),
            option("marat_walk_away", "Не лезть, пусть решают сами", "🧊", MONTHLY_TICK,
                Effect(stressDelta = -4, riskDelta = 8, scheduleEvent = ScheduledEvent("marat_bazaar_counter", 1)))
        )
    )

    map["marat_bazaar_counter"] = event(
        id = "marat_bazaar_counter",
        flavor = "🏪",
        tags = setOf("career", "investment"),
        message = story(
            "На барахолке освобождается половина контейнера. Можно продавать кассеты, батарейки и китайские часы. Хозяин просит оплату вперед и предлагает вести все «по-человечески», без лишних бумаг.",
            "Рядом бар, где перекупы вечером решают, кому завтра дадут товар под реализацию."
        ),
        options = listOf(
            option("marat_contract_notebook", "Записать доли, товар и долги в тетрадь при свидетелях", "📓", MONTHLY_TICK,
                Effect(capitalDelta = -5_000_000L, incomeDelta = 2_500_000L, knowledgeDelta = 12, stressDelta = 6, setFlags = setOf("business.documented", "business.started"), scheduleEvent = ScheduledEvent("marat_supplier_prepay", 2))),
            option("marat_cash_partner", "Довериться партнеру и дать наличные", "🤝", MONTHLY_TICK,
                Effect(capitalDelta = -8_000_000L, incomeDelta = 3_000_000L, riskDelta = 18, stressDelta = 8, setFlags = setOf("business.started"), scheduleEvent = ScheduledEvent("marat_supplier_prepay", 2))),
            option("marat_repair_focus", "Остаться на ремонте техники", "🔧", MONTHLY_TICK,
                Effect(incomeDelta = 1_000_000L, knowledgeDelta = 6, stressDelta = -4, scheduleEvent = ScheduledEvent("marat_supplier_prepay", 3)))
        )
    )

    map["marat_supplier_prepay"] = event(
        id = "marat_supplier_prepay",
        flavor = "🚚",
        tags = setOf("scam", "scam.middleman", "career"),
        schemeExplanation = story(
            "Предоплата без проверяемого поставщика превращает обычную торговлю в схему посредника: деньги уходят вперед, а контроль над товаром остается у незнакомого человека.",
            "Защита в 90-х: маленькая тестовая партия, расписка с паспортными данными, свидетели, проверка склада лично, оплата частями после получения товара."
        ),
        message = story(
            "На рынке появляется «челнок» из Турции. Обещает привезти джинсы и магнитолы дешевле всех, но просит всю сумму до пятницы. Говорит уверенно, показывает чужие накладные и торопит: «Не внесешь сейчас, место уйдет другому».",
            "Пара продавцов уже скинулась. Один шепчет Марату: «Надо брать, это шанс»."
        ),
        options = listOf(
            option("marat_full_prepay", "Внести полную предоплату", "💸", MONTHLY_TICK,
                Effect(capitalDelta = -12_000_000L, riskDelta = 22, stressDelta = 8, scheduleEvent = ScheduledEvent("scam_middleman_result", 2))),
            option("marat_test_batch", "Дать деньги только за тестовую коробку", "📦", MONTHLY_TICK,
                Effect(capitalDelta = -2_000_000L, knowledgeDelta = 12, stressDelta = 4, setFlags = setOf("learned.scam.middleman"), scheduleEvent = ScheduledEvent("marat_wedding", 2))),
            option("marat_verify_supplier", "Съездить на склад и проверить документы", "🔍", "scam_middleman_contract_refused",
                Effect(knowledgeDelta = 12, stressDelta = 2, scheduleEvent = ScheduledEvent("marat_wedding", 2)))
        )
    )

    map["marat_wedding"] = event(
        id = "marat_wedding",
        flavor = "💒",
        tags = setOf("family"),
        message = story(
            "Весной родственники заводят разговор о свадьбе. Марат встречается с Ляззат уже год, семьи знакомы, но той может съесть весь оборот.",
            "Ляззат тихо говорит: «Мне не нужен показ. Мне нужно, чтобы мы не начали жизнь с долга»."
        ),
        options = listOf(
            option("marat_small_toi", "Сделать маленький той и сохранить оборот", "🍽️", MONTHLY_TICK,
                Effect(capitalDelta = -4_000_000L, expensesDelta = 600_000L, stressDelta = -8, knowledgeDelta = 8, setFlags = setOf("married"), scheduleEvent = ScheduledEvent("final_review", 5))),
            option("marat_big_toi_debt", "Взять долг ради большого тоя", "🎉", MONTHLY_TICK,
                Effect(capitalDelta = -2_000_000L, debtDelta = 12_000_000L, debtPaymentDelta = 1_000_000L, stressDelta = 18, setFlags = setOf("married"), scheduleEvent = ScheduledEvent("final_review", 5))),
            option("marat_delay_wedding", "Отложить и честно объяснить семьям", "🕰️", MONTHLY_TICK,
                Effect(knowledgeDelta = 6, stressDelta = 6, scheduleEvent = ScheduledEvent("final_review", 5)))
        )
    )
}

fun ruslan2005StoryArc(): EventArc = EventArc { map ->
    map["intro"] = event(
        id = "intro",
        flavor = "🏗️",
        tags = setOf("career", "family"),
        message = story(
            "Астана, 2005. Руслан Темирбаев работает кредитным специалистом в банке. В городе кранов больше, чем деревьев, ипотеку обсуждают даже в маршрутке.",
            "Он встречается с Айгуль, помогает младшему брату с университетом и каждый день видит, как люди подписывают договоры, которые сами не читали.",
            "Старт: {capital}, доход {income}/мес, расходы {expenses}/мес."
        ),
        options = listOf(
            option("ruslan_build_reserve", "Сначала собрать резерв и не лезть в чужой хайп", "🛡️", "ruslan_bank_offer",
                Effect(knowledgeDelta = 8, stressDelta = -4, setFlags = setOf("reserve.focus"))),
            option("ruslan_join_sales", "Взять агрессивный план продаж в банке", "📈", "ruslan_bank_offer",
                Effect(incomeDelta = 45_000L, stressDelta = 12, riskDelta = 8)),
            option("ruslan_help_brother", "Оплатить брату первый семестр", "🎓", "ruslan_bank_offer",
                Effect(capitalDelta = -120_000L, expensesDelta = 20_000L, stressDelta = 6, knowledgeDelta = 3))
        )
    )

    map["ruslan_bank_offer"] = event(
        id = "ruslan_bank_offer",
        flavor = "🏦",
        tags = setOf("career"),
        message = story(
            "Начальник просит Руслана продавать клиентам кредитные карты вместе с ипотекой. Формально это не обязательно, но премия зависит от «комплекта».",
            "Вечером Айгуль зовет в кино, а Руслан вместо фильма сидит над таблицей: премия хорошая, но людям потом платить проценты."
        ),
        options = listOf(
            option("ruslan_sell_clean", "Продавать только то, что клиент понял", "📋", MONTHLY_TICK,
                Effect(incomeDelta = 15_000L, knowledgeDelta = 12, stressDelta = -2, scheduleEvent = ScheduledEvent("ruslan_presale_flat", 2))),
            option("ruslan_push_cards", "Гнать план и молчать о переплате", "💳", MONTHLY_TICK,
                Effect(incomeDelta = 70_000L, stressDelta = 12, riskDelta = 12, scheduleEvent = ScheduledEvent("ruslan_presale_flat", 2))),
            option("ruslan_move_credit_risk", "Перейти в риск-отдел и учиться договорам", "🔍", MONTHLY_TICK,
                Effect(incomeDelta = 25_000L, knowledgeDelta = 18, stressDelta = 4, scheduleEvent = ScheduledEvent("ruslan_presale_flat", 2)))
        )
    )

    map["ruslan_presale_flat"] = event(
        id = "ruslan_presale_flat",
        flavor = "🏢",
        tags = setOf("scam", "scam.presale", "mortgage"),
        schemeExplanation = story(
            "Опасная схема долевого строительства: застройщик продает будущую квартиру до полной готовности, но деньги не защищены эскроу и не привязаны к этапам стройки.",
            "Проверки: земля, разрешение на строительство, история застройщика, отдельный счет, график работ, штрафы за задержку, запрет платить наличными «в обход кассы»."
        ),
        message = story(
            "Коллега приносит буклет: «Квартира на котловане, минус 25% от рынка. Нужно внести бронь до понедельника». Офис застройщика красивый, макет дома блестит, менеджер угощает кофе.",
            "Но в договоре Руслан замечает странное: деньги идут не застройщику, а ТОО-посреднику, а сроки сдачи написаны расплывчато."
        ),
        options = listOf(
            option("ruslan_pay_booking", "Внести бронь, пока дешево", "🔑", MONTHLY_TICK,
                Effect(capitalDelta = -350_000L, riskDelta = 18, stressDelta = 10, scheduleEvent = ScheduledEvent("scam_presale_delay", 3))),
            option("ruslan_check_builder", "Проверить землю, разрешения и судебные дела", "🧾", "scam_presale_checked",
                Effect(knowledgeDelta = 15, stressDelta = 2, scheduleEvent = ScheduledEvent("ruslan_wedding_credit", 2))),
            option("ruslan_skip_presale", "Не покупать то, что нельзя проверить", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 10, stressDelta = -4, setFlags = setOf("learned.scam.presale"), scheduleEvent = ScheduledEvent("ruslan_wedding_credit", 2)))
        )
    )

    map["ruslan_wedding_credit"] = event(
        id = "ruslan_wedding_credit",
        flavor = "💍",
        tags = setOf("family"),
        message = story(
            "Родители Айгуль хотят свадьбу в большом ресторане. У Руслана на руках калькулятор: банкет, платье, подарки родственникам, фото, музыка. Банк легко даст потребкредит.",
            "Айгуль не против праздника, но прямо спрашивает: «Мы покупаем память или чужое мнение?»"
        ),
        options = listOf(
            option("ruslan_small_wedding", "Сделать теплую свадьбу без кредита", "🍽️", MONTHLY_TICK,
                Effect(capitalDelta = -250_000L, expensesDelta = 45_000L, stressDelta = -10, knowledgeDelta = 8, setFlags = setOf("married"), scheduleEvent = ScheduledEvent("final_review", 6))),
            option("ruslan_credit_wedding", "Взять кредит и не спорить с родней", "🎉", MONTHLY_TICK,
                Effect(capitalDelta = 400_000L, debtDelta = 900_000L, debtPaymentDelta = 35_000L, expensesDelta = 55_000L, stressDelta = 16, setFlags = setOf("married"), scheduleEvent = ScheduledEvent("final_review", 6))),
            option("ruslan_delay_wedding", "Отложить свадьбу на год и накопить", "📅", MONTHLY_TICK,
                Effect(knowledgeDelta = 8, stressDelta = 8, scheduleEvent = ScheduledEvent("final_review", 6)))
        )
    )
}

fun zhanar2015StoryArc(): EventArc = EventArc { map ->
    map["intro"] = event(
        id = "intro",
        flavor = "📱",
        tags = setOf("career", "family"),
        message = story(
            "Караганда, 2015. Жанар Сейтова преподает английский в школе и ведет вечерние занятия для подростков. У нее сын идет в первый класс, маме нужны лекарства, а WhatsApp-чат родителей уже спорит о форме и взносах.",
            "Летом тенге держится нервно. Все спрашивают: хранить деньги в долларах, покупать технику или просто не трогать накопления?",
            "На старте у Жанар {capital}, доход {income}/мес и расходов {expenses}/мес."
        ),
        options = listOf(
            option("zhanar_course_plan", "Запустить мини-группу по английскому", "📚", "zhanar_course_launch",
                Effect(knowledgeDelta = 8, stressDelta = 4)),
            option("zhanar_family_first", "Сначала закрыть лекарства и школу", "👪", "zhanar_school_choice",
                Effect(capitalDelta = -120_000L, expensesDelta = 25_000L, stressDelta = 6, knowledgeDelta = 3)),
            option("zhanar_currency_plan", "Разложить деньги: тенге, доллары, резерв", "💵", MONTHLY_TICK,
                Effect(knowledgeDelta = 12, stressDelta = -3, scheduleEvent = ScheduledEvent("zhanar_forex_relative", 2)))
        )
    )

    map["zhanar_course_launch"] = event(
        id = "zhanar_course_launch",
        flavor = "🎓",
        tags = setOf("career"),
        message = story(
            "У Жанар есть идея: не просто репетиторство, а маленький курс с домашками в WhatsApp и пробным тестом каждую пятницу. Директор школы косится: «Только чтобы без рекламы в классе».",
            "Сын просит планшет для мультиков, а ей нужен ноутбук для занятий."
        ),
        options = listOf(
            option("zhanar_buy_laptop", "Купить ноутбук и вести группы вечером", "💻", MONTHLY_TICK,
                Effect(capitalDelta = -180_000L, incomeDelta = 90_000L, stressDelta = 14, knowledgeDelta = 14, setFlags = setOf("business.started"), scheduleEvent = ScheduledEvent("zhanar_forex_relative", 2))),
            option("zhanar_start_small", "Начать с тетрадей и старого телефона", "✏️", MONTHLY_TICK,
                Effect(incomeDelta = 35_000L, stressDelta = 6, knowledgeDelta = 8, scheduleEvent = ScheduledEvent("zhanar_forex_relative", 2))),
            option("zhanar_postpone_course", "Не распыляться до сентября", "🕰️", MONTHLY_TICK,
                Effect(stressDelta = -6, knowledgeDelta = 4, scheduleEvent = ScheduledEvent("zhanar_forex_relative", 2)))
        )
    )

    map["zhanar_forex_relative"] = event(
        id = "zhanar_forex_relative",
        flavor = "📊",
        tags = setOf("scam", "scam.forex", "family"),
        schemeExplanation = story(
            "Форекс-кухня имитирует брокера: клиент видит красивые графики и «прибыль» в личном кабинете, но сделки могут не выводиться на реальный рынок.",
            "Ключевые проверки: лицензия регулятора, юрлицо, где хранятся деньги клиента, комиссия и спред, возможность вывести маленькую сумму без новых платежей."
        ),
        message = story(
            "Двоюродный брат зовет Жанар в интернет-кафе. На экране платформа с графиками: «На девальвации можно заработать. У меня наставник из Алматы, вход всего 100 тысяч».",
            "Он не выглядит мошенником. Он сам верит. Именно поэтому разговор неприятнее."
        ),
        options = listOf(
            option("zhanar_put_forex", "Вложить 100 тысяч и попробовать", "🚀", MONTHLY_TICK,
                Effect(capitalDelta = -100_000L, riskDelta = 18, stressDelta = 8, scheduleEvent = ScheduledEvent("scam_forex_withdrawal", 2))),
            option("zhanar_check_license", "Попросить лицензию и тестовый вывод", "🔍", "scam_forex_checked",
                Effect(knowledgeDelta = 16, stressDelta = 2, scheduleEvent = ScheduledEvent("zhanar_school_choice", 2))),
            option("zhanar_refuse_forex", "Отказаться и объяснить брату спокойно", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 12, stressDelta = -2, setFlags = setOf("learned.scam.forex"), scheduleEvent = ScheduledEvent("zhanar_school_choice", 2)))
        )
    )

    map["zhanar_school_choice"] = event(
        id = "zhanar_school_choice",
        flavor = "🏫",
        tags = setOf("family"),
        message = story(
            "Перед первым сентября родительский чат кипит: частная подготовка, форма подороже, английский лагерь, подарок учителю. Жанар видит, как тревога родителей превращается в соревнование кошельков.",
            "Сыну важнее спокойная мама, чем самый дорогой рюкзак."
        ),
        options = listOf(
            option("zhanar_expensive_school", "Взять дорогую подготовку и лагерь", "🎒", MONTHLY_TICK,
                Effect(expensesDelta = 70_000L, stressDelta = 12, knowledgeDelta = 3, scheduleEvent = ScheduledEvent("final_review", 6))),
            option("zhanar_balanced_school", "Школа плюс домашний план занятий", "📖", MONTHLY_TICK,
                Effect(expensesDelta = 25_000L, knowledgeDelta = 10, stressDelta = -4, scheduleEvent = ScheduledEvent("final_review", 6))),
            option("zhanar_parent_chat_boundary", "Поставить границы родительскому чату", "📵", MONTHLY_TICK,
                Effect(stressDelta = -12, knowledgeDelta = 6, scheduleEvent = ScheduledEvent("final_review", 6)))
        )
    )
}

fun amir2024StoryArc(): EventArc = EventArc { map ->
    map["intro"] = event(
        id = "intro",
        flavor = "🚀",
        tags = setOf("career", "family"),
        message = story(
            "Алматы, 2024. Амир Нурланов ведет кабинет продавца на маркетплейсе: днем аналитика, вечером созвоны с поставщиками, ночью короткие катки с друзьями.",
            "Он снимает квартиру, помогает родителям и пытается понять, как жить не от зарплаты до зарплаты. В Telegram каждый день кто-то обещает «пассивный доход».",
            "Старт: {capital}, доход {income}/мес, долг {debt}."
        ),
        options = listOf(
            option("amir_marketplace_focus", "Разобрать бизнес-модель магазина", "📦", "amir_marketplace",
                Effect(knowledgeDelta = 10, stressDelta = 4)),
            option("amir_salary_focus", "Сначала закрыть долг и резерв", "🛡️", MONTHLY_TICK,
                Effect(debtDelta = -60_000L, capitalDelta = -60_000L, knowledgeDelta = 6, stressDelta = -2, scheduleEvent = ScheduledEvent("amir_dating_app", 2))),
            option("amir_side_product", "Собрать бота для продавцов по вечерам", "🤖", "amir_side_product",
                Effect(stressDelta = 12, riskDelta = 8, knowledgeDelta = 8))
        )
    )

    map["amir_marketplace"] = event(
        id = "amir_marketplace",
        flavor = "🛒",
        tags = setOf("scam", "scam.escrow", "career"),
        schemeExplanation = story(
            "Фейковый escrow строится на знакомом интерфейсе: мошенник дает ссылку на якобы безопасную оплату или доставку, но деньги уходят на поддельный сайт.",
            "Проверки: домен, платеж внутри официального приложения, номер заказа в личном кабинете, отсутствие просьб перейти в Telegram/WhatsApp для оплаты."
        ),
        message = story(
            "Новый поставщик предлагает партию наушников с хорошей маржой. Он присылает ссылку на «безопасную сделку маркетплейса» и давит: «У меня уже второй покупатель ждет».",
            "Ссылка выглядит почти как настоящая, только домен длиннее обычного. Амир замечает это уже перед оплатой."
        ),
        options = listOf(
            option("amir_pay_fake_escrow", "Оплатить через ссылку, пока партия не ушла", "💳", MONTHLY_TICK,
                Effect(capitalDelta = -180_000L, stressDelta = 14, riskDelta = 18, scheduleEvent = ScheduledEvent("scam_escrow_loss", 1))),
            option("amir_check_in_app", "Проверить сделку внутри официального кабинета", "🔍", "scam_escrow_checked",
                Effect(knowledgeDelta = 16, stressDelta = 2, scheduleEvent = ScheduledEvent("amir_dating_app", 2))),
            option("amir_supplier_contract", "Попросить договор и оплату после приемки", "📋", MONTHLY_TICK,
                Effect(knowledgeDelta = 12, stressDelta = -2, setFlags = setOf("learned.scam.escrow"), scheduleEvent = ScheduledEvent("amir_dating_app", 2)))
        )
    )

    map["amir_dating_app"] = event(
        id = "amir_dating_app",
        flavor = "💌",
        tags = setOf("scam", "scam.romance", "scam.crypto"),
        schemeExplanation = story(
            "Romance scam растит доверие через переписку, затем переводит разговор к инвестициям. Часто показывают фальшивую прибыль и блокируют вывод под видом налога или комиссии.",
            "Защита: не переводить деньги человеку из переписки, не ставить приложения по ссылке, проверять площадку отдельно, тестировать вывод до вложений."
        ),
        message = story(
            "В dating-приложении Амир знакомится с Милой. Она смешно спорит про сериалы, помнит его день рождения и через неделю рассказывает, что ее брат торгует на криптобирже.",
            "Она не просит денег прямо. Просто показывает скрин прибыли и говорит: «Можешь начать с маленькой суммы, я помогу»."
        ),
        options = listOf(
            option("amir_romance_invest", "Закинуть маленькую сумму и посмотреть", "📈", MONTHLY_TICK,
                Effect(capitalDelta = -120_000L, stressDelta = 6, riskDelta = 20, scheduleEvent = ScheduledEvent("scam_romance_freeze", 2))),
            option("amir_video_and_withdraw", "Попросить видеозвонок и тестовый вывод", "🧪", "scam_romance_caught",
                Effect(knowledgeDelta = 18, stressDelta = 4, scheduleEvent = ScheduledEvent("amir_side_product", 2))),
            option("amir_keep_dating_off_money", "Оставить свидания отдельно от инвестиций", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 12, stressDelta = -4, setFlags = setOf("learned.scam.romance"), scheduleEvent = ScheduledEvent("amir_side_product", 2)))
        )
    )

    map["amir_side_product"] = event(
        id = "amir_side_product",
        flavor = "🤖",
        tags = setOf("career", "investment"),
        message = story(
            "Бот для продавцов начинает работать: считает маржу с учетом комиссии, возвратов и рекламы. Два знакомых готовы платить подписку, но просят новые функции каждую ночь.",
            "Друзья зовут в клуб, родители просят помочь с ремонтом, а Амир впервые видит, что знания экономят больше денег, чем удачная ставка."
        ),
        options = listOf(
            option("amir_product_scale", "Взять двух платных клиентов и вести учет", "📊", MONTHLY_TICK,
                Effect(incomeDelta = 130_000L, stressDelta = 16, knowledgeDelta = 14, setFlags = setOf("business_scaled"), scheduleEvent = ScheduledEvent("final_review", 6))),
            option("amir_product_limit", "Оставить как спокойную подработку", "⚖️", MONTHLY_TICK,
                Effect(incomeDelta = 55_000L, stressDelta = 4, knowledgeDelta = 8, scheduleEvent = ScheduledEvent("final_review", 6))),
            option("amir_rest_and_debt", "Закрыть часть долга и не выгорать", "🧘", MONTHLY_TICK,
                Effect(capitalDelta = -80_000L, debtDelta = -80_000L, stressDelta = -12, knowledgeDelta = 6, scheduleEvent = ScheduledEvent("final_review", 6)))
        )
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  DANIYAR 2005 — механик в Шымкенте, кредитный бум 2005-2010
//
//  Темы арки:
//   1. «Серый импорт» — друг зовёт в наличную схему, без бумаг
//   2. Формализация бизнеса — клиенты просят договор, пора ли оформлять ИП?
//   3. Давление ауыла — отец просит денег на крышу и учёбу сестры
//   4. Долевое строительство — друг из Астаны привозит «квартиру мечты»
//
//  Все тексты читаются из i18n-карт (ru/kk/en) по ключам evt_daniyar_*_msg.
// ═════════════════════════════════════════════════════════════════════════════

fun daniyar2005StoryArc(): EventArc = EventArc { map ->

    // Use `intro` as the entry-point id so it lines up with the standard contract
    // (ScenarioGraphContentTest asserts every graph has an "intro" event).
    map["intro"] = event(
        id = "intro",
        flavor = "🔧",
        tags = setOf("family", "career", "scam"),
        message = Strings["evt_daniyar_intro_msg"],
        options = listOf(
            option("daniyar_bolat_listen", Strings["evt_daniyar_intro_opt_bolat_listen"], "👂", "daniyar_bolat_import",
                Effect(knowledgeDelta = 4, stressDelta = 4)),
            option("daniyar_refuse_chat", Strings["evt_daniyar_intro_opt_refuse_chat"], "🚫", "daniyar_garage_formalize",
                Effect(knowledgeDelta = 10, stressDelta = -4, setFlags = setOf("learned.scam.gray_import"))),
            option("daniyar_ask_for_time", Strings["evt_daniyar_intro_opt_ask_for_time"], "⏳", "daniyar_village_call",
                Effect(knowledgeDelta = 6, stressDelta = 2, scheduleEvent = ScheduledEvent("daniyar_bolat_import", 1)))
        )
    )

    map["daniyar_bolat_import"] = event(
        id = "daniyar_bolat_import",
        flavor = "🚗",
        tags = setOf("scam", "scam.gray_import", "career"),
        schemeExplanation = Strings["evt_daniyar_bolat_import_scheme_explanation"],
        message = Strings["evt_daniyar_bolat_import_msg"],
        options = listOf(
            option("daniyar_invest_full", Strings["evt_daniyar_bolat_import_opt_invest_full"], "💸", "daniyar_garage_formalize",
                Effect(capitalDelta = -500_000L, stressDelta = 12, riskDelta = 22,
                    scheduleEvent = ScheduledEvent("daniyar_gray_outcome_loss", 3))),
            option("daniyar_invest_half", Strings["evt_daniyar_bolat_import_opt_invest_half"], "🤝", "daniyar_garage_formalize",
                Effect(capitalDelta = -250_000L, stressDelta = 8, riskDelta = 14,
                    scheduleEvent = ScheduledEvent("daniyar_gray_outcome_loss", 3))),
            option("daniyar_refuse_gray", Strings["evt_daniyar_bolat_import_opt_refuse"], "🛡️", "daniyar_garage_formalize",
                Effect(knowledgeDelta = 14, stressDelta = -4, setFlags = setOf("learned.scam.gray_import"),
                    scheduleEvent = ScheduledEvent("daniyar_village_call", 1)))
        )
    )

    map["daniyar_gray_outcome_loss"] = event(
        id = "daniyar_gray_outcome_loss",
        flavor = "💀",
        tags = setOf("consequence", "scam.gray_import"),
        unique = true,
        message = "Через три месяца от Болата приходит короткое сообщение: на границе задержали вагон, документов нет, фамилии в обходном листе, уголовное дело. Данияр сидит на верстаке и смотрит на свой телефон, как на чужой предмет.\n\nВ гараж приходят те, кто \"тоже скидывался\", и молча смотрят. Деньги — минус. Доверие — минус. Здоровье — минус. И ни одного документа, по которому можно что-то вернуть.",
        options = listOf(
            option("daniyar_accept_loss", "Признать ошибку и не усугублять", "📚", "daniyar_village_call",
                Effect(knowledgeDelta = 22, stressDelta = 18, setFlags = setOf("learned.scam.gray_import", "lost_money_to_scam"),
                    scheduleEvent = ScheduledEvent("daniyar_village_call", 1)))
        )
    )

    map["daniyar_garage_formalize"] = event(
        id = "daniyar_garage_formalize",
        flavor = "🧾",
        tags = setOf("career", "investment"),
        message = Strings["evt_daniyar_garage_formalize_msg"],
        options = listOf(
            option("daniyar_register_ip", Strings["evt_daniyar_garage_formalize_opt_register_ip"], "📋", "daniyar_village_call",
                Effect(incomeDelta = -25_000L, expensesDelta = 8_000L, knowledgeDelta = 16, stressDelta = 4,
                    setFlags = setOf("business.documented"), scheduleEvent = ScheduledEvent("daniyar_village_call", 1))),
            option("daniyar_stay_cash", Strings["evt_daniyar_garage_formalize_opt_stay_cash"], "🌑", "daniyar_village_call",
                Effect(stressDelta = -2, riskDelta = 12, knowledgeDelta = 2, scheduleEvent = ScheduledEvent("daniyar_village_call", 1))),
            option("daniyar_half_formal", Strings["evt_daniyar_garage_formalize_opt_half_formal"], "⚖️", "daniyar_village_call",
                Effect(incomeDelta = -10_000L, expensesDelta = 4_000L, stressDelta = 6, riskDelta = 6, knowledgeDelta = 8,
                    scheduleEvent = ScheduledEvent("daniyar_village_call", 1)))
        )
    )

    map["daniyar_village_call"] = event(
        id = "daniyar_village_call",
        flavor = "📞",
        tags = setOf("family"),
        message = Strings["evt_daniyar_village_call_msg"],
        options = listOf(
            option("daniyar_send_big", Strings["evt_daniyar_village_call_opt_send_big"], "💸", "daniyar_presale_flat",
                Effect(capitalDelta = -350_000L, stressDelta = 14, riskDelta = 6,
                    setFlags = setOf("family.helped"), scheduleEvent = ScheduledEvent("daniyar_presale_flat", 1))),
            option("daniyar_send_partial", Strings["evt_daniyar_village_call_opt_send_partial"], "🤲", "daniyar_presale_flat",
                Effect(capitalDelta = -200_000L, stressDelta = 8,
                    setFlags = setOf("family.helped"), scheduleEvent = ScheduledEvent("daniyar_presale_flat", 1))),
            option("daniyar_send_plan", Strings["evt_daniyar_village_call_opt_send_plan"], "🧭", "daniyar_presale_flat",
                Effect(capitalDelta = -50_000L, knowledgeDelta = 12, stressDelta = 6,
                    setFlags = setOf("family.helped"), scheduleEvent = ScheduledEvent("daniyar_presale_flat", 1)))
        )
    )

    map["daniyar_presale_flat"] = event(
        id = "daniyar_presale_flat",
        flavor = "🏗️",
        tags = setOf("scam", "scam.presale", "mortgage"),
        schemeExplanation = Strings["evt_daniyar_presale_flat_scheme_explanation"],
        message = Strings["evt_daniyar_presale_flat_msg"],
        options = listOf(
            option("daniyar_pay_booking", Strings["evt_daniyar_presale_flat_opt_pay_booking"], "🔑", "daniyar_wedding_credit",
                Effect(capitalDelta = -400_000L, riskDelta = 22, stressDelta = 10,
                    scheduleEvent = ScheduledEvent("daniyar_presale_loss", 4))),
            option("daniyar_check_builder", Strings["evt_daniyar_presale_flat_opt_check_builder"], "🧾", "daniyar_wedding_credit",
                Effect(knowledgeDelta = 18, stressDelta = 2,
                    setFlags = setOf("learned.scam.presale"), scheduleEvent = ScheduledEvent("daniyar_wedding_credit", 1))),
            option("daniyar_skip_flat", Strings["evt_daniyar_presale_flat_opt_skip_flat"], "🛡️", "daniyar_wedding_credit",
                Effect(knowledgeDelta = 12, stressDelta = -4,
                    setFlags = setOf("learned.scam.presale"), scheduleEvent = ScheduledEvent("daniyar_wedding_credit", 1)))
        )
    )

    map["daniyar_presale_loss"] = event(
        id = "daniyar_presale_loss",
        flavor = "💀",
        tags = setOf("consequence", "scam.presale", "mortgage"),
        unique = true,
        message = "Стройка встала на этапе третьего этажа. На сайте застройщика — заглушка, офис закрыт, директор ТОО \"КазГрадИнвест\" оказался тем же человеком, что уже фигурирует в двух других замороженных проектах в Астане.\n\nСуд идёт, но юристы говорят честно: шансы вернуть хотя бы часть — низкие. Айгуль впервые за долгие месяцы молчит за ужином целую минуту. Потом говорит: \"Ладно, поехали дальше\". Данияр понимает, что \"поехали дальше\" — это её форма любви.",
        options = listOf(
            option("daniyar_accept_presale_loss", "Признать потерю и не усугублять долгом", "📚", "daniyar_wedding_credit",
                Effect(knowledgeDelta = 20, stressDelta = 18,
                    setFlags = setOf("learned.scam.presale", "lost_money_to_scam"),
                    scheduleEvent = ScheduledEvent("daniyar_wedding_credit", 1)))
        )
    )

    map["daniyar_wedding_credit"] = event(
        id = "daniyar_wedding_credit",
        flavor = "💍",
        tags = setOf("family"),
        message = Strings["evt_daniyar_wedding_credit_msg"],
        options = listOf(
            option("daniyar_give_full", Strings["evt_daniyar_wedding_credit_opt_give_full"], "🎁", "final_review",
                Effect(capitalDelta = -250_000L, stressDelta = 4, setFlags = setOf("family.gave_full"),
                    scheduleEvent = ScheduledEvent("final_review", 4))),
            option("daniyar_give_partial", Strings["evt_daniyar_wedding_credit_opt_give_partial"], "🤝", "final_review",
                Effect(capitalDelta = -120_000L, stressDelta = 6, setFlags = setOf("family.gave_partial"),
                    scheduleEvent = ScheduledEvent("final_review", 4))),
            option("daniyar_refuse_toi", Strings["evt_daniyar_wedding_credit_opt_refuse"], "🧊", "final_review",
                Effect(stressDelta = 12, knowledgeDelta = 6, scheduleEvent = ScheduledEvent("final_review", 4)))
        )
    )

    map["daniyar_normal_life"] = event(
        id = "daniyar_normal_life",
        flavor = "☕",
        poolWeight = 22,
        message = Strings["evt_daniyar_normal_life_msg"],
        options = listOf(
            option("daniyar_save_reserve", Strings["evt_daniyar_normal_life_opt_save"], "🐷", MONTHLY_TICK,
                Effect(capitalDelta = -20_000L, stressDelta = -4, knowledgeDelta = 2)),
            option("daniyar_study_term", Strings["evt_daniyar_normal_life_opt_study"], "📖", MONTHLY_TICK,
                Effect(knowledgeDelta = 7, stressDelta = -1)),
            option("daniyar_breathe", Strings["evt_daniyar_normal_life_opt_breathe"], "🍖", MONTHLY_TICK,
                Effect(capitalDelta = -18_000L, stressDelta = -10))
        )
    )
}

fun daniyar2005Conditionals(balance: StoryBalance): List<GameEvent> = listOf(
    event(
        id = "daniyar_burnout",
        priority = 30,
        flavor = "😮‍💨",
        cooldownMonths = 5,
        conditions = listOf(cond(STRESS, GTE, 78L)),
        tags = setOf("crisis", "family"),
        message = Strings["evt_daniyar_burnout_msg"],
        options = listOf(
            option("daniyar_burnout_rest", Strings["evt_daniyar_burnout_opt_rest"], "🛏️", MONTHLY_TICK,
                Effect(capitalDelta = -40_000L, incomeDelta = -15_000L, stressDelta = -28, knowledgeDelta = 3)),
            option("daniyar_burnout_push", Strings["evt_daniyar_burnout_opt_push"], "😤", MONTHLY_TICK,
                Effect(incomeDelta = 25_000L, stressDelta = 14, riskDelta = 6)),
            option("daniyar_burnout_talk", Strings["evt_daniyar_burnout_opt_talk"], "🫂", MONTHLY_TICK,
                Effect(stressDelta = -18, knowledgeDelta = 8, setFlags = setOf("family.opened_up")))
        )
    ),
    event(
        id = "daniyar_investment_unlock",
        priority = 20,
        flavor = "💡",
        unique = true,
        conditions = listOf(cond(KNOWLEDGE, GTE, 40L), cond(CAPITAL, GTE, balance.investmentTicket)),
        tags = setOf("investment"),
        message = Strings["evt_daniyar_investment_unlock_msg"],
        options = listOf(
            option("daniyar_open_deposit", Strings["evt_daniyar_investment_unlock_opt_open_deposit"], "🏦", MONTHLY_TICK,
                Effect(capitalDelta = -balance.investmentTicket, investmentsDelta = balance.investmentTicket, knowledgeDelta = 8, stressDelta = -4)),
            option("daniyar_skip_deposit", Strings["evt_daniyar_investment_unlock_opt_skip"], "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 4, stressDelta = -2))
        )
    )
)

/**
 * Daniyar-specific endings, keyed by the standard IDs (`ending_wealth`, etc.)
 * so the `storyConditionals` triggers route to them via [ScenarioGraph.findEvent].
 * Build order in [DaniyarScenarioGraph] puts this arc AFTER [endingsArc], so
 * these entries overwrite the standard ones and bring proper kk/ru/en text.
 */
fun daniyar2005Endings(): EventArc = EventArc { map ->
    // These IDs MUST match the `next` field of `ending_*_trigger` events in
    // `storyConditionals` (ending_wealth_trigger -> "ending_wealth", etc.).
    map["ending_wealth"] = event(
        id = "ending_wealth",
        isEnding = true,
        flavor = "🏆",
        message = Strings["evt_daniyar_ending_wealth_msg"],
        options = emptyList()
    )
    map["ending_stability"] = event(
        id = "ending_stability",
        isEnding = true,
        flavor = "💼",
        message = Strings["evt_daniyar_ending_stable_msg"],
        options = emptyList()
    )
    map["ending_freedom"] = event(
        id = "ending_freedom",
        isEnding = true,
        flavor = "🌅",
        message = Strings["evt_daniyar_ending_freedom_msg"],
        options = emptyList()
    )
    map["ending_regular"] = event(
        id = "ending_regular",
        isEnding = true,
        flavor = "😐",
        message = Strings["evt_daniyar_ending_paycheck_msg"],
        options = emptyList()
    )
    map["ending_bankruptcy"] = event(
        id = "ending_bankruptcy",
        isEnding = true,
        flavor = "💀",
        message = Strings["evt_daniyar_ending_broke_msg"],
        options = emptyList()
    )
}

fun regularLifeArc(eraId: String): EventArc = EventArc { map ->
    val tv = when (eraId) {
        "kz_90s" -> "видеосалон и кассеты"
        "kz_2005" -> "DVD, Counter-Strike и кабельное ТВ"
        "kz_2015" -> "сериал, PlayStation и Instagram"
        else -> "стриминг, игры и короткие видео"
    }
    val dating = when (eraId) {
        "kz_90s" -> "знакомые зовут на день рождения, где будет человек, который тебе нравится"
        "kz_2005" -> "Айгуль предлагает сходить в кино после работы"
        "kz_2015" -> "подруга зовет на кофе без детей и родительских чатов"
        else -> "матч в dating-приложении зовет на кофе без разговоров про инвестиции"
    }

    map["normal_life"] = event(
        id = "normal_life",
        flavor = "☕",
        poolWeight = 24,
        message = story(
            "Обычный месяц без большой драмы. Деньги пришли, часть уже ушла на еду, транспорт и обязательства.",
            "Такие месяцы и решают игру: не один великий ход, а повторяемая привычка."
        ),
        options = listOf(
            option("save_cash", "Отложить часть денег в резерв", "🐷", MONTHLY_TICK,
                Effect(stressDelta = -4, knowledgeDelta = 2)),
            option("study_finance", "Разобрать один финансовый термин", "📖", MONTHLY_TICK,
                Effect(knowledgeDelta = 7, stressDelta = -1)),
            option("celebrate_small", "Позволить себе маленький праздник", "🍰", MONTHLY_TICK,
                Effect(capitalDelta = -25_000L, stressDelta = -8)),
            option("do_nothing", "Просто дожить до следующего месяца", "😐", MONTHLY_TICK,
                Effect(stressDelta = 2))
        )
    )

    map["new_year_event"] = event(
        id = "new_year_event",
        flavor = "🎄",
        tags = setOf("family"),
        unique = true,
        message = "Новый год. Родные спорят о подарках, столе и том, кто сколько должен добавить. Праздник легко превращается в незаметный кредит самому себе.",
        options = listOf(
            option("new_year_budget", "Заранее назвать сумму и не выходить за нее", "🧾", MONTHLY_TICK,
                Effect(capitalDelta = -35_000L, knowledgeDelta = 7, stressDelta = -4)),
            option("new_year_big", "Сделать праздник как у всех", "🎁", MONTHLY_TICK,
                Effect(capitalDelta = -120_000L, stressDelta = -8, riskDelta = 5)),
            option("new_year_home", "Домашний вечер без показухи", "🏠", MONTHLY_TICK,
                Effect(capitalDelta = -15_000L, stressDelta = -6))
        )
    )

    map["birthday_event"] = event(
        id = "birthday_event",
        flavor = "🎂",
        tags = setOf("family"),
        unique = true,
        message = "День рождения. Хочется отметить нормально, но в голове уже список расходов: гости, место, такси, подарок самому себе.",
        options = listOf(
            option("birthday_budget", "Отметить в рамках бюджета", "🍰", MONTHLY_TICK,
                Effect(capitalDelta = -30_000L, stressDelta = -10, knowledgeDelta = 3)),
            option("birthday_status", "Снять место подороже ради статуса", "🥂", MONTHLY_TICK,
                Effect(capitalDelta = -150_000L, stressDelta = -8, riskDelta = 8)),
            option("birthday_skip", "Перенести праздник и закрыть обязательства", "📌", MONTHLY_TICK,
                Effect(debtDelta = -30_000L, capitalDelta = -30_000L, knowledgeDelta = 5))
        )
    )

    map["dating_event"] = event(
        id = "dating_event",
        flavor = "💬",
        tags = setOf("family"),
        unique = true,
        conditions = listOf(Condition.NotFlag("relationship")),
        message = "Личная жизнь напоминает, что деньги не единственная метрика. $dating.",
        options = listOf(
            option("dating_go", "Сходить и не изображать богаче, чем есть", "☕", MONTHLY_TICK,
                Effect(capitalDelta = -20_000L, stressDelta = -8, knowledgeDelta = 3, setFlags = setOf("relationship"))),
            option("dating_impress", "Произвести впечатление дорогим вечером", "✨", MONTHLY_TICK,
                Effect(capitalDelta = -90_000L, stressDelta = -10, riskDelta = 7, setFlags = setOf("relationship"))),
            option("dating_skip", "Не сейчас", "🕰️", MONTHLY_TICK,
                Effect(stressDelta = 3))
        )
    )

    map["breakup_event"] = event(
        id = "breakup_event",
        flavor = "💔",
        tags = setOf("family"),
        unique = true,
        conditions = listOf(Condition.HasFlag("relationship"), Condition.NotFlag("married")),
        message = "Разговор заканчивается честно: ожидания разные, денег и внимания постоянно не хватает. Расставание бьет не только по настроению, но и по решениям.",
        options = listOf(
            option("breakup_spend", "Заглушить вечер покупками и баром", "🍸", MONTHLY_TICK,
                Effect(capitalDelta = -80_000L, stressDelta = -6, riskDelta = 8, clearFlags = setOf("relationship"))),
            option("breakup_walk", "Пройтись, выспаться и не трогать накопления", "🚶", MONTHLY_TICK,
                Effect(stressDelta = -4, knowledgeDelta = 5, clearFlags = setOf("relationship"))),
            option("breakup_work", "Уйти в работу на месяц", "💼", MONTHLY_TICK,
                Effect(incomeDelta = 20_000L, stressDelta = 10, clearFlags = setOf("relationship")))
        )
    )

    map["wedding_event"] = event(
        id = "wedding_event",
        flavor = "💍",
        tags = setOf("family"),
        unique = true,
        conditions = listOf(Condition.HasFlag("relationship"), cond(CAPITAL, GTE, 250_000L)),
        message = "Отношения стали серьезными. Семьи уже обсуждают свадьбу, а бюджет почему-то растет быстрее списка гостей.",
        options = listOf(
            option("wedding_small", "Небольшая свадьба без кредита", "🍽️", MONTHLY_TICK,
                Effect(capitalDelta = -220_000L, expensesDelta = 45_000L, stressDelta = -8, knowledgeDelta = 7, setFlags = setOf("married"))),
            option("wedding_debt", "Взять кредит на большой той", "🎉", MONTHLY_TICK,
                Effect(debtDelta = 800_000L, debtPaymentDelta = 35_000L, capitalDelta = -120_000L, stressDelta = 16, setFlags = setOf("married"))),
            option("wedding_delay", "Отложить и накопить", "📅", MONTHLY_TICK,
                Effect(knowledgeDelta = 6, stressDelta = 6))
        )
    )

    map["child_birth_event"] = event(
        id = "child_birth_event",
        flavor = "👶",
        tags = setOf("family"),
        unique = true,
        conditions = listOf(Condition.HasFlag("married")),
        message = "В семье появляется ребенок. Радость настоящая, но вместе с ней приходят стабильные расходы: медицина, одежда, транспорт, помощь дома.",
        options = listOf(
            option("child_budget", "Пересобрать бюджет под ребенка", "🧾", MONTHLY_TICK,
                Effect(expensesDelta = 65_000L, knowledgeDelta = 10, stressDelta = 4, setFlags = setOf("has_child"))),
            option("child_ignore_budget", "Покупать по ситуации", "🧸", MONTHLY_TICK,
                Effect(expensesDelta = 95_000L, stressDelta = 12, setFlags = setOf("has_child"))),
            option("child_family_help", "Попросить помощь семьи и не геройствовать", "👪", MONTHLY_TICK,
                Effect(expensesDelta = 45_000L, stressDelta = -4, setFlags = setOf("has_child")))
        )
    )

    map["school_event"] = event(
        id = "school_event",
        flavor = "🏫",
        tags = setOf("family"),
        conditions = listOf(Condition.HasFlag("has_child")),
        cooldownMonths = 10,
        message = "Вопрос школы появляется раньше, чем хотелось: кружки, форма, подготовка, взносы. Образование легко продают через страх отстать.",
        options = listOf(
            option("school_plan", "Выбрать школу по качеству и бюджету", "📚", MONTHLY_TICK,
                Effect(expensesDelta = 25_000L, knowledgeDelta = 8, stressDelta = -2)),
            option("school_prestige", "Платить за престиж", "🎓", MONTHLY_TICK,
                Effect(expensesDelta = 90_000L, stressDelta = 10, riskDelta = 4)),
            option("school_home_support", "Усилить дом и бесплатные ресурсы", "🏠", MONTHLY_TICK,
                Effect(knowledgeDelta = 6, stressDelta = 3))
        )
    )

    map["university_event"] = event(
        id = "university_event",
        flavor = "🎓",
        tags = setOf("family"),
        unique = true,
        message = "Родственник поступает в университет. Все смотрят на тебя: помочь сейчас или объяснить, что образование тоже должно иметь бюджет.",
        options = listOf(
            option("university_help_plan", "Помочь частью и составить план расходов", "📋", MONTHLY_TICK,
                Effect(capitalDelta = -90_000L, knowledgeDelta = 8, stressDelta = 2)),
            option("university_pay_all", "Оплатить все самому", "🤲", MONTHLY_TICK,
                Effect(capitalDelta = -300_000L, expensesDelta = 30_000L, stressDelta = 10)),
            option("university_scholarship", "Искать гранты и подработку", "🔍", MONTHLY_TICK,
                Effect(knowledgeDelta = 10, stressDelta = 4))
        )
    )

    map["bar_club_event"] = event(
        id = "bar_club_event",
        flavor = "🍸",
        tags = setOf("family"),
        cooldownMonths = 5,
        message = "Друзья зовут в бар/клуб. Ничего плохого в отдыхе нет, пока отдых не маскирует усталость и не съедает платежи.",
        options = listOf(
            option("bar_budget", "Пойти с лимитом", "💳", MONTHLY_TICK,
                Effect(capitalDelta = -25_000L, stressDelta = -8, knowledgeDelta = 3)),
            option("bar_all_in", "Гулять без счета", "🥂", MONTHLY_TICK,
                Effect(capitalDelta = -110_000L, stressDelta = -10, riskDelta = 8)),
            option("bar_skip", "Пропустить и выспаться", "🛏️", MONTHLY_TICK,
                Effect(stressDelta = -6))
        )
    )

    map["gaming_tv_event"] = event(
        id = "gaming_tv_event",
        flavor = "🎮",
        tags = setOf("family"),
        cooldownMonths = 4,
        message = "Вечер тянет в $tv. Отдых нужен, но еще один импульсный платеж легко становится подпиской, техникой или ставкой «для интереса».",
        options = listOf(
            option("gaming_limit", "Поставить лимит времени и денег", "⏱️", MONTHLY_TICK,
                Effect(stressDelta = -6, knowledgeDelta = 4)),
            option("gaming_buy", "Купить обновку/подписку сразу", "🛒", MONTHLY_TICK,
                Effect(capitalDelta = -75_000L, stressDelta = -7, riskDelta = 5)),
            option("gaming_replace_study", "Заменить час отдыха разбором бюджета", "📊", MONTHLY_TICK,
                Effect(knowledgeDelta = 8, stressDelta = 2))
        )
    )

    map["final_review"] = event(
        id = "final_review",
        flavor = "🧭",
        tags = setOf("reflection"),
        message = story(
            "Прошло достаточно месяцев, чтобы случайность стала системой. Сейчас видно не только сколько денег осталось, но и какие правила появились.",
            "Капитал: {capital}. Долг: {debt}. Знания: {knowledge}/100. Стресс: {stress}/100.",
            "Можно подвести итог или дать себе еще один месяц."
        ),
        options = listOf(
            option("final_check", "Подвести итог честно", "🧾", MONTHLY_TICK,
                Effect(setFlags = setOf("arc.final_check"))),
            option("final_one_more_month", "Еще месяц без финала", "📆", MONTHLY_TICK,
                Effect(stressDelta = 1, scheduleEvent = ScheduledEvent("final_review", 1))),
            option("final_study_before_check", "Перед итогом разобрать ошибки", "📚", MONTHLY_TICK,
                Effect(knowledgeDelta = 10, stressDelta = 3, setFlags = setOf("arc.final_check")))
        )
    )
}

fun endingsArc(): EventArc = EventArc { map ->
    map["ending_wealth"] = event(
        id = "ending_wealth",
        flavor = "🏆",
        isEnding = true,
        endingType = EndingType.WEALTH,
        message = story(
            "Победа: ты не просто заработал, а построил систему.",
            "Требования этого финала: высокий капитал, масштабируемый доход и достаточная финансовая грамотность, чтобы не путать рост с азартом."
        ),
        options = emptyList()
    )
    map["ending_freedom"] = event(
        id = "ending_freedom",
        flavor = "🌅",
        isEnding = true,
        endingType = EndingType.FINANCIAL_FREEDOM,
        message = story(
            "Финансовая свобода: расходы под контролем, долг не душит, решения принимаются не из паники.",
            "Требования: капитал выше цели эпохи, знания не ниже 55 и стресс не в красной зоне."
        ),
        options = emptyList()
    )
    map["ending_stability"] = event(
        id = "ending_stability",
        flavor = "💼",
        isEnding = true,
        endingType = EndingType.FINANCIAL_STABILITY,
        message = story(
            "Стабильный финал: без богатства на обложке, зато с резервом, понятными платежами и привычкой проверять схемы до оплаты.",
            "Требования: умеренный капитал, знания не ниже 40 и управляемый стресс."
        ),
        options = emptyList()
    )
    map["ending_regular"] = event(
        id = "ending_regular",
        flavor = "😐",
        isEnding = true,
        endingType = EndingType.PAYCHECK_TO_PAYCHECK,
        message = story(
            "Обычный финал: жизнь идет, но решения все еще слишком зависят от следующей зарплаты.",
            "Это не провал. Это сигнал: резерв, долги и финансовые правила пока слабее внешнего шума."
        ),
        options = emptyList()
    )
    map["ending_bankruptcy"] = event(
        id = "ending_bankruptcy",
        flavor = "💀",
        isEnding = true,
        endingType = EndingType.BANKRUPTCY,
        message = story(
            "Плохой финал: деньги закончились раньше, чем стресс. Ошибки наложились друг на друга: долг, импульсные траты, доверие без проверки.",
            "Главный урок: схема почти всегда продает срочность. Хорошее финансовое решение выдерживает паузу и проверку."
        ),
        options = emptyList()
    )
}

fun storyConditionals(balance: StoryBalance): List<GameEvent> = listOf(
    event(
        id = "ending_bankruptcy_trigger",
        priority = 120,
        flavor = "🚨",
        conditions = listOf(cond(CAPITAL, LTE, 0L), cond(STRESS, GTE, 85L)),
        message = "Денег не осталось, платежи давят, а стресс уже мешает принимать решения. Это точка, где игра честно фиксирует провал.",
        options = listOf(option("accept_bankruptcy", "Признать банкротство", "💀", "ending_bankruptcy"))
    ),
    event(
        id = "ending_wealth_trigger",
        priority = 100,
        unique = true,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            Condition.HasFlag("business_scaled"),
            cond(CAPITAL, GTE, balance.wealthCapital),
            cond(KNOWLEDGE, GTE, 55L)
        ),
        message = "Итог показывает редкую комбинацию: капитал вырос, доход масштабируется, а решения стали проверяемыми. Можно фиксировать сильную победу.",
        options = listOf(option("claim_wealth", "Зафиксировать победу", "🏆", "ending_wealth"))
    ),
    event(
        id = "ending_freedom_trigger",
        priority = 95,
        unique = true,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, balance.freedomCapital),
            cond(KNOWLEDGE, GTE, 55L),
            cond(STRESS, LTE, 65L)
        ),
        message = "Ты не обязан срочно соглашаться на сомнительные предложения. Есть запас, есть правила, есть спокойствие. Это финансовая свобода для этой эпохи.",
        options = listOf(option("claim_freedom", "Закончить свободным", "🌅", "ending_freedom"))
    ),
    event(
        id = "ending_stability_trigger",
        priority = 90,
        unique = true,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, GTE, balance.stabilityCapital),
            cond(KNOWLEDGE, GTE, 40L),
            cond(STRESS, LTE, 75L)
        ),
        message = "Финал без фейерверка, но с фундаментом: резерв есть, долги под контролем, чужие обещания больше не управляют твоим кошельком.",
        options = listOf(option("claim_stability", "Закончить стабильно", "💼", "ending_stability"))
    ),
    event(
        id = "ending_paycheck_trigger",
        priority = 85,
        unique = true,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),
            cond(CAPITAL, LTE, balance.stabilityCapital / 3),
            cond(KNOWLEDGE, LTE, 38L)
        ),
        message = "Итог честный: запас слишком тонкий, знания выросли мало, любая новая срочность снова может толкнуть к плохому долгу.",
        options = listOf(option("claim_paycheck", "Принять обычный финал", "😐", "ending_regular"))
    ),
    event(
        id = "ending_regular_trigger",
        priority = 80,
        unique = true,
        conditions = listOf(Condition.HasFlag("arc.final_check")),
        message = "Ты прошел историю без катастрофы, но до устойчивой системы не хватило капитала, знаний или спокойствия.",
        options = listOf(option("claim_regular", "Закончить как есть", "📓", "ending_regular"))
    ),
    event(
        id = "debt_crisis",
        priority = 35,
        flavor = "🚨",
        cooldownMonths = 4,
        conditions = listOf(cond(DEBT, GT, balance.debtCrisisDebt), cond(CAPITAL, LTE, balance.debtCrisisCapital)),
        tags = setOf("crisis"),
        message = "Долг начал управлять месяцем: платежи съедают воздух, а любой праздник или болезнь теперь выглядит как угроза.",
        options = listOf(
            option("debt_restructure", "Договориться о реструктуризации", "🏦", MONTHLY_TICK,
                Effect(debtPaymentDelta = -20_000L, stressDelta = 10, knowledgeDelta = 8)),
            option("debt_sell_assets", "Продать лишнее и снизить долг", "📉", MONTHLY_TICK,
                Effect(capitalDelta = -80_000L, debtDelta = -160_000L, stressDelta = 8, knowledgeDelta = 5)),
            option("debt_ignore", "Потянуть еще месяц", "😶", MONTHLY_TICK,
                Effect(stressDelta = 16, riskDelta = 8))
        )
    ),
    event(
        id = "burnout_warning",
        priority = 30,
        flavor = "😮‍💨",
        cooldownMonths = 5,
        conditions = listOf(cond(STRESS, GTE, 78L)),
        tags = setOf("crisis"),
        message = "Стресс дошел до точки, где ты уже платишь вниманием. Ошибки в деньгах часто начинаются не с незнания, а с усталости.",
        options = listOf(
            option("burnout_rest", "Снизить темп на месяц", "🛏️", MONTHLY_TICK,
                Effect(capitalDelta = -40_000L, incomeDelta = -15_000L, stressDelta = -28, knowledgeDelta = 3)),
            option("burnout_push", "Продавить еще месяц", "😤", MONTHLY_TICK,
                Effect(incomeDelta = 20_000L, stressDelta = 12, riskDelta = 5)),
            option("burnout_rules", "Упростить бюджет и убрать лишнее", "📋", MONTHLY_TICK,
                Effect(expensesDelta = -25_000L, stressDelta = -12, knowledgeDelta = 6))
        )
    ),
    event(
        id = "investment_unlock",
        priority = 20,
        flavor = "💡",
        unique = true,
        conditions = listOf(cond(KNOWLEDGE, GTE, 42L), cond(CAPITAL, GTE, balance.investmentTicket)),
        tags = setOf("investment"),
        message = "Теперь ты видишь разницу между инвестицией и ставкой. Можно начать консервативно: маленькая сумма, понятный инструмент, без обещаний «иксов».",
        options = listOf(
            option("invest_conservative", "Вложить небольшую сумму регулярно", "📈", MONTHLY_TICK,
                Effect(capitalDelta = -balance.investmentTicket, investmentsDelta = balance.investmentTicket, knowledgeDelta = 6, stressDelta = -2)),
            option("skip_investing", "Пока оставить деньги в резерве", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 3, stressDelta = -3))
        )
    )
)

fun storyEventPool(eraId: String): List<PoolEntry> = buildList {
    add(PoolEntry("normal_life", 24))
    add(PoolEntry("new_year_event", 6))
    add(PoolEntry("birthday_event", 6))
    add(PoolEntry("dating_event", 6))
    add(PoolEntry("breakup_event", 3))
    add(PoolEntry("wedding_event", 4))
    add(PoolEntry("child_birth_event", 3))
    add(PoolEntry("school_event", 3))
    add(PoolEntry("university_event", 4))
    add(PoolEntry("bar_club_event", 5))
    add(PoolEntry("gaming_tv_event", 5))
    addAll(ScamEventLibrary.poolEntries)
}
