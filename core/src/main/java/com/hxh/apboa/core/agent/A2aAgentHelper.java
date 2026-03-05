package com.hxh.apboa.core.agent;

import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.JsonNode;
import com.hxh.apboa.a2a.config.NacosAgentConfig;
import com.hxh.apboa.a2a.config.WellKnownAgentConfig;
import com.hxh.apboa.a2a.service.AgentA2aService;
import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.common.entity.AgentA2A;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.enums.AgentType;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.core.agui.AgentContext;
import com.hxh.apboa.core.hook.HooksFactory;
import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.a2a.agent.card.WellKnownAgentCardResolver;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.nacos.a2a.discovery.NacosAgentCardResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;

/**
 * 描述：A2a智能体Helper
 *
 * @author huxuehao
 **/
@Component
@RequiredArgsConstructor
public class A2aAgentHelper {
    private final HooksFactory hooksFactory;
    private final AgentA2aService agentA2aService;
    private final AgentDefinitionService agentDefinitionService;
    /**
     * 获取 A2aAgent
     * @param agentId agentId
     */
    public A2aAgent getA2aAgent(Long agentId) {
        AgentDefinition definition = agentDefinitionService.getById(agentId);
        if (definition == null) {
            throw new RuntimeException("Agent not found, agentId: " + agentId);
        }

        if (!definition.getEnabled()) {
            throw new RuntimeException("Agent is disabled, agentId: " + agentId);
        }

        if (definition.getAgentType() != AgentType.A2A) {
            throw new RuntimeException("Agent type is not a2a, agentId: " + agentId);
        }

        return getA2aAgent(definition);
    }

    /**
     * 获取 A2aAgent
     * @param definition agent 定义
     */
    public A2aAgent getA2aAgent(AgentDefinition definition) {
        AgentA2A agentA2A = agentA2aService.getA2aConfigByAgentId(definition.getId());
        if (agentA2A == null)
            throw new RuntimeException("Agent A2A config not found, agentId: " + definition.getId());

        return switch (agentA2A.getA2aType()) {
            case NACOS -> createNacosA2aAgent(agentA2A.getA2aConfig(), definition);
            case WELLKNOWN ->createWellknownA2aAgent(agentA2A.getA2aConfig(), definition);
        };
    }

    public A2aAgent createWellknownA2aAgent(JsonNode a2aConfig, AgentDefinition definition) {
        WellKnownAgentConfig config = JsonUtils.parse(a2aConfig.toString(), WellKnownAgentConfig.class);
        A2aAgent.Builder builder = A2aAgent.builder()
                .name(config.getAgentName())
                .agentCardResolver(WellKnownAgentCardResolver.builder()
                        .baseUrl(config.getBaseUrl())
                        .relativeCardPath(config.getRelativeCardPath())
                        .authHeaders(config.getRealAuthHeaders())
                        .build());

        return fillA2aAgentExpand(definition, builder);
    }

    public A2aAgent createNacosA2aAgent(JsonNode a2aConfig, AgentDefinition definition) {
        NacosAgentConfig nacosAgentConfig = JsonUtils.parse(a2aConfig.toString(), NacosAgentConfig.class);
        Properties nacosProperties = nacosAgentConfig.getNacosProperties();

        AiService aiService;
        try {
            aiService = AiFactory.createAiService(nacosProperties);
        } catch (NacosException e) {
            throw new RuntimeException("Nacos AI service initialization failed", e);
        }

        NacosAgentCardResolver nacosAgentCardResolver = new NacosAgentCardResolver(aiService);
        A2aAgent.Builder builder = A2aAgent.builder()
                .name(nacosAgentConfig.getAgentName())
                .agentCardResolver(nacosAgentCardResolver);

        return fillA2aAgentExpand(definition, builder);
    }

    private A2aAgent fillA2aAgentExpand(AgentDefinition definition, A2aAgent.Builder builder) {
        // 使用可变列表，避免 getHooks 返回 List.of() 时 add 抛 UnsupportedOperationException
        List<Hook> hooks = hooksFactory.getHooks(definition);
        if (hooks != null && !hooks.isEmpty()) {
            builder.hooks(hooks);
        }

        // 配置记忆
        Boolean isMemoryActive = AgentContext.getIfExists().map(AgentContext::isMemoryActive).orElse(false);
        if (definition.getEnableMemory() && isMemoryActive) {
            builder.memory(new InMemoryMemory());
        }

        return builder.build();
    }
}
