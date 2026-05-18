ALTER TABLE `mcp_server`
    ADD COLUMN `failure_source` ENUM('NONE','RUNTIME_AUTO_DEGRADE') NOT NULL DEFAULT 'NONE' COMMENT '失败来源' AFTER `activation_message`,
    ADD COLUMN `activation_status_changed_at` DATETIME NULL DEFAULT NULL COMMENT '连接状态最近一次变更时间' AFTER `failure_source`,
    ADD COLUMN `runtime_fail_threshold` INT NOT NULL DEFAULT 3 COMMENT '运行时自动降级连续失败阈值，0 表示关闭' AFTER `tool_count`;

UPDATE `mcp_server`
SET
    `failure_source` = 'NONE',
    `runtime_fail_threshold` = CASE
        WHEN `runtime_fail_threshold` IS NULL THEN 3
        WHEN `runtime_fail_threshold` < 0 THEN 0
        ELSE `runtime_fail_threshold`
    END,
    `activation_status_changed_at` = COALESCE(
        `activation_status_changed_at`,
        `last_tool_sync_time`,
        `last_activation_time`,
        `last_health_check`,
        `updated_at`,
        `created_at`
    );
