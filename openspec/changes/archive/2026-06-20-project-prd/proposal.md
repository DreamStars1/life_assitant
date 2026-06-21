## Why

情侣/个人需要一个智能生活助手，通过微信自然语言交互来管理待办、日程、生活记录，并通过 AI 分析生成建议。现有方案要么功能单一，要么缺乏协作共享能力。本项目旨在构建一个覆盖"对话录入→数据沉淀→AI分析→推送闭环"的完整工具链，2 人起步，架构可扩展至 10 人以下。

## What Changes

- **新增**：微信 Bot 对话交互，通过 iLink 长轮询收发消息，LLM 解析意图并路由到对应技能
- **新增**：待办事项管理，支持创建/查看/完成/分配，含优先级、分类、截止时间、提醒
- **新增**：日程事件管理，支持 CRUD、重复规则（RRULE）、提前提醒、伴侣共享
- **新增**：生活记录（饮食/运动/工作/心情/睡眠），支持结构化元数据存储
- **新增**：AI 分析引擎，基于生活数据生成日/周/月分析报告和个性化建议
- **新增**：伴侣协作共享，两人共享待办/日程，互相分配任务
- **新增**：Web Push 推送通知，支持待办提醒、日程提醒、每日摘要、周报推送
- **新增**：PWA 管理界面（Vue3 + Vant），支持移动端安装、离线访问
- **新增**：Docker Compose 一键部署，含 MySQL、Redis、Nginx、SSL 证书

## Capabilities

### New Capabilities

- `wechat-bot`: 微信 Bot 接入，通过 iLink 长轮询收发消息，LLM 意图识别与技能路由
- `todo-management`: 待办事项全生命周期管理（创建/查看/完成/分配），含优先级、分类、截止时间与提醒
- `event-management`: 日程事件管理，支持重复规则、提前提醒、伴侣共享
- `life-logging`: 生活记录（饮食/运动/工作/心情/睡眠），结构化元数据存储
- `ai-analysis`: AI 分析引擎，基于生活数据生成分析报告与个性化建议
- `partner-collaboration`: 伴侣协作共享，两人互相查看/分配待办与日程
- `push-notification`: Web Push 推送通知，含定时提醒、每日摘要、周报
- `pwa-app`: PWA 管理界面，移动端安装、离线访问、数据可视化

### Modified Capabilities

<!-- 无已有 capability 需要修改 -->

## Impact

- **后端**：FastAPI 应用，含 Bot Router / API Router / Push Router 三大路由模块，Service 层 + Agent 层架构
- **前端**：Vue3 + Vant + Pinia + ECharts 构建 PWA，支持 Service Worker 离线缓存
- **数据库**：MySQL 8.0，新增 users/todos/events/life_logs/conversations/push_subscriptions/push_logs/shared_data/analysis_results 九张表
- **外部依赖**：DeepSeek API（LLM）、iLink API（微信）、Web Push API（FCM 推送）
- **基础设施**：Docker Compose 管理 MySQL 8.0 + Redis 7 + Nginx + 后端 + 前端五大服务
- **部署**：需云服务器（2C4G 起步）、域名、SSL 证书
