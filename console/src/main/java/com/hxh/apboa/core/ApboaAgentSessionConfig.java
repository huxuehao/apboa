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

/**
 * 配置 MysqlSession 替代 InMemorySession，实现状态持久化
 *
 * @author huxuehao
 */
@Slf4j
@Configuration
@ConditionalOnClass({DataSource.class, MysqlSession.class})
public class ApboaAgentSessionConfig {

    private static final String DATABASE_NAME = "apboa";

    /**
     * 创建 MysqlSession Bean
     *
     * @param dataSource 数据源
     * @return MysqlSession 实例
     */
    @Bean
    @Primary
    public Session agentSession(DataSource dataSource) {
        return new MysqlSession(dataSource, DATABASE_NAME, TableConst.AGENT_SCOPE_SESSIONS, true);
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
