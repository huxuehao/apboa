package com.hxh.apboa.core.mcp;

import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.enums.McpProtocol;
import com.hxh.apboa.core.mcp.impl.HttpMcpClientConfig;
import com.hxh.apboa.core.mcp.impl.SseMcpClientConfig;
import com.hxh.apboa.core.mcp.impl.StdioMcpClientConfig;
import com.hxh.apboa.mcp.service.AgentMcpServerService;
import com.hxh.apboa.mcp.service.McpServerService;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 描述：MCP客户端工厂类
 *
 * @author huxuehao
 **/
@Component
@RequiredArgsConstructor
public class McpClientFactory {
    private static final Map<McpProtocol, McpClientConfig> INSTANCE = Map.of(
            McpProtocol.STDIO, new StdioMcpClientConfig(),
            McpProtocol.HTTP, new HttpMcpClientConfig(),
            McpProtocol.SSE, new SseMcpClientConfig()
    );

    private final McpServerService mcpServerService;
    private final AgentMcpServerService agentMcpServerService;

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
}
