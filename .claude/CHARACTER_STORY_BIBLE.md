# Character Story Bible & Authoring Brief (V2)

> Date: 2026-06-15 · Author: Claude (design partner)
> **Audience: the AI that writes these characters' scenarios** (story arcs, events, choices).
> This is the *base document to rely on*: it defines **what** to write and **why**. It does **not** redefine engine mechanics.

---

## 0. How to use this document

This brief is the creative + pedagogical layer. It sits on top of the existing tooling. Read in this order before writing any character:

| Read | For | Authority |
|------|-----|-----------|
| **This file** | Who each character is, their lesson, voice, arc, seed scenes | Creative direction |
| `.claude/skills/scenario-writer/SKILL.md` (+ `references/`) | The engine data contract, DSL, validation, **how to code & test** | **Mechanics — source of truth, wins all conflicts** |
| `.claude/CHARACTER_SELECTION_V2.md` | Why these 8, scoring, cut rationale | Selection rationale |
| `.claude/SCENARIO_REFERENCE.md` | Real fraud schemes (RU) to build scam events from | Scam content |
| `.claude/SCENARIO_GRAPH_GUIDE.md` | DSL/style reference (note: skill lists its out-of-date points) | Secondary |

**Golden rule:** when this brief and the `scenario-writer` skill disagree on *how* something is built, the **skill wins**. This brief never overrides the engine contract (e.g. endings are `options = emptyList()`, `normal_life` must exist, register in `ScenarioGraphFactory.buildGraph` + `ScenarioTestCatalog.combos`, pass the 3-stage validation gate). Don't restate those rules from memory — open the skill.

---

## ⚠️ Open decision #1 — narrative voice (confirm before writing)

The original game concept (your imported instructions) was **1st-person character chat** — the character texts the player ("Блин, кажется я сделал глупость…") and the player advises.

The **shipped engine + scenario-writer skill use 2nd-person "Ты" narration** ("Ты стоишь у прилавка…") — the player *is* the character, narrated like an interactive gamebook.

**This brief follows the shipped style (2nd-person "Ты"), to stay test-aligned.** All seed scenes below are written that way. If you'd rather have true 1st-person Lifeline chat, that's a deliberate engine/UX change (and a different `MessageSender.CHARACTER` rendering) — decide it once, globally, before authoring, because it rewrites every line. Flagging because the two source concepts conflict.

---

## 1. What the game is teaching (the pedagogy in one screen)

A consequence-driven personal-finance game set in Kazakhstan. **Each character owns one core financial lesson.** The player makes choices for the character; the *consequences arrive later* (next month, in 3 months, after a historical shock). The player learns because they're emotionally invested and because the **math punishes or rewards them** — never because the game lectures.

Three non-negotiables that follow:
- **Teach through failure.** Early mistakes must be cheap and survivable, so the player learns the lesson without rage-quitting.
- **Consequences are delayed.** A choice that "feels good now" (`expenses ↑`) should bite months later via `Effect.scheduleEvent` or a monthly-tick crisis. Delay is the teacher.
- **No free lunch.** Every option carries a tradeoff. There is no obviously-correct button; the "right" path is only clear in hindsight.

---

## 2. Core writing principles (apply to every event)

1. **One character, one lesson.** Stay in your character's lane (table §6). Cross-lesson moments are fine as *secondary* colour, never the spine.
2. **Show the number move, name the human stakes.** "Минус 180,000 ₸ в месяц. Дочь спрашивает, когда поедем к морю." Stakes > stats, but the stat must actually change.
3. **Two to four options, each a real strategy.** Save / spend / hedge / gamble — distinct philosophies, not "yes / yes but smaller."
4. **The lesson must be *demonstrably* learnable.** A wrong path must visibly fail (toward Bankruptcy / Paycheck-to-Paycheck); a disciplined path must visibly succeed (toward Stability / Freedom / Wealth). The sim harness will prove both are reachable.
5. **Multiple valid paths + 5 endings.** Replayability is a design requirement, not a bonus.
6. **Authentic locality.** Real prices, goods, places, and pressures of the era (see §5). Tenge, baursaki, той, Kaspi QR, "помоги родне", барахолка, аул→город.
7. **Realistic economics only.** No 500%/year returns, no risk-free riches. Deposits ~10–15%/yr, business is volatile, crypto is shown honestly as casino-grade risk. (See guardrails §3.)

---

## 3. Guardrails (hard limits — never cross)

- **Not financial advice.** It's a story. Never phrase an outcome as a recommendation to the *player's real life*. Keep the "entertainment + learning" framing; outcomes belong to the character.
- **Realistic returns.** No magical multipliers. A pyramid/crypto pump can *promise* absurd returns — that's the scam — but the modeled consequence must be loss, framed as a lesson (`learned.scam.{type}`).
- **Don't glorify crime, corruption, or gambling.** Bolat's blat arc and all scam arcs are **cautionary**. The player may choose the dark path, but the downside risk must be real and the framing must let them see the cost. No how-to detail for real fraud.
- **No tax or legal advice.** Treat as setting, not instruction.
- **Age-appropriate.** Minors may play. No explicit sexual content, no graphic violence, no self-harm framing. Hardship is shown with dignity, not exploitation.
- **Not an ad.** Real brands (Kaspi, Halyk, Freedom) appear only as neutral setting texture — never endorsed, never the "smart choice" by name.
- **Cultural care.** Желтоксан, Semipalatinsk/полигон, ethnicity, religion, family roles — handle with respect and specificity, never as stereotype or punchline.

---

## 4. Voice & message style

`message` / `story()` text (shipped style — see Open Decision #1):
- **2nd person singular:** Ты, Тебе, Твой, Твоя.
- **Present tense or vivid past:** "Ты открываешь конверт с первой зарплатой."
- **Short and grounded.** 1–3 short paragraphs via `story("…","…")`. Emotion over exposition.
- **Historically/culturally specific.** Name the real thing (барахолка, «КамАЗ муки», курс 4.7, ипотека под 14%).
- **Emoji = mood,** one per event via `flavor` (😰💰🏦🚗📉🎉). Don't pepper the body with them.

`option()` text:
- **Start with an infinitive verb:** "Отложить", "Взять кредит", "Отказаться", "Спросить совета у брата".
- **Concrete and quantified:** "Взять рассрочку на iPhone — 25,000 ₸/мес", not "Купить телефон".
- **Imply risk without spoiling:** "Рискнуть — всё или ничего".
- **≤ ~15 words.**

Tone: grounded, slightly tense, warm, **never preachy.** If a sentence sounds like a lesson, cut it — let the next month's report be the lesson.

Supporting cast speak in quotes inside the narration: `Брат пишет: «Вписывайся, я уже поднял 200 тысяч».`

---

## 5. Story architecture every character follows

**Graph anatomy** (full contract in the skill): `initialPlayerState`, `events` map (story + endings), `conditionalEvents` (priority-checked), `eventPool` (`PoolEntry(id, weight)`). `normal_life` **must** exist. Prefer the **arc pattern** (`buildEvents()` over `EventArc`s, see `AsanScenarioGraph` + `arcs/EraCharacterArcs.kt`) for large graphs.

**Three-act shape** (map chapters to months):

- **Act I — Hook (≈3–5 unique story events).** Establish the character, the starting squeeze, and the lesson's first temptation. End Act I on a choice that sets the arc's direction (a flag).
- **Act II — Pressure (conditionals + pool + era events).** The lesson *bites*. Delayed consequences land. A historical world event (era global) tests them. Recurring temptations (scams, lifestyle creep, family asks) drawn from the pool. This is most of the playtime.
- **Act III — Reckoning (`final_review` → ending triggers → 5 terminal endings).** State resolves to one of the 5 `EndingType`s. Build per the skill's ending pattern (terminal = `options = emptyList()`; reached via trigger events/conditionals; `ending_bankruptcy_trigger` for the floor).

**The teaching loop (reuse this rhythm):**
```
temptation/decision  →  immediate small effect (feels fine)
                     →  scheduleEvent(consequence, afterMonths = 2–6)
                     →  monthly tick / era shock amplifies it
                     →  reckoning beat: the number forces a new choice
```

**Decision templates** (parameterise per character):
- *Lifestyle-creep:* offer a nice thing → `expensesDelta+` (sticky) now, dopamine → a shock later exposes the raised baseline. (Marat, Aidana)
- *Working-capital cycle:* buy stock → `capitalDelta−`, `debtDelta+` now → `scheduleEvent("sold", 1)` whose payout depends on a market flag. (Aigül)
- *Scam touchpoint:* tempting offer, `Condition.NotFlag("learned.scam.{t}")` → take it = loss + `lost_money_to_scam`; refuse = set `learned.scam.{t}` + small `knowledge↑`. (everyone; library in `ScamEventLibrary` + `SCENARIO_REFERENCE.md`)
- *Human-capital fork:* spend `capital`/time now on a skill/education → delayed `incomeDelta+`. (Serik, Timur, Zeynep-for-daughter)
- *Privilege/shortcut:* use a connection/blat → gain now, `scheduleEvent("investigation_risk", n)` tail risk. (Bolat)

---

## 6. Lesson ownership map (stay in lane)

| Character | OWNS (spine) | May touch (colour) | Must NOT carry |
|-----------|--------------|--------------------|----------------|
| Daniyar | Literacy from zero, budgeting basics | first scams, building a network | investing, business scaling |
| Aigül | Entrepreneurship: working capital & cash flow | debt, pyramids, family asks | salaried budgeting, investing theory |
| Marat | Lifestyle inflation & concentration risk | consumer credit, FX | entrepreneurship, ethics |
| Zeynep | Social capital + education-vs-cushion + deficit repair | insurance/health, informal loans | business, modern investing |
| Serik | Human-capital obsolescence & requalification | diversification of income, inflation | scams, entrepreneurship-from-zero |
| Bolat | Ethics/legality + converting privilege | due diligence, fraud, asset buying | budgeting basics |
| Timur | Income diversification & human-capital investment | FX earnings, entrepreneurship | survival budgeting, corruption |
| Aidana | First salary: emergency fund, consumer-credit traps, first investing | lifestyle creep, crypto hype | business, Soviet-era themes |

**Cross-cutting (not owned by anyone):** inflation/devaluation (era events), scams (`ScamEventLibrary`, every character meets ≥1 fitting type).

---

## 7. Starting states — reconciled to engine scale

Anchors below obey the era's money scale (from the era shells in `Scenarios.kt`) and the skill's heuristics. **Final numbers are tuned in the sim harness** — treat these as the starting point, not gospel. "Pressure point" = the stat that creates the arc's tension. Flag intentional deviations so the harness/reviewer doesn't "correct" them.

| # | Character | eraId | year | cur | capital | income | expenses | debt | debtPay | stress | know | risk | start flags | pressure point (⚠ = intentional deviation) |
|---|-----------|-------|------|-----|---------|--------|----------|------|---------|--------|------|------|-------------|--------------------------------------------|
| 1 | Daniyar | kz_90s | 1994 | KZT | 3 000 | 6 000 | 5 000 | 0 | 0 | 45 | 5 | 25 | — | ⚠ capital <1× expenses; knowledge 5 |
| 2 | Aigül | kz_90s | 1994 | KZT | 30 000 | 0 | 18 000 | 12 000 | 4 000 | 65 | 20 | 40 | `single_mother` | ⚠ income 0 — earns via scheduled sale events |
| 3 | Marat | **kz_2015** | 2014 | KZT | 500 000 | 700 000 | 220 000 | 0 | 0 | 15 | 8 | 45 | — | ⚠ huge surplus + knowledge 8 → he'll inflate it |
| 4 | Zeynep | kz_90s | 1991 | RUB | 1 500 | 220 | 240 | 200 | 10 | 50 | 80 | 15 | `single_mother`, `chronic_illness` | ⚠ expenses>income (deficit to repair) |
| 5 | Serik | kz_90s | 1991 | RUB | 2 200 | 320 | 200 | 0 | 0 | 20 | 70 | 5 | `closed_city` | income collapses (scheduled); knowledge is *specific* |
| 6 | Bolat | kz_90s | 1991 | RUB | 5 000 | 480 | 280 | 0 | 0 | 25 | 50 | 10 | `party_member`, `has_dacha`, `has_blat` | ⚠ wealthy start — must convert before the system dies |
| 7 | Timur | kz_2024 | 2024 | KZT | 80 000 | 200 000 | 120 000 | 0 | 0 | 30 | 20 | 30 | `skill.tech` | variable freelance income; capital-poor |
| 8 | Aidana | kz_2024 | 2024 | KZT | 150 000 | 350 000 | 280 000 | 90 000 | 15 000 | 35 | 12 | 20 | — | ⚠ ~0 emergency fund + creep |

**Currency & era notes (critical):**
- `kz_90s` spans 1991–2000. Use **`CurrencyCode.RUB` only for starts before Nov 1993** (Serik/Zeynep/Bolat, 1991). They live through the **RUB→KZT redenomination** (`era_tenge_introduced`, Nov 1993). *Verify how the reform is applied* (`MonetaryReform` 500:1 — confirm whether `era_tenge_introduced` in `EraEventLibrary` carries it, or whether your graph must).
- Pre-1993 RUB figures are small (hundreds), then **hyperinflation 1992–93** erodes them — that erosion *is* the inflation lesson; don't pre-bake it into the start.
- Daniyar/Aigül start **1994 in KZT** (post-tenge) — their arcs are "build in the new money", and they catch `era_mmm_wave_90s` (Jun 1994) → the pyramid lesson.
- `investmentReturnRate` is **annual** (0.08 = 8%/yr).

**Era global events available** (auto-fire, don't define — just design around them):
- `kz_90s`: USSR collapse (Dec 91), tenge (Nov 93), MMM wave (Jun 94), Chechen-war broadcast (Dec 94), nuclear disarmament reaction (Apr 95), capital-move debate (Dec 97).
- `kz_2015`: **devaluation 2015 (Aug 15) — Marat's crash.**
- `kz_2024`: COVID (Mar 2020), devaluation (Mar 2022, p0.85). **Note:** both are *before* 2024 — so **Aidana/Timur starting 2024 miss them.** Deliver their market shocks via **authored pool/conditional events** (crypto-hype-crash, freelance-dry-spell, FX swing), or start them in 2022 to catch the devaluation. Decide per character.

---

## 8. The 8 character briefs

> Each brief = a writing prompt. Seed scenes are starters in the shipped 2nd-person style — extend them into full arcs via the skill's workflow.

### 1. Daniyar — «В город» · `daniyar_90s` · kz_90s · 1994 · KZT
- **Logline:** 22, из аула под Талдыкорганом, впервые в Алматы. В кармане 3 000 ₸ и ноль понимания, как тут устроены деньги.
- **Lesson:** money literacy from zero — what a budget is, why even a tiny buffer matters, how scams find the naive. *Secondary:* building a network (земляки) from nothing.
- **Voice & texture:** наивный, гордый, не хочет ударить в грязь лицом перед земляками. Стройка, общага, базар, переводы домой.
- **Supporting cast:** Нурлан (земляк-бригадир, даёт работу и сомнительные советы), мать (звонит, просит денег), сосед по общаге (зовёт в «быстрый заработок»).
- **Pressure point:** near-zero capital + knowledge 5 — one bad move = hungry month. Optional "hustler" branch (`risk`+30, reused from cut Dauren) for replay.
- **Era/scam hooks:** `era_mmm_wave_90s` (Jun 94) hits him at his most gullible → flagship pyramid lesson; market "развод" / fake-job middleman from `ScamEventLibrary`.
- **Beats:** *I* — first wage, first temptation (send all home? blow it? keep a buffer?). *II* — MMM/«быстрые деньги», learn-by-loss or learn-by-refusing; build a skill or a network. *III* — стабильная работа vs свой маленький бизнес.
- **Signature mechanics:** budget choices (`capitalDelta`, `expensesDelta`); first `learned.scam.pyramid`; `network.t1→t3` flags unlocking job/info events.
- **Win:** survives, learns to keep a buffer, builds network → Stability (Wealth via business branch). **Fail:** sends/loses everything, no buffer → Paycheck-to-Paycheck / Bankruptcy.
- **Seed scene:**
  ```
  event "intro" 🧧
  story: «Первая зарплата на стройке — 6 000 тенге наличными.
          Нурлан хлопает по плечу: "Вечером той, скидываемся по тысяче".
          Мать утром просила прислать на лекарства.»
  options:
    - "Отложить 2 000 ₸ в резерв, остальное по нуждам"  💰  (capital+2000, knowledge+2) → MONTHLY_TICK
    - "Послать почти всё матери, себе оставить на хлеб"   📮  (capital−, stress+, flag family.first) → MONTHLY_TICK
    - "Пойти на той — нельзя терять лицо"                 🥳  (capital−, stress−, risk+) → MONTHLY_TICK
  ```

### 2. Aigül — «Базар» · `aigul_90s` · kz_90s · 1994 · KZT
- **Logline:** 30, Шымкент, мать-одиночка. Муж уехал на заработки и пропал. Челночит товар из Кыргызстана, чтобы прокормить дочь.
- **Lesson:** entrepreneurship as **cash-flow timing** — buy stock, sell, reinvest vs draw; survive the month goods don't move; supplier credit, рэкет, конкуренция. *Secondary:* pyramids, family asks.
- **Voice & texture:** жёсткая, считает каждую тиынку, гордая. Барахолка, баулы, «КамАЗ товара», рэкетиры-«крыша».
- **Supporting cast:** дочь Амина (7, болеет некстати), оптовик Ваня (даёт товар в долг под %), рэкетир Серый («за место платят все»).
- **Pressure point:** income 0 — she only earns through scheduled **sale** events; a slow month bleeds capital. `single_mother` gates child-crisis events.
- **Era/scam hooks:** MMM 1994 («вложи выручку, удвоим»); финансовая пирамида как соблазн вместо реинвеста.
- **Beats:** *I* — first баул on credit: how much to buy, what margin. *II* — working-capital cycle bites (непроданный товар, порча, рэкет, налоговая); reinvest vs pull cash for Амина; pyramid temptation. *III* — лоток → точка → опт, или перекредитовалась и прогорела.
- **Signature mechanics:** working-capital template (`capitalDelta−`/`debtDelta+` → `scheduleEvent("goods_sold",1)` payout via market flag); `kiosk.opened` domain flags; рэкет = recurring `expensesDelta`.
- **Win:** disciplined reinvestment + says no to the pyramid → Stability → Wealth. **Fail:** over-leverages on Ваня's goods or dumps capital into MMM → Bankruptcy.
- **Seed scene:**
  ```
  event "intro" 🧳
  story: «Ваня отгружает баул турецкого трикотажа: "Бери на 30 тысяч,
          отдашь с выручки. Не продашь — долг твой".
          Амина кашляет вторую неделю. Аптека — это ещё 4 тысячи.»
  options:
    - "Взять полный баул — рискнуть на максимум"      📦  (debt+30000, capital+goods, stress+) → MONTHLY_TICK
    - "Взять половину, остальное — подушка на аптеку"  🧶  (debt+15000, capital buffer) → MONTHLY_TICK
    - "Сначала к врачу, товар потом"                   🏥  (capital−4000, stress−, flag child.first) → "amina_clinic"
  ```

### 3. Marat — «Нефть» · `marat_2015` · kz_2015 · 2014 · KZT
- **Logline:** 25, Атырау, вахтовик на нефтянке. Зарплата — мечта региона. Чувствует себя непробиваемым.
- **Lesson:** **lifestyle inflation & concentration risk** — rising income ≠ wealth; 100% dependence on one sector + credit = fragility when oil turns. *Secondary:* consumer credit, FX.
- **Voice & texture:** уверенный, щедрый, любит понты. Новый Land Cruiser, той на 300 человек, ипотека «потому что могу».
- **Supporting cast:** невеста Динара (хочет свадьбу-мечту), друг Ербол (зовёт в долю на ещё одну тачку), банковский менеджер (одобряет всё).
- **Pressure point:** huge surplus + knowledge 8 → he *will* ratchet `expenses`/`debt` up. The trap is self-inflicted.
- **Era/scam hooks:** **`era_devaluation_2015` (Aug 2015) is the detonator** — oil down, тенге обвал, валютная ипотека взрывается.
- **Beats:** *I* — first big bonus: save/invest vs upgrade life. *II* — lifestyle-creep template ratchets fixed costs; FX mortgage temptation; 2015 deval + oil crash slashes `income`. *III* — can he downshift fast enough, or does the raised baseline sink him?
- **Signature mechanics:** sticky `expensesDelta+` on every "ты заслужил" choice; `lifestyle.creep.tN`; car/flat `debtDelta`+`debtPaymentDelta`; era shock = big `incomeDelta−`.
- **Win:** keeps lifestyle modest, diversifies, no FX debt → Stability. **Fail (default):** ratcheted costs + FX loan + crash → Paycheck-to-Paycheck / Bankruptcy.
- **Seed scene:**
  ```
  event "intro" 🛢️
  story: «Премия за вахту — 700 000 тенге, и это не разовая.
          Ербол скидывает фото нового Prado: "Бери в кредит, нефть же вечная".
          Динара прислала ссылку на свадебный зал за 2 миллиона.»
  options:
    - "Отложить премию, жить на прежнем уровне"        💰  (capital+, knowledge+2) → MONTHLY_TICK
    - "Взять Prado в кредит — ты заслужил"             🚗  (debt+, debtPay+, expenses+, stress−) → MONTHLY_TICK
    - "Половину на свадьбу мечты, половину — на депозит" 💍  (capital−, expenses+, flag wedding.big) → MONTHLY_TICK
  ```

### 4. Zeynep — «Связи» · `zeynep_90s` · kz_90s · 1991 · RUB→KZT
- **Logline:** 45, Алма-Ата, врач-педиатр, мать-одиночка. Хроническая болезнь. Дочь-подросток мечтает учиться в Москве.
- **Lesson:** **social capital as real money** in a weak-institution economy + **invest-in-daughter vs build-own-cushion** + **deficit repair** (expenses>income at start). *Secondary:* health/insurance shock.
- **Voice & texture:** интеллигентная, усталая, бесконечно ответственная. Поликлиника, очереди, «достать по знакомству», дефицит лекарств.
- **Supporting cast:** дочь Камила (хочет в МГУ), коллега Роза (узел нетворкинга — лекарства, подработки, связи), бывший пациент-кооператор (предлагает «вложиться»).
- **Pressure point:** **deficit** (expenses 240 > income 220) + debt 200 — must repair cash flow before anything else. `chronic_illness` = scheduled stress/expense spikes.
- **Era/scam hooks:** гиперинфляция 1992–93 съедает сбережения и зарплату-бюджетницу; RUB→KZT redenomination; «вложись в кооператив» как риск/соблазн.
- **Beats:** *I* — close the monthly gap (подработки vs связи vs урезать). *II* — social-capital favours pay off (informal loans, дефицит по знакомству, info); inflation erodes the ruble; the big fork — пробить Камиле дорогу в Москву (delayed payoff, risk of losing her) vs свой резерв. *III* — конвертирует связи в устойчивость 90-х, или инфляция и болезнь добивают.
- **Signature mechanics (⚠ feasibility):** social capital = **flag tiers** `social.t1→t3` (favours unlock discount/loan/info events); health = `chronic_illness`-gated scheduled `stressDelta`/`expensesDelta`; education = human-capital fork on Камила (`scheduleEvent` years out). *If you want these numeric, see the `resources` map proposal in `CHARACTER_SELECTION_V2.md` §5.*
- **Win:** repairs deficit, banks favours, smart bet on Камила → Stability/Freedom. **Fail:** deficit + inflation + health spiral → Bankruptcy.
- **Seed scene:**
  ```
  event "intro" 🩺
  story: «Зарплату в поликлинике опять задержали, а цены растут каждую неделю.
          Роза шепчет: "Есть халтура — профосмотры на заводе, платят потом".
          Камила раскладывает на столе брошюру МГУ.»
  options:
    - "Взять халтуру через Розу — закрыть дыру"        🤝  (income+, flag social.t1, stress+) → MONTHLY_TICK
    - "Урезать всё до предела, копить на Камилу"        ✂️  (expenses−, stress+, flag save.kamila) → MONTHLY_TICK
    - "Занять у бывшего пациента под обещание"          📄  (capital+, debt+, risk+) → "patient_loan"
  ```

### 5. Serik — «Завод» · `serik_90s` · kz_90s · 1991 · RUB→KZT  *(renamed from Kimix "Marat")*
- **Logline:** 35, Усть-Каменогорск, инженер на «почтовом ящике» (ВПК). Секретность, гордость, knowledge 70 — но всё это про одну умирающую отрасль.
- **Lesson:** **human-capital obsolescence & requalification** — specific skill is priceless while the industry lives, worthless the day it dies; over-specialisation is risk; adaptability is insurance. *Secondary:* income diversification, inflation.
- **Voice & texture:** педантичный, лояльный, не верит, что «всё это» рухнет. Закрытый город, пропуска, КБ, «нам должны выплатить».
- **Supporting cast:** начальник КБ («потерпи, оборонзаказ вернётся»), брат в Алма-Ате (зовёт в кооператив/коммерцию), жена (давит: дети, реальность).
- **Pressure point:** income **collapses** on a schedule (завод месяцами не платит); knowledge 70 is *specific* (`defense_skill`) and doesn't transfer unless retrained.
- **Era/scam hooks:** USSR collapse (Dec 91) kills the order book; hyperinflation guts savings; tenge redenomination; «конверсия» schemes.
- **Beats:** *I* — wages start slipping: wait / co-op at the plant / retrain. *II* — the **timing puzzle**: leave too late (`left.defense.late`) = late-game income penalty; leave on time = keep `defense_skill` for bonus tech events; inflation erodes the «накопленное на книжке». *III* — переквалификация в новую экономику, или нищета достоинства.
- **Signature mechanics:** scheduled wage-collapse events; `closed_city` + `defense_skill`/`left.defense.late` flags gating late income; human-capital fork (retrain: `capital−`/time now → `incomeDelta+` later).
- **Win:** retrains/diversifies in time → Stability. **Fail:** loyal too long → Paycheck-to-Paycheck.
- **Seed scene:**
  ```
  event "intro" 🏭
  story: «Третий месяц зарплату "задерживают". В КБ говорят: "Оборонзаказ вот-вот вернётся".
          Брат звонит из Алма-Аты: "Бросай свой ящик, открываем кооператив, руки золотые нужны".
          На сберкнижке — 2 200 рублей, которые тают на глазах.»
  options:
    - "Ждать выплат — завод не бросают"                 ⏳  (stress+, flag loyal.plant) → MONTHLY_TICK
    - "Остаться, но подрабатывать в кооперативе при заводе" 🔧  (income+, knowledge+, compromise) → MONTHLY_TICK
    - "Уехать к брату в коммерцию"                      🚂  (risk+, flag chose.commerce, clear closed_city) → "almaty_coop"
  ```

### 6. Bolat — «Приватизация» · `bolat_90s` · kz_90s · 1991 · RUB→KZT
- **Logline:** 52, начальник цеха и бывший партсекретарь. Дача, связи, блат. Система, на которой держалась вся его жизнь, рушится — но у него есть фора.
- **Lesson:** **ethics/legality + converting privilege** — political/relationship capital converts to money; the uncomfortable truth that many «self-made» fortunes started from a non-zero, connected base; due diligence; the tail risk of grey zones. *Secondary:* asset buying, fraud spotting.
- **Voice & texture:** вальяжный, прагматичный, умеет «решать вопросы». Приватизационные чеки, «свои люди», красные директора, первые рэкет-войны.
- **Supporting cast:** замакима (предлагает завод по дешёвке «по-свойски»), молодой бандит-конкурент, честный юрист (предупреждает о фальшивых акциях).
- **Pressure point:** **the only wealthy start** — the question isn't survival, it's *what kind of man he becomes* and whether he converts before the window shuts (and before a «дело» catches him).
- **Era/scam hooks:** приватизация 1993–96; фальшивые акции (due-diligence test, reused from cut A3); рэкет-война tail risk; redenomination.
- **Beats:** *I* — first offer: privatise the цех clean, cut a corner, or stay a man of the old system. *II* — each blat use buys an asset but schedules `investigation_risk`; fake-share scam tests due diligence; конкуренты давят. *III* — «новый» с капиталом (clean or dirty), or sudden Bankruptcy if a case lands / a war turns.
- **Signature mechanics (⚠ feasibility):** blat = consumable via flags `blat.3→blat.1`, each use → `scheduleEvent("investigation_risk", n)` (possible `capitalDelta−` seizure / reputation); due-diligence branch (verify → avoid loss; skip → fake-share loss).
- **Win:** converts privilege, legitimises, stays clean enough → Wealth. **Fail:** over-uses blat / skips diligence / a case lands → Bankruptcy.
- **Seed scene:**
  ```
  event "intro" 🏛️
  story: «Замакима наливает коньяк: "Цех твой. По остаточной стоимости, по-свойски.
          Чеки я организую". В кармане — приватизационные купоны на полгорода.
          Юрист Виктор тихо: "Болат Аскарович, проверьте те акции, что вам сватают".»
  options:
    - "Взять цех по-свойски — грех не воспользоваться"   🥃  (capital+asset, use blat, schedule investigation_risk) → MONTHLY_TICK
    - "Оформить всё чисто, пусть дороже"                 📚  (capital−, knowledge+, flag clean.deal) → MONTHLY_TICK
    - "Сначала проверить акции у Виктора"               🔍  (flag did.diligence, knowledge+) → "fake_shares_check"
  ```

### 7. Timur — «Свой продукт» · `timur_2024` · kz_2024 · 2024 · KZT
- **Logline:** 20, Астана, студент-айтишник, фрилансит на Upwork. Скилл есть, денег нет, мир открыт.
- **Lesson:** **income diversification & human-capital investment** — resilience = multiple streams (job + freelance + product) + investing in skills + earning hard currency as FX insurance. *Secondary:* entrepreneurship, FX.
- **Voice & texture:** амбициозный, онлайн, немного циничный про «систему». Коворкинг, USD-инвойсы, «уехать или остаться», стартап-чатики.
- **Supporting cast:** одногруппник (зовёт в безопасную госслужбу), иностранный клиент (нестабильный, но платит в USD), ментор-фаундер (двигает к своему продукту).
- **Pressure point:** variable freelance income (lumpy) + capital-poor — feast/famine cash flow; the choice between safe salary and uncertain upside.
- **Era/scam hooks:** kz_2024 era events are pre-2024 → author shocks: freelance dry-spell, client non-payment, crypto-hype-crash; **FX swing** (тенге слабеет → USD-доход выигрывает = the diversification lesson made tangible).
- **Beats:** *I* — first $-client: take safe local gig vs risky USD client vs invest in a course. *II* — build streams (`stream.freelance/product/job`); dry spell tests the lack of buffer; FX move rewards hard-currency earners; safe-salary siren call. *III* — свой продукт / релокация / устойчивый фриланс, vs single-threaded burnout.
- **Signature mechanics:** stackable `incomeDelta` streams via flags; skill-course human-capital fork (`capital−` now → `incomeDelta+` later); USD income → `capitalDelta+` on devaluation events.
- **Win:** diversified streams + skill investment → Freedom/Wealth. **Fail:** single-threaded, no buffer, dry spell → Paycheck-to-Paycheck.
- **Seed scene:**
  ```
  event "intro" 💻
  story: «Первый зарубежный заказ: 500 долларов за лендинг — это три твоих стипендии.
          Клиент пишет на ломаном английском, дедлайн жёсткий.
          Одногруппник зовёт на стабильную практику в министерство: "там хоть оклад".»
  options:
    - "Взять заказ, вложить часть в курс по React"     📈  (capital+, capital− course, schedule income+) → MONTHLY_TICK
    - "Пойти на госпрактику ради стабильности"          🏢  (income steady+, flag chose.govjob) → MONTHLY_TICK
    - "Брать всё подряд, лишь бы платили"               🔥  (income+, stress+, risk+) → MONTHLY_TICK
  ```

### 8. Aidana — «Первая зарплата» · `aidana_2024` · kz_2024 · 2024 · KZT  *(NEW — launch beachhead)*
- **Logline:** 26, Алматы, маркетолог, первая «взрослая» зарплата 350 000 ₸. Снимает квартиру, телефон в рассрочку, подписки, кафе. Денег почему-то не остаётся.
- **Lesson:** the foundational loop for the target player — **budget a real salary, build a 3–6 month emergency fund, dodge Kaspi-рассрочка / credit-card traps, make a first investment** (депозит vs акции vs крипто-хайп). *Secondary:* lifestyle creep.
- **Voice & texture:** современная, в Instagram, тревожно-амбициозная. Kaspi QR, рассрочка «0-0-12», бранчи, «инвестируй как все», крипто-чатик подруги.
- **Supporting cast:** подруга Камила (зовёт в крипту/«сигналы»), мама (советует «деньги в недвижку/доллары»), коллега (молча копит и инвестирует в депозит).
- **Pressure point:** **~0 emergency fund** + existing рассрочка — one shock (job wobble, медрасход) and she's in the красная зона. Creep eats the surplus.
- **Era/scam hooks:** author them — крипто-хайп-крах, «инвест-наставник»/forex (`ScamEventLibrary`), market dip on her first depo/stocks. (`Condition.NotFlag("learned.scam.crypto")` etc.)
- **Beats:** *I* — first salary: spend / save / "invest like everyone". *II* — Kaspi-рассрочка temptations ratchet `debtPayment`; build the buffer (`has_emergency_fund`); first investment fork, then a market/crypto swing tests it; lifestyle creep at consumer scale. *III* — подушка + разумные инвестиции (Stability→Freedom) vs рассрочка-спираль (Paycheck-to-Paycheck).
- **Signature mechanics:** `has_emergency_fund` — note `Condition.Stat` compares to a **constant**, not `3×expenses`; gate on a fixed anchor (`Stat(CAPITAL, GTE, 840_000)` ≈3×280k) **or** set the flag in an event effect when she crosses it. Kaspi-installment events (`debtDelta+`,`debtPaymentDelta+`); first-investment branch sets `investments`+`investmentReturnRate` then exposed to an authored swing; creep template like Marat at consumer scale.
- **Win:** builds buffer, says no to рассрочка, invests boringly → Stability→Freedom. **Fail:** creep + рассрочка + chases crypto hype → Paycheck-to-Paycheck.
- **Seed scene:**
  ```
  event "intro" 💼
  story: «Первая зарплата упала на Kaspi — 350 000 ₸. Чувство — будто богатая.
          Сторис подруги Камилы: "+40% на крипте за неделю, пишите, скину сигнал".
          В приложении баннер: "iPhone 16 в рассрочку 0-0-24, всего 35 000 ₸/мес".»
  options:
    - "Отложить 100 000 ₸ на подушку, остальное по бюджету"  💰  (capital+, knowledge+2) → MONTHLY_TICK
    - "Взять iPhone в рассрочку — все берут"                  📱  (debt+, debtPay+35000, expenses+, stress−) → MONTHLY_TICK
    - "Закинуть 50 000 ₸ в крипту по сигналу Камилы"          🎰  (investments+ high-risk, risk+, NotFlag learned.scam.crypto) → "crypto_signal"
  ```

---

## 9. Per-character authoring checklist (copy this per character)

```
[ ] eraId + characterId set; PlayerState from §7 (note any intentional deviation)
[ ] currency/year correct for era (RUB only pre-Nov-1993; redenomination handled)
[ ] Act I: 3–5 unique story events, ends on a direction-setting flag
[ ] Act II: 8–12 conditionals (incl. ending_bankruptcy_trigger, debt_crisis, burnout_{era}),
    30–50 pool events; thresholds scaled to era money (StoryBalance)
[ ] ≥1 scam touchpoint fitting this character (tags scam + scam.{type}; learned.scam.{type})
[ ] designed AROUND the era global events that fire in this window (§7)
[ ] normal_life exists; its pool weight > any single scam event
[ ] all 5 endings: terminal events options=emptyList(), reached via triggers
[ ] the lesson is demonstrably learnable: a wrong path fails, a disciplined path succeeds
[ ] voice = 2nd person "Ты"; options = infinitive + concrete number; never preachy
[ ] guardrails (§3) respected
[ ] registered in ScenarioGraphFactory.buildGraph + ScenarioTestCatalog.combos
[ ] VALIDATION GATE (skill): validate_scenarios.py → :shared compile → :shared:test all green
```

## 10. Definition of done (per character)

A character is done when: it **compiles**, is **registered** (factory + test catalog), the **sim harness passes** (40 seeds × up to 800 steps: no crash, no dead-end, no out-of-bounds stat, all 5 endings reachable from `intro`), the **lesson is demonstrably teachable** (a clearly wrong play trends to Bankruptcy/P2P, a disciplined play trends to Stability/Freedom/Wealth), the **voice is consistent**, and **all guardrails hold**. Never weaken a test to pass the gate — fix the content.

## 11. Suggested build order

1. **Aidana** (kz_2024) — launch beachhead, simplest era, highest relatability. First playable demo.
2. **Timur** (kz_2024) — same era, reuses modern systems.
3. **Marat** (kz_2015) — reuses `era_devaluation_2015` as the crash.
4. **Daniyar, Aigül** (kz_90s, 1994 KZT) — new-economy 90s pair; MMM pyramid lesson.
5. **Serik, Zeynep, Bolat** (kz_90s, 1991 RUB→KZT) — Soviet-collapse trio; build last (currency reform + the two flag-staged soft-currency mechanics). Decide the optional `resources` map (Selection doc §5) before Zeynep/Bolat.
