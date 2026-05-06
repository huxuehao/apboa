package com.hxh.apboa.core.rag.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Ollama 嵌入服务提供商实现
 *
 * @author huxuehao
 */
@Component
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getType() {
        return "ollama";
    }

    @Override
    public String getDefaultBaseUrl() {
        return "http://localhost:11434/api/embed";
    }

    @Override
    public String getDefaultModel() {
        return "qwen3-embedding:4b";
    }

    @Override
    public String execute(WebClient webClient, String path, String requestBody, JsonNode connectionConfig) {
        return webClient.post()
                .uri(path)
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
            JsonNode embeddingsNode = root.get("embeddings");
            if (embeddingsNode == null || !embeddingsNode.isArray()) {
                throw new RuntimeException("Ollama Embedding响应格式异常: 缺少embeddings字段");
            }

            List<float[]> result = new ArrayList<>();
            for (JsonNode embeddingNode : embeddingsNode) {
                float[] embedding = new float[embeddingNode.size()];
                for (int i = 0; i < embeddingNode.size(); i++) {
                    embedding[i] = (float) embeddingNode.get(i).asDouble();
                }
                result.add(embedding);
            }
            return result;
        } catch (Exception e) {
            log.error("解析Ollama Embedding响应失败", e);
            throw new RuntimeException("解析嵌入向量响应失败", e);
        }
    }
}
