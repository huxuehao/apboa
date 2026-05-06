package com.hxh.apboa.core.rag.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * 嵌入服务提供商接口，定义各模型提供商的契约。
 * 新增提供商只需实现此接口并添加 @Component 即可自动注册。
 *
 * @author huxuehao
 */
public interface EmbeddingProvider {

    /**
     * 返回提供商类型标识，如 "ollama"、"bailian"
     */
    String getType();

    /**
     * 默认服务地址（完整URL，含路径）
     */
    String getDefaultBaseUrl();

    /**
     * 默认嵌入模型名称
     */
    String getDefaultModel();

    /**
     * 执行嵌入请求：添加鉴权Header，发送请求，返回原始响应字符串
     *
     * @param webClient       已配置好 baseUrl 和 exchangeStrategies 的 WebClient
     * @param path            请求路径（从完整URL中解析）
     * @param requestBody     已序列化的请求体 JSON 字符串
     * @param connectionConfig 知识库连接配置
     * @return 原始响应字符串
     */
    String execute(WebClient webClient, String path, String requestBody, JsonNode connectionConfig);

    /**
     * 解析响应体为向量列表
     *
     * @param responseBody 原始响应字符串
     * @return 向量列表，与输入文本一一对应
     */
    List<float[]> parseResponse(String responseBody);
}
