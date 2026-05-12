package com.hxh.apboa.core.rag.rerank;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * 重排序服务提供商接口，定义各ReRank模型提供商的契约。
 * 新增提供商只需实现此接口并添加 @Component 即可自动注册。
 *
 * @author huxuehao
 */
public interface ReRankProvider {

    /**
     * 返回提供商类型标识，如 "xinference"
     */
    String getType();

    /**
     * 默认服务地址（完整URL，含路径）
     */
    String getDefaultBaseUrl();

    /**
     * 默认重排序模型名称
     */
    String getDefaultModel();

    /**
     * 执行重排序请求：添加鉴权Header，发送请求，返回原始响应字符串
     *
     * @param webClient       已配置好 baseUrl 和 exchangeStrategies 的 WebClient
     * @param path            请求路径（从完整URL中解析）
     * @param requestBody     已序列化的请求体 JSON 字符串
     * @param connectionConfig 知识库连接配置
     * @return 原始响应字符串
     */
    String execute(WebClient webClient, String path, String requestBody, JsonNode connectionConfig);

    /**
     * 解析响应体为排序结果列表
     *
     * @param responseBody 原始响应字符串
     * @return 排序结果列表，按相关性分数降序排列
     */
    List<ReRankResult> parseResponse(String responseBody);
}
