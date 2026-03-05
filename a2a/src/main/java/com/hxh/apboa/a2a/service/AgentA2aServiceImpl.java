package com.hxh.apboa.a2a.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxh.apboa.a2a.mapper.AgentA2aMapper;
import com.hxh.apboa.common.entity.AgentA2A;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 描述：AgentA2aServiceImpl
 *
 * @author huxuehao
 **/
@Service
public class AgentA2aServiceImpl extends ServiceImpl<AgentA2aMapper, AgentA2A> implements AgentA2aService {
    @Override
    public AgentA2A getA2aConfigByAgentId(Long agentId) {
        return lambdaQuery().eq(AgentA2A::getAgentDefinitionId, agentId).one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveA2aConfig(AgentA2A agentA2A) {
        lambdaUpdate()
                .eq(AgentA2A::getAgentDefinitionId, agentA2A.getAgentDefinitionId())
                .remove();

        return save(agentA2A);
    }

    @Override
    public boolean deleteA2aConfig(List<Long> agentIds) {
        return lambdaUpdate()
                .in(AgentA2A::getAgentDefinitionId, agentIds)
                .remove();
    }
}
