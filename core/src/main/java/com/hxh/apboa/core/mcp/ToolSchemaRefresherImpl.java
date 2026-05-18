package com.hxh.apboa.core.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.enums.McpProtocol;
import com.hxh.apboa.common.mcp.ToolSchemaRefreshResult;
import com.hxh.apboa.common.mcp.ToolSchemaRefresher;
import com.hxh.apboa.core.mcp.impl.HttpMcpClientConfig;
import com.hxh.apboa.core.mcp.impl.SseMcpClientConfig;
import com.hxh.apboa.core.mcp.impl.StdioMcpClientConfig;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * MCP 工具目录刷新实现。
 *
 * @author huxuehao
 */
@Component
@RequiredArgsConstructor
public class ToolSchemaRefresherImpl implements ToolSchemaRefresher {
    private static final Logger log = LoggerFactory.getLogger(ToolSchemaRefresherImpl.class);
    private static final Map<McpProtocol, McpClientConfig> CLIENT_CONFIGS = Map.of(
            McpProtocol.STDIO, new StdioMcpClientConfig(),
            McpProtocol.HTTP, new HttpMcpClientConfig(),
            McpProtocol.SSE, new SseMcpClientConfig()
    );

    private final ObjectMapper objectMapper;

    @Override
    public ToolSchemaRefreshResult refreshToolSchemas(McpServer server) {
        if (server == null) {
            return new ToolSchemaRefreshResult(false, null, 0, "MCP 服务器不存在");
        }

        McpClientConfig clientConfig = CLIENT_CONFIGS.get(server.getProtocol());
        if (clientConfig == null) {
            return new ToolSchemaRefreshResult(false, null, 0, "不支持的 MCP 协议");
        }

        McpClientWrapper wrapper = null;
        try {
            wrapper = clientConfig.getMcpClient(server);
            List<?> tools = wrapper.initialize()
                    .then(Mono.defer(wrapper::listTools))
                    .block();
            String json = objectMapper.writeValueAsString(tools);
            int toolCount = tools == null ? 0 : tools.size();
            String message = toolCount == 0 ? "连接成功，但未发现可用工具" : "工具目录同步成功";
            return new ToolSchemaRefreshResult(true, json, toolCount, message);
        } catch (Exception e) {
            log.warn("Failed to refresh tool schemas for MCP '{}': {}", server.getName(), e.getMessage());
            return new ToolSchemaRefreshResult(false, null, 0, "工具目录同步失败: " + e.getMessage());
        } finally {
            if (wrapper != null) {
                wrapper.close();
            }
        }
    }
}
