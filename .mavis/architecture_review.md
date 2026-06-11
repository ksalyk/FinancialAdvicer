# FinancialLifeV2 — Architecture Review

**Date:** 2026-06-11
**Reviewer:** Architecture审查专家
**Project:** `/Users/kuanyshsalyk/AndroidStudioProjects/FinancialLifeV2/`

---

## 1. 模块结构（Module Structure）

### 1.1 整体模块布局

| 模块 | 类型 | 依赖关系 | 入口 |
|------|------|---------|------|
| `shared/` | KMP (commonMain) | — | engine/models/scenarios/i18n |
| `composeApp/` | CMP (Android+iOS) | → shared | App.kt |
| `server/` | Ktor JVM | → shared | Application.kt |
| `landing/` | KMP WASM | 独立 | Main.kt |
| `admin/` | KMP WASM | → shared | Main.kt |

### 1.2 模块边界评估

**shared/ — 清晰，职责单一**
- `engine/` — 游戏FSM核心（490行），3层架构（叙事图+状态系统+经济模拟）
- `model/` — `GameModels.kt`(241行) + `Models.kt`(341行) 分层，边界清晰
- `scenarios/` — 叙事DSL，角色scenario图工厂（locale缓存）
- `i18n/` — 翻译资源与字符串键分离
- `admin/` — Admin DTO（轻量，仅用于shared→admin的API契约）

**composeApp/ — 边界清晰**
- `ui/navigation/AppNavigation.kt` — 唯一导航入口，手动back-stack
- `presentation/` — 7个Presenter（非ViewModel），生命周期绑定到Composable composition
- `data/` — Repository层（AuthRepository, GameSessionRepository, LocaleRepository, FeatureFlagRepository）
- `di/` — Koin模块分离（commonModule / androidModule / iosModule）

**server/ — 分层清晰**
```
Application.kt
  ├── plugins/        (Security, Routing, CORS, Logging, Serialization, RateLimiter, StatusPages, AdminSession, SecurityHeaders)
  ├── database/       (DatabaseFactory, tables/, migrations/)
  ├── repository/     (Interface + Exposed实现)
  └── routes/        (Auth, Game, AdminAuth, Admin, AdminUser, AdminScenario)
```

**landing/ — 独立，无shared依赖**，这意味着landing页面无法复用shared中的DTO和模型。如果landing需要与server通信，需自行定义契约。建议：如果landing未来需要与server交互，应考虑引入shared依赖。

**admin/ — 依赖shared**，消费`kz.fearsom.financiallifev2.admin`包的DTO（AdminUserRow, ScenarioComboDto等），架构正确。

### 1.3 模块边界问题

| 问题 | 严重性 | 描述 |
|------|--------|------|
| landing/ 无 shared 依赖 | 低 | landing页面无法复用shared的模型，若需server通信需自行定义DTO |
| shared/admin 包存放Admin DTO | 低 | Admin DTO放在shared中是正确的设计（server→admin共享契约） |
| landing和admin都是WASM目标但独立构建 | 低 | 正确分离，关注点不同 |

---

## 2. i18n 实现（Internationalization）

### 2.1 文件结构

```
shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/i18n/
├── StringKeys.kt          (278行, ~200个常量)
├── Strings.kt             (485行, 核心运行时)
└── translations/
    ├── en.kt              (18K)   — UI字符串
    ├── en_content.kt      (138K)  — 游戏内容字符串
    ├── en_hardcoded.kt    (33K)   — 硬编码/遗留字符串
    ├── kk.kt              (21K)   — UI字符串
    ├── kk_content.kt      (207K)  — 游戏内容字符串 ✓
    ├── kk_hardcoded.kt    (51K)   — 硬编码/遗留字符串
    ├── ru.kt              (224K)   — 俄语：UI+内容合并
    └── ru_hardcoded.kt    (49K)   — 硬编码/遗留字符串
```

### 2.2 StringKeys 与翻译文件一致性

StringKeys enum定义了~200个字符串键，分布在以下类别：
- Login/MainMenu/Era/CharacterSelection/Characters/CharacterDetail/Statistics/Settings/ChatScreen/StatsPanelOverlay/Ending/Currency/System/MonthlyReport/Month/Era/AuthError

**一致性状态：** ✅ 基本一致
- 所有UI键都在StringKeys中有对应常量
- 俄语(ru)是最完整的源语言（224K），包含UI+游戏内容合并
- 哈萨克语(kk)有完整三段结构：kk.kt + kk_content.kt(207K) + kk_hardcoded.kt — 与英语(en + en_content + en_hardcoded)模式完全一致
- 英语相对较小（en.kt 18K + en_content 138K），游戏内容翻译相对完整

### 2.3 反应式语言切换 — 严重问题 🔴

`Strings.kt`中的实现：

```kotlin
object Strings {
    var currentLocale: String = "ru"  // 可变单例

    private val maps: Map<String, Map<String, String>> = mapOf(
        "ru" to ruStrings,
        "kk" to kkStrings,
        "en" to enStrings
    )

    operator fun get(key: String): String =
        maps[currentLocale]?.get(key)
            ?: maps["ru"]?.get(key)
            ?: key
}
```

**问题1：可变全局状态**
- `currentLocale`是`object Strings`的`var`字段，是可变单例
- 任何Composable都可以直接修改`Strings.currentLocale`，绕过了`LocaleRepository`
- 没有线程安全保护（Kotlin的`var`对多线程没有原子性保证）

**问题2：语言切换非热更新**
- 代码注释明确说明："Set once at app init via AppModule"
- 实际通过`LocaleRepository.setLocale()`写入`Strings.currentLocale`
- UI响应靠`LaunchedEffect(settingsUiState.currentLocale)`驱动各Presenter的`refreshLocalizedData()`
- 但`Strings.get()`本身是同步的、直接的，没有任何订阅机制
- 如果某处直接调用`Strings["key"]`而不是通过Presenter的UIState，在语言切换后不会自动更新

**问题3：类型不安全**
- `operator fun get(key: String)`接受任意字符串键
- 编译期无检查，错误的key会静默返回fallback（ru或raw key）
- StringKeys enum定义存在但未被用于类型安全的字符串获取

**建议改进方向：**
1. 将`Strings`从`object`改为`interface`，具体实现注入DI
2. 或者保留object但使用`StateFlow<String>`作为currentLocale，Composable使用`collectAsState()`
3. 使用`@Composable inline fun`提供类型安全的字符串获取（泛型擦除但IDE可检查）

### 2.4 LocaleRepository 语言切换流程

```
SettingsPresenter.selectLocale(locale)
  → LocaleRepository.setLocale(normalizedLocale)
      → Strings.currentLocale = normalizedLocale  (直接写入可变单例)
      → SecureStorage.save("selected_locale", normalizedLocale)
  → settingsUiState.currentLocale 更新
  → LaunchedEffect(settingsUiState.currentLocale)
      → 各Presenter.refreshLocalizedData()
```

流程正确，但底层依赖可变单例。

---

## 3. 数据模型（Data Models）

### 3.1 模型分层

| 文件 | 内容 | 行数 |
|------|------|------|
| `shared/model/GameModels.kt` | Era, CharacterStats, Difficulty, GameEnding, PredefinedCharacter, CharacterBundle, GameSession, 统计模型 | 241 |
| `shared/model/Models.kt` | Effect, PlayerState, GameState, GameEvent, GameOption, ChatMessage, MonthlyReport, Auth模型 | 341 |
| `shared/model/Extensions.kt` | `moneyFormat()`扩展 | 18 |

### 3.2 PlayerState 结构

```kotlin
data class PlayerState(
    // 货币层
    capital: Long, income: Long, expenses: Long, debt: Long,
    debtPaymentMonthly: Long, investments: Long, investmentReturnRate: Double,
    // 软属性
    stress: Int, financialKnowledge: Int, riskLevel: Int,  // 0-100
    // 时间
    month: Int, year: Int,
    // 身份
    characterId: String, eraId: String, currency: CurrencyCode,
    // 事件追踪
    flags: Map<String, Boolean>,
    triggeredUniqueEvents: Set<String>,
    pendingScheduled: List<ScheduledEvent>,
    eventCooldowns: Map<String, Long>,
    // 计算属性（computed properties，不是data class字段）
    monthLabel, netMonthlyFlow, monthlyInvestmentReturn,
    netWorth, absoluteMonth
)
```

**设计评估：** ✅ 良好
- 职责分离清晰：货币层/软属性/时间/身份/事件追踪
- 计算属性作为computed properties（不占data class空间）
- `flags` Map用于灵活的叙事状态标记
- `eventCooldowns` Map用于防止事件重复触发

### 3.3 结局定义 — 与任务描述不符 ⚠️

任务描述称"5种结局"，实际代码有**6种**：

```kotlin
enum class GameEnding {
    BANKRUPTCY,           // 破产
    PAYCHECK_TO_PAYCHECK,  // 月光
    FINANCIAL_STABILITY,   // 财务稳定
    FINANCIAL_FREEDOM,     // 财务自由
    WEALTH,               // 富裕
    PRISON                // 监狱（经济犯罪）
}
```

同时`EndingType`在Models.kt中定义了5种（BANKRUPTCY, PAYCHECK_TO_PAYCHECK, FINANCIAL_STABILITY, FINANCIAL_FREEDOM, WEALTH），**没有PRISON**。这是两个不同enum的重叠定义，`GameEnding`用于服务端统计，`EndingType`用于客户端叙事。需要确认是否存在PRISON作为可触发的结局。

### 3.4 Effect 模型 — Delta 设计

```kotlin
data class Effect(
    val capital: Long? = null,
    val income: Long? = null,
    val expenses: Long? = null,
    val debt: Long? = null,
    val investments: Long? = null,
    val stress: Int? = null,
    val knowledge: Int? = null,
    val risk: Int? = null,
    val flag: String? = null,
    val flagValue: Boolean? = null,
    val scheduledEvent: ScheduledEvent? = null,
    val monetaryReform: MonetaryReform? = null
)
```

**设计评估：** ✅ Delta模式优秀
- 所有字段可null，表示"不修改"
- 避免了对PlayerState的直接修改，保证不可变性
- 支持复合效果（一次选择可触发多个状态变化）

---

## 4. 客户端架构（Client Architecture）

### 4.1 导航 — AppNavigation.kt 手动 Back-Stack

```kotlin
var backStack  by remember { mutableStateOf(listOf<AppScreen>(AppScreen.Splash)) }
var navForward by remember { mutableStateOf(true) }

// 深度驱动动画方向
private fun AppScreen.depth(): Int = when (...) { ... }
```

**评估：** ✅ 简洁有效
- 深度系统驱动slide动画方向（前向/后向）
- `AnimatedContent` + `slideInHorizontally/slideOutHorizontally` 实现流畅过渡
- 没有使用Compose Navigation库，手动实现保持轻量
- `key(settingsUiState.currentLocale)` 重组整个UI树用于语言切换刷新

**潜在问题：**
- 手动back-stack不支持系统back键自动处理（需要额外处理`BackHandler`或`onBackPressedDispatcher`）
- 没有深层链接支持（如`financelifeline://game/session123`）
- back-stack状态不持久化（进程杀死后丢失）

### 4.2 Presenter 模式（无 ViewModel）

**评估：** ✅ 架构正确

```kotlin
class AuthPresenter(
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        scope.launch { authRepository.authState.collect { ... } }
    }
}
```

- 所有7个Presenter使用相同模式：`MutableStateFlow + CoroutineScope + collectAsStateWithLifecycle`
- 生命周期绑定到Composable composition（`remember { Presenter(...) }`）
- **零 ViewModel 使用** — 完全兼容iOS CMP
- `collectAsStateWithLifecycle` 确保在Android lifecycle-aware场景正确挂起/恢复

### 4.3 Koin DI 配置

| 模块 | 内容 |
|------|------|
| `commonModule` | LocaleRepository, TokenStorage, HttpClient, AuthRepository, GameEngine, GameSessionRepository, FeatureFlagRepository, GameApiService |
| `androidModule` | SecureStorage(androidContext) |
| `iosModule` | SecureStorage() |

**评估：** ✅ 平台分离正确
- `commonModule`提供跨平台依赖
- 平台特定模块（android/ios）仅提供平台特定实现（SecureStorage）
- Koin初始化在MainActivity/MainViewController入口完成

### 4.4 HttpClient 与 Token 刷新

```kotlin
buildHttpClient(
    tokenManager = TokenStorage,
    onTokenRefreshed = { newToken -> ... },
    onRefreshFailed = { logout() }
)
```

Token自动刷新机制已实现。

---

## 5. Server 架构（Server Architecture）

### 5.1 JWT — 与任务描述不符 ⚠️

**任务描述称"JWT RS256"，实际为 HMAC-SHA256 🔴**

```kotlin
// JwtConfig.kt
val secret: String get() = System.getenv("JWT_SECRET") ?: "dev-secret-change-in-production"

// AuthRoutes.kt
.sign(Algorithm.HMAC256(JwtConfig.secret))
```

| 对比 | RS256 (非对称) | HMAC-SHA256 (对称) — 当前实现 |
|------|---------------|------------------------------|
| 密钥 | 公钥+私钥对 | 单一共享密钥 |
| 验证方 | 不需要密钥 | 需要知道secret |
| 适用场景 | 跨服务/跨平台 | 单服务+受信任客户端 |
| 密钥轮换 | 可单独更换公钥 | 需重新分发secret |
| 当前实现 | ❌ | ✅ |

**当前设计是合适的** — HMAC-SHA256对于单服务端+移动客户端场景完全足够。RS256的额外复杂度（密钥管理、轮换）在没有跨服务验证需求时不必要。

### 5.2 Token 设计

| Token | 生命周期 | 存储 | 轮换 |
|-------|---------|------|------|
| Access Token (JWT) | 15分钟 | 客户端内存 | 自动 |
| Refresh Token (UUID) | 30天 | PostgreSQL `refresh_tokens`表 | 每次使用后轮换 |

**评估：** ✅ 行业标准
- One-time refresh token rotation防止replay攻击
- Logout撤销所有用户refresh tokens（所有设备）
- 正确的token分层

### 5.3 Admin 认证 — 双机制

| 场景 | 机制 |
|------|------|
| 浏览器SPA (admin/) | httpOnly cookie session (HMAC-SHA256签名) |
| 程序化API客户端 | `Authorization: Bearer <ADMIN_KEY>` (常量时间比较) |

**评估：** ✅ 分层合理
- Admin SPA用session cookie避免XSS token泄露
- API客户端用静态key适合运维集成
- `ADMIN_KEY`环境变量默认值`"dev-admin-key"` — **生产必须覆盖**

### 5.4 Exposed ORM 使用

8张表：users, refresh_tokens, game_sessions, game_state_snapshots, completed_sessions, characters, eras, schema_versions

**评估：** ✅ 架构良好
- Repository接口+Exposed实现分离
- JSON列存储复杂对象（eraIds, stateJson）利用PostgreSQL JSONB
- 适当的索引（userId, ending, composite index）

### 5.5 版本化 Migrations

```kotlin
interface Migration {
    val version: Int
    val description: String
    fun up(db: Database)
    fun down(db: Database)
}

// MigrationRunner — 顺序执行，跳过已成功记录
// schema_versions — 追加式记录
```

**评估：** ✅ 可靠
- V001 (InitialSchema), V002 (AddStatisticsIndex) 已实施
- `IF NOT EXISTS` / `CREATE TABLE IF NOT EXISTS` 保证幂等性
- 失败停止，无自动回滚（需手动调用rollback()）

### 5.6 ChoiceLockManager — 并发安全

```kotlin
// ConcurrentHashMap<String, Entry(Mutex, lastAccessed)>
// 15分钟idle超时 + 5分钟惰性修剪
// 确保 load → process → save 原子化
```

**评估：** ✅ 优秀
- 无锁设计 + 细粒度Mutex per user
- 内存bounded by active users (非all-time)
- 惰性修剪，无额外GC线程

---

## 6. 总结与建议

### 6.1 架构优势

| 方面 | 评分 | 说明 |
|------|------|------|
| 模块边界 | ⭐⭐⭐⭐⭐ | 5模块职责清晰，shared作为KMP核心 |
| i18n覆盖 | ⭐⭐⭐⭐ | ru/kk/en完整，kk_content已实现 |
| 数据模型 | ⭐⭐⭐⭐⭐ | Delta effect模式，计算属性分离 |
| 客户端架构 | ⭐⭐⭐⭐⭐ | Presenter+StateFlow，无ViewModel，全CMP兼容 |
| Server架构 | ⭐⭐⭐⭐⭐ | JWT+Refresh分离，Exposed分层，migration可靠 |
| 并发安全 | ⭐⭐⭐⭐⭐ | ChoiceLockManager设计优秀 |

### 6.2 待修复问题

| # | 问题 | 严重性 | 建议 |
|---|------|--------|------|
| 1 | `Strings.currentLocale`可变单例无线程安全 | 🔴 高 | 改用`StateFlow<String>`，Composable端`collectAsState()` |
| 2 | 语言切换非热更新，需依赖`key()`重组UI树 | 🟡 中 | 研究Compose对StringMap变化的响应机制 |
| 3 | landing/无shared依赖，DTO重复风险 | 🟡 中 | 若landing需server通信，考虑引入shared |
| 4 | `GameEnding`(6种) vs `EndingType`(5种) 定义重复 | 🟡 中 | 统一为一个enum，消除二义性 |
| 5 | 手动back-stack不支持系统back键 | 🟡 中 | 补充`BackHandler`或`onBackPressedDispatcher`处理 |
| 6 | JWT dev fallback secret | 🟡 中 | 生产确保JWT_SECRET env var设置 |

### 6.3 架构健康度

```
模块结构      ████████████ 100%
i18n一致性    █████████░░░  85%
数据模型设计  ████████████ 100%
客户端架构    ████████████ 100%
Server架构   ████████████ 100%
整体评分      ██████████░░  95%
```