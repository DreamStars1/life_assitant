# Partner Info + Editable Together-Since Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 `partner_info`（共享 `partner_since` + `points_balance` 缓存），支持可编辑「在一起」日期，看板按含起始日公式计天；积分读缓存、写流水时同步更新余额。

**Architecture:** 保留 `user.partner_id` 与 `partner_points` 流水。一对情侣一行 `partner_info`（`user_a_id`/`user_b_id` 字典序）。绑定建行、解绑/删号删行并清流水；`PUT /identity/partner-since` 改日期；`UserPublicResp.partnerSince` 从关系表组装。前端看板+详情可编辑。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + Flyway / Vue 3 + Vant + Pinia

**Spec:** `docs/superpowers/specs/2026-08-07-partner-info-meet-date-design.md`

## Global Constraints

- UI 文案保持「在一起」（`dashboard.anniversary`），不改成「认识日期」
- 天数公式：`(today - partnerSince).toDays() + 1`（当天 = 第 1 天）；无 `partnerSince` 显示 0
- `partner_since` 不可晚于服务端当天；未绑定不可改
- 任一方可改，无需对方确认
- 保留 `user.partner_id` 与 `partner_points` 流水表
- 解绑/删号：删 `partner_info` + 删双方积分流水 + 清 `partner_id`（共享记录删除逻辑保持现状）
- 迁移回填：双向绑定对；`partner_since = DATE(LEAST(created_at))`；`points_balance = SUM(流水)`
- Flyway 下一版本号：`V18`（当前最高 `V17`）
- PowerShell：git commit 用 `-m "..."`，路径用双引号
- 仅提交本任务相关文件；勿把工作区其它脏文件打进 commit
- Java 纯逻辑测试风格对齐 `HealthEnergyRulesTest`（无 Spring 容器）

## File map

| Path | Role |
|------|------|
| `.../db/migration/V18__create_partner_info.sql` | 建表 + 回填 |
| `.../partner/model/entity/PartnerInfoDO.java` | 实体 |
| `.../partner/mapper/PartnerInfoMapper.java` | Mapper |
| `.../partner/service/PartnerInfoRules.java` | 纯函数：id 排序、日期校验 |
| `.../partner/service/PartnerInfoService.java` | 创建/查/改 since/改余额/删 |
| `.../partner/.../PartnerInfoRulesTest.java` | 单元测试 |
| `IdentityController` / `UserServiceImpl` / `UserController` | 绑定、解绑、删号、PUT、me 组装 |
| `UserPublicResp` | `partnerSince` |
| `PartnerPointsService` | 读缓存、写流水+余额 |
| 前端 `user` store / API / dashboard / detail | 映射、编辑、天数 |

---

### Task 1: Flyway `partner_info` + 回填

**Files:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V18__create_partner_info.sql`

**Produces:** 表 `partner_info`；已绑定双向对有行

- [ ] **Step 1: 写迁移 SQL**

```sql
CREATE TABLE partner_info (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_a_id CHAR(36) NOT NULL COMMENT '字典序较小的用户 ID',
    user_b_id CHAR(36) NOT NULL COMMENT '字典序较大的用户 ID',
    partner_since DATE NOT NULL COMMENT '在一起起点',
    points_balance INT NOT NULL DEFAULT 0 COMMENT '积分余额缓存',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_partner_pair (user_a_id, user_b_id),
    KEY idx_user_a (user_a_id),
    KEY idx_user_b (user_b_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='伴侣关系属性';

-- 双向绑定对回填（单向脏数据跳过）
INSERT INTO partner_info (id, user_a_id, user_b_id, partner_since, points_balance, created_at, update_time)
SELECT
    UUID(),
    IF(a.id < b.id, a.id, b.id),
    IF(a.id < b.id, b.id, a.id),
    DATE(LEAST(a.created_at, b.created_at)),
    COALESCE((
        SELECT SUM(p.points_change)
        FROM partner_points p
        WHERE p.created_by IN (a.id, b.id)
    ), 0),
    NOW(),
    NOW()
FROM user a
INNER JOIN user b ON a.partner_id = b.id AND b.partner_id = a.id
WHERE a.id < b.id;
```

- [ ] **Step 2: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V18__create_partner_info.sql"
git commit -m "feat(db): add partner_info table and backfill existing couples"
```

---

### Task 2: PartnerInfo 实体、规则、Service、单元测试

**Files:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/model/entity/PartnerInfoDO.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/mapper/PartnerInfoMapper.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerInfoRules.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerInfoService.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/test/java/top/lifeassistant/partner/service/PartnerInfoRulesTest.java`

**Interfaces:**
- Produces:
  - `PartnerInfoRules.orderedPair(String u1, String u2)` → `String[]{a,b}` with `a.compareTo(b) < 0`
  - `PartnerInfoRules.requireNotFuture(LocalDate since, LocalDate today)` throws `BadRequestException` if `since.isAfter(today)`
  - `PartnerInfoService.createForPair(String userId, String partnerId, LocalDate since)` → `PartnerInfoDO` balance 0
  - `PartnerInfoService.requireByUser(String userId, String partnerId)` → `PartnerInfoDO` or BadRequest
  - `PartnerInfoService.findByUser(String userId, String partnerId)` → `PartnerInfoDO|null`
  - `PartnerInfoService.updatePartnerSince(String userId, String partnerId, LocalDate since)`
  - `PartnerInfoService.addPointsBalance(String userId, String partnerId, int delta)`
  - `PartnerInfoService.deleteForPair(String userId, String partnerId)`

- [ ] **Step 1: 写失败测试 `PartnerInfoRulesTest`**

```java
package top.lifeassistant.partner.service;

import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BadRequestException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PartnerInfoRulesTest {

    @Test
    void orderedPair_sortsLexicographically() {
        String[] p = PartnerInfoRules.orderedPair("b-uuid", "a-uuid");
        assertEquals("a-uuid", p[0]);
        assertEquals("b-uuid", p[1]);
        String[] same = PartnerInfoRules.orderedPair("a-uuid", "b-uuid");
        assertArrayEquals(p, same);
    }

    @Test
    void requireNotFuture_rejectsTomorrow() {
        LocalDate today = LocalDate.of(2026, 8, 7);
        assertThrows(BadRequestException.class,
            () -> PartnerInfoRules.requireNotFuture(today.plusDays(1), today));
    }

    @Test
    void requireNotFuture_allowsToday() {
        LocalDate today = LocalDate.of(2026, 8, 7);
        assertDoesNotThrow(() -> PartnerInfoRules.requireNotFuture(today, today));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

```powershell
cd backend/lifeassistant; mvn -pl lifeassistant-system -am test "-Dtest=PartnerInfoRulesTest" -q
```

Expected: 编译失败或测试失败（类不存在）

- [ ] **Step 3: 实现 Rules + DO + Mapper + Service**

`PartnerInfoDO`：字段 `id, userAId, userBId, partnerSince, pointsBalance, createdAt, updateTime`；`@TableName("partner_info")`；`@TableField` 映射 snake_case。不继承 `BaseDO`（无 create_by）。

`PartnerInfoMapper`：`extends BaseMapper<PartnerInfoDO>`。

`PartnerInfoRules`：如上两个静态方法。

`PartnerInfoService`：
- `ordered` 后 `LambdaQueryWrapper` eq a/b 查找
- `createForPair`：UUID id、since、balance=0、insert
- `updatePartnerSince`：校验日期后 update
- `addPointsBalance`：读出行后 `setPointsBalance(get + delta)` update（ponytail: 无并发锁，两人 App 可接受）
- `deleteForPair`：按 pair delete

- [ ] **Step 4: 跑测试通过**

```powershell
cd backend/lifeassistant; mvn -pl lifeassistant-system -am test "-Dtest=PartnerInfoRulesTest" -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```powershell
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/model/entity/PartnerInfoDO.java" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/mapper/PartnerInfoMapper.java" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerInfoRules.java" "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/partner/service/PartnerInfoService.java" "backend/lifeassistant/lifeassistant-system/src/test/java/top/lifeassistant/partner/service/PartnerInfoRulesTest.java"
git commit -m "feat(partner): partner_info entity service and ordering rules"
```

---

### Task 3: 绑定 / 解绑 / 删号 / PUT partner-since / 用户响应

**Files:**
- Modify: `.../user/model/resp/UserPublicResp.java`
- Modify: `.../identity/controller/IdentityController.java`
- Modify: `.../system/service/impl/UserServiceImpl.java`
- Modify: `.../system/service/UserService.java`（若需签名调整）
- Modify: `.../user/controller/UserController.java`
- Create: `.../partner/model/req/PartnerSinceUpdateReq.java`（或 identity 包下）

**Consumes:** Task 2 `PartnerInfoService` / `PartnerInfoRules`

**Produces:**
- `UserPublicResp.partnerSince: LocalDate`
- `PUT /identity/partner-since` body `{ "partnerSince": "yyyy-MM-dd" }`
- `GET /users/me`（及 bind 返回）带 `partnerSince`

- [ ] **Step 1: 扩展 `UserPublicResp`**

增加 `private LocalDate partnerSince;`。新增重载：

```java
public static UserPublicResp from(UserDO user, LocalDate partnerSince) {
    UserPublicResp r = from(user);
    r.setPartnerSince(partnerSince);
    return r;
}
```

保留原 `from(user)`（partnerSince null）。

- [ ] **Step 2: 请求体**

```java
@Data
public class PartnerSinceUpdateReq {
    @NotNull
    private LocalDate partnerSince;
}
```

- [ ] **Step 3: IdentityController**

注入 `PartnerInfoService`。

`bindPartner`：双向 update 后调用 `partnerInfoService.createForPair(me.getId(), partnerId, LocalDate.now())`；返回 `UserPublicResp.from(me, LocalDate.now())`。

新增：

```java
@PutMapping("/identity/partner-since")
@Transactional
public ApiResponse<UserPublicResp> updatePartnerSince(
    @CurrentUser UserDO me, @Valid @RequestBody PartnerSinceUpdateReq req) {
    if (me.getPartnerId() == null) throw new BadRequestException("尚未绑定伴侣");
    PartnerInfoRules.requireNotFuture(req.getPartnerSince(), LocalDate.now());
    partnerInfoService.updatePartnerSince(me.getId(), me.getPartnerId(), req.getPartnerSince());
    return ApiResponse.ok(UserPublicResp.from(me, req.getPartnerSince()));
}
```

- [ ] **Step 4: UserServiceImpl.unbindPartner / delete**

注入 `PartnerInfoService`、`PartnerPointsService`。

`unbindPartner`：在清 `partner_id` 前：
1. `partnerInfoService.deleteForPair(me.getId(), partner.getId())`
2. `partnerPointsService.deleteByUsers(me.getId(), partner.getId())`
3. 现有 shared_record 删除
4. 清双方 partner_id

`delete`：若有 partner，同样 deleteForPair + deleteByUsers，再清对方 partner_id，再删自己。

- [ ] **Step 5: UserController.me（及 updateMe 返回）**

注入 `PartnerInfoService`。组装：

```java
LocalDate since = null;
if (user.getPartnerId() != null) {
    var info = partnerInfoService.findByUser(user.getId(), user.getPartnerId());
    if (info != null) since = info.getPartnerSince();
}
return ApiResponse.ok(UserPublicResp.from(user, since));
```

`getUserById` 可不带 since（公开资料非必须）；`me` / bind / PUT 必须带。

- [ ] **Step 6: 编译**

```powershell
cd backend/lifeassistant; mvn -pl lifeassistant-system -am compile -q
```

Expected: SUCCESS

- [ ] **Step 7: Commit**

```powershell
git commit -m "feat(partner): wire partner_since into bind unbind delete and identity API"
```

（仅 add 本任务改动文件）

---

### Task 4: 积分余额读缓存、写时双写

**Files:**
- Modify: `.../partner/service/PartnerPointsService.java`

**Consumes:** `PartnerInfoService.requireByUser` / `addPointsBalance`

- [ ] **Step 1: 改 `getBalance`**

```java
public Integer getBalance(String userId) {
    String partnerId = getPartnerId(userId);
    PartnerInfoDO info = partnerInfoService.requireByUser(userId, partnerId);
    return info.getPointsBalance() != null ? info.getPointsBalance() : 0;
}
```

- [ ] **Step 2: 改 `addPoints`**

在现有 insert 流水后（同方法已有 `@Transactional`）：

```java
partnerInfoService.addPointsBalance(userId, getPartnerId(userId), pointsChange);
```

注意：`getPartnerId` 不要调两次若中间状态会变；先取 `partnerId` 局部变量。

- [ ] **Step 3: 编译**

```powershell
cd backend/lifeassistant; mvn -pl lifeassistant-system -am compile -q
```

- [ ] **Step 4: Commit**

```powershell
git commit -m "feat(partner): read points balance from partner_info cache"
```

---

### Task 5: 前端 store、API、看板与详情编辑

**Files:**
- Modify: `front/vue3-vant-mobile/src/stores/modules/user.ts`
- Modify: `front/vue3-vant-mobile/src/api/user.ts`（或新建 `api/modules/partner-identity.ts`）
- Modify: `front/vue3-vant-mobile/src/pages/partner/dashboard/index.vue`
- Modify: `front/vue3-vant-mobile/src/pages/partner/detail.vue`
- Modify: `front/vue3-vant-mobile/src/locales/zh-CN.json` / `en-US.json`（仅必要时补 toast 文案）

**Consumes:** `PUT /identity/partner-since`；`UserState.partnerSince`

- [ ] **Step 1: Store 映射 `partnerSince`**

`UserState` 增加 `partnerSince?: string | null`。`setInfo`：

```ts
partnerSince: (raw.partnerSince || raw.partner_since) as string | null | undefined,
```

- [ ] **Step 2: API**

```ts
export function updatePartnerSince(partnerSince: string) {
  return request.put('/identity/partner-since', { partnerSince })
}
```

- [ ] **Step 3: 看板天数**

```ts
const daysTogether = computed(() => {
  const since = userStore.userInfo.partnerSince
  if (!since) return 0
  const start = new Date(since.slice(0, 10) + 'T00:00:00')
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const diff = Math.floor((today.getTime() - start.getTime()) / 86400000)
  return diff < 0 ? 0 : diff + 1
})
```

- [ ] **Step 4: 看板卡片可编辑**

纪念日卡片：去掉跳转 `/share`；点击打开 `van-calendar`（或 `van-date-picker`），确认后调 `updatePartnerSince`，成功 toast，再 `userStore.info()`。`max-date` = 今天。

- [ ] **Step 5: 伴侣详情页**

已绑定态增加：展示当前 `partnerSince`；「修改在一起日期」→ 同样日历 + API + `userStore.info()`。

- [ ] **Step 6: 手动冒烟清单（agent 在报告中写明；若有前端测试框架可加纯函数测天数，非必须）**

1. 已绑定用户刷新后 `partnerSince` 有值（迁移后）
2. 改天数正确（当天 since → 1）
3. 选未来日期被拒
4. 积分加减后余额仍正确

- [ ] **Step 7: Commit**

```powershell
git commit -m "feat(front): editable together-since on dashboard and partner detail"
```

---

## Spec coverage checklist

| Spec 项 | Task |
|---------|------|
| `partner_info` 表 + 唯一序 | 1 |
| 迁移回填 since/balance | 1 |
| Rules/Service | 2 |
| 绑定建行 | 3 |
| 解绑/删号清关系+流水 | 3 |
| PUT partner-since | 3 |
| UserPublicResp.partnerSince | 3 |
| 余额缓存读写 | 4 |
| 天数 +1 / 双入口编辑 | 5 |
| 文案「在一起」 | 5（不改 key） |

## Self-review notes

- 无 TBD；`V18` 与现网 `V17` 对齐
- `getUserById` 故意不强制 since（减少无关查询）
- 积分 `addPointsBalance` 用读改写，注释天花板
