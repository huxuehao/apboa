package com.hxh.apboa.core.knowledge.impl;

import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.common.enums.KbType;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.core.knowledge.IKnowledge;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.integration.bailian.BailianConfig;
import io.agentscope.core.rag.integration.bailian.BailianKnowledge;
import io.agentscope.core.rag.integration.bailian.RerankConfig;
import io.agentscope.core.rag.integration.bailian.RewriteConfig;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 描述：BAILIAN 知识库
 *
 * @author huxuehao
 **/
@Component
public class BailianIKnowledge implements IKnowledge {

    private static final Logger log = LoggerFactory.getLogger(BailianIKnowledge.class);

    // 配置字段常量
    private static final String ACCESS_KEY_ID = "accessKeyId";
    private static final String ACCESS_KEY_SECRET = "accessKeySecret";
    private static final String WORKSPACE_ID = "workspaceId";
    private static final String INDEX_ID = "indexId";
    private static final String ENDPOINT = "endpoint";
    private static final String DENSE_SIMILARITY_TOP_K = "denseSimilarityTopK";
    private static final String SPARSE_SIMILARITY_TOP_K = "sparseSimilarityTopK";
    private static final String ENABLE_RERANKING = "enableReranking";
    private static final String RERANK_CONFIG = "rerankConfig";
    private static final String ENABLE_REWRITE = "enableRewrite";
    private static final String REWRITE_CONFIG = "rewriteConfig";
    private static final String SAVE_RETRIEVER_HISTORY = "saveRetrieverHistory";
    private static final String MODEL_NAME = "modelName";
    private static final String RERANK_MIN_SCORE = "rerankMinScore";
    private static final String RERANK_TOP_N = "rerankTopN";

    // 默认值
    private static final String DEFAULT_ENDPOINT = "bailian.cn-beijing.aliyuncs.com";
    private static final int DEFAULT_DENSE_SIMILARITY_TOP_K = 100;
    private static final int DEFAULT_SPARSE_SIMILARITY_TOP_K = 100;
    private static final boolean DEFAULT_ENABLE_RERANKING = true;
    private static final boolean DEFAULT_ENABLE_REWRITE = false;
    private static final boolean DEFAULT_SAVE_RETRIEVER_HISTORY = false;
    private static final String DEFAULT_RERANK_MODEL = "gte-rerank-hybrid";
    private static final float DEFAULT_RERANK_MIN_SCORE = 0.3f;
    private static final int DEFAULT_RERANK_TOP_N = 5;
    private static final String DEFAULT_REWRITE_MODEL = "conv-rewrite-qwen-1.8b";

    @Override
    public Knowledge build(KnowledgeBaseConfig knowledgeBaseConfig) {
        try {
            validateConfig(knowledgeBaseConfig);

            BailianConfig config = buildBailianConfig(knowledgeBaseConfig);
            return BailianKnowledge.builder().config(config).build();
        } catch (Exception e) {
            log.error("Failed to build Bailian knowledge configuration", e);
            throw new IllegalArgumentException("Invalid Bailian knowledge configuration", e);
        }
    }

    private void validateConfig(KnowledgeBaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("KnowledgeBaseConfig cannot be null");
        }

        if (config.getConnectionConfig() == null || config.getConnectionConfig().isEmpty()) {
            throw new IllegalArgumentException("Connection config is required for Bailian knowledge");
        }

        // 验证必填字段
        JsonNode connectionConfig = config.getConnectionConfig();
        validateRequiredField(connectionConfig, ACCESS_KEY_ID, "accessKeyId");
        validateRequiredField(connectionConfig, ACCESS_KEY_SECRET, "accessKeySecret");
        validateRequiredField(connectionConfig, WORKSPACE_ID, "workspaceId");
        validateRequiredField(connectionConfig, INDEX_ID, "indexId");
    }

    private void validateRequiredField(JsonNode node, String fieldName, String fieldDisplayName) {
        if (!node.has(fieldName) || node.get(fieldName).isNull() ||
                node.get(fieldName).asText().trim().isEmpty()) {
            throw new IllegalArgumentException(fieldDisplayName + " is required in connection config");
        }
    }

    private BailianConfig buildBailianConfig(KnowledgeBaseConfig knowledgeBaseConfig) {
        BailianConfig.Builder builder = BailianConfig.builder();

        // 1. 从connectionConfig获取连接配置
        applyConnectionConfig(builder, knowledgeBaseConfig.getConnectionConfig());

        // 2. 从endpointConfig获取端点配置
        applyEndpointConfig(builder, knowledgeBaseConfig.getEndpointConfig());

        // 3. 从retrievalConfig获取检索配置
        applyRetrievalConfig(builder, knowledgeBaseConfig.getRetrievalConfig());

        // 4. 从rerankingConfig获取重排序配置
        applyRerankingConfig(builder, knowledgeBaseConfig.getRerankingConfig());

        // 5. 从queryRewriteConfig获取查询重写配置
        applyQueryRewriteConfig(builder, knowledgeBaseConfig.getQueryRewriteConfig());

        // 6. 从connectionConfig获取其他配置
        applyOtherConfig(builder, knowledgeBaseConfig.getConnectionConfig());

        return builder.build();
    }

    private void applyConnectionConfig(BailianConfig.Builder builder, JsonNode connectionConfig) {
        if (connectionConfig == null) {
            throw new IllegalArgumentException("Connection config cannot be null");
        }

        // 安全获取并设置必填字段
        builder.accessKeyId(JsonUtils.getStringValue(connectionConfig, ACCESS_KEY_ID, true))
                .accessKeySecret(JsonUtils.getStringValue(connectionConfig, ACCESS_KEY_SECRET, true))
                .workspaceId(JsonUtils.getStringValue(connectionConfig, WORKSPACE_ID, true))
                .indexId(JsonUtils.getStringValue(connectionConfig, INDEX_ID, true));
    }

    private void applyEndpointConfig(BailianConfig.Builder builder, JsonNode endpointConfig) {
        if (endpointConfig != null && !endpointConfig.isEmpty()) {
            String endpoint = JsonUtils.getStringValue(endpointConfig, ENDPOINT, false);
            builder.endpoint(Objects.requireNonNullElse(endpoint, DEFAULT_ENDPOINT));
        } else {
            builder.endpoint(DEFAULT_ENDPOINT);
        }
    }

    private void applyRetrievalConfig(BailianConfig.Builder builder, JsonNode retrievalConfig) {
        if (retrievalConfig != null && !retrievalConfig.isEmpty()) {
            int denseTopK = JsonUtils.getIntValue(retrievalConfig, DENSE_SIMILARITY_TOP_K, DEFAULT_DENSE_SIMILARITY_TOP_K);
            int sparseTopK = JsonUtils.getIntValue(retrievalConfig, SPARSE_SIMILARITY_TOP_K, DEFAULT_SPARSE_SIMILARITY_TOP_K);

            // 验证范围
            denseTopK = clamp(denseTopK);
            sparseTopK = clamp(sparseTopK);

            builder.denseSimilarityTopK(denseTopK)
                    .sparseSimilarityTopK(sparseTopK);
        } else {
            builder.denseSimilarityTopK(DEFAULT_DENSE_SIMILARITY_TOP_K)
                    .sparseSimilarityTopK(DEFAULT_SPARSE_SIMILARITY_TOP_K);
        }
    }

    private void applyRerankingConfig(BailianConfig.Builder builder, JsonNode rerankingConfig) {
        if (rerankingConfig != null && !rerankingConfig.isEmpty()) {
            boolean enableReranking = JsonUtils.getBooleanValue(rerankingConfig, ENABLE_RERANKING, DEFAULT_ENABLE_RERANKING);
            builder.enableReranking(enableReranking);

            if (enableReranking && rerankingConfig.has(RERANK_CONFIG)) {
                JsonNode rerankConfigNode = rerankingConfig.get(RERANK_CONFIG);
                if (!rerankConfigNode.isNull()) {
                    RerankConfig rerankConfig = buildRerankConfig(rerankConfigNode);
                    builder.rerankConfig(rerankConfig);
                }
            }
        } else {
            builder.enableReranking(DEFAULT_ENABLE_RERANKING)
                    .rerankConfig(RerankConfig.builder()
                            .modelName(DEFAULT_RERANK_MODEL)
                            .rerankMinScore(DEFAULT_RERANK_MIN_SCORE)
                            .rerankTopN(DEFAULT_RERANK_TOP_N)
                            .build());
        }
    }

    private RerankConfig buildRerankConfig(JsonNode rerankConfigNode) {
        RerankConfig.Builder builder = RerankConfig.builder();

        String modelName = JsonUtils.getStringValue(rerankConfigNode, MODEL_NAME, DEFAULT_RERANK_MODEL);
        float minScore = JsonUtils.getFloatValue(rerankConfigNode, RERANK_MIN_SCORE, DEFAULT_RERANK_MIN_SCORE);
        int topN = JsonUtils.getIntValue(rerankConfigNode, RERANK_TOP_N, DEFAULT_RERANK_TOP_N);

        // 验证范围
        minScore = clamp(minScore);
        topN = Math.max(topN, 1);

        return builder.modelName(modelName)
                .rerankMinScore(minScore)
                .rerankTopN(topN)
                .build();
    }

    private void applyQueryRewriteConfig(BailianConfig.Builder builder, JsonNode queryRewriteConfig) {
        if (queryRewriteConfig != null && !queryRewriteConfig.isEmpty()) {
            boolean enableRewrite = JsonUtils.getBooleanValue(queryRewriteConfig, ENABLE_REWRITE, DEFAULT_ENABLE_REWRITE);
            builder.enableRewrite(enableRewrite);

            if (enableRewrite && queryRewriteConfig.has(REWRITE_CONFIG)) {
                JsonNode rewriteConfigNode = queryRewriteConfig.get(REWRITE_CONFIG);
                if (!rewriteConfigNode.isNull()) {
                    RewriteConfig rewriteConfig = buildRewriteConfig(rewriteConfigNode);
                    builder.rewriteConfig(rewriteConfig);
                }
            }
        } else {
            builder.enableRewrite(DEFAULT_ENABLE_REWRITE);
        }
    }

    private RewriteConfig buildRewriteConfig(JsonNode rewriteConfigNode) {
        String modelName = JsonUtils.getStringValue(rewriteConfigNode, MODEL_NAME, DEFAULT_REWRITE_MODEL);
        return RewriteConfig.builder()
                .modelName(modelName)
                .build();
    }

    private void applyOtherConfig(BailianConfig.Builder builder, JsonNode connectionConfig) {
        if (connectionConfig != null && !connectionConfig.isEmpty()) {
            boolean saveHistory = JsonUtils.getBooleanValue(connectionConfig, SAVE_RETRIEVER_HISTORY, DEFAULT_SAVE_RETRIEVER_HISTORY);
            builder.saveRetrieverHistory(saveHistory);
        } else {
            builder.saveRetrieverHistory(DEFAULT_SAVE_RETRIEVER_HISTORY);
        }
    }


    // 工具方法：限制数值范围
    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private float clamp(float value) {
        return Math.max((float) 0.0, Math.min((float) 1.0, value));
    }

    @Override
    public KbType type() {
        return KbType.BAILIAN;
    }
}
