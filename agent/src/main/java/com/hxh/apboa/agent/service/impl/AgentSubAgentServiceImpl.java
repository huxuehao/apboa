package com.hxh.apboa.agent.service.impl;

import com.hxh.apboa.agent.mapper.AgentSubAgentMapper;
import com.hxh.apboa.agent.service.AgentSubAgentService;
import com.hxh.apboa.common.entity.AgentSubAgent;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能体子智能体关联Service实现
 *
 * @author huxuehao
 */
@Service
public class AgentSubAgentServiceImpl extends ServiceImpl<AgentSubAgentMapper, AgentSubAgent> implements AgentSubAgentService {
    @Override
    public List<Long> getSubAgentIds(Long agentDefinitionId) {
        return lambdaQuery()
                .eq(AgentSubAgent::getSubAgentId, agentDefinitionId)
                .list()
                .stream()
                .map(AgentSubAgent::getParentAgentId).toList();
    }

    @Override
    public Boolean insertSubAgent(Long agentDefinitionId, List<Long> subAgentIds) {
        subAgentIds.forEach(subAgentId -> {
            save(new AgentSubAgent(null, agentDefinitionId, subAgentId));
        });

        return true;
    }

    @Override
    public Boolean deleteSubAgent(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return true;
        }
        return lambdaUpdate().in(AgentSubAgent::getParentAgentId, agentIds).remove();
    }

    @Override
    public Boolean saveSubAgent(Long agentDefinitionId, List<Long> subAgentIds) {
        deleteSubAgent(List.of(agentDefinitionId));
        insertSubAgent(agentDefinitionId, subAgentIds);

        return Boolean.TRUE;
    }
}
