package com.hxh.apboa.core.rag;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.Points;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
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
 * Qdrant向量存储服务，负责与Qdrant向量数据库的交互
 *
 * @author huxuehao
 */
@Component
public class QdrantVectorStore {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStore.class);

    /**
     * 支持的向量维度列表
     */
    private static final int[] SUPPORTED_DIMENSIONS = {1024, 2048, 2560};

    private final QdrantClient qdrantClient;
    private final String collectionPrefix;

    public QdrantVectorStore(@Autowired(required = false) QdrantClient qdrantClient,
                             @Value("${rag.qdrant.collection-prefix:apboa_rag}") String collectionPrefix) {
        this.qdrantClient = qdrantClient;
        this.collectionPrefix = collectionPrefix;
    }

    @PostConstruct
    public void init() {
        if (qdrantClient != null) {
            initCollections();
        }
    }

    /**
     * 根据向量维度拼接集合名称
     */
    private String getCollectionName(int dimension) {
        return collectionPrefix + "_" + dimension;
    }

    /**
     * 初始化多维度集合结构
     */
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

    /**
     * 是否可用
     */
    public boolean isAvailable() {
        return qdrantClient != null;
    }

    /**
     * 存储向量，根据embedding数组长度自动确定目标集合
     */
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

    /**
     * 批量存储向量
     */
    public void storeEmbeddings(List<EmbeddingRecord> records) {
        if (!isAvailable()) {
            throw new RuntimeException("Qdrant客户端未配置");
        }

        for (EmbeddingRecord record : records) {
            storeEmbedding(record.id, record.chunkId, record.documentId,
                    record.knowledgeBaseConfigId, record.embedding);
        }
    }

    /**
     * 向量相似度检索，根据查询向量长度自动确定目标集合
     */
    public List<RetrievalResult> search(float[] queryEmbedding, Long knowledgeBaseConfigId,
                                        int limit, double scoreThreshold) {
        if (!isAvailable()) {
            throw new RuntimeException("Qdrant客户端未配置");
        }

        int dimension = queryEmbedding.length;
        String collectionName = getCollectionName(dimension);

        Common.Filter filter = Common.Filter.newBuilder()
                .addMust(match("knowledge_base_config_id", knowledgeBaseConfigId))
                .build();

        try {
            List<Points.ScoredPoint> results = qdrantClient.searchAsync(
                    Points.SearchPoints.newBuilder()
                            .setCollectionName(collectionName)
                            .addAllVector(floatList(queryEmbedding))
                            .setFilter(filter)
                            .setLimit(limit)
                            .setWithPayload(enable(true))
                            .build()).get();

            List<RetrievalResult> retrievalResults = new ArrayList<>();
            for (Points.ScoredPoint point : results) {
                double score = point.getScore();
                if (score >= scoreThreshold) {
                    Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = point.getPayloadMap();
                    long chunkId = payload.get("chunk_id").getIntegerValue();
                    long documentId = payload.get("document_id").getIntegerValue();
                    retrievalResults.add(new RetrievalResult(chunkId, documentId, score));
                }
            }

            return retrievalResults;
        } catch (Exception e) {
            throw new RuntimeException("Qdrant向量检索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除指定文档的所有向量（遍历所有维度集合）
     */
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

    /**
     * 删除指定知识库的所有向量（遍历所有维度集合）
     */
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

    /**
     * 删除指定分块的向量（遍历所有维度集合）
     */
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
