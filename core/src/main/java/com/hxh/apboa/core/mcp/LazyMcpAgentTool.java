package com.hxh.apboa.core.mcp;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.core.tool.mcp.McpContentConverter;
import io.agentscope.core.tool.mcp.McpTool;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * 基于缓存工具目录注册的懒加载 MCP 工具。
 */
public class LazyMcpAgentTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(LazyMcpAgentTool.class);

    private final String mcpServerName;
    private final McpSchema.Tool toolSchema;
    private final Supplier<Mono<McpClientWrapper>> initializedClientSupplier;
    private final Map<String, Object> parameters;
    private final Map<String, Object> outputSchema;

    public LazyMcpAgentTool(
            String mcpServerName,
            McpSchema.Tool toolSchema,
            Supplier<Mono<McpClientWrapper>> initializedClientSupplier) {
        this.mcpServerName = mcpServerName;
        this.toolSchema = toolSchema;
        this.initializedClientSupplier = initializedClientSupplier;
        this.parameters = McpTool.convertMcpSchemaToParameters(toolSchema.inputSchema(), Set.of());
        this.outputSchema = toolSchema.outputSchema() != null
                ? new HashMap<>(toolSchema.outputSchema())
                : null;
    }

    @Override
    public String getName() {
        return toolSchema.name();
    }

    @Override
    public String getDescription() {
        return toolSchema.description() != null ? toolSchema.description() : "";
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public Map<String, Object> getOutputSchema() {
        return outputSchema;
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        return initializedClientSupplier.get()
                .flatMap(client -> client.callTool(getName(), param.getInput()))
                .map(McpContentConverter::convertCallToolResult)
                .onErrorResume(e -> {
                    log.warn("MCP tool '{}' from '{}' unavailable: {}",
                            getName(), mcpServerName, e.getMessage());
                    return Mono.just(ToolResultBlock.error(unavailableMessage(e)));
                });
    }

    private String unavailableMessage(Throwable e) {
        String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return "MCP service '" + mcpServerName + "' is unavailable. Tool '" + getName()
                + "' cannot be used right now. Reason: " + reason;
    }
}
