package com.hxh.apboa.core.agent;

import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.core.agui.AgentContext;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.agent.Agent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 描述：智能体工厂类
 *
 * @author huxuehao
 **/
@Component
@RequiredArgsConstructor
public class IAgentFactory {
    private final A2aAgentHelper a2aAgentHelper;
    private final ReActAgentHelper reActAgentHelper;
    private final AgentDefinitionService agentDefinitionService;

    /**
     * 根据Agent定义ID获取Agent
     * @param agentId Agent ID
     */
    public Agent getAgent(Long agentId) {
        try {
            AgentDefinition definition = agentDefinitionService.getById(agentId);
            validAgentDefinition(definition);

            return switch (definition.getAgentType()) {
                case CUSTOM -> getReActAgent(definition);
                case A2A -> getA2aAgent(definition);
                default -> throw new IllegalArgumentException("未知的智能体类型");
            };
        } catch (Exception e) {
            AgentContext.clean();
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据Agent定义获取Agent
     * @param definition Agent 定义
     */
    public Agent getAgent(AgentDefinition definition) {
        try {
            return switch (definition.getAgentType()) {
                case CUSTOM -> getReActAgent(definition);
                case A2A -> getA2aAgent(definition);
                default -> throw new IllegalArgumentException("未知的智能体类型");
            };
        } catch (Exception e) {
            AgentContext.clean();
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据Agent定义ID获取A2aAgent
     * @param agentId Agent ID
     */
    public A2aAgent getA2aAgent(Long agentId) {
        try {
            AgentDefinition definition = agentDefinitionService.getById(agentId);
            validAgentDefinition(definition);

            return a2aAgentHelper.getA2aAgent(definition);
        } catch (Exception e) {
            AgentContext.clean();
            throw new RuntimeException(e);
        }
    }


    /**
     * 根据Agent定义获取A2aAgent
     * @param definition Agent 定义
     */
    public A2aAgent getA2aAgent(AgentDefinition definition) {
        try {
            return a2aAgentHelper.getA2aAgent(definition);
        } catch (Exception e) {
            AgentContext.clean();
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据Agent定义ID获取ReActAgent
     * @param agentId Agent ID
     */
    public ReActAgent getReActAgent(Long agentId) {
        try {
            AgentDefinition definition = agentDefinitionService.getById(agentId);
            validAgentDefinition(definition);

            return reActAgentHelper.getReActAgent(definition);
        } catch (Exception e) {
            AgentContext.clean();
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据Agent定义获取ReActAgent
     * @param definition Agent 定义
     */
    public ReActAgent getReActAgent(AgentDefinition definition) {
        try {
            return reActAgentHelper.getReActAgent(definition);
        } catch (Exception e) {
            AgentContext.clean();
            throw new RuntimeException(e);
        }
    }

    private void validAgentDefinition(AgentDefinition definition) {
        if (definition == null) {
            throw new RuntimeException("Agent not found" );
        }

        if (!definition.getEnabled()) {
            throw new RuntimeException("Agent is disabled, agentCode: " + definition.getAgentCode());
        }
    }
}
