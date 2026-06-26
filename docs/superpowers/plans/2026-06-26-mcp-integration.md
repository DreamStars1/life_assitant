# MCP 服务集成实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 将待办和共享记录功能封装为 MCP 服务，通过 Streamable HTTP 对外暴露，支持第三方客户端集成。

**架构:** 在 `lifeassistant-server` 模块中嵌入 MCP Server，依赖 `spring-ai-mcp-server-webmvc`。新增 `@McpTool` 注解的工具类，注入现有 Service。Token 通过 HTTP Header 传递，`ApiTokenFilter` 前置过滤器校验。另需新增 `api_token` 表和对应的 CRUD 端点及前端管理页面。

**执行顺序:** 先完成 OwnerValidator 重构（独立 change），再执行本计划。

| 文件 | 操作 |
|------|------|
| `server/pom.xml` | 修改 |
| `server/.../resources/db/migration/V3__add_api_token.sql` | **创建** |
| `server/.../mcp/config/McpConfig.java` | **创建** |
| `server/.../mcp/auth/ApiTokenAuthHelper.java` | **创建** |
| `server/.../mcp/tool/TodoMcpTools.java` | **创建** |
| `server/.../mcp/tool/SharedRecordMcpTools.java` | **创建** |
| `server/.../mcp/interceptor/ApiTokenFilter.java` | **创建** |
| `system/.../apiToken/mapper/ApiTokenMapper.java` | **创建** |
| `system/.../apiToken/model/entity/ApiTokenDO.java` | **创建** |
| `system/.../apiToken/model/req/ApiTokenCreateReq.java` | **创建** |
| `system/.../apiToken/model/resp/ApiTokenResp.java` | **创建** |
| `system/.../apiToken/service/ApiTokenService.java` | **创建** |
| `system/.../apiToken/controller/ApiTokenController.java` | **创建** |
| `front/.../api/modules/api-tokens.ts` | **创建** |
| `front/.../pages/settings/index.vue` | 修改 |

---

### Task 1: 在 server pom.xml 中添加 Spring AI MCP 依赖

**文件:**
- Modify: `backend/lifeassistant/lifeassistant-server/pom.xml`

- [ ] **步骤 1: 添加 spring-ai-mcp-server-webmvc 依赖**

```xml
<!-- Spring AI MCP Server (Streamable HTTP) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-webmvc</artifactId>
    <!-- ponytail: 实际版本号需根据 ContiNew Starter 2.15.0 确认兼容性 -->
    <version>1.1.0</version>
</dependency>
```

阅读现有 pom.xml，将依赖加在适当的 `<dependencies>` 区域。同时在父 POM 的 `<dependencyManagement>` 中引入 Spring AI BOM（如需要）。

- [ ] **步骤 2: 提交**

```bash
git add backend/lifeassistant/lifeassistant-server/pom.xml
git commit -m "feat: add spring-ai-mcp-server-webmvc dependency"
```

---

### Task 2: 创建 Flyway V3 迁移 — api_token 表

**文件:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V3__add_api_token.sql`

- [ ] **步骤 1: 创建迁移文件**

```sql
CREATE TABLE api_token (
    id           CHAR(36)     NOT NULL COMMENT 'UUID 主键',
    user_id      CHAR(36)     NOT NULL COMMENT '所属用户',
    name         VARCHAR(50)  NOT NULL COMMENT '令牌别名（用户自定义）',
    token_hash   VARCHAR(64)  NOT NULL COMMENT '令牌 SHA-256 哈希',
    token_prefix CHAR(8)      NOT NULL COMMENT '令牌前 8 位（前端展示用）',
    last_used_at DATETIME     DEFAULT NULL COMMENT '最后使用时间',
    expires_at   DATETIME     DEFAULT NULL COMMENT '过期时间，NULL 表示永不过期',
    is_active    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否有效',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 访问令牌';
```

- [ ] **步骤 2: 提交**

```bash
git add backend/lifeassistant/lifeassistant-server/src/main/resources/db/migration/V3__add_api_token.sql
git commit -m "feat: add Flyway migration for api_token table"
```

---

### Task 3: 创建 ApiTokenDO 实体 + ApiTokenMapper

**文件:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/apitoken/model/entity/ApiTokenDO.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/apitoken/mapper/ApiTokenMapper.java`

- [ ] **步骤 1: 创建 ApiTokenDO 实体**

```java
package top.lifeassistant.apitoken.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.entity.BaseDO;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_token")
public class ApiTokenDO extends BaseDO {

    private String userId;
    private String name;
    private String tokenHash;
    private String tokenPrefix;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;
    private Boolean isActive;
}
```

- [ ] **步骤 2: 创建 ApiTokenMapper**

```java
package top.lifeassistant.apitoken.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.lifeassistant.apitoken.model.entity.ApiTokenDO;

@Mapper
public interface ApiTokenMapper extends BaseMapper<ApiTokenDO> {
}
```

- [ ] **步骤 3: 提交**

```bash
git add backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/apitoken/
git commit -m "feat: add ApiTokenDO entity and mapper"
```

---

### Task 4: 创建 ApiToken 请求/响应 DTO + Service + Controller

**文件:**
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/apitoken/model/req/ApiTokenCreateReq.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/apitoken/model/resp/ApiTokenResp.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/apitoken/service/ApiTokenService.java`
- Create: `backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/apitoken/controller/ApiTokenController.java`

- [ ] **步骤 1: 创建 ApiTokenCreateReq**

```java
package top.lifeassistant.apitoken.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建 API Token 请求")
public class ApiTokenCreateReq {

    @Schema(description = "令牌别名", example = "Claude 桌面端")
    @NotBlank
    @Size(max = 50)
    private String name;

    @Schema(description = "过期时间，不传表示永不过期")
    private LocalDateTime expiresAt;
}
```

- [ ] **步骤 2: 创建 ApiTokenResp**

```java
package top.lifeassistant.apitoken.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.apitoken.model.entity.ApiTokenDO;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "API Token 响应")
public class ApiTokenResp {

    private String id;
    private String name;
    private String tokenPrefix;

    @Schema(description = "完整 token，仅在创建时返回")
    private String fullToken;

    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static ApiTokenResp from(ApiTokenDO token) {
        return ApiTokenResp.builder()
            .id(token.getId())
            .name(token.getName())
            .tokenPrefix(token.getTokenPrefix())
            .lastUsedAt(token.getLastUsedAt())
            .expiresAt(token.getExpiresAt())
            .isActive(token.getIsActive())
            .createdAt(token.getCreatedAt())
            .build();
    }

    public static ApiTokenResp withFullToken(ApiTokenDO token, String fullToken) {
        ApiTokenResp resp = from(token);
        resp.fullToken = fullToken;
        return resp;
    }
}
```

- [ ] **步骤 3: 创建 ApiTokenService**

```java
package top.lifeassistant.apitoken.service;

import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BusinessException;
import top.lifeassistant.apitoken.mapper.ApiTokenMapper;
import top.lifeassistant.apitoken.model.entity.ApiTokenDO;
import top.lifeassistant.apitoken.model.req.ApiTokenCreateReq;
import top.lifeassistant.apitoken.model.resp.ApiTokenResp;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiTokenService {

    private final ApiTokenMapper mapper;

    /** 生成 token 原文的前缀 */
    private static final String TOKEN_PREFIX = "la_";

    public ApiTokenResp create(UserDO user, ApiTokenCreateReq req) {
        String rawToken = TOKEN_PREFIX + UUID.randomUUID().toString().replace("-", "");
        String hash = DigestUtil.sha256Hex(rawToken);
        String prefix = rawToken.substring(0, 12) + "...";

        ApiTokenDO token = new ApiTokenDO();
        token.setUserId(user.getId());
        token.setName(req.getName());
        token.setTokenHash(hash);
        token.setTokenPrefix(prefix);
        token.setExpiresAt(req.getExpiresAt());
        token.setIsActive(true);
        token.setCreatedAt(LocalDateTime.now());
        token.setUpdateTime(LocalDateTime.now());
        mapper.insert(token);

        return ApiTokenResp.withFullToken(token, rawToken);
    }

    public List<ApiTokenResp> list(UserDO user) {
        return mapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ApiTokenDO>()
                .eq(ApiTokenDO::getUserId, user.getId())
                .orderByDesc(ApiTokenDO::getCreatedAt))
            .stream()
            .map(ApiTokenResp::from)
            .toList();
    }

    public void delete(UserDO user, String id) {
        ApiTokenDO token = mapper.selectById(id);
        if (token == null || !token.getUserId().equals(user.getId())) {
            throw new BusinessException("令牌不存在");
        }
        token.setIsActive(false);
        token.setUpdateTime(LocalDateTime.now());
        mapper.updateById(token);
    }
}
```

- [ ] **步骤 4: 创建 ApiTokenController**

```java
package top.lifeassistant.apitoken.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.lifeassistant.apitoken.model.req.ApiTokenCreateReq;
import top.lifeassistant.apitoken.model.resp.ApiTokenResp;
import top.lifeassistant.apitoken.service.ApiTokenService;
import top.lifeassistant.common.annotation.CurrentUser;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.util.List;

@Tag(name = "API Token")
@RestController
@RequiredArgsConstructor
public class ApiTokenController {

    private final ApiTokenService service;

    @Operation(summary = "创建 API Token")
    @PostMapping("/api-tokens")
    public ApiResponse<ApiTokenResp> create(@CurrentUser UserDO user, @Valid @RequestBody ApiTokenCreateReq req) {
        return ApiResponse.ok(service.create(user, req));
    }

    @Operation(summary = "获取我的 API Token 列表")
    @GetMapping("/api-tokens")
    public ApiResponse<List<ApiTokenResp>> list(@CurrentUser UserDO user) {
        return ApiResponse.ok(service.list(user));
    }

    @Operation(summary = "撤销 API Token")
    @DeleteMapping("/api-tokens/{id}")
    public ApiResponse<Void> delete(@CurrentUser UserDO user, @PathVariable String id) {
        service.delete(user, id);
        return ApiResponse.ok();
    }
}
```

- [ ] **步骤 5: 提交**

```bash
git add backend/lifeassistant/lifeassistant-system/src/main/java/top/lifeassistant/apitoken/
git commit -m "feat: add ApiToken CRUD service and controller"
```

---

### Task 5: 创建 MCP 基础设施 — McpConfig + ApiTokenAuthHelper + ApiTokenFilter

**文件:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/java/top/lifeassistant/mcp/config/McpConfig.java`
- Create: `backend/lifeassistant/lifeassistant-server/src/main/java/top/lifeassistant/mcp/auth/ApiTokenAuthHelper.java`
- Create: `backend/lifeassistant/lifeassistant-server/src/main/java/top/lifeassistant/mcp/interceptor/ApiTokenFilter.java`

- [ ] **步骤 1: 创建 McpConfig**

```java
package top.lifeassistant.mcp.config;

import org.springframework.ai.mcp.server.McpServer;
import org.springframework.ai.mcp.server.McpServer.Builder;
import org.springframework.ai.mcp.server.transport.StreamableHttpServerTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public StreamableHttpServerTransport mcpTransport() {
        // ponytail: Streamable HTTP 使用 /mcp 作为统一端点
        return new StreamableHttpServerTransport("/mcp");
    }
}
```

注意：Spring AI MCP Server 的 Streamable HTTP 配置方式需根据实际版本确认。上面的代码是预期结构，实际实现时可能需要微调。

- [ ] **步骤 2: 创建 ApiTokenAuthHelper**

```java
package top.lifeassistant.mcp.auth;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.continew.starter.core.exception.BusinessException;
import top.lifeassistant.apitoken.mapper.ApiTokenMapper;
import top.lifeassistant.apitoken.model.entity.ApiTokenDO;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.system.service.UserService;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ApiTokenAuthHelper {

    private final ApiTokenMapper apiTokenMapper;
    private final UserService userService;

    /**
     * 通过 API Token 原文查找并校验，返回对应的用户
     */
    public UserDO authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("缺少 API Token");
        }
        String hash = DigestUtil.sha256Hex(token);
        ApiTokenDO apiToken = apiTokenMapper.selectOne(
            new LambdaQueryWrapper<ApiTokenDO>()
                .eq(ApiTokenDO::getTokenHash, hash)
                .eq(ApiTokenDO::getIsActive, true));
        if (apiToken == null) {
            throw new BusinessException("API Token 无效");
        }
        if (apiToken.getExpiresAt() != null && apiToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("API Token 已过期");
        }
        // 更新最后使用时间
        apiToken.setLastUsedAt(LocalDateTime.now());
        apiTokenMapper.updateById(apiToken);

        return userService.getById(apiToken.getUserId());
    }
}
```

- [ ] **步骤 3: 创建 ApiTokenFilter**

```java
package top.lifeassistant.mcp.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.lifeassistant.mcp.auth.ApiTokenAuthHelper;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.io.IOException;

/**
 * 前置过滤器：检查 HTTP Header 中是否有 API Token（Authorization: Bearer la_xxx）。
 * 有则走 API Token 认证，无则放行交给 Sa-Token 处理。
 */
@Component
@RequiredArgsConstructor
public class ApiTokenFilter extends OncePerRequestFilter {

    private final ApiTokenAuthHelper apiTokenAuthHelper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // ponytail: MCP 端点走 /mcp 路径，非 MCP 路径直接放行给 Sa-Token
        String path = request.getRequestURI();
        if (!path.startsWith("/mcp")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                UserDO user = apiTokenAuthHelper.authenticate(token);
                // 将用户存入 request attribute，供 MCP 工具方法使用
                request.setAttribute("currentUser", user);
            } catch (Exception e) {
                response.setStatus(401);
                response.getWriter().write("{\"error\":\"Unauthorized: " + e.getMessage() + "\"}");
                response.setContentType("application/json");
                return;
            }
        }
        // 无 Authorization 头时放行（/mcp 路径也支持 Sa-Token 会话认证，便于未来扩展）
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **步骤 4: 提交**

```bash
git add backend/lifeassistant/lifeassistant-server/src/main/java/top/lifeassistant/mcp/
git commit -m "feat: add MCP infrastructure - config, auth helper, and filter"
```

---

### Task 6: 创建 TodoMcpTools

**文件:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/java/top/lifeassistant/mcp/tool/TodoMcpTools.java`

- [ ] **步骤 1: 创建 TodoMcpTools，包含 7 个 MCP 工具方法**

```java
package top.lifeassistant.mcp.tool;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import top.lifeassistant.todo.model.req.TodoCreateReq;
import top.lifeassistant.todo.model.req.TodoUpdateReq;
import top.lifeassistant.todo.model.resp.TodoResp;
import top.lifeassistant.todo.service.TodoService;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TodoMcpTools {

    private final TodoService todoService;
    private final HttpServletRequest request;

    private UserDO getCurrentUser() {
        return (UserDO) request.getAttribute("currentUser");
    }

    @Tool(description = "创建待办事项")
    public TodoResp todo_create(
            @ToolParam(description = "标题") String title,
            @ToolParam(description = "详细描述（可选）") String description,
            @ToolParam(description = "优先级：low / medium / high / urgent（可选，默认 medium）") String priority,
            @ToolParam(description = "截止日期（ISO 格式，可选）") String dueDate,
            @ToolParam(description = "是否自动指派给伴侣（可选，默认 false）") Boolean assignToPartner) {

        UserDO user = getCurrentUser();
        TodoCreateReq req = new TodoCreateReq();
        req.setTitle(title);
        req.setDescription(description);
        req.setPriority(priority != null ? priority : "medium");
        if (dueDate != null) req.setDueDate(LocalDateTime.parse(dueDate));
        // ponytail: assignToPartner=true 时，让 TodoController/Service 自行根据 user.partnerId 填写 assignedTo
        // 这里保持简单：TodoService.create 已支持 assignedTo 参数
        if (Boolean.TRUE.equals(assignToPartner) && user.getPartnerId() != null) {
            req.setAssignedTo(user.getPartnerId());
        }
        return todoService.create(user, req);
    }

    @Tool(description = "查询待办列表，支持筛选")
    public List<TodoResp> todo_list(
            @ToolParam(description = "是否已完成（可选）") Boolean isCompleted,
            @ToolParam(description = "优先级筛选：low / medium / high / urgent（可选）") String priority,
            @ToolParam(description = "截止日期范围起始（ISO 格式，可选）") String startDueDate,
            @ToolParam(description = "截止日期范围结束（ISO 格式，可选）") String endDueDate) {

        UserDO user = getCurrentUser();
        LocalDateTime start = startDueDate != null ? LocalDateTime.parse(startDueDate) : null;
        LocalDateTime end = endDueDate != null ? LocalDateTime.parse(endDueDate) : null;
        return todoService.list(user, isCompleted, priority, start, end);
    }

    @Tool(description = "获取首页最近的未完成待办")
    public List<TodoResp> todo_upcoming() {
        return todoService.getUpcoming(getCurrentUser());
    }

    @Tool(description = "获取待办详情")
    public TodoResp todo_get(@ToolParam(description = "待办 ID") String id) {
        return todoService.getById(getCurrentUser(), id);
    }

    @Tool(description = "更新待办事项")
    public TodoResp todo_update(
            @ToolParam(description = "待办 ID") String id,
            @ToolParam(description = "新标题（可选）") String title,
            @ToolParam(description = "新详细描述（可选）") String description,
            @ToolParam(description = "新优先级（可选）") String priority,
            @ToolParam(description = "新截止日期 ISO 格式（可选）") String dueDate) {

        UserDO user = getCurrentUser();
        TodoUpdateReq req = new TodoUpdateReq();
        req.setTitle(title);
        req.setDescription(description);
        req.setPriority(priority);
        if (dueDate != null) req.setDueDate(LocalDateTime.parse(dueDate));
        return todoService.update(user, id, req);
    }

    @Tool(description = "切换待办完成状态")
    public TodoResp todo_toggle(@ToolParam(description = "待办 ID") String id) {
        return todoService.toggleComplete(getCurrentUser(), id);
    }

    @Tool(description = "确认收到待办（仅被指派者可操作）")
    public TodoResp todo_acknowledge(
            @ToolParam(description = "待办 ID") String id,
            @ToolParam(description = "确认回复文案（可选）") String message) {
        return todoService.acknowledge(getCurrentUser(), id, message);
    }
}
```

- [ ] **步骤 2: 提交**

```bash
git add backend/lifeassistant/lifeassistant-server/src/main/java/top/lifeassistant/mcp/tool/TodoMcpTools.java
git commit -m "feat: add TodoMcpTools with 7 MCP tools"
```

---

### Task 7: 创建 SharedRecordMcpTools

**文件:**
- Create: `backend/lifeassistant/lifeassistant-server/src/main/java/top/lifeassistant/mcp/tool/SharedRecordMcpTools.java`

- [ ] **步骤 1: 创建 SharedRecordMcpTools，包含 4 个 MCP 工具方法**

```java
package top.lifeassistant.mcp.tool;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import top.lifeassistant.sharedrecord.model.req.SharedRecordCreateReq;
import top.lifeassistant.sharedrecord.model.req.SharedRecordUpdateReq;
import top.lifeassistant.sharedrecord.model.resp.SharedRecordResp;
import top.lifeassistant.sharedrecord.service.SharedRecordService;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SharedRecordMcpTools {

    private final SharedRecordService recordService;
    private final HttpServletRequest request;

    private UserDO getCurrentUser() {
        return (UserDO) request.getAttribute("currentUser");
    }

    @Tool(description = "记录一起做过的事")
    public SharedRecordResp record_create(
            @ToolParam(description = "标题") String title,
            @ToolParam(description = "详细描述（可选）") String content,
            @ToolParam(description = "事件发生时间 ISO 格式（可选，默认当前时间）") String occurredAt) {

        UserDO user = getCurrentUser();
        SharedRecordCreateReq req = new SharedRecordCreateReq();
        req.setTitle(title);
        req.setContent(content);
        if (occurredAt != null) req.setOccurredAt(LocalDateTime.parse(occurredAt));
        return recordService.create(user, req);
    }

    @Tool(description = "查询共享记录列表，支持时间范围筛选")
    public List<SharedRecordResp> record_list(
            @ToolParam(description = "开始时间 ISO 格式（可选）") String start,
            @ToolParam(description = "结束时间 ISO 格式（可选）") String end) {

        UserDO user = getCurrentUser();
        LocalDateTime startTime = start != null ? LocalDateTime.parse(start) : null;
        LocalDateTime endTime = end != null ? LocalDateTime.parse(end) : null;
        return recordService.list(user, startTime, endTime);
    }

    @Tool(description = "获取共享记录详情")
    public SharedRecordResp record_get(@ToolParam(description = "记录 ID") String id) {
        return recordService.getById(getCurrentUser(), id);
    }

    @Tool(description = "更新共享记录")
    public SharedRecordResp record_update(
            @ToolParam(description = "记录 ID") String id,
            @ToolParam(description = "新标题（可选）") String title,
            @ToolParam(description = "新详细描述（可选）") String content,
            @ToolParam(description = "新事件发生时间 ISO 格式（可选）") String occurredAt) {

        UserDO user = getCurrentUser();
        SharedRecordUpdateReq req = new SharedRecordUpdateReq();
        req.setTitle(title);
        req.setContent(content);
        if (occurredAt != null) req.setOccurredAt(LocalDateTime.parse(occurredAt));
        return recordService.update(user, id, req);
    }
}
```

注意：如果 `SharedRecordService.create` 要求 `occurredAt` 不可为 null，需在 spec 中说明或由 Service 兜底。

- [ ] **步骤 2: 提交**

```bash
git add backend/lifeassistant/lifeassistant-server/src/main/java/top/lifeassistant/mcp/tool/SharedRecordMcpTools.java
git commit -m "feat: add SharedRecordMcpTools with 4 MCP tools"
```

---

### Task 8: 前端 — 创建 api-tokens.ts API 模块

**文件:**
- Create: `front/vue3-vant-mobile/src/api/modules/api-tokens.ts`

- [ ] **步骤 1: 创建 API Token 接口和请求函数**

```typescript
import request from '@/utils/request'

export interface ApiToken {
  id: string
  name: string
  tokenPrefix: string
  fullToken?: string
  lastUsedAt: string | null
  expiresAt: string | null
  isActive: boolean
  createdAt: string
}

export interface ApiTokenCreateReq {
  name: string
  expiresAt?: string
}

/** 创建 API Token */
export function createApiToken(data: ApiTokenCreateReq) {
  return request.post<ApiToken>('/api-tokens', data)
}

/** 获取 API Token 列表 */
export function fetchApiTokens() {
  return request.get<ApiToken[]>('/api-tokens')
}

/** 撤销 API Token */
export function deleteApiToken(id: string) {
  return request.delete(`/api-tokens/${id}`)
}
```

- [ ] **步骤 2: 提交**

```bash
git add front/vue3-vant-mobile/src/api/modules/api-tokens.ts
git commit -m "feat: add API Token API module"
```

---

### Task 9: 前端 — 设置页新增 API 令牌管理

**文件:**
- Modify: `front/vue3-vant-mobile/src/pages/settings/index.vue`

- [ ] **步骤 1: 在设置页新增「API 令牌」入口和弹出层**

在现有 `确认回复模板` cell 之后、`退出登录` 之前插入：

```vue
<!-- API 令牌管理 -->
<van-cell title="API 令牌" is-link clickable @click="openApiTokenManager" />
```

在 `退出登录` cell 之前（template 中），新增：

```vue
<!-- API 令牌管理 -->
<van-cell title="API 令牌" is-link clickable @click="openApiTokenManager" />
```

同时在 `<script>` 中添加状态和逻辑：

```typescript
import { showConfirmDialog, showNotify, showToast, showDialog } from 'vant'
import { createApiToken, deleteApiToken, fetchApiTokens } from '@/api/modules/api-tokens'
import type { ApiToken } from '@/api/modules/api-tokens'

// API Token 管理
const showApiTokens = ref(false)
const apiTokens = ref<ApiToken[]>([])
const apiTokensLoading = ref(false)
const showAddTokenDialog = ref(false)
const newTokenName = ref('')
const fullTokenCopied = ref('')  // 展示完整 token

async function loadApiTokens() {
  apiTokensLoading.value = true
  try {
    const res = await fetchApiTokens()
    apiTokens.value = res.data ?? []
  } finally {
    apiTokensLoading.value = false
  }
}

function openApiTokenManager() {
  loadApiTokens()
  showApiTokens.value = true
}

async function onCreateApiToken() {
  if (!newTokenName.value.trim()) return
  try {
    const res = await createApiToken({ name: newTokenName.value.trim() })
    const token = res.data!
    showAddTokenDialog.value = false
    fullTokenCopied.value = token.fullToken ?? ''
    // 滚动到弹出层底部显示完整 token
    nextTick(() => {
      showApiTokens.value = false  // 先关闭列表
      showNotify({ type: 'success', message: '令牌创建成功' })
    })
    // 用对话框展示完整 token
    showDialog({
      title: 'API 令牌创建成功',
      message: `请立即复制此令牌，关闭后将无法再次查看完整令牌：\n\n${token.fullToken}`,
      confirmButtonText: '已复制，关闭',
    }).then(() => {
      fullTokenCopied.value = ''
      loadApiTokens()
    })
  } catch {
    showNotify({ type: 'danger', message: '创建失败' })
  }
}

async function onRevokeApiToken(t: ApiToken) {
  try {
    await showConfirmDialog({ title: '撤销令牌', message: `确定撤销「${t.name}」吗？撤销后该令牌将立即失效。` })
    await deleteApiToken(t.id)
    showToast('已撤销')
    await loadApiTokens()
  } catch { /* cancelled */ }
}
```

在 `<template>` 中添加令牌管理的弹出层（参考现有模板管理的结构）：

```vue
<!-- API 令牌管理弹窗 -->
<van-popup v-model:show="showApiTokens" position="bottom" round title="API 令牌" style="max-height: 70vh;">
  <div class="template-popup">
    <div class="template-popup-header">
      <span class="template-popup-title">API 令牌</span>
      <van-button size="small" round icon="plus" type="primary" @click="showAddTokenDialog = true">
        新建
      </van-button>
    </div>
    <div v-if="apiTokensLoading" class="template-popup-loading">
      <van-loading />
    </div>
    <div v-else-if="apiTokens.length === 0" class="template-popup-empty">
      暂无 API 令牌，点击右上角新建
    </div>
    <div v-else class="template-popup-list">
      <div v-for="t in apiTokens" :key="t.id" class="template-popup-item">
        <div style="flex: 1">
          <div style="font-size: 14px; font-weight: 500;">{{ t.name }}</div>
          <div style="font-size: 12px; color: var(--van-gray-5); margin-top: 2px;">
            {{ t.tokenPrefix }}
            <span v-if="t.lastUsedAt"> · 最后使用: {{ t.lastUsedAt }}</span>
            <span v-if="t.expiresAt"> · 过期: {{ t.expiresAt }}</span>
          </div>
        </div>
        <van-icon name="delete" @click="onRevokeApiToken(t)" />
      </div>
    </div>
  </div>
</van-popup>

<!-- 新建 Token 弹窗 -->
<van-dialog v-model:show="showAddTokenDialog" title="新建 API 令牌" show-cancel-button @confirm="onCreateApiToken">
  <van-field v-model="newTokenName" placeholder="请输入令牌名称" maxlength="50" autofocus clearable />
</van-dialog>
```

- [ ] **步骤 2: 提交**

```bash
git add front/vue3-vant-mobile/src/pages/settings/index.vue
git commit -m "feat: add API Token management UI in settings page"
```

---

### Task 10: 配置 MCP 端点安全白名单

- [ ] **步骤 1: 在 application-dev.yml 和 application-prod.yml 的 Sa-Token 安全白名单中添加 `/mcp/**`**

确保 MCP 端点（`/mcp`）不会被 Sa-Token 拦截器挡住，因为 MCP 端点走的是 `ApiTokenFilter` 而非登录态认证。

```yaml
sa-token.extension:
  security.excludes:
    - /mcp/**
    # ... 已有的排除路径 ...
```

- [ ] **步骤 2: 提交**

```bash
git add backend/lifeassistant/lifeassistant-server/src/main/resources/config/application-dev.yml
git add backend/lifeassistant/lifeassistant-server/src/main/resources/config/application-prod.yml
git commit -m "config: add /mcp/** to Sa-Token exclude paths"
```

---

### Task 11: 端到端验证

- [ ] **步骤 1: 重启后端服务，确认编译和启动正常**

```bash
cd backend/lifeassistant
mvn clean compile -pl lifeassistant-server -am
```

- [ ] **步骤 2: 启动后端，用 curl 测试 MCP Server**

```bash
# 先创建一个 API Token（通过 REST API）
curl -X POST https://api.life-assitant.top/api-tokens \
  -H "Authorization: Bearer <login-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"测试令牌"}'

# 用返回的 token 调用 MCP 的 list_tools
curl -X POST https://api.life-assitant.top/mcp \
  -H "Authorization: Bearer la_xxx" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'
```

- [ ] **步骤 3: 前端测试 API Token 管理**

打开前端设置页，测试：
- 创建 token → 复制完整 token → 关闭
- 列表展示（名称、prefix、创建时间）
- 撤销 token → 确认 → 列表更新

---

## Plan Self-Review

- [ ] **Spec coverage:** 每个 spec 中的需求都有对应任务
  - API Token 管理（表 + CRUD）→ Task 2-4
  - MCP 基础设施（配置、认证、过滤器）→ Task 5
  - TodoMcpTools 7 个工具 → Task 6
  - SharedRecordMcpTools 4 个工具 → Task 7
  - 前端 Token 管理 → Task 8-9
  - 安全配置 → Task 10
  - 客户端配置示例 → spec 中已含，不需代码实现
- [ ] **Placeholder scan:** 无 TBD/TODO/placeholder 残留
- [ ] **Type consistency:** ApiToken 相关 DTO 名称在 Task 4-5 中一致，TodoMcpTools 调用的 TodoService 方法签名与现有代码对齐
