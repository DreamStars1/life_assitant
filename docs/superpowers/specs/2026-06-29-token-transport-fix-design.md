# Token 列表过滤 & MCP 传输类型修正

## 背景

在 MCP 服务集成完成后发现两个问题：

1. **撤销的令牌仍在列表中显示**：`ApiTokenService.list()` 查询时不区分 `is_active` 状态，导致前端列表混入了已软删除的令牌
2. **前端配置模板传输类型错误**：Python Agent 实际使用 FastMCP 的 `streamable_http_app()`（Streamable HTTP 传输），但前端提供的配置模板写的是 `"type": "sse"` 和 `/sse` URL 后缀，与实际情况不符

## 方案

### 修复 1：Token 列表过滤 isActive

**问题定位：** `ApiTokenService.list()` 的 MyBatis-Plus LambdaQueryWrapper 只有 `userId` 过滤条件，缺少 `isActive = true` 条件。

**改动：**

```java
// ApiTokenService.java line 45-51
return mapper.selectList(
    new LambdaQueryWrapper<ApiTokenDO>()
        .eq(ApiTokenDO::getUserId, user.getId())
        .eq(ApiTokenDO::getIsActive, true)    // ← 新增过滤
        .orderByDesc(ApiTokenDO::getCreatedAt))
    .stream()
    .map(ApiTokenResp::from)
    .toList();
```

**影响：** 仅影响列表查询，不影响删除（软删除）逻辑。已撤销的令牌不再出现在前端列表中，但数据库保留审计痕迹。

---

### 修复 2：配置模板改为 Streamable HTTP

**问题定位：** 前端 `settings/index.vue` 中的 `configTemplates` 数组使用了 SSE 传输格式，但 Python Agent 是 Streamable HTTP Server。

**修正对照表：**

| 客户端 | 原配置 | 新配置 |
|--------|--------|--------|
| **Cursor** | `"type": "sse"` + `url: .../sse` | `"url": "https://mcp.life-assitant.top"`，省略 type |
| **Claude Desktop** (mcp-remote) | `--sse` + URL 末尾不加路径 | 去掉 `--sse`，URL 保持原样 |
| **Claude Code CLI** | `--transport sse` | `--transport http` |
| **Workbuddy** | 同 Cursor | 同 Cursor |

**关键点：**
- Streamable HTTP 是 MCP 规范中的标准传输协议，Cursor 原生支持
- FastMCP 的 `streamable_http_app()` 监听根路径 `/`，不需要 `/sse` 后缀
- Nginx 配置已将所有 `mcp.${DOMAIN}` 请求代理到 agent:8089，无需修改
- `cursor-agent` CLI 接受 `"type": "http"` 但不接受 `"type": "streamable-http"`，省略 type 最安全
- mcp-remote 去掉 `--sse` 后的默认传输行为未经测试。如果无法连接，可回退为保留 `--sse` 参数并将 URL 改为 `https://mcp.life-assitant.top/sse`，Nginx 层按原样透传

**影响范围：** 仅 `settings/index.vue` 中的模板字符串，Nginx、Python Agent、后端均无需改动。

---

## 不变的部分

- 删除令牌的 Service 方法 `ApiTokenService.delete()` 不动（逻辑正确）
- Nginx 配置不动
- Python Agent (`server.py`) 不动
- Java Controller 层不动
- 前端页面结构、交互逻辑不动

## 验证方式

1. 创建令牌 → 前端列表可见
2. 撤销令牌 → 前端列表不再显示该令牌
3. 复制 Cursor 配置模板 → 检查 `mcp.json` 格式：正确格式为无 `type` 字段，URL 无 `/sse` 后缀
