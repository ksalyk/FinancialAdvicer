package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.model.ChatMessage
import kz.fearsom.financiallifev2.model.CurrencyCode
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.GameState
import kz.fearsom.financiallifev2.model.MessageSender
import kz.fearsom.financiallifev2.model.PendingEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.characters.AidarScenarioGraph
import kz.fearsom.financiallifev2.scenarios.characters.DaniyarScenarioGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ScenarioNarrativeRegressionTest {

    @Test
    fun `aidar 90s starts before tenge in rubles`() {
        val graph = ScenarioGraphFactory.forCharacter("aidar_90s", "kz_90s")
        val ps = graph.initialPlayerState

        assertEquals(1993, ps.year)
        assertEquals(1, ps.month)
        assertEquals(CurrencyCode.RUB, ps.currency)
    }

    @Test
    fun `2015 devaluation still wins over pool events on August 2015 tick`() {
        val engine = GameEngine.forCharacterAndEra("dana", "kz_2015")
        val graph = ScenarioGraphFactory.forCharacter("dana", "kz_2015")
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState.copy(month = 7, year = 2015),
                currentEventId = "normal_life",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Жанар"
        )

        val next = engine.makeChoice("save_cash")
        assertEquals("era_devaluation_2015", next.currentEventId)
    }

    @Test
    fun `2015 devaluation does not delete due zhanar story event`() {
        val engine = GameEngine.forCharacterAndEra("dana", "kz_2015")
        val graph = ScenarioGraphFactory.forCharacter("dana", "kz_2015")
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState.copy(
                    month = 7,
                    year = 2015,
                    pendingScheduled = listOf(PendingEvent("zhanar_forex_relative", 2015, 8))
                ),
                currentEventId = "normal_life",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Жанар"
        )

        val devaluation = engine.makeChoice("save_cash")
        assertEquals("era_devaluation_2015", devaluation.currentEventId)
        assertTrue(devaluation.playerState.pendingScheduled.any { it.eventId == "zhanar_forex_relative" })

        val story = engine.makeChoice("convert_to_dollar")
        assertEquals("zhanar_forex_relative", story.currentEventId)
    }

    @Test
    fun `2008 mortgage freeze wins over pool events on September 2008 tick`() {
        val engine = GameEngine.forCharacterAndEra("aidar", "kz_2005")
        val graph = ScenarioGraphFactory.forCharacter("aidar", "kz_2005")
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState.copy(month = 8, year = 2008),
                currentEventId = "normal_life",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Руслан"
        )

        val next = engine.makeChoice("do_nothing")
        assertEquals("era_mortgage_freeze_2008", next.currentEventId)
        assertEquals("mortgage", next.messages.last { it.sender == MessageSender.CHARACTER }.sceneTag)
    }

    @Test
    fun `2024 online credit rules wins over pool events on July 2024 tick`() {
        val engine = GameEngine.forCharacterAndEra("asan", "kz_2024")
        val graph = ScenarioGraphFactory.forCharacter("asan", "kz_2024")
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState.copy(month = 6, year = 2024),
                currentEventId = "normal_life",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Амир"
        )

        val next = engine.makeChoice("do_nothing")
        assertEquals("era_online_credit_rules_2024", next.currentEventId)
    }

    @Test
    fun `tenge reform converts ruble balances at 500 to 1`() {
        val engine = GameEngine.forCharacterAndEra("aidar_90s", "kz_90s")
        val graph = ScenarioGraphFactory.forCharacter("aidar_90s", "kz_90s")
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState.copy(month = 11, year = 1993),
                currentEventId = "era_tenge_introduced",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Айдар"
        )

        val next = engine.makeChoice("exchange_all")
        assertEquals(CurrencyCode.KZT, next.playerState.currency)
        assertEquals(53_000L, next.playerState.capital)
        assertEquals(15_000L, next.playerState.income)
        assertEquals(12_000L, next.playerState.expenses)
    }

    @Test
    fun `unique conditional event does not repeat after it was triggered once`() {
        val engine = GameEngine.forCharacterAndEra("asan", "kz_2024")
        val graph = ScenarioGraphFactory.forCharacter("asan", "kz_2024")
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState.copy(
                    capital = 600_000L,
                    financialKnowledge = 45
                ),
                currentEventId = "normal_life",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Амир"
        )

        val first = engine.makeChoice("save_cash")
        assertEquals("investment_unlock", first.currentEventId)

        val afterSkip = engine.makeChoice("skip_investing")
        assertTrue("investment_unlock" in afterSkip.playerState.triggeredUniqueEvents)

        val reloaded = GameEngine.forCharacterAndEra("asan", "kz_2024")
        reloaded.loadState(
            afterSkip.copy(
                currentEventId = "normal_life",
                isWaitingForChoice = true,
                gameOver = false,
                endingType = null
            ),
            characterName = "Амир"
        )

        val second = reloaded.makeChoice("save_cash")
        assertNotEquals("investment_unlock", second.currentEventId)
    }

    @Test
    fun `daniyar final review can reach terminal ending with type`() {
        val engine = GameEngine.forCharacterAndEra("daniyar", "kz_2005")
        val graph = ScenarioGraphFactory.forCharacter("daniyar", "kz_2005")
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState.copy(month = 1, year = 2006),
                currentEventId = "final_review",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Данияр"
        )

        val trigger = engine.makeChoice("final_check")
        assertEquals("ending_regular_trigger", trigger.currentEventId)

        val ending = engine.makeChoice("claim_regular")
        assertTrue(ending.gameOver)
        assertEquals(EndingType.PAYCHECK_TO_PAYCHECK, ending.endingType)
    }

    @Test
    fun `daniyar direct story navigation does not duplicate same scheduled event`() {
        val engine = GameEngine.forCharacterAndEra("daniyar", "kz_2005")
        val graph = ScenarioGraphFactory.forCharacter("daniyar", "kz_2005")
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState,
                currentEventId = "daniyar_garage_formalize",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Данияр"
        )

        val next = engine.makeChoice("daniyar_register_ip")
        assertEquals("daniyar_village_call", next.currentEventId)
        assertTrue(next.playerState.pendingScheduled.none { it.eventId == "daniyar_village_call" })
    }

    @Test
    fun `ruslan 2005 story naturally reaches mortgage freeze before final review`() {
        val engine = GameEngine(graph = Ruslan2005StoryOnlyGraph(), eraDefinition = EraRegistry.findById("kz_2005"))
        engine.startGame(characterName = "Руслан")

        assertEquals("ruslan_bank_offer", engine.makeChoice("ruslan_build_reserve").currentEventId)
        assertEquals("investment_unlock", engine.makeChoice("ruslan_sell_clean").currentEventId)
        assertEquals("ruslan_presale_flat", engine.makeChoice("skip_investing").currentEventId)
        assertEquals("scam_presale_checked", engine.makeChoice("ruslan_check_builder").currentEventId)
        assertEquals("normal_life", engine.makeChoice("presale_lesson").currentEventId)
        assertEquals("ruslan_wedding_credit", engine.makeChoice("save_cash").currentEventId)
        assertEquals("normal_life", engine.makeChoice("ruslan_delay_wedding").currentEventId)

        val freeze = advanceUntil(engine, "era_mortgage_freeze_2008")
        assertTrue(freeze.playerState.pendingScheduled.any { it.eventId == "final_review" })

        val review = engine.makeChoice("freeze_restructure")
        assertEquals("final_review", review.currentEventId)
    }

    @Test
    fun `daniyar 2005 story naturally reaches mortgage freeze before final review`() {
        val engine = GameEngine(graph = Daniyar2005StoryOnlyGraph(), eraDefinition = EraRegistry.findById("kz_2005"))
        engine.startGame(characterName = "Данияр")

        assertEquals("daniyar_garage_formalize", engine.makeChoice("daniyar_refuse_chat").currentEventId)
        assertEquals("daniyar_village_call", engine.makeChoice("daniyar_register_ip").currentEventId)
        assertEquals("daniyar_presale_flat", engine.makeChoice("daniyar_send_plan").currentEventId)
        assertEquals("daniyar_wedding_credit", engine.makeChoice("daniyar_check_builder").currentEventId)
        assertEquals("daniyar_investment_unlock", engine.makeChoice("daniyar_refuse_toi").currentEventId)

        val freeze = advanceUntil(engine, "era_mortgage_freeze_2008", firstOption = "daniyar_skip_deposit")
        assertTrue(freeze.playerState.pendingScheduled.any { it.eventId == "final_review" })

        val review = engine.makeChoice("freeze_restructure")
        assertEquals("final_review", review.currentEventId)
    }

    @Test
    fun `low cash high stress gets escape hatch before burnout`() {
        val graph = Ruslan2005StoryOnlyGraph()
        val engine = GameEngine(graph = graph, eraDefinition = EraRegistry.findById("kz_2005"))
        engine.loadState(
            GameState(
                playerState = graph.initialPlayerState.copy(
                    capital = 100_000L,
                    debt = 0L,
                    debtPaymentMonthly = 0L,
                    stress = 90,
                    month = 1,
                    year = 2006
                ),
                currentEventId = "normal_life",
                messages = listOf(ChatMessage(sender = MessageSender.SYSTEM, text = "seed")),
                isWaitingForChoice = true
            ),
            characterName = "Руслан"
        )

        val next = engine.makeChoice("save_cash")
        assertEquals("stress_escape_hatch", next.currentEventId)
    }

    @Test
    fun `marat safe path survives tenge reform and reaches ending`() {
        val graph = MaratStoryOnlyGraph()
        val engine = GameEngine(graph = graph, eraDefinition = EraRegistry.findById("kz_90s"))
        engine.startGame(characterName = "Марат")

        assertEquals("marat_family_pyramid", engine.makeChoice("marat_stop_family").currentEventId)
        assertEquals("marat_bazaar_counter", engine.makeChoice("marat_explain_pyramid").currentEventId)
        assertEquals("investment_unlock", engine.makeChoice("marat_contract_notebook").currentEventId)
        assertEquals("marat_supplier_prepay", engine.makeChoice("skip_investing").currentEventId)
        assertEquals("scam_middleman_contract_refused", engine.makeChoice("marat_verify_supplier").currentEventId)
        assertEquals("normal_life", engine.makeChoice("middleman_lesson").currentEventId)
        assertEquals("marat_wedding", engine.makeChoice("save_cash").currentEventId)
        assertEquals("normal_life", engine.makeChoice("marat_delay_wedding").currentEventId)

        var state = engine.state.value ?: error("State missing")
        repeat(4) {
            state = engine.makeChoice("save_cash")
        }

        assertEquals("era_tenge_introduced", state.currentEventId)
        assertTrue(state.playerState.pendingScheduled.any { it.eventId == "final_review" })

        val review = engine.makeChoice("exchange_all")
        assertEquals(CurrencyCode.KZT, review.playerState.currency)
        assertEquals("final_review", review.currentEventId)

        val trigger = engine.makeChoice("final_check")
        assertTrue(trigger.currentEventId in setOf(
            "ending_wealth_trigger",
            "ending_freedom_trigger",
            "ending_stability_trigger",
            "ending_paycheck_trigger",
            "ending_regular_trigger"
        ))

        val ending = engine.makeChoice(engine.currentOptions().single().id)
        assertTrue(ending.gameOver)
        assertTrue(ending.endingType != null)
    }

    private class MaratStoryOnlyGraph : ScenarioGraph() {
        private val delegate = Aidar90sScenarioGraph()

        override val initialPlayerState: PlayerState = delegate.initialPlayerState
        override val events: Map<String, GameEvent> = delegate.events
        override val conditionalEvents: List<GameEvent> = delegate.conditionalEvents
        override val eventPool: List<PoolEntry> = emptyList()
    }

    private class Ruslan2005StoryOnlyGraph : ScenarioGraph() {
        private val delegate = AidarScenarioGraph()

        override val initialPlayerState: PlayerState = delegate.initialPlayerState
        override val events: Map<String, GameEvent> = delegate.events
        override val conditionalEvents: List<GameEvent> = delegate.conditionalEvents
        override val eventPool: List<PoolEntry> = emptyList()
    }

    private class Daniyar2005StoryOnlyGraph : ScenarioGraph() {
        private val delegate = DaniyarScenarioGraph()

        override val initialPlayerState: PlayerState = delegate.initialPlayerState
        override val events: Map<String, GameEvent> = delegate.events
        override val conditionalEvents: List<GameEvent> = delegate.conditionalEvents
        override val eventPool: List<PoolEntry> = emptyList()
    }

    private fun advanceUntil(
        engine: GameEngine,
        targetEventId: String,
        firstOption: String? = null
    ): GameState {
        var state = engine.state.value ?: error("State missing")
        var pendingFirstOption = firstOption
        repeat(60) {
            if (state.currentEventId == targetEventId) return state
            val optionId = pendingFirstOption ?: when (state.currentEventId) {
                "normal_life" -> "save_cash"
                "investment_unlock" -> "skip_investing"
                "daniyar_investment_unlock" -> "daniyar_skip_deposit"
                "burnout_warning" -> "burnout_rules"
                "daniyar_burnout" -> "daniyar_burnout_talk"
                "debt_crisis" -> "debt_restructure"
                "stress_escape_hatch" -> "escape_minimize_month"
                else -> error("Unexpected event while advancing: ${state.currentEventId}")
            }
            pendingFirstOption = null
            state = engine.makeChoice(optionId)
        }
        error("Did not reach $targetEventId; stopped at ${state.currentEventId}")
    }
}
