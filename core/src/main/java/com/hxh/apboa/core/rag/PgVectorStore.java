package com.hxh.apboa.core.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PgVector向量存储服务，负责与PostgreSQL/pgvector的交互
 *
 * @author huxuehao
 */
@Component
public class PgVectorStore {

    private static final Logger log = LoggerFactory.getLogger(PgVectorStore.class);

    /**
     * 支持的向量维度列表
     */
    private static final int[] SUPPORTED_DIMENSIONS = {64, 128, 256, 512, 768, 1024, 2048, 2560};

    private final JdbcTemplate pgJdbcTemplate;

    public PgVectorStore(@Autowired(required = false) @Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {
        if (pgVectorDataSource != null) {
            this.pgJdbcTemplate = new JdbcTemplate(pgVectorDataSource);
            initSchema();
        } else {
            this.pgJdbcTemplate = null;
            log.warn("PgVector数据源未配置，本地RAG功能不可用");
        }
    }

    /**
     * 根据向量维度拼接表名
     */
    private String getTableName(int dimension) {
        return "rag_embedding_" + dimension;
    }

    /**
     * 初始化pgvector扩展和多维度表结构
     */
    private void initSchema() {
        try {
            pgJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            for (int dim : SUPPORTED_DIMENSIONS) {
                String embeddingType = "vector";
                if (dim > 2000) {
                    embeddingType = "halfvec";
                }
                String tableName = getTableName(dim);
                pgJdbcTemplate.execute(String.format("""
                    CREATE TABLE IF NOT EXISTS %s (
                        id BIGINT PRIMARY KEY,
                        chunk_id BIGINT NOT NULL,
                        document_id BIGINT NOT NULL,
                        knowledge_base_config_id BIGINT NOT NULL,
                        embedding %s(%d) NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                """, tableName, embeddingType, dim));
                pgJdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_embedding_kbc_" + dim + " ON " + tableName + "(knowledge_base_config_id)");
                pgJdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_embedding_doc_" + dim + " ON " + tableName + "(document_id)");
                pgJdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_vectors_embedding_" + dim + " ON " + tableName + " USING hnsw (embedding " + embeddingType + "_cosine_ops)");
            }
            log.info("PgVector表结构初始化完成，共{}张表", SUPPORTED_DIMENSIONS.length);
        } catch (Exception e) {
            log.error("PgVector表结构初始化失败", e);
        }
    }

    /**
     * 是否可用
     */
    public boolean isAvailable() {
        return pgJdbcTemplate != null;
    }

    /**
     * 存储向量，根据embedding数组长度自动确定目标表
     */
    public void storeEmbedding(Long id, Long chunkId, Long documentId,
                               Long knowledgeBaseConfigId, float[] embedding) {
        if (!isAvailable()) {
            throw new RuntimeException("PgVector数据源未配置");
        }

        int dimension = embedding.length;
        String tableName = getTableName(dimension);
        String vectorStr = arrayToVectorString(embedding);
        String sql = "INSERT INTO " + tableName + " (id, chunk_id, document_id, knowledge_base_config_id, embedding) " +
                "VALUES (?, ?, ?, ?, ?::vector)";

        pgJdbcTemplate.update(sql, id, chunkId, documentId, knowledgeBaseConfigId, vectorStr);
    }

    /**
     * 批量存储向量
     */
    public void storeEmbeddings(List<EmbeddingRecord> records) {
        if (!isAvailable()) {
            throw new RuntimeException("PgVector数据源未配置");
        }

        for (EmbeddingRecord record : records) {
            storeEmbedding(record.id, record.chunkId, record.documentId,
                    record.knowledgeBaseConfigId, record.embedding);
        }
    }

    /**
     * 向量相似度检索，根据查询向量长度自动确定目标表
     */
    public List<RetrievalResult> search(float[] queryEmbedding, Long knowledgeBaseConfigId,
                                        int limit, double scoreThreshold) {
        if (!isAvailable()) {
            throw new RuntimeException("PgVector数据源未配置");
        }

        int dimension = queryEmbedding.length;
        String tableName = getTableName(dimension);
        String vectorStr = arrayToVectorString(queryEmbedding);
        String sql = "SELECT chunk_id, document_id, 1 - (embedding <=> ?::vector) AS score " +
                "FROM " + tableName + " " +
                "WHERE knowledge_base_config_id = ? " +
                "ORDER BY embedding <=> ?::vector " +
                "LIMIT ?";

        List<Map<String, Object>> rows = pgJdbcTemplate.queryForList(sql, vectorStr, knowledgeBaseConfigId, vectorStr, limit);

        List<RetrievalResult> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            double score = ((Number) row.get("score")).doubleValue();
            if (score >= scoreThreshold) {
                results.add(new RetrievalResult(
                        ((Number) row.get("chunk_id")).longValue(),
                        ((Number) row.get("document_id")).longValue(),
                        score
                ));
            }
        }

        return results;
    }

    /**
     * 删除指定文档的所有向量（遍历所有维度表）
     */
    public void deleteByDocumentId(Long documentId) {
        if (!isAvailable()) return;
        for (int dim : SUPPORTED_DIMENSIONS) {
            pgJdbcTemplate.update("DELETE FROM " + getTableName(dim) + " WHERE document_id = ?", documentId);
        }
    }

    /**
     * 删除指定知识库的所有向量（遍历所有维度表）
     */
    public void deleteByKnowledgeBaseConfigId(Long knowledgeBaseConfigId) {
        if (!isAvailable()) return;
        for (int dim : SUPPORTED_DIMENSIONS) {
            pgJdbcTemplate.update("DELETE FROM " + getTableName(dim) + " WHERE knowledge_base_config_id = ?", knowledgeBaseConfigId);
        }
    }

    /**
     * 删除指定分块的向量（遍历所有维度表）
     */
    public void deleteByChunkId(Long chunkId) {
        if (!isAvailable()) return;
        for (int dim : SUPPORTED_DIMENSIONS) {
            pgJdbcTemplate.update("DELETE FROM " + getTableName(dim) + " WHERE chunk_id = ?", chunkId);
        }
    }

    private String arrayToVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 嵌入向量记录
     */
    public record EmbeddingRecord(Long id, Long chunkId, Long documentId,
                                  Long knowledgeBaseConfigId, float[] embedding) {
    }

    /**
     * 检索结果
     */
    public record RetrievalResult(Long chunkId, Long documentId, double score) {
    }
}
