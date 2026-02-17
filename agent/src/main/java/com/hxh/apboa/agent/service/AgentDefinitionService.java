package com.hxh.apboa.agent.service;

import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.vo.AgentDefinitionVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 智能体定义Service
 *
 * @author huxuehao
 */
public interface AgentDefinitionService extends IService<AgentDefinition> {
    AgentDefinitionVO agentDefinitionDetail(Long id);
    Boolean saveAgentDefinition(AgentDefinitionVO agentDefinition);
    Boolean updateAgentDefinition(AgentDefinitionVO agentDefinition);
    Boolean deleteAgentDefinition(List<Long> ids);
    List<Object> usedWithAgent(List<Long> ids);
    /**
     * 获取所有Tag
     *
     * @return Tag列表
     */
    List<String> listTags();
}
