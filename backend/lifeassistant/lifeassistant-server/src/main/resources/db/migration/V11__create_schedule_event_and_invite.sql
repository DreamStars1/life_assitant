CREATE TABLE schedule_event (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    title VARCHAR(200) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    note VARCHAR(500) DEFAULT NULL,
    recurrence VARCHAR(10) NOT NULL DEFAULT 'none' COMMENT 'none/daily/weekly',
    recurrence_end_date DATE DEFAULT NULL,
    linked_event_id CHAR(36) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) DEFAULT NULL,
    update_by VARCHAR(64) DEFAULT NULL,
    INDEX idx_schedule_event_user_start (user_id, start_at),
    INDEX idx_schedule_event_linked (linked_event_id)
);

CREATE TABLE schedule_invite (
    id CHAR(36) PRIMARY KEY,
    source_event_id CHAR(36) NOT NULL,
    inviter_user_id CHAR(36) NOT NULL,
    invitee_user_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/accepted/rejected',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at DATETIME DEFAULT NULL,
    INDEX idx_schedule_invite_invitee (invitee_user_id, status),
    INDEX idx_schedule_invite_source (source_event_id)
);
