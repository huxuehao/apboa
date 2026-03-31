DROP TABLE IF EXISTS `code_execution_config`;
CREATE TABLE code_execution_config (
id              BIGINT       PRIMARY KEY COMMENT '主键',
config_name     VARCHAR(128) NOT NULL UNIQUE COMMENT '配置名称，便于识别',
work_dir        VARCHAR(512)          COMMENT '工作目录，空则使用临时目录',
upload_dir      VARCHAR(512)          COMMENT '脚本上传目录，空则使用work_dir/skills',
auto_upload     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否自动上传skill文件，0=false',
enable_shell    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用ShellCommandTool',
enable_read     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否启用ReadFileTool',
enable_write    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否启用WriteFileTool',
command   VARCHAR(300)  NOT NULL COMMENT '允许执行的命令，如 python3、bash',
enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否可用',
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
created_by bigint NULL DEFAULT NULL,
updated_by bigint NULL DEFAULT NULL
) COMMENT '代码执行环境配置';

DROP TABLE IF EXISTS `agent_code_execution`;
CREATE TABLE `agent_code_execution` (
`id` bigint NOT NULL,
`agent_definition_id` bigint NOT NULL,
`code_execution_id` bigint NOT NULL,
PRIMARY KEY (`id`),
KEY `idx_code_execution` (`code_execution_id`),
KEY `idx_agent` (`agent_definition_id`),
KEY `idx_agent_code_execution` (`agent_definition_id`,`code_execution_id`)
) COMMENT='智能体与代码执行环境配置关联表';
