package kz.fearsom.financiallifev2.scenarios.characters

import kz.fearsom.financiallifev2.model.CurrencyCode
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.arcs.StoryBalance
import kz.fearsom.financiallifev2.scenarios.arcs.buildEvents
import kz.fearsom.financiallifev2.scenarios.arcs.endingsArc
import kz.fearsom.financiallifev2.scenarios.arcs.regularLifeArc
import kz.fearsom.financiallifev2.scenarios.arcs.serik2005Endings
import kz.fearsom.financiallifev2.scenarios.arcs.serik2005StoryArc
import kz.fearsom.financiallifev2.scenarios.arcs.storyConditionals
import kz.fearsom.financiallifev2.scenarios.arcs.storyEventPool

/**
 * Serik Zhumabekov, 35, physics teacher in Karaganda during the 2005-2010
 * oil & credit boom — a "regular man" on a small salary while the economy roars.
 *
 * Story arc themes (teaching good-time patterns + entrepreneurship):
 *  1. Turn evening tutoring into a real learning centre (build an asset, not just earn a wage)
 *  2. Off-plan "foundation-pit" apartment scam at a boom-time discount (scam.presale)
 *  3. Mortgage discipline: a modest tenge loan (an appreciating asset) vs. a stretched
 *     dollar loan (currency-risk trap punished by the 2009 devaluation)
 *  4. Consumer-credit temptation — a car and a big toi "because everyone does it"
 *  5. The 2008 mortgage freeze (era_mortgage_freeze_2008) as the moment the good times end
 *
 * Low income + low starting knowledge make this a HARD start: the boom rewards
 * those who convert labour into assets and punishes those who lever up to consume.
 *
 * All event text is read from the kk/ru/en i18n maps (evt_serik_* keys). Serik's
 * endings override the standard ending_* events with localized text; build order
 * (serik2005Endings AFTER endingsArc) makes the override win, and the standard
 * storyConditionals triggers route to them via the shared ending IDs.
 */
class SerikScenarioGraph(eraId: String = "kz_2005") : ScenarioGraph() {

    override val initialPlayerState: PlayerState = PlayerState(
        capital = 150_000L,
        income = 50_000L,
        expenses = 32_000L,
        debt = 0L,
        debtPaymentMonthly = 0L,
        investments = 0L,
        investmentReturnRate = 0.06,
        stress = 44,
        financialKnowledge = 16,
        riskLevel = 18,
        month = 2,
        year = 2005,
        characterId = "serik",
        eraId = "kz_2005",
        currency = CurrencyCode.KZT
    )

    // Order matters: `serik2005Endings()` is listed AFTER `endingsArc()` so its
    // entries overwrite the standard Russian-only endings with localized text.
    override val events: Map<String, GameEvent> = listOf(
        serik2005StoryArc(),
        regularLifeArc("kz_2005"),
        endingsArc(),
        serik2005Endings()
    ).buildEvents()

    override val conditionalEvents: List<GameEvent> = storyConditionals(
        StoryBalance(
            stabilityCapital = 800_000L,
            freedomCapital = 2_200_000L,
            wealthCapital = 6_000_000L,
            debtCrisisDebt = 1_100_000L,
            debtCrisisCapital = 170_000L,
            investmentTicket = 120_000L
        )
    )

    override val eventPool: List<PoolEntry> = storyEventPool("kz_2005")
}
