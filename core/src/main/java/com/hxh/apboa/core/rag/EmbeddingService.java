package com.hxh.apboa.core.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.common.entity.KnowledgeBaseConfig;
import com.hxh.apboa.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * 嵌入服务，调用Ollama的Embedding API进行文本向量化
 *
 * @author huxuehao
 */
@Component
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 对文本列表进行向量化
     *
     * @param texts   文本列表
     * @param config  知识库配置（包含Ollama连接信息）
     * @return 向量列表，与输入文本一一对应
     */
    public List<float[]> embed(List<String> texts, KnowledgeBaseConfig config) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        String baseUrl = getOllamaBaseUrl(config);
        String modelName = getEmbeddingModelName(config);

        try {
            ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                    .build();

            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .exchangeStrategies(exchangeStrategies)
                    .build();

            String requestBody = objectMapper.writeValueAsString(new EmbedRequest(modelName, texts));

            String response = webClient.post()
                    .uri("/api/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseEmbeddingResponse(response);
        } catch (Exception e) {
            log.error("调用Ollama Embedding API失败, baseUrl={}, model={}", baseUrl, modelName, e);
            throw new RuntimeException("文本向量化失败", e);
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
        return results.getFirst();
    }

    private String getOllamaBaseUrl(KnowledgeBaseConfig config) {
        JsonNode connectionConfig = config.getConnectionConfig();
        String url = JsonUtils.getStringValue(connectionConfig, "ollamaBaseUrl", "http://localhost:11434");
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String getEmbeddingModelName(KnowledgeBaseConfig config) {
        JsonNode connectionConfig = config.getConnectionConfig();
        return JsonUtils.getStringValue(connectionConfig, "embeddingModel", "qwen3-embedding:4b");
    }

    private List<float[]> parseEmbeddingResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
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

    /**
     * Ollama Embed API请求体
     */
    private record EmbedRequest(String model, List<String> input) {
    }
}
