-- 说明：`tool_schemas` 列已在 `docs/2026-05-13-01.sql` 中增量添加，本脚本仅承接 Phase 1 激活状态相关字段。

ALTER TABLE `mcp_server`
    ADD COLUMN `activation_status` ENUM('NOT_ACTIVATED','ACTIVATING','ACTIVE','FAILED') NOT NULL DEFAULT 'NOT_ACTIVATED' COMMENT 'MCP 激活状态' AFTER `tool_schemas`,
    ADD COLUMN `activation_message` VARCHAR(500) NULL DEFAULT NULL COMMENT '激活或同步说明' AFTER `activation_status`,
    ADD COLUMN `last_activation_time` DATETIME NULL DEFAULT NULL COMMENT '上次激活时间' AFTER `activation_message`,
    ADD COLUMN `last_tool_sync_time` DATETIME NULL DEFAULT NULL COMMENT '上次工具同步时间' AFTER `last_activation_time`,
    ADD COLUMN `tool_count` INT NOT NULL DEFAULT 0 COMMENT '当前工具数量' AFTER `last_tool_sync_time`,
    ADD COLUMN `activation_revision` BIGINT NOT NULL DEFAULT 0 COMMENT '激活版本号' AFTER `tool_count`,
    ADD COLUMN `config_hash` VARCHAR(64) NULL DEFAULT NULL COMMENT '当前连接配置哈希' AFTER `activation_revision`,
    ADD COLUMN `needs_sync` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否需要同步工具列表' AFTER `config_hash`,
    ADD COLUMN `activation_request_id` VARCHAR(64) NULL DEFAULT NULL COMMENT '当前激活请求标识' AFTER `needs_sync`;

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
        WHEN `enabled` = 0 THEN '未激活'
        WHEN `tool_schemas` IS NULL OR TRIM(`tool_schemas`) = '' THEN '待激活'
        WHEN JSON_VALID(`tool_schemas`) = 1 AND TRIM(`tool_schemas`) = '[]' THEN '连接成功但无工具（历史缓存迁移）'
        WHEN JSON_VALID(`tool_schemas`) = 1 AND TRIM(`tool_schemas`) LIKE '[%' THEN '沿用历史工具缓存，建议同步'
        ELSE '待激活'
    END,
    `tool_count` = CASE
        WHEN JSON_VALID(`tool_schemas`) = 1 AND TRIM(`tool_schemas`) LIKE '[%' THEN JSON_LENGTH(`tool_schemas`)
        ELSE 0
    END,
    `activation_revision` = CASE
        WHEN `enabled` = 1 AND JSON_VALID(`tool_schemas`) = 1 AND TRIM(`tool_schemas`) LIKE '[%' THEN 1
        ELSE 0
    END,
    `needs_sync` = CASE
        WHEN `enabled` = 1 THEN 1
        ELSE 0
    END,
    `activation_request_id` = NULL;
