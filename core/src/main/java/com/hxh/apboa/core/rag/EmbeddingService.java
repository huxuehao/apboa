package com.hxh.apboa.core.rag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.core.rag.embedding.EmbeddingProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 嵌入服务门面，根据 providerType 路由到对应的 EmbeddingProvider 实现。
 * 新增提供商只需实现 EmbeddingProvider 接口并添加 @Component，无需修改本类。
 *
 * @author huxuehao
 */
@Component
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final List<EmbeddingProvider> providers;
    private final Map<String, EmbeddingProvider> providerMap = new HashMap<>();

    public EmbeddingService(List<EmbeddingProvider> providers) {
        this.providers = providers;
    }

    @PostConstruct
    private void init() {
        for (EmbeddingProvider provider : providers) {
            providerMap.put(provider.getType().toLowerCase(), provider);
            log.info("注册EmbeddingProvider: {} -> {}", provider.getType(), provider.getClass().getSimpleName());
        }
    }

    /**
     * 对文本列表进行向量化
     *
     * @param texts   文本列表
     * @param config  知识库配置
     * @return 向量列表，与输入文本一一对应
     */
    public List<float[]> embed(List<String> texts, KnowledgeBaseConfig config) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        JsonNode connectionConfig = config.getConnectionConfig();
        EmbeddingProvider provider = resolveProvider(connectionConfig);
        String fullUrl = getFullUrl(connectionConfig, provider);
        String modelName = getModelName(connectionConfig, provider);
        int bufferSizeMb = getBufferSizeMb(connectionConfig);
        Integer dimension = getDimension(connectionConfig);
        int batchSize = getBatchSize(connectionConfig);

        try {
            URI uri = new URI(fullUrl);
            String base = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            String path = uri.getPath() + (uri.getQuery() != null ? "?" + uri.getQuery() : "");

            ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(bufferSizeMb * 1024 * 1024))
                    .build();

            WebClient webClient = WebClient.builder()
                    .baseUrl(base)
                    .exchangeStrategies(exchangeStrategies)
                    .build();

            // 按批次大小切割，逐批请求
            List<float[]> allResults = new ArrayList<>();
            for (int i = 0; i < texts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, texts.size());
                List<String> batch = texts.subList(i, end);

                String requestBody = objectMapper.writeValueAsString(new EmbedRequest(modelName, batch, dimension));
                String response = provider.execute(webClient, path, requestBody, connectionConfig);
                allResults.addAll(provider.parseResponse(response));

                if (log.isDebugEnabled()) {
                    log.debug("Embedding批次完成, provider={}, batch {}-{}/{}", provider.getType(), i + 1, end, texts.size());
                }
            }
            return allResults;
        } catch (Exception e) {
            log.error("调用Embedding API失败, provider={}, baseUrl={}, model={}", provider.getType(), fullUrl, modelName, e);
            throw new RuntimeException("文本向量化失败" + e.getMessage(), e);
        }
    }

    /**
     * 对单个文本进行向量化
     */
    public float[] embed(String text, KnowledgeBaseConfig config) {
        List<float[]> results = embed(List.of(text), config);
        if (results.isEmpty()) {
            throw new RuntimeException("文本向量化返回空结果");
        }
        return results.get(0);
    }

    /**
     * 解析 API Key，优先使用环境变量名获取，其次使用直接配置的 apiKey。
     * 各 EmbeddingProvider 可调用此静态方法解析凭证。
     */
    public static String resolveApiKey(JsonNode connectionConfig) {
        String envVarName = JsonUtils.getStringValue(connectionConfig, "envVarName", null);
        if (envVarName != null && !envVarName.isBlank()) {
            String envValue = System.getenv(envVarName);
            if (envValue != null && !envValue.isBlank()) {
                return envValue;
            }
            log.warn("环境变量 {} 未设置或为空，回退使用 apiKey 字段", envVarName);
        }

        String apiKey = JsonUtils.getStringValue(connectionConfig, "apiKey", null);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API Key 未配置，请设置 apiKey 或 envVarName");
        }

        if (apiKey.startsWith("${") && apiKey.endsWith("}")) {
            String envName = apiKey.substring(2, apiKey.length() - 1);
            String envValue = System.getenv(envName);
            if (envValue != null && !envValue.isBlank()) {
                return envValue;
            }
            throw new IllegalArgumentException("环境变量 " + envName + " 未设置或为空");
        }

        return apiKey;
    }

    private EmbeddingProvider resolveProvider(JsonNode connectionConfig) {
        String providerType = JsonUtils.getStringValue(connectionConfig, "providerType", "ollama");
        EmbeddingProvider provider = providerMap.get(providerType.toLowerCase());
        if (provider == null) {
            throw new IllegalArgumentException("不支持的嵌入模型提供商: " + providerType + "，已注册: " + providerMap.keySet());
        }
        return provider;
    }

    private String getFullUrl(JsonNode connectionConfig, EmbeddingProvider provider) {
        String url = JsonUtils.getStringValue(connectionConfig, "baseUrl", provider.getDefaultBaseUrl());
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String getModelName(JsonNode connectionConfig, EmbeddingProvider provider) {
        return JsonUtils.getStringValue(connectionConfig, "embeddingModel", provider.getDefaultModel());
    }

    private int getBufferSizeMb(JsonNode connectionConfig) {
        return JsonUtils.getIntValue(connectionConfig, "bufferSizeMb", 50);
    }

    private int getBatchSize(JsonNode connectionConfig) {
        return JsonUtils.getIntValue(connectionConfig, "batchSize", 10);
    }

    /**
     * 获取向量维度，各提供商默认不同：Bailian 默认 2560，Ollama 不传(null)
     */
    private Integer getDimension(JsonNode connectionConfig) {
        if (connectionConfig.has("dimension")) {
            return JsonUtils.getIntValue(connectionConfig, "dimension", 1024);
        }
        // Ollama 不需要维度参数
        String providerType = JsonUtils.getStringValue(connectionConfig, "providerType", "ollama");
        if ("bailian".equalsIgnoreCase(providerType)) {
            return 1024;
        }
        return null;
    }

    /**
     * Embed API 请求体
     */
    private record EmbedRequest(String model, List<String> input, Integer dimension) {
    }
}
