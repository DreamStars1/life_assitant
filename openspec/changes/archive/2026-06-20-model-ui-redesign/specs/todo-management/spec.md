## MODIFIED Requirements

### Requirement: Todo 状态简化为 is_completed

Todo 模型 SHALL 将 `status` 四态（pending/in_progress/done/cancelled）替换为 `is_completed`（bool）字段。

- is_completed=false 表示待处理或已取消（未完成）
- is_completed=true 表示已完成
- completed_at（datetime?）记录完成时间
- cancelled_at（datetime?）记录取消时间
- **Reason**: 情侣场景不需要复杂的工作流状态，简化减少 UI 复杂度
- **Migration**: status='done' → is_completed=true；其他 → is_completed=false

#### Scenario: 标记待办完成

- **WHEN** 用户在待办列表中勾选一个待办
- **THEN** is_completed 变为 true，completed_at 记录当前时间

#### Scenario: 取消已完成的待办

- **WHEN** 用户取消勾选一个已完成的待办
- **THEN** is_completed 变为 false，completed_at 清空
