package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.*
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.*

/**
 * Shared library of financial scam and fraud events.
 * Each scam type has ERA VARIANTS — trigger events differ per era,
 * but follow-up/consequence events are shared.
 *
 * Era mapping:
 *   kz_90s  — 1991-2000: no internet, personal visits, МММ wave
 *   kz_2005 — 2000-2009: early internet, dial-up, SMS, ICQ
 *   kz_2015 — 2014-2019: VKontakte, early Instagram, early WhatsApp
 *   kz_2024 — 2020+:     Telegram, Instagram, TikTok, Crypto, Pig Butchering
 *
 * Flag conventions:
 *   "learned.scam.X"     — player understands this scam type → weight drops 85%
 *   "lost_money_to_scam" — any scam weight drops further
 *   "in_mlm"             — player is in MLM now
 */
object ScamEventLibrary {

    // ── DSL helpers ───────────────────────────────────────────────────────────

    private fun event(
        id: String,
        message: String,
        flavor: String = "💬",
        priority: Int = 0,
        conditions: List<Condition> = emptyList(),
        tags: Set<String> = emptySet(),
        poolWeight: Int = 10,
        unique: Boolean = false,
        cooldownMonths: Int = 0,
        isEnding: Boolean = false,
        endingType: EndingType? = null,
        options: List<GameOption>
    ) = GameEvent(id, message, flavor, options, conditions, priority, isEnding, endingType,
                  tags, poolWeight, unique, cooldownMonths)

    private fun option(
        id: String, text: String, emoji: String, next: String, fx: Effect = Effect()
    ) = GameOption(id, text, emoji, fx, next)

    private fun stat(field: Condition.Stat.Field, op: Condition.Stat.Op, value: Long) =
        Condition.Stat(field, op, value)

    private fun inEra(eraId: String) = Condition.InEra(eraId)
    private fun notFlag(flag: String) = Condition.NotFlag(flag)

    // ════════════════════════════════════════════════════════════════════
    //  1. PYRAMID SCHEME — Пирамиды
    //  Общие follow-up: pyramidAvoided, pyramidCollapse, pyramidSmallLoss
    // ════════════════════════════════════════════════════════════════════

    /** kz_90s — сосед приходит домой с предложением */
    private val pyramidNeighbor90s = event(
        id = "scam_pyramid_neighbor_90s",
        message = Strings["scam_pyramid_neighbor_90s_msg"],
        flavor = "🏚️",
        tags = setOf("scam", "scam.pyramid", "social.friend"),
        poolWeight = 18,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 100_000L),
            notFlag("learned.scam.pyramid"),
            inEra("kz_90s")
        ),
        options = listOf(
            option("pyramid_invest_full", Strings["scam_pyramid_neighbor_90s_opt_invest_full"], "💸",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, stressDelta = 10, riskDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_pyramid_collapse", afterMonths = 2)
                )),
            option("pyramid_ask_docs", Strings["scam_pyramid_neighbor_90s_opt_ask_docs"], "🔍",
                next = "scam_pyramid_avoided",
                fx = Effect(knowledgeDelta = 8, stressDelta = -5)),
            option("pyramid_small_bet", Strings["scam_pyramid_neighbor_90s_opt_small_bet"], "🎲",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -20_000, stressDelta = 10, riskDelta = 10,
                    scheduleEvent = ScheduledEvent("scam_pyramid_small_loss", afterMonths = 1)
                )),
            option("pyramid_decline", Strings["scam_pyramid_neighbor_90s_opt_decline"], "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5, stressDelta = -8,
                    setFlags = setOf("learned.scam.pyramid")
                ))
        )
    )

    /** kz_2005 — звонок по стационарному телефону + письмо по email */
    private val pyramidEmail2005 = event(
        id = "scam_pyramid_email_2005",
        message = Strings["scam_pyramid_email_2005_msg"],
        flavor = "📧",
        tags = setOf("scam", "scam.pyramid", "social.friend"),
        poolWeight = 18,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 100_000L),
            notFlag("learned.scam.pyramid"),
            inEra("kz_2005")
        ),
        options = listOf(
            option("pyramid_invest_full", Strings["scam_pyramid_email_2005_opt_invest_full"], "💸",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, stressDelta = 10, riskDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_pyramid_collapse", afterMonths = 2)
                )),
            option("pyramid_ask_docs", Strings["scam_pyramid_email_2005_opt_ask_docs"], "🔍",
                next = "scam_pyramid_avoided",
                fx = Effect(knowledgeDelta = 8, stressDelta = -5)),
            option("pyramid_small_bet", Strings["scam_pyramid_email_2005_opt_small_bet"], "🎲",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -20_000, stressDelta = 10, riskDelta = 10,
                    scheduleEvent = ScheduledEvent("scam_pyramid_small_loss", afterMonths = 1)
                )),
            option("pyramid_decline", Strings["scam_pyramid_email_2005_opt_decline"], "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5, stressDelta = -8,
                    setFlags = setOf("learned.scam.pyramid")
                ))
        )
    )

    /** kz_2015 — звонок в WhatsApp + пост в VKontakte */
    private val pyramidVk2015 = event(
        id = "scam_pyramid_vk_2015",
        message = Strings["scam_pyramid_vk_2015_msg"],
        flavor = "📱",
        tags = setOf("scam", "scam.pyramid", "social.friend"),
        poolWeight = 18,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 100_000L),
            notFlag("learned.scam.pyramid"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("pyramid_invest_full", Strings["scam_pyramid_vk_2015_opt_invest_full"], "💸",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, stressDelta = 10, riskDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_pyramid_collapse", afterMonths = 2)
                )),
            option("pyramid_ask_docs", Strings["scam_pyramid_vk_2015_opt_ask_docs"], "🔍",
                next = "scam_pyramid_avoided",
                fx = Effect(knowledgeDelta = 8, stressDelta = -5)),
            option("pyramid_small_bet", Strings["scam_pyramid_vk_2015_opt_small_bet"], "🎲",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -20_000, stressDelta = 10, riskDelta = 10,
                    scheduleEvent = ScheduledEvent("scam_pyramid_small_loss", afterMonths = 1)
                )),
            option("pyramid_decline", Strings["scam_pyramid_vk_2015_opt_decline"], "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5, stressDelta = -8,
                    setFlags = setOf("learned.scam.pyramid")
                ))
        )
    )

    /** kz_2024 — Telegram/WhatsApp */
    private val pyramidFriendCall = event(
        id = "scam_pyramid_friend",
        message = Strings["scam_pyramid_friend_msg"],
        flavor = "😰",
        tags = setOf("scam", "scam.pyramid", "social.friend"),
        poolWeight = 18,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 100_000L),
            notFlag("learned.scam.pyramid"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("pyramid_invest_full", Strings["scam_pyramid_friend_opt_invest_full"], "💸",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, stressDelta = 10, riskDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_pyramid_collapse", afterMonths = 2)
                )),
            option("pyramid_ask_docs", Strings["scam_pyramid_friend_opt_ask_docs"], "🔍",
                next = "scam_pyramid_avoided",
                fx = Effect(knowledgeDelta = 8, stressDelta = -5)),
            option("pyramid_small_bet", Strings["scam_pyramid_friend_opt_small_bet"], "🎲",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -20_000, stressDelta = 10, riskDelta = 10,
                    scheduleEvent = ScheduledEvent("scam_pyramid_small_loss", afterMonths = 1)
                )),
            option("pyramid_decline", Strings["scam_pyramid_friend_opt_decline"], "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5, stressDelta = -8,
                    setFlags = setOf("learned.scam.pyramid")
                ))
        )
    )

    // ── Pyramid shared follow-ups ─────────────────────────────────────────────

    private val pyramidAvoided = event(
        id = "scam_pyramid_avoided",
        message = Strings["scam_pyramid_avoided_msg"],
        flavor = "📚",
        tags = setOf("scam.pyramid", "educational"),
        options = listOf(
            option("pyramid_lesson_learned", Strings["scam_pyramid_avoided_opt_lesson_learned"], "✅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 20, stressDelta = -10,
                    setFlags = setOf("learned.scam.pyramid")
                ))
        )
    )

    private val pyramidCollapse = event(
        id = "scam_pyramid_collapse",
        message = Strings["scam_pyramid_collapse_msg"],
        flavor = "💀",
        tags = setOf("scam.pyramid", "consequence"),
        options = listOf(
            option("pyramid_rebuild", Strings["scam_pyramid_collapse_opt_rebuild"], "📖",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 25, stressDelta = 20,
                    setFlags = setOf("learned.scam.pyramid", "lost_money_to_scam")
                ))
        )
    )

    private val pyramidSmallLoss = event(
        id = "scam_pyramid_small_loss",
        message = Strings["scam_pyramid_small_loss_msg"],
        flavor = "😕",
        tags = setOf("scam.pyramid", "consequence"),
        options = listOf(
            option("pyramid_small_lesson", Strings["scam_pyramid_small_loss_opt_lesson"], "📚",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 20, stressDelta = 5,
                    setFlags = setOf("learned.scam.pyramid")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  2. MLM — Сетевой маркетинг
    // ════════════════════════════════════════════════════════════════════

    /** kz_90s — дверь в дверь, Гербалайф/Амвэй */
    private val mlmDoor90s = event(
        id = "scam_mlm_door_90s",
        message = Strings["scam_mlm_door_90s_msg"],
        flavor = "🚪",
        tags = setOf("scam", "scam.mlm", "social.colleague"),
        poolWeight = 14,
        unique = true,
        conditions = listOf(
            notFlag("learned.scam.mlm"),
            inEra("kz_90s")
        ),
        options = listOf(
            option("mlm_go_meeting", Strings["scam_mlm_door_90s_opt_go_meeting"], "💳",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -50_000, stressDelta = 5, riskDelta = 10,
                    setFlags = setOf("in_mlm"),
                    scheduleEvent = ScheduledEvent("scam_mlm_month_later", afterMonths = 3)
                )),
            option("mlm_ask_directly", Strings["scam_mlm_door_90s_opt_ask_directly"], "🔍",
                next = "scam_mlm_confronted",
                fx = Effect(knowledgeDelta = 5)),
            option("mlm_decline", Strings["scam_mlm_door_90s_opt_decline"], "🙂",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 3,
                    setFlags = setOf("learned.scam.mlm")
                ))
        )
    )

    /** kz_2005 — звонок + физическая встреча, ранний интернет-маркетинг */
    private val mlmPhone2005 = event(
        id = "scam_mlm_phone_2005",
        message = Strings["scam_mlm_phone_2005_msg"],
        flavor = "📞",
        tags = setOf("scam", "scam.mlm", "social.colleague"),
        poolWeight = 14,
        unique = true,
        conditions = listOf(
            notFlag("learned.scam.mlm"),
            inEra("kz_2005")
        ),
        options = listOf(
            option("mlm_go_meeting", Strings["scam_mlm_phone_2005_opt_go_meeting"], "💳",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -35_000, stressDelta = 5, riskDelta = 10,
                    setFlags = setOf("in_mlm"),
                    scheduleEvent = ScheduledEvent("scam_mlm_month_later", afterMonths = 3)
                )),
            option("mlm_ask_directly", Strings["scam_mlm_phone_2005_opt_ask_directly"], "🔍",
                next = "scam_mlm_confronted",
                fx = Effect(knowledgeDelta = 5)),
            option("mlm_decline", Strings["scam_mlm_phone_2005_opt_decline"], "🙂",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 3,
                    setFlags = setOf("learned.scam.mlm")
                ))
        )
    )

    /** kz_2015 — Instagram + WhatsApp группа */
    private val mlmInstagram2015 = event(
        id = "scam_mlm_instagram_2015",
        message = Strings["scam_mlm_instagram_2015_msg"],
        flavor = "✨",
        tags = setOf("scam", "scam.mlm", "social.colleague"),
        poolWeight = 14,
        unique = true,
        conditions = listOf(
            notFlag("learned.scam.mlm"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("mlm_go_meeting", Strings["scam_mlm_instagram_2015_opt_go_meeting"], "👀",
                next = "scam_mlm_presentation",
                fx = Effect(stressDelta = 3)),
            option("mlm_ask_directly", Strings["scam_mlm_instagram_2015_opt_ask_directly"], "🔍",
                next = "scam_mlm_confronted",
                fx = Effect(knowledgeDelta = 5)),
            option("mlm_decline", Strings["scam_mlm_instagram_2015_opt_decline"], "🙂",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 3,
                    setFlags = setOf("learned.scam.mlm")
                ))
        )
    )

    /** kz_2024 — Telegram/Instagram */
    private val mlmColleague = event(
        id = "scam_mlm_colleague",
        message = Strings["scam_mlm_colleague_msg"],
        flavor = "✨",
        tags = setOf("scam", "scam.mlm", "social.colleague"),
        poolWeight = 14,
        unique = true,
        conditions = listOf(
            notFlag("learned.scam.mlm"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("mlm_go_meeting", Strings["scam_mlm_colleague_opt_go_meeting"], "👀",
                next = "scam_mlm_presentation",
                fx = Effect(stressDelta = 3)),
            option("mlm_ask_directly", Strings["scam_mlm_colleague_opt_ask_directly"], "🔍",
                next = "scam_mlm_confronted",
                fx = Effect(knowledgeDelta = 5)),
            option("mlm_decline", Strings["scam_mlm_colleague_opt_decline"], "🙂",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 3,
                    setFlags = setOf("learned.scam.mlm")
                ))
        )
    )

    // ── MLM shared follow-ups ─────────────────────────────────────────────────

    private val mlmPresentation = event(
        id = "scam_mlm_presentation",
        message = Strings["scam_mlm_presentation_msg"],
        flavor = "📊",
        tags = setOf("scam.mlm"),
        options = listOf(
            option("mlm_join", Strings["scam_mlm_presentation_opt_join"], "💳",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -35_000, stressDelta = 5, riskDelta = 10,
                    setFlags = setOf("in_mlm"),
                    scheduleEvent = ScheduledEvent("scam_mlm_month_later", afterMonths = 3)
                )),
            option("mlm_decline_after", Strings["scam_mlm_presentation_opt_decline_after"], "❌",
                next = "scam_mlm_confronted",
                fx = Effect(knowledgeDelta = 12))
        )
    )

    private val mlmConfronted = event(
        id = "scam_mlm_confronted",
        message = Strings["scam_mlm_confronted_msg"],
        flavor = "📖",
        tags = setOf("scam.mlm", "educational"),
        options = listOf(
            option("mlm_understand", Strings["scam_mlm_confronted_opt_understand"], "✅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 18, stressDelta = -5,
                    setFlags = setOf("learned.scam.mlm")
                ))
        )
    )

    private val mlmMonthLater = event(
        id = "scam_mlm_month_later",
        message = Strings["scam_mlm_month_later_msg"],
        flavor = "📦",
        tags = setOf("scam.mlm", "consequence"),
        options = listOf(
            option("mlm_exit", Strings["scam_mlm_month_later_opt_exit"], "🚪",
                next = MONTHLY_TICK,
                fx = Effect(
                    stressDelta = -15, knowledgeDelta = 25,
                    clearFlags = setOf("in_mlm"),
                    setFlags = setOf("learned.scam.mlm")
                )),
            option("mlm_continue", Strings["scam_mlm_month_later_opt_continue"], "😤",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -35_000, stressDelta = 10, riskDelta = 5
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  3. BETTING / CAPPER
    //  kz_90s/2005 — нет, kz_2015 — ранний, kz_2024 — Telegram
    // ════════════════════════════════════════════════════════════════════

    /** kz_2005 — SMS-типстер, ранние букмекеры */
    private val capperSms2005 = event(
        id = "scam_capper_sms_2005",
        message = Strings["scam_capper_sms_2005_msg"],
        flavor = "📟",
        tags = setOf("scam", "scam.betting", "scam.capper"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            notFlag("learned.scam.betting"),
            inEra("kz_2005")
        ),
        options = listOf(
            option("capper_buy", Strings["scam_capper_sms_2005_opt_buy"], "💰",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -15_000, riskDelta = 15,
                    scheduleEvent = ScheduledEvent("scam_capper_loses", afterMonths = 1)
                )),
            option("capper_research", Strings["scam_capper_sms_2005_opt_research"], "🔍",
                next = "scam_capper_explained",
                fx = Effect(knowledgeDelta = 10)),
            option("capper_ignore", Strings["scam_capper_sms_2005_opt_ignore"], "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.betting")
                ))
        )
    )

    /** kz_2015 — VKontakte канал + личное сообщение */
    private val capperVk2015 = event(
        id = "scam_capper_vk_2015",
        message = Strings["scam_capper_vk_2015_msg"],
        flavor = "⚽",
        tags = setOf("scam", "scam.betting", "scam.capper"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            notFlag("learned.scam.betting"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("capper_buy", Strings["scam_capper_vk_2015_opt_buy"], "💰",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -25_000, riskDelta = 15,
                    scheduleEvent = ScheduledEvent("scam_capper_loses", afterMonths = 1)
                )),
            option("capper_research", Strings["scam_capper_vk_2015_opt_research"], "🔍",
                next = "scam_capper_explained",
                fx = Effect(knowledgeDelta = 10)),
            option("capper_ignore", Strings["scam_capper_vk_2015_opt_ignore"], "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.betting")
                ))
        )
    )

    /** kz_2024 — Telegram канал */
    private val capperTelegram = event(
        id = "scam_capper_telegram",
        message = Strings["scam_capper_telegram_msg"],
        flavor = "⚽",
        tags = setOf("scam", "scam.betting", "scam.capper"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            notFlag("learned.scam.betting"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("capper_buy", Strings["scam_capper_telegram_opt_buy"], "💰",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -30_000, riskDelta = 15,
                    scheduleEvent = ScheduledEvent("scam_capper_loses", afterMonths = 1)
                )),
            option("capper_research", Strings["scam_capper_telegram_opt_research"], "🔍",
                next = "scam_capper_explained",
                fx = Effect(knowledgeDelta = 10)),
            option("capper_ignore", Strings["scam_capper_telegram_opt_ignore"], "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.betting")
                ))
        )
    )

    // ── Capper shared follow-ups ──────────────────────────────────────────────

    private val capperExplained = event(
        id = "scam_capper_explained",
        message = Strings["scam_capper_explained_msg"],
        flavor = "🔢",
        tags = setOf("scam.betting", "educational"),
        options = listOf(
            option("capper_lesson", Strings["scam_capper_explained_opt_lesson"], "📖",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 20, stressDelta = -5,
                    setFlags = setOf("learned.scam.betting")
                ))
        )
    )

    private val capperLoses = event(
        id = "scam_capper_loses",
        message = Strings["scam_capper_loses_msg"],
        flavor = "😤",
        tags = setOf("scam.betting", "consequence"),
        options = listOf(
            option("capper_stop", Strings["scam_capper_loses_opt_stop"], "🛑",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 18, stressDelta = 5,
                    setFlags = setOf("learned.scam.betting", "lost_money_to_scam")
                )),
            option("capper_escalate", Strings["scam_capper_loses_opt_escalate"], "😰",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, stressDelta = 20, riskDelta = 15,
                    scheduleEvent = ScheduledEvent("scam_betting_deep_loss", afterMonths = 2)
                ))
        )
    )

    private val bettingDeepLoss = event(
        id = "scam_betting_deep_loss",
        message = Strings["scam_betting_deep_loss_msg"],
        flavor = "💀",
        tags = setOf("scam.betting", "consequence"),
        options = listOf(
            option("betting_final_lesson", Strings["scam_betting_deep_loss_opt_final_lesson"], "📖",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 30, stressDelta = 15,
                    setFlags = setOf("learned.scam.betting", "lost_money_to_scam")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  4. ROMANCE SCAM / PIG BUTCHERING
    //  kz_2015 — ранняя версия (ВКонтакте/Mamba), kz_2024 — WhatsApp
    //  kz_90s / kz_2005 — не существует (нет интернета / слишком рано)
    // ════════════════════════════════════════════════════════════════════

    /** kz_2015 — VKontakte / Mamba / ранний dating */
    private val romanceMamba2015 = event(
        id = "scam_romance_mamba_2015",
        message = Strings["scam_romance_mamba_2015_msg"],
        flavor = "💌",
        tags = setOf("scam", "scam.romance", "scam.crypto"),
        poolWeight = 8,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 200_000L),
            notFlag("learned.scam.romance"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("romance_respond", Strings["scam_romance_mamba_2015_opt_respond"], "💬",
                next = "scam_romance_buildup",
                fx = Effect(stressDelta = -3)),
            option("romance_suspicious", Strings["scam_romance_mamba_2015_opt_suspicious"], "🔍",
                next = "scam_romance_caught",
                fx = Effect(knowledgeDelta = 12)),
            option("romance_ignore", Strings["scam_romance_mamba_2015_opt_ignore"], "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.romance")
                ))
        )
    )

    /** kz_2024 — WhatsApp "ошиблась номером" */
    private val romanceFirstContact = event(
        id = "scam_romance_contact",
        message = Strings["scam_romance_contact_msg"],
        flavor = "💌",
        tags = setOf("scam", "scam.romance", "scam.crypto"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 200_000L),
            notFlag("learned.scam.romance"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("romance_respond", Strings["scam_romance_contact_opt_respond"], "💬",
                next = "scam_romance_buildup",
                fx = Effect(stressDelta = -3)),
            option("romance_suspicious", Strings["scam_romance_contact_opt_suspicious"], "🔍",
                next = "scam_romance_caught",
                fx = Effect(knowledgeDelta = 12)),
            option("romance_ignore", Strings["scam_romance_contact_opt_ignore"], "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.romance")
                ))
        )
    )

    // ── Romance shared follow-ups ─────────────────────────────────────────────

    private val romanceBuildup = event(
        id = "scam_romance_buildup",
        message = Strings["scam_romance_buildup_msg"],
        flavor = "💕",
        tags = setOf("scam.romance", "scam.crypto"),
        options = listOf(
            option("romance_interested", Strings["scam_romance_buildup_opt_interested"], "🤔",
                next = "scam_romance_crypto_intro",
                fx = Effect(stressDelta = 2, riskDelta = 5)),
            option("romance_red_flag", Strings["scam_romance_buildup_opt_red_flag"], "🚩",
                next = "scam_romance_caught",
                fx = Effect(knowledgeDelta = 15))
        )
    )

    private val romanceCryptoIntro = event(
        id = "scam_romance_crypto_intro",
        message = Strings["scam_romance_crypto_intro_msg"],
        flavor = "📈",
        tags = setOf("scam.romance", "scam.crypto"),
        options = listOf(
            option("romance_more_money", Strings["scam_romance_crypto_intro_opt_more_money"], "💸",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -200_000, riskDelta = 20, stressDelta = 5,
                    scheduleEvent = ScheduledEvent("scam_romance_freeze", afterMonths = 2)
                )),
            option("romance_withdraw_test", Strings["scam_romance_crypto_intro_opt_withdraw_test"], "🧪",
                next = "scam_romance_withdrawal_blocked",
                fx = Effect(knowledgeDelta = 15))
        )
    )

    private val romanceWithdrawalBlocked = event(
        id = "scam_romance_withdrawal_blocked",
        message = Strings["scam_romance_withdrawal_blocked_msg"],
        flavor = "🚨",
        tags = setOf("scam.romance", "scam.crypto"),
        options = listOf(
            option("romance_pay_tax", Strings["scam_romance_withdrawal_blocked_opt_pay_tax"], "😰",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -57_500, stressDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_romance_final", afterMonths = 1)
                )),
            option("romance_realize", Strings["scam_romance_withdrawal_blocked_opt_realize"], "💡",
                next = "scam_romance_caught",
                fx = Effect(knowledgeDelta = 25))
        )
    )

    private val romanceFreeze = event(
        id = "scam_romance_freeze",
        message = Strings["scam_romance_freeze_msg"],
        flavor = "🔒",
        tags = setOf("scam.romance", "consequence"),
        options = listOf(
            option("romance_pay_more", Strings["scam_romance_freeze_opt_pay_more"], "😱",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -250_000, stressDelta = 25,
                    scheduleEvent = ScheduledEvent("scam_romance_final", afterMonths = 1)
                )),
            option("romance_stop_loss", Strings["scam_romance_freeze_opt_stop_loss"], "✋",
                next = "scam_romance_caught",
                fx = Effect(knowledgeDelta = 20, stressDelta = 10))
        )
    )

    private val romanceFinal = event(
        id = "scam_romance_final",
        message = Strings["scam_romance_final_msg"],
        flavor = "💔",
        tags = setOf("scam.romance", "consequence"),
        options = listOf(
            option("romance_report", Strings["scam_romance_final_opt_report"], "📖",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 35, stressDelta = 20,
                    setFlags = setOf("learned.scam.romance", "learned.scam.crypto", "lost_money_to_scam")
                ))
        )
    )

    private val romanceCaught = event(
        id = "scam_romance_caught",
        message = Strings["scam_romance_caught_msg"],
        flavor = "🔍",
        tags = setOf("scam.romance", "educational"),
        options = listOf(
            option("romance_safe", Strings["scam_romance_caught_opt_safe"], "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 25, stressDelta = -5,
                    setFlags = setOf("learned.scam.romance")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  5. CRYPTO SCAM — фейковые биржи
    //  kz_2015 — ранний Bitcoin (не полностью crypto scam, скорее Forex)
    //  kz_2024 — Telegram, полноценный крипто-скам
    // ════════════════════════════════════════════════════════════════════

    /** kz_2015 — ранний Bitcoin / Forex-лохотрон */
    private val cryptoForex2015 = event(
        id = "scam_crypto_forex_2015",
        message = Strings["scam_crypto_forex_2015_msg"],
        flavor = "📊",
        tags = setOf("scam", "scam.crypto"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            notFlag("learned.scam.crypto"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("crypto_exchange_invest", Strings["scam_crypto_forex_2015_opt_crypto_exchange_invest"], "🚀",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, riskDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_crypto_withdrawal_trap", afterMonths = 2)
                )),
            option("crypto_exchange_check", Strings["scam_crypto_forex_2015_opt_crypto_exchange_check"], "🔎",
                next = "scam_crypto_no_license",
                fx = Effect(knowledgeDelta = 10)),
            option("crypto_exchange_ignore", Strings["scam_crypto_forex_2015_opt_crypto_exchange_ignore"], "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.crypto")
                ))
        )
    )

    /** kz_2024 — Telegram канал с 52k подписчиков */
    private val cryptoFakeExchange = event(
        id = "scam_crypto_exchange",
        message = Strings["scam_crypto_exchange_msg"],
        flavor = "📊",
        tags = setOf("scam", "scam.crypto"),
        poolWeight = 15,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            notFlag("learned.scam.crypto"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("crypto_exchange_invest", Strings["scam_crypto_exchange_opt_crypto_exchange_invest"], "🚀",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -100_000, riskDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_crypto_withdrawal_trap", afterMonths = 2)
                )),
            option("crypto_exchange_check", Strings["scam_crypto_exchange_opt_crypto_exchange_check"], "🔎",
                next = "scam_crypto_no_license",
                fx = Effect(knowledgeDelta = 10)),
            option("crypto_exchange_ignore", Strings["scam_crypto_exchange_opt_crypto_exchange_ignore"], "🛡️",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.crypto")
                ))
        )
    )

    // ── Crypto shared follow-ups ──────────────────────────────────────────────

    private val cryptoNoLicense = event(
        id = "scam_crypto_no_license",
        message = Strings["scam_crypto_no_license_msg"],
        flavor = "🔍",
        tags = setOf("scam.crypto", "educational"),
        options = listOf(
            option("crypto_lesson", Strings["scam_crypto_no_license_opt_crypto_lesson"], "✅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 22, stressDelta = -5,
                    setFlags = setOf("learned.scam.crypto")
                ))
        )
    )

    private val cryptoWithdrawalTrap = event(
        id = "scam_crypto_withdrawal_trap",
        message = Strings["scam_crypto_withdrawal_trap_msg"],
        flavor = "🚨",
        tags = setOf("scam.crypto", "consequence"),
        options = listOf(
            option("crypto_pay_more", Strings["scam_crypto_withdrawal_trap_opt_crypto_pay_more"], "😰",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -15_000, stressDelta = 20,
                    scheduleEvent = ScheduledEvent("scam_crypto_final_disappear", afterMonths = 1)
                )),
            option("crypto_stop_now", Strings["scam_crypto_withdrawal_trap_opt_crypto_stop_now"], "🛑",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 25, stressDelta = 15,
                    setFlags = setOf("learned.scam.crypto", "lost_money_to_scam")
                ))
        )
    )

    private val cryptoFinalDisappear = event(
        id = "scam_crypto_final_disappear",
        message = Strings["scam_crypto_final_disappear_msg"],
        flavor = "💀",
        tags = setOf("scam.crypto", "consequence"),
        options = listOf(
            option("crypto_final_lesson", Strings["scam_crypto_final_disappear_opt_crypto_final_lesson"], "📖",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 30, stressDelta = 15,
                    setFlags = setOf("learned.scam.crypto", "lost_money_to_scam")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  6. MFO — Микрофинансовые организации
    // ════════════════════════════════════════════════════════════════════

    /** kz_90s / kz_2005 — неформальный «ростовщик», сосед или знакомый */
    private val mfoNeighborLender90s = event(
        id = "scam_mfo_neighbor_90s",
        message = Strings["scam_mfo_neighbor_90s_msg"],
        flavor = "🏚️",
        tags = setOf("scam", "scam.mfo", "debt"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, LTE, 100_000L),
            stat(STRESS, GTE, 40L),
            notFlag("learned.scam.mfo"),
            inEra("kz_90s")
        ),
        options = listOf(
            option("mfo_take_quick", Strings["scam_mfo_neighbor_90s_opt_mfo_take_quick"], "⚡",
                next = "scam_mfo_signed",
                fx = Effect(capitalDelta = 50_000, debtDelta = 50_000, stressDelta = -5)),
            option("mfo_read_contract", Strings["scam_mfo_neighbor_90s_opt_mfo_read_contract"], "📋",
                next = "scam_mfo_contract_revealed",
                fx = Effect(knowledgeDelta = 10)),
            option("mfo_call_bank", Strings["scam_mfo_neighbor_90s_opt_mfo_call_bank"], "📞",
                next = MONTHLY_TICK,
                fx = Effect(
                    stressDelta = 5, knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.mfo")
                ))
        )
    )

    /** kz_2005 — первые МФО с физическими офисами */
    private val mfoOffice2005 = event(
        id = "scam_mfo_office_2005",
        message = Strings["scam_mfo_office_2005_msg"],
        flavor = "🏦",
        tags = setOf("scam", "scam.mfo", "debt"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, LTE, 100_000L),
            stat(STRESS, GTE, 40L),
            notFlag("learned.scam.mfo"),
            inEra("kz_2005")
        ),
        options = listOf(
            option("mfo_take_quick", Strings["scam_mfo_office_2005_opt_mfo_take_quick"], "⚡",
                next = "scam_mfo_signed",
                fx = Effect(capitalDelta = 80_000, debtDelta = 80_000, stressDelta = -5)),
            option("mfo_read_contract", Strings["scam_mfo_office_2005_opt_mfo_read_contract"], "📋",
                next = "scam_mfo_contract_revealed",
                fx = Effect(knowledgeDelta = 10)),
            option("mfo_call_bank", Strings["scam_mfo_office_2005_opt_mfo_call_bank"], "📞",
                next = MONTHLY_TICK,
                fx = Effect(
                    stressDelta = 5, knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.mfo")
                ))
        )
    )

    /** kz_2015 / kz_2024 — онлайн МФО */
    private val mfoUrgentOnline = event(
        id = "scam_mfo_urgent",
        message = Strings["scam_mfo_urgent_msg"],
        flavor = "🏦",
        tags = setOf("scam", "scam.mfo", "debt"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, LTE, 100_000L),
            stat(STRESS, GTE, 40L),
            notFlag("learned.scam.mfo"),
            Condition.InEra("kz_2015")
        ),
        options = listOf(
            option("mfo_take_quick", Strings["scam_mfo_urgent_opt_mfo_take_quick"], "⚡",
                next = "scam_mfo_signed",
                fx = Effect(capitalDelta = 80_000, debtDelta = 80_000, stressDelta = -5)),
            option("mfo_read_contract", Strings["scam_mfo_urgent_opt_mfo_read_contract"], "📋",
                next = "scam_mfo_contract_revealed",
                fx = Effect(knowledgeDelta = 10)),
            option("mfo_call_bank", Strings["scam_mfo_urgent_opt_mfo_call_bank"], "📞",
                next = MONTHLY_TICK,
                fx = Effect(
                    stressDelta = 5, knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.mfo")
                ))
        )
    )

    private val mfoUrgent2024 = event(
        id = "scam_mfo_urgent_2024",
        message = Strings["scam_mfo_urgent_2024_msg"],
        flavor = "🏦",
        tags = setOf("scam", "scam.mfo", "debt"),
        poolWeight = 12,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, LTE, 100_000L),
            stat(STRESS, GTE, 40L),
            notFlag("learned.scam.mfo"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("mfo_take_quick", Strings["scam_mfo_urgent_2024_opt_mfo_take_quick"], "⚡",
                next = "scam_mfo_signed",
                fx = Effect(capitalDelta = 80_000, debtDelta = 80_000, stressDelta = -5)),
            option("mfo_read_contract", Strings["scam_mfo_urgent_2024_opt_mfo_read_contract"], "📋",
                next = "scam_mfo_contract_revealed",
                fx = Effect(knowledgeDelta = 10)),
            option("mfo_call_bank", Strings["scam_mfo_urgent_2024_opt_mfo_call_bank"], "📞",
                next = MONTHLY_TICK,
                fx = Effect(
                    stressDelta = 5, knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.mfo")
                ))
        )
    )

    // ── MFO shared follow-ups ─────────────────────────────────────────────────

    private val mfoContractRevealed = event(
        id = "scam_mfo_contract_revealed",
        message = Strings["scam_mfo_contract_revealed_msg"],
        flavor = "⚠️",
        tags = setOf("scam.mfo", "educational"),
        options = listOf(
            option("mfo_understand", Strings["scam_mfo_contract_revealed_opt_mfo_understand"], "✅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 20,
                    setFlags = setOf("learned.scam.mfo")
                ))
        )
    )

    private val mfoSigned = event(
        id = "scam_mfo_signed",
        message = Strings["scam_mfo_signed_msg"],
        flavor = "😰",
        tags = setOf("scam.mfo", "consequence"),
        options = listOf(
            option("mfo_pay_urgently", Strings["scam_mfo_signed_opt_mfo_pay_urgently"], "🚨",
                next = MONTHLY_TICK,
                fx = Effect(
                    debtDelta = -128_000, capitalDelta = -128_000,
                    stressDelta = -10, knowledgeDelta = 20,
                    setFlags = setOf("learned.scam.mfo")
                )),
            option("mfo_rollover", Strings["scam_mfo_signed_opt_mfo_rollover"], "💳",
                next = MONTHLY_TICK,
                fx = Effect(
                    debtDelta = 128_000, debtPaymentDelta = 15_000,
                    stressDelta = 20, riskDelta = 15
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  7. MIDDLEMAN / CHINA GOODS — Схема посредника
    // ════════════════════════════════════════════════════════════════════

    /** kz_90s — «челнок» из Турции */
    private val middlemanTurkey90s = event(
        id = "scam_middleman_turkey_90s",
        message = Strings["scam_middleman_turkey_90s_msg"],
        flavor = "✈️",
        tags = setOf("scam", "scam.middleman"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 200_000L),
            notFlag("learned.scam.middleman"),
            inEra("kz_90s")
        ),
        options = listOf(
            option("middleman_invest", Strings["scam_middleman_turkey_90s_opt_middleman_invest"], "🏭",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -200_000, riskDelta = 20, stressDelta = 5,
                    scheduleEvent = ScheduledEvent("scam_middleman_result", afterMonths = 2)
                )),
            option("middleman_ask_contract", Strings["scam_middleman_turkey_90s_opt_middleman_ask_contract"], "📋",
                next = "scam_middleman_contract_refused",
                fx = Effect(knowledgeDelta = 10)),
            option("middleman_decline", Strings["scam_middleman_turkey_90s_opt_middleman_decline"], "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.middleman")
                ))
        )
    )

    /** kz_2005 — ранний Alibaba + физический посредник */
    private val middlemanAlibaba2005 = event(
        id = "scam_middleman_alibaba_2005",
        message = Strings["scam_middleman_alibaba_2005_msg"],
        flavor = "📦",
        tags = setOf("scam", "scam.middleman"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 300_000L),
            notFlag("learned.scam.middleman"),
            inEra("kz_2005")
        ),
        options = listOf(
            option("middleman_invest", Strings["scam_middleman_alibaba_2005_opt_middleman_invest"], "🏭",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -500_000, riskDelta = 20, stressDelta = 5,
                    scheduleEvent = ScheduledEvent("scam_middleman_result", afterMonths = 2)
                )),
            option("middleman_ask_contract", Strings["scam_middleman_alibaba_2005_opt_middleman_ask_contract"], "📋",
                next = "scam_middleman_contract_refused",
                fx = Effect(knowledgeDelta = 10)),
            option("middleman_decline", Strings["scam_middleman_alibaba_2005_opt_middleman_decline"], "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.middleman")
                ))
        )
    )

    /** kz_2015 — AliExpress / популярный e-commerce */
    private val middlemanAli2015 = event(
        id = "scam_middleman_ali_2015",
        message = Strings["scam_middleman_ali_2015_msg"],
        flavor = "📦",
        tags = setOf("scam", "scam.middleman"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 300_000L),
            notFlag("learned.scam.middleman"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("middleman_invest", Strings["scam_middleman_ali_2015_opt_middleman_invest"], "🏭",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -500_000, riskDelta = 20, stressDelta = 5,
                    scheduleEvent = ScheduledEvent("scam_middleman_result", afterMonths = 2)
                )),
            option("middleman_ask_contract", Strings["scam_middleman_ali_2015_opt_middleman_ask_contract"], "📋",
                next = "scam_middleman_contract_refused",
                fx = Effect(knowledgeDelta = 10)),
            option("middleman_decline", Strings["scam_middleman_ali_2015_opt_middleman_decline"], "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.middleman")
                ))
        )
    )

    /** kz_2024 — Telegram/Instagram перекуп */
    private val middlemanChina2024 = event(
        id = "scam_middleman_china",
        message = Strings["scam_middleman_china_msg"],
        flavor = "📦",
        tags = setOf("scam", "scam.middleman"),
        poolWeight = 10,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 300_000L),
            notFlag("learned.scam.middleman"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("middleman_invest", Strings["scam_middleman_china_opt_middleman_invest"], "🏭",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -500_000, riskDelta = 20, stressDelta = 5,
                    scheduleEvent = ScheduledEvent("scam_middleman_result", afterMonths = 2)
                )),
            option("middleman_ask_contract", Strings["scam_middleman_china_opt_middleman_ask_contract"], "📋",
                next = "scam_middleman_contract_refused",
                fx = Effect(knowledgeDelta = 10)),
            option("middleman_decline", Strings["scam_middleman_china_opt_middleman_decline"], "🚫",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 8,
                    setFlags = setOf("learned.scam.middleman")
                ))
        )
    )

    // ── Middleman shared follow-ups ───────────────────────────────────────────

    private val middlemanContractRefused = event(
        id = "scam_middleman_contract_refused",
        message = Strings["scam_middleman_contract_refused_msg"],
        flavor = "🚩",
        tags = setOf("scam.middleman", "educational"),
        options = listOf(
            option("middleman_lesson", Strings["scam_middleman_contract_refused_opt_middleman_lesson"], "✅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 18, stressDelta = -5,
                    setFlags = setOf("learned.scam.middleman")
                ))
        )
    )

    private val middlemanResult = event(
        id = "scam_middleman_result",
        message = Strings["scam_middleman_result_msg"],
        flavor = "💀",
        tags = setOf("scam.middleman", "consequence"),
        options = listOf(
            option("middleman_loss_lesson", Strings["scam_middleman_result_opt_middleman_loss_lesson"], "📖",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 28, stressDelta = 20,
                    setFlags = setOf("learned.scam.middleman", "lost_money_to_scam")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  8. TRAINING CULT — Тренинги-секты
    // ════════════════════════════════════════════════════════════════════

    /** kz_90s — семинар по книге Кийосаки / начало тренинговой культуры */
    private val trainingKiyosaki90s = event(
        id = "scam_training_kiyosaki_90s",
        message = Strings["scam_training_kiyosaki_90s_msg"],
        flavor = "📚",
        tags = setOf("scam", "scam.training"),
        poolWeight = 8,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 50_000L),
            notFlag("learned.scam.training"),
            inEra("kz_90s")
        ),
        options = listOf(
            option("training_pay", Strings["scam_training_kiyosaki_90s_opt_training_pay"], "💳",
                next = "scam_training_first_level",
                fx = Effect(capitalDelta = -50_000, stressDelta = -5, knowledgeDelta = 3)),
            option("training_research", Strings["scam_training_kiyosaki_90s_opt_training_research"], "🔍",
                next = "scam_training_reviews",
                fx = Effect(knowledgeDelta = 8)),
            option("training_decline", Strings["scam_training_kiyosaki_90s_opt_training_decline"], "🙅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.training")
                ))
        )
    )

    /** kz_2005 — «Бизнес Молодость» прообраз, корпоративные тренинги */
    private val trainingBusiness2005 = event(
        id = "scam_training_business_2005",
        message = Strings["scam_training_business_2005_msg"],
        flavor = "🧘",
        tags = setOf("scam", "scam.training"),
        poolWeight = 8,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 80_000L),
            notFlag("learned.scam.training"),
            inEra("kz_2005")
        ),
        options = listOf(
            option("training_pay", Strings["scam_training_business_2005_opt_training_pay"], "💳",
                next = "scam_training_first_level",
                fx = Effect(capitalDelta = -80_000, stressDelta = -5, knowledgeDelta = 3)),
            option("training_research", Strings["scam_training_business_2005_opt_training_research"], "🔍",
                next = "scam_training_reviews",
                fx = Effect(knowledgeDelta = 8)),
            option("training_decline", Strings["scam_training_business_2005_opt_training_decline"], "🙅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.training")
                ))
        )
    )

    /** kz_2015 — «Бизнес Молодость», Instagram-коучи */
    private val trainingBm2015 = event(
        id = "scam_training_bm_2015",
        message = Strings["scam_training_bm_2015_msg"],
        flavor = "🔥",
        tags = setOf("scam", "scam.training"),
        poolWeight = 11,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 80_000L),
            notFlag("learned.scam.training"),
            inEra("kz_2015")
        ),
        options = listOf(
            option("training_pay", Strings["scam_training_bm_2015_opt_training_pay"], "💳",
                next = "scam_training_first_level",
                fx = Effect(capitalDelta = -80_000, stressDelta = -5, knowledgeDelta = 3)),
            option("training_research", Strings["scam_training_bm_2015_opt_training_research"], "🔍",
                next = "scam_training_reviews",
                fx = Effect(knowledgeDelta = 8)),
            option("training_decline", Strings["scam_training_bm_2015_opt_training_decline"], "🙅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.training")
                ))
        )
    )

    /** kz_2024 — Telegram/TikTok коучи */
    private val trainingCult2024 = event(
        id = "scam_training_cult",
        message = Strings["scam_training_cult_msg"],
        flavor = "🧘",
        tags = setOf("scam", "scam.training"),
        poolWeight = 11,
        unique = true,
        conditions = listOf(
            stat(CAPITAL, GTE, 80_000L),
            notFlag("learned.scam.training"),
            inEra("kz_2024")
        ),
        options = listOf(
            option("training_pay", Strings["scam_training_cult_opt_training_pay"], "💳",
                next = "scam_training_first_level",
                fx = Effect(capitalDelta = -80_000, stressDelta = -5, knowledgeDelta = 3)),
            option("training_research", Strings["scam_training_cult_opt_training_research"], "🔍",
                next = "scam_training_reviews",
                fx = Effect(knowledgeDelta = 8)),
            option("training_decline", Strings["scam_training_cult_opt_training_decline"], "🙅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 5,
                    setFlags = setOf("learned.scam.training")
                ))
        )
    )

    // ── Training shared follow-ups ────────────────────────────────────────────

    private val trainingReviews = event(
        id = "scam_training_reviews",
        message = Strings["scam_training_reviews_msg"],
        flavor = "🔍",
        tags = setOf("scam.training", "educational"),
        options = listOf(
            option("training_research_lesson", Strings["scam_training_reviews_opt_training_research_lesson"], "✅",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 20, stressDelta = -5,
                    setFlags = setOf("learned.scam.training")
                ))
        )
    )

    private val trainingFirstLevel = event(
        id = "scam_training_first_level",
        message = Strings["scam_training_first_level_msg"],
        flavor = "🔥",
        tags = setOf("scam.training"),
        options = listOf(
            option("training_escalate", Strings["scam_training_first_level_opt_training_escalate"], "💸",
                next = MONTHLY_TICK,
                fx = Effect(
                    capitalDelta = -200_000, stressDelta = -5, knowledgeDelta = 2,
                    scheduleEvent = ScheduledEvent("scam_training_deeper", afterMonths = 2)
                )),
            option("training_stop", Strings["scam_training_first_level_opt_training_stop"], "🛑",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 20, stressDelta = 5,
                    setFlags = setOf("learned.scam.training")
                ))
        )
    )

    private val trainingDeeper = event(
        id = "scam_training_deeper",
        message = Strings["scam_training_deeper_msg"],
        flavor = "💡",
        tags = setOf("scam.training", "consequence"),
        options = listOf(
            option("training_exit", Strings["scam_training_deeper_opt_training_exit"], "🚪",
                next = MONTHLY_TICK,
                fx = Effect(
                    knowledgeDelta = 25, stressDelta = 10,
                    setFlags = setOf("learned.scam.training", "lost_money_to_scam")
                ))
        )
    )

    // ════════════════════════════════════════════════════════════════════
    //  EXPORT
    // ════════════════════════════════════════════════════════════════════

    val all: List<GameEvent> = listOf(
        // Pyramid — 4 era variants + shared follow-ups
        pyramidNeighbor90s, pyramidEmail2005, pyramidVk2015, pyramidFriendCall,
        pyramidAvoided, pyramidCollapse, pyramidSmallLoss,
        // MLM — 4 era variants + shared follow-ups
        mlmDoor90s, mlmPhone2005, mlmInstagram2015, mlmColleague,
        mlmPresentation, mlmConfronted, mlmMonthLater,
        // Capper — 3 era variants (no 90s) + shared follow-ups
        capperSms2005, capperVk2015, capperTelegram,
        capperExplained, capperLoses, bettingDeepLoss,
        // Romance — 2 era variants (2015+) + shared follow-ups
        romanceMamba2015, romanceFirstContact,
        romanceBuildup, romanceCryptoIntro, romanceWithdrawalBlocked,
        romanceFreeze, romanceFinal, romanceCaught,
        // Crypto — 2 era variants (2015+) + shared follow-ups
        cryptoForex2015, cryptoFakeExchange,
        cryptoNoLicense, cryptoWithdrawalTrap, cryptoFinalDisappear,
        // MFO — 4 era variants + shared follow-ups
        mfoNeighborLender90s, mfoOffice2005, mfoUrgentOnline, mfoUrgent2024,
        mfoContractRevealed, mfoSigned,
        // Middleman — 4 era variants + shared follow-ups
        middlemanTurkey90s, middlemanAlibaba2005, middlemanAli2015, middlemanChina2024,
        middlemanContractRefused, middlemanResult,
        // Training — 4 era variants + shared follow-ups
        trainingKiyosaki90s, trainingBusiness2005, trainingBm2015, trainingCult2024,
        trainingReviews, trainingFirstLevel, trainingDeeper
    )

    /**
     * Pool entries — all era variants included.
     * Era conditions on each event act as the gate;
     * EraDefinition.poolWeightModifiers provide further suppression (e.g. crypto=0 in 90s).
     */
    val poolEntries: List<PoolEntry> = listOf(
        // Pyramid
        PoolEntry("scam_pyramid_neighbor_90s", baseWeight = 18),
        PoolEntry("scam_pyramid_email_2005",   baseWeight = 18),
        PoolEntry("scam_pyramid_vk_2015",      baseWeight = 18),
        PoolEntry("scam_pyramid_friend",       baseWeight = 18),
        // MLM
        PoolEntry("scam_mlm_door_90s",         baseWeight = 14),
        PoolEntry("scam_mlm_phone_2005",       baseWeight = 14),
        PoolEntry("scam_mlm_instagram_2015",   baseWeight = 14),
        PoolEntry("scam_mlm_colleague",        baseWeight = 14),
        // Capper (no 90s)
        PoolEntry("scam_capper_sms_2005",      baseWeight = 10),
        PoolEntry("scam_capper_vk_2015",       baseWeight = 12),
        PoolEntry("scam_capper_telegram",      baseWeight = 12),
        // Romance (2015+)
        PoolEntry("scam_romance_mamba_2015",   baseWeight = 8),
        PoolEntry("scam_romance_contact",      baseWeight = 10),
        // Crypto (2015+)
        PoolEntry("scam_crypto_forex_2015",    baseWeight = 10),
        PoolEntry("scam_crypto_exchange",      baseWeight = 15),
        // MFO
        PoolEntry("scam_mfo_neighbor_90s",     baseWeight = 12),
        PoolEntry("scam_mfo_office_2005",      baseWeight = 12),
        PoolEntry("scam_mfo_urgent",           baseWeight = 12),
        PoolEntry("scam_mfo_urgent_2024",      baseWeight = 12),
        // Middleman
        PoolEntry("scam_middleman_turkey_90s", baseWeight = 10),
        PoolEntry("scam_middleman_alibaba_2005", baseWeight = 10),
        PoolEntry("scam_middleman_ali_2015",   baseWeight = 10),
        PoolEntry("scam_middleman_china",      baseWeight = 10),
        // Training
        PoolEntry("scam_training_kiyosaki_90s", baseWeight = 8),
        PoolEntry("scam_training_business_2005", baseWeight = 8),
        PoolEntry("scam_training_bm_2015",     baseWeight = 11),
        PoolEntry("scam_training_cult",        baseWeight = 11)
    )

    fun findById(id: String): GameEvent? = all.find { it.id == id }
}
