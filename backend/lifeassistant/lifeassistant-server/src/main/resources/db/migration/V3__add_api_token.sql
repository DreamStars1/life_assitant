-- Flyway V3: 新增 api_token 表（MCP 认证用）

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
