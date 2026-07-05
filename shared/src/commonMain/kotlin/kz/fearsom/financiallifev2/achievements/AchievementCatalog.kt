package kz.fearsom.financiallifev2.achievements

// ════════════════════════════════════════════════════════════════════
//  ACHIEVEMENT CATALOG — single source of truth for all achievements
//
//  Content (titles, dossiers, signs) is referenced by "ach_*" i18n keys
//  resolved through Strings[...]. Russian is the source of truth
//  (translations/ach_ru.kt); en/kk fall back to ru per Strings.get().
//
//  Scam unlock flags map to `learned.*` flags set by scenario graphs.
//  Scams whose flags no scenario sets yet stay locked until a scenario
//  hook lands — that is by design (collection grows with content).
// ════════════════════════════════════════════════════════════════════

object AchievementCatalog {

    // ── Game achievement ids ─────────────────────────────────────────
    const val FIRST_STEPS = "game.first_steps"
    const val CUSHION     = "game.cushion"
    const val DEBT_FREE   = "game.debt_free"
    const val IRON_NERVES = "game.iron_nerves"
    const val NEW_LIFE    = "game.new_life"

    // ── Scam collection ids ──────────────────────────────────────────
    const val SCAM_PONZI            = "scam.ponzi"
    const val SCAM_SPANISH_PRISONER = "scam.spanish_prisoner"
    const val SCAM_PIG_BUTCHERING   = "scam.pig_butchering"
    const val SCAM_TIPSTER_FORK     = "scam.tipster_fork"
    const val SCAM_MLM              = "scam.mlm"
    const val SCAM_GURU             = "scam.guru"
    const val SCAM_BRIDGE_SALE      = "scam.bridge_sale"
    const val SCAM_CRYPTO_MOON      = "scam.crypto_moon"
    const val SCAM_FINE_PRINT       = "scam.fine_print"
    const val SCAM_DREAM_VISA       = "scam.dream_visa"

    val all: List<AchievementDefinition> = buildList {

        // ══ Игра ═══════════════════════════════════════════════════════
        add(AchievementDefinition(
            id        = FIRST_STEPS,
            kind      = AchievementKind.GAME,
            emoji     = "🌱",
            rarity    = AchievementRarity.COMMON,
            titleKey  = "ach_first_steps_title",
            descKey   = "ach_first_steps_desc",
            hintKey   = "ach_first_steps_hint",
            condition = AchievementCondition.FirstMonthlyReport
        ))
        add(AchievementDefinition(
            id        = CUSHION,
            kind      = AchievementKind.GAME,
            emoji     = "🛡️",
            rarity    = AchievementRarity.RARE,
            titleKey  = "ach_cushion_title",
            descKey   = "ach_cushion_desc",
            hintKey   = "ach_cushion_hint",
            condition = AchievementCondition.EmergencyFund(months = 6)
        ))
        add(AchievementDefinition(
            id        = DEBT_FREE,
            kind      = AchievementKind.GAME,
            emoji     = "⛓️",
            rarity    = AchievementRarity.RARE,
            titleKey  = "ach_debt_free_title",
            descKey   = "ach_debt_free_desc",
            hintKey   = "ach_debt_free_hint",
            condition = AchievementCondition.DebtFree
        ))
        add(AchievementDefinition(
            id        = IRON_NERVES,
            kind      = AchievementKind.GAME,
            emoji     = "🧊",
            rarity    = AchievementRarity.RARE,
            titleKey  = "ach_iron_nerves_title",
            descKey   = "ach_iron_nerves_desc",
            hintKey   = "ach_iron_nerves_hint",
            condition = AchievementCondition.CalmThroughCrisis(months = 12, stressBelow = 30)
        ))
        add(AchievementDefinition(
            id        = NEW_LIFE,
            kind      = AchievementKind.GAME,
            emoji     = "🏁",
            rarity    = AchievementRarity.LEGENDARY,
            titleKey  = "ach_new_life_title",
            descKey   = "ach_new_life_desc",
            hintKey   = "ach_new_life_hint",
            condition = AchievementCondition.StoryCompleted
        ))

        // ══ Коллекция скамов ═══════════════════════════════════════════
        add(scam(
            id = SCAM_PONZI, emoji = "🏛️", rarity = AchievementRarity.LEGENDARY,
            key = "ponzi", variantCount = 2, signCount = 4,
            flags = setOf("learned.scam.pyramid")
        ))
        add(scam(
            id = SCAM_SPANISH_PRISONER, emoji = "✉️", rarity = AchievementRarity.LEGENDARY,
            key = "prisoner", variantCount = 2, signCount = 4,
            flags = setOf("learned.scam.relative")
        ))
        add(scam(
            id = SCAM_PIG_BUTCHERING, emoji = "🐷", rarity = AchievementRarity.RARE,
            key = "pig", variantCount = 2, signCount = 4,
            // No scenario sets this yet — reserved for a romance-scam arc.
            flags = setOf("learned.scam.romance")
        ))
        add(scam(
            id = SCAM_TIPSTER_FORK, emoji = "🎯", rarity = AchievementRarity.RARE,
            key = "tipster", variantCount = 2, signCount = 4,
            // Reserved: betting/tipster arc. learned.scam.forex covers trading signals.
            flags = setOf("learned.scam.tipster", "learned.scam.forex")
        ))
        add(scam(
            id = SCAM_MLM, emoji = "🕸️", rarity = AchievementRarity.RARE,
            key = "mlm", variantCount = 1, signCount = 4,
            flags = setOf("learned.scam.mlm")
        ))
        add(scam(
            id = SCAM_GURU, emoji = "🔮", rarity = AchievementRarity.RARE,
            key = "guru", variantCount = 1, signCount = 4,
            flags = setOf("learned.scam.infocoach", "learned.infocoach.scam")
        ))
        add(scam(
            id = SCAM_BRIDGE_SALE, emoji = "🌉", rarity = AchievementRarity.COMMON,
            key = "bridge", variantCount = 2, signCount = 4,
            flags = setOf("learned.scam.fake_goods")
        ))
        add(scam(
            id = SCAM_CRYPTO_MOON, emoji = "🪙", rarity = AchievementRarity.RARE,
            key = "crypto", variantCount = 2, signCount = 4,
            flags = setOf("learned.scam.crypto", "learned.crypto.scam")
        ))
        add(scam(
            id = SCAM_FINE_PRINT, emoji = "🧾", rarity = AchievementRarity.COMMON,
            key = "fineprint", variantCount = 2, signCount = 4,
            flags = setOf("learned.contract")
        ))
        add(scam(
            id = SCAM_DREAM_VISA, emoji = "✈️", rarity = AchievementRarity.COMMON,
            key = "visa", variantCount = 1, signCount = 4,
            flags = setOf("learned.scam.fake_job")
        ))
    }

    val byId: Map<String, AchievementDefinition> = all.associateBy { it.id }

    val gameAchievements: List<AchievementDefinition> = all.filter { it.kind == AchievementKind.GAME }
    val scamAchievements: List<AchievementDefinition> = all.filter { it.kind == AchievementKind.SCAM }

    fun isValidId(id: String): Boolean = id in byId

    // ── Builder for the repetitive scam-dossier key layout ────────────
    private fun scam(
        id: String,
        emoji: String,
        rarity: AchievementRarity,
        key: String,
        variantCount: Int,
        signCount: Int,
        flags: Set<String>
    ) = AchievementDefinition(
        id       = id,
        kind     = AchievementKind.SCAM,
        emoji    = emoji,
        rarity   = rarity,
        titleKey = "ach_${key}_title",
        dossier  = ScamDossier(
            ageKey = "ach_${key}_age",
            origin = ScamTimelineEntry(
                yearKey  = "ach_${key}_origin_year",
                titleKey = "ach_${key}_origin_title",
                textKey  = "ach_${key}_origin_text"
            ),
            variants = (1..variantCount).map { i ->
                ScamTimelineEntry(
                    yearKey  = "ach_${key}_v${i}_year",
                    titleKey = "ach_${key}_v${i}_title",
                    textKey  = "ach_${key}_v${i}_text"
                )
            },
            modern = ScamTimelineEntry(
                yearKey  = "ach_${key}_modern_year",
                titleKey = "ach_${key}_modern_title",
                textKey  = "ach_${key}_modern_text"
            ),
            signKeys = (1..signCount).map { i -> "ach_${key}_sign_$i" }
        ),
        condition = AchievementCondition.AnyFlag(flags)
    )
}
