package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.data.SeedData
import kz.fearsom.financiallifev2.scenarios.characters.AidarScenarioGraph
import kz.fearsom.financiallifev2.scenarios.characters.AsanScenarioGraph
import kz.fearsom.financiallifev2.scenarios.characters.DanaScenarioGraph
import kz.fearsom.financiallifev2.scenarios.characters.ErbolatScenarioGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ScenarioGraphFactoryTest {

    @Test
    fun `factory returns expected graph for each supported character`() {
        assertIs<Aidar90sScenarioGraph>(ScenarioGraphFactory.forCharacter("aidar_90s", "kz_90s"))
        assertIs<AidarScenarioGraph>(ScenarioGraphFactory.forCharacter("aidar", "kz_2005"))
        assertIs<AidarScenarioGraph>(ScenarioGraphFactory.forCharacter("aidar", "kz_2015"))
        assertIs<AidarScenarioGraph>(ScenarioGraphFactory.forCharacter("aidar", "kz_2024"))
        assertIs<AsanScenarioGraph>(ScenarioGraphFactory.forCharacter("asan", "kz_2024"))
        assertIs<DanaScenarioGraph>(ScenarioGraphFactory.forCharacter("dana", "kz_2005"))
        assertIs<DanaScenarioGraph>(ScenarioGraphFactory.forCharacter("dana", "kz_2015"))
        assertIs<DanaScenarioGraph>(ScenarioGraphFactory.forCharacter("dana", "kz_2024"))
        assertIs<ErbolatScenarioGraph>(ScenarioGraphFactory.forCharacter("erbolat", "kz_2015"))
        assertIs<ErbolatScenarioGraph>(ScenarioGraphFactory.forCharacter("erbolat", "kz_2024"))
    }

    @Test
    fun `seed data exposes only compatible predefined characters per era`() {
        val characterIds = SeedData.predefinedCharacters.map { it.id }.toSet()
        SeedData.eras.forEach { era ->
            era.availableCharacterIds.forEach { characterId ->
                assertTrue(characterId in characterIds, "Missing predefined character $characterId for era ${era.id}")
                val character = SeedData.predefinedCharacters.first { it.id == characterId }
                assertTrue(era.id in character.compatibleEraIds, "Character $characterId is exposed in ${era.id} but is not compatible")
            }
        }
    }

    @Test
    fun `asan is available only in 2024 era`() {
        val asan = SeedData.predefinedCharacters.first { it.id == "asan" }
        assertEquals(listOf("kz_2024"), asan.compatibleEraIds)
        val era2024 = SeedData.eras.first { it.id == "kz_2024" }
        assertTrue("asan" in era2024.availableCharacterIds)
    }
}
