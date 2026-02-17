SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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

-- ----------------------------
-- Table structure for agent_definition
-- ----------------------------
DROP TABLE IF EXISTS `agent_definition`;
CREATE TABLE `agent_definition`  (
`id` bigint NOT NULL,
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
`max_subtasks` int NULL DEFAULT 10 COMMENT '最大子任务数',
`require_plan_confirmation` tinyint(1) NOT NULL DEFAULT 0 COMMENT '计划是否需要确认',
`enable_memory` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用记忆',
`enable_memory_compression` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用记忆压缩',
`memory_compression_config` text NULL COMMENT '记忆压缩配置',
`structured_output_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用结构化输出',
`structured_output_reminder` enum('PROMPT','TOOL_CHOICE') DEFAULT NULL COMMENT '结构化输出模式',
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
INDEX `idx_agent_enabled`(`enabled` ASC) USING BTREE
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
) COMMENT = 'agentscope session';

-- ----------------------------
-- Table structure for chat_message
-- ----------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`  (
`id` bigint NOT NULL COMMENT '消息ID',
`session_id` bigint NOT NULL COMMENT '会话ID',
`role` varchar(20) NOT NULL COMMENT '消息角色',
`content` text NOT NULL COMMENT '消息内容',
`parent_id` bigint NULL DEFAULT NULL COMMENT '父消息ID',
`path` varchar(1024) NULL DEFAULT NULL COMMENT '消息路径，格式如：/1/2/3/',
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
`current_message_id` bigint NULL DEFAULT NULL COMMENT '当前消息ID',
`title` varchar(255) NULL DEFAULT NULL COMMENT '会话标题',
`is_pinned` tinyint(1) NULL DEFAULT 0 COMMENT '是否置顶',
`pin_time` datetime NULL DEFAULT NULL COMMENT '置顶时间',
`created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
`updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
PRIMARY KEY (`id`) USING BTREE
) COMMENT = '聊天会话表';

-- ----------------------------
-- Table structure for hook_config
-- ----------------------------
DROP TABLE IF EXISTS `hook_config`;
CREATE TABLE `hook_config`  (
`id` bigint NOT NULL,
`name` varchar(100) NOT NULL COMMENT 'Hook名称',
`hook_type` enum('BUILTIN','CUSTOM') NOT NULL COMMENT 'Hook类型',
`description` varchar(500) NULL DEFAULT NULL COMMENT '描述',
`class_path` varchar(255) NULL DEFAULT NULL COMMENT 'hook路径（tool_type为SYSTEM时使用）',
`hook_content` text NULL COMMENT 'hook内容（tool_type为CUSTOM时使用）',
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
`kb_type` enum('BAILIAN','DIFY','RAGFLOW') NOT NULL COMMENT '知识库类型',
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

-- ----------------------------
-- Table structure for model_config
-- ----------------------------
DROP TABLE IF EXISTS `model_config`;
CREATE TABLE `model_config`  (
`id` bigint NOT NULL,
`provider_id` bigint NOT NULL COMMENT '提供商ID',
`name` varchar(100) NOT NULL COMMENT '模型名称',
`model_id` varchar(100) NOT NULL COMMENT '模型编号/标识符',
`model_type` enum('CHAT','IMAGE','VIDEO','TTS','EMBEDDING','RERANKER') NOT NULL COMMENT '模型类型',
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
`name` varchar(100) NOT NULL COMMENT '技能包名称',
`description` varchar(500) NOT NULL COMMENT '技能描述',
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
`description` varchar(500) NOT NULL COMMENT '工具描述',
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

SET FOREIGN_KEY_CHECKS = 1;
