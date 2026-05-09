package com.hxh.apboa.core.rag.store.impl;

import com.hxh.apboa.core.rag.EmbeddingRecord;
import com.hxh.apboa.core.rag.RetrievalResult;
import com.hxh.apboa.core.rag.store.VectorStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.qdrant.client.ConditionFactory.match;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

/**
 * Qdrant向量存储服务
 *
 * @author huxuehao
 */
@Component
@ConditionalOnProperty(name = "rag.store", havingValue = "qdrant")
public class QdrantVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStore.class);

    /**
     * 支持的向量维度列表
     */
    private static final int[] SUPPORTED_DIMENSIONS = {64, 128, 256, 512, 768, 1024, 2048, 2560};

    private final QdrantClient qdrantClient;
    private final String collectionPrefix;

    public QdrantVectorStore(@Autowired(required = false) QdrantClient qdrantClient,
                             @Value("${rag.qdrant.collection-prefix:apboa_rag}") String collectionPrefix) {
        this.qdrantClient = qdrantClient;
        this.collectionPrefix = collectionPrefix;
        if (qdrantClient != null) {
            initCollections();
        }
    }

    private String getCollectionName(int dimension) {
        return collectionPrefix + "_" + dimension;
    }

    private void initCollections() {
        try {
            for (int dim : SUPPORTED_DIMENSIONS) {
                String collectionName = getCollectionName(dim);
                ensureCollectionExists(collectionName, dim);
            }
            log.info("Qdrant集合初始化完成，共{}个集合", SUPPORTED_DIMENSIONS.length);
        } catch (Exception e) {
            log.error("Qdrant集合初始化失败", e);
        }
    }

    private void ensureCollectionExists(String collectionName, int dimension) throws ExecutionException, InterruptedException {
        boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
        if (!exists) {
            Collections.Distance distance = Collections.Distance.Cosine;
            qdrantClient.createCollectionAsync(collectionName,
                    Collections.VectorParams.newBuilder()
                            .setSize(dimension)
                            .setDistance(distance)
                            .build()).get();
            log.info("自动创建Qdrant集合: {}, dimension={}", collectionName, dimension);
        }
    }

    @Override
    public boolean isAvailable() {
        return qdrantClient != null;
    }

    /**
     * 存储向量，根据embedding数组长度自动确定目标集合
     */
    @Override
    public void storeEmbedding(Long id, Long chunkId, Long documentId,
                               Long knowledgeBaseConfigId, float[] embedding) {
        if (!isAvailable()) {
            throw new RuntimeException("Qdrant客户端未配置");
        }

        int dimension = embedding.length;
        String collectionName = getCollectionName(dimension);

        Points.PointStruct point = Points.PointStruct.newBuilder()
                .setId(id(id))
                .setVectors(vectors(embedding))
                .putPayload("chunk_id", value(chunkId))
                .putPayload("document_id", value(documentId))
                .putPayload("knowledge_base_config_id", value(knowledgeBaseConfigId))
                .build();

        try {
            qdrantClient.upsertAsync(collectionName, List.of(point)).get();
        } catch (Exception e) {
            throw new RuntimeException("Qdrant向量存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void storeEmbeddings(List<EmbeddingRecord> records) {
        if (!isAvailable()) {
            throw new RuntimeException("Qdrant客户端未配置");
        }

        for (EmbeddingRecord record : records) {
            storeEmbedding(record.id(), record.chunkId(), record.documentId(),
                    record.knowledgeBaseConfigId(), record.embedding());
        }
    }

    @Override
    public List<RetrievalResult> search(float[] queryEmbedding, Long knowledgeBaseConfigId,
                                        int limit, double scoreThreshold) {
        if (!isAvailable()) {
            throw new RuntimeException("Qdrant客户端未配置");
        }

        int dimension = queryEmbedding.length;
        String collectionName = getCollectionName(dimension);

        // 检查集合是否存在
        try {
            if (!qdrantClient.collectionExistsAsync(collectionName).get()) {
                log.warn("集合不存在: {}", collectionName);
                return List.of();
            }
        } catch (Exception e) {
            log.error("检查集合存在性失败: {}", collectionName, e);
            return List.of();
        }

        Common.Filter filter = Common.Filter.newBuilder()
                .addMust(match("knowledge_base_config_id", knowledgeBaseConfigId))
                .build();

        try {
            // 构建搜索请求，直接使用服务端过滤
            Points.SearchPoints.Builder searchBuilder = Points.SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(floatList(queryEmbedding))
                    .setFilter(filter)
                    .setLimit(limit)
                    .setWithPayload(enable(true));

            // 仅在阈值有效时设置（>0 且 <1）
            if (scoreThreshold > 0 && scoreThreshold < 1) {
                searchBuilder.setScoreThreshold((float) scoreThreshold);
            }

            List<Points.ScoredPoint> results = qdrantClient.searchAsync(searchBuilder.build()).get();

            // 直接转换结果，无需再过滤分数
            List<RetrievalResult> retrievalResults = new ArrayList<>(results.size());
            for (Points.ScoredPoint point : results) {
                Map<String, JsonWithInt.Value> payload = point.getPayloadMap();

                // 安全的 payload 解析，避免 NPE
                if (!payload.containsKey("chunk_id") || !payload.containsKey("document_id")) {
                    log.warn("Payload 缺少必要字段，point id: {}", point.getId());
                    continue;
                }

                long chunkId = payload.get("chunk_id").getIntegerValue();
                long documentId = payload.get("document_id").getIntegerValue();
                double score = point.getScore();

                retrievalResults.add(new RetrievalResult(chunkId, documentId, score));
            }

            return retrievalResults;
        } catch (Exception e) {
            throw new RuntimeException("Qdrant向量检索失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (!isAvailable()) return;
        for (int dim : SUPPORTED_DIMENSIONS) {
            String collectionName = getCollectionName(dim);
            Common.Filter filter = Common.Filter.newBuilder()
                    .addMust(match("document_id", documentId))
                    .build();
            try {
                qdrantClient.deleteAsync(collectionName, filter).get();
            } catch (Exception e) {
                log.warn("删除文档向量失败, collection={}, documentId={}", collectionName, documentId, e);
            }
        }
    }

    @Override
    public void deleteByKnowledgeBaseConfigId(Long knowledgeBaseConfigId) {
        if (!isAvailable()) return;
        for (int dim : SUPPORTED_DIMENSIONS) {
            String collectionName = getCollectionName(dim);
            Common.Filter filter = Common.Filter.newBuilder()
                    .addMust(match("knowledge_base_config_id", knowledgeBaseConfigId))
                    .build();
            try {
                qdrantClient.deleteAsync(collectionName, filter).get();
            } catch (Exception e) {
                log.warn("删除知识库向量失败, collection={}, knowledgeBaseConfigId={}", collectionName, knowledgeBaseConfigId, e);
            }
        }
    }

    @Override
    public void deleteByChunkId(Long chunkId) {
        if (!isAvailable()) return;
        for (int dim : SUPPORTED_DIMENSIONS) {
            String collectionName = getCollectionName(dim);
            Common.Filter filter = Common.Filter.newBuilder()
                    .addMust(match("chunk_id", chunkId))
                    .build();
            try {
                qdrantClient.deleteAsync(collectionName, filter).get();
            } catch (Exception e) {
                log.warn("删除分块向量失败, collection={}, chunkId={}", collectionName, chunkId, e);
            }
        }
    }

    private List<Float> floatList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float v : arr) {
            list.add(v);
        }
        return list;
    }
}
