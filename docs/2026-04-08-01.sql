DROP TABLE IF EXISTS `skill_tools`;
CREATE TABLE `skill_tools` (
`id` bigint NOT NULL,
`skill_id` bigint NOT NULL,
`tool_id` bigint NOT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE KEY `uk_skill_tool` (`skill_id`,`tool_id`) USING BTREE,
KEY `idx_skill_id` (`skill_id`) USING BTREE,
KEY `idx_tool_id` (`tool_id`) USING BTREE
) COMMENT='技能工具关联表';
