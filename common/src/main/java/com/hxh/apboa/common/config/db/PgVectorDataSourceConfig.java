package com.hxh.apboa.common.config.db;

import com.alibaba.druid.pool.DruidDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * PostgreSQL/pgvector数据源配置
 *
 * @author huxuehao
 */
@Configuration
@ConditionalOnProperty(name = "rag.pgvector.enabled", havingValue = "true", matchIfMissing = false)
public class PgVectorDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(PgVectorDataSourceConfig.class);

    @Bean(name = "pgVectorDataSource")
    public DataSource pgVectorDataSource(
            @Value("${rag.pgvector.url:jdbc:postgresql://localhost:5432/apboa_vector}") String url,
            @Value("${rag.pgvector.username:postgres}") String username,
            @Value("${rag.pgvector.password:postgres}") String password,
            @Value("${rag.pgvector.driver-class-name:org.postgresql.Driver}") String driverClassName) {

        ensureDatabaseExists(url, username, password, driverClassName);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setInitialSize(2);
        dataSource.setMinIdle(2);
        dataSource.setMaxActive(10);
        dataSource.setMaxWait(60000);
        dataSource.setValidationQuery("SELECT 1");
        log.info("PgVector数据源初始化完成, url={}", url);
        return dataSource;
    }

    /**
     * 自动创建数据库（如果不存在）
     */
    private void ensureDatabaseExists(String url, String username, String password, String driverClassName) {
        try {
            Class.forName(driverClassName);

            String dbName = extractDatabaseName(url);
            String baseUrl = url.replace("/" + dbName, "/postgres");

            try (Connection conn = DriverManager.getConnection(baseUrl, username, password);
                 Statement stmt = conn.createStatement()) {
                String checkSql = "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'";
                var rs = stmt.executeQuery(checkSql);
                if (!rs.next()) {
                    stmt.executeUpdate("CREATE DATABASE \"" + dbName + "\"");
                    log.info("自动创建PgVector数据库: {}", dbName);
                }
            }
        } catch (Exception e) {
            log.warn("PgVector数据库自动创建检查失败（可能已存在）: {}", e.getMessage());
        }
    }

    private String extractDatabaseName(String url) {
        int lastSlash = url.lastIndexOf('/');
        int queryStart = url.indexOf('?', lastSlash);
        if (queryStart > 0) {
            return url.substring(lastSlash + 1, queryStart);
        }
        return url.substring(lastSlash + 1);
    }
}
