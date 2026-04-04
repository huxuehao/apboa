package com.hxh.apboa.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hxh.apboa.common.entity.AgentCodeExecution;

import java.util.List;

/**
 * 描述：AgentCodeExecutionService
 *
 * @author huxuehao
 **/
public interface AgentCodeExecutionService extends IService<AgentCodeExecution> {
    /**
     * 根据代码执行配置ID获取Agent ID列表
     *
     * @param codeExecutionIds 代码执行配置ID列表
     * @return Agent ID列表
     */
    List<Long> getAgentIds(List<Long> codeExecutionIds);

    /**
     * 根据Agent ID获取代码执行配置ID
     *
     * @param agentId Agent ID
     * @return 代码执行配置ID
     */
    Long getCodeExecutionIdByAgentId(Long agentId);

    /**
     * 插入Agent与代码执行配置的关联
     *
     * @param agentDefinitionId Agent ID
     * @param codeExecutionIds 代码执行配置ID列表
     * @return 是否成功
     */
    Boolean insertAgentCodeExecution(Long agentDefinitionId, List<Long> codeExecutionIds);

    /**
     * 删除Agent与代码执行配置的关联
     *
     * @param agentIds Agent ID列表
     * @return 是否成功
     */
    Boolean deleteAgentCodeExecution(List<Long> agentIds);

    /**
     * 保存Agent与代码执行配置的关联（先删后插）
     *
     * @param agentDefinitionId Agent ID
     * @param codeExecutionIds 代码执行配置ID列表
     * @return 是否成功
     */
    Boolean saveAgentCodeExecution(Long agentDefinitionId, List<Long> codeExecutionIds);
}
