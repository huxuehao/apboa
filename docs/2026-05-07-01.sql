-- 为 rag_document_chunk 表添加 file_name 字段
ALTER TABLE `rag_document_chunk`
    ADD COLUMN `file_name` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '文件名' AFTER `document_id`;

-- 回填历史数据
UPDATE `rag_document_chunk` c
JOIN `rag_document` d ON c.document_id = d.id
SET c.file_name = d.file_name
WHERE c.file_name = '';
