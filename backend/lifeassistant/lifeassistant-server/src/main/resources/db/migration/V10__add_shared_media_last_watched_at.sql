ALTER TABLE shared_media
    ADD COLUMN last_watched_at DATE DEFAULT NULL COMMENT '上次一起看日期' AFTER finished_at;
