package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.i18n.Strings

data class EraDefinition(
    val id: String,
    val name: String,
    val startYear: Int,
    val endYear: Int,
    val globalEvents: List<EraGlobalEvent> = emptyList(),
    val poolWeightModifiers: Map<String, Float> = emptyMap()
)

data class EraGlobalEvent(
    val eventId: String,
    val year: Int,
    val month: Int = 1,
    val probability: Float = 1.0f
)

object EraRegistry {

    val MODERN_KZ_2024 get() = EraDefinition(
        id = "kz_2024",
        name = Strings.eraModernKz2024Name,
        startYear = 2020,
        endYear = 2030
    )

    val KZ_90S get() = EraDefinition(
        id = "kz_90s",
        name = Strings.eraKz90sName,
        startYear = 1991,
        endYear = 2000
    )

    val KZ_2005_CREDIT_BOOM get() = EraDefinition(
        id = "kz_2005",
        name = Strings.eraKz2005Name,
        startYear = 2005,
        endYear = 2010
    )

    val KZ_2015_DEVALUATION get() = EraDefinition(
        id = "kz_2015",
        name = Strings.eraKz2015Name,
        startYear = 2014,
        endYear = 2019
    )

    private val all get() = listOf(MODERN_KZ_2024, KZ_90S, KZ_2005_CREDIT_BOOM, KZ_2015_DEVALUATION)

    fun findById(eraId: String): EraDefinition? {
        val normalized = if (eraId == "modern_kz_2024") "kz_2024" else eraId
        return all.find { it.id == normalized }
    }
}
