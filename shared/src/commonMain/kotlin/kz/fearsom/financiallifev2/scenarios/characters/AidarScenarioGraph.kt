package kz.fearsom.financiallifev2.scenarios.characters

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
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.ScheduledEvent
import kz.fearsom.financiallifev2.scenarios.ScamEventLibrary
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.cond
import kz.fearsom.financiallifev2.scenarios.event
import kz.fearsom.financiallifev2.scenarios.option
import kz.fearsom.financiallifev2.scenarios.story

class AidarScenarioGraph(private val eraId: String = "kz_2024") : ScenarioGraph() {

    override val initialPlayerState = when (eraId) {
        "kz_2005" -> PlayerState(
            capital = 120_000L,
            income = 95_000L,
            expenses = 68_000L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            investments = 0L,
            investmentReturnRate = 0.07,
            stress = 28,
            financialKnowledge = 12,
            riskLevel = 22,
            month = 1,
            year = 2005,
            characterId = "aidar",
            eraId = eraId
        )
        "kz_2015" -> PlayerState(
            capital = 260_000L,
            income = 310_000L,
            expenses = 215_000L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            investments = 0L,
            investmentReturnRate = 0.08,
            stress = 30,
            financialKnowledge = 18,
            riskLevel = 24,
            month = 1,
            year = 2015,
            characterId = "aidar",
            eraId = eraId
        )
        else -> PlayerState(
            capital = 420_000L,
            income = 520_000L,
            expenses = 325_000L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            investments = 0L,
            investmentReturnRate = 0.08,
            stress = 32,
            financialKnowledge = 24,
            riskLevel = 28,
            month = 1,
            year = 2024,
            characterId = "aidar",
            eraId = eraId
        )
    }

    override val events: Map<String, GameEvent> = when (eraId) {
        "kz_2005" -> aidar2005Events()
        "kz_2015" -> aidar2015Events()
        else -> aidar2024Events()
    }

    override val conditionalEvents: List<GameEvent> = when (eraId) {
        "kz_2005" -> aidar2005Conditionals()
        "kz_2015" -> aidar2015Conditionals()
        else -> aidar2024Conditionals()
    }

    override val eventPool: List<PoolEntry> = when (eraId) {
        "kz_2005" -> listOf(
            PoolEntry("normal_life", 18),
            PoolEntry("family_pressure", 8),
            PoolEntry("freelance_order", 7)
        ) + ScamEventLibrary.poolEntries
        "kz_2015" -> listOf(
            PoolEntry("normal_life", 18),
            PoolEntry("family_pressure", 8),
            PoolEntry("mortgage_offer", 6),
            PoolEntry("freelance_order", 7)
        ) + ScamEventLibrary.poolEntries
        else -> listOf(
            PoolEntry("normal_life", 18),
            PoolEntry("family_pressure", 8),
            PoolEntry("mortgage_offer", 6),
            PoolEntry("freelance_order", 7)
        ) + ScamEventLibrary.poolEntries
    }

    private fun aidar2005Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "🖥️",
            message = story(
                """
                Весна 2005 года. Айдар выходит из старого интернет-кафе на Абая и держит в кармане первую зарплату. Купюры тёплые, будто только что напечатаны, а в голове всё ещё шумят вентиляторы системных блоков и слова начальника: «Если будешь шевелиться, вырастешь быстрее остальных».
                """,
                """
                Он живёт с матерью и младшим братом в тесной квартире, ездит на работу через полгорода и притворяется спокойным. На самом деле ему страшно: нефть качает экономику вверх, знакомые хватают кредиты, вокруг говорят про «лёгкие деньги», а он пока знает только, как чинить чужие сайты по ночам.
                """,
                """
                На счету {capital}. Доход {income} в месяц. После расходов остаётся слишком мало для ошибки, но именно сейчас друг зовёт в мелкую торговую схему с телефонами, а семья ждёт, что Айдар станет первым, кто вытащит их из постоянной нехватки.
                """
            ),
            options = listOf(
                option("help_brother", "Сначала помочь брату с подготовкой и расходами семьи", "👨‍👩‍👦", "startup_pitch",
                    Effect(expensesDelta = 8_000L, stressDelta = -3, knowledgeDelta = 2, setFlags = setOf("aidar.family.first"))),
                option("save_first", "Спрятать деньги в конверт и думать только о своём росте", "💵", "startup_pitch",
                    Effect(knowledgeDelta = 4, stressDelta = 2, setFlags = setOf("aidar.self.first")))
            )
        ))
        put("startup_pitch", event(
            id = "startup_pitch",
            flavor = "📦",
            message = story(
                """
                На Саяхате пахнет пылью, бензином и шансом. Старый однокурсник Тимур показывает багажник, полный кнопочных телефонов: «Берём партию, раскладываем по точкам, навар за месяц будет как твоя полугодовая зарплата».
                """,
                """
                Айдару хочется впервые перестать считать мелочь перед кассой. Но он видит и другое: бумаги нет, договорённости только на словах, а рынок держится на чужой уверенности и своей способности не моргнуть первым.
                """,
                """
                Войти можно деньгами, временем или совсем не входить. Цена решения для него не только в прибыли: если он ошибётся, семья узнает, что их «надежда» пока такой же мальчишка, как и был.
                """
            ),
            options = listOf(
                option("join_startup", "Войти деньгами и взять риск на себя", "🎲", "startup_3months",
                    Effect(capitalDelta = -80_000L, stressDelta = 14, riskDelta = 12,
                        scheduleEvent = ScheduledEvent("startup_aftershock", 3))),
                option("partial_startup", "Помочь кодом и логистикой, но не вкладывать сбережения", "🛠️", "senior_promotion",
                    Effect(stressDelta = 8, knowledgeDelta = 7, setFlags = setOf("aidar.side_hustle"))),
                option("skip_startup", "Отказаться и остаться в ремесле", "🛡️", "senior_promotion",
                    Effect(knowledgeDelta = 4, stressDelta = -2))
            )
        ))
        put("startup_3months", event(
            id = "startup_3months",
            flavor = "🌃",
            message = story(
                """
                Три месяца Айдар почти не видит дневного света. Днём работа, ночью объявления, таблицы, перепродажи, созвоны. Деньги мелькают быстрее, чем успевают стать спокойствием.
                """,
                """
                Тимур уже говорит как человек из будущего: второй склад, свой сайт, поставки напрямую. Айдар же чувствует, что усталость превращается в азарт, а азарт скоро перестанет различать границу между смелостью и жадностью.
                """,
                """
                Сейчас можно либо зафиксировать результат и вернуться к карьере, либо ещё сильнее зайти в схему и связать с ней ближайший год жизни.
                """
            ),
            options = listOf(
                option("pitch_investors", "Остаться в деле и качнуть схему сильнее", "🚀", "startup_result",
                    Effect(stressDelta = 18, knowledgeDelta = 6, riskDelta = 10)),
                option("exit_startup", "Выйти с тем, что успел сохранить", "🚪", "senior_promotion",
                    Effect(capitalDelta = 45_000L, stressDelta = -10, knowledgeDelta = 5))
            )
        ))
        put("startup_result", event(
            id = "startup_result",
            flavor = "⚖️",
            message = story(
                """
                Осенью рынок остывает так же резко, как разогревался. У двух точек касса просела, поставщик требует предоплату, а Тимур впервые говорит шёпотом. В комнате, где ещё недавно обсуждали рост, теперь считают, кому и сколько должны.
                """,
                """
                Айдар понимает неприятную вещь: быстрые деньги всегда хотят, чтобы ты привязал к ним своё имя. Если сейчас резко уйти, можно сохранить репутацию и часть капитала. Если остаться, есть шанс вытащить проект, но цена ошибки станет личной.
                """,
                """
                Решение уже не про смелость. Оно про взрослость: закрыть глаза на риск или признать, что красивый рассказ не обязан становиться судьбой.
                """
            ),
            options = listOf(
                option("strong_pitch", "Взять управление на себя и спасать дело", "💼", MONTHLY_TICK,
                    Effect(incomeDelta = 60_000L, stressDelta = 20, knowledgeDelta = 12, setFlags = setOf("aidar.2005_trade_risk"))),
                option("safe_exit", "Закрыть историю и вернуться в профессию", "🔄", "senior_promotion",
                    Effect(stressDelta = -12, knowledgeDelta = 8, setFlags = setOf("aidar.learned_hype")))
            )
        ))
        put("senior_promotion", event(
            id = "senior_promotion",
            flavor = "📈",
            message = story(
                """
                В офисе пахнет дешёвым кофе и паяльником. Начальник долго смотрит на Айдара поверх монитора, потом кивает: «Сайты держатся на тебе. Можем поднять тебя, но надо перестать быть мальчиком на подхвате».
                """,
                """
                Это не тот глянцевый карьерный рост, о котором пишут в журналах. Это длинные вечера, чужие ошибки, тяжёлые заказчики и ощущение, что профессионализм здесь рождается без фанфар, почти в тишине.
                """,
                """
                Повышение даст больше воздуха, но и больше ответственности. Параллельно знакомый зовёт на фриланс-проект, который может открыть новую траекторию, если Айдар выдержит двойную нагрузку.
                """
            ),
            options = listOf(
                option("middle_promo", "Взять повышение и строить имя внутри компании", "🧱", MONTHLY_TICK,
                    Effect(incomeDelta = 45_000L, stressDelta = 6, knowledgeDelta = 8)),
                option("senior_astana", "Уйти в более дерзкий рынок и искать рост через новые проекты", "🏙️", "freelance_order",
                    Effect(stressDelta = 12, knowledgeDelta = 10, riskDelta = 6)),
                option("stay_same", "Остаться в тени ещё на время и наблюдать", "🔍", MONTHLY_TICK,
                    Effect(knowledgeDelta = 3, stressDelta = -2))
            )
        ))
        commonAidarEvents()
        commonAidarEndings("2005")
    }

    private fun aidar2015Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "📱",
            message = story(
                """
                Лето 2015-го. Алматы кажется современнее, чем был вчера: коворкинги, кофейни, стартап-митапы, бесконечные разговоры про приложения. Айдар уже не студент из мечты, а разработчик, который наконец-то чувствует, что может выбрать свою жизнь сам.
                """,
                """
                Но под гладкой городской витриной что-то дрожит. Тенге нестабилен, друзья переводят сбережения в доллары, одни уезжают, другие влезают в кредиты, потому что «потом будет поздно». Айдару двадцать четыре, и от него ждут не просто амбиций, а взрослого решения.
                """,
                """
                У него {capital} на счету, доход {income} в месяц и ощущение, что одно точное движение может резко поднять его вверх. Мама всё ещё рассчитывает на помощь семье, а бывший однокурсник зовёт строить продукт, который «взорвёт рынок».
                """
            ),
            options = listOf(
                option("help_brother", "Сначала закрепить семью и взять часть их тревоги на себя", "👪", "startup_pitch",
                    Effect(expensesDelta = 12_000L, stressDelta = -4, knowledgeDelta = 2, setFlags = setOf("aidar.family.first"))),
                option("save_first", "Собрать подушку и не расплескать шанс на рост", "💼", "startup_pitch",
                    Effect(knowledgeDelta = 4, setFlags = setOf("aidar.buffer.first")))
            )
        ))
        put("startup_pitch", event(
            id = "startup_pitch",
            flavor = "💡",
            message = story(
                """
                В кофейне у Данияра глаза горят так, будто он уже увидел будущее. Он раскладывает салфетки как презентацию: маркетплейс для локальных мастеров, экспорт, Astana Hub, ангелы, первый раунд, быстрый рост.
                """,
                """
                Айдар слушает и замечает знакомое чувство. Его тянет не только к деньгам. Его тянет к версии себя, которая не боится большой игры. Но именно это и пугает сильнее всего: иногда мечта звучит так убедительно, что перестаёшь задавать ей жёсткие вопросы.
                """,
                """
                Зайти можно капиталом, работой или красивым отказом. Каждая из этих ролей по-своему честная. Вопрос только в том, кем Айдар готов быть сейчас.
                """
            ),
            options = listOf(
                option("join_startup", "Вложить деньги и по-настоящему войти в стартап", "🚀", "startup_3months",
                    Effect(capitalDelta = -150_000L, stressDelta = 16, knowledgeDelta = 6,
                        scheduleEvent = ScheduledEvent("startup_aftershock", 4))),
                option("partial_startup", "Помочь продукту руками, не рискуя всем капиталом", "💻", "senior_promotion",
                    Effect(stressDelta = 8, knowledgeDelta = 8, setFlags = setOf("aidar.side_hustle"))),
                option("skip_startup", "Остаться в стабильной карьере и не покупать чужую уверенность", "🛡️", "senior_promotion",
                    Effect(knowledgeDelta = 4, setFlags = setOf("aidar.learned_boundaries")))
            )
        ))
        put("startup_3months", event(
            id = "startup_3months",
            flavor = "🕐",
            message = story(
                """
                Три месяца растворяются в ночных сборках, встречах и недосыпе. MVP уже живой, мастера приходят, первые заказы радуют сильнее, чем годовая премия. И всё же за каждым маленьким успехом тянется длинная тень вопроса: хватит ли этого, когда эйфория закончится.
                """,
                """
                Айдар начал говорить о продукте во множественном числе. Это опасный момент. Когда ты начинаешь строить будущее голосом «мы», провал перестаёт быть просто финансовой ошибкой и становится личным стыдом.
                """,
                """
                Данияр настаивает на питче инвесторам. Айдар понимает: либо они сейчас делают шаг в сторону настоящего бизнеса, либо вся эта история останется красивой молодостью, за которую заплатили сном и деньгами.
                """
            ),
            options = listOf(
                option("pitch_investors", "Идти до конца и питчить", "🎤", "startup_result",
                    Effect(stressDelta = 20, knowledgeDelta = 10)),
                option("exit_startup", "Выйти сейчас и забрать то, что можно спасти", "🚪", "senior_promotion",
                    Effect(capitalDelta = 80_000L, stressDelta = -10, knowledgeDelta = 4))
            )
        ))
        put("startup_result", event(
            id = "startup_result",
            flavor = "🎯",
            message = story(
                """
                В Astana Hub холодно и слишком светло. Инвесторы смотрят так, будто уже знают ответ, а Айдар впервые чувствует, как знание кода и знание денег расходятся в разные стороны. Собрать продукт оказалось проще, чем объяснить, почему в него нужно поверить.
                """,
                """
                Он замечает в себе странную двойственность. Одна его часть хочет блеснуть, доказать, что он не зря прожил эти бессонные месяцы. Другая хочет выйти из комнаты и выбрать жизнь, где не надо каждый день выглядеть увереннее, чем ты есть.
                """,
                """
                Дальше будет либо грант и новый уровень давления, либо честное возвращение к карьере. Оба исхода меняют Айдара. Просто в разных направлениях.
                """
            ),
            options = listOf(
                option("strong_pitch", "Говорить жёстко и брать шанс на рост", "💪", "startup_success",
                    Effect(knowledgeDelta = 5)),
                option("safe_exit", "Остановиться до того, как амбиции съедят опору", "🔄", "senior_promotion",
                    Effect(stressDelta = -12, knowledgeDelta = 6))
            )
        ))
        put("startup_success", event(
            id = "startup_success",
            flavor = "🏁",
            message = story(
                """
                Им понравилось. На столе появляются реальные деньги, а в календаре сразу не остаётся свободных дней. Всё, что было мечтой, неожиданно превращается в работу: созвоны, отчёты, чужие ожидания, новые обязательства.
                """,
                """
                Айдар чувствует эйфорию, но не покой. Когда мечта наконец происходит, иногда первым приходит не счастье, а тяжесть. Теперь у него есть шанс стать человеком, который строит своё. И есть риск потерять здоровье и простую жизнь раньше, чем продукт вырастет.
                """,
                """
                Нужно решить, что он отдаст стартапу: только энергию после основной работы или всю карьеру целиком.
                """
            ),
            options = listOf(
                option("quit_job", "Уйти с работы и поставить всё на продукт", "🦅", MONTHLY_TICK,
                    Effect(capitalDelta = 500_000L, incomeDelta = -310_000L, stressDelta = 22, knowledgeDelta = 14,
                        setFlags = setOf("aidar.quit.for.startup"))),
                option("keep_job", "Оставить работу и тащить два фронта сразу", "⚖️", MONTHLY_TICK,
                    Effect(capitalDelta = 500_000L, stressDelta = 28, knowledgeDelta = 9,
                        setFlags = setOf("aidar.double.load")))
            )
        ))
        put("senior_promotion", event(
            id = "senior_promotion",
            flavor = "📬",
            message = story(
                """
                Через несколько месяцев Айдара вызывает тимлид. В голосе нет пафоса, только деловая прямота: его заметили. Параллельно пришёл оффер из Астаны, более резкий по деньгам и громче по статусу.
                """,
                """
                В 2015-м это уже не просто вопрос карьеры. Это вопрос валюты, будущих расходов, съёмного жилья, возможности когда-нибудь купить своё. Одно решение выглядит разумным. Другое выглядит как прыжок в следующую версию себя.
                """,
                """
                Айдару нужно выбрать, где он хочет взрослеть: внутри известной системы или в пространстве, где за рост платят нервами, переездами и риском ошибиться громче обычного.
                """
            ),
            options = listOf(
                option("middle_promo", "Остаться и расти внутри команды", "🧗", MONTHLY_TICK,
                    Effect(incomeDelta = 80_000L, stressDelta = 5, knowledgeDelta = 8)),
                option("senior_astana", "Сорваться в Астану ради большого шага", "🏙️", MONTHLY_TICK,
                    Effect(incomeDelta = 150_000L, expensesDelta = 40_000L, stressDelta = 16, knowledgeDelta = 12,
                        setFlags = setOf("aidar.relocated"))),
                option("stay_same", "Подождать и не разменивать себя на суету", "🕰️", MONTHLY_TICK,
                    Effect(knowledgeDelta = 4, stressDelta = -2))
            )
        ))
        commonAidarEvents()
        commonAidarEndings("2015")
    }

    private fun aidar2024Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "🤖",
            message = story(
                """
                Алматы, 2024 год. На каждом митапе говорят про AI, продуктивность, опционы и собственные продукты. У Айдара уже есть нормальная работа, ноутбук лучше, чем у половины его преподавателей когда-то был, и странное чувство, что рынок ускорился быстрее, чем он успел привыкнуть к взрослой жизни.
                """,
                """
                Снаружи он выглядит собранным. Внутри всё сложнее: семья по-прежнему просит помощи, коллеги внезапно увольняются ради стартапов, а лента новостей каждый день намекает, что если не рискнуть сейчас, то можно навсегда остаться просто «хорошим исполнителем».
                """,
                """
                На счету {capital}, доход {income} в месяц. Это уже не бедность, но ещё не свобода. И именно поэтому каждое решение становится опаснее: ошибаться больнее, когда тебе есть что терять.
                """
            ),
            options = listOf(
                option("help_brother", "Поставить семью выше своего темпа и взять часть их нагрузки", "🤝", "startup_pitch",
                    Effect(expensesDelta = 20_000L, stressDelta = -4, knowledgeDelta = 2, setFlags = setOf("aidar.family.first"))),
                option("save_first", "Построить себе опору, прежде чем спасать всех остальных", "🛡️", "startup_pitch",
                    Effect(knowledgeDelta = 4, setFlags = setOf("aidar.buffer.first")))
            )
        ))
        put("startup_pitch", event(
            id = "startup_pitch",
            flavor = "🧠",
            message = story(
                """
                Друг зовёт не просто в стартап, а в «правильную волну»: AI-сервис для малого бизнеса, быстрый прототип, ангельские деньги, пилоты с реальными компаниями. Всё звучит настолько современно, что отказ почти кажется признаком трусости.
                """,
                """
                Но Айдар уже взрослый достаточно, чтобы замечать, как мода умеет маскировать сырость. Он понимает продукт, но не до конца понимает unit-экономику. И это та самая трещина, через которую из мечты часто вытекают деньги.
                """,
                """
                Можно войти глубоко, можно ограничиться технической ролью, можно остаться в найме и наблюдать со стороны. Каждый из вариантов сохранит что-то важное и лишит чего-то не менее важного.
                """
            ),
            options = listOf(
                option("join_startup", "Вложиться деньгами и стать кофаундером по-настоящему", "🚀", "startup_3months",
                    Effect(capitalDelta = -220_000L, stressDelta = 18, knowledgeDelta = 8,
                        scheduleEvent = ScheduledEvent("startup_aftershock", 3))),
                option("partial_startup", "Войти как техлид без полного финансового прыжка", "💻", "senior_promotion",
                    Effect(stressDelta = 10, knowledgeDelta = 10, setFlags = setOf("aidar.side_hustle"))),
                option("skip_startup", "Остаться в найме и не сжигать опору ради хайпа", "🧱", "senior_promotion",
                    Effect(knowledgeDelta = 5, setFlags = setOf("aidar.learned_boundaries")))
            )
        ))
        put("startup_3months", event(
            id = "startup_3months",
            flavor = "📊",
            message = story(
                """
                Три месяца спустя у продукта есть первые клиенты, но нет чувства победы. Есть метрики, которые можно красиво показать в презентации, и есть команда, которая уже устала раньше, чем успела разбогатеть.
                """,
                """
                Айдар просыпается ночью и не сразу вспоминает, куда идёт утром: в офис или в очередной режим спасения. Он начинает понимать, что взрослый риск отличается от юношеского не размером ставки, а количеством людей, которых ты тянешь за собой.
                """,
                """
                Перед ним снова выбор. Додавить историю до следующего раунда или вернуть себе право на нормальный сон и предсказуемую жизнь.
                """
            ),
            options = listOf(
                option("pitch_investors", "Дожать раунд и доказать, что всё не зря", "🎤", "startup_result",
                    Effect(stressDelta = 22, knowledgeDelta = 10)),
                option("exit_startup", "Остановиться и сохранить себя", "🚪", "senior_promotion",
                    Effect(capitalDelta = 120_000L, stressDelta = -12, knowledgeDelta = 5))
            )
        ))
        put("startup_result", event(
            id = "startup_result",
            flavor = "🧾",
            message = story(
                """
                На встрече с фондом вопросы оказываются взрослее, чем сам проект. Не «как красиво», а «сколько стоит клиент», «когда окупитесь», «кто останется, если продажи просядут». Это уже не игра в воображаемое будущее. Это проверка на то, умеешь ли ты жить в реальности.
                """,
                """
                Айдару вдруг легче, чем ожидалось. Не потому, что он уверен. Потому что он наконец видит проект без романтического фильтра. Это редкий и дорогой момент ясности.
                """,
                """
                Если он сейчас нажмёт на газ, стартап станет центром его жизни. Если отступит, не будет чувствовать себя трусом, потому что поймёт цену осознанного отказа.
                """
            ),
            options = listOf(
                option("strong_pitch", "Забрать шанс и не отводить взгляд", "🔥", "startup_success",
                    Effect(knowledgeDelta = 6)),
                option("safe_exit", "Выбрать взрослую дистанцию вместо красивого хаоса", "🧭", "senior_promotion",
                    Effect(stressDelta = -10, knowledgeDelta = 8))
            )
        ))
        put("startup_success", event(
            id = "startup_success",
            flavor = "🎉",
            message = story(
                """
                Раунд случился. В чатах поздравляют, инвестор пишет «погнали», у команды появляются зарплаты и KPI. Снаружи всё похоже на успех.
                """,
                """
                Внутри Айдар неожиданно чувствует не триумф, а нехватку воздуха. Теперь это не просто продукт, который можно закрыть без объяснений. Теперь это структура, договоры, люди, ожидания и почти постоянное ощущение, что он живёт на краю собственного внимания.
                """,
                """
                Ему нужно решить, как он будет нести этот успех: полностью уйдёт в него или попытается оставить хотя бы одну опору за пределами бизнеса.
                """
            ),
            options = listOf(
                option("quit_job", "Сжечь мосты и полностью уйти в компанию", "🦅", MONTHLY_TICK,
                    Effect(capitalDelta = 900_000L, incomeDelta = -520_000L, stressDelta = 24, knowledgeDelta = 16,
                        setFlags = setOf("aidar.quit.for.startup"))),
                option("keep_job", "Держать две опоры и платить за это усталостью", "⚖️", MONTHLY_TICK,
                    Effect(capitalDelta = 900_000L, stressDelta = 30, knowledgeDelta = 10,
                        setFlags = setOf("aidar.double.load")))
            )
        ))
        put("senior_promotion", event(
            id = "senior_promotion",
            flavor = "📨",
            message = story(
                """
                Рекрутеры уже пишут так, будто знают Айдара лучше, чем он сам: remote-команды, зарплаты в валюте, опционы, новый стек, рост до техлида. Одновременно текущая компания предлагает заметный шаг вверх и обещает «стабильное развитие».
                """,
                """
                В 2024-м стабильность звучит почти экзотически. Всё вокруг учит, что выигрывает тот, кто быстрее. Но Айдар уже знает цену скорости: она часто приходит в комплекте с тревогой, размытыми границами и постоянным ощущением, что ты должен быть лучше своей усталости.
                """,
                """
                Он выбирает не просто зарплату. Он выбирает, какой взрослой жизнью будет объяснять себе следующие несколько лет.
                """
            ),
            options = listOf(
                option("middle_promo", "Расти внутри текущей команды и укреплять базу", "🧱", MONTHLY_TICK,
                    Effect(incomeDelta = 120_000L, stressDelta = 4, knowledgeDelta = 8)),
                option("senior_astana", "Принять более дерзкий оффер и резко ускориться", "🚄", MONTHLY_TICK,
                    Effect(incomeDelta = 220_000L, expensesDelta = 60_000L, stressDelta = 15, knowledgeDelta = 12,
                        setFlags = setOf("aidar.relocated"))),
                option("stay_same", "Сделать паузу и не покупать лишнюю гонку", "🕰️", MONTHLY_TICK,
                    Effect(knowledgeDelta = 4, stressDelta = -3))
            )
        ))
        commonAidarEvents()
        commonAidarEndings("2024")
    }

    private fun MutableMap<String, GameEvent>.commonAidarEvents() {
        put("freelance_order", event(
            id = "freelance_order",
            flavor = "🧾",
            poolWeight = 8,
            tags = setOf("career"),
            message = story(
                """
                Бывший клиент пишет поздно вечером: нужен срочный проект, деньги нормальные, сроки неприятные. Это тот самый дополнительный заработок, который красиво смотрится в таблице и тяжело переживается в жизни.
                """,
                """
                Айдар знает свою слабость: ему трудно отказываться от шанса, который выглядит как ещё один этаж вверх. Но каждый такой этаж строится из часов, которых у него уже почти нет.
                """,
                """
                Можно взять заказ ради денег и опыта, можно переоценить сроки честно, можно отказаться и оставить силы себе.
                """
            ),
            options = listOf(
                option("freelance_yes", "Взять заказ целиком и выжать из месяца максимум", "💼", MONTHLY_TICK,
                    Effect(incomeDelta = 90_000L, stressDelta = 14, knowledgeDelta = 6)),
                option("freelance_half", "Сузить объём и не сжечь себя", "✂️", MONTHLY_TICK,
                    Effect(incomeDelta = 40_000L, stressDelta = 5, knowledgeDelta = 5)),
                option("freelance_no", "Отказаться и сохранить темп", "🛏️", MONTHLY_TICK,
                    Effect(stressDelta = -5, knowledgeDelta = 2))
            )
        ))
        put("family_pressure", event(
            id = "family_pressure",
            flavor = "👨‍👩‍👧",
            poolWeight = 8,
            tags = setOf("family"),
            message = story(
                """
                Мама звонит поздно. Голос у неё всегда становится тише, когда речь о деньгах. Брату нужна помощь, у отца снова проблемы со здоровьем, дома никто не драматизирует вслух, но вся тревога всё равно каким-то образом приходит к Айдару.
                """,
                """
                Он любит их и ненавидит это чувство одновременно. Потому что каждый перевод семье — не просто расход. Это ещё и молчаливое обещание, что он выдержит всё, даже если сам пока едва строит свою устойчивость.
                """,
                """
                На счету {capital}. Помочь можно по-разному, но ни один вариант не позволит остаться совсем невиноватым ни перед ними, ни перед собой.
                """
            ),
            options = listOf(
                option("help_fully", "Закрыть проблему деньгами полностью", "❤️", MONTHLY_TICK,
                    Effect(capitalDelta = -220_000L, stressDelta = 8, setFlags = setOf("aidar.family.helped"))),
                option("help_partial", "Помочь только в самой срочной части", "🏥", MONTHLY_TICK,
                    Effect(capitalDelta = -120_000L, stressDelta = 4, knowledgeDelta = 2)),
                option("help_minimum", "Сказать честно, что больше не потянешь", "🧊", MONTHLY_TICK,
                    Effect(capitalDelta = -40_000L, stressDelta = 14, setFlags = setOf("aidar.guilt"))))
        ))
        put("mortgage_offer", event(
            id = "mortgage_offer",
            flavor = "🏠",
            poolWeight = 6,
            tags = setOf("mortgage"),
            message = story(
                """
                Банк шлёт предварительное одобрение. Съёмная квартира вдруг начинает казаться не временным этапом, а затянувшейся жизнью на чужих правилах. Собственное жильё манит не роскошью, а обещанием наконец-то перестать быть гостем.
                """,
                """
                Но ипотека для Айдара звучит как длинная фраза без права на ошибку. Она требует будущего, в которое надо поверить заранее: что работа останется, здоровье выдержит, рынок не перекроит всё снова.
                """,
                """
                Сейчас он может зайти в долг, продолжить копить или сознательно выбрать аренду как более свободную, пусть и не такую «взрослую» в глазах семьи.
                """
            ),
            options = listOf(
                option("take_mortgage", "Войти в ипотеку и перестать жить временно", "🔑", MONTHLY_TICK,
                    Effect(capitalDelta = -2_400_000L, debtDelta = 9_600_000L, expensesDelta = 85_000L, stressDelta = 16, knowledgeDelta = 4,
                        setFlags = setOf("aidar.has.mortgage"))),
                option("skip_mortgage", "Продолжать копить и не привязывать себя к долгу", "⏳", MONTHLY_TICK,
                    Effect(knowledgeDelta = 4, stressDelta = -2))
            )
        ))
        put("startup_aftershock", event(
            id = "startup_aftershock",
            flavor = "🧨",
            tags = setOf("consequence"),
            message = story(
                """
                Отложенное последствие приходит не письмом, а усталостью и цифрами. Стартап забирает больше внимания, чем обещал, партнёрство оказывается менее ровным, чем казалось в начале, а романтика быстро уступает место бухгалтерии и ответственности.
                """,
                """
                Айдар видит, как у решений появляется хвост. То, что три месяца назад выглядело вдохновением, теперь требует заново решить: он всё ещё строит своё будущее или уже просто удерживает от падения хрупкую конструкцию.
                """,
                """
                Можно сократить участие и спасти ресурс, а можно снова вложиться, потому что страшно признать: иногда хороший старт не обязан становиться большой историей.
                """
            ),
            options = listOf(
                option("aftershock_scale", "Остаться в игре и платить за шанс ещё вниманием", "📈", MONTHLY_TICK,
                    Effect(incomeDelta = 60_000L, stressDelta = 16, knowledgeDelta = 6, riskDelta = 8)),
                option("aftershock_exit", "Аккуратно отойти и сохранить основу", "🛟", MONTHLY_TICK,
                    Effect(stressDelta = -12, knowledgeDelta = 8, setFlags = setOf("aidar.learned_hype")))
            )
        ))
        put("normal_life", event(
            id = "normal_life",
            flavor = "☕",
            poolWeight = 20,
            message = story(
                """
                Месяц прошёл без громких поворотов. Работа, созвоны, дорога, короткие ужины, уведомления банка, чужие успехи в ленте и собственная жизнь, которую всё равно надо строить из обычных дней.
                """,
                """
                Именно в такие месяцы и решается будущее. Не в моменты красивого риска, а в повторах: что Айдар делает с лишними деньгами, как обращается со своей усталостью, чему говорит «да» без свидетелей.
                """,
                """
                На этот раз у него есть возможность укрепить опору, вложиться в знания или снова попробовать ускориться.
                """
            ),
            options = listOf(
                option("invest_etf", "Отправить часть спокойствия в инвестиции", "📊", MONTHLY_TICK,
                    Effect(capitalDelta = -40_000L, investmentsDelta = 40_000L, knowledgeDelta = 4)),
                option("online_course", "Купить себе следующее профессиональное плечо", "📚", MONTHLY_TICK,
                    Effect(capitalDelta = -25_000L, knowledgeDelta = 8, stressDelta = -3)),
                option("save_cash", "Ничего не доказывать миру и просто увеличить подушку", "🐷", MONTHLY_TICK,
                    Effect(stressDelta = -4)),
                option("network_it", "Пойти к людям и искать следующую дверь", "🤝", MONTHLY_TICK,
                    Effect(capitalDelta = -10_000L, knowledgeDelta = 5, stressDelta = -4))
            )
        ))
    }

    private fun MutableMap<String, GameEvent>.commonAidarEndings(eraLabel: String) {
        put("ending_startup_king", event(
            id = "ending_startup_king",
            isEnding = true,
            endingType = EndingType.WEALTH,
            flavor = "🚀",
            message = story(
                """
                В какой-то момент Айдар перестал быть парнем, который пытается догнать рынок. Теперь рынок сам ищет встречи с ним. Компания выросла, люди внутри говорят о продукте как о чём-то надёжном, а не о дерзкой попытке.
                """,
                """
                Он часто вспоминает, как боялся первой большой траты и первого настоящего риска. Не потому, что скучает по бедности, а потому что теперь понимает: богатство выросло не из удачи, а из способности выдержать свои решения дольше, чем выдерживает чужой восторг.
                """,
                """
                Эпоха $eraLabel осталась в прошлом. Капитал {capital}. У Айдара теперь не просто деньги, а масштаб, в котором он может выбирать, кого и что строить дальше.
                """
            ),
            options = emptyList()
        ))
        put("ending_senior_dev", event(
            id = "ending_senior_dev",
            isEnding = true,
            endingType = EndingType.FINANCIAL_STABILITY,
            flavor = "💼",
            message = story(
                """
                Айдар не стал героем громких заголовков. Зато стал человеком, на которого можно опереться. В карьере у него есть имя, в жизни есть ритм, а в голове наконец меньше хаоса, чем было раньше.
                """,
                """
                Он больше не путает стабильность со слабостью. Для него это не отказ от роста, а форма уважения к себе: жить так, чтобы силы хватало не только на работу, но и на жизнь вокруг неё.
                """,
                """
                Капитал {capital}, доход {income}. Не сказка, а честно построенная почва под ногами.
                """
            ),
            options = emptyList()
        ))
        put("ending_freedom", event(
            id = "ending_freedom",
            isEnding = true,
            endingType = EndingType.FINANCIAL_FREEDOM,
            flavor = "🏖️",
            message = story(
                """
                Свобода пришла к Айдару не как громкое событие, а как одна тихая мысль утром: сегодня можно работать не из страха. Можно выбирать проекты, темп, людей. Можно отказываться.
                """,
                """
                Это ощущение оказалось дороже всех фантазий о быстрых победах. Потому что впервые за много лет его решения не продиктованы срочностью, чужими ожиданиями или необходимостью срочно доказать себе собственную ценность.
                """,
                """
                Капитал {capital}. Теперь у него есть не только доход, но и пространство для собственной жизни.
                """
            ),
            options = emptyList()
        ))
        put("ending_broke", event(
            id = "ending_broke",
            isEnding = true,
            endingType = EndingType.BANKRUPTCY,
            flavor = "💀",
            message = story(
                """
                Приходит момент, когда уже не из чего делать вид, что всё под контролем. Денег почти нет, решения накопили тяжёлый хвост, а стыд говорит громче любых расчётов.
                """,
                """
                Айдар переживает не только финансовую потерю. Он переживает обрушение собственной красивой версии себя. И это больнее. Но именно в этой точке он впервые отделяет достоинство от успеха.
                """,
                """
                Капитал почти пуст. История закончилась тяжело, но не бесполезно: теперь он знает цену тем решениям, которые раньше казались романтичными.
                """
            ),
            options = emptyList()
        ))
        put("ending_paycheck", event(
            id = "ending_paycheck",
            isEnding = true,
            endingType = EndingType.PAYCHECK_TO_PAYCHECK,
            flavor = "😐",
            message = story(
                """
                Жизнь не рухнула. И в этом есть своя тишина. Работа есть, деньги приходят, обязательства закрываются. Только ничего не сдвигается по-настоящему.
                """,
                """
                Айдар смотрит на себя без жестокости. Он не проиграл громко, но и не вывел себя туда, где выборов становится больше. Иногда самая неприятная ловушка выглядит именно так: как нормальность без движения.
                """,
                """
                Доход приходит, капитал почти не растёт. Это не конец, но точно не та глава, которую он хотел бы перечитывать.
                """
            ),
            options = emptyList()
        ))
    }

    private fun aidar2005Conditionals(): List<GameEvent> = commonAidarConditionals(
        burnoutLabel = "Глаза красные от старых мониторов, голова шумит даже после сна. Айдару кажется, что он всё ещё сидит перед мерцающим экраном, даже когда едет домой.",
        investmentLabel = "Коллега приносит распечатки про первые фонды и облигации. Всё это выглядит скучно рядом с быстрыми схемами, и именно поэтому внезапно кажется надёжным."
    )

    private fun aidar2015Conditionals(): List<GameEvent> = commonAidarConditionals(
        burnoutLabel = "Айдар замечает, что больше не умеет выключаться. После работы он продолжает жить в тасках, сообщениях и чужих дедлайнах.",
        investmentLabel = "После девальвационных разговоров и первых серьёзных чтений он начинает отличать идею от инструмента и ажиотаж от системы."
    )

    private fun aidar2024Conditionals(): List<GameEvent> = commonAidarConditionals(
        burnoutLabel = "Утро начинается не с бодрости, а с желания отменить весь день. Slack мигает раньше, чем он успевает почувствовать себя человеком.",
        investmentLabel = "Наконец цифры перестают быть только тревогой. Айдар видит, как знания превращают деньги из источника стресса в управляемый инструмент."
    )

    private fun commonAidarConditionals(
        burnoutLabel: String,
        investmentLabel: String
    ): List<GameEvent> = listOf(
        event(
            id = "debt_crisis",
            priority = 10,
            flavor = "🚨",
            conditions = listOf(cond(DEBT, GT, 0L), cond(CAPITAL, LTE, 70_000L)),
            message = story(
                """
                Банк присылает сообщение без эмоций, но тело отвечает на него мгновенно: холод в животе, сухость во рту, раздражение на всё вокруг. Долг перестал быть цифрой в приложении и стал главным героем месяца.
                """,
                """
                Айдар понимает, что тянул слишком много вещей одновременно. В какой-то момент амбиция и усталость начинают работать против него в одной команде.
                """,
                """
                Денег мало, долг {debt}, на счету {capital}. Сейчас нужно не мечтать, а спасать манёвренность.
                """
            ),
            options = listOf(
                option("sell_investments", "Срезать красивое будущее и закрыть срочную дыру", "📉", MONTHLY_TICK,
                    Effect(debtDelta = -200_000L, investmentsDelta = -150_000L, stressDelta = 12)),
                option("debt_restructure", "Пойти в банк и просить реструктуризацию без гордости", "🏦", MONTHLY_TICK,
                    Effect(expensesDelta = -20_000L, stressDelta = 18, knowledgeDelta = 5))
            )
        ),
        event(
            id = "burnout_warning",
            priority = 8,
            flavor = "😮‍💨",
            conditions = listOf(cond(STRESS, GTE, 75L)),
            message = story(
                burnoutLabel,
                """
                Он всё ещё способен работать, но уже почти не способен радоваться тому, ради чего так старался. Это тревожный сигнал: если игнорировать его слишком долго, любая карьерная победа начнёт ощущаться как наказание.
                """,
                """
                Отдых сейчас выглядит роскошью. Но возможно, именно роскошь паузы и удерживает его от более дорогого падения.
                """
            ),
            options = listOf(
                option("take_vacation", "Взять паузу и вернуть себе голову", "🏖️", MONTHLY_TICK,
                    Effect(capitalDelta = -60_000L, stressDelta = -30, knowledgeDelta = 3)),
                option("push_through", "Снова сказать себе «ещё немного»", "😤", MONTHLY_TICK,
                    Effect(stressDelta = 10, knowledgeDelta = 2))
            )
        ),
        event(
            id = "investment_unlock",
            priority = 5,
            flavor = "💡",
            unique = true,
            conditions = listOf(cond(KNOWLEDGE, GTE, 42L)),
            message = story(
                investmentLabel,
                """
                В этот момент Айдар впервые чувствует не азарт, а тихую уверенность. Он уже не ищет магию в деньгах. Он ищет дисциплину, которая даст времени поработать за него.
                """,
                """
                Можно открыть новый уровень в отношениях с финансами или отложить и остаться чуть дольше в режиме наблюдателя.
                """
            ),
            options = listOf(
                option("open_iis", "Открыть счёт и перевести знания в действие", "📈", MONTHLY_TICK,
                    Effect(capitalDelta = -120_000L, investmentsDelta = 120_000L, knowledgeDelta = 5)),
                option("skip_iis", "Оставить эту дверь на потом", "⏸️", MONTHLY_TICK,
                    Effect())
            )
        ),
        event(
            id = "ending_broke_trigger",
            priority = 100,
            flavor = "🧱",
            conditions = listOf(cond(CAPITAL, LTE, 0L), cond(STRESS, GTE, 90L)),
            message = "Дальше уже не про следующий шаг. Дальше про признание того, что старая конструкция рухнула.",
            options = listOf(option("accept_broke", "Признать провал и остановиться", "💀", "ending_broke"))
        ),
        event(
            id = "ending_freedom_trigger",
            priority = 4,
            unique = true,
            flavor = "🌅",
            conditions = listOf(cond(CAPITAL, GTE, 8_000_000L), cond(KNOWLEDGE, GTE, 60L)),
            message = "Однажды цифры складываются так, что страх впервые не успевает проснуться раньше уверенности.",
            options = listOf(option("claim_freedom", "Признать, что свобода уже собрана по частям", "🏖️", "ending_freedom"))
        ),
        event(
            id = "ending_stability_trigger",
            priority = 3,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 3_000_000L), cond(STRESS, LTE, 45L)),
            message = "Жизнь перестала шататься от каждого решения. Это может быть тише, чем мечта, но иногда тишина и есть награда.",
            options = listOf(option("claim_stability", "Остановиться на честно построенной устойчивости", "💼", "ending_senior_dev"))
        ),
        event(
            id = "ending_wealth_trigger",
            priority = 2,
            unique = true,
            conditions = listOf(
                cond(CAPITAL, GTE, 15_000_000L),
                Condition.HasFlag("aidar.quit.for.startup")
            ),
            message = "История стартапа перестала быть попыткой и стала новой финансовой реальностью.",
            options = listOf(option("claim_wealth", "Забрать свою большую концовку", "🚀", "ending_startup_king"))
        ),
        event(
            id = "ending_paycheck_trigger",
            priority = 1,
            unique = true,
            conditions = listOf(cond(CAPITAL, LTE, 120_000L), cond(KNOWLEDGE, LTE, 25L)),
            message = "Месяцы идут, а пространство для манёвра так и не выросло.",
            options = listOf(option("accept_paycheck", "Признать жизнь от зарплаты до зарплаты", "😐", "ending_paycheck"))
        )
    )
}
