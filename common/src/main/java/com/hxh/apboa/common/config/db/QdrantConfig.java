package com.hxh.apboa.common.config.db;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Qdrant 向量数据库客户端配置
 *
 * @author huxuehao
 */
@Configuration
@ConditionalOnProperty(name = "rag.qdrant.enabled", havingValue = "true", matchIfMissing = false)
public class QdrantConfig {

    private static final Logger log = LoggerFactory.getLogger(QdrantConfig.class);

    @Bean
    public QdrantClient qdrantClient(
            @Value("${rag.qdrant.host:localhost}") String host,
            @Value("${rag.qdrant.port:6334}") int port,
            @Value("${rag.qdrant.api-key:}") String apiKey) {

        QdrantGrpcClient.Builder grpcBuilder = QdrantGrpcClient.newBuilder(host, port, false);
        if (apiKey != null && !apiKey.isBlank()) {
            grpcBuilder.withApiKey(apiKey);
        }

        QdrantClient client = new QdrantClient(grpcBuilder.build());
        log.info("Qdrant客户端初始化完成, host={}, port={}", host, port);
        return client;
    }
}
