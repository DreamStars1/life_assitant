ALTER TABLE shared_media
    ADD COLUMN is_private TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=仅自己可见' AFTER last_watched_at;
