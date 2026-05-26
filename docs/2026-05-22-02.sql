-- =====================================================
-- 技能子系统重构 - 增量迁移脚本
-- 日期: 2026-05-22
-- 描述:
--   1. 新建 skill_file 表，将 references/examples/scripts/skill_content 从 JSON 列拆分为独立记录
--   2. 删除 skill_package 旧列: references, examples, scripts, skill_content
-- =====================================================

-- ----------------------------
-- 1. 创建 skill_file 表
-- ----------------------------
DROP TABLE IF EXISTS `skill_file`;
CREATE TABLE `skill_file` (
    `id` bigint NOT NULL COMMENT '主键',
    `skill_id` bigint NOT NULL COMMENT '技能包ID',
    `file_type` enum('SKILL_MD','REFERENCES','EXAMPLES','SCRIPTS') NOT NULL COMMENT '文件类型',
    `file_name` varchar(255) NOT NULL COMMENT '文件名',
    `file_path` varchar(1000) NOT NULL COMMENT '相对路径（相对于技能包根目录）',
    `content` longtext NULL COMMENT '文件内容',
    `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` bigint DEFAULT NULL,
    `updated_by` bigint DEFAULT NULL,
    `enabled` tinyint(1) DEFAULT '1' COMMENT '是否可用',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_skill_file_skill`(`skill_id` ASC) USING BTREE,
    INDEX `idx_skill_file_type`(`file_type` ASC) USING BTREE,
    INDEX `idx_skill_file_path`(`skill_id` ASC, `file_path`(255) ASC) USING BTREE
) COMMENT = '技能包文件表';

-- ============================================
-- 数据迁移：将 skill_package 中的 JSON 数据迁移到 skill_file 表
-- ============================================

-- 初始化行号变量
SET @rownum = 0;

-- ----------------------------
-- 2. 迁移 references JSON 数据到 skill_file
-- ----------------------------
INSERT INTO `skill_file` (`id`, `skill_id`, `file_type`, `file_name`, `file_path`, `content`, `sort`, `created_at`, `updated_at`, `created_by`, `updated_by`, `enabled`)
SELECT
    FLOOR(UNIX_TIMESTAMP(NOW(3)) * 1000 + @rownum := @rownum + 1 + RAND() * 0) AS id,
    sp.id AS skill_id,
    'REFERENCES' AS file_type,
    JSON_UNQUOTE(JSON_EXTRACT(jt.item, '$.name')) AS file_name,
    CONCAT('references/', JSON_UNQUOTE(JSON_EXTRACT(jt.item, '$.name'))) AS file_path,
    JSON_UNQUOTE(JSON_EXTRACT(jt.item, '$.content')) AS content,
    jt.idx AS sort,
    sp.created_at,
    sp.updated_at,
    sp.created_by,
    sp.updated_by,
    sp.enabled
FROM `skill_package` sp
         JOIN JSON_TABLE(
        sp.`references`,
        '$[*]' COLUMNS (
        idx FOR ORDINALITY,
        item JSON PATH '$'
    )
              ) AS jt
WHERE sp.`references` IS NOT NULL
  AND JSON_LENGTH(sp.`references`) > 0;

-- ----------------------------
-- 3. 迁移 examples JSON 数据到 skill_file
-- ----------------------------
INSERT INTO `skill_file` (`id`, `skill_id`, `file_type`, `file_name`, `file_path`, `content`, `sort`, `created_at`, `updated_at`, `created_by`, `updated_by`, `enabled`)
SELECT
    FLOOR(UNIX_TIMESTAMP(NOW(3)) * 1000 + @rownum := @rownum + 1 + RAND() * 0) AS id,
    sp.id AS skill_id,
    'EXAMPLES' AS file_type,
    JSON_UNQUOTE(JSON_EXTRACT(jt.item, '$.name')) AS file_name,
    CONCAT('examples/', JSON_UNQUOTE(JSON_EXTRACT(jt.item, '$.name'))) AS file_path,
    JSON_UNQUOTE(JSON_EXTRACT(jt.item, '$.content')) AS content,
    jt.idx AS sort,
    sp.created_at,
    sp.updated_at,
    sp.created_by,
    sp.updated_by,
    sp.enabled
FROM `skill_package` sp
         JOIN JSON_TABLE(
        sp.`examples`,
        '$[*]' COLUMNS (
        idx FOR ORDINALITY,
        item JSON PATH '$'
    )
              ) AS jt
WHERE sp.`examples` IS NOT NULL
  AND JSON_LENGTH(sp.`examples`) > 0;

-- ----------------------------
-- 4. 迁移 scripts JSON 数据到 skill_file
-- ----------------------------
INSERT INTO `skill_file` (`id`, `skill_id`, `file_type`, `file_name`, `file_path`, `content`, `sort`, `created_at`, `updated_at`, `created_by`, `updated_by`, `enabled`)
SELECT
    FLOOR(UNIX_TIMESTAMP(NOW(3)) * 1000 + @rownum := @rownum + 1 + RAND() * 0) AS id,
    sp.id AS skill_id,
    'SCRIPTS' AS file_type,
    JSON_UNQUOTE(JSON_EXTRACT(jt.item, '$.name')) AS file_name,
    CONCAT('scripts/', JSON_UNQUOTE(JSON_EXTRACT(jt.item, '$.name'))) AS file_path,
    JSON_UNQUOTE(JSON_EXTRACT(jt.item, '$.content')) AS content,
    jt.idx AS sort,
    sp.created_at,
    sp.updated_at,
    sp.created_by,
    sp.updated_by,
    sp.enabled
FROM `skill_package` sp
         JOIN JSON_TABLE(
        sp.`scripts`,
        '$[*]' COLUMNS (
        idx FOR ORDINALITY,
        item JSON PATH '$'
    )
              ) AS jt
WHERE sp.`scripts` IS NOT NULL
  AND JSON_LENGTH(sp.`scripts`) > 0;

-- ----------------------------
-- 5. 迁移 skill_content 为 SKILL.md 文件记录（补充 YAML 头：name / description）
-- ----------------------------
INSERT INTO `skill_file` (`id`, `skill_id`, `file_type`, `file_name`, `file_path`, `content`, `sort`, `created_at`, `updated_at`, `created_by`, `updated_by`, `enabled`)
SELECT
    FLOOR(UNIX_TIMESTAMP(NOW(3)) * 1000 + @rownum := @rownum + 1 + RAND() * 0) AS id,
    sp.id AS skill_id,
    'SKILL_MD' AS file_type,
    'SKILL.md' AS file_name,
    'SKILL.md' AS file_path,
    CONCAT(
        '---\n',
        'name: ', IFNULL(sp.name, ''), '\n',
        'description: ', IFNULL(sp.description, ''), '\n',
        '---\n\n',
        IFNULL(sp.skill_content, '')
    ) AS content,
    0 AS sort,
    sp.created_at,
    sp.updated_at,
    sp.created_by,
    sp.updated_by,
    sp.enabled
FROM `skill_package` sp
WHERE sp.skill_content IS NOT NULL
  AND sp.skill_content <> '';

-- ============================================
-- 迁移完成检查
-- ============================================
SELECT
    'references' AS source_type,
    COUNT(*) AS migrated_count
FROM `skill_file` WHERE file_type = 'REFERENCES'
UNION ALL
SELECT
    'examples' AS source_type,
    COUNT(*) AS migrated_count
FROM `skill_file` WHERE file_type = 'EXAMPLES'
UNION ALL
SELECT
    'scripts' AS source_type,
    COUNT(*) AS migrated_count
FROM `skill_file` WHERE file_type = 'SCRIPTS'
UNION ALL
SELECT
    'skill_content' AS source_type,
    COUNT(*) AS migrated_count
FROM `skill_file` WHERE file_type = 'SKILL_MD';

-- 重置会话变量（可选）
SET @rownum = NULL;

-- ----------------------------
-- 6. 删除 skill_package 旧列
-- ----------------------------
ALTER TABLE `skill_package`
    DROP COLUMN `references`,
    DROP COLUMN `examples`,
    DROP COLUMN `scripts`,
    DROP COLUMN `skill_content`;

-- 完成

-- ----------------------------
-- 7. 初始化技能包文件入库扩展名白名单
-- ----------------------------
INSERT INTO `params` (`id`, `param_name`, `param_key`, `param_value`)
VALUES (7, '技能包文件允许入库的扩展名', 'SKILL_FILE_ALLOWED_EXTENSIONS', 'md,py,sh,js,ts,json,yaml,yml,xml,txt,java,cs,go,rs,rb,php,sql,html,css,scss,less,cfg,conf,toml')
ON DUPLICATE KEY UPDATE `param_value` = VALUES(`param_value`);
