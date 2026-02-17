package com.hxh.apboa.core.knowledge.impl;

import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.common.enums.KbType;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.core.knowledge.IKnowledge;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.integration.dify.*;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 描述：DIFY 知识库
 *
 * @author huxuehao
 **/
@Component
public class DifyIKnowledge implements IKnowledge {

    private static final Logger log = LoggerFactory.getLogger(DifyIKnowledge.class);

    // 配置字段常量
    // 连接配置字段
    private static final String API_KEY = "apiKey";
    private static final String DATASET_ID = "datasetId";

    // 端点配置字段
    private static final String API_BASE_URL = "apiBaseUrl";

    // 检索配置字段
    private static final String RETRIEVAL_MODE = "retrievalMode";
    private static final String TOP_K = "topK";
    private static final String SCORE_THRESHOLD = "scoreThreshold";
    private static final String WEIGHTS = "weights";

    // 重排序配置字段
    private static final String ENABLE_RERANK = "enableRerank";
    private static final String RERANK_CONFIG = "rerankConfig";
    private static final String PROVIDER_NAME = "providerName";
    private static final String RERANK_MODEL_NAME = "modelName";
    private static final String RERANK_TOP_N = "topN";

    // 元数据过滤字段
    private static final String METADATA_FILTER = "metadataFilter";
    private static final String LOGICAL_OPERATOR = "logicalOperator";
    private static final String CONDITIONS = "conditions";
    private static final String CONDITION_NAME = "name";
    private static final String COMPARISON_OPERATOR = "comparisonOperator";
    private static final String CONDITION_VALUE = "value";

    // HTTP配置字段
    private static final String HTTP_CONFIG = "httpConfig";
    private static final String CONNECT_TIMEOUT = "connectTimeout";
    private static final String READ_TIMEOUT = "readTimeout";
    private static final String MAX_RETRIES = "maxRetries";
    private static final String CUSTOM_HEADERS = "customHeaders";

    // 默认值
    private static final String DEFAULT_API_BASE_URL = "https://api.dify.ai/v1";
    private static final RetrievalMode DEFAULT_RETRIEVAL_MODE = RetrievalMode.HYBRID_SEARCH;
    private static final int DEFAULT_TOP_K = 10;
    private static final double DEFAULT_SCORE_THRESHOLD = 0.0;
    private static final double DEFAULT_WEIGHTS = 0.6;
    private static final boolean DEFAULT_ENABLE_RERANK = false;
    private static final String DEFAULT_RERANK_PROVIDER = "cohere";
    private static final String DEFAULT_RERANK_MODEL = "rerank-english-v2.0";
    private static final String DEFAULT_LOGICAL_OPERATOR = "AND";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);
    private static final int DEFAULT_MAX_RETRIES = 3;

    @Override
    public Knowledge build(KnowledgeBaseConfig knowledgeBaseConfig) {
        try {
            validateConfig(knowledgeBaseConfig);

            DifyRAGConfig config = buildDifyRAGConfig(knowledgeBaseConfig);
            return DifyKnowledge.builder().config(config).build();
        } catch (Exception e) {
            log.error("Failed to build Dify knowledge configuration", e);
            throw new IllegalArgumentException("Invalid Dify knowledge configuration", e);
        }
    }

    private void validateConfig(KnowledgeBaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("KnowledgeBaseConfig cannot be null");
        }

        if (config.getConnectionConfig() == null || config.getConnectionConfig().isEmpty()) {
            throw new IllegalArgumentException("Connection config is required for Dify knowledge");
        }

        // 验证必填字段
        JsonNode connectionConfig = config.getConnectionConfig();
        validateRequiredField(connectionConfig, API_KEY, "apiKey");
        validateRequiredField(connectionConfig, DATASET_ID, "datasetId");

        // 验证数据集ID格式（UUID）
        String datasetId = JsonUtils.getStringValue(connectionConfig, DATASET_ID, true);
        if (!isValidUUID(datasetId)) {
            log.warn("Dataset ID '{}' may not be a valid UUID format", datasetId);
        }
    }

    private boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return false;
        }
        // 简单的UUID格式验证
        String uuidPattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
        return uuid.toLowerCase().matches(uuidPattern);
    }

    private void validateRequiredField(JsonNode node, String fieldName, String fieldDisplayName) {
        if (!node.has(fieldName) || node.get(fieldName).isNull() ||
                node.get(fieldName).asText().trim().isEmpty()) {
            throw new IllegalArgumentException(fieldDisplayName + " is required in connection config");
        }
    }

    private DifyRAGConfig buildDifyRAGConfig(KnowledgeBaseConfig knowledgeBaseConfig) {
        DifyRAGConfig.Builder builder = DifyRAGConfig.builder();

        // 1. 从connectionConfig获取连接配置
        applyConnectionConfig(builder, knowledgeBaseConfig.getConnectionConfig());

        // 2. 从endpointConfig获取端点配置
        applyEndpointConfig(builder, knowledgeBaseConfig.getEndpointConfig());

        // 3. 从retrievalConfig获取检索配置
        applyRetrievalConfig(builder, knowledgeBaseConfig.getRetrievalConfig());

        // 4. 从rerankingConfig获取重排序配置
        applyRerankingConfig(builder, knowledgeBaseConfig.getRerankingConfig());

        // 5. 从metadataFilters获取元数据过滤配置
        applyMetadataFilterConfig(builder, knowledgeBaseConfig.getMetadataFilters());

        // 6. 从httpConfig获取HTTP配置
        applyHttpConfig(builder, knowledgeBaseConfig.getHttpConfig());

        return builder.build();
    }

    private void applyConnectionConfig(DifyRAGConfig.Builder builder, JsonNode connectionConfig) {
        if (connectionConfig == null) {
            throw new IllegalArgumentException("Connection config cannot be null");
        }

        // 安全获取并设置必填字段
        builder.apiKey(JsonUtils.getStringValue(connectionConfig, API_KEY, true))
                .datasetId(JsonUtils.getStringValue(connectionConfig, DATASET_ID, true));
    }

    private void applyEndpointConfig(DifyRAGConfig.Builder builder, JsonNode endpointConfig) {
        if (endpointConfig != null && !endpointConfig.isEmpty()) {
            String apiBaseUrl = JsonUtils.getStringValue(endpointConfig, API_BASE_URL, false);
            if (apiBaseUrl != null) {
                // 验证URL格式
                if (isValidUrl(apiBaseUrl)) {
                    builder.apiBaseUrl(apiBaseUrl);
                } else {
                    log.warn("Invalid URL format for apiBaseUrl: {}, using default", apiBaseUrl);
                    builder.apiBaseUrl(DEFAULT_API_BASE_URL);
                }
            } else {
                builder.apiBaseUrl(DEFAULT_API_BASE_URL);
            }
        } else {
            builder.apiBaseUrl(DEFAULT_API_BASE_URL);
        }
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private void applyRetrievalConfig(DifyRAGConfig.Builder builder, JsonNode retrievalConfig) {
        if (retrievalConfig != null && !retrievalConfig.isEmpty()) {
            // 检索模式
            RetrievalMode retrievalMode = getRetrievalMode(retrievalConfig);
            builder.retrievalMode(retrievalMode);

            // Top K
            int topK = JsonUtils.getIntValue(retrievalConfig, TOP_K, DEFAULT_TOP_K);
            topK = clamp(topK);
            builder.topK(topK);

            // 分数阈值
            double scoreThreshold = JsonUtils.getDoubleValue(retrievalConfig, SCORE_THRESHOLD, DEFAULT_SCORE_THRESHOLD);
            scoreThreshold = clamp(scoreThreshold);
            builder.scoreThreshold(scoreThreshold);

            // 权重（仅混合搜索时有效）
            if (retrievalMode == RetrievalMode.HYBRID_SEARCH) {
                double weights = JsonUtils.getDoubleValue(retrievalConfig, WEIGHTS, DEFAULT_WEIGHTS);
                weights = clamp(weights);
                builder.weights(weights);
            }
        } else {
            builder.retrievalMode(DEFAULT_RETRIEVAL_MODE)
                    .topK(DEFAULT_TOP_K)
                    .scoreThreshold(DEFAULT_SCORE_THRESHOLD)
                    .weights(DEFAULT_WEIGHTS);
        }
    }

    private RetrievalMode getRetrievalMode(JsonNode retrievalConfig) {
        if (!retrievalConfig.has(RETRIEVAL_MODE) || retrievalConfig.get(RETRIEVAL_MODE).isNull()) {
            return DEFAULT_RETRIEVAL_MODE;
        }

        String modeStr = retrievalConfig.get(RETRIEVAL_MODE).asText();
        try {
            return RetrievalMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid retrieval mode: {}, using default: {}", modeStr, DEFAULT_RETRIEVAL_MODE);
            return DEFAULT_RETRIEVAL_MODE;
        }
    }

    private void applyRerankingConfig(DifyRAGConfig.Builder builder, JsonNode rerankingConfig) {
        if (rerankingConfig != null && !rerankingConfig.isEmpty()) {
            boolean enableRerank = JsonUtils.getBooleanValue(rerankingConfig, ENABLE_RERANK, DEFAULT_ENABLE_RERANK);
            builder.enableRerank(enableRerank);

            if (enableRerank && rerankingConfig.has(RERANK_CONFIG)) {
                JsonNode rerankConfigNode = rerankingConfig.get(RERANK_CONFIG);
                if (!rerankConfigNode.isNull()) {
                    RerankConfig rerankConfig = buildRerankConfig(rerankConfigNode);
                    builder.rerankConfig(rerankConfig);
                }
            }
        } else {
            builder.enableRerank(DEFAULT_ENABLE_RERANK);
        }
    }

    private RerankConfig buildRerankConfig(JsonNode rerankConfigNode) {
        RerankConfig.Builder builder = RerankConfig.builder();

        String providerName = JsonUtils.getStringValue(rerankConfigNode, PROVIDER_NAME, DEFAULT_RERANK_PROVIDER);
        String modelName = JsonUtils.getStringValue(rerankConfigNode, RERANK_MODEL_NAME, DEFAULT_RERANK_MODEL);

        builder.providerName(providerName)
                .modelName(modelName);

        // 可选字段：topN
        if (rerankConfigNode.has(RERANK_TOP_N) && !rerankConfigNode.get(RERANK_TOP_N).isNull()) {
            int topN = rerankConfigNode.get(RERANK_TOP_N).asInt();
            topN = Math.max(topN, 1);
            //builder.topN(topN); // TODO
        }

        return builder.build();
    }

    private void applyMetadataFilterConfig(DifyRAGConfig.Builder builder, JsonNode metadataFilters) {
        if (metadataFilters != null && !metadataFilters.isEmpty()) {
            MetadataFilter metadataFilter = buildMetadataFilter(metadataFilters);
            if (metadataFilter != null) {
                builder.metadataFilter(metadataFilter);
            }
        }
    }

    private MetadataFilter buildMetadataFilter(JsonNode metadataFilters) {
        if (!metadataFilters.has(CONDITIONS) ||
                metadataFilters.get(CONDITIONS).isNull() ||
                !metadataFilters.get(CONDITIONS).isArray()) {
            log.warn("No valid conditions found in metadata filter config");
            return null;
        }

        MetadataFilter.Builder builder = MetadataFilter.builder();

        // 逻辑运算符
        String logicalOperator = JsonUtils.getStringValue(metadataFilters, LOGICAL_OPERATOR, DEFAULT_LOGICAL_OPERATOR);
        if ("AND".equalsIgnoreCase(logicalOperator) || "OR".equalsIgnoreCase(logicalOperator)) {
            builder.logicalOperator(logicalOperator.toUpperCase());
        } else {
            log.warn("Invalid logical operator: {}, using default: {}", logicalOperator, DEFAULT_LOGICAL_OPERATOR);
            builder.logicalOperator(DEFAULT_LOGICAL_OPERATOR);
        }

        // 构建条件列表
        ArrayNode conditionsArray = (ArrayNode) metadataFilters.get(CONDITIONS);
        for (JsonNode conditionNode : conditionsArray) {
            if (conditionNode.isNull()) {
                continue;
            }

            try {
                MetadataFilterCondition condition = buildMetadataFilterCondition(conditionNode);
                builder.addCondition(condition);
            } catch (Exception e) {
                log.warn("Failed to parse metadata filter condition: {}", e.getMessage());
            }
        }

        // 如果没有任何有效条件，返回null
        if (builder.build().getConditions() == null ||
                builder.build().getConditions().isEmpty()) {
            return null;
        }

        return builder.build();
    }

    private MetadataFilterCondition buildMetadataFilterCondition(JsonNode conditionNode) {
        String name = JsonUtils.getStringValue(conditionNode, CONDITION_NAME, true);
        String comparisonOperator = JsonUtils.getStringValue(conditionNode, COMPARISON_OPERATOR, true);
        String value = JsonUtils.getStringValue(conditionNode, CONDITION_VALUE, true);

        // 验证比较运算符
        if (!isValidComparisonOperator(comparisonOperator)) {
            throw new IllegalArgumentException("Invalid comparison operator: " + comparisonOperator);
        }

        return MetadataFilterCondition.builder()
                .name(name)
                .comparisonOperator(comparisonOperator)
                .value(value)
                .build();
    }

    private boolean isValidComparisonOperator(String operator) {
        if (operator == null) {
            return false;
        }
        // 支持的比较运算符
        String[] validOperators = {"=", "!=", ">", ">=", "<", "<=", "in", "not in", "contains", "not contains"};
        for (String validOp : validOperators) {
            if (validOp.equalsIgnoreCase(operator)) {
                return true;
            }
        }
        return false;
    }

    private void applyHttpConfig(DifyRAGConfig.Builder builder, JsonNode httpConfig) {
        if (httpConfig != null && !httpConfig.isEmpty()) {
            // 连接超时
            Duration connectTimeout = JsonUtils.getDurationValue(httpConfig, CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
            builder.connectTimeout(connectTimeout);

            // 读取超时
            Duration readTimeout = JsonUtils.getDurationValue(httpConfig, READ_TIMEOUT, DEFAULT_READ_TIMEOUT);
            builder.readTimeout(readTimeout);

            // 最大重试次数
            int maxRetries = JsonUtils.getIntValue(httpConfig, MAX_RETRIES, DEFAULT_MAX_RETRIES);
            maxRetries = Math.max(0, maxRetries);
            builder.maxRetries(maxRetries);

            // 自定义请求头
            if (httpConfig.has(CUSTOM_HEADERS) && !httpConfig.get(CUSTOM_HEADERS).isNull()) {
                JsonNode headersNode = httpConfig.get(CUSTOM_HEADERS);
                if (headersNode.isObject()) {
                    headersNode.fields().forEachRemaining(entry -> {
                        String headerName = entry.getKey();
                        String headerValue = entry.getValue().asText();
                        if (StringUtils.hasText(headerName) && StringUtils.hasText(headerValue)) {
                            builder.addCustomHeader(headerName, headerValue);
                        }
                    });
                }
            }
        } else {
            builder.connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                    .readTimeout(DEFAULT_READ_TIMEOUT)
                    .maxRetries(DEFAULT_MAX_RETRIES);
        }
    }

    // 工具方法：限制数值范围
    private int clamp(int value) {
        return Math.max(1, Math.min(100, value));
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    @Override
    public KbType type() {
        return KbType.DIFY;
    }
}
