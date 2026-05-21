package kz.fearsom.financiallifev2.i18n

import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.model.GameState
import kz.fearsom.financiallifev2.scenarios.ScenarioGraphFactory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocaleRefreshRegressionTest {

    private val originalLocale = Strings.currentLocale

    @AfterTest
    fun tearDown() {
        Strings.currentLocale = originalLocale
    }

    @Test
    fun `scenario graph returns Russian-first story content`() {
        Strings.currentLocale = "ru"
        val intro = ScenarioGraphFactory
            .forCharacter("asan", "kz_2024")
            .findEvent("intro")

        assertTrue(intro?.message?.contains("Амир") == true)
        assertTrue(intro.options.first().text.isNotEmpty())
    }

    @Test
    fun `loaded game state keeps Russian scenario content until localization pass`() {
        Strings.currentLocale = "ru"
        val engine = GameEngine.forCharacterAndEra("asan", "kz_2024")
        val ruState = engine.startGame(characterName = "Амир Нурланов")
        val ruIntro = ruState.messages.last().text

        Strings.currentLocale = "en"
        engine.loadState(ruState, "Амир Нурланов")
        val enState = engine.state.value!!

        assertEquals(ruIntro, enState.messages.last().text)
    }

    @Test
    fun `loaded game state without character name preserves saved text`() {
        Strings.currentLocale = "ru"
        val engine = GameEngine.forCharacterAndEra("asan", "kz_2024")
        val ruState = engine.startGame(characterName = "Амир Нурланов")
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
