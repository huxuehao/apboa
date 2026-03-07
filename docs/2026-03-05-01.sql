ALTER TABLE `agent_definition`
    ADD COLUMN `agent_type` varchar(100) NULL COMMENT '智能体类型' AFTER `id`,
ADD INDEX `idx_agent_type`(`agent_type`);

UPDATE `model_config` SET `model_type` = 'CUSTOM';

DROP TABLE IF EXISTS `agent_a2a`;
CREATE TABLE `agent_a2a`  (
`id` bigint NOT NULL COMMENT '主键',
`agent_definition_id` bigint NOT NULL COMMENT '智能体ID',
`a2a_type` varchar(40) NOT NULL COMMENT 'A2A类型',
`a2a_config` text NOT NULL COMMENT 'A2A配置',
PRIMARY KEY (`id`),
INDEX `idx_agent_id`(`agent_definition_id`),
INDEX `idx_agent_type`(`a2a_type`)
) comment='智能体A2A关联表';
