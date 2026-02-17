package com.hxh.apboa.core.agui;

import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.core.agent.ReActAgentFactory;
import io.agentscope.core.agui.registry.AguiAgentRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * 描述：AGUI智能体注册
 *
 * @author huxuehao
 **/
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AguiAgentConfiguration {
    private final AgentDefinitionService agentDefinitionService;
    private final ReActAgentFactory reActAgentFactory;

    private AguiAgentRegistry registry;

    @Autowired
    public void configureAgents(AguiAgentRegistry registry) {
        this.registry = registry;
        try {
            agentDefinitionService.list()
                    .stream()
                    .filter(item -> item.getEnabled() == true)
                    .forEach(agentDefinition ->
                        registry.registerFactory(
                                agentDefinition.getAgentCode(),
                                () -> reActAgentFactory.getReActAgent(agentDefinition)));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 重新注册智能体
     * @param agentDefinition 智能体定义
     */
    public void reRegisterAgent(AgentDefinition agentDefinition) {
        if (registry == null) return;
        if (agentDefinition.getEnabled() == false) {
            unregisterAgent(agentDefinition);
            return;
        }

        try {
            unregisterAgent(agentDefinition);
            registry.registerFactory(
                    agentDefinition.getAgentCode(),
                    () -> reActAgentFactory.getReActAgent(agentDefinition));

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 注销单个智能体注册
     *
     * @param agentDefinition 智能体
     */
    private void unregisterAgent(AgentDefinition agentDefinition) {
        try {
            registry.unregister(agentDefinition.getAgentCode());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
