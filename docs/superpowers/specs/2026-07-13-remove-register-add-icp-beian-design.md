# 删除注册按钮 & 悬挂 ICP 备案号

## 概述

对前端移动端应用做两处改动：移除注册入口（登录页按钮 + 路由守卫），以及在登录页和设置页底部悬挂 ICP 备案号。

## 改动一：删除注册按钮

### 背景

不再允许用户自行注册，只保留管理员/预置账号体系。

### 具体变更

| 文件 | 操作 |
|------|------|
| `front/vue3-vant-mobile/src/pages/login/index.vue` | 删除第 90-92 行的 `<GhostButton to="register">` |
| `front/vue3-vant-mobile/src/router/index.ts` | `PUBLIC_ROUTES` 数组中移除 `'Register'` |
| `front/vue3-vant-mobile/src/App.vue` | `PUBLIC_ROUTES` 数组中移除 `'Register'` |
| `front/vue3-vant-mobile/src/pages/register/index.vue` | 移到 `_backup/register/index.vue` |

### 影响

- `/register` 路由不再被注册，auto-routes 也不会生成该路由
- 已登录用户不会被重定向到 Today（因为路由不存在了）
- 备份文件保留，需要时可恢复

## 改动二：悬挂 ICP 备案号

### 背景

根据工信部要求，网站底部需悬挂备案号并链接至工信部官网。腾讯云文档（`https://cloud.tencent.com/document/product/243/61412`）要求的代码格式：

```html
<a href="https://beian.miit.gov.cn/" target="_blank">您的备案号</a>
```

### 备案号

`苏ICP备2026047415号-1`

### 悬挂位置

**登录页（公开页面）：**

在登录表单底部，添加备案号链接。样式：居中、小字、灰色。

```
┌────────────────────┐
│       logo         │
│  邮箱               │
│  密码               │
│  [登录]             │
│                    │
│ 苏ICP备2026047415号-1│
└────────────────────┘
```

**设置页（私有页面，仅已登录用户可见）：**

在版本号行下方追加备案号。

```
┌────────────────────┐
│  ...设置项...       │
│  退出登录           │
│                    │
│  当前版本: v0.x.x   │
│ 苏ICP备2026047415号-1│
└────────────────────┘
```

### 样式要求

- 字体大小 `12px`
- 颜色 `var(--van-gray-5)`（与版本号一致）
- 去除下划线（`text-decoration: none`）
- 点击在新标签页打开工信部官网

## 不涉及

- 不改动后端代码
- 不新增依赖
- 不改动其他页面
- 不改动英文语言包（备案号是中文要求，保持统一即可）
