package com.hxh.apboa.core.rag.embedding;

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
 * 百炼 (Bailian / DashScope) 嵌入服务提供商实现
 *
 * @author huxuehao
 */
@Component
public class BailianEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(BailianEmbeddingProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "bailian";
    }

    @Override
    public String getDefaultBaseUrl() {
        return "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";
    }

    @Override
    public String getDefaultModel() {
        return "text-embedding-v4";
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
    public List<float[]> parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // OpenAI兼容格式: {"data": [{"embedding": [...], "index": 0}, ...]}
            JsonNode dataNode = root.get("data");
            if (dataNode == null || !dataNode.isArray()) {
                throw new RuntimeException("Bailian Embedding响应格式异常: 缺少data字段");
            }

            List<float[]> result = new ArrayList<>();
            for (JsonNode item : dataNode) {
                JsonNode embeddingNode = item.get("embedding");
                if (embeddingNode != null && embeddingNode.isArray()) {
                    float[] embedding = new float[embeddingNode.size()];
                    for (int i = 0; i < embeddingNode.size(); i++) {
                        embedding[i] = (float) embeddingNode.get(i).asDouble();
                    }
                    result.add(embedding);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("解析Bailian Embedding响应失败", e);
            throw new RuntimeException("解析嵌入向量响应失败", e);
        }
    }
}
