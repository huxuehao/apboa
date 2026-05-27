package com.hxh.apboa.common.config.db;

import io.weaviate.client.Config;
import io.weaviate.client.WeaviateAuthClient;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.v1.auth.exception.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Weaviate 向量数据库客户端配置
 * <p>
 * 支持以下场景：
 * <ul>
 *   <li>HTTP（明文）- 默认</li>
 *   <li>HTTPS + 公共 CA 证书 - 无需额外配置</li>
 *   <li>HTTPS + API Key 认证 - 设置 rag.weaviate.api-key</li>
 * </ul>
 *
 * @author wei.liu
 */
@Configuration
@ConditionalOnProperty(name = "rag.store", havingValue = "weaviate")
public class WeaviateConfig {

    private static final Logger log = LoggerFactory.getLogger(WeaviateConfig.class);

    @Bean
    public WeaviateClient weaviateClient(
            @Value("${rag.weaviate.host:localhost}") String host,
            @Value("${rag.weaviate.port:8080}") int port,
            @Value("${rag.weaviate.scheme:http}") String scheme,
            @Value("${rag.weaviate.api-key:}") String apiKey) {

        Config config = new Config(scheme, host + ":" + port);

        WeaviateClient client;
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                client = WeaviateAuthClient.apiKey(config, apiKey);
            } catch (AuthException e) {
                throw new RuntimeException("Weaviate API Key 认证失败", e);
            }
        } else {
            client = new WeaviateClient(config);
        }

        log.info("Weaviate客户端初始化完成, scheme={}, host={}, port={}",
                scheme, host, port);
        return client;
    }
}
