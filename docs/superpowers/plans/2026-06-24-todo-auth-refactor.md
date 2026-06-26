# 后端授权校验重构：OwnerValidator 组件 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 抽取"查 → 判空 → 鉴权"重复模式为可复用组件，消除 3 个 Service 中 11 处重复代码，统一异常语义。

**架构:** 新增 `OwnedEntity` 接口让实体声明所有者字段，新增 `OwnerValidator` Spring Bean 提供 `findAndCheck`/`requireOwner` 泛型方法。Service 注入后一行调用替代 5 行模板。不改变继承链，新老 Service 均可立即使用。

**涉及 Service:** TodoService (5处) · TodoAckTemplateService (3处) · SharedRecordService (3处)

---

### 文件总览

| 文件 | 操作 |
|---|---|
| `common/.../base/model/entity/OwnedEntity.java` | **创建** |
| `common/.../base/component/OwnerValidator.java` | **创建** |
| `system/.../todo/model/entity/TodoDO.java` | 修改 |
| `system/.../todo/model/entity/TodoAckTemplateDO.java` | 修改 |
| `system/.../sharedrecord/model/entity/SharedRecordDO.java` | 修改 |
| `system/.../todo/service/TodoService.java` | 重构 |
| `system/.../todo/service/TodoAckTemplateService.java` | 重构 |
| `system/.../sharedrecord/service/SharedRecordService.java` | 重构 |
| `system/.../todo/mapper/TodoMapper.java` | 删除死代码 |

---

### Task 1: 创建 OwnedEntity 接口

**文件:**
- Create: `backend/lifeassistant/lifeassistant-common/src/main/java/top/lifeassistant/common/base/model/entity/OwnedEntity.java`

- [ ] **步骤 1: 创建 OwnedEntity 接口**

```java
package top.lifeassistant.common.base.model.entity;

/**
 * 声明实体"所有者"的接口，供 OwnerValidator 进行所有权校验。
 *
 * <p>实现类只需返回所有者的用户 ID（如 userId、createdBy 等字段的值）。</p>
 */
public interface OwnedEntity {

    /** 返回所有者的用户 ID */
    String getOwnerId();
}
```

- [ ] **步骤 2: 提交**

```bash
git add backend/lifeassistant/lifeassistant-common/src/main/java/top/lifeassistant/common/base/model/entity/OwnedEntity.java
git commit -m "feat: add OwnedEntity interface for ownership validation"
```

---

### Task 2: 创建 OwnerValidator 组件

**文件:**
- Create: `backend/lifeassistant/lifeassistant-common/src/main/java/top/lifeassistant/common/base/component/OwnerValidator.java`

- [ ] **步骤 1: 创建 OwnerValidator**

```java
package top.lifeassistant.common.base.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.continew.starter.core.exception.BusinessException;
import top.lifeassistant.common.base.model.entity.OwnedEntity;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 轻量所有者校验组件。
 *
 * <p>消除"查 → 判空 → 鉴权"重复模式。支持自定义谓词校验和快捷所有者校验。</p>
 */
@Component
@RequiredArgsConstructor
public class OwnerValidator {

    /**
     * 查 → 判空 → 谓词校验，不通过统一抛 BusinessException。
     *
     * @param fetcher   实体获取函数（通常是 mapper::selectById）
     * @param errMsg    校验失败时返回的消息（不通过时不区分"不存在"还是"无权限"）
     * @param predicate 校验逻辑：接收实体，返回 true=通过
     * @param <T>       实体类型
     * @return 校验通过的实体
     */
    public <T> T findAndCheck(Supplier<T> fetcher, String errMsg, Predicate<T> predicate) {
        T entity = fetcher.get();
        if (entity == null || !predicate.test(entity)) {
            throw new BusinessException(errMsg);
        }
        return entity;
    }

    /**
     * 快捷方法：查 → 判空 → 校验所有者身份。
     * 不存在或非本人，统抛 BusinessException("资源不存在")。
     */
    public <T extends OwnedEntity> T requireOwner(Supplier<T> fetcher, String ownerId) {
        return findAndCheck(fetcher, "资源不存在", e -> ownerId.equals(e.getOwnerId()));
    }
}
```

- [ ] **步骤 2: 提交**

```bash
git add backend/lifeassistant/lifeassistant-common/src/main/java/top/lifeassistant/common/base/component/OwnerValidator.java
git commit -m "feat: add OwnerValidator component for ownership checks"
```

---

### Task 3: 三个 Entity 实现 OwnedEntity

**文件:**
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/model/entity/TodoDO.java`
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/model/entity/TodoAckTemplateDO.java`
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedrecord/model/entity/SharedRecordDO.java`

- [ ] **步骤 1: TodoDO 实现 OwnedEntity**

```java
// 在类声明上加 implements OwnedEntity
public class TodoDO extends BaseDO implements OwnedEntity {

    // 已有字段不变，新增方法：
    @Override
    public String getOwnerId() { return userId; }
```

- [ ] **步骤 2: TodoAckTemplateDO 实现 OwnedEntity**

```java
public class TodoAckTemplateDO extends BaseDO implements OwnedEntity {

    @Override
    public String getOwnerId() { return userId; }
```

- [ ] **步骤 3: SharedRecordDO 实现 OwnedEntity**

```java
public class SharedRecordDO extends BaseDO implements OwnedEntity {

    @Override
    public String getOwnerId() { return createdBy; }
```

- [ ] **步骤 4: 提交**

```bash
git add backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/model/entity/TodoDO.java backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/model/entity/TodoAckTemplateDO.java backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedrecord/model/entity/SharedRecordDO.java
git commit -m "feat: implement OwnedEntity on TodoDO, TodoAckTemplateDO, SharedRecordDO"
```

---

### Task 4: 重构 TodoService — 5 处替换

**文件:**
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/service/TodoService.java`

注入 OwnerValidator，5 处查+鉴权替换为单行调用。此外修正异常类型：
- `update`/`delete` 的 `BadRequestException("只能修改/删除自己的待办")` → `BusinessException("资源不存在")`

- [ ] **步骤 1: 注入 OwnerValidator + 重写 TodoService**

```java
package top.lifeassistant.todo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.common.base.component.OwnerValidator;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.todo.mapper.TodoMapper;
import top.lifeassistant.todo.model.entity.TodoDO;
import top.lifeassistant.todo.model.req.TodoCreateReq;
import top.lifeassistant.todo.model.req.TodoUpdateReq;
import top.lifeassistant.todo.model.resp.TodoResp;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoMapper mapper;
    private final OwnerValidator ownerValidator;

    public TodoResp create(UserDO user, TodoCreateReq req) {
        TodoDO todo = new TodoDO();
        todo.setUserId(user.getId());
        todo.setTitle(req.getTitle());
        todo.setDescription(req.getDescription());
        todo.setPriority(req.getPriority());
        todo.setIsCompleted(false);
        todo.setDueDate(req.getDueDate());
        todo.setCreatedAt(LocalDateTime.now());
        todo.setUpdateTime(LocalDateTime.now());

        if (req.getAssignedTo() != null) {
            if (user.getPartnerId() == null) {
                throw new BadRequestException("请先绑定伴侣");
            }
            todo.setAssignedTo(req.getAssignedTo());
            todo.setAssignedBy(user.getId());
            todo.setAckStatus("unconfirmed");
        } else {
            todo.setAckStatus("none");
        }

        mapper.insert(todo);
        return TodoResp.from(todo);
    }

    public List<TodoResp> list(UserDO user, Boolean isCompleted, String priority,
                                LocalDateTime startDueDate, LocalDateTime endDueDate) {
        LambdaQueryWrapper<TodoDO> qw = new LambdaQueryWrapper<>();
        qw.and(w -> w.eq(TodoDO::getUserId, user.getId())
                     .or().eq(TodoDO::getAssignedTo, user.getId()));
        if (isCompleted != null) qw.eq(TodoDO::getIsCompleted, isCompleted);
        if (priority != null) qw.eq(TodoDO::getPriority, priority);
        if (startDueDate != null) qw.ge(TodoDO::getDueDate, startDueDate);
        if (endDueDate != null) qw.le(TodoDO::getDueDate, endDueDate);
        qw.orderByDesc(TodoDO::getCreatedAt);
        return mapper.selectList(qw).stream().map(TodoResp::from).toList();
    }

    public TodoResp getById(UserDO user, String id) {
        TodoDO todo = ownerValidator.findAndCheck(
            () -> mapper.selectById(id), "资源不存在",
            t -> user.getId().equals(t.getUserId()) || user.getId().equals(t.getAssignedTo()));
        return TodoResp.from(todo);
    }

    public TodoResp update(UserDO user, String id, TodoUpdateReq req) {
        TodoDO todo = ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        if (req.getTitle() != null) todo.setTitle(req.getTitle());
        if (req.getDescription() != null) todo.setDescription(req.getDescription());
        if (req.getPriority() != null) todo.setPriority(req.getPriority());
        if (req.getDueDate() != null) todo.setDueDate(req.getDueDate());
        todo.setUpdateTime(LocalDateTime.now());
        mapper.updateById(todo);
        return TodoResp.from(todo);
    }

    public void delete(UserDO user, String id) {
        ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        mapper.deleteById(id);
    }

    public TodoResp toggleComplete(UserDO user, String id) {
        TodoDO todo = ownerValidator.findAndCheck(
            () -> mapper.selectById(id), "资源不存在",
            t -> user.getId().equals(t.getUserId()) || user.getId().equals(t.getAssignedTo()));
        todo.setIsCompleted(!Boolean.TRUE.equals(todo.getIsCompleted()));
        todo.setCompletedAt(Boolean.TRUE.equals(todo.getIsCompleted()) ? LocalDateTime.now() : null);
        todo.setUpdateTime(LocalDateTime.now());
        mapper.updateById(todo);
        return TodoResp.from(todo);
    }

    public TodoResp acknowledge(UserDO user, String id, String message) {
        TodoDO todo = ownerValidator.findAndCheck(
            () -> mapper.selectById(id), "只有被指派者才能确认",
            t -> user.getId().equals(t.getAssignedTo()));
        if (!"unconfirmed".equals(todo.getAckStatus())) {
            throw new BadRequestException("待办无需确认或已确认");
        }
        todo.setAckStatus("confirmed");
        todo.setAckMessage(message);
        todo.setUpdateTime(LocalDateTime.now());
        mapper.updateById(todo);
        return TodoResp.from(todo);
    }

    public List<TodoResp> getUpcoming(UserDO user) {
        List<TodoDO> todos = mapper.selectIncompleteByDueDate(user.getId());
        return todos.stream().limit(3).map(TodoResp::from).toList();
    }
}
```

关键变化说明：
- `getById` / `toggleComplete`: 所有者 OR 被指派者 → `findAndCheck` + 自定义谓词
- `update` / `delete`: 仅所有者 → `requireOwner`（原 `BadRequestException` 改为 `BusinessException`）
- `acknowledge`: 仅被指派者 → `findAndCheck` + 自定义谓词
- `update`/`delete`/`getById`/`toggleComplete` 的错误消息统一为 `"资源不存在"`
- `acknowledge` 保持"只有被指派者才能确认"（安全含义不同）

- [ ] **步骤 2: 提交**

```bash
git add backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/service/TodoService.java
git commit -m "refactor: use OwnerValidator in TodoService, unify error messages"
```

---

### Task 5: 重构 TodoAckTemplateService — 3 处替换 + NPE 修复

**文件:**
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/service/TodoAckTemplateService.java`

- [ ] **步骤 1: 注入 OwnerValidator + 重写 TodoAckTemplateService**

```java
package top.lifeassistant.todo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.common.base.component.OwnerValidator;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.todo.mapper.TodoAckTemplateMapper;
import top.lifeassistant.todo.model.entity.TodoAckTemplateDO;
import top.lifeassistant.todo.model.resp.TodoAckTemplateResp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class TodoAckTemplateService {

    private static final int MAX_TEMPLATES = 5;
    private static final List<String> DEFAULT_TEMPLATES = List.of("收到", "朕知道了", "臣遵旨");

    private final TodoAckTemplateMapper mapper;
    private final OwnerValidator ownerValidator;

    public List<TodoAckTemplateResp> listByUser(UserDO user) {
        List<TodoAckTemplateDO> templates = mapper.selectList(
            new LambdaQueryWrapper<TodoAckTemplateDO>()
                .eq(TodoAckTemplateDO::getUserId, user.getId())
                .orderByAsc(TodoAckTemplateDO::getSortOrder));

        if (templates.isEmpty()) {
            templates = initDefaults(user);
        }

        return templates.stream().map(TodoAckTemplateResp::from).toList();
    }

    private List<TodoAckTemplateDO> initDefaults(UserDO user) {
        List<TodoAckTemplateDO> defaults = IntStream.range(0, DEFAULT_TEMPLATES.size())
            .mapToObj(i -> {
                TodoAckTemplateDO t = new TodoAckTemplateDO();
                t.setUserId(user.getId());
                t.setContent(DEFAULT_TEMPLATES.get(i));
                t.setSortOrder(i);
                t.setCreatedAt(LocalDateTime.now());
                t.setUpdateTime(LocalDateTime.now());
                mapper.insert(t);
                return t;
            })
            .toList();
        return defaults;
    }

    public TodoAckTemplateResp create(UserDO user, String content) {
        long count = mapper.selectCount(
            new LambdaQueryWrapper<TodoAckTemplateDO>().eq(TodoAckTemplateDO::getUserId, user.getId()));
        if (count >= MAX_TEMPLATES) {
            throw new BadRequestException("模板最多 " + MAX_TEMPLATES + " 条");
        }

        // 修复：selectOne 在无数据时返回 null，Optional 兜底
        int maxOrder = Optional.ofNullable(
            mapper.selectOne(new LambdaQueryWrapper<TodoAckTemplateDO>()
                .eq(TodoAckTemplateDO::getUserId, user.getId())
                .orderByDesc(TodoAckTemplateDO::getSortOrder)
                .last("LIMIT 1")))
            .map(TodoAckTemplateDO::getSortOrder)
            .orElse(-1);

        TodoAckTemplateDO template = new TodoAckTemplateDO();
        template.setUserId(user.getId());
        template.setContent(content);
        template.setSortOrder(maxOrder + 1);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        mapper.insert(template);
        return TodoAckTemplateResp.from(template);
    }

    public TodoAckTemplateResp update(UserDO user, String id, String content) {
        TodoAckTemplateDO template = ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        template.setContent(content);
        template.setUpdateTime(LocalDateTime.now());
        mapper.updateById(template);
        return TodoAckTemplateResp.from(template);
    }

    public void delete(UserDO user, String id) {
        ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        mapper.deleteById(id);
    }

    public void reorder(UserDO user, List<String> ids) {
        for (int i = 0; i < ids.size(); i++) {
            final int order = i;
            TodoAckTemplateDO template = ownerValidator.requireOwner(
                () -> mapper.selectById(ids.get(order)), user.getId());
            template.setSortOrder(order);
            template.setUpdateTime(LocalDateTime.now());
            mapper.updateById(template);
        }
    }
}
```

- [ ] **步骤 2: 提交**

```bash
git add backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/service/TodoAckTemplateService.java
git commit -m "refactor: use OwnerValidator in TodoAckTemplateService, fix NPE in create()"
```

---

### Task 6: 重构 SharedRecordService — 3 处替换 + 异常统一

**文件:**
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedrecord/service/SharedRecordService.java`

- [ ] **步骤 1: 注入 OwnerValidator + 重写 SharedRecordService**

```java
package top.lifeassistant.sharedrecord.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.common.base.component.OwnerValidator;
import top.lifeassistant.sharedrecord.mapper.SharedRecordMapper;
import top.lifeassistant.sharedrecord.model.entity.SharedRecordDO;
import top.lifeassistant.sharedrecord.model.req.SharedRecordCreateReq;
import top.lifeassistant.sharedrecord.model.req.SharedRecordUpdateReq;
import top.lifeassistant.sharedrecord.model.resp.SharedRecordResp;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SharedRecordService {

    private final SharedRecordMapper mapper;
    private final OwnerValidator ownerValidator;

    private void requirePartner(UserDO user) {
        if (user.getPartnerId() == null) {
            throw new BadRequestException("请先绑定伴侣");
        }
    }

    public SharedRecordResp create(UserDO user, SharedRecordCreateReq req) {
        requirePartner(user);
        SharedRecordDO record = new SharedRecordDO();
        record.setCreatedBy(user.getId());
        record.setTitle(req.getTitle());
        record.setContent(req.getContent());
        record.setOccurredAt(req.getOccurredAt() != null ? req.getOccurredAt() : LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        mapper.insert(record);
        return SharedRecordResp.from(record);
    }

    public List<SharedRecordResp> list(UserDO user, LocalDateTime start, LocalDateTime end) {
        requirePartner(user);
        LambdaQueryWrapper<SharedRecordDO> qw = new LambdaQueryWrapper<>();
        qw.in(SharedRecordDO::getCreatedBy, List.of(user.getId(), user.getPartnerId()));
        if (start != null) qw.ge(SharedRecordDO::getOccurredAt, start);
        if (end != null) qw.le(SharedRecordDO::getOccurredAt, end);
        qw.orderByDesc(SharedRecordDO::getOccurredAt);
        return mapper.selectList(qw).stream().map(SharedRecordResp::from).toList();
    }

    public SharedRecordResp getById(UserDO user, String id) {
        requirePartner(user);
        SharedRecordDO record = ownerValidator.findAndCheck(
            () -> mapper.selectById(id), "记录不存在",
            r -> user.getId().equals(r.getCreatedBy()) || user.getId().equals(user.getPartnerId()));
        return SharedRecordResp.from(record);
    }

    public SharedRecordResp update(UserDO user, String id, SharedRecordUpdateReq req) {
        requirePartner(user);
        SharedRecordDO record = ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        if (req.getTitle() != null) record.setTitle(req.getTitle());
        if (req.getContent() != null) record.setContent(req.getContent());
        if (req.getOccurredAt() != null) record.setOccurredAt(req.getOccurredAt());
        record.setUpdateTime(LocalDateTime.now());
        mapper.updateById(record);
        return SharedRecordResp.from(record);
    }

    public void delete(UserDO user, String id) {
        requirePartner(user);
        ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        mapper.deleteById(id);
    }

    public void deleteByCreatedBy(String userId1, String userId2) {
        mapper.delete(new LambdaQueryWrapper<SharedRecordDO>()
            .in(SharedRecordDO::getCreatedBy, List.of(userId1, userId2)));
    }
}
```

- [ ] **步骤 2: 提交**

```bash
git add backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedrecord/service/SharedRecordService.java
git commit -m "refactor: use OwnerValidator in SharedRecordService, unify error messages"
```

---

### Task 7: 删除 dead code `TodoMapper.selectUpcoming()`

**文件:**
- Modify: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/mapper/TodoMapper.java`

- [ ] **步骤 1: 删除 selectUpcoming 方法**

删除 `selectUpcoming` 方法，保留 `selectIncompleteByDueDate`。

```java
@Mapper
public interface TodoMapper extends BaseMapper<TodoDO> {

    @Select("SELECT * FROM todo WHERE (user_id = #{userId} OR assigned_to = #{userId}) " +
            "AND is_completed = 0 ORDER BY due_date ASC")
    List<TodoDO> selectIncompleteByDueDate(@Param("userId") String userId);
}
```

- [ ] **步骤 2: 提交**

```bash
git add backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/mapper/TodoMapper.java
git commit -m "chore: remove unused TodoMapper.selectUpcoming()"
```

---

### Plan Self-Review

- [ ] Spec coverage: 每个 spec 中的需求都有对应任务
- [ ] Placeholder scan: 无 TBD/TODO/placeholder
- [ ] Type consistency: `findAndCheck`/`requireOwner` 签名在各任务中一致
