ALTER TABLE partner_points
    ADD COLUMN pending_record_date DATE NULL COMMENT '待确认的目标业务日' AFTER created_at,
    ADD COLUMN pending_requested_by CHAR(36) NULL COMMENT '改日期发起人' AFTER pending_record_date,
    ADD COLUMN pending_requested_at DATETIME NULL COMMENT '发起时间(24h过期)' AFTER pending_requested_by;
