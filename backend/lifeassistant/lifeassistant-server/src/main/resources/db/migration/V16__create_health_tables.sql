-- V16__create_health_tables.sql
CREATE TABLE health_profile (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    display_name VARCHAR(64) NULL,
    motto VARCHAR(128) NULL,
    height_cm DECIMAL(5,2) NULL,
    target_kg DECIMAL(5,2) NULL COMMENT 'NULL = 待设置',
    resting_kcal INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_health_profile_user (user_id)
);

CREATE TABLE health_meal (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    meal_date DATE NOT NULL,
    meal_type VARCHAR(8) NOT NULL COMMENT '早餐/午餐/晚餐',
    food VARCHAR(512) NOT NULL,
    protein_g DECIMAL(6,1) NULL,
    feedback VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_health_meal (user_id, meal_date, meal_type),
    KEY idx_health_meal_user_date (user_id, meal_date)
);

CREATE TABLE health_weight (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    weight_date DATE NOT NULL,
    weight_type VARCHAR(8) NOT NULL COMMENT '晨重/晚重',
    kg DECIMAL(5,2) NOT NULL,
    standard TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=纳入趋势',
    note VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_health_weight_user_date (user_id, weight_date)
);

CREATE TABLE health_daily (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    daily_date DATE NOT NULL,
    stomach_status VARCHAR(32) NULL,
    stomach_note VARCHAR(128) NULL,
    cycle_phase VARCHAR(32) NULL,
    cycle_day INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_health_daily (user_id, daily_date)
);

CREATE TABLE health_memory (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    title VARCHAR(128) NOT NULL,
    detail VARCHAR(512) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_health_memory_user (user_id)
);

CREATE TABLE health_tolerance (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    name VARCHAR(64) NOT NULL,
    level VARCHAR(16) NOT NULL COMMENT '舒适/低风险/谨慎/高风险',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_health_tolerance_user (user_id)
);

CREATE TABLE health_trigger (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    name VARCHAR(64) NOT NULL,
    note VARCHAR(128) NULL,
    stars TINYINT NOT NULL DEFAULT 3,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_health_trigger_user (user_id)
);
