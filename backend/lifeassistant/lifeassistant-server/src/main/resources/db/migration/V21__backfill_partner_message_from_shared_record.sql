-- 将留言板上线前「一起做过的事」回填到 partner_message（已有 linked 记录的跳过）
INSERT INTO partner_message (id, created_by, content, image_urls, shared_record_id, todo_id, points_id, created_at)
SELECT
    UUID(),
    sr.created_by,
    CASE
        WHEN sr.content IS NOT NULL AND TRIM(sr.content) != '' THEN CONCAT(sr.title, CHAR(10), sr.content)
        ELSE sr.title
    END,
    NULL,
    sr.id,
    NULL,
    NULL,
    COALESCE(sr.occurred_at, sr.created_at)
FROM shared_record sr
WHERE NOT EXISTS (
    SELECT 1 FROM partner_message pm WHERE pm.shared_record_id = sr.id
);
