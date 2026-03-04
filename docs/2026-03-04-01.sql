ALTER TABLE `model_config`
    ADD COLUMN `extend_config` text NULL COMMENT '扩展配置' AFTER `seed`;
