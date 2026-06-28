# MCP Agent 生产部署 + 前端配置补充

## 背景

Python MCP Agent（`life-assistant-agent`，端口 8089）已开发完成，作为 MCP Streamable HTTP Server 代理 Java 后端的待办和共享记录功能。当前 `build-and-push.bat` 仅构建后端、前端和 nginx 三个镜像，Agent 未纳入构建和部署流程。

需要：
1. 将 Agent 纳入 `build-and-push.bat`，支持构建和推送 Docker 镜像
2. 新增 Agent Dockerfile
3. 新增 `mcp.${DOMAIN}` 子域名，在 nginx 中增加反代配置
4. 记录服务器所需的配置变更（DNS、docker-compose 等）
5. 在前端 API 令牌设置页补充使用说明和可复制的客户端配置模板

## 1. build-and-push.bat 更新

### 新增变量

```batch
set AGENT_IMAGE=%REGISTRY%/lifeassistant-agent
```

### 新增步骤

| 改前 | 改后 |
|------|------|
| [1/6] 构建 backend | [1/8] 构建 backend |
| [2/6] 构建 frontend | [2/8] 构建 frontend |
| [3/6] 构建 nginx | [3/8] 构建 nginx |
| — | **[4/8] 构建 agent** |
| [4/6] Push backend | [5/8] Push backend |
| [5/6] Push frontend | [6/8] Push frontend |
| [6/6] Push nginx | [7/8] Push nginx |
| — | **[8/8] Push agent** |

构建上下文：`python/agent/`，Dockerfile：`python/agent/Dockerfile`

```batch
echo [4/8] Building agent...
docker build -t %AGENT_IMAGE%:%TAG% -f python/agent/Dockerfile python/agent/
if %errorlevel% neq 0 (
    echo [FAIL] Agent build failed
    exit /b 1
)
echo [OK] Agent built

rem ...（push 阶段同理，以下是 Push agent）

echo [8/8] Pushing agent...
docker push %AGENT_IMAGE%:%TAG%
```

## 2. Python Agent Dockerfile

`python/agent/Dockerfile`（新建）：

```dockerfile
# ===== 构建阶段 =====
FROM python:3.12-alpine AS build

WORKDIR /build
COPY pyproject.toml src/ ./
RUN pip install build && python -m build

# ===== 运行阶段 =====
FROM python:3.12-alpine

RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -s /bin/sh -D appuser

WORKDIR /app
COPY --from=build /build/dist/*.whl ./
RUN pip install --no-cache-dir *.whl && rm *.whl

USER appuser

EXPOSE 8089

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=10s \
    CMD wget -qO- http://localhost:8089/health || exit 1

ENV JAVA_BASE_URL=http://backend:8000
ENTRYPOINT ["life-assistant-agent"]
```

### Agent 新增 /health 端点

在 `python/agent/src/life_assistant_agent/server.py` 中，在 `AuthASGIWrapper` 内增加对 `/health` 路径的匹配，返回 200：

```python
# 在 AuthASGIWrapper.__call__ 方法中，在提取 token 后：
if scope["type"] == "http":
    path = scope.get("path", "")
    if path == "/health":
        # 直接返回 200
        await self._health_response(send)
        return
    # ... 原有 token 提取逻辑 ...

async def _health_response(self, send: Send) -> None:
    await send({
        "type": "http.response.start",
        "status": 200,
        "headers": [(b"content-type", b"application/json")],
    })
    await send({
        "type": "http.response.body",
        "body": b'{"status":"ok"}',
    })
```

## 3. Nginx 配置变更

### 生产环境 nginx.conf

- 在顶部的 `server_name` 行中增加 `mcp.${DOMAIN}`
- 新增 `mcp.${DOMAIN}` 的 server block

```nginx
# MCP Agent
server {
    listen 443 ssl;
    http2 on;
    server_name mcp.${DOMAIN};

    ssl_certificate     /etc/nginx/certs/fullchain.pem;
    ssl_certificate_key /etc/nginx/certs/privkey.pem;

    proxy_buffering off;
    proxy_request_buffering off;

    proxy_set_header Host               $host;
    proxy_set_header X-Real-IP          $remote_addr;
    proxy_set_header X-Forwarded-For    $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto  $scheme;

    location / {
        proxy_pass http://agent:8089;
    }
}
```

### 开发环境 nginx.dev.conf

```nginx
# MCP Agent
server {
    listen 80;
    server_name ~^mcp\.;

    proxy_buffering off;
    proxy_request_buffering off;

    proxy_set_header Host               $host;
    proxy_set_header X-Real-IP          $remote_addr;
    proxy_set_header X-Forwarded-For    $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto  $scheme;

    location / {
        proxy_pass http://agent:8089;
    }
}
```

## 4. 服务器配置变更

### 4.1 DNS

新增 A 记录：
```
mcp.life-assitant.top → <服务器 IP>
```

### 4.2 docker-compose 新增 agent 服务

在现有 `docker-compose.yml` 中添加：

```yaml
services:
  # ... 现有服务 ...

  agent:
    image: ccr.ccs.tencentyun.com/life-assistant/lifeassistant-agent:latest
    container_name: life-assistant-agent
    restart: unless-stopped
    networks:
      - life-assistant
    environment:
      - JAVA_BASE_URL=http://backend:8000
      - AGENT_PORT=8089
    depends_on:
      backend:
        condition: service_healthy
```

`JAVA_BASE_URL` 指向 Docker 网络中的 `backend:8000`，Agent 通过内部网络访问 Java REST API。

### 4.3 证书

如果使用通配符证书 `*.life-assitant.top`，`mcp.${DOMAIN}` 自动覆盖。否则需要将 `mcp.life-assitant.top` 加入证书签发域名列表。

### 4.4 容器网络

确保所有服务在同一 Docker 网络（`life-assistant`）中。

### 4.5 部署更新脚本

重新部署时执行：

```bash
docker pull ccr.ccs.tencentyun.com/life-assistant/lifeassistant-agent:latest
docker compose up -d agent
```

或一并更新所有服务：

```bash
docker compose pull
docker compose up -d
```

## 5. 前端设置页补充

### 5.1 Tab 布局

将 API 令牌弹出层（`van-popup`）的内容改为两个 Tab 的 `van-tabs`：

- **Tab 1「令牌管理」** — 保留现有令牌列表、新建、撤销功能
- **Tab 2「使用指引」** — 使用说明 + 四个客户端配置模板

### 5.2 Tab 1 — 令牌管理

保持现有代码不变：
- 令牌列表（名称 + prefix + 最后使用时间）
- 新建按钮 → 弹窗输入名称
- 创建后全屏展示 token + 提示关闭后无法再次查看
- 每条可撤销

### 5.3 Tab 2 — 使用指引

使用说明文字：

```html
<p style="font-size: 13px; color: var(--van-gray-6); line-height: 1.6; margin-bottom: 12px;">
  API 令牌用于通过 MCP 协议在外部 AI 客户端中调用待办和共享记录功能。
</p>
<p style="font-size: 13px; color: var(--van-gray-6); line-height: 1.6; margin-bottom: 16px;">
  MCP 服务地址：<code style="background: #f5f5f5; padding: 2px 6px; border-radius: 4px;">https://mcp.life-assitant.top</code>
</p>
```

客户端配置模板，每个模板附带「复制」按钮。使用基础 URL `https://mcp.life-assitant.top`，FastMCP 的 `streamable_http_app()` 默认路径为根路径，各客户端通过 SSE 协议透传方式连接时统一追加 `/sse`。

```
Cursor (.cursor/mcp.json)
────────────────────────────────────
{
  "mcpServers": {
    "life-assistant": {
      "type": "sse",
      "url": "https://mcp.life-assitant.top/sse",
      "headers": {
        "Authorization": "Bearer la_xxx"
      }
    }
  }
}

Claude Desktop (via mcp-remote)
────────────────────────────────────
{
  "mcpServers": {
    "life-assistant": {
      "command": "npx",
      "args": [
        "mcp-remote@latest",
        "--sse", "https://mcp.life-assitant.top",
        "--header", "Authorization: Bearer la_xxx"
      ]
    }
  }
}

Claude Code CLI
────────────────────────────────────
claude mcp add life-assistant --transport sse https://mcp.life-assitant.top/sse --header "Authorization: Bearer la_xxx"

Workbuddy (CodeBuddy)
────────────────────────────────────
{
  "mcpServers": {
    "life-assistant": {
      "type": "sse",
      "url": "https://mcp.life-assitant.top/sse",
      "headers": {
        "Authorization": "Bearer la_xxx"
      }
    }
  }
}
```

> 注意：`la_xxx` 是占位符，请替换为你在「令牌管理」中创建的完整 API 令牌。

每个模板块存储为字符串常量，点击「复制」按钮时调用 `navigator.clipboard.writeText()`，复制成功后提示"已复制"。

## 不变的部分

- Java 后端代码不动
- 现有 nginx 的 `api.${DOMAIN}` / `app.${DOMAIN}` / `monitor.${DOMAIN}` / `logs.${DOMAIN}` 配置不动
- 前端现有 API 令牌的 CRUD 逻辑不动（仅新增 Tab 2）
- 现有前端设置页其他功能不动（确认回复模板、退出登录）
- Agent 工具代码不动
