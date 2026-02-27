ALTER TABLE `hook_config`
    CHANGE COLUMN `hook_content` `code` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'hook内容（thook_type为CUSTOM时使用）' AFTER `class_path`,
    MODIFY COLUMN `class_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'hook路径（hook_type为BUILTIN时使用）' AFTER `description`;
