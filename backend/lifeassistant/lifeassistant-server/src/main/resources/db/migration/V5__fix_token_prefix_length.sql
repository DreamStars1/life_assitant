-- Flyway V5: 修复 token_prefix 列长度过短导致的插入失败
-- 代码生成前缀格式为 "la_xxxxxxxxxxxx..." 共 15 字符
ALTER TABLE api_token
    MODIFY COLUMN token_prefix VARCHAR(20) NOT NULL COMMENT '令牌前缀（前端展示用）';
