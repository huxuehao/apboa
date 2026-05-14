package com.hxh.apboa.core.mcp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.cluster.core.MessagePublisher;
import com.hxh.apboa.common.consts.RedisChannelTopic;
import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.enums.McpProtocol;
import com.hxh.apboa.common.mcp.ToolSchemaRefresher;
import com.hxh.apboa.core.mcp.impl.HttpMcpClientConfig;
import com.hxh.apboa.core.mcp.impl.SseMcpClientConfig;
import com.hxh.apboa.core.mcp.impl.StdioMcpClientConfig;
import com.hxh.apboa.mcp.mapper.McpServerMapper;
import com.hxh.apboa.mcp.service.AgentMcpServerService;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @author huxuehao
 */
@Component
@RequiredArgsConstructor
public class ToolSchemaRefresherImpl implements ToolSchemaRefresher, ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ToolSchemaRefresherImpl.class);
    private static final Map<McpProtocol, McpClientConfig> CLIENT_CONFIGS = Map.of(
            McpProtocol.STDIO, new StdioMcpClientConfig(),
            McpProtocol.HTTP, new HttpMcpClientConfig(),
            McpProtocol.SSE, new SseMcpClientConfig()
    );

    private final ObjectMapper objectMapper;
    private final McpServerMapper mcpServerMapper;
    private final AgentMcpServerService agentMcpServerService;
    private final MessagePublisher messagePublisher;
    private final Set<Long> refreshing = ConcurrentHashMap.newKeySet();

    @Override
    public void run(ApplicationArguments args) {
        mcpServerMapper.selectList(new LambdaQueryWrapper<McpServer>()
                        .eq(McpServer::getEnabled, true)
                        .and(wrapper -> wrapper
                                .isNull(McpServer::getToolSchemas)
                                .or()
                                .eq(McpServer::getToolSchemas, "")))
                .forEach(this::refreshToolSchemas);
    }

    @Override
    public void refreshToolSchemas(McpServer server) {
        if (server == null || server.getId() == null || !refreshing.add(server.getId())) {
            return;
        }
        try {
            McpClientWrapper wrapper = CLIENT_CONFIGS.get(server.getProtocol()).getMcpClient(server);
            wrapper.initialize()
                    .then(Mono.defer(wrapper::listTools))
                    .doOnNext(tools -> {
                        try {
                            String json = objectMapper.writeValueAsString(tools);
                            McpServer update = new McpServer();
                            update.setId(server.getId());
                            update.setToolSchemas(json);
                            mcpServerMapper.updateById(update);
                            publishAgentReregister(server.getId());
                        } catch (Exception e) {
                            log.warn("Failed to serialize tool schemas for MCP '{}': {}",
                                    server.getName(), e.getMessage());
                        }
                    })
                    .doOnError(e -> log.warn(
                            "Failed to refresh tool schemas for MCP '{}': {}",
                            server.getName(), e.getMessage()))
                    .onErrorResume(e -> Mono.empty())
                    .doFinally(signalType -> {
                        refreshing.remove(server.getId());
                        wrapper.close();
                    })
                    .subscribe();
        } catch (Exception e) {
            refreshing.remove(server.getId());
            log.warn("Failed to create MCP client for schema refresh: {}", e.getMessage());
        }
    }

    private void publishAgentReregister(Long mcpServerId) {
        agentMcpServerService.getAgentIds(java.util.List.of(mcpServerId))
                .forEach(agentId -> messagePublisher.publish(
                        RedisChannelTopic.AGENT_REREGISTER_CHANNEL, String.valueOf(agentId)));
    }
}
