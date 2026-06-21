-- Flyway V1: Life Assistant 初始建表
-- 5 张核心表：user / todo / activity / shared_record / push_subscription

-- ============================================
-- 1. user — 用户表
-- ============================================
CREATE TABLE `user` (
    `id` CHAR(36) NOT NULL COMMENT 'UUID 主键',
    `email` VARCHAR(255) NOT NULL COMMENT '登录邮箱',
    `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt 哈希',
    `full_name` VARCHAR(255) DEFAULT NULL COMMENT '显示名称',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否激活',
    `is_superuser` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否超级管理员',
    `partner_id` CHAR(36) DEFAULT NULL COMMENT '伴侣用户 ID（自引用）',
    `timezone` VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '时区',
    `push_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '推送开关',
    `quiet_hours_start` TIME DEFAULT NULL COMMENT '静默起始',
    `quiet_hours_end` TIME DEFAULT NULL COMMENT '静默结束',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_partner_id` (`partner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. todo — 待办表
-- ============================================
CREATE TABLE `todo` (
    `id` CHAR(36) NOT NULL COMMENT 'UUID 主键',
    `user_id` CHAR(36) NOT NULL COMMENT '创建者',
    `title` VARCHAR(255) NOT NULL COMMENT '标题',
    `description` VARCHAR(1024) DEFAULT NULL COMMENT '描述',
    `is_completed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否完成',
    `priority` VARCHAR(10) NOT NULL DEFAULT 'medium' COMMENT '优先级：low/medium/high/urgent',
    `category` VARCHAR(64) DEFAULT NULL COMMENT '分类标签',
    `due_date` DATETIME DEFAULT NULL COMMENT '截止时间',
    `assigned_to` CHAR(36) DEFAULT NULL COMMENT '被分配者',
    `assigned_by` CHAR(36) DEFAULT NULL COMMENT '分配者',
    `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    `cancelled_at` DATETIME DEFAULT NULL COMMENT '取消时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_assigned_to` (`assigned_to`),
    KEY `idx_due_date` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办表';

-- ============================================
-- 3. activity — 时间块活动表
-- ============================================
CREATE TABLE `activity` (
    `id` CHAR(36) NOT NULL COMMENT 'UUID 主键',
    `user_id` CHAR(36) NOT NULL COMMENT '所属用户',
    `title` VARCHAR(255) NOT NULL COMMENT '标题',
    `description` VARCHAR(1024) DEFAULT NULL COMMENT '描述',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间（可为 null = 时间点）',
    `category` VARCHAR(64) DEFAULT NULL COMMENT '分类标签',
    `color` VARCHAR(7) DEFAULT NULL COMMENT '展示色 #RRGGBB',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='时间块活动表';

-- ============================================
-- 4. shared_record — 共享记录表（★第一优先）
-- ============================================
CREATE TABLE `shared_record` (
    `id` CHAR(36) NOT NULL COMMENT 'UUID 主键',
    `created_by` CHAR(36) NOT NULL COMMENT '记录者',
    `title` VARCHAR(255) NOT NULL COMMENT '标题',
    `content` VARCHAR(2048) DEFAULT NULL COMMENT '详细描述',
    `occurred_at` DATETIME DEFAULT NULL COMMENT '事件发生时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_created_by` (`created_by`),
    KEY `idx_occurred_at` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='共享记录表';

-- ============================================
-- 5. push_subscription — Web Push 订阅表
-- ============================================
CREATE TABLE `push_subscription` (
    `id` CHAR(36) NOT NULL COMMENT 'UUID 主键',
    `user_id` CHAR(36) NOT NULL COMMENT '所属用户',
    `endpoint` VARCHAR(1024) NOT NULL COMMENT 'Push Service URL',
    `p256dh` VARCHAR(256) NOT NULL COMMENT 'DH 公钥',
    `auth` VARCHAR(256) NOT NULL COMMENT '认证密钥',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否活跃',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Web Push 订阅表';
