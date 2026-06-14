package kz.fearsom.financiallifev2.i18n

import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.model.GameState
import kz.fearsom.financiallifev2.scenarios.ScenarioGraphFactory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LocaleRefreshRegressionTest {

    private val originalLocale = Strings.currentLocale

    @AfterTest
    fun tearDown() {
        Strings.currentLocale = originalLocale
    }

    @Test
    fun `scenario graph returns empty era intro`() {
        Strings.currentLocale = "ru"
        val intro = ScenarioGraphFactory
            .forCharacter("asan", "kz_2024")
            .findEvent("intro")

        assertTrue(intro?.message?.contains("Сценарий эпохи") == true)
        assertTrue(intro.isEnding)
        assertTrue(intro.options.isEmpty())
    }

    @Test
    fun `loaded game state relocalizes empty era intro when character name is supplied`() {
        Strings.currentLocale = "ru"
        val engine = GameEngine.forCharacterAndEra("asan", "kz_2024")
        val ruState = engine.startGame(characterName = "Амир Нурланов")
        val ruIntro = ruState.messages.last().text

        Strings.currentLocale = "en"
        engine.loadState(ruState, "Амир Нурланов")
        val enState = engine.state.value!!

        assertNotEquals(ruIntro, enState.messages.last().text)
        assertTrue(enState.messages.last().text.contains(Strings.eraModernKz2024Name))
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
