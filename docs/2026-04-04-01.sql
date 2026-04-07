ALTER TABLE `tool_config`
    MODIFY COLUMN `description` text NOT NULL COMMENT '工具描述' AFTER `tool_id`;

DROP TABLE IF EXISTS `secret_key`;
CREATE TABLE `secret_key` (
`id` bigint NOT NULL COMMENT '主键ID',
`name` varchar(100) NOT NULL COMMENT '秘钥名称（业务可读）',
`value` varchar(128) DEFAULT NULL COMMENT '密钥',
`enabled` tinyint DEFAULT '1' COMMENT '状态 1-启用 0-禁用',
`expire_time` datetime DEFAULT NULL COMMENT '过期时间（为空表示不过期）',
`created_by` bigint NOT NULL COMMENT '创建人',
`created_at` datetime NOT NULL COMMENT '创建时间',
`updated_by` bigint DEFAULT NULL COMMENT '更新人',
`updated_at` datetime DEFAULT NULL COMMENT '更新时间',
`remark` varchar(255) DEFAULT NULL COMMENT '备注',
PRIMARY KEY (`id`),
KEY `idx_value` (`value`),
KEY `idx_status` (`enabled`),
KEY `idx_expire_time` (`expire_time`)
) COMMENT='访问秘钥';
