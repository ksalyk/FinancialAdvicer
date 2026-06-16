# Данияр «В город» — История (дизайн-док)

> `daniyar_90s` · era `kz_90s` · 1994 · KZT · ~20 минут игры
> Синтез трёх черновиков (Codex / MiniMax / Qwen), сверен с **реальным** движком (`GameEngine.kt`, `Models.kt`, `GameModels.kt`, тесты `:shared/commonTest`), а не только с Bible/skill.
> Голос: 2-е лицо «Ты» (shipped-style, Bible Open Decision #1). Урок учит **последствием**, не подсказкой.
> Статус: дизайн на ревью → после апрува пишем `DaniyarScenarioGraph.kt`. Цифры — отправная точка, финал тюнится в sim-harness (Bible §7, §10).

---

## 0. TL;DR

Данияр, 22, из аула под Талдыкорганом, приезжает в Алматы 1994 с 3 000 ₸ и `knowledge = 5`. Город учит его, что **всё считается**: койка, обед, ботинки, доверие к земляку, задержка зарплаты. Три педагогических петли — **подушка**, **тёплая куртка vs понты**, **МММ-пирамида** — пересекаются так, что к концу года игрок *сам* понимает: выживает не сильнейший, а тот, у кого есть запас и кто умеет сказать «нет».

Арка собрана как **scheduled-backbone**: сценарные сцены Акта I идут жёсткой цепочкой (`next` напрямую, без тика), а дальше каждый крупный бит запускает следующий через `Effect.scheduleEvent`. Между битами движок сам подсовывает пул (рутина, скамы, звонки домой) и условные события (задержка зарплаты, выгорание, банкротство). 5 концовок — терминальные условные узлы, разруливаются по `capital` + флагам.

---

## 1. Вижн и урок

**Спина (одна на персонажа, Bible §6):** финансовая грамотность с нуля — что такое бюджет, зачем буфер, как скам находит наивного.
**Колор (вторично):** сеть земляков (Нурлан — хороший советчик; Руслан — «шальной»).
**Не несёт:** инвестиции, масштабирование бизнеса (рано для Данияра — Bible §6).

Данияр **не глуп** (тезис Codex). Он из мира, где деньги были конвертом, просьбой и стыдом отказать. Город просто играет по другим правилам, и он учится в реальном времени.

Три не-обсуждаемых (Bible §1):
1. **Учим провалом** — ранние ошибки дешёвые и переживаемые (capital зажат, но `intro`-тик даёт +1 000, мгновенного банкротства нет).
2. **Последствие отложено** — «приятно сейчас» бьёт через 2–3 месяца (`scheduleEvent`).
3. **Нет бесплатного сыра** — у каждой опции цена. Правильный путь виден только задним числом.

---

## 2. Стартовое состояние (`initialPlayerState`)

Из Bible §7 (помечено как **намеренное отклонение**: `capital < 1× expenses`, `knowledge 5`). Под движок:

```kotlin
PlayerState(
    capital            = 3_000L,
    income             = 6_000L,
    expenses           = 5_000L,
    debt               = 0L,
    debtPaymentMonthly = 0L,
    investments        = 0L,
    investmentReturnRate = 0.05,     // не используется в арке Данияра (инвестиций нет)
    stress             = 45,
    financialKnowledge = 5,
    riskLevel          = 25,
    month              = 3,          // весна 1994 → МММ (scheduleEvent +3) ляжет на июнь 94
    year               = 1994,
    characterId        = "daniyar_90s",
    eraId              = "kz_90s",
    currency           = CurrencyCode.KZT,   // 1994 = после деноминации, RUB НЕ нужен и реформа НЕ нужна
    flags              = emptySet(),
)
```

**Почему так:**
- `netFlow = 6000 − 5000 − 0 = +1 000/мес` — медленно положительный (Bible §2 «slow tension»). Первый тик: 3 000 → 4 000, банкротства нет.
- `month = 3` выбран сознательно: МММ как scheduled +3 → июнь 1994 (исторически точно, Bible §7).
- `currency = KZT` без `MonetaryReform`: Данияр стартует **после** ноября 1993 (тенге уже введён), в отличие от Серика/Зейнеп/Болата. Деноминация RUB→KZT его не касается.

> ⚠️ Масштаб денег маленький (тысячи ₸). Все траты/потери держим в десятках–тысячах: ботинки 1 200, той 1 000, косуха 4 000, МММ-вклад 2 000–3 000, зимняя разгрузка +15 000.

---

## 3. Архитектура под движок (как это играется на 20 минут)

Движок после `MONTHLY_TICK` выбирает следующее событие по 4-уровневой очереди: **era-global → scheduled → conditional → weighted pool → (фолбэк `normal_life`)**. Ключевые факты (проверено по коду — см. §10):

- **era-глобалы пусты** в `EraRegistry.KZ_90S` → МММ и прочие исторические шоки мы **пишем сами** (через scheduled-backbone), а не получаем от эпохи.
- **поля `YEAR` в `Condition` нет** (только `MONTH` 1–12) → вся хронология держится на **флагах + scheduleEvent**, не на датах.
- движок **фаст-форвардит** пустые месяцы, только пока пул пуст *и* впереди есть дата (scheduled/era). Пока в пуле есть кандидаты — он их и показывает, по одному в месяц. Значит: достаточно ~12 пул-событий, и месяцы между битами заполняются органически.

### Scheduled-backbone (сценарный хребет)

Каждый крупный бит планирует **следующий** (одна `scheduleEvent` на опцию — это лимит движка). Между ними — пул + условные.

```
АКТ I (scripted, next напрямую, БЕЗ тика):
  intro → almaty_arrival → obshaga → first_day → first_wage
                                                     │  (выбор: направление-флаг)
                                                     ▼  MONTHLY_TICK + schedule(mmm_arrives,+3)
АКТ II (sim-фаза, фаст-форвард + пул + условные):
  [апр–май 94: пул — звонки домой, рутина, фейк-работа у Саяхата, тетрадь Нурлана]
  mmm_arrives        (июнь 94)  ── schedule(mmm_aftermath,+2) ──► MONTHLY_TICK
  [пул + условные: задержка зарплаты, базар-дублёнка…]
  mmm_aftermath      (авг 94)   ── schedule(winter_gear,+2)   ──► MONTHLY_TICK
  winter_gear        (окт 94)   ── косуха|газеты: schedule(cold_winter,+2)
                                   ватник:        schedule(winter_steady,+2) ──► TICK
  cold_winter|winter_steady (дек 94) ── schedule(final_fork,+2) ──► MONTHLY_TICK
АКТ III (расчёт):
  final_fork         (фев 95)   ── schedule(final_review,+3) ──► MONTHLY_TICK
  final_review       (~май 95)  ── setFlag(arc.final_check)  ──► MONTHLY_TICK
                                                     ▼
  ending_* (условные-терминальные, по приоритету: bankruptcy→wealth→freedom→stability→p2p)
```

Хребет ~17 месяцев (мар 94 → ~май/авг 95). 5 сценарных сцен Акта I + 6 backbone-битов + final_review + ~12–18 пул/условных вставок ≈ **25–30 точек решения ≈ 20 минут**.

**Почему scheduled, а не era-global для МММ:** scheduleEvent даёт детерминированную дату без правки `EraRegistry`, без зависимости от RNG-probability, и гарантирует, что бит увидит *каждый* игрок. Альтернатива (зарегистрировать `era_mmm_wave_90s` в `EraRegistry.KZ_90S.globalEvents`) — общая для всех 90-х персонажей, но требует правки движкового реестра и всё равно — тела события в графе. Решение зафиксировано в §10; обсуждаемо на этапе кода.

---

## 4. АКТ I — Прибытие  (scripted-цепочка, `next` напрямую, без `MONTHLY_TICK`)

> Задача (Bible §5, Codex): посадить игрока в тело Данияра. **Никаких финансовых развилок.** Все опции — про характер и атмосферу, мелкие эффекты. Тик не запускаем до `first_wage`, поэтому ни банкротство, ни пул здесь не сработают — Акт I безопасен by design.

### `intro` 🌅 — проводы / вагон
```
Ты выходишь из вагона с клетчатой сумкой, узлом баурсаков от матери и 3 000 тенге
в потайном кармане. На перроне «Алматы-2» пахнет углём, мокрой газетой и чужой спешкой.

Нурлан должен был встретить — его нет. На табло мигают буквы, будто город сам не уверен,
как теперь называется жизнь после рубля.
```
| опция | emoji | next | effects |
|---|---|---|---|
| Надеть отцовскую телогрейку, ждать | 🧥 | `almaty_arrival` | — |
| Спросить дорогу у милиционера | 👮 | `almaty_arrival` | `knowledge+1` |
| Пойти за толпой к остановке | 🚶 | `almaty_arrival` | `stress-3` |

*tags:* `family` · *unique:* да

### `almaty_arrival` 🏙️ — Нурлан находит к вечеру
```
Нурлан — в оранжевой жилетке, «Бонд» в зубах — хлопает по плечу так, что ты делаешь шаг назад.
«Ну, аульный, доехал? Пошли, покажу, где жить».

Общага на Райымбека: бывший техникум под рабочих. Четыре койки, шкаф, стол с трещиной.
Сосед Руслан, усатый, с золотой фиксой, пьёт «Nescafe» и крутит видеомагнитофон.
«Оклад 6 000 в месяц, — говорит Нурлан. — Аванс через неделю. Жильё бесплатно. Еда — сами».

6 000. Это больше, чем отец берёт за полгода в колхозе.
```
| опция | emoji | next | effects |
|---|---|---|---|
| Пожать руку Руслану — почти земляк | 🤝 | `obshaga` | `setFlags=[net.t1]`, `knowledge+1` |
| Расспросить Нурлана про его путь | 🗣️ | `obshaga` | `setFlags=[net.t1]` |
| Лечь спать — устал с дороги | 💤 | `obshaga` | `stress-8` |

### `obshaga` 🔐 — первая ночь, чужие правила
```
Руслан кивает на твою сумку: «Деньги под матрас не клади. Тут все хорошие — пока голодные не стали».
У общей тумбочки замка нет. Койка верхняя, гвоздь в стене — твой.
```
| опция | emoji | next | effects |
|---|---|---|---|
| Купить маленький замок — 80 ₸ | 🔒 | `first_day` | `capital-80`, `knowledge+1` |
| Спрятать деньги в носок | 🧦 | `first_day` | `stress+3` |
| Попросить Нурлана подержать часть | 🙏 | `first_day` | `setFlags=[net.t1]` |

> Учим «прятать ценное» сюжетом, не лекцией. Замок за 80 ₸ — первая «инвестиция в безопасность»; не наказываем ни один выбор.

### `first_day` 🧱 — стройка, общий котелок
```
Подъём в шесть. Чай с сушками на всех. Монолит, второй этаж заливают.
Прораб Тимур-ага командует коротко: «Нурлан, новенького на подсобку».

Спина горит, ноги гудят. К обеду Нурлан суёт булку и плавленый сырок: «Первое время так. Втянешься».
Вечером Руслан варит кашу на чайнике — на четверых пакет крупы и две банки тушёнки.
Ты впервые ешь с незнакомыми мужиками из одного котла.
```
| опция | emoji | next | effects |
|---|---|---|---|
| Скинуться с Русланом на хлеб и сахар | 🍞 | `first_wage` | `capital-150`, `setFlags=[net.t1]` |
| Поесть что дают, помолчать | 🤐 | `first_wage` | — |
| Расспросить, как тут все устроились | 🗣️ | `first_wage` | `knowledge+1` |

### `first_wage` 🧧 — ПЕРВЫЙ АВАНС  *(узел направления арки → запускает sim-фазу + МММ)*
```
Суббота. Нурлан выдаёт конверт: «3 000 аванс, остальное в конце месяца».
Стопка новых купюр в руке — столько ты не держал никогда.

В общаге Руслан зовёт «отметить» — по 1 000 на стол: «Не отметить аванс — плохая примета».
По автомату дозвонилась мать: «Сынок, отец колено разбил, таблетки… можно полторы тысячи?» —
голос ровный, будто 1 500 это мелочь, которую ты не заметишь.
А Нурлан отзывает в сторону: «Скидываемся прорабу на той сына, 500 с носа. Не скинешь — на хороший объект не позовут».
```
| опция | emoji | next | effects |
|---|---|---|---|
| Отметить с ребятами | 🥳 | `MONTHLY_TICK` | `capital-1000`, `stress-8`, `riskDelta+5`, `setFlags=[path.brother]`, `scheduleEvent=(mmm_arrives, +3)` |
| Послать матери на таблетки | 📮 | `MONTHLY_TICK` | `capital-1500`, `stress+8`, `setFlags=[path.family, family.first]`, `scheduleEvent=(mmm_arrives, +3)` |
| Скинуться прорабу на той | 🏗️ | `MONTHLY_TICK` | `capital-500`, `knowledge+1`, `setFlags=[path.work, net.t1]`, `scheduleEvent=(mmm_arrives, +3)` |
| Отложить почти всё в конверт «не трогать» | ✉️ | `MONTHLY_TICK` | `capital+0`, `knowledge+2`, `setFlags=[buffer.kept]`, `scheduleEvent=(mmm_arrives, +3)` |

> 4 опции = 4 настоящие человеческие стратегии (Bible §2.3). **Все** планируют МММ на +3 (июнь 94), чтобы пирамида гарантированно дошла. Текст опций — про отношения, не про «+X». Флаг `buffer.kept` — единственный, кто реально готовит к Акту II; игрок этого ещё не знает.

---

## 5. АКТ II — Город проверяет  (sim-фаза)

> Большая часть playtime. Давление снаружи. Backbone-биты (МММ, куртка) фиксированы по датам; между ними движок крутит пул и условные. Последствия Акта I всплывают сами.

### 5.1 Backbone-бит: `mmm_arrives` 💸 (июнь 1994) — пирамида приходит в общагу
```
За столом — третий вечер спора. Руслан машет цветными билетами: «Вложил 2 000 — через месяц 4 000. Гарантия».
Нурлан: «Это пирамида. Лёня Голубков в телевизоре — артист, ему платят за слова».
Сосед Аскар: «Я мать хочу втянуть, у неё 8 000 на книжке всё равно лежат».

У тебя в кармане полторы тысячи. А где-то в Талдыкоргане мать копит на ремонт крыши —
и слушает, что скажет сын.
```
*tags:* `scam`, `scam.pyramid`, `world` · *schemeExplanation:* короткий разбор «как работала пирамида МММ» (показывается после, Bible §3 — без how-to)
| опция | emoji | next | effects |
|---|---|---|---|
| Вложить свои 2 000 | 🎟️ | `MONTHLY_TICK` | `capital-2000`, `riskDelta+10`, `setFlags=[scam.taken]`, `scheduleEvent=(mmm_aftermath, +2)` |
| Посоветовать матери вложить «книжку» | 📞 | `MONTHLY_TICK` | `setFlags=[scam.taken, scam.mother]`, `stress+5`, `scheduleEvent=(mmm_aftermath, +2)` |
| Поддержать Нурлана: «пирамида и есть» | 🛑 | `MONTHLY_TICK` | `knowledge+3`, `setFlags=[learned.scam.pyramid]`, `scheduleEvent=(mmm_aftermath, +2)` |
| Промолчать, написать матери «не лезь» | ✍️ | `MONTHLY_TICK` | `knowledge+1`, `setFlags=[learned.scam.pyramid, family.trusted]`, `scheduleEvent=(mmm_aftermath, +2)` |

> `learned.scam.pyramid` даёт движковый иммунитет: `EventPoolSelector` режет вес всех `scam.pyramid` ×0.15. Это и есть «выучил урок» — без плашки.

### 5.2 Backbone-бит: `mmm_aftermath` 📉 (авг 1994) — пирамида рушится
Текст ветвится по флагу (одно событие, две редакции — стандартный приём Qwen):
```
[scam.mother] Звонок. Мать тихо, без слёз — это хуже слёз: «Сынок, МММ закрыли.
Я вложила, как ты сказал… крыша теперь без денег. Прости».
[scam.taken, без матери] Руслана нет три дня. Койка пуста, видика нет. Твои 2 000 сгорели.
[learned.scam.pyramid] На стройке мужики плачут — отдали последнее. Руслан исчез. Твоё — цело.
```
| опция | emoji | next | effects |
|---|---|---|---|
| «Верну тебе, мать. Постепенно» *(если scam.mother)* | 🤝 | `MONTHLY_TICK` | `debtDelta+3000`, `debtPaymentDelta+500`, `setFlags=[debt.family, learned.scam.pyramid]`, `stress+10`, `scheduleEvent=(winter_gear, +2)` |
| Молча проглотить потерю | 😞 | `MONTHLY_TICK` | `stress+10`, `setFlags=[learned.scam.pyramid, lost_money_to_scam]`, `scheduleEvent=(winter_gear, +2)` |
| Отвезти матери что осталось | 🚂 | `MONTHLY_TICK` | `capital-800`, `stress-5`, `setFlags=[learned.scam.pyramid]`, `scheduleEvent=(winter_gear, +2)` |

> `lost_money_to_scam` → движок ×0.3 ко *всем* скам-подтипам (обжёгшись на молоке). После аф­термата у всех стоит `learned.scam.pyramid` — урок усвоен, дальше пирамиды почти не всплывают.

### 5.3 Backbone-бит: `winter_gear` 🧥 (окт 1994) — барахолка перед морозами
```
С гор тянет ледяным ветром. Нурлан: «Без нормального ватника на лесах застудишь почки».
На барахолке среди контейнеров продавец трясёт чёрной кожаной косухой: «Брат, бери — 4 000, все девки твои».
Рядом висит брезентовый ватник и резиновые сапоги — уродливо, но 2 500.
```
*tags:* `crisis`
| опция | emoji | next | effects |
|---|---|---|---|
| Взять косуху — в ауле будут гордиться | 🧥 | `MONTHLY_TICK` | `capital-4000`, `setFlags=[gear.style]`, `scheduleEvent=(cold_winter, +2)` |
| Взять ватник и валенки | 🧣 | `MONTHLY_TICK` | `capital-2500`, `setFlags=[gear.warm]`, `scheduleEvent=(winter_steady, +2)` |
| Утеплиться газетами под свитер | 📰 | `MONTHLY_TICK` | `setFlags=[gear.none]`, `scheduleEvent=(cold_winter, +2)` |

### 5.4 Backbone-бит: `cold_winter` 🤒 / `winter_steady` 💪 (дек 1994) — расплата за куртку
`cold_winter` (если `gear.style` или `gear.none`):
```
Морозы под −20. Просыпаешься с тяжёлым кашлем, грудь горит. Косуха продувается, газеты не греют.
Нурлан жёстко: «Не выйдешь завтра на бетон — на твоё место очередь с вокзала».
```
| опция | emoji | next | effects |
|---|---|---|---|
| Отлежаться, купить лекарства | 💊 | `MONTHLY_TICK` | `capital-2000`, `incomeDelta-1000`* , `stress+8`, `scheduleEvent=(final_fork, +2)` |
| Выйти больным на бетон | 🥶 | `MONTHLY_TICK` | `stress+15`, `riskDelta+5`, `scheduleEvent=(final_fork, +2)` |

\* `incomeDelta-1000` моделирует прогулы; восстанавливается отдельной пул-веткой или просто остаётся как цена урока (тюним в sim).

`winter_steady` (если `gear.warm`):
```
В ватнике жарко, ты здоров, кладёшь кирпич шов в шов. Нурлан хвалит: «Бережёшь себя — сделаю старшим по инструменту».
```
| опция | emoji | next | effects |
|---|---|---|---|
| Принять повышение | 🔧 | `MONTHLY_TICK` | `incomeDelta+1000`, `knowledge+1`, `setFlags=[net.t2]`, `scheduleEvent=(final_fork, +2)` |
| Поблагодарить, остаться как есть | 🙂 | `MONTHLY_TICK` | `stress-5`, `scheduleEvent=(final_fork, +2)` |

> Петля «куртка»: статус (косуха) греет самолюбие сейчас → болезнь и потеря дохода зимой; практичность (ватник) скучна сейчас → здоровье + повышение. Классический lifestyle-vs-resilience, доказуемо (см. §9).

### 5.5 Условные события (`conditionalEvents`, проверяются после тика по убыванию priority)

| id | priority | conditions | смысл / опции (кратко) | повтор |
|---|---|---|---|---|
| `ending_bankruptcy` | 200 | `cond(CAPITAL, LTE, 0)` | **терминал** (см. §6) — фолбэк-дно, ловит «обнулился» в любой момент | — |
| `ending_wealth` | 150 | `HasFlag(arc.final_check)` + `cond(CAPITAL, GTE, 22000)` + `HasFlag(net.t2)` + `HasFlag(learned.scam.pyramid)` | **терминал** | — |
| `ending_freedom` | 140 | `HasFlag(arc.final_check)` + `cond(CAPITAL, GTE, 14000)` + `HasFlag(learned.scam.pyramid)` | **терминал** | — |
| `ending_stability` | 130 | `HasFlag(arc.final_check)` + `cond(CAPITAL, GTE, 7000)` | **терминал** | — |
| `ending_p2p` | 110 | `HasFlag(arc.final_check)` | **терминал** — фолбэк-пол над банкротством | — |
| `debt_crisis` | 100 | `cond(DEBT, GT, 8000)` | Нурлан/ломбард давит. Гасить (`capital−`,`debt−`) / занять ещё (`debt+` спираль) / продать часы (`capital+`) | `cooldown 3` |
| `burnout` | 90 | `cond(STRESS, GT, 75)` | Измотан. Отдых в ауле (`stress−`,`capital−800`) / переть (`stress+`,`income+`) | `cooldown 6` |
| `wage_arrears` | 80 | `cond(CAPITAL, LT, 1500)` | «Заказчик не рассчитался, две недели нет денег». Без буфера → занять у Нурлана (`debt+3000`,`debtPay+500`,`setFlags=[debt.brigadier]`); с буфером — спокойно купить гречку | `cooldown 4` |

Все условные (кроме терминалов) ведут в `MONTHLY_TICK`. `wage_arrears` на `CAPITAL < 1500` — главный «зуб» урока про подушку: бьёт по бедным повторно, толкает в долг к бригадиру → путь к P2P/банкротству.

### 5.6 Пул (`eventPool`, рутина 60%+, скамы редкие) — `normal_life` доминирует

| id | weight | tags | повтор | суть (опции → эффекты) |
|---|---|---|---|---|
| `normal_life` | 10 | — | `cooldown 1` | «Обычный месяц на стройке». Работать дальше / отложить в конверт (`capital+200`,`setFlags=[buffer.kept]`) / отдохнуть (`stress-2`). Все → TICK. **Также хардкод-фолбэк движка.** |
| `pool_call_home` | 8 | `family` | `cooldown 2` | Звонит мать. Послать 1 000 (`capital-1000`) / 3 000 (`capital-3000`,`stress-3`) / честно «коплю» (`knowledge+1`) |
| `pool_notebook` | 6 | — | `maxOcc 1` | Тетрадь Нурлана: «хлеб, дорога, долг…». Завести свою (`knowledge+2`,`setFlags=[lesson.budget]`) / «и так помню» (—) |
| `pool_side_work` | 6 | `career` | `cooldown 2` | Нурлан подкидывает вечернюю халтуру. Взять (`capital+1500`,`stress+4`) / отдохнуть (`stress-3`) |
| `pool_kairat_tile` | 4 | `career` | `maxOcc 2` | Мастер Кайрат учит класть плитку. Учиться (`knowledge+2`,`setFlags=[skill.tile]`) / не до того (—) |
| `pool_bazaar` | 5 | — | `cooldown 3` | Барахолка. Подарок матери (`capital-800`,`stress-3`) / просто пройтись (—) |
| `network_favor` | 3 | `family` | `maxOcc 1` | Земляк в милиции, залог 1 500. Выручить (`capital-1500`,`setFlags=[net.t2]`) / пройти мимо (`stress+8`) |
| `pool_banya` | 4 | — | `cooldown 3` | Баня по субботам. С Нурланом (`setFlags=[net.t1]`,`stress-5`) / одному (`stress-4`) |
| `pool_roommate` | 6 | — | `cooldown 2` | Вечер в общаге. Слушать про подработки (`knowledge+1`) / лечь спать (`stress-3`) |
| `pool_scam_fakejob` | 2 | `scam`,`scam.fake_job` | `NotFlag(learned.scam.fake_job)` | Посредник у Саяхата: 1 800 «за оформление тёплой работы». Заплатить (`capital-1800`,`setFlags=[lost_money_to_scam]`) / спросить адрес конторы (`knowledge+2`,`setFlags=[learned.scam.fake_job]`) / уйти (`setFlags=[learned.scam.fake_job]`) |
| `pool_scam_goods` | 3 | `scam`,`scam.fake_goods` | `NotFlag(learned.scam.fake_goods)` | «Таджикская дублёнка, натуральная кожа, 4 000». Купить (`capital-4000`,`scheduleEvent=(goods_peeled,+3)`,`setFlags=[lost_money_to_scam]`) / отказаться (`knowledge+1`,`setFlags=[learned.scam.fake_goods]`) |
| `pool_scam_relative` | 2 | `scam`,`scam.relative` | `NotFlag(learned.scam.relative)` | «Брат двоюродный приехал, одолжи до понедельника» — 2 000. Дать (`capital-2000`) / отказать (`knowledge+1`,`setFlags=[learned.scam.relative]`) |
| `goods_peeled` | — | `scam.fake_goods` | (только как scheduled-таргет) | Через 3 мес кожа облезла, мать: «ай, сынок, не надо было». `stress+4`, `knowledge+1`, `setFlags=[learned.scam.fake_goods]` → TICK |

`normal_life` (10) строго больше любого одиночного скама (3/2/2) — требование Bible §5 / skill. Скам-веса дополнительно режутся флагами `learned.scam.*` и `lost_money_to_scam` (движок). Рефуз-ветки скамов ставят `learned.scam.{type}` — это и есть прививка.

---

## 6. АКТ III — Расчёт

### `final_fork` ❄️ (фев 1995) — последняя развилка: стабильность vs азарт
```
Стройку морозят на две недели. Нурлан: «Поехали в Алматы на разгрузку — 1 500 в сутки, десять дней, 15 000».
Руслан, объявившийся, тянет в другое: «Возят шубы из Оренбурга под реализацию. Поставим на барахолке —
наш процент. Не продадим — вернём». На сберкнижке у тебя то, что осталось от полугода.
```
*tags:* `career`
| опция | emoji | next | effects |
|---|---|---|---|
| Поехать с Нурланом на разгрузку | 👷 | `MONTHLY_TICK` | `capital+15000`, `stress+6`, `setFlags=[end.stable, net.t2]`, `scheduleEvent=(final_review, +3)` |
| Вписаться с Русланом в шубы | 🧥 | `MONTHLY_TICK` | `debtDelta+8000`, `debtPaymentDelta+1000`, `riskDelta+15`, `setFlags=[end.gamble]`, `scheduleEvent=(shuby_result, +2)` |
| Сидеть в общаге, ждать стройку | ⏳ | `MONTHLY_TICK` | `stress+5`, `scheduleEvent=(final_review, +3)` |

`shuby_result` (исход азарта, +2 мес; ветка по `riskLevel`/RNG-флагу — детерминируем через предыдущий выбор): продал → `capital+12000`,`debtDelta-8000`,`setFlags=[end.gamble.win]`; не продал → долг остаётся, `stress+12` → толкает в `debt_crisis`/`ending_bankruptcy`. Оба → `scheduleEvent=(final_review, +3)`.

### `final_review` 📜 (~май 1995) — итог года
```
Ты раскладываешь записи. Полгода с лишним позади. Алматы изменился — и ты тоже.
Нурлан зовёт прорабом, но просит вложиться в инструмент. Мать пишет: в ауле ждут твоего решения.
Сколько у тебя в конверте — столько у тебя и выбора.
```
| опция | emoji | next | effects |
|---|---|---|---|
| Посчитать всё честно и решить | 🧾 | `MONTHLY_TICK` | `setFlags=[arc.final_check]` |

> Единственная опция ставит `arc.final_check` и делает тик. Дальше срабатывают терминальные условные `ending_*` по приоритету. Фолбэк `ending_p2p` (только `arc.final_check`) гарантирует, что игра всегда разрешится — без зацикливания.

### Пять концовок (терминальные условные узлы; `isEnding=true`, `endingType`, `options = emptyList()`)

> Паттерн: каждая концовка — это **сам** условный узел (с непустыми `conditions` + `isEnding`), а не отдельный «trigger → terminal». Проверено: проходит `ScenarioGraphContentTest` («endings terminal», «transitions resolve») и `ScenarioSimulationTest` (reachability через tick-замыкание). Это сознательное упрощение vs двухузловой пример в skill (§10).

`ending_bankruptcy` 💔 `BANKRUPTCY` — `cond(CAPITAL, LTE, 0)`, priority 200
```
Ты возвращаешься в аул раньше, чем хотел. Пустая сумка, тяжёлое молчание.
Больнее всего не потерянные деньги — а то, что теперь ты точно знаешь, где ошибся.
```

`ending_p2p` 😰 `PAYCHECK_TO_PAYCHECK` — `HasFlag(arc.final_check)`, priority 110
```
Зарплата уходит в зарплату. Матери — сколько можешь, себе — сколько осталось. Запаса нет.
Заболеешь — идти к Нурлану занимать. Ты работаешь. Но легче не становится.
```

`ending_stability` 😊 `FINANCIAL_STABILITY` — `arc.final_check` + `CAPITAL ≥ 7 000`, priority 130
```
Тетрадь, нормальные ботинки, свой инструмент, люди, которым можно звонить, и конверт,
который не трогают без причины. Ты ещё не богат. Но город больше не свалит тебя одним плохим месяцем.
```

`ending_freedom` 🎯 `FINANCIAL_FREEDOM` — `arc.final_check` + `CAPITAL ≥ 14 000` + `learned.scam.pyramid`, priority 140
```
Ты впервые отказываешься от сомнительной работы без страха — не из гордости, а потому что можешь
позволить себе сказать «нет». Подушка, чистая голова, и год, который тебя ничему зря не научил.
```

`ending_wealth` 🤑 `WEALTH` — `arc.final_check` + `CAPITAL ≥ 22 000` + `net.t2` + `learned.scam.pyramid`, priority 150
```
Не империя. Просто редкая для 90-х победа: стабильная бригада, репутация, инструмент, резерв,
помощь дому без самоуничтожения. Земляки доверяют, ты научился считать и не верить пустым обещаниям.
Для парня из аула это и есть большое богатство.
```

---

## 7. Карта флагов (для `Effect.setFlags` / `Condition`)

| флаг | ставится | читается | педагогический смысл |
|---|---|---|---|
| `buffer.kept` | `first_wage` (отложить), `normal_life` | (драйвит `capital` → концовки) | привычка платить сначала себе |
| `lesson.budget` | `pool_notebook` | колор | деньги без записи исчезают |
| `net.t1` / `net.t2` | рукопожатия, баня, `network_favor`, `final_fork`, `winter_steady` | `ending_wealth` | соцкапитал = страховка и доступ |
| `gear.style` / `gear.warm` / `gear.none` | `winter_gear` | `cold_winter` vs `winter_steady` | статус сейчас vs устойчивость потом |
| `scam.taken` / `scam.mother` | `mmm_arrives` | `mmm_aftermath` (редакция текста, `debt.family`) | наивность к пирамиде |
| `learned.scam.pyramid` | `mmm_arrives` (рефуз), весь `mmm_aftermath` | `ending_freedom/wealth`; ×0.15 вес пирамид | прививка от МММ |
| `learned.scam.fake_job` / `fake_goods` / `relative` | рефуз соответствующих пул-скамов | ×0.15 вес подтипа | прививка по подтипам |
| `lost_money_to_scam` | взять любой скам | ×0.3 ко всем скамам | обжёгшись на молоке |
| `debt.brigadier` / `debt.family` | `wage_arrears`, `mmm_aftermath` | путь к P2P/`debt_crisis` | кабала без подушки |
| `arc.final_check` | `final_review` | ворота всех «хороших» концовок | гейт расчёта |
| `path.brother/family/work` | `first_wage` | колор/текст | какой Данияр тебе ближе |
| `end.stable` / `end.gamble[.win]` | `final_fork`/`shuby_result` | колор концовок | стабильность vs азарт |

---

## 8. Педагогические петли (Bible §5 «teaching loop»)

1. **Подушка.** `first_wage`(понты/семья без буфера) → +1 000/мес мало → `wage_arrears` на `CAPITAL<1500` → занять у бригадира (`debt.brigadier`) → `debt_crisis` → P2P/банкротство. *Контр-пример:* `buffer.kept` → `wage_arrears` проходит спокойно.
2. **Куртка (статус vs устойчивость).** `winter_gear`(косуха) → `cold_winter` → лекарства + прогулы (`capital−`,`income−`). *Контр:* ватник → `winter_steady` → повышение (`income+`,`net.t2`).
3. **Скам (МММ flagship).** `mmm_arrives`(вложить/мать) → `mmm_aftermath`(потеря + `debt.family`). *Контр:* рефуз → `learned.scam.pyramid` (иммунитет + knowledge) → ворота к freedom/wealth. Пул-скамы (Саяхат, дублёнка, «брат») — те же петли в миниатюре.

Каждая: **искушение → приятный мелкий эффект сейчас → отложенное последствие (scheduleEvent) → расплата → инсайт**. Ни одной строки «это скам» / «отложи подушку» — урок несёт следующий месяц.

---

## 9. Достижимость концовок (доказуемость урока, Bible §10)

| Концовка | Как дойти (профиль игры) | Достижима статически? |
|---|---|---|
| `BANKRUPTCY` | шубы-проигрыш / МММ-вклад + нет буфера + кабала → `CAPITAL→0` | да: условный priority 200, ловится после любого тика |
| `PAYCHECK_TO_PAYCHECK` | выжил, но без буфера/в долгах: `arc.final_check`, `CAPITAL < 7 000` | да: фолбэк-пол (только `arc.final_check`) |
| `FINANCIAL_STABILITY` | дисциплина, тетрадь, ватник, без крупных потерь: `CAPITAL ≥ 7 000` | да |
| `FINANCIAL_FREEDOM` | буфер + рефуз МММ + разгрузка/халтура: `CAPITAL ≥ 14 000` + `learned.scam.pyramid` | да |
| `WEALTH` | всё вышеперечисленное + соцкапитал + крупные возможности: `≥ 22 000` + `net.t2` + immune | да |

**Статическая достижимость гарантирована:** `final_review` достигается по scheduled-цепочке; его `MONTHLY_TICK` в BFS-модели harness раскрывается во *все* условные (включая 5 концовок) + `normal_life`. `ending_bankruptcy` ловится с любого тика. → `every_reference_resolves_and_all_endings_are_reachable` зелёный.
**Liveness:** все не-терминальные узлы имеют ≥1 опцию; `normal_life` определён и репитбелен; деньги/статы клампятся движком → `random_playthroughs_never_crash_dead_end…` зелёный (после тюнинга цифр).
**Доказуемость урока:** ветка понты+скам+нет буфера тренди́т в P2P/банкротство; дисциплина+рефуз+устойчивость — в stability/freedom/wealth. Финальные пороги (7k/14k/22k) и доходы (разгрузка +15k, халтура +1.5k) тюним в sim, чтобы оба коридора реально достигались на 40 сидах.

---

## 10. ⚠️ Реальность движка vs доки (Bible/skill частично «на бумаге»)

Сверено с кодом на 2026-06-17. Bible §0 сам пишет «не цитируй правила по памяти — открой скилл»; скилл местами опережает код. Что важно для этапа кода:

1. **era-глобалы НЕ подключены.** `EraRegistry.KZ_90S.globalEvents = emptyList()`. `era_mmm_wave_90s`, `era_tenge_introduced` и т.д. **не существуют**. → МММ пишем как **scheduled-бит** в графе Данияра (выбрано), либо отдельной задачей регистрируем era-global + тело события. Bible §7 «auto-fire, don't define» сейчас неверно.
2. **Нет `YEAR` в `Condition.Stat`** (поля: CAPITAL/INCOME/EXPENSES/DEBT/STRESS/KNOWLEDGE/RISK/MONTH). Хронология — только через **флаги + scheduleEvent**. Никаких «if year==1994».
3. **DSL-хелперов (`event`/`option`/`story`/`cond`) НЕТ** в `Scenarios.kt`/нет `NarrativeDsl.kt`, хотя `references/data-model.md` их описывает. → либо конструируем `GameEvent`/`GameOption`/`Effect` напрямую (named-args), либо первой задачей добавляем мини-DSL. `GameOption` поле эффектов называется **`effects`** (не `fx`), `next` обязателен.
4. **Фабрика — `ScenarioGraphFactory.forCharacter(characterId, eraId)`**, не `buildGraph()`. Сейчас все персонажи маршрутизируются в пустой era-шелл. → добавляем ветку `"daniyar_90s" -> DaniyarScenarioGraph()` (по characterId, до `forEra`).
5. **Тест `empty era graphs contain only terminal intro` СЛОМАЕТСЯ** при регистрации первого реального персонажа: он требует, чтобы у *каждого* графа в `ScenarioTestCatalog.graphs()` было ровно `{"intro"}`, без условных/пула. → нужно **отрефакторить** `ScenarioTestCatalog` (разделить «empty shells» и «authored») и этот тест. Skill «добавь пару в combos — авто-энролл» сейчас неполон.
6. **Концовки — одноузловой терминал-условный** работает (conditions + `isEnding` + пустые options). Skill показывает двухузловой `*_trigger → ending_*`; оба валидны, выбран одноузловой (меньше узлов, проходит тесты).
7. **`normal_life` обязателен** в `events` с опциями→TICK: это хардкод-фолбэк (`?: "normal_life"`); без него — `DEAD_END` в симуляции.
8. **Клампы:** все денежные поля `coerceAtLeast(0)`, мягкие статы `coerceIn(0,100)`. `cond(CAPITAL, LTE, 0)` ≡ «ровно 0». Чтобы банкротство «залипло», нужен устойчиво отрицательный netFlow (долговая ветка), иначе +1 000/мес вытащит обратно.
9. **`schemeExplanation`** — отдельное поле события для разбора скама (МММ). Используем для пост-фактум объяснения без how-to (Bible §3).
10. **i18n:** контент эпох-шеллов хранится инлайн-строкой; событийный текст Данияра тоже пишем инлайн (RU). Кеинг в `Strings`/`StringKeys` — отдельная задача, не блокирует прохождение гейта.

---

## 11. Чек-лист авторинга (Bible §9) — статус по доку

```
[x] eraId + characterId; PlayerState из §7 (capital<expenses помечено как намеренное)
[x] валюта/год: KZT 1994, RUB/реформа НЕ нужны
[x] Акт I: 5 уникальных сцен, заканчивается флагом направления (path.* + buffer.kept)
[x] Акт II: 7 условных (вкл. ending_bankruptcy(200), debt_crisis, burnout, wage_arrears) + 13 пул
[x] ≥1 скам под персонажа: МММ (scam.pyramid) + 3 пул-скама, learned.scam.* на рефузе
[~] спроектировано вокруг era-глобалов — их НЕТ → МММ как scheduled-бит (см. §10.1)
[x] normal_life существует; его вес (10) > любого скама (≤3)
[x] все 5 концовок: терминальные (options=emptyList), достижимы через триггеры
[x] урок доказуемо learnable: понты+скам→P2P/банкротство; дисциплина+рефуз→stability/freedom/wealth
[x] голос «Ты»; опции — инфинитив + конкретное число; не нравоучительно
[x] guardrails §3 (не реклама, реалистичные потери, без how-to скама, с достоинством)
[x] зарегистрирован в forCharacter (buildGraph) + ScenarioTestCatalog (split) + SeedData — СДЕЛАНО
[~] VALIDATION GATE — статика зелёная (validate_scenarios.py 0 ошибок; 0 dangling refs); :shared:test — на твоём Mac
```

## 12. Статус реализации (2026-06-17)

Код написан и статически проверен (Gradle в песочнице не поднять — Java 11, нет SDK):

- `scenarios/NarrativeDsl.kt` — DSL: `story/cond/option/event/ending` + `EventArc/arc/buildEvents/flattenEvents`.
- `scenarios/DaniyarScenarioGraph.kt` — граф из 7 арок (arrival, mmm, winter, endgame, regularLife, conditionals, endings); `initialPlayerState` из §2; 28 событий + 8 условных/концовок.
- `scenarios/Scenarios.kt` — `forCharacter` → `buildGraph(characterId)` → `daniyar_90s` → `DaniyarScenarioGraph()`; шеллы как фолбэк.
- `data/SeedData.kt` — `PredefinedCharacter("daniyar_90s")` + добавлен в `kz_90s.availableCharacterIds`.
- `commonTest`: `ScenarioTestCatalog` (split emptyShell/authored), `ScenarioGraphContentTest` (empty-тест только для шеллов + новые authored-проверки: normal_life, покрытие 5 EndingType), `ScenarioGraphFactoryTest` (ожидаемый ростер kz_90s обновлён).

Проверено: linter 0 errors; кросс-реф 0 dangling (36 событий, все scheduleEvent/pool/next резолвятся); 5 концовок достижимы через tick-замыкание из `intro`. **Осталось:** `./gradlew :shared:test` на Mac + балансировка порогов/доходов по выхлопу sim (40 сидов).

---

## Что дальше (этап кода, после твоего ревью)

1. (опц.) мини-`NarrativeDsl` или прямые `GameEvent(...)`.
2. `DaniyarScenarioGraph : ScenarioGraph()` — `initialPlayerState` (§2), `events` (Акт I + backbone + пул + final + 5 терминалов), `conditionalEvents` (§5.5), `eventPool` (§5.6).
3. `forCharacter`: ветка `daniyar_90s`.
4. Рефактор `ScenarioTestCatalog` + `ScenarioGraphContentTest` (разделить empty/authored, §10.5); добавить `"daniyar_90s" to "kz_90s"`.
5. Гейт на Mac: `validate_scenarios.py` (если есть) → `:shared:compileKotlinCommon` → `:shared:test`. Тюнить пороги/доходы по выхлопу sim, не ослабляя тесты.
