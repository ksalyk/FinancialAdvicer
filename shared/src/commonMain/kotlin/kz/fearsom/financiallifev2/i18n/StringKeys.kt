package kz.fearsom.financiallifev2.i18n

// ════════════════════════════════════════════════════════════════════
//  STRING KEYS — compile-time constants for every localised string
//
//  Naming convention:
//    UI_<SCREEN>_<ELEMENT>   — user-facing screen text
//    ENDING_*                — EndingType display labels
//    SYS_*                   — GameEngine / MonthlyReport system strings
//    MONTH_*                 — month names (1-indexed)
//    ERA_*                   — era names (used in EraRegistry)
//    ERR_*                   — validation / network error messages
//
//  Game-content strings (scenario event messages & option texts) are
//  referenced by plain string literals (e.g. "evt_aidar_intro_msg")
//  and do not require const vals here.
// ════════════════════════════════════════════════════════════════════

object StringKeys {

    // ── LoginScreen ──────────────────────────────────────────────────
    const val UI_LOGIN_SUBTITLE               = "ui_login_subtitle"
    const val UI_LOGIN_TAB_REGISTER           = "ui_login_tab_register"
    const val UI_LOGIN_TAB_LOGIN              = "ui_login_tab_login"
    const val UI_LOGIN_FIELD_USERNAME         = "ui_login_field_username"
    const val UI_LOGIN_FIELD_PASSWORD         = "ui_login_field_password"
    const val UI_LOGIN_BTN_REGISTER           = "ui_login_btn_register"
    const val UI_LOGIN_BTN_LOGIN              = "ui_login_btn_login"
    const val UI_LOGIN_DEMO_HINT              = "ui_login_demo_hint"
    const val UI_LOGIN_ALREADY_HAVE_ACCOUNT   = "ui_login_already_have_account"
    const val UI_LOGIN_NO_ACCOUNT             = "ui_login_no_account"

    // ── MainMenuScreen ───────────────────────────────────────────────
    const val UI_MAIN_TAGLINE                 = "ui_main_tagline"
    const val UI_MAIN_CONTINUE                = "ui_main_continue"
    const val UI_MAIN_NEW_GAME                = "ui_main_new_game"
    const val UI_MAIN_NEW_GAME_SUBTITLE       = "ui_main_new_game_subtitle"
    const val UI_MAIN_CHARACTERS              = "ui_main_characters"
    const val UI_MAIN_CHARACTERS_SUBTITLE     = "ui_main_characters_subtitle"
    const val UI_MAIN_STATS                   = "ui_main_stats"
    const val UI_MAIN_GAMES_PLAYED            = "ui_main_games_played"
    const val UI_MAIN_BEST_ENDING             = "ui_main_best_ending"
    const val UI_MAIN_START_STORY             = "ui_main_start_story"
    const val UI_MAIN_SETTINGS                = "ui_main_settings"
    const val UI_MAIN_SETTINGS_SUBTITLE       = "ui_main_settings_subtitle"
    const val UI_MAIN_LOGOUT                  = "ui_main_logout"

    // ── EraSelectionScreen ───────────────────────────────────────────
    const val UI_ERA_TITLE                    = "ui_era_title"
    const val UI_ERA_SUBTITLE                 = "ui_era_subtitle"
    const val UI_ERA_LOCKED_HINT              = "ui_era_locked_hint"
    const val UI_ERA_INFLATION                = "ui_era_inflation"
    const val UI_ERA_SALARY                   = "ui_era_salary"

    // ── CharacterSelectionScreen ─────────────────────────────────────
    const val UI_CHAR_SEL_TITLE               = "ui_char_sel_title"
    const val UI_CHAR_SEL_TAB_CHARACTERS      = "ui_char_sel_tab_characters"
    const val UI_CHAR_SEL_TAB_BUNDLES         = "ui_char_sel_tab_bundles"
    const val UI_CHAR_SEL_AGE_SUFFIX          = "ui_char_sel_age_suffix"
    const val UI_CHAR_SEL_LOCKED              = "ui_char_sel_locked"
    const val UI_CHAR_SEL_DIFF_EASY           = "ui_char_sel_diff_easy"
    const val UI_CHAR_SEL_DIFF_MEDIUM         = "ui_char_sel_diff_medium"
    const val UI_CHAR_SEL_DIFF_HARD           = "ui_char_sel_diff_hard"
    const val UI_CHAR_SEL_DIFF_NIGHTMARE      = "ui_char_sel_diff_nightmare"
    const val UI_CHAR_SEL_UNLOCK_COMPLETE     = "ui_char_sel_unlock_complete"
    const val UI_CHAR_SEL_UNLOCK_REACH        = "ui_char_sel_unlock_reach"
    const val UI_CHAR_SEL_UNLOCK_ERA          = "ui_char_sel_unlock_era"
    const val UI_CHAR_SEL_UNLOCK_COMPLETE_N   = "ui_char_sel_unlock_complete_n"
    const val UI_CHAR_SEL_PER_MONTH           = "ui_char_sel_per_month"

    // ── CharactersScreen ─────────────────────────────────────────────
    const val UI_CHARS_TITLE                  = "ui_chars_title"
    const val UI_CHARS_SUBTITLE               = "ui_chars_subtitle"
    const val UI_CHARS_AGE                    = "ui_chars_age"

    // ── CharacterDetailScreen ────────────────────────────────────────
    const val UI_CHAR_DETAIL_NOT_FOUND        = "ui_char_detail_not_found"
    const val UI_CHAR_DETAIL_BACKSTORY        = "ui_char_detail_backstory"
    const val UI_CHAR_DETAIL_STATS            = "ui_char_detail_stats"
    const val UI_CHAR_DETAIL_ERAS             = "ui_char_detail_eras"
    const val UI_CHAR_DETAIL_DIFFICULTY       = "ui_char_detail_difficulty"
    const val UI_CHAR_DETAIL_LOCKED_ERA       = "ui_char_detail_locked_era"
    const val UI_CHAR_DETAIL_PLAY             = "ui_char_detail_play"
    const val UI_CHAR_DETAIL_LOCKED_CHAR      = "ui_char_detail_locked_char"
    const val UI_CHAR_DETAIL_AGE_ERA          = "ui_char_detail_age_era"
    const val UI_CHAR_DETAIL_STAT_CAPITAL     = "ui_char_detail_stat_capital"
    const val UI_CHAR_DETAIL_STAT_INCOME      = "ui_char_detail_stat_income"
    const val UI_CHAR_DETAIL_STAT_EXPENSES    = "ui_char_detail_stat_expenses"
    const val UI_CHAR_DETAIL_STAT_DEBT        = "ui_char_detail_stat_debt"
    const val UI_CHAR_DETAIL_STAT_STRESS      = "ui_char_detail_stat_stress"
    const val UI_CHAR_DETAIL_STAT_KNOWLEDGE   = "ui_char_detail_stat_knowledge"
    const val UI_CHAR_DETAIL_DIFF_EASY_DESC   = "ui_char_detail_diff_easy_desc"
    const val UI_CHAR_DETAIL_DIFF_MEDIUM_DESC = "ui_char_detail_diff_medium_desc"
    const val UI_CHAR_DETAIL_DIFF_HARD_DESC   = "ui_char_detail_diff_hard_desc"
    const val UI_CHAR_DETAIL_DIFF_NM_DESC     = "ui_char_detail_diff_nm_desc"

    // ── StatisticsScreen ─────────────────────────────────────────────
    const val UI_STATS_TITLE                  = "ui_stats_title"
    const val UI_STATS_SUBTITLE               = "ui_stats_subtitle"
    const val UI_STATS_LOADING                = "ui_stats_loading"
    const val UI_STATS_EMPTY                  = "ui_stats_empty"
    const val UI_STATS_EMPTY_HINT             = "ui_stats_empty_hint"
    const val UI_STATS_TOTAL_GAMES            = "ui_stats_total_games"
    const val UI_STATS_COMPLETED              = "ui_stats_completed"
    const val UI_STATS_AVG_CAPITAL            = "ui_stats_avg_capital"
    const val UI_STATS_BEST_ENDING            = "ui_stats_best_ending"
    const val UI_STATS_TAB_ENDINGS            = "ui_stats_tab_endings"
    const val UI_STATS_TAB_CHARACTERS         = "ui_stats_tab_characters"
    const val UI_STATS_TAB_ERAS               = "ui_stats_tab_eras"
    const val UI_STATS_GAMES_AVG              = "ui_stats_games_avg"
    const val UI_STATS_GAMES                  = "ui_stats_games"

    // ── SettingsScreen ───────────────────────────────────────────────
    const val UI_SETTINGS_TITLE               = "ui_settings_title"
    const val UI_SETTINGS_SUBTITLE            = "ui_settings_subtitle"
    const val UI_SETTINGS_LANGUAGE            = "ui_settings_language"
    const val UI_SETTINGS_LANGUAGE_SUBTITLE   = "ui_settings_language_subtitle"
    const val UI_SETTINGS_LANGUAGE_RUSSIAN    = "ui_settings_language_russian"
    const val UI_SETTINGS_LANGUAGE_KAZAKH     = "ui_settings_language_kazakh"
    const val UI_SETTINGS_LANGUAGE_ENGLISH    = "ui_settings_language_english"

    // ── ChatScreen ───────────────────────────────────────────────────
    const val UI_CHAT_RESET_TITLE             = "ui_chat_reset_title"
    const val UI_CHAT_RESET_MESSAGE           = "ui_chat_reset_message"
    const val UI_CHAT_RESET_CONFIRM           = "ui_chat_reset_confirm"
    const val UI_CHAT_CANCEL                  = "ui_chat_cancel"
    const val UI_CHAT_CD_HOME                 = "ui_chat_cd_home"
    const val UI_CHAT_CD_STATS                = "ui_chat_cd_stats"
    const val UI_CHAT_CD_OPTIONS              = "ui_chat_cd_options"
    const val UI_CHAT_DIARY                   = "ui_chat_diary"
    const val UI_CHAT_RESTART                 = "ui_chat_restart"
    const val UI_CHAT_PLAYER_PREFIX           = "ui_chat_player_prefix"
    const val UI_CHAT_ACTION_LABEL            = "ui_chat_action_label"
    const val UI_CHAT_WRITING                 = "ui_chat_writing"
    const val UI_CHAT_MONTHLY_REPORT          = "ui_chat_monthly_report"
    const val UI_CHAT_SCENE_SCAM              = "ui_chat_scene_scam"
    const val UI_CHAT_SCENE_CRISIS            = "ui_chat_scene_crisis"
    const val UI_CHAT_SCENE_CAREER            = "ui_chat_scene_career"
    const val UI_CHAT_SCENE_FAMILY            = "ui_chat_scene_family"
    const val UI_CHAT_SCENE_INVESTMENT        = "ui_chat_scene_investment"
    const val UI_CHAT_SCENE_MORTGAGE          = "ui_chat_scene_mortgage"
    const val UI_CHAT_SCENE_WINDFALL          = "ui_chat_scene_windfall"
    const val UI_CHAT_SCENE_WORLD             = "ui_chat_scene_world"
    const val UI_CHAT_RESTART_GAME            = "ui_chat_restart_game"
    // Short month names (Янв, Фев …)
    const val UI_CHAT_SHORT_MONTH_1           = "ui_chat_short_month_1"
    const val UI_CHAT_SHORT_MONTH_2           = "ui_chat_short_month_2"
    const val UI_CHAT_SHORT_MONTH_3           = "ui_chat_short_month_3"
    const val UI_CHAT_SHORT_MONTH_4           = "ui_chat_short_month_4"
    const val UI_CHAT_SHORT_MONTH_5           = "ui_chat_short_month_5"
    const val UI_CHAT_SHORT_MONTH_6           = "ui_chat_short_month_6"
    const val UI_CHAT_SHORT_MONTH_7           = "ui_chat_short_month_7"
    const val UI_CHAT_SHORT_MONTH_8           = "ui_chat_short_month_8"
    const val UI_CHAT_SHORT_MONTH_9           = "ui_chat_short_month_9"
    const val UI_CHAT_SHORT_MONTH_10          = "ui_chat_short_month_10"
    const val UI_CHAT_SHORT_MONTH_11          = "ui_chat_short_month_11"
    const val UI_CHAT_SHORT_MONTH_12          = "ui_chat_short_month_12"
    // Genitive month names (января, февраля …)
    const val UI_CHAT_MONTH_GEN_1             = "ui_chat_month_gen_1"
    const val UI_CHAT_MONTH_GEN_2             = "ui_chat_month_gen_2"
    const val UI_CHAT_MONTH_GEN_3             = "ui_chat_month_gen_3"
    const val UI_CHAT_MONTH_GEN_4             = "ui_chat_month_gen_4"
    const val UI_CHAT_MONTH_GEN_5             = "ui_chat_month_gen_5"
    const val UI_CHAT_MONTH_GEN_6             = "ui_chat_month_gen_6"
    const val UI_CHAT_MONTH_GEN_7             = "ui_chat_month_gen_7"
    const val UI_CHAT_MONTH_GEN_8             = "ui_chat_month_gen_8"
    const val UI_CHAT_MONTH_GEN_9             = "ui_chat_month_gen_9"
    const val UI_CHAT_MONTH_GEN_10            = "ui_chat_month_gen_10"
    const val UI_CHAT_MONTH_GEN_11            = "ui_chat_month_gen_11"
    const val UI_CHAT_MONTH_GEN_12            = "ui_chat_month_gen_12"

    // ── StatsPanelOverlay ────────────────────────────────────────────
    const val UI_STATS_PANEL_TITLE            = "ui_stats_panel_title"
    const val UI_STATS_PANEL_FREEDOM          = "ui_stats_panel_freedom"
    const val UI_STATS_PANEL_START            = "ui_stats_panel_start"
    const val UI_STATS_PANEL_FREEDOM_LABEL    = "ui_stats_panel_freedom_label"
    const val UI_STATS_PANEL_FLOW             = "ui_stats_panel_flow"
    const val UI_STATS_PANEL_PROFIT           = "ui_stats_panel_profit"
    const val UI_STATS_PANEL_DEFICIT          = "ui_stats_panel_deficit"
    const val UI_STATS_PANEL_CAPITAL          = "ui_stats_panel_capital"
    const val UI_STATS_PANEL_DEBT             = "ui_stats_panel_debt"
    const val UI_STATS_PANEL_INCOME           = "ui_stats_panel_income"
    const val UI_STATS_PANEL_EXPENSES         = "ui_stats_panel_expenses"
    const val UI_STATS_PANEL_INVESTMENTS      = "ui_stats_panel_investments"
    const val UI_STATS_PANEL_INDICATORS       = "ui_stats_panel_indicators"
    const val UI_STATS_PANEL_STRESS           = "ui_stats_panel_stress"
    const val UI_STATS_PANEL_KNOWLEDGE        = "ui_stats_panel_knowledge"
    const val UI_STATS_PANEL_RISK             = "ui_stats_panel_risk"
    const val UI_STATS_PANEL_PER_MONTH        = "ui_stats_panel_per_month"
    // Full month names for stats panel
    const val UI_STATS_PANEL_MONTH_1          = "ui_stats_panel_month_1"
    const val UI_STATS_PANEL_MONTH_2          = "ui_stats_panel_month_2"
    const val UI_STATS_PANEL_MONTH_3          = "ui_stats_panel_month_3"
    const val UI_STATS_PANEL_MONTH_4          = "ui_stats_panel_month_4"
    const val UI_STATS_PANEL_MONTH_5          = "ui_stats_panel_month_5"
    const val UI_STATS_PANEL_MONTH_6          = "ui_stats_panel_month_6"
    const val UI_STATS_PANEL_MONTH_7          = "ui_stats_panel_month_7"
    const val UI_STATS_PANEL_MONTH_8          = "ui_stats_panel_month_8"
    const val UI_STATS_PANEL_MONTH_9          = "ui_stats_panel_month_9"
    const val UI_STATS_PANEL_MONTH_10         = "ui_stats_panel_month_10"
    const val UI_STATS_PANEL_MONTH_11         = "ui_stats_panel_month_11"
    const val UI_STATS_PANEL_MONTH_12         = "ui_stats_panel_month_12"

    // ── AppBar ───────────────────────────────────────────────────────
    const val UI_APPBAR_BACK                  = "ui_appbar_back"

    // ── Ending labels (GamePresenter) ────────────────────────────────
    const val ENDING_BANKRUPTCY               = "ending_bankruptcy"
    const val ENDING_PAYCHECK_TO_PAYCHECK     = "ending_paycheck"
    const val ENDING_STABILITY                = "ending_stability"
    const val ENDING_FREEDOM                  = "ending_freedom"
    const val ENDING_WEALTH                   = "ending_wealth"
    const val ENDING_PRISON                   = "ending_prison"
    const val ENDING_GAME_OVER                = "ending_game_over"

    // ── Currency suffixes ────────────────────────────────────────────
    const val CURRENCY_RUB                    = "currency_rub"

    // ── System / GameEngine ──────────────────────────────────────────
    const val SYS_GAME_START                  = "sys_game_start"
    const val SYS_DEFAULT_CHARACTER_NAME      = "sys_default_character_name"

    // ── MonthlyReport ────────────────────────────────────────────────
    const val SYS_MONTHLY_TITLE               = "sys_monthly_title"
    const val SYS_MONTHLY_INCOME              = "sys_monthly_income"
    const val SYS_MONTHLY_EXPENSES            = "sys_monthly_expenses"
    const val SYS_MONTHLY_DEBT_PAYMENT        = "sys_monthly_debt_payment"
    const val SYS_MONTHLY_INVESTMENTS         = "sys_monthly_investments"
    const val SYS_MONTHLY_NET_POSITIVE        = "sys_monthly_net_positive"
    const val SYS_MONTHLY_NET_NEGATIVE        = "sys_monthly_net_negative"
    const val SYS_MONTHLY_CAPITAL             = "sys_monthly_capital"
    const val SYS_MONTHLY_DEBT_REMAINING      = "sys_monthly_debt_remaining"

    // ── Month names (MonthlyReport, 1-indexed) ───────────────────────
    const val MONTH_1                         = "month_1"
    const val MONTH_2                         = "month_2"
    const val MONTH_3                         = "month_3"
    const val MONTH_4                         = "month_4"
    const val MONTH_5                         = "month_5"
    const val MONTH_6                         = "month_6"
    const val MONTH_7                         = "month_7"
    const val MONTH_8                         = "month_8"
    const val MONTH_9                         = "month_9"
    const val MONTH_10                        = "month_10"
    const val MONTH_11                        = "month_11"
    const val MONTH_12                        = "month_12"

    // ── Era names (EraRegistry) ──────────────────────────────────────
    const val ERA_MODERN_KZ_2024_NAME         = "era_modern_kz_2024_name"
    const val ERA_KZ_90S_NAME                 = "era_kz_90s_name"
    const val ERA_KZ_2015_NAME                = "era_kz_2015_name"

    // ── Auth errors ──────────────────────────────────────────────────
    const val ERR_AUTH_FILL_FIELDS            = "err_auth_fill_fields"
    const val ERR_AUTH_SERVER_UNAVAILABLE     = "err_auth_server_unavailable"
    const val ERR_AUTH_LOGIN_TOO_SHORT        = "err_auth_login_too_short"
    const val ERR_AUTH_PASSWORD_TOO_SHORT     = "err_auth_password_too_short"
}
