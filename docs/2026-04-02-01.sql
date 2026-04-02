ALTER TABLE `skill_package`
    MODIFY COLUMN `name` varchar(500) NOT NULL COMMENT '技能包名称' AFTER `id`,
    MODIFY COLUMN `description` text NOT NULL COMMENT '技能描述' AFTER `name`;
