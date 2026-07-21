-- V15__media_comment_images.sql
ALTER TABLE media_comment
    MODIFY COLUMN content TEXT NULL,
    ADD COLUMN image_urls JSON NULL COMMENT '评论图片相对路径数组' AFTER content;
