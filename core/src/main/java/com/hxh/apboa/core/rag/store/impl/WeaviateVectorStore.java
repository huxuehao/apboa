package com.hxh.apboa.core.rag.store.impl;

import com.hxh.apboa.core.rag.EmbeddingRecord;
import com.hxh.apboa.core.rag.RetrievalResult;
import com.hxh.apboa.core.rag.store.VectorStore;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.filters.WhereFilter;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.graphql.query.argument.NearVectorArgument;
import io.weaviate.client.v1.graphql.query.argument.WhereArgument;
import io.weaviate.client.v1.graphql.query.fields.Field;
import io.weaviate.client.v1.schema.model.DataType;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Weaviate向量存储服务
 *
 * @author wei.liu
 */
@Component
@ConditionalOnProperty(name = "rag.store", havingValue = "weaviate")
public class WeaviateVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(WeaviateVectorStore.class);

    /**
     * 支持的向量维度列表
     */
    private static final int[] SUPPORTED_DIMENSIONS = {64, 128, 256, 512, 768, 1024, 2048, 2560};

    private final WeaviateClient weaviateClient;
    private final String classPrefix;

    public WeaviateVectorStore(@Autowired(required = false) WeaviateClient weaviateClient,
                               @Value("${rag.weaviate.class-prefix:ApboaRag}") String classPrefix) {
        this.weaviateClient = weaviateClient;
        this.classPrefix = classPrefix;
        if (weaviateClient != null) {
            initSchema();
        } else {
            log.warn("Weaviate客户端未配置，本地RAG功能不可用");
        }
    }

    private String getClassName(int dimension) {
        return classPrefix + "_" + dimension;
    }

    private void initSchema() {
        try {
            for (int dim : SUPPORTED_DIMENSIONS) {
                String className = getClassName(dim);
                ensureClassExists(className, dim);
            }
            log.info("Weaviate schema初始化完成，共{}个class", SUPPORTED_DIMENSIONS.length);
        } catch (Exception e) {
            log.error("Weaviate schema初始化失败", e);
        }
    }

    private void ensureClassExists(String className, int dimension) {
        try {
            // 检查 class 是否已存在
            Result<Boolean> existsResult = weaviateClient.schema().exists()
                    .withClassName(className)
                    .run();
            if (existsResult.getResult() != null && existsResult.getResult()) {
                return;
            }
        } catch (Exception e) {
            log.warn("检查Weaviate class是否存在时出错: {}", e.getMessage());
        }

        // 构建属性列表
        // 使用 string 类型存储 ID，避免 Double 精度丢失（雪花算法 ID 为 19 位，Double 只有 15-16 位有效数字）
        List<Property> properties = List.of(
                Property.builder()
                        .name("chunk_id")
                        .dataType(List.of(DataType.TEXT))
                        .build(),
                Property.builder()
                        .name("document_id")
                        .dataType(List.of(DataType.TEXT))
                        .build(),
                Property.builder()
                        .name("knowledge_base_config_id")
                        .dataType(List.of(DataType.TEXT))
                        .build()
        );

        WeaviateClass clazz = WeaviateClass.builder()
                .className(className)
                .description("RAG embedding vectors for dimension " + dimension)
                .properties(properties)
                .vectorizer("none") // 使用外部向量，不启用 Weaviate 内置向量化
                .build();

        Result<Boolean> createResult = weaviateClient.schema().classCreator()
                .withClass(clazz)
                .run();

        if (createResult.getError() != null) {
            log.warn("Weaviate class创建失败（可能已存在）: {}", createResult.getError().getMessages());
        } else {
            log.info("自动创建Weaviate class: {}, dimension={}", className, dimension);
        }
    }

    @Override
    public boolean isAvailable() {
        return weaviateClient != null;
    }

    @Override
    public void storeEmbedding(Long id, Long chunkId, Long documentId,
                               Long knowledgeBaseConfigId, float[] embedding) {
        if (!isAvailable()) {
            throw new RuntimeException("Weaviate客户端未配置");
        }

        int dimension = embedding.length;
        String className = getClassName(dimension);

        Map<String, Object> properties = new HashMap<>();
        // 使用 String 类型存储 ID，避免 Double 精度丢失（雪花算法 ID 为 19 位，Double 只有 15-16 位有效数字）
        properties.put("chunk_id", String.valueOf(chunkId));
        properties.put("document_id", String.valueOf(documentId));
        properties.put("knowledge_base_config_id", String.valueOf(knowledgeBaseConfigId));

        Float[] vector = new Float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            vector[i] = embedding[i];
        }

        // Weaviate 要求 ID 必须是 UUID 格式，使用 nameUUIDFromBytes 基于原始 ID 生成确定性 UUID
        String uuid = UUID.nameUUIDFromBytes(String.valueOf(id).getBytes()).toString();
        Result<io.weaviate.client.v1.data.model.WeaviateObject> result = weaviateClient.data().creator()
                .withID(uuid)
                .withClassName(className)
                .withProperties(properties)
                .withVector(vector)
                .run();

        if (result.getError() != null) {
            String errorMsg = result.getError().getMessages().stream()
                    .map(io.weaviate.client.base.WeaviateErrorMessage::getMessage)
                    .collect(java.util.stream.Collectors.joining(", "));
            throw new RuntimeException("Weaviate向量存储失败: " + errorMsg);
        }
    }

    @Override
    public void storeEmbeddings(List<EmbeddingRecord> records) {
        if (!isAvailable()) {
            throw new RuntimeException("Weaviate客户端未配置");
        }

        for (EmbeddingRecord record : records) {
            storeEmbedding(record.id(), record.chunkId(), record.documentId(),
                    record.knowledgeBaseConfigId(), record.embedding());
        }
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<RetrievalResult> search(float[] queryEmbedding, Long knowledgeBaseConfigId,
                                        int limit, double scoreThreshold) {
        if (!isAvailable()) {
            throw new RuntimeException("Weaviate客户端未配置");
        }

        int dimension = queryEmbedding.length;
        String className = getClassName(dimension);

        Float[] vector = new Float[dimension];
        for (int i = 0; i < dimension; i++) {
            vector[i] = queryEmbedding[i];
        }

        // 构建 WhereFilter（使用 String 类型匹配，因为 ID 以 String 类型存储）
        WhereFilter whereFilter = WhereFilter.builder()
                .path("knowledge_base_config_id")
                .operator("Equal")
                .valueText(String.valueOf(knowledgeBaseConfigId))
                .build();

        // 构建 NearVectorArgument
        NearVectorArgument nearVector = NearVectorArgument.builder()
                .vector(vector)
                .build();

        // 构建查询字段
        Field chunkIdField = Field.builder().name("chunk_id").build();
        Field documentIdField = Field.builder().name("document_id").build();
        Field knowledgeBaseConfigIdField = Field.builder().name("knowledge_base_config_id").build();

        // 同时请求 distance 和 certainty，确保至少有一个能返回
        Field distanceField = Field.builder().name("distance").build();
        Field certaintyField = Field.builder().name("certainty").build();
        Field _additional = Field.builder()
                .name("_additional")
                .fields(distanceField, certaintyField)
                .build();

        Field[] fields = new Field[]{
                chunkIdField, documentIdField, knowledgeBaseConfigIdField, _additional
        };

        Result<GraphQLResponse> result = weaviateClient.graphQL().get()
                .withClassName(className)
                .withFields(fields)
                .withWhere(WhereArgument.builder().filter(whereFilter).build())
                .withNearVector(nearVector)
                .withLimit(limit)
                .run();

        if (result.getError() != null) {
            String errorMsg = result.getError().getMessages().stream()
                    .map(io.weaviate.client.base.WeaviateErrorMessage::getMessage)
                    .collect(java.util.stream.Collectors.joining(", "));
            throw new RuntimeException("Weaviate向量检索失败: " + errorMsg);
        }

        if (result.getResult() == null || result.getResult().getData() == null) {
            return List.of();
        }

        // 解析 GraphQL 返回结果
        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        if (get == null) return List.of();

        List<Map<String, Object>> items = (List<Map<String, Object>>) get.get(className);
        if (items == null || items.isEmpty()) return List.of();

        List<RetrievalResult> retrievalResults = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, Object> additional = (Map<String, Object>) item.get("_additional");
            if (additional == null) continue;

            double score;
            // 优先使用 distance（余弦距离），如果不可用则使用 certainty
            // distance 范围 [0, 2]，0 表示完全相同，2 表示完全相反
            // certainty 范围 [0, 1]，1 表示完全相同，0 表示完全相反
            Object distanceObj = additional.get("distance");
            Object certaintyObj = additional.get("certainty");
            if (distanceObj != null) {
                double distance = ((Number) distanceObj).doubleValue();
                score = 1.0 - distance / 2.0;
            } else if (certaintyObj != null) {
                score = ((Number) certaintyObj).doubleValue();
            } else {
                log.warn("_additional 中既没有 distance 也没有 certainty，跳过该结果");
                continue;
            }

            // 应用分数阈值过滤
            if (score < scoreThreshold) continue;

            Object chunkIdObj = item.get("chunk_id");
            Object documentIdObj = item.get("document_id");
            if (chunkIdObj == null || documentIdObj == null) continue;

            // ID 以 String 类型存储，需要从 String 解析回 Long
            long chunkId = Long.parseLong(String.valueOf(chunkIdObj));
            long documentId = Long.parseLong(String.valueOf(documentIdObj));

            retrievalResults.add(new RetrievalResult(chunkId, documentId, score));
        }

        return retrievalResults;
    }

    @Override
    public void deleteByDocumentId(Long documentId) {
        if (!isAvailable()) return;
        for (int dim : SUPPORTED_DIMENSIONS) {
            String className = getClassName(dim);
            WhereFilter whereFilter = WhereFilter.builder()
                    .path("document_id")
                    .operator("Equal")
                    .valueText(String.valueOf(documentId))
                    .build();
            try {
                weaviateClient.batch().objectsBatchDeleter()
                        .withClassName(className)
                        .withWhere(whereFilter)
                        .run();
            } catch (Exception e) {
                log.warn("删除文档向量失败, class={}, documentId={}", className, documentId, e);
            }
        }
    }

    @Override
    public void deleteByKnowledgeBaseConfigId(Long knowledgeBaseConfigId) {
        if (!isAvailable()) return;
        for (int dim : SUPPORTED_DIMENSIONS) {
            String className = getClassName(dim);
            WhereFilter whereFilter = WhereFilter.builder()
                    .path("knowledge_base_config_id")
                    .operator("Equal")
                    .valueText(String.valueOf(knowledgeBaseConfigId))
                    .build();
            try {
                weaviateClient.batch().objectsBatchDeleter()
                        .withClassName(className)
                        .withWhere(whereFilter)
                        .run();
            } catch (Exception e) {
                log.warn("删除知识库向量失败, class={}, knowledgeBaseConfigId={}", className, knowledgeBaseConfigId, e);
            }
        }
    }

    @Override
    public void deleteByChunkId(Long chunkId) {
        if (!isAvailable()) return;
        for (int dim : SUPPORTED_DIMENSIONS) {
            String className = getClassName(dim);
            WhereFilter whereFilter = WhereFilter.builder()
                    .path("chunk_id")
                    .operator("Equal")
                    .valueText(String.valueOf(chunkId))
                    .build();
            try {
                weaviateClient.batch().objectsBatchDeleter()
                        .withClassName(className)
                        .withWhere(whereFilter)
                        .run();
            } catch (Exception e) {
                log.warn("删除分块向量失败, class={}, chunkId={}", className, chunkId, e);
            }
        }
    }
}
