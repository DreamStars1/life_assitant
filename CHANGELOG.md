# Life Assistant 更新日志

---

## v1.2.0 (2026-07-10)

### ✨ 新特性

- **共享媒体**：新增进度管理 — 支持标记/取消"已看完"、自动记录看完时间
- **共享媒体**：媒体列表支持滑动删除，带确认弹窗
- **共享媒体**：新增 `finished_at` 字段记录看完日期，列表页展示

### 🖥 后端

- `SharedMediaDO/Resp` 增加 `finishedAt` 字段
- `PATCH /shared-media/{id}` 支持 `isFinished` 参数，自动更新 `finished_at`
- Flyway: 新增 `V7__add_finished_at.sql` 迁移
- 版本号: `1.0.0-SNAPSHOT` → `1.1.0-SNAPSHOT`

### 📱 前端

- 共享媒体列表页：`van-swipe-cell` 左滑删除 + `showConfirmDialog` 确认弹窗
- 媒体详情页：进度弹窗增加「标记为已看完」开关，保存时同步更新 `isFinished`
- 列表页：已看完项目旁显示完成日期（如 `07-10`）
- 版本号: `1.1.0` → `1.2.0`
