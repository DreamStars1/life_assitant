-- V4: 为所有表补充 BaseDO 通用审计字段（create_by / update_by）

ALTER TABLE `user` ADD COLUMN `create_by` CHAR(36) DEFAULT NULL COMMENT '创建人' AFTER `update_time`;
ALTER TABLE `user` ADD COLUMN `update_by` CHAR(36) DEFAULT NULL COMMENT '修改人' AFTER `create_by`;

ALTER TABLE `todo` ADD COLUMN `create_by` CHAR(36) DEFAULT NULL COMMENT '创建人' AFTER `update_time`;
ALTER TABLE `todo` ADD COLUMN `update_by` CHAR(36) DEFAULT NULL COMMENT '修改人' AFTER `create_by`;

ALTER TABLE `activity` ADD COLUMN `create_by` CHAR(36) DEFAULT NULL COMMENT '创建人' AFTER `update_time`;
ALTER TABLE `activity` ADD COLUMN `update_by` CHAR(36) DEFAULT NULL COMMENT '修改人' AFTER `create_by`;

ALTER TABLE `shared_record` ADD COLUMN `create_by` CHAR(36) DEFAULT NULL COMMENT '创建人' AFTER `update_time`;
ALTER TABLE `shared_record` ADD COLUMN `update_by` CHAR(36) DEFAULT NULL COMMENT '修改人' AFTER `create_by`;

ALTER TABLE `push_subscription` ADD COLUMN `create_by` CHAR(36) DEFAULT NULL COMMENT '创建人' AFTER `update_time`;
ALTER TABLE `push_subscription` ADD COLUMN `update_by` CHAR(36) DEFAULT NULL COMMENT '修改人' AFTER `create_by`;

ALTER TABLE `todo_ack_template` ADD COLUMN `create_by` CHAR(36) DEFAULT NULL COMMENT '创建人' AFTER `update_time`;
ALTER TABLE `todo_ack_template` ADD COLUMN `update_by` CHAR(36) DEFAULT NULL COMMENT '修改人' AFTER `create_by`;

ALTER TABLE `api_token` ADD COLUMN `create_by` CHAR(36) DEFAULT NULL COMMENT '创建人' AFTER `update_time`;
ALTER TABLE `api_token` ADD COLUMN `update_by` CHAR(36) DEFAULT NULL COMMENT '修改人' AFTER `create_by`;
