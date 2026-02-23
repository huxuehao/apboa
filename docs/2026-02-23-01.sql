DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
`id` bigint NOT NULL COMMENT '会话ID',
`user_id` bigint NOT NULL COMMENT '用户ID',
`agent_id` bigint NOT NULL COMMENT '智能体ID',
`current_message_id` int DEFAULT NULL COMMENT '当前消息ID',
`title` varchar(255) DEFAULT NULL COMMENT '会话标题',
`is_pinned` tinyint(1) DEFAULT '0' COMMENT '是否置顶',
`pin_time` datetime DEFAULT NULL COMMENT '置顶时间',
`created_at` datetime DEFAULT NULL COMMENT '创建时间',
`updated_at` datetime DEFAULT NULL COMMENT '更新时间',
PRIMARY KEY (`id`) USING BTREE
) COMMENT='聊天会话表';

DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
`id` int NOT NULL AUTO_INCREMENT COMMENT '消息ID',
`session_id` bigint NOT NULL COMMENT '会话ID',
`role` varchar(20) NOT NULL COMMENT '消息角色',
`content` text NOT NULL COMMENT '消息内容',
`parent_id` int DEFAULT NULL COMMENT '父消息ID',
`path` varchar(2048) DEFAULT NULL COMMENT '消息路径，格式如：/1/2/3/',
`depth` int DEFAULT NULL COMMENT '消息深度，从0开始，根消息深度为0',
`created_at` datetime DEFAULT NULL COMMENT '创建时间',
PRIMARY KEY (`id`) USING BTREE,
KEY `idx_session` (`session_id`) USING BTREE COMMENT '会话ID索引',
KEY `idx_parent` (`parent_id`) USING BTREE COMMENT '父消息ID索引',
KEY `idx_path` (`path`(255)) USING BTREE COMMENT '消息路径前缀索引'
) COMMENT='聊天消息表';
