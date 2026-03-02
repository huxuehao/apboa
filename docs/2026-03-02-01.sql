ALTER TABLE `model_config`
    MODIFY COLUMN `model_type` varchar(100)
    NULL
    COMMENT '模型类型';
UPDATE `model_config` SET `model_type` = '["CHAT"]';
