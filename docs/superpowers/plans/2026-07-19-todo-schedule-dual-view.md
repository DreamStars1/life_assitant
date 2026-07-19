# 待办 / 日程双视图 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在今日页与待办页增加「待办 | 日程」双视图；日程为独立时间块域，支持日/周、只看我 / 两人并排，以及共同日程邀约。

**Architecture:** 新建 `schedule_event` + `schedule_invite`（Flyway V11）；后端 `top.lifeassistant.schedule` 包提供 CRUD、区间展开、伴侣只读与邀约 ack；前端新建 `schedule` API 与日/周组件，挂到 `/` 与 `/todos` 一级 Tab。旧 `/events` 占位重定向或下线。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway / Vue 3 + Vant 4

**Spec:** `docs/superpowers/specs/2026-07-19-todo-schedule-dual-view-design.md`

## Global Constraints

- 日程与 Todo **分离**；不做私密日程、全天事件、复杂重复、「仅此一次」编辑
- 重复仅 `none|daily|weekly` + 可选 `recurrenceEndDate`；**服务端**按 `from`/`to` 展开
- 查询区间硬上限 **62 天**（含首尾）
- 伴侣日程只读；邀约 accept 后双方各一份、`linkedEventId` 互指、之后改不同步
- 日两人 = 左时间轴 + 右双栏；周两人 = 上我下 TA 双轨；今日页日程仅当日
- 未绑定：**隐藏**「两人」，禁止 partner/invite API
- API 路径风格对齐 `/todos`：方法级全路径、`ApiResponse`、`@CurrentUser UserDO`、camelCase JSON
- 异常用 `BadRequestException`（与 Todo 一致）
- PowerShell：`git commit -m "msg"`，路径用双引号
- 本仓库暂无 Java 单测基建：纯逻辑用 `main` 自检；接口用手测/启动后 curl

---

## 文件结构

```
backend/.../db/migration/
  V11__create_schedule_event_and_invite.sql

backend/.../schedule/
  model/entity/ScheduleEventDO.java
  model/entity/ScheduleInviteDO.java
  model/req/ScheduleEventCreateReq.java
  model/req/ScheduleEventUpdateReq.java
  model/req/ScheduleInviteAckReq.java
  model/resp/ScheduleEventResp.java
  model/resp/ScheduleInviteResp.java
  mapper/ScheduleEventMapper.java
  mapper/ScheduleInviteMapper.java
  service/ScheduleRecurrenceExpander.java   — 纯展开逻辑 + main 自检
  service/ScheduleService.java
  controller/ScheduleController.java

front/vue3-vant-mobile/src/api/modules/schedule.ts
front/vue3-vant-mobile/src/components/schedule/
  DayTimeline.vue          — 日视图（单人/双栏）
  WeekDualTrack.vue        — 周双轨
  ScheduleEventForm.vue    — 创建/编辑表单
front/vue3-vant-mobile/src/pages/todos/index.vue   — 一级 Tab
front/vue3-vant-mobile/src/pages/index.vue         — 一级 Tab + 当日日程
front/vue3-vant-mobile/src/pages/events/index.vue  — 重定向到 /todos?view=schedule
front/vue3-vant-mobile/src/locales/{zh-CN,en-US}.json
```

---

### Task 1: Flyway 建表

**Files:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V11__create_schedule_event_and_invite.sql`

**Interfaces:**
- Produces: 表 `schedule_event`、`schedule_invite`

- [ ] **Step 1: 写迁移**

```sql
CREATE TABLE schedule_event (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    note VARCHAR(500) DEFAULT NULL,
    recurrence VARCHAR(10) NOT NULL DEFAULT 'none' COMMENT 'none/daily/weekly',
    recurrence_end_date DATE DEFAULT NULL,
    linked_event_id CHAR(36) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) DEFAULT NULL,
    update_by VARCHAR(64) DEFAULT NULL,
    INDEX idx_schedule_event_user_start (user_id, start_at),
    INDEX idx_schedule_event_linked (linked_event_id)
);

CREATE TABLE schedule_invite (
    id CHAR(36) PRIMARY KEY,
    source_event_id CHAR(36) NOT NULL,
    inviter_user_id CHAR(36) NOT NULL,
    invitee_user_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/accepted/rejected',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at DATETIME DEFAULT NULL,
    INDEX idx_schedule_invite_invitee (invitee_user_id, status),
    INDEX idx_schedule_invite_source (source_event_id)
);
```

- [ ] **Step 2: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V11__create_schedule_event_and_invite.sql"
git commit -m "feat(schedule): add Flyway V11 schedule_event and schedule_invite"
```

---

### Task 2: Entity / Mapper / Req / Resp

**Files:**
- Create: 上表「文件结构」中 entity、mapper、req、resp 全部（包名 `top.lifeassistant.schedule`）

**Interfaces:**
- Produces:
  - `ScheduleEventDO` extends `BaseDO`：`userId, title, startAt, endAt, note, recurrence, recurrenceEndDate, linkedEventId`
  - `ScheduleInviteDO`：`id, sourceEventId, inviterUserId, inviteeUserId, status, createdAt, respondedAt`（invite 可不继承 BaseDO 若字段不全；或继承并忽略多余 fill）
  - `ScheduleEventResp`：实体字段 + `instanceKey`（展开实例用）+ 可选 `pendingInviteId`
  - `ScheduleEventCreateReq`：`@NotBlank title`，`@NotNull startAt/endAt`，`recurrence` 默认 `none`，`recurrenceEndDate` 可选，`note` 可选
  - `ScheduleEventUpdateReq`：各字段可选
  - `ScheduleInviteAckReq`：`action` = `accept` | `reject`

- [ ] **Step 1: 建 DO**

`ScheduleEventDO`：`@TableName("schedule_event")`，字段如上，对齐 `TodoDO` 风格。

`ScheduleInviteDO`：`@TableName("schedule_invite")`，`@TableId` on `id`；手写 `createdAt`/`respondedAt`（无需 updateBy fill 也可）。

- [ ] **Step 2: Mapper**

```java
@Mapper
public interface ScheduleEventMapper extends BaseMapper<ScheduleEventDO> {}

@Mapper
public interface ScheduleInviteMapper extends BaseMapper<ScheduleInviteDO> {}
```

- [ ] **Step 3: Req/Resp**

`ScheduleEventResp.from(ScheduleEventDO e)`；展开时再设 `instanceKey = e.getId() + "|" + instanceStart`。
CreateReq 用 Jakarta Validation；UpdateReq 字段全 optional。

- [ ] **Step 4: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/schedule"
git commit -m "feat(schedule): add entities mappers and DTOs"
```

---

### Task 3: 重复展开器（纯逻辑 + 自检）

**Files:**
- Create: `backend/.../schedule/service/ScheduleRecurrenceExpander.java`

**Interfaces:**
- Produces:
  `public static List<ScheduleEventResp> expand(ScheduleEventDO event, LocalDateTime from, LocalDateTime to)`
  - `none`：若事件区间与 `[from,to)` 有交集则返回 1 条（start/end 用原值）
  - `daily`：从 `startAt` 起按天加，直到超过 `to` 或 `recurrenceEndDate`（当日 23:59:59）
  - `weekly`：按 7 天步进，规则同
  - 每条 resp 的 `startAt`/`endAt` 为该实例时间，`id` 仍为系列 id，`instanceKey = id + "|" + startAt`

- [ ] **Step 1: 实现 expand**

核心约束：只产出 `instanceStart < to && instanceEnd > from` 的实例；单次调用结果不超过合理上限（例如 400），超出抛 `BadRequestException("展开实例过多")`。

- [ ] **Step 2: 同文件 `main` 自检**

```java
public static void main(String[] args) {
    ScheduleEventDO e = new ScheduleEventDO();
    e.setId("e1");
    e.setTitle("standup");
    e.setStartAt(LocalDateTime.of(2026, 7, 1, 9, 0));
    e.setEndAt(LocalDateTime.of(2026, 7, 1, 9, 30));
    e.setRecurrence("daily");
    e.setRecurrenceEndDate(LocalDate.of(2026, 7, 3));
    var list = expand(e, LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 5, 0, 0));
    if (list.size() != 3) throw new AssertionError("expected 3 got " + list.size());
    System.out.println("ScheduleRecurrenceExpander OK");
}
```

- [ ] **Step 3: 跑自检**

```powershell
# 在 IDE 跑 main，或用 javac/java；预期打印 ScheduleRecurrenceExpander OK
```

- [ ] **Step 4: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/schedule/service/ScheduleRecurrenceExpander.java"
git commit -m "feat(schedule): add recurrence expander with self-check"
```

---

### Task 4: ScheduleService CRUD + 列表展开 + Partner

**Files:**
- Create: `backend/.../schedule/service/ScheduleService.java`

**Interfaces:**
- Consumes: mappers, `ScheduleRecurrenceExpander`, `UserDO.partnerId`
- Produces:
  - `listMine(user, from, to)` → 查询候选行后 expand 合并排序
  - `listPartner(user, from, to)` → 无 partner 抛「请先绑定伴侣」；只读对方
  - `create / update / delete` — 仅 owner；校验 `endAt > startAt`、recurrence 枚举、区间
  - 候选查询：`user_id = ? AND (recurrence <> 'none' OR (start_at < to AND end_at > from))`
    对 `daily/weekly` 另需：`start_at < to AND (recurrence_end_date IS NULL OR recurrence_end_date >= from.toLocalDate())`

- [ ] **Step 1: 实现 `assertRange(from, to)`**

```java
private void assertRange(LocalDateTime from, LocalDateTime to) {
    if (from == null || to == null || !to.isAfter(from)) {
        throw new BadRequestException("时间区间无效");
    }
    if (from.plusDays(62).isBefore(to)) {
        throw new BadRequestException("查询区间不能超过62天");
    }
}
```

- [ ] **Step 2: 实现 create/update/delete/listMine/listPartner**

update/delete：仅 `userId` 匹配。
delete：取消该事件上所有 `pending` invite；若有 `linkedEventId`，清空对方事件的 `linkedEventId`（不删对方）。

- [ ] **Step 3: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/schedule/service/ScheduleService.java"
git commit -m "feat(schedule): CRUD list mine/partner with expansion"
```

---

### Task 5: 邀约 invite + acknowledge

**Files:**
- Modify: `ScheduleService.java`
- （Controller 在 Task 6 暴露）

**Interfaces:**
- Produces:
  - `invite(user, eventId)` → 校验 owner、已绑定、无其它 pending；插 `schedule_invite` status=pending
  - `acknowledge(user, inviteId, action)` → 仅 invitee；reject 更新状态；accept 克隆镜像事件并互写 `linkedEventId`

- [ ] **Step 1: invite**

无 partner → `BadRequestException("请先绑定伴侣")`
非 owner → 资源不存在/无权限
已有 pending → `BadRequestException("已有待处理邀约")`

- [ ] **Step 2: acknowledge accept**

事务 `@Transactional`：更新 invite；`insert` 镜像（同 title/start/end/note，recurrence=none 的**单次快照**——取 source 当前 start/end，不复制重复规则，避免双人各一套展开地狱；若 source 是系列，邀约针对「该系列代表时段」第一版简化为复制当前存储的 start/end + recurrence 字段原样也可，**本计划采用：镜像复制 title/note/startAt/endAt，recurrence 固定 none**）；双方 `linkedEventId` 互指。

- [ ] **Step 3: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/schedule/service/ScheduleService.java"
git commit -m "feat(schedule): partner invite and acknowledge"
```

---

### Task 6: ScheduleController

**Files:**
- Create: `backend/.../schedule/controller/ScheduleController.java`

**Interfaces:**
- Produces HTTP:

| 方法 | 路径 |
|------|------|
| GET | `/schedule/events?from=&to=` |
| GET | `/schedule/events/partner?from=&to=` |
| POST | `/schedule/events` |
| PATCH | `/schedule/events/{id}` |
| DELETE | `/schedule/events/{id}` |
| POST | `/schedule/events/{id}/invite` |
| POST | `/schedule/invites/{id}/acknowledge` |

`from`/`to` 用 `ISO_LOCAL_DATE_TIME` 或 `Instant` 字符串，与项目 Jackson 配置一致（优先 `LocalDateTime` 查询参数）。

- [ ] **Step 1: 写 Controller**（对齐 `TodoController`：`@Tag` `@RestController` `@RequiredArgsConstructor`）

- [ ] **Step 2: 启动后端，手测**

```powershell
# 登录后带 token：
# GET /schedule/events?from=2026-07-19T00:00:00&to=2026-07-20T00:00:00
# POST /schedule/events { "title":"x","startAt":"...","endAt":"...","recurrence":"none" }
```

- [ ] **Step 3: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/schedule/controller/ScheduleController.java"
git commit -m "feat(schedule): expose schedule HTTP APIs"
```

---

### Task 7: 前端 schedule API

**Files:**
- Create: `front/vue3-vant-mobile/src/api/modules/schedule.ts`
- 可选：弃用或标注 `events.ts` 为 legacy（Task 10 处理路由）

**Interfaces:**
- Produces camelCase 类型与函数：`fetchMyScheduleEvents`, `fetchPartnerScheduleEvents`, `createScheduleEvent`, `updateScheduleEvent`, `deleteScheduleEvent`, `inviteScheduleEvent`, `acknowledgeScheduleInvite`

```typescript
export interface ScheduleEventItem {
  id: string
  userId: string
  title: string
  startAt: string
  endAt: string
  note?: string | null
  recurrence: 'none' | 'daily' | 'weekly'
  recurrenceEndDate?: string | null
  linkedEventId?: string | null
  instanceKey: string
  pendingInviteId?: string | null
  createdAt?: string
}
```

- [ ] **Step 1: 实现 API 模块**（`request` 同 `todos.ts`）

- [ ] **Step 2: Commit**

```powershell
git add "front/vue3-vant-mobile/src/api/modules/schedule.ts"
git commit -m "feat(schedule): add frontend schedule API module"
```

---

### Task 8: DayTimeline + ScheduleEventForm

**Files:**
- Create: `front/.../components/schedule/DayTimeline.vue`
- Create: `front/.../components/schedule/ScheduleEventForm.vue`
- Modify: `locales/zh-CN.json`, `en-US.json`（日程相关文案）

**Interfaces:**
- `DayTimeline` props:
  `date: string` (yyyy-MM-dd),
  `mode: 'self' | 'dual'`,
  `mine: ScheduleEventItem[]`,
  `partner: ScheduleEventItem[]`
  emits: `create-at`, `open-event`
- 布局：左小时 0–23（可默认滚动到 8）；`dual` 时右双栏「我|TA」
- `ScheduleEventForm`：title、start、end、note、recurrence、recurrenceEndDate；Vant `van-field` + `van-popup` 时间选择

- [ ] **Step 1: 实现 Form**

- [ ] **Step 2: 实现 DayTimeline**（事件块按分钟定位：`top = (startMinutes/1440)*height`）

- [ ] **Step 3: Commit**

```powershell
git add "front/vue3-vant-mobile/src/components/schedule" "front/vue3-vant-mobile/src/locales/zh-CN.json" "front/vue3-vant-mobile/src/locales/en-US.json"
git commit -m "feat(schedule): day timeline and event form"
```

---

### Task 9: WeekDualTrack

**Files:**
- Create: `front/.../components/schedule/WeekDualTrack.vue`

**Interfaces:**
- props: `weekStart: string`, `mode: 'self' | 'dual'`, `mine`, `partner`
- emits: `select-day(date: string)`
- UI：上轨我、下轨 TA（`self` 时只上轨）；每天显示当日事件标题摘要

- [ ] **Step 1: 实现组件**

- [ ] **Step 2: Commit**

```powershell
git add "front/vue3-vant-mobile/src/components/schedule/WeekDualTrack.vue"
git commit -m "feat(schedule): week dual-track view"
```

---

### Task 10: 挂载 `/todos` 与 `/` + 清理 events

**Files:**
- Modify: `front/.../pages/todos/index.vue`
- Modify: `front/.../pages/index.vue`
- Modify: `front/.../pages/events/index.vue` → `router.replace('/todos?view=schedule')` 或静态提示并链过去
- 用 `localStorage` 键 `todos.primaryTab` 记住待办|日程

**行为：**
- `/todos`：一级 `van-tabs` 待办|日程；日程内 `日|周` +（有 partner 时）`只看我|两人`；拉 mine / partner；点周某天切到日
- `/`：一级 待办|日程；日程固定今天 + DayTimeline
- 创建/编辑/删除/邀约走 Form + API；pending 角标可选：列表里带 `pendingInviteId` 时由后端在 list 填充（Task 4/5 可附带查 invite）

- [ ] **Step 1: 改 todos 页**

- [ ] **Step 2: 改今日页**

- [ ] **Step 3: events 重定向**

- [ ] **Step 4: 端到端手测 checklist（对照 spec §测试要点）**

1. 创建 daily/weekly，日/周展开正确
2. 绑定后日双栏、周双轨
3. 邀约 accept/reject
4. 未绑定无两人、invite 失败
5. 今日仅当天

- [ ] **Step 5: Commit**

```powershell
git add "front/vue3-vant-mobile/src/pages/todos/index.vue" "front/vue3-vant-mobile/src/pages/index.vue" "front/vue3-vant-mobile/src/pages/events/index.vue"
git commit -m "feat(schedule): wire todo and today dual views"
```

---

## Spec 覆盖自检

| Spec 项 | Task |
|---------|------|
| 独立 Schedule 域 + V11 | 1–2 |
| 服务端展开 none/daily/weekly | 3–4 |
| CRUD + partner 只读 | 4, 6 |
| 邀约 + 各持一份不同步 | 5 |
| 日布局 A / 周 W2 / 今日仅当日 | 8–10 |
| 未绑定隐藏两人 | 10 |
| 旧 /events | 10 |
| 测试要点 | 3 自检 + 10 手测 |

**镜像复制策略（已钉死）：** accept 时镜像 `recurrence=none`，只复制当时的 title/note/startAt/endAt，避免双系列展开。
