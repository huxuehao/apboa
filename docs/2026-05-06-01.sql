-- 修改 API Key 长度
ALTER TABLE `apboa`.`secret_key`
    MODIFY COLUMN `value` varchar(500) NULL DEFAULT NULL COMMENT '密钥' AFTER `name`;
