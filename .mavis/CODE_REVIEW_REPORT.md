# FinancialLifeV2 — 综合代码审查报告

**审查日期:** 2026-06-11
**审查范围:** Admin Session Auth（未提交） + GameEngine FSM + 整体架构

---

## 执行摘要

| 区域 | 状态 | 严重问题数 |
|------|------|-----------|
| Admin Session Auth | ⚠️ 安全通过，编译失败 | 1 🔴 编译阻止 |
| GameEngine FSM | ❌ 多处严重问题 | 2 CRITICAL + 2 HIGH |
| 整体架构 | ✅ 整体健康 | 1 HIGH + 1 MEDIUM |

**总体评估:** 项目架构设计优秀，核心引擎设计合理，但 GameEngine 中存在需要紧急修复的逻辑 bug，Admin auth 代码因依赖问题无法编译。

---

## 一、Admin Session Auth 审查

### 1.1 编译状态 — 🔴 阻塞

```
./gradlew :server:test
FAILURE: Could not find io.ktor:ktor-server-static-content-jvm:3.4.2
```

**根因:** `libs.versions.toml:73` 声明了不存在的 artifact。Ktor 3.x 的 `staticResources()` 功能已包含在 `ktor-server-core-jvm` 中，无需单独依赖。

**修复:** 删除 `libs.versions.toml` 中的 `ktor-server-static-content-jvm` 及 `server/build.gradle.kts` 中的引用。

### 1.2 安全评估 — ✅ 通过

| 检查项 | 结果 | 说明 |
|--------|------|------|
| HMAC-SHA256 cookie 签名 | ✅ | Ktor SessionTransportTransformerMessageAuthentication 实现正确 |
| 用户名/密码常量时间比较 | ✅ | `MessageDigest.isEqual` 防 timing attack |
| ADMIN_KEY Bearer 常量时间比较 | ✅ | 同上 |
| HttpOnly cookie | ✅ | XSS 无法读取 session |
| Secure flag 默认 false | ⚠️ | 需在 HTTPS 生产环境设置 `SESSION_SECURE=true` |
| SameSite=Lax | ⚠️ | 不可配置，部分场景可能需要 `SameSite=None` |
| SESSION_SECRET fallback | ⚠️ | 有 warning log，生产必须设置 |
| ADMIN_KEY/ADMIN_PASSWORD fallback | ⚠️ | 与 JWT dev default 模式一致 |
| isAdminAuthorized() 覆盖 18 个端点 | ✅ | 全部支持 session cookie + Bearer token 双机制 |
| configureAdminSession() 正确先于 configureSecurity() | ✅ | 顺序正确 |
| 测试覆盖 | ✅ | AdminAuthRoutesTest + AdminUserRoutesTest |

### 1.3 Admin Auth 修复优先级

1. **立即修复** — 删除 `ktor-server-static-content-jvm` 依赖（阻塞所有 server 编译）
2. **建议修复** — 生产部署时添加 `SESSION_SECRET`/`ADMIN_KEY`/`ADMIN_PASSWORD` 环境变量检查fail-fast
3. **建议增强** — `POST /admin/login` 添加 RateLimit plugin 防暴力破解

---

## 二、GameEngine FSM 审查

### 2.1 🔴 CRITICAL: 非唯一条件事件无限重注入

**位置:** `EraCharacterArcs.kt:719` + `GameEngine.kt:308–313`

所有非 unique 的条件事件（如 `ending_regular_trigger`、`ending_wealth_trigger` 等）一旦满足条件，会在**每个月**持续触发，因为：
- `unique = false` → 从不加入 `triggeredUniqueEvents`
- `findConditionalEvent()` 每次都通过过滤
- 条件持续满足 → 无限循环

**修复:** 将所有条件性结局触发器标记为 `unique = true`。

### 2.2 🔴 CRITICAL: 投资回报截断至零

**位置:** `Models.kt:243–244`

```kotlin
val monthlyInvestmentReturn: Long = (investments * investmentReturnRate / 12).toLong()
```

当投资额 < 125 KZT 时，月回报为 0。长期游戏中小额投资者完全看不到收益增长。

**修复:** 使用 `BigDecimal` 计算或累积小数部分。

### 2.3 🟡 HIGH: 破产结局 stress 门槛不可达

**位置:** `EraCharacterArcs.kt` — `ending_bankruptcy_trigger`

条件要求 `capital ≤ 0` + `stress ≥ 85`，但 `capital` 在 `monthlyTick` 中已被 clamp 到 0。若玩家此时 stress < 85，破产结局永远无法触发，游戏继续以 `capital = 0` 运行。

**修复:** 移除 `stress ≥ 85` 条件，或添加独立 `capital ≤ 0` 触发器。

### 2.4 🟡 HIGH: 结局触发器优先级排序脆弱

`ending_regular_trigger`（priority 80）无其他条件，只要求 `arc.final_check`。结合 BUG-1，进入 `final_review` 但不满足任何其他条件的玩家会陷入 `ending_regular_trigger` 无限循环。

**修复:** 同 BUG-1 修复（标记 `unique = true`）。

### 2.5 🟡 MEDIUM: moneyFormat 显示截断

```kotlin
this >= 1_000_000L -> "${this / 1_000_000L}M"  // 1.8M → "1M" (44%误差!)
this >= 1_000L     -> "${this / 1_000L}k"        // 950K → "950k"
```

玩家看到错误的资本数字，无法做出正确决策。

**修复:** 使用 locale-aware 千分位格式化。

### 2.6 ⚠️ WARNING: 债务月付款更新延迟问题

**位置:** `GameEngine.kt:227`

`principalPaid = (ps.debtPaymentMonthly * 0.30).toLong()` — 使用上个月旧值。如果事件效果改变了 `debtPaymentMonthly`，本月 principal reduction 仍基于旧值。

**严重性:** 低 — 延迟一个 tick，错误量级小。

### 2.7 ⚠️ WARNING: Aidar90s 货币符号硬编码错误

`Aidar90sScenarioGraph` 是 1993 年 RUB 时代，但 intro 文本中硬编码了 `₸`（KZT 符号）。应该用 `{currency}` token 或 `CurrencyCode.RUB`。

### 2.8 ⚠️ WARNING: EventPoolSelector 普通事件处理不一致

`normal_life` 直接在 `storyEventPool()` 中 add，不经过 `ScamEventLibrary.poolEntries` 列表。设计上与其他 pool entry 不一致，易在重构时破坏。

### 2.9 ✅ GameEngine 正确的部分

- FSM 3层架构设计优秀
- 4层事件优先级队列逻辑正确
- 经济模拟公式正确（收入−支出−债务+投资回报）
- 5种结局类型与叙事事件映射正确
- Pool weight modifiers（era + knowledge）逻辑正确
- 状态不可变性（所有 PlayerState 变化创建新副本）

---

## 三、整体架构审查

### 3.1 模块结构 — ✅ 优秀

5个模块边界清晰，shared 作为 KMP 核心被 composeApp + server + admin 正确依赖。landing 独立构建（无 shared 依赖）合理。

### 3.2 i18n 实现 — ⚠️ 有问题

**🔴 HIGH: `Strings.currentLocale` 可变单例无线程安全**

```kotlin
object Strings {
    var currentLocale: String = "ru"  // 直接修改，无锁保护
}
```

任何 Composable 可直接修改，绕过 `LocaleRepository`。语言切换依赖 `key()` 重组整个 UI 树实现，非真正的响应式更新。

**建议:** 改用 `StateFlow<String>` + `collectAsState()`。

**⚠️ MEDIUM: StringKeys enum 未用于类型安全的字符串获取**

`operator fun get(key: String)` 接受任意字符串，编译期无检查。StringKeys 定义存在但未被利用。

### 3.3 数据模型 — ✅ 优秀

- `Effect` Delta 模式避免直接修改，保证不可变性
- PlayerState 计算属性分离（`absoluteMonth`、`monthlyInvestmentReturn` 等）
- `flags` Map 用于灵活叙事状态
- `eventCooldowns` Map 防止事件重复触发

**⚠️ MEDIUM: `GameEnding`(6种) vs `EndingType`(5种) 定义重复**

`GameEnding.PRISON` 在服务端统计中存在，但 `EndingType` 中没有对应项。需统一。

### 3.4 客户端架构 — ✅ 优秀

- Presenter 模式正确（无 ViewModel），100% CMP 兼容
- 7个 Presenter 全部使用 `MutableStateFlow + CoroutineScope + collectAsStateWithLifecycle`
- Koin DI 模块分离正确（common/android/ios）
- Token 自动刷新机制已实现

**⚠️ MEDIUM: 手动 back-stack 不支持系统返回键**

`AppNavigation.kt` 需补充 `BackHandler` 处理。

### 3.5 Server 架构 — ✅ 优秀

- JWT 实现为 HMAC-SHA256（非文档所述 RS256，但**当前设计更适合**单服务端+移动客户端场景）
- Access Token(15min) + Refresh Token(30天，UUID，DB存储，一次性轮换) 分层合理
- Admin 双认证机制（cookie session + Bearer key）设计优秀
- Exposed ORM + Repository 接口分离
- Migration 系统幂等可靠
- ChoiceLockManager（ConcurrentHashMap + Mutex）设计优秀

### 3.6 JWT 说明

文档提到 "JWT(RS256)" 但实际实现为 HMAC-SHA256。这是**设计选择上的差异，非缺陷** — HMAC-SHA256 对单服务端+受信任移动客户端场景完全足够，RS256 的额外密钥管理复杂度在当前场景不必要。

---

## 四、修复优先级总览

### 必须立即修复（阻塞发布）

| # | 问题 | 区域 | 修复方式 |
|---|------|------|---------|
| 1 | `ktor-server-static-content-jvm` 依赖不存在 | Admin Auth | 删除依赖行 |
| 2 | 非 unique 条件事件无限重注入 | GameEngine | 所有条件性结局触发器标记 `unique = true` |
| 3 | 投资回报对小额投资截断为 0 | GameEngine | BigDecimal 或累积小数 |

### 高优先级（下次发布前修复）

| # | 问题 | 区域 | 修复方式 |
|---|------|------|---------|
| 4 | 破产结局 stress ≥ 85 门槛不可达 | GameEngine | 移除 stress 条件或独立触发器 |
| 5 | `ending_regular_trigger` 无限循环 | GameEngine | 同 #2 |
| 6 | `Strings.currentLocale` 可变单例 | i18n | StateFlow + collectAsState |
| 7 | `GameEnding` vs `EndingType` 不一致 | 模型 | 统一为一个 enum |

### 中优先级（计划中修复）

| # | 问题 | 区域 | 修复方式 |
|---|------|------|---------|
| 8 | moneyFormat 44% 截断误差 | GameEngine | locale-aware 千分位格式化 |
| 9 | 手动 back-stack 无系统返回键处理 | 客户端 | 添加 BackHandler |
| 10 | Aidar90s 硬编码 ₸ 符号 | 场景 | 使用 CurrencyCode.RUB |
| 11 | landing 无 shared 依赖（DTO 重复风险） | 模块 | 评估是否需要 |

---

## 五、AdminPanelImpl.md Step 2 状态评估

当前 3 个未提交文件：

| 文件 | 状态 | 说明 |
|------|------|------|
| `AdminSession.kt` | ✅ 设计正确 | HMAC cookie session 实现优秀 |
| `AdminAuthRoutes.kt` | ✅ 设计正确 | 常量时间比较，测试覆盖完整 |
| `AdminRoutes.kt` | ✅ 设计正确 | isAdminAuthorized() 覆盖 18 端点 |

**Step 2 剩余完成条件：**
- [ ] `configureAdminSession()` 已在 `Application.module()` wire ✅（代码已确认存在）
- [ ] `adminAuthRoutes()` 已在 `Routing.kt` mount ✅（代码已确认存在）
- [ ] **删除 `ktor-server-static-content-jvm` 依赖**（阻塞编译）

---

*综合报告由 Mavis 团队审查生成 — 2026-06-11*