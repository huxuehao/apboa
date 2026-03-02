package com.hxh.apboa.core.agent;

import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.core.agui.AgentContext;
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
        try {
            return reActAgentHelper.getReActAgent(agentId);
        } finally {
            AgentContext.clean();
        }
    }


    public ReActAgent getReActAgent(AgentDefinition definition) {
        try {
            return reActAgentHelper.getReActAgent(definition);
        } finally {
            AgentContext.clean();
        }
    }

    public ReActAgent.Builder getReActAgentBuilder(Long agentId) {
        try {
            return reActAgentHelper.getReActAgentBuilder(agentId);
        } finally {
            AgentContext.clean();
        }
    }

    public ReActAgent.Builder getReActAgentBuilder(AgentDefinition definition) {
        try {
            return reActAgentHelper.getReActAgentBuilder(definition);
        } finally {
            AgentContext.clean();
        }
    }
}
