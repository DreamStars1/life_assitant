CREATE TABLE partner_info (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_a_id CHAR(36) NOT NULL COMMENT '字典序较小的用户 ID',
    user_b_id CHAR(36) NOT NULL COMMENT '字典序较大的用户 ID',
    partner_since DATE NOT NULL COMMENT '在一起起点',
    points_balance INT NOT NULL DEFAULT 0 COMMENT '积分余额缓存',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_partner_pair (user_a_id, user_b_id),
    KEY idx_user_a (user_a_id),
    KEY idx_user_b (user_b_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='伴侣关系属性';

-- 双向绑定对回填（单向脏数据跳过）
INSERT INTO partner_info (id, user_a_id, user_b_id, partner_since, points_balance, created_at, update_time)
SELECT
    UUID(),
    IF(a.id < b.id, a.id, b.id),
    IF(a.id < b.id, b.id, a.id),
    DATE(LEAST(a.created_at, b.created_at)),
    COALESCE((
        SELECT SUM(p.points_change)
        FROM partner_points p
        WHERE p.created_by IN (a.id, b.id)
    ), 0),
    NOW(),
    NOW()
FROM user a
INNER JOIN user b ON a.partner_id = b.id AND b.partner_id = a.id
WHERE a.id < b.id;
