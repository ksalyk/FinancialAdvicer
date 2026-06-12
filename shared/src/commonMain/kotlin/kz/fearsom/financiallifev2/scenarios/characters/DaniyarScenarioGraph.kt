package kz.fearsom.financiallifev2.scenarios.characters

import kz.fearsom.financiallifev2.model.CurrencyCode
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.arcs.StoryBalance
import kz.fearsom.financiallifev2.scenarios.arcs.buildEvents
import kz.fearsom.financiallifev2.scenarios.arcs.daniyar2005Conditionals
import kz.fearsom.financiallifev2.scenarios.arcs.daniyar2005Endings
import kz.fearsom.financiallifev2.scenarios.arcs.daniyar2005StoryArc
import kz.fearsom.financiallifev2.scenarios.arcs.endingsArc
import kz.fearsom.financiallifev2.scenarios.arcs.regularLifeArc
import kz.fearsom.financiallifev2.scenarios.arcs.storyEventPool

/**
 * Daniyar Akhmetov, 31, auto mechanic in Shymkent during the 2005-2010 credit boom.
 *
 * Story arc themes:
 *  1. Grey-import cash scheme pitched by an old friend (tax/customs risk)
 *  2. Whether to formalize the garage as a sole proprietorship (ИП)
 *  3. Family pressure from the village (roof, mother's medicine, sister's tuition)
 *  4. Off-plan apartment scam at 30% below market price
 *  5. The 2008 mortgage freeze as a global era event
 *
 * All event text is read from the kk/ru/en i18n maps (evt_daniyar_* keys).
 * Daniyar's endings override the standard `ending_wealth` / `ending_stability` /
 * `ending_freedom` / `ending_regular` / `ending_bankruptcy` events with
 * localized text, so the `storyConditionals` triggers route to them
 * automatically via the standard IDs.
 */
class DaniyarScenarioGraph(eraId: String = "kz_2005") : ScenarioGraph() {

    override val initialPlayerState: PlayerState = PlayerState(
        capital = 400_000L,
        income = 200_000L,
        expenses = 130_000L,
        debt = 0L,
        debtPaymentMonthly = 0L,
        investments = 0L,
        investmentReturnRate = 0.06,
        stress = 38,
        financialKnowledge = 18,
        riskLevel = 30,
        month = 3,
        year = 2005,
        characterId = "daniyar",
        eraId = "kz_2005",
        currency = CurrencyCode.KZT
    )

    // Order matters: `daniyar2005Endings()` is listed AFTER `endingsArc()` so its
    // entries overwrite the standard Russian-only endings with localized text.
    override val events: Map<String, GameEvent> = listOf(
        daniyar2005StoryArc(),
        regularLifeArc("kz_2005"),
        endingsArc(),
        daniyar2005Endings()
    ).buildEvents()

    override val conditionalEvents: List<GameEvent> = daniyar2005Conditionals(
        StoryBalance(
            stabilityCapital = 950_000L,
            freedomCapital = 2_800_000L,
            wealthCapital = 7_500_000L,
            debtCrisisDebt = 1_300_000L,
            debtCrisisCapital = 200_000L,
            investmentTicket = 130_000L
        )
    )

    override val eventPool: List<PoolEntry> = storyEventPool("kz_2005")
}
