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

class DanaScenarioGraph(private val eraId: String = "kz_2024") : ScenarioGraph() {

    override val initialPlayerState = when (eraId) {
        "kz_2005" -> PlayerState(
            capital = 320_000L,
            income = 110_000L,
            expenses = 95_000L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            investments = 0L,
            investmentReturnRate = 0.05,
            stress = 42,
            financialKnowledge = 18,
            riskLevel = 8,
            month = 1,
            year = 2005,
            characterId = "dana",
            eraId = eraId
        )
        "kz_2015" -> PlayerState(
            capital = 900_000L,
            income = 260_000L,
            expenses = 225_000L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            investments = 0L,
            investmentReturnRate = 0.06,
            stress = 44,
            financialKnowledge = 22,
            riskLevel = 10,
            month = 1,
            year = 2015,
            characterId = "dana",
            eraId = eraId
        )
        else -> PlayerState(
            capital = 1_100_000L,
            income = 390_000L,
            expenses = 310_000L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            investments = 0L,
            investmentReturnRate = 0.06,
            stress = 46,
            financialKnowledge = 26,
            riskLevel = 12,
            month = 1,
            year = 2024,
            characterId = "dana",
            eraId = eraId
        )
    }

    override val events: Map<String, GameEvent> = when (eraId) {
        "kz_2005" -> dana2005Events()
        "kz_2015" -> dana2015Events()
        else -> dana2024Events()
    }

    override val conditionalEvents: List<GameEvent> = when (eraId) {
        "kz_2005" -> commonDanaConditionals(
            burnoutIntro = "После уроков у Даны дрожит голос. Она всё ещё улыбается детям, но дома молчит дольше обычного.",
            investmentIntro = "Первые разговоры о накоплениях звучат почти неловко: в её мире деньги обычно либо тратят, либо прячут на всякий случай."
        )
        "kz_2015" -> commonDanaConditionals(
            burnoutIntro = "День разделён на роли без пауз: школа, семья, ученики онлайн, тревога за курс, тревога за завтра.",
            investmentIntro = "После девальвации Дана впервые всерьёз понимает, что осторожность без знаний не всегда спасает деньги."
        )
        else -> commonDanaConditionals(
            burnoutIntro = "Даже любимая работа начинает звучать как непрерывный фон. Сообщения от родителей учеников не заканчиваются вместе с уроками.",
            investmentIntro = "У Даны появляется редкое чувство контроля: она уже не просто экономит, а учится делать деньги частью осмысленного плана."
        )
    }

    override val eventPool: List<PoolEntry> = listOf(
        PoolEntry("normal_life", 18),
        PoolEntry("husband_layoff", 8),
        PoolEntry("child_education", 7)
    ) + ScamEventLibrary.poolEntries

    private fun dana2005Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "👩‍🏫",
            message = story(
                """
                2005 год. Дана идёт домой после школы по сырому весеннему двору и считает не шаги, а ближайшие месяцы. Муж работает руками, она держит в руках чужие тетради, а их собственная жизнь всё ещё помещается в съёмную квартиру с тонкими стенами.
                """,
                """
                Ей тридцать два, и она слишком часто слышит фразу «надо потерпеть ещё немного». Терпение стало почти семейной профессией. Но внутри Даны живёт упрямая математика: если каждый месяц всё расписано до копейки, значит однажды можно рассчитать и выход.
                """,
                """
                На счету {capital}. Доход {income}. Денег хватает только на аккуратную жизнь без права на внезапность. Коллега рассказывает про частных учеников, а соседка снова шепчет, что «своё жильё само себя не купит».
                """
            ),
            options = listOf(
                option("start_tutoring", "Попробовать репетиторство и выкрасть для семьи ещё один шанс", "✏️", "tutoring_platform",
                    Effect(knowledgeDelta = 3)),
                option("explore_mortgage", "Сначала понять, как вообще выглядит путь к своей квартире", "🏠", "mortgage_decision",
                    Effect(knowledgeDelta = 5))
            )
        ))
        put("tutoring_platform", event(
            id = "tutoring_platform",
            flavor = "📚",
            message = story(
                """
                Первый ученик приходит по рекомендации, второй приводит двоих cousins, третий уже платит без торга. Репетиторство не выглядит как большой перелом, но именно такие тихие прибавки иногда меняют судьбу семьи сильнее, чем громкие обещания государства.
                """,
                """
                Дана чувствует себя виноватой уже заранее. Каждый вечерний урок для неё одновременно и облегчение, и потерянное время с дочерью. Деньги приходят правильные, но не бесплатные.
                """,
                """
                Можно усилить это направление, оставить его маленькой подпоркой или свернуть, пока жизнь не стала состоять только из чужих ожиданий.
                """
            ),
            options = listOf(
                option("join_platform", "Встроить репетиторство в жизнь серьёзно", "💻", "tutoring_growth",
                    Effect(incomeDelta = 35_000L, stressDelta = 10, knowledgeDelta = 5)),
                option("skip_platform", "Оставить силы дому и не дробить себя", "👧", "mortgage_decision",
                    Effect(stressDelta = -5))
            )
        ))
        put("tutoring_growth", event(
            id = "tutoring_growth",
            flavor = "🪜",
            message = story(
                """
                Через несколько месяцев у Даны уже сложилась маленькая вечерняя школа на кухне. Тетради лежат стопками, чай остывает, а за столом регулярно появляются чужие дети, которым она даёт не только знания, но и очень нужное ощущение, что взрослый рядом собран.
                """,
                """
                Один из родителей предлагает неожиданное: арендовать маленький кабинет и собрать свою группу. Это больше денег и больше риска. Для Даны это также первый в жизни шанс не просто подрабатывать, а строить что-то своё, не переставая быть собой.
                """,
                """
                Вопрос не в том, способна ли она. Вопрос в том, готова ли семья жить с этой новой нагрузкой.
                """
            ),
            options = listOf(
                option("launch_school", "Открыть маленький учебный кружок и рискнуть ростом", "🏫", MONTHLY_TICK,
                    Effect(capitalDelta = -120_000L, incomeDelta = 70_000L, stressDelta = 18, knowledgeDelta = 8,
                        setFlags = setOf("dana.school.started"))),
                option("stay_quiet", "Оставить всё камерным и безопасным", "🛡️", "mortgage_decision",
                    Effect(stressDelta = -4, knowledgeDelta = 4))
            )
        ))
        put("mortgage_decision", event(
            id = "mortgage_decision",
            flavor = "🏡",
            message = story(
                """
                Разговор о квартире снова всплывает вечером на кухне. Не как мечта, а как почти физическая усталость от чужих стен, чужих правил, хозяйки, которая может поднять аренду именно тогда, когда ребёнку нужна новая куртка.
                """,
                """
                Дана считает цифры и понимает: в 2005-м собственное жильё для них пока больше похоже на долгий маршрут, чем на одно решение. Но именно сейчас можно выбрать направление: копить агрессивнее, рискнуть кредитом или признать, что семье пока важнее ликвидность.
                """,
                """
                Ошибка здесь будет стоить не только денег. Она ударит по ощущению безопасности, которое Дана строит для близких почти в одиночку.
                """
            ),
            options = listOf(
                option("take_mortgage", "Взять кредит раньше, чем стало удобно", "🔑", "child_education",
                    Effect(capitalDelta = -1_000_000L, debtDelta = 4_500_000L, expensesDelta = 55_000L, stressDelta = 18, knowledgeDelta = 6,
                        setFlags = setOf("dana.has.mortgage"))),
                option("save_more", "Копить ещё и не пускать банк слишком близко", "⏳", "child_education",
                    Effect(knowledgeDelta = 5, stressDelta = -4)),
                option("rent_forever", "Пока остаться в съёме и беречь гибкость", "📦", "child_education",
                    Effect(investmentsDelta = 20_000L, capitalDelta = -20_000L, knowledgeDelta = 5))
            )
        ))
        put("child_education", commonChildEducationEvent(
            """
            Дочь подрастает, и Дана впервые чувствует, как все её финансовые решения складываются в один большой вопрос: какое детство они могут ей позволить не на словах, а на практике.
            """.trimIndent()
        ))
        commonDanaPoolAndEndings("2005")
    }

    private fun dana2015Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "📱",
            message = story(
                """
                2015 год. В ленте новостей спорят о курсе тенге, а дома у Даны спорят о том, брать ли ипотеку, пока цены не убежали ещё дальше. Она всё так же учит детей математике, только теперь мир вокруг словно сам стал задачей с постоянно меняющимися условиями.
                """,
                """
                Дана привыкла жить осторожно, но осторожность начинает уставать. Когда валюта дёргается, цены ползут вверх, а у ребёнка впереди школа, простая аккуратность уже не кажется достаточной стратегией.
                """,
                """
                У неё {capital} сбережений, доход {income} и привычка держать семью в руках даже тогда, когда руки сами дрожат. Перед ней два пути: попробовать превратить знания в дополнительный доход или ещё сильнее упереться в идею собственной квартиры.
                """
            ),
            options = listOf(
                option("start_tutoring", "Сделать репетиторство реальным вторым доходом", "💻", "tutoring_platform",
                    Effect(knowledgeDelta = 3)),
                option("explore_mortgage", "Разобраться с ипотекой, пока реальность снова не изменилась", "🏠", "mortgage_decision",
                    Effect(knowledgeDelta = 5))
            )
        ))
        put("tutoring_platform", event(
            id = "tutoring_platform",
            flavor = "💻",
            message = story(
                """
                Онлайн-платформы обещают новый рынок для учителей. Дане это кажется одновременно удобным и чуть унизительным: теперь ей нужно продавать не только знания, но и саму себя как услугу, отвечать быстро, писать красиво, вести расписание без права на слабость.
                """,
                """
                И всё же в этом есть надежда. Впервые дополнительный доход может не зависеть от прихоти директора школы. Впервые её профессионализм способен приносить деньги прямо ей.
                """,
                """
                Остаётся решить, хочет ли она открыть эту дверь по-настоящему или оставить её в приоткрытом состоянии.
                """
            ),
            options = listOf(
                option("join_platform", "Зайти всерьёз и построить себе вторую опору", "✅", "tutoring_growth",
                    Effect(capitalDelta = -15_000L, incomeDelta = 60_000L, stressDelta = 14, knowledgeDelta = 6)),
                option("skip_platform", "Не разрывать вечера и сохранить силы семье", "👧", "mortgage_decision",
                    Effect(stressDelta = -5))
            )
        ))
        put("tutoring_growth", event(
            id = "tutoring_growth",
            flavor = "📈",
            message = story(
                """
                Платформа сработала лучше ожиданий. Ученики приходят стабильно, родители благодарят, а Дана впервые видит, как знания превращаются не просто в уважение, а в реальные деньги, которые можно направить на будущее.
                """,
                """
                Возникает новая развилка. Один родитель предлагает собрать мини-группу онлайн и сделать курс. Это уже почти маленькое дело. Для Даны, привыкшей быть осторожной, такой шаг звучит дерзко и почти неправильно.
                """,
                """
                Но иногда собственная смелость начинается именно с того, что перестаёшь считать себя человеком только для стабильности.
                """
            ),
            options = listOf(
                option("launch_school", "Собрать первую онлайн-группу и рискнуть ростом", "🎥", MONTHLY_TICK,
                    Effect(capitalDelta = -180_000L, incomeDelta = 95_000L, stressDelta = 18, knowledgeDelta = 10,
                        setFlags = setOf("dana.school.started"))),
                option("stay_quiet", "Оставить всё в формате спокойной подработки", "🛡️", "mortgage_decision",
                    Effect(stressDelta = -3, knowledgeDelta = 3))
            )
        ))
        put("mortgage_decision", event(
            id = "mortgage_decision",
            flavor = "🏠",
            message = story(
                """
                Банк улыбается в рекламных буклетах, будто ипотека это всего лишь взрослая форма мечты. Дана же видит другое: долгие годы платежей, зависимость от одного-двух доходов и страх перед любым кризисом, который может прийти не по расписанию.
                """,
                """
                При этом съёмная квартира всё сильнее ощущается как временная жизнь. Ребёнку нужна устойчивость. Ей самой хочется наконец перестать объяснять дочери, почему нельзя рисовать на этих обоях, потому что они не их.
                """,
                """
                Решение придётся принять не сердцем и не только логикой. Придётся принять его всем телом, которое потом будет жить с этим долгом или без него.
                """
            ),
            options = listOf(
                option("take_mortgage", "Взять ипотеку и закрепить семью в своём доме", "🔑", "child_education",
                    Effect(capitalDelta = -3_000_000L, debtDelta = 12_000_000L, expensesDelta = 95_000L, stressDelta = 24, knowledgeDelta = 8,
                        setFlags = setOf("dana.has.mortgage"))),
                option("save_more", "Пока копить и держать пространство для манёвра", "⏳", "child_education",
                    Effect(knowledgeDelta = 6, stressDelta = -4)),
                option("rent_forever", "Снимать и направлять разницу в более гибкое будущее", "📊", "child_education",
                    Effect(capitalDelta = -60_000L, investmentsDelta = 60_000L, knowledgeDelta = 8))
            )
        ))
        put("child_education", commonChildEducationEvent(
            """
            Время выбирать не только школу, а общую модель жизни семьи: платить больше сейчас ради уверенности ребёнка или признать, что любовь родителей не всегда измеряется самой дорогой опцией.
            """.trimIndent()
        ))
        commonDanaPoolAndEndings("2015")
    }

    private fun dana2024Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "🎙️",
            message = story(
                """
                2024 год. Дана уже давно не просто учительница, а человек, которого родители учеников советуют друг другу шёпотом: «если хотите, чтобы ребёнок не боялся математики, идите к ней». Но репутация не всегда превращается в лёгкую жизнь.
                """,
                """
                Днём школа, вечером онлайн-занятия, ночью тревога о том, что хороших людей в образовании просят работать на износ. Она любит своё дело, но всё чаще задаёт себе вопрос: можно ли сохранить смысл работы и при этом перестать постоянно жить на пределе.
                """,
                """
                У неё {capital}, доход {income}, семья, которая держится на её собранности, и шанс превратить преподавание в edtech-направление. Но у любого роста есть цена: временем, нервами, вниманием к близким.
                """
            ),
            options = listOf(
                option("start_tutoring", "Перевести талант в современный онлайн-формат", "💻", "tutoring_platform",
                    Effect(knowledgeDelta = 4)),
                option("explore_mortgage", "Сначала довести до ума вопрос дома и базовой опоры", "🏠", "mortgage_decision",
                    Effect(knowledgeDelta = 5))
            )
        ))
        put("tutoring_platform", event(
            id = "tutoring_platform",
            flavor = "📲",
            message = story(
                """
                Платформа просит снять приветственное видео, оформить профиль, отвечать быстро и говорить о себе уверенно. Для кого-то это мелочь, для Даны почти новая профессия. Теперь мало быть хорошим учителем, нужно ещё уметь не стесняться своей ценности.
                """,
                """
                Первые ученики приходят быстро, и вместе с ними приходит новая мысль: если построить систему, можно перестать продавать только свои вечера и начать продавать метод. Это уже похоже на маленький бизнес.
                """,
                """
                Она может сделать шаг в сторону edtech, а может оставить всё как дополнительный, но управляемый доход.
                """
            ),
            options = listOf(
                option("join_platform", "Запустить серьёзное онлайн-направление", "🚀", "tutoring_growth",
                    Effect(capitalDelta = -30_000L, incomeDelta = 80_000L, stressDelta = 16, knowledgeDelta = 8)),
                option("skip_platform", "Оставить преподавание в тёплом камерном формате", "🌿", "mortgage_decision",
                    Effect(stressDelta = -6))
            )
        ))
        put("tutoring_growth", event(
            id = "tutoring_growth",
            flavor = "🎥",
            message = story(
                """
                Онлайн-группы растут быстрее, чем Дана успевает привыкнуть к своей новой роли. Её уроки покупают не только за знания, но и за то спокойствие, которое она умеет создавать. Это редкий талант, и рынок внезапно это заметил.
                """,
                """
                Один знакомый продюсер предлагает собрать полноценный курс, нанять помощника и перестать завязывать доход только на её личное время. Дана слышит в этом и шанс, и угрозу: не размоется ли главное, если всё это станет слишком большим.
                """,
                """
                Выбор теперь не между деньгами и смыслом, а между разными способами сохранить смысл, не обрекая себя на хроническое выгорание.
                """
            ),
            options = listOf(
                option("launch_school", "Собрать мини-команду и превратить опыт в систему", "🏫", MONTHLY_TICK,
                    Effect(capitalDelta = -260_000L, incomeDelta = 140_000L, stressDelta = 20, knowledgeDelta = 12,
                        setFlags = setOf("dana.school.started"))),
                option("stay_quiet", "Оставить всё на личном качестве и не расти через силу", "🛡️", "mortgage_decision",
                    Effect(stressDelta = -5, knowledgeDelta = 4))
            )
        ))
        put("mortgage_decision", event(
            id = "mortgage_decision",
            flavor = "🔑",
            message = story(
                """
                Дом в 2024-м для Даны означает не статус, а право наконец жить без внутреннего «если что, нас попросят съехать». После пандемий, скачков цен и общего ощущения нестабильности это право стало почти базовой мечтой.
                """,
                """
                Но она уже достаточно взрослая, чтобы не путать мечту с обязательством. Ипотека может дать стенам имена, но вместе с этим забрать гибкость, которой семья пока всё ещё пользуется как страховкой от неожиданностей.
                """,
                """
                Перед ней снова та же взрослая математика: сколько можно себе позволить так, чтобы дом не стал новой формой тревоги.
                """
            ),
            options = listOf(
                option("take_mortgage", "Взять ипотеку и перевести семью в своё пространство", "🏡", "child_education",
                    Effect(capitalDelta = -3_400_000L, debtDelta = 14_500_000L, expensesDelta = 120_000L, stressDelta = 22, knowledgeDelta = 8,
                        setFlags = setOf("dana.has.mortgage"))),
                option("save_more", "Подождать ещё и купить себе больше воздуха", "⏳", "child_education",
                    Effect(knowledgeDelta = 6, stressDelta = -5)),
                option("rent_forever", "Оставить аренду и ставить на ликвидность и инвестиции", "📈", "child_education",
                    Effect(capitalDelta = -100_000L, investmentsDelta = 100_000L, knowledgeDelta = 9))
            )
        ))
        put("child_education", commonChildEducationEvent(
            """
            Теперь образование дочери для Даны не просто расход. Это зеркало всего, что она строила столько лет: выдержит ли их семейная система следующий уровень ответственности.
            """.trimIndent()
        ))
        commonDanaPoolAndEndings("2024")
    }

    private fun commonChildEducationEvent(introParagraph: String) = event(
        id = "child_education",
        flavor = "👧",
        message = story(
            introParagraph,
            """
            Дана хорошо знает, что ребёнку важны не только школа и кружки. Но она также знает цену среды. Иногда она чувствует вину уже от одного факта, что приходится выбирать между хорошим и возможным.
            """,
            """
            Семейный бюджет надо защитить, не превратив ребёнка в вечное оправдание любого расхода. Это тонкая и болезненная граница.
            """
        ),
        options = listOf(
            option("private_school", "Платить больше ради сильной среды и спокойствия", "🎓", MONTHLY_TICK,
                Effect(expensesDelta = 80_000L, stressDelta = 10, knowledgeDelta = 2)),
            option("state_tutor", "Оставить госшколу, но добавить индивидуальную поддержку", "✏️", MONTHLY_TICK,
                Effect(expensesDelta = 35_000L, stressDelta = 5, knowledgeDelta = 3)),
            option("state_school", "Держать базу под контролем и не рвать бюджет", "🏫", MONTHLY_TICK,
                Effect(stressDelta = -3))
        )
    )

    private fun MutableMap<String, GameEvent>.commonDanaPoolAndEndings(eraLabel: String) {
        put("husband_layoff", event(
            id = "husband_layoff",
            flavor = "😰",
            poolWeight = 10,
            tags = setOf("family", "crisis"),
            message = story(
                """
                Вечером муж садится напротив и долго молчит, прежде чем произнести главное: работу сократили. Именно такие фразы меняют температуру в доме сильнее любых холодов.
                """,
                """
                Для Даны это означает не только минус один доход. Это означает, что её собственная собранность снова становится центральной несущей конструкцией семьи, и от этого уже почти физически болят плечи.
                """,
                """
                Можно резко затянуть пояс, жить на запасе или превратить её работу в ещё более тяжёлую, но доходную ношу.
                """
            ),
            options = listOf(
                option("cut_expenses", "Резать расходы раньше, чем паника сделает это за них", "✂️", MONTHLY_TICK,
                    Effect(expensesDelta = -60_000L, stressDelta = 14, knowledgeDelta = 5)),
                option("use_savings", "Дать семье дышать за счёт накоплений", "🐷", MONTHLY_TICK,
                    Effect(capitalDelta = -180_000L, stressDelta = 8)),
                option("more_tutoring", "Взять на себя ещё больше учеников", "💪", MONTHLY_TICK,
                    Effect(incomeDelta = 55_000L, stressDelta = 22))
            )
        ))
        put("normal_life", event(
            id = "normal_life",
            flavor = "🍵",
            poolWeight = 20,
            message = story(
                """
                Внешне месяц проходит спокойно: уроки, тетради, продукты, семейные разговоры, редкие тихие вечера. Но именно в такие месяцы Дана и строит будущее, почти незаметно для окружающих.
                """,
                """
                Её жизнь давно состоит из решений, которые не выглядят героическими. И всё же именно они определяют, будет ли семья жить в постоянной тревоге или постепенно соберёт себе устойчивость.
                """,
                """
                На этот раз свободные деньги можно направить в запас, знания, консервативные инвестиции или просто в восстановление семьи.
                """
            ),
            options = listOf(
                option("family_savings", "Положить лишнее в семейную подушку", "🐷", MONTHLY_TICK,
                    Effect(stressDelta = -4, knowledgeDelta = 2)),
                option("read_finance", "Понять деньги глубже, чтобы меньше их бояться", "📖", MONTHLY_TICK,
                    Effect(knowledgeDelta = 7, stressDelta = -2)),
                option("invest_conservative", "Выбрать осторожный инструмент вместо пустого ожидания", "🏦", MONTHLY_TICK,
                    Effect(capitalDelta = -40_000L, investmentsDelta = 40_000L, knowledgeDelta = 4)),
                option("family_trip", "Потратить на близость, а не только на устойчивость", "🌿", MONTHLY_TICK,
                    Effect(capitalDelta = -25_000L, stressDelta = -15))
            )
        ))
        put("ending_own_school", event(
            id = "ending_own_school",
            flavor = "🏆",
            isEnding = true,
            endingType = EndingType.WEALTH,
            message = story(
                """
                Дана построила не просто доходный проект, а пространство, где дети перестают бояться математики, а взрослые наконец видят в её профессии не «призвание вопреки», а настоящий капитал.
                """,
                """
                Она долго боялась, что рост отнимет у неё смысл. Но оказалось наоборот: когда хаоса стало меньше, сил на настоящее преподавание стало больше. Теперь её работа не выжигает её, а расширяет.
                """,
                """
                Эпоха $eraLabel осталась позади. Капитал {capital}. И самое важное: у семьи больше нет ощущения, что устойчивость каждый месяц приходится буквально выцарапывать.
                """
            ),
            options = emptyList()
        ))
        put("ending_stable_family", event(
            id = "ending_stable_family",
            flavor = "❤️",
            isEnding = true,
            endingType = EndingType.FINANCIAL_STABILITY,
            message = story(
                """
                Дана не стала человеком из деловых журналов. Она стала человеком, который сумел сделать дом местом покоя, а не постоянной тревоги. Иногда это и есть самая редкая форма победы.
                """,
                """
                В её жизни всё ещё много дел, но стало меньше страха. Ребёнок растёт в предсказуемости, отношения больше не держатся на постоянной экономической панике, а деньги наконец перестали диктовать интонацию каждого разговора.
                """,
                """
                Капитал {capital}. Устойчивость получилась не роскошной, а честной. И именно поэтому она так дорога.
                """
            ),
            options = emptyList()
        ))
        put("ending_freedom", event(
            id = "ending_freedom",
            flavor = "🌅",
            isEnding = true,
            endingType = EndingType.FINANCIAL_FREEDOM,
            message = story(
                """
                Однажды Дана ловит себя на простой мысли: теперь можно работать потому, что любит, а не потому, что страшно остановиться. Это новое, почти непривычное состояние.
                """,
                """
                Она всё ещё учит, но делает это без внутренней дрожи за каждый следующий месяц. Доход от системы и капитала закрывает базовые расходы, а значит больше не нужно разрывать себя на части ради самого факта устойчивости.
                """,
                """
                Капитал {capital}. Свобода для неё выглядит не как роскошь, а как возвращённое право на собственный ритм.
                """
            ),
            options = emptyList()
        ))
        put("ending_debt_trap", event(
            id = "ending_debt_trap",
            flavor = "😞",
            isEnding = true,
            endingType = EndingType.PAYCHECK_TO_PAYCHECK,
            message = story(
                """
                Жизнь не развалилась, но превратилась в цепочку платежей. Ипотека, ремонт, школа, бытовые траты. Каждый новый месяц начинается уже слегка виноватым.
                """,
                """
                Дана по-прежнему сильная. Но вся её сила уходит на удержание конструкции, в которой почти не осталось места ни для радости, ни для спокойного роста.
                """,
                """
                Долг {debt}. Семья на плаву, но вода всё время слишком близко.
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
                Самое тяжёлое здесь даже не цифры. Самое тяжёлое — чувство, что все годы аккуратности всё равно не защитили их от падения. Дом снова перестаёт быть домом, а будущее приходится собирать почти с пустого места.
                """,
                """
                Дана переживает этот провал не как конец достоинства, а как тяжёлую, но честную отметку: осторожность без запаса и без стратегии тоже уязвима. И этот урок она уже не забудет.
                """,
                """
                Капитал истощён. Но опыт остался. А значит история ещё не закончилась окончательно.
                """
            ),
            options = emptyList()
        ))
    }

    private fun commonDanaConditionals(
        burnoutIntro: String,
        investmentIntro: String
    ): List<GameEvent> = listOf(
        event(
            id = "debt_crisis",
            priority = 10,
            flavor = "🚨",
            conditions = listOf(cond(DEBT, GT, 0L), cond(CAPITAL, LTE, 120_000L)),
            message = story(
                """
                Банк присылает уведомление сухим официальным языком, но Дана читает его всем телом. Она слишком хорошо знает, как быстро одна просрочка превращается в месяцы тревоги и унизительных разговоров.
                """,
                """
                Сейчас ей не нужен героизм. Ей нужна холодная точность, чтобы спасти семью от каскада последствий.
                """,
                """
                Долг {debt}, капитал {capital}. Пора выбрать, чем именно заплатить за выживание: инвестициями, гордостью или ещё большим внутренним напряжением.
                """
            ),
            options = listOf(
                option("sell_investments", "Продать накопленное и закрыть срочную дыру", "📉", MONTHLY_TICK,
                    Effect(debtDelta = -200_000L, investmentsDelta = -160_000L, stressDelta = 18)),
                option("ask_parents", "Попросить помощи и проглотить неловкость", "👵", MONTHLY_TICK,
                    Effect(capitalDelta = 150_000L, stressDelta = 22))
            )
        ),
        event(
            id = "burnout_warning",
            priority = 8,
            flavor = "😮‍💨",
            conditions = listOf(cond(STRESS, GTE, 72L)),
            message = story(
                burnoutIntro,
                """
                Её проблема не в том, что она мало любит свою работу. Наоборот: она слишком долго вытаскивала всё на одном чувстве долга. Любое призвание однажды ломается, если его используют как бесплатное топливо.
                """,
                """
                Нужно либо дать себе паузу, либо признать, что Дана опять выбирает не себя.
                """
            ),
            options = listOf(
                option("take_break", "Отступить на шаг и вернуть себе дыхание", "🌿", MONTHLY_TICK,
                    Effect(capitalDelta = -90_000L, incomeDelta = -40_000L, stressDelta = -32)),
                option("reduce_tutoring", "Снизить нагрузку и не умереть от полезности", "📉", MONTHLY_TICK,
                    Effect(incomeDelta = -30_000L, stressDelta = -20))
            )
        ),
        event(
            id = "investment_unlock",
            priority = 5,
            flavor = "💡",
            unique = true,
            conditions = listOf(cond(KNOWLEDGE, GTE, 38L)),
            message = story(
                investmentIntro,
                """
                Для Даны это важный внутренний поворот. Она перестаёт воспринимать финансы как территорию чужих уверенных людей и впервые чувствует, что может принимать решения не по страху, а по пониманию.
                """,
                """
                Появляется шанс сделать следующий шаг осторожно, но не пассивно.
                """
            ),
            options = listOf(
                option("buy_bonds", "Выбрать предсказуемый инструмент и зафиксировать дисциплину", "📊", MONTHLY_TICK,
                    Effect(capitalDelta = -200_000L, investmentsDelta = 200_000L, knowledgeDelta = 5)),
                option("skip_bonds", "Пока оставить деньги ближе к руке", "🏦", MONTHLY_TICK,
                    Effect())
            )
        ),
        event(
            id = "ending_wealth_trigger",
            priority = 2,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 14_000_000L), Condition.HasFlag("dana.school.started")),
            message = "Собранная Данией система перестала зависеть только от её износа и превратилась в настоящий капитал.",
            options = listOf(option("claim_wealth", "Признать большую победу", "🏆", "ending_own_school"))
        ),
        event(
            id = "ending_freedom_trigger",
            priority = 3,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 7_000_000L), cond(KNOWLEDGE, GTE, 55L)),
            message = "Тревога больше не управляет каждым решением семьи.",
            options = listOf(option("claim_freedom", "Войти в главу финансовой свободы", "🌅", "ending_freedom"))
        ),
        event(
            id = "ending_stability_trigger",
            priority = 4,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 2_500_000L), cond(STRESS, LTE, 48L)),
            message = "Наконец система семьи работает не на чуде, а на устойчивости.",
            options = listOf(option("claim_stability", "Закрепить спокойную концовку", "❤️", "ending_stable_family"))
        ),
        event(
            id = "ending_bankruptcy_trigger",
            priority = 100,
            conditions = listOf(cond(CAPITAL, LTE, 0L), cond(STRESS, GTE, 90L)),
            message = "Дальше уже не про планирование, а про признание тяжёлого падения.",
            options = listOf(option("claim_bankruptcy", "Остановиться и признать крах", "💀", "ending_bankruptcy"))
        ),
        event(
            id = "ending_paycheck_trigger",
            priority = 1,
            unique = true,
            conditions = listOf(cond(DEBT, GT, 2_000_000L), cond(CAPITAL, LTE, 100_000L)),
            message = "Месяцы стали похожи друг на друга и почти целиком принадлежат обязательствам.",
            options = listOf(option("claim_debt_trap", "Зафиксировать жизнь в долговой ловушке", "😞", "ending_debt_trap"))
        )
    )
}
