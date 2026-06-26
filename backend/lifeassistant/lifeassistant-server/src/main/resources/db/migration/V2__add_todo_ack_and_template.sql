-- Flyway V2: todo 表新增确认字段 + 新建确认文案模板表

ALTER TABLE todo
  ADD COLUMN ack_status  VARCHAR(16)  NOT NULL DEFAULT 'none'
    COMMENT '确认状态：none=个人待办 / unconfirmed=待确认 / confirmed=已确认';

ALTER TABLE todo
  ADD COLUMN ack_message VARCHAR(100) DEFAULT NULL
    COMMENT '确认回复文案';

CREATE TABLE todo_ack_template (
    id          CHAR(36)     NOT NULL COMMENT 'UUID 主键',
    user_id     CHAR(36)     NOT NULL COMMENT '所属用户',
    content     VARCHAR(100) NOT NULL COMMENT '文案内容',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序顺序',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='确认回复文案模板';
