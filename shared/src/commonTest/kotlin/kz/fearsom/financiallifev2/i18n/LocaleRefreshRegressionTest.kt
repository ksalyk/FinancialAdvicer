package kz.fearsom.financiallifev2.i18n

import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.model.GameState
import kz.fearsom.financiallifev2.scenarios.ScenarioGraphFactory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotEquals

class LocaleRefreshRegressionTest {

    private val originalLocale = Strings.currentLocale

    @AfterTest
    fun tearDown() {
        Strings.currentLocale = originalLocale
    }

    @Test
    fun `scenario graph cache is locale aware`() {
        Strings.currentLocale = "ru"
        val ruOption = ScenarioGraphFactory
            .forCharacter("aidar", "kz_2024")
            .findEvent("intro")
            ?.options
            ?.first()
            ?.text

        Strings.currentLocale = "en"
        val enOption = ScenarioGraphFactory
            .forCharacter("aidar", "kz_2024")
            .findEvent("intro")
            ?.options
            ?.first()
            ?.text

        assertNotEquals(ruOption, enOption)
    }

    @Test
    fun `loaded game state is re-rendered in current locale`() {
        Strings.currentLocale = "ru"
        val engine = GameEngine.forCharacterAndEra("aidar", "kz_2024")
        val ruState = engine.startGame(characterName = Strings["seed_char_aidar_name"])
        val ruIntro = ruState.messages.last().text

        Strings.currentLocale = "en"
        engine.loadState(ruState, Strings["seed_char_aidar_name"])
        val enState = engine.state.value!!

        assertNotEquals(ruIntro, enState.messages.last().text)
        assertNotEquals(ruState.messages.first().text, enState.messages.first().text)
    }

    @Test
    fun `loaded game state without character name preserves saved text`() {
        Strings.currentLocale = "ru"
        val engine = GameEngine.forCharacterAndEra("aidar", "kz_2024")
        val ruState = engine.startGame(characterName = Strings["seed_char_aidar_name"])
        val legacyState = GameState(
            playerState = ruState.playerState,
            currentEventId = ruState.currentEventId,
            messages = ruState.messages,
            isWaitingForChoice = ruState.isWaitingForChoice,
            gameOver = ruState.gameOver,
            endingType = ruState.endingType
        )

        Strings.currentLocale = "en"
        val restoredEngine = GameEngine()
        restoredEngine.loadState(legacyState)
        val loadedState = restoredEngine.state.value!!

        kotlin.test.assertEquals(legacyState.messages, loadedState.messages)
    }
}
