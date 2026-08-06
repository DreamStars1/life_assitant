# Health Meal Kcal + Daily Burn Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为健康管家增加每餐结构化 `kcal`、日况 `burn_kcal` 覆盖（默认档案静息），并在今日概览与趋势「平均摄入」展示派生总量。

**Architecture:** Flyway 给 `health_meal` / `health_daily` 加列 → 纯函数 `HealthEnergyRules` 管校验与聚合 → Service 接线 meal/daily/summary → MCP 透传字段 → 前端今日能量行 + 餐卡/表单 + 趋势接线 → 更新 Skill。

**Tech Stack:** Java 17 / Spring Boot / MyBatis-Plus / Flyway；Vue 3 + Vant；Python FastMCP；JUnit 5；unittest。

**Spec:** `docs/superpowers/specs/2026-08-06-health-meal-kcal-design.md`

## Global Constraints

- 不新建日消耗表；能量覆盖挂在 `health_daily`
- 日总摄入不落库，读时 `SUM`；部分餐未填只加已填，不提示
- 今日消耗 = `burn_kcal ?? resting_kcal`
- `kcal` / `burnKcal` / `restingKcal`：非负整数，`> 20000` 拒写
- 日况清空 `burnKcal`：JSON **含键**才写（`burnKcalPresent`），对齐 `targetKg`
- 餐次 `kcal` 写入对齐 `proteinG`（整餐 upsert，空 → `null`）
- 不解析/迁移 `feedback` 里的热量文案
- 不引入新依赖；PowerShell：`git commit -m "msg"`；路径双引号
- 范围外：体重明细筛选、「最舒适」、自动估热

## Agent 并行建议

| 波次 | 可并行 Task | 依赖 |
|------|-------------|------|
| A | Task 1 | 无 |
| B | Task 2 | 无（可与 Task 1 并行） |
| C | Task 3 | Task 1、2 |
| D | Task 4 + Task 5 | Task 3（接口字段稳定） |
| E | Task 6 | Task 4 |

---

## 文件结构

```
backend/.../db/migration/
  V17__health_meal_kcal_and_daily_burn.sql

backend/.../health/
  model/entity/HealthMealDO.java          (+kcal)
  model/entity/HealthDailyDO.java         (+burnKcal)
  model/req/HealthMealUpsertReq.java      (+kcal)
  model/req/HealthDailyUpsertReq.java     (+burnKcal + present 反序列化)
  model/resp/HealthMealResp.java          (+kcal)
  model/resp/HealthDailyResp.java         (+burnKcal)
  model/resp/HealthSummaryResp.java       (+avg30IntakeKcal)
  service/HealthEnergyRules.java          (新建：校验 + 日均摄入)
  service/HealthService.java              (接线)
  # test
  .../src/test/java/.../health/service/HealthEnergyRulesTest.java

front/.../api/modules/health.ts           (类型)
front/.../pages/health/index.vue          (UI)

python/agent/src/.../tools/health.py
python/agent/tests/test_domain_dispatch.py

.cursor/skills/health-butler-mcp/SKILL.md
```

---

### Task 1: Flyway V17 + Entity 字段

**Files:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V17__health_meal_kcal_and_daily_burn.sql`
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/health/model/entity/HealthMealDO.java`
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/health/model/entity/HealthDailyDO.java`

**Interfaces:**
- Produces: DB 列 `health_meal.kcal`、`health_daily.burn_kcal`；DO 属性 `kcal: Integer`、`burnKcal: Integer`
- Consumes: 无

- [ ] **Step 1: 写迁移**

```sql
-- V17__health_meal_kcal_and_daily_burn.sql
ALTER TABLE health_meal
    ADD COLUMN kcal INT NULL COMMENT '该餐热量 kcal' AFTER protein_g;

ALTER TABLE health_daily
    ADD COLUMN burn_kcal INT NULL COMMENT '当日消耗覆盖；NULL=回退静息' AFTER cycle_day;
```

- [ ] **Step 2: 更新 HealthMealDO**

在 `proteinG` 后增加：

```java
@TableField(value = "kcal", updateStrategy = FieldStrategy.ALWAYS)
private Integer kcal;
```

`import com.baomidou.mybatisplus.annotation.FieldStrategy;`
（`ALWAYS`：允许 upsert 把 `kcal` 写成 `null` 以清除）

- [ ] **Step 3: 更新 HealthDailyDO**

在 `cycleDay` 后增加：

```java
@TableField(value = "burn_kcal", updateStrategy = FieldStrategy.ALWAYS)
private Integer burnKcal;
```

- [ ] **Step 4: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V17__health_meal_kcal_and_daily_burn.sql" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/health/model/entity/HealthMealDO.java" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/health/model/entity/HealthDailyDO.java"
git commit -m "feat(health): add meal kcal and daily burn_kcal columns"
```

---

### Task 2: HealthEnergyRules（TDD）

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/health/service/HealthEnergyRules.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/test/java/top/lifeassistant/health/service/HealthEnergyRulesTest.java`

**Interfaces:**
- Produces:
  - `HealthEnergyRules.validateKcal(Integer kcal, String fieldLabel)` — null 通过；`<0` 或 `>20000` 抛 `BusinessException`
  - `HealthEnergyRules.averageDailyIntakeKcal(List<HealthMealDO> mealsWithKcal)` — 入参已过滤非空 kcal；按日加总再平均；空列表 → null；返回 `BigDecimal` scale 0 HALF_UP
  - `HealthEnergyRules.effectiveBurnKcal(Integer burnKcal, Integer restingKcal)` — `burn ?? resting`（可为 null）
- Consumes: `HealthMealDO.getMealDate()` / `getKcal()`；`top.continew.starter.core.exception.BusinessException`（与现有 health 一致；若工程实际 import 不同则以 `HealthService` 现用类为准）

- [ ] **Step 1: 写失败测试**

```java
package top.lifeassistant.health.service;

import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BusinessException;
import top.lifeassistant.health.model.entity.HealthMealDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HealthEnergyRulesTest {

    @Test
    void validateKcal_nullOk() {
        assertDoesNotThrow(() -> HealthEnergyRules.validateKcal(null, "kcal"));
    }

    @Test
    void validateKcal_rejectsNegative() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> HealthEnergyRules.validateKcal(-1, "kcal"));
        assertTrue(ex.getMessage().contains("kcal"));
    }

    @Test
    void validateKcal_rejectsOver20000() {
        assertThrows(BusinessException.class,
            () -> HealthEnergyRules.validateKcal(20001, "burnKcal"));
    }

    @Test
    void averageDailyIntake_partialMealsAndSkipEmptyDays() {
        // day1: 150+200=350; day2: 100; day3 has meal but no kcal → not in list
        HealthMealDO a = meal(LocalDate.of(2026, 8, 1), 150);
        HealthMealDO b = meal(LocalDate.of(2026, 8, 1), 200);
        HealthMealDO c = meal(LocalDate.of(2026, 8, 2), 100);
        BigDecimal avg = HealthEnergyRules.averageDailyIntakeKcal(List.of(a, b, c));
        // (350+100)/2 = 225
        assertEquals(0, new BigDecimal("225").compareTo(avg));
    }

    @Test
    void averageDailyIntake_emptyNull() {
        assertNull(HealthEnergyRules.averageDailyIntakeKcal(List.of()));
    }

    @Test
    void effectiveBurn_prefersOverride() {
        assertEquals(1800, HealthEnergyRules.effectiveBurnKcal(1800, 1400));
        assertEquals(1400, HealthEnergyRules.effectiveBurnKcal(null, 1400));
        assertNull(HealthEnergyRules.effectiveBurnKcal(null, null));
    }

    private static HealthMealDO meal(LocalDate d, int kcal) {
        HealthMealDO m = new HealthMealDO();
        m.setMealDate(d);
        m.setKcal(kcal);
        return m;
    }
}
```

若 `BusinessException` 包名与 `HealthService` import 不一致，测试与实现都改成与 `HealthService` 相同的异常类。

- [ ] **Step 2: 跑测试确认失败**

```powershell
cd "backend/lifeassistant"
mvn -pl lifeassistant-system -am test "-Dtest=HealthEnergyRulesTest"
```

Expected: 编译失败或测试失败（类不存在）

- [ ] **Step 3: 最小实现**

```java
package top.lifeassistant.health.service;

import top.continew.starter.core.exception.BusinessException;
import top.lifeassistant.health.model.entity.HealthMealDO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HealthEnergyRules {

    public static final int MAX_KCAL = 20000;

    private HealthEnergyRules() {}

    public static void validateKcal(Integer kcal, String fieldLabel) {
        if (kcal == null) {
            return;
        }
        if (kcal < 0 || kcal > MAX_KCAL) {
            throw new BusinessException(fieldLabel + " must be 0.." + MAX_KCAL);
        }
    }

    /** 入参应为已带非空 kcal 的餐次；按日加总后对天数求平均。 */
    public static BigDecimal averageDailyIntakeKcal(List<HealthMealDO> mealsWithKcal) {
        if (mealsWithKcal == null || mealsWithKcal.isEmpty()) {
            return null;
        }
        Map<LocalDate, Integer> byDay = new HashMap<>();
        for (HealthMealDO meal : mealsWithKcal) {
            byDay.merge(meal.getMealDate(), meal.getKcal(), Integer::sum);
        }
        int sum = byDay.values().stream().mapToInt(Integer::intValue).sum();
        return BigDecimal.valueOf(sum)
            .divide(BigDecimal.valueOf(byDay.size()), 0, RoundingMode.HALF_UP);
    }

    public static Integer effectiveBurnKcal(Integer burnKcal, Integer restingKcal) {
        return burnKcal != null ? burnKcal : restingKcal;
    }
}
```

（`effectiveBurnKcal` 供前端逻辑对照；后端 summary 可不调用，但测试锁住契约。）

- [ ] **Step 4: 跑测试确认通过**

```powershell
cd "backend/lifeassistant"
mvn -pl lifeassistant-system -am test "-Dtest=HealthEnergyRulesTest"
```

Expected: PASS

- [ ] **Step 5: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/health/service/HealthEnergyRules.java" "backend/lifeassistant/lifeassistant-system/src/test/java/top/lifeassistant/health/service/HealthEnergyRulesTest.java"
git commit -m "feat(health): add HealthEnergyRules for kcal validation and averages"
```

---

### Task 3: 后端 meal / daily / summary 接线

**Files:**
- Modify: `.../model/req/HealthMealUpsertReq.java`
- Modify: `.../model/resp/HealthMealResp.java`
- Modify: `.../model/req/HealthDailyUpsertReq.java`（整类自定义反序列化，对齐 `HealthProfileUpsertReq` 的 `targetKgPresent`）
- Modify: `.../model/resp/HealthDailyResp.java`
- Modify: `.../model/resp/HealthSummaryResp.java`
- Modify: `.../service/HealthService.java`

**Interfaces:**
- Consumes: Task 1 DO 字段；`HealthEnergyRules.validateKcal` / `averageDailyIntakeKcal`
- Produces:
  - Meal API JSON: `kcal` (Integer|null)
  - Daily API JSON: `burnKcal` (Integer|null)；写入尊重 `burnKcalPresent`
  - Summary JSON: `avg30IntakeKcal` (BigDecimal|null)

- [ ] **Step 1: Meal Req/Resp**

`HealthMealUpsertReq` 增加：

```java
@Schema(description = "热量 kcal，可空")
private Integer kcal;
```

`HealthMealResp.from` 增加 `.kcal(row.getKcal())` 与字段 `private Integer kcal;`

- [ ] **Step 2: Daily Req 带 burnKcalPresent**

将 `HealthDailyUpsertReq` 改为与 `HealthProfileUpsertReq` 同模式的 `@JsonDeserialize`：

- 字段：`date`, `stomachStatus`, `stomachNote`, `cyclePhase`, `cycleDay`, `burnKcal`, `boolean burnKcalPresent`
- Deserializer：`burnKcalPresent = node.has("burnKcal")`；若 present 则 `burnKcal = intOrNull(node.get("burnKcal"))`
- 其他字段：有则读，无则 null（保持现有 partial：非 burn 字段仍 `!= null` 才更新）

- [ ] **Step 3: Daily Resp**

```java
private Integer burnKcal;
// from(): .burnKcal(row.getBurnKcal())
```

- [ ] **Step 4: HealthService.upsertMeal**

在写库前：

```java
HealthEnergyRules.validateKcal(req.getKcal(), "kcal");
```

insert/update 时 `row.setKcal(req.getKcal());`（与 protein 一样）

- [ ] **Step 5: HealthService.upsertDaily**

在现有 partial 分支中增加：

```java
if (req.isBurnKcalPresent()) {
    HealthEnergyRules.validateKcal(req.getBurnKcal(), "burnKcal");
    row.setBurnKcal(req.getBurnKcal());
}
```

新建行时：若 `burnKcalPresent` 则校验并 set，否则保持 null。

对 `restingKcal`：在 `applyProfileFields` 里对 `req.getRestingKcal() != null` 分支调用 `HealthEnergyRules.validateKcal(req.getRestingKcal(), "restingKcal")`（spec 要求同一上限）。

- [ ] **Step 6: summary 增加 avg30IntakeKcal**

在 `getSummary` 中，蛋白查询旁增加：

```java
List<HealthMealDO> kcalMeals = mealMapper.selectList(new LambdaQueryWrapper<HealthMealDO>()
    .eq(HealthMealDO::getUserId, userId)
    .ge(HealthMealDO::getMealDate, thirtyDaysAgo)
    .le(HealthMealDO::getMealDate, today)
    .isNotNull(HealthMealDO::getKcal));
BigDecimal avg30IntakeKcal = HealthEnergyRules.averageDailyIntakeKcal(kcalMeals);
```

`HealthSummaryResp` 加字段并 `.avg30IntakeKcal(avg30IntakeKcal)`。

- [ ] **Step 7: 编译校验**

```powershell
cd "backend/lifeassistant"
mvn -pl lifeassistant-system -am test "-Dtest=HealthEnergyRulesTest"
```

Expected: PASS

- [ ] **Step 8: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/health/"
git commit -m "feat(health): wire meal kcal, daily burnKcal, avg30IntakeKcal"
```

---

### Task 4: MCP + Python 测试

**Files:**
- Modify: `python/agent/src/life_assistant_agent/tools/health.py`
- Modify: `python/agent/tests/test_domain_dispatch.py`

**Interfaces:**
- Consumes: REST `kcal` / `burnKcal`（camelCase body）
- Produces: MCP 字段 `kcal`、`burn_kcal`（snake）；省略则不写入 body

- [ ] **Step 1: 扩展 meal_upsert body**

在 `protein_g` / `feedback` 旁：

```python
if fields.get("kcal") is not None:
    body["kcal"] = fields["kcal"]
```

注意：若需支持 MCP 显式清空餐次热量，可另议；本任务与现有 `protein_g` 一致——仅当字段传入非 None 时带上。

- [ ] **Step 2: 扩展 _daily_body**

在映射元组中增加 `("burn_kcal", "burnKcal")`。

清空覆盖：MCP 若传 `burn_kcal=None` 且键存在较难；约定文档：清除请在 App 操作，或传 JSON null 需 `fields` 含键。为支持清除，在 `daily_update`：

```python
if "burn_kcal" in fields:
    body["burnKcal"] = fields["burn_kcal"]  # may be None
```

（不要只用 `is not None` 判断，否则无法清。）

- [ ] **Step 3: 写/改测试**

扩展 `test_health_meal_upsert_ok`：增加 `kcal=150`，断言 body 含 `"kcal": 150`。

新增：

```python
async def test_health_daily_update_burn_kcal(self):
    from life_assistant_agent.tools import health as health_tools
    c = _client()
    await health_tools.dispatch(
        c, "daily_update",
        date="2026-08-06", burn_kcal=1800,
    )
    c.put.assert_awaited_with(
        "/health/daily",
        {"date": "2026-08-06", "burnKcal": 1800},
    )

async def test_health_daily_update_clear_burn_kcal(self):
    from life_assistant_agent.tools import health as health_tools
    c = _client()
    await health_tools.dispatch(
        c, "daily_update",
        date="2026-08-06", burn_kcal=None,
    )
    c.put.assert_awaited_with(
        "/health/daily",
        {"date": "2026-08-06", "burnKcal": None},
    )
```

`dispatch` 必须用 `"burn_kcal" in fields`（见 Step 2），否则 clear 测不过。

- [ ] **Step 4: 跑测试**

```powershell
cd "python/agent"
python -m unittest tests.test_domain_dispatch -v
```

Expected: health 相关用例 PASS

- [ ] **Step 5: Commit**

```powershell
git add "python/agent/src/life_assistant_agent/tools/health.py" "python/agent/tests/test_domain_dispatch.py"
git commit -m "feat(mcp): pass meal kcal and daily burn_kcal"
```

---

### Task 5: 前端今日 / 餐次 / 趋势

**Files:**
- Modify: `front/vue3-vant-mobile/src/api/modules/health.ts`
- Modify: `front/vue3-vant-mobile/src/pages/health/index.vue`

**Interfaces:**
- Consumes: `kcal`, `burnKcal`, `avg30IntakeKcal`, `restingKcal`
- Produces: UI 派生 `intakeTotal`、`effectiveBurn`

- [ ] **Step 1: 更新 TS 类型**

```typescript
// HealthMeal
kcal?: number | null

// HealthDaily
burnKcal?: number | null

// HealthSummary
avg30IntakeKcal?: number | null
```

- [ ] **Step 2: 派生 computed**

在 `index.vue` script：

```typescript
const intakeTotal = computed(() => {
  const vals = meals.value.map(m => m.kcal).filter((k): k is number => k != null)
  if (!vals.length)
    return null
  return vals.reduce((a, b) => a + b, 0)
})

const effectiveBurn = computed(() => {
  if (daily.value.burnKcal != null)
    return daily.value.burnKcal
  return profile.value.restingKcal ?? null
})
```

- [ ] **Step 3: 今日卡能量行**

在现有 `.metrics`（晨重/胃/周期）**下方**再加一块：

```html
<div class="metrics energy-metrics">
  <div class="metric">
    <span>已摄入</span>
    <strong>{{ intakeTotal != null ? `${intakeTotal} kcal` : '—' }}</strong>
  </div>
  <button type="button" class="metric metric-btn" @click="openBurn">
    <span>今日消耗</span>
    <strong>{{ effectiveBurn != null ? `${effectiveBurn} kcal` : '—' }}</strong>
    <em>{{ daily.burnKcal != null ? '已覆盖' : (profile.restingKcal != null ? '默认静息' : '点击设置') }}</em>
  </button>
</div>
```

CSS：`.energy-metrics { margin-top: 12px; }`（两列即可）

- [ ] **Step 4: 消耗弹层**

对齐胃状态 dialog 模式：

```typescript
const showBurnDialog = ref(false)
const burnForm = reactive({ burnKcal: '' })

function openBurn(): void {
  burnForm.burnKcal = daily.value.burnKcal != null
    ? String(daily.value.burnKcal)
    : (profile.value.restingKcal != null ? String(profile.value.restingKcal) : '')
  showBurnDialog.value = true
}

async function onBurn(): Promise<void> {
  const raw = burnForm.burnKcal.trim()
  const body = {
    date: selectedDate.value,
    burnKcal: raw === '' ? null : Number(raw),
  }
  // 必须带 burnKcal 键（含 null）以触发后端 present
  await putHealthDaily(body)
  showBurnDialog.value = false
  await loadDaily()
}
```

确认 `putHealthDaily` 用 JSON.stringify 会保留 `burnKcal: null`（axios 默认保留）。若拦截器剥 null，改为显式传或检查 request 工具。

Dialog：

```html
<van-dialog v-model:show="showBurnDialog" title="今日消耗" show-cancel-button @confirm="onBurn">
  <div class="dialog-form">
    <van-field v-model="burnForm.burnKcal" type="digit" placeholder="kcal（清空则回退静息）" clearable />
  </div>
</van-dialog>
```

- [ ] **Step 5: 餐卡 + 表单**

`mealForm` 增加 `kcal: string`；`openMeal` / `onMeal` 读写 `kcal`（空 → `null`）。

卡片 `small`：

```html
<small v-if="mealRow(m.type)">{{ mealRow(m.type)?.kcal != null ? `热量 ${mealRow(m.type)?.kcal} kcal` : '热量：待填' }}</small>
```

表单 field：`placeholder="热量 kcal（可空）"`。

- [ ] **Step 6: 趋势平均摄入**

替换占位：

```html
<div>平均摄入<b>{{ summary.avg30IntakeKcal != null ? `${Math.round(Number(summary.avg30IntakeKcal))} kcal` : '—' }}</b></div>
```

- [ ] **Step 7: 手工自检（对照 spec）**

1. 早餐 150、午餐无 → 已摄入 150
2. 静息 1400、无覆盖 → 今日消耗 1400；覆盖 1800 → 1800；清空 → 1400
3. 趋势有数据后天均摄入非 `—`

- [ ] **Step 8: Commit**

```powershell
git add "front/vue3-vant-mobile/src/api/modules/health.ts" "front/vue3-vant-mobile/src/pages/health/index.vue"
git commit -m "feat(health): show intake total, burn override, meal kcal UI"
```

---

### Task 6: Skill 文档

**Files:**
- Modify: `.cursor/skills/health-butler-mcp/SKILL.md`

**Interfaces:**
- Consumes: MCP 字段名 `kcal`、`burn_kcal`
- Produces: 工作流说明更新

- [ ] **Step 1: 更新「记饮食」**

参数列表增加：

- `kcal`（可选，整数，该餐热量）

查询饮食返回说明加上 kcal。

- [ ] **Step 2: 更新「胃状态 / 周期」为「日况」**

```markdown
## 6. 日况（胃 / 周期 / 消耗）

- `action=daily_update`
- 参数：`date`，以及需要的：
  - `stomach_status` / `stomach_note`
  - `cycle_phase` / `cycle_day`
  - `burn_kcal`（可选；当日消耗覆盖。省略=不改；传 `null`=清除覆盖）
- 展示用今日消耗 = `burn_kcal` ?? 档案 `resting_kcal`
```

- [ ] **Step 3: 更新「趋势摘要」**

- `action=summary` → …、近 30 日均蛋白、**近 30 日均摄入 kcal**

- [ ] **Step 4: Commit**

```powershell
git add ".cursor/skills/health-butler-mcp/SKILL.md"
git commit -m "docs(skill): meal kcal and daily burn_kcal workflows"
```

---

## Spec 覆盖自检

| Spec 要求 | Task |
|-----------|------|
| `health_meal.kcal` | 1, 3 |
| `health_daily.burn_kcal` 日况抽象 | 1, 3 |
| 摄入 SUM / 部分餐规则 | 2, 5 |
| 消耗 `burn ?? resting` | 2, 5 |
| `burnKcalPresent` 清空 | 3, 4, 5 |
| `avg30IntakeKcal` | 2, 3, 5 |
| 校验 0..20000 | 2, 3 |
| MCP kcal / burn_kcal | 4, 6 |
| 今日 UI + 餐卡表单 | 5 |
| 趋势平均摄入 | 5 |
| 不迁 feedback / 不建新表 | 约束（无任务违反） |

## Placeholder / 类型一致性

- 字段名全程：`kcal`、`burnKcal`/`burn_kcal`、`avg30IntakeKcal`、`burnKcalPresent`
- 无 TBD；自检步骤可执行
