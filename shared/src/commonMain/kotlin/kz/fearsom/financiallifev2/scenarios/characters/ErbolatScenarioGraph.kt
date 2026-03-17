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
import kz.fearsom.financiallifev2.scenarios.ScamEventLibrary
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.cond
import kz.fearsom.financiallifev2.scenarios.event
import kz.fearsom.financiallifev2.scenarios.option
import kz.fearsom.financiallifev2.scenarios.story

class ErbolatScenarioGraph(private val eraId: String = "kz_2024") : ScenarioGraph() {

    override val initialPlayerState = when (eraId) {
        "kz_2015" -> PlayerState(
            capital = 2_800_000L,
            income = 780_000L,
            expenses = 650_000L,
            debt = 3_600_000L,
            debtPaymentMonthly = 100_000L,
            investments = 0L,
            investmentReturnRate = 0.08,
            stress = 64,
            financialKnowledge = 34,
            riskLevel = 48,
            month = 1,
            year = 2015,
            characterId = "erbolat",
            eraId = eraId
        )
        else -> PlayerState(
            capital = 4_200_000L,
            income = 1_050_000L,
            expenses = 860_000L,
            debt = 4_000_000L,
            debtPaymentMonthly = 111_111L,
            investments = 0L,
            investmentReturnRate = 0.10,
            stress = 66,
            financialKnowledge = 38,
            riskLevel = 52,
            month = 1,
            year = 2024,
            characterId = "erbolat",
            eraId = eraId
        )
    }

    override val events: Map<String, GameEvent> = when (eraId) {
        "kz_2015" -> erbolat2015Events()
        else -> erbolat2024Events()
    }

    override val conditionalEvents: List<GameEvent> = when (eraId) {
        "kz_2015" -> commonErbolatConditionals(
            burnoutIntro = "Ерболат перестал отличать деловую злость от усталости. Домой он приходит как на вторую смену, а не как к семье.",
            financeIntro = "В 2015-м он впервые слышит про инструменты, которые работают не через «чутьё», а через дисциплину и структуру."
        )
        else -> commonErbolatConditionals(
            burnoutIntro = "Телефон Ерболата не замолкает ни в магазине, ни в машине, ни ночью. Даже успех теперь приходит в виде уведомлений и претензий.",
            financeIntro = "Цифровая торговля учит его болезненному, но важному: интуиции уже мало, нужен язык цифр и процессов."
        )
    }

    override val eventPool: List<PoolEntry> = listOf(
        PoolEntry("normal_life", 16),
        PoolEntry("supplier_scam", 10),
        PoolEntry("franchise_offer", 6)
    ) + ScamEventLibrary.poolEntries

    private fun erbolat2015Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "💼",
            message = story(
                """
                2015 год. Ерболат стоит в пустеющем зале второго магазина и впервые замечает, что музыка в торговом центре звучит слишком бодро для его реальности. Выручка ещё держится, но маржа уже стала тонкой, как чужое терпение.
                """,
                """
                Он привык считать себя человеком действия. Такие люди не жалуются, не показывают страх и всегда находят следующий ход. Но у любого предпринимателя есть момент, когда за словом «двигаемся» начинает прятаться простая усталость от постоянной ответственности.
                """,
                """
                На нём долг {debt}, капитал {capital}, семья и бизнес, который всё ещё может вытащить их вверх, а может утянуть целиком. Гульнара осторожно говорит о сокращении рисков. Сам Ерболат пока ещё тянется к идее большого хода.
                """
            ),
            options = listOf(
                option("close_second_store", "Сократить масштаб раньше, чем рынок заставит это сделать", "🔒", "ecommerce_pivot",
                    Effect(incomeDelta = -220_000L, expensesDelta = -180_000L, stressDelta = -8, knowledgeDelta = 4)),
                option("fight_competition", "Пойти в атаку и выжать рост из старой модели", "⚡", "franchise_offer",
                    Effect(capitalDelta = -180_000L, incomeDelta = 50_000L, stressDelta = 12, knowledgeDelta = 5)),
                option("wait_and_see", "Ещё немного понаблюдать и не рубить с плеча", "⏳", "supplier_scam",
                    Effect(stressDelta = 8))
            )
        ))
        put("franchise_offer", event(
            id = "franchise_offer",
            flavor = "🤝",
            message = story(
                """
                Представитель крупной сети говорит очень уверенно. Франшиза звучит как чужой порядок, который можно просто купить: бренд, стандарты, реклама, логистика, будто бы вместе с деньгами приходит и меньшая тревога.
                """,
                """
                Для Ерболата это искушение особого рода. Он устал быть человеком, который всё придумывает сам. Возможность опереться на готовую систему кажется почти роскошью. Но роскошь эта продаётся в кредит и с очень длинным хвостом обязательств.
                """,
                """
                Если он зайдёт, долг вырастет. Если откажется, придётся самому признать: спасение бизнеса всё ещё лежит на его собственных решениях.
                """
            ),
            options = listOf(
                option("take_franchise", "Купить систему и надеяться, что масштаб спасёт", "🚀", "franchise_result",
                    Effect(capitalDelta = -1_500_000L, debtDelta = 2_000_000L, incomeDelta = 180_000L, expensesDelta = 50_000L, stressDelta = 18, knowledgeDelta = 8)),
                option("skip_franchise", "Отказаться и удержать долг в пределах реальности", "🛡️", "ecommerce_pivot",
                    Effect(knowledgeDelta = 5, stressDelta = -3)),
                option("negotiate_franchise", "Торговаться и не брать на себя весь предложенный масштаб", "🗣️", "ecommerce_pivot",
                    Effect(capitalDelta = -900_000L, debtDelta = 1_000_000L, incomeDelta = 90_000L, stressDelta = 8, knowledgeDelta = 8))
            )
        ))
        put("franchise_result", event(
            id = "franchise_result",
            flavor = "📊",
            message = story(
                """
                Полгода спустя Ерболат смотрит на цифры и понимает: рост оборота сам по себе ещё не победа. Выручка стала красивее, но вместе с ней выросли и расходы, а свободы почему-то не прибавилось.
                """,
                """
                Это болезненное открытие для человека, который привык верить, что масштаб почти автоматически оправдывает риск. Теперь ясно: можно стать больше и при этом стать уязвимее.
                """,
                """
                Перед ним снова развилка. Жать дальше и строить почти империю или признать, что главная задача сейчас не экспансия, а спасение качества жизни и манёвренности.
                """
            ),
            options = listOf(
                option("third_store", "Идти дальше и покупать рост в долг", "🏬", MONTHLY_TICK,
                    Effect(debtDelta = 3_000_000L, incomeDelta = 220_000L, expensesDelta = 180_000L, stressDelta = 22)),
                option("master_franchise", "Сделать ставку на очень большую игру", "🌐", MONTHLY_TICK,
                    Effect(debtDelta = 8_000_000L, incomeDelta = 420_000L, stressDelta = 30, knowledgeDelta = 14)),
                option("hold_position", "Остановиться и сначала научиться удерживать", "🎯", MONTHLY_TICK,
                    Effect(stressDelta = -6, knowledgeDelta = 4))
            )
        ))
        put("ecommerce_pivot", event(
            id = "ecommerce_pivot",
            flavor = "🛍️",
            message = story(
                """
                Гульнара показывает экран телефона как медицинский снимок будущего: продажи уходят в онлайн. Пока Ерболат спорил с арендой и поставщиками, рынок уже научился покупать иначе.
                """,
                """
                Для него это почти удар по самолюбию. Он строил себя как офлайн-предпринимателя, человека точки, витрины, разговора с клиентом лицом к лицу. Переход в цифру ощущается не просто как новый канал, а как признание, что старый способ перестал быть центром.
                """,
                """
                Но именно здесь и может лежать спасение: быстрый маркетплейс, собственный сайт или попытка сделать всё разом и снова работать на износ.
                """
            ),
            options = listOf(
                option("kaspi_marketplace", "Пойти туда, где уже есть трафик и деньги", "🛒", MONTHLY_TICK,
                    Effect(capitalDelta = -150_000L, incomeDelta = 150_000L, stressDelta = 4, knowledgeDelta = 10,
                        setFlags = setOf("erbolat.digital"))),
                option("own_website", "Строить независимость, даже если она медленнее", "🌐", MONTHLY_TICK,
                    Effect(capitalDelta = -350_000L, incomeDelta = 90_000L, stressDelta = 12, knowledgeDelta = 14,
                        setFlags = setOf("erbolat.digital"))),
                option("both_channels", "Захватить всё сразу и жить в режиме перегруза", "⚡", MONTHLY_TICK,
                    Effect(capitalDelta = -650_000L, incomeDelta = 220_000L, stressDelta = 24, knowledgeDelta = 18,
                        setFlags = setOf("erbolat.digital")))
            )
        ))
        put("supplier_scam", commonSupplierScamEvent())
        commonErbolatHubAndEndings("2015")
    }

    private fun erbolat2024Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "📦",
            message = story(
                """
                2024 год. У Ерболата уже есть не просто магазины, а усталость человека, который слишком долго удерживал бизнес личной волей. Теперь проблемы приходят не только из офлайна: маркетплейсы давят маржу, поставщики требуют предоплату, налоговая хочет прозрачности, а семья хочет видеть дома живого человека, а не постоянно включённого директора.
                """,
                """
                Он всё ещё умеет собирать деньги из хаоса, но всё хуже умеет собирать себя. В его мире успех давно перестал быть красивым словом и стал синонимом круглосуточной доступности.
                """,
                """
                Капитал {capital}, доход {income}, долг {debt}. Бизнес можно ещё масштабировать, можно оздоровить, можно заново собрать в более спокойную и технологичную систему. Но нельзя делать вид, что ничего не меняется.
                """
            ),
            options = listOf(
                option("close_second_store", "Сократить офлайн и вернуть бизнесу кислород", "🔒", "ecommerce_pivot",
                    Effect(incomeDelta = -260_000L, expensesDelta = -220_000L, stressDelta = -10, knowledgeDelta = 5)),
                option("fight_competition", "Биться за долю рынка и не отдавать сцену маркетплейсам", "⚔️", "franchise_offer",
                    Effect(capitalDelta = -250_000L, incomeDelta = 60_000L, stressDelta = 14, knowledgeDelta = 5)),
                option("wait_and_see", "Оставить всё как есть ещё на квартал", "🕰️", "supplier_scam",
                    Effect(stressDelta = 10))
            )
        ))
        put("franchise_offer", event(
            id = "franchise_offer",
            flavor = "🌐",
            message = story(
                """
                На этот раз предложение звучит современнее: не просто франшиза, а подключение к федеральной digital-системе с CRM, рекламой, аналитикой и узнаваемым брендом. Всё то, на что самому Ерболату всегда не хватало либо времени, либо дисциплины.
                """,
                """
                Его тянет к этой сделке не только жадность. Его тянет надежда, что кто-то наконец-то привнесёт порядок в его постоянно гудящую машину. Но цена порядка всё так же выписывается долгом и уступками в контроле.
                """,
                """
                Нужно решить, покупать ли чужую систему, если она одновременно может спасти бизнес и сделать его менее своим.
                """
            ),
            options = listOf(
                option("take_franchise", "Зайти глубоко и купить масштаб вместе с контролем", "🚀", "franchise_result",
                    Effect(capitalDelta = -2_000_000L, debtDelta = 2_500_000L, incomeDelta = 220_000L, expensesDelta = 80_000L, stressDelta = 18, knowledgeDelta = 10)),
                option("skip_franchise", "Не продавать спокойствие ради красивой оболочки", "🛡️", "ecommerce_pivot",
                    Effect(knowledgeDelta = 6, stressDelta = -3)),
                option("negotiate_franchise", "Зайти частично и выторговать меньше зависимости", "🤝", "ecommerce_pivot",
                    Effect(capitalDelta = -1_100_000L, debtDelta = 1_200_000L, incomeDelta = 110_000L, stressDelta = 8, knowledgeDelta = 8))
            )
        ))
        put("franchise_result", event(
            id = "franchise_result",
            flavor = "📈",
            message = story(
                """
                Бизнес становится технологичнее, но и холоднее. Цифры выглядят лучше, процессы прозрачнее, однако Ерболат неожиданно ощущает, что каждое следующее решение теперь требует согласования не только с рынком, но и с чужой моделью.
                """,
                """
                Это полезный, но неприятный урок. Можно перестать тонуть в хаосе и всё равно потерять чувство, что компания принадлежит тебе не только юридически, но и внутренне.
                """,
                """
                Дальше надо выбрать: снова давить в масштаб или сделать бизнес устойчивым, не превращая себя в оператора чужого шаблона.
                """
            ),
            options = listOf(
                option("third_store", "Открыть ещё точку и взять рост числом", "🏬", MONTHLY_TICK,
                    Effect(debtDelta = 3_500_000L, incomeDelta = 280_000L, expensesDelta = 230_000L, stressDelta = 24)),
                option("master_franchise", "Идти в по-настоящему большую игру", "🌍", MONTHLY_TICK,
                    Effect(debtDelta = 10_000_000L, incomeDelta = 520_000L, stressDelta = 32, knowledgeDelta = 14)),
                option("hold_position", "Зафиксировать масштаб и чинить качество жизни", "🧭", MONTHLY_TICK,
                    Effect(stressDelta = -8, knowledgeDelta = 5))
            )
        ))
        put("ecommerce_pivot", event(
            id = "ecommerce_pivot",
            flavor = "💻",
            message = story(
                """
                В 2024-м цифровой поворот уже не выглядит экспериментом. Он выглядит санитарной мерой. Если Ерболат не перестроит бизнес, рынок сам перестроит его без спроса.
                """,
                """
                Гульнара говорит жёстко: нужны маркетплейсы, аналитика, прозрачный учёт, быстрая логистика и нормальная налоговая дисциплина. Для Ерболата это звучит как признание, что харизма предпринимателя больше не заменяет систему.
                """,
                """
                Он может сделать быстрый рывок через готовые платформы, строить собственный канал или снова выбрать перегруз как универсальный язык спасения.
                """
            ),
            options = listOf(
                option("kaspi_marketplace", "Подружиться с маркетплейсом и перестать воевать с настоящим", "🛍️", MONTHLY_TICK,
                    Effect(capitalDelta = -180_000L, incomeDelta = 170_000L, stressDelta = 2, knowledgeDelta = 12,
                        setFlags = setOf("erbolat.digital"))),
                option("own_website", "Сделать собственный канал и сохранить больше контроля", "🌐", MONTHLY_TICK,
                    Effect(capitalDelta = -550_000L, incomeDelta = 120_000L, stressDelta = 12, knowledgeDelta = 16,
                        setFlags = setOf("erbolat.digital"))),
                option("both_channels", "Снова взяться за всё сразу", "⚡", MONTHLY_TICK,
                    Effect(capitalDelta = -800_000L, incomeDelta = 260_000L, stressDelta = 26, knowledgeDelta = 18,
                        setFlags = setOf("erbolat.digital")))
            )
        ))
        put("supplier_scam", commonSupplierScamEvent())
        commonErbolatHubAndEndings("2024")
    }

    private fun commonSupplierScamEvent() = event(
        id = "supplier_scam",
        flavor = "⚠️",
        message = story(
            """
            Новый поставщик предлагает слишком хорошие условия. Именно это и настораживает. У Ерболата за годы бизнеса выработалась интуиция, но он знает и её слабость: когда денег не хватает, подозрения начинают казаться роскошью.
            """,
            """
            Контракт выглядит прилично, менеджер отвечает быстро, цена почти неприлично выгодная. Всё в этой сделке будто бы специально настроено так, чтобы предприниматель устал сомневаться.
            """,
            """
            Можно рискнуть оборотными средствами, потратить деньги на проверку или отказаться и дальше жить с мыслью, что, возможно, упустил шанс.
            """
        ),
        options = listOf(
            option("pay_supplier", "Перевести деньги и надеяться, что удача всё ещё любит смелых", "💸", "supplier_result",
                Effect(capitalDelta = -800_000L, stressDelta = 10)),
            option("check_supplier", "Заплатить за проверку и купить себе ясность", "🔍", "supplier_safe",
                Effect(capitalDelta = -40_000L, knowledgeDelta = 8)),
            option("skip_supplier", "Не заходить в слишком сладкую историю", "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 5))
        )
    )

    private fun MutableMap<String, GameEvent>.commonErbolatHubAndEndings(eraLabel: String) {
        put("supplier_result", event(
            id = "supplier_result",
            flavor = "💀",
            message = story(
                """
                Груз не приходит. Телефон выключен. Деньги ушли туда, где у них больше нет имени. Для предпринимателя это не просто потеря суммы, а унижение: тебя поймали на твоей же усталости и жадности до облегчения.
                """,
                """
                Ерболат злится прежде всего на себя. Не за то, что рискнул, а за то, что рискнул без структуры. В этом эпизоде рушатся не только деньги, но и привычное чувство контроля над ситуацией.
                """,
                """
                Теперь ему нужно либо жёстко принять урок и перестраиваться, либо тратить ещё силы на попытку вернуть то, что уже, возможно, исчезло.
                """
            ),
            options = listOf(
                option("cut_losses", "Переварить потерю и идти чинить бизнес дальше", "💪", "ecommerce_pivot",
                    Effect(stressDelta = 24, knowledgeDelta = 10)),
                option("sue_supplier", "Судиться, даже если шансы малы", "⚖️", MONTHLY_TICK,
                    Effect(capitalDelta = -60_000L, stressDelta = 15, knowledgeDelta = 5))
            )
        ))
        put("supplier_safe", event(
            id = "supplier_safe",
            flavor = "✅",
            message = story(
                """
                Юрист смотрит бумаги всего несколько минут и сразу говорит то, что Ерболат уже, кажется, знал внутри: это пустышка. На этот раз его спасли не нюх и не удача, а дисциплина.
                """,
                """
                Для него это почти новый опыт. Проверка кажется дорогой только до тех пор, пока не посчитаешь цену ошибки. После этого она выглядит самой дешёвой инвестицией месяца.
                """,
                """
                Теперь можно идти через проверенных людей или снова пытаться выторговать рынок в одиночку.
                """
            ),
            options = listOf(
                option("find_reliable", "Оплатить надёжность и укрепить операционку", "🤝", MONTHLY_TICK,
                    Effect(incomeDelta = 60_000L, stressDelta = -10, knowledgeDelta = 5)),
                option("search_yourself", "Искать самому, но уже не вслепую", "🔍", MONTHLY_TICK,
                    Effect(knowledgeDelta = 8))
            )
        ))
        put("normal_life", event(
            id = "normal_life",
            flavor = "📋",
            poolWeight = 20,
            message = story(
                """
                Очередной операционный месяц. Зарплаты, аренда, поставщики, возвраты, налоговые вопросы, семейные разговоры, усталость, которая уже почти стала фоном. Бизнес живёт не только большими решениями, а и бесконечной чередой мелких управленческих поступков.
                """,
                """
                Ерболат давно понял: если он не задаёт деньгам направление, они сами находят для себя самый шумный и неэффективный выход.
                """,
                """
                Свободный ресурс можно пустить в снижение долга, рост трафика, товар или собственную управленческую голову.
                """
            ),
            options = listOf(
                option("pay_debt", "Погасить часть долга и купить себе чуть больше сна", "💳", MONTHLY_TICK,
                    Effect(capitalDelta = -250_000L, debtDelta = -250_000L, stressDelta = -6, knowledgeDelta = 2)),
                option("marketing", "Купить внимание рынка, а не ждать его милости", "📣", MONTHLY_TICK,
                    Effect(capitalDelta = -120_000L, incomeDelta = 90_000L, knowledgeDelta = 5)),
                option("inventory", "Вложиться в ассортимент и надеяться на сезон", "📦", MONTHLY_TICK,
                    Effect(capitalDelta = -320_000L, incomeDelta = 130_000L, stressDelta = 5)),
                option("finance_course", "Научиться управлять взрослее, чем раньше", "🎓", MONTHLY_TICK,
                    Effect(capitalDelta = -60_000L, knowledgeDelta = 12, stressDelta = -4))
            )
        ))
        put("ending_empire", event(
            id = "ending_empire",
            flavor = "🏆",
            isEnding = true,
            endingType = EndingType.WEALTH,
            message = story(
                """
                Ерболат сумел пройти путь от человека, который тушил операционные пожары своей нервной системой, до владельца настоящей системы. Бизнес перестал держаться на его личной панике и начал работать как структура.
                """,
                """
                Он по-прежнему предприниматель, но уже не человек, который обязан лично быть во всех местах сразу. Это главное богатство, пришедшее вместе с цифрами: вернуть себе собственную жизнь, а не только умножить активы.
                """,
                """
                Эпоха $eraLabel осталась позади. Капитал {capital}. Теперь масштаб не пожирает его, а подчиняется ему.
                """
            ),
            options = emptyList()
        ))
        put("ending_pivot_success", event(
            id = "ending_pivot_success",
            flavor = "💻",
            isEnding = true,
            endingType = EndingType.FINANCIAL_FREEDOM,
            message = story(
                """
                Самая взрослая победа Ерболата оказалась не в том, чтобы открыть как можно больше точек, а в том, чтобы вовремя отпустить старую идентичность. Он перевёл бизнес в цифровую модель и перестал путать объём с качеством жизни.
                """,
                """
                Доход стал устойчивее, долговая удавка ослабла, семья снова видит дома человека, а не только перевозбуждённый центр управления.
                """,
                """
                Капитал {capital}. Свобода пришла к нему в форме ясности и управляемости.
                """
            ),
            options = emptyList()
        ))
        put("ending_stable_business", event(
            id = "ending_stable_business",
            flavor = "🏪",
            isEnding = true,
            endingType = EndingType.FINANCIAL_STABILITY,
            message = story(
                """
                Ерболат не взял рынок штурмом, но собрал для семьи куда более редкую вещь: бизнес, который не разваливается от каждого плохого месяца. Иногда именно это и есть зрелое предпринимательство.
                """,
                """
                Он больше не живёт в режиме перманентной тревоги. Компания работает, долг под контролем, дом перестал быть филиалом офиса.
                """,
                """
                Капитал {capital}. Это не легенда о стремительном взлёте, а история о честно отвоёванной устойчивости.
                """
            ),
            options = emptyList()
        ))
        put("ending_bankruptcy", event(
            id = "ending_bankruptcy",
            flavor = "💀",
            isEnding = true,
            endingType = EndingType.BANKRUPTCY,
            message = story(
                """
                Самое страшное в банкротстве для Ерболата не потеря магазинов. Самое страшное — признать, что всё это время он мерил себя только способностью удерживать и тащить. Когда бизнес падает, вместе с ним падает и эта старая, жёсткая версия мужественности.
                """,
                """
                Но именно на руинах у него появляется шанс понять себя не как совокупность активов, а как человека, который всё ещё может построить новую жизнь уже без прежних иллюзий.
                """,
                """
                Капитал истощён, долг задавил конструкцию. История закончилась больно, но окончательно ли — зависит уже не от рынка.
                """
            ),
            options = emptyList()
        ))
        put("ending_paycheck", event(
            id = "ending_paycheck",
            flavor = "😰",
            isEnding = true,
            endingType = EndingType.PAYCHECK_TO_PAYCHECK,
            message = story(
                """
                Бизнес ещё жив, но живёт как человек после тяжёлой болезни: двигается, дышит, но сил на новый рывок нет. Каждый месяц уходит на то, чтобы просто не упасть.
                """,
                """
                Ерболат всё ещё предприниматель на бумаге, но внутренне давно работает скорее на обязательства, чем на мечту. У такого состояния нет драмы большого краха, зато есть вязкая усталость ежедневного выживания.
                """,
                """
                Долг {debt}, капитал {capital}. Не катастрофа, но и не та жизнь, ради которой всё начиналось.
                """
            ),
            options = emptyList()
        ))
    }

    private fun commonErbolatConditionals(
        burnoutIntro: String,
        financeIntro: String
    ): List<GameEvent> = listOf(
        event(
            id = "debt_crisis",
            priority = 10,
            flavor = "🚨",
            conditions = listOf(cond(DEBT, GT, 3_000_000L), cond(CAPITAL, LTE, 700_000L)),
            message = story(
                """
                Банк перестаёт разговаривать уважительно. Когда денег становится мало, любой партнёр очень быстро вспоминает, кто на самом деле слабее в переговорах.
                """,
                """
                Ерболат ненавидит этот момент, потому что здесь уже не работают уверенный голос и предпринимательский темперамент. Работают только цифры, а цифры упрямее любого характера.
                """,
                """
                Долг {debt}, капитал {capital}. Надо выбирать, что именно резать: масштаб, долю, самолюбие или будущий доход.
                """
            ),
            options = listOf(
                option("sell_store", "Продать часть бизнеса, чтобы спасти остальное", "🔒", MONTHLY_TICK,
                    Effect(capitalDelta = 1_500_000L, debtDelta = -1_500_000L, incomeDelta = -280_000L, expensesDelta = -180_000L, stressDelta = 10)),
                option("bank_restructure", "Идти в реструктуризацию и покупать себе время", "🏦", MONTHLY_TICK,
                    Effect(expensesDelta = -50_000L, stressDelta = 20, knowledgeDelta = 8)),
                option("find_investor", "Впустить инвестора и поделиться контролем", "🤝", MONTHLY_TICK,
                    Effect(capitalDelta = 2_000_000L, stressDelta = 14, knowledgeDelta = 10))
            )
        ),
        event(
            id = "burnout_warning",
            priority = 8,
            flavor = "😮‍💨",
            conditions = listOf(cond(STRESS, GTE, 82L)),
            message = story(
                burnoutIntro,
                """
                Для Ерболата отдых долгое время казался слабостью. Теперь врач, жена и собственное тело говорят одно и то же: если не остановиться, бизнес получит владельца, который всё ещё на месте, но уже не принимает внятных решений.
                """,
                """
                Пришло время признать, что делегирование и пауза тоже бывают формами силы.
                """
            ),
            options = listOf(
                option("family_vacation", "Вырвать себя из режима тревоги хотя бы на две недели", "✈️", MONTHLY_TICK,
                    Effect(capitalDelta = -300_000L, stressDelta = -38)),
                option("delegate", "Нанять управляющего и купить себе воздух", "👤", MONTHLY_TICK,
                    Effect(expensesDelta = 150_000L, stressDelta = -24, knowledgeDelta = 8))
            )
        ),
        event(
            id = "investment_unlock",
            priority = 5,
            flavor = "💡",
            unique = true,
            conditions = listOf(cond(KNOWLEDGE, GTE, 52L)),
            message = story(
                financeIntro,
                """
                Это неприятный, но важный момент взросления бизнеса: признать, что предпринимательская интуиция не обязана решать всё в одиночку. Инструменты, процессы и финансовая дисциплина не убивают хватку, а защищают её от хаоса.
                """,
                """
                Ерболат может использовать новый инструмент сейчас или продолжить управлять так, как привык раньше.
                """
            ),
            options = listOf(
                option("use_factoring", "Подключить инструмент и вернуть бизнесу манёвренность", "⚡", MONTHLY_TICK,
                    Effect(capitalDelta = 500_000L, expensesDelta = 10_000L, knowledgeDelta = 5)),
                option("skip_factoring", "Остаться в старой манере управления", "🛡️", MONTHLY_TICK,
                    Effect())
            )
        ),
        event(
            id = "ending_bankruptcy_trigger",
            priority = 100,
            conditions = listOf(cond(CAPITAL, LTE, 0L), cond(STRESS, GTE, 90L)),
            message = "Все способы оттянуть удар закончились.",
            options = listOf(option("claim_bankruptcy", "Признать банкротство", "💀", "ending_bankruptcy"))
        ),
        event(
            id = "ending_wealth_trigger",
            priority = 2,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 18_000_000L)),
            message = "Бизнес вырос настолько, что перестал питаться исключительно нервной системой владельца.",
            options = listOf(option("claim_empire", "Забрать большую концовку", "🏆", "ending_empire"))
        ),
        event(
            id = "ending_freedom_trigger",
            priority = 3,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 9_000_000L), Condition.HasFlag("erbolat.digital")),
            message = "Перестройка бизнеса наконец начала работать на Ерболата, а не только требовать от него всё новые силы.",
            options = listOf(option("claim_pivot", "Войти в цифровую свободу", "💻", "ending_pivot_success"))
        ),
        event(
            id = "ending_stability_trigger",
            priority = 4,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 3_500_000L), cond(STRESS, LTE, 55L)),
            message = "Штормы не исчезли, но бизнес перестал тонуть от каждого из них.",
            options = listOf(option("claim_stability", "Зафиксировать стабильный бизнес", "🏪", "ending_stable_business"))
        ),
        event(
            id = "ending_paycheck_trigger",
            priority = 1,
            unique = true,
            conditions = listOf(cond(DEBT, GT, 2_000_000L), cond(CAPITAL, LTE, 300_000L)),
            message = "Компания всё ещё работает, но почти весь её смысл ушёл в обслуживание обязательств.",
            options = listOf(option("claim_paycheck", "Признать режим выживания", "😰", "ending_paycheck"))
        )
    )
}
