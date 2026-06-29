# 分页与编辑表单 UI 修复设计文档

## 概述

修复前端的三个问题：待办页和共享记录页缺少分页、共享记录页编辑表单不在卡片位置。

## 改动一览

| 模块 | 改动 |
|------|------|
| common | 新增 `query/PageQuery.java`、`query/PageResult.java` |
| todo | 新增 `TodoPageQuery`，修改 Controller/Service 支持分页 |
| sharedrecord | 新增 `SharedRecordPageQuery`，修改 Controller/Service 支持分页 |
| frontend API 类型 | 新增 `PageResult<T>` 泛型 |
| frontend Todo 页 | 改用 `van-list` 真实分页 |
| frontend Share 页 | 改用 `van-list` 真实分页 + 编辑表单改为卡片原地展开 |

## 后端

### 1. 分页基类（lifeassistant-common）

**`base/model/query/PageQuery.java`** — 分页查询参数基类，所有列表查询参数扩展此类：

```java
package top.lifeassistant.common.base.model.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PageQuery {

    @Schema(description = "页码", example = "1")
    private int page = 1;

    @Schema(description = "每页条数", example = "20")
    private int size = 20;

    public <T> Page<T> toPage() {
        return new Page<>(page, size);
    }
}
```

**`base/model/query/PageResult.java`** — 分页结果泛型，统一返回格式：

```java
package top.lifeassistant.common.base.model.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    @Schema(description = "数据列表")
    private List<T> records;

    @Schema(description = "总记录数", example = "100")
    private long total;

    @Schema(description = "当前页码", example = "1")
    private int page;

    @Schema(description = "每页条数", example = "20")
    private int size;

    @Schema(description = "总页数", example = "5")
    private long pages;

    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> r = new PageResult<>();
        r.setRecords(page.getRecords());
        r.setTotal(page.getTotal());
        r.setPage((int) page.getCurrent());
        r.setSize((int) page.getSize());
        r.setPages(page.getPages());
        return r;
    }
}
```

### 2. MyBatis-Plus 分页插件配置

检查项目中是否已配置 `PaginationInnerInterceptor`：

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

如果项目已有 ContiNew Starter 的自动配置则跳过此步。

### 3. 待办分页

**新增 `todo/model/query/TodoPageQuery.java`**：

```java
@Schema(description = "待办分页查询参数")
public class TodoPageQuery extends PageQuery {
    private Boolean isCompleted;
    private String priority;
    private LocalDateTime startDueDate;
    private LocalDateTime endDueDate;
}
```

**修改 `TodoController.java`**：

```java
@GetMapping("/todos")
public ApiResponse<PageResult<TodoResp>> list(@CurrentUser UserDO user, TodoPageQuery query) {
    return ApiResponse.ok(service.list(user, query));
}
```

**修改 `TodoService.java`** — `list()` 方法改为 MP Page 查询：

```java
public PageResult<TodoResp> list(UserDO user, TodoPageQuery query) {
    Page<TodoDO> page = query.toPage();
    LambdaQueryWrapper<TodoDO> qw = new LambdaQueryWrapper<>();
    qw.and(w -> w.eq(TodoDO::getUserId, user.getId())
                 .or().eq(TodoDO::getAssignedTo, user.getId()));
    if (query.getIsCompleted() != null) qw.eq(TodoDO::getIsCompleted, query.getIsCompleted());
    if (query.getPriority() != null) qw.eq(TodoDO::getPriority, query.getPriority());
    if (query.getStartDueDate() != null) qw.ge(TodoDO::getDueDate, query.getStartDueDate());
    if (query.getEndDueDate() != null) qw.le(TodoDO::getDueDate, query.getEndDueDate());
    qw.orderByDesc(TodoDO::getCreatedAt);
    Page<TodoDO> result = mapper.selectPage(page, qw);
    // 将 DO 列表转为 DTO
    Page<TodoResp> respPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
    respPage.setRecords(result.getRecords().stream().map(TodoResp::from).toList());
    return PageResult.of(respPage);
}
```

**说明**：保留 `getUpcoming()` 方法不变（首页使用，不需要分页）。

### 4. 共享记录分页

**新增 `sharedrecord/model/query/SharedRecordPageQuery.java`**：

```java
@Schema(description = "共享记录分页查询参数")
public class SharedRecordPageQuery extends PageQuery {
    private LocalDateTime start;
    private LocalDateTime end;
}
```

**修改 `SharedRecordController.java`**：

```java
@GetMapping("/shared-records")
public ApiResponse<PageResult<SharedRecordResp>> list(@CurrentUser UserDO user, SharedRecordPageQuery query) {
    return ApiResponse.ok(service.list(user, query));
}
```

**修改 `SharedRecordService.java`**：

```java
public PageResult<SharedRecordResp> list(UserDO user, SharedRecordPageQuery query) {
    requirePartner(user);
    Page<SharedRecordDO> page = query.toPage();
    LambdaQueryWrapper<SharedRecordDO> qw = new LambdaQueryWrapper<>();
    qw.in(SharedRecordDO::getCreatedBy, List.of(user.getId(), user.getPartnerId()));
    if (query.getStart() != null) qw.ge(SharedRecordDO::getOccurredAt, query.getStart());
    if (query.getEnd() != null) qw.le(SharedRecordDO::getOccurredAt, query.getEnd());
    qw.orderByDesc(SharedRecordDO::getOccurredAt);
    Page<SharedRecordDO> result = mapper.selectPage(page, qw);
    Page<SharedRecordResp> respPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
    respPage.setRecords(result.getRecords().stream().map(SharedRecordResp::from).toList());
    return PageResult.of(respPage);
}
```

## 前端

### 1. 通用分页类型

**`types/api.ts`** 新增 `PageResult`：

```typescript
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}
```

### 2. 待办页改造

**`api/modules/todos.ts`** — `fetchTodos` 改为分页参数：

```typescript
export function fetchTodos(params?: {
  page?: number
  size?: number
  isCompleted?: boolean
  priority?: string
  startDueDate?: string
  endDueDate?: string
}) {
  return request.get<ApiResponse<PageResult<TodoItem>>>('/todos', { params })
}
```

**`stores/modules/todo.ts`** — 适配分页：

```typescript
// 新增分页状态
const currentPage = ref(1)
const hasMore = ref(true)
const pageSize = 20

async function loadTodos(reset = false) {
  if (reset) {
    currentPage.value = 1
    todos.value = []
    hasMore.value = true
  }
  if (!hasMore.value) return
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: currentPage.value, size: pageSize }
    // ... 原有过滤参数不变
    const res = await fetchTodos(params)
    const data = res.data!
    if (reset) {
      todos.value = data.records
    } else {
      todos.value.push(...data.records)
    }
    hasMore.value = currentPage.value < data.pages
    currentPage.value++
  } finally {
    loading.value = false
  }
}

// loadTodos() → loadTodos(true)
// create/update/remove/toggleComplete 之后调 loadTodos(true)
```

**`pages/todos/index.vue`** — 模板层改造：

```html
<van-pull-refresh v-model="todoStore.loading" @refresh="onRefresh">
  <van-list
    v-model:loading="listLoading"
    :finished="!todoStore.hasMore"
    finished-text="没有更多了"
    @load="onLoadMore"
  >
    <!-- ... 内容不变 ... -->
  </van-list>
</van-pull-refresh>
```

```typescript
const listLoading = ref(false)

async function onRefresh() {
  await todoStore.loadTodos(true)
}

async function onLoadMore() {
  listLoading.value = true
  await todoStore.loadTodos(false)
  listLoading.value = false
}
```

**注意**：`van-list` 的 `loading` 和 `van-pull-refresh` 的 `v-model` 需要独立控制，不能绑定同一个值。`van-pull-refresh` 使用 store 的 loading，`van-list` 使用本地 `listLoading`。

### 3. 共享记录页改造

**分页 + 卡片原地编辑**，较为综合，需要重写页面核心逻辑：

**数据结构变化**：

- 移除全局 `showForm` / `editingId` / `form`，改为每张卡片独立控制
- 新增 `editingRecordId` 控制当前哪张卡片在编辑
- 新增分页状态 `currentPage` / `hasMore` / `pageSize`

**编辑交互**：

```html
<van-swipe-cell v-for="r in records" :key="r.id">
  <!-- 查看模式 -->
  <template v-if="editingRecordId !== r.id">
    <van-cell ... @click="openEdit(r)" />
  </template>
  <!-- 编辑模式：卡片原地展开为表单 -->
  <template v-else>
    <div class="edit-card">
      <van-field v-model="editForm.title" placeholder="标题" />
      <van-field v-model="editForm.content" placeholder="详细描述" />
      <van-field v-model="editForm.occurredAt" is-link readonly placeholder="日期" @click="showCalendar = true" />
      <div class="flex gap-2">
        <van-button size="small" type="primary" @click="saveEdit(r)">保存</van-button>
        <van-button size="small" @click="cancelEdit">取消</van-button>
      </div>
    </div>
  </template>
  <template #right>
    <van-button square type="danger" text="删除" @click="deleteRecord(r.id)" />
  </template>
</van-swipe-cell>
```

**页面底部**保留"添加"按钮，新建时也在底部展开表单（保持原有交互）：

```html
<div class="px-4 mt-3">
  <van-button v-if="!showCreateForm" type="primary" round block @click="openCreate">添加一起做过的事</van-button>
  <div v-else class="p-3 rounded-lg bg-white">
    <!-- 新建表单 -->
  </div>
</div>
```

**分页**：使用 `van-list` 包装列表：

```html
<van-list
  v-model:loading="listLoading"
  :finished="!hasMore"
  finished-text="没有更多了"
  @load="loadMore"
>
  <!-- 列表内容 -->
</van-list>
```

**搜索逻辑改为服务端搜索或保留客户端搜索**：当前搜索是 `computed` 对全量数据过滤。分页后搜索范围缩小的处理方式：

- **保持客户端搜索**，但只对当前页生效 → 用户体验差
- **改为服务端搜索** → 需要后端新增 `keyword` 参数

推荐：**短期保留客户端搜索 + 兜底"当前页无匹配结果"提示，后续改为服务端搜索**。`ponytail:` 标记。

## 前端类型安全备注

- 共享记录页当前大量使用 `any` 类型，改造时按项目已有模式保持一致
- `PageResult<T>` 泛型需要后端返回结构一致

## 不涉及的改动

- 编辑表单组件 `TodoForm.vue` 保持不变（待办页已用 ActionSheet + TodoForm，交互合理）
- 首页 `today/index.vue` 不变
- 共享记录页绑卡逻辑和伴侣绑定引导页面不变
