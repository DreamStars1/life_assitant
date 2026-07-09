-- V6__create_shared_media_tables.sql

CREATE TABLE shared_media (
    id          CHAR(36)     PRIMARY KEY,
    created_by  CHAR(36)     NOT NULL,
    title       VARCHAR(255) NOT NULL,
    media_type  VARCHAR(20)  NOT NULL COMMENT 'movie | book | tv',
    cover_path  VARCHAR(256) DEFAULT NULL COMMENT '服务器本地存储路径',
    description TEXT         DEFAULT NULL,
    is_finished TINYINT(1)   DEFAULT 0 COMMENT '双方都看完了',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   CHAR(36)     DEFAULT NULL,
    update_by   CHAR(36)     DEFAULT NULL,
    INDEX idx_created_by (created_by),
    INDEX idx_media_type (media_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE media_comment (
    id         CHAR(36)     PRIMARY KEY,
    media_id   CHAR(36)     NOT NULL,
    user_id    CHAR(36)     NOT NULL,
    content    TEXT         NOT NULL,
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_media_id (media_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE media_progress (
    id            CHAR(36)      PRIMARY KEY,
    media_id      CHAR(36)      NOT NULL,
    user_id       CHAR(36)      DEFAULT NULL COMMENT 'NULL=共同进度(scope=shared), 非NULL=个人进度(scope=personal)',
    progress_text VARCHAR(100)  NOT NULL COMMENT '如"第5集/共24集"',
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_media_user (media_id, user_id),
    INDEX idx_media_id (media_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
