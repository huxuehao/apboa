package com.hxh.apboa.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxh.apboa.agent.mapper.AgentCodeExecutionMapper;
import com.hxh.apboa.agent.service.AgentCodeExecutionService;
import com.hxh.apboa.common.entity.AgentCodeExecution;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 描述：AgentCodeExecutionServiceImpl
 *
 * @author huxuehao
 **/
@Service
public class AgentCodeExecutionServiceImpl extends ServiceImpl<AgentCodeExecutionMapper, AgentCodeExecution> implements AgentCodeExecutionService {
    @Override
    public List<Long> getAgentIds(List<Long> codeExecutionIds) {
        return lambdaQuery()
                .in(AgentCodeExecution::getCodeExecutionId, codeExecutionIds)
                .list()
                .stream()
                .map(AgentCodeExecution::getAgentDefinitionId)
                .distinct()
                .toList();
    }

    @Override
    public Long getCodeExecutionIdByAgentId(Long agentId) {
        return lambdaQuery()
                .eq(AgentCodeExecution::getAgentDefinitionId, agentId)
                .oneOpt()
                .map(AgentCodeExecution::getCodeExecutionId)
                .orElse(null);
    }

    @Override
    public Boolean insertAgentCodeExecution(Long agentDefinitionId, List<Long> codeExecutionIds) {
        if (codeExecutionIds == null || codeExecutionIds.isEmpty()) {
            return false;
        }
        codeExecutionIds.forEach(codeExecutionId -> {
            save(new AgentCodeExecution(null, agentDefinitionId, codeExecutionId));
        });

        return true;
    }

    @Override
    public Boolean deleteAgentCodeExecution(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return true;
        }
        return lambdaUpdate().in(AgentCodeExecution::getAgentDefinitionId, agentIds).remove();
    }

    @Override
    public Boolean saveAgentCodeExecution(Long agentDefinitionId, List<Long> codeExecutionIds) {
        deleteAgentCodeExecution(List.of(agentDefinitionId));
        insertAgentCodeExecution(agentDefinitionId, codeExecutionIds);

        return Boolean.TRUE;
    }
}
