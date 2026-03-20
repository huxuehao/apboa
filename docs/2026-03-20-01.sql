DROP TABLE IF EXISTS `studio_config`;
CREATE TABLE `studio_config` (
`id` bigint NOT NULL COMMENT 'ID',
`url` varchar(40)  NOT NULL COMMENT 'Studio Url',
`project` varchar(60) NOT NULL COMMENT 'project',
PRIMARY KEY (`id`)
) COMMENT='Studio配置';

DROP TABLE IF EXISTS `agent_studio`;
CREATE TABLE `agent_studio` (
`id` bigint NOT NULL,
`agent_definition_id` bigint NOT NULL,
`studio_id` bigint NOT NULL,
PRIMARY KEY (`id`),
KEY `idx_studio` (`studio_id`),
KEY `idx_agent` (`agent_definition_id`),
KEY `idx_agent_studio` (`agent_definition_id`,`studio_id`)
) COMMENT='智能体与Studio关联表';
