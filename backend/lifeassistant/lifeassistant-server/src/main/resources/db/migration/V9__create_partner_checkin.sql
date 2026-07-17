CREATE TABLE partner_checkin (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    checkin_type VARCHAR(10) NOT NULL COMMENT 'wake/sleep',
    checkin_time DATETIME NOT NULL,
    checkin_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date_type (user_id, checkin_date, checkin_type)
);
