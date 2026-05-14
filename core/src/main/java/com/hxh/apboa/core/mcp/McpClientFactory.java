package com.hxh.apboa.core.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.enums.McpProtocol;
import com.hxh.apboa.common.mcp.ToolSchemaRefresher;
import com.hxh.apboa.core.mcp.impl.HttpMcpClientConfig;
import com.hxh.apboa.core.mcp.impl.SseMcpClientConfig;
import com.hxh.apboa.core.mcp.impl.StdioMcpClientConfig;
import com.hxh.apboa.mcp.service.AgentMcpServerService;
import com.hxh.apboa.mcp.service.McpServerService;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 描述：MCP客户端工厂类
 *
 * @author huxuehao
 */
@Component
@RequiredArgsConstructor
public class McpClientFactory {
    private static final Map<McpProtocol, McpClientConfig> INSTANCE = Map.of(
            McpProtocol.STDIO, new StdioMcpClientConfig(),
            McpProtocol.HTTP, new HttpMcpClientConfig(),
            McpProtocol.SSE, new SseMcpClientConfig()
    );

    private static final Logger log = LoggerFactory.getLogger(McpClientFactory.class);

    private final McpServerService mcpServerService;
    private final AgentMcpServerService agentMcpServerService;
    private final ObjectMapper objectMapper;
    private final ToolSchemaRefresher toolSchemaRefresher;

    public List<McpClientWrapper> getMcpClient(AgentDefinition agentDefinition) {
        List<McpClientWrapper> mcpClients = new ArrayList<>();

        List<Long> mcpIds = agentMcpServerService.getMcpIds(agentDefinition.getId());
        for (Long mcpId : mcpIds) {
            McpServer mcpServer = mcpServerService.getById(mcpId);
            if (!mcpServer.getEnabled()) {
                continue;
            }

            mcpClients.add(INSTANCE.get(mcpServer.getProtocol()).getMcpClient(mcpServer));
        }

        return mcpClients;
    }

    /**
     * 为单个 MCP 服务器创建一个客户端包装器。供模式刷新任务使用，而非智能体启动时使用。
     */
    public McpClientWrapper getMcpClientForServer(McpServer mcpServer) {
        return INSTANCE.get(mcpServer.getProtocol()).getMcpClient(mcpServer);
    }

    /**
     * 根据缓存的模式构建 MCP 工具，且在创建智能体期间不连接 MCP 服务器。
     */
    public List<AgentTool> getLazyMcpTools(AgentDefinition agentDefinition) {
        List<AgentTool> result = new ArrayList<>();
        List<Long> mcpIds = agentMcpServerService.getMcpIds(agentDefinition.getId());

        for (Long mcpId : mcpIds) {
            McpServer mcpServer = mcpServerService.getById(mcpId);
            if (mcpServer == null || !mcpServer.getEnabled()) {
                continue;
            }

            List<McpSchema.Tool> cachedTools = parseCachedTools(mcpServer.getToolSchemas());
            if (cachedTools.isEmpty()) {
                log.warn("MCP '{}' has no cached tool schemas; skip lazy registration",
                        mcpServer.getName());
                toolSchemaRefresher.refreshToolSchemas(mcpServer);
                continue;
            }

            cachedTools.forEach(tool -> result.add(new LazyMcpAgentTool(
                    mcpServer.getName(),
                    tool,
                    () -> INSTANCE.get(mcpServer.getProtocol()).getMcpClient(mcpServer))));
        }
        return result;
    }

    private List<McpSchema.Tool> parseCachedTools(String toolSchemasJson) {
        if (toolSchemasJson == null || toolSchemasJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(toolSchemasJson,
                    new TypeReference<List<McpSchema.Tool>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse cached MCP tool schemas", e);
            return List.of();
        }
    }
}
