package kz.fearsom.financiallifev2.i18n.translations

// ════════════════════════════════════════════════════════════════════
//  KAZAKH STRINGS
//
//  Only UI strings are translated here.
//  Game-content strings (evt_*, scam_*, era event messages) are NOT
//  included — they fall back to Russian via Strings.get() fallback.
//  Add a "// TODO i18n" comment where a content key is missing.
// ════════════════════════════════════════════════════════════════════

val kkStrings: Map<String, String> = mapOf(

    // ── LoginScreen ──────────────────────────────────────────────────
    "ui_login_subtitle"              to "Қаржылық бостандыққа жол",
    "ui_login_tab_register"          to "Тіркелу",
    "ui_login_tab_login"             to "Кіру",
    "ui_login_field_username"        to "Логин",
    "ui_login_field_password"        to "Құпия сөз",
    "ui_login_btn_register"          to "Тіркелу",
    "ui_login_btn_login"             to "Кіру",
    "ui_login_demo_hint"             to "Демо: demo / demo123",
    "ui_login_already_have_account"  to "Аккаунт бар ма? Кіру",
    "ui_login_no_account"            to "Аккаунт жоқ па? Тіркелу",

    // ── MainMenuScreen ───────────────────────────────────────────────
    "ui_main_tagline"                to "Бай болу — бұл ғылым",
    "ui_main_continue"               to "Ойынды жалғастыру",
    "ui_main_new_game"               to "Жаңа ойын",
    "ui_main_new_game_subtitle"      to "Дәуір мен кейіпкерді таңдаңыз",
    "ui_main_characters"             to "Кейіпкерлер",
    "ui_main_characters_subtitle"    to "Алдыңғы тарихы мен сипаттамасын зерттеңіз",
    "ui_main_stats"                  to "Статистика",
    "ui_main_games_played"           to "Ойналған ойындар:",
    "ui_main_best_ending"            to "Үздік соңы:",
    "ui_main_start_story"            to "Өз тарихыңды бастаңыз",
    "ui_main_settings"               to "Баптаулар",
    "ui_main_settings_subtitle"      to "Тіл және профиль",
    "ui_main_logout"                 to "Шығу",

    // ── EraSelectionScreen ───────────────────────────────────────────
    "ui_era_title"                   to "Дәуірді таңдаңыз",
    "ui_era_subtitle"                to "Әр дәуір — бірегей экономикалық оқиғалар",
    "ui_era_locked_hint"             to "Кез келген басқа дәуірді аяқтау арқылы ашыңыз",
    "ui_era_inflation"               to "Инфляция",
    "ui_era_salary"                  to "Жалақы",

    // ── CharacterSelectionScreen ─────────────────────────────────────
    "ui_char_sel_title"              to "Кейіпкерді таңдаңыз",
    "ui_char_sel_tab_characters"     to "👥 Кейіпкерлер",
    "ui_char_sel_tab_bundles"        to "🎭 Жинақтар",
    "ui_char_sel_age_suffix"         to "жас ·",
    "ui_char_sel_locked"             to "Шартты орындаумен ашылады",
    "ui_char_sel_diff_easy"          to "Оңай",
    "ui_char_sel_diff_medium"        to "Орташа",
    "ui_char_sel_diff_hard"          to "Қиын",
    "ui_char_sel_diff_nightmare"     to "Қорқынышты",
    "ui_char_sel_unlock_complete"    to "Ойынды аяқтаңыз:",
    "ui_char_sel_unlock_reach"       to "Жетіңіз",
    "ui_char_sel_unlock_era"         to "Дәуірде ойнаңыз",
    "ui_char_sel_unlock_complete_n"  to "%d ойын(дар)ын аяқтаңыз",
    "ui_char_sel_per_month"          to "/ай",

    // ── CharactersScreen ─────────────────────────────────────────────
    "ui_chars_title"                 to "Кейіпкерлер",
    "ui_chars_subtitle"              to "Тарихын білу үшін кейіпкерге басыңыз",
    "ui_chars_age"                   to "жас",

    // ── CharacterDetailScreen ────────────────────────────────────────
    "ui_char_detail_not_found"       to "Кейіпкер табылмады",
    "ui_char_detail_backstory"       to "📖 Алдыңғы тарихы",
    "ui_char_detail_stats"           to "💹 Бастапқы көрсеткіштер",
    "ui_char_detail_eras"            to "🗺️ Қолжетімді дәуірлер",
    "ui_char_detail_difficulty"      to "⚔️ Күрделілік",
    "ui_char_detail_locked_era"      to "🔒 Бұғатталған",
    "ui_char_detail_play"            to "🎮 Ойнау үшін",
    "ui_char_detail_locked_char"     to "🔒 Кейіпкер бұғатталған",
    "ui_char_detail_age_era"         to "жас ·",
    "ui_char_detail_stat_capital"    to "💰 Капитал",
    "ui_char_detail_stat_income"     to "📈 Табыс/ай",
    "ui_char_detail_stat_expenses"   to "🏠 Шығыс/ай",
    "ui_char_detail_stat_debt"       to "💳 Қарыз",
    "ui_char_detail_stat_stress"     to "😰 Стресс",
    "ui_char_detail_stat_knowledge"  to "🎓 Білім",
    "ui_char_detail_diff_easy_desc"  to "Көбірек капитал мен табыс. Бірінші танысу үшін тамаша.",
    "ui_char_detail_diff_medium_desc" to "Теңдестірілген бастама. Стандартты тәжірибе.",
    "ui_char_detail_diff_hard_desc"  to "Аз ресурс, көп тәуекел. Тәжірибелі ойыншылар үшін.",
    "ui_char_detail_diff_nm_desc"    to "Шектен тыс жағдайлар. Ең батылдар үшін.",

    // ── StatisticsScreen ─────────────────────────────────────────────
    "ui_stats_title"                 to "Статистика",
    "ui_stats_subtitle"              to "Сіздің қаржылық жетістіктеріңіз",
    "ui_stats_loading"               to "Жүктелуде...",
    "ui_stats_empty"                 to "Статистика бос",
    "ui_stats_empty_hint"            to "Кем дегенде бір ойын ойнаңыз...",
    "ui_stats_total_games"           to "Барлық ойындар",
    "ui_stats_completed"             to "Аяқталған",
    "ui_stats_avg_capital"           to "Орташа капитал",
    "ui_stats_best_ending"           to "Үздік соңы",
    "ui_stats_tab_endings"           to "📈 Соңдар",
    "ui_stats_tab_characters"        to "👥 Кейіпкерлер бойынша",
    "ui_stats_tab_eras"              to "🗺️ Дәуірлер бойынша",
    "ui_stats_games_avg"             to "ойын · Орт. капитал:",
    "ui_stats_games"                 to "ойын",

    // ── SettingsScreen ───────────────────────────────────────────────
    "ui_settings_title"              to "Баптаулар",
    "ui_settings_subtitle"           to "Қолданба тілі",
    "ui_settings_language"           to "Тіл",
    "ui_settings_language_subtitle"  to "Өзгеріс бірден қолданылады және құрылғыда сақталады.",
    "ui_settings_language_russian"   to "Русский",
    "ui_settings_language_kazakh"    to "Қазақша",
    "ui_settings_language_english"   to "English",

    // ── ChatScreen ───────────────────────────────────────────────────
    "ui_chat_reset_title"            to "Прогресті тастау керек пе?",
    "ui_chat_reset_message"          to "Бұл барлық чат тарихын өшіреді және кейіпкерді бастапқы күйге қайтарады.",
    "ui_chat_reset_confirm"          to "Тастау",
    "ui_chat_cancel"                 to "Бас тарту",
    "ui_chat_cd_home"                to "Басты мәзір",
    "ui_chat_cd_stats"               to "Қаржылық статистика",
    "ui_chat_cd_options"             to "Қосымша опциялар",
    "ui_chat_diary"                  to "Күнделік",
    "ui_chat_restart"                to "Ойынды қайта бастау",
    "ui_chat_player_prefix"          to "✍️  Мен шештім:",
    "ui_chat_action_label"           to "Мен не істеймін:",
    "ui_chat_writing"                to "күнделікке жазуда...",
    "ui_chat_monthly_report"         to "Ай нәтижелері",
    "ui_chat_scene_scam"             to "⚠️ Абай болыңыз",
    "ui_chat_scene_crisis"           to "📉 Дағдарыс",
    "ui_chat_scene_career"           to "💼 Мансап",
    "ui_chat_scene_family"           to "🏠 Отбасы",
    "ui_chat_scene_investment"       to "📈 Инвестиция",
    "ui_chat_scene_mortgage"         to "🔑 Ипотека",
    "ui_chat_scene_windfall"         to "🎉 Сәттілік",
    "ui_chat_scene_world"            to "🌙 Ой толғау",
    "ui_chat_restart_game"           to "🔄 Қайтадан бастау",
    // Short month names (Kazakh)
    "ui_chat_short_month_1"          to "Қаң",
    "ui_chat_short_month_2"          to "Ақп",
    "ui_chat_short_month_3"          to "Нау",
    "ui_chat_short_month_4"          to "Сәу",
    "ui_chat_short_month_5"          to "Мам",
    "ui_chat_short_month_6"          to "Мау",
    "ui_chat_short_month_7"          to "Шіл",
    "ui_chat_short_month_8"          to "Там",
    "ui_chat_short_month_9"          to "Қыр",
    "ui_chat_short_month_10"         to "Қаз",
    "ui_chat_short_month_11"         to "Қар",
    "ui_chat_short_month_12"         to "Жел",
    // Genitive month names (Kazakh)
    "ui_chat_month_gen_1"            to "қаңтарда",
    "ui_chat_month_gen_2"            to "ақпанда",
    "ui_chat_month_gen_3"            to "наурызда",
    "ui_chat_month_gen_4"            to "сәуірде",
    "ui_chat_month_gen_5"            to "мамырда",
    "ui_chat_month_gen_6"            to "маусымда",
    "ui_chat_month_gen_7"            to "шілдеде",
    "ui_chat_month_gen_8"            to "тамызда",
    "ui_chat_month_gen_9"            to "қыркүйекте",
    "ui_chat_month_gen_10"           to "қазанда",
    "ui_chat_month_gen_11"           to "қарашада",
    "ui_chat_month_gen_12"           to "желтоқсанда",

    // ── StatsPanelOverlay ────────────────────────────────────────────
    "ui_stats_panel_title"           to "Қаржы",
    "ui_stats_panel_freedom"         to "🎯 Қаржылық бостандық",
    "ui_stats_panel_start"           to "Бастама",
    "ui_stats_panel_freedom_label"   to "Бостандық",
    "ui_stats_panel_flow"            to "💸 Ақша ағыны",
    "ui_stats_panel_profit"          to "Пайда",
    "ui_stats_panel_deficit"         to "Тапшылық",
    "ui_stats_panel_capital"         to "💰 Капитал",
    "ui_stats_panel_debt"            to "💳 Қарыз",
    "ui_stats_panel_income"          to "📈 Табыс",
    "ui_stats_panel_expenses"        to "🛒 Шығыс",
    "ui_stats_panel_investments"     to "📊 Инвестиция",
    "ui_stats_panel_indicators"      to "Көрсеткіштер",
    "ui_stats_panel_stress"          to "😰 Стресс",
    "ui_stats_panel_knowledge"       to "📚 Қар. сауаттылық",
    "ui_stats_panel_risk"            to "🎲 Тәуекел деңгейі",
    "ui_stats_panel_per_month"       to "/ай",
    "ui_stats_panel_month_1"         to "Қаңтар",
    "ui_stats_panel_month_2"         to "Ақпан",
    "ui_stats_panel_month_3"         to "Наурыз",
    "ui_stats_panel_month_4"         to "Сәуір",
    "ui_stats_panel_month_5"         to "Мамыр",
    "ui_stats_panel_month_6"         to "Маусым",
    "ui_stats_panel_month_7"         to "Шілде",
    "ui_stats_panel_month_8"         to "Тамыз",
    "ui_stats_panel_month_9"         to "Қыркүйек",
    "ui_stats_panel_month_10"        to "Қазан",
    "ui_stats_panel_month_11"        to "Қараша",
    "ui_stats_panel_month_12"        to "Желтоқсан",

    // ── AppBar ───────────────────────────────────────────────────────
    "ui_appbar_back"                 to "Артқа",

    // ── Ending labels ────────────────────────────────────────────────
    "ending_bankruptcy"              to "💔 Банкроттық",
    "ending_paycheck"                to "😰 Жалақыдан жалақыға дейін",
    "ending_stability"               to "😊 Қаржылық тұрақтылық",
    "ending_freedom"                 to "🎯 Қаржылық бостандық!",
    "ending_wealth"                  to "🤑 Байлық",
    "ending_prison"                  to "⛓️ Түрме",
    "ending_game_over"               to "🏁 Ойын аяқталды",

    // ── Currency suffixes ────────────────────────────────────────────
    "currency_rub"                   to "руб.",

    // ── System / GameEngine ──────────────────────────────────────────
    "sys_game_start"                 to "🎮 Қаржылық шытырман оқиға басталды! %s-ға қаржылық болашақ құруға көмектесіңіз.",
    "sys_default_character_name"     to "Асан",

    // ── MonthlyReport ────────────────────────────────────────────────
    "sys_monthly_title"              to "📊 %s %d — Ай нәтижелері",
    "sys_monthly_income"             to "💰 Табыс:           +%s",
    "sys_monthly_expenses"           to "🏠 Шығыс:           -%s",
    "sys_monthly_debt_payment"       to "💳 Қарыз төлемі:    -%s",
    "sys_monthly_investments"        to "📈 Инвестиция:       +%s",
    "sys_monthly_net_positive"       to "✅ Барлығы: +%s",
    "sys_monthly_net_negative"       to "⚠️ Барлығы: %s",
    "sys_monthly_capital"            to "💼 Капитал: %s",
    "sys_monthly_debt_remaining"     to "💳 Қарыз: %s",

    // ── Month names ──────────────────────────────────────────────────
    "month_1"                        to "Қаңтар",
    "month_2"                        to "Ақпан",
    "month_3"                        to "Наурыз",
    "month_4"                        to "Сәуір",
    "month_5"                        to "Мамыр",
    "month_6"                        to "Маусым",
    "month_7"                        to "Шілде",
    "month_8"                        to "Тамыз",
    "month_9"                        to "Қыркүйек",
    "month_10"                       to "Қазан",
    "month_11"                       to "Қараша",
    "month_12"                       to "Желтоқсан",

    // ── Era names ────────────────────────────────────────────────────
    "era_modern_kz_2024_name"        to "Қазіргі Қазақстан",
    "era_kz_90s_name"                to "Қазақстан 90-шы жж.",
    "era_kz_2015_name"               to "Қазақстан 2015",

    // ── Auth errors ──────────────────────────────────────────────────
    "err_auth_fill_fields"           to "Барлық өрістерді толтырыңыз",
    "err_auth_server_unavailable"    to "Сервер қолжетімсіз (%d)",
    "err_auth_login_too_short"       to "Логин кемінде 3 таңба",
    "err_auth_password_too_short"    to "Құпия сөз кемінде 6 таңба",

    // Game content strings intentionally absent — fallback to ruStrings.
    // TODO i18n: translate evt_*, scam_*, era event messages to Kazakh.
)
