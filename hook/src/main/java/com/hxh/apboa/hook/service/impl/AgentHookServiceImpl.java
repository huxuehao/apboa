package com.hxh.apboa.hook.service.impl;

import com.hxh.apboa.common.entity.AgentHook;
import com.hxh.apboa.hook.mapper.AgentHookMapper;
import com.hxh.apboa.hook.service.AgentHookService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能体Hook关联Service实现
 *
 * @author huxuehao
 */
@Service
public class AgentHookServiceImpl extends ServiceImpl<AgentHookMapper, AgentHook> implements AgentHookService {
    @Override
    public List<Long> getAgentIds(List<Long> hookIds) {
        return lambdaQuery()
                .in(AgentHook::getHookConfigId, hookIds)
                .list()
                .stream()
                .map(AgentHook::getAgentDefinitionId)
                .distinct()
                .toList();
    }

    @Override
    public List<Long> getHookIds(Long agentDefinitionId) {
        return lambdaQuery()
                .eq(AgentHook::getAgentDefinitionId, agentDefinitionId)
                .list()
                .stream()
                .map(AgentHook::getHookConfigId)
                .toList();
    }

    @Override
    public Boolean insertAgentHook(Long agentDefinitionId, List<Long> hookIds) {
        hookIds.forEach(hookId -> {
            save(new AgentHook(null, agentDefinitionId, hookId));
        });
        return true;
    }

    @Override
    public Boolean deleteAgentHook(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return true;
        }
        return lambdaUpdate().in(AgentHook::getAgentDefinitionId, agentIds).remove();
    }

    @Override
    public Boolean saveAgentHook(Long agentDefinitionId, List<Long> hookIds) {
        deleteAgentHook(List.of(agentDefinitionId));
        insertAgentHook(agentDefinitionId, hookIds);

        return Boolean.TRUE;
    }
}
