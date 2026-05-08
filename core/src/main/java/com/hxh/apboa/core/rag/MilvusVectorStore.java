package com.hxh.apboa.core.rag;

import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Milvus向量存储服务
 *
 * @author huxuehao
 */
@Component
@ConditionalOnProperty(name = "rag.store", havingValue = "milvus")
public class MilvusVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStore.class);

    private static final int[] SUPPORTED_DIMENSIONS = {64, 128, 256, 512, 768, 1024, 2048, 2560};

    private final MilvusServiceClient milvusClient;

    public MilvusVectorStore(@Autowired(required = false) MilvusServiceClient milvusClient) {
        this.milvusClient = milvusClient;
        if (milvusClient != null) {
            initSchema();
        } else {
            log.warn("Milvus客户端未配置，本地RAG功能不可用");
        }
    }

    private String getCollectionName(int dimension) {
        return "rag_embedding_" + dimension;
    }

    private void initSchema() {
        for (int dim : SUPPORTED_DIMENSIONS) {
            String collectionName = getCollectionName(dim);
            try {
                R<Boolean> hasColl = milvusClient.hasCollection(
                        HasCollectionParam.newBuilder().withCollectionName(collectionName).build());
                if (hasColl.getData()) {
                    ensureLoaded(collectionName);
                    continue;
                }

                List<FieldType> fields = List.of(
                        FieldType.newBuilder()
                                .withName("id")
                                .withDataType(DataType.Int64)
                                .withPrimaryKey(true)
                                .withAutoID(false)
                                .build(),
                        FieldType.newBuilder()
                                .withName("chunk_id")
                                .withDataType(DataType.Int64)
                                .build(),
                        FieldType.newBuilder()
                                .withName("document_id")
                                .withDataType(DataType.Int64)
                                .build(),
                        FieldType.newBuilder()
                                .withName("knowledge_base_config_id")
                                .withDataType(DataType.Int64)
                                .build(),
                        FieldType.newBuilder()
                                .withName("embedding")
                                .withDataType(DataType.FloatVector)
                                .withDimension(dim)
                                .build()
                );

                CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withFieldTypes(fields)
                        .build();
                milvusClient.createCollection(createParam);

                CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withFieldName("embedding")
                        .withIndexType(IndexType.HNSW)
                        .withMetricType(MetricType.COSINE)
                        .withExtraParam("{\"M\": 16, \"efConstruction\": 200}")
                        .build();
                milvusClient.createIndex(indexParam);

                milvusClient.loadCollection(
                        LoadCollectionParam.newBuilder().withCollectionName(collectionName).build());

                log.info("Milvus集合创建完成: {}", collectionName);
            } catch (Exception e) {
                log.error("Milvus集合初始化失败: {}", collectionName, e);
            }
        }
        log.info("Milvus向量存储初始化完成，共{}个集合", SUPPORTED_DIMENSIONS.length);
    }

    private void ensureLoaded(String collectionName) {
        try {
            milvusClient.loadCollection(
                    LoadCollectionParam.newBuilder().withCollectionName(collectionName).build());
        } catch (Exception e) {
            log.debug("集合加载检查（可能已加载）: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return milvusClient != null;
    }

    @Override
    public void storeEmbedding(Long id, Long chunkId, Long documentId,
                               Long knowledgeBaseConfigId, float[] embedding) {
        if (!isAvailable()) {
            throw new RuntimeException("Milvus客户端未配置");
        }

        int dimension = embedding.length;
        String collectionName = getCollectionName(dimension);

        List<Float> vector = new ArrayList<>(embedding.length);
        for (float v : embedding) {
            vector.add(v);
        }

        List<InsertParam.Field> fields = List.of(
                new InsertParam.Field("id", List.of(id)),
                new InsertParam.Field("chunk_id", List.of(chunkId)),
                new InsertParam.Field("document_id", List.of(documentId)),
                new InsertParam.Field("knowledge_base_config_id", List.of(knowledgeBaseConfigId)),
                new InsertParam.Field("embedding", List.of(vector))
        );

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();
        milvusClient.insert(insertParam);
    }

    @Override
    public void storeEmbeddings(List<EmbeddingRecord> records) {
        if (!isAvailable()) {
            throw new RuntimeException("Milvus客户端未配置");
        }
        if (records.isEmpty()) {
            return;
        }

        int dimension = records.getFirst().embedding().length;
        String collectionName = getCollectionName(dimension);

        List<Long> ids = new ArrayList<>();
        List<Long> chunkIds = new ArrayList<>();
        List<Long> docIds = new ArrayList<>();
        List<Long> kbConfigIds = new ArrayList<>();
        List<List<Float>> vectors = new ArrayList<>();

        for (EmbeddingRecord record : records) {
            ids.add(record.id());
            chunkIds.add(record.chunkId());
            docIds.add(record.documentId());
            kbConfigIds.add(record.knowledgeBaseConfigId());

            List<Float> vector = new ArrayList<>(record.embedding().length);
            for (float v : record.embedding()) {
                vector.add(v);
            }
            vectors.add(vector);
        }

        List<InsertParam.Field> fields = List.of(
                new InsertParam.Field("id", ids),
                new InsertParam.Field("chunk_id", chunkIds),
                new InsertParam.Field("document_id", docIds),
                new InsertParam.Field("knowledge_base_config_id", kbConfigIds),
                new InsertParam.Field("embedding", vectors)
        );

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();
        milvusClient.insert(insertParam);
    }

    @Override
    public List<RetrievalResult> search(float[] queryEmbedding, Long knowledgeBaseConfigId,
                                         int limit, double scoreThreshold) {
        if (!isAvailable()) {
            throw new RuntimeException("Milvus客户端未配置");
        }

        int dimension = queryEmbedding.length;
        String collectionName = getCollectionName(dimension);

        List<Float> vector = new ArrayList<>(dimension);
        for (float v : queryEmbedding) {
            vector.add(v);
        }
        List<List<Float>> queryVectors = List.of(vector);

        String expr = "knowledge_base_config_id == " + knowledgeBaseConfigId;

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withTopK(limit)
                .withVectorFieldName("embedding")
                .withFloatVectors(queryVectors)
                .withOutFields(List.of("chunk_id", "document_id"))
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .withMetricType(MetricType.COSINE)
                .withExpr(expr)
                .build();

        R<SearchResults> response = milvusClient.search(searchParam);
        if (response.getData() == null) {
            return List.of();
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<SearchResultsWrapper.IDScore> idScores = wrapper.getIDScore(0);
        List<?> fieldChunkIds = wrapper.getFieldData("chunk_id", 0);
        List<?> fieldDocIds = wrapper.getFieldData("document_id", 0);

        List<RetrievalResult> results = new ArrayList<>();
        for (int i = 0; i < idScores.size(); i++) {
            float score = idScores.get(i).getScore();
            if (score >= scoreThreshold) {
                long chunkId = ((Number) fieldChunkIds.get(i)).longValue();
                long docId = ((Number) fieldDocIds.get(i)).longValue();
                results.add(new RetrievalResult(chunkId, docId, score));
            }
        }

        return results;
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (!isAvailable()) return;
        String expr = "document_id == " + documentId;
        for (int dim : SUPPORTED_DIMENSIONS) {
            String collectionName = getCollectionName(dim);
            try {
                milvusClient.delete(DeleteParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withExpr(expr)
                        .build());
            } catch (Exception e) {
                log.warn("Milvus删除失败, collection={}, docId={}: {}", collectionName, documentId, e.getMessage());
            }
        }
    }

    @Override
    public void deleteByKnowledgeBaseConfigId(Long knowledgeBaseConfigId) {
        if (!isAvailable()) return;
        String expr = "knowledge_base_config_id == " + knowledgeBaseConfigId;
        for (int dim : SUPPORTED_DIMENSIONS) {
            String collectionName = getCollectionName(dim);
            try {
                milvusClient.delete(DeleteParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withExpr(expr)
                        .build());
            } catch (Exception e) {
                log.warn("Milvus删除失败, collection={}, kbConfigId={}: {}", collectionName, knowledgeBaseConfigId, e.getMessage());
            }
        }
    }

    @Override
    public void deleteByChunkId(Long chunkId) {
        if (!isAvailable()) return;
        String expr = "chunk_id == " + chunkId;
        for (int dim : SUPPORTED_DIMENSIONS) {
            String collectionName = getCollectionName(dim);
            try {
                milvusClient.delete(DeleteParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withExpr(expr)
                        .build());
            } catch (Exception e) {
                log.warn("Milvus删除失败, collection={}, chunkId={}: {}", collectionName, chunkId, e.getMessage());
            }
        }
    }
}
