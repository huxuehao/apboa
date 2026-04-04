package com.hxh.apboa.studio.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxh.apboa.common.entity.AgentStudio;
import com.hxh.apboa.studio.mapper.AgentStudioMapper;
import com.hxh.apboa.studio.service.AgentStudioService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 描述：AgentStudioServiceImpl
 *
 * @author huxuehao
 **/
@Service
public class AgentStudioServiceImpl extends ServiceImpl<AgentStudioMapper, AgentStudio> implements AgentStudioService {
    @Override
    public List<Long> getAgentIds(List<Long> studioId) {
        return lambdaQuery()
                .in(AgentStudio::getStudioId, studioId)
                .list()
                .stream()
                .map(AgentStudio::getAgentDefinitionId)
                .distinct()
                .toList();
    }

    @Override
    public Long getStudioIdByAgentId(Long agentId) {
        return lambdaQuery()
                .in(AgentStudio::getAgentDefinitionId, agentId)
                .oneOpt()
                .map(AgentStudio::getStudioId).orElse(null);
    }

    @Override
    public Boolean insertAgentStudio(Long agentDefinitionId, List<Long> studioIds) {
        if (studioIds == null || studioIds.isEmpty()) {
            return false;
        }
        studioIds.forEach(studioId -> {
            save(new AgentStudio(null, agentDefinitionId, studioId));
        });

        return true;
    }

    @Override
    public Boolean deleteAgentStudio(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return true;
        }
        return lambdaUpdate().in(AgentStudio::getAgentDefinitionId, agentIds).remove();
    }

    @Override
    public Boolean saveAgentStudio(Long agentDefinitionId, List<Long> studioIds) {
        deleteAgentStudio(List.of(agentDefinitionId));
        insertAgentStudio(agentDefinitionId, studioIds);

        return Boolean.TRUE;
    }
}
