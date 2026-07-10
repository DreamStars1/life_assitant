ALTER TABLE shared_media
    ADD COLUMN finished_at DATETIME DEFAULT NULL COMMENT '看完时间' AFTER is_finished;
