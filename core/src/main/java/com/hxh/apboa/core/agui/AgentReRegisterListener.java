package com.hxh.apboa.core.agui;

import com.hxh.apboa.agent.service.AgentDefinitionService;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.event.AgentReRegisterEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 描述：AgentReRegister事件监听器
 *
 * @author huxuehao
 **/
@Component
@RequiredArgsConstructor
public class AgentReRegisterListener {
    private final AgentDefinitionService agentDefinitionService;
    private final AguiAgentConfiguration aguiAgentConfiguration;

    @EventListener
    public void onAgentReRegisterEvent(AgentReRegisterEvent event) {
        AgentDefinition agentDefinition = agentDefinitionService.getById(event.agentId());
        if (agentDefinition != null) {
            aguiAgentConfiguration.reRegisterAgent(agentDefinition);
        }
    }
}
