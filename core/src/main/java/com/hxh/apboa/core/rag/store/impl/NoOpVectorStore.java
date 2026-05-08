package com.hxh.apboa.core.rag.store.impl;

import com.hxh.apboa.core.rag.EmbeddingRecord;
import com.hxh.apboa.core.rag.RetrievalResult;
import com.hxh.apboa.core.rag.store.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 向量存储空实现，当未配置任何向量数据库时作为回退
 *
 * @author huxuehao
 */
public class NoOpVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(NoOpVectorStore.class);

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void storeEmbedding(Long id, Long chunkId, Long documentId,
                               Long knowledgeBaseConfigId, float[] embedding) {
        log.warn("向量存储未配置，跳过存储操作");
    }

    @Override
    public void storeEmbeddings(List<EmbeddingRecord> records) {
        log.warn("向量存储未配置，跳过批量存储操作, count={}", records.size());
    }

    @Override
    public List<RetrievalResult> search(float[] queryEmbedding, Long knowledgeBaseConfigId,
                                        int limit, double scoreThreshold) {
        log.warn("向量存储未配置，返回空检索结果");
        return List.of();
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        log.warn("向量存储未配置，跳过删除操作, documentId={}", documentId);
    }

    @Override
    public void deleteByKnowledgeBaseConfigId(Long knowledgeBaseConfigId) {
        log.warn("向量存储未配置，跳过删除操作, knowledgeBaseConfigId={}", knowledgeBaseConfigId);
    }

    @Override
    public void deleteByChunkId(Long chunkId) {
        log.warn("向量存储未配置，跳过删除操作, chunkId={}", chunkId);
    }
}
