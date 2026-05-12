package com.hxh.apboa.core.rag.rerank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.core.rag.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述：Xinference 重排序服务提供商实现
 * 兼容 OpenAI 标准 ReRank API 格式
 *
 * @author huxuehao
 **/
@Component
public class XinferenceReRankProvider implements ReRankProvider {

    private static final Logger log = LoggerFactory.getLogger(XinferenceReRankProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "xinference";
    }

    @Override
    public String getDefaultBaseUrl() {
        return "http://localhost:9997/v1/rerank";
    }

    @Override
    public String getDefaultModel() {
        return "Qwen3-Reranker-4B";
    }

    @Override
    public String execute(WebClient webClient, String path, String requestBody, JsonNode connectionConfig) {
        String apiKey = EmbeddingService.resolveApiKey(connectionConfig);

        return webClient.post()
                .uri(path)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @Override
    public List<ReRankResult> parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode resultsNode = root.get("results");
            if (resultsNode == null || !resultsNode.isArray()) {
                throw new RuntimeException("ReRank响应格式异常: 缺少results字段");
            }

            List<ReRankResult> results = new ArrayList<>();
            for (JsonNode item : resultsNode) {
                int index = item.get("index").asInt();
                double score = item.get("relevance_score").asDouble();
                results.add(new ReRankResult(index, score));
            }

            results.sort((a, b) -> Double.compare(b.score(), a.score()));
            return results;
        } catch (Exception e) {
            log.error("解析Xinference ReRank响应失败", e);
            throw new RuntimeException("解析ReRank响应失败", e);
        }
    }
}
