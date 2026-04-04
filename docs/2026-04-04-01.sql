ALTER TABLE `tool_config`
    MODIFY COLUMN `description` text NOT NULL COMMENT '工具描述' AFTER `tool_id`;
