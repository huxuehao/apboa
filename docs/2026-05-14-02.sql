ALTER TABLE `chat_message`
    MODIFY COLUMN `path` text NULL COMMENT '消息路径，格式如：/1/2/3/' AFTER `parent_id`;
