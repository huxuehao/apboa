-- ============================================================
-- 本地RAG系统数据库初始化脚本
-- MySQL部分：RAG文档和分块元数据
-- PostgreSQL部分：向量存储（pgvector）
-- ============================================================

-- ============================================================
-- MySQL 表结构
-- ============================================================

-- RAG文档表
CREATE TABLE IF NOT EXISTS `rag_document` (
    `id`                        BIGINT          NOT NULL,
    `knowledge_base_config_id`  BIGINT          NOT NULL COMMENT '关联的知识库配置ID',
    `file_name`                 VARCHAR(500)    NOT NULL COMMENT '文件名',
    `file_path`                 VARCHAR(1000)   NOT NULL COMMENT '文件存储路径',
    `file_size`                 BIGINT          NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    `file_type`                 VARCHAR(50)     NOT NULL COMMENT '文件类型(pdf/txt/docx/xlsx/md等)',
    `chunk_count`               INT             NOT NULL DEFAULT 0 COMMENT '分块数量',
    `status`                    ENUM('PENDING','PROCESSING','COMPLETED','FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
    `error_message`             TEXT            NULL COMMENT '错误信息',
    `created_at`                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`                BIGINT          NULL,
    `updated_by`                BIGINT          NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_kb_config_id` (`knowledge_base_config_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG文档表';

-- RAG文档分块表
CREATE TABLE IF NOT EXISTS `rag_document_chunk` (
    `id`            BIGINT      NOT NULL,
    `document_id`   BIGINT      NOT NULL COMMENT '关联的文档ID',
    `chunk_index`   INT         NOT NULL COMMENT '分块序号',
    `content`       TEXT        NOT NULL COMMENT '分块文本内容',
    `token_count`   INT         NULL COMMENT 'Token数量(估算)',
    `start_offset`  INT         NULL COMMENT '在原文中的起始偏移',
    `end_offset`    INT         NULL COMMENT '在原文中的结束偏移',
    `metadata`      TEXT        NULL COMMENT '元数据(JSON)',
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_document_id` (`document_id`),
    INDEX `idx_chunk_index` (`document_id`, `chunk_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RAG文档分块表';

-- 更新 knowledge_base_config 表的 kb_type 枚举，添加 LOCAL
ALTER TABLE `knowledge_base_config`
    MODIFY COLUMN `kb_type` ENUM('BAILIAN','DIFY','RAGFLOW','LOCAL') NOT NULL COMMENT '知识库类型';


-- ============================================================
-- PostgreSQL 表结构 (在 apboa_vector 数据库中执行)
-- ============================================================

-- 1. 创建数据库
-- CREATE DATABASE apboa_vector;

-- 2. 启用pgvector扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 3. 创建向量存储表
CREATE TABLE IF NOT EXISTS rag_embedding (
    id                        BIGINT PRIMARY KEY,
    chunk_id                  BIGINT NOT NULL,
    document_id               BIGINT NOT NULL,
    knowledge_base_config_id  BIGINT NOT NULL,
    embedding                 halfvec(2560) NOT NULL,
    created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. 创建索引
CREATE INDEX IF NOT EXISTS idx_embedding_kbc ON rag_embedding(knowledge_base_config_id);
CREATE INDEX IF NOT EXISTS idx_embedding_doc ON rag_embedding(document_id);
CREATE INDEX IF NOT EXISTS idx_rag_vectors_embedding ON rag_embedding USING hnsw (embedding halfvec_cosine_ops);

-- 5. 创建IVFFlat向量索引（数据量较大时启用，提升检索性能）
-- 注意：需要先插入一定量的数据后才能创建IVFFlat索引
-- CREATE INDEX IF NOT EXISTS idx_embedding_vector ON rag_embedding
--   USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
