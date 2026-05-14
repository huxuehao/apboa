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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * 根据缓存的模式注册 MCP 工具，并仅在调用时初始化 MCP 客户端。
 */
public class LazyMcpAgentTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(LazyMcpAgentTool.class);

    private final String mcpServerName;
    private final McpSchema.Tool toolSchema;
    private final Supplier<McpClientWrapper> clientSupplier;
    private final Map<String, Object> parameters;
    private final Map<String, Object> outputSchema;
    private final AtomicReference<Mono<McpClientWrapper>> initAttempt = new AtomicReference<>();

    public LazyMcpAgentTool(
            String mcpServerName,
            McpSchema.Tool toolSchema,
            Supplier<McpClientWrapper> clientSupplier) {
        this.mcpServerName = mcpServerName;
        this.toolSchema = toolSchema;
        this.clientSupplier = clientSupplier;
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
        return initializedClient()
                .flatMap(client -> client.callTool(getName(), param.getInput()))
                .map(McpContentConverter::convertCallToolResult)
                .onErrorResume(e -> {
                    log.warn("MCP tool '{}' from '{}' unavailable: {}",
                            getName(), mcpServerName, e.getMessage());
                    return Mono.just(ToolResultBlock.error(unavailableMessage(e)));
                });
    }

    private Mono<McpClientWrapper> initializedClient() {
        Mono<McpClientWrapper> existing = initAttempt.get();
        if (existing != null) {
            return existing;
        }

        AtomicReference<Mono<McpClientWrapper>> self = new AtomicReference<>();
        Mono<McpClientWrapper> created = Mono.defer(() -> {
                    McpClientWrapper client = clientSupplier.get();
                    return client.initialize().thenReturn(client);
                })
                .doOnError(e -> initAttempt.compareAndSet(self.get(), null))
                .cache();
        self.set(created);

        if (initAttempt.compareAndSet(null, created)) {
            return created;
        }
        return initAttempt.get();
    }

    private String unavailableMessage(Throwable e) {
        String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return "MCP service '" + mcpServerName + "' is unavailable. Tool '" + getName()
                + "' cannot be used right now. Reason: " + reason;
    }
}
