# MCP Agent 部署 + 前端配置指引 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Python MCP Agent 纳入构建部署流程，补充前端 API 令牌页的使用说明和配置模板

**Architecture:** 新增 `mcp.${DOMAIN}` 子域名和 agent Docker 容器，build-and-push.bat 新增 Agent 构建推送步骤，前端设置页增加「使用指引」Tab 展示四个客户端配置模板

**Tech Stack:** Docker, Python 3.12, Nginx, Vue 3 + Vant

---

### 文件总览

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| Create | `python/agent/Dockerfile` | Agent 多阶段 Docker 构建 |
| Modify | `python/agent/src/life_assistant_agent/server.py` | 添加 `/health` 健康检查端点 |
| Modify | `build-and-push.bat` | 新增 Agent 构建和推送步骤 |
| Modify | `backend/nginx/nginx.conf` | 新增 `mcp.${DOMAIN}` server block |
| Modify | `backend/nginx/nginx.dev.conf` | 新增 `mcp.` 子域名 dev server block |
| Modify | `front/vue3-vant-mobile/src/pages/settings/index.vue` | 新增「使用指引」Tab |

---

### Task 1: Agent Dockerfile

**Files:**
- Create: `python/agent/Dockerfile`

- [ ] **Step 1: 创建 Dockerfile**

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

- [ ] **Step 2: 验证 Dockerfile 语法**

```powershell
cd "d:\life_assistant"
docker build -t lifeassistant-agent:test -f python/agent/Dockerfile python/agent/
```

Expected: Build succeeds, image created

- [ ] **Step 3: 提交**

```powershell
git add python/agent/Dockerfile
git commit -m "feat: add agent Dockerfile with multi-stage build"
```

---

### Task 2: Agent /health 端点

**Files:**
- Modify: `python/agent/src/life_assistant_agent/server.py`

- [ ] **Step 1: 在 `AuthASGIWrapper` 中添加 `/health` 处理**

在 `AuthASGIWrapper.__call__` 方法的 `if scope["type"] == "http":` 块中，提取 token 之前的逻辑改为先判断路径是否为 `/health`。在类中添加 `_health_response` 辅助方法。

最终代码改动：

```python
class AuthASGIWrapper:
    """Wraps the MCP ASGI app to extract Bearer token without breaking lifespan."""

    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] == "http":
            path = scope.get("path", "")
            if path == "/health":
                await self._health_response(send)
                return
            headers = dict(scope.get("headers", []))
            auth = headers.get(b"authorization", b"").decode()
            if auth.startswith("Bearer "):
                _auth_token.set(auth.removeprefix("Bearer "))
        await self.app(scope, receive, send)

    @staticmethod
    async def _health_response(send: Send) -> None:
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

- [ ] **Step 2: 验证 server.py 语法**

```powershell
cd "d:\life_assistant\python\agent"
python -c "import ast; ast.parse(open('src/life_assistant_agent/server.py').read()); print('OK')"
```

Expected: `OK`

- [ ] **Step 3: 提交**

```powershell
git add python/agent/src/life_assistant_agent/server.py
git commit -m "feat: add /health endpoint to agent ASGI wrapper"
```

---

### Task 3: build-and-push.bat 更新

**Files:**
- Modify: `build-and-push.bat`

- [ ] **Step 1: 追加 Agent 变量和步骤**

在文件头部新增变量（第 7 行后插入）：

```batch
set AGENT_IMAGE=%REGISTRY%/lifeassistant-agent
```

修改步骤计数从 `[1/6]`~`[6/6]` 改为 `[1/8]`~`[8/8]`，在 nginx 构建之后、Push backend 之前插入 Agent 构建步骤：

```batch
echo [4/8] Building agent...
docker build -t %AGENT_IMAGE%:%TAG% -f python/agent/Dockerfile python/agent/
if %errorlevel% neq 0 (
    echo [FAIL] Agent build failed
    exit /b 1
)
echo [OK] Agent built
echo.
```

后续所有步骤编号依次 +2（5→7, 6→8）。在末尾输出中追加 `%AGENT_IMAGE%:%TAG%`：

```batch
echo  %AGENT_IMAGE%:%TAG%
```

最终的步骤结构：

```
echo [1/8] Building backend...
echo [2/8] Building frontend...
echo [3/8] Building nginx...
echo [4/8] Building agent...
echo [5/8] Pushing backend...
echo [6/8] Pushing frontend...
echo [7/8] Pushing nginx...
echo [8/8] Pushing agent...
```

- [ ] **Step 2: 提交**

```powershell
git add build-and-push.bat
git commit -m "feat: add agent build and push to build-and-push.bat"
```

---

### Task 4: Nginx 新增 MCP 子域名

**Files:**
- Modify: `backend/nginx/nginx.conf`
- Modify: `backend/nginx/nginx.dev.conf`

- [ ] **Step 1: 修改生产 nginx.conf**

在顶部 HTTP 80 server block 的 `server_name` 中追加 `mcp.${DOMAIN}`：

```nginx
server_name api.${DOMAIN} app.${DOMAIN} monitor.${DOMAIN} logs.${DOMAIN} mcp.${DOMAIN};
```

在文件末尾（最后一个 server block 之后）新增：

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

- [ ] **Step 2: 修改开发 nginx.dev.conf**

在文件末尾（最后一个 server block 之后）新增：

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

- [ ] **Step 3: 提交**

```powershell
git add backend/nginx/nginx.conf backend/nginx/nginx.dev.conf
git commit -m "feat: add mcp subdomain to nginx configs"
```

---

### Task 5: 前端 — API 令牌页新增 Tab 2 使用指引

**Files:**
- Modify: `front/vue3-vant-mobile/src/pages/settings/index.vue`

- [ ] **Step 1: 将 `van-popup` 的单一内容改为双 Tab 布局**

把 API 令牌弹出层（`showApiTokens` 的 `van-popup`）从单页内容改为 `van-tabs` 容器，包含两个 Tab。

```vue
    <!-- API 令牌管理弹窗 -->
    <van-popup v-model:show="showApiTokens" position="bottom" round title="API 令牌" style="max-height: 75vh;">
      <van-tabs v-model:active="apiTokenTabActive">
        <van-tab title="令牌管理">
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
                    <span v-if="t.lastUsedAt"> · 最后使用: {{ formatDate(t.lastUsedAt) }}</span>
                  </div>
                </div>
                <van-icon name="delete" @click="onRevokeApiToken(t)" />
              </div>
            </div>
          </div>
        </van-tab>
        <van-tab title="使用指引">
          <div class="guide-popup">
            <p class="guide-desc">
              API 令牌用于通过 MCP 协议在外部 AI 客户端中调用待办和共享记录功能。
            </p>
            <p class="guide-desc" style="margin-bottom: 16px;">
              MCP 服务地址：<code class="guide-code">https://mcp.life-assitant.top</code>
            </p>

            <div v-for="(tmpl, idx) in configTemplates" :key="idx" class="guide-block">
              <div class="guide-block-header">
                <span class="guide-block-title">{{ tmpl.title }}</span>
                <van-button size="mini" plain type="primary" @click="copyTemplate(tmpl.code, tmpl.title)">
                  复制
                </van-button>
              </div>
              <pre class="guide-block-code">{{ tmpl.code }}</pre>
            </div>

            <p class="guide-note">
              注意：模板中的 <code class="guide-code">la_xxx</code> 为占位符，请替换为你在「令牌管理」中创建的完整 API 令牌。
            </p>
          </div>
        </van-tab>
      </van-tabs>
    </van-popup>
```

- [ ] **Step 2: 在 `<script setup>` 中新增数据和方法**

在 `const newTokenName` 定义之后追加：

```typescript
const apiTokenTabActive = ref(0)

const configTemplates = [
  {
    title: 'Cursor (.cursor/mcp.json)',
    code: `{
  "mcpServers": {
    "life-assistant": {
      "type": "sse",
      "url": "https://mcp.life-assitant.top/sse",
      "headers": {
        "Authorization": "Bearer la_xxx"
      }
    }
  }
}`,
  },
  {
    title: 'Claude Desktop (via mcp-remote)',
    code: `{
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
}`,
  },
  {
    title: 'Claude Code CLI',
    code: `claude mcp add life-assistant --transport sse https://mcp.life-assitant.top/sse --header "Authorization: Bearer la_xxx"`,
  },
  {
    title: 'Workbuddy (CodeBuddy)',
    code: `{
  "mcpServers": {
    "life-assistant": {
      "type": "sse",
      "url": "https://mcp.life-assitant.top/sse",
      "headers": {
        "Authorization": "Bearer la_xxx"
      }
    }
  }
}`,
  },
]

async function copyTemplate(code: string, title: string) {
  try {
    await navigator.clipboard.writeText(code)
    showToast(`已复制: ${title}`)
  } catch {
    showNotify({ type: 'danger', message: '复制失败，请手动复制' })
  }
}
```

- [ ] **Step 3: 在 `<style scoped>` 中追加使用指引样式**

```css
.guide-popup {
  padding: 16px;
}
.guide-desc {
  font-size: 13px;
  color: var(--van-gray-6);
  line-height: 1.6;
  margin: 0 0 8px 0;
}
.guide-code {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}
.guide-block {
  margin-bottom: 14px;
  border: 1px solid var(--van-gray-3);
  border-radius: 8px;
  overflow: hidden;
}
.guide-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--van-gray-1);
  border-bottom: 1px solid var(--van-gray-3);
}
.guide-block-title {
  font-size: 13px;
  font-weight: 600;
}
.guide-block-code {
  font-size: 12px;
  line-height: 1.5;
  padding: 12px;
  margin: 0;
  overflow-x: auto;
  white-space: pre;
  background: #fafafa;
}
.guide-note {
  font-size: 12px;
  color: var(--van-orange);
  line-height: 1.5;
  margin: 16px 0 0 0;
  padding: 8px 12px;
  background: #fff8e1;
  border-radius: 6px;
}
```

- [ ] **Step 4: 构建前端验证**

```powershell
cd "d:\life_assistant\front\vue3-vant-mobile"
pnpm build:pro
```

Expected: Build succeeds with no TypeScript errors

- [ ] **Step 5: 提交**

```powershell
git add front/vue3-vant-mobile/src/pages/settings/index.vue
git commit -m "feat: add MCP usage guide tab with client config templates"
```

---

## 验证清单

完成后在服务器验证：

- [ ] 本地 `docker build -f python/agent/Dockerfile` 成功
- [ ] agent 容器启动后 `curl http://localhost:8089/health` 返回 `{"status":"ok"}`
- [ ] 前端 pnpm build 成功，无 TS 错误
- [ ] build-and-push.bat 运行无语法错误（dry-run 验证）
