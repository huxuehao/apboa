package com.hxh.apboa.core.rag.rerank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.common.util.JsonUtils;
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
 * 重排序服务门面，根据 providerType 路由到对应的 ReRankProvider 实现。
 * 新增提供商只需实现 ReRankProvider 接口并添加 @Component，无需修改本类。
 *
 * @author huxuehao
 */
@Component
public class ReRankService {

    private static final Logger log = LoggerFactory.getLogger(ReRankService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final List<ReRankProvider> providers;
    private final Map<String, ReRankProvider> providerMap = new HashMap<>();

    public ReRankService(List<ReRankProvider> providers) {
        this.providers = providers;
    }

    @PostConstruct
    private void init() {
        for (ReRankProvider provider : providers) {
            providerMap.put(provider.getType().toLowerCase(), provider);
            log.info("注册ReRankProvider: {} -> {}", provider.getType(), provider.getClass().getSimpleName());
        }
    }

    /**
     * 对候选文档进行重排序
     *
     * @param query     查询文本
     * @param documents 候选文档内容列表
     * @param config    知识库配置（从中读取 rerankingConfig）
     * @return 重排序结果列表，按相关性分数降序排列
     */
    public List<ReRankResult> rerank(String query, List<String> documents, KnowledgeBaseConfig config) {
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }

        JsonNode rerankingConfig = config.getRerankingConfig();
        if (rerankingConfig == null || !rerankingConfig.has("providerType")) {
            log.debug("ReRank配置未启用或providerType未设置");
            return buildDefaultResults(documents.size());
        }

        ReRankProvider provider = resolveProvider(rerankingConfig);
        String fullUrl = getFullUrl(rerankingConfig, provider);
        String modelName = getModelName(rerankingConfig, provider);
        int bufferSizeMb = getBufferSizeMb(rerankingConfig);
        int topN = getTopN(rerankingConfig, documents.size());

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

            String requestBody = objectMapper.writeValueAsString(
                    new ReRankRequest(modelName, query, documents, topN));
            String response = provider.execute(webClient, path, requestBody, rerankingConfig);
            List<ReRankResult> results = provider.parseResponse(response);

            log.debug("ReRank完成, provider={}, queryLen={}, docs={}, results={}",
                    provider.getType(), query.length(), documents.size(), results.size());
            return results;
        } catch (Exception e) {
            log.error("调用ReRank API失败, provider={}, baseUrl={}, model={}",
                    provider.getType(), fullUrl, modelName, e);
            return buildDefaultResults(documents.size());
        }
    }

    /**
     * 判断ReRank是否已启用
     */
    public boolean isEnabled(KnowledgeBaseConfig config) {
        JsonNode rerankingConfig = config.getRerankingConfig();
        return rerankingConfig != null
                && rerankingConfig.has("enabled")
                && rerankingConfig.get("enabled").asBoolean();
    }

    private ReRankProvider resolveProvider(JsonNode rerankingConfig) {
        String providerType = JsonUtils.getStringValue(rerankingConfig, "providerType", "siliconflow");
        ReRankProvider provider = providerMap.get(providerType.toLowerCase());
        if (provider == null) {
            throw new IllegalArgumentException("不支持的ReRank提供商: " + providerType + "，已注册: " + providerMap.keySet());
        }
        return provider;
    }

    private String getFullUrl(JsonNode rerankingConfig, ReRankProvider provider) {
        String url = JsonUtils.getStringValue(rerankingConfig, "baseUrl", provider.getDefaultBaseUrl());
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String getModelName(JsonNode rerankingConfig, ReRankProvider provider) {
        return JsonUtils.getStringValue(rerankingConfig, "model", provider.getDefaultModel());
    }

    private int getBufferSizeMb(JsonNode rerankingConfig) {
        return JsonUtils.getIntValue(rerankingConfig, "bufferSizeMb", 50);
    }

    private int getTopN(JsonNode rerankingConfig, int defaultValue) {
        return JsonUtils.getIntValue(rerankingConfig, "topN", defaultValue);
    }

    private List<ReRankResult> buildDefaultResults(int size) {
        List<ReRankResult> results = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            results.add(new ReRankResult(i, 0.0));
        }
        return results;
    }

    /**
     * ReRank API 请求体
     */
    private record ReRankRequest(String model, String query, List<String> documents, Integer top_n) {
    }
}
