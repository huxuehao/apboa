package com.hxh.apboa.knowledge.service;

import com.hxh.apboa.common.entity.AgentKnowledgeBase;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 智能体知识库关联Service
 *
 * @author huxuehao
 */
public interface AgentKnowledgeBaseService extends IService<AgentKnowledgeBase> {
    List<Long> getAgentIds(List<Long> knowledgeIds);
    List<Long> getKnowledgeIds(Long agentDefinitionId);
    Boolean insertAgentKnowledge(Long agentDefinitionId, List<Long> knowledgeIds);
    Boolean deleteAgentKnowledge(List<Long> agentIds);
    Boolean saveAgentKnowledge(Long agentDefinitionId, List<Long> knowledgeIds);
}
