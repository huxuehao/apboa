package com.hxh.apboa.knowledge.service.impl;

import com.hxh.apboa.common.entity.AgentKnowledgeBase;
import com.hxh.apboa.knowledge.mapper.AgentKnowledgeBaseMapper;
import com.hxh.apboa.knowledge.service.AgentKnowledgeBaseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 智能体知识库关联Service实现
 *
 * @author huxuehao
 */
@Service
@RequiredArgsConstructor
public class AgentKnowledgeBaseServiceImpl extends ServiceImpl<AgentKnowledgeBaseMapper, AgentKnowledgeBase> implements AgentKnowledgeBaseService {
    @Override
    public List<Long> getAgentIds(List<Long> knowledgeIds) {
        return lambdaQuery()
                .in(AgentKnowledgeBase::getKnowledgeBaseConfigId, knowledgeIds)
                .list()
                .stream()
                .map(AgentKnowledgeBase::getAgentDefinitionId)
                .distinct()
                .toList();
    }

    @Override
    public List<Long> getKnowledgeIds(Long agentDefinitionId) {
        return lambdaQuery()
                .eq(AgentKnowledgeBase::getAgentDefinitionId, agentDefinitionId)
                .list()
                .stream()
                .map(AgentKnowledgeBase::getKnowledgeBaseConfigId)
                .toList();
    }

    @Override
    public Boolean insertAgentKnowledge(Long agentDefinitionId, List<Long> knowledgeIds) {
        knowledgeIds.forEach(knowledgeId -> {
            save(new AgentKnowledgeBase(null, agentDefinitionId, knowledgeId));
        });

        return true;
    }

    @Override
    public Boolean deleteAgentKnowledge(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return true;
        }
        return lambdaUpdate().in(AgentKnowledgeBase::getAgentDefinitionId, agentIds).remove();
    }

    @Override
    public Boolean saveAgentKnowledge(Long agentDefinitionId, List<Long> knowledgeIds) {
        deleteAgentKnowledge(List.of(agentDefinitionId));
        insertAgentKnowledge(agentDefinitionId, knowledgeIds);

        return Boolean.TRUE;
    }
}
