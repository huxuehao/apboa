ALTER TABLE `model_config`
    ADD COLUMN `connectivity_status` enum('NOT_CHECKED','CHECKING','CONNECTED','FAILED') NOT NULL DEFAULT 'NOT_CHECKED' COMMENT '连接性检测状态' AFTER `extend_config`,
    ADD COLUMN `connectivity_message` varchar(500) NULL DEFAULT NULL COMMENT '连接性检测消息' AFTER `connectivity_status`,
    ADD COLUMN `last_connectivity_check` datetime NULL DEFAULT NULL COMMENT '最后连接性检测时间' AFTER `connectivity_message`;
