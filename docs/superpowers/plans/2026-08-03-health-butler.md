# Health Butler Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将「小满 · 健康管家」原型落地为 App 子模块 `/health` + Java REST 持久化 + MCP `health` 域 + Cursor Skill 工作流手册。

**Architecture:** Flyway 建 7 表 → `top.lifeassistant.health` MyBatis-Plus CRUD → `/health/*` REST（JWT / API Token 共用）→ 前端 `/health` 三 Tab + Profile 浮动入口 → Python Agent 新增 `health` 域工具 → `.cursor/skills/health-butler-mcp/SKILL.md`。

**Tech Stack:** Java 17 / Spring Boot / MyBatis-Plus / Flyway；Vue 3 + Vant + file-based routes；Python FastMCP + httpx；unittest。

**Spec:** `docs/superpowers/specs/2026-08-03-health-butler-design.md`
**Prototype:** `docs/原型图/index.html`

## Global Constraints

- 数据仅本人可见：所有查询/变更按 `userId` 过滤；跨用户返回 404（用 `BusinessException`）
- App 内无对话窗；自然语言录入只走 MCP
- REST 路径前缀 `/health`（与现有 `/partner/checkin` 一样，无额外 `/api` 段；前端 `request` baseURL 已含 API 根）
- MCP 新增第 7 域工具名必须为 `health`；`action` 分发；错误 JSON 用现有 `invalid_action` / `missing_field`
- 规律 `health_memory` 最多 **100** 条；满则 400
- 目标体重可空 → UI「待设置」
- 进入今日页默认 **今天**（本地日历日）
- 入口：Q 版医生头像 + 文案「健康管家」（非用户头像）
- Skill 只写工作流与参数，不写隐私边界
- PowerShell：`git commit -m "msg"`；路径双引号
- 包名：`top.lifeassistant.health`（不是 healthbutler）
- 不引入新前后端依赖；MCP 不新增 Python 依赖

## Agent 并行建议

| 波次 | 可并行 Task | 依赖 |
|------|-------------|------|
| A | Task 1 | 无 |
| B | Task 2 | Task 1 |
| C | Task 3 + Task 4 | Task 2 |
| D | Task 5 | Task 3、4（至少接口稳定） |
| E | Task 6 + Task 7 | Task 5 |
| F | Task 8 | Task 3、4 |
| G | Task 9 | Task 8（action 名已定） |

---

## 文件结构

```
backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/
  V16__create_health_tables.sql

backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/health/
  controller/HealthController.java
  service/HealthService.java
  mapper/
    HealthProfileMapper.java
    HealthMealMapper.java
    HealthWeightMapper.java
    HealthDailyMapper.java
    HealthMemoryMapper.java
    HealthToleranceMapper.java
    HealthTriggerMapper.java
  model/
    entity/Health*DO.java          （7 个）
    req/Health*Req.java
    resp/Health*Resp.java
    query/HealthPageQuery.java

front/vue3-vant-mobile/src/
  api/modules/health.ts
  pages/health/index.vue           （三 Tab 容器）
  pages/profile/index.vue          （加浮动入口）
  assets/health/doctor-avatar.svg  （或 png）
  locales/zh-CN.json, en-US.json

python/agent/src/life_assistant_agent/
  tools/health.py
  server.py                        （注册 health）
  tests/test_domain_dispatch.py    （追加用例）

.cursor/skills/health-butler-mcp/SKILL.md
```

---

### Task 1: Flyway V16 建表

**Files:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V16__create_health_tables.sql`

**Interfaces:**
- Produces: 表 `health_profile`, `health_meal`, `health_weight`, `health_daily`, `health_memory`, `health_tolerance`, `health_trigger`

- [ ] **Step 1: 写迁移 SQL**

```sql
-- V16__create_health_tables.sql
CREATE TABLE health_profile (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    display_name VARCHAR(64) NULL,
    motto VARCHAR(128) NULL,
    height_cm DECIMAL(5,2) NULL,
    target_kg DECIMAL(5,2) NULL COMMENT 'NULL = 待设置',
    resting_kcal INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_health_profile_user (user_id)
);

CREATE TABLE health_meal (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    meal_date DATE NOT NULL,
    meal_type VARCHAR(8) NOT NULL COMMENT '早餐/午餐/晚餐',
    food VARCHAR(512) NOT NULL,
    protein_g DECIMAL(6,1) NULL,
    feedback VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_health_meal (user_id, meal_date, meal_type),
    KEY idx_health_meal_user_date (user_id, meal_date)
);

CREATE TABLE health_weight (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    weight_date DATE NOT NULL,
    weight_type VARCHAR(8) NOT NULL COMMENT '晨重/晚重',
    kg DECIMAL(5,2) NOT NULL,
    standard TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=纳入趋势',
    note VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_health_weight_user_date (user_id, weight_date)
);

CREATE TABLE health_daily (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    daily_date DATE NOT NULL,
    stomach_status VARCHAR(32) NULL,
    stomach_note VARCHAR(128) NULL,
    cycle_phase VARCHAR(32) NULL,
    cycle_day INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_health_daily (user_id, daily_date)
);

CREATE TABLE health_memory (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    title VARCHAR(128) NOT NULL,
    detail VARCHAR(512) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_health_memory_user (user_id)
);

CREATE TABLE health_tolerance (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    name VARCHAR(64) NOT NULL,
    level VARCHAR(16) NOT NULL COMMENT '舒适/低风险/谨慎/高风险',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_health_tolerance_user (user_id)
);

CREATE TABLE health_trigger (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    name VARCHAR(64) NOT NULL,
    note VARCHAR(128) NULL,
    stars TINYINT NOT NULL DEFAULT 3,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_health_trigger_user (user_id)
);
```

- [ ] **Step 2: 启动后端确认 Flyway 应用成功**

Run: 按项目惯例启动 `lifeassistant-server`（或看启动日志含 `V16__create_health_tables`）
Expected: 迁移成功，无 ERROR

- [ ] **Step 3: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V16__create_health_tables.sql"
git commit -m "feat(health): add Flyway V16 health tables"
```

---

### Task 2: Entity + Mapper

**Files:**
- Create: `.../health/model/entity/HealthProfileDO.java`（及其余 6 个 DO）
- Create: `.../health/mapper/HealthProfileMapper.java`（及其余 6 个 Mapper）

**Interfaces:**
- Produces: 7 个 `@TableName` DO；7 个 `extends BaseMapper<*DO>`
- ID：`CHAR(36)`，创建时用 `UUID.randomUUID().toString()`（与现网 partner 一致）

- [ ] **Step 1: 写一个代表性 DO + Mapper（其余同模式）**

```java
// HealthProfileDO.java
package top.lifeassistant.health.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("health_profile")
public class HealthProfileDO implements Serializable {
    @TableId
    private String id;
    @TableField("user_id")
    private String userId;
    @TableField("display_name")
    private String displayName;
    private String motto;
    @TableField("height_cm")
    private BigDecimal heightCm;
    @TableField("target_kg")
    private BigDecimal targetKg;
    @TableField("resting_kcal")
    private Integer restingKcal;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
```

```java
package top.lifeassistant.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.lifeassistant.health.model.entity.HealthProfileDO;

@Mapper
public interface HealthProfileMapper extends BaseMapper<HealthProfileDO> {}
```

其余 DO 字段与 V16 列一一对应：
- `HealthMealDO`: mealDate(`LocalDate`), mealType, food, proteinG, feedback
- `HealthWeightDO`: weightDate, weightType, kg, standard(`Boolean`), note
- `HealthDailyDO`: dailyDate, stomachStatus, stomachNote, cyclePhase, cycleDay
- `HealthMemoryDO`: title, detail, sortOrder
- `HealthToleranceDO`: name, level
- `HealthTriggerDO`: name, note, stars(`Integer`)

- [ ] **Step 2: 编译 system 模块**

Run: `mvn -pl lifeassistant-system -am compile -DskipTests`（在 `backend/lifeassistant` 目录）
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/health/"
git commit -m "feat(health): add health entities and mappers"
```

---

### Task 3: HealthService + Controller（profile / meal / daily）

**Files:**
- Create: `.../health/model/req/HealthProfileUpsertReq.java`
- Create: `.../health/model/req/HealthMealUpsertReq.java`
- Create: `.../health/model/req/HealthDailyUpsertReq.java`
- Create: `.../health/model/resp/HealthProfileResp.java`
- Create: `.../health/model/resp/HealthMealResp.java`
- Create: `.../health/model/resp/HealthDailyResp.java`
- Create: `.../health/service/HealthService.java`
- Create: `.../health/controller/HealthController.java`

**Interfaces:**
- Consumes: Task 2 mappers；`@CurrentUser UserDO`；`ApiResponse`；`BusinessException`
- Produces REST（均需登录）:

| Method | Path | Body/Query | Returns |
|--------|------|------------|---------|
| GET | `/health/profile` | — | `HealthProfileResp`（无行则返回空字段默认对象，不 404） |
| PUT | `/health/profile` | `HealthProfileUpsertReq` | `HealthProfileResp` |
| GET | `/health/meals` | `date=yyyy-MM-dd` | `List<HealthMealResp>` |
| PUT | `/health/meals` | `HealthMealUpsertReq`（含 date+mealType+food+proteinG+feedback） | `HealthMealResp` upsert |
| DELETE | `/health/meals/{id}` | — | void |
| GET | `/health/daily` | `date=` | `HealthDailyResp`（可空字段） |
| PUT | `/health/daily` | `HealthDailyUpsertReq` | `HealthDailyResp` |

`HealthMealUpsertReq` 字段：`LocalDate date`, `String mealType`, `String food`, `BigDecimal proteinG`, `String feedback`
`mealType` 仅允许 `早餐|午餐|晚餐`。

- [ ] **Step 1: 实现 Service 核心逻辑（节选）**

```java
public HealthMealResp upsertMeal(String userId, HealthMealUpsertReq req) {
    if (!Set.of("早餐", "午餐", "晚餐").contains(req.getMealType())) {
        throw new BusinessException("mealType must be 早餐/午餐/晚餐");
    }
    HealthMealDO row = mealMapper.selectOne(new LambdaQueryWrapper<HealthMealDO>()
        .eq(HealthMealDO::getUserId, userId)
        .eq(HealthMealDO::getMealDate, req.getDate())
        .eq(HealthMealDO::getMealType, req.getMealType()));
    if (row == null) {
        row = new HealthMealDO();
        row.setId(UUID.randomUUID().toString());
        row.setUserId(userId);
        row.setMealDate(req.getDate());
        row.setMealType(req.getMealType());
        row.setCreatedAt(LocalDateTime.now());
        // set food/protein/feedback ...
        mealMapper.insert(row);
    } else {
        // update fields ...
        mealMapper.updateById(row);
    }
    return toMealResp(row);
}

public void deleteMeal(String userId, String id) {
    HealthMealDO row = mealMapper.selectById(id);
    if (row == null || !userId.equals(row.getUserId())) {
        throw new BusinessException("meal not found"); // GlobalExceptionHandler → 业务错误
    }
    mealMapper.deleteById(id);
}
```

Controller：

```java
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {
    private final HealthService healthService;

    @GetMapping("/profile")
    public ApiResponse<HealthProfileResp> getProfile(@CurrentUser UserDO user) {
        return ApiResponse.ok(healthService.getProfile(user.getId()));
    }

    @PutMapping("/profile")
    public ApiResponse<HealthProfileResp> putProfile(@CurrentUser UserDO user,
            @Valid @RequestBody HealthProfileUpsertReq req) {
        return ApiResponse.ok(healthService.upsertProfile(user.getId(), req));
    }

    @GetMapping("/meals")
    public ApiResponse<List<HealthMealResp>> listMeals(@CurrentUser UserDO user,
            @RequestParam @DateTimeFormat(iso = DATE) LocalDate date) {
        return ApiResponse.ok(healthService.listMeals(user.getId(), date));
    }

    @PutMapping("/meals")
    public ApiResponse<HealthMealResp> upsertMeal(@CurrentUser UserDO user,
            @Valid @RequestBody HealthMealUpsertReq req) {
        return ApiResponse.ok(healthService.upsertMeal(user.getId(), req));
    }

    @DeleteMapping("/meals/{id}")
    public ApiResponse<Void> deleteMeal(@CurrentUser UserDO user, @PathVariable String id) {
        healthService.deleteMeal(user.getId(), id);
        return ApiResponse.ok();
    }

    // daily GET/PUT 同理
}
```

- [ ] **Step 2: 手工/curl 冒烟（登录后）**

```text
PUT /health/meals {"date":"2026-08-03","mealType":"早餐","food":"鸡蛋","proteinG":7}
GET /health/meals?date=2026-08-03
```

Expected: 返回含早餐；再用另一用户 token 读应为空或 404（按实现：list 为空）

- [ ] **Step 3: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/health/"
git commit -m "feat(health): profile meal daily REST"
```

---

### Task 4: weight / memory / tolerance / trigger / summary

**Files:**
- Modify: `HealthService.java`, `HealthController.java`
- Create: 对应 Req/Resp；`HealthPageQuery.java`（page、pageSize）；`HealthSummaryResp.java`

**Interfaces:**
- Produces:

| Method | Path | Notes |
|--------|------|-------|
| GET | `/health/weights` | query: `from`,`to` 可选；`type=晨重\|晚重\|all` |
| POST | `/health/weights` | body: date, weightType, kg, standard |
| DELETE | `/health/weights/{id}` | 本人 |
| GET | `/health/memories` | PageResult；默认 size=5 |
| POST | `/health/memories` | title, detail；count≥100 → BusinessException |
| PATCH | `/health/memories/{id}` | |
| DELETE | `/health/memories/{id}` | |
| GET/POST/PATCH/DELETE | `/health/tolerances` | 默认 size=4 |
| GET/POST/PATCH/DELETE | `/health/triggers` | 默认 size=3；stars 1–5 |
| GET | `/health/summary` | 近 7 日标准晨重均、距目标、近 30 日均蛋白 |

`HealthSummaryResp` 字段建议：
```java
BigDecimal latestMorningKg;
BigDecimal avg7MorningKg;
BigDecimal targetKg;          // 可 null
BigDecimal gapToTargetKg;     // 可 null
BigDecimal avg30ProteinG;     // 可 null（无蛋白天则 null）
```

日均蛋白算法：近 30 天内，按日把有 `protein_g` 的餐加总；只对「至少有一餐填了蛋白」的日期求平均。

记忆满员：

```java
Long cnt = memoryMapper.selectCount(new LambdaQueryWrapper<HealthMemoryDO>()
    .eq(HealthMemoryDO::getUserId, userId));
if (cnt >= 100) {
    throw new BusinessException("健康规律最多 100 条");
}
```

分页用 MyBatis-Plus `Page` + `PageResult.of(page)`。

- [ ] **Step 1: 实现并冒烟 summary / memories 分页**

Expected: `GET /health/memories?page=1&pageSize=5` 返回 `records/total/page/size`；第 101 条 POST 失败。

- [ ] **Step 2: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/health/"
git commit -m "feat(health): weight memory tolerance trigger summary REST"
```

---

### Task 5: 前端 API 模块

**Files:**
- Create: `front/vue3-vant-mobile/src/api/modules/health.ts`

**Interfaces:**
- Consumes: `@/utils/request`，`ApiResponse`，后端 Task 3–4 路径
- Produces: 导出函数供页面调用

- [ ] **Step 1: 写 API 模块**

```typescript
// front/vue3-vant-mobile/src/api/modules/health.ts
import request from '@/utils/request'
import type { ApiResponse } from '@/types/api'

export interface HealthProfile {
  displayName?: string
  motto?: string
  heightCm?: number | null
  targetKg?: number | null
  restingKcal?: number | null
}

export interface HealthMeal {
  id: string
  date: string
  mealType: '早餐' | '午餐' | '晚餐'
  food: string
  proteinG?: number | null
  feedback?: string | null
}

export interface HealthDaily {
  date: string
  stomachStatus?: string | null
  stomachNote?: string | null
  cyclePhase?: string | null
  cycleDay?: number | null
}

export interface HealthWeight {
  id: string
  date: string
  weightType: '晨重' | '晚重'
  kg: number
  standard: boolean
}

export interface HealthSummary {
  latestMorningKg?: number | null
  avg7MorningKg?: number | null
  targetKg?: number | null
  gapToTargetKg?: number | null
  avg30ProteinG?: number | null
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}

export function getHealthProfile() {
  return request.get<any, ApiResponse<HealthProfile>>('/health/profile')
}
export function putHealthProfile(body: HealthProfile) {
  return request.put<any, ApiResponse<HealthProfile>>('/health/profile', body)
}
export function listHealthMeals(date: string) {
  return request.get<any, ApiResponse<HealthMeal[]>>('/health/meals', { params: { date } })
}
export function upsertHealthMeal(body: Omit<HealthMeal, 'id'> & { id?: string }) {
  return request.put<any, ApiResponse<HealthMeal>>('/health/meals', body)
}
export function deleteHealthMeal(id: string) {
  return request.delete<any, ApiResponse<void>>(`/health/meals/${id}`)
}
export function getHealthDaily(date: string) {
  return request.get<any, ApiResponse<HealthDaily>>('/health/daily', { params: { date } })
}
export function putHealthDaily(body: HealthDaily) {
  return request.put<any, ApiResponse<HealthDaily>>('/health/daily', body)
}
export function listHealthWeights(params?: { from?: string, to?: string, type?: string }) {
  return request.get<any, ApiResponse<HealthWeight[]>>('/health/weights', { params })
}
export function createHealthWeight(body: { date: string, weightType: string, kg: number, standard?: boolean }) {
  return request.post<any, ApiResponse<HealthWeight>>('/health/weights', body)
}
export function getHealthSummary() {
  return request.get<any, ApiResponse<HealthSummary>>('/health/summary')
}
export function listHealthMemories(page = 1, pageSize = 5) {
  return request.get<any, ApiResponse<PageResult<{ id: string, title: string, detail?: string }>>>('/health/memories', { params: { page, pageSize } })
}
export function createHealthMemory(body: { title: string, detail?: string }) {
  return request.post('/health/memories', body)
}
export function listHealthTolerances(page = 1, pageSize = 4) {
  return request.get('/health/tolerances', { params: { page, pageSize } })
}
export function createHealthTolerance(body: { name: string, level: string }) {
  return request.post('/health/tolerances', body)
}
export function listHealthTriggers(page = 1, pageSize = 3) {
  return request.get('/health/triggers', { params: { page, pageSize } })
}
export function createHealthTrigger(body: { name: string, note?: string, stars: number }) {
  return request.post('/health/triggers', body)
}
```

（PATCH/DELETE 按后端补齐导出。）

- [ ] **Step 2: Commit**

```powershell
git add "front/vue3-vant-mobile/src/api/modules/health.ts"
git commit -m "feat(health): frontend health API module"
```

---

### Task 6: `/health` 三页 UI

**Files:**
- Create: `front/vue3-vant-mobile/src/pages/health/index.vue`
- Modify: `front/vue3-vant-mobile/src/locales/zh-CN.json` — `navbar.Health`: `健康管家`
- Modify: `front/vue3-vant-mobile/src/locales/en-US.json` — `navbar.Health`: `Health`
- **不要**把 `Health` 加入 `rootRouteList`

**Interfaces:**
- Consumes: Task 5 API；UI 对齐 `docs/原型图/index.html`
- Produces: 路由名 `Health` → `/health`

- [ ] **Step 1: 页面骨架 + route**

```vue
<!-- pages/health/index.vue 结构 -->
<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
// import APIs from '@/api/modules/health'
const tab = ref(0) // 0今日 1档案 2趋势
const selectedDate = ref(todayLocal()) // 进入默认今天
function todayLocal() {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}
</script>

<template>
  <div class="health-page">
    <!-- tab===0: 今日（概览+日期居中+记饮食+三餐） -->
    <!-- tab===1: 档案（编辑档案+三块翻页列表） -->
    <!-- tab===2: 趋势（记体重+图+明细+summary） -->
    <van-tabbar v-model="tab" :fixed="false" safe-area-inset-bottom>
      <van-tabbar-item icon="notes-o">今日情况</van-tabbar-item>
      <van-tabbar-item icon="user-o">身体档案</van-tabbar-item>
      <van-tabbar-item icon="chart-trending-o">趋势</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<style scoped>
/* 色板对齐原型：--green #315d43；--lime #cfe681；metrics flex:1 均分 */
</style>

<route lang="json5">
{ name: 'Health', meta: { title: '健康管家' } }
</route>
```

- [ ] **Step 2: 按原型实现交互（用 Vant Dialog/Form 替代 prompt）**

必做清单：
1. 晨重展示；点目标 → 设/清空 `targetKg`
2. 点胃状态 / 周期 → `putHealthDaily`
3. 日期 ‹ › + 日历；**无「今日记录」标题**；「记饮食」
4. 三餐：食物 → 蛋白 → 反馈；卡片展示蛋白 g
5. 档案：规律/耐受/触发翻页 + 添加（满 100 toast）
6. 趋势：记体重、筛选、summary 日均蛋白

- [ ] **Step 3: 手动打开 `/health` 走通三 Tab**

Expected: 默认今天；写入后刷新仍在。

- [ ] **Step 4: Commit**

```powershell
git add "front/vue3-vant-mobile/src/pages/health/" "front/vue3-vant-mobile/src/locales/zh-CN.json" "front/vue3-vant-mobile/src/locales/en-US.json"
git commit -m "feat(health): health three-tab page aligned to prototype"
```

---

### Task 7: Profile 浮动入口

**Files:**
- Create: `front/vue3-vant-mobile/src/assets/health/doctor-avatar.svg`（简洁 Q 版医生头像）
- Modify: `front/vue3-vant-mobile/src/pages/profile/index.vue`

**Interfaces:**
- Consumes: `vue-router` → `{ name: 'Health' }`；登录态 `useUserStore`

- [ ] **Step 1: 在 Profile 加右下角入口**

参考 todos FAB，但内容为头像+文案：

```vue
<!-- 仅 isLogin 时显示 -->
<div v-if="isLogin" class="health-fab" @click="router.push({ name: 'Health' })">
  <img class="health-fab__avatar" src="@/assets/health/doctor-avatar.svg" alt="" />
  <span class="health-fab__label">健康管家</span>
</div>
```

```css
.health-fab {
  position: fixed;
  right: 16px;
  bottom: 80px; /* 避开主 TabBar */
  z-index: 100;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.health-fab__avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  box-shadow: 0 6px 16px rgba(32, 53, 39, 0.18);
  background: #dceaa3;
}
.health-fab__label {
  font-size: 11px;
  color: #315d43;
  font-weight: 600;
}
```

- [ ] **Step 2: 从「我的」点进健康管家再返回**

Expected: 进入 `/health`；返回仍在「我的」。

- [ ] **Step 3: Commit**

```powershell
git add "front/vue3-vant-mobile/src/pages/profile/index.vue" "front/vue3-vant-mobile/src/assets/health/"
git commit -m "feat(health): profile floating entry to health butler"
```

---

### Task 8: MCP `health` 域

**Files:**
- Create: `python/agent/src/life_assistant_agent/tools/health.py`
- Modify: `python/agent/src/life_assistant_agent/server.py`
- Modify: `python/agent/src/life_assistant_agent/tools/__init__.py`（若需导出）
- Modify: `python/agent/tests/test_domain_dispatch.py`

**Interfaces:**
- Consumes: Java `/health/*`；`dispatch_util.require/invalid_action`；`JavaClient`
- Produces: `@mcp.tool` 函数 `health(action=..., **fields)`

**Actions（固定集合）：**

| action | 必填字段 | HTTP |
|--------|----------|------|
| `profile_get` | — | GET `/health/profile` |
| `profile_update` | 任意可写字段 | PUT `/health/profile` |
| `meal_list` | `date` | GET `/health/meals?date=` |
| `meal_upsert` | `date`,`meal_type`,`food`；可选 `protein_g`,`feedback` | PUT `/health/meals`（JSON camelCase） |
| `meal_delete` | `id` | DELETE `/health/meals/{id}` |
| `weight_list` | 可选 `from_`,`to`,`type_` | GET `/health/weights` |
| `weight_add` | `date`,`weight_type`,`kg`；可选 `standard` | POST `/health/weights` |
| `daily_get` | `date` | GET `/health/daily` |
| `daily_update` | `date` + 字段 | PUT `/health/daily` |
| `memory_list` | 可选 `page`,`page_size` | GET `/health/memories` |
| `memory_add` | `title`；可选 `detail` | POST `/health/memories` |
| `tolerance_list` / `tolerance_add` | 同分页/name+level | |
| `trigger_list` / `trigger_add` | 同分页/name+stars | |
| `summary` | — | GET `/health/summary` |

JSON 字段映射：MCP snake_case → REST camelCase（`meal_type`→`mealType`，`protein_g`→`proteinG`）。

- [ ] **Step 1: 写失败测试再实现 dispatch**

```python
# 追加到 test_domain_dispatch.py
async def test_health_meal_upsert_requires_fields(self):
    from life_assistant_agent.tools import health as health_tools
    c = _client()
    out = await health_tools.dispatch(c, "meal_upsert", date="2026-08-03")
    self.assertEqual(out["error"], "missing_field")

async def test_health_meal_upsert_ok(self):
    from life_assistant_agent.tools import health as health_tools
    c = _client()
    await health_tools.dispatch(
        c, "meal_upsert",
        date="2026-08-03", meal_type="早餐", food="鸡蛋", protein_g=7,
    )
    c.put.assert_awaited()
```

- [ ] **Step 2: 跑测试**

```powershell
cd python/agent
python -m unittest tests.test_domain_dispatch -v
```

Expected: PASS（含新用例）

- [ ] **Step 3: server.py 注册**

```python
from .tools import health as health_tools

@mcp.tool(description="健康管家：action=profile_get|profile_update|meal_list|meal_upsert|meal_delete|weight_list|weight_add|daily_get|daily_update|memory_list|memory_add|tolerance_list|tolerance_add|trigger_list|trigger_add|summary")
async def health(
    action: str,
    date: str | None = None,
    meal_type: str | None = None,
    food: str | None = None,
    protein_g: float | None = None,
    feedback: str | None = None,
    id: str | None = None,
    weight_type: str | None = None,
    kg: float | None = None,
    standard: bool | None = None,
    title: str | None = None,
    detail: str | None = None,
    name: str | None = None,
    level: str | None = None,
    note: str | None = None,
    stars: int | None = None,
    page: int | None = None,
    page_size: int | None = None,
    target_kg: float | None = None,
    height_cm: float | None = None,
    resting_kcal: int | None = None,
    display_name: str | None = None,
    motto: str | None = None,
    stomach_status: str | None = None,
    stomach_note: str | None = None,
    cycle_phase: str | None = None,
    cycle_day: int | None = None,
    from_: str | None = None,
    to: str | None = None,
    type_: str | None = None,
) -> str:
    return await _run(
        health_tools.dispatch,
        action,
        date=date,
        meal_type=meal_type,
        food=food,
        protein_g=protein_g,
        feedback=feedback,
        id=id,
        weight_type=weight_type,
        kg=kg,
        standard=standard,
        title=title,
        detail=detail,
        name=name,
        level=level,
        note=note,
        stars=stars,
        page=page,
        page_size=page_size,
        target_kg=target_kg,
        height_cm=height_cm,
        resting_kcal=resting_kcal,
        display_name=display_name,
        motto=motto,
        stomach_status=stomach_status,
        stomach_note=stomach_note,
        cycle_phase=cycle_phase,
        cycle_day=cycle_day,
        from_=from_,
        to=to,
        type_=type_,
    )
```

- [ ] **Step 4: Commit**

```powershell
git add "python/agent/src/life_assistant_agent/tools/health.py" "python/agent/src/life_assistant_agent/server.py" "python/agent/tests/test_domain_dispatch.py"
git commit -m "feat(agent): add health MCP domain tool"
```

---

### Task 9: Cursor Skill 工作流手册

**Files:**
- Create: `.cursor/skills/health-butler-mcp/SKILL.md`

**Interfaces:**
- Consumes: Task 8 的 action 与参数名（必须一致）

- [ ] **Step 1: 写 Skill（工作流 + 参数，无隐私章节）**

```markdown
---
name: health-butler-mcp
description: 通过 Life Assistant MCP health 域记录/查询健康管家数据（饮食、体重、规律等）。在用户要求用 MCP/AI 记饮食、记体重、加健康规律或查健康数据时使用。
---

# 健康管家 MCP

工具名：`health`。每次调用传 `action` + 所需字段。鉴权使用用户 API Token（`Authorization: Bearer la_...`）。

## 1. 记饮食

1. 确定 `date`（默认今天 `YYYY-MM-DD`）与 `meal_type`：`早餐` | `午餐` | `晚餐`
2. 调用 `action=meal_upsert`，参数：
   - `date`（必填）
   - `meal_type`（必填）
   - `food`（必填，吃了什么）
   - `protein_g`（可选，数字）
   - `feedback`（可选，饭后感受）
3. 同日同餐次再次 upsert 即覆盖。

## 2. 查询饮食

- `action=meal_list`，参数 `date`
- 返回该日各餐 food / protein_g / feedback

## 3. 记体重

- `action=weight_add`
- 参数：`date`，`weight_type`=`晨重`|`晚重`，`kg`，可选 `standard`（晨重建议 true）

## 4. 增加健康规律

- 先可选 `memory_list`（`page`,`page_size`）查看
- `action=memory_add`：`title` 必填，`detail` 可选
- 上限 100 条；失败时提示用户删旧再加

## 5. 设目标体重

- `action=profile_update`，参数 `target_kg`（数字；若需清空，按 REST 约定传 null——若 MCP 无法传 null，则文档写明用 App 清空）

## 6. 胃状态 / 周期

- `action=daily_update`
- 参数：`date`，以及 `stomach_status`/`stomach_note`/`cycle_phase`/`cycle_day` 中需要的字段

## 7. 耐受 / 触发（翻页）

- `tolerance_list` / `tolerance_add`（`name`,`level`）
- `trigger_list` / `trigger_add`（`name`,`stars` 1–5，`note` 可选）

## 8. 趋势摘要

- `action=summary` → 7 日均晨重、目标缺口、近 30 日均蛋白
```

- [ ] **Step 2: Commit**

```powershell
git add ".cursor/skills/health-butler-mcp/SKILL.md"
git commit -m "docs(skill): health butler MCP workflows"
```

---

## 端到端验收（全部 Task 完成后）

1. 「我的」右下角「健康管家」进入三页
2. 记早餐含蛋白 → 趋势日均蛋白变化
3. API Token：`meal_upsert` → App 今日页可见
4. App 改目标 → MCP `profile_get` 一致
5. `memory_add` 至 100 后第 101 条失败

---

## Spec 覆盖自检

| Spec 项 | Task |
|---------|------|
| 7 表 + meal 蛋白 + daily | 1–2 |
| REST CRUD/分页/summary | 3–4 |
| `/health` 三页对齐原型 | 6 |
| Profile Q 版医生入口 | 7 |
| MCP health 域 | 8 |
| Skill 工作流 | 9 |
| 默认今天 / 待设置目标 / 无对话 | 6 Global + 文案 |
| 不做导出/共享/对话 | 未建对应 Task |
