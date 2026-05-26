package com.hxh.apboa.core;

import com.hxh.apboa.common.consts.TableConst;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.registry.AguiAgentRegistry;
import io.agentscope.core.session.Session;
import io.agentscope.core.session.mysql.MysqlSession;
import io.agentscope.spring.boot.agui.common.AguiProperties;
import io.agentscope.spring.boot.agui.common.ThreadSessionManager;
import io.agentscope.spring.boot.agui.mvc.AguiMvcController;
import io.agentscope.spring.boot.agui.webflux.AguiWebFluxHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 配置 MysqlSession 替代 InMemorySession，实现状态持久化
 *
 * 核心特性：
 * 1. 自动探测主数据源的数据库名，零配置
 * 2. 支持多层降级策略，保证健壮性
 * 3. 兼容主流连接池（HikariCP、Druid、Tomcat JDBC）
 * 4. 清晰的异常提示，便于问题排查
 *
 * @author huxuehao
 */
@Slf4j
@Configuration
@ConditionalOnClass({DataSource.class, MysqlSession.class})
public class ApboaAgentSessionConfig {

    /**
     * 创建 MysqlSession Bean
     * 自动从 DataSource 中提取数据库名，无需硬编码
     *
     * @param dataSource 主数据源
     * @return MysqlSession 实例
     */
    @Bean
    @Primary
    public Session agentSession(DataSource dataSource) {
        String databaseName = extractDatabaseName(dataSource);
        log.info("Initializing MysqlSession with database: {}", databaseName);
        return new MysqlSession(dataSource, databaseName, TableConst.AGENT_SCOPE_SESSIONS, true);
    }

    /**
     * 从 DataSource 中提取数据库名
     * 采用多层降级策略，确保尽可能获取到正确的数据库名
     *
     * @param dataSource 数据源
     * @return 数据库名
     * @throws IllegalStateException 无法获取数据库名时抛出
     */
    private String extractDatabaseName(DataSource dataSource) {
        // 策略1：从连接获取（最准确，支持所有数据库）
        String databaseName = getDatabaseNameFromConnection(dataSource);
        if (databaseName != null) {
            return databaseName;
        }

        // 策略2：从 JDBC URL 解析（兜底方案）
        databaseName = getDatabaseNameFromJdbcUrl(dataSource);
        if (databaseName != null) {
            log.warn("Failed to get database name from connection, fallback to URL parsing: {}", databaseName);
            return databaseName;
        }

        // 策略3：无法获取，抛出明确异常
        throw new IllegalStateException(
                "Cannot determine database name from DataSource. " +
                        "Please ensure your DataSource is properly configured with a default database. " +
                        "For HikariCP, check 'jdbcUrl' contains database name. " +
                        "For other connection pools, verify the connection returns non-null catalog/schema."
        );
    }

    /**
     * 通过 Connection 获取数据库名
     * 优先使用 catalog，如果为空则尝试 schema
     */
    private String getDatabaseNameFromConnection(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            String databaseName = conn.getCatalog();
            if (databaseName == null || databaseName.trim().isEmpty()) {
                databaseName = conn.getSchema();
            }

            if (databaseName != null && !databaseName.trim().isEmpty()) {
                log.debug("Successfully got database name from connection: {}", databaseName);
                return databaseName.trim();
            }

            log.debug("Connection returned empty catalog and schema");
        } catch (SQLException e) {
            log.debug("Failed to get database name from connection: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从 JDBC URL 解析数据库名
     * 支持 MySQL、PostgreSQL、H2、MariaDB 等主流数据库
     */
    private String getDatabaseNameFromJdbcUrl(DataSource dataSource) {
        String url = extractJdbcUrl(dataSource);
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        String databaseName = parseDatabaseNameFromUrl(url);
        if (databaseName != null && !databaseName.trim().isEmpty()) {
            log.debug("Successfully parsed database name from URL: {}", databaseName);
            return databaseName;
        }

        return null;
    }

    /**
     * 从 DataSource 中提取 JDBC URL
     * 支持主流连接池：HikariCP、Druid、Tomcat JDBC、dbcp2
     */
    private String extractJdbcUrl(DataSource dataSource) {
        String dataSourceType = dataSource.getClass().getName();

        try {
            // HikariCP
            if (dataSourceType.contains("Hikari")) {
                return (String) dataSource.getClass().getMethod("getJdbcUrl").invoke(dataSource);
            }

            // Druid
            if (dataSourceType.contains("Druid")) {
                return (String) dataSource.getClass().getMethod("getUrl").invoke(dataSource);
            }

            // Tomcat JDBC
            if (dataSourceType.contains("tomcat")) {
                return (String) dataSource.getClass().getMethod("getUrl").invoke(dataSource);
            }

            // Apache DBCP2
            if (dataSourceType.contains("dbcp")) {
                return (String) dataSource.getClass().getMethod("getUrl").invoke(dataSource);
            }

            // 通用反射尝试（最后的尝试）
            for (String methodName : new String[]{"getJdbcUrl", "getUrl", "getURL"}) {
                try {
                    return (String) dataSource.getClass().getMethod(methodName).invoke(dataSource);
                } catch (NoSuchMethodException ignored) {
                    // 继续尝试下一个方法名
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract JDBC URL from DataSource {}: {}", dataSourceType, e.getMessage());
        }

        return null;
    }

    /**
     * 从 JDBC URL 中解析数据库名
     * 支持的数据库格式：
     * - MySQL: jdbc:mysql://host:3306/database?params
     * - PostgreSQL: jdbc:postgresql://host:5432/database?params
     * - H2: jdbc:h2:~/database
     * - MariaDB: jdbc:mariadb://host:3306/database?params
     */
    private String parseDatabaseNameFromUrl(String url) {
        // MySQL / MariaDB
        Pattern mysqlPattern = Pattern.compile("jdbc:(mysql|mariadb)://[^/]+/([^?;]+)");
        Matcher matcher = mysqlPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(2);
        }

        // PostgreSQL
        Pattern pgPattern = Pattern.compile("jdbc:postgresql://[^/]+/([^?]+)");
        matcher = pgPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // H2 (文件模式)
        Pattern h2Pattern = Pattern.compile("jdbc:h2:.*[:/]([^;]+)");
        matcher = h2Pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Oracle
        Pattern oraclePattern = Pattern.compile("jdbc:oracle:thin:@.*[:/]([^:]+)");
        matcher = oraclePattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }

        log.warn("Unsupported JDBC URL format: {}", url);
        return null;
    }

    /**
     * 配置 AguiMvcController
     * 覆盖 agentscope-agui-spring-boot-starter 的默认配置
     */
    @Bean
    @Primary
    @ConditionalOnClass(name = "io.agentscope.spring.boot.agui.mvc.AguiMvcController")
    public AguiMvcController aguiMvcController(
            @Autowired JdbcTemplate jdbcTemplate,
            @Autowired(required = false) AguiAgentRegistry registry,
            @Autowired(required = false) ThreadSessionManager sessionManager,
            AguiProperties props,
            Session session) {

        if (registry == null) {
            log.warn("AguiAgentRegistry not found, skip AguiMvcController configuration");
            return null;
        }

        if (session != null) {
            registry.setSessionManager(sessionManager);
        }

        return AguiMvcController.builder()
                .agentRegistry(registry)
                .sessionManager(sessionManager)
                .serverSideMemory(props.isServerSideMemory())
                .session(session)
                .jdbcTemplate(jdbcTemplate)
                .sseTimeout(600000L)
                .config(buildAguiAdapterConfig(props))
                .build();
    }

    /**
     * 配置 AguiWebFluxHandler
     * 覆盖 agentscope-agui-spring-boot-starter 的默认配置
     */
    @Bean
    @Primary
    @ConditionalOnClass(name = "io.agentscope.spring.boot.agui.webflux.AguiWebFluxHandler")
    public AguiWebFluxHandler aguiWebFluxHandler(
            @Autowired JdbcTemplate jdbcTemplate,
            @Autowired(required = false) AguiAgentRegistry registry,
            @Autowired(required = false) ThreadSessionManager sessionManager,
            AguiProperties props,
            Session session) {

        if (registry == null) {
            log.warn("AguiAgentRegistry not found, skip AguiWebFluxHandler configuration");
            return null;
        }

        if (session != null) {
            registry.setSessionManager(sessionManager);
        }

        return AguiWebFluxHandler.builder()
                .agentRegistry(registry)
                .sessionManager(sessionManager)
                .serverSideMemory(props.isServerSideMemory())
                .session(session)
                .jdbcTemplate(jdbcTemplate)
                .config(buildAguiAdapterConfig(props))
                .build();
    }

    /**
     * 构建 Agui 适配器配置
     */
    private AguiAdapterConfig buildAguiAdapterConfig(AguiProperties props) {
        return AguiAdapterConfig.builder()
                .toolMergeMode(props.getDefaultToolMergeMode())
                .runTimeout(props.getRunTimeout())
                .emitStateEvents(props.isEmitStateEvents())
                .emitToolCallArgs(props.isEmitToolCallArgs())
                .enableReasoning(props.isEnableReasoning())
                .defaultAgentId(props.getDefaultAgentId())
                .build();
    }
}
