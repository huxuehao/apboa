CREATE TABLE `agent_chat_key` (
`agent_code` varchar(100) NOT NULL COMMENT '智能体code',
`chat_key` varchar(100) NOT NULL COMMENT 'chat key',
UNIQUE KEY `uniq_agent_code_chat_key` (`agent_code`,`chat_key`),
KEY `idx_agent_code` (`agent_code`),
KEY `idx_chat_key` (`chat_key`)
) COMMENT='智能体对话Key';
