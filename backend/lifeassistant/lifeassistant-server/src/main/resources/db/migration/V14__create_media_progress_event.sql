CREATE TABLE media_progress_event (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    media_id      VARCHAR(36)  NOT NULL,
    user_id       VARCHAR(36)  NOT NULL,
    scope         VARCHAR(16)  NOT NULL COMMENT 'shared|personal',
    progress_text VARCHAR(512) NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mpe_media_created (media_id, created_at)
) COMMENT '媒体进度变更历史';
