ALTER TABLE `agent_mcp_servers`
    ADD COLUMN `exposure_mode` ENUM('ALL_GLOBAL','SELECTED_ONLY') NOT NULL DEFAULT 'ALL_GLOBAL' COMMENT 'Agent 侧 MCP 工具暴露模式' AFTER `mcp_server_id`;

CREATE TABLE IF NOT EXISTS `mcp_tool` (
    `id` bigint NOT NULL,
    `mcp_server_id` bigint NOT NULL COMMENT '所属 MCP 服务 ID',
    `tool_name` varchar(200) NOT NULL COMMENT '工具名',
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
) COMMENT = 'Agent 与 MCP 工具局部选择关联表';
