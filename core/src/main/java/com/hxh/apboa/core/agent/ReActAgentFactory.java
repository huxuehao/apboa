package com.hxh.apboa.core.agent;

import com.hxh.apboa.common.entity.AgentDefinition;
import io.agentscope.core.ReActAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 描述：智能体工厂类
 *
 * @author huxuehao
 **/
@Component
@RequiredArgsConstructor
public class ReActAgentFactory {
    private final ReActAgentHelper reActAgentHelper;

    public ReActAgent getReActAgent(Long agentId) {
        return reActAgentHelper.getReActAgent(agentId);
    }


    public ReActAgent getReActAgent(AgentDefinition definition) {
        return reActAgentHelper.getReActAgent(definition);
    }

    public ReActAgent.Builder getReActAgentBuilder(Long agentId) {
        return reActAgentHelper.getReActAgentBuilder(agentId);
    }

    public ReActAgent.Builder getReActAgentBuilder(AgentDefinition definition) {
        return reActAgentHelper.getReActAgentBuilder(definition);
    }
}
