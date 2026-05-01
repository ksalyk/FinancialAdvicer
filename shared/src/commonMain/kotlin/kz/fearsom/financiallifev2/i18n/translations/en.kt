package kz.fearsom.financiallifev2.i18n.translations

// ════════════════════════════════════════════════════════════════════
//  ENGLISH STRINGS
//
//  UI strings live here. Game-content strings are generated in
//  en_content.kt and merged below.
// ════════════════════════════════════════════════════════════════════

val enStrings: Map<String, String> = mapOf(

    // ── LoginScreen ──────────────────────────────────────────────────
    "ui_login_subtitle"              to "The path to financial freedom",
    "ui_login_tab_register"          to "Register",
    "ui_login_tab_login"             to "Sign In",
    "ui_login_field_username"        to "Username",
    "ui_login_field_password"        to "Password",
    "ui_login_btn_register"          to "Create Account",
    "ui_login_btn_login"             to "Sign In",
    "ui_login_demo_hint"             to "Demo: demo / demo123",
    "ui_login_already_have_account"  to "Already have an account? Sign In",
    "ui_login_no_account"            to "No account? Register",

    // ── MainMenuScreen ───────────────────────────────────────────────
    "ui_main_tagline"                to "Getting rich is a science",
    "ui_main_continue"               to "Continue Game",
    "ui_main_new_game"               to "New Game",
    "ui_main_new_game_subtitle"      to "Choose an era and character",
    "ui_main_characters"             to "Characters",
    "ui_main_characters_subtitle"    to "Explore backstory and stats",
    "ui_main_stats"                  to "Statistics",
    "ui_main_games_played"           to "Games played:",
    "ui_main_best_ending"            to "Best ending:",
    "ui_main_start_story"            to "Start your story",
    "ui_main_settings"               to "Settings",
    "ui_main_settings_subtitle"      to "Language and profile",
    "ui_main_logout"                 to "Sign Out",

    // ── EraSelectionScreen ───────────────────────────────────────────
    "ui_era_title"                   to "Choose an Era",
    "ui_era_subtitle"                to "Each era — unique economic events",
    "ui_era_locked_hint"             to "Unlock by completing any other era",
    "ui_era_inflation"               to "Inflation",
    "ui_era_salary"                  to "Salaries",

    // ── CharacterSelectionScreen ─────────────────────────────────────
    "ui_char_sel_title"              to "Choose a Character",
    "ui_char_sel_tab_characters"     to "👥 Characters",
    "ui_char_sel_tab_bundles"        to "🎭 Bundles",
    "ui_char_sel_age_suffix"         to "y.o. ·",
    "ui_char_sel_locked"             to "Unlocks under condition",
    "ui_char_sel_diff_easy"          to "Easy",
    "ui_char_sel_diff_medium"        to "Medium",
    "ui_char_sel_diff_hard"          to "Hard",
    "ui_char_sel_diff_nightmare"     to "Nightmare",
    "ui_char_sel_unlock_complete"    to "Complete a game:",
    "ui_char_sel_unlock_reach"       to "Reach",
    "ui_char_sel_unlock_era"         to "Play the era",
    "ui_char_sel_unlock_complete_n"  to "Complete %d game(s)",
    "ui_char_sel_per_month"          to "/mo",

    // ── CharactersScreen ─────────────────────────────────────────────
    "ui_chars_title"                 to "Characters",
    "ui_chars_subtitle"              to "Tap a character to learn their story",
    "ui_chars_age"                   to "y.o.",

    // ── CharacterDetailScreen ────────────────────────────────────────
    "ui_char_detail_not_found"       to "Character not found",
    "ui_char_detail_backstory"       to "📖 Backstory",
    "ui_char_detail_stats"           to "💹 Starting Stats",
    "ui_char_detail_eras"            to "🗺️ Available Eras",
    "ui_char_detail_difficulty"      to "⚔️ Difficulty",
    "ui_char_detail_locked_era"      to "🔒 Locked",
    "ui_char_detail_play"            to "🎮 Play as",
    "ui_char_detail_locked_char"     to "🔒 Character Locked",
    "ui_char_detail_age_era"         to "y.o. ·",
    "ui_char_detail_stat_capital"    to "💰 Capital",
    "ui_char_detail_stat_income"     to "📈 Income/mo",
    "ui_char_detail_stat_expenses"   to "🏠 Expenses/mo",
    "ui_char_detail_stat_debt"       to "💳 Debt",
    "ui_char_detail_stat_stress"     to "😰 Stress",
    "ui_char_detail_stat_knowledge"  to "🎓 Knowledge",
    "ui_char_detail_diff_easy_desc"  to "More capital and income. Perfect for first-time players.",
    "ui_char_detail_diff_medium_desc" to "Balanced start. Standard experience.",
    "ui_char_detail_diff_hard_desc"  to "Less resources, more risks. For experienced players.",
    "ui_char_detail_diff_nm_desc"    to "Extreme conditions. For the bravest.",

    // ── StatisticsScreen ─────────────────────────────────────────────
    "ui_stats_title"                 to "Statistics",
    "ui_stats_subtitle"              to "Your financial achievements",
    "ui_stats_loading"               to "Loading...",
    "ui_stats_empty"                 to "No statistics yet",
    "ui_stats_empty_hint"            to "Complete at least one game...",
    "ui_stats_total_games"           to "Total games",
    "ui_stats_completed"             to "Completed",
    "ui_stats_avg_capital"           to "Avg. capital",
    "ui_stats_best_ending"           to "Best ending",
    "ui_stats_tab_endings"           to "📈 Endings",
    "ui_stats_tab_characters"        to "👥 By character",
    "ui_stats_tab_eras"              to "🗺️ By era",
    "ui_stats_games_avg"             to "games · Avg. capital:",
    "ui_stats_games"                 to "games",

    // ── SettingsScreen ───────────────────────────────────────────────
    "ui_settings_title"              to "Settings",
    "ui_settings_subtitle"           to "App language",
    "ui_settings_language"           to "Language",
    "ui_settings_language_subtitle"  to "Changes apply immediately and are saved on this device.",
    "ui_settings_language_russian"   to "Русский",
    "ui_settings_language_kazakh"    to "Қазақша",
    "ui_settings_language_english"   to "English",

    // ── Settings — Gameplay ───────────────────────────────────────────
    "ui_settings_gameplay"           to "Gameplay",
    "ui_settings_typing_anim"        to "Typing animation",
    "ui_settings_typing_anim_sub"    to "Text appears gradually, as if being typed",
    "ui_settings_typing_pace"        to "Typing speed",
    "ui_settings_pace_slow"          to "Slow",
    "ui_settings_pace_normal"        to "Normal",
    "ui_settings_pace_fast"          to "Fast",

    // ── ChatScreen ───────────────────────────────────────────────────
    "ui_chat_reset_title"            to "Reset progress?",
    "ui_chat_reset_message"          to "This will delete all chat history and return the character to their initial state.",
    "ui_chat_reset_confirm"          to "Reset",
    "ui_chat_cancel"                 to "Cancel",
    "ui_chat_cd_home"                to "Home menu",
    "ui_chat_cd_stats"               to "Financial statistics",
    "ui_chat_cd_options"             to "More options",
    "ui_chat_diary"                  to "Diary",
    "ui_chat_restart"                to "Restart game",
    "ui_chat_player_prefix"          to "✍️  I decided:",
    "ui_chat_action_label"           to "What I'll do:",
    "ui_chat_writing"                to "writing in diary...",
    "ui_chat_skip"                   to "Skip",
    "ui_chat_monthly_report"         to "Monthly summary",
    "ui_chat_scene_scam"             to "⚠️ Watch out",
    "ui_chat_scene_crisis"           to "📉 Crisis",
    "ui_chat_scene_career"           to "💼 Career",
    "ui_chat_scene_family"           to "🏠 Family",
    "ui_chat_scene_investment"       to "📈 Investment",
    "ui_chat_scene_mortgage"         to "🔑 Mortgage",
    "ui_chat_scene_windfall"         to "🎉 Windfall",
    "ui_chat_scene_world"            to "🌙 Reflection",
    "ui_chat_restart_game"           to "🔄 Start over",
    // Short month names
    "ui_chat_short_month_1"          to "Jan",
    "ui_chat_short_month_2"          to "Feb",
    "ui_chat_short_month_3"          to "Mar",
    "ui_chat_short_month_4"          to "Apr",
    "ui_chat_short_month_5"          to "May",
    "ui_chat_short_month_6"          to "Jun",
    "ui_chat_short_month_7"          to "Jul",
    "ui_chat_short_month_8"          to "Aug",
    "ui_chat_short_month_9"          to "Sep",
    "ui_chat_short_month_10"         to "Oct",
    "ui_chat_short_month_11"         to "Nov",
    "ui_chat_short_month_12"         to "Dec",
    // Genitive (used as "on the Nth of [month]" — same in English)
    "ui_chat_month_gen_1"            to "January",
    "ui_chat_month_gen_2"            to "February",
    "ui_chat_month_gen_3"            to "March",
    "ui_chat_month_gen_4"            to "April",
    "ui_chat_month_gen_5"            to "May",
    "ui_chat_month_gen_6"            to "June",
    "ui_chat_month_gen_7"            to "July",
    "ui_chat_month_gen_8"            to "August",
    "ui_chat_month_gen_9"            to "September",
    "ui_chat_month_gen_10"           to "October",
    "ui_chat_month_gen_11"           to "November",
    "ui_chat_month_gen_12"           to "December",

    // ── StatsPanelOverlay ────────────────────────────────────────────
    "ui_stats_panel_title"           to "Finances",
    "ui_stats_panel_freedom"         to "🎯 Financial Freedom",
    "ui_stats_panel_start"           to "Start",
    "ui_stats_panel_freedom_label"   to "Freedom",
    "ui_stats_panel_flow"            to "💸 Cash Flow",
    "ui_stats_panel_profit"          to "Profit",
    "ui_stats_panel_deficit"         to "Deficit",
    "ui_stats_panel_capital"         to "💰 Capital",
    "ui_stats_panel_debt"            to "💳 Debt",
    "ui_stats_panel_income"          to "📈 Income",
    "ui_stats_panel_expenses"        to "🛒 Expenses",
    "ui_stats_panel_investments"     to "📊 Investments",
    "ui_stats_panel_indicators"      to "Indicators",
    "ui_stats_panel_stress"          to "😰 Stress",
    "ui_stats_panel_knowledge"       to "📚 Fin. literacy",
    "ui_stats_panel_risk"            to "🎲 Risk level",
    "ui_stats_panel_per_month"       to "/mo",
    "ui_stats_panel_month_1"         to "January",
    "ui_stats_panel_month_2"         to "February",
    "ui_stats_panel_month_3"         to "March",
    "ui_stats_panel_month_4"         to "April",
    "ui_stats_panel_month_5"         to "May",
    "ui_stats_panel_month_6"         to "June",
    "ui_stats_panel_month_7"         to "July",
    "ui_stats_panel_month_8"         to "August",
    "ui_stats_panel_month_9"         to "September",
    "ui_stats_panel_month_10"        to "October",
    "ui_stats_panel_month_11"        to "November",
    "ui_stats_panel_month_12"        to "December",

    // ── AppBar ───────────────────────────────────────────────────────
    "ui_appbar_back"                 to "Back",

    // ── Ending labels ────────────────────────────────────────────────
    "ending_bankruptcy"              to "💔 Bankruptcy",
    "ending_paycheck"                to "😰 Paycheck to Paycheck",
    "ending_stability"               to "😊 Financial Stability",
    "ending_freedom"                 to "🎯 Financial Freedom!",
    "ending_wealth"                  to "🤑 Wealth",
    "ending_prison"                  to "⛓️ Prison",
    "ending_game_over"               to "🏁 Game Over",

    // ── Currency suffixes ────────────────────────────────────────────
    "currency_rub"                   to "rub.",

    // ── System / GameEngine ──────────────────────────────────────────
    "sys_game_start"                 to "🎮 The financial adventure has begun! Help %s build their financial future.",
    "sys_default_character_name"     to "Asan",

    // ── MonthlyReport ────────────────────────────────────────────────
    "sys_monthly_title"              to "📊 %s %d — Monthly Summary",
    "sys_monthly_income"             to "💰 Income:           +%s",
    "sys_monthly_expenses"           to "🏠 Expenses:         -%s",
    "sys_monthly_debt_payment"       to "💳 Debt payment:     -%s",
    "sys_monthly_investments"        to "📈 Investments:      +%s",
    "sys_monthly_net_positive"       to "✅ Total: +%s",
    "sys_monthly_net_negative"       to "⚠️ Total: %s",
    "sys_monthly_capital"            to "💼 Capital: %s",
    "sys_monthly_debt_remaining"     to "💳 Debt: %s",

    // ── Month names ──────────────────────────────────────────────────
    "month_1"                        to "January",
    "month_2"                        to "February",
    "month_3"                        to "March",
    "month_4"                        to "April",
    "month_5"                        to "May",
    "month_6"                        to "June",
    "month_7"                        to "July",
    "month_8"                        to "August",
    "month_9"                        to "September",
    "month_10"                       to "October",
    "month_11"                       to "November",
    "month_12"                       to "December",

    // ── Era names ────────────────────────────────────────────────────
    "era_modern_kz_2024_name"        to "Modern Kazakhstan",
    "era_kz_90s_name"                to "Kazakhstan 1990s",
    "era_kz_2015_name"               to "Kazakhstan 2015",

    // ── Auth errors ──────────────────────────────────────────────────
    "err_auth_fill_fields"           to "Please fill in all fields",
    "err_auth_server_unavailable"    to "Server unavailable (%d)",
    "err_auth_login_too_short"       to "Username must be at least 3 characters",
    "err_auth_password_too_short"    to "Password must be at least 6 characters",
    "err_auth_user_exists"           to "User already exists",
    "err_auth_user_not_found"        to "User not found",
    "err_auth_wrong_password"        to "Incorrect password",
    "err_auth_refresh_missing"       to "Missing refresh token",
    "err_auth_refresh_invalid"       to "Invalid or expired refresh token",

) + enContentStrings + enHardcodedStrings
