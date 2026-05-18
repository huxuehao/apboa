package com.hxh.apboa.core.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.McpServer;
import com.hxh.apboa.common.entity.McpTool;
import com.hxh.apboa.common.enums.McpActivationStatus;
import com.hxh.apboa.common.enums.McpProtocol;
import com.hxh.apboa.common.enums.McpToolExposureMode;
import com.hxh.apboa.common.vo.AgentMcpBindingVO;
import com.hxh.apboa.core.mcp.impl.HttpMcpClientConfig;
import com.hxh.apboa.core.mcp.impl.SseMcpClientConfig;
import com.hxh.apboa.core.mcp.impl.StdioMcpClientConfig;
import com.hxh.apboa.mcp.service.AgentMcpServerService;
import com.hxh.apboa.mcp.service.McpRuntimeDegradeService;
import com.hxh.apboa.mcp.service.McpServerService;
import com.hxh.apboa.mcp.service.McpToolService;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * MCP 客户端工厂
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
    private final McpToolService mcpToolService;
    private final McpRuntimeDegradeService mcpRuntimeDegradeService;
    private final ObjectMapper objectMapper;
    private final Map<Long, SharedMcpClientContext> sharedContexts = new ConcurrentHashMap<>();

    public List<McpClientWrapper> getMcpClient(AgentDefinition agentDefinition) {
        List<McpClientWrapper> mcpClients = new ArrayList<>();

        List<Long> mcpIds = agentMcpServerService.getMcpIds(agentDefinition.getId());
        for (Long mcpId : mcpIds) {
            McpServer mcpServer = mcpServerService.getById(mcpId);
            if (!isRuntimeAvailable(mcpServer)) {
                closeStaleContext(mcpId);
                continue;
            }

            mcpClients.add(INSTANCE.get(mcpServer.getProtocol()).getMcpClient(mcpServer));
        }

        return mcpClients;
    }

    /**
     * 为单个 MCP 服务创建客户端包装器，供刷新工具目录时使用。
     */
    public McpClientWrapper getMcpClientForServer(McpServer mcpServer) {
        return INSTANCE.get(mcpServer.getProtocol()).getMcpClient(mcpServer);
    }

    /**
     * 根据落库的工具目录构建 MCP 工具，并且在创建智能体期间不连接 MCP 服务。
     */
    public List<AgentTool> getLazyMcpTools(AgentDefinition agentDefinition) {
        List<AgentTool> result = new ArrayList<>();
        List<AgentMcpBindingVO> bindings = agentMcpServerService.getBindings(agentDefinition.getId());

        for (AgentMcpBindingVO binding : bindings) {
            Long mcpId = binding.getMcpServerId();
            McpServer mcpServer = mcpServerService.getById(mcpId);
            if (!isRuntimeAvailable(mcpServer)) {
                closeStaleContext(mcpId);
                continue;
            }

            mcpToolService.ensureBackfilledFromCache(mcpServer);
            List<McpTool> runtimeTools = mcpToolService.listRuntimeTools(mcpId);
            if (binding.getExposureMode() == McpToolExposureMode.SELECTED_ONLY) {
                Set<Long> selectedIds = new HashSet<>(binding.getMcpToolIds() == null
                        ? List.of()
                        : binding.getMcpToolIds());
                runtimeTools = runtimeTools.stream()
                        .filter(tool -> selectedIds.contains(tool.getId()))
                        .toList();
            }

            if (runtimeTools.isEmpty()) {
                closeStaleContext(mcpId);
                log.warn("MCP '{}' has no runtime tools after governance filtering; skip lazy registration",
                        mcpServer.getName());
                continue;
            }

            LazyMcpAgentTool.RuntimeDegradeContext degradeContext = new LazyMcpAgentTool.RuntimeDegradeContext(
                    mcpServer.getId(),
                    mcpServer.getName(),
                    mcpServer.getActivationRevision(),
                    mcpServer.getConfigHash(),
                    mcpServer.getRuntimeFailThreshold());

            runtimeTools.forEach(tool -> {
                McpSchema.Tool toolSchema = parseToolSchema(tool);
                if (toolSchema == null) {
                    return;
                }
                result.add(new LazyMcpAgentTool(
                        degradeContext,
                        toolSchema,
                        () -> getInitializedClient(mcpServer.getId()),
                        mcpRuntimeDegradeService));
            });
        }
        return result;
    }

    public Mono<McpClientWrapper> getInitializedClient(Long mcpServerId) {
        McpServer current = mcpServerService.getById(mcpServerId);
        if (!isRuntimeAvailable(current)) {
            closeStaleContext(mcpServerId);
            return Mono.error(new IllegalStateException("MCP 当前不可用"));
        }

        String contextKey = buildContextKey(current);
        SharedMcpClientContext context = sharedContexts.compute(mcpServerId, (id, existing) -> {
            if (existing != null && Objects.equals(existing.contextKey, contextKey)) {
                return existing;
            }

            SharedMcpClientContext created = createContext(current, contextKey);
            if (existing != null) {
                closeContext(existing);
            }
            return created;
        });

        return context.initializedClient;
    }

    private SharedMcpClientContext createContext(McpServer mcpServer, String contextKey) {
        SharedMcpClientContext context = new SharedMcpClientContext(contextKey);
        context.initializedClient = Mono.defer(() -> {
                    McpClientWrapper client = getMcpClientForServer(mcpServer);
                    context.clientRef.set(client);
                    return client.initialize().thenReturn(client);
                })
                .doOnError(e -> {
                    SharedMcpClientContext current = sharedContexts.get(mcpServer.getId());
                    if (current == context) {
                        sharedContexts.remove(mcpServer.getId());
                    }
                    closeContext(context);
                })
                .cache();
        return context;
    }

    private void closeContext(SharedMcpClientContext context) {
        McpClientWrapper client = context.clientRef.getAndSet(null);
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.debug("Close MCP client context failed: {}", e.getMessage());
            }
        }
    }

    private void closeStaleContext(Long mcpServerId) {
        SharedMcpClientContext context = sharedContexts.remove(mcpServerId);
        if (context != null) {
            closeContext(context);
        }
    }

    private boolean isRuntimeAvailable(McpServer mcpServer) {
        return mcpServer != null
                && Boolean.TRUE.equals(mcpServer.getEnabled())
                && mcpServer.getActivationStatus() == McpActivationStatus.ACTIVE;
    }

    private String buildContextKey(McpServer mcpServer) {
        return mcpServer.getId()
                + ":"
                + (mcpServer.getActivationRevision() == null ? 0L : mcpServer.getActivationRevision())
                + ":"
                + (mcpServer.getConfigHash() == null ? "" : mcpServer.getConfigHash());
    }

    private McpSchema.Tool parseToolSchema(McpTool tool) {
        if (tool.getRawSchema() == null) {
            log.warn("MCP tool '{}' raw schema is empty", tool.getToolName());
            return null;
        }
        try {
            return objectMapper.treeToValue(tool.getRawSchema(), McpSchema.Tool.class);
        } catch (Exception e) {
            log.warn("Failed to parse MCP tool schema '{}': {}", tool.getToolName(), e.getMessage());
            return null;
        }
    }

    private static final class SharedMcpClientContext {
        private final String contextKey;
        private final AtomicReference<McpClientWrapper> clientRef = new AtomicReference<>();
        private Mono<McpClientWrapper> initializedClient;

        private SharedMcpClientContext(String contextKey) {
            this.contextKey = contextKey;
        }
    }
}
