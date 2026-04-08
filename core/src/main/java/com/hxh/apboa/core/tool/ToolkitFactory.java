package com.hxh.apboa.core.tool;

import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.agent.service.AgentSubAgentService;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.ToolConfig;
import com.hxh.apboa.common.enums.ToolType;
import com.hxh.apboa.core.agent.A2aAgentHelper;
import com.hxh.apboa.core.agent.ReActAgentHelper;
import com.hxh.apboa.core.agui.AgentContext;
import com.hxh.apboa.core.hook.builtins.IConfirmationHook;
import com.hxh.apboa.core.mcp.McpClientFactory;
import com.hxh.apboa.core.tool.dynamices.DynamicAgentTool;
import com.hxh.apboa.tool.service.AgentToolService;
import com.hxh.apboa.tool.service.ToolService;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 描述：钩子工厂
 *
 * @author huxuehao
 **/
@Slf4j
@Component
public class ToolkitFactory {
    private final ToolService toolService;
    private final AgentToolService agentToolService;
    private final AgentSubAgentService agentSubAgentService;
    private final ReActAgentHelper reActAgentHelper;
    private final A2aAgentHelper a2aAgentHelper;
    private final McpClientFactory mcpClientFactory;
    private final AgentDefinitionService agentDefinitionService;

    public ToolkitFactory(ToolService toolService,
                          AgentToolService agentToolService,
                          AgentSubAgentService agentSubAgentService,
                          @Lazy
                          ReActAgentHelper reActAgentHelper,
                          @Lazy
                          A2aAgentHelper a2aAgentHelper,
                          McpClientFactory mcpClientFactory,
                          AgentDefinitionService agentDefinitionService) {
        this.toolService = toolService;
        this.agentToolService = agentToolService;
        this.agentSubAgentService = agentSubAgentService;
        this.reActAgentHelper = reActAgentHelper;
        this.a2aAgentHelper = a2aAgentHelper;
        this.mcpClientFactory = mcpClientFactory;
        this.agentDefinitionService = agentDefinitionService;
    }

    public Toolkit getToolkit(AgentDefinition agentDefinition) {
        List<Long> toolIds = agentToolService.getToolIds(agentDefinition.getId());
        Toolkit toolkit = getToolkit(toolIds);
        if (!toolIds.isEmpty()) {
            // 获取是否开启记忆
            Boolean isMemoryActive = AgentContext.getIfExists().map(AgentContext::isMemoryActive).orElse(false);
            // 注册工具
            toolService.listByIds(toolIds)
                    .stream()
                    .filter(ToolConfig::getEnabled)
                    .forEach(toolConfig -> {
                        // 内置工具注册
                        if (toolConfig.getToolType() == ToolType.BUILTIN) {
                            toolkit.registerTool(ToolsRegister.getTool(toolConfig.getClassPath()));
                        }
                        // 动态工具注册
                        else {
                            toolkit.registerTool(new DynamicAgentTool(toolConfig));
                        }

                        if (toolConfig.getNeedConfirm() && isMemoryActive) {
                            IConfirmationHook.setNeedConfirmTool(toolConfig.getToolId());
                        } else {
                            IConfirmationHook.removeNeedConfirmTool(toolConfig.getToolId());
                        }
                    });
        }

        // 注册MCP
        mcpClientFactory.getMcpClient(agentDefinition).forEach(
                (mcpClient) -> toolkit.registerMcpClient(mcpClient).block());

        // 注册 Agent as Tool
        List<Long> subAgentIds = agentSubAgentService.getSubAgentIds(agentDefinition.getId());
        if (!subAgentIds.isEmpty()) {
            registerSubAgents(toolkit, subAgentIds);
        }

        return toolkit;
    }

    public Toolkit getToolkit(List<Long> toolIds) {
        Toolkit toolkit = new Toolkit();
        if (!toolIds.isEmpty()) {
            // 获取是否开启记忆
            Boolean isMemoryActive = AgentContext.getIfExists().map(AgentContext::isMemoryActive).orElse(false);
            // 注册工具
            toolService.listByIds(toolIds)
                    .stream()
                    .filter(ToolConfig::getEnabled)
                    .forEach(toolConfig -> {
                        // 内置工具注册
                        if (toolConfig.getToolType() == ToolType.BUILTIN) {
                            toolkit.registerTool(ToolsRegister.getTool(toolConfig.getClassPath()));
                        }
                        // 动态工具注册
                        else {
                            toolkit.registerTool(new DynamicAgentTool(toolConfig));
                        }

                        if (toolConfig.getNeedConfirm() && isMemoryActive) {
                            IConfirmationHook.setNeedConfirmTool(toolConfig.getToolId());
                        } else {
                            IConfirmationHook.removeNeedConfirmTool(toolConfig.getToolId());
                        }
                    });
        }
        return toolkit;
    }

    private void registerSubAgents(Toolkit toolkit, List<Long> subAgentIds) {
        for (Long subAgentId : subAgentIds) {
            AgentDefinition definition = agentDefinitionService.getById(subAgentId);

            if (definition == null || !definition.getEnabled()) {
                continue;
            }

            try {
                // Agent as Tool
                switch (definition.getAgentType()) {
                    case CUSTOM:
                        toolkit.registration()
                                .subAgent(() -> reActAgentHelper.getReActAgent(definition),
                                        createSubAgentConfig(definition))
                                .apply();
                        break;
                    case A2A:
                        toolkit.registration()
                                .subAgent(() -> a2aAgentHelper.getA2aAgent(definition),
                                        createSubAgentConfig(definition))
                                .apply();
                        break;
                    default:
                        break;
                }
                log.debug("Register sub agent: {}", subAgentId);
            } catch (Exception e) {
                log.error("Registration of sub agent failed: {}", subAgentId, e);
            }
        }
    }

    private SubAgentConfig createSubAgentConfig(AgentDefinition definition) {
        return SubAgentConfig.builder()
                .toolName(definition.getAgentCode().toLowerCase())
                .description(definition.getDescription() != null ?
                        definition.getDescription() : definition.getName())
                .forwardEvents(true)
                .build();
    }
}
