package com.hxh.apboa.core.rag;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.ListDatabasesResponse;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.param.collection.CreateDatabaseParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 客户端配置，项目启动时自动创建数据库（对标 PgVectorDataSourceConfig 的 ensureDatabaseExists）
 *
 * @author huxuehao
 */
@Configuration
@ConditionalOnProperty(name = "rag.store", havingValue = "milvus")
public class MilvusConfig {

    private static final Logger log = LoggerFactory.getLogger(MilvusConfig.class);

    @Bean
    public MilvusServiceClient milvusServiceClient(
            @Value("${rag.milvus.host:127.0.0.1}") String host,
            @Value("${rag.milvus.port:19530}") int port,
            @Value("${rag.milvus.username:}") String username,
            @Value("${rag.milvus.password:}") String password,
            @Value("${rag.milvus.token:}") String token,
            @Value("${rag.milvus.database:default}") String database) {

        ensureDatabaseExists(host, port, username, password, token, database);

        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .withDatabaseName(database);

        applyAuth(builder, username, password, token);

        MilvusServiceClient client = new MilvusServiceClient(builder.build());
        log.info("Milvus客户端初始化完成, host={}, port={}, database={}", host, port, database);
        return client;
    }

    /**
     * 自动创建 Milvus 数据库（如果不存在），对标 PgVectorDataSourceConfig 的 ensureDatabaseExists
     */
    private void ensureDatabaseExists(String host, int port, String username, String password,
                                      String token, String database) {
        if ("default".equals(database)) {
            return;
        }

        ConnectParam.Builder tempBuilder = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port);

        applyAuth(tempBuilder, username, password, token);

        MilvusServiceClient tempClient = null;
        try {
            tempClient = new MilvusServiceClient(tempBuilder.build());
            R<ListDatabasesResponse> listResp = tempClient.listDatabases();
            if (listResp.getData() != null && !listResp.getData().getDbNamesList().contains(database)) {
                tempClient.createDatabase(
                        CreateDatabaseParam.newBuilder().withDatabaseName(database).build());
                log.info("自动创建Milvus数据库: {}", database);
            }
        } catch (Exception e) {
            log.warn("Milvus数据库自动创建检查失败（可能已存在或权限不足）: {}", e.getMessage());
        } finally {
            if (tempClient != null) {
                try {
                    tempClient.close(5000);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 应用认证信息：优先用户名/密码，其次 token
     */
    private void applyAuth(ConnectParam.Builder builder, String username, String password, String token) {
        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            builder.withAuthorization(username, password);
        } else if (token != null && !token.isBlank()) {
            builder.withAuthorization(token);
        }
    }
}
