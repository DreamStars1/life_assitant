# 分页与编辑表单 UI 修复 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为待办页和共享记录页添加服务端分页，并修复共享记录页编辑表单不跟卡片的问题。

**Architecture:** 后端在 `lifeassistant-common` 新增 `PageQuery/PageResult` 基类，各业务模块继承后使用 MyBatis-Plus `selectPage`；前端使用 `van-list` 滚动加载 + `PageResult<T>` 泛型类型，共享记录页同时改造为卡片原地编辑 + 服务端搜索。

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus (via ContiNew Starter), Vue 3, Vant 4, Pinia

---

## 文件映射

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| **Create** | `lifeassistant-common/.../base/model/query/PageQuery.java` | 分页查询参数基类（page/size + `@Min/@Max` 校验） |
| **Create** | `lifeassistant-common/.../base/model/query/PageResult.java` | 分页结果泛型（records/total/page/size/pages + `of()` 工厂） |
| **Create** | `lifeassistant-system/.../todo/model/query/TodoPageQuery.java` | 待办分页参数（继承 PageQuery + isCompleted/priority/dueDate 范围） |
| **Create** | `lifeassistant-system/.../sharedrecord/model/query/SharedRecordPageQuery.java` | 共享记录分页参数（继承 PageQuery + keyword + start/end） |
| **Create** | `lifeassistant-system/.../sharedrecord/model/resp/SharedRecordResp.java` | 共享记录响应类型（确认已有，需要确认是否独立文件） |
| **Modify** | `lifeassistant-system/.../todo/controller/TodoController.java` | list 方法改用 `@Valid TodoPageQuery` + 返回 `PageResult` |
| **Modify** | `lifeassistant-system/.../todo/service/TodoService.java` | list 方法使用 `mapper.selectPage()` |
| **Modify** | `lifeassistant-system/.../sharedrecord/controller/SharedRecordController.java` | list 方法改用 `@Valid SharedRecordPageQuery` + 返回 PageResult |
| **Modify** | `lifeassistant-system/.../sharedrecord/service/SharedRecordService.java` | list 方法使用 `mapper.selectPage()` + keyword like 查询 |
| **Modify** | `vue3-vant-mobile/src/types/api.ts` | 新增 `PageResult<T>` 接口 |
| **Modify** | `vue3-vant-mobile/src/api/modules/todos.ts` | `fetchTodos` 改为分页参数 + `PageResult` 返回 |
| **Create** | `vue3-vant-mobile/src/api/modules/shared-records.ts` | 新增共享记录 API 模块（带类型定义） |
| **Modify** | `vue3-vant-mobile/src/stores/modules/todo.ts` | 适配分页（currentPage/hasMore/pageSize） |
| **Modify** | `vue3-vant-mobile/src/pages/todos/index.vue` | van-list 真实分页 + 页面大小切换 |
| **Modify** | `vue3-vant-mobile/src/pages/share/index.vue` | 分页 + 卡片原地编辑 + 服务端搜索 + 页面大小切换 |

---

## 任务

### Task 1：创建 PageQuery.java 和 PageResult.java（common 模块）

**Files:**
- Create: `lifeassistant-common/src/main/java/top/lifeassistant/common/base/model/query/PageQuery.java`
- Create: `lifeassistant-common/src/main/java/top/lifeassistant/common/base/model/query/PageResult.java`

- [ ] **Step 1: 创建 PageQuery.java**

```java
package top.lifeassistant.common.base.model.query;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQuery {

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小为 1")
    private int page = 1;

    @Schema(description = "每页条数", example = "5")
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    private int size = 5;

    public <T> Page<T> toPage() {
        return new Page<>(page, size);
    }
}
```

- [ ] **Step 2: 创建 PageResult.java**

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

    @Schema(description = "每页条数", example = "5")
    private int size;

    @Schema(description = "总页数", example = "20")
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

- [ ] **Step 3: 提交**

```bash
git add "backend/lifeassistant/lifeassistant-common/src/main/java/top/lifeassistant/common/base/model/query/*.java"
git commit -m "feat(common): add PageQuery base class and PageResult generic"
```

---

### Task 2：配置 MyBatis-Plus 分页拦截器

**Files:**
- Verify/Modify: `lifeassistant-server/.../config/` 目录（检查是否需要新建 MybatisPlusConfig）

ContiNew Starter 2.15.0 的 `continew-starter-extension-crud-mp` 模块通常会自动配置 `PaginationInnerInterceptor`。先检查是否已有配置：

- [ ] **Step 1: 检查是否有 MybatisPlusConfig**

检查 `lifeassistant-server/src/main/java/top/lifeassistant/config/` 目录下是否有 `MybatisPlusConfig.java` 或类似配置。

- [ ] **Step 2: 如没有，创建 MybatisPlusConfig.java**

```java
package top.lifeassistant.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

- [ ] **Step 3: 提交**

```bash
git add "backend/lifeassistant/lifeassistant-server/src/main/java/top/lifeassistant/config/MybatisPlusConfig.java"
git commit -m "feat: add MyBatis-Plus pagination interceptor"
```

---

### Task 3：待办后端分页

**Files:**
- Create: `lifeassistant-system/.../todo/model/query/TodoPageQuery.java`
- Modify: `lifeassistant-system/.../todo/controller/TodoController.java`
- Modify: `lifeassistant-system/.../todo/service/TodoService.java`

- [ ] **Step 1: 创建 TodoPageQuery.java**

```java
package top.lifeassistant.todo.model.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.query.PageQuery;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "待办分页查询参数")
public class TodoPageQuery extends PageQuery {

    @Schema(description = "是否已完成")
    private Boolean isCompleted;

    @Schema(description = "优先级：low/medium/high/urgent")
    private String priority;

    @Schema(description = "截止时间范围（开始）")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDueDate;

    @Schema(description = "截止时间范围（结束）")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDueDate;
}
```

- [ ] **Step 2: 修改 TodoController.java**

将：
```java
@GetMapping("/todos")
public ApiResponse<List<TodoResp>> list(@CurrentUser UserDO user,
                                         @RequestParam(required = false) Boolean isCompleted,
                                         @RequestParam(required = false) String priority,
                                         @RequestParam(required = false) LocalDateTime startDueDate,
                                         @RequestParam(required = false) LocalDateTime endDueDate) {
    return ApiResponse.ok(service.list(user, isCompleted, priority, startDueDate, endDueDate));
}
```

改为：
```java
@GetMapping("/todos")
public ApiResponse<PageResult<TodoResp>> list(@CurrentUser UserDO user, @Valid TodoPageQuery query) {
    return ApiResponse.ok(service.list(user, query));
}
```

记得在 import 中添加 `@Valid`、`TodoPageQuery`、`PageResult`。

- [ ] **Step 3: 修改 TodoService.java**

将 `list()` 方法替换为：
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
    Page<TodoResp> respPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
    respPage.setRecords(result.getRecords().stream().map(TodoResp::from).toList());
    return PageResult.of(respPage);
}
```

同时更新 import：去掉 `List`（如果其他方法仍用），加上 `Page`、`TodoPageQuery`、`PageResult`。

- [ ] **Step 4: 提交**

```bash
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/todo/"
git commit -m "feat(todo): add pagination support"
```

---

### Task 4：共享记录后端分页 + 服务端搜索

**Files:**
- Create: `lifeassistant-system/.../sharedrecord/model/query/SharedRecordPageQuery.java`
- Modify: `lifeassistant-system/.../sharedrecord/controller/SharedRecordController.java`
- Modify: `lifeassistant-system/.../sharedrecord/service/SharedRecordService.java`

- [ ] **Step 1: 创建 SharedRecordPageQuery.java**

```java
package top.lifeassistant.sharedrecord.model.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.query.PageQuery;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "共享记录分页查询参数")
public class SharedRecordPageQuery extends PageQuery {

    @Schema(description = "搜索关键字（匹配标题或内容）")
    private String keyword;

    @Schema(description = "发生时间范围（开始）")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime start;

    @Schema(description = "发生时间范围（结束）")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime end;
}
```

- [ ] **Step 2: 修改 SharedRecordController.java**

将：
```java
@GetMapping("/shared-records")
public ApiResponse<List<SharedRecordResp>> list(@CurrentUser UserDO user,
                                                 @RequestParam(required = false) LocalDateTime start,
                                                 @RequestParam(required = false) LocalDateTime end) {
    return ApiResponse.ok(service.list(user, start, end));
}
```

改为：
```java
@GetMapping("/shared-records")
public ApiResponse<PageResult<SharedRecordResp>> list(@CurrentUser UserDO user, @Valid SharedRecordPageQuery query) {
    return ApiResponse.ok(service.list(user, query));
}
```

- [ ] **Step 3: 修改 SharedRecordService.java**

将 `list()` 方法替换为：
```java
public PageResult<SharedRecordResp> list(UserDO user, SharedRecordPageQuery query) {
    requirePartner(user);
    Page<SharedRecordDO> page = query.toPage();
    LambdaQueryWrapper<SharedRecordDO> qw = new LambdaQueryWrapper<>();
    qw.in(SharedRecordDO::getCreatedBy, List.of(user.getId(), user.getPartnerId()));
    if (query.getKeyword() != null) {
        String kw = query.getKeyword();
        qw.and(w -> w.like(SharedRecordDO::getTitle, kw)
                      .or().like(SharedRecordDO::getContent, kw));
    }
    if (query.getStart() != null) qw.ge(SharedRecordDO::getOccurredAt, query.getStart());
    if (query.getEnd() != null) qw.le(SharedRecordDO::getOccurredAt, query.getEnd());
    qw.orderByDesc(SharedRecordDO::getOccurredAt);
    Page<SharedRecordDO> result = mapper.selectPage(page, qw);
    Page<SharedRecordResp> respPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
    respPage.setRecords(result.getRecords().stream().map(SharedRecordResp::from).toList());
    return PageResult.of(respPage);
}
```

- [ ] **Step 4: 提交**

```bash
git add "backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/sharedrecord/"
git commit -m "feat(shared-record): add pagination and keyword search"
```

---

### Task 5：前端类型和 API 层

**Files:**
- Modify: `vue3-vant-mobile/src/types/api.ts`
- Modify: `vue3-vant-mobile/src/api/modules/todos.ts`
- Create: `vue3-vant-mobile/src/api/modules/shared-records.ts`

- [ ] **Step 1: types/api.ts — 新增 PageResult**

在 `FieldError` 之后添加：
```typescript
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
  pages: number
}
```

- [ ] **Step 2: 修改 api/modules/todos.ts**

将 `fetchTodos` 改为：
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

import 添加 `PageResult`。

- [ ] **Step 3: 创建 api/modules/shared-records.ts**

```typescript
import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/api'

export interface SharedRecordItem {
  id: string
  createdBy: string
  title: string
  content?: string | null
  occurredAt: string
  createdAt: string
  updateTime?: string | null
}

export function fetchSharedRecords(params?: {
  page?: number
  size?: number
  keyword?: string
  start?: string
  end?: string
}) {
  return request.get<ApiResponse<PageResult<SharedRecordItem>>>('/shared-records', { params })
}

export function createSharedRecord(data: { title: string; content?: string; occurredAt?: string }) {
  return request.post<ApiResponse<SharedRecordItem>>('/shared-records', data)
}

export function updateSharedRecord(id: string, data: { title?: string; content?: string; occurredAt?: string }) {
  return request.patch<ApiResponse<SharedRecordItem>>(`/shared-records/${id}`, data)
}

export function deleteSharedRecord(id: string) {
  return request.delete<ApiResponse<void>>(`/shared-records/${id}`)
}
```

- [ ] **Step 4: 提交**

```bash
git add "front/vue3-vant-mobile/src/types/api.ts"
git add "front/vue3-vant-mobile/src/api/modules/todos.ts"
git add "front/vue3-vant-mobile/src/api/modules/shared-records.ts"
git commit -m "feat(front): add PageResult type and shared-records API module"
```

---

### Task 6：前端 Todo store 适配分页

**Files:**
- Modify: `vue3-vant-mobile/src/stores/modules/todo.ts`

- [ ] **Step 1: 重写 todo store**

将 Store 改为分页模式：

```typescript
import { defineStore } from 'pinia'
import { acknowledgeTodo, createTodo, deleteTodo, fetchTodos, fetchUpcomingTodos, toggleTodo, updateTodo } from '@/api/modules/todos'
import { fetchTemplates } from '@/api/modules/ack-templates'
import type { TodoItem } from '@/api/modules/todos'
import { showNotify } from 'vant'

export const useTodoStore = defineStore('todo', () => {
  const todos = ref<TodoItem[]>([])
  const upcoming = ref<TodoItem[]>([])
  const loading = ref(false)
  const currentPage = ref(1)
  const hasMore = ref(true)
  const pageSize = ref(5)
  const filter = ref<{ isCompleted?: boolean, startDueDate?: string, endDueDate?: string }>({})

  async function loadTodos(reset = false) {
    if (reset) {
      currentPage.value = 1
      todos.value = []
      hasMore.value = true
    }
    if (!hasMore.value) return
    loading.value = true
    try {
      const params: Record<string, unknown> = { page: currentPage.value, size: pageSize.value }
      if (filter.value.isCompleted !== undefined) params.isCompleted = filter.value.isCompleted
      if (filter.value.startDueDate) params.startDueDate = filter.value.startDueDate
      if (filter.value.endDueDate) params.endDueDate = filter.value.endDueDate
      const res = await fetchTodos(params)
      const data = res.data!
      todos.value.push(...data.records)
      hasMore.value = currentPage.value < data.pages
      currentPage.value++
    } finally {
      loading.value = false
    }
  }

  async function loadUpcoming() {
    try {
      const res = await fetchUpcomingTodos()
      upcoming.value = res.data ?? []
    } catch {
      // silent
    }
  }

  async function create(data: { title: string, description?: string, priority: string, dueDate?: string, assignedTo?: string }) {
    const res = await createTodo(data)
    await loadTodos(true)
    return res.data
  }

  async function update(id: string, data: { title?: string, description?: string, priority?: string, dueDate?: string }) {
    const res = await updateTodo(id, data)
    await loadTodos(true)
    return res.data
  }

  async function remove(id: string) {
    await deleteTodo(id)
    await loadTodos(true)
  }

  async function toggleComplete(id: string) {
    const item = upcoming.value.find(t => t.id === id)
    if (item) item.isCompleted = !item.isCompleted
    await toggleTodo(id)
    loadUpcoming()
    loadTodos(true)
  }

  async function acknowledge(id: string) {
    const templatesRes = await fetchTemplates()
    const templates = templatesRes.data ?? []
    const message = templates.length > 0 ? templates[0]!.content : '收到'
    await acknowledgeTodo(id, message)
    showNotify({ type: 'success', message: '已确认收到' })
    await loadTodos(true)
  }

  function setFilter(f: { isCompleted?: boolean, startDueDate?: string, endDueDate?: string }) {
    filter.value = f
    loadTodos(true)
  }

  function changePageSize(size: number) {
    pageSize.value = size
    loadTodos(true)
  }

  return { todos, upcoming, loading, currentPage, hasMore, pageSize, loadTodos, loadUpcoming, create, update, remove, toggleComplete, acknowledge, setFilter, changePageSize }
})
```

关键变化：
- 新增 `currentPage`, `hasMore`, `pageSize` 状态
- `loadTodos(reset)` — `reset=true` 重置到第一页，`reset=false` 追加加载下一页
- `create/update/remove/toggleComplete` 操作后调 `loadTodos(true)` 重置
- 新增 `changePageSize()` 方法

- [ ] **Step 2: 提交**

```bash
git add "front/vue3-vant-mobile/src/stores/modules/todo.ts"
git commit -m "feat(todo-store): adapt pagination"
```

---

### Task 7：前端 Todo 页面适配分页 + 页面大小切换

**Files:**
- Modify: `vue3-vant-mobile/src/pages/todos/index.vue`

- [ ] **Step 1: 重写 todo 页面 template**

替换 `van-list` 部分：
```html
<van-pull-refresh v-model="todoStore.loading" @refresh="onRefresh">
  <van-list
    v-model:loading="listLoading"
    :finished="!todoStore.hasMore"
    finished-text="没有更多了"
    @load="onLoadMore"
  >
    <!-- 空状态和列表内容保持不变 -->
    <div v-if="todoStore.todos.length === 0" class="empty-state">
      <van-icon name="todo-list-o" size="48" color="var(--van-gray-4)" />
      <p>暂无待办</p>
    </div>
    <!-- ... todo-item 循环不变 ... -->

    <!-- 页面大小切换 -->
    <template #default>
      <div class="page-size-trigger" @click="showPageSize = true">
        每页 {{ todoStore.pageSize }} 条 <van-icon name="arrow-down" />
      </div>
    </template>
  </van-list>
</van-pull-refresh>

<van-action-sheet v-model:show="showPageSize" title="每页显示">
  <van-cell v-for="s in [3, 5, 10]" :key="s" :title="`${s} 条`" :label="s === 5 ? '推荐' : ''" is-link @click="onPageSizeChange(s)" />
</van-action-sheet>
```

- [ ] **Step 2: 添加 script 逻辑**

添加新状态和方法：
```typescript
const listLoading = ref(false)
const showPageSize = ref(false)

todoStore.loadTodos(true)

async function onRefresh() {
  await todoStore.loadTodos(true)
}

async function onLoadMore() {
  listLoading.value = true
  await todoStore.loadTodos(false)
  listLoading.value = false
}

function onPageSizeChange(size: number) {
  showPageSize.value = false
  todoStore.changePageSize(size)
}
```

注意：
- 移除原来 `todoStore.loadTodos()`（已经不会自动调用，改为 `loadTodos(true)`）
- `van-pull-refresh` 使用 store 的 `loading`，`van-list` 使用本地 `listLoading`
- `todoStore.pageSize` 改为 `ref` 了，模板中要用 `todoStore.pageSize`（已经是 ref 属性）

- [ ] **Step 3: 添加页面大小切换样式**

```css
.page-size-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 12px;
  font-size: 12px;
  color: var(--van-gray-5);
  cursor: pointer;
}
```

- [ ] **Step 4: 提交**

```bash
git add "front/vue3-vant-mobile/src/pages/todos/index.vue"
git commit -m "feat(todo-page): add pagination and page size switcher"
```

---

### Task 8：前端 Share 页面全面改造（分页 + 卡片原地编辑 + 服务端搜索 + 页面大小切换）

**Files:**
- Modify: `vue3-vant-mobile/src/pages/share/index.vue`

这是改动最大的任务。需要：分页、编辑移入卡片、搜索改为服务端、页面大小切换。

- [ ] **Step 1: 重写 script 部分**

```vue
<script setup lang="ts">
import { showNotify, showToast } from 'vant'
import { useUserStore } from '@/stores'
import request from '@/utils/request'
import { fetchSharedRecords, createSharedRecord, updateSharedRecord, deleteSharedRecord } from '@/api/modules/shared-records'
import type { SharedRecordItem } from '@/api/modules/shared-records'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

// ---- 伴侣邀请/绑定（保持不变） ----
const inviteToken = ref('')
const loading = ref(false)
const bindCode = ref('')
// ... generateInvite, copyInvite, bindPartner 保持不变 ...

function toLocalDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

// ---- 分页状态 ----
const records = ref<SharedRecordItem[]>([])
const currentPage = ref(1)
const hasMore = ref(true)
const pageSize = ref(5)
const listLoading = ref(false)
const showPageSize = ref(false)
const searchKeyword = ref('')

// ---- 时间筛选 ----
const filterStart = ref('')
const filterEnd = ref('')
const showFilterStart = ref(false)
const showFilterEnd = ref(false)

// ---- 卡片编辑 ----
const editingRecordId = ref<string | null>(null)
const editForm = reactive({ title: '', content: '', occurredAt: '' })
const showCalendar = ref(false)

// ---- 新建表单 ----
const showCreateForm = ref(false)
const createForm = reactive({ title: '', content: '', occurredAt: '' })
const showCreateCalendar = ref(false)

async function loadRecords(reset = false) {
  if (reset) {
    currentPage.value = 1
    records.value = []
    hasMore.value = true
  }
  if (!hasMore.value) return
  const params: Record<string, unknown> = { page: currentPage.value, size: pageSize.value }
  if (searchKeyword.value.trim()) params.keyword = searchKeyword.value.trim()
  if (filterStart.value) params.start = filterStart.value
  if (filterEnd.value) params.end = filterEnd.value
  const res = await fetchSharedRecords(params)
  const data = res.data!
  records.value.push(...data.records)
  hasMore.value = currentPage.value < data.pages
  currentPage.value++
}

function onLoadMore() {
  listLoading.value = true
  loadRecords(false).finally(() => { listLoading.value = false })
}

function changePageSize(size: number) {
  showPageSize.value = false
  pageSize.value = size
  loadRecords(true)
}

function onSearch() {
  loadRecords(true)
}

function onFilterStartConfirm(d: Date) {
  filterStart.value = toLocalDateStr(d)
  showFilterStart.value = false
  loadRecords(true)
}
function onFilterEndConfirm(d: Date) {
  filterEnd.value = toLocalDateStr(d)
  showFilterEnd.value = false
  loadRecords(true)
}
function clearFilter() {
  filterStart.value = ''
  filterEnd.value = ''
  loadRecords(true)
}

function onCalendarConfirm(d: Date) {
  const target = editingRecordId.value ? editForm : createForm
  target.occurredAt = toLocalDateStr(d)
  showCalendar.value = false
}

// ---- 编辑 ----
function openEdit(r: SharedRecordItem) {
  cancelCreate() // 关闭新建
  editingRecordId.value = r.id
  editForm.title = r.title
  editForm.content = r.content || ''
  editForm.occurredAt = r.occurredAt ? r.occurredAt.slice(0, 10) : ''
}

function cancelEdit() {
  editingRecordId.value = null
}

async function saveEdit(r: SharedRecordItem) {
  if (!editForm.title.trim()) return
  await updateSharedRecord(r.id, {
    title: editForm.title,
    content: editForm.content || undefined,
    occurredAt: editForm.occurredAt ? `${editForm.occurredAt}T00:00:00` : undefined,
  })
  showToast('已更新')
  editingRecordId.value = null
  await loadRecords(true)
}

// ---- 新增 ----
function openCreate() {
  cancelEdit()
  showCreateForm.value = true
  createForm.title = ''
  createForm.content = ''
  createForm.occurredAt = toLocalDateStr(new Date())
}

function cancelCreate() {
  showCreateForm.value = false
}

async function saveCreate() {
  if (!createForm.title.trim()) return
  await createSharedRecord({
    title: createForm.title,
    content: createForm.content || undefined,
    occurredAt: createForm.occurredAt ? `${createForm.occurredAt}T00:00:00` : undefined,
  })
  showToast('记录已添加')
  showCreateForm.value = false
  await loadRecords(true)
}

// ---- 删除 ----
async function deleteRecord(id: string) {
  await deleteSharedRecord(id)
  showToast('已删除')
  await loadRecords(true)
}

function onCreateCalendarConfirm(d: Date) {
  createForm.occurredAt = toLocalDateStr(d)
  showCreateCalendar.value = false
}

function formatDate(d: string): string {
  if (!d) return ''
  const [y, m, day] = d.slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, day).toLocaleDateString('zh-CN')
}

const partnerId = computed(() => userInfo.value?.partnerId || (userInfo.value as any)?.partner_id)
const hasRecords = computed(() => records.value.length > 0)

onMounted(async () => {
  await userStore.info()
  if (partnerId.value) loadRecords(true)
})

watch(partnerId, (val) => {
  if (val) loadRecords(true)
})
</script>
```

- [ ] **Step 2: 重写 template 部分**

关键变化：
- 列表用 `van-list` 包装实现分页
- 每条卡片可展开为编辑表单（`editingRecordId === r.id`）
- 搜索框输入触发 `loadRecords(true)`（服务端搜索）
- 底部"添加"保留，新建时底部展开表单
- 列表尾部增加页面大小切换

```html
<template>
  <div>
    <!-- 未绑定伴侣引导（同原有） -->
    <div v-if="!partnerId" class="p-4 space-y-3">
      <!-- ... 同原有 ... -->
    </div>

    <template v-if="partnerId">
      <!-- 搜索 + 时间筛选（搜索框改为触发服务端搜索） -->
      <div class="px-4 pb-1 pt-3 space-y-2">
        <van-field
          v-model="searchKeyword"
          placeholder="搜索标题或内容"
          clearable
          left-icon="search"
          class="!p-0"
          @clear="onSearch"
          @search="onSearch"
        />
        <!-- 时间筛选同原有 -->
        <div class="flex gap-2 items-center">
          <van-field v-model="filterStart" is-link readonly placeholder="开始日期" class="!p-0 !flex-1" @click="showFilterStart = true" />
          <span class="text-gray-400">—</span>
          <van-field v-model="filterEnd" is-link readonly placeholder="结束日期" class="!p-0 !flex-1" @click="showFilterEnd = true" />
          <van-button v-if="filterStart || filterEnd" size="small" plain @click="clearFilter">清除</van-button>
        </div>
      </div>
      <van-calendar v-model:show="showFilterStart" :min-date="new Date('2020-01-01')" @confirm="onFilterStartConfirm" />
      <van-calendar v-model:show="showFilterEnd" :min-date="new Date('2020-01-01')" @confirm="onFilterEndConfirm" />

      <!-- 分页列表 -->
      <van-list
        v-model:loading="listLoading"
        :finished="!hasMore"
        finished-text="没有更多了"
        @load="onLoadMore"
      >
        <van-cell-group :inset="true" title="一起做过的事">
          <van-empty v-if="!hasRecords" description="还没有记录，添加第一条吧" />
          <van-swipe-cell v-for="r in records" :key="r.id">
            <!-- 查看模式 -->
            <template v-if="editingRecordId !== r.id">
              <van-cell
                :title="r.title"
                :label="r.content || ''"
                :value="formatDate(r.occurredAt)"
                is-link
                @click="openEdit(r)"
              />
            </template>
            <!-- 编辑模式：卡片原地展开 -->
            <template v-else>
              <div class="edit-card p-3">
                <van-field v-model="editForm.title" placeholder="标题" class="!mb-2" />
                <van-field v-model="editForm.content" placeholder="详细描述（可选）" class="!mb-2" />
                <van-field v-model="editForm.occurredAt" is-link readonly placeholder="日期（可选）" @click="showCalendar = true" />
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
        </van-cell-group>

        <!-- 页面大小切换 -->
        <div class="page-size-trigger" @click="showPageSize = true">
          每页 {{ pageSize }} 条 <van-icon name="arrow-down" />
        </div>
      </van-list>

      <van-calendar v-model:show="showCalendar" :min-date="new Date('2020-01-01')" @confirm="onCalendarConfirm" />

      <!-- 新建表单 -->
      <div class="px-4 mt-3">
        <van-button v-if="!showCreateForm" type="primary" round block @click="openCreate">
          添加一起做过的事
        </van-button>
        <div v-else class="p-3 rounded-lg bg-white">
          <van-field v-model="createForm.title" placeholder="标题（如：一起看了电影）" class="!mb-2" />
          <van-field v-model="createForm.content" placeholder="详细描述（可选）" class="!mb-2" />
          <van-field v-model="createForm.occurredAt" is-link readonly placeholder="日期（可选）" @click="showCreateCalendar = true" />
          <van-calendar v-model:show="showCreateCalendar" :min-date="new Date('2020-01-01')" @confirm="onCreateCalendarConfirm" />
          <div class="flex gap-2">
            <van-button type="primary" size="small" @click="saveCreate">保存</van-button>
            <van-button size="small" @click="cancelCreate">取消</van-button>
          </div>
        </div>
      </div>

      <!-- 页面大小切换 ActionSheet -->
      <van-action-sheet v-model:show="showPageSize" title="每页显示">
        <van-cell v-for="s in [3, 5, 10]" :key="s" :title="`${s} 条`" :label="s === 5 ? '推荐' : ''" is-link @click="changePageSize(s)" />
      </van-action-sheet>
    </template>
  </div>
</template>
```

注意: `onCreateCalendarConfirm` 方法也需要添加：
```typescript
function onCreateCalendarConfirm(d: Date) {
  createForm.occurredAt = toLocalDateStr(d)
  showCreateCalendar.value = false
}
```

- [ ] **Step 3: 添加样式**

```css
.edit-card {
  background: #f7f8fa;
}
.page-size-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 12px;
  font-size: 12px;
  color: var(--van-gray-5);
  cursor: pointer;
}
```

- [ ] **Step 4: 提交**

```bash
git add "front/vue3-vant-mobile/src/pages/share/index.vue"
git commit -m "feat(share-page): pagination, inline edit, server-side search, page size"
```

---

## 各任务依赖关系

```
Task 1 (PageQuery 基类)
  ├─→ Task 2 (分页拦截器)
  ├─→ Task 3 (Todo 后端)
  └─→ Task 4 (SharedRecord 后端)
         └─→ Task 5 (前端类型/API)
                ├─→ Task 6 (Todo store)
                │      └─→ Task 7 (Todo 页面)
                └─→ Task 8 (Share 页面)
```

Task 1 和 Task 2 可并行，Task 3 和 Task 4 可并行（都依赖 Task 1），Task 7 和 Task 8 可并行（都依赖 Task 5 + Task 6）。
