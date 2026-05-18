-- MCP 综合改造增量脚本
-- 说明：
-- 1. 本脚本汇总原 Phase 1 / Phase 2 / 自动降级三批 MCP 改造的增量变更
-- 2. 原分散脚本 2026-05-13-01.sql、2026-05-14-01.sql、2026-05-15-01.sql 已合并到本文件
-- 3. 执行顺序应晚于 master 分支既有的 2026-05-14-02.sql

ALTER TABLE `mcp_server`
    ADD COLUMN `tool_schemas` TEXT COMMENT '缓存的上次成功获取的 MCP 工具列表（JSON 格式）';

ALTER TABLE `mcp_server`
    ADD COLUMN `activation_status` ENUM('NOT_ACTIVATED','ACTIVATING','ACTIVE','FAILED') NOT NULL DEFAULT 'NOT_ACTIVATED' COMMENT 'MCP 连接状态' AFTER `tool_schemas`,
    ADD COLUMN `activation_message` VARCHAR(500) NULL DEFAULT NULL COMMENT '连接或同步说明' AFTER `activation_status`,
    ADD COLUMN `failure_source` ENUM('NONE','RUNTIME_AUTO_DEGRADE') NOT NULL DEFAULT 'NONE' COMMENT '失败来源' AFTER `activation_message`,
    ADD COLUMN `activation_status_changed_at` DATETIME NULL DEFAULT NULL COMMENT '连接状态最近一次变更时间' AFTER `failure_source`,
    ADD COLUMN `last_activation_time` DATETIME NULL DEFAULT NULL COMMENT '上次连接时间' AFTER `activation_status_changed_at`,
    ADD COLUMN `last_tool_sync_time` DATETIME NULL DEFAULT NULL COMMENT '上次工具同步时间' AFTER `last_activation_time`,
    ADD COLUMN `tool_count` INT NOT NULL DEFAULT 0 COMMENT '当前工具数量' AFTER `last_tool_sync_time`,
    ADD COLUMN `runtime_fail_threshold` INT NOT NULL DEFAULT 3 COMMENT '运行时自动降级连续失败阈值，0 表示关闭' AFTER `tool_count`,
    ADD COLUMN `activation_revision` BIGINT NOT NULL DEFAULT 0 COMMENT '连接代次号' AFTER `runtime_fail_threshold`,
    ADD COLUMN `config_hash` VARCHAR(64) NULL DEFAULT NULL COMMENT '当前连接配置哈希' AFTER `activation_revision`,
    ADD COLUMN `needs_sync` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否需要同步工具目录' AFTER `config_hash`,
    ADD COLUMN `activation_request_id` VARCHAR(64) NULL DEFAULT NULL COMMENT '当前连接请求标识' AFTER `needs_sync`;

UPDATE `mcp_server`
SET
    `activation_status` = CASE
        WHEN `enabled` = 0 THEN 'NOT_ACTIVATED'
        WHEN `tool_schemas` IS NULL OR TRIM(`tool_schemas`) = '' THEN 'NOT_ACTIVATED'
        WHEN JSON_VALID(`tool_schemas`) = 1 AND TRIM(`tool_schemas`) = '[]' THEN 'ACTIVE'
        WHEN JSON_VALID(`tool_schemas`) = 1 AND TRIM(`tool_schemas`) LIKE '[%' THEN 'ACTIVE'
        ELSE 'NOT_ACTIVATED'
    END,
    `activation_message` = CASE
        WHEN `enabled` = 0 THEN '未连接'
        WHEN `tool_schemas` IS NULL OR TRIM(`tool_schemas`) = '' THEN '待连接'
        WHEN JSON_VALID(`tool_schemas`) = 1 AND TRIM(`tool_schemas`) = '[]' THEN '连接成功但无工具（历史缓存迁移）'
        WHEN JSON_VALID(`tool_schemas`) = 1 AND TRIM(`tool_schemas`) LIKE '[%' THEN '沿用历史工具缓存，建议同步'
        ELSE '待连接'
    END,
    `failure_source` = 'NONE',
    `activation_status_changed_at` = COALESCE(
        `activation_status_changed_at`,
        `last_tool_sync_time`,
        `last_activation_time`,
        `last_health_check`,
        `updated_at`,
        `created_at`
    ),
    `tool_count` = CASE
        WHEN JSON_VALID(`tool_schemas`) = 1 AND TRIM(`tool_schemas`) LIKE '[%' THEN JSON_LENGTH(`tool_schemas`)
        ELSE 0
    END,
    `runtime_fail_threshold` = 3,
    `activation_revision` = CASE
        WHEN `enabled` = 1 AND JSON_VALID(`tool_schemas`) = 1 AND TRIM(`tool_schemas`) LIKE '[%' THEN 1
        ELSE 0
    END,
    `needs_sync` = CASE
        WHEN `enabled` = 1 THEN 1
        ELSE 0
    END,
    `activation_request_id` = NULL;

ALTER TABLE `agent_mcp_servers`
    ADD COLUMN `exposure_mode` ENUM('ALL_GLOBAL','SELECTED_ONLY') NOT NULL DEFAULT 'ALL_GLOBAL' COMMENT 'Agent 侧 MCP 工具暴露模式' AFTER `mcp_server_id`;

CREATE TABLE IF NOT EXISTS `mcp_tool` (
    `id` bigint NOT NULL,
    `mcp_server_id` bigint NOT NULL COMMENT '所属 MCP 服务 ID',
    `tool_name` varchar(200) NOT NULL COMMENT '工具名称',
    `description` varchar(1000) NULL DEFAULT NULL COMMENT '工具描述',
    `input_schema` json NULL COMMENT '输入 Schema',
    `output_schema` json NULL COMMENT '输出 Schema',
    `raw_schema` json NULL COMMENT '原始工具 Schema',
    `schema_hash` varchar(64) NULL DEFAULT NULL COMMENT 'Schema 摘要',
    `missing` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已从当前 MCP 服务中消失',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否全局可用',
    `last_discovered_at` datetime NULL DEFAULT NULL COMMENT '首次发现时间',
    `last_seen_at` datetime NULL DEFAULT NULL COMMENT '最近发现时间',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by` bigint NULL DEFAULT NULL,
    `updated_by` bigint NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_mcp_tool_name`(`mcp_server_id` ASC, `tool_name` ASC) USING BTREE,
    INDEX `idx_mcp_tool_server`(`mcp_server_id` ASC) USING BTREE,
    INDEX `idx_mcp_tool_runtime`(`mcp_server_id` ASC, `enabled` ASC, `missing` ASC) USING BTREE
) COMMENT = 'MCP 工具目录表';

CREATE TABLE IF NOT EXISTS `agent_mcp_tool` (
    `id` bigint NOT NULL,
    `agent_definition_id` bigint NOT NULL,
    `mcp_tool_id` bigint NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_agent_mcp_tool`(`agent_definition_id` ASC, `mcp_tool_id` ASC) USING BTREE,
    INDEX `idx_agent_mcp_tool_agent`(`agent_definition_id` ASC) USING BTREE,
    INDEX `idx_agent_mcp_tool_tool`(`mcp_tool_id` ASC) USING BTREE
) COMMENT = 'Agent 侧 MCP 工具局部选择关联表';
