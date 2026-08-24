CREATE TABLE `partner_message` (
    `id` CHAR(36) NOT NULL COMMENT 'UUID 主键',
    `created_by` CHAR(36) NOT NULL COMMENT '作者用户 ID',
    `content` TEXT NULL COMMENT '文字；纯图可空',
    `image_urls` JSON NULL COMMENT '图片相对路径数组',
    `shared_record_id` CHAR(36) NULL COMMENT '同步记事 ID',
    `todo_id` CHAR(36) NULL COMMENT '同步待办 ID',
    `points_id` CHAR(36) NULL COMMENT '同步积分流水 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_created_by` (`created_by`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='伴侣留言板';
