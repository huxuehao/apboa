SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 创建数据库（不存在则创建）
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `apboa` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `apboa`;

-- ----------------------------
-- Table structure for account
-- ----------------------------
DROP TABLE IF EXISTS `account`;
CREATE TABLE `account`  (
`id` bigint NOT NULL COMMENT '主键',
`nickname` varchar(10) NULL DEFAULT NULL COMMENT '昵称',
`email` varchar(100) NULL DEFAULT NULL COMMENT '邮箱',
`username` varchar(40) NULL DEFAULT NULL COMMENT '用户名',
`password` varchar(100) NULL DEFAULT NULL COMMENT '密码',
`enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`created_by` bigint NULL DEFAULT NULL,
`updated_by` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE,
UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
INDEX `idx_enabled`(`enabled` ASC) USING BTREE
) COMMENT = '账号';

INSERT INTO `account` (`id`, `nickname`, `email`, `username`, `password`, `enabled`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (1111111111111111111, '管理员', 'admin@gmail.com', 'admin', '277fc0217db5d364b3b886a9672ea9d3', 1, '2026-02-07 18:50:51', '2026-02-12 22:26:50', NULL, 1111111111111111111);

-- ----------------------------
-- Table structure for account_role
-- ----------------------------
DROP TABLE IF EXISTS `account_role`;
CREATE TABLE `account_role`  (
`id` bigint NOT NULL COMMENT 'ID',
`account_id` bigint NOT NULL COMMENT '账号ID',
`role` enum('READ_ONLY','EDIT','ADMIN') NOT NULL COMMENT '角色（\'READ_ONLY\',\'EDIT\',\'ADMIN\'）',
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_account_role`(`account_id` ASC, `role` ASC) USING BTREE,
INDEX `idx_account_id`(`account_id` ASC) USING BTREE,
INDEX `idx_role`(`role` ASC) USING BTREE
) COMMENT = '账号与Role关联表';

INSERT INTO `account_role` (`id`, `account_id`, `role`) VALUES (1111111111111111111, 1111111111111111111, 'ADMIN');

-- ----------------------------
-- Table structure for agent_a2a
-- ----------------------------
DROP TABLE IF EXISTS `agent_a2a`;
CREATE TABLE `agent_a2a`  (
`id` bigint NOT NULL COMMENT '主键',
`agent_definition_id` bigint NOT NULL COMMENT '智能体ID',
`a2a_type` varchar(40) NOT NULL COMMENT 'A2A类型',
`a2a_config` text NOT NULL COMMENT 'A2A配置',
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_agent_id`(`agent_definition_id` ASC) USING BTREE,
INDEX `idx_agent_type`(`a2a_type` ASC) USING BTREE
) COMMENT = '智能体A2A关联表';

-- ----------------------------
-- Table structure for agent_chat_key
-- ----------------------------
DROP TABLE IF EXISTS `agent_chat_key`;
CREATE TABLE `agent_chat_key`  (
`agent_code` varchar(100) NOT NULL COMMENT '智能体code',
`chat_key` varchar(100) NOT NULL COMMENT 'chat key',
UNIQUE INDEX `uniq_agent_code_chat_key`(`agent_code` ASC, `chat_key` ASC) USING BTREE,
INDEX `idx_agent_code`(`agent_code` ASC) USING BTREE,
INDEX `idx_chat_key`(`chat_key` ASC) USING BTREE
) COMMENT = '智能体对话Key';

-- ----------------------------
-- Table structure for agent_code_execution
-- ----------------------------
DROP TABLE IF EXISTS `agent_code_execution`;
CREATE TABLE `agent_code_execution`  (
`id` bigint NOT NULL,
`agent_definition_id` bigint NOT NULL,
`code_execution_id` bigint NOT NULL,
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_code_execution`(`code_execution_id` ASC) USING BTREE,
INDEX `idx_agent`(`agent_definition_id` ASC) USING BTREE,
INDEX `idx_agent_code_execution`(`agent_definition_id` ASC, `code_execution_id` ASC) USING BTREE
) COMMENT = '智能体与代码执行环境配置关联表';

-- ----------------------------
-- Table structure for agent_definition
-- ----------------------------
DROP TABLE IF EXISTS `agent_definition`;
CREATE TABLE `agent_definition`  (
`id` bigint NOT NULL,
`agent_type` varchar(100) NULL DEFAULT NULL COMMENT '智能体类型',
`name` varchar(100) NOT NULL COMMENT '智能体名称',
`agent_code` varchar(100) NOT NULL COMMENT '智能体代码（英文小写下划线）',
`description` text NULL COMMENT '智能体描述',
`model_config_id` bigint NULL DEFAULT NULL COMMENT '基础模型配置ID',
`model_params_override` text NULL COMMENT '模型参数覆盖',
`tool_choice_strategy` enum('AUTO','NONE','REQUIRED','SPECIFIC') NULL DEFAULT 'AUTO' COMMENT '工具选择策略',
`specific_tool_name` varchar(100) NULL DEFAULT NULL COMMENT '指定工具名称（当tool_choice_strategy=SPECIFIC时）',
`system_prompt_template_id` bigint NULL DEFAULT NULL COMMENT '系统提示词模板ID',
`follow_template` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否跟随模板变化',
`system_prompt` text NULL COMMENT '系统提示词内容（当不跟随模板或模板为空时使用）',
`sensitive_word_config_id` bigint NULL DEFAULT NULL COMMENT '敏感词配置ID',
`sensitive_filter_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用敏感词过滤',
`max_iterations` int NULL DEFAULT 10 COMMENT 'React最大迭代次数',
`enable_planning` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用计划',
`show_tool_process` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否显示工具调用过程',
`max_subtasks` int NULL DEFAULT 10 COMMENT '最大子任务数',
`require_plan_confirmation` tinyint(1) NOT NULL DEFAULT 0 COMMENT '计划是否需要确认',
`enable_memory` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用记忆',
`enable_memory_compression` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用记忆压缩',
`memory_compression_config` text NULL COMMENT '记忆压缩配置',
`structured_output_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用结构化输出',
`structured_output_reminder` enum('PROMPT','TOOL_CHOICE') NULL DEFAULT NULL COMMENT '结构化输出模式',
`structured_output_schema` text NULL COMMENT '结构化输出模板/JSON Schema',
`enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用',
`version` varchar(20) NULL DEFAULT '1.0.0' COMMENT '版本号',
`tag` varchar(100) NULL DEFAULT NULL COMMENT '标签',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`created_by` bigint NULL DEFAULT NULL,
`updated_by` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_agent_code`(`agent_code` ASC) USING BTREE,
UNIQUE INDEX `uk_agent_name`(`name` ASC) USING BTREE,
INDEX `idx_agent_enabled`(`enabled` ASC) USING BTREE,
INDEX `idx_agent_type`(`agent_type` ASC) USING BTREE
) COMMENT = '智能体定义表';

-- ----------------------------
-- Table structure for agent_hooks
-- ----------------------------
DROP TABLE IF EXISTS `agent_hooks`;
CREATE TABLE `agent_hooks`  (
`id` bigint NOT NULL,
`agent_definition_id` bigint NOT NULL,
`hook_config_id` bigint NOT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_agent_hook`(`agent_definition_id` ASC, `hook_config_id` ASC) USING BTREE,
INDEX `idx_agent_id`(`agent_definition_id` ASC) USING BTREE,
INDEX `idx_hook_id`(`hook_config_id` ASC) USING BTREE
) COMMENT = '智能体与Hook关联表';

-- ----------------------------
-- Table structure for agent_knowledge_bases
-- ----------------------------
DROP TABLE IF EXISTS `agent_knowledge_bases`;
CREATE TABLE `agent_knowledge_bases`  (
`id` bigint NOT NULL,
`agent_definition_id` bigint NOT NULL,
`knowledge_base_config_id` bigint NOT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_agent_kb`(`agent_definition_id` ASC, `knowledge_base_config_id` ASC) USING BTREE,
INDEX `idx_agent_id`(`agent_definition_id` ASC) USING BTREE,
INDEX `idx_kb_id`(`knowledge_base_config_id` ASC) USING BTREE
) COMMENT = '智能体与知识库关联表';

-- ----------------------------
-- Table structure for agent_mcp_servers
-- ----------------------------
DROP TABLE IF EXISTS `agent_mcp_servers`;
CREATE TABLE `agent_mcp_servers`  (
`id` bigint NOT NULL,
`agent_definition_id` bigint NOT NULL,
`mcp_server_id` bigint NOT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_agent_mcp`(`agent_definition_id` ASC, `mcp_server_id` ASC) USING BTREE,
INDEX `idx_agent_id`(`agent_definition_id` ASC) USING BTREE,
INDEX `idx_mcp_id`(`mcp_server_id` ASC) USING BTREE
) COMMENT = '智能体与MCP服务器关联表';

-- ----------------------------
-- Table structure for agent_skill_packages
-- ----------------------------
DROP TABLE IF EXISTS `agent_skill_packages`;
CREATE TABLE `agent_skill_packages`  (
`id` bigint NOT NULL,
`agent_definition_id` bigint NOT NULL,
`skill_package_id` bigint NOT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_agent_skill`(`agent_definition_id` ASC, `skill_package_id` ASC) USING BTREE,
INDEX `idx_agent_id`(`agent_definition_id` ASC) USING BTREE,
INDEX `idx_skill_id`(`skill_package_id` ASC) USING BTREE
) COMMENT = '智能体与技能包关联表';

-- ----------------------------
-- Table structure for agent_studio
-- ----------------------------
DROP TABLE IF EXISTS `agent_studio`;
CREATE TABLE `agent_studio`  (
`id` bigint NOT NULL,
`agent_definition_id` bigint NOT NULL,
`studio_id` bigint NOT NULL,
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_studio`(`studio_id` ASC) USING BTREE,
INDEX `idx_agent`(`agent_definition_id` ASC) USING BTREE,
INDEX `idx_agent_studio`(`agent_definition_id` ASC, `studio_id` ASC) USING BTREE
) COMMENT = '智能体与Studio关联表';

-- ----------------------------
-- Table structure for agent_sub_agents
-- ----------------------------
DROP TABLE IF EXISTS `agent_sub_agents`;
CREATE TABLE `agent_sub_agents`  (
`id` bigint NOT NULL,
`parent_agent_id` bigint NOT NULL,
`sub_agent_id` bigint NOT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_parent_sub_agent`(`parent_agent_id` ASC, `sub_agent_id` ASC) USING BTREE,
INDEX `idx_parent_agent`(`parent_agent_id` ASC) USING BTREE,
INDEX `idx_sub_agent`(`sub_agent_id` ASC) USING BTREE
) COMMENT = '智能体与子智能体关联表';

-- ----------------------------
-- Table structure for agent_tools
-- ----------------------------
DROP TABLE IF EXISTS `agent_tools`;
CREATE TABLE `agent_tools`  (
`id` bigint NOT NULL,
`agent_definition_id` bigint NOT NULL,
`tool_id` bigint NOT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_agent_tool`(`agent_definition_id` ASC, `tool_id` ASC) USING BTREE,
INDEX `idx_agent_id`(`agent_definition_id` ASC) USING BTREE,
INDEX `idx_tool_id`(`tool_id` ASC) USING BTREE
) COMMENT = '智能体与工具关联表';

-- ----------------------------
-- Table structure for agentscope_sessions
-- ----------------------------
DROP TABLE IF EXISTS `agentscope_sessions`;
CREATE TABLE `agentscope_sessions`  (
`session_id` varchar(255) NOT NULL,
`state_key` varchar(255) NOT NULL,
`item_index` int NOT NULL DEFAULT 0,
`state_data` longtext NOT NULL,
`created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
PRIMARY KEY (`session_id`, `state_key`, `item_index`) USING BTREE
);

-- ----------------------------
-- Table structure for attach
-- ----------------------------
DROP TABLE IF EXISTS `attach`;
CREATE TABLE `attach`  (
`id` bigint NOT NULL COMMENT '主键',
`file_id` bigint NULL DEFAULT NULL COMMENT '文件id',
`link` varchar(1000) NULL DEFAULT NULL COMMENT '附件地址',
`domain` varchar(500) NULL DEFAULT NULL COMMENT '附件域名',
`name` varchar(500) NULL DEFAULT NULL COMMENT '附件名称',
`original_name` varchar(500) NULL DEFAULT NULL COMMENT '附件原名',
`extension` varchar(12) NULL DEFAULT NULL COMMENT '附件拓展名',
`attach_size` bigint NULL DEFAULT NULL COMMENT '附件大小',
`path` varchar(255) NULL DEFAULT NULL COMMENT '存储路径',
`create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
`create_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
`update_by` bigint NULL DEFAULT NULL COMMENT '修改人',
`update_at` datetime NULL DEFAULT NULL COMMENT '修改时间',
`protocol` varchar(40) NULL DEFAULT NULL COMMENT '存储协议',
`status` int NULL DEFAULT NULL COMMENT '状态',
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_attach_file_id`(`file_id` ASC) USING BTREE,
INDEX `idx_attach_name`(`name` ASC) USING BTREE,
INDEX `idx_attach_path`(`path` ASC) USING BTREE,
INDEX `idx_attach_protocol_status`(`protocol` ASC, `status` ASC) USING BTREE,
INDEX `idx_attach_create_at`(`create_at` ASC) USING BTREE,
INDEX `idx_attach_protocol_create`(`protocol` ASC, `create_at` ASC) USING BTREE,
INDEX `idx_attach_extension`(`extension` ASC) USING BTREE
) COMMENT = '附件表';

-- ----------------------------
-- Table structure for attach_chunk
-- ----------------------------
DROP TABLE IF EXISTS `attach_chunk`;
CREATE TABLE `attach_chunk`  (
`id` bigint NOT NULL COMMENT '主键',
`chunk_hash` varchar(40) NULL DEFAULT NULL COMMENT '分片的hash值',
`chunk_index` int NULL DEFAULT NULL COMMENT '分片的索引',
`chunk_totals` int NULL DEFAULT NULL COMMENT '分片总数',
`file_key` varchar(40) NULL DEFAULT NULL COMMENT '文件唯一标识',
`file_total_size` int NULL DEFAULT NULL COMMENT '文件大小',
`file_name` varchar(255) NULL DEFAULT NULL COMMENT '文件名称',
`create_by` bigint NULL DEFAULT NULL COMMENT '创建人',
`create_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_chunk_file_key`(`file_key` ASC) USING BTREE,
INDEX `idx_chunk_create_at`(`create_at` ASC) USING BTREE
) COMMENT = '附件表分片记录表';

-- ----------------------------
-- Table structure for attach_log
-- ----------------------------
DROP TABLE IF EXISTS `attach_log`;
CREATE TABLE `attach_log`  (
`id` bigint NOT NULL COMMENT '主键',
`file_id` bigint NULL DEFAULT NULL COMMENT '文件id',
`original_name` varchar(500) NULL DEFAULT NULL COMMENT '附件原名',
`extension` varchar(12) NULL DEFAULT NULL COMMENT '附件拓展名',
`attach_size` bigint NULL DEFAULT NULL COMMENT '附件大小',
`opt_user` bigint NULL DEFAULT NULL COMMENT '操作人',
`opt_user_name` varchar(40) NULL DEFAULT NULL COMMENT '操作人名称',
`opt_time` datetime NULL DEFAULT NULL COMMENT '操作时间',
`opt_ip` varchar(20) NULL DEFAULT NULL COMMENT '操作IP',
`opt_type` varchar(10) NULL DEFAULT NULL COMMENT '操作类型',
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_log_file_id`(`file_id` ASC) USING BTREE,
INDEX `idx_log_opt_time`(`opt_time` ASC) USING BTREE,
INDEX `idx_log_time_type`(`opt_time` ASC, `opt_type` ASC) USING BTREE
) COMMENT = '附件操作日志表';

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
`id` int NOT NULL AUTO_INCREMENT COMMENT '消息ID',
`session_id` bigint NOT NULL COMMENT '会话ID',
`role` varchar(20) NOT NULL COMMENT '消息角色',
`content` text NOT NULL COMMENT '消息内容',
`parent_id` int NULL DEFAULT NULL COMMENT '父消息ID',
`path` text NULL DEFAULT NULL COMMENT '消息路径，格式如：/1/2/3/',
`depth` int NULL DEFAULT NULL COMMENT '消息深度，从0开始，根消息深度为0',
`created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_session`(`session_id` ASC) USING BTREE COMMENT '会话ID索引',
INDEX `idx_parent`(`parent_id` ASC) USING BTREE COMMENT '父消息ID索引',
INDEX `idx_path`(`path`(255) ASC) USING BTREE COMMENT '消息路径前缀索引'
) COMMENT = '聊天消息表';

-- ----------------------------
-- Table structure for chat_session
-- ----------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session`  (
`id` bigint NOT NULL COMMENT '会话ID',
`user_id` bigint NOT NULL COMMENT '用户ID',
`agent_id` bigint NOT NULL COMMENT '智能体ID',
`current_message_id` int NULL DEFAULT NULL COMMENT '当前消息ID',
`title` varchar(255) NULL DEFAULT NULL COMMENT '会话标题',
`is_pinned` tinyint(1) NULL DEFAULT 0 COMMENT '是否置顶',
`pin_time` datetime NULL DEFAULT NULL COMMENT '置顶时间',
`created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
`updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
PRIMARY KEY (`id`) USING BTREE
) COMMENT = '聊天会话表';

-- ----------------------------
-- Table structure for code_execution_config
-- ----------------------------
DROP TABLE IF EXISTS `code_execution_config`;
CREATE TABLE `code_execution_config`  (
`id` bigint NOT NULL COMMENT '主键',
`config_name` varchar(128) NOT NULL COMMENT '配置名称，便于识别',
`work_dir` varchar(512) NULL DEFAULT NULL COMMENT '工作目录，空则使用临时目录',
`upload_dir` varchar(512) NULL DEFAULT NULL COMMENT '脚本上传目录，空则使用work_dir/skills',
`auto_upload` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否自动上传skill文件，0=false',
`enable_shell` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用ShellCommandTool',
`enable_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用ReadFileTool',
`enable_write` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用WriteFileTool',
`command` varchar(300) NOT NULL COMMENT '允许执行的命令，如 python3、bash',
`enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`created_by` bigint NULL DEFAULT NULL,
`updated_by` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `config_name`(`config_name` ASC) USING BTREE
) COMMENT = '代码执行环境配置';

-- ----------------------------
-- Table structure for hook_config
-- ----------------------------
DROP TABLE IF EXISTS `hook_config`;
CREATE TABLE `hook_config`  (
`id` bigint NOT NULL,
`name` varchar(100) NOT NULL COMMENT 'Hook名称',
`hook_type` enum('BUILTIN','CUSTOM') NOT NULL COMMENT 'Hook类型',
`description` varchar(500) NULL DEFAULT NULL COMMENT '描述',
`class_path` varchar(255) NULL DEFAULT NULL COMMENT 'hook路径（hook_type为BUILTIN时使用）',
`code` text NULL COMMENT 'hook内容（thook_type为CUSTOM时使用）',
`enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用',
`priority` int NOT NULL DEFAULT 0 COMMENT '执行优先级',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`created_by` bigint NULL DEFAULT NULL,
`updated_by` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_hook_type`(`hook_type` ASC) USING BTREE,
INDEX `idx_hook_enabled`(`enabled` ASC) USING BTREE
) COMMENT = 'Hook配置表';

-- ----------------------------
-- Table structure for knowledge_base_config
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_base_config`;
CREATE TABLE `knowledge_base_config`  (
`id` bigint NOT NULL,
`name` varchar(100) NOT NULL COMMENT '知识库名称',
`kb_type` enum('BAILIAN','DIFY','RAGFLOW','LOCAL') NOT NULL COMMENT '知识库类型',
`rag_mode` enum('GENERIC','AGENTIC') NOT NULL DEFAULT 'GENERIC' COMMENT '集成模式',
`description` varchar(500) NULL DEFAULT NULL COMMENT '描述',
`connection_config` text NOT NULL COMMENT '连接配置',
`endpoint_config` text NULL COMMENT '端点配置',
`retrieval_config` text NULL COMMENT '检索配置',
`reranking_config` text NULL COMMENT '重排序配置',
`query_rewrite_config` text NULL COMMENT '查询重写配置',
`metadata_filters` text NULL COMMENT '元数据过滤',
`http_config` text NULL COMMENT 'HTTP配置',
`enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用',
`health_status` enum('HEALTHY','UNHEALTHY','UNKNOWN') NULL DEFAULT 'UNKNOWN' COMMENT '健康状态',
`last_sync_time` datetime NULL DEFAULT NULL COMMENT '最后同步时间',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`created_by` bigint NULL DEFAULT NULL,
`updated_by` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_kb_name`(`name` ASC) USING BTREE,
INDEX `idx_kb_type`(`kb_type` ASC) USING BTREE,
INDEX `idx_kb_enabled`(`enabled` ASC) USING BTREE
) COMMENT = '知识库配置表';

-- ----------------------------
-- Table structure for mcp_server
-- ----------------------------
DROP TABLE IF EXISTS `mcp_server`;
CREATE TABLE `mcp_server`  (
`id` bigint NOT NULL,
`name` varchar(100) NOT NULL COMMENT '服务器名称',
`protocol` enum('HTTP','SSE','STDIO') NOT NULL COMMENT '协议类型',
`enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用',
`mode` enum('SYNC','ASYNC') NOT NULL DEFAULT 'SYNC' COMMENT '运行模式',
`timeout` int NULL DEFAULT 30 COMMENT '超时时间（秒）',
`protocol_config` text NULL COMMENT '协议配置',
`description` varchar(500) NULL DEFAULT NULL COMMENT '描述',
`health_status` enum('HEALTHY','UNHEALTHY','UNKNOWN') NULL DEFAULT 'UNKNOWN' COMMENT '健康状态',
`last_health_check` datetime NULL DEFAULT NULL COMMENT '最后健康检查时间',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`created_by` bigint NULL DEFAULT NULL,
`updated_by` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_mcp_name`(`name` ASC) USING BTREE,
INDEX `idx_mcp_protocol`(`protocol` ASC) USING BTREE,
INDEX `idx_mcp_enabled`(`enabled` ASC) USING BTREE
) COMMENT = 'MCP服务器配置表';

INSERT INTO `mcp_server` (`id`, `name`, `protocol`, `enabled`, `mode`, `timeout`, `protocol_config`, `description`, `health_status`, `last_health_check`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2024821058360176641, 'lbs-amap-http-mcp', 'HTTP', 1, 'SYNC', 30000, '{\"url\":\"https://mcp.amap.com/mcp\",\"queryParams\":[{\"key\":\"key\",\"value\":\"yourself key\"}],\"headers\":[]}', '高度地图 HTTP MCP', 'UNKNOWN', NULL, '2026-02-20 20:18:54', '2026-02-20 23:52:37', 1111111111111111111, 1111111111111111111);
INSERT INTO `mcp_server` (`id`, `name`, `protocol`, `enabled`, `mode`, `timeout`, `protocol_config`, `description`, `health_status`, `last_health_check`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2024825210448453633, 'lbs-amap-sse-mcp', 'SSE', 1, 'SYNC', 30000, '{\"url\":\"https://mcp.amap.com/sse\",\"queryParams\":[{\"key\":\"key\",\"value\":\"yourself key\"}],\"headers\":[]}', '高度地图 SSE MCP', 'UNKNOWN', NULL, '2026-02-20 20:35:24', '2026-02-20 23:52:51', 1111111111111111111, 1111111111111111111);
INSERT INTO `mcp_server` (`id`, `name`, `protocol`, `enabled`, `mode`, `timeout`, `protocol_config`, `description`, `health_status`, `last_health_check`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2024829036727439361, 'daidu-map-http-mcp', 'HTTP', 0, 'SYNC', 30000, '{\"url\":\"https://mcp.map.baidu.com/mcp\",\"queryParams\":[{\"key\":\"ak\",\"value\":\"yourself key\"}],\"headers\":[]}', '百度地图 HTTP MCP', 'UNKNOWN', NULL, '2026-02-20 20:50:37', '2026-02-22 00:11:27', 1111111111111111111, 1111111111111111111);
INSERT INTO `mcp_server` (`id`, `name`, `protocol`, `enabled`, `mode`, `timeout`, `protocol_config`, `description`, `health_status`, `last_health_check`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2024829212489748481, 'baidu-amp-sse-mcp', 'SSE', 0, 'SYNC', 30000, '{\"url\":\"https://mcp.map.baidu.com/sse\",\"queryParams\":[{\"key\":\"ak\",\"value\":\"yourself key\"}],\"headers\":[]}', '百度地图 SSE MCP', 'UNKNOWN', NULL, '2026-02-20 20:51:18', '2026-02-22 00:11:32', 1111111111111111111, 1111111111111111111);
INSERT INTO `mcp_server` (`id`, `name`, `protocol`, `enabled`, `mode`, `timeout`, `protocol_config`, `description`, `health_status`, `last_health_check`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2024830349192269825, 'baidu-maps', 'STDIO', 1, 'SYNC', 30000, '{\"command\":\"D:\\\\\\\\environment\\\\\\\\nvm\\\\\\\\nodejs\\\\\\\\npx.cmd\",\"args\":[\"-y\",\"@baidumap/mcp-server-baidu-map\"],\"env\":[{\"key\":\"BAIDU_MAP_API_KEY\",\"value\":\"yourself key\"}],\"encoding\":\"UTF-8\"}', '百度地图 STDIO MCP', 'UNKNOWN', NULL, '2026-02-20 20:55:49', '2026-02-20 21:02:07', 1111111111111111111, 1111111111111111111);

-- ----------------------------
-- Table structure for model_config
-- ----------------------------
DROP TABLE IF EXISTS `model_config`;
CREATE TABLE `model_config`  (
`id` bigint NOT NULL,
`provider_id` bigint NOT NULL COMMENT '提供商ID',
`name` varchar(100) NOT NULL COMMENT '模型名称',
`model_id` varchar(100) NOT NULL COMMENT '模型编号/标识符',
`model_type` varchar(100) NULL DEFAULT NULL COMMENT '模型类型',
`description` varchar(500) NULL DEFAULT NULL COMMENT '模型描述',
`streaming` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否支持流式',
`thinking` tinyint(1) NULL DEFAULT NULL COMMENT '是否支持思考',
`context_window` int NULL DEFAULT 2048 COMMENT '上下文窗口大小',
`max_tokens` int NULL DEFAULT 2000 COMMENT '最大输出token数',
`temperature` decimal(3, 2) NULL DEFAULT 0.70 COMMENT '温度参数',
`top_p` decimal(3, 2) NULL DEFAULT 0.90 COMMENT '核采样参数',
`top_k` int NULL DEFAULT 40 COMMENT 'Top-K采样',
`repeat_penalty` decimal(3, 2) NULL DEFAULT 1.10 COMMENT '重复惩罚',
`seed` bigint NULL DEFAULT 42 COMMENT '随机种子',
`extend_config` text NULL COMMENT '扩展配置',
`enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`created_by` bigint NULL DEFAULT NULL,
`updated_by` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_model_type`(`model_type` ASC) USING BTREE,
INDEX `idx_model_enabled`(`enabled` ASC) USING BTREE,
INDEX `idx_model_id`(`model_id` ASC) USING BTREE,
INDEX `idx_provider_id`(`provider_id` ASC) USING BTREE
) COMMENT = '模型配置表';

-- ----------------------------
-- Table structure for model_provider
-- ----------------------------
DROP TABLE IF EXISTS `model_provider`;
CREATE TABLE `model_provider`  (
`id` bigint NOT NULL,
`type` varchar(50) NOT NULL COMMENT '提供商类型: DashScope, OpenAI, Anthropic, Gemini, Ollama',
`name` varchar(100) NOT NULL COMMENT '提供商名称',
`description` varchar(500) NULL DEFAULT NULL COMMENT '提供商描述',
`base_url` varchar(500) NULL DEFAULT NULL COMMENT '基础URL',
`auth_type` enum('CONFIG','ENV') NOT NULL DEFAULT 'CONFIG' COMMENT '认证类型: 直接配置/环境变量',
`api_key` varchar(500) NULL DEFAULT NULL COMMENT '加密后的API密钥（当auth_type=CONFIG时使用）',
`env_var_name` varchar(100) NULL DEFAULT NULL COMMENT '环境变量名（当auth_type=ENV时使用）',
`enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用',
`config_meta` text NULL COMMENT '提供商特定配置元数据',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`created_by` bigint NULL DEFAULT NULL,
`updated_by` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_provider_name`(`name` ASC) USING BTREE,
INDEX `idx_provider_type`(`type` ASC) USING BTREE,
INDEX `idx_provider_enabled`(`enabled` ASC) USING BTREE
) COMMENT = '模型提供商表';

-- ----------------------------
-- Table structure for params
-- ----------------------------
DROP TABLE IF EXISTS `params`;
CREATE TABLE `params`  (
`id` bigint NOT NULL COMMENT '主键',
`param_name` varchar(40) NULL DEFAULT NULL COMMENT '参数名称',
`param_key` varchar(40) NULL DEFAULT NULL COMMENT '参数Key',
`param_value` varchar(255) NULL DEFAULT NULL COMMENT '参数Value',
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_param_key`(`param_key` ASC) USING BTREE,
INDEX `idx_param_name`(`param_name` ASC) USING BTREE
) COMMENT = '参数表';

-- ----------------------------
-- Table structure for quartz_job_info
-- ----------------------------
DROP TABLE IF EXISTS `quartz_job_info`;
CREATE TABLE `quartz_job_info`  (
`id` varchar(64) NOT NULL COMMENT '任务身份唯一标识',
`type` varchar(100) NULL DEFAULT NULL COMMENT '类型',
`biz_id` varchar(64) NULL DEFAULT NULL COMMENT '关联业务ID',
`cron` varchar(64) NULL DEFAULT NULL COMMENT 'cron',
`job_class` varchar(100) NULL DEFAULT NULL COMMENT 'job类路径',
`data_map` text NULL COMMENT '执行参数',
`enabled` tinyint(1) NULL DEFAULT NULL COMMENT '状态（0停止 1启动）',
PRIMARY KEY (`id`) USING BTREE
) COMMENT = 'quartz定时任务状态';

-- ----------------------------
-- Table structure for quartz_job_log
-- ----------------------------
DROP TABLE IF EXISTS `quartz_job_log`;
CREATE TABLE `quartz_job_log`  (
`id` varchar(64) NOT NULL COMMENT '定时任务日志主键',
`identity` varchar(255) NULL DEFAULT NULL COMMENT '任务身份标识',
`start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
`end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
`content` text NULL COMMENT '执行情况（1标识成功，0标识失败）',
`status` varchar(64) NULL DEFAULT NULL COMMENT '状态',
`duration` decimal(11, 0) NULL DEFAULT NULL COMMENT '持续时间（秒）',
PRIMARY KEY (`id`) USING BTREE
) COMMENT = 'quartz定时任务日志';

-- ----------------------------
-- Table structure for rag_document
-- ----------------------------
DROP TABLE IF EXISTS `rag_document`;
CREATE TABLE `rag_document`  (
`id` bigint NOT NULL,
`knowledge_base_config_id` bigint NOT NULL COMMENT '关联的知识库配置ID',
`file_name` varchar(500) NOT NULL COMMENT '文件名',
`file_path` varchar(1000) NOT NULL COMMENT '文件存储路径',
`file_size` bigint NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
`file_type` varchar(50) NOT NULL COMMENT '文件类型(pdf/txt/docx/xlsx/md等)',
`chunk_count` int NOT NULL DEFAULT 0 COMMENT '分块数量',
`status` enum('PENDING','PROCESSING','COMPLETED','FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
`error_message` text NULL COMMENT '错误信息',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`created_by` bigint NULL DEFAULT NULL,
`updated_by` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_kb_config_id`(`knowledge_base_config_id` ASC) USING BTREE,
INDEX `idx_status`(`status` ASC) USING BTREE
) COMMENT = 'RAG文档表';

-- ----------------------------
-- Table structure for rag_document_chunk
-- ----------------------------
DROP TABLE IF EXISTS `rag_document_chunk`;
CREATE TABLE `rag_document_chunk` (
`id` bigint NOT NULL,
`document_id` bigint NOT NULL COMMENT '关联的文档ID',
`file_name` varchar(500) NOT NULL DEFAULT '' COMMENT '文件名',
`chunk_index` int NOT NULL COMMENT '分块序号',
`content` text NOT NULL COMMENT '分块文本内容',
`token_count` int DEFAULT NULL COMMENT 'Token数量(估算)',
`start_offset` int DEFAULT NULL COMMENT '在原文中的起始偏移',
`end_offset` int DEFAULT NULL COMMENT '在原文中的结束偏移',
`metadata` text COMMENT '元数据(JSON)',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
KEY `idx_document_id` (`document_id`),
KEY `idx_chunk_index` (`document_id`,`chunk_index`)
) COMMENT='RAG文档分块表';

-- ----------------------------
-- Table structure for secret_key
-- ----------------------------
DROP TABLE IF EXISTS `secret_key`;
CREATE TABLE `secret_key`  (
`id` bigint NOT NULL COMMENT '主键ID',
`name` varchar(100) NOT NULL COMMENT '秘钥名称（业务可读）',
`value` varchar(500) NULL DEFAULT NULL COMMENT '密钥',
`enabled` tinyint NULL DEFAULT 1 COMMENT '状态 1-启用 0-禁用',
`expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间（为空表示不过期）',
`created_by` bigint NOT NULL COMMENT '创建人',
`created_at` datetime NOT NULL COMMENT '创建时间',
`updated_by` bigint NULL DEFAULT NULL COMMENT '更新人',
`updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
`remark` varchar(255) NULL DEFAULT NULL COMMENT '备注',
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_value`(`value` ASC) USING BTREE,
INDEX `idx_status`(`enabled` ASC) USING BTREE,
INDEX `idx_expire_time`(`expire_time` ASC) USING BTREE
) COMMENT = '访问秘钥';

-- ----------------------------
-- Table structure for sensitive_word_config
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_word_config`;
CREATE TABLE `sensitive_word_config`  (
`id` bigint NOT NULL,
`category` varchar(100) NULL DEFAULT NULL COMMENT '分类',
`name` varchar(100) NOT NULL COMMENT '配置名称',
`description` varchar(500) NULL DEFAULT NULL COMMENT '描述',
`words` text NOT NULL COMMENT '敏感词列表',
`action` enum('BLOCK','REPLACE','WARN') NOT NULL DEFAULT 'BLOCK' COMMENT '处理动作',
`replacement` varchar(50) NULL DEFAULT '***' COMMENT '替换文本（当action=REPLACE时使用）',
`enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`created_by` bigint NULL DEFAULT NULL,
`updated_by` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_sw_name_category`(`category` ASC, `name` ASC) USING BTREE,
INDEX `idx_sw_category`(`category` ASC) USING BTREE,
INDEX `idx_sw_enabled`(`enabled` ASC) USING BTREE
) COMMENT = '敏感词配置表';

-- ----------------------------
-- Table structure for skill_package
-- ----------------------------
DROP TABLE IF EXISTS `skill_package`;
CREATE TABLE `skill_package`  (
`id` bigint NOT NULL,
`name` varchar(500) NOT NULL COMMENT '技能包名称',
`description` text NOT NULL COMMENT '技能描述',
`skill_content` text NULL COMMENT '技能内容（概述）',
`category` varchar(100) NULL DEFAULT NULL COMMENT '技能分类',
`references` longtext NULL COMMENT '资源列表: [{\"name\": \"api_docs.md\", \"content\": \"...\"}]',
`examples` longtext NULL COMMENT '示例列表',
`scripts` longtext NULL COMMENT '脚本列表',
`enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`created_by` bigint NULL DEFAULT NULL,
`updated_by` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_skill_name`(`name` ASC) USING BTREE,
INDEX `idx_skill_enabled`(`enabled` ASC) USING BTREE,
INDEX `idx_skill_category`(`category` ASC) USING BTREE
) COMMENT = '技能包表';

INSERT INTO `skill_package` (`id`, `name`, `description`, `skill_content`, `category`, `references`, `examples`, `scripts`, `enabled`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2038859803983978498, 'doGetCurrentTime', '通过技能你可以获取当前时间', '执行下面的命令获取当前时间\npython skills/doGetCurrentTime/scripts/getCurrentTime.py', '通用', NULL, NULL, '[{\"prefix\":\"scripts\",\"name\":\"getCurrentTime.py\",\"content\":\"from datetime import datetime\\n\\nnow = datetime.now()\\nweekdays = [\'Monday\', \'Tuesday\', \'Wednesday\', \'Thursday\', \'Friday\', \'Saturday\', \'Sunday\']\\n\\nprint(f\\\"Current time: {now.strftime(\'%Y-%m-%d %H:%M:%S\')}\\\")\\nprint(f\\\"Day: {weekdays[now.weekday()]}\\\")\"}]', 1, '2026-03-31 14:03:52', '2026-05-03 13:28:36', 1111111111111111111, 1111111111111111111);


-- ----------------------------
-- Table structure for skill_tools
-- ----------------------------
DROP TABLE IF EXISTS `skill_tools`;
CREATE TABLE `skill_tools`  (
`id` bigint NOT NULL,
`skill_id` bigint NOT NULL,
`tool_id` bigint NOT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_skill_tool`(`skill_id` ASC, `tool_id` ASC) USING BTREE,
INDEX `idx_skill_id`(`skill_id` ASC) USING BTREE,
INDEX `idx_tool_id`(`tool_id` ASC) USING BTREE
) COMMENT = '技能工具关联表';

-- ----------------------------
-- Table structure for storage_protocol
-- ----------------------------
DROP TABLE IF EXISTS `storage_protocol`;
CREATE TABLE `storage_protocol`  (
`id` bigint NOT NULL COMMENT '主键',
`name` varchar(255) NULL DEFAULT NULL COMMENT '名称',
`protocol` varchar(255) NULL DEFAULT NULL COMMENT '存储协议',
`protocol_config` varchar(1000) NULL DEFAULT NULL COMMENT '协议配置',
`create_by` varchar(40) NULL DEFAULT NULL COMMENT '创建人',
`create_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
`update_by` varchar(40) NULL DEFAULT NULL COMMENT '修改人',
`update_at` datetime NULL DEFAULT NULL COMMENT '修改时间',
`remark` varchar(255) NULL DEFAULT NULL COMMENT '备注',
`valid` int NULL DEFAULT 1 COMMENT '是否有效',
PRIMARY KEY (`id`) USING BTREE,
INDEX `idx_protocol_protocol`(`protocol` ASC) USING BTREE,
INDEX `idx_protocol_valid`(`valid` ASC) USING BTREE
) COMMENT = '文件存储协议配置';

-- ----------------------------
-- Table structure for studio_config
-- ----------------------------
DROP TABLE IF EXISTS `studio_config`;
CREATE TABLE `studio_config`  (
`id` bigint NOT NULL COMMENT 'ID',
`url` varchar(40) NOT NULL COMMENT 'Studio Url',
`project` varchar(60) NOT NULL COMMENT 'project',
PRIMARY KEY (`id`) USING BTREE
) COMMENT = 'Studio配置';

-- ----------------------------
-- Table structure for system_prompt_template
-- ----------------------------
DROP TABLE IF EXISTS `system_prompt_template`;
CREATE TABLE `system_prompt_template`  (
`id` bigint NOT NULL,
`category` varchar(100) NULL DEFAULT NULL COMMENT '分类',
`name` varchar(100) NOT NULL COMMENT '模板名称',
`description` varchar(500) NULL DEFAULT NULL COMMENT '模板描述',
`content` text NOT NULL COMMENT '模板内容',
`enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用',
`usage_count` int NULL DEFAULT 0 COMMENT '使用次数统计',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`created_by` bigint NULL DEFAULT NULL,
`updated_by` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_template_name_category`(`category` ASC, `name` ASC) USING BTREE,
INDEX `idx_template_category`(`category` ASC) USING BTREE,
INDEX `idx_template_enabled`(`enabled` ASC) USING BTREE
) COMMENT = '系统提示词模板表';

-- ----------------------------
-- Table structure for tool_config
-- ----------------------------
DROP TABLE IF EXISTS `tool_config`;
CREATE TABLE `tool_config`  (
`id` bigint NOT NULL,
`name` varchar(100) NOT NULL COMMENT '工具名称',
`tool_id` varchar(100) NOT NULL COMMENT '工具编号',
`description` text NOT NULL COMMENT '工具描述',
`category` varchar(100) NULL DEFAULT NULL COMMENT '工具分类',
`tool_type` enum('BUILTIN','CUSTOM') NOT NULL COMMENT '工具类型: 内置/自定义',
`input_schema` text NULL COMMENT '输入参数schema',
`output_schema` text NULL COMMENT '输出格式schema',
`class_path` varchar(255) NULL DEFAULT NULL COMMENT '工具路径（tool_type为SYSTEM时使用）',
`language` enum('JAVA','JAVASCRIPT') NULL DEFAULT NULL COMMENT '代码预演（tool_type为CUSTOM时使用）',
`code` text NULL COMMENT '工具内容（tool_type为CUSTOM时使用）',
`need_confirm` tinyint(1) NULL DEFAULT NULL COMMENT '是否需要用户确认',
`enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用',
`version` varchar(20) NULL DEFAULT '1.0.0' COMMENT '版本号',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
`created_by` bigint NULL DEFAULT NULL,
`updated_by` bigint NULL DEFAULT NULL,
PRIMARY KEY (`id`) USING BTREE,
UNIQUE INDEX `uk_tool_id`(`tool_id` ASC) USING BTREE,
INDEX `idx_tool_type`(`tool_type` ASC) USING BTREE,
INDEX `idx_tool_enabled`(`enabled` ASC) USING BTREE,
INDEX `idx_tool_category`(`category` ASC) USING BTREE
) COMMENT = '工具表';

INSERT INTO `tool_config` (`id`, `name`, `tool_id`, `description`, `category`, `tool_type`, `input_schema`, `output_schema`, `class_path`, `language`, `code`, `need_confirm`, `enabled`, `version`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2020017365391609857, 'get_current_datetime', 'get_current_datetime', '获取当前的日期时间', '通用', 'BUILTIN', '[{\"name\":\"format\",\"description\":\"日期时间格式，默认值 yyyy-MM-dd HH:mm:ss\",\"type\":\"string\",\"defaultValue\":null,\"required\":false}]', NULL, 'com.hxh.apboa.core.tool.builtins.GetCurrentTimeTool', NULL, NULL, 0, 1, '1.0.0', '2026-02-07 14:10:45', '2026-03-02 14:34:27', NULL, 0);
INSERT INTO `tool_config` (`id`, `name`, `tool_id`, `description`, `category`, `tool_type`, `input_schema`, `output_schema`, `class_path`, `language`, `code`, `need_confirm`, `enabled`, `version`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2021603578967851010, '获取当前用户的个人信息', 'get_current_userinfo', '通过此工具，你可以获取到当前和你对话用户的个人信息，包括：姓名、年龄、性别和爱好。\n', '通用', 'CUSTOM', '[]', NULL, NULL, 'JAVA', 'import java.util.*;\nimport com.hxh.apboa.core.tool.dynamices.IDynamicAgentTool;\nimport com.hxh.apboa.core.agui.AgentContext;\n\npublic class CustomTool implements IDynamicAgentTool {\n\n    @Override\n    public Object execute(AgentContext context, Object... args) {\n        Map<String, Object> resMap = new HashMap<String, Object>() {{\n            put(\"name\", \"胡学好\");\n            put(\"age\", 28);\n            put(\"gender\", \"男\");\n            put(\"hobby\", \"跑步、乒乓球、编程\");\n        }};\n      \n        return resMap;\n    }\n}', 0, 1, '1.0.0', '2026-02-11 23:13:47', '2026-05-08 22:16:52', 1111111111111111111, 1111111111111111111);
INSERT INTO `tool_config` (`id`, `name`, `tool_id`, `description`, `category`, `tool_type`, `input_schema`, `output_schema`, `class_path`, `language`, `code`, `need_confirm`, `enabled`, `version`, `created_at`, `updated_at`, `created_by`, `updated_by`) VALUES (2025895208227008514, '数学计算函数', 'math_calculator', '通用数学计算工具，支持加、减、乘、除、取余、幂运算等基本数学运算，接收两个数字字符串和一个运算符，返回计算结果及详细运算信息。', '通用', 'CUSTOM', '[{\"name\":\"num1\",\"description\":\"第一个数字\",\"type\":\"string\",\"defaultValue\":\"\",\"required\":true},{\"name\":\"num2\",\"description\":\"第二个数字\",\"type\":\"string\",\"defaultValue\":\"\",\"required\":true},{\"name\":\"operator\",\"description\":\"运算符，只支持 \\\"+\\\", \\\"-\\\", \\\"*\\\", \\\"/\\\", \\\"%\\\", \\\"pow\\\"\",\"type\":\"string\",\"defaultValue\":\"\",\"required\":true}]', NULL, NULL, 'JAVA', 'import java.util.*;\nimport com.hxh.apboa.core.tool.dynamices.IDynamicAgentTool;\nimport com.hxh.apboa.core.agui.AgentContext;\n\npublic class CustomTool implements IDynamicAgentTool {\n\n    @Override\n    public Object execute(AgentContext context, Object... args) {\n        // 参数验证\n        if (args == null || args.length < 3) {\n            Map<String, Object> errorMap = new HashMap<>();\n            errorMap.put(\"error\", \"参数不足，需要3个参数：数字1、数字2、运算符\");\n            errorMap.put(\"required_format\", \"第一个参数: 数字1(字符串), 第二个参数: 数字2(字符串), 第三个参数: 运算符(字符串)\");\n            return errorMap;\n        }\n\n        try {\n            // 获取参数（都是字符串类型）\n            String num1Str = args[0].toString();\n            String num2Str = args[1].toString();\n            String operator = args[2].toString();\n\n            // 转换为数字\n            double num1 = Double.parseDouble(num1Str);\n            double num2 = Double.parseDouble(num2Str);\n\n            // 计算结果\n            double result = 0;\n            String calculation = \"\";\n\n            switch (operator) {\n                case \"+\":\n                    result = num1 + num2;\n                    calculation = num1 + \" + \" + num2 + \" = \" + result;\n                    break;\n                case \"-\":\n                    result = num1 - num2;\n                    calculation = num1 + \" - \" + num2 + \" = \" + result;\n                    break;\n                case \"*\":\n                    result = num1 * num2;\n                    calculation = num1 + \" × \" + num2 + \" = \" + result;\n                    break;\n                case \"/\":\n                    if (num2 == 0) {\n                        throw new ArithmeticException(\"除数不能为0\");\n                    }\n                    result = num1 / num2;\n                    calculation = num1 + \" ÷ \" + num2 + \" = \" + result;\n                    break;\n                case \"%\":\n                    result = num1 % num2;\n                    calculation = num1 + \" % \" + num2 + \" = \" + result;\n                    break;\n                case \"pow\":\n                    result = Math.pow(num1, num2);\n                    calculation = num1 + \" ^ \" + num2 + \" = \" + result;\n                    break;\n                default:\n                    Map<String, Object> errorMap = new HashMap<>();\n                    errorMap.put(\"error\", \"不支持的运算符: \" + operator);\n                    errorMap.put(\"supported_operators\", \"+, -, *, /, %, pow\");\n                    return errorMap;\n            }\n\n            // 构建返回结果\n            Map<String, Object> resMap = new HashMap<String, Object>() {{\n                put(\"num1\", num1Str);\n                put(\"num2\", num2Str);\n                put(\"operator\", operator);\n                put(\"result\", result);\n                put(\"calculation\", calculation);\n                put(\"timestamp\", new Date().toString());\n            }};\n\n            return resMap;\n\n        } catch (NumberFormatException e) {\n            Map<String, Object> errorMap = new HashMap<>();\n            errorMap.put(\"error\", \"数字格式错误，请确保传入有效的数字字符串\");\n            errorMap.put(\"message\", e.getMessage());\n            return errorMap;\n        } catch (ArithmeticException e) {\n            Map<String, Object> errorMap = new HashMap<>();\n            errorMap.put(\"error\", \"数学运算错误\");\n            errorMap.put(\"message\", e.getMessage());\n            return errorMap;\n        } catch (Exception e) {\n            Map<String, Object> errorMap = new HashMap<>();\n            errorMap.put(\"error\", \"未知错误\");\n            errorMap.put(\"message\", e.getMessage());\n            return errorMap;\n        }\n    }\n}', 0, 1, '1.0.0', '2026-02-23 19:27:12', '2026-05-08 22:16:37', 1111111111111111111, 1111111111111111111);

SET FOREIGN_KEY_CHECKS = 1;
