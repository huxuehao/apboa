package com.hxh.apboa.tool.service;

import com.hxh.apboa.common.entity.AgentTool;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 智能体工具关联Service
 *
 * @author huxuehao
 */
public interface AgentToolService extends IService<AgentTool> {
    List<Long> getAgentIds(List<Long> tools);
    List<Long> getToolIds(Long agentDefinitionId);
    Boolean insertAgentTool(Long agentDefinitionId, List<Long> toolIds);
    Boolean deleteAgentTool(List<Long> agentIds);
    Boolean saveAgentTool(Long agentDefinitionId, List<Long> toolIds);
}
