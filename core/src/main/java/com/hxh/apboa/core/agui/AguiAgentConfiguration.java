package com.hxh.apboa.core.agui;

import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.core.agent.IAgentFactory;
import io.agentscope.core.agui.registry.AguiAgentRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 描述：AGUI智能体注册
 *
 * @author huxuehao
 **/
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AguiAgentConfiguration implements ApplicationRunner {
    private final AgentDefinitionService agentDefinitionService;
    private final IAgentFactory iAgentFactory;
    private final AguiAgentRegistry registry;

    @Override
    public void run(ApplicationArguments args) {
        configureAgents(registry);
    }
    public void configureAgents(AguiAgentRegistry registry) {
        try {
            agentDefinitionService.list()
                    .stream()
                    .filter(item -> item.getEnabled() == true)
                    .forEach(agentDefinition ->
                        registry.registerFactory(
                                agentDefinition.getAgentCode(),
                                () -> iAgentFactory.getAgent(agentDefinition)));
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
                    () -> iAgentFactory.getAgent(agentDefinition));

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
