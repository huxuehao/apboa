package com.hxh.apboa.core.knowledge.impl;

import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.common.enums.KbType;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.core.knowledge.IKnowledge;
import com.fasterxml.jackson.databind.JsonNode;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.integration.ragflow.RAGFlowConfig;
import io.agentscope.core.rag.integration.ragflow.RAGFlowKnowledge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 描述：RAGFLOW 知识库
 *
 * @author huxuehao
 **/
@Component
public class RagFlowIKnowledge implements IKnowledge {

    private static final Logger log = LoggerFactory.getLogger(RagFlowIKnowledge.class);

    // 配置字段常量
    // 连接配置字段
    private static final String API_KEY = "apiKey";
    private static final String BASE_URL = "baseUrl";
    private static final String DATASET_IDS = "datasetIds";

    // 文档过滤字段
    private static final String DOCUMENT_IDS = "documentIds";

    // 检索配置字段
    private static final String TOP_K = "topK";
    private static final String SIMILARITY_THRESHOLD = "similarityThreshold";
    private static final String VECTOR_SIMILARITY_WEIGHT = "vectorSimilarityWeight";
    private static final String PAGE = "page";
    private static final String PAGE_SIZE = "pageSize";

    // 高级检索功能字段
    private static final String USE_KG = "useKg";
    private static final String TOC_ENHANCE = "tocEnhance";
    private static final String RERANK_ID = "rerankId";
    private static final String KEYWORD = "keyword";
    private static final String HIGHLIGHT = "highlight";
    private static final String CROSS_LANGUAGES = "crossLanguages";

    // HTTP配置字段
    private static final String HTTP_CONFIG = "httpConfig";
    private static final String TIMEOUT = "timeout";
    private static final String MAX_RETRIES = "maxRetries";
    private static final String CUSTOM_HEADERS = "customHeaders";

    // 默认值
    private static final int DEFAULT_TOP_K = 1024;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.2;
    private static final double DEFAULT_VECTOR_SIMILARITY_WEIGHT = 0.3;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final boolean DEFAULT_USE_KG = false;
    private static final boolean DEFAULT_TOC_ENHANCE = false;
    private static final boolean DEFAULT_KEYWORD = false;
    private static final boolean DEFAULT_HIGHLIGHT = false;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_MAX_RETRIES = 3;

    @Override
    public Knowledge build(KnowledgeBaseConfig knowledgeBaseConfig) {
        try {
            validateConfig(knowledgeBaseConfig);

            RAGFlowConfig config = buildRAGFlowConfig(knowledgeBaseConfig);
            return RAGFlowKnowledge.builder().config(config).build();
        } catch (Exception e) {
            log.error("Failed to build RAGFlow knowledge configuration", e);
            throw new IllegalArgumentException("Invalid RAGFlow knowledge configuration", e);
        }
    }

    private void validateConfig(KnowledgeBaseConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("KnowledgeBaseConfig cannot be null");
        }

        if (config.getConnectionConfig() == null || config.getConnectionConfig().isEmpty()) {
            throw new IllegalArgumentException("Connection config is required for RAGFlow knowledge");
        }

        // 验证必填字段
        JsonNode connectionConfig = config.getConnectionConfig();
        validateRequiredField(connectionConfig, API_KEY, "apiKey");
        validateRequiredField(connectionConfig, BASE_URL, "baseUrl");

        // 验证URL格式
        String baseUrl = JsonUtils.getStringValue(connectionConfig, BASE_URL, true);
        if (!isValidUrl(baseUrl)) {
            log.warn("Base URL '{}' may not be a valid URL format", baseUrl);
        }

        // 验证至少有一个数据集ID
        if (!connectionConfig.has(DATASET_IDS) ||
                connectionConfig.get(DATASET_IDS).isNull() ||
                (connectionConfig.get(DATASET_IDS).isArray() && connectionConfig.get(DATASET_IDS).isEmpty())) {
            throw new IllegalArgumentException("At least one datasetId is required in connection config");
        }
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        // 简单验证是否以http://或https://开头
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private void validateRequiredField(JsonNode node, String fieldName, String fieldDisplayName) {
        if (!node.has(fieldName) || node.get(fieldName).isNull() ||
                node.get(fieldName).asText().trim().isEmpty()) {
            throw new IllegalArgumentException(fieldDisplayName + " is required in connection config");
        }
    }

    private RAGFlowConfig buildRAGFlowConfig(KnowledgeBaseConfig knowledgeBaseConfig) {
        RAGFlowConfig.Builder builder = RAGFlowConfig.builder();

        // 1. 从connectionConfig获取连接配置
        applyConnectionConfig(builder, knowledgeBaseConfig.getConnectionConfig());

        // 2. 从connectionConfig获取文档过滤配置
        applyDocumentFilterConfig(builder, knowledgeBaseConfig.getConnectionConfig());

        // 3. 从retrievalConfig获取检索配置
        applyRetrievalConfig(builder, knowledgeBaseConfig.getRetrievalConfig());

        // 4. 从retrievalConfig获取高级检索功能配置
        applyAdvancedRetrievalConfig(builder, knowledgeBaseConfig.getRetrievalConfig());

        // 5. 从httpConfig获取HTTP配置
        applyHttpConfig(builder, knowledgeBaseConfig.getHttpConfig());

        return builder.build();
    }

    private void applyConnectionConfig(RAGFlowConfig.Builder builder, JsonNode connectionConfig) {
        if (connectionConfig == null) {
            throw new IllegalArgumentException("Connection config cannot be null");
        }

        // 安全获取并设置必填字段
        builder.apiKey(JsonUtils.getStringValue(connectionConfig, API_KEY, true))
                .baseUrl(JsonUtils.getStringValue(connectionConfig, BASE_URL, true));

        // 设置数据集ID
        List<String> datasetIds = getStringListValue(connectionConfig, DATASET_IDS);
        if (datasetIds != null && !datasetIds.isEmpty()) {
            for (String datasetId : datasetIds) {
                if (StringUtils.hasText(datasetId)) {
                    builder.addDatasetId(datasetId.trim());
                }
            }
        }
    }

    private List<String> getStringListValue(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }

        JsonNode valueNode = node.get(fieldName);
        List<String> result = new ArrayList<>();

        if (valueNode.isArray()) {
            for (JsonNode item : valueNode) {
                if (!item.isNull() && item.isTextual()) {
                    String value = item.asText();
                    if (StringUtils.hasText(value)) {
                        result.add(value.trim());
                    }
                }
            }
        } else if (valueNode.isTextual()) {
            // 支持逗号分隔的字符串
            String value = valueNode.asText();
            if (StringUtils.hasText(value)) {
                String[] items = value.split(",");
                for (String item : items) {
                    if (StringUtils.hasText(item.trim())) {
                        result.add(item.trim());
                    }
                }
            }
        }

        return result.isEmpty() ? null : result;
    }

    private void applyDocumentFilterConfig(RAGFlowConfig.Builder builder, JsonNode connectionConfig) {
        if (connectionConfig == null) {
            return;
        }

        List<String> documentIds = getStringListValue(connectionConfig, DOCUMENT_IDS);
        if (documentIds != null && !documentIds.isEmpty()) {
            for (String documentId : documentIds) {
                if (StringUtils.hasText(documentId)) {
                    builder.addDocumentId(documentId.trim());
                }
            }
        }
    }

    private void applyRetrievalConfig(RAGFlowConfig.Builder builder, JsonNode retrievalConfig) {
        if (retrievalConfig != null && !retrievalConfig.isEmpty()) {
            // Top K
            int topK = JsonUtils.getIntValue(retrievalConfig, TOP_K, DEFAULT_TOP_K);
            topK = Math.max(1, topK); // 确保至少为1
            builder.topK(topK);

            // 相似度阈值
            double similarityThreshold = JsonUtils.getDoubleValue(retrievalConfig, SIMILARITY_THRESHOLD, DEFAULT_SIMILARITY_THRESHOLD);
            similarityThreshold = clamp(similarityThreshold);
            builder.similarityThreshold(similarityThreshold);

            // 向量相似度权重
            double vectorSimilarityWeight = JsonUtils.getDoubleValue(retrievalConfig, VECTOR_SIMILARITY_WEIGHT, DEFAULT_VECTOR_SIMILARITY_WEIGHT);
            vectorSimilarityWeight = clamp(vectorSimilarityWeight);
            builder.vectorSimilarityWeight(vectorSimilarityWeight);

            // 分页参数
            int page = JsonUtils.getIntValue(retrievalConfig, PAGE, DEFAULT_PAGE);
            page = Math.max(1, page); // 页码至少为1
            builder.page(page);

            int pageSize = JsonUtils.getIntValue(retrievalConfig, PAGE_SIZE, DEFAULT_PAGE_SIZE);
            pageSize = Math.max(1, pageSize); // 每页数量至少为1
            builder.pageSize(pageSize);
        } else {
            builder.topK(DEFAULT_TOP_K)
                    .similarityThreshold(DEFAULT_SIMILARITY_THRESHOLD)
                    .vectorSimilarityWeight(DEFAULT_VECTOR_SIMILARITY_WEIGHT)
                    .page(DEFAULT_PAGE)
                    .pageSize(DEFAULT_PAGE_SIZE);
        }
    }

    private void applyAdvancedRetrievalConfig(RAGFlowConfig.Builder builder, JsonNode retrievalConfig) {
        if (retrievalConfig != null && !retrievalConfig.isEmpty()) {
            // 知识图谱多跳查询
            boolean useKg = JsonUtils.getBooleanValue(retrievalConfig, USE_KG, DEFAULT_USE_KG);
            builder.useKg(useKg);

            // 目录增强检索
            boolean tocEnhance = JsonUtils.getBooleanValue(retrievalConfig, TOC_ENHANCE, DEFAULT_TOC_ENHANCE);
            builder.tocEnhance(tocEnhance);

            // 重排序模型ID
            if (retrievalConfig.has(RERANK_ID) && !retrievalConfig.get(RERANK_ID).isNull()) {
                int rerankId = retrievalConfig.get(RERANK_ID).asInt();
                rerankId = Math.max(1, rerankId); // ID至少为1
                builder.rerankId(rerankId);
            }

            // 关键词匹配
            boolean keyword = JsonUtils.getBooleanValue(retrievalConfig, KEYWORD, DEFAULT_KEYWORD);
            builder.keyword(keyword);

            // 高亮匹配结果
            boolean highlight = JsonUtils.getBooleanValue(retrievalConfig, HIGHLIGHT, DEFAULT_HIGHLIGHT);
            builder.highlight(highlight);

            // 跨语言检索
            List<String> crossLanguages = getStringListValue(retrievalConfig, CROSS_LANGUAGES);
            if (crossLanguages != null && !crossLanguages.isEmpty()) {
                for (String language : crossLanguages) {
                    if (StringUtils.hasText(language) && isValidLanguageCode(language.trim())) {
                        builder.addCrossLanguage(language.trim());
                    }
                }
            }
        } else {
            builder.useKg(DEFAULT_USE_KG)
                    .tocEnhance(DEFAULT_TOC_ENHANCE)
                    .keyword(DEFAULT_KEYWORD)
                    .highlight(DEFAULT_HIGHLIGHT);
        }
    }

    private boolean isValidLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return false;
        }
        // 简单的语言代码验证（2-3个字母）
        String code = languageCode.trim().toLowerCase();
        return code.matches("^[a-z]{2,3}$");
    }

    private void applyHttpConfig(RAGFlowConfig.Builder builder, JsonNode httpConfig) {
        if (httpConfig != null && !httpConfig.isEmpty()) {
            // 超时时间
            Duration timeout = JsonUtils.getDurationValue(httpConfig, TIMEOUT, DEFAULT_TIMEOUT);
            builder.timeout(timeout);

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
            builder.timeout(DEFAULT_TIMEOUT)
                    .maxRetries(DEFAULT_MAX_RETRIES);
        }
    }




    // 工具方法：限制数值范围
    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    @Override
    public KbType type() {
        return KbType.RAGFLOW;
    }
}
