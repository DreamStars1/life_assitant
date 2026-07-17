CREATE TABLE partner_points (
    id CHAR(36) PRIMARY KEY,
    created_by CHAR(36) NOT NULL,
    points_change INT NOT NULL,
    reason VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
