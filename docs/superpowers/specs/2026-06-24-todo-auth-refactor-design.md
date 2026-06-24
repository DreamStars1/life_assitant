# 后端授权校验重构：OwnerValidator 组件

## 背景

项目中有 3 个 Service（TodoService、TodoAckTemplateService、SharedRecordService）共 11 处重复了同一模式：

```java
Entity e = mapper.selectById(id);
if (e == null) throw new BusinessException("X not found");
if (!e.getUserId().equals(user.getId())) throw new BadRequestException("只能修改自己的X");
```

此外还存在以下问题：

- 异常类型不一致（`BusinessException` vs `BadRequestException`），错误信息对外暴露资源存在性
- `TodoAckTemplateService.create()` 中 `selectOne().getSortOrder()` 在无数据时 NPE
- `TodoMapper.selectUpcoming()` 死代码未被调用
- 手动设置时间字段与 `BaseDO` 自动填充注解不一致

## 方案

新增一个无框架依赖的轻量组件，抽取"查 → 判空 → 鉴权"逻辑，不改变任何 Service 的继承链，新老 Service 均可立即使用。

### 新增：`OwnedEntity` 接口

```java
// top.lifeassistant.common.base.model.entity.OwnedEntity
public interface OwnedEntity {
    /** 返回所有者的用户 ID */
    String getOwnerId();
}
```

三个 Entity 实现：

| Entity | 所有者字段 | `getOwnerId()` 返回 |
|---|---|---|
| `TodoDO` | `userId` | `this.userId` |
| `TodoAckTemplateDO` | `userId` | `this.userId` |
| `SharedRecordDO` | `createdBy` | `this.createdBy` |

### 新增：`OwnerValidator` 组件

```java
// top.lifeassistant.common.base.component.OwnerValidator
@Component
public class OwnerValidator {

    /**
     * 查 → 判空 → 谓词校验，不通过统一抛 BusinessException("资源不存在")
     */
    public <T> T findAndCheck(Supplier<T> fetcher, String errMsg, Predicate<T> predicate) {
        T entity = fetcher.get();
        if (entity == null || !predicate.test(entity)) {
            throw new BusinessException(errMsg);
        }
        return entity;
    }

    /**
     * 快捷方法：仅校验所有者身份
     */
    public <T extends OwnedEntity> T requireOwner(Supplier<T> fetcher, String ownerId) {
        return findAndCheck(fetcher, "资源不存在", e -> ownerId.equals(e.getOwnerId()));
    }
}
```

### Service 改造

三种鉴权规则对应三种调用方式：

```java
// 规则 1：仅所有者（TodoService.update/delete, TodoAckTemplateService.update/delete, SharedRecordService.update/delete）
TodoDO todo = ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());

// 规则 2：所有者 OR 关联方（TodoService.getById/toggleComplete）
TodoDO todo = ownerValidator.findAndCheck(
    () -> mapper.selectById(id), "资源不存在",
    t -> user.getId().equals(t.getUserId()) || user.getId().equals(t.getAssignedTo())
);

// 规则 3：仅关联方（TodoService.acknowledge）
// 不存在或非被指派者，均返回"只有被指派者才能确认"（不泄露存在性）
TodoDO todo = ownerValidator.findAndCheck(
    () -> mapper.selectById(id), "只有被指派者才能确认",
    t -> user.getId().equals(t.getAssignedTo())
);

// 规则 4：所有者 OR 伴侣（SharedRecordService.getById）
SharedRecordDO r = ownerValidator.findAndCheck(
    () -> mapper.selectById(id), "记录不存在",
    rec -> user.getId().equals(rec.getCreatedBy()) || user.getId().equals(user.getPartnerId())
);
```

### 顺便修复

| 问题 | 修复 |
|---|---|
| `TodoAckTemplateService.create()` NPE | `selectOne` 结果可为 null，改用 `Optional` 兜底 |
| `TodoMapper.selectUpcoming()` 死代码 | 删除 |
| 英文提示对外暴露存在性 | 统一为 `"资源不存在"`（中文），异常类型统一为 `BusinessException` |

## 不改动事项（明确延期）

| 事项 | 理由 |
|---|---|
| PK 迁移 Long + UUID 业务键 | 全栈改动（DB 迁移 + 所有 Entity + 所有 Service），独立一轮 |
| 接入 ContiNew CrudServiceImpl | 依赖 PK 迁移完成，再开一轮 |
| 新建 Service 规范 | 新建 Service 应使用 OwnerValidator，但本项目不做强制 |

## 变更清单

| 文件 | 变更类型 |
|---|---|
| `common/.../base/model/entity/OwnedEntity.java` | **新增** |
| `common/.../base/component/OwnerValidator.java` | **新增** |
| `system/.../todo/model/entity/TodoDO.java` | 修改 |
| `system/.../todo/model/entity/TodoAckTemplateDO.java` | 修改 |
| `system/.../sharedrecord/model/entity/SharedRecordDO.java` | 修改 |
| `system/.../todo/service/TodoService.java` | 重构 |
| `system/.../todo/service/TodoAckTemplateService.java` | 重构 |
| `system/.../sharedrecord/service/SharedRecordService.java` | 重构 |
| `system/.../todo/mapper/TodoMapper.java` | 删除 `selectUpcoming` |
